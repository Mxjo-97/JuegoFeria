package juegoferia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Decisiones con TURNOS para cuatro jugadores (en un mismo computador o cada uno en el suyo).
 * Usa las ventanas de decision de Lugar y las encadena: cuando uno elige, le toca al siguiente.
 *
 *   digital(...)  -> eligen los cuatro, uno por uno (decision digital A-E).
 *   respeto(...)  -> responde quien quiera primero; si elige la opcion que respeta (A),
 *                    los otros pueden sumarse con "Yo también".
 *   cadaUno(...)  -> cada jugador elige entre las mismas opciones (decision sin celular).
 */
public final class DecisionGrupal {

    private DecisionGrupal() {
    }

    // ===================== DECISION DIGITAL =====================

    public static void digital(Lugar l, String autor, String publicacion, String contexto, String[] opciones,
            BiConsumer<Jugador, Integer> alElegir, Consumer<Map<Rol, Integer>> alTerminar) {
        turnoDigital(l, l.partida().jugadores(), 0, new EnumMap<>(Rol.class),
                autor, publicacion, contexto, opciones, alElegir, alTerminar);
    }

    private static void turnoDigital(Lugar l, List<Jugador> js, int i, Map<Rol, Integer> elecciones,
            String autor, String publicacion, String contexto, String[] opciones,
            BiConsumer<Jugador, Integer> alElegir, Consumer<Map<Rol, Integer>> alTerminar) {
        if (i >= js.size()) {
            if (alTerminar != null) {
                alTerminar.accept(elecciones);
            }
            return;
        }
        Jugador j = js.get(i);
        Lugar.Opcion[] ops = new Lugar.Opcion[opciones.length];
        for (int k = 0; k < opciones.length; k++) {
            final int op = k;
            ops[k] = Lugar.op(opciones[k], () -> {
                elecciones.put(j.rol, op);
                if (alElegir != null) {
                    alElegir.accept(j, op);
                }
                l.esperarYLuego(() -> turnoDigital(l, js, i + 1, elecciones, autor, publicacion, contexto,
                        opciones, alElegir, alTerminar));
            });
        }
        l.decidirCelular("TURNO DE " + j.nombre.toUpperCase(), autor, publicacion, contexto, ops);
        l.colorTurno(j.rol.color);
        l.soloPara(j.rol);
    }

    // ===================== DECISION DE RESPETO =====================

    /**
     * @param situacion   lo que acaba de pasar
     * @param opciones    textos A, B, C (la A es la que respeta)
     * @param alResponder quien respondio y que eligio (aqui el guion pone dialogos y efectos)
     * @param alSumarse   los que dijeron "Yo también" (solo si la respuesta fue A); puede ser lista vacia
     */
    public static void respeto(Lugar l, String situacion, String[] opciones,
            BiConsumer<Jugador, Integer> alResponder, Consumer<List<Jugador>> alSumarse) {
        List<Jugador> js = l.partida().jugadores();
        Lugar.Opcion[] quien = new Lugar.Opcion[js.size()];
        for (int i = 0; i < js.size(); i++) {
            Jugador j = js.get(i);
            quien[i] = Lugar.op(j.titulo() + " responde", () -> {
                Lugar.Opcion[] ops = new Lugar.Opcion[opciones.length];
                for (int k = 0; k < opciones.length; k++) {
                    final int op = k;
                    ops[k] = Lugar.op(opciones[k], () -> {
                        if (alResponder != null) {
                            alResponder.accept(j, op);
                        }
                        if (op == 0) {
                            l.esperarYLuego(() -> yoTambien(l, l.partida().otros(j.rol), 0, new ArrayList<>(), alSumarse));
                        }
                    });
                }
                l.decidirTurno(j, situacion, ops);
            }).soloPara(j.rol);
        }
        l.decidir("DECISIÓN DE RESPETO", situacion + " ¿Quién responde primero?", 0, null, quien);
    }

    private static void yoTambien(Lugar l, List<Jugador> otros, int i, List<Jugador> sumados,
            Consumer<List<Jugador>> alSumarse) {
        if (i >= otros.size()) {
            if (alSumarse != null) {
                alSumarse.accept(sumados);
            }
            return;
        }
        Jugador j = otros.get(i);
        l.decidirTurno(j, "¿Te sumas a lo que se dijo?",
                Lugar.op("Yo también", () -> {
                    sumados.add(j);
                    yoTambien(l, otros, i + 1, sumados, alSumarse);
                }),
                Lugar.op("No", () -> yoTambien(l, otros, i + 1, sumados, alSumarse)));
    }

    // ===================== CADA UNO ELIGE =====================

    public static void cadaUno(Lugar l, String situacion, String[] opciones,
            BiConsumer<Jugador, Integer> alElegir, Consumer<Map<Rol, Integer>> alTerminar) {
        turno(l, l.partida().jugadores(), 0, new EnumMap<>(Rol.class), situacion, opciones, alElegir, alTerminar);
    }

    /** Igual que cadaUno pero solo para algunos jugadores (ej: los que median una pelea). */
    public static void algunos(Lugar l, List<Jugador> quienes, String situacion, String[] opciones,
            BiConsumer<Jugador, Integer> alElegir, Consumer<Map<Rol, Integer>> alTerminar) {
        turno(l, quienes, 0, new EnumMap<>(Rol.class), situacion, opciones, alElegir, alTerminar);
    }

    private static void turno(Lugar l, List<Jugador> js, int i, Map<Rol, Integer> elecciones, String situacion,
            String[] opciones, BiConsumer<Jugador, Integer> alElegir, Consumer<Map<Rol, Integer>> alTerminar) {
        if (i >= js.size()) {
            if (alTerminar != null) {
                alTerminar.accept(elecciones);
            }
            return;
        }
        Jugador j = js.get(i);
        Lugar.Opcion[] ops = new Lugar.Opcion[opciones.length];
        for (int k = 0; k < opciones.length; k++) {
            final int op = k;
            ops[k] = Lugar.op(opciones[k], () -> {
                elecciones.put(j.rol, op);
                if (alElegir != null) {
                    alElegir.accept(j, op);
                }
                l.esperarYLuego(() -> turno(l, js, i + 1, elecciones, situacion, opciones, alElegir, alTerminar));
            });
        }
        l.decidirTurno(j, situacion, ops);
    }
}
