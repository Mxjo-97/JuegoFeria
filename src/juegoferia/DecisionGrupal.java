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
 *   votacion(...) -> VOTACION DE LA CASA: los cuatro votan en su celular (30 s cada uno),
 *                    gana la mayoria; empate = decide Dona Rosa (la opcion prudente).
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

    // ===================== VOTACION DE LA CASA =====================

    /** Segundos que tiene cada jugador para votar (el reloj del dia se detiene mientras tanto). */
    public static final double SEGUNDOS_VOTO = 30;

    /**
     * Votacion de la casa (guion: "Votacion de la casa").
     *   - Cada jugador vota en su celular durante 30 segundos. El chat anuncia: "Rol votó A."
     *   - Gana la opcion con mas votos. Si hay empate, decide Dona Rosa: la opcion prudente.
     *   - Si los cuatro votan lo mismo, la confianza entre roles sube 1.
     *   - Quien quedo en minoria elige una linea: "No estoy de acuerdo, pero cuenta conmigo."
     *     (confianza +1) o "Voy a mi ritmo." (no puede pedir ayuda en la escena que sigue).
     *
     * @param pregunta  la pregunta ("¿Cómo entramos a la Alcaldía?")
     * @param opciones  los textos A, B, C...
     * @param prudente  indice de la opcion marcada como prudente (la que elige Dona Rosa en empate)
     * @param alGanar   recibe el indice ganador; ahi el guion pone los dialogos y los efectos
     */
    public static void votacion(Lugar l, String pregunta, String[] opciones, int prudente, Consumer<Integer> alGanar) {
        Partida p = l.partida();
        p.chat.escribir(p.reloj.texto(), ChatCasa.NOMBRE, "Votación: " + pregunta);
        turnoVoto(l, p.jugadores(), 0, new EnumMap<>(Rol.class), pregunta, opciones, prudente, alGanar);
    }

    private static void turnoVoto(Lugar l, List<Jugador> js, int i, Map<Rol, Integer> votos, String pregunta,
            String[] opciones, int prudente, Consumer<Integer> alGanar) {
        if (i >= js.size()) {
            contarVotos(l, votos, opciones, prudente, alGanar);
            return;
        }
        Jugador j = js.get(i);
        Partida p = l.partida();
        Runnable siguiente = () -> l.esperarYLuego(() -> turnoVoto(l, js, i + 1, votos, pregunta, opciones, prudente, alGanar));
        Lugar.Opcion[] ops = new Lugar.Opcion[opciones.length];
        for (int k = 0; k < opciones.length; k++) {
            final int op = k;
            ops[k] = Lugar.op(opciones[k], () -> {
                votos.put(j.rol, op);
                p.chat.escribir(p.reloj.texto(), ChatCasa.NOMBRE, j.rol.nombre + " votó " + (char) ('A' + op) + ".");
                siguiente.run();
            });
        }
        l.decidirCelular("VOTA " + j.nombre.toUpperCase(), ChatCasa.NOMBRE, pregunta,
                "Votación de la casa. Gana la mayoría; si hay empate, decide Doña Rosa.", ops);
        l.colorTurno(j.rol.color);
        l.soloPara(j.rol);
        l.limiteDecision(SEGUNDOS_VOTO, () -> {
            p.chat.escribir(p.reloj.texto(), ChatCasa.NOMBRE, j.rol.nombre + " no alcanzó a votar.");
            siguiente.run();
        }, "Si no votas en 30 segundos, tu voto no cuenta...");
    }

    private static void contarVotos(Lugar l, Map<Rol, Integer> votos, String[] opciones, int prudente,
            Consumer<Integer> alGanar) {
        Partida p = l.partida();
        int[] cuenta = new int[opciones.length];
        for (int v : votos.values()) {
            cuenta[v]++;
        }
        int max = 0;
        for (int c : cuenta) {
            max = Math.max(max, c);
        }
        List<Integer> empatadas = new ArrayList<>();
        for (int k = 0; k < cuenta.length; k++) {
            if (cuenta[k] == max) {
                empatadas.add(k);
            }
        }
        boolean empate = empatadas.size() > 1;
        final int ganador = !empate ? empatadas.get(0) : (empatadas.contains(prudente) || max == 0 ? prudente : empatadas.get(0));
        String letra = String.valueOf((char) ('A' + ganador));
        p.chat.escribir(p.reloj.texto(), ChatCasa.NOMBRE, "Ganó la opción " + letra + ".");

        List<String> d = new ArrayList<>();
        if (empate) {
            d.add("Narrador: Empate. Decide Doña Rosa.");
            d.add("DOÑA ROSA: Cuando no se ponen de acuerdo, se hace lo prudente.");
        }
        d.add("Narrador: Gana la opción " + letra + ": " + opciones[ganador]);
        boolean unanime = votos.size() == p.jugadores().size() && max == votos.size() && votos.size() > 1;
        if (unanime) {
            l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, 1), "Votación unánime");
            d.add("Narrador: Los cuatro votaron lo mismo. La confianza entre ustedes sube.");
        }
        List<Jugador> minoria = new ArrayList<>();
        for (Jugador j : p.jugadores()) {
            Integer v = votos.get(j.rol);
            if (v != null && v != ganador) {
                minoria.add(j);
            }
        }
        l.dialogo(() -> lineaMinoria(l, minoria, 0, () -> alGanar.accept(ganador)), d.toArray(new String[0]));
    }

    private static void lineaMinoria(Lugar l, List<Jugador> minoria, int i, Runnable fin) {
        if (i >= minoria.size()) {
            l.esperarYLuego(fin);
            return;
        }
        Jugador j = minoria.get(i);
        l.decidirTurno(j, "Quedaste en minoría. ¿Qué dices?",
                Lugar.op("«No estoy de acuerdo, pero cuenta conmigo.»", () -> {
                    l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, 1), "Minoría: cuenta conmigo");
                    l.burbuja(l.actorDe(j.rol), "No estoy de acuerdo, pero cuenta conmigo.", 3.5);
                    l.esperarYLuego(() -> lineaMinoria(l, minoria, i + 1, fin));
                }),
                Lugar.op("«Voy a mi ritmo.»", () -> {
                    j.poner("a_su_ritmo_pendiente");
                    l.burbuja(l.actorDe(j.rol), "Voy a mi ritmo.", 3.5);
                    l.toast(j.nombre + " no podrá pedir ayuda en la escena que sigue.", new java.awt.Color(0xFFE08A));
                    l.esperarYLuego(() -> lineaMinoria(l, minoria, i + 1, fin));
                }));
    }
}
