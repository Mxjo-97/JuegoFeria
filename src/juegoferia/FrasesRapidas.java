package juegoferia;

import java.util.List;

/**
 * Pedir ayuda: frases rapidas (tecla F). Quien camina escoge una frase y a quien va;
 * la burbuja le aparece al otro en el mapa y ese responde con una de cuatro respuestas.
 */
public final class FrasesRapidas {

    private FrasesRapidas() {
    }

    public static final String[] FRASES = {
        "Verifícame esto.",
        "Ayúdame a repartirlo.",
        "Acompáñame.",
        "Necesito una fuente.",
        "Dímelo más claro.",
        "Dame un minuto.",
        "Esto me da mala espina.",
        "Yo me encargo.",
        "Cuenta conmigo.",
        "Mejor esperemos."
    };

    public static final String[] RESPUESTAS = {"Voy.", "Ahora no puedo.", "Cuenta conmigo.", "Mira esto antes."};

    public static void abrir(Lugar l) {
        Partida p = l.partida();
        Jugador de = p.lider();
        if (de == null) {
            return;
        }
        Lugar.Opcion[] ops = new Lugar.Opcion[FRASES.length];
        for (int i = 0; i < FRASES.length; i++) {
            String f = FRASES[i];
            ops[i] = Lugar.op(f, () -> elegirDestino(l, de, f));
        }
        l.decidirTurno(de, "Frase rápida (la tecla 0 es la décima)", ops);
    }

    private static void elegirDestino(Lugar l, Jugador de, String frase) {
        List<Jugador> otros = l.partida().otros(de.rol);
        Lugar.Opcion[] ops = new Lugar.Opcion[otros.size()];
        for (int i = 0; i < otros.size(); i++) {
            Jugador a = otros.get(i);
            ops[i] = Lugar.op("Para " + a.titulo(), () -> enviar(l, de, a, frase));
        }
        l.decidirTurno(de, "«" + frase + "» ¿Para quién?", ops);
    }

    private static void enviar(Lugar l, Jugador de, Jugador a, String frase) {
        l.burbuja(l.actorDe(de.rol), frase, 3.5);
        Lugar.Opcion[] ops = new Lugar.Opcion[RESPUESTAS.length];
        for (int i = 0; i < RESPUESTAS.length; i++) {
            String r = RESPUESTAS[i];
            ops[i] = Lugar.op(r, () -> l.burbuja(l.actorDe(a.rol), r, 3.5));
        }
        l.decidirTurno(a, de.nombre + " te dice: «" + frase + "»", ops);
    }
}
