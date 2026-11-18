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
        /** Dia 2, agravio repetido (guion "Pelea ... en el Dia 2"). null = se usan las del Dia 1. */
        String[] lineasDia2, cierresDia2;

        Pelea dia2(String[] lineas, String[] cierres) {
            this.lineasDia2 = lineas;
            this.cierresDia2 = cierres;
            return this;
        }

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
                    "Ringo: Doña Rosa se asoma."},
                new String[]{
                    "INFLUENCER: Lo hice porque me daba miedo que me dejaran de ver.",
                    "PERIODISTA: Y yo porque me da miedo equivocarme."},
                new String[]{"Pedir perdón y reparar: «Mañana lo desmiento en mi perfil. Pásame lo que verificaste.»",
                    "Defenderse: «Fue un mensaje, no una sentencia.»", "Irse de la mesa."})
                .dia2(new String[]{
                    "PERIODISTA: Ayer te pedí que preguntaras antes. Hoy lo hiciste de nuevo.",
                    "INFLUENCER: Fue un reenvío. Solo un reenvío.",
                    "PERIODISTA: Un reenvío que llegó a la casa de quien nos recibió.",
                    "DOÑA ROSA: (desde la cocina) A mí no me gusta que me mencionen por nada.",
                    "INFLUENCER: …Perdón. No pensé que llegaba hasta aquí."}, null),

        new Pelea("candidato_ciudadano", "Candidato contra Ciudadano", Rol.CANDIDATO, Rol.CIUDADANO, 3,
                new String[]{"uso_nombre_vecinos", "promesa_por_escrito"},
                new String[]{
                    "CIUDADANO: Hoy dijiste en público que «los vecinos que hablaron conmigo me respaldan». Esos vecinos hablaron conmigo, no contigo.",
                    "CANDIDATO: Les conté lo que escuché. Es parte de la campaña.",
                    "CIUDADANO: Les prometí discreción. Hablaste en mi nombre.",
                    "CANDIDATO: No di nombres.",
                    "CIUDADANO: No hacía falta. Todos sabían quién los había escuchado.",
                    "Ringo: Doña Rosa deja el cucharón."},
                new String[]{
                    "CANDIDATO: Quería que me creyeran. No supe cómo.",
                    "CIUDADANO: Yo quería que me creyeran a mí, y tampoco supe cómo."},
                new String[]{"Pedir perdón y reparar: «Mañana lo aclaro con los vecinos: lo que dije fue mío, no de ellos.»",
                    "Defenderse: «Hay cosas que se cuentan en campaña.»", "Irse de la mesa."})
                .dia2(new String[]{
                    "CIUDADANO: Ayer te pedí que no hablaras en mi nombre. Hoy lo hiciste en la Alcaldía.",
                    "CANDIDATO: Dije «hay vecinos que me respaldan». No dije quiénes.",
                    "CIUDADANO: Hay vecinos que me contaron cosas a mí. Y esas cosas aparecieron en tu discurso.",
                    "CANDIDATO: Eso me ayudó a que me escucharan.",
                    "CIUDADANO: Te escucharon porque yo estaba al lado. Eso no es tuyo.",
                    "Ringo: Doña Rosa deja el cucharón sobre la mesa.",
                    "DOÑA ROSA: Dos días de pan y todavía no aprenden a pedirse permiso."},
                    new String[]{"Reparar: «Mañana aclaro con los vecinos que lo que dije fue mío. Y te pido permiso antes de nombrar a quien me cuente algo.»",
                        "Defenderse: «Es una campaña. Hay cosas que se cuentan.»",
                        "Irse de la mesa. (Ciudadano ya no responde por Candidato ante los vecinos en el final.)"}),

        new Pelea("periodista_ciudadano", "Periodista contra Ciudadano", Rol.PERIODISTA, Rol.CIUDADANO, 3,
                new String[]{"cito_cuenta_anonima", "publico_fuente_reservada"},
                new String[]{
                    "CIUDADANO: Un vecino me contó algo y me pidió reserva. Lo vi publicado en tu libreta con su nombre.",
                    "PERIODISTA: Es información de interés público.",
                    "CIUDADANO: Los vecinos me creyeron a mí, y ahora no confían en mí.",
                    "PERIODISTA: Tampoco puedo tapar todo lo que me cuentan.",
                    "Ringo: Doña Rosa sirve tinto, sin decir nada."},
                new String[]{
                    "PERIODISTA: Debí preguntarte antes. No lo hice porque pensé que me lo ibas a negar.",
                    "CIUDADANO: Y lo habría negado. Pero igual debías preguntar."},
                new String[]{"Pedir perdón y reparar: «Voy a quitar el nombre. Y le pido disculpas al vecino con usted presente.»",
                    "Defenderse: «Una fuente es una fuente.»", "Irse de la mesa."})
                .dia2(new String[]{
                    "CIUDADANO: Ayer pedí que el nombre del vecino no saliera. Hoy salió en tu libreta otra vez.",
                    "PERIODISTA: Es información de interés público.",
                    "CIUDADANO: Para quien la cuenta. Para quien la vive es su casa.",
                    "PERIODISTA: Si me callo, quien tiene el poder gana.",
                    "CIUDADANO: Si hablas sin avisar, el vecino pierde la confianza. Y yo con él.",
                    "Ringo: Samuel, en la punta de la mesa, deja de comer.",
                    "SAMUEL: ¿Se están peleando por mí?",
                    "DOÑA ROSA: No, mijo. Por un nombre."},
                    new String[]{"Reparar: «Voy a quitar el nombre y a preguntarle al vecino si quiere que lo cuente. Contigo presente.»",
                        "Defenderse: «Una fuente es una fuente.»",
                        "Irse de la mesa. (Periodista no puede usar la lupa sobre rumores de Ciudadano en el final.)"}),

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
                    "Defenderse: «La pieza era mía.»", "Irse de la mesa."})
                .dia2(new String[]{
                    "{AFECTADO}: Ayer tu silla estaba vacía. Hoy también.",
                    "{AGRESOR}: Es que la pieza me costó. No quiero que se pierda.",
                    "{AFECTADO}: Una pieza guardada no protege nada. Una pieza puesta, sí.",
                    "{AGRESOR}: Y si la ponen mal, ¿quién responde?",
                    "{AFECTADO}: Los cuatro. Para eso la mesa tiene cuatro huecos.",
                    "Ringo: Doña Rosa se sienta por primera vez en la mesa.",
                    "DOÑA ROSA: Nadie les pide que no tengan miedo. Les pido que lo traigan a la mesa."},
                    new String[]{"Reparar: «Hoy pongo mi pieza primero y miro cómo la usan.»",
                        "Defenderse: «La pieza era mía.»",
                        "Irse de la mesa. (Esa pieza no se puede usar en el final.)"}),

        new Pelea("influencer_casa", "Influencer contra los tres", Rol.INFLUENCER, Rol.PERIODISTA, 4,
                new String[]{"grabo_casa_sin_permiso"},
                new String[]{
                    "PERIODISTA: Reconozco mi cuarto en tu video.",
                    "CANDIDATO: Y mis afiches en el fondo.",
                    "CIUDADANO: Y mi cama sin hacer.",
                    "INFLUENCER: Son tres segundos. Nadie se va a fijar.",
                    "PERIODISTA: No tenías derecho a mostrar eso.",
                    "Ringo: Doña Rosa deja de lavar la taza."},
                new String[]{
                    "INFLUENCER: No pensé que importaba. Para mí enseñar es parte de contar.",
                    "PERIODISTA: Contar es una cosa. Mostrar sin permiso es otra."},
                new String[]{"Pedir perdón y reparar: «Lo bajo ahora. Y desde hoy pregunto antes de grabar en la casa.»",
                    "Defenderse: «Es mi cuenta.»", "Irse de la mesa."})
                .dia2(new String[]{
                    "PERIODISTA: Ayer pediste perdón por el video de mi cuarto. Hoy subiste otro de la cocina.",
                    "INFLUENCER: No salía nadie.",
                    "CANDIDATO: Salía el calendario de mi campaña.",
                    "CIUDADANO: Y la bolsa de pan de Doña Rosa, que no te dio permiso.",
                    "INFLUENCER: Es contenido. A la gente le gusta ver cómo vivimos.",
                    "PERIODISTA: A la gente le gusta ver. A nosotros nos toca decidir qué se muestra.",
                    "Ringo: Doña Rosa apaga la estufa.",
                    "DOÑA ROSA: Mi cocina no es un escenario."},
                    new String[]{"Reparar: «Lo bajo ahora. Y desde hoy lo que grabo en la casa pasa por ustedes antes.»",
                        "Defenderse: «Es mi cuenta.»",
                        "Irse de la mesa. (Influencer no puede usar el megáfono en la casa en el final.)"})
    };

    /** Todas las banderas de agravio (para guardarlas como "_d1" al empezar el Dia 2). */
    public static List<String> banderasDeAgravio() {
        List<String> r = new ArrayList<>();
        for (Pelea pe : PELEAS) {
            for (String b : pe.banderas) {
                if (!r.contains(b)) {
                    r.add(b);
                }
            }
        }
        return r;
    }

    /**
     * Al empezar el Dia 2: los agravios de ayer pasan a "<bandera>_d1". Asi la pelea de hoy solo sale
     * si alguien vuelve a hacer dano HOY (y si ya lo habia hecho ayer, es "agravio repetido").
     */
    public static void archivarAgravios(Partida p) {
        for (Jugador j : p.jugadores()) {
            for (String b : banderasDeAgravio()) {
                if (j.tiene(b)) {
                    j.quitar(b);
                    j.poner(b + "_d1");
                }
            }
        }
    }

    /** true si alguien hizo ese dano en cualquiera de los dos dias. */
    public static boolean alguienEnLosDosDias(Partida p, String bandera) {
        for (Jugador j : p.jugadores()) {
            if (j.tiene(bandera) || j.tiene(bandera + "_d1")) {
                return true;
            }
        }
        return false;
    }

    /** La pelea mas grave que se activo hoy, o null si nadie dano a nadie. */
    public static Pelea elegir(Partida p) {
        Pelea mejor = null;
        for (Pelea pe : PELEAS) {
            if (agresorDe(p, pe) != null && (mejor == null || pe.gravedad > mejor.gravedad)) {
                mejor = pe;
            }
        }
        if (mejor == null && p.dia >= 2) {
            // "Si la pelea del Dia 1 quedo abierta, sale de nuevo" aunque hoy no haya agravio nuevo
            for (Pelea pe : PELEAS) {
                if (p.bandera("pelea_abierta_" + pe.id) && agresorDe(p, pe) != null
                        && (mejor == null || pe.gravedad > mejor.gravedad)) {
                    mejor = pe;
                }
            }
        }
        return mejor;
    }

    private static Jugador agresorDe(Partida p, Pelea pe) {
        Jugador ayer = null;
        for (Jugador j : p.jugadores()) {
            if (pe.agresor != null && j.rol != pe.agresor) {
                continue;
            }
            for (String b : pe.banderas) {
                if (j.tiene(b) || p.bandera(b)) {
                    return j;
                }
                if (ayer == null && p.bandera("pelea_abierta_" + pe.id) && j.tiene(b + "_d1")) {
                    ayer = j;      // pelea abierta de ayer: el agresor es el de ayer
                }
            }
        }
        return ayer;
    }

    /** Dia 2: el mismo agravio sale por segunda vez (o la pelea de ayer quedo abierta). */
    private static boolean repetida(Partida p, Pelea pe, Jugador agresor) {
        if (p.dia < 2) {
            return false;
        }
        if (p.bandera("pelea_d1_" + pe.id) || p.bandera("pelea_abierta_" + pe.id)) {
            return true;
        }
        for (String b : pe.banderas) {
            if (agresor.tiene(b + "_d1")) {
                return true;
            }
        }
        return false;
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
        final boolean repetida = repetida(p, pe, agresor);
        final boolean estabaAbierta = p.dia >= 2 && p.bandera("pelea_abierta_" + pe.id);
        String[] base = repetida && pe.lineasDia2 != null ? pe.lineasDia2 : pe.lineas;
        final String[] cierres = repetida && pe.cierresDia2 != null ? pe.cierresDia2 : pe.cierres;
        List<String> ls = new ArrayList<>();
        if (estabaAbierta) {
            // "sale de nuevo con una linea mas"
            ls.add("DOÑA ROSA: Esta conversación quedó abierta anoche. Hoy se termina en la mesa.");
        }
        for (String s : base) {
            ls.add(s.replace("{AGRESOR}", agresor.rol.name()).replace("{AFECTADO}", afectado.rol.name()));
        }
        if (repetida) {
            ls.add("Ringo: Es la segunda vez. Si esto se rompe, se rompe también la ayuda entre los dos para el final.");
        }
        final String[] lineas = ls.toArray(new String[0]);
        // lo recuerda la manana siguiente (GuionDia2) y la cena del Dia 2
        p.poner("pelea_d" + p.dia + "_" + pe.id);
        p.poner("pelea_d" + p.dia + "_agresor_" + agresor.rol.name());
        p.poner("pelea_d" + p.dia + "_afectado_" + afectado.rol.name());
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
                                    p.poner("pelea_d" + p.dia + "_abierta");
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
                        Lugar.op(cierres[0], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, 2), "Pelea: reparar");
                            p.confianza.bloquear(agresor.rol, afectado.rol, false);
                            agresor.objetos.add("Objeto pequeño de " + afectado.nombre);
                            p.poner("pelea_d" + p.dia + "_reparo");
                            if (p.dia >= 2) {
                                p.poner("pelea_resuelta_" + pe.id);
                                if (pe.id.equals("silla_vacia")) {
                                    // "el Letrero del lugar sube una pieza si todavia se puede"
                                    for (Pieza pz : agresor.piezas()) {
                                        Letrero le = p.letrero(pz.lugar);
                                        if (le != null && !le.tiene(agresor.rol)) {
                                            le.poner(pz);
                                        }
                                    }
                                    agresor.quitar("guardo_pieza");
                                }
                            }
                            sig.run();
                        }),
                        Lugar.op(cierres[1], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Pelea: defenderse");
                            p.poner("pelea_d" + p.dia + "_defendio");
                            sig.run();
                        }),
                        Lugar.op(cierres[2], () -> {
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, -2), "Pelea: irse de la mesa");
                            p.confianza.bloquear(agresor.rol, afectado.rol, true);
                            p.poner("pelea_d" + p.dia + "_se_fue");
                            if (repetida) {
                                // "puede romper la ayuda entre los dos roles para el final"
                                p.poner("ayuda_rota_" + agresor.rol.name() + "_" + afectado.rol.name());
                            }
                            sig.run();
                        })))
                .hacer(() -> {
                    if (fin != null) {
                        fin.run();
                    }
                })
                .iniciar();
    }

    /**
     * LA PELEA BUENA (cuando nadie hizo dano): dos jugadores eligieron distinto en dos decisiones
     * digitales del mismo dia (uno verifico y el otro se tomo un tinto). Maximo una por partida.
     * El guion la registra en GuionDia2 con banderas "dig_<lugar>_<ROL>_<opcion>".
     *
     * @return los dos que discuten {verifico, tinto}, o null si no aplica
     */
    public static Jugador[] peleaBuena(Partida p, List<String> lugares) {
        if (p.bandera("pelea_buena_hecha")) {
            return null;
        }
        for (Jugador a : p.jugadores()) {
            for (Jugador b : p.jugadores()) {
                if (a == b) {
                    continue;
                }
                int distintas = 0;
                boolean verificoYTinto = false;
                for (String lu : lugares) {
                    int oa = opcionDigital(p, lu, a.rol), ob = opcionDigital(p, lu, b.rol);
                    if (oa >= 0 && ob >= 0 && oa != ob) {
                        distintas++;
                        verificoYTinto |= oa == Lugar.A && ob == Lugar.C;
                    }
                }
                if (distintas >= 2 && verificoYTinto) {
                    return new Jugador[]{a, b};
                }
            }
        }
        return null;
    }

    private static int opcionDigital(Partida p, String lugar, Rol r) {
        for (int op = 0; op < 5; op++) {
            if (p.bandera("dig_" + lugar + "_" + r.name() + "_" + op)) {
                return op;
            }
        }
        return -1;
    }

    public static void jugarPeleaBuena(Lugar l, Jugador verifico, Jugador tinto, Runnable fin) {
        Partida p = l.partida();
        p.poner("pelea_buena_hecha");
        List<Jugador> otros = p.otros(verifico.rol, tinto.rol);
        String v = verifico.rol.name(), t = tinto.rol.name();
        String o1 = otros.size() > 0 ? otros.get(0).rol.name() : "Ringo";
        String o2 = otros.size() > 1 ? otros.get(1).rol.name() : "Ringo";
        List<Jugador> dos = new ArrayList<>();
        dos.add(verifico);
        dos.add(tinto);
        final boolean[] seEscucharon = {false};
        new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: La cena, sin agravio.",
                        v + ": Hoy tú te tomaste un tinto y yo me puse a verificar. ¿Cuál estuvo mejor?",
                        t + ": Yo creo que el tinto. Si no respiro, me equivoco.",
                        v + ": Y yo creo que si no verifico, otros se equivocan por mí.",
                        o1 + ": Los dos tienen algo de razón.",
                        o2 + ": Eso no vale. Escojan.",
                        "Ringo: Doña Rosa sirve más tinto y se sienta a escuchar."))
                .paso(sig -> DecisionGrupal.algunos(l, otros, "La pelea buena. ¿Qué haces?",
                        new String[]{"Mediar: «Cada uno hace lo que necesita. Pero hoy es mejor sumarlo.»",
                            "Apoyar a " + verifico.rol.nombre, "Apoyar a " + tinto.rol.nombre, "Callar y escuchar"},
                        (j, op) -> {
                            if (op == 0) {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, 1), "Pelea buena: mediar");
                                l.mariposa(j);
                            } else if (op == 1) {
                                p.confianza.sumar(j.rol, verifico.rol, 1);
                            } else if (op == 2) {
                                p.confianza.sumar(j.rol, tinto.rol, 1);
                            }
                        }, r -> sig.run()))
                .paso(sig -> DecisionGrupal.algunos(l, dos, "Cierre de la pelea buena:",
                        new String[]{"Escucharse: «Dime qué viste tú.»", "Cada uno defiende lo suyo: «Yo sigo con lo mío.»"},
                        (j, op) -> {
                            if (op == 0) {
                                p.confianza.sumar(verifico.rol, tinto.rol, 1);
                                seEscucharon[0] = true;
                            }
                        }, r -> {
                            if (seEscucharon[0]) {
                                l.efectoCompartido(Efecto.de(Partida.ANIMO, 1), "Pelea buena: se escucharon");
                                p.confianza.bloquear(verifico.rol, tinto.rol, false);
                            }
                            sig.run();
                        }))
                .paso(sig -> l.dialogo(sig, "DOÑA ROSA: En esta casa se puede discutir. Lo malo es no volver a sentarse."))
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
                "Ringo: Los cuatro se ríen. Samuel se ríe más fuerte.");
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
