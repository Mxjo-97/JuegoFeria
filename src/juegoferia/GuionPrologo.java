
package juegoferia;

import java.awt.Color;

/**
 * Lo que NO se juega al principio:
 * antesDelMenu(): los 8 cuadros iniciales (con camara, olas, viento, niebla, mariposa, etc.).
 * llegada(): el prologo despues de elegir personajes.
 *
 * El sonido del mar acompaña las escenas de playa
 * y se detiene al cambiar de escenario o saltar.
 *
 * AJUSTE DE EFECTOS: las coordenadas de los efectos (mariposa, rumor, olas, focos) estan en el
 * lienzo 1920x1080 y son una primera estimacion; mueve los numeros hasta que calcen con tus PNG.
 */
public final class GuionPrologo {

    private GuionPrologo() {
    }

    private static final String C = "/imagenes/cinematicas/";
    private static final String MAR = "/sonidos/mar.wav";

    public static Cinematica antesDelMenu() {

        return new Cinematica(null)

                // 01: negro, solo el mar a lo lejos. Bruma y motas azules en la oscuridad.
                .cuadro(C + "01_negro.png",
                        "Pantalla negra. Se oye el mar y, muy lejos, un pito de latón.",
                        "RINGO: Hay una isla que no figura en ningún mapa.")
                .camara(1.02, 1.08, 0, 0)
                .efecto1(Efectos.niebla(0.35))
                .efecto1(Efectos.particulas(new Color(150, 190, 255), 40, 0.3))

                // 02: la isla desde arriba. Camara que avanza sobre el plano, nubes y viento.
                .cuadro(C + "02_isla.png",
                        "La isla desde arriba. Un plano de la ciudad en general.",
                        "RINGO: Se llama Macondo, y en ella todo tiene nombre.",
                        "RINGO: Casi todo.")
                .camara(1.04, 1.16, 30, -14)
                .efecto1(Efectos.niebla(0.5))
                .efecto1(Efectos.viento(10))

                // 03: Melquiades llega con su carreta. Polvo calido y brisa.
                .cuadro(C + "03_gitano.png",
                        "Melquíades baja de una carreta con imanes, lupas y aparatos de latón. Los niños se acercan.",
                        "RINGO: Cada tanto Melquíades, el gitano aventurero, llega con cosas del mundo de afuera.",
                        "RINGO: Un imán, un foco, una lupa. Una idea nueva.")
                .efecto1(Efectos.particulas(new Color(255, 214, 130), 60, 0.25))
                .efecto1(Efectos.viento(8))

                // 04: la niebla del olvido baja por el rio. Niebla muy cerrada.
                .cuadro(C + "04_niebla.png",
                        "Una bruma tibia baja por el río y entra entre las casas. Una mujer mira su taza y no sabe cómo se llama. Un niño señala una vaca.",
                        "RINGO: Pero hay días en que la isla olvida.",
                        "RINGO: No olvida a la gente. Olvida cómo se llaman las cosas y para qué servían.")
                .camara(1.04, 1.13, -36, 10)
                .efecto1(Efectos.niebla(1.0))
                .efecto1(Efectos.viento(12))

                // 05: los letreros. De un letrero nace la mariposa amarilla y se va volando.
                // mariposa(xNace, yNace, xDestino, yDestino, segundosHastaQueNace, segundosDeVuelo, tamano)
                .cuadro(C + "05_mariposas.png",
                        "Un anciano con un pincel pone letreros sobre las cosas: ESTA ES UNA PUERTA. ESTA ES UNA VACA. De un letrero sale volando una mariposa amarilla.",
                        "RINGO: Contra la niebla se hacen letreros.",
                        "RINGO: Y donde alguien trata bien a otro, aparece una mariposa amarilla.")
                .efecto1(Efectos.particulas(new Color(255, 226, 110), 35, 0.5))
                .efecto1(Efectos.mariposa(760, 470, 1450, 170, 3.2, 9.5, 1.0))
                .efecto1(Efectos.mariposa(800, 520, 1180, 110, 5.2, 8.0, 0.6))

                // 06: el rumor. La linea de luz salta de casa en casa y se rompe en pedazos.
                // rumor(segundosHastaQueSale, segundosDeRecorrido, x1, y1, x2, y2, ...casas...)
                .cuadro(C + "06_rumor.png",
                        "Una mujer manda un audio desde un celular de latas y cables. El audio sale como una línea de luz, salta de casa en casa y se rompe en pedazos.",
                        "RINGO: Pero hay algo que viaja más rápido que la niebla.",
                        "RINGO: Un rumor.")
                .camara(1.04, 1.12, 36, -12)
                .efecto1(Efectos.viento(6))
                .efecto1(Efectos.rumor(2.0, 4.2, 360, 640, 700, 470, 1060, 560, 1420, 420))

                // ESCENA DE LA PLAYA: COMIENZA EL SONIDO DEL MAR
                // olas(yMar, yOrilla): el frente de espuma viaja de yMar a yOrilla (ajustalo a tu imagen).
                .cuadro(C + "07_los_cuatro.png",
                        "La playa de noche. Cuatro personas dormidas sobre la arena, cada una con un celular encendido y sin señal.",
                        "RINGO: Una noche, cerca de las elecciones del alcalde local, llegaron cuatro personas que nadie esperaba.",
                        "RINGO: No se conocen entre sí. Y traen en la mano una invención que la isla nunca ha visto.")
                // Se reutiliza el mismo PNG de cada personaje girado para simular que duerme.
                // Asi no necesitamos generar cuatro sprites adicionales de poses acostadas.
                .sprite("/imagenes/personajes/ciudadano.png", 330, 535, 205, 250, 90)
                .sprite("/imagenes/personajes/influencer.png", 650, 535, 205, 250, 90)
                .sprite("/imagenes/personajes/periodista.png", 970, 535, 205, 250, 90)
                .sprite("/imagenes/personajes/candidato.png", 1290, 535, 205, 250, 90)
                .ambiente(MAR)
                .camara(1.05, 1.13, 36, 8)
                .efecto1(Efectos.olas(300, 500))
                .efecto1(Efectos.viento(7))
                .efecto1(Efectos.foco(432, 640, new Color(120, 190, 255), 150, 3.2, 0.8))
                .efecto1(Efectos.foco(752, 640, new Color(120, 190, 255), 150, 3.2, 1.2))
                .efecto1(Efectos.foco(1072, 640, new Color(120, 190, 255), 150, 3.2, 1.6))
                .efecto1(Efectos.foco(1392, 640, new Color(120, 190, 255), 150, 3.2, 2.0))

                // AL CAMBIAR A ESTA ESCENA, EL MAR SE DETIENE
                // 08: los cuatro celulares se encienden a la vez: destello y corte a negro.
                .cuadro(C + "08_senal.png",
                        "Los cuatro celulares se encienden a la vez. Aparece una sola raya de señal. Corte a negro.")
                .camara(1.05, 1.18, 0, -10)
                .efecto1(Efectos.foco(432, 640, new Color(150, 215, 255), 230, 1.6, 0.2))
                .efecto1(Efectos.foco(752, 640, new Color(150, 215, 255), 230, 1.6, 0.2))
                .efecto1(Efectos.foco(1072, 640, new Color(150, 215, 255), 230, 1.6, 0.2))
                .efecto1(Efectos.foco(1392, 640, new Color(150, 215, 255), 230, 1.6, 0.2))
                .efecto1(Efectos.destello(1.6))
                .duracion(4.5)

                .titulo("TODOS HABLAN EN MACONDO",
                        "uso saludable de las redes sociales")
                .efecto1(Efectos.particulas(new Color(255, 211, 92), 80, 0.4))
                .duracion(8);
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
