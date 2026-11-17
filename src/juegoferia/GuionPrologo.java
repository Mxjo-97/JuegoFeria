
package juegoferia;

/**
 * Lo que NO se juega al principio:
 * antesDelMenu(): los 8 cuadros iniciales.
 * llegada(): el prologo CORTO despues de elegir personajes (5 cuadros, sin cinematicas nuevas).
 *
 * El sonido del mar acompaña las escenas de playa
 * y se detiene al cambiar de escenario o saltar.
 */
public final class GuionPrologo {

    private GuionPrologo() {
    }

    private static final String C = "/imagenes/cinematicas/";
    private static final String MAR = "/sonidos/mar.wav";
    private static final String SND = "/sonidos/";

    // ---- AJUSTES DE LA ESCENA DE LA PLAYA (mueve estos numeros si algo no calza con tu fondo) ----
    /** Y de la orilla: el mar queda arriba de esta linea y la arena abajo. */
    private static final int ORILLA_Y = 470;
    /** Donde sale la mariposa (el letrero del anciano) y a donde vuela. */
    private static final int MARIPOSA_X0 = 820, MARIPOSA_Y0 = 470, MARIPOSA_X1 = 1500, MARIPOSA_Y1 = 190;
    /** Centros de los celulares de los cuatro dormidos (x, y). */
    private static final int[] CELULARES = {512, 700, 832, 700, 1152, 700, 1472, 700};

    public static Cinematica antesDelMenu() {

        return new Cinematica(null)
                .cine()   // look de pelicula: fundidos, vineta, grano, camara y efectos

                // 1. Negro: estrellas que aparecen de a poco y el mar de fondo
                .cuadro(C + "01_negro.png",
                        "Pantalla negra. Se oye el mar y, muy lejos, un pito de latón.",
                        "RINGO: Hay una isla que no figura en ningún mapa.")
                .estrellas()
                .camara(1.00, 1.10, 0, 0)
                .ambiente(MAR)
                .efectoSonido(SND + "pito_laton.wav", 3.2, 0.5, 0)       // pito lejano de latón

                // 2. Toma aerea: nubes cruzando y la camara baja hacia la isla
                .cuadro(C + "02_isla.png",
                        "La isla desde arriba. Un plano de la ciudad en general.",
                        "RINGO: Se llama Macondo, y en ella todo tiene nombre.",
                        "RINGO: Casi todo.")
                .camara(1.03, 1.24, 0, -70)
                .nubes()
                .viento(0.5)
                .musica(SND + "musica_isla.wav", 0.65, 2.5)               // entra la música al ver la isla

                // 3. El gitano: paneo lateral, polvo del camino y luz dorada
                .cuadro(C + "03_gitano.png",
                        "Melquíades baja de una carreta con imanes, lupas y aparatos de latón. Los niños se acercan.",
                        "RINGO: Cada tanto Melquíades, el gitano aventurero, llega con cosas del mundo de afuera.",
                        "RINGO: Un imán, un foco, una lupa. Una idea nueva.")
                .camara(1.16, 1.16, -130, 0)
                .rayos(0xFFE0A0)
                .polvo(0xFFD78A, 55)
                .viento(0.8)
                .efectoSonido(SND + "carreta.wav", 0.3, 0.8, 6)           // carreta y aparatos, máx. 6 s

                // 4. La niebla: se va espesando mientras la camara avanza
                .cuadro(C + "04_niebla.png",
                        "Una bruma tibia baja por el río y entra entre las casas. Una mujer mira su taza y no sabe cómo se llama. Un niño señala una vaca.",
                        "RINGO: Pero hay días en que la isla olvida.",
                        "RINGO: No olvida a la gente. Olvida cómo se llaman las cosas y para qué servían.")
                .camara(1.00, 1.16, 0, 0)
                .niebla()
                .viento(0.4)
                .efectoSonido(SND + "viento_niebla.wav", 0.5, 0.5, 0)

                // 5. La mariposa amarilla sale del letrero y vuela
                .cuadro(C + "05_mariposas.png",
                        "Un anciano con un pincel pone letreros sobre las cosas: ESTA ES UNA PUERTA. ESTA ES UNA VACA. De un letrero sale volando una mariposa amarilla.",
                        "RINGO: Contra la niebla se hacen letreros.",
                        "RINGO: Y donde alguien trata bien a otro, aparece una mariposa amarilla.")
                .camara(1.04, 1.14, 60, 0)
                .polvo(0xFFE08A, 40)
                .mariposa(MARIPOSA_X0, MARIPOSA_Y0, MARIPOSA_X1, MARIPOSA_Y1)
                .efectoSonido(SND + "mariposa.wav", 0.9, 0.8, 0)          // sale justo cuando despega

                // 6. El rumor: la linea de luz salta de casa en casa y se rompe
                .cuadro(C + "06_rumor.png",
                        "Una mujer manda un audio desde un celular de latas y cables. El audio sale como una línea de luz, salta de casa en casa y se rompe en pedazos.",
                        "RINGO: Pero hay algo que viaja más rápido que la niebla.",
                        "RINGO: Un rumor.")
                .camara(1.00, 1.10, -50, 0)
                .viento(0.6)
                .rumor(520, 520, 860, 430, 1210, 540, 1560, 410)
                .efectoSonido(SND + "rumor_saltos.wav", 1.0, 0.8, 0)      // los tres saltos de la linea de luz
                .efectoSonido(SND + "rumor_rompe.wav", 4.15, 0.9, 0)      // cuando se rompe en pedazos

                // ESCENA DE LA PLAYA: olas, viento, estrellas, celulares brillando y gente respirando
                .cuadro(C + "07_los_cuatro.png",
                        "La playa de noche. Cuatro personas dormidas sobre la arena, cada una con un celular encendido y sin señal.",
                        "RINGO: Una noche, cerca de las elecciones del alcalde local, llegaron cuatro personas que nadie esperaba.",
                        "RINGO: No se conocen entre sí. Y traen en la mano una invención que la isla nunca ha visto.")
                // Sprites ya acostados (sin rotar). Caja horizontal 260x180: si se ven estirados, cambia ancho/alto.
                .sprite("/imagenes/personajes/ciudadano/acostado.png", 300, 570, 260, 180).respirar()
                .sprite("/imagenes/personajes/influencer/acostado.png", 620, 570, 260, 180).respirar()
                .sprite("/imagenes/personajes/periodista/acostado.png", 940, 570, 260, 180).respirar()
                .sprite("/imagenes/personajes/candidato/acostado.png", 1260, 570, 260, 180).respirar()
                .estrellas()
                .olas(ORILLA_Y)
                .resplandor(CELULARES[0], CELULARES[1], 110, 0x8FE3FF, 0.0)
                .resplandor(CELULARES[2], CELULARES[3], 110, 0x8FE3FF, 1.3)
                .resplandor(CELULARES[4], CELULARES[5], 110, 0x8FE3FF, 2.6)
                .resplandor(CELULARES[6], CELULARES[7], 110, 0x8FE3FF, 3.9)
                .viento(0.7)
                .camara(1.00, 1.12, -70, 0)
                .ambiente(MAR)
                .silencio(2.0)                                            // la música se apaga y queda solo el mar

                // AL CAMBIAR A ESTA ESCENA, EL MAR SE DETIENE
                // Los cuatro celulares se encienden a la vez, la luz se junta en UNA raya de senal y corte a negro.
                .cuadro(C + "08_senal.png",
                        "Los cuatro celulares se encienden a la vez. Aparece una sola raya de señal. Corte a negro.")
                .senal(960, 380, CELULARES)
                .efectoSonido(SND + "celulares_on.wav", 0.4, 0.8, 0)      // los cuatro se encienden
                .efectoSonido(SND + "senal.wav", 2.7, 0.9, 0)             // aparece la raya de señal
                .camara(1.00, 1.08, 0, 0)
                .duracion(6.2)

                .titulo("TODOS HABLAN EN MACONDO",
                        "uso saludable de las redes sociales")
                .musica(SND + "musica_titulo.wav", 0.8, 0.8);
    }

    /** Fondos que ya existen en el proyecto (no hacen falta cinematicas nuevas). */
    private static final String PLAYA = C + "07_los_cuatro.png";      // la misma playa de la intro
    private static final String CASA = "/imagenes/casapuertasin.png";
    private static final String MALECON = "/imagenes/malecon.png";
    private static final String PANADERIA = "/imagenes/panaderia.png"; // la tele ya dice "Día 1 de 2"

    /** Los cuatro de pie (solo los principales tienen animaciones; aqui basta "quieto"). */
    private static Cinematica losCuatro(Cinematica c, int x0, int y, int paso) {
        String[] roles = {"ciudadano", "influencer", "periodista", "candidato"};
        for (int i = 0; i < 4; i++) {
            c.sprite("/imagenes/personajes/" + roles[i] + "/quieto.png", x0 + i * paso, y + (i % 2) * 20, 205, 250);
        }
        return c;
    }

    /** NPC con su unico sprite estatico (/imagenes/personaje/<Nombre>/<Nombre>.png). */
    private static Cinematica npc(Cinematica c, String id, int x, int y, int ancho, int alto) {
        return c.sprite(Rutas.spriteVecino(id), x, y, ancho, alto);
    }

    /**
     * Prologo CORTO (5 cuadros, antes eran 15): la playa, Doña Rosa, la casa, tres semanas despues y la tele.
     * Solo usa la playa de la intro y tres fondos del juego. Nadie camina: los principales salen "quieto"
     * y Melquiades, Doña Rosa y Samuel con su sprite estatico.
     */
    public static Cinematica llegada(Partida p) {

        Cinematica c = new Cinematica(p);

        // ---- 1. LA PLAYA: despiertan y llega Melquiades ----
        c.cuadro(PLAYA,
                "De noche. Los cuatro despiertan en la arena, sin señal. Suena un pito de latón: Melquíades llega con un aparato de latas, alambres y un imán.",
                "CANDIDATO: ¿Dónde estamos?",
                "PERIODISTA: No sé. Pero esto no es mi barrio.",
                "INFLUENCER: ¿Alguien tiene señal?",
                "MELQUÍADES: Con esto sus aparatos vuelven a hablar. Pero solo en cuatro lugares: la panadería, el malecón, la Junta y la Alcaldía.",
                "MELQUÍADES: Y un consejo: aquí las palabras corren más que los pies.")
                .ambiente(MAR);
        losCuatro(c, 200, 500, 300);
        npc(c, "melquiades", 1480, 440, 220, 270);

        // ---- 2. LA PLAYA: llega Doña Rosa ----
        c.cuadro(PLAYA,
                "En cada celular aparece una raya de señal. Llega Doña Rosa con un farol y una canasta de pan.",
                "CIUDADANO: Por aquí debe de haber gente...",
                "DOÑA ROSA: Ya, ya. No se queden ahí mirando a la nada. Ustedes cuatro se vienen conmigo. Aquí no se duerme en la arena.");
        losCuatro(c, 200, 500, 300);
        npc(c, "rosa", 1480, 430, 230, 280);

        // ---- 3. LA CASA DE DOÑA ROSA ----
        c.cuadro(CASA,
                "La casa de huéspedes, sobre la panadería. Doña Rosa reparte cobijas. Samuel, su nieto, se asoma con los ojos grandes.",
                "DOÑA ROSA: Cuatro cuartos, uno para cada quien. Si quieren pan, me avisan antes de las siete.",
                "CIUDADANO: Yo buscaré trabajo. Si necesitan algo, avísenme temprano.",
                "INFLUENCER: Yo hago videos. Si alguien encuentra señal, que me diga dónde.",
                "PERIODISTA: Yo escribo para un diario. Si me cuentan algo, que sea verdad.",
                "CANDIDATO: Yo quiero lanzarme a la alcaldía. Vi un cartel: hay elecciones en tres semanas.",
                "SAMUEL: ¿Son de verdad de la tele?",
                "DOÑA ROSA: Samuel, a dormir. Mañana les preguntas.");
        losCuatro(c, 280, 520, 240);
        npc(c, "rosa", 1300, 470, 230, 280);
        npc(c, "samuel", 1580, 540, 180, 220);

        // ---- 4. TRES SEMANAS DESPUES ----
        c.cuadro(MALECON,
                "Pasan tres semanas. {CIUDADANO} trae pan de su turno, {INFLUENCER} filma el malecón, {PERIODISTA} anota lo que le cuenta Doña Úrsula y {CANDIDATO} pega afiches: «Una candidatura independiente. Con ustedes.»",
                "RINGO: Pasaron tres semanas. Los cuatro se quedaron.",
                "RINGO: Llegaron justo cuando la isla se preparaba para elegir alcalde, y desde entonces comparten techo, pan y celular.");
        losCuatro(c, 480, 520, 260);

        // ---- 5. LA TELE DE LA PANADERIA ----
        c.cuadro(PANADERIA,
                "Se enciende la tele de la panadería. Aparece Vlado.",
                "VLADO: Faltan dos días para la elección, Macondo. Y hay una candidatura nueva: una independiente que se inscribió hace tres semanas.",
                "INFLUENCER: Tres semanas y todavía me sorprende ver tu cara en un afiche. Luces bien, {CANDIDATO}.",
                "CANDIDATO: A mí también.");
        losCuatro(c, 380, 500, 240);

        return c.titulo("7:00", "Día 1 de 2. Faltan dos días para la elección.");
    }
}
