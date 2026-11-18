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

    /**
     * Lista de un fondo OFICIAL (los de fondos_renombrados.zip, mas plaza_apagon.png): se prueba
     * /imagenes/<nombre>.png y /imagenes/fondos/<nombre>.png, y despues los respaldos (nombres viejos).
     * Asi da igual en cual de las dos carpetas los pongan, y el nuevo siempre gana sobre el viejo.
     */
    private static String[] oficial(String nombre, String... respaldo) {
        String[] r = new String[2 + respaldo.length];
        r[0] = I + nombre + ".png";
        r[1] = F + nombre + ".png";
        for (int k = 0; k < respaldo.length; k++) {
            r[2 + k] = respaldo[k];
        }
        return r;
    }

    // ---- fondos de los lugares (DIA 1) ----
    public static final String[] CASA = oficial("casapuertasin", F + "casa_pasillo.png");
    public static final String[] CASA_CENA = oficial("casa_cena", I + "casapuertasin.png");
    /** Panaderia del Dia 1: la nueva (panaderia.png) antes que la vieja (panaderiasin.png). */
    public static final String[] PANADERIA = oficial("panaderia", I + "panaderiasin.png", F + "panaderiasin.png");
    public static final String[] CAMINO_MALECON = oficial("camino_malecon", I + "exteriorsin.png");
    public static final String[] MALECON = oficial("malecon");
    public static final String[] JUNTA = oficial("junta_piso1", I + "junta.png", F + "junta.png");
    /** El apagon: la plaza del malecon de tarde, con el cielo raro y los faroles apagados. */
    public static final String[] PLAZA_APAGON = oficial("plaza_apagon", I + "malecon.png", F + "malecon.png");
    public static final String[] ALCALDIA = oficial("interioralcaldia", I + "alcaldia.png", F + "alcaldia.png");

    // ---- fondos del DIA 2 ----
    public static final String[] CASA_DIA2 = oficial("casapuertasin", F + "casa_pasillo.png");
    /** Panaderia del Dia 2 (la tele dice "Día 2 de 2"); si falta, la del Dia 1. */
    public static final String[] PANADERIA_DIA2 = oficial("panaderia_dia2", I + "panaderia.png", F + "panaderia.png",
            I + "panaderiasin.png");
    /** Camino a la Alcaldia: la fachada (alcaldia.png). */
    public static final String[] CAMINO_ALCALDIA = oficial("alcaldia", I + "camino_malecon.png", F + "camino_malecon.png");
    /** La fachada con las puertas abiertas: se pone cuando el grupo decide como entrar. */
    public static final String[] ALCALDIA_PUERTAS = oficial("alcaldia_puertas_abiertas", I + "alcaldia.png", F + "alcaldia.png");
    /** Primer piso de la Alcaldia (la fila, la ventanilla de Marisol). */
    public static final String[] ALCALDIA_DIA2 = oficial("interioralcaldia");
    /** El despacho de Quiroga: se pone cuando Quiroga sale a contar la verdad. */
    public static final String[] ALCALDIA_DESPACHO = oficial("alcaldia_despacho", I + "interioralcaldia.png", F + "interioralcaldia.png");
    public static final String[] MALECON_DIA2 = oficial("malecon");
    /** Salon de la Junta donde empieza la reunion. */
    public static final String[] JUNTA_SALON = oficial("junta", I + "junta_piso1.png", F + "junta_piso1.png");
    /** Primer piso de la Junta (si bajan la reunion por Don Alirio). */
    public static final String[] JUNTA_COCINA = oficial("junta_piso1", I + "junta.png", F + "junta.png");
    public static final String[] CASA_TARDE = oficial("casapuertasin", F + "casa_pasillo.png");
    public static final String[] CASA_CENA_DIA2 = oficial("casa_cena", I + "casapuertasin.png");

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

    /**
     * Sprite de un vecino. Prueba /imagenes/personajes/<id>.png (como siempre) y despues el nombre
     * del sprite oficial de definitiva_560x680 (Rosa.png, Santa_sofia.png...), en personajes/ o en sprites/.
     */
    public static String spriteVecino(String id) {
        String oficial = SPRITE_OFICIAL.getOrDefault(id, id.isEmpty() ? id
                : Character.toUpperCase(id.charAt(0)) + id.substring(1));
        String estatico = spriteEstatico(oficial, id);
        if (estatico != null) {
            return estatico;
        }
        String[] pruebas = {
            PERSONAJES + id + ".png",
            PERSONAJES + oficial + ".png",
            PERSONAJES + "definitiva/" + oficial + ".png",
            I + "definitiva/" + oficial + ".png",
            I + "sprites/" + oficial + ".png"
        };
        for (String p : pruebas) {
            if (Recursos.existe(p)) {
                return p;
            }
        }
        return pruebas[0];
    }

    /** Carpeta de los NPC estaticos: /imagenes/personaje/<Nombre>/<Nombre>.png (un solo PNG, no caminan). */
    public static final String PERSONAJE_ESTATICO = I + "personaje/";

    /**
     * Sprite estatico de un NPC en /imagenes/personaje/<Nombre>/<Nombre>.png, o null si no hay.
     * Se prueban varios nombres (el oficial, el id con mayuscula, sin guiones, en minuscula) para que
     * de igual "Alirio/Alirio.png" o "alirio/alirio.png". OJO: dentro de un .jar las mayusculas importan.
     */
    public static String spriteEstatico(String... nombres) {
        java.util.LinkedHashSet<String> n = new java.util.LinkedHashSet<>();
        for (String x : nombres) {
            if (x == null || x.isEmpty()) {
                continue;
            }
            n.add(x);
            n.add(Character.toUpperCase(x.charAt(0)) + x.substring(1));
            n.add(x.toLowerCase());
            StringBuilder camel = new StringBuilder();
            for (String parte : x.split("[_ ]+")) {
                if (!parte.isEmpty()) {
                    camel.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1).toLowerCase());
                }
            }
            n.add(camel.toString());
            // "santa_sofia" -> "Santa_Sofia" (cada palabra con mayuscula, sin quitar el guion bajo)
            StringBuilder titulo = new StringBuilder();
            for (String parte : x.split("_", -1)) {
                if (titulo.length() > 0) {
                    titulo.append('_');
                }
                if (!parte.isEmpty()) {
                    titulo.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1).toLowerCase());
                }
            }
            n.add(titulo.toString());
        }
        for (String carpeta : n) {
            for (String archivo : n) {
                String r = PERSONAJE_ESTATICO + carpeta + "/" + archivo + ".png";
                if (Recursos.existe(r)) {
                    return r;
                }
            }
        }
        // si solo hay una version con apellido ("Kevin_celular.png"), se usa esa
        for (String carpeta : n) {
            for (String archivo : n) {
                for (String extra : new String[]{"_celular", "_ig", "_objeto", "_cansado"}) {
                    String r = PERSONAJE_ESTATICO + carpeta + "/" + archivo + extra + ".png";
                    if (Recursos.existe(r)) {
                        return r;
                    }
                }
            }
        }
        return null;
    }

    /** id del vecino en el guion -> nombre del sprite oficial (solo los que no son "id con mayuscula"). */
    private static final java.util.Map<String, String> SPRITE_OFICIAL = new java.util.HashMap<>();

    static {
        SPRITE_OFICIAL.put("visitacion", "Rectora");
        SPRITE_OFICIAL.put("rectora", "Rectora");
        SPRITE_OFICIAL.put("sofia", "Santa_sofia");
        SPRITE_OFICIAL.put("santa_sofia", "Santa_sofia");
        SPRITE_OFICIAL.put("dona_rosa", "Rosa");
        SPRITE_OFICIAL.put("jose_arcadio", "JoseArcadio");
        SPRITE_OFICIAL.put("rosa", "Rosa");
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
