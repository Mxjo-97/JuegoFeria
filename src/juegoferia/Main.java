package juegoferia;

/**
 * Arranca el juego. Para ver las estructuras de datos en consola (grafos, arbol de reenvios),
 * correr DemoEstructuras.
 */
public class Main {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new VentanaJuego().setVisible(true));
    }
}
