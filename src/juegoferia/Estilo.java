package juegoferia;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;

/**
 * Estética común de los minijuegos: la misma del resto del juego
 * (marcos de madera, pergamino, hojas verdes y letras redondeadas).
 * Todo se dibuja con Java2D, no necesita imágenes.
 */
public final class Estilo {

    private Estilo() {
    }

    public static final Color MADERA_OSCURA = new Color(0x6A3F1C);
    public static final Color MADERA = new Color(0xB5793E);
    public static final Color MADERA_CLARA = new Color(0xD9A05B);
    public static final Color PERGAMINO = new Color(0xFBF0D2);
    public static final Color PERGAMINO_BORDE = new Color(0xE6D09B);
    public static final Color ARENA = new Color(0xF1DFB0);
    public static final Color TEXTO = new Color(0x5A3B1F);
    public static final Color TEXTO_SUAVE = new Color(0x9C7B55);
    public static final Color HOJA = new Color(0x5BA843);
    public static final Color HOJA_OSCURA = new Color(0x3E7F2F);
    public static final Color ROJO = new Color(0xC9553A);
    public static final Color DORADO = new Color(0xF2B01E);
    public static final Color DORADO_CLARO = new Color(0xFFD35C);

    private static String familia;

    /** Letra redondeada y amigable: usa la primera de estas que exista en el equipo. */
    public static Font fuente(int estilo, float tam) {
        if (familia == null) {
            Set<String> hay = new HashSet<>(Arrays.asList(GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            String[] pref = {"Baloo 2", "Fredoka", "Fredoka One", "Nunito", "Comic Sans MS",
                "Trebuchet MS", "Verdana"};
            familia = Font.SANS_SERIF;
            for (String f : pref) {
                if (hay.contains(f)) {
                    familia = f;
                    break;
                }
            }
        }
        return new Font(familia, estilo, Math.round(tam));
    }

    public static void suave(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Tabla de madera con vetas, borde oscuro y sombra. */
    public static void madera(Graphics2D g, int x, int y, int w, int h, int arco) {
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new RoundRectangle2D.Float(x + 2, y + 4, w, h, arco, arco));
        Shape forma = new RoundRectangle2D.Float(x, y, w, h, arco, arco);
        g.setPaint(new GradientPaint(0, y, MADERA_CLARA, 0, y + h, MADERA));
        g.fill(forma);
        Shape antes = g.getClip();
        g.clip(forma);
        g.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < Math.max(3, h / 9); i++) {
            int yy = y + 4 + i * 9 + (i * 7919 % 5);
            g.setColor(new Color(0x7A4A24 & 0xFFFFFF | 0x30000000, true));
            int o = (i * 37) % 40;
            g.drawLine(x + o, yy, x + w - (i * 53) % 60, yy + ((i % 3) - 1));
        }
        g.setClip(antes);
        g.setColor(MADERA_OSCURA);
        g.setStroke(new BasicStroke(3f));
        g.draw(forma);
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x + 3, y + 3, w - 6, h - 6, arco - 3, arco - 3));
    }

    /** Hoja de papel/pergamino con degradado suave y borde. */
    public static void pergamino(Graphics2D g, int x, int y, int w, int h, int arco) {
        Shape forma = new RoundRectangle2D.Float(x, y, w, h, arco, arco);
        g.setPaint(new GradientPaint(0, y, new Color(0xFFF8E3), 0, y + h, PERGAMINO));
        g.fill(forma);
        g.setColor(PERGAMINO_BORDE);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(forma);
    }

    /** Hoja verde (como las de las esquinas de los carteles del juego). */
    public static void hoja(Graphics2D g, double cx, double cy, double ang, double largo) {
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.rotate(Math.toRadians(ang));
        double a = largo, b = largo * 0.42;
        Path2D p = new Path2D.Double();
        p.moveTo(0, 0);
        p.curveTo(a * 0.2, -b * 1.4, a * 0.8, -b * 1.1, a, 0);
        p.curveTo(a * 0.8, b * 1.1, a * 0.2, b * 1.4, 0, 0);
        g.setColor(HOJA);
        g.fill(p);
        g.setColor(HOJA_OSCURA);
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
        g.setColor(new Color(255, 255, 255, 90));
        g.drawLine((int) (a * 0.1), 0, (int) (a * 0.85), 0);
        g.setTransform(at);
    }

    /** Grupo de hojas para decorar una esquina. dir: -1/1 horizontal, -1/1 vertical. */
    public static void hojas(Graphics2D g, int x, int y, int dx, int dy, int tam) {
        double base = (dx > 0 ? 0 : 180) + (dy > 0 ? 0 : 0);
        hoja(g, x, y, (dx > 0 ? -35 : 215) + (dy > 0 ? 70 : 0), tam);
        hoja(g, x, y, (dx > 0 ? 20 : 160) + (dy > 0 ? 40 : -40), tam * 0.85);
        hoja(g, x, y, (dx > 0 ? 75 : 105) + (dy > 0 ? 20 : -30), tam * 0.7);
    }

    /** Texto con contorno (el estilo de los títulos del juego). */
    public static void textoContorno(Graphics2D g, String s, int x, int y, Color relleno, Color contorno) {
        g.setColor(contorno);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(s, x + dx, y + dy);
                }
            }
        }
        g.setColor(relleno);
        g.drawString(s, x, y);
    }

    /** Botón de madera redondeado, como los del juego ("Volver", etc.). */
    public static class BotonMadera extends JButton {

        private boolean encima, pulsado;

        public BotonMadera(String texto) {
            super(texto);
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(new Color(0xFFF3D6));
            setFont(fuente(Font.BOLD, 15f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pulsado = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pulsado = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 44, 38);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            int off = pulsado ? 2 : 0;
            int w = getWidth() - 4, h = getHeight() - 6;
            Color a = encima ? MADERA_CLARA.brighter() : MADERA_CLARA;
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new RoundRectangle2D.Float(2, 5, w, h, 20, 20));
            Shape f = new RoundRectangle2D.Float(2, 2 + off, w, h, 20, 20);
            g.setPaint(new GradientPaint(0, 2, a, 0, h, MADERA));
            g.fill(f);
            g.setColor(MADERA_OSCURA);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(f);
            g.setColor(new Color(255, 255, 255, 80));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Float(5, 5 + off, w - 6, h - 6, 16, 16));
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int tx = 2 + (w - fm.stringWidth(getText())) / 2;
            int ty = 2 + off + (h + fm.getAscent() - fm.getDescent()) / 2;
            g.setColor(MADERA_OSCURA);
            g.drawString(getText(), tx + 1, ty + 2);
            g.setColor(getForeground());
            g.drawString(getText(), tx, ty);
            g.dispose();
        }
    }
}
