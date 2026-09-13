package juegoferia;

public class Main {
    public static void main(String[] args) {
        System.out.println("---");
        System.out.println(" CONSOLA ");
        System.out.println("---\n");

        // -------------------------------------------------------------------
        // partes hecha por majo y valen
        // -------------------------------------------------------------------
        Nodo raiz = ArbolPublicacion.construirArbolEscena1();

        System.out.println("Descripcion de la raiz: " + raiz.descripcion);
        System.out.println("Tipo: " + raiz.tipo);
        System.out.println("Numero de hijos: " + raiz.hijos.size());

        for (Nodo hijo : raiz.hijos) {
            System.out.println(" - Hijo: " + hijo.descripcion + " (tipo: " + hijo.tipo + ")");
        }

        System.out.println("\n--- Profundizando respecto a cada rama ---");

        for (Nodo hijo : raiz.hijos) {
            System.out.println("\nRama: " + hijo.descripcion);
            if (hijo.hijos.isEmpty()) {
                System.out.println("  (es hoja directa, efecto: " + hijo.efecto.getCambios() + ")");
            } else {
                for (Nodo nieto : hijo.hijos) {
                    System.out.println("  - " + nieto.descripcion + " -> efecto: " + nieto.efecto.getCambios());
                }
            }
        }

        System.out.println("\n--- Probando insercion y eliminacion ---");

        ArbolPublicacion.insertarSubArbol("Panaderia", raiz);
        System.out.println("¿Hay algo activo en panaderia? " + ArbolPublicacion.raicesActivas.containsKey("Panaderia"));

        ArbolPublicacion.eliminarSubArbol("Panaderia");
        System.out.println("¿Hay algo activo en panaderia? " + ArbolPublicacion.raicesActivas.containsKey("Panaderia"));

        ArbolPublicacion.eliminarSubArbol("colegio");

        System.out.println("\n--- Verificando las 3 publicaciones nuevas ---");

        Nodo raiz2 = ArbolPublicacion.construirArbolEscena2();
        System.out.println("Escena 2 - hijos: " + raiz2.hijos.size());

        Nodo raiz3 = ArbolPublicacion.construirArbolEscena3();
        System.out.println("Escena 3 - hijos: " + raiz3.hijos.size());

        Nodo raiz4 = ArbolPublicacion.construirArbolEscena4();
        System.out.println("Escena 4 - hijos: " + raiz4.hijos.size());

        // -------------------------------------------------------------------
        // parte hecha por isa
        // -------------------------------------------------------------------
        System.out.println("\n---");
        System.out.println("   INTEGRACION CON SISTEMA DE ESTADO Y CICLO");
        System.out.println("---");

      
        EstadoJuego estado = new EstadoJuego();
        ControladorDePartidas partida = new ControladorDePartidas();

        System.out.println("\n--- Estado Inicial de la Ciudad ---");
        System.out.println("Dias restantes para las elecciones: " + partida.getDiasRestantes());
        System.out.println("¿Panaderia resuelta hoy?: " + partida.laIslaResuelta("Panaderia"));
        estado.imprimirEstadoPartida();

        // A modo de demostración, se simulará un día completo de juego (Escena 1 en la Panadería)
        System.out.println("\n--- Simulando interaccion de juego en Panaderia (Dia 1) ---");
        ArbolPublicacion.insertarSubArbol("Panaderia", raiz);

        // El jugador elige: Verificar (Hijo 0) -> Reportar ya verificado (Hijo 0 de Verificar)
        Nodo decisionJugador = raiz.hijos.get(0).hijos.get(0);
        System.out.println("Jugador selecciona camino hacia hoja: " + decisionJugador.descripcion);

        estado.aplicarEfecto(decisionJugador.efecto);
        partida.checkIslaCompletada("Panaderia");
        ArbolPublicacion.eliminarSubArbol("Panaderia");

        System.out.println("\n--- Estado de la Ciudad tras la decision ---");
        System.out.println("¿Panaderia resuelta hoy?: " + partida.laIslaResuelta("Panaderia"));
        System.out.println("¿Queda subarbol activo en panaderia?: " + ArbolPublicacion.raicesActivas.containsKey("Panaderia"));
        estado.imprimirEstadoPartida();


        System.out.println("\n--- Isla completada!! Siguiente dia!! (n_n)  ---");
        partida.avanzarDia();
        System.out.println("Dias restantes ahora: " + partida.getDiasRestantes());
        System.out.println("¿Panaderia resuelta hoy (nuevo dia)?: " + partida.laIslaResuelta("Panaderia"));
        
    }
}