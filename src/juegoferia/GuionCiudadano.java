package juegoferia;

import java.util.ArrayList;
import java.util.List;

/**
 * Escenas NO JUGABLES del rol CIUDADANO, Dia 1 (cinematicas con fondo + dialogos en PNG).
 * Se reproducen con MotorGuion sobre PantallaJuego ANTES de que empiece la parte jugable,
 * que sigue siendo la de partvisual (movimiento, celular, panaderia, decisiones).
 * Cada escena se arma con Escena (ver Escena.java).
 *
 * COMO SE AGREGA UNA ESCENA:
 *   Escena e = new Escena("ciudadano/e0");      // carpeta: src/imagenes/ciudadano/e0/
 *   e.fondo("casa.png");                        // imagen de fondo (1591 x 989)
 *   e.dialogo("samuel_1.png");                  // un dialogo = un PNG transparente tuyo
 *   e.dialogos("a.png", "b.png", "c.png");      // varios dialogos seguidos, en ese orden
 *
 * Todos los dialogos salen en la MISMA posicion (PantallaJuego.ZONA_DIALOGO).
 * Si un PNG no esta en la carpeta, se ve un cuadro provisional con su nombre y resumen() lo marca [FALTA].
 */
public class GuionCiudadano {

    // Duraciones (ms) de las imagenes que pasan solas. Cambialas aqui.
    private static final int T_TARJETA = 2500;   // pantallas negras con texto
    private static final int T_IMAGEN = 3500;    // imagenes de escena sin dialogo

    /** Dev: true = salta las escenas no jugables y entra directo a la parte jugable (para probar rapido). */
    public static boolean SALTAR_ESCENAS = false;

    /**
     * Todas las escenas no jugables, en orden. Cuando terminan, partvisual arranca la parte jugable.
     * Para agregar una escena nueva: crea un metodo como escena0() y anadelo aqui.
     */
    public static List<Paso> escenasNoJugables() {
        List<Paso> g = new ArrayList<>();
        if (!SALTAR_ESCENAS) {
            g.addAll(prologo().build());
            g.addAll(escena0().build());
            // g.addAll(escenaNueva().build());   // <- las proximas escenas no jugables, aqui
        }
        return g;
    }

    /** Imprime en consola el orden de cada escena y las imagenes que faltan. */
    public static void resumenTodo() {
        prologo().resumen();
        escena0().resumen();
    }

       // ===================== PROLOGO: HACE TRES SEMANAS / ACTUALMENTE =====================
    // Imagenes en src/imagenes/ciudadano/prologo/

    public static Escena prologo() {
        Escena e = new Escena("ciudadano/prologo");

        e.fondo("hace_tres_semanas.png", T_TARJETA);   // tarjeta negra
        e.fondo("playa_noche.png", T_IMAGEN);       // playa de noche, la chica con el celular
        e.dialogo("151.png");
        e.dialogo("152.png");
        e.dialogo("153.png");
        e.fondo("playa_melquiades.png", T_IMAGEN);     // aparece el viejo vendedor
        e.dialogo("154.png");
        e.dialogo("155.png");
        e.dialogo("156.png");
   

        e.fondo("actualmente.png", T_TARJETA);         // tarjeta negra
        e.fondo("plaza.png", T_IMAGEN);                // la plaza, dos hombres con el celular
        // e.dialogos("tu_png_3.png");                  // <- tus dialogos de la plaza, en orden

        return e;
    }

    // ===================== ESCENA 0: TU CASA, ANTES DE AMANECER =====================
    // Imagenes en src/imagenes/ciudadano/e0/

    public static Escena escena0() {
        Escena e = new Escena("ciudadano/prologo");

        e.fondo("casa.png");
        // e.dialogos("tu_png_1.png", "tu_png_2.png", ...);   // <- tus dialogos, en el orden del guion

        return e;
    }
}
