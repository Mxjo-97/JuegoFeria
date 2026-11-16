package juegoferia;

/**
 * Minijuego fijo de la CENA del Dia 1: "Los pergaminos". Lo juega Periodista.
 * El pergamino de Melquiades esta cortado en cinco pedazos desordenados.
 */
public class MjPergaminos extends MjOrdenar {

    @Override
    public String nombre() {
        return "Los pergaminos";
    }

    @Override
    public String instrucciones() {
        return "El pergamino de Melquíades está cortado en cinco pedazos. Ponlos en orden "
                + "cambiándolos de puesto con dos clics. Cuando estés, pulsa Listo.";
    }

    @Override
    protected String[] tarjetasEnOrden() {
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
            case REGULAR: return "Falta una frase. Melquíades: Casi.";
            default: return "Melquíades lo termina de leer: «Siempre se me confunde el orden».";
        }
    }

    @Override
    protected Efecto efecto(int nivel) {
        return nivel == BIEN ? Efecto.de(Partida.ANIMO, 1) : new Efecto();
    }
}
