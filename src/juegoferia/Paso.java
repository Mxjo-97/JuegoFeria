package juegoferia;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Un paso del guion. El guion es una lista de pasos que el MotorGuion ejecuta en orden.
 * Los pasos "bloqueantes" (dialogo, aviso, espera, clic, decision) esperan al jugador
 * o al tiempo; los demas (fondo, mostrar, ocultar, accion) pasan de inmediato al siguiente.
 *
 * Todas las coordenadas estan en la resolucion de diseno 1591 x 989.
 */
public class Paso {

    public enum Tipo {
        FONDO, MOSTRAR, OCULTAR, DIALOGO_IMG, DIALOGO_TXT, AVISO, ESPERA,
        ESPERAR_CLIC, DECISION, ACCION, CONTROL
    }

    final Tipo tipo;
    String ruta, quien, texto, id;
    int ms, x, y, w, h;
    Rectangle[] zonas;
    DatosDecision decision;
    Runnable accion;
    BooleanSupplier control;

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

    /** Pone una imagen suelta (icono, personaje) en una posicion. */
    public static Paso mostrar(String id, String ruta, int x, int y, int w, int h) {
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

    /** Dialogo que es una IMAGEN (burbuja ya hecha). Se muestra en la zona fija de dialogos. */
    public static Paso dialogoImg(String ruta) {
        Paso p = new Paso(Tipo.DIALOGO_IMG);
        p.ruta = ruta;
        return p;
    }

    /** Dialogo dibujado por codigo: nombre + texto, en la zona fija de dialogos. */
    public static Paso dialogo(String quien, String texto) {
        Paso p = new Paso(Tipo.DIALOGO_TXT);
        p.quien = quien;
        p.texto = texto;
        return p;
    }

    /** Aviso de imagen en el centro (ej: "nuevo coleccionable", "dia 1 de 7"). */
    public static Paso avisoImg(String ruta, int ms) {
        Paso p = new Paso(Tipo.AVISO);
        p.ruta = ruta;
        p.ms = ms;
        return p;
    }

    /** Aviso de texto en el centro. */
    public static Paso aviso(String texto, int ms) {
        Paso p = new Paso(Tipo.AVISO);
        p.texto = texto;
        p.ms = ms;
        return p;
    }

    public static Paso espera(int ms) {
        Paso p = new Paso(Tipo.ESPERA);
        p.ms = ms;
        return p;
    }

    /** Muestra una pista y espera a que el jugador toque UNA de las zonas. */
    public static Paso esperarClic(String pista, Rectangle... zonas) {
        Paso p = new Paso(Tipo.ESPERAR_CLIC);
        p.texto = pista;
        p.zonas = zonas;
        return p;
    }

    /** Abre el menu de decision de un arbol de publicacion. */
    public static Paso decision(DatosDecision d) {
        Paso p = new Paso(Tipo.DECISION);
        p.decision = d;
        return p;
    }

    /** Ejecuta codigo propio y sigue de inmediato. */
    public static Paso accion(Runnable r) {
        Paso p = new Paso(Tipo.ACCION);
        p.accion = r;
        return p;
    }

    /** Uso interno del motor: devuelve true si debe esperar. */
    static Paso control(BooleanSupplier c) {
        Paso p = new Paso(Tipo.CONTROL);
        p.control = c;
        return p;
    }

    // ===================== DATOS DE UNA DECISION =====================

    /**
     * Une un arbol (ArbolPublicacion) con lo que pasa en pantalla:
     *  - titulo: el texto de la publicacion que se ve en el menu
     *  - reaccion(idNodo, pasos...): pasos que se ejecutan al llegar a ese nodo
     *    (si es una hoja, antes se aplica su efecto al EstadoJuego)
     *  - resolver(idNodo, ...): si un nodo NO debe preguntar al jugador sino decidirse solo
     *    (ej: "se vuelve viral" al azar), se dice aqui que hijo elegir
     *  - limite(segundos): cuenta regresiva; si se acaba se elige el hijo "tiempo_agotado"
     */
    public static class DatosDecision {
        final Nodo raiz;
        String titulo;
        String tituloSubmenu = "¿Qué quieres hacer ahora?";
        int limiteSegundos = 0;
        final Map<String, List<Paso>> reacciones = new HashMap<>();
        final Map<String, Supplier<String>> resolutores = new HashMap<>();

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

        public DatosDecision reaccion(String idNodo, Paso... pasos) {
            reacciones.put(idNodo, Arrays.asList(pasos));
            return this;
        }

        /** eleccion devuelve el final del id del hijo a elegir (ej: "viral" o "noviral"). */
        public DatosDecision resolver(String idNodo, Supplier<String> eleccion) {
            resolutores.put(idNodo, eleccion);
            return this;
        }
    }
}
