package juegoferia;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Base de las pantallas nuevas (mundo caminable y minijuegos).
 *
 * - Se dibuja en coordenadas de diseno 1920 x 1080 y se escala sola a la ventana.
 * - Tiene su propio reloj: arranca cuando la pantalla se muestra y se detiene cuando se oculta.
 * - Las subclases solo implementan actualizar(dt) y dibujar(g).
 */
public abstract class PanelDiseno extends JPanel {

    public static final int W = 1920;
    public static final int H = 1080;

    private final Timer reloj = new Timer(16, e -> tic());
    private long ultimo = System.nanoTime();

    /** Segundos desde que la pantalla se mostro (o se reinicio). */
    protected double tiempo;

    protected PanelDiseno() {
        setOpaque(true);
        setBackground(Color.BLACK);
        setFocusable(true);
        // TAB (y las flechas) llegan al juego en vez de mover el foco entre componentes de Swing
        setFocusTraversalKeysEnabled(false);
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Sincronia.soltarTodas();
            }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    ultimo = System.nanoTime();
                    alMostrar();
                    reloj.start();
                    requestFocusInWindow();
                } else {
                    reloj.stop();
                    alOcultar();
                }
            }
        });
    }

    protected void alMostrar() {
    }

    protected void alOcultar() {
    }

    protected abstract void actualizar(double dt);

    /** Dibuja en el espacio de diseno 1920 x 1080. */
    protected abstract void dibujar(Graphics2D g);

    private void tic() {
        if (Sincronia.activa()) {
            // en red el tiempo lo marca Sincronia (todos avanzan igual); aqui solo se dibuja
            repaint();
            return;
        }
        long ahora = System.nanoTime();
        double dt = Math.min(0.05, (ahora - ultimo) / 1e9);
        ultimo = ahora;
        avanzar(dt);
        repaint();
    }

    /** Un paso de tiempo (visible para poder probar sin ventana). */
    void avanzar(double dt) {
        if (pausado && conPausa()) {
            tPausa += dt;          // en pausa no pasa NADA del juego (en red, igual en las cuatro)
            return;
        }
        tiempo += dt;
        actualizar(dt);
    }

    // ===================== PAUSA =====================
    //
    // ESC (o el boton || de abajo a la izquierda) pausa la partida. En red la pausa viaja como una
    // tecla mas (VK_PAUSE), asi que las cuatro computadoras se detienen en el MISMO cuadro y
    // cualquiera puede continuar. En pausa no avanzan ni el reloj, ni el rumor, ni los minijuegos.

    private static boolean pausado;
    private static double tPausa;
    private static String pausoQuien;
    private static int pausaHover = -1;
    /** Lo pone VentanaJuego: que hacer con "Salir al menú" (solo un PC). */
    public static Runnable alSalirAlMenu;

    private static final java.awt.Rectangle BOTON_PAUSA = new java.awt.Rectangle(84, 1010, 52, 52);
    private static final java.awt.Rectangle P_CONTINUAR = new java.awt.Rectangle(W / 2 - 230, 640, 460, 84);
    private static final java.awt.Rectangle P_SALIR = new java.awt.Rectangle(W / 2 - 150, 744, 300, 54);

    public static boolean pausado() {
        return pausado;
    }

    /** Al empezar o terminar una partida. */
    public static void quitarPausa() {
        pausado = false;
        pausoQuien = null;
    }

    /** true = esta pantalla se puede pausar (por defecto: las de la partida). */
    protected boolean conPausa() {
        return conAjustes();
    }

    /** false en las cinematicas: ahi ESC sirve para saltar (se pausa con el boton). */
    protected boolean pausaConEscape() {
        return true;
    }

    /** Al entrar en pausa (para soltar las teclas de caminar, etc.). */
    protected void alPausar() {
    }

    private boolean esTeclaPausa(int code) {
        return code == KeyEvent.VK_PAUSE || (code == KeyEvent.VK_ESCAPE && (pausaConEscape() || pausado));
    }

    private void alternarPausa() {
        pausado = !pausado;
        tPausa = 0;
        pausaHover = -1;
        if (pausado) {
            Rol r = Sincronia.inyectando() ? Sincronia.rolActual() : null;
            pausoQuien = r != null ? Sincronia.nombreDe(r) : null;
            alPausar();
        }
        Ajustes.sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
        repaint();
    }

    /** Pausar / continuar desde esta computadora (en red se avisa a todos). */
    private void pedirPausa() {
        if (Sincronia.activa()) {
            Sincronia.pulsar(KeyEvent.VK_PAUSE);
        } else {
            alternarPausa();
        }
    }

    /** Sincronia pregunta: este evento de otro jugador tiene que llegar aunque la pantalla no sea "suya". */
    boolean esDePausa(String tipo, int codigo) {
        return conPausa() && (tipo.equals("KD") || tipo.equals("KU")) && esTeclaPausa(codigo);
    }

    // ===================== ESCALA =====================

    private double escala(int w, int h) {
        return Math.min(w / (double) W, h / (double) H);
    }

    // ===================== ENTRADA (local o por red) =====================

    /** En red, quien puede mandar teclas y clics a esta pantalla ahora mismo (por defecto todos). */
    protected boolean aceptaEntradaDe(Rol r) {
        return true;
    }

    /** Teclas que NO viajan por red (solo cambian algo en tu pantalla, como F1). */
    protected boolean teclaSoloLocal(int codigo) {
        return false;
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (Sincronia.activa() && !Sincronia.inyectando()) {
            if (pausado && conPausa() && e.getKeyCode() != KeyEvent.VK_ESCAPE && e.getKeyCode() != KeyEvent.VK_PAUSE
                    && e.getID() == KeyEvent.KEY_PRESSED) {
                e.consume();       // en pausa no se manda nada (salvo continuar)
                return;
            }
            if (teclaSoloLocal(e.getKeyCode())) {
                super.processKeyEvent(e);
            } else {
                Sincronia.teclaLocal(e);
            }
            e.consume();
            return;
        }
        if (conPausa() && e.getID() != KeyEvent.KEY_TYPED) {
            if (esTeclaPausa(e.getKeyCode())) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    alternarPausa();
                }
                e.consume();
                return;
            }
            if (pausado) {
                if (e.getID() == KeyEvent.KEY_RELEASED) {
                    super.processKeyEvent(e);     // soltar si: que nadie quede caminando solo
                }
                e.consume();
                return;
            }
        }
        super.processKeyEvent(e);
    }

    /** Clics del boton de pausa y de la tarjeta de pausa (son de esta pantalla; continuar viaja por red). */
    private boolean ratonDePausa(MouseEvent e) {
        if (!conPausa()) {
            return false;
        }
        if (Sincronia.inyectando()) {
            return pausado;            // los clics de otros no le llegan al juego mientras esta en pausa
        }
        Point d = aDisenoReal(e.getPoint());
        int id = e.getID();
        if (!pausado) {
            if (id == MouseEvent.MOUSE_PRESSED && BOTON_PAUSA.contains(d)) {
                pedirPausa();
                e.consume();
                return true;
            }
            return false;
        }
        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
            int antes = pausaHover;
            pausaHover = P_CONTINUAR.contains(d) || BOTON_PAUSA.contains(d) ? 0
                    : !Sincronia.activa() && alSalirAlMenu != null && P_SALIR.contains(d) ? 1 : -1;
            if (antes != pausaHover) {
                repaint();
            }
        } else if (id == MouseEvent.MOUSE_PRESSED) {
            if (P_CONTINUAR.contains(d) || BOTON_PAUSA.contains(d)) {
                pedirPausa();
            } else if (!Sincronia.activa() && alSalirAlMenu != null && P_SALIR.contains(d)) {
                quitarPausa();
                Ajustes.sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
                alSalirAlMenu.run();
            }
        }
        e.consume();
        return true;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (ratonDeAjustes(e) || ratonDePausa(e)) {
            return;
        }
        if (Sincronia.activa() && !Sincronia.inyectando()) {
            if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                requestFocusInWindow();
            }
            Sincronia.ratonLocal(e, aDisenoReal(e.getPoint()));
            e.consume();
            return;
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (ratonDeAjustes(e) || ratonDePausa(e)) {
            return;
        }
        if (Sincronia.activa() && !Sincronia.inyectando()) {
            Sincronia.ratonLocal(e, aDisenoReal(e.getPoint()));
            e.consume();
            return;
        }
        super.processMouseMotionEvent(e);
    }

    /** Lo usa Sincronia para entregar la tecla de otro jugador (o la tuya, ya ordenada por el anfitrion). */
    final void inyectarTecla(boolean presionada, int codigo) {
        processKeyEvent(new KeyEvent(this, presionada ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                0L, 0, codigo, KeyEvent.CHAR_UNDEFINED));
    }

    /** id: MOUSE_PRESSED, MOUSE_RELEASED o MOUSE_MOVED. La posicion ya viene en espacio de diseno. */
    final void inyectarRaton(int id, int boton) {
        MouseEvent ev = new MouseEvent(this, id, 0L, 0, 0, 0, 1, false, boton);
        if (id == MouseEvent.MOUSE_MOVED) {
            processMouseMotionEvent(ev);
        } else {
            processMouseEvent(ev);
        }
    }

    /** Pasa un punto de la ventana al espacio de diseno 1920 x 1080. */
    protected Point aDiseno(Point p) {
        if (Sincronia.inyectando()) {
            return Sincronia.puntoDiseno();
        }
        return aDisenoReal(p);
    }

    private Point aDisenoReal(Point p) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        double s = escala(w, h);
        double ox = (w - W * s) / 2;
        double oy = (h - H * s) / 2;
        return new Point((int) ((p.x - ox) / s), (int) ((p.y - oy) / s));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        pintarEn(g, getWidth(), getHeight());
        g.dispose();
    }

    void pintarEn(Graphics2D g, int w, int h) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        double s = escala(w, h);
        g.translate((w - W * s) / 2, (h - H * s) / 2);
        g.scale(s, s);
        g.setClip(0, 0, W, H);
        Estilo.suave(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        dibujar(g);
        if (conPausa()) {
            if (pausado) {
                dibujarPausa(g);
            }
            dibujarBotonPausa(g);
        }
        if (conAjustes()) {
            Ajustes.dibujar(g, 1 / 60.0);
        }
    }

    private static final Color PAUSA_CREMA = new Color(255, 250, 240, 240);
    private static final Color PAUSA_BORDE = new Color(0xEAD7AE);
    private static final Color PAUSA_TEXTO = new Color(0x5A3B1F);
    private static final Color PAUSA_SUAVE = new Color(0xA88B6A);

    /** El botoncito || al lado del parlante. */
    private void dibujarBotonPausa(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        int cx = BOTON_PAUSA.x + 26, cy = BOTON_PAUSA.y + 26, r = 26;
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, pausado ? 1f : 0.82f));
        g.setColor(new Color(60, 35, 15, 40));
        g.fillOval(cx - r + 1, cy - r + 3, r * 2, r * 2);
        g.setColor(PAUSA_CREMA);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(pausado ? Estilo.DORADO : PAUSA_BORDE);
        g.setStroke(new java.awt.BasicStroke(pausado ? 2.4f : 1.6f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(PAUSA_TEXTO);
        if (pausado) {
            g.fill(new Path2D.Double() {
                {
                    moveTo(cx - 7, cy - 11);
                    lineTo(cx + 12, cy);
                    lineTo(cx - 7, cy + 11);
                    closePath();
                }
            });
        } else {
            g.fill(new RoundRectangle2D.Double(cx - 10, cy - 11, 7, 22, 4, 4));
            g.fill(new RoundRectangle2D.Double(cx + 3, cy - 11, 7, 22, 4, 4));
        }
        g.dispose();
    }

    /** La tarjeta de pausa: continuar, controles y (en un solo PC) salir al menu. */
    private void dibujarPausa(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        double a = Math.min(1, tPausa / 0.12 + 0.6);
        g.setColor(new Color(40, 24, 10, (int) (120 * a)));
        g.fillRect(0, 0, W, H);
        int cw = 760, ch = 590, x = (W - cw) / 2, y = 225;
        g.setColor(new Color(60, 35, 15, 50));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 8, cw, ch, 44, 44));
        g.setColor(PAUSA_CREMA);
        g.fill(new RoundRectangle2D.Double(x, y, cw, ch, 44, 44));
        g.setColor(PAUSA_BORDE);
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, cw, ch, 44, 44));
        // Ringo asomado
        if (Recursos.existe("/imagenes/ringo.png")) {
            java.awt.image.BufferedImage rg = Recursos.escalada("/imagenes/ringo.png", 230, 230);
            int bob = (int) (Math.sin((System.nanoTime() / 1e9) * 2) * 4);
            g.drawImage(rg, x + cw - 170, y - 150 + bob, 230, 230, null);
        }
        g.setFont(Estilo.fuente(java.awt.Font.BOLD, 54f));
        g.setColor(PAUSA_TEXTO);
        g.drawString("Pausa", x + 56, y + 86);
        g.setFont(Estilo.fuente(java.awt.Font.BOLD, 18f));
        g.setColor(PAUSA_SUAVE);
        String sub = Sincronia.activa()
                ? (pausoQuien != null ? "La pausó " + pausoQuien + " · " : "") + "es para los cuatro: cualquiera puede continuar"
                : "El rumor también se detiene. Respira.";
        g.drawString(sub, x + 60, y + 120);

        // controles
        String[][] ctl = {
            {"WASD", "caminar"}, {"E", "hablar / seguir"}, {"Q", "celular"}, {"R", "emociones"},
            {"H", "habilidad"}, {"F", "frases"}, {"TAB", "misiones"}, {Sincronia.activa() ? "ESC" : "1-4", Sincronia.activa() ? "pausa" : "cambiar personaje"}};
        g.setFont(Estilo.fuente(java.awt.Font.BOLD, 17f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < ctl.length; i++) {
            int col = i % 2, fila = i / 2;
            int tx = x + 64 + col * 330, ty = y + 170 + fila * 52;
            int kw = Math.max(40, fm.stringWidth(ctl[i][0]) + 22);
            g.setColor(new Color(0xFFFDF7));
            g.fill(new RoundRectangle2D.Double(tx, ty, kw, 38, 12, 12));
            g.setColor(PAUSA_BORDE);
            g.setStroke(new java.awt.BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(tx, ty, kw, 38, 12, 12));
            g.setColor(PAUSA_TEXTO);
            g.drawString(ctl[i][0], tx + (kw - fm.stringWidth(ctl[i][0])) / 2, ty + 26);
            g.setColor(PAUSA_SUAVE);
            g.drawString(ctl[i][1], tx + kw + 14, ty + 26);
        }
        // botones
        java.awt.Rectangle c = P_CONTINUAR;
        boolean h0 = pausaHover == 0;
        g.setPaint(new java.awt.GradientPaint(c.x, c.y, Estilo.DORADO_CLARO, c.x, c.y + c.height, Estilo.DORADO));
        g.fill(new RoundRectangle2D.Double(c.x, c.y - (h0 ? 3 : 0), c.width, c.height, c.height, c.height));
        g.setColor(new Color(0xC98A12));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(c.x, c.y - (h0 ? 3 : 0), c.width, c.height, c.height, c.height));
        g.setFont(Estilo.fuente(java.awt.Font.BOLD, 32f));
        g.setColor(PAUSA_TEXTO);
        centrado(g, "Continuar  (ESC)", c.x + c.width / 2, c.y + 55 - (h0 ? 3 : 0));
        if (!Sincronia.activa() && alSalirAlMenu != null) {
            java.awt.Rectangle s = P_SALIR;
            g.setColor(pausaHover == 1 ? new Color(0xFBE3DC) : new Color(0xFFFDF7));
            g.fill(new RoundRectangle2D.Double(s.x, s.y, s.width, s.height, s.height, s.height));
            g.setColor(new Color(0xE0B4A4));
            g.draw(new RoundRectangle2D.Double(s.x, s.y, s.width, s.height, s.height, s.height));
            g.setFont(Estilo.fuente(java.awt.Font.BOLD, 20f));
            g.setColor(new Color(0xB5473A));
            centrado(g, "Salir al menú", s.x + s.width / 2, s.y + 35);
        }
        g.dispose();
    }


    public static void dibujarAPantalla(Graphics2D g, java.awt.image.BufferedImage img, int x, int y, int w, int h) {
        java.awt.geom.AffineTransform t = g.getTransform();
        if (t.getShearX() != 0 || t.getShearY() != 0) {
            g.drawImage(img, x, y, w, h, null);
            return;
        }
        int dx = (int) Math.round(t.getTranslateX() + x * t.getScaleX());
        int dy = (int) Math.round(t.getTranslateY() + y * t.getScaleY());
        int dw = (int) Math.round(w * t.getScaleX()), dh = (int) Math.round(h * t.getScaleY());
        if (dw <= 0 || dh <= 0) {
            return;
        }
        java.awt.image.BufferedImage listo = ESCALADAS.get(img);
        if (listo == null || listo.getWidth() != dw || listo.getHeight() != dh) {
            listo = new java.awt.image.BufferedImage(dw, dh, img.getTransparency() == java.awt.Transparency.OPAQUE
                    ? java.awt.image.BufferedImage.TYPE_INT_RGB : java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D gg = listo.createGraphics();
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gg.drawImage(img, 0, 0, dw, dh, null);
            gg.dispose();
            listo = Recursos.compatible(listo);
            ESCALADAS.put(img, listo);
        }
        g.setTransform(new java.awt.geom.AffineTransform());
        g.drawImage(listo, dx, dy, null);
        g.setTransform(t);
    }

    private static final java.util.Map<java.awt.image.BufferedImage, java.awt.image.BufferedImage> ESCALADAS =
            new java.util.WeakHashMap<>();

    /** true = esta pantalla muestra el boton del parlante (lugares, minijuegos y cinematicas). */
    protected boolean conAjustes() {
        return false;
    }

    /** El boton del parlante se queda con el clic si es suyo (el juego no lo recibe). */
    private boolean ratonDeAjustes(MouseEvent e) {
        if (!conAjustes() || Sincronia.inyectando()) {
            return false;
        }
        if (Ajustes.raton(e.getID(), aDisenoReal(e.getPoint()))) {
            e.consume();
            repaint();
            return true;
        }
        return false;
    }

    // ===================== UTILIDADES DE DIBUJO =====================

    /** Igual que envolver(...), pero publico (lo usa el kit de arte de los minijuegos). */
    public static List<String> envolverTexto(String texto, FontMetrics fm, int maxW) {
        return envolver(texto, fm, maxW);
    }

    protected static List<String> envolver(String texto, FontMetrics fm, int maxW) {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : texto.split("\n")) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (fm.stringWidth(prueba) > maxW && actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(prueba);
                }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    protected static void centrado(Graphics2D g, String s, int cx, int baseY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, baseY);
    }

    protected static Path2D corazon(double cx, double cy, double tam) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy + tam * 0.9);
        p.curveTo(cx - tam * 1.4, cy - tam * 0.1, cx - tam * 0.7, cy - tam * 1.1, cx, cy - tam * 0.35);
        p.curveTo(cx + tam * 0.7, cy - tam * 1.1, cx + tam * 1.4, cy - tam * 0.1, cx, cy + tam * 0.9);
        p.closePath();
        return p;
    }

    /** Barra de progreso redondeada. frac de 0 a 1. */
    protected static void barra(Graphics2D g, int x, int y, int w, int h, double frac, Color color) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        double f = Math.max(0, Math.min(1, frac));
        if (f > 0) {
            g.setColor(color);
            g.fill(new RoundRectangle2D.Double(x, y, Math.max(h, w * f), h, h, h));
            g.setColor(new Color(255, 255, 255, 80));
            g.fill(new RoundRectangle2D.Double(x + 4, y + 3, Math.max(h - 8, w * f - 8), h * 0.35, h, h));
        }
        g.setColor(Estilo.MADERA_OSCURA);
        g.setStroke(new java.awt.BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
    }
}
