package juegoferia;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MODO FERIA: la partida corta (unos 12-15 minutos) para la feria.
 *
 *   Intro corta (cinematica) -> PANADERIA: investigar (3 pistas + "Rastrea el audio")
 *   -> el grupo ESCOGE el orden: MALECON (respeto con Daniela + el celular) y JUNTA (votacion
 *      con Doña Petra + "Los faroles" con Don Alirio). Dentro de cada lugar, las cosas se hacen
 *      en el orden que quieran (cada una tiene su "!").
 *   -> FINAL: la cena, como quedo la isla y las tarjetas con titulos y misiones secretas.
 *
 * Al terminar un lugar queda un TIEMPO LIBRE (40 s) para hablar con quien quieran (si hablan con
 * todos hay mariposa extra) y salir por la puerta. En red se sale cuando los cuatro llegan a ella.
 *
 * Cada lugar tiene 2:30 de juego (el contador solo corre cuando nadie esta leyendo ni decidiendo).
 * Si se acaba, el rumor se riega (+15) y el grupo pasa al siguiente lugar.
 * El rumor crece solo mientras juegan: es el "enemigo" de la partida.
 *
 * Cada jugador recibe una MISION SECRETA al empezar (solo la ve el, en red). Al final se revela.
 * Usa los mismos fondos, vecinos y minijuegos del juego completo: no necesita archivos nuevos.
 */
public final class GuionFeria {

    private GuionFeria() {
    }

    public static final String PANADERIA = GuionDia1.PANADERIA;
    public static final String MALECON = GuionDia1.MALECON;
    public static final String JUNTA = GuionDia1.JUNTA;
    public static final String PRIMER_LUGAR = PANADERIA;

    /** Segundos de juego por lugar. */
    public static final double SEGUNDOS_LUGAR = 150;
    /** Cuanto sube el rumor por segundo mientras juegan (el enemigo). */
    static final double RITMO_RUMOR = 0.14;

    public static Lugar crear(String lugar, Partida p, Lugar.Anfitrion h) {
        p.poner("feria_visto_" + lugar);
        switch (lugar) {
            case PANADERIA: return panaderia(p, h);
            case MALECON: return malecon(p, h);
            case JUNTA: return junta(p, h);
            default:
                throw new IllegalArgumentException("El modo feria no tiene el lugar " + lugar);
        }
    }

    // =====================================================================================
    // EL CAMINO: despues de la panaderia el grupo escoge a donde ir
    // =====================================================================================

    /** Los lugares que faltan por visitar (malecon y/o junta). */
    static List<String> faltan(Partida p) {
        List<String> f = new ArrayList<>();
        for (String lg : new String[]{MALECON, JUNTA}) {
            if (!p.bandera("feria_visto_" + lg)) {
                f.add(lg);
            }
        }
        return f;
    }

    /** La puerta del lugar: lleva a lo que falte (si faltan los dos, el grupo escoge) o a la cena. */
    private static void puerta(Lugar l, Partida p, Lugar.Anfitrion h, int x, int y, int w, int alto) {
        List<String> f = faltan(p);
        String etiqueta = f.isEmpty() ? "Volver a la casa (la cena)"
                : f.size() == 2 ? "Salir a la calle"
                : f.get(0).equals(MALECON) ? "Ir al malecón" : "Subir a la Junta";
        l.salidaHacia(x, y, w, alto, etiqueta, () -> seguir(l, p, h));
    }

    private static void seguir(Lugar l, Partida p, Lugar.Anfitrion h) {
        List<String> f = faltan(p);
        if (f.isEmpty()) {
            h.terminarDia();
            return;
        }
        if (f.size() == 1) {
            h.ir(f.get(0));
            return;
        }
        String[] ops = {
            "Al malecón: dicen que van a sacar a los vendedores del río.",
            "A la Junta: dicen que se robaron la plata del alcantarillado."};
        if (Sincronia.activa()) {
            DecisionGrupal.votacion(l, "¿A dónde vamos primero?", ops, 0, g -> h.ir(g == 0 ? MALECON : JUNTA));
        } else {
            l.decidir("¿A DÓNDE VAN PRIMERO?", "Hay dos rumores corriendo por la isla. Ustedes escogen el orden.", 0, null,
                    Lugar.op(ops[0], () -> h.ir(MALECON)), Lugar.op(ops[1], () -> h.ir(JUNTA)));
        }
    }

    /** Pone el "!" a un vecino para los cuatro. */
    private static void marcar(Lugar l, String npc) {
        for (Rol r : Rol.values()) {
            l.marcarPrueba(npc, r);
        }
    }

    /** Lo que pasa si se acaba el tiempo de un lugar. */
    private static void tiempoAgotado(Lugar l) {
        l.partida().sumarDesinfo(l.id, 15);
        l.toast("¡Se acabó el tiempo aquí! El rumor se regó un poco más.", new Color(0xFF8A7A));
        l.terminarYa();
    }

    // =====================================================================================
    // INTRO
    // =====================================================================================

    public static Cinematica intro(Partida p) {
        Cinematica c = new Cinematica(p);
        c.cuadro("/imagenes/casapuertasin.png",
                "Macondo, a dos días de elegir alcalde. Cuatro forasteros viven en la casa de Doña Rosa, encima de la panadería.",
                "DOÑA ROSA: Hoy van a correr rumores por toda la isla. Y aquí, cuando la gente deja de preguntar, empieza a olvidar.",
                "SAMUEL: ¿Y si olvidamos todo?",
                "DOÑA ROSA: Para eso están ellos cuatro.",
                "RINGO: Tienen un ratico en cada lugar. Si el rumor llega muy alto, la niebla se traga los nombres. ¡A moverse!")
                .musica("/sonidos/musica_isla.wav", 0.6, 1.5);
        GuionDia2.losCuatro(c, 280, 520, 240);
        GuionDia2.npcFinal(c, "rosa", 1300, 470, 230, 280);
        GuionDia2.npcFinal(c, "samuel", 1560, 540, 180, 220);
        return c.titulo("Modo Feria", "Panadería, y después ustedes escogen el camino  —  ¡que no gane el rumor!");
    }

    // =====================================================================================
    // MISIONES SECRETAS
    // =====================================================================================

    /** {id, nombre corto, lo que hay que hacer} */
    static final String[][] MISIONES = {
        {"detective", "Detective", "En el celular del malecón, escoge VERIFICAR (la primera opción)."},
        {"guardian", "Guardián del río", "Que el rumor del malecón termine por debajo de 50%."},
        {"coleccion", "Coleccionista", "Termina la partida con 3 mariposas o más."},
        {"voz", "Voz de la casa", "Que gane lo que tú votes en la Junta."},
        {"pistas", "Sabueso", "Encuentra tú mismo 2 de las pistas de la panadería."},
        {"ayuda", "Mano amiga", "Usa tu habilidad (H) en dos lugares distintos."},
        {"animo", "Corazón de la casa", "Que el ánimo de la casa termine en 7 o más."}
    };

    /** Reparte una mision distinta a cada uno (igual en las cuatro computadoras) y se la cuenta solo a el. */
    static void repartirMisiones(Lugar l, Runnable despues) {
        Partida p = l.partida();
        List<String[]> m = new ArrayList<>();
        Collections.addAll(m, MISIONES);
        Collections.shuffle(m, Sincronia.nuevoRandom());
        List<Jugador> js = p.jugadores();
        for (int i = 0; i < js.size(); i++) {
            js.get(i).misionId = m.get(i)[0];
            js.get(i).misionNombre = m.get(i)[1];
            js.get(i).misionTexto = m.get(i)[2];
        }
        if (Sincronia.activa()) {
            // cada quien ve SOLO la suya; 'despues' corre cuando los cuatro la leyeron
            int[] faltan = {js.size()};
            for (Jugador j : js) {
                l.conAudiencia(j.rol, () -> l.dialogo(() -> {
                    faltan[0]--;
                    if (faltan[0] == 0) {
                        l.conAudiencia(null, despues);
                    }
                }, "RINGO: (en secreto) Tu MISIÓN SECRETA, " + j.nombre + ": " + j.misionTexto,
                        "RINGO: No se la digas a nadie. Al final se revela quién la cumplió."));
            }
        } else {
            List<String> ls = new ArrayList<>();
            ls.add("RINGO: Misiones secretas. Cada quien lee la suya... y los demás se tapan los ojos.");
            for (Jugador j : js) {
                ls.add("RINGO: " + j.nombre + ": " + j.misionTexto);
            }
            l.dialogo(despues, ls.toArray(new String[0]));
        }
    }

    /** Revisa las misiones al final (pone la bandera "mision_ok" a quien la cumplio). */
    static void revisarMisiones(Partida p, String ganadorJunta) {
        for (Jugador j : p.jugadores()) {
            if (j.misionId == null) {
                continue;
            }
            boolean ok;
            switch (j.misionId) {
                case "detective": ok = j.tiene("feria_verifico"); break;
                case "guardian": ok = p.desinfo(MALECON) < 50; break;
                case "coleccion": ok = j.mariposas >= 3; break;
                case "voz": ok = ganadorJunta != null && j.tiene("feria_voto_" + ganadorJunta); break;
                case "pistas": ok = j.tiene("feria_pista_2"); break;
                case "ayuda": {
                    int n = 0;
                    for (String lugar : new String[]{PANADERIA, MALECON, JUNTA}) {
                        if (p.bandera("hab_" + lugar + "_" + j.rol.name())) {
                            n++;
                        }
                    }
                    ok = n >= 2;
                    break;
                }
                case "animo": ok = p.animo >= 7; break;
                default: ok = false;
            }
            if (ok) {
                j.poner("mision_ok");
            }
        }
    }

    // =====================================================================================
    // PISTAS (panaderia): vecinos con "!" que cuentan algo una sola vez
    // =====================================================================================

    private static final class Pistas {
        int encontradas;
        final int total;
        final Runnable alTerminar;

        Pistas(int total, Runnable alTerminar) {
            this.total = total;
            this.alTerminar = alTerminar;
        }
    }

    private static void pista(Lugar l, Pistas ps, String id, String nombre, String enNiebla, double x, double y,
            Color ropa, String repetir, String... lineas) {
        Actor a = l.npc(id, nombre, enNiebla, x, y, ropa);
        for (Rol r : Rol.values()) {
            l.marcarPrueba(id, r);
        }
        boolean[] dicha = {false};
        a.interactuar("Hablar con " + nombre, () -> {
            if (dicha[0] || !l.partida().bandera("feria_pistas_" + l.id)) {
                l.dialogo(null, repetir);
                return;
            }
            dicha[0] = true;
            l.quitarMarca(id);
            Jugador quien = l.partida().lider();
            l.dialogo(() -> {
                ps.encontradas++;
                if (quien != null) {
                    l.mariposa(quien);
                    int suyas = 1;
                    while (quien.tiene("feria_pista_" + suyas)) {
                        suyas++;
                    }
                    quien.poner("feria_pista_" + suyas);
                }
                l.toast("Pista " + ps.encontradas + " de " + ps.total + (quien != null ? " (la encontró " + quien.nombre + ")" : ""),
                        new Color(0xFFD35C));
                if (ps.encontradas == ps.total) {
                    l.conAudiencia(null, ps.alTerminar);
                }
            }, lineas);
        });
    }

    // =====================================================================================
    // 1. PANADERIA: INVESTIGAR
    // =====================================================================================

    private static Lugar panaderia(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, PANADERIA, "Panadería de Doña Rosa", Reloj.h(7, 10));
        l.fondo(Rutas.PANADERIA);
        l.caminable(70, 570, 1780, 470);
        l.obstaculo(980, 440, 740, 160, "Mostrador");
        l.obstaculo(100, 820, 300, 200, "Mesita");
        l.equipoEn(900, 960);
        l.rumor("Dicen que cierran el colegio en enero.", RITMO_RUMOR);
        l.niebla("Una bruma tibia entra por la ventana y se mete entre los panes.",
                "DOÑA ÚRSULA: Cuando la gente deja de preguntar, empieza a olvidar.");
        l.objetivo("pistas", "Encontrar 3 pistas (los del !)");
        l.objetivo("minijuego", "Rastrear el audio");
        l.tiempoFeria(SEGUNDOS_LUGAR, () -> tiempoAgotado(l));

        // decorado (sin charla larga)
        l.npc("ursula", "Doña Úrsula", "la anciana", 200, 670, new Color(0x8A6A9A))
                .interactuar("Hablar con Doña Úrsula", () -> l.dialogo(null, "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo."));
        l.npc("samuel", "Samuel", "el niño", 1350, 880, new Color(0x6FA8DC))
                .interactuar("Hablar con Samuel", () -> l.dialogo(null, "SAMUEL: ¿De verdad cierran mi colegio?"));
        l.npc("eliecer", "Don Eliécer", "el señor", 450, 930, new Color(0x7A6A5A))
                .interactuar("Hablar con Don Eliécer", () -> l.dialogo(null, "DON ELIÉCER: Si lo cierran, por algo será."));

        Pistas ps = new Pistas(3, () -> {
            if (l.completado()) {
                return;            // (se acabo el tiempo antes: ya no se juega el minijuego)
            }
            l.completar("pistas");
            l.dialogo(() -> l.minijuegoFijo(Rol.CIUDADANO,
                    new MjRastreaAudio(true, p.bandera("lupa_" + PANADERIA)), res -> l.completar("minijuego")),
                    "DOÑA ROSA: Ya saben de quién vino. Ahora vean cómo fue cambiando por el camino.",
                    "DOÑA ROSA: Tome el celular, {CIUDADANO}. Ahí se ven los reenvíos.");
        });
        pista(l, ps, "nubia", "Nubia", "la del audio", 620, 700, new Color(0xD98AB0),
                "NUBIA: Ya les dije todo lo que sé.",
                "NUBIA: (bajito) Me lo mandó Yolanda. Yo solo lo pasé.",
                "NUBIA: No pregunté de dónde salió. Todo el mundo lo estaba mandando.");
        pista(l, ps, "vlado", "Vlado (tele)", "la tele", 1720, 620, new Color(0x333344),
                "VLADO: Buenos días, Macondo.",
                "VLADO: Yo dije «se rumora». No dije «se confirma».",
                "VLADO: Fue un error de énfasis. Y ya ven cómo corrió.");
        pista(l, ps, "rosa", "Doña Rosa", "la señora", 1400, 600, new Color(0xB5473A),
                "DOÑA ROSA: El pan caliente no espera.",
                "DOÑA ROSA: Nadie del colegio ha dicho nada. Nadie ha ido a preguntar.",
                "DOÑA ROSA: Cuando algo asusta tanto, primero se pregunta. Después se pasa.");

        l.alPrimeraEntrada(() -> l.dialogo(() -> repartirMisiones(l, () -> {
            p.poner("feria_pistas_" + PANADERIA);
            l.mostrarMarcas(true);
            l.toast("Hablen con los del ! para encontrar 3 pistas", new Color(0xFFD35C));
        }),
                "Ringo: Los cuatro celulares vibran a la vez. Nubia reenvió un audio al grupo del barrio.",
                "NUBIA: (en el audio) Vecinos, dicen que cierran el colegio en enero.",
                "SAMUEL: ¿Cierran mi colegio?",
                "PERIODISTA: Cien mil vistas no es una fuente. Averigüemos de dónde salió.",
                "CIUDADANO: Separémonos y preguntemos."));

        l.alCompletar(() -> l.dialogo(() -> l.mostrarMarcas(false),
                "RECTORA VISITACIÓN: (entra con un folder) Nadie ha anunciado cierre. Hay una revisión de programas, nada más.",
                "SAMUEL: ¿Entonces mi colegio sigue?",
                "DOÑA ROSA: Sigue. Afuera hay dos rumores más: uno en el malecón y otro en la Junta.",
                "DOÑA ROSA: Ustedes escogen por dónde empezar. La puerta es esa, la del tapete."));
        puerta(l, p, h, 1480, 930, 280, 110);
        return l;
    }

    // =====================================================================================
    // 2. MALECON: DECIDIR
    // =====================================================================================

    private static Lugar malecon(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, MALECON, "Parque y malecón", -1);
        l.fondo(Rutas.MALECON);
        l.caminable(60, 450, 1800, 380);
        l.obstaculo(1240, 700, 260, 80, "Banca");
        l.obstaculo(700, 440, 470, 60, "Banca del árbol");
        l.obstaculo(60, 560, 280, 250, "Carrito de arepas");
        l.equipoEn(720, 760);
        l.rumor("Hay que limpiar el río. Los vendedores se van.", RITMO_RUMOR);
        l.niebla("La bruma sube desde el río y se enreda en los carritos.",
                "CHELA: El río tiene su modo. Cuando se enoja, se lo cuenta uno al otro.");
        l.objetivo("respeto", "Lo que pasa con Daniela");
        l.objetivo("digital", "La publicación del malecón (Q)");
        l.tiempoFeria(SEGUNDOS_LUGAR, () -> tiempoAgotado(l));

        l.npc("chela", "Chela", "la pescadora", 1380, 640, new Color(0x3F8EDC))
                .interactuar("Hablar con Chela", () -> l.dialogo(null,
                        "CHELA: El papel del río es de hace un año. Lo viejo ya se arregló.",
                        "CHELA: Lo que falta es el alcantarillado, y de eso nadie habla."));
        l.npc("emiro", "Emiro", "el de los jugos", 1290, 520, new Color(0xE0A030))
                .interactuar("Hablar con Emiro", () -> l.dialogo(null,
                        "EMIRO: Subieron una foto de mi carrito sin preguntarme. Así cualquiera opina."));
        l.npc("daniela", "Daniela", "la de las arepas", 430, 640, new Color(0x3F8EDC))
                .interactuar("Hablar con Daniela", () -> l.dialogo(null,
                        "DANIELA: Las arepas las hago con huevo y con cariño. Y con mi nombre."));
        Actor beatriz = l.npc("beatriz", "Doña Beatriz", "la señora", 1530, 520, new Color(0xD9A5EC));
        l.npc("julian", "Julián", "el muchacho", 1000, 540, new Color(0x7A6A5A))
                .interactuar("Hablar con Julián", () -> l.dialogo(null, "JULIÁN: Yo solo puse me gusta. ¿Eso cuenta?"));

        // lo de Daniela pasa cuando alguien se acerca a Doña Beatriz (en el orden que quieran)
        boolean[] respetoVisto = {false};
        beatriz.interactuar("Hablar con Doña Beatriz", () -> {
            if (respetoVisto[0]) {
                l.dialogo(null, "DOÑA BEATRIZ: Daniela. Ya me lo aprendí, mijo.");
                return;
            }
            respetoVisto[0] = true;
            l.quitarMarca("beatriz");
            l.conAudiencia(null, () -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: Llega Daniela con su carrito azul.",
                        "DOÑA BEATRIZ: (sin bajar la voz) Y ahí viene el señor de las arepas…",
                        "DANIELA: Doña Beatriz. Ya se lo dije la otra vez."))
                .paso(sig -> DecisionGrupal.respeto(l, "Doña Beatriz confunde a Daniela con «el señor de las arepas».",
                        new String[]{
                            "Decirle con calma que es Daniela y que merece el mismo respeto.",
                            "Cambiar de tema hacia el río.",
                            "Reírte para que pase."},
                        (j, op) -> {
                            if (op == 0) {
                                GuionDia1.respetoA(l, j, 2);
                                l.dialogo(null, GuionDia1.comoJugador(j,
                                        "JUGADOR: Doña Beatriz, ella es Daniela. Y merece el mismo respeto que cualquiera.",
                                        "DOÑA BEATRIZ: Tiene razón. Disculpe, Daniela.",
                                        "DANIELA: Gracias. Es lo único que pido."));
                            } else if (op == 1) {
                                l.dialogo(null, GuionDia1.comoJugador(j, "JUGADOR: Y del río, ¿qué decía el papel?",
                                        "Ringo: Daniela baja la mirada y se pone a limpiar su carrito."));
                                l.completar("respeto");
                            } else {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -3, Partida.ANIMO, -1), "Respeto: se rió");
                                Actor dn = l.actor("daniela");
                                if (dn != null) {
                                    l.quitar(dn);
                                }
                                l.dialogo(null, GuionDia1.comoJugador(j, "JUGADOR: Ja, ja, son cosas de uno.",
                                        "Ringo: Daniela agarra su carrito y se va.",
                                        "CIUDADANO: Eso se arregla con una disculpa, pero hay que dar la cara."));
                                l.completar("respeto");
                            }
                        },
                        sumados -> {
                            GuionDia1.yoTambien(l, sumados);
                            l.completar("respeto");
                            if (!l.completado()) {
                                l.toast("¿Ya revisaron el celular (Q)? Están compartiendo una publicación", new Color(0xFFD35C));
                            }
                            sig.run();
                        }))
                .iniciar());
        });

        l.alPrimeraEntrada(() -> l.dialogo(() -> {
            marcar(l, "beatriz");
            l.mostrarMarcas(true);
            l.toast("Dos cosas en el malecón: Doña Beatriz (!) y el celular (Q). Ustedes escogen el orden", new Color(0xFFD35C));
        },
                "Ringo: En la pared hay un papel: «Hay que limpiar el río. Los vendedores se van.»",
                "EMIRO: Limpio está bien. Lo malo es que «limpio» siempre empieza por sacarnos a nosotros.",
                "CHELA: Y en el grupo del barrio ya están compartiendo una foto. Revisen el celular."));

        l.digital("Grupo del barrio",
                "«Hay que limpiar el malecón. Fuera los puestos. ¿Quién apoya?» (con una foto del carrito de Emiro)",
                "Lleva 52 pulgares arriba.",
                new String[]{
                    "Verificar: reviso quién lo subió y cuándo, y pregunto si Emiro dio permiso.",
                    "Reportar: reporto la publicación por señalar a un vendedor sin su permiso.",
                    "Tomarme un jugo en el muro y dejar de leer los comentarios.",
                    "Guardar silencio: sigo caminando. No es mi pelea.",
                    "Difundir: le doy me gusta y comento que apoyo."},
                null,
                el -> {
                    for (Map.Entry<Rol, Integer> e : el.entrySet()) {
                        Jugador j = p.jugador(e.getKey());
                        if (j != null && e.getValue() == Lugar.A) {
                            j.poner("feria_verifico");
                        }
                    }
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("CHELA: La cuenta se creó hace dos días. Preguntar de dónde viene es lo que nos falta.");
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("EMIRO: (cansado) Ya se está regando.");
                    }
                    d.add("SAMUEL: ¿Entonces no se va nadie del malecón?");
                    d.add(faltan(p).contains(JUNTA) ? "CHELA: Por hoy, no. Suban a la Junta: allá también hay lío."
                            : "CHELA: Por hoy, no. Y ya va siendo hora de la cena: Doña Rosa los espera.");
                    l.dialogo(null, d.toArray(new String[0]));
                });

        puerta(l, p, h, 1600, 450, 220, 110);
        return l;
    }

    // =====================================================================================
    // 3. JUNTA: COOPERAR (votacion + los faroles)
    // =====================================================================================

    private static Lugar junta(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, JUNTA, "Junta de Acción Comunal", -1);
        l.fondo(Rutas.JUNTA);
        l.caminable(60, 640, 1800, 400);
        l.obstaculo(1200, 680, 220, 70, "Banca");
        l.obstaculo(170, 700, 470, 180, "Escritorio");
        l.equipoEn(560, 980);
        l.rumor("La Junta se robó la plata del alcantarillado.", RITMO_RUMOR);
        l.niebla("La niebla se mete por la escalera angosta.", "DOÑA PETRA: Leer no es lo mismo que comprobar.");
        l.objetivo("voto", "Votación de la casa");
        l.objetivo("faroles", "Prender los faroles (los cuatro)");
        l.tiempoFeria(SEGUNDOS_LUGAR, () -> tiempoAgotado(l));

        Actor petra = l.npc("petra", "Doña Petra", "la presidenta", 1010, 720, new Color(0xC9553A));
        l.npc("wilson", "Wilson", "el muchacho", 1150, 800, new Color(0x4C8BD9))
                .interactuar("Hablar con Wilson", () -> l.dialogo(null, "WILSON: Yo solo digo lo que leo."));
        Actor alirio = l.npc("alirio", "Don Alirio", "el señor", 700, 760, new Color(0x6A5ACD));
        l.npc("amaranta", "Amaranta", "la secretaria", 1350, 880, new Color(0x2B2350));

        String[] ops = {
            "Pegar en la pared el papel de las cuentas, para que todos lo lean (prudente).",
            "Hacer un en vivo contando lo que sabemos.",
            "No meternos: no es asunto nuestro."};
        String[] votoLetra = {"A", "B", "C"};
        // dos cosas, en el orden que quieran (pero una a la vez: las dos son de los cuatro)
        boolean[] votoVisto = {false}, farolesVisto = {false}, enCurso = {false};

        petra.interactuar("Hablar con Doña Petra", () -> {
            if (votoVisto[0]) {
                l.dialogo(null, "DOÑA PETRA: Leer no es lo mismo que comprobar, Wilson.");
                return;
            }
            if (enCurso[0]) {
                l.dialogo(null, "DOÑA PETRA: Primero terminen con los faroles, que sin luz no se lee nada.");
                return;
            }
            votoVisto[0] = true;
            enCurso[0] = true;
            l.quitarMarca("petra");
            l.conAudiencia(null, () -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "DOÑA PETRA: Las cuentas están en ese papel que nadie ha leído.",
                        "WILSON: Pero en el grupo dicen que se robaron la plata.",
                        "CANDIDATO: Hay que decidir qué hacemos. Entre los cuatro."))
                .paso(sig -> {
                    for (Jugador j : p.jugadores()) {
                        j.quitar("feria_voto_A");
                        j.quitar("feria_voto_B");
                        j.quitar("feria_voto_C");
                    }
                    DecisionGrupal.votacion(l, "¿Cómo frenamos el rumor de la Junta?", ops, 0, g -> {
                        p.poner("feria_gano_" + votoLetra[g]);
                        if (g == 0) {
                            p.sumarDesinfo(JUNTA, -15);
                            l.dialogo(sig, "DOÑA PETRA: Así sí. El papel en la pared, a la vista de todos.",
                                    "WILSON: Ah… la plata está ahí. Solo que nadie había leído.");
                        } else if (g == 1) {
                            p.sumarDesinfo(JUNTA, -5);
                            l.dialogo(sig, "INFLUENCER: Salimos en vivo. Algunos escuchan, otros no.",
                                    "DOÑA PETRA: Mejor que nada. Pero el papel sigue sin leerse.");
                        } else {
                            p.sumarDesinfo(JUNTA, 10);
                            l.dialogo(sig, "WILSON: Pues si nadie dice nada, será verdad.",
                                    "DOÑA PETRA: (suspira) Así se arman los rumores.");
                        }
                    });
                })
                .paso(sig -> {
                    enCurso[0] = false;
                    l.completar("voto");
                    if (!farolesVisto[0]) {
                        l.toast("Falta ayudar a Don Alirio con los faroles (!)", new Color(0xFFD35C));
                    }
                })
                .iniciar());
        });

        alirio.interactuar("Hablar con Don Alirio", () -> {
            if (farolesVisto[0]) {
                l.dialogo(null, "DON ALIRIO: Con luz la gente sale a hablar. Y hablando se entiende.");
                return;
            }
            if (enCurso[0]) {
                l.dialogo(null, "DON ALIRIO: Terminen la votación primero. Yo aquí espero.");
                return;
            }
            farolesVisto[0] = true;
            enCurso[0] = true;
            l.quitarMarca("alirio");
            l.conAudiencia(null, () -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Ringo: De pronto se va la luz. Los celulares se apagan. Afuera, los faroles titilan.",
                        "DON ALIRIO: Otra vez. Y sin señal nadie se entera de nada.",
                        "CIUDADANO: Sin señal, la gente se cuenta las cosas a la cara. Prendamos los faroles, ¡los cuatro a la vez!"))
                .paso(sig -> l.lanzar(new MjFaroles(), res -> {
                    l.efectoCompartido(res.efecto, "Los faroles");
                    enCurso[0] = false;
                    l.completar("faroles");
                    if (!votoVisto[0]) {
                        l.toast("Falta la votación con Doña Petra (!)", new Color(0xFFD35C));
                    }
                    sig.run();
                }))
                .iniciar());
        });

        l.alPrimeraEntrada(() -> l.dialogo(() -> {
            marcar(l, "petra");
            marcar(l, "alirio");
            l.mostrarMarcas(true);
            l.toast("Dos cosas en la Junta: la votación (Doña Petra) y los faroles (Don Alirio)", new Color(0xFFD35C));
        },
                "WILSON: Dicen que la Junta se robó la plata del alcantarillado. Lo leí en el grupo.",
                "DOÑA PETRA: Leer no es lo mismo que comprobar.",
                "CANDIDATO: Dos cosas: decidir con Doña Petra qué hacemos y ayudar a Don Alirio con la luz. ¿Por cuál empezamos?"));

        puerta(l, p, h, 700, 990, 600, 60);
        return l;
    }

    /** Guarda el voto de cada jugador (lo llama la votacion) para la mision "Voz de la casa". */
    static void anotarVoto(Jugador j, int op) {
        j.poner("feria_voto_" + (char) ('A' + op));
    }

    // =====================================================================================
    // FINAL
    // =====================================================================================

    public static Cinematica finalFeria(Partida p) {
        String gano = p.bandera("feria_gano_A") ? "A" : p.bandera("feria_gano_B") ? "B" : p.bandera("feria_gano_C") ? "C" : null;
        revisarMisiones(p, gano);

        String[] lugares = {PANADERIA, MALECON, JUNTA};
        double suma = 0;
        int nieblas = 0, limpios = 0;
        for (String lg : lugares) {
            double d = p.desinfo(lg);
            suma += d;
            if (p.hayNiebla(lg)) {
                nieblas++;
            }
            if (d < 50) {
                limpios++;
            }
        }
        double prom = suma / lugares.length;
        char f = prom < 45 && nieblas == 0 ? 'A' : (prom >= 70 || nieblas >= 2) ? 'C' : 'B';

        Cinematica c = new Cinematica(p).cine();
        c.cuadro("/imagenes/casa_cena.png",
                "La cena. Doña Rosa pone cuatro tazas de tinto. Afuera llueve un poco.",
                "DOÑA ROSA: Hoy la gente habló de todo. Veamos qué escuchó.")
                .musica("/sonidos/musica_isla.wav", 0.5, 1.5);
        GuionDia2.alrededorDeLaMesa(c);
        GuionDia2.npcFinal(c, "rosa", 1560, 340, 230, 280);

        if (f == 'A') {
            c.cuadro("/imagenes/malecon.png",
                    "Amanece. La isla se acuerda de los nombres. Las mariposas amarillas vuelven a la plaza.",
                    "DOÑA ROSA: Hoy sí nos acordamos.",
                    "SAMUEL: ¿Ya los van a dejar quedarse?")
                    .mariposa(700, 520, 1500, 180)
                    .mariposa(300, 700, 1200, 260);
        } else if (f == 'B') {
            c.cuadro("/imagenes/malecon.png",
                    "Amanece. Unas calles claras y otras con bruma. La mitad cree una cosa y la mitad otra.",
                    "DOÑA ROSA: Nos acordamos de unas cosas y de otras no.",
                    "CIUDADANO: Faltó preguntar un poco más.")
                    .mariposa(600, 600, 1300, 300);
        } else {
            c.cuadro("/imagenes/casapuertasin.png",
                    "Amanece. La bruma lo cubre casi todo. La gente se mira y no se encuentra el nombre.",
                    "DOÑA ROSA: ¿Quién era usted?",
                    "SAMUEL: Yo sí me acuerdo. Todavía están a tiempo.");
        }
        GuionDia2.losCuatro(c, 380, 470, 250);

        c.cuadro("/imagenes/casa_cena.png", "Así jugó cada quien.",
                "RINGO: ¡Miren esos títulos! Y las misiones secretas…",
                "RINGO: Rumor promedio " + (int) Math.round(prom) + "% · Lugares sin niebla " + (lugares.length - nieblas)
                        + " de 3 · Mariposas " + p.totalMariposas() + ".")
                .capa(new FinalTarjetas(p));

        String titulo = f == 'A' ? "¡La isla despertó!" : f == 'B' ? "La isla quedó dividida" : "La isla olvidó";
        return c.cuadro("/imagenes/fondoinicio.png", "La isla, de lejos.",
                "RINGO: Hay islas que olvidan y hay islas que recuerdan. Y hay gente que ayuda a decidir cuál.")
                .musica("/sonidos/musica_titulo.wav", 0.8, 1.5)
                .titulo(titulo, "Todos hablan en Macondo · Modo Feria");
    }
}
