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
    private JLabel iconoPanico;
    private JLabel situacionMiniatura;
    private boolean abanicoOpcionesVisible = false;
    private JLabel opciones;
    private JLabel opcionesSecundarias;
    private boolean ramaAActiva = false;
    private boolean ramaTintoCalmaActiva = false;
    private boolean ramaDActiva = false;
    private boolean ramaCActiva = false;
    private boolean ramaBActiva = false;
    private boolean ramaBViral = false;
    private boolean consecuenciaRamaBActiva = false;
    private boolean publicacionFalsa = true;
    private Nodo arbolEscena1;
    private EstadoJuego estadoJuego;
    private Nodo nodoActual;
    private boolean celularAbierto = false;
    private boolean twitterAbierto = false;
        private boolean mensajesAbierto = false;
            private boolean perfilAbierto = false;
            private boolean situacionActiva = false;
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
    private JLabel dialogoJuego;
    private boolean juegoPreparado = false;
    private boolean dialogoActivo = false;
    private boolean dialogoDonaRosaIniciado = false;
    private int dialogoActual = 40;
    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(partvisual.class.getName());

    public partvisual() {
     this.setUndecorated(true);
    initComponents();
     
    arbolEscena1 = ArbolPublicacion.construirArbolEscena1();
    estadoJuego = new EstadoJuego();
    nodoActual = arbolEscena1;
    
    addComponentListener(new java.awt.event.ComponentAdapter() {
            
    @Override
    public void componentResized(java.awt.event.ComponentEvent e) {
        ajustarBotonesInterfaz();
    }
    });
        
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
        BTvolver3.setContentAreaFilled(false);
        BTvolver3.setBorderPainted(false);
        BTvolver3.setFocusPainted(false);
        BTvolver3.setOpaque(false);
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
    
    private void ajustarBotonesInterfaz() {



    int anchoInicio = iniciojuego.getWidth();
    int altoInicio = iniciojuego.getHeight();

    if (anchoInicio > 0 && altoInicio > 0) {

        double escalaX = anchoInicio / 1591.0;
        double escalaY = altoInicio / 989.0;

        BTjugar.setBounds(
                (int) (580 * escalaX),
                (int) (420 * escalaY),
                (int) (410 * escalaX),
                (int) (100 * escalaY)
        );

        BTinst.setBounds(
                (int) (580 * escalaX),
                (int) (530 * escalaY),
                (int) (410 * escalaX),
                (int) (100 * escalaY)
        );

        BTsalir.setBounds(
                (int) (580 * escalaX),
                (int) (650 * escalaY),
                (int) (410 * escalaX),
                (int) (80 * escalaY)
        );

        iniciojuego.revalidate();
        iniciojuego.repaint();
    }

    int anchoNombre = ponernombre.getWidth();
    int altoNombre = ponernombre.getHeight();

    if (anchoNombre > 0 && altoNombre > 0) {

        double escalaX = anchoNombre / 1591.0;
        double escalaY = altoNombre / 989.0;

        BTvolver2.setBounds(
                (int) (50 * escalaX),
                (int) (760 * escalaY),
                (int) (210 * escalaX),
                (int) (60 * escalaY)
        );

        BTcontinuar.setBounds(
                (int) (550 * escalaX),
                (int) (690 * escalaY),
                (int) (450 * escalaX),
                (int) (80 * escalaY)
        );

        Digitarnombre.setBounds(
                (int) (550 * escalaX),
                (int) (550 * escalaY),
                (int) (510 * escalaX),
                (int) (70 * escalaY)
        );

        ponernombre.revalidate();
        ponernombre.repaint();
    }
}
    private void prepararJuego() {

        juego.setLayout(null);
        juego.setFocusable(true);
        juego.addKeyListener(this);
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
            
                // CLICK EN EL BOTÓN DE LA SITUACIÓN
if (situacionActiva) {

    boolean clicEnPlaySituacion =
            x >= juego.getWidth() * 0.70
            && x <= juego.getWidth() * 0.88
            && y >= juego.getHeight() * 0.58
            && y <= juego.getHeight() * 0.84;

    if (clicEnPlaySituacion) {

        situacionActiva = false;

        // Volver a mostrar el personaje
        personajeJuego.setVisible(true);

        // Volver al fondo anterior según el estado del teléfono
        actualizarImagenCelular();

        // Volver a permitir el movimiento
        if (timerMovimiento != null) {
            timerMovimiento.start();
        }
        
        javax.swing.Timer esperaPanico =
        new javax.swing.Timer(2000, e2 -> {

    mostrarIconoPanico();});

        esperaPanico.setRepeats(false);
        esperaPanico.start();

        juego.revalidate();
        juego.repaint();
    }

    return;
}
                
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
        
        iconoPanico = new JLabel();

ImageIcon icono =
        new ImageIcon(
                getClass().getResource("/imagenes/dialogos/panico.png")
        );

Image imagenPanico = icono.getImage();

Image imagenPanicoEscalada =
        imagenPanico.getScaledInstance(
                70,
                70,
                Image.SCALE_SMOOTH
        );

iconoPanico.setIcon(
        new ImageIcon(imagenPanicoEscalada)
);

iconoPanico.setBounds(0, 0, 70, 70);

iconoPanico.setVisible(false);
juego.add(iconoPanico);

iconoPanico.addMouseListener(new java.awt.event.MouseAdapter() {

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {

        if (!abanicoOpcionesVisible) {

            abanicoOpcionesVisible = true;

            situacionMiniatura.setVisible(true);

            opciones.setVisible(true);
            
            
            // Posición del NIÑO CON GORRA (Mateo)
int mateoX = (int) (1415.0 / 1591.0 * juego.getWidth());
int mateoY = (int) (735.0 / 989.0 * juego.getHeight());

// Centrar el abanico respecto a Mateo
int opcionesX = mateoX - opciones.getWidth() / 2 + 20;
int opcionesY = mateoY - opciones.getHeight() / 2 - 175;

opciones.setLocation(opcionesX, opcionesY);
            
            juego.setComponentZOrder(opciones, 0);
            juego.setComponentZOrder(situacionMiniatura, 1);


            juego.revalidate();
            juego.repaint();
        }
    }
});

situacionMiniatura = new JLabel();

ImageIcon imagenSituacion =
        new ImageIcon(
                getClass().getResource("/imagenes/dialogos/situaciondepiloto.png")
        );

Image imagenSituacionOriginal = imagenSituacion.getImage();

Image imagenSituacionEscalada =
        imagenSituacionOriginal.getScaledInstance(
                300,
                186,
                Image.SCALE_SMOOTH
        );

situacionMiniatura.setIcon(
        new ImageIcon(imagenSituacionEscalada)
);

situacionMiniatura.setBounds(
        juego.getWidth() - 320,
        20,
        300,
        186
);

situacionMiniatura.setVisible(false);

juego.add(situacionMiniatura);

opciones = new JLabel();
    

ImageIcon iconoOriginal = new ImageIcon(
        getClass().getResource("/imagenes/dialogos/opciones.png")
);

int anchoOpciones = 220;
int altoOpciones = 137;

java.awt.Image imagenOpcionesEscalada =
        iconoOriginal.getImage().getScaledInstance(
                anchoOpciones,
                altoOpciones,
                java.awt.Image.SCALE_SMOOTH
        );

opciones.setIcon(new ImageIcon(imagenOpcionesEscalada));

opciones.setBounds(
        0,
        0,
        anchoOpciones,
        altoOpciones
);


opciones.setVisible(false);

opcionesSecundarias = new JLabel();

ImageIcon iconoOpcionesSecundarias = new ImageIcon(
        getClass().getResource("/imagenes/dialogos/opcionesverificacion.png")
);

int anchoOpcionesSecundarias = 500;
int altoOpcionesSecundarias = 290;

java.awt.Image imagenOpcionesSecundariasEscalada =
        iconoOpcionesSecundarias.getImage().getScaledInstance(
                anchoOpcionesSecundarias,
                altoOpcionesSecundarias,
                java.awt.Image.SCALE_SMOOTH
        );

opcionesSecundarias.setIcon(
        new ImageIcon(imagenOpcionesSecundariasEscalada)
);

opcionesSecundarias.setBounds(
        0,
        0,
        anchoOpcionesSecundarias,
        altoOpcionesSecundarias
);

opcionesSecundarias.setVisible(false);

juego.add(opcionesSecundarias);

opcionesSecundarias.addMouseListener(new java.awt.event.MouseAdapter() {

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {

        int x = e.getX();
        int y = e.getY();

        int ancho = opcionesSecundarias.getWidth();
        int alto = opcionesSecundarias.getHeight();

      if (ramaBActiva) {

    if (x <= ancho * 0.50
            && y >= alto * 0.45) {

        System.out.println("Elegiste: SE VUELVE VIRAL");

        seleccionarDecision("viral");
        ramaBViral = true;

        opcionesSecundarias.setVisible(false);

        dialogoActivo = true;
        dialogoActual = 92;
        mostrarDialogo(dialogoActual);

        juego.revalidate();
        juego.repaint();
        return;

    } else if (x >= ancho * 0.50
            && y >= alto * 0.45) {

        System.out.println("Elegiste: NO SE VUELVE VIRAL");

        seleccionarDecision("noviral");
        ramaBViral = false;

        opcionesSecundarias.setVisible(false);

        dialogoActivo = true;
        dialogoActual = 94;
        mostrarDialogo(dialogoActual);

        juego.revalidate();
        juego.repaint();
        return;
    }
}
        if (x <= ancho * 0.50
                && y >= alto * 0.55) {

            System.out.println("Elegiste REPORTAR");

            opcionesSecundarias.setVisible(false);
            abanicoOpcionesVisible = false;
            situacionMiniatura.setVisible(false);


            iconoPanico.setVisible(false);

// Avanzamos al nodo correcto de REPORTAR
seleccionarDecision("reportar");

ramaAActiva = true;

if (timerMovimiento != null) {
    timerMovimiento.stop();
}

System.out.println("ANTES DE MOSTRAR DIALOGO");

mostrarDialogoRamaAReportado();
    
   System.out.println("DESPUES DE MOSTRAR DIALOGO");

            juego.revalidate();
            juego.repaint();

        
         } else if (x >= ancho * 0.50
        && y >= alto * 0.55) {

    System.out.println("Elegiste TINTO / CALMA");

    // Avanzamos al nodo:
    // pub1_verificar -> pub1_verificar_ignorar
    seleccionarDecision("ignorar");

    opcionesSecundarias.setVisible(false);
    abanicoOpcionesVisible = false;
    situacionMiniatura.setVisible(false);

    // Quitamos el signo de exclamación de Mateo
    iconoPanico.setVisible(false);

    // Detener movimiento mientras aparece el diálogo
    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    // Mostrar diálogo de Tinto / Calma
    mostrarDialogoTintoCalma();

    juego.revalidate();
    juego.repaint();
    }
}
}
);

juego.add(opciones);
juego.setComponentZOrder(opciones, 0);

opciones.addMouseListener(new java.awt.event.MouseAdapter() {

    
    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {

        int x = e.getX();
        int y = e.getY();

        int ancho = opciones.getWidth();
        int alto = opciones.getHeight();

        // VERIFICAR - parte superior
        if (x >= ancho * 0.25
                && x <= ancho * 0.75
                && y <= alto * 0.35) {

            seleccionarDecision("verificar");

        // DIFUNDIR - parte izquierda
        // DIFUNDIR - parte izquierda
} else if (x <= ancho * 0.45
        && y >= alto * 0.30
        && y <= alto * 0.65) {

    System.out.println("Elegiste DIFUNDIR");

    seleccionarDecision("difundir");

    ramaBActiva = true;

    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    situacionMiniatura.setVisible(false);
    iconoPanico.setVisible(false);

    abanicoOpcionesVisible = false;

    // Mostrar las opciones de Rama B
    ImageIcon imagenDifundir =
            new ImageIcon(
                    getClass().getResource(
                            "/imagenes/dialogos/opcionesdifundir.png"
                    )
            );

    Image imagenDifundirEscalada =
            imagenDifundir.getImage().getScaledInstance(
                    500,
                    290,
                    Image.SCALE_SMOOTH
            );

    opcionesSecundarias.setIcon(
            new ImageIcon(imagenDifundirEscalada)
    );

    opcionesSecundarias.setSize(500, 290);

    opcionesSecundarias.setLocation(
            (juego.getWidth() - opcionesSecundarias.getWidth()) / 2,
            (juego.getHeight() - opcionesSecundarias.getHeight()) / 2
    );

    opcionesSecundarias.setVisible(true);

    juego.setComponentZOrder(opcionesSecundarias, 0);

    juego.revalidate();
    juego.repaint();
    
    
     // REPORTAR - parte derecha
} else if (x >= ancho * 0.55
        && y >= alto * 0.30
        && y <= alto * 0.65) {

    System.out.println("Elegiste REPORTAR");

    // Pasamos al nodo REPORTAR
    seleccionarDecision("reportar");

    // Activamos Rama C
    ramaCActiva = true;

    // Detener movimiento
    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    // Ocultar elementos de la decisión
    situacionMiniatura.setVisible(false);
    iconoPanico.setVisible(false);

    

    if (publicacionFalsa) {

        // La publicación era falsa
        // Por lo tanto REPORTAR fue correcto
        seleccionarDecision("acerto");

        mostrarConsecuenciaRamaC(true);

    } else {

        // La publicación era verdadera
        // Por lo tanto REPORTAR fue incorrecto
        seleccionarDecision("equivoco");

        mostrarConsecuenciaRamaC(false);
    }

    juego.revalidate();
    juego.repaint();   

        // TINTO/CALMA - parte inferior
} else if (y >= alto * 0.65) {

    System.out.println("Elegiste TINTO / CALMA");

    seleccionarDecision("ignorar");

    ramaDActiva = true;

    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    mostrarDialogoRamaD();
}
    }
});



        personajeJuego.setBounds(
                700,
                600,
                140,
                170
        );

        juego.add(personajeJuego);

        juego.setComponentZOrder(personajeJuego, 0);
        juego.setComponentZOrder(iconoPanico, 0);
        juego.setComponentZOrder(opciones, 0);
        juego.setComponentZOrder(situacionMiniatura, 0);
        
        iniciarMovimiento();
        juego.setFocusable(true);
        juego.requestFocusInWindow();
        
        this.setFocusable(true);
        this.requestFocusInWindow();
        
        juego.revalidate();
        juego.repaint();
        
        
    }
        
      private void mostrarDialogoTintoCalma() {

    String ruta =
            "/imagenes/dialogos/tintoycalmaconpuntuacion.png";

    java.net.URL recurso =
            getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original =
                ImageIO.read(recurso);

        // Buscar solamente la parte visible del PNG
        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < original.getHeight(); y++) {

            for (int x = 0; x < original.getWidth(); x++) {

                int alpha =
                        (original.getRGB(x, y) >> 24) & 0xFF;

                if (alpha > 10) {

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        BufferedImage recortada =
                original.getSubimage(
                        minX,
                        minY,
                        maxX - minX + 1,
                        maxY - minY + 1
                );

        int ancho = 500;

        double proporcion =
                (double) recortada.getHeight()
                / recortada.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                recortada.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                ancho,
                alto
        );

        // CENTRADO EN TODA LA PANTALLA
        int x =
                (juego.getWidth() - ancho) / 2;

        int y =
                (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(x, y);

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();

        System.out.println("Aviso TINTO/CALMA mostrado.");

        // Esperar un momento y continuar automáticamente
        javax.swing.Timer timerTintoCalma =
                new javax.swing.Timer(
                        2000,
                        e -> {

                            dialogoJuego.setVisible(false);

                            // Aplicar el +3 de información verificada
                            if (nodoActual != null) {
                                estadoJuego.aplicarEfecto(
                                        nodoActual.efecto
                                );
                            }

                            System.out.println(
                                    "Tinto/Calma completado."
                            );

                            System.out.println(
                                    "Información verificada +3"
                            );

                            estadoJuego.imprimirEstadoPartida();

                            // Mostrar coleccionable
                            mostrarColeccionableRamaA();
                        }
                );

        timerTintoCalma.setRepeats(false);
        timerTintoCalma.start();

    } catch (IOException e) {
        e.printStackTrace();
    }
}
      
      private void mostrarDialogoRamaD() {

    String ruta =
            "/imagenes/dialogos/ramaDIgnorarTintoCalmaDoñaRosa.png";

    java.net.URL recurso =
            getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original =
                ImageIO.read(recurso);

       

        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < original.getHeight(); y++) {

            for (int x = 0; x < original.getWidth(); x++) {

                int alpha =
                        (original.getRGB(x, y) >> 24) & 0xFF;

                if (alpha > 10) {

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        BufferedImage recortada =
                original.getSubimage(
                        minX,
                        minY,
                        maxX - minX + 1,
                        maxY - minY + 1
                );

        

        int ancho = 360;

        double proporcion =
                (double) recortada.getHeight()
                / recortada.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                recortada.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                ancho,
                alto
        );
        
        int xDialogo =
        (int) (1190.0 / 1591.0 * juego.getWidth()) - 45;
        
        int yDialogo =
        (int) (250.0 / 989.0 * juego.getHeight())
        - alto - 20;
        
        dialogoJuego.setLocation(
        xDialogo,
        yDialogo);

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();

        System.out.println(
                "Diálogo RAMA D mostrado."
        );

    } catch (IOException e) {
        e.printStackTrace();
    }
}
        
    
    private void seleccionarDecision(String decision) {

    if (nodoActual == null) {
        return;
    }


    if (nodoActual == arbolEscena1) {

        if (decision.equals("verificar")) {

            nodoActual = nodoActual.hijos.get(0);
            
            opcionesSecundarias.setLocation(
            (juego.getWidth() - opcionesSecundarias.getWidth()) / 2,
            (juego.getHeight() - opcionesSecundarias.getHeight()) / 2
    );

    opcionesSecundarias.setVisible(true);
    juego.setComponentZOrder(opcionesSecundarias, 0);

    juego.revalidate();
    juego.repaint();

        } else if (decision.equals("difundir")) {

            nodoActual = nodoActual.hijos.get(1);

        } else if (decision.equals("reportar")) {

            nodoActual = nodoActual.hijos.get(2);

        } else if (decision.equals("ignorar")) {

            nodoActual = nodoActual.hijos.get(3);
        }

    } else if (nodoActual.id.equals("pub1_verificar")) {

        if (decision.equals("reportar")) {

            nodoActual = nodoActual.hijos.get(0);

        } else if (decision.equals("ignorar")) {

            nodoActual = nodoActual.hijos.get(1);
        }

    } else if (nodoActual.id.equals("pub1_difundir")) {

        if (decision.equals("viral")) {

            nodoActual = nodoActual.hijos.get(0);

        } else if (decision.equals("noviral")) {

            nodoActual = nodoActual.hijos.get(1);
        }

    } else if (nodoActual.id.equals("pub1_reportar")) {

        if (decision.equals("acerto")) {

            nodoActual = nodoActual.hijos.get(0);

        } else if (decision.equals("equivoco")) {

            nodoActual = nodoActual.hijos.get(1);
        }
    }

    System.out.println("Decisión elegida: " + decision);
    System.out.println("Nodo actual: " + nodoActual.id);
    System.out.println("Descripción: " + nodoActual.descripcion);

    opciones.setVisible(false);
    abanicoOpcionesVisible = false;
}
    
    
    private void prepararDialogos() {

    if (dialogoJuego != null) {
        return;
    }

    dialogoJuego = new JLabel();
    dialogoJuego.setVisible(false);

    // Hacer clickeable el botón PLAY que está dentro de la imagen
    dialogoJuego.addMouseListener(new java.awt.event.MouseAdapter() {

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
            
            int x = e.getX();
            int y = e.getY();

            int ancho = dialogoJuego.getWidth();
            int alto = dialogoJuego.getHeight();
            
            
    if (consecuenciaRamaBActiva) {

    consecuenciaRamaBActiva = false;

    dialogoJuego.setVisible(false);
    dialogoActivo = false;

    // Aplicar efectos de la decisión
    if (nodoActual != null) {
        estadoJuego.aplicarEfecto(nodoActual.efecto);
    }

    System.out.println("=================================");
    System.out.println("RAMA B COMPLETADA");

    if (nodoActual != null
            && nodoActual.id.equals("pub1_difundir_viral")) {

        System.out.println("Resultado: VIRAL");
        System.out.println("Desinformación +12");
        System.out.println("Confianza -6");
        System.out.println("Conflictos +4");

    } else if (nodoActual != null
            && nodoActual.id.equals("pub1_difundir_noviral")) {

        System.out.println("Resultado: NO VIRAL");
        System.out.println("Desinformación -2");
    }

    System.out.println("=================================");

    estadoJuego.imprimirEstadoPartida();

    // Igual que las otras ramas:
    // consecuencia → coleccionable → fin del día
    mostrarColeccionableRamaA();

    return;
}
            
        boolean esMateo =
        dialogoActual == 43
        || dialogoActual == 44
        || dialogoActual == 47
        || dialogoActual == 61
        || dialogoActual == 63;
        
boolean esPresentador =
        dialogoActual == 52
        || dialogoActual == 53
        || dialogoActual == 54
        || dialogoActual == 55
        || dialogoActual == 57
        || dialogoActual == 58
        || dialogoActual == 59;

boolean clicEnPlay;

if (ramaBActiva) {

    // Rama B:
    // cualquier clic sobre el diálogo avanza
    clicEnPlay = true;

} else if (ramaAActiva) {

    // Rama A: PLAY está a la derecha
    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;

} else if (ramaCActiva) {

    // Rama C: toda la consecuencia se puede clicar
    clicEnPlay = true;

} else if (ramaDActiva) {

    // Rama D: PLAY está a la derecha
    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;

} else if (esMateo) {

    // Diálogos de Mateo: PLAY a la izquierda
    clicEnPlay =
            x <= ancho * 0.28
            && y >= alto * 0.55;

} else {

    // Doña Rosa y Presentador: PLAY a la derecha
    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;
}


if (clicEnPlay) {
   
if (ramaBActiva) {

    if (ramaBViral && dialogoActual == 92) {

        dialogoActual = 93;
        mostrarDialogo(dialogoActual);
        return;
    }


    if (ramaBViral && dialogoActual == 93) {

        ramaBActiva = false;
        consecuenciaRamaBActiva = true;
        
        dialogoActivo = false;
        dialogoJuego.setVisible(false);
        mostrarConsecuenciaRamaB(true);

        System.out.println("=================================");
        System.out.println("RAMA B - SE VOLVIÓ VIRAL");
        System.out.println("=================================");


        return;
    }

    if (!ramaBViral && dialogoActual == 94) {

        dialogoActual = 95;

        mostrarDialogo(dialogoActual);

        return;
    }

    if (!ramaBViral && dialogoActual == 95) {

        ramaBActiva = false;
        consecuenciaRamaBActiva = true;
        dialogoActivo = false;
        dialogoJuego.setVisible(false);

        mostrarConsecuenciaRamaB(false);
        
        System.out.println("=================================");
        System.out.println("RAMA B - NO SE VOLVIÓ VIRAL");
        System.out.println("=================================");

        return;
    }
}


   
    if (ramaAActiva) {

        ramaAActiva = false;

        dialogoActivo = false;
        dialogoJuego.setVisible(false);

        // Aplicar efectos de la decisión
        if (nodoActual != null) {
            estadoJuego.aplicarEfecto(nodoActual.efecto);
        }

        System.out.println("=================================");
        System.out.println("RAMA A COMPLETADA");
        System.out.println("Reporte confirmado.");
        System.out.println("Efectos aplicados:");
        System.out.println("Confianza +8");
        System.out.println("Información verificada +5");
        System.out.println("Desinformación -3");
        System.out.println("=================================");

        estadoJuego.imprimirEstadoPartida();

        // Mostrar coleccionable
        mostrarColeccionableRamaA();

        return;
    }
    
        if (ramaCActiva) {

        ramaCActiva = false;

        dialogoActivo = false;
        dialogoJuego.setVisible(false);

        // Aplicar los efectos del resultado elegido
        if (nodoActual != null) {
            estadoJuego.aplicarEfecto(nodoActual.efecto);
        }

        System.out.println("=================================");
        System.out.println("RAMA C COMPLETADA");
        System.out.println("Reporte evaluado.");
        System.out.println("=================================");

        estadoJuego.imprimirEstadoPartida();

        // Si acertó, mostrar coleccionable
        if (nodoActual != null
                && nodoActual.id.equals("pub1_reportar_acerto")) {

            System.out.println("Reporte acertado.");
            System.out.println("Confianza +5");
            System.out.println("Información verificada +2");

            mostrarColeccionableRamaA();

        } else {

            // Si se equivocó, no hay coleccionable
            System.out.println("Reporte erróneo.");
            System.out.println("Reputación del jugador -4");

            mostrarFinDia7();
        }

        return;
    }
    
    if (ramaDActiva) {

        ramaDActiva = false;

        dialogoActivo = false;
        dialogoJuego.setVisible(false);

        System.out.println("=================================");
        System.out.println("RAMA D COMPLETADA");
        System.out.println("Tinto / calma.");
        System.out.println("Sin efectos en las estadísticas.");
        System.out.println("=================================");

        estadoJuego.imprimirEstadoPartida();

        // Mostrar coleccionable
        mostrarColeccionableRamaA();

        return;
    }

    if (dialogoActual >= 40 && dialogoActual < 49) {

        dialogoActual++;
        mostrarDialogo(dialogoActual);

    } else if (dialogoActual >= 57 && dialogoActual < 59) {

        dialogoActual++;
        mostrarDialogo(dialogoActual);

    } else if (dialogoActual == 49) {

        dialogoActivo = false;
        dialogoJuego.setVisible(false);

        javax.swing.Timer espera =
                new javax.swing.Timer(2000, i -> {

            dialogoActual = 57;
            dialogoActivo = true;

            mostrarDialogo(dialogoActual);
        });

        espera.setRepeats(false);
        espera.start();

    } else if (dialogoActual == 59) {

         dialogoActivo = false;
    dialogoJuego.setVisible(false);

    dialogoActual = 61;
    dialogoActivo = true;

    mostrarDialogo(dialogoActual);

        juego.revalidate();
        juego.repaint();
        
        } else if (dialogoActual ==61) {
            dialogoActivo = false;
            dialogoActual=62;
            dialogoActivo=true;
            
            mostrarDialogo(dialogoActual);
       } else if (dialogoActual == 62) {

    dialogoActivo = false;
    dialogoJuego.setVisible(false);

    dialogoActual = 63;
    dialogoActivo = true;

    mostrarDialogo(dialogoActual);

} else if (dialogoActual == 63) {

    dialogoActivo = false;
    dialogoJuego.setVisible(false);
    
    personajeJuego.setVisible(false);
    
    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    mostrarSituacion();
}
    
    } 
    
        }
    });

    juego.add(dialogoJuego);
}
    
    private void mostrarConsecuenciaRamaB(boolean viral) {

    String ruta;

    if (viral) {
        ruta = "/imagenes/dialogos/viral.png";
    } else {
        ruta = "/imagenes/dialogos/noviral.png";
    }

    try {

        BufferedImage original =
                ImageIO.read(
                        getClass().getResource(ruta)
                );

        int ancho = 500;

        double proporcion =
                (double) original.getHeight()
                / original.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                original.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                ancho,
                alto
        );

        int x =
                (juego.getWidth() - ancho) / 2;

        int y =
                (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(x, y);

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();
        
        javax.swing.Timer continuarRamaB =
        new javax.swing.Timer(
                2000,
                e -> {

                    dialogoJuego.setVisible(false);

                    // Aplicar los efectos de la decisión
                    if (nodoActual != null) {
                        estadoJuego.aplicarEfecto(
                                nodoActual.efecto
                        );
                    }

                    System.out.println("=================================");
                    System.out.println("RAMA B COMPLETADA");

                    if (viral) {

                        System.out.println("Resultado: VIRAL");
                        System.out.println("Desinformación +12");
                        System.out.println("Confianza -6");
                        System.out.println("Conflictos +4");

                    } else {

                        System.out.println("Resultado: NO VIRAL");
                        System.out.println("Desinformación -2");
                    }

                    System.out.println("=================================");

                    estadoJuego.imprimirEstadoPartida();

                    
                    mostrarColeccionableRamaA();
                }
        );

continuarRamaB.setRepeats(false);
continuarRamaB.start();

        System.out.println(
                viral
                ? "Consecuencia VIRAL mostrada."
                : "Consecuencia NO VIRAL mostrada."
        );

    } catch (IOException e) {

        e.printStackTrace();
    }
}
    
   private void mostrarSituacion() {
    
       situacionActiva = true;
       
    String ruta;

    if (twitterAbierto) {

        ruta = "/imagenes/situaciones/twitterabierto.png";

    } else if (mensajesAbierto) {

        ruta = "/imagenes/situaciones/mensajesabierto.png";

    } else if (perfilAbierto) {

        ruta = "/imagenes/situaciones/perfilabierto.png";

    } else if (celularAbierto) {

        ruta = "/imagenes/situaciones/telefonocon3apps.png";

    } else {

        ruta = "/imagenes/situaciones/telefonoguardado.png";
    }

    cambiarFondoJuego(ruta);

    juego.revalidate();
    juego.repaint();
}
   
   private void mostrarIconoPanico() {

    // Por ahora solamente activamos el JLabel
    iconoPanico.setVisible(true);

    colocarIconoPanico();

    juego.revalidate();
    juego.repaint();
}
   
   private void colocarIconoPanico() {

    if (iconoPanico == null) {
        return;
    }

    
    int mateoX = (int) (1415.0 / 1591.0 * juego.getWidth());
    int mateoY = (int) (380.0 / 989.0 * juego.getHeight());

   
    int x = mateoX - iconoPanico.getWidth() / 2;
    int y = mateoY - iconoPanico.getHeight();

    iconoPanico.setLocation(x, y);
}    
    
    
private void mostrarDialogo(int numero) {

    String ruta = "/imagenes/dialogos/" + numero + ".png";

    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original = ImageIO.read(recurso);

        // Buscar los límites de la parte visible del PNG
        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < original.getHeight(); y++) {

            for (int x = 0; x < original.getWidth(); x++) {

                int alpha = (original.getRGB(x, y) >> 24) & 0xFF;

                if (alpha > 10) {

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // Recortar solamente la burbuja visible
        BufferedImage recortada = original.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );

      // Tamaño de la burbuja en pantalla
int anchoDialogo;

if (numero == 43 || numero == 44 || numero == 47 || numero == 61 ||  numero == 92 || numero == 93 || numero == 94 ) {

    // Mateo
    anchoDialogo = 300;

} else {

    // Doña Rosa y Presentador
    anchoDialogo = 360;
}

        double proporcion =
                (double) recortada.getHeight()
                / recortada.getWidth();

        int altoDialogo =
                (int) (anchoDialogo * proporcion);

        Image imagenEscalada = recortada.getScaledInstance(
                anchoDialogo,
                altoDialogo,
                Image.SCALE_SMOOTH
        );

        dialogoJuego.setIcon(new ImageIcon(imagenEscalada));

        dialogoJuego.setSize(
                anchoDialogo,
                altoDialogo
        );


int xDialogo, yDialogo;

if (
numero == 57 || numero == 58 || numero == 59) {

    xDialogo = (int) (750.0 / 1591.0 * juego.getWidth())
            - anchoDialogo / 2;

    yDialogo = (int) (220.0 / 989.0 * juego.getHeight())
            - altoDialogo - 20;


} else if (numero == 43 || numero == 44 || numero == 47 || numero == 61 || numero == 63) {

    xDialogo = (int) (1390.0 / 1591.0 * juego.getWidth()) - 180;

    yDialogo = (int) (500.0 / 989.0 * juego.getHeight())
            - altoDialogo - 20;

} else if  (numero == 92
        || numero == 93
        || numero == 94) {

    // Mateo - Rama B
    int mateoX =
            (int) (1415.0 / 1591.0 * juego.getWidth());

    int mateoY =
            (int) (735.0 / 989.0 * juego.getHeight());

    xDialogo =
            mateoX - anchoDialogo / 2;

    yDialogo =
            mateoY - altoDialogo - 40;
} else {

    xDialogo = (int) (1190.0 / 1591.0 * juego.getWidth()) - 45;

    yDialogo = (int) (250.0 / 989.0 * juego.getHeight())
            - altoDialogo - 20;
}      
        

dialogoJuego.setLocation(xDialogo, yDialogo);
dialogoJuego.setVisible(true);

        juego.setComponentZOrder(dialogoJuego, 0);

        juego.revalidate();
        juego.repaint();

    } catch (IOException e) {
        e.printStackTrace();
    }
}
 
 private boolean estaCercaDeDonaRosa() {

    if (!dentroPanaderia) {
        return false;
    }

    double escalaX = 1591.0 / juego.getWidth();
    double escalaY = 989.0 / juego.getHeight();

    int jugadorX = (int) (
            (personajeJuego.getX()
            + personajeJuego.getWidth() / 2.0) * escalaX
    );

    int jugadorY = (int) (
            (personajeJuego.getY()
            + personajeJuego.getHeight() / 2.0) * escalaY
    );

    // Posición aproximada de Doña Rosa
    int donaRosaX = 1190;
    int donaRosaY = 250;

    double distancia = Math.sqrt(
            Math.pow(jugadorX - donaRosaX, 2)
            + Math.pow(jugadorY - donaRosaY, 2)
    );

    return distancia <= 180;
}
 
 private void mostrarDialogoRamaAReportado() {

    String ruta = "/imagenes/dialogos/ramaAReportado.png";

    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original = ImageIO.read(recurso);

        // Buscar solamente la parte visible del PNG
        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < original.getHeight(); y++) {

            for (int x = 0; x < original.getWidth(); x++) {

                int alpha = (original.getRGB(x, y) >> 24) & 0xFF;

                if (alpha > 10) {

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        BufferedImage recortada = original.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );

        // MISMO TAMAÑO QUE DOÑA ROSA
        int anchoDialogo = 360;

        double proporcion =
                (double) recortada.getHeight()
                / recortada.getWidth();

        int altoDialogo =
                (int) (anchoDialogo * proporcion);

        Image imagenEscalada = recortada.getScaledInstance(
                anchoDialogo,
                altoDialogo,
                Image.SCALE_SMOOTH
        );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                anchoDialogo,
                altoDialogo
        );

  
        int xDialogo =
                (int) (1190.0 / 1591.0 * juego.getWidth()) - 45;

        int yDialogo =
                (int) (250.0 / 989.0 * juego.getHeight())
                - altoDialogo - 20;

        dialogoJuego.setLocation(
        xDialogo,
        yDialogo
);

// Asegurarnos de que el diálogo esté dentro del juego
if (dialogoJuego.getParent() != juego) {
    juego.add(dialogoJuego);
}

// Mostrar el diálogo por encima de los demás elementos
dialogoJuego.setVisible(true);
juego.setComponentZOrder(dialogoJuego, 0);

juego.revalidate();
juego.repaint();

System.out.println("Diálogo Rama A mostrado.");
System.out.println("Posición X: " + xDialogo);
System.out.println("Posición Y: " + yDialogo);
System.out.println("Ancho: " + anchoDialogo);
System.out.println("Alto: " + altoDialogo);
System.out.println("Visible: " + dialogoJuego.isVisible());

    } catch (IOException e) {
        e.printStackTrace();
    }
}
 
 
 
 
 
 
 
 
 
 
 
 private void mostrarColeccionableRamaA() {

    String ruta =
            "/imagenes/dialogos/nuevocoleccionable.png";

    java.net.URL recurso =
            getClass().getResource(ruta);


    try {

        BufferedImage original =
                ImageIO.read(recurso);

        int ancho = 600;

        double proporcion =
                (double) original.getHeight()
                / original.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                original.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                ancho,
                alto
        );

        // Centrar en pantalla
        int x =
                (juego.getWidth() - ancho) / 2;

        int y =
                (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(x, y);

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();


        javax.swing.Timer timerColeccionable =
                new javax.swing.Timer(
                        2000,
                        e -> {

                            dialogoJuego.setVisible(false);

                            mostrarFinDia7();
                        }
                );

        timerColeccionable.setRepeats(false);
        timerColeccionable.start();

    } catch (IOException e) {

        e.printStackTrace();
    }
}
 
 private void mostrarFinDia7() {

    String ruta =
            "/imagenes/dialogos/findia7.png";

    java.net.URL recurso =
            getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original =
                ImageIO.read(recurso);

    

        int ancho = 800;

        double proporcion =
                (double) original.getHeight()
                / original.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                original.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(
                ancho,
                alto
        );

       

        int x =
                (juego.getWidth() - ancho) / 2;

        int y =
                (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(
                x,
                y
        );

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();

        System.out.println("Día 7 de 7 completado.");
        System.out.println("Regresando al inicio...");

       

        javax.swing.Timer timerFinDia =
                new javax.swing.Timer(
                        5000,
                        e -> {

                            dialogoJuego.setVisible(false);

                            volverAlInicioDespuesDia7();
                        }
                );

        timerFinDia.setRepeats(false);
        timerFinDia.start();

    } catch (IOException e) {

        e.printStackTrace();
    }
}
 
 private void volverAlInicioDespuesDia7() {

   
    dialogoJuego.setVisible(false);

    if (opciones != null) {
        opciones.setVisible(false);
    }

    if (opcionesSecundarias != null) {
        opcionesSecundarias.setVisible(false);
    }

    if (situacionMiniatura != null) {
        situacionMiniatura.setVisible(false);
    }

    if (iconoPanico != null) {
        iconoPanico.setVisible(false);
    }

  
    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    
    ramaAActiva = false;
    ramaCActiva = false;
    ramaDActiva = false;
    abanicoOpcionesVisible = false;
    dialogoActivo = false;
    publicacionFalsa=true;
    
    ramaBActiva = false;
    ramaBViral = false;
    consecuenciaRamaBActiva = false;
    
    teclaW = false;
teclaA = false;
teclaS = false;
teclaD = false;

// Reiniciar estado de la panadería
panaderiaAbierta = false;
dentroPanaderia = false;

// Reiniciar teléfono
celularAbierto = false;
twitterAbierto = false;
mensajesAbierto = false;
perfilAbierto = false;

// Reiniciar situación
situacionActiva = false;

// Volver a la raíz del árbol
nodoActual = arbolEscena1;
    
    


    card = (CardLayout) Panel_cambiante.getLayout();

    card.show(
            Panel_cambiante,
            "inicio"
    );

    Panel_cambiante.revalidate();
    Panel_cambiante.repaint();

    System.out.println("Se regresó a la pantalla de inicio.");
}
 
 private void mostrarConsecuenciaRamaC(boolean acerto) {

    String ruta;

    if (acerto) {
        ruta = "/imagenes/dialogos/reporteacertado.png";
    } else {
        ruta = "/imagenes/dialogos/reporteeroneo.png";
    }

    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original = ImageIO.read(recurso);

        // Buscar solamente la parte visible del PNG
        int minX = original.getWidth();
        int minY = original.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < original.getHeight(); y++) {

            for (int x = 0; x < original.getWidth(); x++) {

                int alpha =
                        (original.getRGB(x, y) >> 24) & 0xFF;

                if (alpha > 10) {

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        BufferedImage recortada = original.getSubimage(
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
        );

        // Tamaño de la consecuencia
        int ancho = 500;

        double proporcion =
                (double) recortada.getHeight()
                / recortada.getWidth();

        int alto =
                (int) (ancho * proporcion);

        Image imagenEscalada =
                recortada.getScaledInstance(
                        ancho,
                        alto,
                        Image.SCALE_SMOOTH
                );

        dialogoJuego.setIcon(
                new ImageIcon(imagenEscalada)
        );

        dialogoJuego.setSize(ancho, alto);

        // CENTRADO EN LA PANTALLA
        int x = (juego.getWidth() - ancho) / 2;
        int y = (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(x, y);

        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(dialogoJuego, 0);

        juego.revalidate();
        juego.repaint();

        if (acerto) {
            System.out.println("RAMA C: REPORTE ACERTADO");
        } else {
            System.out.println("RAMA C: REPORTE ERRÓNEO");
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
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
            
            if (dialogoActivo) {
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
            
            if (!dialogoActivo && !dialogoDonaRosaIniciado
        && estaCercaDeDonaRosa()) {

    dialogoActivo = true;
    dialogoDonaRosaIniciado = true;
    dialogoActual = 40;

    mostrarDialogo(dialogoActual);
}

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

    java.awt.Dimension pantalla =
            java.awt.Toolkit.getDefaultToolkit().getScreenSize();

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
    
    private void reiniciarPartida() {


    ramaAActiva = false;
    ramaCActiva = false;
    ramaDActiva = false;

    abanicoOpcionesVisible = false;
    dialogoActivo = false;
    dialogoDonaRosaIniciado = false;

    publicacionFalsa = true;

    nodoActual = arbolEscena1;

    estadoJuego = new EstadoJuego();



    teclaW = false;
    teclaA = false;
    teclaS = false;
    teclaD = false;



    panaderiaAbierta = false;
    dentroPanaderia = false;




    celularAbierto = false;
    twitterAbierto = false;
    mensajesAbierto = false;
    perfilAbierto = false;




    situacionActiva = false;


    if (dialogoJuego != null) {
        dialogoJuego.setVisible(false);
    }

    if (opciones != null) {
        opciones.setVisible(false);
    }

    if (opcionesSecundarias != null) {
        opcionesSecundarias.setVisible(false);
    }

    if (situacionMiniatura != null) {
        situacionMiniatura.setVisible(false);
    }

    if (iconoPanico != null) {
        iconoPanico.setVisible(false);
    }


    ImageIcon iconoPersonaje =
            new ImageIcon(
                    getClass().getResource(
                            "/imagenes/personajecivil.png"
                    )
            );

    Image imagenPersonaje =
            iconoPersonaje.getImage();

    Image imagenEscalada =
            imagenPersonaje.getScaledInstance(
                    140,
                    170,
                    Image.SCALE_SMOOTH
            );

    personajeJuego.setIcon(
            new ImageIcon(imagenEscalada)
    );

    personajeJuego.setSize(140, 170);

    personajeJuego.setLocation(
            700,
            600
    );

    cambiarFondoJuego(
            "/imagenes/exteriorsin.png"
    );


    juego.revalidate();
    juego.repaint();
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
        instrucciones = new javax.swing.JPanel();
        BTvolver3 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        Panel_cambiante.setLayout(new java.awt.CardLayout());

        iniciojuego.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTjugar.addActionListener(this::BTjugarActionPerformed);
        iniciojuego.add(BTjugar, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 550, 410, 100));

        BTsalir.addActionListener(this::BTsalirActionPerformed);
        iniciojuego.add(BTsalir, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 800, 410, 80));

        BTinst.addActionListener(this::BTinstActionPerformed);
        iniciojuego.add(BTinst, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 670, 410, 100));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Inicio.png"))); // NOI18N
        iniciojuego.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        Panel_cambiante.add(iniciojuego, "inicio");

        elegirroles.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver.addActionListener(this::BTvolverActionPerformed);
        elegirroles.add(BTvolver, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 770, 210, 50));

        BTcivil.addActionListener(this::BTcivilActionPerformed);
        elegirroles.add(BTcivil, new org.netbeans.lib.awtextra.AbsoluteConstraints(930, 310, 210, 170));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Roles.png"))); // NOI18N
        elegirroles.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1591, 989));
        jLabel2.getAccessibleContext().setAccessibleName("");

        Panel_cambiante.add(elegirroles, "roles");

        ponernombre.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver2.addActionListener(this::BTvolver2ActionPerformed);
        ponernombre.add(BTvolver2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 760, 210, 60));

        BTcontinuar.addActionListener(this::BTcontinuarActionPerformed);
        ponernombre.add(BTcontinuar, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 340, 450, 80));

        Digitarnombre.setFont(new java.awt.Font("Segoe UI", 2, 24)); // NOI18N
        Digitarnombre.setForeground(new java.awt.Color(124, 112, 88));
        Digitarnombre.addActionListener(this::DigitarnombreActionPerformed);
        ponernombre.add(Digitarnombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 250, 510, 70));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Nombre.png"))); // NOI18N
        ponernombre.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        Panel_cambiante.add(ponernombre, "nombre");

        javax.swing.GroupLayout juegoLayout = new javax.swing.GroupLayout(juego);
        juego.setLayout(juegoLayout);
        juegoLayout.setHorizontalGroup(
            juegoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1791, Short.MAX_VALUE)
        );
        juegoLayout.setVerticalGroup(
            juegoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 990, Short.MAX_VALUE)
        );

        Panel_cambiante.add(juego, "juego");

        instrucciones.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver3.addActionListener(this::BTvolver3ActionPerformed);
        instrucciones.add(BTvolver3, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 750, 280, 90));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/instrucciones.png"))); // NOI18N
        instrucciones.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1590, 990));

        Panel_cambiante.add(instrucciones, "inst");

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
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inst");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel4);
        });
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

    // Preparar el juego solamente la primera vez
    if (!juegoPreparado) {

        prepararJuego();
        prepararDialogos();

        juegoPreparado = true;

    } else {

        // Si ya había una partida anterior,
        // simplemente reiniciamos su estado
        reiniciarPartida();
    }

    juego.setFocusable(true);
    juego.requestFocusInWindow();

    if (timerMovimiento != null) {
        timerMovimiento.start();
    }

    System.out.println("NUEVA PARTIDA INICIADA");
    }//GEN-LAST:event_BTcontinuarActionPerformed

    private void BTvolver3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolver3ActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inicio");
    }//GEN-LAST:event_BTvolver3ActionPerformed

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
    private javax.swing.JButton BTvolver3;
    private javax.swing.JTextField Digitarnombre;
    private javax.swing.JPanel Panel_cambiante;
    private javax.swing.JPanel elegirroles;
    private javax.swing.JPanel iniciojuego;
    private javax.swing.JPanel instrucciones;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
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
