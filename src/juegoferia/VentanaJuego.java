package juegoferia;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * La ventana del juego. SOLO cambia de pantalla; la logica vive en cada pantalla. Orden:
 *
 *   cinematica de inicio (8 cuadros, se puede saltar)
 *     -> menu (PantallaInicio)
 *        -> JUGAR: como quieren jugar (PantallaModo)
 *             -> EN RED: conectar / sala (PanelRed) -> cuando estan los 4: JuegoMacondo en red
 *             -> EN UN SOLO PC: elegir personaje -> JuegoMacondo (TAB cambia quien camina)
 *        -> INSTRUCCIONES: tu imagen "instrucciones.png" -> menu
 *        -> MINIJUEGOS: pantalla de pruebas -> menu
 *        -> SALIR
 *
 * La ventana se puede agrandar y achicar (el juego se escala solo, sin deformarse).
 * F11 = pantalla completa / ventana.
 */
public class VentanaJuego extends JFrame {

    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);
    private PantallaElegirPersonaje elegir;
    private PanelRed panelRed;
    private JuegoMacondo juego;
    private boolean completa;

    public VentanaJuego() {
        super("Todos hablan en Macondo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 540));
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setContentPane(raiz);

        if (Recursos.existe("/imagenes/cursores/normal.png")) {
            CursorJuego.instalar(this);
        }

        // F11: pantalla completa (es solo de TU pantalla: no viaja por la red)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F11 && isActive()) {
                    alternarPantallaCompleta();
                    return true;
                }
                return false;
            }
        });

        // menu
        PantallaInicio menu = new PantallaInicio(this::irAModo, this::irAInstrucciones,
                () -> System.exit(0), this::irAMinijuegos);
        raiz.add(menu, "menu");

        // como jugar: tu imagen instrucciones.png (1591 x 989) o, si no esta, la nueva (1920 x 1080)
        raiz.add(crearComoJugar(), "instrucciones");

        // minijuegos (modo prueba)
        raiz.add(new PantallaMinijuegos(this::irAlMenu, null, new EstadoJuego()), "minijuegos");

        // como quieren jugar
        raiz.add(new PantallaModo(this::irARed, this::irAElegir, this::irAlMenu), "modo");

        // en red
        panelRed = new PanelRed(new PanelRed.Anfitrion() {
            @Override
            public void empezar(Partida partida, Rol miRol, RedMacondo.Cliente red) {
                empezarPartidaRed(partida, miRol, red);
            }

            @Override
            public void volver() {
                irAModo();
            }
        });
        raiz.add(panelRed, "red");

        // un solo PC: elegir personaje
        elegir = new PantallaElegirPersonaje(this::empezarPartida, this::irAModo);
        raiz.add(elegir, "elegir");

        // la cinematica de inicio
        Cinematica inicio = GuionPrologo.antesDelMenu();
        raiz.add(inicio, "inicio");
        cartas.show(raiz, "inicio");
        inicio.iniciar(this::irAlMenu);
    }

    private PantallaImagen crearComoJugar() {
        String ruta = Rutas.primera(Rutas.COMO_JUGAR);
        PantallaImagen p = new PantallaImagen(ruta);
        if (ruta.equals(Rutas.COMO_JUGAR[0])) {
            p.zona(40, 860, 280, 90, this::irAlMenu);            // tu boton "Volver" (1591 x 989)
        } else {
            p.disenoDe(1920, 1080).zona(40, 960, 300, 100, this::irAlMenu);
        }
        return p;
    }

    private void alternarPantallaCompleta() {
        completa = !completa;
        dispose();
        setUndecorated(completa);
        setExtendedState(completa ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
        if (!completa) {
            setSize(1280, 720);
            setLocationRelativeTo(null);
        }
        setVisible(true);
        raiz.requestFocusInWindow();
    }

    private void irAlMenu() {
        cartas.show(raiz, "menu");
    }

    private void irAModo() {
        cartas.show(raiz, "modo");
    }

    private void irAInstrucciones() {
        cartas.show(raiz, "instrucciones");
    }

    private void irAMinijuegos() {
        cartas.show(raiz, "minijuegos");
    }

    private void irARed() {
        panelRed.reiniciar();
        cartas.show(raiz, "red");
    }

    private void irAElegir() {
        elegir.reiniciar();
        cartas.show(raiz, "elegir");
        elegir.requestFocusInWindow();
    }

    // ===================== UN SOLO PC =====================

    private void empezarPartida(Partida partida) {
        if (juego != null) {
            raiz.remove(juego);
        }
        juego = new JuegoMacondo(partida, this::irAlMenu);
        raiz.add(juego, "juego");
        cartas.show(raiz, "juego");
        juego.empezar();
    }

    // ===================== EN RED =====================

    private void empezarPartidaRed(Partida partida, Rol miRol, RedMacondo.Cliente red) {
        if (juego != null) {
            raiz.remove(juego);
        }
        juego = new JuegoMacondo(partida, () -> terminarRed(null));
        final JuegoMacondo j = juego;
        raiz.add(j, "juego");
        cartas.show(raiz, "juego");
        Sincronia.activar(miRol, red, j::panelActivo, () -> partida, j::huella, this::terminarRed);
        j.empezar();
        red.enviar("LISTO");      // cuando los cuatro avisan, el anfitrion empieza a mandar cuadros
    }

    /** Se acabo la partida (final del juego) o se cayo alguien (motivo != null). */
    private void terminarRed(String motivo) {
        Sincronia.desactivar();
        panelRed.cerrarRed(motivo == null);
        if (juego != null) {
            raiz.remove(juego);
            juego = null;
        }
        irAlMenu();
        if (motivo != null) {
            JOptionPane.showMessageDialog(this, motivo, "Partida en red", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
