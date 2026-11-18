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
        tiempo += dt;
        actualizar(dt);
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
            if (teclaSoloLocal(e.getKeyCode())) {
                super.processKeyEvent(e);
            } else {
                Sincronia.teclaLocal(e);
            }
            e.consume();
            return;
        }
        super.processKeyEvent(e);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (ratonDeAjustes(e)) {
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
        if (ratonDeAjustes(e)) {
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
        if (conAjustes()) {
            Ajustes.dibujar(g, 1 / 60.0);
        }
    }

    /**
     * OPTIMIZACION: dibuja una imagen grande (un fondo) ya achicada al tamaño REAL de la ventana.
     * Se achica una sola vez (y otra vez solo si cambia el tamaño de la ventana); despues es copiar pixeles.
     */
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
