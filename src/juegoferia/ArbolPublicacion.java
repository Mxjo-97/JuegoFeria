package juegoferia;

public class ArbolPublicacion {
    public static Nodo construirArbolEscena1() {

        // --- Raíz y sus 4 hijos directos ---
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

        // --- Hojas de Verificar ---
        Nodo verificarReportar = new Nodo("pub1_verificar_reportar", "hoja", "Reportar (ya verificado)");
        verificarReportar.efecto.agregar("confianza", 8);
        verificarReportar.efecto.agregar("info_verificada", 5);
        verificarReportar.efecto.agregar("desinformacion", -3);

        Nodo verificarIgnorar = new Nodo("pub1_verificar_ignorar", "hoja", "Tinto/calma (ya verificado)");
        verificarIgnorar.efecto.agregar("info_verificada", 3);

        verificar.agregarHijo(verificarReportar);
        verificar.agregarHijo(verificarIgnorar);

        // --- Hojas de Difundir ---
        Nodo difundirViral = new Nodo("pub1_difundir_viral", "hoja", "Se vuelve viral");
        difundirViral.efecto.agregar("desinformacion", 12);
        difundirViral.efecto.agregar("confianza", -6);
        difundirViral.efecto.agregar("conflictos", 4);

        Nodo difundirNoViral = new Nodo("pub1_difundir_noviral", "hoja", "No se vuelve viral");
        difundirNoViral.efecto.agregar("desinformacion", -2);

        difundir.agregarHijo(difundirViral);
        difundir.agregarHijo(difundirNoViral);

        // --- Hojas de Reportar ---
        Nodo reportarAcerto = new Nodo("pub1_reportar_acerto", "hoja", "Acertó");
        reportarAcerto.efecto.agregar("confianza", 5);
        reportarAcerto.efecto.agregar("info_verificada", 2);
        reportarAcerto.coleccionable = new Coleccionable("El rumor del cierre del colegio", "educacion");

        Nodo reportarSeEquivoco = new Nodo("pub1_reportar_equivoco", "hoja", "Se equivocó");
        reportarSeEquivoco.efecto.agregar("reputacion_jugador", -4);

        reportar.agregarHijo(reportarAcerto);
        reportar.agregarHijo(reportarSeEquivoco);

        // --- Se devuelve la raíz, con todo el árbol ya colgado de ella ---
        return raiz;
    }
}
