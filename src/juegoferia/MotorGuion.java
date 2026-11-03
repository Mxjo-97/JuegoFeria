package juegoferia;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ejecuta un guion (lista de Paso) sobre una PantallaJuego.
 * Es la unica clase que "sabe" de arboles de decision y del EstadoJuego;
 * la pantalla solo dibuja y avisa cuando el jugador hizo clic.
 */
public class MotorGuion {

    private final PantallaJuego pantalla;
    private final EstadoJuego estado;
    private final LinkedList<Paso> pendientes = new LinkedList<>();
    private boolean enBucle = false;
    private Nodo ultimoNodo;
    private Runnable alTerminar;

    public MotorGuion(PantallaJuego pantalla, EstadoJuego estado) {
        this.pantalla = pantalla;
        this.estado = estado;
    }

    public EstadoJuego getEstado() {
        return estado;
    }

    /** Ultimo nodo del arbol al que llego el jugador. */
    public Nodo getUltimoNodo() {
        return ultimoNodo;
    }

    public void iniciar(List<Paso> pasos, Runnable alTerminar) {
        pendientes.clear();
        pendientes.addAll(pasos);
        this.alTerminar = alTerminar;
        continuar();
    }

    /** Corta el guion (para volver al menu). Hay que llamar tambien a pantalla.reiniciar(). */
    public void detener() {
        pendientes.clear();
        alTerminar = null;
    }

    /** Ejecuta pasos hasta toparse con uno que tenga que esperar. */
    public void continuar() {
        if (enBucle) {
            return;
        }
        enBucle = true;
        try {
            while (!pendientes.isEmpty()) {
                Paso p = pendientes.poll();
                if (ejecutar(p)) {
                    return;
                }
            }
        } finally {
            enBucle = false;
        }
        pantalla.ocultarDialogo();
        Runnable fin = alTerminar;
        alTerminar = null;
        if (fin != null) {
            fin.run();
        }
    }

    /** @return true si el paso es bloqueante (el motor sigue cuando la pantalla llame a continuar). */
    private boolean ejecutar(Paso p) {
        switch (p.tipo) {
            case FONDO:
                pantalla.setFondo(p.ruta);
                return false;
            case MOSTRAR:
                pantalla.mostrarSprite(p.id, p.ruta, p.x, p.y, p.w, p.h);
                return false;
            case OCULTAR:
                pantalla.ocultarSprite(p.id);
                return false;
            case ACCION:
                p.accion.run();
                return false;
            case CONTROL:
                return p.control.getAsBoolean();
            case DIALOGO_IMG:
                pantalla.mostrarDialogoImagen(p.ruta, this::continuar);
                return true;
            case DIALOGO_TXT:
                pantalla.mostrarDialogoTexto(p.quien, p.texto, this::continuar);
                return true;
            case AVISO:
                pantalla.mostrarAviso(p.ruta, p.texto, p.ms, this::continuar);
                return true;
            case ESPERA:
                pantalla.esperar(p.ms, this::continuar);
                return true;
            case ESPERAR_CLIC:
                pantalla.esperarClic(p.texto, p.zonas, this::continuar);
                return true;
            case DECISION:
                return abrirMenuODecidir(p.decision.raiz, p.decision);
            default:
                return false;
        }
    }

    // ===================== DECISIONES =====================

    /**
     * Muestra el menu con los hijos de "nodo" o, si hay un resolutor para ese nodo,
     * elige el hijo automaticamente.
     * @return true si queda esperando al jugador
     */
    private boolean abrirMenuODecidir(Nodo nodo, Paso.DatosDecision d) {
        Supplier<String> resolutor = d.resolutores.get(nodo.id);
        if (resolutor != null) {
            Nodo hijo = nodo.buscarHijo(resolutor.get());
            if (hijo == null && !nodo.hijos.isEmpty()) {
                hijo = nodo.hijos.get(0);
            }
            if (hijo != null) {
                prepararEleccion(hijo, d);
                return false;
            }
        }

        // el hijo "tiempo_agotado" nunca sale como boton
        List<Nodo> opciones = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        for (Nodo h : nodo.hijos) {
            if (!h.id.endsWith("tiempo_agotado")) {
                opciones.add(h);
                etiquetas.add(h.descripcion.toUpperCase());
            }
        }
        if (opciones.isEmpty()) {
            return false;
        }

        boolean esRaiz = (nodo == d.raiz);
        String titulo = esRaiz ? (d.titulo != null ? d.titulo : nodo.descripcion) : d.tituloSubmenu;
        int limite = esRaiz ? d.limiteSegundos : 0;

        pantalla.mostrarMenu(titulo, etiquetas, limite,
                i -> {
                    prepararEleccion(opciones.get(i), d);
                    continuar();
                },
                () -> {
                    Nodo agotado = d.raiz.buscarHijo("tiempo_agotado");
                    if (agotado != null) {
                        prepararEleccion(agotado, d);
                    }
                    continuar();
                });
        return true;
    }

    /**
     * El jugador (o el resolutor) llego a "hijo". Se mete al principio de la cola lo que sigue:
     *  - hoja: se aplica el efecto YA, luego su reaccion y, si trae, el aviso de coleccionable
     *  - nodo interno: su reaccion y luego el siguiente menu
     */
    private void prepararEleccion(Nodo hijo, Paso.DatosDecision d) {
        ultimoNodo = hijo;
        System.out.println("[Decision] " + hijo.id + " -> " + hijo.descripcion);

        List<Paso> lista = new ArrayList<>();
        List<Paso> reaccion = d.reacciones.get(hijo.id);

        if (hijo.esHoja()) {
            estado.aplicarEfecto(hijo.efecto, hijo.descripcion);
            estado.imprimirEstadoPartida();
            if (reaccion != null) {
                lista.addAll(reaccion);
            }
            if (hijo.coleccionable != null) {
                System.out.println("[Coleccionable] " + hijo.coleccionable.nombre);
                String panel = Rutas.panelColeccionable(hijo.coleccionable.nombre);
                lista.add(panel != null
                        ? Paso.avisoImg(panel, 2200)
                        : Paso.aviso("Nuevo coleccionable:\n" + hijo.coleccionable.nombre, 2200));
            }
        } else {
            if (reaccion != null) {
                lista.addAll(reaccion);
            }
            lista.add(Paso.control(() -> abrirMenuODecidir(hijo, d)));
        }
        pendientes.addAll(0, lista);
    }
}
