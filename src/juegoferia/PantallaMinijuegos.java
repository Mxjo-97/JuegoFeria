package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Hub de minijuegos del Influencer.
 * Muestra tarjetas para "Tendencia" y "En vivo". Al elegir una, la corre
 * y al terminar aplica el resultado al EstadoJuego y vuelve al hub.
 */
public class PantallaMinijuegos extends JPanel {

    private static final int W = 1920, H = 1080;

    private static final class Tarjeta {
        final String titulo, subtitulo;
        final Color color;
        final int indice;
        Rectangle rect = new Rectangle();
        double hover = 0;
        Tarjeta(String t, String s, Color c, int i) {
            titulo = t; subtitulo = s; color = c; indice = i;
        }
    }

    private final java.awt.CardLayout card = new java.awt.CardLayout();
    private final JPanel contenedor = new JPanel(card);
    private final List<Tarjeta> tarjetas = new ArrayList<>();

    private final Runnable alVolver;
    private final Runnable alTerminarTodo;
    private final EstadoJuego estado;

    private Minijuego actual;
    private final Timer reloj = new Timer(16, e -> repaint());

    public PantallaMinijuegos(Runnable alVolver, Runnable alTerminarTodo, EstadoJuego estado) {
        this.alVolver = alVolver;
        this.alTerminarTodo = alTerminarTodo;
        this.estado = estado;

        setLayout(card);
        setOpaque(true);
        setBackground(Color.BLACK);

        add(hubPanel(), "hub");
        contenedor.setOpaque(false);
        add(contenedor, "minijuego");
        card.show(this, "hub");

        Color[] colores = {new Color(0x5B3FD8), new Color(0x3F8EDC), new Color(0xE0A030), new Color(0x5BA843),
            new Color(0xD9304F), new Color(0x8A3A5A), new Color(0xC9A020)};
        for (int i = 0; i < Minijuegos.LISTA.length; i++) {
            Minijuegos.Entrada m = Minijuegos.LISTA[i];
            tarjetas.add(new Tarjeta(m.titulo, m.tipo + ". " + m.descripcion, colores[i % colores.length], i));
        }

        MouseAdapter raton = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (actual != null) return;   // no estamos en el hub
                Point p = aDiseno(e.getPoint());
                if (zonaVolver().contains(p) && SwingUtilities.isLeftMouseButton(e)) {
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    alVolver.run();
                    return;
                }
                for (Tarjeta t : tarjetas) {
                    if (t.rect.contains(p)) {
                        lanzar(t.indice);
                        return;
                    }
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                if (actual != null) return;
                Point p = aDiseno(e.getPoint());
                for (Tarjeta t : tarjetas) {
                    double obj = t.rect.contains(p) ? 1 : 0;
                    t.hover += (obj - t.hover) * 0.25;
                }
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) reloj.start();
                else reloj.stop();
            }
        });
    }

    // ===================== NAVEGACIÓN =====================

    private void lanzar(int indice) {
        Funciones.reproducirSonido("/sonidos/click.wav");
        actual = Minijuegos.crear(indice);
        if (actual == null) return;

        contenedor.removeAll();
        contenedor.add(actual, "mj");
        card.show(this, "minijuego");
        actual.requestFocusInWindow();

        actual.iniciar(resultado -> {
            estado.aplicarEfecto(resultado.efecto, "Prueba de minijuego");
            estado.imprimir("Prueba: " + resultado.resumen);
            contenedor.removeAll();
            actual = null;
            card.show(this, "hub");
            if (alTerminarTodo != null) alTerminarTodo.run();
        });
    }

    private Rectangle zonaVolver() {
        return new Rectangle(50, 950, 260, 80);
    }

    // ===================== ESCALA =====================

    private Point aDiseno(Point p) {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double s = Math.min(w / (double) W, h / (double) H);
        double ox = (w - W * s) / 2, oy = (h - H * s) / 2;
        return new Point((int) ((p.x - ox) / s), (int) ((p.y - oy) / s));
    }

    // ===================== DIBUJO DEL HUB =====================

    private JPanel hubPanel() {
        JPanel hub = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                dibujarHub(g, getWidth(), getHeight());
                g.dispose();
            }
        };
        hub.setOpaque(false);
        return hub;
    }

    private void dibujarHub(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x1B0F3B), 0, h, new Color(0x3A1F5C)));
        g.fillRect(0, 0, w, h);

        double s = Math.min(w / (double) W, h / (double) H);
        g.translate((w - W * s) / 2, (h - H * s) / 2);
        g.scale(s, s);

        g.setFont(new Font("SansSerif", Font.BOLD, 90));
        String titulo = "Minijuegos (modo prueba)";
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(titulo, (W - fm.stringWidth(titulo)) / 2, 180);

        g.setFont(new Font("SansSerif", Font.PLAIN, 34));
        String sub = "Para probar cada minijuego sin jugar todo el día.";
        fm = g.getFontMetrics();
        g.setColor(new Color(0xCCCCDD));
        g.drawString(sub, (W - fm.stringWidth(sub)) / 2, 240);

        int bw = 420, bh = 300, gap = 30, y = 300;
        int x0 = (W - (bw * 4 + gap * 3)) / 2;
        for (int i = 0; i < tarjetas.size(); i++) {
            Tarjeta t = tarjetas.get(i);
            t.rect = new Rectangle(x0 + (i % 4) * (bw + gap), y + (i / 4) * (bh + gap), bw, bh);
            dibujarTarjeta(g, t);
        }

        Rectangle r = zonaVolver();
        RoundRectangle2D pill = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 40, 40);
        g.setColor(new Color(0x333344));
        g.fill(pill);
        g.setColor(new Color(0xDDDDDD));
        g.setStroke(new BasicStroke(4f));
        g.draw(pill);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString("Volver", r.x + (r.width - fm.stringWidth("Volver")) / 2, r.y + 50);
    }

    private void dibujarTarjeta(Graphics2D g, Tarjeta t) {
        Rectangle r = t.rect;
        RoundRectangle2D caja = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 50, 50);
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(r.x + 8, r.y + 14, r.width, r.height, 50, 50));
        g.setPaint(new GradientPaint(r.x, r.y, t.color.brighter(), r.x, r.y + r.height, t.color.darker()));
        g.fill(caja);

        g.setColor(new Color(255, 255, 255, (int) (40 + 100 * t.hover)));
        g.fill(new RoundRectangle2D.Double(r.x + 20, r.y + 20, r.width - 40, 100, 40, 40));

        g.setColor(new Color(0x2A1A3A));
        g.setStroke(new BasicStroke(6f));
        g.draw(caja);

        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(t.titulo, r.x + (r.width - fm.stringWidth(t.titulo)) / 2, r.y + 80);

        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        fm = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 220));
        int y = r.y + 150;
        for (String l : envolver(t.subtitulo, fm, r.width - 80)) {
            g.drawString(l, r.x + (r.width - fm.stringWidth(l)) / 2, y);
            y += fm.getHeight();
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.setColor(new Color(0xFFF1A8));
        String jugar = "¡Jugar!";
        fm = g.getFontMetrics();
        g.drawString(jugar, r.x + (r.width - fm.stringWidth(jugar)) / 2, r.y + r.height - 40);
    }

    private static List<String> envolver(String texto, FontMetrics fm, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) > maxW && actual.length() > 0) {
                out.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        out.add(actual.toString());
        return out;
    }
}