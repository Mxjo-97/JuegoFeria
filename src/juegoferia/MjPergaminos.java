package juegoferia;

/**
 * Minijuego fijo de la CENA: "Los pergaminos". El pergamino de Melquiades esta cortado en cinco
 * pedazos desordenados. Dia 1: lo juega Periodista. Dia 2: otro texto, lo juega Ciudadano
 * (guion: "el quinto lo juega quien menos haya jugado, y si hay empate, Ciudadano").
 * Arte: /imagenes/minijuegos/pergaminos/ (mismas piezas los dos dias).
 */
public class MjPergaminos extends MjOrdenar {

    private final int dia;

    /** El del Dia 1. */
    public MjPergaminos() {
        this(1);
    }

    public MjPergaminos(int dia) {
        this.dia = dia;
    }

    /** El pergamino del Dia 2, en orden (guion "Pergamino del Dia 2"). */
    public static final String[] TEXTO_DIA2 = {
        "Dirá la isla que lo vio todo",
        "y no habrá mirado a nadie.",
        "Guardará cada número en una lista sin dueño,",
        "y aun así olvidará el rostro de quien le abrió la puerta.",
        "Quien hoy pida permiso antes de mirar, mañana no tendrá que pedir que lo dejen en paz."
    };

    @Override
    protected String carpetaArte() {
        return "pergaminos";
    }

    @Override
    protected String[] fondoLugar() {
        return dia == 2 ? Rutas.CASA_CENA_DIA2 : Rutas.CASA_CENA;
    }

    @Override
    protected void iconoAviso(java.awt.Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "pergaminos", "pergamino_rollo_sello", cx, cy, 190)) {
            ArteMj.hojaPapel(g, cx - 70, cy - 90, 140, 180, ArteMj.CREMA);
        }
    }

    /** Cada tarjeta es un pedazo de pergamino rasgado con su numero de fragmento. */
    @Override
    protected void tarjeta(java.awt.Graphics2D g, int puesto, java.awt.Rectangle r, String texto, boolean elegida, boolean fija) {
        int off = elegida ? -10 : 0;
        java.awt.Rectangle rr = new java.awt.Rectangle(r.x + 60, r.y + off, r.width - 60, r.height);
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        int pasos = 26;
        p.moveTo(rr.x, rr.y + 6);
        for (int i = 1; i <= pasos; i++) {
            p.lineTo(rr.x + rr.width * i / (double) pasos, rr.y + 6 + ArteMj.ruido(puesto * 31 + i) * 6);
        }
        for (int i = 1; i <= 4; i++) {
            p.lineTo(rr.x + rr.width + ArteMj.ruido(puesto * 7 + i) * 7, rr.y + rr.height * i / 4.0);
        }
        for (int i = pasos - 1; i >= 0; i--) {
            p.lineTo(rr.x + rr.width * i / (double) pasos, rr.y + rr.height - 6 + ArteMj.ruido(puesto * 53 + i) * 6);
        }
        for (int i = 3; i >= 1; i--) {
            p.lineTo(rr.x + ArteMj.ruido(puesto * 11 + i) * 7, rr.y + rr.height * i / 4.0);
        }
        p.closePath();
        ArteMj.sticker(g, p, elegida ? ArteMj.AMARILLO_CLARO : fija ? ArteMj.VERDE_CLARO : new java.awt.Color(0xF9E3B4), 4.5f);
        if (!ArteMj.pegar(g, "pergaminos", "pergamino_fragmento_numero_" + puesto, r.x + 52, r.y + off + r.height / 2.0, r.height * 0.95)) {
            ArteMj.numero(g, puesto, r.x + 40, r.y + off + r.height / 2.0, r.height * 0.8, elegida);
        }
        g.setFont(Estilo.fuente(java.awt.Font.ITALIC, 30f));
        ArteMj.textoEnCaja(g, texto, new java.awt.Rectangle(rr.x + 90, rr.y + 4, rr.width - 130, rr.height - 8), ArteMj.TEXTO);
    }

    @Override
    protected void adornos(java.awt.Graphics2D g) {
        ArteMj.pegar(g, "pergaminos", "pergamino_pluma_tintero", 1840, 200, 120);
        ArteMj.pegar(g, "pergaminos", "pergamino_sello_mariposa", 1840, 860, 110);
    }

    @Override
    public String nombre() {
        return "Los pergaminos";
    }

    @Override
    public String instrucciones() {
        return "Un pergamino se rompió en 5 pedazos. Ordénalos para leer el mensaje completo: "
                + "toca un pedazo y luego otro para cambiarlos de puesto. Cuando estés, pulsa Listo.";
    }

    @Override
    protected String[] tarjetasEnOrden() {
        if (dia == 2) {
            return TEXTO_DIA2;
        }
        return new String[]{
            "Dirá la isla que lo oyó todo",
            "y no habrá escuchado nada.",
            "Le pondrá nombre a cada cosa para no perderla,",
            "y aun así olvidará el nombre de quien le dio de comer.",
            "Quien hoy pregunte primero, mañana no tendrá que pedir perdón."
        };
    }

    @Override
    protected int paraBien() {
        return 5;
    }

    @Override
    protected int paraRegular() {
        return 3;
    }

    @Override
    protected String[] consejos() {
        if (dia == 2) {
            return new String[]{"Empieza por «Dirá».", "El último dice «permiso».", "Mira las comas."};
        }
        return new String[]{"El primero empieza con «Dirá».", "El último habla de quien pregunta.", "Mira las comas."};
    }

    @Override
    protected String rutaFondo() {
        return "/imagenes/minijuegos/pergaminos.png";
    }

    @Override
    protected String resumen(int nivel) {
        switch (nivel) {
            case BIEN: return "Melquíades: Eso es lo que yo escribí. Y lo que ustedes acaban de leer.";
            case REGULAR: return dia == 2 ? "Melquíades: Falta una frase." : "Falta una frase. Melquíades: Casi.";
            default: return dia == 2 ? "Melquíades termina de leerlo él. El pergamino queda incompleto."
                    : "Melquíades lo termina de leer: «Siempre se me confunde el orden».";
        }
    }

    @Override
    protected Efecto efecto(int nivel) {
        return nivel == BIEN ? Efecto.de(Partida.ANIMO, 1) : new Efecto();
    }
}
