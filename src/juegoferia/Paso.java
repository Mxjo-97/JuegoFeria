package juegoferia;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Un paso del guion. El guion es una lista de pasos
 * que el MotorGuion ejecuta en orden.
 *
 * Todas las coordenadas están en la resolución
 * de diseño 1591 x 989.
 */
public class Paso {

    public enum Tipo {
        FONDO,
        MOSTRAR,
        OCULTAR,
        DIALOGO_IMG,
        DIALOGO_TXT,
        AVISO,
        CONSECUENCIA,
        ESPERA,
        ESPERAR_CLIC,
        DECISION,
        SELECCION_ZONAS,
        ACCION,
        CONTROL
    }

    final Tipo tipo;

    String ruta, quien, texto, id;

    int ms, x, y, w, h;

    Rectangle[] zonas;

    DatosDecision decision;

    Runnable accion;

    BooleanSupplier control;

    // NUEVO: historias correspondientes a cada zona seleccionable
    List<List<Paso>> ramas;


    private Paso(Tipo tipo) {
        this.tipo = tipo;
    }


    // ===================== FABRICAS =====================

    /** Cambia la imagen de fondo. */
    public static Paso fondo(String ruta) {
        Paso p = new Paso(Tipo.FONDO);
        p.ruta = ruta;
        return p;
    }

    /** Pone una imagen suelta en una posición. */
    public static Paso mostrar(
            String id,
            String ruta,
            int x,
            int y,
            int w,
            int h) {

        Paso p = new Paso(Tipo.MOSTRAR);

        p.id = id;
        p.ruta = ruta;
        p.x = x;
        p.y = y;
        p.w = w;
        p.h = h;

        return p;
    }

    public static Paso ocultar(String id) {
        Paso p = new Paso(Tipo.OCULTAR);
        p.id = id;
        return p;
    }

    /** Diálogo mediante imagen. */
    public static Paso dialogoImg(String ruta) {
        Paso p = new Paso(Tipo.DIALOGO_IMG);
        p.ruta = ruta;
        return p;
    }

    /** Diálogo mediante texto. */
    public static Paso dialogo(String quien, String texto) {
        Paso p = new Paso(Tipo.DIALOGO_TXT);
        p.quien = quien;
        p.texto = texto;
        return p;
    }

    /** Aviso de imagen. */
    public static Paso avisoImg(String ruta, int ms) {
        Paso p = new Paso(Tipo.AVISO);
        p.ruta = ruta;
        p.ms = ms;
        return p;
    }

    /** Aviso de texto. */
    public static Paso aviso(String texto, int ms) {
        Paso p = new Paso(Tipo.AVISO);
        p.texto = texto;
        p.ms = ms;
        return p;
    }

    /** Consecuencia: imagen centrada que se quita con un clic. */
    public static Paso consecuencia(String ruta) {
        Paso p = new Paso(Tipo.CONSECUENCIA);
        p.ruta = ruta;
        return p;
    }

    public static Paso espera(int ms) {
        Paso p = new Paso(Tipo.ESPERA);
        p.ms = ms;
        return p;
    }

    /** Espera un clic en una de las zonas indicadas. */
    public static Paso esperarClic(
            String pista,
            Rectangle... zonas) {

        Paso p = new Paso(Tipo.ESPERAR_CLIC);

        p.texto = pista;
        p.zonas = zonas;

        return p;
    }


    // ===================== NUEVA SELECCIÓN DE ZONAS =====================

    /**
     * Permite seleccionar una de varias zonas de una imagen.
     *
     * Cada zona tiene asociada una historia diferente.
     *
     * Al pulsar una zona, se ejecuta únicamente
     * la historia correspondiente.
     */
    @SafeVarargs
    public static Paso seleccionarZonas(
            Rectangle[] zonas,
            List<Paso>... ramas) {

        if (zonas == null || ramas == null) {
            throw new IllegalArgumentException(
                "Las zonas y las historias no pueden ser nulas."
            );
        }

        if (zonas.length == 0 || zonas.length != ramas.length) {
            throw new IllegalArgumentException(
                "Debe existir una zona por cada historia."
            );
        }

        Paso p = new Paso(Tipo.SELECCION_ZONAS);

        p.zonas = zonas.clone();

        p.ramas = new ArrayList<>();

        for (List<Paso> rama : ramas) {

            if (rama == null) {
                throw new IllegalArgumentException(
                    "Una de las historias está vacía o es nula."
                );
            }

            p.ramas.add(new ArrayList<>(rama));
        }

        return p;
    }


    /** Abre el menú de decisión de un árbol de publicación. */
    public static Paso decision(DatosDecision d) {
        Paso p = new Paso(Tipo.DECISION);
        p.decision = d;
        return p;
    }

    /** Ejecuta código propio y continúa inmediatamente. */
    public static Paso accion(Runnable r) {
        Paso p = new Paso(Tipo.ACCION);
        p.accion = r;
        return p;
    }

    /** Uso interno del motor. */
    static Paso control(BooleanSupplier c) {
        Paso p = new Paso(Tipo.CONTROL);
        p.control = c;
        return p;
    }


    // ===================== DATOS DE UNA DECISIÓN =====================

    /**
     * Une un árbol (ArbolPublicacion) con lo que pasa en pantalla.
     */
    public static class DatosDecision {

        final Nodo raiz;

        String titulo;

        String tituloSubmenu = "¿Qué quieres hacer ahora?";

        int limiteSegundos = 0;

        final Map<String, List<Paso>> reacciones =
                new HashMap<>();

        final Map<String, Supplier<String>> resolutores =
                new HashMap<>();

        public DatosDecision(Nodo raiz) {
            this.raiz = raiz;
        }

        public DatosDecision titulo(String t) {
            this.titulo = t;
            return this;
        }

        public DatosDecision tituloSubmenu(String t) {
            this.tituloSubmenu = t;
            return this;
        }

        public DatosDecision limite(int segundos) {
            this.limiteSegundos = segundos;
            return this;
        }

        public DatosDecision reaccion(
                String idNodo,
                Paso... pasos) {

            reacciones.put(
                idNodo,
                Arrays.asList(pasos)
            );

            return this;
        }

        /** Permite elegir automáticamente una opción. */
        public DatosDecision resolver(
                String idNodo,
                Supplier<String> eleccion) {

            resolutores.put(idNodo, eleccion);

            return this;
        }
    }
}
