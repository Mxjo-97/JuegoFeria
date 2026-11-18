package juegoferia;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * El guion del DIA 2 convertido en juego (un solo guion para los cuatro roles).
 *
 *   La casa (7:00) -> Panaderia (7:10) -> Camino a la Alcaldia (9:00) -> Alcaldia (9:30)
 *   -> Parque y malecon (12:00) -> Junta (15:00) -> El audio vuelve, en la casa (17:00)
 *   -> La cena y la revelacion (19:00) -> La niebla del Dia 2 -> Final A, B o C
 *
 * Se escribe igual que GuionDia1 (ver su comentario): vecinos con c.vecino(...), decision de respeto,
 * decision digital con l.digital(...), minijuego fijo, las cuatro pruebas y alCompletar(...).
 * Lo nuevo del Dia 2 son las VOTACIONES DE LA CASA (DecisionGrupal.votacion).
 *
 * Lo que llega del Dia 1 (banderas):
 *   - origen_audio_A / _B / _C: lo que se voto anoche sobre el origen del audio.
 *   - rumor_alcaldia_adelantado: alguien conto el rumor en el apagon (la Alcaldia ya tiene +10).
 *   - apagon_pregunto_mauricio: alguien le pregunto a Mauricio en el apagon.
 *   - niebla_vuelve_<lugar> / letrero_cuidado_<lugar>: Letreros de tres piezas (y si se cuidaron de noche).
 *   - pelea_d1_*, pelea_abierta_<id>: como termino la pelea de anoche.
 *   - se_rio_ante_la_burla (en un Jugador): Daniela no llega al malecon hasta las 15:00.
 *
 * Fondos: los 13 oficiales (ver Rutas, seccion "fondos del DIA 2"). Posiciones PROVISIONALES:
 * acomodarlas con F1 cuando esten los fondos de Canva en el proyecto.
 */
public final class GuionDia2 {

    private GuionDia2() {
    }

    // ---- ids de los lugares del dia 2 (los del mapa son los mismos del Dia 1: asi siguen su desinformacion) ----
    public static final String CASA = GrafoLocaciones.CASA;
    public static final String PANADERIA = GrafoLocaciones.PANADERIA;
    public static final String CAMINO = "Camino_alcaldia";
    public static final String ALCALDIA = GrafoLocaciones.ALCALDIA;
    public static final String MALECON = GrafoLocaciones.MALECON;
    public static final String JUNTA = GrafoLocaciones.JUNTA;
    public static final String TARDE = "Casa_tarde";
    public static final String CENA = "Casa_noche";

    public static final String PRIMER_LUGAR = CASA;

    /** Los cuatro lugares que se cuentan al final (2.8 La niebla del Dia 2). */
    public static final String[] LUGARES_FINAL = {PANADERIA, ALCALDIA, MALECON, JUNTA};

    private static final Color AVISO = new Color(0xFFD35C);

    public static Lugar crear(String lugar, Partida p, Lugar.Anfitrion h) {
        switch (lugar) {
            case CASA: return casaManana(p, h);
            case PANADERIA: return panaderia(p, h);
            case CAMINO: return camino(p, h);
            case ALCALDIA: return alcaldia(p, h);
            case MALECON: return malecon(p, h);
            case JUNTA: return junta(p, h);
            case TARDE: return elAudioVuelve(p, h);
            case CENA: return cena(p, h);
            default:
                throw new IllegalArgumentException("El Día 2 no tiene el lugar " + lugar);
        }
    }

    // =====================================================================================
    // AYUDAS
    // =====================================================================================

    /** Guarda lo que eligio cada uno en la decision digital (para "la pelea buena" de la cena). */
    private static void registrarDigital(Partida p, String lugar, Map<Rol, Integer> el) {
        for (Map.Entry<Rol, Integer> e : el.entrySet()) {
            p.poner("dig_" + lugar + "_" + e.getKey().name() + "_" + e.getValue());
        }
    }

    /** Alguien difundio sin verificar en cualquiera de los dos dias. */
    private static boolean alguienDifundio(Partida p) {
        return GestorPeleas.alguienEnLosDosDias(p, "difundio_sin_verificar");
    }

    private static List<Jugador> quienesDifundieron(Partida p) {
        List<Jugador> r = new ArrayList<>();
        for (Jugador j : p.jugadores()) {
            if (j.tiene("difundio_sin_verificar") || j.tiene("difundio_sin_verificar_d1")) {
                r.add(j);
            }
        }
        return r;
    }

    private static Rol rolDeBandera(Partida p, String prefijo) {
        for (Rol r : Rol.values()) {
            if (p.bandera(prefijo + r.name())) {
                return r;
            }
        }
        return null;
    }

    /** Lo que pasa una sola vez al empezar el Dia 2 (consecuencias de anoche). */
    private static void empezarDia(Partida p) {
        if (p.bandera("dia2_empezado")) {
            return;
        }
        p.poner("dia2_empezado");
        GestorPeleas.archivarAgravios(p);
        for (Jugador j : p.jugadores()) {
            for (Pieza pz : j.piezas()) {
                j.sacarPieza(pz.lugar);      // las piezas de ayer ya estan en los Letreros de ayer
            }
        }
        // Letreros de tres piezas: la niebla vuelve hoy, salvo que los hayan cuidado por turnos
        for (String lu : new String[]{PANADERIA, MALECON, JUNTA, GrafoLocaciones.PLAZA, ALCALDIA}) {
            if (p.bandera("niebla_vuelve_" + lu) && !p.bandera("letrero_cuidado_" + lu)) {
                p.nieblaOn(lu);
            }
        }
        // lo que se voto anoche sobre el origen del audio
        if (p.bandera("origen_audio_A")) {
            p.sumarAnimo(1);
            p.confianza.sumarTodas(1);
            p.sumarDesinfo(PANADERIA, -5);
        } else if (p.bandera("origen_audio_B")) {
            p.sumarAnimo(-1);
        } else if (p.bandera("origen_audio_C")) {
            p.sumarDesinfo(PANADERIA, 5);
        }
    }

    // =====================================================================================
    // LA MANANA EN LA CASA (7:00)
    // =====================================================================================

    private static Lugar casaManana(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CASA, "La casa · Día 2", Reloj.h(7, 0));
        GuionDia1.Ctx c = new GuionDia1.Ctx(l);
        l.fondo(Rutas.CASA_DIA2);
        l.caminable(80, 620, 1760, 440);
        l.equipoEn(900, 900);
        l.objetivo("manana", "Bajar a desayunar");
        c.vecino("samuel", "Samuel", "el niño", 1380, 700, new Color(0x6FA8DC),
                "SAMUEL: Salomé me dice que en el colegio van a revisar los celulares. ¿Es verdad que se los van a quitar?");
        l.salida(440, 930, 290, 130, PANADERIA, "Bajar a la panadería");
        l.obstaculo(560, 640, 440, 160, "Mesa");  l.obstaculo(60, 760, 380, 320, "Baranda");

        l.alPrimeraEntrada(() -> {
            empezarDia(p);
            new Secuencia()
                    .paso(sig -> l.dialogo(sig,
                            "Ringo: Día 2 de 2. Mañana es la elección.",
                            "Ringo: Los Letreros del Día 1 siguen colgados. En los lugares sin Letrero todavía queda un poco de bruma. Doña Rosa ya abrió la panadería."))
                    .paso(sig -> mananaSegunLaPelea(l, sig))
                    .paso(sig -> l.dialogo(sig,
                            "INFLUENCER: ¿Alguien más soñó con el audio?",
                            "PERIODISTA: Yo no soñé. Estuve toda la noche rastreando de dónde salió.",
                            "CANDIDATO: ¿Y?",
                            "PERIODISTA: Y nada. Nadie sabe.",
                            "CIUDADANO: Bajemos. Doña Rosa ya abrió.",
                            "Ringo: Samuel baja sin levantar la vista del cuaderno.",
                            "SAMUEL: Cinco minutos más. Salomé me dice que en el colegio van a revisar los celulares. ¿Es verdad que se los van a quitar?"))
                    .paso(sig -> {
                        if (p.bandera("origen_audio_A")) {
                            l.dialogo(sig, "DOÑA ROSA: (desde abajo) Ya lo sé. Y ya desayunaron sin esconderme nada, así que les toca pan doble.",
                                    "Ringo: Ánimo +1. La panadería de hoy empieza con menos desinformación.");
                        } else if (p.bandera("origen_audio_B")) {
                            l.dialogo(sig, "DOÑA ROSA: (seca) Me enteré por Vlado. Qué curioso, en mi propia casa.",
                                    "Ringo: Ánimo -1. «Las cinco de una noticia» arranca con un campo más marcado.");
                        } else if (p.bandera("origen_audio_C")) {
                            l.dialogo(sig, "Ringo: Doña Rosa pone el pan sin preguntar nada. Se nota que algo falta por decir.",
                                    "Ringo: La panadería de hoy empieza con más desinformación.");
                        } else {
                            sig.run();
                        }
                    })
                    .hacer(() -> {
                        l.completar("manana");
                        l.toast("Bajen a la panadería (derecha)", AVISO);
                    })
                    .iniciar();
        });
        return l;
    }

    /** "El Dia 2 empieza segun lo que paso anoche" (pelea reparada, alguien se fue, quedo abierta o no hubo). */
    private static void mananaSegunLaPelea(Lugar l, Runnable fin) {
        Partida p = l.partida();
        Rol agresor = rolDeBandera(p, "pelea_d1_agresor_");
        Rol afectado = rolDeBandera(p, "pelea_d1_afectado_");
        boolean hubo = agresor != null && afectado != null;
        if (hubo && p.bandera("pelea_d1_reparo")) {
            l.efectoCompartido(Efecto.de(Partida.ANIMO, 1), "Mañana: pelea reparada");
            p.confianza.bloquear(agresor, afectado, false);
            l.dialogo(fin, "Ringo: Los dos se encuentran en la escalera.",
                    agresor.name() + ": Ya lo hice. Está bajado.",
                    afectado.name() + ": Gracias. Yo también tengo que decir algo: no te expliqué antes.",
                    "Ringo: Se saludan, un poco torpes. Pueden prestarse habilidades.");
        } else if (hubo && p.bandera("pelea_d1_se_fue")) {
            l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Mañana: se evitan");
            p.confianza.bloquear(agresor, afectado, true);
            l.dialogo(fin, "Ringo: Los dos se evitan en el pasillo. Uno mira por la ventana, el otro baja por la escalera sin esperar.",
                    "DOÑA ROSA: (desde abajo) Si van a evitarse, al menos háganlo con un pan de los míos en la mano.",
                    "Ringo: Hoy no pueden prestarse habilidades.");
        } else if (hubo && p.bandera("pelea_d1_abierta")) {
            l.dialogo(fin, "Ringo: Los dos hablan con frases muy cortas.",
                    agresor.name() + ": Buenos días.",
                    afectado.name() + ": Buenos días.",
                    "DOÑA ROSA: Cuando quieran, esa conversación me la cuentan con tinto.",
                    "Ringo: La pelea vuelve esta noche, más fuerte, si no se resuelve antes.");
        } else if (hubo) {
            l.dialogo(fin, "Ringo: Los dos se cruzan en la escalera. Ninguno dice nada de anoche.");
        } else {
            l.efectoCompartido(Efecto.de(Partida.ANIMO, 1), "Mañana: sin pelea");
            l.dialogo(fin, "Ringo: Sobre la mesa hay una bandeja de pan caliente y una nota de Doña Rosa: «Para los cuatro.»",
                    "DOÑA ROSA: (desde abajo) ¡Se comen todo!");
        }
    }

    // =====================================================================================
    // 2.1 PANADERIA (7:10)
    // =====================================================================================

    private static Lugar panaderia(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, PANADERIA, "Panadería · Día 2", Reloj.h(7, 10));
        GuionDia1.Ctx c = new GuionDia1.Ctx(l);
        l.fondo(Rutas.PANADERIA_DIA2);
        l.caminable(70, 570, 1780, 470);
        l.obstaculo(980, 440, 740, 160, "Mostrador");
        l.obstaculo(100, 820, 300, 200, "Mesita");
        l.equipoEn(900, 960);

        l.rumor("En el colegio van a revisar todos los celulares a la entrada.", 0);
        l.letrero("ESTO ES UNA REGLA PARA GUARDAR EL CELULAR, NO UNA LLAVE PARA ABRIRLO.");
        l.niebla("La bruma vuelve a entrar por la ventana. En la fila, nadie recuerda para qué sirve un casillero.",
                "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo. Siempre hay algo que dicen que van a quitar.");

        l.objetivo("respeto", "Lo que pasa con Mateo");
        l.objetivo("digital", "El audio del colegio (celular, Q)");
        l.objetivo("minijuego", "Las cinco de una noticia");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("rosa", "Doña Rosa", "la señora", 1400, 600, new Color(0xB5473A),
                "DOÑA ROSA: Hoy llegó más gente que ayer. Y todos con hambre de chisme.");
        c.vecino("ursula", "Doña Úrsula", "la anciana", 200, 670, new Color(0x8A6A9A),
                "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo. Siempre hay algo que dicen que van a quitar.");
        c.vecino("leonel", "Don Leonel", "el señor", 470, 720, new Color(0x7A6A5A),
                "DON LEONEL: (doblando el periódico) Antes uno sabía el chisme porque lo oía en la esquina. Ahora el chisme lo sigue a uno hasta el baño.");
        c.vecino("cecilia", "Cecilia", "la de la vitrina", 1620, 620, new Color(0xE58FA8),
                "CECILIA: (detrás de la vitrina) Hoy llegó más gente que ayer. Todos con el celular en la mano.");
        c.vecino("mateo", "Mateo", "el muchacho", 820, 680, new Color(0x4C8BD9),
                "MATEO: Mi hermanita guarda en el celular hasta los dibujos.");
        c.vecino("nelly", "Doña Nelly", "la señora", 640, 760, new Color(0xC9A0DC),
                "DOÑA NELLY: Yo a mi hijo le leo todo y no por eso me quiere menos.");
        c.vecino("eliecer", "Don Eliécer", "el señor", 450, 930, new Color(0x7A6A5A),
                "DON ELIÉCER: Yo le reviso el celular a mi hijo todas las noches.");
        c.vecino("vlado", "Vlado (tele)", "la tele", 1720, 620, new Color(0x333344),
                "VLADO: Buenos días, Macondo. Último día antes de la elección.");
        c.vecino("visitacion", "Rectora Visitación", "la rectora", 1150, 780, new Color(0x2E4A7A),
                "RECTORA VISITACIÓN: Un audio sin nombre viaja más rápido que una citación con fecha.");

        l.salida(1480, 930, 280, 110, CAMINO, "Salir hacia la Alcaldía");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "DON LEONEL: Antes uno sabía el chisme porque lo oía en la esquina. Ahora el chisme lo sigue a uno hasta el baño.",
                        "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo. Siempre hay algo que dicen que van a quitar.",
                        "CECILIA: Hoy llegó más gente que ayer. Todos con el celular en la mano.",
                        "Ringo: En el grupo del barrio circula un audio de una voz de mujer, sin nombre: «En el colegio van a revisar todos los celulares a la entrada.» Ya tiene cuarenta pulgares arriba.",
                        "VLADO: (voz de la tele) Nos llegan videos de padres alarmados: se dice que el colegio revisará los celulares de los estudiantes a la entrada. Repito: revisará los celulares.",
                        "MATEO: (en la fila, apretando el celular) Doña Rosa, ¿sí vio? Mi hermanita tiene doce años y guarda ahí hasta los dibujos. Si alguien se los ve, se muere de la pena.",
                        "DOÑA NELLY: Yo a mi hijo le leo todo y no por eso me quiere menos.",
                        "DON ELIÉCER: Yo le reviso el celular a mi hijo todas las noches, y usted debería hacer lo mismo.",
                        "INFLUENCER: Yo no sé qué opino. ¿Ustedes?",
                        "CIUDADANO: Yo creo que depende de cada casa.",
                        "PERIODISTA: Yo creo que depende de qué diga la citación.",
                        "CANDIDATO: Yo creo que depende de quién pregunte.",
                        "Ringo: Mateo se queda callado, apretando el celular contra el pecho."))
                .paso(sig -> respetoMateo(l))
                .iniciar());

        l.digital("Grupo del barrio",
                "Audio de una voz de mujer (0:42): «En el colegio van a revisar todos los celulares a la entrada.»",
                "El audio sigue corriendo. Ya tiene cuarenta pulgares arriba y una señora preguntando si es cierto.",
                new String[]{
                    "Verificar: antes de pasarlo, pregunto de dónde salió y busco la citación del colegio.",
                    "Reportar: marco el audio como dudoso y aviso al administrador del grupo.",
                    "Tomarme un tinto: dejo el celular, pido un tinto y respiro.",
                    "Guardar silencio: no digo nada y sigo en la fila.",
                    "Difundir: lo reenvío a mi familia por si acaso."},
                new String[]{
                    "{rol} se puso a verificar el audio del celular.",
                    "{rol} reportó el audio del colegio.",
                    "{rol} se tomó un tinto.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió el audio del colegio."},
                el -> despuesDigitalPanaderia(c, el));
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "eliecer"));

        // ---- las cuatro pruebas ----
        c.prueba("mateo", Rol.CIUDADANO, "carton_rosa_d2", "Cartón de Doña Rosa",
                "CIUDADANO: Mateo, ¿qué dice tu hermanita?",
                "MATEO: Que no le da pena guardarlo. Le da pena que se lo vean.",
                "Ringo: Doña Rosa le entrega otro cartón escrito con carbón.");
        c.prueba("visitacion", Rol.PERIODISTA, "citacion", "Citación",
                "PERIODISTA: ¿Me puede dar la citación?",
                "RECTORA VISITACIÓN: Aquí está. Dice «guardar», no «revisar».");
        c.prueba("rosa", Rol.INFLUENCER, "pintura_carbon_d2", "Pintura de carbón",
                "INFLUENCER: Va el aviso.",
                "Ringo: Pinta con carbón: «La regla es guardar el celular, no revisarlo.»");
        c.prueba("visitacion", Rol.CANDIDATO, "firma_panaderia_d2", "Firma de Candidato",
                "CANDIDATO: Me comprometo a no pedir celulares de estudiantes en mi campaña.",
                "RECTORA VISITACIÓN: Eso sí se agradece.");

        // ---- la verdad del rumor ----
        l.alCompletar(() -> l.dialogo(() -> l.toast("Salgan hacia la Alcaldía (puerta de la izquierda)", AVISO),
                "RECTORA VISITACIÓN: Lo que se va a pedir es que los celulares se guarden en la entrada, en un casillero con llave. No se abre nadie.",
                "RECTORA VISITACIÓN: Un audio sin nombre viaja más rápido que una citación con fecha.",
                "DOÑA NELLY: (despacio) Entonces no iban a revisar nada.",
                "MATEO: (sonriendo por fin) Esta noche le digo a mi hermanita que sus dibujos están a salvo."));
        return l;
    }

    private static void respetoMateo(Lugar l) {
        Map<Rol, String> reacA = new EnumMap<>(Rol.class);
        reacA.put(Rol.PERIODISTA, "Eso es lo que hay que decir.");
        DecisionGrupal.respeto(l, "Mateo se queda callado, apretando el celular contra el pecho.",
                new String[]{
                    "Decirle a Mateo que los dibujos de su hermanita son de ella.",
                    "Cambiar de tema hacia el audio.",
                    "Decirle a Don Eliécer: «Tiene razón, revise todo.»"},
                (j, op) -> {
                    if (op == 0) {
                        GuionDia1.respetoA(l, j, 2);
                        l.dialogo(null, GuionDia1.unir(GuionDia1.comoJugador(j,
                                "JUGADOR: Mateo, los dibujos de tu hermanita son de ella. A nadie le gusta que se los miren sin permiso.",
                                "Ringo: Mateo levanta la cara.",
                                "MATEO: ¿De verdad? Gracias. A veces no sé cómo decírselo a ella."), GuionDia1.reacciones(j, reacA)));
                    } else if (op == 1) {
                        l.dialogo(null, GuionDia1.comoJugador(j,
                                "JUGADOR: Veamos de dónde salió el audio, a ver si es cierto.",
                                "Ringo: Mateo baja la cabeza y guarda el celular."));
                        l.completar("respeto");
                    } else {
                        l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -3, Partida.ANIMO, -1), "Respeto: revisar todo");
                        j.poner("apoyo_revisar_sin_permiso");
                        String quien = j.rol == Rol.CIUDADANO ? "PERIODISTA" : "CIUDADANO";
                        l.dialogo(null, GuionDia1.comoJugador(j,
                                "JUGADOR: Tiene razón, Don Eliécer. Los padres tienen que saber qué hay en el celular.",
                                "Ringo: Mateo mira al piso.",
                                quien + ": A la hermanita de Mateo le tocaba decidir sobre sus dibujos. No a nosotros."));
                        l.completar("respeto");
                    }
                },
                sumados -> {
                    GuionDia1.yoTambien(l, sumados);
                    l.completar("respeto");
                    l.toast("Revisen el celular (Q): el audio sigue corriendo", AVISO);
                });
    }

    private static void despuesDigitalPanaderia(GuionDia1.Ctx c, Map<Rol, Integer> el) {
        Lugar l = c.l;
        Partida p = c.p;
        registrarDigital(p, PANADERIA, el);
        List<String> d = new ArrayList<>();
        if (el.containsValue(Lugar.A)) {
            d.add("RECTORA VISITACIÓN: Gracias por preguntar antes de repartir.");
        }
        if (el.containsValue(Lugar.B)) {
            d.add("Ringo: El audio queda marcado y baja en el grupo.");
        }
        if (el.containsValue(Lugar.C)) {
            d.add("DOÑA ROSA: Tinto para quien respira antes de reenviar.");
        }
        if (el.containsValue(Lugar.E)) {
            d.add("MATEO: (en la fila, leyendo) Ahora sí me asusté.");
            d.add("PERIODISTA: Ayer ya vimos cómo termina esto.");
            d.add("INFLUENCER: Pero hoy el tema es otro.");
        }
        if (el.containsValue(Lugar.A)) {
            d.add("CIUDADANO: Eso es seguir el hilo.");
        }
        d.add("Ringo: La rectora Visitación saca un papel roto en seis pedazos.");
        d.add("RECTORA VISITACIÓN: Si usted arma bien este papel, sabe más que el audio.");

        boolean pistaQuien = el.containsValue(Lugar.A);
        boolean pistaOtra = p.bandera("origen_audio_B");
        l.dialogo(() -> l.minijuegoFijo(Rol.PERIODISTA, new MjCincoNoticia(pistaQuien, pistaOtra).dialogoFinal(nv -> {
            String[] r;
            if (nv == Minijuego.BIEN) {
                r = new String[]{"PERIODISTA: El audio no dice quién, ni cuándo, ni por qué. Y dice «revisar». La citación dice «guardar».",
                    "RECTORA VISITACIÓN: Exacto."};
            } else if (nv == Minijuego.REGULAR) {
                r = new String[]{"RECTORA VISITACIÓN: Falta ver qué no dice el audio."};
            } else {
                r = new String[]{"RECTORA VISITACIÓN: (lee ella) Se las explico yo."};
            }
            return r;
        }), res -> {
                l.completar("minijuego");
                c.activarPruebas();
        }), d.toArray(new String[0]));
    }

    // =====================================================================================
    // 2.2 CAMINO A LA ALCALDIA (9:00)
    // =====================================================================================

    private static Lugar camino(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CAMINO, "Camino a la Alcaldía", Reloj.h(9, 0));
        l.fondo(Rutas.CAMINO_ALCALDIA);
        l.caminable(60, 820, 1800, 220);
        l.obstaculo(240, 820, 410, 120, "Jardinera");  l.obstaculo(1340, 820, 500, 120, "Jardinera");  l.obstaculo(60, 920, 290, 120, "Banca");
        l.equipoEn(700, 1000);
        l.objetivo("charla", "Caminar juntos");
        l.objetivo("voto", "Votar cómo entrar");
        l.salida(860, 800, 280, 90, ALCALDIA, "Entrar a la Alcaldía");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "CANDIDATO: Esta es mi parte. Aquí sí sé qué decir.",
                        "CIUDADANO: Hoy deja hablar a la gente, por favor.",
                        "CANDIDATO: Me estás pidiendo que no sea yo.",
                        "CIUDADANO: Te estoy pidiendo que escuches primero."))
                .paso(sig -> {
                    Jugador cand = p.jugador(Rol.CANDIDATO);
                    l.decidirTurno(cand, "Respuesta rápida a {CIUDADANO}:",
                            Lugar.op("«Está bien, escucho.»", () -> {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, 1), "Camino: escucho");
                                l.dialogo(sig, "CANDIDATO: Está bien, escucho.");
                            }),
                            Lugar.op("«No me callo.»", () -> {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, -1), "Camino: no me callo");
                                l.dialogo(sig, "CANDIDATO: No me callo.",
                                        "CIUDADANO: Entonces te voy a traducir lo que oigas.");
                            }),
                            Lugar.op("«Hablemos mientras caminamos.»", () -> l.dialogo(sig,
                                    "CANDIDATO: Hablemos mientras caminamos. Y ya veremos quién convence a quién.")));
                })
                .hacer(() -> l.completar("charla"))
                .paso(sig -> DecisionGrupal.votacion(l, "¿Cómo entramos a la Alcaldía?",
                        new String[]{"Entrar preguntando, sin discurso (prudente).",
                            "Entrar con propuesta: Candidato abre.",
                            "Entrar cada uno por su lado."}, 0,
                        g -> {
                            p.poner("entrada_alcaldia_" + (char) ('A' + g));
                            l.fondo(Rutas.ALCALDIA_PUERTAS);
                            if (g == 0) {
                                p.sumarDesinfo(ALCALDIA, -5);
                                l.dialogo(sig, "CIUDADANO: Entramos callados y escuchamos la fila primero.",
                                        "Ringo: La Alcaldía arranca con menos desinformación.");
                            } else if (g == 1) {
                                l.efecto(p.jugador(Rol.CANDIDATO), Efecto.de(EstadoJuego.ALCANCE, 1), "Votación: Candidato abre");
                                l.dialogo(sig, "CANDIDATO: Yo abro, ustedes completan.");
                            } else {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, -1), "Votación: cada uno por su lado");
                                l.dialogo(sig, "INFLUENCER: Cada quien por su lado y nos vemos en el patio.",
                                        "Ringo: En el minijuego de la circular, los otros tres no van a poder aconsejar.");
                            }
                        }))
                .paso(sig -> {
                    l.npc("samuel", "Samuel", "el niño", 1550, 1000, new Color(0x6FA8DC));
                    Jugador lider = p.lider();
                    if (lider != null) {
                        lider.objetos.add("Bolsa de pan de Samuel");
                    }
                    l.dialogo(sig, "Ringo: Samuel los alcanza corriendo.",
                            "SAMUEL: Mi abuela dice que vayan con cuidado.",
                            "INFLUENCER: ¿Y tú qué dices?",
                            "SAMUEL: Que lleven pan.",
                            "Ringo: Samuel les da una bolsa de pan y regresa corriendo.");
                })
                .hacer(() -> {
                    Actor s = l.actor("samuel");
                    if (s != null) {
                        l.quitar(s);
                    }
                    l.completar("voto");
                    l.toast("Entren a la Alcaldía (derecha)", AVISO);
                })
                .iniciar());
        return l;
    }

    // =====================================================================================
    // 2.3 ALCALDIA (9:30)
    // =====================================================================================

    private static Lugar alcaldia(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, ALCALDIA, "Alcaldía", Reloj.h(9, 30));
        GuionDia1.Ctx c = new GuionDia1.Ctx(l);
        l.fondo(Rutas.ALCALDIA_DIA2);
        l.caminable(60, 660, 1800, 380);
        l.obstaculo(60, 880, 410, 160, "Sillas");  l.obstaculo(1400, 680, 230, 90, "Cordón");
        l.equipoEn(800, 960);

        l.rumor("Nos van a revisar los celulares a todos, y al que dé me gusta a un candidato lo botan.", 0);
        l.letrero("ESTO ES UNA CIRCULAR. DICE LO QUE NO SE PUEDE HACER, NO QUIÉN VA A MIRAR.");
        l.niebla("La niebla sube por la escalera de mármol. En la fila nadie recuerda qué decía la circular ni para qué era.",
                "MARISOL: Y cada uno la leyó como le dio miedo.");

        l.objetivo("marisol", "Lo que le pasa a Marisol");
        l.objetivo("madres", "Las dos madres en la fila");
        l.objetivo("digital", "El audio de la Alcaldía (Q)");
        l.objetivo("minijuego", "El recorrido de la circular");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("teofilo", "Don Teófilo", "el señor", 680, 780, new Color(0x8A7A5A),
                "DON TEÓFILO: Yo no sé usar eso. Me dijeron que lo hiciera por la aplicación. Mi nieta me ayuda pero hoy no está.");
        c.vecino("ramiro", "Don Ramiro", "el señor", 820, 760, new Color(0x5A6A8A),
                "DON RAMIRO: Yo llevo veinte años aquí y no sé si me quedo.");
        c.vecino("kevin", "Kevin", "el muchacho", 960, 780, new Color(0x4C8BD9),
                "KEVIN: Una candidatura dice números claros y me asusta. Otra dice continuidad y me asusta más.");
        c.vecino("brayan", "Brayan", "el muchacho", 1100, 760, new Color(0x3E7F2F),
                "BRAYAN: La Alcaldía debería tener un intérprete de señas. Mi hermana Lucía es sorda y nunca ha podido hacer un trámite sola.");
        c.vecino("marisol", "Marisol", "la de la ventanilla", 1260, 650, new Color(0x9A3A4A),
                "MARISOL: Siguiente, por favor.");
        c.vecino("mauricio", "Mauricio", "el de la bicicleta", 250, 760, new Color(0xE0C030),
                "MAURICIO: Yo llevé esa circular a todos los pisos. Nadie me preguntó qué decía.");
        c.vecino("clemencia", "Doña Clemencia", "la funcionaria", 1500, 800, new Color(0x7A5AA0),
                "DOÑA CLEMENCIA: El sistema no tiene casilla para eso.");
        c.vecino("lorena", "Lorena", "la señora", 1350, 900, new Color(0x3E7F2F),
                "LORENA: Solo venimos a registrar a nuestra hija.");
        c.vecino("ximena", "Ximena", "la señora", 1550, 950, new Color(0x5BA843),
                "XIMENA: Está en la ley.");
        c.vecino("quiroga", "Quiroga", "el del despacho", 1790, 700, new Color(0x2B2350),
                "QUIROGA: Si quiere leer la circular, léala completa.");
        l.salida(800, 990, 500, 50, MALECON, "Salir hacia el parque y malecón");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: Edificio de dos pisos con una escalera de mármol, una fila en el primer piso, la ventanilla de Marisol y el despacho de Quiroga. Un cartel dice «Trámites en línea» y, en letra pequeña, «Atención presencial martes y jueves».",
                        "DON TEÓFILO: Yo no sé usar eso. Me dijeron que lo hiciera por la aplicación. Mi nieta me ayuda pero hoy no está.",
                        "DON RAMIRO: Yo llevo veinte años aquí y no sé si me quedo.",
                        "KEVIN: Una candidatura dice números claros y me asusta. Otra dice continuidad y me asusta más.",
                        "BRAYAN: La Alcaldía debería tener un intérprete de señas. Mi hermana Lucía es sorda y nunca ha podido hacer un trámite sola.",
                        "Ringo: En el primer piso todos los celulares suenan a la vez: un audio sin nombre, reenviado por alguien que no lo escuchó entero.",
                        "AUDIO: «Nos van a revisar los celulares a todos, y al que dé me gusta a un candidato lo botan.»",
                        p.bandera("rumor_alcaldia_adelantado")
                                ? "Ringo: Ya lo reenviaron noventa veces. Y el rumor llegó desde anoche: alguien lo contó en el apagón."
                                : "Ringo: Ya lo reenviaron noventa veces.",
                        "MARISOL: (en voz baja, solo para ti) Esto me tiene muerta. Mi contrato vence en tres semanas y nadie me ha dicho si me renuevan.",
                        "MAURICIO: (entrando al patio, con la camisa amarilla) Yo llevé esa circular a todos los pisos. Nadie me preguntó qué decía. Hoy me preguntan los de todos los pisos.",
                        "Ringo: Marisol parece a punto de llorar."))
                .paso(sig -> DecisionGrupal.respeto(l, "Marisol parece a punto de llorar.",
                        new String[]{"Decirle que su trabajo importa.", "Preguntar por la circular y seguir.",
                            "Decirle que mejor no hable de eso aquí."},
                        (j, op) -> {
                            if (op == 0) {
                                GuionDia1.respetoA(l, j, 2);
                                l.dialogo(null, GuionDia1.comoJugador(j,
                                        "JUGADOR: Tu trabajo importa y no debería depender de a quién le des un me gusta.",
                                        "Ringo: Marisol respira hondo y asiente.",
                                        "MARISOL: Gracias. Hacía días que nadie me decía eso."));
                            } else if (op == 1) {
                                l.dialogo(sig, GuionDia1.comoJugador(j, "JUGADOR: ¿Y la circular qué dice?",
                                        "Ringo: Marisol se limpia los ojos y responde por trámite."));
                                l.completar("marisol");
                            } else {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -3, Partida.ANIMO, -1), "Respeto: que no hable");
                                l.dialogo(sig, GuionDia1.comoJugador(j, "JUGADOR: Mejor no hables de eso aquí.",
                                        "Ringo: Marisol se queda callada, vuelve a su ventanilla y no levanta la vista."));
                                l.completar("marisol");
                            }
                        },
                        sumados -> {
                            GuionDia1.yoTambien(l, sumados);
                            l.completar("marisol");
                            sig.run();
                        }))
                .paso(sig -> l.dialogo(sig,
                        "Ringo: Más adelante en la fila, Lorena y Ximena discuten con otra funcionaria.",
                        "DOÑA CLEMENCIA: Es que el formulario pide nombre del padre y de la madre. No puedo dejar uno en blanco.",
                        "XIMENA: Es que no tiene padre. Tiene dos madres. Está en la ley.",
                        "DOÑA CLEMENCIA: (incómoda) Es que el sistema no tiene casilla…",
                        "Ringo: Marisol se asoma desde su ventanilla.",
                        "MARISOL: Escriba acudiente en las dos casillas. Yo después arreglo el sistema.",
                        "Ringo: Lorena respira. Ximena mira a Marisol y asiente."))
                .paso(sig -> DecisionGrupal.respeto(l, "A Lorena y Ximena les trancan el registro de su hija porque el sistema no tiene casilla para dos madres.",
                        new String[]{"Acercarte y decirle a la funcionaria que el sistema debería recibir dos madres.",
                            "Quedarte en tu fila."},
                        (j, op) -> {
                            if (op == 0) {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, 2, EstadoJuego.PARTICIPACION, 1, Partida.ANIMO, 1), "Respeto: dos madres");
                                l.mariposa(j);
                                l.dialogo(null, GuionDia1.comoJugador(j,
                                        "JUGADOR: Disculpe, la ley permite registrar acudientes sin distinción. Es injusto trancar a la familia por un sistema viejo."));
                            } else {
                                l.dialogo(null, GuionDia1.comoJugador(j,
                                        "JUGADOR: (piensa) Mejor no me meto y espero mi turno."));
                                l.completar("madres");
                                l.toast("Revisen el celular (Q): el audio sigue corriendo", AVISO);
                            }
                        },
                        sumados -> {
                            GuionDia1.yoTambien(l, sumados);
                            l.completar("madres");
                            l.toast("Revisen el celular (Q): el audio sigue corriendo", AVISO);
                        }))
                .iniciar());

        l.digital("Usuarios de la Alcaldía",
                "Audio de una voz de mujer: «Nos van a revisar los celulares a todos, y al que dé me gusta a un candidato lo botan.»",
                "Lo han reenviado noventa veces.",
                new String[]{
                    "Verificar: pregunto a un funcionario si es cierto y busco la circular.",
                    "Reportar: marco el audio como falso y aviso al administrador del grupo.",
                    "Tomarme un tinto: guardo el celular, me siento y espero con calma.",
                    "Guardar silencio: escucho el audio y no respondo.",
                    "Difundir: lo reenvío. Mejor prevenir."},
                new String[]{
                    "{rol} se puso a verificar el audio de la Alcaldía.",
                    "{rol} reportó el audio de la Alcaldía.",
                    "{rol} se tomó un tinto.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió el audio de la Alcaldía."},
                el -> {
                    registrarDigital(p, ALCALDIA, el);
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("MAURICIO: Esa pregunta es la que nadie hizo.");
                        d.add("PERIODISTA: Ahora sí tenemos por dónde empezar.");
                    }
                    if (el.containsValue(Lugar.B)) {
                        d.add("Ringo: El audio queda marcado.");
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("Ringo: Marisol lo ve en su celular.");
                        d.add("MARISOL: (en voz baja) Ahora sí me van a botar.");
                        d.add("CANDIDATO: Eso no se hace con quien trabaja ahí.");
                    }
                    d.add("Ringo: Mauricio le entrega a {INFLUENCER} un tablero con los pisos de la Alcaldía.");
                    d.add("MAURICIO: Cada copia tapa una oficina y las que le quedan al lado. El audio va saltando de piso en piso. Usted decide dónde poner las copias.");
                    boolean pista = el.containsValue(Lugar.A);
                    boolean solos = p.bandera("entrada_alcaldia_C");
                    l.dialogo(() -> l.minijuegoFijo(Rol.INFLUENCER, new MjRecorridoCircular(pista).sinConsejos(solos).dialogoFinal(nv -> {
                        String[] r = nv == Minijuego.BIEN
                                ? new String[]{"INFLUENCER: La circular corta el rumor donde se cruzan los pasillos.",
                                    "MAURICIO: Eso es leerla antes de repartirla."}
                                : nv == Minijuego.REGULAR
                                        ? new String[]{"MAURICIO: Casi. Faltó cubrir el segundo piso."}
                                        : new String[]{"MAURICIO: Ya se llenó el edificio. Se lo explico yo."};
            return r;
        }), res -> {
                            l.completar("minijuego");
                            c.activarPruebas();
                    }), d.toArray(new String[0]));
                });
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "mauricio"));

        c.prueba("mauricio", Rol.CIUDADANO, "carton_mauricio", "Cartón de Mauricio",
                "CIUDADANO: ¿Qué entendiste de la circular?",
                "Ringo: La fila responde con tres versiones distintas.",
                "Ringo: Mauricio le entrega un cartón pintado con brocha.");
        c.prueba("quiroga", Rol.PERIODISTA, "circular_marcada", "Circular marcada",
                "PERIODISTA: ¿Puedo leer la circular completa?",
                "QUIROGA: Léala. Es de tres párrafos.",
                "Ringo: {PERIODISTA} marca la línea sobre recursos públicos.");
        c.prueba("mauricio", Rol.INFLUENCER, "pintura_brocha", "Pintura de brocha",
                "INFLUENCER: Va el mensaje, en grande.",
                "Ringo: Pinta con letras grandes y goteos de brocha: «La circular no habla de celulares.»");
        c.prueba("quiroga", Rol.CANDIDATO, "firma_alcaldia", "Firma de Candidato",
                "CANDIDATO: Si piden explicaciones, doy la cara.",
                "QUIROGA: Eso es lo que yo quería oír.");

        l.alCompletar(() -> {
            l.fondo(Rutas.ALCALDIA_DESPACHO);
            List<String> d = new ArrayList<>();
            d.add("QUIROGA: (saliendo de su despacho) La circular dice que no se pueden usar los recursos públicos en campaña. Nada más. No dice nada de celulares.");
            d.add("MARISOL: Y cada uno la leyó como le dio miedo.");
            d.add("QUIROGA: Sobre lo que se dijo anoche: no hay despidos. Hay una ley de garantías: durante campaña no se contrata ni se renueva. Son cuarenta y seis contratos que vencen en tres semanas, y se decide después de las elecciones.");
            d.add("MARISOL: O sea que sigo esperando. Pero ya sé de qué es la espera.");
            if (p.bandera("apagon_pregunto_mauricio")) {
                d.add("MAURICIO: Ya me habían dicho. Ustedes sí preguntaron.");
            }
            d.add("DON TEÓFILO: Bien, bien. Hoy sí entendí algo.");
            d.add("DOÑA CLEMENCIA: Voy a pedir que el formulario tenga una casilla para acudientes.");
            l.dialogo(() -> l.toast("Sigan al parque y malecón (izquierda)", AVISO), d.toArray(new String[0]));
        });
        return l;
    }

    // =====================================================================================
    // 2.4 PARQUE Y MALECON (12:00)
    // =====================================================================================

    private static Lugar malecon(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, MALECON, "Parque y malecón · Día 2", Reloj.h(12, 0));
        GuionDia1.Ctx c = new GuionDia1.Ctx(l);
        l.fondo(Rutas.MALECON_DIA2);
        l.caminable(60, 450, 1800, 380);
        l.obstaculo(1240, 700, 260, 80, "Banca");
        l.obstaculo(700, 440, 470, 60, "Banca del árbol");  l.obstaculo(60, 560, 280, 250, "Carrito de arepas");
        l.equipoEn(720, 760);

        boolean danielaNoVino = p.alguien("se_rio_ante_la_burla");

        l.rumor("La Alcaldía instaló cámaras con reconocimiento facial en el malecón para identificar a los vendedores sin permiso.", 0);
        l.letrero("ESTO ES UNA CÁMARA. VE, NO RECONOCE. Y NADIE HA DICHO QUIÉN MIRA.");
        l.niebla("La bruma se enreda en el poste de la cámara. Nadie recuerda para qué la pusieron ni quién la mira.",
                "CHELA: Esas cámaras llevan años ahí. Nadie dijo nunca para qué.");

        l.objetivo("respeto", "Lo que dice Doña Beatriz");
        l.objetivo("digital", "La foto del jugo (Q)");
        l.objetivo("minijuego", "Quién mira");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("samuel", "Samuel", "el niño", 560, 520, new Color(0x6FA8DC),
                "SAMUEL: ¿Puedo ver cómo lo hacen?");
        c.vecino("chela", "Chela", "la pescadora", 1380, 640, new Color(0x3F8EDC),
                "CHELA: Esas cámaras llevan años ahí. Nadie dijo nunca para qué.");
        c.vecino("emiro", "Emiro", "el de los jugos", 1290, 520, new Color(0xE0A030),
                "EMIRO: Segura o no, yo ya me veo en el noticiero.");
        c.vecino("orlando", "Orlando", "el técnico", 1680, 620, new Color(0x2E4A7A),
                "ORLANDO: Yo solo reviso que prendan. De lo demás no me dicen nada.");
        if (!danielaNoVino) {
            c.vecino("daniela", "Daniela", "la de las arepas", 430, 640, new Color(0x3F8EDC),
                    "DANIELA: A mí lo que me da miedo no es que me vean. Es que no sé quién ve.");
        }
        c.vecino("beatriz", "Doña Beatriz", "la señora", 1080, 700, new Color(0xD9A5EC),
                "DOÑA BEATRIZ: ¿A ustedes no les da más seguridad que haya cámaras?");
        c.vecino("julian", "Julián", "el muchacho", 1000, 540, new Color(0x7A6A5A),
                "JULIÁN: Yo solo subí una foto.");
        l.salida(1600, 450, 220, 110, JUNTA, "Subir a la Junta");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> {
                    List<String> d = new ArrayList<>();
                    d.add("Ringo: Los cuatro cruzan la plaza.");
                    d.add("INFLUENCER: ¿Ya vieron que Samuel nos sigue de lejos?");
                    d.add("CIUDADANO: Que venga. Que vea cómo se hace.");
                    d.add("Ringo: Samuel se les une con una sonrisa enorme.");
                    d.add("Ringo: Mediodía. Chela está en el muro. Emiro acomoda su carrito de jugos. En un poste, Orlando, el técnico, revisa una cámara blanca. Hay otra, más lejos, junto al muro del conjunto.");
                    if (danielaNoVino) {
                        d.add("EMIRO: Daniela hoy no vino.");
                    } else {
                        d.add("Ringo: Daniela llega empujando su carrito azul y se queda mirando la cámara del poste.");
                    }
                    d.add("Ringo: En la pantalla del celular: «La Alcaldía instaló cámaras con reconocimiento facial en el malecón para identificar a los vendedores sin permiso.»");
                    d.add("CHELA: Esas cámaras llevan años ahí. Nadie dijo nunca para qué.");
                    d.add("DOÑA BEATRIZ: ¿A ustedes no les da más seguridad que haya cámaras?");
                    d.add("EMIRO: Segura o no, yo ya me veo en el noticiero.");
                    if (!danielaNoVino) {
                        d.add("DANIELA: A mí lo que me da miedo no es que me vean. Es que no sé quién ve.");
                    }
                    d.add("ORLANDO: Yo solo reviso que prendan. De lo demás no me dicen nada.");
                    l.dialogo(sig, d.toArray(new String[0]));
                })
                .paso(sig -> DecisionGrupal.respeto(l, "Doña Beatriz pregunta si las cámaras no dan más seguridad.",
                        new String[]{"Responder con calma.", "Quedarte en silencio.",
                            "Decir: «Hay que dejar las cámaras, así uno sabe quién anda por aquí.»"},
                        (j, op) -> {
                            if (op == 0) {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, 2, EstadoJuego.PARTICIPACION, 1, Partida.ANIMO, 1), "Respeto: con calma");
                                l.mariposa(j);
                                List<String> d = new ArrayList<>();
                                d.add("JUGADOR: Las cámaras no son malas, pero si no sabemos quién las ve ni cuánto guardan, tampoco cuidan a nadie.");
                                if (danielaNoVino) {
                                    d.add("Ringo: Doña Beatriz se queda pensando. Emiro asiente despacio.");
                                } else {
                                    d.add("Ringo: Doña Beatriz se queda pensando. Daniela levanta la vista.");
                                    d.add("DANIELA: Eso era lo que quería decir y no encontraba cómo.");
                                }
                                l.dialogo(null, GuionDia1.comoJugador(j, d.toArray(new String[0])));
                            } else if (op == 1) {
                                l.dialogo(sig, danielaNoVino
                                        ? "Ringo: Emiro mira la cámara. Doña Beatriz asiente como si hubiera ganado."
                                        : "Ringo: Daniela mira la cámara. Doña Beatriz asiente como si hubiera ganado.");
                                l.completar("respeto");
                            } else {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -2, Partida.ANIMO, -1), "Respeto: dejar las cámaras");
                                l.dialogo(sig, GuionDia1.comoJugador(j,
                                        "JUGADOR: Hay que dejar las cámaras. Así uno sabe quién anda por aquí.",
                                        danielaNoVino ? "Ringo: Emiro se aleja." : "Ringo: Daniela se queda quieta. Emiro se aleja."));
                                l.completar("respeto");
                            }
                        },
                        sumados -> {
                            GuionDia1.yoTambien(l, sumados);
                            l.completar("respeto");
                            sig.run();
                        }))
                .hacer(() -> l.toast("Revisen el celular (Q): hay una foto de ustedes", AVISO))
                .iniciar());

        l.digital("Grupo del conjunto",
                "Foto de su grupo tomando jugo con Emiro: «Otros que apoyan a los informales.»",
                "Ya tiene comentarios.",
                new String[]{
                    "Verificar: pregunto quién la subió y le pido que la baje o que me pregunte antes.",
                    "Reportar: reporto la foto por mostrar personas sin permiso.",
                    "Tomarme un jugo de Emiro en el muro y dejar de leer los comentarios.",
                    "Guardar silencio: sigo caminando.",
                    "Difundir: le doy me gusta y comento que es un chiste."},
                new String[]{
                    "{rol} se puso a verificar quién subió la foto.",
                    "{rol} reportó la foto del jugo.",
                    "{rol} se tomó un jugo en el muro.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió la foto del jugo."},
                el -> {
                    registrarDigital(p, MALECON, el);
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("Ringo: Se descubre que la subió Julián.");
                        d.add("JULIÁN: Era un chiste… Bueno, la bajo.");
                    }
                    if (el.containsValue(Lugar.B)) {
                        d.add("Ringo: La foto queda marcada.");
                    }
                    if (el.containsValue(Lugar.C)) {
                        d.add("EMIRO: Este va por la casa.");
                    }
                    for (Map.Entry<Rol, Integer> e : el.entrySet()) {
                        if (e.getValue() == Lugar.E) {
                            // Animo -2 en total: la foto muestra a alguien de la casa sin permiso
                            Jugador j = p.jugador(e.getKey());
                            l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Difundió la foto de la casa");
                            j.poner("grabo_casa_sin_permiso");     // puede activar la Pelea 5
                        }
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("EMIRO: (serio) Esa soy yo en la foto, y no me preguntaron.");
                    }
                    d.add("Ringo: Orlando le entrega a {CANDIDATO} un tablero con piezas.");
                    d.add("ORLANDO: Arme el camino de la grabación. Hay dos cámaras. Cada una tiene su recorrido.");
                    l.dialogo(() -> l.minijuegoFijo(Rol.CANDIDATO, new MjQuienMira(el.containsValue(Lugar.A)).dialogoFinal(nv -> {
                        String[] r = nv == Minijuego.BIEN
                                ? new String[]{"CANDIDATO: La cámara graba, el disco guarda treinta días. Y aquí falta una flecha: nadie dice quién pide ver ni quién ve.",
                                    "ORLANDO: Esa es la que nadie ha dibujado."}
                                : nv == Minijuego.REGULAR
                                        ? new String[]{"ORLANDO: Uno bien. Revise el de la otra cámara."}
                                        : new String[]{"ORLANDO: Se lo cuento yo."};
            return r;
        }), res -> {
                            l.completar("minijuego");
                            c.activarPruebas();
                    }), d.toArray(new String[0]));
                });
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "julian"));

        if (danielaNoVino) {
            c.prueba("emiro", Rol.CIUDADANO, "tabla_chela_d2", "Tabla de Chela",
                    "CIUDADANO: Emiro, ¿cómo se siente con la cámara?",
                    "EMIRO: De día, tranquila. De noche, preocupada. Lo que quiero es saber quién mira.",
                    "Ringo: Chela le entrega otra tabla.");
        } else {
            c.prueba("daniela", Rol.CIUDADANO, "tabla_chela_d2", "Tabla de Chela",
                    "CIUDADANO: Daniela, ¿cómo te sientes con la cámara?",
                    "DANIELA: Tranquila de día, preocupada de noche. Quiero saber quién mira.",
                    "Ringo: Chela le entrega otra tabla.");
        }
        c.prueba("orlando", Rol.PERIODISTA, "explicacion_orlando", "Explicación de Orlando",
                "PERIODISTA: ¿Me puede explicar qué hacen las cámaras?",
                "ORLANDO: Graban treinta días y no reconocen caras.");
        c.prueba("chela", Rol.INFLUENCER, "pintura_botes_d2", "Pintura de botes",
                "INFLUENCER: Va el mensaje.",
                "Ringo: Pinta con la pintura de los botes: «La cámara ve, pero no reconoce.»");
        c.prueba("orlando", Rol.CANDIDATO, "firma_malecon_d2", "Firma de Candidato",
                "CANDIDATO: Me comprometo a pedir que digan quién ve las grabaciones.",
                "ORLANDO: Ojalá lo cumpla. Nadie me lo ha preguntado.");

        l.alCompletar(() -> {
            List<String> d = new ArrayList<>();
            d.add("ORLANDO: Disculpen que me meta. Esas dos cámaras las instalaron hace años. Graban y guardan treinta días. No reconocen caras: eso lo revisé yo con mis manos.");
            d.add("ORLANDO: Lo que no sé es quién ve las grabaciones ni quién le pidió a quién que las viera. Eso no lo dice ningún papel.");
            d.add(danielaNoVino ? "EMIRO: Entonces la pregunta no es si graban. Es quién mira."
                    : "DANIELA: Entonces la pregunta no es si graban. Es quién mira.");
            d.add("CHELA: Y cuántos días.");
            l.dialogo(() -> votoQuienMira(l, danielaNoVino), d.toArray(new String[0]));
        });
        return l;
    }

    private static void votoQuienMira(Lugar l, boolean danielaNoVino) {
        Partida p = l.partida();
        DecisionGrupal.votacion(l, "¿Qué hacemos con la pregunta «quién mira»?",
                new String[]{"Pedirlo en voz alta, con Orlando y Chela presentes (prudente).",
                    "Preguntarlo primero a la Alcaldía mañana.",
                    "Dejarlo así."}, 0,
                g -> {
                    Runnable fin = () -> l.toast("Suban a la Junta (derecha)", AVISO);
                    if (g == 0) {
                        l.efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -5), "Votación: quién mira");
                        for (Jugador j : p.jugadores()) {
                            l.efecto(j, Efecto.de(EstadoJuego.PARTICIPACION, 1), "Votación: pedirlo en voz alta");
                        }
                        l.dialogo(fin, "PERIODISTA: Que conste: queremos saber quién ve las grabaciones.",
                                "ORLANDO: (anota en su libreta) Se lo llevo a la Alcaldía.");
                    } else if (g == 1) {
                        l.dialogo(fin, "CANDIDATO: Mejor por el camino correcto. Mañana llevo la pregunta.");
                    } else {
                        l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Votación: dejarlo así");
                        l.dialogo(fin, "CIUDADANO: Ya sabemos lo que graban. Con eso alcanza por hoy.",
                                danielaNoVino ? "EMIRO: (bajito) Entonces seguimos sin saber." : "DANIELA: (bajito) Entonces seguimos sin saber.");
                    }
                });
    }

    // =====================================================================================
    // 2.5 JUNTA DE ACCION COMUNAL (15:00)
    // =====================================================================================

    private static Lugar junta(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, JUNTA, "Junta de Acción Comunal · Día 2", Reloj.h(15, 0));
        GuionDia1.Ctx c = new GuionDia1.Ctx(l);
        l.fondo(Rutas.JUNTA_SALON);
        l.caminable(60, 640, 1800, 400);
        l.obstaculo(680, 700, 470, 130, "Mesa larga");
        l.obstaculo(60, 680, 220, 160, "Estante");  l.obstaculo(1600, 660, 200, 100, "Cómoda");
        l.equipoEn(560, 980);

        l.rumor("La Junta vendió la lista de teléfonos de los vecinos a una campaña.", 0);
        l.letrero("ESTO ES UNA LISTA. NADIE LA VENDIÓ. NADIE PREGUNTÓ.");
        l.niebla("La niebla se mete por la escalera angosta. Nadie recuerda quién anotó cada número.",
                "DOÑA PETRA: Una lista sin dueño es un libro sin autor: cualquiera lo lee y nadie responde.");

        l.objetivo("respeto", "Don Alirio en la entrada");
        l.objetivo("digital", "El grupo Vecinos Unidos (Q)");
        l.objetivo("minijuego", "La lista con permiso");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("amaranta", "Amaranta", "la secretaria", 1380, 700, new Color(0x2B2350),
                "AMARANTA: (sin levantar la vista) La lista tiene una fecha. Lo que no tiene es una línea que diga quién dio permiso.");
        c.vecino("petra", "Doña Petra", "la presidenta", 920, 900, new Color(0xC9553A),
                "DOÑA PETRA: Una lista sin dueño es un libro sin autor.");
        c.vecino("alirio", "Don Alirio", "el señor", 380, 700, new Color(0x6A5ACD),
                "DON ALIRIO: Otra reunión arriba. ¿Y yo?");
        c.vecino("gustavo", "Don Gustavo", "el señor", 520, 760, new Color(0x5A7ACD),
                "DON GUSTAVO: Con un permiso, hasta la lista más larga se vuelve sencilla.");
        c.vecino("wilson", "Wilson", "el muchacho", 1220, 760, new Color(0x4C8BD9),
                "WILSON: Yo anoté mi número porque me lo pidieron. Nadie me dijo para qué.");
        c.vecino("sofia", "Santa Sofía de la Piedad", "la señora", 1500, 860, new Color(0xA0522D),
                "SANTA SOFÍA: Y a mí me anotaron sin preguntarme.");
        l.salida(1780, 700, 140, 200, TARDE, "Volver a la casa");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: Salón del segundo piso. Se oye el reloj de la Junta, que adelanta siete minutos.",
                        "Ringo: En el grupo «Vecinos Unidos» alguien escribió que la Junta vendió la lista de teléfonos de los vecinos a una campaña.",
                        "AMARANTA: (sin levantar la vista) La lista tiene una fecha: la del día en que se armó el grupo. Lo que no tiene es una línea que diga quién dio permiso.",
                        "DOÑA PETRA: (sosteniendo la carpeta con fuerza) Esto ya lo viví, aunque no sabría decir cuándo. Una lista sin dueño es un libro sin autor: cualquiera lo lee y nadie responde.",
                        "DON ALIRIO: (en la entrada, mirando la escalera) Otra reunión arriba. ¿Y yo?",
                        "CANDIDATO: Yo tenía una lista así. ¿Es mala?",
                        "PERIODISTA: Depende de quién dio permiso.",
                        "CANDIDATO: …Ahora que lo dices.",
                        "WILSON: Yo anoté mi número porque me lo pidieron. Nadie me dijo para qué.",
                        "SANTA SOFÍA: Y a mí me anotaron sin preguntarme.",
                        "DON GUSTAVO: Con un permiso, hasta la lista más larga se vuelve sencilla."))
                .paso(sig -> DecisionGrupal.respeto(l, "Don Alirio se queda en la entrada: la reunión es en el segundo piso y no hay rampa.",
                        new String[]{"Proponer bajar la reunión a la cocina del primer piso para que Don Alirio participe.",
                            "Seguir con la reunión arriba."},
                        (j, op) -> {
                            if (op == 0) {
                                GuionDia1.respetoA(l, j, 2);
                                l.fondo(Rutas.JUNTA_COCINA);     // bajan a la cocina del primer piso
                                p.poner("junta_en_la_cocina");
                                l.dialogo(null, GuionDia1.comoJugador(j,
                                        "JUGADOR: ¿Y si bajamos la reunión a la cocina? Así Don Alirio participa sin esperar a que lo suban.",
                                        "Ringo: Don Alirio levanta la cabeza. Don Gustavo le aprieta el hombro.",
                                        "DON ALIRIO: Dos años pidiendo una rampa y lo resuelven en un minuto.",
                                        "DOÑA PETRA: Que baje todo el mundo. Sofía, ¿hay tinto para ocho?"));
                            } else {
                                l.dialogo(sig, "Ringo: Don Alirio se queda en la entrada. Don Gustavo sube a representarlo.");
                                l.completar("respeto");
                            }
                        },
                        sumados -> {
                            GuionDia1.yoTambien(l, sumados);
                            l.completar("respeto");
                            sig.run();
                        }))
                .hacer(() -> l.toast("Revisen el celular (Q): los agregaron a un grupo", AVISO))
                .iniciar());

        l.digital("Vecinos Unidos (800 personas)",
                "«Para organizar: escriba cada quien su número de casa y su barrio.»",
                "Te añadieron a «Vecinos Unidos» con ochocientas personas, sin preguntarte.",
                new String[]{
                    "Verificar: pregunto quién creó el grupo y para qué sirve, y pido salir si no hubo permiso.",
                    "Reportar: aviso a la administradora de la Junta y a la plataforma.",
                    "Tomarme un tinto: silencio el grupo y me tomo un tinto.",
                    "Guardar silencio: lo dejo y no respondo.",
                    "Difundir: respondo con mi dirección. Ya me añadieron, qué más da."},
                new String[]{
                    "{rol} se puso a verificar el grupo Vecinos Unidos.",
                    "{rol} reportó el grupo Vecinos Unidos.",
                    "{rol} se tomó un tinto.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió su dirección en Vecinos Unidos."},
                el -> {
                    registrarDigital(p, JUNTA, el);
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("DOÑA PETRA: Esa es la pregunta: ¿quién lo creó y para qué?");
                    }
                    if (el.containsValue(Lugar.B)) {
                        d.add("Ringo: El grupo queda señalado.");
                    }
                    if (el.containsValue(Lugar.C)) {
                        d.add("SANTA SOFÍA: Con calma, que lo urgente casi nunca es lo importante.");
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("AMARANTA: (sin levantar la vista) Ahora hay una dirección más que alguien sabe.");
                    }
                    d.add("Ringo: Amaranta le entrega a {CIUDADANO} la lista de afiliados.");
                    d.add("AMARANTA: Mire el nombre, mire la hoja de permisos. Si el nombre no está, no dio permiso.");
                    l.dialogo(() -> l.minijuegoFijo(Rol.CIUDADANO, new MjListaPermiso(el.containsValue(Lugar.A)).dialogoFinal(nv -> {
                        String[] r = nv == Minijuego.BIEN
                                ? new String[]{"CIUDADANO: Tres dieron permiso y cinco no. El grupo se armó antes de preguntar.",
                                    "AMARANTA: Gracias por leerlo con cuidado."}
                                : nv == Minijuego.REGULAR
                                        ? new String[]{"AMARANTA: Casi. Revise los dos últimos."}
                                        : new String[]{"AMARANTA: Se lo muestro yo."};
            return r;
        }), res -> {
                            l.completar("minijuego");
                            c.activarPruebas();
                    }), d.toArray(new String[0]));
                });
        // Difundir: el guion habla del esporadico PIXELAR (todavia no existe). Cuando exista, va aqui en vez de null.
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new, null, null);

        c.prueba("sofia", Rol.CIUDADANO, "hoja_amaranta_d2", "Hoja de Amaranta",
                "CIUDADANO: ¿Dieron permiso para que los pusieran en el grupo?",
                "SANTA SOFÍA: Nadie me preguntó.",
                "Ringo: Amaranta le entrega otra hoja de carpeta.");
        c.prueba("amaranta", Rol.PERIODISTA, "registro_grupo", "Registro del grupo",
                "PERIODISTA: ¿Puedo ver cómo se armó el grupo?",
                "AMARANTA: Mire la fecha. Es de hace dos semanas.");
        c.prueba("amaranta", Rol.INFLUENCER, "pintura_letra_firme_d2", "Pintura de letra firme",
                "INFLUENCER: Va el aviso.",
                "Ringo: Copia con letra pequeña y firme: «Tu número no se vendió. Nadie preguntó.»");
        c.prueba("petra", Rol.CANDIDATO, "firma_junta_d2", "Firma de Candidato",
                "CANDIDATO: Me comprometo a no usar listas sin permiso.",
                "DOÑA PETRA: Y a decir de dónde las saca.",
                "CANDIDATO: Y a decir de dónde las saco.");

        l.alCompletar(() -> l.dialogo(() -> votoVecinosUnidos(l),
                "AMARANTA: La lista no se vendió. Hace dos semanas armaron el grupo «Vecinos Unidos» con los teléfonos de la lista de afiliados, sin preguntarle a nadie.",
                "DON GUSTAVO: Y el silencio sobre el permiso alimentó la sospecha tanto como el rumor.",
                "DOÑA PETRA: Hoy sí quedamos claros. Hay que pedir permiso antes de agregar a alguien.",
                p.bandera("junta_en_la_cocina") ? "DON ALIRIO: Y dejar la mesa aquí abajo, en la cocina."
                        : "DON ALIRIO: Y bajar la mesa a la cocina."));
        return l;
    }

    private static void votoVecinosUnidos(Lugar l) {
        DecisionGrupal.votacion(l, "¿Qué hacemos con el grupo Vecinos Unidos?",
                new String[]{"Proponer reglas: pedir permiso antes de agregar a alguien (prudente).",
                    "Salir los cuatro del grupo.",
                    "Quedarse callados."}, 0,
                g -> {
                    Runnable fin = () -> l.toast("Vuelvan a la casa (izquierda)", AVISO);
                    if (g == 0) {
                        l.efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -5, Partida.ANIMO, 1), "Votación: reglas del grupo");
                        l.dialogo(fin, "CIUDADANO: Que quede escrito: nadie entra sin decir que sí.",
                                "DOÑA PETRA: (anota) Eso va en el acta.");
                    } else if (g == 1) {
                        l.efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -3), "Votación: salir del grupo");
                        l.dialogo(fin, "INFLUENCER: Salimos juntos y listo.",
                                "Ringo: Los vecinos que no saben salir se quedan adentro sin ayuda.");
                    } else {
                        l.dialogo(fin, "CANDIDATO: No es nuestro grupo. No nos toca.");
                    }
                });
    }

    // =====================================================================================
    // 2.6 EL AUDIO VUELVE (17:00) · en la casa
    // =====================================================================================

    private static Lugar elAudioVuelve(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, TARDE, "El audio vuelve", Reloj.h(17, 0));
        l.fondo(Rutas.CASA_TARDE);
        l.caminable(80, 620, 1760, 440);
        l.obstaculo(560, 640, 440, 160, "Mesa");  l.obstaculo(60, 760, 380, 320, "Baranda");
        l.equipoEn(900, 900);
        l.npc("rosa", "Doña Rosa", "la señora", 1380, 700, new Color(0xB5473A));
        l.npc("vlado", "Vlado (tele)", "la tele", 1740, 640, new Color(0x333344));

        l.alPrimeraEntrada(() -> {
            Runnable aLaCena = () -> l.dialogo(() -> h.ir(CENA),
                    "Ringo: Doña Rosa pone cuatro tazas sobre la mesa. Es hora de la cena.");
            Secuencia s = new Secuencia()
                    .paso(sig -> l.dialogo(sig,
                            "Ringo: Los cuatro celulares suenan a la vez. Es el audio del colegio, ahora con noventa reenvíos.",
                            "Ringo: El origen se rastrea entre los cuatro."));
            if (alguienDifundio(p)) {
                List<Jugador> fueron = quienesDifundieron(p);
                p.poner("cena_dura");
                s.paso(sig -> {
                    List<String> d = new ArrayList<>();
                    d.add("Ringo: La tele se enciende sola. Vlado aparece con papeles en la mano.");
                    d.add("VLADO: El audio que se multiplicó noventa veces salió de un celular de la casa de Doña Rosa.");
                    d.add("Ringo: Doña Rosa, en la puerta de la cocina, se queda quieta. Los cuatro se miran.");
                    d.add(fueron.get(0).rol.name() + ": " + (fueron.size() > 1 ? "Fuimos varios. Lo mandamos sin verificar." : "Fui yo. Lo mandé sin verificar."));
                    d.add("DOÑA ROSA: (se limpia las manos en el delantal) Yo no los eché a la calle cuando llegaron. No me hagan arrepentirme.");
                    if (fueron.size() > 1) {
                        d.add("Ringo: La cena de hoy va a ser más dura.");
                    }
                    l.dialogo(sig, d.toArray(new String[0]));
                }).paso(sig -> DecisionGrupal.votacion(l, "¿Lo decimos nosotros primero?",
                        new String[]{"Decirlo los cuatro juntos en la cena (prudente).",
                            "Que lo diga solo quien lo difundió.",
                            "Callar hasta que pregunten."}, 0,
                        g -> {
                            if (g == 0) {
                                l.efectoCompartido(Efecto.de(Partida.ANIMO, 1, Partida.CONFIANZA_ROLES, 1), "Votación: lo decimos juntos");
                                l.dialogo(sig, "CIUDADANO: Si esto salió de esta casa, lo contamos nosotros antes de que lo cuente la tele.");
                            } else if (g == 1) {
                                for (Jugador j : fueron) {
                                    l.efecto(j, Efecto.de(EstadoJuego.CONFIANZA, -1), "Votación: lo dice quien difundió");
                                }
                                l.dialogo(sig, "Ringo: Quien difundió respira hondo.");
                            } else {
                                l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Votación: callar");
                                l.dialogo(sig, "PERIODISTA: Si nadie pregunta, no hay nada que decir.");
                            }
                        }));
            } else {
                s.paso(sig -> {
                    l.efectoCompartido(Efecto.de(Partida.ANIMO, 1), "Nadie difundió");
                    l.dialogo(sig,
                            "VLADO: El audio sale de una cuenta que usan varias manos. No se puede saber cuáles.",
                            "PERIODISTA: Entonces no fuimos nosotros.",
                            "CIUDADANO: Pero tampoco ayudamos a frenarlo del todo.",
                            "Ringo: La cena va a ser de reflexión.");
                });
            }
            s.hacer(aLaCena).iniciar();
        });
        return l;
    }

    // =====================================================================================
    // 2.7 LA CENA Y LA REVELACION (19:00) + 2.8 LA NIEBLA DEL DIA 2
    // =====================================================================================

    private static Lugar cena(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CENA, "La cena · Día 2", Reloj.h(19, 0));
        l.fondo(Rutas.CASA_CENA_DIA2);
        l.caminable(80, 300, 1760, 740);
        l.obstaculo(470, 250, 980, 600, "Mesa");
        l.equipoEn(960, 900);
        l.npc("rosa", "Doña Rosa", "la señora", 1600, 620, new Color(0xB5473A));
        l.npc("samuel", "Samuel", "el niño", 330, 620, new Color(0x6FA8DC));
        l.npc("ursula", "Doña Úrsula", "la anciana", 1790, 900, new Color(0x8A6A9A));
        // la cena se ve como cinematica: los cuatro sentados al fondo de la mesa, nadie camina
        l.modoCinematica(new int[][]{{600, 400}, {810, 375}, {1110, 375}, {1320, 400}});

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: La casa. Cuatro tazas de tinto. Samuel en su silla. Cada jugador pone sus pruebas sobre la mesa.",
                        p.bandera("cena_dura") ? "Ringo: Nadie se atreve a servir primero." : "Ringo: Huele a pan y a tinto recién hecho.",
                        "DOÑA ROSA: Hoy no pregunto qué hicieron. Pregunto qué piensan hacer mañana."))
                .paso(sig -> GuionDia1.armarLetreros(l, sig))
                .paso(sig -> {
                    p.confianza.desbloquearTodo();   // los bloqueos de ayer ya pasaron
                    GestorPeleas.Pelea pe = GestorPeleas.elegir(p);
                    if (pe != null) {
                        GestorPeleas.jugar(l, pe, sig);
                        return;
                    }
                    Jugador[] buena = GestorPeleas.peleaBuena(p, java.util.Arrays.asList(LUGARES_FINAL));
                    if (buena != null) {
                        GestorPeleas.jugarPeleaBuena(l, buena[0], buena[1], sig);
                    } else {
                        GestorPeleas.sinPelea(l, sig);
                    }
                })
                .paso(sig -> {
                    boolean compartieron = true;
                    for (Letrero le : p.letreros()) {
                        compartieron &= le.piezas() == 4;
                    }
                    l.npc("melquiades", "Melquíades", "el gitano", 1790, 600, new Color(0x7A3A8A));
                    l.dialogo(sig,
                            "Ringo: Entra Melquíades. Deja el papel junto al tinto.",
                            compartieron ? "Ringo: Compartieron las pruebas: el papel está escrito, pero roto en cinco pedazos."
                                    : "Ringo: El papel está casi en blanco: solo se ven cinco pedazos sueltos.",
                            "Ringo: Esta noche lo arma {CIUDADANO}.");
                })
                .paso(sig -> l.minijuegoFijo(Rol.CIUDADANO, new MjPergaminos(2).dialogoFinal(nv -> nv == Minijuego.BIEN
                        ? new String[]{"MELQUÍADES: Eso es lo que yo escribí. Y lo que ustedes acaban de leer.",
                            "Ringo: «Dirá la isla que lo vio todo y no habrá mirado a nadie. Guardará cada número en una lista sin dueño, y aun así olvidará el rostro de quien le abrió la puerta. Quien hoy pida permiso antes de mirar, mañana no tendrá que pedir que lo dejen en paz.»"}
                        : nv == Minijuego.REGULAR
                                ? new String[]{"MELQUÍADES: Falta una frase."}
                                : new String[]{"Ringo: Melquíades termina de leerlo él. El pergamino queda incompleto."}), res -> sig.run()))
                .paso(sig -> l.dialogo(sig,
                        "Ringo: Melquíades se queda de pie. Es la primera vez que habla de más.",
                        "MELQUÍADES: Anoche revisé mi aparato. El rumor del colegio no nació de una persona. Nació de un calendario.",
                        "MELQUÍADES: Alguien escribió horas cada siete minutos: quién habla, qué dice, qué se repite. Hay un turno de las siete y siete, y hay una cuenta que usan varias manos.",
                        "Ringo: Silencio.",
                        "DOÑA ÚRSULA: Esto ya lo viví. Y lo que no se pone en nombre se vuelve de nadie.",
                        "MELQUÍADES: Nadie lo escribió. Y todos lo repartieron. Eso es Macondo despierta.",
                        "PERIODISTA: Entonces no hay un culpable.",
                        "CIUDADANO: Hay cuatro de nosotros que podemos no repartirlo.",
                        "SAMUEL: ¿Entonces el audio no era de nadie?",
                        "DOÑA ROSA: Era de quien lo repartió, mijo.",
                        "Ringo: La bruma vuelve a bajar por el río. Esta vez es más espesa."))
                .paso(sig -> decisionFinal(l, sig))
                .paso(sig -> laNiebla(l, sig))
                .hacer(() -> h.terminarDia())
                .iniciar());
        return l;
    }

    /** Decision final de los cuatro (votacion; si hay empate, Dona Rosa elige B). */
    private static void decisionFinal(Lugar l, Runnable fin) {
        Partida p = l.partida();
        DecisionGrupal.votacion(l, "Decisión final: ¿qué hacemos con lo que dijo Melquíades?",
                new String[]{"Contarlo en la plaza.", "Seguir verificando sin contarlo todavía.", "Dejarlo pasar."}, 1,
                g -> {
                    p.poner("final_" + (char) ('A' + g));
                    if (g == 0) {
                        l.efectoCompartido(Efecto.de(Partida.ANIMO, 2), "Final: contarlo en la plaza");
                        for (Jugador j : p.jugadores()) {
                            l.mariposa(j);
                        }
                        l.dialogo(fin, "CANDIDATO: Convoco a la plaza. Les cuento lo que sabemos.",
                                "INFLUENCER: Yo pongo el megáfono.",
                                "Ringo: Se juntan Convocatoria y Megáfono. La plaza se llena.");
                    } else if (g == 1) {
                        l.dialogo(fin, "PERIODISTA: Tengo que comprobarlo con el calendario completo.");
                    } else {
                        l.efectoCompartido(Efecto.de(Partida.ANIMO, -1), "Final: dejarlo pasar");
                        l.dialogo(fin, "CIUDADANO: Ya bastante hicimos hoy.");
                    }
                });
    }

    /**
     * 2.8 La niebla del Dia 2: cada Letrero colgado baja la desinformacion de su lugar y frena la niebla.
     * Un lugar esta PROTEGIDO si su Letrero del Dia 2 tiene tres o mas piezas, o si su desinformacion
     * termino por debajo de 40. La decision final suma (A) o resta (C) un lugar protegido.
     * El resultado queda en banderas para el final: "protegido_<lugar>" y "protegidos_<n>".
     */
    private static void laNiebla(Lugar l, Runnable fin) {
        Partida p = l.partida();
        List<String> d = new ArrayList<>();
        d.add("Ringo: La campana suena una sola vez. La bruma baja por el río hacia la Panadería, la Alcaldía, el Malecón y la Junta.");
        List<String> protegidos = new ArrayList<>();
        List<String> sinProteger = new ArrayList<>();
        for (String lu : LUGARES_FINAL) {
            Letrero le = p.letrero(lu);
            if (le != null && le.sePuedeColgar()) {
                p.sumarDesinfo(lu, -le.reduccion());
            }
            boolean prot = (le != null && le.frenaNiebla()) || p.desinfo(lu) < Partida.DESINFO_INICIAL;
            (prot ? protegidos : sinProteger).add(lu);
        }
        // decision final: A protege el lugar con menos desinformacion sin cubrir; C quita uno
        if (p.bandera("final_A") && !sinProteger.isEmpty()) {
            String mejor = sinProteger.get(0);
            for (String lu : sinProteger) {
                if (p.desinfo(lu) < p.desinfo(mejor)) {
                    mejor = lu;
                }
            }
            sinProteger.remove(mejor);
            protegidos.add(mejor);
            d.add("Ringo: Lo que se contó en la plaza protege " + nombreLugar(mejor) + ".");
        } else if (p.bandera("final_C") && !protegidos.isEmpty()) {
            String peor = protegidos.get(0);
            for (String lu : protegidos) {
                if (p.desinfo(lu) > p.desinfo(peor)) {
                    peor = lu;
                }
            }
            protegidos.remove(peor);
            sinProteger.add(peor);
            d.add("Ringo: Como nadie lo contó, la niebla alcanza " + nombreLugar(peor) + ".");
        }
        for (String lu : LUGARES_FINAL) {
            if (protegidos.contains(lu)) {
                p.nieblaOff(lu);
                p.poner("protegido_" + lu);
                d.add("Ringo: " + nombreLugar(lu) + ": la niebla se detiene en la puerta. Una mariposa se posa en el letrero.");
            } else {
                p.nieblaOn(lu);
                d.add("Ringo: " + nombreLugar(lu) + ": la niebla entra. Los vecinos no encuentran el nombre de las cosas.");
            }
        }
        p.poner("protegidos_" + protegidos.size());
        d.add("Ringo: Lugares protegidos: " + protegidos.size() + " de 4. Ánimo de la casa: " + p.animo + ".");
        l.dialogo(fin, d.toArray(new String[0]));
    }

    static String nombreLugar(String id) {
        switch (id) {
            case PANADERIA: return "la Panadería";
            case ALCALDIA: return "la Alcaldía";
            case MALECON: return "el Malecón";
            case JUNTA: return "la Junta";
            default: return id.replace("_", " ");
        }
    }

    // =====================================================================================
    // LOS FINALES
    // =====================================================================================

    /** Lugares protegidos al terminar el Dia 2 (lo deja laNiebla). */
    public static int protegidos(Partida p) {
        for (int n = 4; n >= 0; n--) {
            if (p.bandera("protegidos_" + n)) {
                return n;
            }
        }
        return 0;
    }

    /** 'A', 'B' o 'C' (guion "Como se decide el final"). */
    public static char cualFinal(Partida p) {
        int n = protegidos(p);
        if (n >= 3 && p.animo >= 5) {
            return 'A';
        }
        if (n <= 1) {
            return 'C';
        }
        return 'B';
    }

    // Fondos que YA estan en el proyecto: el final no necesita imagenes nuevas.
    private static final String F_MALECON = "/imagenes/malecon.png";
    private static final String F_CENA = "/imagenes/casa_cena.png";
    private static final String F_CASA = "/imagenes/casapuertasin.png";
    private static final String F_LEJOS = "/imagenes/fondoinicio.png";

    /** Los cuatro de pie (sprite "quieto"), en fila. */
    static void losCuatro(Cinematica c, int x0, int y, int paso) {
        String[] roles = {"ciudadano", "influencer", "periodista", "candidato"};
        for (int i = 0; i < 4; i++) {
            c.sprite("/imagenes/personajes/" + roles[i] + "/quieto.png", x0 + i * paso, y + (i % 2) * 18, 205, 250);
        }
    }

    /** En la cena (vista desde arriba): dos a cada lado de la mesa, por encima del cuadro de dialogo. */
    static void alrededorDeLaMesa(Cinematica c) {
        String[] roles = {"ciudadano", "influencer", "periodista", "candidato"};
        int[][] pos = {{200, 290}, {400, 320}, {1320, 290}, {1520, 320}};
        for (int i = 0; i < 4; i++) {
            c.sprite("/imagenes/personajes/" + roles[i] + "/quieto.png", pos[i][0], pos[i][1], 205, 250);
        }
    }

    /** Un NPC con su sprite estatico (/imagenes/personaje/<Nombre>/<Nombre>.png). */
    static void npcFinal(Cinematica c, String id, int x, int y, int ancho, int alto) {
        c.sprite(Rutas.spriteVecino(id), x, y, ancho, alto);
    }

    /** Cinematica final: Final A, B o C, las mariposas y la pantalla final con el puntaje. */
    public static Cinematica finalDelJuego(Partida p) {
        char f = cualFinal(p);
        int n = protegidos(p);
        Cinematica cine = new Cinematica(p).cine();
        if (f == 'A') {
            cine.cuadro(F_MALECON,
                    "Amanece. En cada lugar hay un Letrero colgado y una mariposa posada encima. La gente se dice los nombres en voz alta.",
                    "DOÑA ROSA: Hoy sí nos acordamos.",
                    "DOÑA ÚRSULA: Esto ya lo viví. Sin embargo, esta vez salió bien.",
                    "VLADO: (voz de la tele) Hoy Macondo votó sabiendo lo que vota. No fue fácil. Fue verdad.",
                    "SAMUEL: ¿Ya los van a dejar quedarse?")
                    .camara(1.00, 1.08, 0, 0)
                    .musica("/sonidos/musica_isla.wav", 0.6, 2.0)
                    .rayos(0xFFE0A0)
                    .polvo(0xFFE08A, 40)
                    .mariposa(700, 520, 1500, 180);
            losCuatro(cine, 380, 470, 250);
            npcFinal(cine, "rosa", 1400, 430, 230, 280);
            npcFinal(cine, "samuel", 1640, 500, 180, 220);
            cine.cuadro(F_MALECON,
                    "Los cuatro se miran. Ya no son de afuera.",
                    "CIUDADANO: Pregunté. Y me contestaron.",
                    "PERIODISTA: Comprobé. Y me creyeron.",
                    "INFLUENCER: ¿Yo? ¡Yo repartí lo que valía la pena repartir!",
                    "CANDIDATO: Prometí lo que podía cumplir. Mi palabra no fue en vano.")
                    .camara(1.08, 1.16, 0, 20)
                    .rayos(0xFFE0A0);
            losCuatro(cine, 520, 470, 240);
            cine.titulo("La isla despertó", "Lugares protegidos: " + n + " de 4 · Ánimo de la casa: " + p.animo);
        } else if (f == 'B') {
            cine.cuadro(F_MALECON,
                    "Amanece. Dos lugares claros y dos con niebla. La gente se mira con desconfianza.",
                    "DOÑA ROSA: Nos acordamos de unas cosas y de otras no. ¿Unas cosas por otras, supongo?",
                    "VLADO: Macondo votó hoy. La mitad cree una cosa y la mitad cree otra.")
                    .camara(1.00, 1.08, 0, 0)
                    .musica("/sonidos/musica_isla.wav", 0.45, 2.0)
                    .niebla()
                    .viento(0.4);
            npcFinal(cine, "rosa", 1300, 430, 230, 280);
            losCuatro(cine, 300, 470, 230);
            cine.cuadro(F_CENA,
                    "Los cuatro se sientan a la mesa. Hay sillas vacías.",
                    "SAMUEL: ¿Falta alguien?",
                    "CIUDADANO: Faltó escuchar un poco más.",
                    "PERIODISTA: Faltó comprobar un poco más.",
                    "INFLUENCER: Faltó callar a tiempo.",
                    "CANDIDATO: Faltó prometer menos.")
                    .camara(1.00, 1.06, 0, 0);
            npcFinal(cine, "samuel", 860, 40, 180, 220);
            alrededorDeLaMesa(cine);
            cine.titulo("La isla quedó dividida", "Lugares protegidos: " + n + " de 4 · Ánimo de la casa: " + p.animo);
        } else {
            cine.cuadro(F_CASA,
                    "Amanece. La bruma cubre casi todo. Los letreros están ahí, pero nadie los lee.",
                    "DOÑA ROSA: ¿Quién era usted? ¿Qué hacía aquí?",
                    "Ringo: Doña Rosa los mira como si los viera por primera vez.",
                    "SAMUEL: Yo sí me acuerdo.",
                    "Ringo: Samuel les entrega un letrero chiquito que él mismo dibujó: ESTOS SON LOS CUATRO.")
                    .camara(1.00, 1.10, 0, 0)
                    .musica("/sonidos/musica_isla.wav", 0.35, 2.0)
                    .niebla()
                    .viento(0.5);
            losCuatro(cine, 280, 520, 230);
            npcFinal(cine, "rosa", 1300, 470, 230, 280);
            npcFinal(cine, "samuel", 1560, 540, 180, 220);
            cine.titulo("La isla olvidó", "Todavía están a tiempo de recordarla. Lugares protegidos: " + n + " de 4");
        }

        // ---- mariposas al final (se suman las de los cuatro) ----
        int m = p.totalMariposas();
        String bandada = m <= 3 ? "Una mariposa solitaria cruza la plaza."
                : m <= 8 ? "Una bandada pequeña rodea la panadería."
                : "La bandada cubre la plaza entera y los vecinos levantan la cabeza.";
        cine.cuadro(F_MALECON, bandada, "RINGO: Mariposas amarillas entre los cuatro: " + m + ".")
                .camara(1.05, 1.18, 0, -40)
                .polvo(0xFFE08A, 30)
                .mariposa(300, 800, 1600, 200);
        for (int k = 1; k < Math.min(5, 1 + m / 3); k++) {
            cine.mariposa(200 + k * 260, 860 - k * 40, 1700 - k * 120, 160 + k * 50);
        }

        // ---- "Asi jugo cada quien": tarjetas con titulo, barritas y mariposas ----
        String resumen = "Ánimo de la casa " + p.animo + "/10 · Lugares protegidos " + n + "/4 · Mariposas " + m + ".";
        cine.cuadro(F_CENA, "Así jugó cada quien.",
                "RINGO: Así jugó cada quien. ¡Miren esos títulos!",
                "RINGO: " + resumen)
                .camara(1.00, 1.03, 0, 0)
                .capa(new FinalTarjetas(p));

        cine.cuadro(F_LEJOS, "La isla, de lejos.",
                "RINGO: Hay islas que olvidan. Hay islas que recuerdan. Y hay gente que ayuda a decidir cuál.")
                .camara(1.00, 1.12, 0, 0)
                .nubes()
                .mariposa(400, 700, 1500, 250)
                .musica("/sonidos/musica_titulo.wav", 0.8, 1.5)
                .titulo("FIN", "Todos hablan en Macondo · uso saludable de las redes sociales");
        return cine;
    }
}
