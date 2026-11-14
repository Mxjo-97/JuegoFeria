/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package juegoferia;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import javax.swing.SwingConstants;

/**
 *
 * @author Mary
 */
public class partvisual extends javax.swing.JFrame implements KeyListener {
    private final AnimacionPersonaje animacion = new AnimacionPersonaje();
private boolean mirandoIzquierda = false;
private static final boolean ESPEJAR_IZQUIERDA = true; 
    private IntroLlegada introLlegada;
    private CardLayout card;
    private int idPartida =0;
    private JLabel fondoJuego;
    private JLabel personajeJuego;
    private JLabel iconoPanico;
    private JLabel situacionMiniatura;
    private boolean abanicoOpcionesVisible = false;
    private JLabel opciones;
    private JLabel opcionesSecundarias;
    private boolean guionPanaderiaIniciado = false;
    private boolean guionMaleconIniciado = false;
    private boolean guionJuntaIniciado = false;   // <- nueva
    private boolean guionJuntaPiso2Iniciado = false;   // conversación del piso 2 (105 a 113)
    private boolean ramaAActiva = false;
    
    
    
    private boolean ramaTintoCalmaActiva = false;
    private boolean ramaDActiva = false;
    private boolean ramaCActiva = false;
    private boolean ramaBActiva = false;
    private boolean ramaBViral = false;
    private boolean consecuenciaRamaBActiva = false;
    private boolean publicacionFalsa = true;
    private Nodo arbolActual;
    private EstadoJuego estadoJuego;
    private Nodo nodoActual;
    private String nombreJugador = "";
    private JLabel nombrePerfilJuego;
    private boolean celularAbierto = false;
    private boolean twitterAbierto = false;
    private boolean mensajesAbierto = false;
    private boolean perfilAbierto = false;
    private boolean situacionActiva = false;
    private boolean panaderiaAbierta = false;
    private boolean dentroPanaderia = false;
    private boolean decisionEnCurso = false;
    private boolean imagenTemporizadaActiva = false;
    private boolean posicionesBotonesGuardadas = false;
    private int posicionExteriorX;
    private int posicionExteriorY;
    private int velocidad = 3;
    private BufferedImage mapaColisiones;
    private BufferedImage mapaColisionesCasa;
    private BufferedImage mapaInteracciones;
    private BufferedImage mapaClicsCelular;
    private BufferedImage mapaColisionesPanaderia;
    
    // NUEVAS VARIABLES - VALENTINA
        private BufferedImage mapaColisionesMalecon;
    private boolean maleconActivo = false;
       // ===== JUNTA DE ACCIÓN COMUNAL (exterior + piso 1 + piso 2) =====
    private BufferedImage mapaColisionesJuntaExterior;
    private BufferedImage mapaColisionesJuntaPiso1;
    private BufferedImage mapaColisionesJuntaPiso2;
    private boolean juntaActiva = false;
    private int juntaLugar = 0;                 // 0 = exterior, 1 = piso 1, 2 = piso 2
    private boolean puertaJuntaAbierta = false;
    private boolean salioDeLaPuertaJunta = false;   // para volver a entrar por la puerta abierta

    // ===== ALCALDÍA (exterior + interior) =====
    private BufferedImage mapaColisionesAlcaldia;
    private BufferedImage mapaColisionesInteriorAlcaldia;
    private boolean alcaldiaActiva = false;
    private int alcaldiaLugar = 0;                  // 0 = afuera (alcaldia.png / puertaabierta.png), 1 = interior
    private boolean puertaAlcaldiaAbierta = false;
    private boolean salioDelAmarilloAlcaldia = false;
    private boolean guionAlcaldiaIniciado = false;

    // Al llegar a un mapa nuevo, los colores de cambio de mapa no hacen nada
    // hasta que el personaje se salga de ellos (evita el bucle piso 1 <-> piso 2).
    private boolean esperandoSalirDeZona = false;

    // Historia final de la casa (azul en colisiones_casa.png)
    private boolean guionCasaFinalIniciado = false;

    // ===== MAPA DE LA ISLA (tecla M) =====
    private final GrafoLocaciones grafoIsla = GrafoLocaciones.crearMapaIsla();
    private JLabel mapaIsla;
    private boolean mapaAbierto = false;
    
    // VIEJAS VARIABLES 
    private boolean casaActiva = true;
    private boolean teclaW = false;
    private boolean teclaA = false;
    private boolean teclaS = false;
    private boolean teclaD = false;
    private javax.swing.Timer timerMovimiento;
    private static final String IMAGENOPCIONESDEVERIFICACION = "/imagenes/dialogos/opcionesverificacion.png";
    private static final String IMAGENOPCIONESDIFUNDIR = "/imagenes/dialogos/opcionesdifundir.png";
    private JLabel dialogoJuego;
    private boolean juegoPreparado = false;
    // Escenas no jugables (cinematicas) del motor de escenas: Escena / Paso / MotorGuion / PantallaJuego
    private PantallaJuego pantallaEscenas;
    private MotorGuion motorEscenas;
    private boolean dialogoActivo = false;
    private boolean dialogoDonaRosaIniciado = false;
    private int dialogoActual = 40;
    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(partvisual.class.getName());

        public partvisual() {
        this.setUndecorated(true);
        initComponents();
        
        // --- OCULTAR LOS BOTONES VIEJOS - VALENTINA ---
        BTjugar.setVisible(false);
        BTinst.setVisible(false);
        BTsalir.setVisible(false);
        // -------------------------------------------------------------
        
        setCursor(Cursores.NORMAL);

     
    arbolActual = ArbolPublicacion.construirArbolEscena1();
    estadoJuego = new EstadoJuego();
    nodoActual = arbolActual;
        
        this.addKeyListener(this);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        crearPantallasPorCodigo();
        CursorJuego.instalar(this);
        
           java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(evento -> {
    if (evento.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED
            && evento.getSource() instanceof javax.swing.AbstractButton boton
            && boton.isEnabled()
            && boton.isShowing()) {
        Funciones.reproducirSonido("/sonidos/click.wav");
    }
}, java.awt.AWTEvent.MOUSE_EVENT_MASK);
        
        
        
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
    private void crearPantallasPorCodigo() {

    Panel_cambiante.remove(iniciojuego);
    Panel_cambiante.remove(elegirroles);
    Panel_cambiante.remove(ponernombre);
    Panel_cambiante.remove(instrucciones);

    iniciojuego = crearPantallaInicio();
    elegirroles = crearPantallaRoles();
    ponernombre = crearPantallaNombre();
    instrucciones = crearPantallaInstrucciones();

    Panel_cambiante.add(iniciojuego, "inicio");
    Panel_cambiante.add(elegirroles, "roles");
    Panel_cambiante.add(ponernombre, "nombre");
    Panel_cambiante.add(instrucciones, "inst");

    pantallaEscenas = new PantallaJuego();
    pantallaEscenas.setAlSalir(this::salirDeEscenas);
    Panel_cambiante.add(pantallaEscenas, "escenas");
    

// intro llegada - VALENTINA
    introLlegada = new IntroLlegada();
Panel_cambiante.add(introLlegada, "intro");


    card = (CardLayout) Panel_cambiante.getLayout();

    card.show(Panel_cambiante, "inicio");

    Panel_cambiante.revalidate();
    Panel_cambiante.repaint();
}
  private javax.swing.JPanel crearPantallaRoles() {
    return new PantallaRoles(
            () -> card.show(Panel_cambiante, "inicio"),
            () -> card.show(Panel_cambiante, "nombre"));
}
  
    private javax.swing.JPanel crearPantallaNombre() {

    javax.swing.JPanel panel = new javax.swing.JPanel(null);

    javax.swing.JLabel fondo = new javax.swing.JLabel();

    ImageIcon icono = new ImageIcon(
            getClass().getResource("/imagenes/Nombre.png")
    );

    fondo.setIcon(icono);
    panel.add(fondo);

    BTvolver2 = crearBotonInvisible();

    BTcontinuar = crearBotonInvisible();

    Digitarnombre = new javax.swing.JTextField();


Digitarnombre.setOpaque(false);
Digitarnombre.setBorder(null);


Digitarnombre.setBackground(new java.awt.Color(0, 0, 0, 0));


Digitarnombre.setForeground(new java.awt.Color(80, 60, 40));


Digitarnombre.setCaretColor(new java.awt.Color(80, 60, 40));


Digitarnombre.setFont(
        new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 28)
);

    panel.add(BTvolver2);
    panel.add(BTcontinuar);
    panel.add(Digitarnombre);

    BTvolver2.addActionListener(e -> {
        card.show(Panel_cambiante, "roles");
    });

    BTcontinuar.addActionListener(e -> {

    String nombre = Digitarnombre.getText().trim();

    
    if (nombre.isEmpty()) {

        System.out.println("Debes escribir un nombre.");

        Digitarnombre.requestFocusInWindow();

        return;
    }
    nombreJugador = nombre; 
    if (pantallaEscenas != null) pantallaEscenas.setNombreJugador(nombreJugador); 
    System.out.println("Nombre del jugador: " + nombre);

    iniciarIntroLlegada();;
});

    panel.addComponentListener(new java.awt.event.ComponentAdapter() {

        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            ajustarPantallaNombre(panel, fondo);
        }
    });

    return panel;
}
    private void ajustarPantallaNombre(
        javax.swing.JPanel panel,
        javax.swing.JLabel fondo) {

    int ancho = panel.getWidth();
    int alto = panel.getHeight();

    if (ancho <= 0 || alto <= 0) {
        return;
    }

    double escalaX = ancho / 1591.0;
    double escalaY = alto / 989.0;

    fondo.setBounds(0, 0, ancho, alto);

    ImageIcon original = new ImageIcon(
            getClass().getResource("/imagenes/Nombre.png")
    );

    Image imagenEscalada = original.getImage()
            .getScaledInstance(
                    ancho,
                    alto,
                    Image.SCALE_SMOOTH
            );

    fondo.setIcon(new ImageIcon(imagenEscalada));

    BTvolver2.setBounds(
            (int)(50 * escalaX),
            (int)(880 * escalaY),
            (int)(210 * escalaX),
            (int)(60 * escalaY)
    );

    BTcontinuar.setBounds(
            (int)(570 * escalaX),
            (int)(790 * escalaY),
            (int)(450 * escalaX),
            (int)(80 * escalaY)
    );

    Digitarnombre.setBounds(
            (int)(550 * escalaX),
            (int)(630 * escalaY),
            (int)(510 * escalaX),
            (int)(70 * escalaY)
    );

    panel.setComponentZOrder(fondo, panel.getComponentCount() - 1);

    panel.revalidate();
    panel.repaint();
}
    private javax.swing.JPanel crearPantallaInstrucciones() {

    javax.swing.JPanel panel = new javax.swing.JPanel(null);

    javax.swing.JLabel fondo = new javax.swing.JLabel();

    ImageIcon icono = new ImageIcon(
            getClass().getResource("/imagenes/instrucciones.png")
    );

    fondo.setIcon(icono);
    panel.add(fondo);

    BTvolver3 = crearBotonInvisible();

    panel.add(BTvolver3);

    BTvolver3.addActionListener(e -> {
        card.show(Panel_cambiante, "inicio");
    });

    panel.addComponentListener(new java.awt.event.ComponentAdapter() {

        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            ajustarPantallaInstrucciones(panel, fondo);
        }
    });

    return panel;
}
    private void ajustarPantallaInstrucciones(
        javax.swing.JPanel panel,
        javax.swing.JLabel fondo) {

    int ancho = panel.getWidth();
    int alto = panel.getHeight();

    if (ancho <= 0 || alto <= 0) {
        return;
    }

    double escalaX = ancho / 1591.0;
    double escalaY = alto / 989.0;

    fondo.setBounds(0, 0, ancho, alto);

    ImageIcon original = new ImageIcon(
            getClass().getResource("/imagenes/instrucciones.png")
    );

    Image imagenEscalada = original.getImage()
            .getScaledInstance(
                    ancho,
                    alto,
                    Image.SCALE_SMOOTH
            );

    fondo.setIcon(new ImageIcon(imagenEscalada));

    BTvolver3.setBounds(
            (int)(40 * escalaX),
            (int)(860 * escalaY),
            (int)(280 * escalaX),
            (int)(90 * escalaY)
    );

    panel.setComponentZOrder(fondo, panel.getComponentCount() - 1);

    panel.revalidate();
    panel.repaint();
}
    
  
    
    
  
    
    
    
    
      private javax.swing.JPanel crearPantallaInicio() {
        // En lugar de crear botones invisibles, devolvemos tu nueva clase animada
        return new PantallaInicio(
                () -> card.show(Panel_cambiante, "roles"),
                () -> card.show(Panel_cambiante, "inst"),
                () -> System.exit(0),
                () -> {
                    // Acción del botón de Minijuegos
                    System.out.println("¡Abriendo sección de MINIJUEGOS!");
                    // Cuando tengas el panel de minijuegos listo, cambia esto por:
                    // card.show(Panel_cambiante, "minijuegos");
                }
        );
    }
      
      
      
      
    private javax.swing.JButton crearBotonInvisible() {

    javax.swing.JButton boton = new javax.swing.JButton();

    boton.setContentAreaFilled(false);
    boton.setBorderPainted(false);
    boton.setFocusPainted(false);
    boton.setOpaque(false);

    
    
    boton.setCursor(Cursores.MANO);
    
    return boton;
}   
//    private void ajustarPantallaInicio(
//        javax.swing.JPanel panel,
//        javax.swing.JLabel fondo) {
//
//    int ancho = panel.getWidth();
//    int alto = panel.getHeight();
//
//    if (ancho <= 0 || alto <= 0) {
//        return;
//    }
//
//    double escalaX = ancho / 1591.0;
//    double escalaY = alto / 989.0;
//
//
//    fondo.setBounds(
//            0,
//            0,
//            ancho,
//            alto
//    );
//
//    ImageIcon original = new ImageIcon(
//            getClass().getResource("/imagenes/Inicio.png")
//    );
//
//    Image imagenEscalada = original.getImage()
//            .getScaledInstance(
//                    ancho,
//                    alto,
//                    Image.SCALE_SMOOTH
//            );
//
//    fondo.setIcon(new ImageIcon(imagenEscalada));
//    BTjugar.setBounds(
//            (int)(600 * escalaX),
//            (int)(470 * escalaY),
//            (int)(420 * escalaX),
//            (int)(120 * escalaY)
//    );
//    BTinst.setBounds(
//            (int)(600 * escalaX),
//            (int)(610 * escalaY),
//            (int)(420 * escalaX),
//            (int)(110 * escalaY)
//    );
//    BTsalir.setBounds(
//            (int)(590 * escalaX),
//            (int)(740 * escalaY),
//            (int)(440 * escalaY),
//            (int)(110 * escalaY)
//    );
//
//    panel.setComponentZOrder(fondo, panel.getComponentCount() - 1);
//
//    panel.revalidate();
//    panel.repaint();
//}
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
            mapaColisionesCasa = ImageIO.read(
        getClass().getResourceAsStream(
                "/imagenes/colisiones_casa.png"
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
        
        
        
        // CARGAR MAPA MALECÓN - VALENTINA
        
                mapaColisionesMalecon = cargarMapa("/imagenes/colisiones_malecon.png");
                        // MAPAS DE COLISIÓN DE LA JUNTA
        mapaColisionesJuntaExterior = cargarMapa("/imagenes/colisiones_junta_exterior.png");
        mapaColisionesJuntaPiso1 = cargarMapa("/imagenes/colisiones_junta_piso1.png");
        mapaColisionesJuntaPiso2 = cargarMapa("/imagenes/colisiones_junta_piso2.png");

        // MAPAS DE COLISIÓN DE LA ALCALDÍA
        mapaColisionesAlcaldia = cargarMapa("/imagenes/colisiones_alcaldia.png");
        mapaColisionesInteriorAlcaldia = cargarMapa("/imagenes/colisiones_interioralcaldia.png");
        
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
            
                
if (situacionActiva) {

    boolean clicEnPlaySituacion =
            x >= juego.getWidth() * 0.70
            && x <= juego.getWidth() * 0.88
            && y >= juego.getHeight() * 0.58
            && y <= juego.getHeight() * 0.84;

    if (clicEnPlaySituacion) {
        
        Funciones.reproducirSonido("/sonidos/click.wav");
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
                getClass().getResource("/imagenes/casapuertasin.png")
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
        nombrePerfilJuego = new JLabel();
nombrePerfilJuego.setHorizontalAlignment(SwingConstants.CENTER);
nombrePerfilJuego.setVerticalAlignment(SwingConstants.CENTER);

nombrePerfilJuego.setFont(new Font("Arial", Font.BOLD, 22));
nombrePerfilJuego.setForeground(new Color(70, 70, 100));

nombrePerfilJuego.setBounds(
        (int)(35.0 / 1591.0 * juego.getWidth()),
        (int)(770.0 / 989.0 * juego.getHeight()),
        (int)(210.0 / 1591.0 * juego.getWidth()),
        (int)(45.0 / 989.0 * juego.getHeight())
);

nombrePerfilJuego.setVisible(false);

juego.add(nombrePerfilJuego);
juego.setComponentZOrder(nombrePerfilJuego, 0);

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

        // Si ya se está eligiendo o ya se eligió, no se hace nada
        if (decisionEnCurso || abanicoOpcionesVisible) {
            return;
        }

        abanicoOpcionesVisible = true;

        situacionMiniatura.setVisible(true);
                Funciones.reproducirSonido("/sonidos/sonidos.wav");
        opciones.setVisible(true);

        int mateoX = (int) (1415.0 / 1591.0 * juego.getWidth());
        int mateoY = (int) (735.0 / 989.0 * juego.getHeight());


        int opcionesX = mateoX - opciones.getWidth() / 2 - 30;
        int opcionesY = mateoY - opciones.getHeight() / 2 - 175;

        opciones.setLocation(opcionesX, opcionesY);

        juego.setComponentZOrder(opciones, 0);
        juego.setComponentZOrder(situacionMiniatura, 1);

        juego.revalidate();
        juego.repaint();
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
        10,
        300,
        186
);

situacionMiniatura.setVisible(false);

juego.add(situacionMiniatura);

opciones = new JLabel();
    

ImageIcon iconoOriginal = new ImageIcon(
        getClass().getResource("/imagenes/dialogos/opciones.png")
);

int anchoOpciones = 510;
int altoOpciones = 306;

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

            if (x <= ancho * 0.50 && y >= alto * 0.45) {

                System.out.println("Elegiste: SE VUELVE VIRAL");
                Funciones.reproducirSonido("/sonidos/click.wav");
                seleccionarDecision("viral");
                ramaBViral = true;

                // Se quita la situación, se conserva el pánico
                cerrarSituacion(true);

                dialogoActivo = true;
                dialogoActual = 92;
                mostrarDialogo(dialogoActual);

            } else if (x >= ancho * 0.50 && y >= alto * 0.45) {

                System.out.println("Elegiste: NO SE VUELVE VIRAL");
                Funciones.reproducirSonido("/sonidos/click.wav");
                seleccionarDecision("noviral");
                ramaBViral = false;

                // Se quita la situación, se conserva el pánico
                cerrarSituacion(true);

                dialogoActivo = true;
                dialogoActual = 94;
                mostrarDialogo(dialogoActual);
            }

            return;
        }

   
        if (x <= ancho * 0.50 && y >= alto * 0.55) {

            System.out.println("Elegiste REPORTAR (verificado)");
            Funciones.reproducirSonido("/sonidos/click.wav");
            seleccionarDecision("reportar");

            ramaAActiva = true;

            // Se quita la situación, se conserva el pánico
            cerrarSituacion(true);

            if (timerMovimiento != null) {
                timerMovimiento.stop();
            }

            mostrarDialogoRamaAReportado();

        } else if (x >= ancho * 0.50 && y >= alto * 0.55) {

            System.out.println("Elegiste TINTO / CALMA (verificado)");
            Funciones.reproducirSonido("/sonidos/click.wav");
            seleccionarDecision("ignorar");

            // Se quita la situación Y el pánico
            cerrarSituacion(false);

            if (timerMovimiento != null) {
                timerMovimiento.stop();
            }

            mostrarDialogoTintoCalma();
        }
    }
});

juego.add(opciones);
juego.setComponentZOrder(opciones, 0);

opciones.addMouseListener(new java.awt.event.MouseAdapter() {

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {

        // Si ya se tomó una decisión, ignorar más clics en el abanico
        if (decisionEnCurso) {
            return;
        }

        int x = e.getX();
        int y = e.getY();

        int ancho = opciones.getWidth();
        int alto = opciones.getHeight();


        if (x >= ancho * 0.25
                && x <= ancho * 0.75
                && y <= alto * 0.35) {

            System.out.println("Elegiste VERIFICAR");
            Funciones.reproducirSonido("/sonidos/click.wav");
            decisionEnCurso = true;

            // La situación sigue visible mientras se elige en el segundo menú
            seleccionarDecision("verificar");
            
            situacionMiniatura.setVisible(false);


        } else if (x <= ancho * 0.45
                && y >= alto * 0.30
                && y <= alto * 0.65) {

            System.out.println("Elegiste DIFUNDIR");
            Funciones.reproducirSonido("/sonidos/click.wav");
            decisionEnCurso = true;
            seleccionarDecision("difundir");
            
            situacionMiniatura.setVisible(false);

            ramaBActiva = true;

            if (timerMovimiento != null) {
                timerMovimiento.stop();
            }

            abanicoOpcionesVisible = false;
            
            cambiarImagenOpcionesSecundarias(IMAGENOPCIONESDIFUNDIR);

            opcionesSecundarias.setLocation(
                    (juego.getWidth() - opcionesSecundarias.getWidth()) / 2,
                    (juego.getHeight() - opcionesSecundarias.getHeight()) / 2
            );

            opcionesSecundarias.setVisible(true);

            juego.setComponentZOrder(opcionesSecundarias, 0);

            juego.revalidate();
            juego.repaint();

    
        } else if (x >= ancho * 0.55
                && y >= alto * 0.30
                && y <= alto * 0.65) {

            System.out.println("Elegiste REPORTAR");
            Funciones.reproducirSonido("/sonidos/click.wav");
            decisionEnCurso = true;

            seleccionarDecision("reportar");

            ramaCActiva = true;

            if (timerMovimiento != null) {
                timerMovimiento.stop();
            }

            // Se quita la situación pero se conserva el pánico
            cerrarSituacion(!publicacionFalsa);

            if (publicacionFalsa) {

                seleccionarDecision("acerto");
                mostrarConsecuenciaRamaC(true);

            } else {

                seleccionarDecision("equivoco");
                mostrarConsecuenciaRamaC(false);
            }

            juego.revalidate();
            juego.repaint();

        
        } else if (y >= alto * 0.65) {

            System.out.println("Elegiste TINTO / CALMA");
            Funciones.reproducirSonido("/sonidos/click.wav");
            decisionEnCurso = true;

            seleccionarDecision("ignorar");

            // Se quita la situación Y el pánico
            cerrarSituacion(false);

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
        imagenTemporizadaActiva = true;
        dialogoJuego.setVisible(true);

        juego.setComponentZOrder(
                dialogoJuego,
                0
        );

        juego.revalidate();
        juego.repaint();

        System.out.println("Aviso TINTO/CALMA mostrado.");

        javax.swing.Timer timerTintoCalma =
                new javax.swing.Timer(
                        2000,
                        e -> {
                            imagenTemporizadaActiva = false;
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
                            mostrarColeccionableDia1();
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

    // Bajar por el arbol: buscar el hijo que corresponde a la opcion elegida
    Nodo siguiente = nodoActual.buscarHijo(decision);

    if (siguiente == null) {
        System.out.println("La opcion '" + decision + "' no existe en " + nodoActual.id);
        return;
    }

    nodoActual = siguiente;

    // Si "verificar" tiene sub-opciones, se muestra el segundo menu
    if (decision.equals("verificar") && !nodoActual.esHoja()) {
        cambiarImagenOpcionesSecundarias(IMAGENOPCIONESDEVERIFICACION);
        opcionesSecundarias.setLocation(
                (juego.getWidth() - opcionesSecundarias.getWidth()) / 2,
                (juego.getHeight() - opcionesSecundarias.getHeight()) / 2
        );
        opcionesSecundarias.setVisible(true);
        juego.setComponentZOrder(opcionesSecundarias, 0);
        juego.revalidate();
        juego.repaint();
    }

    System.out.println("Decisión elegida: " + decision);
    System.out.println("Nodo actual: " + nodoActual.id);
    System.out.println("Descripción: " + nodoActual.descripcion);

    opciones.setVisible(false);
    abanicoOpcionesVisible = false;
}
    
    
    
        private void cerrarSituacion(boolean conservarPanico) {

        situacionMiniatura.setVisible(false);
        opciones.setVisible(false);
        opcionesSecundarias.setVisible(false);
        abanicoOpcionesVisible = false;

        iconoPanico.setVisible(conservarPanico);

        juego.revalidate();
        juego.repaint();
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
            
            if (imagenTemporizadaActiva) {
                return;
            }
            

            int ancho = dialogoJuego.getWidth();
            int alto = dialogoJuego.getHeight();
            
            
    if (consecuenciaRamaBActiva) {

    consecuenciaRamaBActiva = false;

    dialogoJuego.setVisible(false);
    dialogoActivo = false;

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


    mostrarColeccionableDia1();

    return;
}
            
        boolean esMateo =
        dialogoActual == 43
        || dialogoActual == 44
        || dialogoActual == 47
        || dialogoActual == 61
        || dialogoActual == 63
        || dialogoActual == 92
        || dialogoActual == 94;
        
        boolean esCliente1 = dialogoActual ==100
                || dialogoActual ==103;
        
                
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

    if (esMateo) {

        clicEnPlay = x <= ancho * 0.28
                && y >= alto * 0.55;
    } else {
        
        clicEnPlay = x >= ancho * 0.60
                && y >= alto * 0.40;
    }

} else if (ramaAActiva) {


    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;

}  else if (ramaDActiva) {


    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;

} else if (esMateo) {

    clicEnPlay =
            x <= ancho * 0.28
            && y >= alto * 0.55;

} else if (esCliente1) {
    
     clicEnPlay =
            x <= ancho * 0.28
            && y >= alto * 0.55;
    
} else {

    clicEnPlay =
            x >= ancho * 0.60
            && y >= alto * 0.40;
}

if (clicEnPlay) {
    
    Funciones.reproducirSonido("/sonidos/click.wav");
   
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


        mostrarColeccionableDia1();

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

        mostrarColeccionableDia1();

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
    
     else if (dialogoActual >= 100 && dialogoActual < 106) {

    // Conversación final
    dialogoActual++;
    mostrarDialogo(dialogoActual);

} else if (dialogoActual == 106) {

    // Terminó la conversación final: aviso de fin de día
    dialogoActivo = false;
    dialogoJuego.setVisible(false);

    mostrarFinDia7();
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
        imagenTemporizadaActiva = true;

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
                    
                    imagenTemporizadaActiva = false;
                    consecuenciaRamaBActiva = false;
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

                    
                    mostrarColeccionableDia1();
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

    String ruta;
    switch (numero) {
        case 100: ruta = "/imagenes/dialogos/cliente1dialogo1.png"; break;
        case 101: ruta = "/imagenes/dialogos/cliente2dialogo2.png"; break;
        case 102: ruta = "/imagenes/dialogos/cliente2dialogo3.png"; break;
        case 103: ruta = "/imagenes/dialogos/cliente1dialogo4.png"; break;
        case 104: ruta = "/imagenes/dialogos/cliente2dialogo5.png"; break;
        case 105: ruta = "/imagenes/dialogos/doñarosadialogo6.png"; break;
        case 106: ruta = "/imagenes/dialogos/doñarosadialogo7.png"; break;
        default: ruta = "/imagenes/dialogos/" + numero + ".png";
    }
    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original = ImageIO.read(recurso);


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

int anchoDialogo;

if (numero == 43 || numero == 44 || numero == 47 || numero == 61 || numero== 63 || numero == 92 || numero == 93 || numero == 94 ) {

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
  if (numero == 57) {
        Funciones.reproducirSonido("/sonidos/noticia.wav");
    }
    xDialogo = (int) (750.0 / 1591.0 * juego.getWidth())
            - anchoDialogo / 2;

    yDialogo = (int) (220.0 / 989.0 * juego.getHeight())
            - altoDialogo - 20;


} else if (numero == 43 || numero == 44 || numero == 47
        || numero == 61 || numero == 63
        || numero == 92 || numero == 94) {

    xDialogo = (int) (1390.0 / 1591.0 * juego.getWidth()) - 230 -60;

        yDialogo = (int) (500.0 / 989.0 * juego.getHeight())
            - altoDialogo - 50;

} else if (numero == 100 || numero == 103) {

    // Cliente 1: señora del sombrero (izquierda)
    xDialogo = (int) (60.0 / 1591.0 * juego.getWidth());

    yDialogo = (int) (620.0 / 989.0 * juego.getHeight())
            - altoDialogo;

} else if (numero == 101 || numero == 102 || numero == 104) {

    // Cliente 2: señor de corbata (centro)
    xDialogo = (int) (718.0 / 1591.0 * juego.getWidth());

    yDialogo = (int) (670.0 / 989.0 * juego.getHeight())
            - altoDialogo;

} else {

    xDialogo = (int) (1190.0 / 1591.0 * juego.getWidth()) - 45;

    yDialogo = (int) (250.0 / 989.0 * juego.getHeight())
            - altoDialogo - 20;
}      
        

dialogoJuego.setLocation(xDialogo, yDialogo);
dialogoJuego.setVisible(true);

        juego.setComponentZOrder(dialogoJuego, 0);
        

        if (iconoPanico != null && iconoPanico.isVisible()) {
            juego.setComponentZOrder(iconoPanico, 0);
        }

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


if (dialogoJuego.getParent() != juego) {
    juego.add(dialogoJuego);
}


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
 
 
 private void mostrarColeccionableDia1() {
     
     // Solo se muestra si la hoja a la que llegó el jugador trae coleccionable
    if (nodoActual == null || nodoActual.coleccionable == null) {
        dialogoActivo = true;
        dialogoActual = 100;
        mostrarDialogo(dialogoActual);   // sigue directo a la conversación final
        return;
    }
    
    System.out.println("Coleccionable obtenido: " + nodoActual.coleccionable.nombre);

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

        imagenTemporizadaActiva = true;
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
                            
                            imagenTemporizadaActiva = false;
                            dialogoJuego.setVisible(false);
                            
                            dialogoActivo= true;
                            dialogoActual=100;
                            mostrarDialogo(dialogoActual);
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
            "/imagenes/dialogos/dia1de7.png";

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
        
        imagenTemporizadaActiva = true;
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
                            
                            imagenTemporizadaActiva = false;
                            dialogoJuego.setVisible(false);
                            dialogoActivo=true;
                            dialogoActual= 100;
                            volverAlInicio();
                        }
                );

        timerFinDia.setRepeats(false);
        timerFinDia.start();

    } catch (IOException e) {

        e.printStackTrace();
    }
}
 
 
 private void mostrarConsecuenciaRamaC(boolean acerto) {

    String ruta;

    if (acerto) {
        ruta = "/imagenes/dialogos/reporteacertado.png";
    } else {
        ruta = "/imagenes/dialogos/reporteeroneo.png";
    }
    
javax.swing.Timer timerConsecuenciaC =
        new javax.swing.Timer(
                2000,
                e -> {
                    
                    imagenTemporizadaActiva = false;
                    dialogoJuego.setVisible(false);
                    dialogoActivo = false;
                    ramaCActiva = false;

                    // Aplicar los efectos
                    if (nodoActual != null) {
                        estadoJuego.aplicarEfecto(nodoActual.efecto);
                    }

                    System.out.println("=================================");
                    System.out.println("RAMA C COMPLETADA");
                    System.out.println("=================================");

                    estadoJuego.imprimirEstadoPartida();

                    mostrarColeccionableDia1();
                    
                }
        );

timerConsecuenciaC.setRepeats(false);
timerConsecuenciaC.start();

    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    try {

        BufferedImage original = ImageIO.read(recurso);

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

        // centrado en la pantalla
        int x = (juego.getWidth() - ancho) / 2;
        int y = (juego.getHeight() - alto) / 2;

        dialogoJuego.setLocation(x, y);
        
        imagenTemporizadaActiva = true;
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
 
 private void cambiarImagenOpcionesSecundarias(String ruta) {

    java.net.URL recurso = getClass().getResource(ruta);

    if (recurso == null) {
        System.out.println("No se encontró: " + ruta);
        return;
    }

    Image escalada = new ImageIcon(recurso).getImage()
            .getScaledInstance(500, 290, Image.SCALE_SMOOTH);

    opcionesSecundarias.setIcon(new ImageIcon(escalada));
    opcionesSecundarias.setSize(500, 290);
}
 
    private boolean esZonaSalidaPanaderia(int color) {

    int rojo = (color >> 16) & 0xFF;
    int verde = (color >> 8) & 0xFF;
    int azul = color & 0xFF;

    return rojo > 180 && verde < 100 && azul < 100;
}
    private void reproducirGuion(
        java.util.List<Paso> pasos,
        Runnable alTerminar) {

    if (pasos == null || pasos.isEmpty()) {
        if (alTerminar != null) {
            alTerminar.run();
        }
        return;
    }

    // Detener movimiento
    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    // Soltar teclas
    teclaW = false;
    teclaA = false;
    teclaS = false;
    teclaD = false;

    dialogoActivo = false;

    if (dialogoJuego != null) {
        dialogoJuego.setVisible(false);
    }

    pantallaEscenas.reiniciar();
    pantallaEscenas.setHud(estadoJuego);
    pantallaEscenas.setNombreJugador(nombreJugador);


    motorEscenas = new MotorGuion(
            pantallaEscenas,
            estadoJuego
    );

    card.show(Panel_cambiante, "escenas");
    pantallaEscenas.requestFocusInWindow();

    motorEscenas.iniciar(pasos, () -> {

        pantallaEscenas.reiniciar();
        motorEscenas = null;

        if (alTerminar != null) {
            alTerminar.run();
        }
    });
}
    
    
    
    
    
    
    
    private void actualizarSpritePersonaje(boolean caminando) {
    if (personajeJuego == null) {
        return;
    }
    if (teclaA && !teclaD) {
        mirandoIzquierda = true;
    } else if (teclaD && !teclaA) {
        mirandoIzquierda = false;
    }
    personajeJuego.setIcon(animacion.icono(
            caminando,
            personajeJuego.getWidth(),
            personajeJuego.getHeight(),
            ESPEJAR_IZQUIERDA && mirandoIzquierda));
}
    
    
    private void iniciarMovimiento() {
        if (timerMovimiento != null && timerMovimiento.isRunning()) {
          
            return;
        }
        timerMovimiento = new javax.swing.Timer(16, e -> {

            if (personajeJuego == null) {
                return;
            }
            
            if (dialogoActivo || mapaAbierto) {
                    actualizarSpritePersonaje(false);
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

// NUEVA VAINA - VALENTINA
                
double escalaX = (double) mapaColisionesPanaderia.getWidth() / juego.getWidth();
double escalaY = (double) mapaColisionesPanaderia.getHeight() / juego.getHeight();

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
    actualizarSpritePersonaje(false);
    return;
}

boolean seMovio = (x != personajeJuego.getX() || y != personajeJuego.getY());
personajeJuego.setLocation(x, y);
actualizarSpritePersonaje(seMovio);
            
            if (!guionPanaderiaIniciado
        && estaCercaDeDonaRosa()) {

    guionPanaderiaIniciado = true;

    reproducirGuion(
            GuionCiudadano.escenaPanaderia().build(),
            this::reanudarJuegoDespuesDeHistoria
    );

    return;
}
            
            
            
            // JUNTA: al acercarse a la puerta azul, se abre y entra al piso 1  - VALENTINA
            if (juntaActiva && juntaLugar == 0 && !puertaJuntaAbierta
                    && estaCercaDePuertaJunta(x, y)) {
                abrirPuertaJunta();
                return;
            }

            // JUNTA: la puerta ya está abierta (salió del piso 1). Primero tiene que
            // alejarse de la puerta y, si vuelve a ella, entra otra vez al piso 1.
            if (juntaActiva && juntaLugar == 0 && puertaJuntaAbierta) {
                boolean enPuerta = estaCercaDePuertaJunta(x, y);
                if (!enPuerta) {
                    salioDeLaPuertaJunta = true;
                } else if (salioDeLaPuertaJunta) {
                    salioDeLaPuertaJunta = false;
                    irAJunta(1, JUNTA_SPAWN_PISO1_DESDE_PUERTA);
                    return;
                }
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


// NUEVO REEMPLAZO - VALENTINA
    
BufferedImage mapaActual;

if (dentroPanaderia) {

    mapaActual = mapaColisionesPanaderia;

} else if (alcaldiaActiva) {

    mapaActual = (alcaldiaLugar == 0)
            ? mapaColisionesAlcaldia
            : mapaColisionesInteriorAlcaldia;

} else if (juntaActiva) {

    if (juntaLugar == 0) {
        mapaActual = mapaColisionesJuntaExterior;
    } else if (juntaLugar == 1) {
        mapaActual = mapaColisionesJuntaPiso1;
    } else {
        mapaActual = mapaColisionesJuntaPiso2;
    }

} else if (maleconActivo) {

    mapaActual = mapaColisionesMalecon;

} else if (casaActiva) {

    mapaActual = mapaColisionesCasa;

} else {

    mapaActual = mapaColisiones;
}

if (mapaActual == null) {
    return true;
}
    int izquierda = x + 35;
    int derecha = x + personajeJuego.getWidth() - 35;
    int pies = y + personajeJuego.getHeight() - 10;

 double escalaX = (double) mapaActual.getWidth() / juego.getWidth();
double escalaY = (double) mapaActual.getHeight() / juego.getHeight();

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

    // RECIÉN LLEGÓ A ESTE MAPA: si está parado sobre un color de cambio de mapa,
    // se puede caminar por encima pero NO cambia de mapa hasta que se salga de él.
    if (esperandoSalirDeZona) {
        boolean sobreZona = esZonaDeCambio(colorIzquierda)
                || esZonaDeCambio(colorCentro)
                || esZonaDeCambio(colorDerecha);
        if (sobreZona) {
            return true;
        }
        esperandoSalirDeZona = false;   // ya se salió: desde ahora los colores funcionan normal
    }

    // CASA (casapuertasin.png): azul -> historia final. Al terminar, se cierra el juego.
    if (casaActiva && !guionCasaFinalIniciado
            && (esZonaAzul(colorIzquierda)
            || esZonaAzul(colorCentro)
            || esZonaAzul(colorDerecha))) {

        guionCasaFinalIniciado = true;
        System.out.println("Casa: empieza la historia final");

        reproducirGuion(
            GuionCiudadano.escenaCasaFinal().build(),
            () -> {
                System.out.println("Fin del juego");
                System.exit(0);   // cierra el juego
            }
        );
        return false;
    }

    
    
    // NUEVA DETECCIÓN DE COLORES - VALENTINA
    
    if (!casaActiva && !dentroPanaderia && !maleconActivo && !juntaActiva && !alcaldiaActiva) {

        // verde -> alcaldía
        if (esZonaVerdeAlcaldia(colorIzquierda)
                || esZonaVerdeAlcaldia(colorCentro)
                || esZonaVerdeAlcaldia(colorDerecha)) {
            irAAlcaldia(0, ALCALDIA_SPAWN_DESDE_PANADERIA, false);
            return false;
        }
    
    
        if (esZonaMalecon(colorIzquierda)
                || esZonaMalecon(colorCentro)
                || esZonaMalecon(colorDerecha)) {
            cambiarAMalecon();
            return false;
        }

        if (esZonaCasitas(colorIzquierda)
                || esZonaCasitas(colorCentro)
                || esZonaCasitas(colorDerecha)) {
            cambiarACasa();
            return false;
        }
    }
    
    
    
        // MALECÓN -> EXTERIOR DE LA JUNTA (zona amarilla en colisiones_malecon.png)
    if (maleconActivo
            && (esZonaAmarilla(colorIzquierda)
            || esZonaAmarilla(colorCentro)
            || esZonaAmarilla(colorDerecha))) {
        irAJunta(0, JUNTA_SPAWN_EXTERIOR_DESDE_MALECON);
        return false;
    }

    // MALECÓN -> EXTERIOR DE LA PANADERÍA con la puerta abierta (zona azul en colisiones_malecon.png)
    if (maleconActivo
            && (esZonaAzul(colorIzquierda)
            || esZonaAzul(colorCentro)
            || esZonaAzul(colorDerecha))) {
        volverDelMaleconAPanaderia();
        return false;
    }

    // ALCALDÍA
    //   Afuera:   azul = vuelve a la panadería (puertasin.png)
    //             amarillo = 1ª vez abre la puerta (puertaabierta.png), 2ª vez entra al interior
    //   Interior: azul = sale a la alcaldía con la puerta abierta (puertaabierta.png)
    if (alcaldiaActiva) {
        boolean amarilloA = esZonaAmarilla(colorIzquierda)
                || esZonaAmarilla(colorCentro)
                || esZonaAmarilla(colorDerecha);
        boolean azulA = esZonaAzul(colorIzquierda)
                || esZonaAzul(colorCentro)
                || esZonaAzul(colorDerecha);

        if (alcaldiaLugar == 0) {

            if (azulA) {
                volverDeAlcaldiaAPanaderia();
                return false;
            }

            if (amarilloA) {
                if (!puertaAlcaldiaAbierta) {
                    // primera vez: solo se abre la puerta
                    puertaAlcaldiaAbierta = true;
                    salioDelAmarilloAlcaldia = false;
                    actualizarImagenCelular();      // muestra puertaabierta.png
                    juego.repaint();
                    System.out.println("Se abrió la puerta de la alcaldía");
                    return true;
                }
                if (salioDelAmarilloAlcaldia) {
                    // ya estaba abierta y volvió a tocar el amarillo: entra
                    irAAlcaldia(1, ALCALDIA_SPAWN_INTERIOR, true);
                    return false;
                }
                return true;   // sigue parado en el amarillo después de abrir
            }

            // se alejó del amarillo: la próxima vez que lo toque, entra
            salioDelAmarilloAlcaldia = true;

        } else if (alcaldiaLugar == 1) {

            if (azulA) {
                irAAlcaldia(0, ALCALDIA_SPAWN_DESDE_INTERIOR, true);
                return false;
            }
        }
    }

    // DENTRO DE LA JUNTA
    //   Piso 1: azul = salida a la calle (puerta abierta), amarillo = sube al piso 2
    //   Piso 2: azul = baja al piso 1
    if (juntaActiva) {
        boolean amarillo = esZonaAmarilla(colorIzquierda)
                || esZonaAmarilla(colorCentro)
                || esZonaAmarilla(colorDerecha);
        boolean verde = esZonaVerde(colorIzquierda)
                || esZonaVerde(colorCentro)
                || esZonaVerde(colorDerecha);
        boolean azul = esZonaAzul(colorIzquierda)
                || esZonaAzul(colorCentro)
                || esZonaAzul(colorDerecha);

        if (juntaLugar == 0) {
            if (azul) {                           // afuera de la junta: azul -> vuelve al malecón
                volverAlMaleconDesdeJunta();
                return false;
            }
        } else if (juntaLugar == 1) {
            if (azul) {                           // salida del piso 1 -> calle, con la puerta abierta
                irAJunta(0, JUNTA_SPAWN_EXTERIOR_DESDE_PISO1, true);
                return false;
            }
            if (amarillo) {                       // escalera: sube al piso 2
                irAJunta(2, JUNTA_SPAWN_PISO2);
                return false;
            }
        } else if (juntaLugar == 2) {
            if (azul) {                           // escalera: baja al piso 1
                irAJunta(1, JUNTA_SPAWN_PISO1_DESDE_PISO2);
                return false;
            }
        }
    }
    
    
    
    
    // Detectar si alguno de los puntos está en la zona roja
    boolean rojoIzquierda = esZonaPuerta(colorIzquierda);
    boolean rojoCentro = esZonaPuerta(colorCentro);
    boolean rojoDerecha = esZonaPuerta(colorDerecha);

    boolean estaEnPuerta = rojoIzquierda
            || rojoCentro
            || rojoDerecha;
    
    // Si está en la casa y llega a la zona roja,
// pasar al exterior de la panadería.
if (casaActiva && estaEnPuerta) {

    cambiarAExteriorPanaderia();

    return false;
}
    
    
    
    
    

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
    
    
    
    
    // METODO NUEVO - VALENTINA
    
    
        private boolean esZonaMalecon(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        // El azul tiene que ser el color más fuerte: así el borde del verde
        // (que se mezcla con el blanco y queda verde-menta) ya no cuenta como azul.
        return azul > 170 && verde > rojo + 25 && rojo < 160 && azul > verde + 20;
    }

    private boolean esZonaCasitas(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        return azul > 160 && rojo > verde && rojo > 100 && verde < 160;
    }

    
    
    
    
    
        // ===================== JUNTA DE ACCIÓN COMUNAL - VALENTINA =====================

    // Dónde aparece el personaje (esquina superior izquierda), en coordenadas de diseño 1920 x 1080.
    // Los PIES del personaje (parte de abajo) tienen que caer sobre BLANCO en el mapa de colisiones.
    private static final int[] JUNTA_SPAWN_EXTERIOR_DESDE_MALECON = {365, 620};
    private static final int[] JUNTA_SPAWN_EXTERIOR_DESDE_PISO1 = {1063, 730};
    private static final int[] JUNTA_SPAWN_PISO1_DESDE_PUERTA = {885, 700};   // AJUSTAR al dibujo
    private static final int[] JUNTA_SPAWN_PISO1_DESDE_PISO2 = {1400, 500};   // AJUSTAR al dibujo
    private static final int[] JUNTA_SPAWN_PISO2 = {500, 500};                // AJUSTAR al dibujo

    // Zona frente a la puerta azul (donde se abre), medida sobre el dibujo a 1920 x 1080.
    private static final java.awt.Rectangle ZONA_PUERTA_JUNTA =
            new java.awt.Rectangle(1000, 690, 280, 130);

    private boolean esZonaAmarilla(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        return rojo > 200 && verde > 200 && azul < 100;
    }

    private boolean esZonaVerde(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        return verde > 200 && rojo < 100 && azul < 100;
    }

    // ===================== MALECÓN -> PANADERÍA (zona azul) =====================

    // Dónde aparece el personaje al volver del malecón (esquina superior izquierda),
    // en coordenadas de diseño 1591 x 989. Queda a la izquierda de la zona azul del exterior.
    private static final int[] PANADERIA_SPAWN_DESDE_MALECON = {1000, 560};

    /** Azul de colisiones_malecon.png (sirve para azul claro y azul puro). */
    private boolean esZonaAzul(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        return azul > 150 && rojo < 150 && azul > rojo + 40 && azul > verde + 20;
    }

    /** Vuelve al exterior de la panadería con la puerta abierta (puertasin.png). */
    private void volverDelMaleconAPanaderia() {
        maleconActivo = false;
        juntaActiva = false;
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = true;     // así se muestra puertasin.png
        esperandoSalirDeZona = true;

        personajeJuego.setLocation(
                (int) (PANADERIA_SPAWN_DESDE_MALECON[0] / 1591.0 * juego.getWidth()),
                (int) (PANADERIA_SPAWN_DESDE_MALECON[1] / 989.0 * juego.getHeight())
        );

        actualizarImagenCelular();   // como panaderiaAbierta = true, pone puertasin.png
        acomodarEnZonaCaminable();
        juego.repaint();

        System.out.println("Regresaste del malecón a la panadería");
    }

    // ===================== QUE EL PERSONAJE NO QUEDE ATASCADO =====================

    /** El mapa de colisiones del lugar donde está el personaje ahora. */
    private BufferedImage mapaColisionActual() {
        if (dentroPanaderia) {
            return mapaColisionesPanaderia;
        } else if (alcaldiaActiva) {
            return (alcaldiaLugar == 0) ? mapaColisionesAlcaldia : mapaColisionesInteriorAlcaldia;
        } else if (juntaActiva) {
            if (juntaLugar == 0) {
                return mapaColisionesJuntaExterior;
            } else if (juntaLugar == 1) {
                return mapaColisionesJuntaPiso1;
            }
            return mapaColisionesJuntaPiso2;
        } else if (maleconActivo) {
            return mapaColisionesMalecon;
        } else if (casaActiva) {
            return mapaColisionesCasa;
        }
        return mapaColisiones;
    }

    /** true si los pies del personaje en (x, y) caen sobre blanco en el mapa actual. */
    private boolean esCaminable(int x, int y) {
        BufferedImage mapa = mapaColisionActual();
        if (mapa == null) {
            return true;
        }
        double escalaX = (double) mapa.getWidth() / juego.getWidth();
        double escalaY = (double) mapa.getHeight() / juego.getHeight();

        int izquierda = x + 35;
        int derecha = x + personajeJuego.getWidth() - 35;
        int pies = y + personajeJuego.getHeight() - 10;

        int[] puntosX = {
                (int) (izquierda * escalaX),
                (int) (((izquierda + derecha) / 2.0) * escalaX),
                (int) (derecha * escalaX)
        };
        int py = (int) (pies * escalaY);

        if (py < 0 || py >= mapa.getHeight()) {
            return false;
        }
        for (int px : puntosX) {
            if (px < 0 || px >= mapa.getWidth()) {
                return false;
            }
            int rgb = mapa.getRGB(px, py);
            int r = (rgb >> 16) & 0xFF;
            if (r <= 200) {   // misma regla que puedeMoverse: negro = pared
                return false;
            }
        }
        return true;
    }

    /**
     * Si el personaje apareció encima de negro (pared), lo mueve al punto blanco
     * más cercano. Así nunca queda congelado al cambiar de mapa.
     */
    private void acomodarEnZonaCaminable() {
        int x0 = personajeJuego.getX();
        int y0 = personajeJuego.getY();

        if (esCaminable(x0, y0)) {
            return;
        }

        BufferedImage mapa = mapaColisionActual();
        if (mapa != null) {
            int px = (int) ((x0 + personajeJuego.getWidth() / 2.0) * mapa.getWidth() / juego.getWidth());
            int py = (int) ((y0 + personajeJuego.getHeight() - 10.0) * mapa.getHeight() / juego.getHeight());
            if (px >= 0 && py >= 0 && px < mapa.getWidth() && py < mapa.getHeight()) {
                int rgb = mapa.getRGB(px, py);
                System.out.println("OJO: el personaje apareció sobre un color que no es blanco: RGB("
                        + ((rgb >> 16) & 0xFF) + ", " + ((rgb >> 8) & 0xFF) + ", " + (rgb & 0xFF)
                        + "). Buscando el blanco más cercano...");
            }
        }

        int maxX = juego.getWidth() - personajeJuego.getWidth();
        int maxY = juego.getHeight() - personajeJuego.getHeight();

        for (int radio = 5; radio <= 600; radio += 5) {
            for (int dx = -radio; dx <= radio; dx += 5) {
                for (int dy = -radio; dy <= radio; dy += 5) {
                    if (Math.abs(dx) != radio && Math.abs(dy) != radio) {
                        continue;   // solo el borde del cuadro, del más cercano al más lejano
                    }
                    int x = x0 + dx;
                    int y = y0 + dy;
                    if (x < 0 || y < 0 || x > maxX || y > maxY) {
                        continue;
                    }
                    if (esCaminable(x, y)) {
                        personajeJuego.setLocation(x, y);
                        System.out.println("Personaje acomodado en x=" + x + ", y=" + y);
                        return;
                    }
                }
            }
        }

        System.out.println("OJO: no encontré zona blanca cerca. Revisa que el mapa de colisiones "
                + "tenga el camino en BLANCO puro (255, 255, 255).");
    }

    // ===================== ALCALDÍA =====================

    // Dónde aparece el personaje (esquina superior izquierda), en coordenadas de diseño 1591 x 989.
    // Si cae sobre negro, acomodarEnZonaCaminable() lo mueve solo. AJÚSTALOS a tu dibujo.
    private static final int[] ALCALDIA_SPAWN_DESDE_PANADERIA = {1000, 560};   // cerca del azul (regreso)
    private static final int[] ALCALDIA_SPAWN_DESDE_INTERIOR = {400, 650};     // debajo de la puerta (amarillo)
    private static final int[] ALCALDIA_SPAWN_INTERIOR = {700, 650};           // dentro de la alcaldía
    private static final int[] PANADERIA_SPAWN_DESDE_ALCALDIA = {460, 480};    // al lado del verde

    /** Verde de colisiones_exterior.png (el camino a la alcaldía). Acepta verde claro y oscuro. */
    private boolean esZonaVerdeAlcaldia(int color) {
        int rojo = (color >> 16) & 0xFF;
        int verde = (color >> 8) & 0xFF;
        int azul = color & 0xFF;
        return verde > 120 && verde > rojo + 50 && verde > azul + 50;
    }

    // ===================== JUNTA -> MALECÓN (azul afuera de la junta) =====================

    // Dónde aparece el personaje al volver al malecón (esquina superior izquierda),
    // en coordenadas de diseño 1591 x 989. Queda al lado del amarillo que lleva a la junta.
    private static final int[] MALECON_SPAWN_DESDE_JUNTA = {1150, 250};

    /** Vuelve al malecón desde afuera de la junta. La historia del malecón no se repite. */
    private void volverAlMaleconDesdeJunta() {
        juntaActiva = false;
        juntaLugar = 0;
        puertaJuntaAbierta = false;
        alcaldiaActiva = false;
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = false;
        maleconActivo = true;
        esperandoSalirDeZona = true;   // si aparece sobre el amarillo, no vuelve de una a la junta

        cambiarTamanoPersonaje(TAMANO_PERSONAJE_NORMAL);

        personajeJuego.setLocation(
                (int) (MALECON_SPAWN_DESDE_JUNTA[0] / 1591.0 * juego.getWidth()),
                (int) (MALECON_SPAWN_DESDE_JUNTA[1] / 989.0 * juego.getHeight())
        );

        actualizarImagenCelular();     // muestra malecon.png
        acomodarEnZonaCaminable();
        juego.repaint();

        System.out.println("Regresaste de la junta al malecón");
    }

    /** true si el color es de los que cambian de mapa (amarillo, azul, verde o morado). */
    private boolean esZonaDeCambio(int color) {
        return esZonaAmarilla(color)
                || esZonaAzul(color)
                || esZonaVerde(color)
                || esZonaVerdeAlcaldia(color)
                || esZonaMalecon(color)
                || esZonaCasitas(color);
    }

    /** Lleva al jugador a la alcaldía: lugar 0 = afuera, 1 = interior. */
    private void irAAlcaldia(int lugar, int[] spawn, boolean puertaAbierta) {
        alcaldiaActiva = true;
        alcaldiaLugar = lugar;
        puertaAlcaldiaAbierta = (lugar == 1) || puertaAbierta;
        salioDelAmarilloAlcaldia = false;   // si aparece sobre el amarillo, no entra de una
        esperandoSalirDeZona = true;

        juntaActiva = false;
        maleconActivo = false;
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = false;

        // Dentro de la alcaldía el personaje es más grande; afuera, tamaño normal
        cambiarTamanoPersonaje(lugar == 1 ? TAMANO_PERSONAJE_ALCALDIA : TAMANO_PERSONAJE_NORMAL);

        personajeJuego.setLocation(
                (int) (spawn[0] / 1591.0 * juego.getWidth()),
                (int) (spawn[1] / 989.0 * juego.getHeight())
        );

        actualizarImagenCelular();
        acomodarEnZonaCaminable();
        juego.repaint();

        System.out.println("Alcaldía: " + (lugar == 0 ? "afuera" : "interior"));

        // La historia de la alcaldía empieza la primera vez que entra al interior
        if (lugar == 1 && !guionAlcaldiaIniciado) {
            guionAlcaldiaIniciado = true;
            reproducirGuion(
                GuionCiudadano.escenaAlcaldia().build(),
                this::reanudarJuegoDespuesDeHistoria
            );
        }
    }

    /** Azul de la alcaldía: vuelve al exterior de la panadería con la puerta abierta (puertasin.png). */
    private void volverDeAlcaldiaAPanaderia() {
        alcaldiaActiva = false;
        alcaldiaLugar = 0;
        juntaActiva = false;
        maleconActivo = false;
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = true;     // así se muestra puertasin.png
        esperandoSalirDeZona = true;

        personajeJuego.setLocation(
                (int) (PANADERIA_SPAWN_DESDE_ALCALDIA[0] / 1591.0 * juego.getWidth()),
                (int) (PANADERIA_SPAWN_DESDE_ALCALDIA[1] / 989.0 * juego.getHeight())
        );

        actualizarImagenCelular();
        acomodarEnZonaCaminable();
        juego.repaint();

        System.out.println("Regresaste de la alcaldía a la panadería");
    }

    // ===================== TAMAÑO DEL PERSONAJE =====================

    // Tamaño normal (calle, malecón, casa) y tamaño dentro de la junta (piso 1 y piso 2).
    // Para hacerlo más grande o más pequeño, cambia estos números (ancho, alto).
    private static final int[] TAMANO_PERSONAJE_NORMAL = {140, 170};
    private static final int[] TAMANO_PERSONAJE_JUNTA = {185, 225};
    private static final int[] TAMANO_PERSONAJE_ALCALDIA = {230, 280};   // dentro de interioralcaldia.png

    /** Cambia el tamaño del personaje (la imagen y el cuadro que se usa para las colisiones). */
    private void cambiarTamanoPersonaje(int[] tamano) {
        if (personajeJuego.getWidth() == tamano[0] && personajeJuego.getHeight() == tamano[1]) {
            return;   // ya tiene ese tamaño
        }
        Image img = new ImageIcon(getClass().getResource("/imagenes/personajecivil.png"))
                .getImage().getScaledInstance(tamano[0], tamano[1], Image.SCALE_SMOOTH);
        personajeJuego.setIcon(new ImageIcon(img));
        personajeJuego.setSize(tamano[0], tamano[1]);
    }

    /** Lleva al jugador a un lugar de la junta: 0 = exterior, 1 = piso 1, 2 = piso 2. */
    private void irAJunta(int lugar, int[] spawn) {
        irAJunta(lugar, spawn, false);
    }

    /**
     * Igual que el anterior, pero si puertaAbierta = true y va al exterior,
     * se muestra junta_puertaabierta.png (por ejemplo, al salir del piso 1).
     */
    private void irAJunta(int lugar, int[] spawn, boolean puertaAbierta) {
        juntaActiva = true;
        juntaLugar = lugar;
        maleconActivo = false;
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = false;
        puertaJuntaAbierta = (lugar == 0) && puertaAbierta;
        salioDeLaPuertaJunta = false;
        esperandoSalirDeZona = true;        // si aparece sobre la escalera, no vuelve a subir/bajar

        // Dentro de la junta el personaje es más grande; afuera vuelve a su tamaño normal
        cambiarTamanoPersonaje(lugar == 0 ? TAMANO_PERSONAJE_NORMAL : TAMANO_PERSONAJE_JUNTA);

        personajeJuego.setLocation(
                (int) (spawn[0] / 1920.0 * juego.getWidth()),
                (int) (spawn[1] / 1080.0 * juego.getHeight())
        );

        actualizarImagenCelular();
        acomodarEnZonaCaminable();   // por si el spawn cae sobre negro
        juego.repaint();

        System.out.println("Junta: " + (lugar == 0 ? "exterior" : "piso " + lugar));

    // Escena de la Junta: se dispara la primera vez que entra al piso 1
    if (lugar == 1 && !guionJuntaIniciado) {
        guionJuntaIniciado = true;
        reproducirGuion(
            GuionCiudadano.escenaJunta1().build(),
            this::reanudarJuegoDespuesDeHistoria
        );
    }

    // Conversación del piso 2: se dispara la primera vez que sube al piso 2
    if (lugar == 2 && !guionJuntaPiso2Iniciado) {
        guionJuntaPiso2Iniciado = true;
        reproducirGuion(
            GuionCiudadano.escenaJuntaPiso2().build(),
            this::reanudarJuegoDespuesDeHistoria
        );
    }

        
        
        
    }

    private boolean estaCercaDePuertaJunta(int x, int y) {
        // pies del personaje, pasados a coordenadas 1920 x 1080
        int cx = (int) ((x + personajeJuego.getWidth() / 2.0) * 1920.0 / juego.getWidth());
        int pies = (int) ((y + personajeJuego.getHeight() - 10) * 1080.0 / juego.getHeight());
        return ZONA_PUERTA_JUNTA.contains(cx, pies);
    }

    /** Abre la puerta (cambia la imagen), espera un momento y entra al piso 1. */
    private void abrirPuertaJunta() {
        puertaJuntaAbierta = true;

        // el jugador se queda quieto mientras se abre la puerta
        teclaW = false;
        teclaA = false;
        teclaS = false;
        teclaD = false;
        dialogoActivo = true;

        actualizarImagenCelular();   // muestra junta_puertaabierta.png
        juego.repaint();

        final int id = idPartida;    // si reinician la partida, este timer se cancela solo
        javax.swing.Timer espera = new javax.swing.Timer(900, ev -> {
            if (id != idPartida) {
                return;
            }
            dialogoActivo = false;
            irAJunta(1, JUNTA_SPAWN_PISO1_DESDE_PUERTA);
        });
        espera.setRepeats(false);
        espera.start();
    }
    
    
    
    
    
    
    
    
    
    
    
    private BufferedImage cargarMapa(String ruta) {
        try (java.io.InputStream in = getClass().getResourceAsStream(ruta)) {
            if (in == null) {
                System.out.println("No se encontró: " + ruta);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void cambiarAMalecon() {

    maleconActivo = true;
    casaActiva = false;
    esperandoSalirDeZona = true;
    dentroPanaderia = false;
    panaderiaAbierta = false;

    personajeJuego.setLocation(
        (int) (636.0 / 1920.0 * juego.getWidth()),
        (int) (300.0 / 1080.0 * juego.getHeight())
    );

    // Mostrar el escenario del malecón
    actualizarImagenCelular();
    juego.repaint();

    System.out.println("Entraste al malecón");

    // INICIAR LA HISTORIA AUTOMÁTICAMENTE
    if (!guionMaleconIniciado) {

        guionMaleconIniciado = true;

        reproducirGuion(
            GuionCiudadano.escenaMalecon().build(),
            this::reanudarJuegoDespuesDeHistoria
        );
    }}

    
    private boolean esZonaJunta(int color) {
    int rojo = (color >> 16) & 0xFF;
    int verde = (color >> 8) & 0xFF;
    int azul = color & 0xFF;
    return rojo > 200 && verde > 200 && azul < 100;
}

private void cambiarAJunta() {
    juntaActiva = true;
    maleconActivo = false;
    casaActiva = false;
    dentroPanaderia = false;
    panaderiaAbierta = false;

    // Dónde aparece el personaje (coordenadas de diseño 1591 x 989). AJÚSTALAS.
    personajeJuego.setLocation(
        (int) (200.0 / 1591.0 * juego.getWidth()),
        (int) (650.0 / 989.0 * juego.getHeight())
    );

    actualizarImagenCelular();
    juego.repaint();
    System.out.println("Entraste a la Junta Comunal");

    if (!guionJuntaIniciado) {
        guionJuntaIniciado = true;
        reproducirGuion(
            GuionCiudadano.escenaJunta1().build(),
            this::reanudarJuegoDespuesDeHistoria
        );
    }
}
    
    
    
    

    private void cambiarACasa() {
        casaActiva = true;
        maleconActivo = false;
        dentroPanaderia = false;
        panaderiaAbierta = false;

        personajeJuego.setLocation(700, 600);

        actualizarImagenCelular();
        juego.repaint();
        System.out.println("Regresaste a las casitas");
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
    private void actualizarImagenCasa() {

    if (!casaActiva) {
        return;
    }

    if (twitterAbierto) {
        cambiarFondoJuego("/imagenes/casapuertatw.png");

    } else if (mensajesAbierto) {
        cambiarFondoJuego("/imagenes/casapuertams.png");

    } else if (perfilAbierto) {
        cambiarFondoJuego("/imagenes/casapuertapf.png");

    } else if (celularAbierto) {
        cambiarFondoJuego("/imagenes/casapuertacon.png");

    } else {
        cambiarFondoJuego("/imagenes/casapuertasin.png");
    }
}
  
    // ===================== MAPA DE LA ISLA =====================

    // Zonas de clic sobre mapa.png, en coordenadas de diseño 1591 x 989.
    private static final java.awt.Rectangle MAPA_ALCALDIA = new java.awt.Rectangle(120, 70, 355, 330);
    private static final java.awt.Rectangle MAPA_PANADERIA = new java.awt.Rectangle(525, 140, 315, 250);
    private static final java.awt.Rectangle MAPA_JUNTA = new java.awt.Rectangle(1030, 120, 320, 270);
    private static final java.awt.Rectangle MAPA_MALECON = new java.awt.Rectangle(720, 395, 620, 200);
    private static final java.awt.Rectangle MAPA_CASAS = new java.awt.Rectangle(190, 585, 925, 285);

    /** Abre el mapa encima del juego. Llama a este método desde un botón si quieres. */
    public void abrirMapaIsla() {

        // No se abre durante diálogos, situaciones o avisos
        if (!juegoPreparado || mapaAbierto || dialogoActivo || situacionActiva
                || imagenTemporizadaActiva) {
            return;
        }

        if (mapaIsla == null) {
            mapaIsla = new JLabel();

            mapaIsla.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    clicEnMapaIsla(e.getX(), e.getY());
                }
            });
            // Para que el mouse sobre el mapa no abra el celular de abajo
            mapaIsla.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });

            juego.add(mapaIsla);
        }

        java.net.URL recurso = getClass().getResource("/imagenes/mapa.png");
        if (recurso == null) {
            System.out.println("No se encontró: /imagenes/mapa.png");
            return;
        }

        Image img = new ImageIcon(recurso).getImage()
                .getScaledInstance(juego.getWidth(), juego.getHeight(), Image.SCALE_SMOOTH);
        mapaIsla.setIcon(new ImageIcon(img));
        mapaIsla.setBounds(0, 0, juego.getWidth(), juego.getHeight());

        // El personaje se queda quieto mientras el mapa está abierto
        teclaW = false;
        teclaA = false;
        teclaS = false;
        teclaD = false;
        mapaAbierto = true;

        mapaIsla.setVisible(true);
        juego.setComponentZOrder(mapaIsla, 0);
        juego.revalidate();
        juego.repaint();

        String aqui = lugarActualEnGrafo();
        System.out.println("Mapa abierto. Estás en: " + aqui
                + " | Puedes ir directo a: " + grafoIsla.vecinos(aqui));
    }

    public void cerrarMapaIsla() {
        mapaAbierto = false;
        if (mapaIsla != null) {
            mapaIsla.setVisible(false);
        }
        juego.requestFocusInWindow();
        juego.repaint();
    }

    /** Qué locación del grafo corresponde a donde está el personaje ahora. */
    private String lugarActualEnGrafo() {
        if (alcaldiaActiva) {
            return GrafoLocaciones.ALCALDIA;
        } else if (juntaActiva) {
            return GrafoLocaciones.JUNTA;
        } else if (maleconActivo) {
            return GrafoLocaciones.MALECON;
        } else if (casaActiva) {
            return GrafoLocaciones.CASAS;
        }
        return GrafoLocaciones.PANADERIA;   // exterior o dentro de la panadería
    }

    private void clicEnMapaIsla(int xPantalla, int yPantalla) {

        // pasar el clic a coordenadas de diseño 1591 x 989
        int x = (int) (xPantalla * 1591.0 / juego.getWidth());
        int y = (int) (yPantalla * 989.0 / juego.getHeight());

        String destino = null;
        if (MAPA_ALCALDIA.contains(x, y)) {
            destino = GrafoLocaciones.ALCALDIA;
        } else if (MAPA_PANADERIA.contains(x, y)) {
            destino = GrafoLocaciones.PANADERIA;
        } else if (MAPA_JUNTA.contains(x, y)) {
            destino = GrafoLocaciones.JUNTA;
        } else if (MAPA_MALECON.contains(x, y)) {
            destino = GrafoLocaciones.MALECON;
        } else if (MAPA_CASAS.contains(x, y)) {
            destino = GrafoLocaciones.CASAS;
        }

        if (destino == null) {
            return;   // clic fuera de los lugares: no pasa nada
        }

        Funciones.reproducirSonido("/sonidos/click.wav");
        viajarPorGrafo(destino);
    }

    /** Usa el grafo para calcular el camino y lleva al jugador al destino. */
    private void viajarPorGrafo(String destino) {

        String origen = lugarActualEnGrafo();

        java.util.List<String> camino = grafoIsla.caminoMasCorto(origen, destino);
        int distancia = grafoIsla.distanciaTotal(camino);

        System.out.println("=================================");
        System.out.println("VIAJE POR EL MAPA");
        System.out.println("Desde: " + origen + "  Hasta: " + destino);
        System.out.println("Camino (BFS): " + String.join(" -> ", camino));
        System.out.println("Distancia total: " + distancia);
        System.out.println("=================================");

        cerrarMapaIsla();

        if (camino.isEmpty()) {
            System.out.println("No hay camino en el grafo hacia " + destino);
            return;
        }

        irALocacion(destino);
    }

    /** Lleva al personaje a la foto de cada locación. */
    private void irALocacion(String locacion) {

        // Salir de donde esté
        casaActiva = false;
        dentroPanaderia = false;
        panaderiaAbierta = false;
        maleconActivo = false;
        juntaActiva = false;
        juntaLugar = 0;
        puertaJuntaAbierta = false;
        alcaldiaActiva = false;
        alcaldiaLugar = 0;
        puertaAlcaldiaAbierta = false;
        cambiarTamanoPersonaje(TAMANO_PERSONAJE_NORMAL);
        esperandoSalirDeZona = true;   // si aparece sobre un color, no cambia de mapa de una

        switch (locacion) {

            case GrafoLocaciones.CASAS:          // casapuertasin.png
                cambiarACasa();
                break;

            case GrafoLocaciones.PANADERIA:      // exteriorsin.png
                personajeJuego.setLocation(700, 600);
                actualizarImagenCelular();
                acomodarEnZonaCaminable();
                break;

            case GrafoLocaciones.MALECON:        // malecon.png
                cambiarAMalecon();
                break;

            case GrafoLocaciones.JUNTA:          // junta_puertacerrada.png
                irAJunta(0, JUNTA_SPAWN_EXTERIOR_DESDE_MALECON);
                break;

            case GrafoLocaciones.ALCALDIA:       // alcaldia.png
                irAAlcaldia(0, ALCALDIA_SPAWN_DESDE_PANADERIA, false);
                break;

            default:
                break;
        }

        esperandoSalirDeZona = true;
        juego.repaint();
    }

  private void cambiarAExteriorPanaderia() {

    // Desactivar el escenario de la casa
    casaActiva = false;

    // La puerta todavía debe estar cerrada
    panaderiaAbierta = false;
    dentroPanaderia = false;

    // Cerrar el celular y sus aplicaciones
    celularAbierto = false;
    twitterAbierto = false;
    mensajesAbierto = false;
    perfilAbierto = false;

    // Mostrar el exterior de la panadería
    cambiarFondoJuego("/imagenes/exteriorsin.png");

    // Ubicar al personaje en el exterior
    personajeJuego.setLocation(700, 600);

    juego.repaint();
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
    
    
    
    //REEMPLAZO - VALENTINA
private void actualizarImagenCelular() {

    if (alcaldiaActiva) {

        if (alcaldiaLugar == 0) {
            cambiarFondoJuego(puertaAlcaldiaAbierta
                    ? "/imagenes/puertaabierta.png"
                    : "/imagenes/alcaldia.png");
        } else {
            cambiarFondoJuego("/imagenes/interioralcaldia.png");
        }

    } else if (juntaActiva) {

        if (juntaLugar == 0) {
            cambiarFondoJuego(puertaJuntaAbierta
                    ? "/imagenes/junta_puertaabierta.png"
                    : "/imagenes/junta_puertacerrada.png");
        } else if (juntaLugar == 1) {
            cambiarFondoJuego("/imagenes/junta_piso1.png");
        } else {
            cambiarFondoJuego("/imagenes/junta_piso2.png");
        }

    } else if (maleconActivo) {

        cambiarFondoJuego("/imagenes/malecon.png");

    } else if (dentroPanaderia) {

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

    } else if (casaActiva) {

        actualizarImagenCasa();

    } else if (panaderiaAbierta) {

        actualizarImagenPuerta();

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

    actualizarNombrePerfil();
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

    private void actualizarNombrePerfil() {

     if (nombrePerfilJuego == null) {
        return;
    }

    if (perfilAbierto) {

        nombrePerfilJuego.setText(nombreJugador);
        nombrePerfilJuego.setVisible(true);
        nombrePerfilJuego.setOpaque(false);

        juego.setComponentZOrder(nombrePerfilJuego, 0);

    } else {

        nombrePerfilJuego.setVisible(false);
    }

    juego.revalidate();
    juego.repaint();
}
    public void prepararPantallas() {

    java.awt.Dimension pantalla =
            java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    int ancho = pantalla.width;
    int alto = pantalla.height;

    ajustarImagenInicial(jLabel1, ancho, alto);
    ajustarImagenInicial(jLabel2, ancho, alto);
    ajustarImagenInicial(jLabel3, ancho, alto);
    ajustarImagenInicial(jLabel4, ancho, alto);
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
    
    // ===================== ESCENAS NO JUGABLES =====================

    /** Reproduce las escenas no jugables (GuionCiudadano) y, al terminar, arranca la parte jugable. */
    private void iniciarEscenasNoJugables() {

    java.util.List<Paso> escenas =
            GuionCiudadano.escenasNoJugables();

    if (escenas.isEmpty()) {
        iniciarParteJugable();
        return;
    }

    GuionCiudadano.resumenTodo();

    reproducirGuion(
            escenas,
            this::iniciarParteJugable
    );
}
    private void reanudarJuegoDespuesDeHistoria() {

    card.show(Panel_cambiante, "juego");

    juego.setFocusable(true);
    juego.requestFocusInWindow();

    if (timerMovimiento != null) {
        timerMovimiento.start();
    }

    System.out.println(
            "Historia terminada. Continuando partida."
    );
}
    /** Parte jugable (movimiento, celular, panaderia, decisiones): el codigo de siempre de partvisual. */
    private void iniciarParteJugable() {
        pantallaEscenas.reiniciar();
        motorEscenas = null;

        card.show(Panel_cambiante, "juego");

        if (!juegoPreparado) {
            prepararJuego();
            prepararDialogos();
            juegoPreparado = true;
        } else {
            reiniciarPartida();
        }

        juego.setFocusable(true);
        juego.requestFocusInWindow();

        if (timerMovimiento != null) {
            timerMovimiento.start();
        }

        System.out.println("NUEVA PARTIDA INICIADA");
    }

    /** ESC durante una escena no jugable: corta el guion y vuelve al menu de inicio. */
    private void salirDeEscenas() {
        if (motorEscenas != null) {
            motorEscenas.detener();
            motorEscenas = null;
        }
        pantallaEscenas.reiniciar();
        volverAlInicio();
    }

    private void reiniciarPartida() {

    idPartida++; // invalida TODOS los timers pendientes

    // --- Ramas y decisiones ---
    ramaAActiva = false;
    ramaBActiva = false;
    ramaBViral = false;
    consecuenciaRamaBActiva = false;
    ramaCActiva = false;
    ramaDActiva = false;
    decisionEnCurso = false;
    abanicoOpcionesVisible = false;
    publicacionFalsa = true;
    nodoActual = arbolActual;
    estadoJuego = new EstadoJuego();

    // --- Diálogos ---
    dialogoActivo = false;
    guionPanaderiaIniciado = false;
guionMaleconIniciado = false;
guionJuntaIniciado = false;   // <- nueva - VALENTINA
guionJuntaPiso2Iniciado = false;
    dialogoActual = 40;
    imagenTemporizadaActiva = false;

    // --- Movimiento ---
    teclaW = false;
    teclaA = false;
    teclaS = false;
    teclaD = false;

    // --- Escenario ---
    panaderiaAbierta = false;
    dentroPanaderia = false;
    situacionActiva = false;
    maleconActivo = false;
    juntaActiva = false;
    juntaLugar = 0;
    puertaJuntaAbierta = false;
    salioDeLaPuertaJunta = false;
    alcaldiaActiva = false;
    alcaldiaLugar = 0;
    puertaAlcaldiaAbierta = false;
    salioDelAmarilloAlcaldia = false;
    guionAlcaldiaIniciado = false;
    esperandoSalirDeZona = false;
    guionCasaFinalIniciado = false;
    mapaAbierto = false;
    if (mapaIsla != null) mapaIsla.setVisible(false);
    casaActiva = true;
// NUEVAS VARIABLES - VALENTINA
    
    
    // --- Teléfono ---
    celularAbierto = false;
    twitterAbierto = false;
    mensajesAbierto = false;
    perfilAbierto = false;

    // --- Componentes visuales ---
    if (dialogoJuego != null) dialogoJuego.setVisible(false);
    if (opciones != null) opciones.setVisible(false);
    if (situacionMiniatura != null) situacionMiniatura.setVisible(false);
    if (iconoPanico != null) iconoPanico.setVisible(false);

    if (opcionesSecundarias != null) {
        opcionesSecundarias.setVisible(false);
        cambiarImagenOpcionesSecundarias(IMAGENOPCIONESDEVERIFICACION);
    }

    if (personajeJuego != null) {

        Image img = new ImageIcon(getClass().getResource("/imagenes/personajecivil.png"))
                .getImage().getScaledInstance(140, 170, Image.SCALE_SMOOTH);

        personajeJuego.setIcon(new ImageIcon(img));
        personajeJuego.setSize(140, 170);
        personajeJuego.setLocation(700, 600);
        personajeJuego.setVisible(true); // se oculta durante la situación
    }

    if (fondoJuego != null) {
        cambiarFondoJuego("/imagenes/casapuertasin.png");
    }

    juego.revalidate();
    juego.repaint();
}

    
    
    /** Funde la pantalla de nombre, muestra el prólogo de Ringo y luego arranca tus escenas de siempre. */
private void iniciarIntroLlegada() {

    BufferedImage foto = new BufferedImage(
            Panel_cambiante.getWidth(),
            Panel_cambiante.getHeight(),
            BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D gf = foto.createGraphics();
    Panel_cambiante.paint(gf);          // "foto" de la pantalla de nombre
    gf.dispose();

    card.show(Panel_cambiante, "intro");
    introLlegada.iniciar(nombreJugador, foto, this::iniciarEscenasNoJugables);
}
    
    
    
    
        /** Devuelve el nombre que el jugador escribió en la pantalla de nombre. */
    public String getNombreJugador() {
        return nombreJugador;
    }
    
    
    
    
    
    
    
    

    
// Sirve tanto para el fin del día como para salir a mitad de partida
private void volverAlInicio() {

    reiniciarPartida();

    if (timerMovimiento != null) {
        timerMovimiento.stop();
    }

    card = (CardLayout) Panel_cambiante.getLayout();
    card.show(Panel_cambiante, "inicio");

    Panel_cambiante.revalidate();
    Panel_cambiante.repaint();

    System.out.println("Se regresó a la pantalla de inicio.");
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
        iniciojuego.add(BTjugar, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 470, 420, 120));

        BTsalir.addActionListener(this::BTsalirActionPerformed);
        iniciojuego.add(BTsalir, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 740, 440, 110));

        BTinst.addActionListener(this::BTinstActionPerformed);
        iniciojuego.add(BTinst, new org.netbeans.lib.awtextra.AbsoluteConstraints(600, 610, 420, 110));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Inicio.png"))); // NOI18N
        iniciojuego.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        Panel_cambiante.add(iniciojuego, "inicio");

        elegirroles.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver.addActionListener(this::BTvolverActionPerformed);
        elegirroles.add(BTvolver, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 880, 220, 70));

        BTcivil.addActionListener(this::BTcivilActionPerformed);
        elegirroles.add(BTcivil, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 350, 320, 330));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Roles.png"))); // NOI18N
        elegirroles.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1591, 989));

        Panel_cambiante.add(elegirroles, "roles");

        ponernombre.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        BTvolver2.addActionListener(this::BTvolver2ActionPerformed);
        ponernombre.add(BTvolver2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 880, 210, 60));

        BTcontinuar.addActionListener(this::BTcontinuarActionPerformed);
        ponernombre.add(BTcontinuar, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 790, 450, 80));

        Digitarnombre.setFont(new java.awt.Font("Segoe UI", 2, 24)); // NOI18N
        Digitarnombre.setForeground(new java.awt.Color(124, 112, 88));
        Digitarnombre.addActionListener(this::DigitarnombreActionPerformed);
        ponernombre.add(Digitarnombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 630, 510, 70));

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
        instrucciones.add(BTvolver3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 860, 280, 90));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/instrucciones.png"))); // NOI18N
        instrucciones.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1590, 990));

        Panel_cambiante.add(instrucciones, "inst");

        getContentPane().add(Panel_cambiante, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    
    private void BTvolver3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolver3ActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inicio");
    }//GEN-LAST:event_BTvolver3ActionPerformed

    private void DigitarnombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DigitarnombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_DigitarnombreActionPerformed

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

    private void BTvolver2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolver2ActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "roles");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel2);
        });
    }//GEN-LAST:event_BTvolver2ActionPerformed

    private void BTvolverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTvolverActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inicio");
    }//GEN-LAST:event_BTvolverActionPerformed

    private void BTcivilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTcivilActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "nombre");
    }//GEN-LAST:event_BTcivilActionPerformed

    private void BTsalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTsalirActionPerformed
        System.exit(0);
    }//GEN-LAST:event_BTsalirActionPerformed

    private void BTinstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTinstActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "inst");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel4);
        });
    }//GEN-LAST:event_BTinstActionPerformed

    private void BTjugarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTjugarActionPerformed
        card = (CardLayout) this.Panel_cambiante.getLayout();
        card.show(Panel_cambiante, "roles");
        SwingUtilities.invokeLater(() -> {
            ajustarImagen(jLabel2);
        });
    }//GEN-LAST:event_BTjugarActionPerformed

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
                case KeyEvent.VK_M:
             // Abrir / cerrar el mapa de la isla
             System.out.println("Tecla M presionada");
             if (mapaAbierto) {
                 cerrarMapaIsla();
             } else {
                 abrirMapaIsla();
             }
             break;

         case KeyEvent.VK_ESCAPE:
             if (mapaAbierto) {
                 cerrarMapaIsla();
             }
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



