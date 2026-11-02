package juegoferia;

import java.util.*;

public class ArbolPublicacion {

    public static Map<String, Nodo> raicesActivas = new HashMap<>();

    public static void insertarSubArbol(String isla, Nodo nuevaRaiz) {
        raicesActivas.put(isla, nuevaRaiz);
        System.out.println("Se insertó una nueva publicación en: " + isla);
    }

    public static void eliminarSubArbol(String isla) {
        if (raicesActivas.containsKey(isla)) {
            raicesActivas.remove(isla);
            System.out.println("Se eliminó la publicación activa de: " + isla);
        } else {
            System.out.println("No había ninguna publicación activa en: " + isla);
        }
        
    }


    public static Nodo construirArbolEscena1() {

        // raíz y sus 4 hijos directos
        Nodo raiz = new Nodo("pub1_raiz", "decision", "El candidato Juan quiere cerrar el colegio");
        Nodo verificar = new Nodo("pub1_verificar", "decision", "Verificar");
        Nodo difundir  = new Nodo("pub1_difundir", "decision", "Difundir");
        Nodo reportar  = new Nodo("pub1_reportar", "decision", "Reportar");
        Nodo ignorar   = new Nodo("pub1_ignorar", "hoja", "Tinto/calma");
        ignorar.efecto.agregar("neutro", 0);

        raiz.agregarHijo(verificar);
        raiz.agregarHijo(difundir);
        raiz.agregarHijo(reportar);
        raiz.agregarHijo(ignorar);

        // hojas de Verificar
        Nodo verificarReportar = new Nodo("pub1_verificar_reportar", "hoja", "Reportar (ya verificado)");
        verificarReportar.efecto.agregar("confianza", 8);
        verificarReportar.efecto.agregar("info_verificada", 5);
        verificarReportar.efecto.agregar("desinformacion", -3);

        Nodo verificarIgnorar = new Nodo("pub1_verificar_ignorar", "hoja", "Tinto/calma (ya verificado)");
        verificarIgnorar.efecto.agregar("info_verificada", 3);

        verificar.agregarHijo(verificarReportar);
        verificar.agregarHijo(verificarIgnorar);

        // hojas de Difundir
        Nodo difundirViral = new Nodo("pub1_difundir_viral", "hoja", "Se vuelve viral");
        difundirViral.efecto.agregar("desinformacion", 12);
        difundirViral.efecto.agregar("confianza", -6);
        difundirViral.efecto.agregar("conflictos", 4);

        Nodo difundirNoViral = new Nodo("pub1_difundir_noviral", "hoja", "No se vuelve viral");
        difundirNoViral.efecto.agregar("desinformacion", -2);

        difundir.agregarHijo(difundirViral);
        difundir.agregarHijo(difundirNoViral);

        // hojas de reportar
        Nodo reportarAcerto = new Nodo("pub1_reportar_acerto", "hoja", "Acertó");
        reportarAcerto.efecto.agregar("confianza", 5);
        reportarAcerto.efecto.agregar("info_verificada", 2);
        reportarAcerto.coleccionable = new Coleccionable("El rumor del cierre del colegio", "educacion");

        Nodo reportarSeEquivoco = new Nodo("pub1_reportar_equivoco", "hoja", "Se equivocó");
        reportarSeEquivoco.efecto.agregar("reputacion_jugador", -4);

        reportar.agregarHijo(reportarAcerto);
        reportar.agregarHijo(reportarSeEquivoco);

        // Se devuelve la raíz, con todo el árbol abajo de ella 
        return raiz;
    }

    public static Nodo construirArbolEscena2() {
    Nodo raiz = new Nodo("pub2_raiz", "decision", "Nueva biblioteca en el colegio");

    Nodo verificar = new Nodo("pub2_verificar", "decision", "Verificar");
    Nodo difundir = new Nodo("pub2_difundir", "hoja", "Difundir sin verificar");
    // "mismo efecto positivo, pero con riesgo aleatorio: -2 confianza"
    // Se elige una de las dos al azar (50/50) cada vez que se aplica.
    difundir.efecto.agregarAlternativa(new Efecto().agregar("confianza", 6).agregar("convivencia", 4));
    difundir.efecto.agregarAlternativa(new Efecto().agregar("confianza", -2));

    Nodo reportar = new Nodo("pub2_reportar", "hoja", "Reportar");
    reportar.efecto.agregar("reputacion_jugador", -4);

    Nodo ignorar = new Nodo("pub2_ignorar", "hoja", "Tinto/calma");
    ignorar.efecto.agregar("neutro", 0);

    raiz.agregarHijo(verificar);
    raiz.agregarHijo(difundir);
    raiz.agregarHijo(reportar);
    raiz.agregarHijo(ignorar);

    Nodo compartirInformado = new Nodo("pub2_verificar_compartir", "hoja", "Compartir informado");
    compartirInformado.efecto.agregar("confianza", 6);
    compartirInformado.efecto.agregar("convivencia", 4);

    Nodo tintoVerificado = new Nodo("pub2_verificar_ignorar", "hoja", "Tinto/calma (ya verificado)");
    tintoVerificado.efecto.agregar("neutro", 0);

    verificar.agregarHijo(compartirInformado);
    verificar.agregarHijo(tintoVerificado);

    return raiz;
}

public static Nodo construirArbolEscena3() {
    Nodo raiz = new Nodo("pub3_raiz", "decision", "Yo creo que la alcaldesa Marta no se preocupa por los barrios");

    Nodo verificar = new Nodo("pub3_verificar", "hoja", "Comentar con respeto");
    verificar.efecto.agregar("convivencia", 2);

    Nodo difundir = new Nodo("pub3_difundir", "hoja", "Difundir (genera debate)");
    // Guion: +-3 conflictos al azar (pleito de comentarios o discusion sana).
    difundir.efecto.agregarAlternativa(new Efecto().agregar("conflictos", 3));
    difundir.efecto.agregarAlternativa(new Efecto().agregar("conflictos", -3));

    Nodo reportar = new Nodo("pub3_reportar", "hoja", "Reportar (censura)");
    reportar.efecto.agregar("reputacion_jugador", -3);

    Nodo ignorar = new Nodo("pub3_ignorar", "hoja", "Tinto/calma");
    ignorar.efecto.agregar("neutro", 0);

    raiz.agregarHijo(verificar);
    raiz.agregarHijo(difundir);
    raiz.agregarHijo(reportar);
    raiz.agregarHijo(ignorar);

    return raiz;
}

public static Nodo construirArbolEscena4() {
    Nodo raiz = construirArbolEscena1();
    raiz.id = "pub4_raiz";
    raiz.descripcion = "INFORMACIÓN VIRAL: el rumor del colegio se está compartiendo (10 segundos)";

    Nodo tiempoAgotado = new Nodo("pub4_tiempo_agotado", "hoja", "Tiempo agotado (no eligió)");
    tiempoAgotado.efecto.agregar("desinformacion", 7);

    raiz.agregarHijo(tiempoAgotado);

    return raiz;
}
}
