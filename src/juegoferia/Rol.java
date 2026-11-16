package juegoferia;

import java.awt.Color;

/**
 * Los cuatro roles. Todo lo que es "propio de cada rol" vive aqui:
 * frase de la tarjeta, habilidad, pieza que aporta al Letrero, fortaleza, carencia y sprites.
 *
 * Sprites (fondo transparente, de pie, pies en el borde de abajo):
 *   /imagenes/personajes/ciudadano/quieto.png
 *   /imagenes/personajes/ciudadano/caminar_01.png ... caminar_04.png   (opcionales)
 *   /imagenes/personajes/ciudadano/triste.png, feliz.png ...           (expresiones opcionales)
 */
public enum Rol {

    CIUDADANO("Ciudadano", "Vivo de preguntar y de escuchar.",
            "Persona de barrio, con una libreta en el bolsillo.",
            Habilidad.VER_VINCULOS, "material",
            "los vecinos confían", "le falta información comprobada y alcance",
            "Aquí va el material. Lo que me dieron por confiar.",
            "ciudadano", new Color(0x5BA843)),

    PERIODISTA("Periodista", "Si no lo puedo comprobar, no lo cuento.",
            "Persona con una grabadora y una libreta llena de tachones.",
            Habilidad.LUPA, "frase verdadera",
            "verifica y tiene fuentes", "le falta alcance y tiempo",
            "Aquí va la frase. Con fecha y con fuente.",
            "periodista", new Color(0x3F8EDC)),

    INFLUENCER("Influencer", "Si no lo ven, no existe.",
            "Persona con un celular en alto y una luz de aro en la mochila.",
            Habilidad.MEGAFONO, "pintura",
            "llega a todo el mundo", "le faltan fuentes y a veces se equivoca",
            "Aquí va la pintura. Que se lea desde lejos.",
            "influencer", new Color(0xD9304F)),

    CANDIDATO("Candidato", "Quien convoca, responde.",
            "Persona de saco, con un afiche enrollado bajo el brazo.",
            Habilidad.CONVOCATORIA, "firma",
            "puede prometer y reunir gente", "le falta confianza",
            "Aquí va mi firma. Y mi responsabilidad.",
            "candidato", new Color(0xE0A030));

    public final String nombre;
    public final String frase;
    public final String descripcion;
    public final Habilidad habilidad;
    public final String pieza;
    public final String fortaleza;
    public final String carencia;
    public final String lineaLetrero;
    public final String carpeta;
    public final Color color;

    Rol(String nombre, String frase, String descripcion, Habilidad habilidad, String pieza,
            String fortaleza, String carencia, String lineaLetrero, String carpeta, Color color) {
        this.nombre = nombre;
        this.frase = frase;
        this.descripcion = descripcion;
        this.habilidad = habilidad;
        this.pieza = pieza;
        this.fortaleza = fortaleza;
        this.carencia = carencia;
        this.lineaLetrero = lineaLetrero;
        this.carpeta = carpeta;
        this.color = color;
    }

    /** Carpeta de sprites del rol, ej: "/imagenes/personajes/ciudadano/". */
    public String sprites() {
        return "/imagenes/personajes/" + carpeta + "/";
    }

    /** Devuelve el rol por nombre ("CIUDADANO"...) o null si no existe ("-" o vacio = ninguno). */
    public static Rol de(String s) {
        try {
            return s == null ? null : valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Etiqueta que se usa en el guion: "CIUDADANO:" etc. */
    public String etiqueta() {
        return name();
    }
}
