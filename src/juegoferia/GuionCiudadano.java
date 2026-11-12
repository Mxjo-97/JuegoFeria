package juegoferia;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Escenas NO JUGABLES del rol CIUDADANO, Dia 1 (cinematicas con fondo + dialogos en PNG).
 * Se reproducen con MotorGuion sobre PantallaJuego ANTES de que empiece la parte jugable,
 * que sigue siendo la de partvisual (movimiento, celular, panaderia, decisiones).
 * Cada escena se arma con Escena (ver Escena.java).
 *
 * COMO SE AGREGA UNA ESCENA:
 *   Escena e = new Escena("ciudadano/e0");      // carpeta: src/imagenes/ciudadano/e0/
 *   e.fondo("casa.png");                        // imagen de fondo (1591 x 989)
 *   e.dialogo("samuel_1.png");                  // un dialogo = un PNG transparente tuyo
 *   e.dialogos("a.png", "b.png", "c.png");      // varios dialogos seguidos, en ese orden
 *
 * Todos los dialogos salen en la MISMA posicion (PantallaJuego.ZONA_DIALOGO).
 * Si un PNG no esta en la carpeta, se ve un cuadro provisional con su nombre y resumen() lo marca [FALTA].
 */
public class GuionCiudadano {

    // Duraciones (ms) de las imagenes que pasan solas. Cambialas aqui.
    private static final int T_TARJETA = 2500;   // pantallas negras con texto
    private static final int T_IMAGEN =  1500;    // imagenes de escena sin dialogo

    /** Dev: true = salta las escenas no jugables y entra directo a la parte jugable (para probar rapido). */
    public static boolean SALTAR_ESCENAS = false;

    /**
     * Todas las escenas no jugables, en orden. Cuando terminan, partvisual arranca la parte jugable.
     * Para agregar una escena nueva: crea un metodo como escena0() y anadelo aqui.
     */
    public static List<Paso> escenasNoJugables() {
        List<Paso> g = new ArrayList<>();
        if (!SALTAR_ESCENAS) {
            g.addAll(prologo().build());
            g.addAll(escena0().build());
            // g.addAll(escenaNueva().build());   // <- las proximas escenas no jugables, aqui
        }
        return g;
    }

    /** Imprime en consola el orden de cada escena y las imagenes que faltan. */
    public static void resumenTodo() {
        prologo().resumen();
        escena0().resumen();
    }

       // ===================== PROLOGO: HACE TRES SEMANAS / ACTUALMENTE =====================
    // Imagenes en src/imagenes/ciudadano/prologo/

    public static Escena prologo() {
        Escena e = new Escena("ciudadano/prologo");

        e.fondo("playa.png", T_IMAGEN);       // playa de noche, la chica con el celular
        e.dialogo("2.png");
        e.dialogo("3.png");
        e.dialogo("4.png");
        e.dialogo("5.png");
        e.dialogo("6.png");
     return e;
    }

    // ===================== ESCENA 0: TU CASA, ANTES DE AMANECER =====================
    // Imagenes en src/imagenes/ciudadano/e0/

    public static Escena escena0() {
        Escena e = new Escena("ciudadano/prologo");

        e.fondo("fondo_casa.png",T_IMAGEN);
        e.dialogos("8.png");
        e.dialogos("9.png");
        e.fondo("celular_nubia.png",T_IMAGEN);
        e.dialogos("10.png");
        e.dialogos("11.png");
        e.fondo("fondo_casa.png",T_IMAGEN);
        e.dialogos("12.png");
        e.dialogos("13.png");
        e.dialogos("14.png");
        e.dialogos("15.png");
        e.dialogos("16.png");
        return e;
    }
    
    // ===================== HISTORIA DE LA PANADERÍA =====================

public static Escena escenaPanaderia() {

    Escena e = new Escena("ciudadano/prologo");

    e.fondo("panaderiasin.png");

    e.dialogos(
        "18.png",
        "19.png",
        "20.png"
    );
    
    e.fondo("celular_inicio.png");

e.esperarClic(
    "",
    new Rectangle(660, 535, 105, 100)
);

e.fondo("celular_mensajes.png");

e.esperarClic(
    "",
    new Rectangle(0, 0, 1591, 989)
);
e.fondo("celular_audio.png");
e.dialogos(
        "21.png"
    );
e.fondo("decisiondg1.png");
// ======================================
// RESULTADO 1: VERIFICAR
// ======================================
List<Paso> verificar = new Escena("ciudadano/prologo")
    .dialogos("verificardg.png")
    .build();


// ======================================
// RESULTADO 2: DIFUNDIR
// ======================================
List<Paso> difundir = new Escena("ciudadano/prologo")
    .dialogos("difundirdg.png")
    .build();


// ======================================
// RESULTADO 3: REPORTAR
// ======================================
List<Paso> reportar = new Escena("ciudadano/prologo")
    .dialogos("reportardg.png")
    .build();


// ======================================
// RESULTADO 4: TOMARTE UN TINTO
// ======================================
List<Paso> tinto = new Escena("ciudadano/prologo")
    .dialogos("tintodg.png")
    .build();


// ======================================
// RESULTADO 5: GUARDAR SILENCIO
// ======================================
List<Paso> silencio = new Escena("ciudadano/prologo")
    .dialogos("silenciodg.png")
    .build();


// ======================================
// CINCO ZONAS DE SELECCIÓN
// ======================================
e.seleccionarZonas(

    new Rectangle[] {

        new Rectangle(660, 250, 275, 160), // VERIFICAR

        new Rectangle(360, 405, 280, 160), // DIFUNDIR

        new Rectangle(950, 405, 280, 160), // REPORTAR

        new Rectangle(430, 605, 285, 160), // TINTO

        new Rectangle(875, 605, 285, 160)  // SILENCIO
    },

    verificar,
    difundir,
    reportar,
    tinto,
    silencio
);


// ======================================
// HISTORIA COMÚN
// ======================================
e.fondo("panaderiasin.png");

e.dialogos(
    "21.png",
    "22.png",
    "23.png",
    "24.png",
    "25.png",
    "26.png",
    "27.png"
);
   e.fondo("televisor.png"); 
    e.dialogos(
    "28.png",
    "29.png",
    "30.png"
);
    e.fondo("panaderiasin.png");

e.dialogos(
    "31.png",
    "32.png",
    "33.png",
    "34.png",
    "35.png",
    "36.png",
    "37.png",
    "38.png",
    "39.png",
    "40.png",
    "41.png"
);

e.fondo("decisionrl1.png");

// OPCIÓN 1: RESPONDER CON CALMA
List<Paso> calma = new Escena("ciudadano/prologo")
    .fondo("panaderiasin.png")
    .dialogos("43.png")
    .build();

// OPCIÓN 2: CALLAR Y MIRAR
List<Paso> callar = new Escena("ciudadano/prologo")
    .fondo("panaderiasin.png")
    .dialogos("44.png", "45.png")
    .build();

// OPCIÓN 3: REÍRTE
List<Paso> reir = new Escena("ciudadano/prologo")
    .fondo("panaderiasin.png")
    .dialogos("46.png", "47.png")
    .build();

// TRES ZONAS INVISIBLES
e.seleccionarZonas(

    new Rectangle[] {

        new Rectangle(407, 485, 265, 261), // CALMA

        new Rectangle(672, 490, 260, 261), // CALLAR

        new Rectangle(920, 467, 273, 279)  // REÍR
    },

    calma,
    callar,
    reir
);

// HISTORIA COMÚN
e.fondo("panaderiasin.png");

e.dialogos(
    "48.png",
    "49.png",
    "50.png",
    "51.png",
    "52.png",
    "53.png",
    "54.png",
    "55.png",
    "56.png",
    "57.png",
    "58.png",
    "59.png",
    "60.png",
    "61.png"    
);

    return e;
}
public static Escena escenaMalecon() {

   Escena e = new Escena("ciudadano/malecon");

e.fondo("malecon.png");

// de la 63 a la 93
String[] fotos = new String[93 - 63 + 1];

for (int i = 63; i <= 93; i++) {
    fotos[i - 63] = i + ".png";
}

e.dialogos(fotos);

    return e;
}
}
