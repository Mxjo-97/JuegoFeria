package juegoferia;

import java.util.ArrayList;
import java.util.List;

/**
 * Peleas en la cena, SOLO si hay agravio. Maximo una por dia: la del agravio mas grave.
 * Si nadie dano a nadie, Dona Rosa cuenta la anecdota del pan con piedra (sinPelea).
 *
 * Cada pelea se activa con banderas que el guion pone durante el dia (en el Jugador que hizo el dano):
 *   difundio_sin_verificar, pauta_oculta, uso_nombre_vecinos, promesa_por_escrito,
 *   cito_cuenta_anonima, publico_fuente_reservada, guardo_pieza, grabo_casa_sin_permiso.
 *
 * En los dialogos: {AGRESOR} y {AFECTADO} se cambian por el rol que corresponda.
 */
public final class GestorPeleas {

    private GestorPeleas() {
    }

    public static final class Pelea {

        final String id, titulo;
        final Rol agresor;          // null = quien tenga la bandera
        final Rol afectado;         // null = el primero de los otros
        final int gravedad;
        final String[] banderas;
        final String[] lineas, mediarBien, cierres;

        Pelea(String id, String titulo, Rol agresor, Rol afectado, int gravedad, String[] banderas,
                String[] lineas, String[] mediarBien, String[] cierres) {
            this.id = id;
            this.titulo = titulo;
            this.agresor = agresor;
            this.afectado = afectado;
            this.gravedad = gravedad;
            this.banderas = banderas;
            this.lineas = lineas;
            this.mediarBien = mediarBien;
            this.cierres = cierres;
        }
    }

    /** gravedad: mas alto = mas grave. AJUSTAR si el equipo decide otro orden. */
    public static final Pelea[] PELEAS = {
        new Pelea("influencer_periodista", "Influencer contra Periodista", Rol.INFLUENCER, Rol.PERIODISTA, 3,
                new String[]{"difundio_sin_verificar", "pauta_oculta"},
                new String[]{
                    "PERIODISTA: Pasé la tarde comprobando que el audio del colegio no tenía nada firme. Y reenviaste el audio a todo el mundo.",
                    "INFLUENCER: Vi que todos lo reenviaban. ¿Qué querías, que me quedara en silencio?",
                    "PERIODISTA: Sí. En silencio, o preguntándome primero. Como lo haría alguien con sentido común.",
                    "INFLUENCER: Me dijiste que no me metiera en tus fuentes.",
                    "Narrador: Doña Rosa se asoma."},
                new String[]{
                    "INFLUENCER: Lo hice porque me daba miedo que me dejaran de ver.",
                    "PERIODISTA: Y yo porque me da miedo equivocarme."},
                new String[]{"Pedir perdón y reparar: «Mañana lo desmiento en mi perfil. Pásame lo que verificaste.»",
                    "Defenderse: «Fue un mensaje, no una sentencia.»", "Irse de la mesa."}),

        new Pelea("candidato_ciudadano", "Candidato contra Ciudadano", Rol.CANDIDATO, Rol.CIUDADANO, 3,
                new String[]{"uso_nombre_vecinos", "promesa_por_escrito"},
                new String[]{
                    "CIUDADANO: Hoy dijiste en público que «los vecinos que hablaron conmigo me respaldan». Esos vecinos hablaron conmigo, no contigo.",
                    "CANDIDATO: Les conté lo que escuché. Es parte de la campaña.",
                    "CIUDADANO: Les prometí discreción. Hablaste en mi nombre.",
                    "CANDIDATO: No di nombres.",
                    "CIUDADANO: No hacía falta. Todos sabían quién los había escuchado.",
                    "Narrador: Doña Rosa deja el cucharón."},
                new String[]{
                    "CANDIDATO: Quería que me creyeran. No supe cómo.",
                    "CIUDADANO: Yo quería que me creyeran a mí, y tampoco supe cómo."},
                new String[]{"Pedir perdón y reparar: «Mañana lo aclaro con los vecinos: lo que dije fue mío, no de ellos.»",
                    "Defenderse: «Hay cosas que se cuentan en campaña.»", "Irse de la mesa."}),

        new Pelea("periodista_ciudadano", "Periodista contra Ciudadano", Rol.PERIODISTA, Rol.CIUDADANO, 3,
                new String[]{"cito_cuenta_anonima", "publico_fuente_reservada"},
                new String[]{
                    "CIUDADANO: Un vecino me contó algo y me pidió reserva. Lo vi publicado en tu libreta con su nombre.",
                    "PERIODISTA: Es información de interés público.",
                    "CIUDADANO: Los vecinos me creyeron a mí, y ahora no confían en mí.",
                    "PERIODISTA: Tampoco puedo tapar todo lo que me cuentan.",
                    "Narrador: Doña Rosa sirve tinto, sin decir nada."},
                new String[]{
                    "PERIODISTA: Debí preguntarte antes. No lo hice porque pensé que me lo ibas a negar.",
                    "CIUDADANO: Y lo habría negado. Pero igual debías preguntar."},
                new String[]{"Pedir perdón y reparar: «Voy a quitar el nombre. Y le pido disculpas al vecino con usted presente.»",
                    "Defenderse: «Una fuente es una fuente.»", "Irse de la mesa."}),

        new Pelea("silla_vacia", "Tu silla estaba vacía", null, null, 2,
                new String[]{"guardo_pieza"},
                new String[]{
                    "{AFECTADO}: Tu pieza no llegó. El Letrero salió incompleto.",
                    "{AGRESOR}: Quería asegurarme de que no la usaran mal.",
                    "{AFECTADO}: Eso lo decides cuando la pones, no cuando la guardas.",
                    "{AGRESOR}: A mí también me da miedo equivocarme.",
                    "SAMUEL: ¿Por qué hay una silla sin plato?"},
                new String[]{},
                new String[]{"Pedir perdón y reparar: «Mañana pongo mi pieza primero.»",
                    "Defenderse: «La pieza era mía.»", "Irse de la mesa."}),

        new Pelea("influencer_casa", "Influencer contra los tres", Rol.INFLUENCER, Rol.PERIODISTA, 4,
                new String[]{"grabo_casa_sin_permiso"},
                new String[]{
                    "PERIODISTA: Reconozco mi cuarto en tu video.",
                    "CANDIDATO: Y mis afiches en el fondo.",
                    "CIUDADANO: Y mi cama sin hacer.",
                    "INFLUENCER: Son tres segundos. Nadie se va a fijar.",
                    "PERIODISTA: No tenías derecho a mostrar eso.",
                    "Narrador: Doña Rosa deja de lavar la taza."},
                new String[]{
                    "INFLUENCER: No pensé que importaba. Para mí enseñar es parte de contar.",
                    "PERIODISTA: Contar es una cosa. Mostrar sin permiso es otra."},
                new String[]{"Pedir perdón y reparar: «Lo bajo ahora. Y desde hoy pregunto antes de grabar en la casa.»",
                    "Defenderse: «Es mi cuenta.»", "Irse de la mesa."})
    };

    /** La pelea mas grave que se activo hoy, o null si nadie dano a nadie. */
    public static Pelea elegir(Partida p) {
        Pelea mejor = null;
        for (Pelea pe : PELEAS) {
            if (agresorDe(p, pe) != null && (mejor == null || pe.gravedad > mejor.gravedad)) {
                mejor = pe;
            }
        }
        return mejor;
    }

    private static Jugador agresorDe(Partida p, Pelea pe) {
        for (Jugador j : p.jugadores()) {
            if (pe.agresor != null && j.rol != pe.agresor) {
                continue;
            }
            for (String b : pe.banderas) {
                if (j.tiene(b) || p.bandera(b)) {
                    return j;
                }
            }
        }
        return null;
    }

    /** Juega la pelea completa y al final llama a fin. */
    public static void jugar(Lugar l, Pelea pe, Runnable fin) {
        Partida p = l.partida();
        Jugador agresor = agresorDe(p, pe);
        Jugador afectado = pe.afectado != null ? p.jugador(pe.afectado) : p.otros(agresor.rol).get(0);
        List<Jugador> mediadores = p.otros(agresor.rol, afectado.rol);
        if (pe.id.equals("influencer_casa")) {
            mediadores = p.otros(Rol.INFLUENCER, Rol.PERIODISTA);   // el tercero habla como afectado
        }
        String[] lineas = new String[pe.lineas.length];
        for (int i = 0; i < lineas.length; i++) {
            lineas[i] = pe.lineas[i].replace("{AGRESOR}", agresor.rol.name()).replace("{AFECTADO}", afectado.rol.name());
        }
        String[] ops = {
            "Mediar: «Esperen. Díganse qué necesitaban del otro.»",
            "Tomar partido por " + afectado.rol.nombre,
            "Tomar partido por " + agresor.rol.nombre,
            "Callar"
        };
        final List<Jugador> meds = mediadores;
        final boolean[] alguienMedio = {false};

        new Secuencia()
                .paso(sig -> l.dialogo(sig, lineas))
                .paso(sig -> DecisionGrupal.algunos(l, meds, "Pelea: " + pe.titulo + ". ¿Qué haces?", ops,
                        (j, op) -> {
                            switch (op) {
                                case 0:
                                    alguienMedio[0] = true;
                                    l.mariposa(j);
                                    break;
                                case 1:
                                    p.confianza.sumar(j.rol, afectado.rol, 1);
                                    p.confianza.sumar(j.rol, agresor.rol, -1);
                                    break;
                                case 2:
                                    p.confianza.sumar(j.rol, agresor.rol, 1);
                                    p.confianza.sumar(j.rol, afectado.rol, -1);
                                    break;
                                default:
                                    p.poner("pelea_abierta_" + pe.id);   // queda abierta hasta el Dia 2
                            }
                        }, r -> sig.run()))
                .paso(sig -> {
                    if (alguienMedio[0] && pe.mediarBien.length > 0) {
                        l.dialogo(sig, pe.mediarBien);
                    } else {
                        sig.run();
                    }
                })
                .paso(sig -> l.decidirTurno(agresor, "Cierre: tú decides cómo termina.",
                        Lugar.op(pe.cierres[0], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, 2), "Pelea: reparar");
                            p.confianza.bloquear(agresor.rol, afectado.rol, false);
                            agresor.objetos.add("Objeto pequeño de " + afectado.nombre);
                            sig.run();
                        }),
                        Lugar.op(pe.cierres[1], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Pelea: defenderse");
                            sig.run();
                        }),
                        Lugar.op(pe.cierres[2], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, -2), "Pelea: irse de la mesa");
                            p.confianza.bloquear(agresor.rol, afectado.rol, true);
                            sig.run();
                        })))
                .hacer(() -> {
                    if (fin != null) {
                        fin.run();
                    }
                })
                .iniciar();
    }

    /** Si nadie dano a nadie. */
    public static void sinPelea(Lugar l, Runnable fin) {
        l.efectoCompartido(Efecto.de(Partida.ANIMO, 2), "Sin pelea");
        l.dialogo(fin,
                "DOÑA ROSA: Les cuento algo. Hoy, en la panadería, un niño intentó comprar pan con una piedra de río.",
                "DOÑA ROSA: Le dije que era muy bonita, pero que el pan se paga con plata.",
                "Narrador: Los cuatro se ríen. Samuel se ríe más fuerte.");
    }

    /** Para la consola / la demo: que peleas estarian activas. */
    public static List<String> activas(Partida p) {
        List<String> r = new ArrayList<>();
        for (Pelea pe : PELEAS) {
            if (agresorDe(p, pe) != null) {
                r.add(pe.titulo);
            }
        }
        return r;
    }
}
