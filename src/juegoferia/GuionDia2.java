package juegoferia;

import java.awt.Color;

/**
 * El guion del DIA 2. ESQUELETO: el guion del Dia 2 todavia no esta escrito.
 * Aqui ya queda lo que el Dia 1 deja como consecuencia, para que quien escriba el Dia 2 lo use:
 *
 *   - p.bandera("rumor_alcaldia_adelantado"): alguien conto el rumor en el apagon;
 *     la Alcaldia ya empieza con +10 de desinformacion (se sumo en GuionDia1).
 *   - p.bandera("pelea_abierta_<id>"): alguien callo en la pelea; queda abierta hasta hoy.
 *   - p.bandera("niebla_vuelve_<lugar>"): el Letrero tenia 3 piezas; la niebla vuelve hoy.
 *   - p.hayNiebla(lugar): lugares donde la niebla entro anoche.
 *   - p.confianza.estaBloqueada(a, b): parejas que hoy no se pueden prestar habilidades.
 *
 * Para escribir cada lugar, copiar la estructura de GuionDia1.panaderia().
 */
public final class GuionDia2 {

    private GuionDia2() {
    }

    public static final String CASA = "Casa_dia2";
    public static final String ALCALDIA = GrafoLocaciones.ALCALDIA;
    public static final String PRIMER_LUGAR = CASA;

    public static Lugar crear(String lugar, Partida p, Lugar.Anfitrion h) {
        switch (lugar) {
            case CASA: return casa(p, h);
            case ALCALDIA: return alcaldia(p, h);
            default:
                // los lugares del Dia 1 que se repitan hoy se agregan aqui
                throw new IllegalArgumentException("El Día 2 todavía no tiene el lugar " + lugar);
        }
    }

    private static Lugar casa(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CASA, "La casa · Día 2", Reloj.h(7, 0));
        l.fondo(Rutas.CASA);
        l.caminable(80, 560, 1760, 480);
        l.equipoEn(900, 900);
        l.salida(1760, 600, 160, 420, ALCALDIA, "Ir a la Alcaldía");
        l.alPrimeraEntrada(() -> {
            String niebla = p.bandera("niebla_vuelve_" + GrafoLocaciones.PANADERIA)
                    ? "Narrador: Abajo, la niebla volvió a la panadería. Hace falta la pieza que faltó anoche."
                    : "Narrador: Abajo, la panadería huele a pan. Sin niebla.";
            l.dialogo(null,
                    "Narrador: Día 2 de 2. Mañana es la elección.",
                    niebla,
                    "DOÑA ROSA: (TODO Día 2: escribir la mañana)");
        });
        return l;
    }

    private static Lugar alcaldia(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, ALCALDIA, "Alcaldía", Reloj.h(9, 0));
        l.fondo(Rutas.ALCALDIA);
        l.caminable(60, 600, 1800, 440);
        l.equipoEn(300, 940);
        l.rumor("Dicen que botan a quien no vote por el alcalde.", 0);
        l.letrero("TODO: texto del Letrero de la Alcaldía");
        l.objetivo("todo", "TODO: escribir la Alcaldía");
        l.npc("mauricio", "Mauricio", "el de la bicicleta", 1100, 760, new Color(0x5BA843))
                .interactuar("Hablar con Mauricio", () -> l.dialogo(() -> {
                    l.completar("todo");
                    h.terminarDia();
                }, "MAURICIO: Son cuarenta y seis contratos que vencen en tres semanas. No sé más.",
                        "Narrador: (TODO Día 2: aquí sigue el guion. Por ahora, esto termina la partida.)"));
        l.alPrimeraEntrada(() -> {
            if (p.bandera("rumor_alcaldia_adelantado")) {
                l.dialogo(null, "Narrador: El rumor llegó antes que ustedes: la Alcaldía amaneció con más desinformación.");
            }
        });
        return l;
    }

    /** Cinematica final. TODO: escribirla con el resultado de la eleccion y los medidores. */
    public static Cinematica finalDelJuego(Partida p) {
        String animo = p.nivelAnimo();
        return new Cinematica(p)
                .cuadro("/imagenes/cinematicas/final.png", "La casa de Doña Rosa, de madrugada. Cuatro tazas vacías en la mesa.",
                        "RINGO: Y así fueron los dos días en que todos hablaron en Macondo.",
                        animo.equals("alto") ? "RINGO: La casa terminó llena de mariposas."
                                : animo.equals("medio") ? "RINGO: La casa terminó en paz, más o menos."
                                : "RINGO: La casa terminó en silencio. Y no del bueno.")
                .titulo("FIN", "Mariposas entre los cuatro: " + p.totalMariposas());
    }
}
