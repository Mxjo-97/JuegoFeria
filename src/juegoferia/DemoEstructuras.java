package juegoferia;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Demo en CONSOLA de las estructuras de datos del juego (para la sustentacion de ED2).
 * No abre ventanas: imprime los grafos, el arbol de reenvios y una partida de prueba.
 */
public class DemoEstructuras {

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        System.out.println("=========== 1. GRAFO DE LOCACIONES (con peso = minutos) ===========");
        GrafoLocaciones mapa = GrafoLocaciones.crearMapaIsla();
        mapa.imprimir();
        List<String> c = mapa.caminoMasRapido(GrafoLocaciones.MALECON, GrafoLocaciones.ALCALDIA);
        System.out.println("Dijkstra Malecón -> Alcaldía: " + c + " = " + mapa.distanciaTotal(c) + " min");
        System.out.println("BFS (menos saltos) Malecón -> Alcaldía: "
                + mapa.caminoMasCorto(GrafoLocaciones.MALECON, GrafoLocaciones.ALCALDIA));
        System.out.println("DFS desde la Casa: " + mapa.recorridoDFS(GrafoLocaciones.CASA));

        System.out.println("\n=========== 2. GRAFO DE RELACIONES DEL BARRIO ===========");
        GrafoRelaciones barrio = GrafoRelaciones.crearBarrioMacondo();
        System.out.println("Vecinos: " + barrio.cantidadCiudadanos());
        String clave = barrio.masConectado();
        System.out.println("Más conectado (habilidad del Ciudadano): " + clave + " con " + barrio.grado(clave) + " contactos");
        System.out.println("BFS desde Nubia: " + barrio.recorridoBFS("nubia"));
        System.out.println("A cuántos llega un audio de Nubia en 2 reenvíos: " + barrio.alcanceEn("nubia", 2));
        System.out.println("Camino Nubia -> Daniela: " + barrio.caminoMasCorto("nubia", "daniela"));

        System.out.println("\n=========== 3. ÁRBOL DE REENVÍOS (Rastrea el audio) ===========");
        Nodo raiz = MjRastreaAudio.arbolDelAudio();
        System.out.println("BFS por niveles:");
        for (Nodo n : Recorridos.recorridoBFS(raiz)) {
            System.out.println("  nivel " + n.nivel() + "  " + n);
        }
        System.out.println("Altura: " + Recorridos.altura(raiz) + "   Reenvíos: " + Recorridos.reenvios(raiz)
                + "   Hojas: " + Recorridos.hojas(raiz).size());
        for (Nodo n : Recorridos.recorridoDFS(raiz)) {
            if (n.id.equals("rosa")) {
                System.out.println("Camino del celular de Doña Rosa al origen:");
                for (Nodo k : n.caminoALaRaiz()) {
                    System.out.println("  <- " + k);
                }
            }
        }

        System.out.println("\n=========== 4. CONFIANZA ENTRE ROLES (grafo completo con peso) ===========");
        Partida p = new Partida();
        p.agregarJugador("Ana", Rol.CIUDADANO);
        p.agregarJugador("Beto", Rol.PERIODISTA);
        p.agregarJugador("Caro", Rol.INFLUENCER);
        p.agregarJugador("Dani", Rol.CANDIDATO);
        p.prestar(Rol.PERIODISTA, Rol.INFLUENCER);
        p.confianza.bloquear(Rol.CANDIDATO, Rol.CIUDADANO, true);
        p.confianza.imprimir();

        System.out.println("\n=========== 5. UNA DECISIÓN DIGITAL DE PRUEBA ===========");
        String[] nombres = Lugar.NOMBRE_DIGITAL;
        int[] eleccion = {Lugar.A, Lugar.A, Lugar.E, Lugar.C};
        int i = 0;
        for (Jugador j : p.jugadores()) {
            p.aplicar(j, Lugar.EFECTO_DIGITAL[eleccion[i]], "Panadería: " + nombres[eleccion[i]], GrafoLocaciones.PANADERIA);
            p.chat.anunciarDigital(p.reloj.texto(), j.rol, eleccion[i], null);
            if (eleccion[i] == Lugar.E) {
                j.poner("difundio_sin_verificar");
            }
            i++;
        }
        for (ChatCasa.Mensaje m : p.chat.todos()) {
            System.out.println("[" + ChatCasa.NOMBRE + "] " + m.texto);
        }
        System.out.println("Desinformación de la panadería: " + (int) p.desinfo(GrafoLocaciones.PANADERIA));
        System.out.println("Peleas que se activarían esta noche: " + GestorPeleas.activas(p));
        p.imprimir();
    }
}
