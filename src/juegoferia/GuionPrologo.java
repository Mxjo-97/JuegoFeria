
package juegoferia;

/**
 * Lo que NO se juega al principio:
 * antesDelMenu(): los 8 cuadros iniciales.
 * llegada(): el prologo despues de elegir personajes.
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

    /**
     * Prologo: Escena 1 (la playa),
     * Escena 2 (la casa de Dona Rosa),
     * Escena 3 (tres semanas despues).
     */
    public static Cinematica llegada(Partida p) {

        return new Cinematica(p)

                // ---- ESCENA 1: LA PLAYA ----

                .cuadro(C + "p1_playa.png",
                        "De noche. Los cuatro despiertan en la arena, a unos pasos uno del otro. El celular no tiene señal. Se oye el mar.",
                        "CANDIDATO: ¿Dónde estamos?",
                        "PERIODISTA: No sé. Pero esto no es mi barrio.",
                        "INFLUENCER: ¡Y no hay señal! ¿Alguien tiene señal?",
                        "CIUDADANO: Por aquí debe de haber gente. Vamos.")
                // Sprites independientes. Los cuatro roles usan el mismo formato de personaje.
                .sprite("/imagenes/personajes/ciudadano/quieto.png", 330, 465, 205, 250)
                .sprite("/imagenes/personajes/influencer/quieto.png", 650, 465, 205, 250)
                .sprite("/imagenes/personajes/periodista.png", 970, 465, 205, 250)
                .sprite("/imagenes/personajes/candidato.png", 1290, 465, 205, 250)
                .ambiente(MAR)

                .cuadro(C + "p1_melquiades.png",
                        "Suena un pito de latón. Melquíades aparece con su aparato de latas, alambres y un imán atado con cabuya.",
                        "MELQUÍADES: Con esto a lo mejor sus aparatos vuelven a hablar.",
                        "MELQUÍADES: Pero solo en cuatro lugares: la panadería, el malecón, la Junta y la Alcaldía. En ningún otro sitio.")
                // Melquiades utiliza un unico sprite estatico; no necesita animacion de caminar.
                .sprite("/imagenes/personajes/melquiades.png", 1430, 405, 220, 270)

                .cuadro(C + "p1_senal.png",
                        "En cada celular aparece una raya de señal. Desde las casas, el pueblo los mira como si hubieran bajado del cielo.",
                        "MELQUÍADES: Y les doy un consejo: aquí las palabras corren más que los pies.")

                .cuadro(C + "p1_rosa.png",
                        "Llega Doña Rosa con un farol y una canasta de pan.",
                        "DOÑA ROSA: Ya, ya. No se queden ahí mirando a la nada. Ustedes cuatro se vienen conmigo.",
                        "DOÑA ROSA: Aquí no se duerme en la arena.")
                // Dona Rosa utiliza un unico sprite estatico; no necesita animacion de caminar.
                .sprite("/imagenes/personajes/dona_rosa.png", 1430, 405, 230, 280)

                // ---- ESCENA 2: CASA DE DOÑA ROSA ----

                .cuadro(C + "p2_casa.png",
                        "La casa de huéspedes, sobre la panadería. Cuatro cuartos, una cocina y una mesa grande con cuatro sillas. Doña Rosa reparte cobijas.",
                        "DOÑA ROSA: Cuatro cuartos, uno para cada quien. Escojan sin pelear.",
                        "DOÑA ROSA: Abajo está la panadería. Si quieren pan, me avisan antes de las siete.")

                .cuadro(C + "p2_pasillo.png",
                        "Los cuatro se quedan en el pasillo, sin saber qué decir.",
                        "CIUDADANO: Yo buscaré un trabajo. Si necesitan algo, avísenme temprano.",
                        "INFLUENCER: Yo hago videos. Si alguien encuentra señal, que me diga dónde.",
                        "PERIODISTA: Yo escribo para un diario. Aunque ahora no sé si este lugar tenga uno. Soy todo oídos por si me quieren comentar algo, pero que sea verdad.",
                        "CANDIDATO: Yo quiero lanzarme a la alcaldía. Vi un cartel por ahí de que hay elecciones en tres semanas.")

                .cuadro(C + "p2_samuel.png",
                        "Se miran. Cada quien entra a su cuarto. Samuel, el nieto de Doña Rosa, asoma desde la cocina con los ojos grandes.",
                        "SAMUEL: ¿Son de verdad de la tele?",
                        "DOÑA ROSA: Samuel, a dormir.",
                        "SAMUEL: Es que nunca había visto a alguien que viniera de afuera.",
                        "DOÑA ROSA: Mañana les preguntas. Hoy, a dormir.")

                // ---- ESCENA 3: TRES SEMANAS DESPUÉS ----

                .cuadro(C + "p3_calendario.png",
                        "Hojas de calendario que caen una por una sobre la mesa de la casa.")

                .cuadro(C + "p3_ciudadano.png",
                        "{CIUDADANO} llega de su turno con una bolsa de pan y la pasa a los demás.")

                .cuadro(C + "p3_influencer.png",
                        "{INFLUENCER} filma el malecón con el celular en alto. Unos niños se asoman a la cámara.")

                .cuadro(C + "p3_periodista.png",
                        "{PERIODISTA} escribe en una libreta mientras Doña Úrsula le cuenta algo al oído.")

                .cuadro(C + "p3_candidato.png",
                        "{CANDIDATO} pega un afiche en un poste: «Una candidatura independiente. Con ustedes.» Doña Rosa lo mira con las manos en la cintura.",
                        "RINGO: Pasaron tres semanas. Los cuatro se quedaron.",
                        "RINGO: Llegaron justo cuando la isla se preparaba para elegir alcalde, y desde entonces comparten techo, pan y celular.")

                .cuadro(C + "p3_tele.png",
                        "Se enciende la tele del salón. Aparece Vlado.",
                        "VLADO: Faltan dos días para la elección, Macondo. Hay varias candidaturas, y una es nueva: una candidatura independiente que se inscribió hace tres semanas.")

                .cuadro(C + "p3_tele.png",
                        "Los otros tres miran a {CANDIDATO}.",
                        "INFLUENCER: Tres semanas y todavía me sorprende ver tu cara en un afiche. Luces bien, {CANDIDATO}.",
                        "CANDIDATO: A mí también.")

                .titulo("7:00", "Día 1 de 2. Faltan dos días para la elección.");
    }
}
