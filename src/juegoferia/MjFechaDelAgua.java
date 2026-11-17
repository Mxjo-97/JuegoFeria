package juegoferia;

import java.awt.Graphics2D;

/**
 * Minijuego fijo del MALECON: "La fecha del agua". Lo juega Influencer.
 * Chela entrega siete tarjetas y hay que ordenarlas por fecha.
 *
 * Pendiente (guion): "conectar cada una con lo que afirma el papel". Por ahora solo se ordena.
 */
public class MjFechaDelAgua extends MjOrdenar {

    private final boolean pista;

    /** pista = alguien verifico la publicacion: arranca con la primera tarjeta puesta. */
    public MjFechaDelAgua(boolean pista) {
        this.pista = pista;
    }

    @Override
    public String nombre() {
        return "La fecha del agua";
    }

    @Override
    protected String carpetaArte() {
        return "fecha_agua";
    }

    @Override
    protected String[] fondoLugar() {
        return Rutas.MALECON;
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "fecha_agua", "agua_calendario_mensual", cx, cy - 20, 170)) {
            ArteMj.numero(g, 1, cx, cy, 120, true);
        }
        ArteMj.pegar(g, "fecha_agua", "agua_gota_grande", cx + 80, cy + 80, 100);
    }

    @Override
    protected void adornos(Graphics2D g) {
        ArteMj.pegar(g, "fecha_agua", "agua_tarjeta_noticia", 1830, 200, 120);
        ArteMj.pegar(g, "fecha_agua", "agua_gota_con_ondas", 1830, 330, 90);
        g.setFont(ArteMj.letra(22f));
        ArteMj.centrado(g, "más vieja", 95, 190, ArteMj.CREMA);
        ArteMj.flecha(g, 95, 210, 95, 880, ArteMj.AZUL, 16, 0);
        ArteMj.centrado(g, "hoy", 95, 920, ArteMj.CREMA);
    }

    @Override
    public String instrucciones() {
        return "Chela te da siete tarjetas. Ordénalas de la más vieja a la de hoy. "
                + "Haz clic en una y luego en otra para cambiarlas de puesto. Cuando estés, pulsa Listo.";
    }

    @Override
    protected String[] tarjetasEnOrden() {
        return new String[]{
            "Permiso de venta de Chela. Hace ocho años.",
            "Queja por olor en el río. Hace catorce meses.",
            "Primer análisis del agua: bacterias altas. Hace trece meses.",
            "Cierre de la tubería averiada. Hace doce meses.",
            "Titular «Río contaminado». Hace doce meses.",
            "Segundo análisis del agua: normal. Hace dos semanas.",
            "El papel pegado hoy en la pared."
        };
    }

    @Override
    protected int fijasAlInicio() {
        return pista ? 1 : 0;
    }

    @Override
    protected int paraBien() {
        return 6;
    }

    @Override
    protected int paraRegular() {
        return 4;
    }

    @Override
    protected String[] consejos() {
        return new String[]{"Empieza por la más vieja.", "Esa es de hoy.", "Mira el año."};
    }

    @Override
    protected String rutaFondo() {
        return "/imagenes/minijuegos/fecha_del_agua.png";
    }

    @Override
    protected String resumen(int nivel) {
        switch (nivel) {
            case BIEN: return "El titular ya se cerró hace un año. El papel de hoy lo usa como si fuera de ahora.";
            case REGULAR: return "Casi. Revise el orden de las dos del medio.";
            default: return "Chela te lo explica: todo eso ya se cerró hace un año.";
        }
    }

    @Override
    protected Efecto efecto(int nivel) {
        return nivel == BIEN ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, Partida.DESINFO_LUGAR, -3)
                : nivel == REGULAR ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1) : new Efecto();
    }
}
