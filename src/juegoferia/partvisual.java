/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package juegoferia;

import java.awt.CardLayout;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;


/**
 *
 * @author Mary
 */
public class partvisual extends javax.swing.JFrame implements KeyListener {

    private CardLayout card;
    private JLabel fondoJuego;
    private JLabel personajeJuego;
    private boolean celularAbierto = false;
    private boolean twitterAbierto = false;
        private boolean mensajesAbierto = false;
            private boolean perfilAbierto = false;
            private boolean panaderiaAbierta = false;
            private boolean dentroPanaderia = false;
private int posicionExteriorX;
private int posicionExteriorY;
    private int velocidad = 3;
    private BufferedImage mapaColisiones;
    private BufferedImage mapaInteracciones;
    private BufferedImage mapaClicsCelular;
    private BufferedImage mapaColisionesPanaderia;
    private boolean teclaW = false;
    private boolean teclaA = false;
    private boolean teclaS = false;
    private boolean teclaD = false;
    private javax.swing.Timer timerMovimiento;
    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(partvisual.class.getName());

    public partvisual() {
        this.setUndecorated(true);
        initComponents();
        this.addKeyListener(this);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BTjugar.setContentAreaFilled(false);
        BTjugar.setBorderPainted(false);
        BTjugar.setFocusPainted(false);
        BTjugar.setOpaque(false);
        BTinst.setContentAreaFilled(false);
        BTinst.setBorderPainted(false);
        BTinst.setFocusPainted(false);
        BTinst.setOpaque(false);
        BTsalir.setContentAreaFilled(false);
        BTsalir.setBorderPainted(false);
        BTsalir.setFocusPainted(false);
        BTsalir.setOpaque(false);
        BTvolver.setContentAreaFilled(false);
        BTvolver.setBorderPainted(false);
        BTvolver.setFocusPainted(false);
        BTvolver.setOpaque(false);
        BTcivil.setContentAreaFilled(false);
        BTcivil.setBorderPainted(false);
        BTcivil.setFocusPainted(false);
        BTcivil.setOpaque(false);
        BTcontinuar.setContentAreaFilled(false);
        BTcontinuar.setBorderPainted(false);
        BTcontinuar.setFocusPainted(false);
        BTcontinuar.setOpaque(false);
        BTvolver2.setContentAreaFilled(false);
        BTvolver2.setBorderPainted(false);
        BTvolver2.setFocusPainted(false);
        BTvolver2.setOpaque(false);
        Digitarnombre.setOpaque(false);
        Digitarnombre.setBorder(BorderFactory.createEmptyBorder());
    }

    private void ajustarImagen(JLabel label) {

        ImageIcon iconoOriginal = (ImageIcon) label.getIcon();

        if (iconoOriginal == null) {
            return;
        }

        int ancho = label.getParent().getWidth();
        int alto = label.getParent().getHeight();

        if (ancho <= 0 || alto <= 0) {
            java.awt.Dimension pantalla
                    = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

            ancho = pantalla.width;
            alto = pantalla.height;
        }
        Image imagen = iconoOriginal.getImage();

        Image imagenEscalada = imagen.getScaledInstance(
                ancho,
                alto,
                Image.SCALE_SMOOTH
        );

        label.setBounds(0, 0, ancho, alto);
        label.setIcon(new ImageIcon(imagenEscalada));
    }
    

    private void prepararJuego() {

        juego.setLayout(null);
        juego.setFocusable(true);
        try {
            mapaColisiones = ImageIO.read(
        getClass().getResourceAsStream(
                "/imagenes/colisiones_exterior.png"
        )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapaColisionesPanaderia = ImageIO.read(
        getClass().getResourceAsStream(
                "/imagenes/colisiones_panaderia.png"
        )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapaInteracciones = ImageIO.read(
                    getClass().getResource("/imagenes/interacciones_exterior.png")
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mapaClicsCelular = ImageIO.read(
                    getClass().getResource("/imagenes/clics_celular.png")
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        juego.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {

                if (estaEnInteraccion(e.getX(), e.getY())) {

                    if (!celularAbierto) {

    celularAbierto = true;

    actualizarImagenCelular();
}
                }
            }
        });
        juego.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                if (celularAbierto && estaEnTwitter(x, y)) {

    twitterAbierto = true;
    mensajesAbierto = false;
    perfilAbierto = false;

    actualizarImagenCelular();
    return;
}

if (celularAbierto && estarmensajes(x, y)) {

    mensajesAbierto = true;
    twitterAbierto = false;
    perfilAbierto = false;

    actualizarImagenCelular();
    return;
}

if (celularAbierto && estarperfil(x, y)) {

    perfilAbierto = true;
    twitterAbierto = false;
    mensajesAbierto = false;

    actualizarImagenCelular();
    return;
}
            }
        });
        ImageIcon iconoFondo = new ImageIcon(
                getClass().getResource("/imagenes/exteriorsin.png")
        );

        Image imagenFondo = iconoFondo.getImage();

        Image imagenFondoEscalada = imagenFondo.getScaledInstance(
                juego.getWidth(),
                juego.getHeight(),
                Image.SCALE_SMOOTH
        );

        juego.setBackground(java.awt.Color.BLACK);

        fondoJuego = new JLabel(new ImageIcon(imagenFondoEscalada));

        fondoJuego.setBounds(
                0,
                0,
                juego.getWidth(),
                juego.getHeight()
        );

        juego.add(fondoJuego);

        ImageIcon iconoPersonaje = new ImageIcon(
                getClass().getResource("/imagenes/personajecivil.png")
        );

        Image imagenPersonaje = iconoPersonaje.getImage();

        Image imagenPersonajeEscalada = imagenPersonaje.getScaledInstance(
                140,
                170,
                Image.SCALE_SMOOTH
        );

        personajeJuego = new JLabel(
                new ImageIcon(imagenPersonajeEscalada)
        );

        personajeJuego.setBounds(
                700,
                600,
                140,
                170
        );

        juego.add(personajeJuego);

        juego.setComponentZOrder(personajeJuego, 0);

        iniciarMovimiento();
        juego.revalidate();
        juego.repaint();
    }
    private boolean esZonaSalidaPanaderia(int color) {

    int rojo = (color >> 16) & 0xFF;
    int verde = (color >> 8) & 0xFF;
    int azul = color & 0xFF;

    return rojo > 180 && verde < 100 && azul < 100;
}
    private void iniciarMovimiento() {
        if (timerMovimiento != null && timerMovimiento.isRunning()) {
            return;
        }
        timerMovimiento = new javax.swing.Timer(16, e -> {

            if (personajeJuego == null) {
                return;
            }

            int x = personajeJuego.getX();
            int y = personajeJuego.getY();

            if (teclaW) {
                y -= velocidad;
            }

            if (teclaS) {
                y += velocidad;
            }

            if (teclaA) {
                x -= velocidad;
            }

            if (teclaD) {
                x += velocidad;
            }
            
            int maxX = juego.getWidth() - personajeJuego.getWidth();
            int maxY = juego.getHeight() - personajeJuego.getHeight();

            if (x < 0) {
                x = 0;
            }

            if (y < 0) {
                y = 0;
            }

            if (x > maxX) {
                x = maxX;
            }

            if (y > maxY) {
                y = maxY;
            }
            if (dentroPanaderia) {

    double escalaX = 1591.0 / juego.getWidth();
    double escalaY = 989.0 / juego.getHeight();

    int centroX = (int) (
            (x + personajeJuego.getWidth() / 2.0) * escalaX
    );

    int piesAnterior = (int) (
            (personajeJuego.getY() + personajeJuego.getHeight() - 10)
            * escalaY
    );

    int piesNuevo = (int) (
            (y + personajeJuego.getHeight() - 10)
            * escalaY
    );

    int colorSalida = mapaColisionesPanaderia.getRGB(
            centroX,
            piesNuevo
    );

    boolean estaEnSalida =
            esZonaSalidaPanaderia(colorSalida);

    // La salida está abajo: debe atravesar la zona hacia abajo
    boolean atravesoSalida =
            estaEnSalida
            && piesAnterior < piesNuevo;

    if (atravesoSalida) {
        salirPanaderia();
        return;
    }
}
            if (panaderiaAbierta && !dentroPanaderia && teclaW) {

    double escalaX = 1591.0 / juego.getWidth();
    double escalaY = 989.0 / juego.getHeight();

    int centroX = (int) ((x + personajeJuego.getWidth() / 2.0) * escalaX);

    int piesAnterior = (int) (
            (personajeJuego.getY() + personajeJuego.getHeight() - 10)
            * escalaY
    );

    int piesNuevo = (int) (
            (y + personajeJuego.getHeight() - 10)
            * escalaY
    );

    boolean estaEnPuerta = centroX >= 835 && centroX <= 969;

    boolean atravesoPuerta =
            estaEnPuerta
            && piesAnterior > 473
            && piesNuevo <= 473;

    if (atravesoPuerta) {
        entrarPanaderia();
        return;
    }
}
            if (!puedeMoverse(x, y)) {
                return;
            }

            personajeJuego.setLocation(x, y);

        });

        timerMovimiento.start();
    }

    private void salirPanaderia() {

    dentroPanaderia = false;

    // Restaurar el tamaño original del personaje
    ImageIcon iconoPersonaje =
            new ImageIcon(getClass().getResource(
                    "/imagenes/personajecivil.png"
            ));

    Image imagenPersonaje = iconoPersonaje.getImage();

    Image imagenEscalada = imagenPersonaje.getScaledInstance(
            150,
            180,
            Image.SCALE_SMOOTH
    );

    personajeJuego.setIcon(new ImageIcon(imagenEscalada));
    personajeJuego.setSize(150, 180);

    // Volver exactamente al lugar donde estaba antes de entrar
    personajeJuego.setLocation(
            posicionExteriorX,
            posicionExteriorY
    );

    actualizarImagenExterior();
}
    private boolean puedeMoverse(int x, int y) {

    BufferedImage mapaActual;

if (dentroPanaderia) {
    mapaActual = mapaColisionesPanaderia;
} else {
    mapaActual = mapaColisiones;
}

if (mapaActual == null) {
    return true;
}

    int izquierda = x + 35;
    int derecha = x + personajeJuego.getWidth() - 35;
    int pies = y + personajeJuego.getHeight() - 10;

    double escalaX = 1591.0 / juego.getWidth();
    double escalaY = 989.0 / juego.getHeight();

    int pxIzquierda = (int) (izquierda * escalaX);
    int pxCentro = (int) (((izquierda + derecha) / 2.0) * escalaX);
    int pxDerecha = (int) (derecha * escalaX);

    int pyPies = (int) (pies * escalaY);

    if (pxIzquierda < 0 || pxCentro < 0 || pxDerecha < 0
            || pyPies < 0
            || pxIzquierda >= mapaActual.getWidth()
            || pxCentro >= mapaActual.getWidth()
            || pxDerecha >= mapaActual.getWidth()
            || pyPies >= mapaActual.getHeight()) {

        return false;
    }

    int colorIzquierda = mapaActual.getRGB(pxIzquierda, pyPies);
    int colorCentro = mapaActual.getRGB(pxCentro, pyPies);
    int colorDerecha = mapaActual.getRGB(pxDerecha, pyPies);

    // Detectar si alguno de los puntos está en la zona roja
    boolean rojoIzquierda = esZonaPuerta(colorIzquierda);
    boolean rojoCentro = esZonaPuerta(colorCentro);
    boolean rojoDerecha = esZonaPuerta(colorDerecha);

    boolean estaEnPuerta = rojoIzquierda
            || rojoCentro
            || rojoDerecha;

    // Si llegó a la zona roja, abre la puerta
    if (estaEnPuerta && !panaderiaAbierta) {
        panaderiaAbierta = true;

        actualizarImagenPuerta();
    }

    // Si la puerta ya está abierta, puede atravesar la zona roja
    if (panaderiaAbierta && estaEnPuerta) {
        return true;
    }

    // Colores normales de colisión
    int grisIzquierda = (colorIzquierda >> 16) & 0xFF;
    int grisCentro = (colorCentro >> 16) & 0xFF;
    int grisDerecha = (colorDerecha >> 16) & 0xFF;

    return grisIzquierda > 200
            && grisCentro > 200
            && grisDerecha > 200;
}
    
    private boolean esZonaPuerta(int color) {

    int rojo = (color >> 16) & 0xFF;
    int verde = (color >> 8) & 0xFF;
    int azul = color & 0xFF;

    return rojo > 180
            && verde < 100
            && azul < 100;
}
    private void entrarPanaderia() {
        posicionExteriorX = personajeJuego.getX();
posicionExteriorY = personajeJuego.getY();

    dentroPanaderia = true;

    ImageIcon iconoPersonaje =
            new ImageIcon(getClass().getResource("/imagenes/personajecivil.png"));

    Image imagenPersonaje = iconoPersonaje.getImage();

    Image imagenEscalada = imagenPersonaje.getScaledInstance(
            185,
            226,
            Image.SCALE_SMOOTH
    );

    personajeJuego.setIcon(new ImageIcon(imagenEscalada));
    personajeJuego.setSize(180, 216);

    int nuevoX = (int) (1250.0 / 1591.0 * juego.getWidth());
    int nuevoY = (int) (700.0 / 989.0 * juego.getHeight());

    personajeJuego.setLocation(nuevoX, nuevoY);

    cambiarFondoJuego("/imagenes/panaderiasin.png");
}

    private void actualizarImagenExterior() {

    if (panaderiaAbierta) {
        actualizarImagenPuerta();
        return;
    }

    if (twitterAbierto) {
        cambiarFondoJuego("/imagenes/exteriortw.png");

    } else if (mensajesAbierto) {
        cambiarFondoJuego("/imagenes/exteriorms.png");

    } else if (perfilAbierto) {
        cambiarFondoJuego("/imagenes/exteriorpf.png");

    } else if (celularAbierto) {
        cambiarFondoJuego("/imagenes/exteriorcon.png");

    } else {
        cambiarFondoJuego("/imagenes/exteriorsin.png");
    }
}
    private void actualizarImagenPuerta() {

    if (!panaderiaAbierta) {
        return;
    }

    if (twitterAbierto) {
        cambiarFondoJuego("/imagenes/puertatw.png");

    } else if (mensajesAbierto) {
        cambiarFondoJuego("/imagenes/puertams.png");

    } else if (perfilAbierto) {
        cambiarFondoJuego("/imagenes/puertapf.png");

    } else if (celularAbierto) {
        cambiarFondoJuego("/imagenes/puertacon.png");

    } else {
        cambiarFondoJuego("/imagenes/puertasin.png");
    }
}
private void actualizarImagenCelular() {

    if (dentroPanaderia) {

        if (twitterAbierto) {
            cambiarFondoJuego("/imagenes/panaderiatw.png");

        } else if (mensajesAbierto) {
            cambiarFondoJuego("/imagenes/panaderiams.png");

        } else if (perfilAbierto) {
            cambiarFondoJuego("/imagenes/panaderiapf.png");

        } else if (celularAbierto) {
            cambiarFondoJuego("/imagenes/panaderiacon.png");

        } else {
            cambiarFondoJuego("/imagenes/panaderiasin.png");
        }

    } else if (panaderiaAbierta) {

        if (twitterAbierto) {
            cambiarFondoJuego("/imagenes/puertatw.png");

        } else if (mensajesAbierto) {
            cambiarFondoJuego("/imagenes/puertams.png");

        } else if (perfilAbierto) {
            cambiarFondoJuego("/imagenes/puertapf.png");

        } else if (celularAbierto) {
            cambiarFondoJuego("/imagenes/puertacon.png");

        } else {
            cambiarFondoJuego("/imagenes/puertasin.png");
        }

    } else {

        if (twitterAbierto) {
            cambiarFondoJuego("/imagenes/exteriortw.png");

        } else if (mensajesAbierto) {
            cambiarFondoJuego("/imagenes/exteriorms.png");

        } else if (perfilAbierto) {
            cambiarFondoJuego("/imagenes/exteriorpf.png");

        } else if (celularAbierto) {
            cambiarFondoJuego("/imagenes/exteriorcon.png");

        } else {
            cambiarFondoJuego("/imagenes/exteriorsin.png");
        }
    }
}
private boolean estaEnInteraccion(int x, int y) {

        if (mapaInteracciones == null) {
            return false;
        }

        double escalaX = 1591.0 / juego.getWidth();
        double escalaY = 989.0 / juego.getHeight();

        int px = (int) (x * escalaX);
        int py = (int) (y * escalaY);

        if (px < 0 || py < 0
                || px >= mapaInteracciones.getWidth()
                || py >= mapaInteracciones.getHeight()) {
            return false;
        }

        int rgb = mapaInteracciones.getRGB(px, py);

        int rojo = (rgb >> 16) & 0xFF;
        int verde = (rgb >> 8) & 0xFF;
        int azul = rgb & 0xFF;

        return rojo > 200 && verde < 100 && azul < 100;
    }

    private boolean estaEnTwitter(int x, int y) {

        if (mapaClicsCelular == null) {
            return false;
        }

        double escalaX = 1591.0 / juego.getWidth();
        double escalaY = 989.0 / juego.getHeight();

        int px = (int) (x * escalaX);
        int py = (int) (y * escalaY);

        if (px < 0 || py < 0
                || px >= mapaClicsCelular.getWidth()
                || py >= mapaClicsCelular.getHeight()) {
            return false;
        }

        int rgb = mapaClicsCelular.getRGB(px, py);

        int rojo = (rgb >> 16) & 0xFF;
        int verde = (rgb >> 8) & 0xFF;
        int azul = rgb & 0xFF;

        return azul > 150
                && rojo < 100
                && verde < 180;
    }
    private boolean estarperfil(int x, int y) {

        if (mapaClicsCelular == null) {
            return false;
        }

        double escalaX = 1591.0 / juego.getWidth();
        double escalaY = 989.0 / juego.getHeight();

        int px = (int) (x * escalaX);
        int py = (int) (y * escalaY);

        if (px < 0 || py < 0
                || px >= mapaClicsCelular.getWidth()
                || py >= mapaClicsCelular.getHeight()) {
            return false;
        }

        int rgb = mapaClicsCelular.getRGB(px, py);

    int rojo = (rgb >> 16) & 0xFF;
    int verde = (rgb >> 8) & 0xFF;
    int azul = rgb & 0xFF;

    return rojo > 100
            && azul > 100
            && verde < 120;
}
    private boolean estarmensajes(int x, int y) {

        if (mapaClicsCelular == null) {
            return false;
        }

        double escalaX = 1591.0 / juego.getWidth();
        double escalaY = 989.0 / juego.getHeight();

        int px = (int) (x * escalaX);
        int py = (int) (y * escalaY);

        if (px < 0 || py < 0
                || px >= mapaClicsCelular.getWidth()
                || py >= mapaClicsCelular.getHeight()) {
            return false;
        }

            int rgb = mapaClicsCelular.getRGB(px, py);

    int rojo = (rgb >> 16) & 0xFF;
    int verde = (rgb >> 8) & 0xFF;
    int azul = rgb & 0xFF;

    return verde > 150
            && rojo < 150
            && azul < 150;
    }
    

    private void cambiarFondoJuego(String ruta) {

        ImageIcon icono = new ImageIcon(
                getClass().getResource(ruta)
        );

        Image imagen = icono.getImage();

        Image imagenEscalada = imagen.getScaledInstance(
                juego.getWidth(),
                juego.getHeight(),
                Image.SCALE_SMOOTH
        );

        fondoJuego.setIcon(new ImageIcon(imagenEscalada));
    }

    public void prepararPantallas() {

        java.awt.Dimension pantalla
                = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

        int ancho = pantalla.width;
        int alto = pantalla.height;

        ajustarImagenInicial(jLabel1, ancho, alto);
        ajustarImagenInicial(jLabel2, ancho, alto);
        ajustarImagenInicial(jLabel3, ancho, alto);
    }

    private void ajustarImagenInicial(JLabel label, int ancho, int alto) {

        ImageIcon iconoOriginal = (ImageIcon) label.getIcon();

        if (iconoOriginal == null) {
            return;
        }

        Image imagen = iconoOriginal.getImage();

        Image imagenEscalada = imagen.getScaledInstance(
                ancho,
                alto,
                Image.SCALE_SMOOTH
        );

        label.setBounds(0, 0, ancho, alto);
        label.setIcon(new ImageIcon(imagenEscalada));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        Panel_cambiante = new javax.swing.JPanel();
        iniciojuego = new javax.swing.JPanel();
        BTjugar = new javax.swing.JButton();
        BTsalir = new javax.swing.JButton();
        BTinst = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        elegirroles = new javax.swing.JPanel();
        BTvolver = new javax.swing.JButton();
        BTcivil = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        ponernombre = new javax.swing.JPanel();
        BTvolver2 = new javax.swing.JButton();
        BTcontinuar = new javax.swing.JButton();
        Digitarnombre = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        juego = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Panel_cambiante.setLayout(new java.awt.CardLayout());

        iniciojuego.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTjugar.addActionListener(this::BTjugarActionPerformed);
        iniciojuego.add(BTjugar, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 420, 410, 100));

        BTsalir.addActionListener(this::BTsalirActionPerformed);
        iniciojuego.add(BTsalir, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 650, 410, 80));

        BTinst.addActionListener(this::BTinstActionPerformed);
        iniciojuego.add(BTinst, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 530, 410, 100));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Inicio.png"))); // NOI18N
        iniciojuego.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        Panel_cambiante.add(iniciojuego, "inicio");

        elegirroles.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver.addActionListener(this::BTvolverActionPerformed);
        elegirroles.add(BTvolver, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 770, 210, 50));

        BTcivil.addActionListener(this::BTcivilActionPerformed);
        elegirroles.add(BTcivil, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 320, 300, 270));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Roles.png"))); // NOI18N
        elegirroles.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1591, 989));
        jLabel2.getAccessibleContext().setAccessibleName("");

        Panel_cambiante.add(elegirroles, "roles");

        ponernombre.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver2.addActionListener(this::BTvolver2ActionPerformed);
        ponernombre.add(BTvolver2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 760, 210, 60));

        BTcontinuar.addActionListener(this::BTcontinuarActionPerformed);
        ponernombre.add(BTcontinuar, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 690, 450, 80));

        Digitarnombre.setFont(new java.awt.Font("Segoe UI", 2, 24)); // NOI18N
        Digitarnombre.setForeground(new java.awt.Color(124, 112, 88));
        Digitarnombre.addActionListener(this::DigitarnombreActionPerformed);
        ponernombre.add(Digitarnombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 550, 510, 70));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Nombre.png"))); // NOI18N
        ponernombre.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        Panel_cambiante.add(ponernombre, "nombre");

        javax.swing.GroupLayout juegoLayout = new javax.swing.GroupLayout(juego);
        juego.setLayout(juegoLayout);
        juegoLayout.setHorizontalGroup(
            juegoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1591, Short.MAX_VALUE)
        );
        juegoLayout.setVerticalGroup(
            juegoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 990, Short.MAX_VALUE)
        );

        Panel_cambiante.add(juego, "juego");

        getContentPane().add(Panel_cambiante, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BTjugarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTjugarActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "roles");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel2);
        });
    }//GEN-LAST:event_BTjugarActionPerformed

    private void BTinstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTinstActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BTinstActionPerformed

    private void BTsalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTsalirActionPerformed
        System.exit(0);
    }//GEN-LAST:event_BTsalirActionPerformed

    private void BTvolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolverActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inicio");
    }//GEN-LAST:event_BTvolverActionPerformed

    private void BTcivilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTcivilActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "nombre");
    }//GEN-LAST:event_BTcivilActionPerformed

    private void DigitarnombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DigitarnombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_DigitarnombreActionPerformed

    private void BTvolver2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolver2ActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "roles");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel2);
        });
    }//GEN-LAST:event_BTvolver2ActionPerformed

    private void BTcontinuarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTcontinuarActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "juego");
        prepararJuego();
    }//GEN-LAST:event_BTcontinuarActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new partvisual().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BTcivil;
    private javax.swing.JButton BTcontinuar;
    private javax.swing.JButton BTinst;
    private javax.swing.JButton BTjugar;
    private javax.swing.JButton BTsalir;
    private javax.swing.JButton BTvolver;
    private javax.swing.JButton BTvolver2;
    private javax.swing.JTextField Digitarnombre;
    private javax.swing.JPanel Panel_cambiante;
    private javax.swing.JPanel elegirroles;
    private javax.swing.JPanel iniciojuego;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel juego;
    private javax.swing.JPanel ponernombre;
    // End of variables declaration//GEN-END:variables

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                teclaW = true;
                break;

            case KeyEvent.VK_A:
                teclaA = true;
                break;

            case KeyEvent.VK_S:
                teclaS = true;
                break;

            case KeyEvent.VK_D:
                teclaD = true;
                break;

            case KeyEvent.VK_X:

                if (celularAbierto) {

                    celularAbierto = false;
                    twitterAbierto = false;
                    mensajesAbierto=false;
                    perfilAbierto=false;

                   actualizarImagenCelular();
                }

                break;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                teclaW = false;
                break;

            case KeyEvent.VK_A:
                teclaA = false;
                break;

            case KeyEvent.VK_S:
                teclaS = false;
                break;

            case KeyEvent.VK_D:
                teclaD = false;
                break;
        }
    }
}
