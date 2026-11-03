package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Pantalla del juego, dibujada toda por codigo (sin NetBeans Form).
 * Capas, de abajo hacia arriba: fondo, imagenes sueltas, pista/zonas, dialogo, menu, aviso.
 *
 * Los DIALOGOS salen siempre en la misma zona fija (ZONA_DIALOGO), asi no hay que
 * acomodar cada burbuja a mano. Se avanza con clic en la zona, ESPACIO o ENTER.
 *
 * Teclas: H = ver/ocultar indicadores, ESC = volver al menu.
 */
public class PantallaJuego extends JPanel {

    /** Dev: poner en 0.05 para probar rapido (los avisos y esperas duran 5%). */
    public static double ESCALA_TIEMPO = 1.0;

    /** Zona fija de dialogos, en coordenadas de diseno 1591 x 989. Ajustala a tu gusto. */
public static final Rectangle ZONA_DIALOGO = new Rectangle(250, 50, 1091, 250);

    private static final Rectangle RECT_TITULO_MENU = new Rectangle(395, 200, 800, 130);
    private static final Color CREMA = new Color(255, 236, 208);
    private static final Color MARRON = new Color(110, 62, 28);
    private static final Color NARANJA = new Color(232, 140, 40);

    private enum Modo { NINGUNO, DIALOGO, AVISO, ESPERA, CLIC, MENU }

    private static class Sprite {
        BufferedImage img;
        int x, y, w, h;
    }

    private Modo modo = Modo.NINGUNO;

    private BufferedImage fondo;
    private final Map<String, Sprite> sprites = new LinkedHashMap<>();

    // dialogo
    private BufferedImage dlgImg;
    private String dlgQuien, dlgTexto;
    private Runnable dlgFin;

    // aviso / espera
    private BufferedImage avisoImg;
    private String avisoTexto;
    private Timer timerUnico;

    // espera de clic
    private Rectangle[] zonasClic;
    private String pistaClic;
    private Runnable clicFin;

    // menu
    private String menuTitulo;
    private List<String> menuEtiquetas;
    private final List<Rectangle> menuRects = new ArrayList<>();
    private IntConsumer menuAlElegir;
    private Runnable menuAlAgotar;
    private int menuHover = -1;
    private int menuLimiteMs, menuRestanteMs;
    private Timer timerCuenta;

    // indicadores
    private EstadoJuego hudEstado;
    private boolean mostrarHud = false;

    private Runnable alSalir;

    public PantallaJuego() {
        setFocusable(true);
        setBackground(Color.BLACK);

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clic(aBase(e.getPoint()));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mover(aBase(e.getPoint()));
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ENTER:
                        if (modo == Modo.DIALOGO) {
                            terminarDialogo();
                        }
                        break;
                    case KeyEvent.VK_H:
                        mostrarHud = !mostrarHud;
                        repaint();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        if (alSalir != null) {
                            alSalir.run();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    // ===================== API PARA EL MOTOR =====================

    public void setHud(EstadoJuego estado) {
        this.hudEstado = estado;
    }

    public void setAlSalir(Runnable r) {
        this.alSalir = r;
    }

    public void setFondo(String ruta) {
        fondo = Recursos.fondo(ruta);
        repaint();
    }

    public void mostrarSprite(String id, String ruta, int x, int y, int w, int h) {
        Sprite s = new Sprite();
        s.img = Recursos.sprite(ruta);
        s.x = x;
        s.y = y;
        s.w = w;
        s.h = h;
        sprites.put(id, s);
        repaint();
    }

    public void ocultarSprite(String id) {
        sprites.remove(id);
        repaint();
    }

    public void mostrarDialogoImagen(String ruta, Runnable fin) {
        dlgImg = Recursos.dialogo(ruta);
        dlgQuien = null;
        dlgTexto = null;
        dlgFin = fin;
        modo = Modo.DIALOGO;
        repaint();
    }

    public void mostrarDialogoTexto(String quien, String texto, Runnable fin) {
        dlgImg = null;
        dlgQuien = quien;
        dlgTexto = texto;
        dlgFin = fin;
        modo = Modo.DIALOGO;
        repaint();
    }

    public void ocultarDialogo() {
        dlgImg = null;
        dlgTexto = null;
        dlgQuien = null;
        repaint();
    }

    /** Aviso en el centro: si ruta no es null se dibuja la imagen, si no el texto. */
    public void mostrarAviso(String ruta, String texto, int ms, Runnable fin) {
        avisoImg = (ruta != null) ? Recursos.imagen(ruta, 800, 400) : null;
        avisoTexto = texto;
        modo = Modo.AVISO;
        repaint();
        iniciarTimerUnico(ms, () -> {
            avisoImg = null;
            avisoTexto = null;
            modo = Modo.NINGUNO;
            repaint();
            fin.run();
        });
    }

    public void esperar(int ms, Runnable fin) {
        modo = Modo.ESPERA;
        iniciarTimerUnico(ms, () -> {
            modo = Modo.NINGUNO;
            fin.run();
        });
    }

    public void esperarClic(String pista, Rectangle[] zonas, Runnable fin) {
        pistaClic = pista;
        zonasClic = zonas;
        clicFin = fin;
        modo = Modo.CLIC;
        repaint();
    }

    /**
     * Menu de opciones (los 4 botones de siempre u otro menu).
     * @param limiteSeg 0 = sin limite; si se acaba el tiempo se llama a alAgotar
     */
    public void mostrarMenu(String titulo, List<String> etiquetas, int limiteSeg,
                            IntConsumer alElegir, Runnable alAgotar) {
        menuTitulo = titulo;
        menuEtiquetas = etiquetas;
        menuAlElegir = alElegir;
        menuAlAgotar = alAgotar;
        menuHover = -1;
        calcularRectsMenu(etiquetas.size());
        modo = Modo.MENU;

        detenerTimerCuenta();
        menuLimiteMs = (int) (limiteSeg * 1000 * ESCALA_TIEMPO);
        menuRestanteMs = menuLimiteMs;
        if (menuLimiteMs > 0) {
            timerCuenta = new Timer(50, e -> {
                menuRestanteMs -= 50;
                if (menuRestanteMs <= 0) {
                    Runnable agotar = menuAlAgotar;
                    ocultarMenu();
                    if (agotar != null) {
                        agotar.run();
                    }
                } else {
                    repaint();
                }
            });
            timerCuenta.start();
        }
        repaint();
        requestFocusInWindow();
    }

    public void ocultarMenu() {
        detenerTimerCuenta();
        menuEtiquetas = null;
        menuRects.clear();
        menuAlElegir = null;
        menuAlAgotar = null;
        menuLimiteMs = 0;
        if (modo == Modo.MENU) {
            modo = Modo.NINGUNO;
        }
        repaint();
    }

    /** Deja la pantalla limpia: cancela timers y olvida todo callback pendiente. */
    public void reiniciar() {
        if (timerUnico != null) {
            timerUnico.stop();
            timerUnico = null;
        }
        detenerTimerCuenta();
        sprites.clear();
        fondo = null;
        dlgImg = null;
        dlgTexto = null;
        dlgQuien = null;
        dlgFin = null;
        avisoImg = null;
        avisoTexto = null;
        zonasClic = null;
        clicFin = null;
        pistaClic = null;
        menuEtiquetas = null;
        menuRects.clear();
        menuAlElegir = null;
        menuAlAgotar = null;
        menuLimiteMs = 0;
        modo = Modo.NINGUNO;
        repaint();
    }

    // ===================== ENTRADA =====================

    private Point aBase(Point p) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        return new Point((int) (p.x * (double) Recursos.BASE_W / w),
                (int) (p.y * (double) Recursos.BASE_H / h));
    }

    private void clic(Point b) {
        switch (modo) {
            case DIALOGO:
                if (dlgImg != null || ZONA_DIALOGO.contains(b)) {
                    terminarDialogo();
                }
                break;
            case CLIC:
                for (Rectangle z : zonasClic) {
                    if (z.contains(b)) {
                        Runnable f = clicFin;
                        zonasClic = null;
                        clicFin = null;
                        pistaClic = null;
                        modo = Modo.NINGUNO;
                        repaint();
                        Funciones.reproducirSonido("/sonidos/click.wav");
                        f.run();
                        return;
                    }
                }
                break;
            case MENU:
                for (int i = 0; i < menuRects.size(); i++) {
                    if (menuRects.get(i).contains(b)) {
                        IntConsumer elegir = menuAlElegir;
                        ocultarMenu();
                        Funciones.reproducirSonido("/sonidos/click.wav");
                        elegir.accept(i);
                        return;
                    }
                }
                break;
            default:
                break;
        }
    }

    private void mover(Point b) {
        boolean mano = false;
        if (modo == Modo.MENU) {
            int h = -1;
            for (int i = 0; i < menuRects.size(); i++) {
                if (menuRects.get(i).contains(b)) {
                    h = i;
                }
            }
            if (h != menuHover) {
                menuHover = h;
                repaint();
            }
            mano = h >= 0;
        } else if (modo == Modo.DIALOGO) {
            mano = dlgImg != null || ZONA_DIALOGO.contains(b);
        } else if (modo == Modo.CLIC && zonasClic != null) {
            for (Rectangle z : zonasClic) {
                if (z.contains(b)) {
                    mano = true;
                }
            }
        }
        setCursor(mano ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    private void terminarDialogo() {
        if (modo != Modo.DIALOGO) {
            return;
        }
        Runnable f = dlgFin;
        dlgFin = null;
        dlgImg = null;
        dlgTexto = null;
        dlgQuien = null;
        modo = Modo.NINGUNO;
        repaint();
        Funciones.reproducirSonido("/sonidos/click.wav");
        if (f != null) {
            f.run();
        }
    }

    private void iniciarTimerUnico(int ms, Runnable alAcabar) {
        if (timerUnico != null) {
            timerUnico.stop();
        }
        timerUnico = new Timer(Math.max(1, (int) (ms * ESCALA_TIEMPO)), e -> {
            timerUnico = null;
            alAcabar.run();
        });
        timerUnico.setRepeats(false);
        timerUnico.start();
    }

    private void detenerTimerCuenta() {
        if (timerCuenta != null) {
            timerCuenta.stop();
            timerCuenta = null;
        }
    }

    /** Botones del menu en coordenadas de diseno: 2 columnas, la ultima fila se centra. */
    private void calcularRectsMenu(int n) {
        menuRects.clear();
        int bw = 380, bh = 100, gap = 30;
        int cols = (n == 1) ? 1 : 2;
        int centroX = Recursos.BASE_W / 2;
        int y0 = 380;
        for (int i = 0; i < n; i++) {
            int fila = i / cols;
            int col = i % cols;
            int enFila = Math.min(cols, n - fila * cols);
            int anchoFila = enFila * bw + (enFila - 1) * gap;
            int x = centroX - anchoFila / 2 + col * (bw + gap);
            int y = y0 + fila * (bh + gap);
            menuRects.add(new Rectangle(x, y, bw, bh));
        }
    }

    // ===================== DIBUJO =====================

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth(), H = getHeight();
        double sx = W / (double) Recursos.BASE_W;
        double sy = H / (double) Recursos.BASE_H;

        if (fondo != null) {
            g.drawImage(fondo, 0, 0, W, H, null);
        }
        for (Sprite s : sprites.values()) {
            g.drawImage(s.img, (int) (s.x * sx), (int) (s.y * sy), (int) (s.w * sx), (int) (s.h * sy), null);
        }
        if (modo == Modo.CLIC) {
            dibujarPistaYZonas(g, sx, sy);
        }
        if (dlgImg != null || dlgTexto != null) {
            dibujarDialogo(g, sx, sy);
        }
        if (modo == Modo.MENU && menuEtiquetas != null) {
            dibujarMenu(g, sx, sy);
        }
        if (modo == Modo.AVISO) {
            dibujarAviso(g, sx, sy);
        }
        dibujarHud(g, sx, sy);
        g.dispose();
    }

    private Rectangle escalar(Rectangle r, double sx, double sy) {
        return new Rectangle((int) (r.x * sx), (int) (r.y * sy), (int) (r.width * sx), (int) (r.height * sy));
    }

    private void caja(Graphics2D g, Rectangle r, double sy, Color relleno) {
        int arc = (int) (40 * sy);
        g.setColor(relleno);
        g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        g.setStroke(new BasicStroke(Math.max(2f, (float) (6 * sy))));
        g.setColor(MARRON);
        g.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);
    }

    private static List<String> envolver(String texto, FontMetrics fm, int maxW) {
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

    /** Texto centrado dentro de una caja; baja el tamano de letra hasta que quepa. */
    private void textoCentrado(Graphics2D g, String texto, Rectangle caja, int tamano, boolean negrita, Color color) {
        int tam = Math.max(10, tamano);
        List<String> lineas;
        FontMetrics fm;
        while (true) {
            g.setFont(new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, tam));
            fm = g.getFontMetrics();
            lineas = envolver(texto, fm, (int) (caja.width * 0.92));
            if (lineas.size() * fm.getHeight() <= caja.height * 0.92 || tam <= 10) {
                break;
            }
            tam--;
        }
        g.setColor(color);
        int total = lineas.size() * fm.getHeight();
        int y = caja.y + (caja.height - total) / 2 + fm.getAscent();
        for (String l : lineas) {
            g.drawString(l, caja.x + (caja.width - fm.stringWidth(l)) / 2, y);
            y += fm.getHeight();
        }
    }

    /** Dibuja una imagen de lienzo completo (burbuja, panel) centrada y sin deformarla. */
    private void dibujarImagenCompleta(Graphics2D g, BufferedImage img) {
        double esc = Math.min(getWidth() / (double) img.getWidth(), getHeight() / (double) img.getHeight());
        int w = (int) (img.getWidth() * esc);
        int h = (int) (img.getHeight() * esc);
        g.drawImage(img, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
    }

    private void dibujarDialogo(Graphics2D g, double sx, double sy) {
        Rectangle z = escalar(ZONA_DIALOGO, sx, sy);

        if (dlgImg != null) {
            // Posicion UNICA para todas las burbujas: centradas en ZONA_DIALOGO, pegadas abajo.
            // Escala fija (la de la pantalla); solo se reduce si la burbuja no cabe en la zona.
            double esc = Math.min(sx, sy);
            double ajuste = Math.min(1.0, Math.min(z.width / (dlgImg.getWidth() * esc),
                    z.height / (dlgImg.getHeight() * esc)));
            int w = (int) (dlgImg.getWidth() * esc * ajuste);
            int h = (int) (dlgImg.getHeight() * esc * ajuste);
            g.drawImage(dlgImg, z.x + (z.width - w) / 2, z.y + z.height - h, w, h, null);
            return;
        }

        int placaH = (int) (46 * sy);
        Rectangle cuerpo = new Rectangle(z.x, z.y + placaH / 2, z.width, z.height - placaH / 2);
        caja(g, cuerpo, sy, CREMA);

        if (dlgQuien != null && !dlgQuien.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, (int) (26 * sy))));
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(dlgQuien) + (int) (70 * sx);
            int px = z.x + (int) (50 * sx);
            g.setColor(MARRON);
            g.fillRoundRect(px, z.y, pw, placaH, (int) (24 * sy), (int) (24 * sy));
            g.setColor(Color.WHITE);
            g.drawString(dlgQuien, px + (pw - fm.stringWidth(dlgQuien)) / 2,
                    z.y + (placaH + fm.getAscent() - fm.getDescent()) / 2);
        }

        Rectangle t = new Rectangle(z.x + (int) (50 * sx), cuerpo.y + (int) (36 * sy),
                z.width - (int) (150 * sx), cuerpo.height - (int) (50 * sy));
        int tam = Math.max(12, (int) (30 * sy));
        List<String> lineas;
        FontMetrics fmt;
        while (true) {
            g.setFont(new Font("SansSerif", Font.PLAIN, tam));
            fmt = g.getFontMetrics();
            lineas = envolver(dlgTexto, fmt, t.width);
            if (lineas.size() * fmt.getHeight() <= t.height || tam <= 12) {
                break;
            }
            tam--;
        }
        g.setColor(MARRON.darker());
        int y = t.y + fmt.getAscent();
        for (String l : lineas) {
            g.drawString(l, t.x, y);
            y += fmt.getHeight();
        }

        // triangulo "siguiente"
        int tw = (int) (36 * sx), th = (int) (40 * sy);
        int tx = z.x + z.width - (int) (80 * sx), ty = z.y + z.height - (int) (70 * sy);
        g.setColor(NARANJA);
        g.fillPolygon(new int[]{tx, tx, tx + tw}, new int[]{ty, ty + th, ty + th / 2}, 3);
    }

    private void dibujarMenu(Graphics2D g, double sx, double sy) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRect(0, 0, getWidth(), getHeight());

        Rectangle tr = escalar(RECT_TITULO_MENU, sx, sy);
        caja(g, tr, sy, CREMA);
        textoCentrado(g, menuTitulo, tr, (int) (36 * sy), true, MARRON.darker());

        int abajo = 0;
        for (int i = 0; i < menuRects.size(); i++) {
            Rectangle r = escalar(menuRects.get(i), sx, sy);
            boolean hover = (i == menuHover);
            caja(g, r, sy, hover ? new Color(255, 205, 120) : NARANJA);
            textoCentrado(g, menuEtiquetas.get(i), r, (int) (32 * sy), true, hover ? MARRON.darker() : Color.WHITE);
            abajo = Math.max(abajo, menuRects.get(i).y + menuRects.get(i).height);
        }

        if (menuLimiteMs > 0) {
            Rectangle barra = escalar(new Rectangle(395, abajo + 40, 800, 36), sx, sy);
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRoundRect(barra.x, barra.y, barra.width, barra.height, barra.height, barra.height);
            double frac = Math.max(0, menuRestanteMs / (double) menuLimiteMs);
            g.setColor(frac > 0.3 ? new Color(90, 190, 90) : new Color(220, 60, 50));
            g.fillRoundRect(barra.x, barra.y, (int) (barra.width * frac), barra.height, barra.height, barra.height);
            int segundos = (int) Math.ceil(menuRestanteMs / (1000.0 * ESCALA_TIEMPO));
            textoCentrado(g, "Tiempo restante: " + segundos + " s", barra, (int) (24 * sy), true, Color.WHITE);
        }
    }

    private void dibujarAviso(Graphics2D g, double sx, double sy) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (avisoImg != null) {
            dibujarImagenCompleta(g, avisoImg);
        } else if (avisoTexto != null) {
            Rectangle z = escalar(new Rectangle(345, 330, 900, 330), sx, sy);
            caja(g, z, sy, CREMA);
            textoCentrado(g, avisoTexto, z, (int) (46 * sy), true, MARRON.darker());
        }
    }

    private void dibujarPistaYZonas(Graphics2D g, double sx, double sy) {
        if (zonasClic != null) {
            g.setStroke(new BasicStroke(Math.max(2f, (float) (3 * sy)), BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10f, new float[]{14f, 10f}, 0f));
            for (Rectangle z : zonasClic) {
                Rectangle r = escalar(z, sx, sy);
                g.setColor(new Color(255, 255, 255, 30));
                g.fillRoundRect(r.x, r.y, r.width, r.height, 24, 24);
                g.setColor(new Color(255, 255, 255, 170));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 24, 24);
            }
        }
        if (pistaClic != null) {
            Rectangle r = escalar(new Rectangle(395, 30, 800, 90), sx, sy);
            caja(g, r, sy, CREMA);
            textoCentrado(g, pistaClic, r, (int) (28 * sy), true, MARRON.darker());
        }
    }

    private void dibujarHud(Graphics2D g, double sx, double sy) {
        if (!mostrarHud || hudEstado == null) {
            return;
        }
        g.setFont(new Font("Monospaced", Font.BOLD, Math.max(11, (int) (18 * sy))));
        FontMetrics fm = g.getFontMetrics();
        Map<String, Integer> ind = hudEstado.getIndicadores();
        int x = (int) (15 * sx), y = (int) (15 * sy);
        int w = (int) (340 * sx), h = ind.size() * fm.getHeight() + (int) (20 * sy);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(Color.WHITE);
        int ty = y + fm.getAscent() + (int) (10 * sy);
        for (Map.Entry<String, Integer> e : ind.entrySet()) {
            g.drawString(String.format("%-20s %3d", e.getKey(), e.getValue()), x + (int) (14 * sx), ty);
            ty += fm.getHeight();
        }
    }
}