package juegoferia;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El guion del DIA 1 convertido en juego (un solo guion para los cuatro roles).
 *
 *   Casa (7:00) -> Panaderia (7:10) -> Camino al malecon (9:00) -> Malecon (11:00)
 *   -> Junta (14:00) -> Plaza, el apagon (17:00) -> Casa de noche, la cena (19:00)
 *
 * COMO SE ESCRIBE UN LUGAR (copiar el de la panaderia, que esta completo):
 *   1. new Lugar(...), fondo, zona caminable (F1 muestra las cajas), obstaculos y equipoEn(x, y).
 *   2. vecinos con c.vecino(npc, etiqueta, lineas de charla).
 *   3. lo que pasa al entrar: alPrimeraEntrada(...) con dialogos y la DECISION DE RESPETO.
 *   4. la DECISION DIGITAL con l.digital(...): solo los textos, los efectos A-E ya estan en Lugar.
 *   5. el MINIJUEGO FIJO con l.minijuegoFijo(rol, minijuego, resultado -> ...).
 *   6. las CUATRO PRUEBAS con c.prueba(...): cada rol consigue su pieza hablando con un vecino.
 *   7. alCompletar(...): la verdad del rumor y la salida al siguiente lugar.
 *
 * Los dialogos van como en el guion: "DOÑA ROSA: texto", "CIUDADANO: texto".
 * {CANDIDATO}, {PERIODISTA}... se cambian por el nombre de quien juega ese rol.
 * Las posiciones son PROVISIONALES: acomodarlas cuando esten los fondos de Canva.
 */
public final class GuionDia1 {

    private GuionDia1() {
    }

    // ---- ids de los lugares del dia 1 ----
    public static final String CASA = GrafoLocaciones.CASA;
    public static final String PANADERIA = GrafoLocaciones.PANADERIA;
    public static final String CAMINO = "Camino_malecon";
    public static final String MALECON = GrafoLocaciones.MALECON;
    public static final String JUNTA = GrafoLocaciones.JUNTA;
    public static final String PLAZA = GrafoLocaciones.PLAZA;
    public static final String CENA = "Casa_noche";

    public static final String PRIMER_LUGAR = CASA;

    // ---- fondos de Canva (1920x1080, sin personajes) ----
    static final String F = "/imagenes/fondos/";

    public static Lugar crear(String lugar, Partida p, Lugar.Anfitrion h) {
        switch (lugar) {
            case CASA: return casaManana(p, h);
            case PANADERIA: return panaderia(p, h);
            case CAMINO: return camino(p, h);
            case MALECON: return malecon(p, h);
            case JUNTA: return junta(p, h);
            case PLAZA: return apagon(p, h);
            case CENA: return cena(p, h);
            default:
                throw new IllegalArgumentException("El Día 1 no tiene el lugar " + lugar);
        }
    }

    // =====================================================================================
    // AYUDA: vecinos con charla y las cuatro pruebas
    // =====================================================================================

    /** Lo que recuerda cada lugar mientras se juega: charla de cada vecino y pruebas hechas. */
    static final class Ctx {

        final Lugar l;
        final Partida p;
        boolean pruebasActivas;
        final Set<Rol> hechas = EnumSet.noneOf(Rol.class);
        private final Map<String, String[]> charla = new HashMap<>();
        private final Map<String, Map<Rol, Runnable>> pruebas = new HashMap<>();
        private Runnable alTerminarPruebas;

        Ctx(Lugar l) {
            this.l = l;
            this.p = l.partida();
        }

        /** Un vecino con quien se puede hablar (E). */
        Actor vecino(String id, String nombre, String enNiebla, double x, double y, Color ropa, String... lineas) {
            Actor a = l.npc(id, nombre, enNiebla, x, y, ropa);
            charla.put(id, lineas);
            a.interactuar("Hablar con " + nombre, () -> hablar(a));
            return a;
        }

        void charla(String id, String... lineas) {
            charla.put(id, lineas);
        }

        /** La prueba de un rol: se hace hablando con el vecino 'id' cuando ese rol camina. */
        void prueba(String id, Rol rol, String idPieza, String nombrePieza, String... lineas) {
            pruebas.computeIfAbsent(id, k -> new EnumMap<>(Rol.class)).put(rol, () -> l.dialogo(() -> {
                l.darPieza(rol, idPieza, nombrePieza);
                hechas.add(rol);
                if (hechas.size() == 4) {
                    l.completar("pruebas");
                    if (alTerminarPruebas != null) {
                        alTerminarPruebas.run();
                    }
                }
            }, lineas));
        }

        void activarPruebas() {
            pruebasActivas = true;
            l.toast(Sincronia.activa() ? "Cada quien consigue su pieza del Letrero, con su propio personaje." : "Cada quien consigue su pieza del Letrero. TAB cambia quién camina.", new Color(0xFFD35C));
        }

        private void hablar(Actor a) {
            if (pruebasActivas && pruebas.containsKey(a.id)) {
                Rol lider = p.lider().rol;
                Map<Rol, Runnable> m = pruebas.get(a.id);
                if (m.containsKey(lider) && !hechas.contains(lider)) {
                    m.get(lider).run();
                    return;
                }
                for (Rol r : m.keySet()) {
                    if (!hechas.contains(r)) {
                        Jugador j = p.jugador(r);
                        l.burbuja(a, "Esto es con " + (j != null ? j.nombre : r.nombre) + "." + (Sincronia.activa() ? "" : " (TAB)"), 3);
                        return;
                    }
                }
            }
            String[] c = charla.get(a.id);
            if (c != null && c.length > 0) {
                l.dialogo(null, c);
            }
        }
    }

    /** Reemplaza "JUGADOR:" del guion por quien eligio. */
    static String[] comoJugador(Jugador j, String... lineas) {
        String[] r = new String[lineas.length];
        for (int i = 0; i < lineas.length; i++) {
            r[i] = lineas[i].replace("JUGADOR:", j.rol.name() + ":");
        }
        return r;
    }

    /** Reacciones "segun quien respondio": las lineas de los OTROS roles. */
    static String[] reacciones(Jugador quien, Map<Rol, String> porRol) {
        List<String> r = new ArrayList<>();
        for (Map.Entry<Rol, String> e : porRol.entrySet()) {
            if (e.getKey() != quien.rol) {
                r.add(e.getKey().name() + ": " + e.getValue());
            }
        }
        return r.toArray(new String[0]);
    }

    static String[] unir(String[]... partes) {
        List<String> r = new ArrayList<>();
        for (String[] p : partes) {
            for (String s : p) {
                r.add(s);
            }
        }
        return r.toArray(new String[0]);
    }

    /** Efectos de la decision de respeto (guion: "Decision de respeto"). */
    static void respetoA(Lugar l, Jugador j, int convivencia) {
        l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, convivencia, Partida.ANIMO, 1), "Respeto: A");
        l.mariposa(j);
    }

    static void yoTambien(Lugar l, List<Jugador> sumados) {
        for (Jugador s : sumados) {
            l.efecto(s, Efecto.de(EstadoJuego.CONVIVENCIA, 1), "Respeto: yo también");
            l.mariposa(s);
        }
    }

    // =====================================================================================
    // LA MANANA EN LA CASA (7:00)
    // =====================================================================================

    private static Lugar casaManana(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CASA, "La casa de Doña Rosa", Reloj.h(7, 0));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.CASA);
        l.caminable(80, 560, 1760, 480);
        l.equipoEn(900, 900);
        c.vecino("samuel", "Samuel", "el niño", 1500, 760, new Color(0x6FA8DC),
                "SAMUEL: Salomé me dijo que hoy hay algo raro en el grupo del barrio.",
                "CIUDADANO: ¿Raro cómo?",
                "SAMUEL: No sé. Mi abuela dice que no lea cosas de los grandes.",
                "INFLUENCER: Tu abuela tiene razón. Y no tiene razón.");
            l.salida(400, 850, 350, 230, PANADERIA, "Bajar a la panadería");
       
            l.alPrimeraEntrada(() -> l.dialogo(null,
                "Narrador: Los cuatro despiertan a la vez. Abajo se oye a Doña Rosa abriendo la panadería.",
                "INFLUENCER: ¿Alguien más durmió? Yo soñé que mi afiche me hablaba.",
                "PERIODISTA: Yo no. Llevo dos horas escribiendo lo que se comenta de la elección. Luego lo mando a la imprenta y lo reparto por ahí.",
                "INFLUENCER: Eso es tan del siglo pasado, {PERIODISTA}.",
                "CANDIDATO: Deja tú eso a un lado… ¿Aquí siquiera hay de eso?",
                "CIUDADANO: No subestimen esta isla. De no haberla, es cuestión de tiempo para que llegue. Aquí todo pasa demasiado rápido.",
                "CANDIDATO: Faltan dos días y el pueblo está inquieto. Mi campaña no se hace sola.",
                "CIUDADANO: Desayunemos primero. Doña Rosa ya abrió."));
        return l;
    }

    // =====================================================================================
    // 1.1 PANADERIA DE DONA ROSA (7:10)  ·  LUGAR COMPLETO DE EJEMPLO
    // =====================================================================================

    private static Lugar panaderia(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, PANADERIA, "Panadería de Doña Rosa", Reloj.h(7, 10));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.PANADERIA);
        l.caminable(70, 570, 1780, 470);
        l.obstaculo(1060, 440, 760, 150, "Mostrador");
        l.equipoEn(900, 960);

        l.rumor("Dicen que cierran el colegio en enero.", 0);
        l.letrero("ESTO ES LO QUE DIJO, NO LO QUE DICEN QUE DIJO.");
        l.niebla("Una bruma tibia entra por la ventana y se mete entre los panes. La gente de la fila mira las cosas y no les encuentra el nombre.",
                "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo. Cuando la gente deja de preguntar, empieza a olvidar.");

        l.objetivo("respeto", "Lo que pasa en la fila");
        l.objetivo("digital", "El audio de Nubia (celular, Q)");
        l.objetivo("minijuego", "Rastrear el audio");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        // ---- vecinos (posicion = pies) ----
        c.vecino("rosa", "Doña Rosa", "la señora", 1400, 610, new Color(0xB5473A),
                "DOÑA ROSA: Siéntense, que el pan caliente no espera.");
        c.vecino("ursula", "Doña Úrsula", "la anciana", 250, 700, new Color(0x8A6A9A),
                "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo.");
        c.vecino("nubia", "Nubia", "la del audio", 600, 680, new Color(0xD98AB0),
                "NUBIA: ¿Vieron lo del noticiero?");
        c.vecino("samuel", "Samuel", "el niño", 1650, 900, new Color(0x6FA8DC),
                "SAMUEL: ¿Cierran mi colegio?");
        c.vecino("mateo", "Mateo", "el muchacho", 880, 720, new Color(0x4C8BD9),
                "MATEO: Mi hermana y yo estudiamos ahí. ¿De dónde sacan eso?");
        c.vecino("camila", "Camila", "la niña", 980, 770, new Color(0xF2B01E));
        c.vecino("ximena", "Ximena", "la señora", 1150, 830, new Color(0x5BA843));
        c.vecino("lorena", "Lorena", "la señora", 1260, 850, new Color(0x3E7F2F));
        c.vecino("salome", "Salomé", "la niña", 1200, 920, new Color(0xF9A3BF));
        c.vecino("eliecer", "Don Eliécer", "el señor", 450, 900, new Color(0x7A6A5A),
                "DON ELIÉCER: Pues si lo cierran, por algo será.");
        c.vecino("vlado", "Vlado (tele)", "la tele", 1760, 640, new Color(0x333344),
                "VLADO: Buenos días, Macondo.");
        l.salida(1490, 960, 330, 120, CAMINO, "Salir hacia el malecón");

        // ---- al entrar: el audio, la tele y la decision de respeto ----
        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "DOÑA ÚRSULA: Siéntense, que el pan caliente no espera. Esto ya lo viví, aunque no sabría decir cuándo.",
                        "Narrador: Los cuatro celulares vibran a la vez. En el grupo del barrio, Nubia reenvió un audio sin nombre de dos minutos. Ya tiene treinta pulgares arriba.",
                        "NUBIA: (en el audio) Vecinos, ¿vieron lo del noticiero? Dicen que cierran el colegio en enero.",
                        "INFLUENCER: Ya tiene cien mil vistas en una cuenta que no conozco.",
                        "PERIODISTA: Cien mil vistas no es una fuente.",
                        "CANDIDATO: Mi equipo dice que Aguado ya lo negó en vivo. Hay que aprovechar.",
                        "CIUDADANO: Esperen. Miren a Doña Rosa.",
                        "SAMUEL: ¿Cierran mi colegio?",
                        "DOÑA ROSA: Eso lo dijo Nubia. No lo escuchaste de mí, Samu, pero a Nubia le enseñaron a mandar audios hace tres semanas y desde entonces no para. No creo que sea de fiar.",
                        "VLADO: Buenos días, Macondo. A dos días de una elección que pinta brava. Se rumora con fuerza que el candidato Aguado cerraría el colegio distrital si gana.",
                        "MATEO: ¿Cerrar el colegio? Mi hermana y yo estudiamos ahí. ¿De dónde sacan eso?",
                        "XIMENA: Salomé no durmió anoche. Dice que si lo cierran, ¿dónde van a meter a los chicos del comité de convivencia?",
                        "DON ELIÉCER: Pues si lo cierran, por algo será. Los colegios deberían enseñar matemáticas y dejar tanta vaina de ideologías modernas. Pff, como si eso fuera real.",
                        "Narrador: Ximena le aprieta la mano a su hija."))
                .paso(sig -> respetoPanaderia(l))
                .iniciar());

        // ---- decision digital ----
        l.digital("Grupo del barrio",
                "Audio de Nubia (2:00): «Vecinos, ¿vieron lo del noticiero? Dicen que cierran el colegio en enero.»",
                "El audio sigue circulando. Ya tiene cuarenta pulgares arriba y una señora preguntando si es cierto.",
                new String[]{
                    "Verificar: antes de pasarlo, pregunto de dónde salió y busco la noticia.",
                    "Reportar: marco el audio como dudoso y aviso al administrador del grupo.",
                    "Tomarme un tinto: dejo el celular, pido un tinto y respiro.",
                    "Guardar silencio: no digo nada y sigo en la fila.",
                    "Difundir: lo reenvío a mi familia por si acaso."},
                new String[]{
                    "{rol} se puso a verificar de dónde salió el audio.",
                    "{rol} reportó el audio.",
                    "{rol} se tomó un tinto.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió el audio."},
                el -> despuesDigitalPanaderia(c, el));
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "nubia"));

        // ---- las cuatro pruebas ----
        c.prueba("nubia", Rol.CIUDADANO, "carton_rosa", "Cartón de Doña Rosa",
                "CIUDADANO: Nubia, ¿de quién te llegó el audio?",
                "NUBIA: (bajito) Me lo mandó Yolanda. Yo solo lo pasé.",
                "DOÑA ROSA: Tome un cartón limpio. Y carbón, que de eso tengo.");
        c.prueba("vlado", Rol.PERIODISTA, "nota_vlado", "Nota de Vlado",
                "Narrador: {PERIODISTA} llama al noticiero con el teléfono de latas de la panadería.",
                "PERIODISTA: Vlado, ¿me puede dar la nota original?",
                "VLADO: Claro. Mire: dice «se rumora». No dice «se confirma».");
        c.prueba("rosa", Rol.INFLUENCER, "pintura_carbon", "Pintura de carbón",
                "INFLUENCER: Va el aviso para el grupo del barrio.",
                "Narrador: Escribe con el carbón sobre el cartón: «Ojo: nadie ha anunciado el cierre del colegio. Dijeron 'se rumora'. Pregunta antes de pasar.»");
        c.prueba("rosa", Rol.CANDIDATO, "firma_panaderia", "Firma de Candidato",
                "CANDIDATO: No voy a usar este rumor en mi campaña. Ni a favor ni en contra.",
                "DOÑA ROSA: (lo anota al pie del cartón) Firmado.",
                "INFLUENCER: Eso sí lo voy a grabar.",
                "CANDIDATO: No lo grabes, que me comprometes.");

        // ---- la verdad del rumor ----
        l.alCompletar(() -> {
            l.npc("visitacion", "Rectora Visitación", "la rectora", 300, 820, new Color(0x2E4A7A));
            l.dialogo(() -> l.toast("Salgan hacia el malecón (puerta de la izquierda)", new Color(0xFFD35C)),
                    "VLADO: Quiero aclarar algo. Dije «se rumora». No dije «se confirma». Fue un error de énfasis.",
                    "RECTORA VISITACIÓN: (entra con un folder) Nadie ha anunciado cierre. Hay una revisión de programas, nada más.",
                    "Narrador: Nubia baja la cabeza y se esconde detrás de una torta.",
                    "NUBIA: Yo no sabía.",
                    "DOÑA ROSA: Nadie sabe hasta que pregunta, Nubia.",
                    "SAMUEL: ¿Entonces mi colegio sigue?",
                    "RECTORA VISITACIÓN: Sigue, Samuel.",
                    "Narrador: Samuel respira y se ríe.",
                    "DOÑA ROSA: Samuel se me escapó por la puerta de atrás. Dijo que iba a avisarle a Salomé. Creo que se fue al malecón. ¿Me lo buscan?");
            Actor s = l.actor("samuel");
            if (s != null) {
                l.quitar(s);
            }
        });
        return l;
    }

    private static void respetoPanaderia(Lugar l) {
        Map<Rol, String> reacA = new EnumMap<>(Rol.class);
        reacA.put(Rol.PERIODISTA, "Bien dicho. No es más.");
        reacA.put(Rol.INFLUENCER, "Me quedé sin palabras. Gracias.");
        reacA.put(Rol.CANDIDATO, "Eso me sirve para apelar… Definitivamente hay que decirlo así.");
        reacA.put(Rol.CIUDADANO, "Eso es lo que tocaba.");

        DecisionGrupal.respeto(l, "Don Eliécer se burla del comité de convivencia delante de Ximena y Salomé.",
                new String[]{
                    "Responder con calma: «Ese comité es de gran ayuda para mucha gente.»",
                    "Cambiar de tema hacia el audio.",
                    "Reírte con Don Eliécer para no armar lío."},
                (j, op) -> {
                    if (op == 0) {
                        respetoA(l, j, 2);
                        l.dialogo(null, unir(comoJugador(j,
                                "JUGADOR: Don Eliécer, ese comité es de gran ayuda para mucha gente. Es cuestión de que a nadie lo molesten por quien es.",
                                "Narrador: Don Eliécer se queda callado y mira a la nada, avergonzado.",
                                "DON ELIÉCER: No lo había visto así.",
                                "XIMENA: (a su hija) ¿Oíste, Salomé? Aquí también hay gente que nos defiende.",
                                "SALOMÉ: (sonríe, bajito) Gracias."), reacciones(j, reacA)));
                    } else if (op == 1) {
                        l.dialogo(null, comoJugador(j,
                                "JUGADOR: Mejor hablemos del audio, que es lo que realmente preocupa.",
                                "Narrador: Ximena baja la mirada. Salomé también. Doña Rosa mueve el pan.",
                                "DOÑA ROSA: Ay, no hay peor ciego que el que no quiere ver.",
                                "CIUDADANO: Se nos fue la oportunidad. Hay otra más adelante."));
                        l.completar("respeto");
                    } else {
                        l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -4, Partida.ANIMO, -1), "Respeto: se rió");
                        j.poner("se_rio_con_eliecer");
                        for (String id : new String[]{"ximena", "salome", "lorena"}) {
                            Actor a = l.actor(id);
                            if (a != null) {
                                l.quitar(a);
                            }
                        }
                        l.dialogo(null, comoJugador(j,
                                "JUGADOR: Ja, ja. Es verdad, tanta moda. ¿Dos mujeres criando a una niña? ¿Y el papá? Así nunca prosperará ese hogar.",
                                "Narrador: Ximena toma a Salomé de la mano y se van. Lorena las sigue.",
                                "SAMUEL: ¿Por qué se fue Salomé?",
                                "DOÑA ROSA: (seca, a los cuatro) Pregúntenle a quien se rió.",
                                "CIUDADANO: ¿Te parece que este era el momento adecuado para reírse?",
                                "JUGADOR: Fue para no pelear.",
                                "PERIODISTA: Entonces debiste de haberte callado. A veces menos es más."));
                        l.completar("respeto");
                    }
                },
                sumados -> {
                    yoTambien(l, sumados);
                    l.completar("respeto");
                    l.toast("Revisen el celular (Q): el audio sigue circulando", new Color(0xFFD35C));
                });
    }

    private static void despuesDigitalPanaderia(Ctx c, Map<Rol, Integer> el) {
        Lugar l = c.l;
        Partida p = c.p;
        List<String> d = new ArrayList<>();
        if (el.containsValue(Lugar.A)) {
            d.add("DOÑA ROSA: Preguntar primero no cuesta nada.");
        }
        if (el.containsValue(Lugar.B)) {
            Actor n = l.actor("nubia");
            if (n != null) {
                n.expresion("roja");
            }
            d.add("Narrador: El audio queda marcado como «contenido dudoso». Nubia recibe el aviso y se pone roja.");
            d.add("DOÑA ROSA: Eso también se arregla hablando con Nubia, no solo marcando.");
        }
        if (el.containsValue(Lugar.C)) {
            d.add("DOÑA ROSA: Quien se calma a tiempo decide mejor.");
            d.add("INFLUENCER: ¿Tinto ahora? Se te enfría el audio.");
            d.add("CIUDADANO: Se enfría el audio y se enfría quien lo manda.");
        }
        if (el.containsValue(Lugar.D)) {
            d.add("CANDIDATO: A veces callar también es una decisión.");
        }
        if (el.containsValue(Lugar.E)) {
            d.add("Narrador: El audio sube a cincuenta reenvíos.");
            d.add("NUBIA: (orgullosa) ¿Ven? Yo no estaba loca.");
            d.add("SAMUEL: Entonces sí cierran mi colegio.");
            d.add("PERIODISTA: ¿Lo mandaste sin ver de dónde salió?");
            d.add("INFLUENCER: Todo el mundo lo estaba mandando.");
            d.add("PERIODISTA: Todo el mundo no es una fuente.");
        }
        boolean todosVerificaron = el.size() == 4 && !el.containsValue(Lugar.B) && !el.containsValue(Lugar.C)
                && !el.containsValue(Lugar.D) && !el.containsValue(Lugar.E);
        if (todosVerificaron) {
            d.add("CIUDADANO: Nos tocó trabajo, pero ya sabemos de dónde viene.");
        }
        d.add("DOÑA ROSA: Tome el celular, {CIUDADANO}. Ahí se ven los reenvíos.");

        boolean pista = el.containsValue(Lugar.A);
        boolean lupa = p.bandera("lupa_" + PANADERIA);
        l.dialogo(() -> l.minijuegoFijo(Rol.CIUDADANO, new MjRastreaAudio(pista, lupa), res -> {
            String[] r;
            if (res.puntos == Minijuego.BIEN) {
                r = new String[]{"CIUDADANO: La tele dijo «se rumora». El segundo dijo «dicen». El tercero ya dice «cierran en enero».",
                    "PERIODISTA: Cada vez que pasa, el verbo se hace más fuerte."};
            } else if (res.puntos == Minijuego.REGULAR) {
                r = new String[]{"PERIODISTA: Llegaste. Falta ver qué cambió por el camino."};
            } else {
                r = new String[]{"VLADO: (desde la tele) Debo aclarar algo. Yo dije «se rumora». No dije «se confirma»."};
            }
            l.dialogo(() -> {
                l.completar("minijuego");
                c.activarPruebas();
            }, r);
        }), d.toArray(new String[0]));
    }

    // =====================================================================================
    // 1.2 CAMINO AL MALECON (9:00)
    // =====================================================================================

    private static Lugar camino(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CAMINO, "Camino al malecón", Reloj.h(9, 0));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.CAMINO_MALECON);
        l.caminable(60, 600, 1800, 440);
        l.equipoEn(200, 900);
        l.objetivo("charla", "Caminar juntos");
        c.vecino("melquiades", "Melquíades", "el gitano", 1100, 700, new Color(0x7A3A8A),
                "MELQUÍADES: La verdad también necesita quien la reparta. Y ustedes son cuatro. Úsense.");
        c.vecino("vendedor", "Vendedor de bolsas", "el vendedor", 600, 760, new Color(0x9A8F6A),
                "UN VENDEDOR DE BOLSAS: Si andan buscando el malecón, siempre es hacia donde huele a río.");
        c.vecino("nina_pajaro", "Niña con un pájaro de papel", "la niña", 1400, 840, new Color(0xF9A3BF),
                "UNA NIÑA: ¿Ustedes saben hacer pajaritos de papel?");
        c.vecino("ursula", "Doña Úrsula", "la anciana", 350, 680, new Color(0x8A6A9A),
                "DOÑA ÚRSULA: (desde su silla) No se demoren. Las cosas que se aplazan se vuelven niebla.");
        l.salida(1780, 600, 140, 440, MALECON, "Seguir al malecón");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Narrador: Los cuatro salen juntos. Calle de tierra con casas bajas. La gente se asoma.",
                        "INFLUENCER: ¿Les pasa que todo el mundo mira como si fuéramos de la luna?",
                        "PERIODISTA: Es que somos de la luna.",
                        "CANDIDATO: Pues que nos miren. Hay una elección.",
                        "CIUDADANO: Una cosa: nadie aquí sabe qué es un celular. Cuidado con lo que mandan.",
                        "INFLUENCER: ¿Y tú cómo sabes cómo se maneja la gente?",
                        "CIUDADANO: Porque vivo de preguntar. Ustedes viven de mostrarse, de escribir y de prometer. A ver quién se equivoca primero."))
                .paso(sig -> {
                    Jugador inf = p.jugador(Rol.INFLUENCER);
                    l.decidirTurno(inf, "Respuesta rápida a {CIUDADANO}:",
                            Lugar.op("«Ja, ya veremos.»", () -> l.dialogo(sig, "INFLUENCER: Apuesto lo que quieras.")),
                            Lugar.op("«No me provoques.»", () -> {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, -1), "Camino: no me provoques");
                                l.dialogo(sig, "CIUDADANO: Perdón, fue una broma. Más o menos.");
                            }),
                            Lugar.op("«Me parece justo.»", () -> {
                                l.efectoCompartido(Efecto.de(Partida.CONFIANZA_ROLES, 1), "Camino: me parece justo");
                                l.dialogo(sig, "CIUDADANO: Eso es lo que quería oír.");
                            }));
                })
                .hacer(() -> l.completar("charla"))
                .iniciar());
        return l;
    }

    // =====================================================================================
    // 1.3 PARQUE Y MALECON (11:00)
    // =====================================================================================

    private static Lugar malecon(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, MALECON, "Parque y malecón", Reloj.h(11, 0));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.MALECON);
        l.caminable(60, 600, 1800, 440);
        l.obstaculo(60, 560, 1800, 60, "Muro del río");
        l.equipoEn(300, 920);

        l.rumor("Hay que limpiar el río. Los vendedores se van.", 0);
        l.letrero("ESTO ES EL RÍO, Y HOY ESTÁ LIMPIO. ESTO ES UN OFICIO, NO UN ESTORBO.");
        l.niebla("La bruma sube desde el río y se enreda en los carritos. Nadie recuerda cómo se llama el oficio de cada quien.",
                "CHELA: El río tiene su modo. Cuando se enoja, se lo cuenta uno al otro.");

        l.objetivo("respeto", "Lo que pasa con Daniela");
        l.objetivo("digital", "La publicación del malecón (Q)");
        l.objetivo("minijuego", "La fecha del agua");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("samuel", "Samuel", "el niño", 1500, 700, new Color(0x6FA8DC),
                "SAMUEL: Salomé quiere ser abogada para que nadie moleste a sus mamás.");
        c.vecino("chela", "Chela", "la pescadora", 700, 720, new Color(0x3F8EDC),
                "CHELA: El río tiene su modo. Cuando se enoja, se lo cuenta uno al otro.");
        c.vecino("emiro", "Emiro", "el de los jugos", 1000, 760, new Color(0xE0A030),
                "EMIRO: Ni se acerquen sin jugo. Aquí manda el hambre.");
        c.vecino("daniela", "Daniela", "la de las arepas", 1250, 800, new Color(0x3F8EDC),
                "DANIELA: Cuando quieran arepa, me avisan. Las hago con huevo y con cariño.");
        c.vecino("beatriz", "Doña Beatriz", "la señora", 1600, 900, new Color(0xD9A5EC));
        c.vecino("julian", "Julián", "el muchacho", 450, 980, new Color(0x7A6A5A));
        l.salida(1780, 620, 140, 420, JUNTA, "Subir a la Junta");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "SAMUEL: Salomé quiere ser abogada para que nadie moleste a sus mamás. Primero tienen que ver lo del agua.",
                        "Narrador: En la pared hay un papel pegado: «Hay que limpiar el río. Los vendedores se van.»",
                        "CHELA: Dicen que nos quieren sacar de aquí por el agua.",
                        "EMIRO: Limpio y tranquilo está bien. Lo malo es que «limpio» siempre empieza por sacarnos a nosotros.",
                        "DOÑA BEATRIZ: Uno paga una administración carísima para tener un malecón decente, y lo que encuentra es olor a fritura y bolsas por el piso.",
                        "EMIRO: (seco) Gente. Tengo nombre, Doña Beatriz.",
                        "Narrador: Llega Daniela con su carrito azul y saluda a Chela con un abrazo.",
                        "DOÑA BEATRIZ: (en voz baja, pero sin bajarla lo suficiente) Y ahí, el señor de las arepas…",
                        "Narrador: Daniela se queda quieta.",
                        "DANIELA: Doña Beatriz. Ya se lo dije la otra vez.",
                        "DOÑA BEATRIZ: Perdón, perdón. Es que uno se confunde.",
                        "INFLUENCER: Eso estuvo mal.",
                        "CANDIDATO: Yo creo que fue sin querer.",
                        "CIUDADANO: Pues digámoslo."))
                .paso(sig -> DecisionGrupal.respeto(l, "Doña Beatriz confunde a Daniela con «el señor de las arepas».",
                        new String[]{
                            "Decirle con calma que es Daniela y que merece el mismo respeto.",
                            "Cambiar de tema hacia el río.",
                            "Reírte para que pase."},
                        (j, op) -> {
                            if (op == 0) {
                                respetoA(l, j, 2);
                                l.dialogo(null, comoJugador(j,
                                        "JUGADOR: Doña Beatriz, ella es Daniela. Y merece el mismo respeto que cualquiera de aquí.",
                                        "DOÑA BEATRIZ: Tiene razón. Disculpe, Daniela.",
                                        "DANIELA: (respira) Gracias. Es lo único que pido.",
                                        "DANIELA: El permiso sanitario lo piden con el nombre de la cédula, y esa cédula todavía no tiene mi nombre. Ya tramité el cambio, pero se demora.",
                                        "DANIELA: Entonces: o saco el permiso con un nombre que no es el mío, o vendo sin permiso. ¿Usted cuál escoge?",
                                        "PERIODISTA: Eso es una historia que hay que contar bien, sin ponerle cara.",
                                        "INFLUENCER: Yo no pensé que era así de difícil.",
                                        "CANDIDATO: Eso es un problema de ley. Yo podría moverlo."));
                            } else if (op == 1) {
                                l.dialogo(null, comoJugador(j, "JUGADOR: Y del río, ¿qué decía el papel?",
                                        "Narrador: Daniela baja la mirada y se pone a limpiar su carrito. Emiro frunce el ceño."));
                                l.completar("respeto");
                            } else {
                                l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, -3, Partida.ANIMO, -1), "Respeto: se rió");
                                j.poner("se_rio_ante_la_burla");
                                Actor dn = l.actor("daniela");
                                if (dn != null) {
                                    l.quitar(dn);
                                }
                                l.dialogo(null, comoJugador(j, "JUGADOR: Ja, ja, son cosas de uno.",
                                        "Narrador: Daniela agarra su carrito y se va hacia la esquina. Emiro la mira irse y no dice nada.",
                                        "INFLUENCER: Ay, no.",
                                        "CIUDADANO: Eso se arregla con una disculpa, pero hay que dar la cara."));
                                l.completar("respeto");
                            }
                        },
                        sumados -> {
                            yoTambien(l, sumados);
                            l.completar("respeto");
                            sig.run();
                        }))
                .iniciar());

        l.digital("Grupo del barrio",
                "«Hay que limpiar el malecón. Fuera los puestos. ¿Quién apoya?» (con una foto del carrito de Emiro)",
                "Lleva 52 pulgares arriba.",
                new String[]{
                    "Verificar: reviso quién lo subió y cuándo, y pregunto si Emiro dio permiso.",
                    "Reportar: reporto la publicación por señalar a un vendedor sin su permiso.",
                    "Tomarme un jugo en el muro y dejar de leer los comentarios.",
                    "Guardar silencio: sigo caminando. No es mi pelea.",
                    "Difundir: le doy me gusta y comento que apoyo."},
                new String[]{
                    "{rol} se puso a verificar la publicación del malecón.",
                    "{rol} reportó la publicación del malecón.",
                    "{rol} se tomó un jugo en el muro.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió la publicación del malecón."},
                el -> {
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("Narrador: Se descubre que la cuenta se creó hace dos días.");
                        d.add("CHELA: Preguntar de dónde viene cada cosa es lo que nos falta.");
                        d.add("CIUDADANO: Con una pregunta se ve que la cuenta es nueva.");
                    }
                    if (el.containsValue(Lugar.B)) {
                        d.add("JULIÁN: Era una opinión, no un ataque. Pero bueno, la borro.");
                    }
                    if (el.containsValue(Lugar.C)) {
                        d.add("EMIRO: Este va por la casa. A quien le cuesta, que no lo tome.");
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("EMIRO: (sin enojo, con cansancio) Ya se está regando.");
                        d.add("DANIELA: Eso era de lo que hablábamos hace un rato.");
                        d.add("PERIODISTA: Revisa la fecha del papel antes de apoyar.");
                        d.add("INFLUENCER: No pensé que fuera tan viejo.");
                    }
                    d.add("CHELA: (le entrega siete tarjetas a {INFLUENCER}) Todo está aquí. Póngalo en orden y verá.");
                    l.dialogo(() -> l.minijuegoFijo(Rol.INFLUENCER, new MjFechaDelAgua(el.containsValue(Lugar.A)), res -> {
                        String[] r = res.puntos == Minijuego.BIEN
                                ? new String[]{"INFLUENCER: El episodio del titular ya se cerró hace un año. El papel de hoy lo usa como si fuera de ahora.",
                                    "CHELA: Eso es lo que digo yo."}
                                : res.puntos == Minijuego.REGULAR
                                        ? new String[]{"CHELA: Casi. Revise el orden de las dos del medio."}
                                        : new String[]{"CHELA: Mire, yo se lo explico. Todo eso ya se cerró hace un año."};
                        l.dialogo(() -> {
                            l.completar("minijuego");
                            c.activarPruebas();
                        }, r);
                    }), d.toArray(new String[0]));
                });
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "julian"));

        c.prueba("chela", Rol.CIUDADANO, "tabla_chela", "Tabla de Chela",
                "CIUDADANO: Chela, ¿desde cuándo vende aquí?",
                "CHELA: Desde que tenía quince. Mi papá me trajo con una caña y una tabla.",
                "Narrador: Chela le entrega un trozo de tabla con pintura de barco.");
        c.prueba("chela", Rol.PERIODISTA, "analisis_agua", "Análisis del agua",
                "PERIODISTA: ¿Tiene el análisis del agua?",
                "CHELA: Los dos. Y el papel de cuando cerraron lo viejo.");
        c.prueba("emiro", Rol.INFLUENCER, "pintura_botes", "Pintura de botes",
                "INFLUENCER: ¿Esta pintura de los botes sirve?",
                "CHELA: Azul y blanca. Es para botes, así que aguanta.",
                "Narrador: Pinta: «El río no es un enemigo de quienes viven de él.»");
        c.prueba("chela", Rol.CANDIDATO, "firma_malecon", "Firma de Candidato",
                "CANDIDATO: Voy a ir a ver el alcantarillado. Y quiero que me acompañe quien lo conozca.",
                "CHELA: Yo lo acompaño.",
                "Narrador: Firma al pie de la tabla.");

        l.alCompletar(() -> l.dialogo(() -> l.toast("Sigan hacia la Junta (derecha)", new Color(0xFFD35C)),
                "CHELA: Que esto sea mentira no quiere decir que todo esté bien. El alcantarillado de esta zona lleva años sin mantenimiento.",
                "JULIÁN: Oí que Tomás quiere recuperar el malecón con operativos. Por fin alguien con carácter.",
                "EMIRO: Juan quiere regularizarnos con permisos más baratos. Eso al menos es hablar con nosotros y no de nosotros.",
                "SAMUEL: (a Chela, bajito) ¿Entonces no se va nadie?",
                "CHELA: Por hoy, no.",
                "SAMUEL: Gracias por venir."));
        return l;
    }

    // =====================================================================================
    // 1.4 JUNTA DE ACCION COMUNAL (14:00)
    // =====================================================================================

    private static Lugar junta(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, JUNTA, "Junta de Acción Comunal", Reloj.h(14, 0));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.JUNTA);
        l.caminable(60, 600, 1800, 440);
        l.obstaculo(700, 560, 700, 120, "Mesa larga");
        l.equipoEn(250, 940);

        l.rumor("La Junta se robó la plata del alcantarillado.", 0);
        l.letrero("ESTO ES LA PLATA, Y ESTÁ EN UN PAPEL QUE NADIE HA LEÍDO.");
        l.niebla("La niebla se mete por la escalera angosta. Nadie encuentra el nombre de la tercera carpeta.",
                "DOÑA PETRA: Leer no es lo mismo que comprobar.");

        l.objetivo("nombre", "La señora del tinto");
        l.objetivo("gustavo", "El amigo de Alirio");
        l.objetivo("digital", "La foto de la pared (Q)");
        l.objetivo("minijuego", "El papel que nadie leyó");
        l.objetivo("pruebas", "Las cuatro piezas del Letrero");

        c.vecino("alirio", "Don Alirio", "el señor", 300, 760, new Color(0x6A5ACD),
                "DON ALIRIO: Ustedes que andan con aparatos, ¿no pueden mandar a alguien que nos suba la mesa?");
        c.vecino("gustavo", "Don Gustavo", "el señor", 450, 780, new Color(0x5A7ACD));
        c.vecino("petra", "Doña Petra", "la presidenta", 1000, 720, new Color(0xC9553A),
                "DOÑA PETRA: Leer no es lo mismo que comprobar, Wilson.");
        c.vecino("wilson", "Wilson", "el muchacho", 1200, 760, new Color(0x4C8BD9),
                "WILSON: Yo solo digo lo que leo.");
        c.vecino("amaranta", "Amaranta", "la secretaria", 1500, 720, new Color(0x2B2350));
        Actor sofia = c.vecino("sofia", "La señora del tinto", "la señora", 1700, 880, new Color(0xA0522D));
        sofia.interactuar("Preguntarle el nombre", () -> respetoNombre(l, sofia));
        l.salida(0, 620, 110, 420, PLAZA, "Bajar a la plaza");

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "CANDIDATO: Esta es mi zona. Aquí hay votos.",
                        "PERIODISTA: Mal uso de tus palabras, {CANDIDATO}. Aquí hay gente, no votos.",
                        "CANDIDATO: Es lo mismo.",
                        "CIUDADANO: Yo creo que te equivocas. Y creo que lo vas a ver ahora.",
                        "DON ALIRIO: (en su silla de ruedas, mirando la escalera) Uno llega arriba si alguien lo sube. Dos años pidiendo una rampa, y ahora dicen que nos robamos la plata.",
                        "DON GUSTAVO: Alirio, no te pongas así. Hola, vecinos.",
                        "DON ALIRIO: Es que me da una cosa. Treinta y un años viviendo en esta cuadra con Gustavo, y todavía hay gente que dice «el amigo de Alirio».",
                        "DON GUSTAVO: (sonriendo cansado) Eso ya lo hablamos, mi amor. No estamos aquí por eso.",
                        "WILSON: Doña Petra, en el grupo dicen que la Junta se robó la plata del alcantarillado.",
                        "DOÑA PETRA: (sin alterarse) Eso es grave, Wilson. Y no porque sea cierto. Es grave porque la gente ya está lista para creerlo sin preguntar.",
                        "Narrador: En la cocina, una mujer sirve tinto en vasos de plástico. Nadie le sabe el nombre."))
                .paso(sig -> l.dialogo(sig,
                        "WILSON: Don Alirio, ¿y su amigo Gustavo no viene?",
                        "Narrador: Don Gustavo se detiene. Don Alirio aprieta los brazos de la silla."))
                .paso(sig -> DecisionGrupal.respeto(l, "Wilson llama a Don Gustavo «su amigo».",
                        new String[]{"«Don Gustavo es su pareja desde hace treinta y un años. Está justo ahí.»",
                            "«¿Y la rampa? ¿Cuándo la ponen?»"},
                        (j, op) -> {
                            if (op == 0) {
                                respetoA(l, j, 2);
                                l.dialogo(null, comoJugador(j,
                                        "JUGADOR: Don Gustavo es su pareja desde hace treinta y un años. Está justo ahí.",
                                        "Narrador: Wilson se pone rojo.",
                                        "WILSON: Perdón. No quise… Perdón, Don Gustavo.",
                                        "DON GUSTAVO: (con una sonrisa pequeña) Está bien, Wilson. Al menos ya lo sabe."));
                            } else {
                                l.dialogo(null, comoJugador(j, "JUGADOR: ¿Y la rampa? ¿Cuándo la ponen?",
                                        "Narrador: Don Alirio mira a otro lado."));
                                l.completar("gustavo");
                            }
                        },
                        sumados -> {
                            yoTambien(l, sumados);
                            l.completar("gustavo");
                            l.toast("Alguien sirve tinto en la cocina. ¿Saben cómo se llama?", new Color(0xFFD35C));
                        }))
                .iniciar());

        l.digital("Grupo del barrio",
                "Foto de una pared con la palabra LADRONES escrita con aerosol. «Miren cómo está la Junta.»",
                "La foto corre por el grupo del barrio.",
                new String[]{
                    "Verificar: pregunto quién tomó la foto y cuándo, y si la pared es de la Junta.",
                    "Reportar: reporto la foto por acusar sin pruebas.",
                    "Tomarme un tinto: salgo un momento a la cocina.",
                    "Guardar silencio: no me meto. Ya se las arreglarán.",
                    "Difundir: reenvío la foto a mi grupo."},
                new String[]{
                    "{rol} se puso a verificar la foto de la pared.",
                    "{rol} reportó la foto de la pared.",
                    "{rol} se tomó un tinto.",
                    "{rol} leyó y no dijo nada.",
                    "{rol} difundió la foto de la pared."},
                el -> {
                    List<String> d = new ArrayList<>();
                    if (el.containsValue(Lugar.A)) {
                        d.add("Narrador: La foto es de otra calle y de hace un año.");
                        d.add("DOÑA PETRA: Hoy sí se lo agradezco. Verificar es cuidar.");
                        d.add("CIUDADANO: Con una pregunta la foto se cae sola.");
                    }
                    if (el.containsValue(Lugar.B)) {
                        d.add("WILSON: Yo no acusé. Solo la mostré.");
                    }
                    if (el.containsValue(Lugar.C)) {
                        d.add("SANTA SOFÍA: Con calma, que lo urgente casi nunca es lo importante.");
                    }
                    if (el.containsValue(Lugar.E)) {
                        d.add("DON GUSTAVO: (sin levantar la voz) Ya llegó a mi sobrina. Ella ya me preguntó si es cierto.");
                        d.add("DOÑA PETRA: Eso no se recoge.");
                        d.add("PERIODISTA: ¿La viste completa antes de mandarla?");
                        d.add("INFLUENCER: … Vi la palabra y ya.");
                    }
                    d.add("AMARANTA: (sin levantar la vista, con un guante de lana negro en la mano izquierda) Si busca el acta de la cuenta del alcantarillado, está en la tercera carpeta. Nadie me la ha pedido en dos años.");
                    l.dialogo(() -> l.minijuegoFijo(Rol.CANDIDATO, new MjPapelQueNadieLeyo(el.containsValue(Lugar.A)), res -> {
                        String[] r = res.puntos == Minijuego.BIEN
                                ? new String[]{"CANDIDATO: El dinero no se perdió: está depositado. El contratista existe: figura con factura. Y los dos millones no aparecen en ningún papel.",
                                    "AMARANTA: Y nadie los había buscado. Gracias."}
                                : res.puntos == Minijuego.REGULAR
                                        ? new String[]{"AMARANTA: Casi. Revise la de los dos millones."}
                                        : new String[]{"AMARANTA: Siéntese. Yo se lo explico con el papel en la mano."};
                        l.dialogo(() -> {
                            l.completar("minijuego");
                            c.activarPruebas();
                        }, r);
                    }), d.toArray(new String[0]));
                });
        l.esporadicos("Las mariposas escriben", MjMariposasEscriben::new,
                "Teléfono roto", () -> new MjTelefonoRoto(p.barrio, "wilson"));

        c.prueba("alirio", Rol.CIUDADANO, "hoja_amaranta", "Hoja de Amaranta",
                "CIUDADANO: ¿Cuánto llevan esperando el alcantarillado?",
                "DON ALIRIO: Cuatro años. Y mi silla no llega al segundo piso.",
                "Narrador: Amaranta le entrega una hoja de carpeta con un clip.");
        c.prueba("amaranta", Rol.PERIODISTA, "acta_cuenta", "Acta de la cuenta",
                "PERIODISTA: ¿Puedo ver el acta?",
                "AMARANTA: Es lo que dice la tercera carpeta. Le marqué la fecha de la certificación que falta.");
        c.prueba("amaranta", Rol.INFLUENCER, "pintura_letra_firme", "Pintura de letra firme",
                "INFLUENCER: Va un aviso sencillo.",
                "Narrador: Copia con letra pequeña y firme, como la de Amaranta: «La plata no se perdió. Falta un papel.»");
        c.prueba("amaranta", Rol.CANDIDATO, "firma_junta", "Firma de Candidato",
                "CANDIDATO: Me comprometo a pedir la certificación a la gobernación. Con fecha.",
                "AMARANTA: Fecha, firma y número. Así es que se hace.");

        l.alCompletar(() -> l.dialogo(() -> l.toast("Bajen a la plaza (izquierda)", new Color(0xFFD35C)),
                "AMARANTA: La plata no se perdió en ningún bolsillo. Está represada por un trámite con la gobernación: falta una certificación que la Junta nunca supo comunicar a tiempo.",
                "DON GUSTAVO: Y como nadie lo explicó, el silencio alimentó la sospecha tanto como el rumor.",
                "DOÑA PETRA: Aunque el robo sea mentira, la demora es real. Y la gente tiene derecho a desconfiar cuando nadie rinde cuentas a tiempo.",
                "DON ALIRIO: ¿Y la rampa?",
                "DOÑA PETRA: La rampa va en el acta, Alirio. Esta vez sí queda escrita."));
        return l;
    }

    /** Decision de respeto 1 de la Junta: el nombre de Santa Sofia de la Piedad. */
    private static void respetoNombre(Lugar l, Actor sofia) {
        if (l.partida().bandera("sofia_nombre")) {
            l.dialogo(null, "SANTA SOFÍA: Con calma, que lo urgente casi nunca es lo importante.");
            return;
        }
        l.partida().poner("sofia_nombre");
        Jugador j = l.partida().lider();
        l.dialogo(() -> l.decidirTurno(j, "Santa Sofía de la Piedad sirve el tinto todos los días y nadie le sabe el nombre.",
                Lugar.op("Repetir su nombre completo en voz alta y darle las gracias.", () -> {
                    l.efecto(j, Efecto.de(EstadoJuego.CONVIVENCIA, 2, Partida.ANIMO, 1), "Respeto: el nombre");
                    l.mariposa(j);
                    sofia.nombre("Santa Sofía de la Piedad", "la señora");
                    l.dialogo(() -> l.completar("nombre"), comoJugador(j,
                            "JUGADOR: Santa Sofía de la Piedad. Gracias por el tinto de todos los días.",
                            "Narrador: Santa Sofía se queda quieta. Se limpia las manos en el delantal.",
                            "SANTA SOFÍA: Hace años que nadie lo dice así.",
                            "INFLUENCER: Yo también le había comprado tinto y no le sabía el nombre.",
                            "CIUDADANO: Ahora sí."));
                }),
                Lugar.op("Seguir hacia la mesa sin decir nada.", () -> l.dialogo(() -> l.completar("nombre"),
                        "Narrador: Santa Sofía sirve el tinto con la cabeza baja."))),
                comoJugador(j, "JUGADOR: Disculpe, ¿cómo se llama?",
                        "SANTA SOFÍA: Sofía. Santa Sofía de la Piedad, pero nadie lo dice completo. Aquí todos me dicen «la señora»."));
    }

    // =====================================================================================
    // 1.5 EL APAGON (17:00)  ·  en la plaza (decision pendiente: ver GrafoLocaciones.PLAZA)
    // =====================================================================================

    private static Lugar apagon(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, PLAZA, "El apagón", Reloj.h(17, 0));
        Ctx c = new Ctx(l);
        l.fondo(Rutas.PLAZA_APAGON);
        l.caminable(60, 560, 1800, 480);
        l.equipoEn(900, 940);
        l.objetivo("faroles", "Prender los faroles");
        l.objetivo("rumor", "El rumor de boca en boca");
        c.vecino("ursula", "Doña Úrsula", "la anciana", 300, 700, new Color(0x8A6A9A),
                "DOÑA ÚRSULA: Cuando se cae la luz, la gente sale de la casa y se cuenta las cosas a la cara.");
        c.vecino("mauricio", "Mauricio", "el de la bicicleta", 1400, 760, new Color(0x5BA843),
                "MAURICIO: Yo no sé de dónde sale eso. Yo solo llevo papeles.");
        l.salida(1780, 600, 140, 440, CENA, "Volver a la casa para la cena");

        String[] boca = {"Preguntarle a Mauricio de dónde lo oyó.", "Contárselo al vecino de al lado.",
            "Quedarte en silencio y seguir con el farol."};

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Narrador: El cielo se pone de un color raro. Los cuatro celulares se apagan a la vez. Los faroles titilan.",
                        "MELQUÍADES: (corre con su aparato humeando) ¡Se cayó el imán! ¡Se cayó el imán!",
                        "VLADO: (voz de la tele que se apaga) Perdemos la señal…",
                        "PERIODISTA: Sin señal no puedo verificar nada.",
                        "INFLUENCER: Sin señal no puedo publicar nada. Es lo más raro que me ha pasado.",
                        "CANDIDATO: Sin señal no puedo hablar con mi equipo. Y es la mayor paz que he tenido en semanas.",
                        "CIUDADANO: Pues habrá que preguntar caminando, como se hacía antes.",
                        "DOÑA ÚRSULA: Esto ya lo viví, aunque no sabría decir cuándo. Cuando se cae la luz, la gente sale de la casa y se cuenta las cosas a la cara."))
                .paso(sig -> l.lanzar(new MjFaroles(), res -> {
                    l.efectoCompartido(res.efecto, "Los faroles");
                    l.completar("faroles");
                    sig.run();
                }))
                .paso(sig -> l.dialogo(sig,
                        "Narrador: Mientras prenden los faroles, un rumor corre por la plaza sin celulares.",
                        "VOZ 1: Dicen que la Alcaldía va a despedir a la mitad.",
                        "VOZ 2: Dicen que van a botar a todos los de contrato.",
                        "VOZ 3: Dicen que botan a quien no vote por el alcalde.",
                        "MAURICIO: (pasa en su bicicleta) Yo no sé de dónde sale eso. Yo solo llevo papeles."))
                .paso(sig -> DecisionGrupal.cadaUno(l, "El rumor de boca en boca. ¿Qué haces?", boca,
                        (j, op) -> {
                            if (op == 0) {
                                l.efecto(j, Efecto.de(EstadoJuego.INFO_VERIFICADA, 1), "Boca en boca: preguntó");
                            } else if (op == 1) {
                                l.efecto(j, Efecto.de(EstadoJuego.INFO_VERIFICADA, -1), "Boca en boca: lo contó");
                                if (!p.bandera("rumor_alcaldia_adelantado")) {
                                    p.poner("rumor_alcaldia_adelantado");   // consecuencia cruzada
                                    p.sumarDesinfo(GrafoLocaciones.ALCALDIA, 10);
                                }
                            }
                        },
                        el -> {
                            if (el.containsValue(0)) {
                                l.dialogo(sig, "MAURICIO: Yo oí que son cuarenta y seis contratos que vencen en tres semanas. No sé más.");
                            } else {
                                sig.run();
                            }
                        }))
                .hacer(() -> {
                    l.completar("rumor");
                    l.toast("Vuelve la señal. Hora de la cena.", new Color(0xFFD35C));
                })
                .iniciar());
        return l;
    }

    // =====================================================================================
    // 1.6 LA CENA (19:00) + 1.7 LA NOCHE + EL GIRO
    // =====================================================================================

    private static Lugar cena(Partida p, Lugar.Anfitrion h) {
        Lugar l = new Lugar(p, h, CENA, "La cena", Reloj.h(19, 0));
        l.fondo(Rutas.CASA_CENA);
        l.caminable(80, 600, 1760, 440);
        l.obstaculo(660, 600, 600, 140, "Mesa");
        l.equipoEn(960, 900);
        l.npc("rosa", "Doña Rosa", "la señora", 1500, 680, new Color(0xB5473A));
        l.npc("samuel", "Samuel", "el niño", 600, 700, new Color(0x6FA8DC));

        String animo = p.nivelAnimo();
        String lineaAnimo = animo.equals("alto") ? "DOÑA ROSA: Hoy se les nota. Siguen así."
                : animo.equals("medio") ? "DOÑA ROSA: No estuvo mal. Pudo estar mejor."
                : "DOÑA ROSA: Hoy la casa pesa. Y no es el pan.";

        l.alPrimeraEntrada(() -> new Secuencia()
                .paso(sig -> l.dialogo(sig,
                        "Narrador: La casa, de noche. Doña Rosa pone cuatro tazas de tinto. Afuera llueve un poco.",
                        "DOÑA ROSA: Hoy la gente habló de todo y no se escuchó con nadie. Siéntense. Cuéntense lo que vieron.",
                        "DOÑA ROSA: Aquí no gana el que grita más.",
                        lineaAnimo))
                .paso(sig -> armarLetreros(l, sig))
                .paso(sig -> {
                    p.confianza.desbloquearTodo();   // los bloqueos de ayer ya pasaron
                    GestorPeleas.Pelea pe = GestorPeleas.elegir(p);
                    if (pe != null) {
                        GestorPeleas.jugar(l, pe, sig);
                    } else {
                        GestorPeleas.sinPelea(l, sig);
                    }
                })
                .paso(sig -> {
                    boolean compartieron = true;
                    for (Letrero le : p.letreros()) {
                        compartieron &= le.piezas() == 4;
                    }
                    l.dialogo(sig,
                            "Narrador: Entra Melquíades. Deja un papel doblado junto al tinto y se va sin decir nada.",
                            compartieron ? "Narrador: El papel está escrito de abajo hacia arriba, cortado en cinco pedazos."
                                    : "Narrador: El papel está casi en blanco: solo se ven cinco pedazos sueltos.",
                            "DOÑA ROSA: (mirándolo) Eso también es un mensaje.");
                })
                .paso(sig -> l.minijuegoFijo(Rol.PERIODISTA, new MjPergaminos(), res -> l.dialogo(sig,
                        res.puntos == Minijuego.BIEN
                                ? "PERIODISTA: «Dirá la isla que lo oyó todo y no habrá escuchado nada.»"
                                : "MELQUÍADES: Siempre se me confunde el orden.")))
                .paso(sig -> laNoche(l, sig))
                .paso(sig -> l.dialogo(sig,
                        "Narrador: Antes de dormir, la tele se enciende sola unos segundos.",
                        "VLADO: Perdón por esto. Me acaban de pasar el origen del audio.",
                        "VLADO: Nadie lo grabó en la isla. Entró por el grupo de la panadería desde una cuenta que nadie conoce, y la reenviaron cuarenta veces en una hora.",
                        "CANDIDATO: ¿Cuarenta veces? Eso no pasa solo.",
                        "PERIODISTA: No. Alguien quería que corriera.",
                        "SAMUEL: (baja las escaleras en pijama) Doña Rosa, ¿nos cierran el colegio?",
                        "DOÑA ROSA: No, mijo. Pero hay que estar pendientes.",
                        "Narrador: Los cuatro se quedan mirando la tele apagada.",
                        "Narrador: Día 1 de 2 completado. Empieza el Día 2."))
                .hacer(() -> h.terminarDia())
                .iniciar());
        return l;
    }

    /** Cada jugador decide si pone sus piezas en los Letreros de la mesa. */
    private static void armarLetreros(Lugar l, Runnable fin) {
        Partida p = l.partida();
        DecisionGrupal.cadaUno(l, "En la mesa hay cuatro huecos por Letrero. ¿Pones tus piezas?",
                new String[]{"Poner mis piezas", "Guardármelas"},
                (j, op) -> {
                    if (op == 0) {
                        for (Pieza pz : j.piezas()) {
                            Letrero le = p.letrero(pz.lugar);
                            if (le != null) {
                                le.poner(pz);
                            }
                        }
                        l.burbuja(l.actorDe(j.rol), j.rol.lineaLetrero, 3);
                    } else {
                        j.poner("guardo_pieza");
                        p.chat.escribir(p.reloj.texto(), ChatCasa.NOMBRE, "Hueco vacío: falta pieza");
                    }
                },
                el -> {
                    List<String> d = new ArrayList<>();
                    for (Letrero le : p.letreros()) {
                        d.add("Narrador: Letrero: " + le.texto);
                        switch (le.resultado()) {
                            case IMPECABLE:
                                d.add("Narrador: El Letrero queda impecable. Una mariposa amarilla sale volando por la ventana.");
                                d.add("DOÑA ROSA: Así es que se hace.");
                                break;
                            case CON_HUECO:
                                d.add("Narrador: " + Letrero.textoFalta(le.faltante()));
                                d.add("DOÑA ROSA: Falta algo. Y ya saben qué.");
                                break;
                            default:
                                d.add("DOÑA ROSA: Con eso no se cuelga nada.");
                        }
                    }
                    if (d.isEmpty()) {
                        fin.run();
                    } else {
                        l.dialogo(fin, d.toArray(new String[0]));
                    }
                });
    }

    /** 1.7 La noche: la niebla llega y cada Letrero colgado la frena en su lugar. */
    private static void laNoche(Lugar l, Runnable fin) {
        Partida p = l.partida();
        List<String> d = new ArrayList<>();
        d.add("Narrador: La campana suena una sola vez. Una bruma tibia baja por el río y entra al malecón.");
        d.add("DOÑA ÚRSULA: Ya viene.");
        for (Letrero le : p.letreros()) {
            if (le.sePuedeColgar()) {
                p.sumarDesinfo(le.lugar, -le.reduccion());
            }
            String donde = le.lugar.replace("_", " ");
            if (le.frenaNieblaDelTodo()) {
                p.nieblaOff(le.lugar);
                d.add("Narrador: " + donde + ": la niebla se detiene en la puerta. Una mariposa se posa en el letrero.");
            } else if (le.frenaNiebla()) {
                d.add("Narrador: " + donde + ": la niebla se frena por una noche y vuelve mañana.");
                p.poner("niebla_vuelve_" + le.lugar);
            } else if (le.sePuedeColgar()) {
                d.add("Narrador: " + donde + ": la niebla entra y borra una palabra del letrero.");
                p.nieblaOn(le.lugar);
            } else {
                d.add("Narrador: " + donde + ": sin letrero, la niebla entra. Los vecinos no encuentran el nombre de las cosas.");
                p.nieblaOn(le.lugar);
            }
        }
        if (p.totalMariposas() >= 3) {
            d.add("Narrador: Una bandada pequeña de mariposas se queda bajo el techo de la panadería y alumbra la casa.");
            d.add("SAMUEL: Las mariposas hacen ruido de papel.");
        }
        l.dialogo(fin, d.toArray(new String[0]));
    }
}
