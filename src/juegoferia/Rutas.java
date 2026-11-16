package juegoferia;

/**
 * Todas las rutas de imagenes del juego en UN solo lugar.
 *
 * Cada fondo tiene una LISTA: se usa la primera imagen que exista. Primero van TUS rutas de
 * siempre (panaderiasin.png, malecon.png...) y despues los nombres nuevos que propone el
 * LEEME (por si algun dia haces el arte nuevo). Si no existe ninguna, se ve un fondo provisional.
 * Para cambiar un fondo basta editar su linea aqui.
 */
public final class Rutas {

    private Rutas() {
    }

    private static final String I = "/imagenes/";
    private static final String F = "/imagenes/fondos/";

    // ---- fondos de los lugares ----
    public static final String[] CASA = {I + "casapuertasin.png", F + "casa_pasillo.png"};
    public static final String[] CASA_CENA = {I + "casapuertasin.png", F + "casa_cena.png"};
    public static final String[] PANADERIA = {I + "panaderia.png"};
    public static final String[] CAMINO_MALECON = {I + "camino_malecon.png"};
    public static final String[] MALECON = {I + "malecon.png", F + "malecon.png"};
    public static final String[] JUNTA = {I + "junta_piso1.png", I + "junta.png"};
    public static final String[] PLAZA_APAGON = {I + "alcaldia.png", I + "exteriorsin.png", I + "malecon.png"};
    public static final String[] ALCALDIA = {I + "interioralcaldia.png"};

    // ---- pantallas ----
    public static final String[] COMO_JUGAR = {I + "instrucciones.png", F + "como_jugar.png"};
    public static final String[] ELEGIR_PERSONAJE = {F + "elegir_personaje.png", I + "fondoinicio.png"};
    public static final String FONDO_INICIO = I + "fondoinicio.png";

    // ---- personajes ----
    /** Los vecinos: /imagenes/personajes/<id>.png (igual que antes). */
    public static final String PERSONAJES = I + "personajes/";

    /**
     * Sprite quieto de un rol. Prueba: carpeta nueva (personajes/ciudadano/quieto.png), la ruta de
     * siempre (personajes/ciudadano.png) y, para el Ciudadano, personajecivil.png.
     * Devuelve null si no hay ninguno (se dibuja una figurita provisional).
     */
    public static String spriteQuieto(Rol r) {
        String[] pruebas = {
            r.sprites() + "quieto.png",
            PERSONAJES + r.carpeta + ".png",
            r == Rol.CIUDADANO ? I + "personajecivil.png" : null
        };
        for (String p : pruebas) {
            if (p != null && Recursos.existe(p)) {
                return p;
            }
        }
        return null;
    }

    /** Primera ruta de la lista que exista (o la primera, para que el aviso de "falta" diga cual). */
    public static String primera(String... rutas) {
        for (String r : rutas) {
            if (Recursos.existe(r)) {
                return r;
            }
        }
        return rutas.length > 0 ? rutas[0] : null;
    }
}
