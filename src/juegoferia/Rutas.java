package juegoferia;

/**
 * Rutas de imagenes que usa el motor de escenas (Escena / MotorGuion / PantallaJuego).
 * Las pantallas del juego jugable viven en partvisual y tienen sus propias rutas.
 * Si una imagen no existe, el juego NO se cae: muestra un cuadro provisional con la ruta.
 */
public class Rutas {

    private static final String D = "/imagenes/dialogos/";

    // ---- Paneles grandes (se usan con Paso.avisoImg) ----
    public static final String P_NUEVO_COLECCIONABLE = D + "nuevocoleccionable.png";

    /** Si el coleccionable tiene panel propio devuelve su ruta; si no, null (se usa aviso de texto). */
    public static String panelColeccionable(String nombre) {
        if ("El rumor del cierre del colegio".equals(nombre)) {
            return P_NUEVO_COLECCIONABLE;
        }
        return null;
    }
}
