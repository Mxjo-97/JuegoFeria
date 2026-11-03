package juegoferia;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructor de escenas. Cada escena es: un fondo (imagen) + una secuencia de dialogos (imagenes).
 *
 * Uso:
 *   Escena e = new Escena("ciudadano/e0");          // carpeta dentro de /imagenes/
 *   e.fondo("casa.png")                             // /imagenes/ciudadano/e0/casa.png
 *    .dialogo("Samuel", "Papa, con quien hablas?", "samuel_1.png")
 *    .dialogo("Ciudadano", "Con Nubia, mijo.", "ciudadano_1.png");
 *   List<Paso> pasos = e.build();
 *
 * - Todas las burbujas salen en la misma posicion (PantallaJuego.ZONA_DIALOGO).
 * - Si el PNG de la burbuja todavia no existe, se muestra el texto en una caja y el juego sigue.
 *   Cuando copies el PNG con ese nombre a la carpeta, pasa a usarse solo.
 * - e.resumen() imprime el orden de la escena y que imagenes faltan.
 */
public class Escena {

    private static final String RAIZ = "/imagenes/";

    private final String carpeta;
    private final List<Paso> pasos = new ArrayList<>();
    private final List<String> orden = new ArrayList<>();

    public Escena(String carpeta) {
        String c = carpeta.startsWith("/") ? carpeta.substring(1) : carpeta;
        this.carpeta = RAIZ + (c.endsWith("/") ? c : c + "/");
    }

    private String ruta(String archivo) {
        return archivo.startsWith("/") ? archivo : carpeta + archivo;
    }

    private void anotar(String tipo, String detalle, String archivo) {
        String marca = "";
        if (archivo != null) {
            marca = Recursos.existe(ruta(archivo)) ? "  [OK] " + archivo : "  [FALTA] " + archivo;
        }
        orden.add(String.format("%02d. %-9s %s%s", orden.size() + 1, tipo, detalle, marca));
    }

    // ===================== IMAGENES =====================

    /** Cambia el fondo de la escena (imagen de 1591 x 989). */
    public Escena fondo(String archivo) {
        pasos.add(Paso.fondo(ruta(archivo)));
        anotar("FONDO", "", archivo);
        return this;
    }

    /** Fondo que se queda en pantalla "ms" milisegundos y pasa solo al siguiente paso (tarjetas, cortes). */
    public Escena fondo(String archivo, int ms) {
        fondo(archivo);
        return espera(ms);
    }

    /** Imagen suelta encima del fondo (coordenadas de diseno 1591 x 989). */
    public Escena imagen(String id, String archivo, int x, int y, int w, int h) {
        pasos.add(Paso.mostrar(id, ruta(archivo), x, y, w, h));
        anotar("IMAGEN", id, archivo);
        return this;
    }

    public Escena quitarImagen(String id) {
        pasos.add(Paso.ocultar(id));
        anotar("QUITAR", id, null);
        return this;
    }

    /** Panel grande centrado (aviso, coleccionable, etc.) con imagen. */
    public Escena panel(String archivo, int ms) {
        pasos.add(Paso.avisoImg(ruta(archivo), ms));
        anotar("PANEL", ms + " ms", archivo);
        return this;
    }

    /** Aviso solo de texto. */
    public Escena aviso(String texto, int ms) {
        pasos.add(Paso.aviso(texto, ms));
        anotar("AVISO", texto.replace('\n', ' '), null);
        return this;
    }

    // ===================== DIALOGOS =====================

    /**
     * Un dialogo = un PNG transparente (la burbuja ya hecha). Siempre sale en la misma
     * posicion (PantallaJuego.ZONA_DIALOGO), da igual el tamano del lienzo del PNG.
     * Se avanza con clic, ESPACIO o ENTER. Si el PNG no existe, se ve un cuadro provisional
     * con su nombre, y en consola resumen() lo marca como [FALTA].
     *
     * @param archivo nombre del PNG, relativo a la carpeta de la escena (o ruta completa con "/")
     */
    public Escena dialogo(String archivo) {
        pasos.add(Paso.dialogoImg(ruta(archivo)));
        anotar("DIALOGO", "", archivo);
        return this;
    }

    /** Varios dialogos seguidos, en el orden dado: e.dialogos("a.png", "b.png", "c.png"). */
    public Escena dialogos(String... archivos) {
        for (String a : archivos) {
            dialogo(a);
        }
        return this;
    }

    // ===================== INTERACCION / CONTROL =====================

    /** Pausa en milisegundos (sin dialogo). */
    public Escena espera(int ms) {
        pasos.add(Paso.espera(ms));
        anotar("ESPERA", ms + " ms", null);
        return this;
    }

    /** Espera que el jugador toque una de las zonas (coordenadas de diseno). */
    public Escena esperarClic(String pista, Rectangle... zonas) {
        pasos.add(Paso.esperarClic(pista, zonas));
        anotar("CLIC", pista, null);
        return this;
    }

    /** Menu de decision con su arbol (ver ArbolPublicacion). */
    public Escena decision(Paso.DatosDecision datos) {
        pasos.add(Paso.decision(datos));
        anotar("DECISION", datos.raiz.descripcion, null);
        return this;
    }

    public Escena accion(String nombre, Runnable r) {
        pasos.add(Paso.accion(r));
        anotar("ACCION", nombre, null);
        return this;
    }

    /** Agrega un Paso ya hecho (para cosas que Escena no cubre). */
    public Escena paso(Paso p) {
        pasos.add(p);
        anotar("PASO", "", null);
        return this;
    }

    /** Une otra escena a continuacion de esta. */
    public Escena luego(Escena otra) {
        pasos.addAll(otra.pasos);
        orden.addAll(otra.orden);
        return this;
    }

    // ===================== SALIDA =====================

    public List<Paso> build() {
        return new ArrayList<>(pasos);
    }

    /** Imprime el orden de la escena y que imagenes faltan por agregar. */
    public Escena resumen() {
        System.out.println("===== ORDEN DE LA ESCENA (" + carpeta + ") =====");
        int faltan = 0;
        for (String linea : orden) {
            System.out.println(linea);
            if (linea.contains("[FALTA]")) {
                faltan++;
            }
        }
        System.out.println("Imagenes que faltan: " + faltan);
        return this;
    }

    private static String recortar(String t) {
        String s = t.replace('\n', ' ');
        return s.length() > 60 ? s.substring(0, 57) + "..." : s;
    }
}
