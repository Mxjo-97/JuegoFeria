package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Piezas de dibujo "acogedoras" que comparten las pantallas bonitas (minijuegos y creditos):
 * fondo con velo crema, cartel de madera con flores, tarjetas tipo sticker, pastillas,
 * florecitas, mariposas y petalos que caen. Todo en el lienzo de diseno 1920 x 1080.
 */
final class Acogedor {

    private Acogedor() {
    }

    static final int W = 1920, H = 1080;
    static final Color MARRON = new Color(0x6E3F1E);
    static final Color MARRON_SUAVE = new Color(0xA0764E);
    static final Color CREMA = new Color(0xFFF9EE);
    static final Color CREMA_2 = new Color(0xFBEFD6);
    static final Color ROSA = new Color(0xF2A7B8);
    static final Color AMARILLO = new Color(0xF6C445);

    /** Fondo: una imagen del juego con velo crema y vineta suave (o un degradado si no esta). */
    static void fondo(Graphics2D g, String ruta, float velo) {
        if (Recursos.existe(ruta)) {
            g.drawImage(Recursos.imagen(ruta, W, H), 0, 0, W, H, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(0xFCE3C4), 0, H, new Color(0xF6C9A8)));
            g.fillRect(0, 0, W, H);
        }
        g.setPaint(new GradientPaint(0, 0, new Color(255, 246, 232, (int) (255 * velo)),
                0, H, new Color(252, 228, 214, (int) (255 * Math.min(1, velo + 0.08)))));
        g.fillRect(0, 0, W, H);
        g.setPaint(new RadialGradientPaint(W / 2f, H / 2f, W * 0.72f, new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(90, 45, 20, 70)}));
        g.fillRect(0, 0, W, H);
    }

    /** Petalos y mariposas que caen despacito (sin estado: depende solo de t). */
    static void petalos(Graphics2D g, double t, int n) {
        Color[] cs = {new Color(255, 255, 255, 200), new Color(242, 167, 184, 190), new Color(246, 196, 69, 190)};
        for (int i = 0; i < n; i++) {
            double r1 = rnd(i * 3.1), r2 = rnd(i * 7.7), r3 = rnd(i * 1.9);
            double vel = 18 + r2 * 26;
            double y = ((r1 * H + t * vel) % (H + 80)) - 40;
            double x = r3 * W + Math.sin(t * (0.4 + r2 * 0.5) + i) * 40;
            AffineTransform at = g.getTransform();
            g.translate(x, y);
            g.rotate(t * (0.6 + r1) + i);
            if (i % 7 == 0) {
                mariposa(g, 0, 0, 0.8, t + i);
            } else {
                g.setColor(cs[i % cs.length]);
                g.fill(new Ellipse2D.Double(-6, -3.5, 12, 7));
            }
            g.setTransform(at);
        }
    }

    static double rnd(double s) {
        double v = Math.sin(s * 12.9898 + 78.233) * 43758.5453;
        return v - Math.floor(v);
    }

    /** Cartel de madera con titulo y una cinta crema debajo (como el del menu). */
    static void cartel(Graphics2D g, double cx, double y, String titulo, String cinta, double t) {
        g.setFont(Estilo.fuente(Font.BOLD, 70f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(titulo) + 140, h = 118;
        int x = (int) (cx - w / 2.0);
        double bob = Math.sin(t * 1.2) * 3;
        int yy = (int) (y + bob);
        // tabla
        g.setColor(new Color(60, 30, 10, 70));
        g.fill(new RoundRectangle2D.Double(x + 4, yy + 10, w, h, 46, 46));
        g.setPaint(new GradientPaint(0, yy, new Color(0xB97A44), 0, yy + h, new Color(0x8A5328)));
        g.fill(new RoundRectangle2D.Double(x, yy, w, h, 46, 46));
        g.setColor(new Color(255, 255, 255, 40));
        for (int i = 1; i < 4; i++) {
            g.fill(new RoundRectangle2D.Double(x + 30, yy + i * 28, w - 60, 3, 3, 3));
        }
        g.setColor(MARRON);
        g.setStroke(new BasicStroke(6f));
        g.draw(new RoundRectangle2D.Double(x, yy, w, h, 46, 46));
        // clavitos
        g.setColor(new Color(0x5A3216));
        g.fill(new Ellipse2D.Double(x + 18, yy + 16, 12, 12));
        g.fill(new Ellipse2D.Double(x + w - 30, yy + 16, 12, 12));
        // titulo con sombra
        g.setFont(Estilo.fuente(Font.BOLD, 70f));
        int tx = (int) (cx - fm.stringWidth(titulo) / 2.0);
        g.setColor(new Color(0x5A2D12));
        g.drawString(titulo, tx + 3, yy + 86);
        g.setPaint(new GradientPaint(0, yy + 30, new Color(0xFFF6E2), 0, yy + 90, new Color(0xFFE2B0)));
        g.drawString(titulo, tx, yy + 82);
        // flores en las esquinas
        flor(g, x + 6, yy + 4, 22, Color.WHITE, t * 0.3);
        flor(g, x + w - 8, yy + h - 6, 18, ROSA, -t * 0.3);
        mariposa(g, x + w - 30, yy - 6 + Math.sin(t * 2) * 5, 1.5, t);
        // cinta
        if (cinta != null) {
            g.setFont(Estilo.fuente(Font.BOLD, 22f));
            FontMetrics f2 = g.getFontMetrics();
            int cw = f2.stringWidth(cinta) + 80, ch = 46;
            int cxx = (int) (cx - cw / 2.0), cy = yy + h - 12;
            g.setColor(new Color(60, 30, 10, 50));
            g.fill(new RoundRectangle2D.Double(cxx + 2, cy + 5, cw, ch, ch, ch));
            g.setPaint(new GradientPaint(0, cy, new Color(0xFFF4D8), 0, cy + ch, new Color(0xF4DDB0)));
            g.fill(new RoundRectangle2D.Double(cxx, cy, cw, ch, ch, ch));
            g.setColor(MARRON);
            g.setStroke(new BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(cxx, cy, cw, ch, ch, ch));
            g.setColor(new Color(0x7A4A24));
            g.drawString(cinta, cxx + 40, cy + 31);
            estrellita(g, cxx + 20, cy + ch / 2.0, 8, AMARILLO);
            estrellita(g, cxx + cw - 20, cy + ch / 2.0, 8, AMARILLO);
        }
    }

    /** Pastilla crema con borde marron (botones como "Volver"). */
    static void pastilla(Graphics2D g, double x, double y, double w, double h, String texto, double hover, Color punto) {
        double lift = hover * 4;
        g.setColor(new Color(60, 30, 10, 60));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 6, w, h, h, h));
        g.setPaint(new GradientPaint(0, (float) (y - lift), hover > 0.5 ? new Color(0xFFF1C9) : CREMA,
                0, (float) (y + h), CREMA_2));
        g.fill(new RoundRectangle2D.Double(x, y - lift, w, h, h, h));
        g.setColor(MARRON);
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(x, y - lift, w, h, h, h));
        g.setFont(Estilo.fuente(Font.BOLD, (float) (h * 0.42)));
        FontMetrics fm = g.getFontMetrics();
        int extra = punto != null ? 22 : 0;
        int tx = (int) (x + (w - fm.stringWidth(texto) + extra) / 2);
        if (punto != null) {
            g.setColor(punto);
            g.fill(new Ellipse2D.Double(tx - 24, y - lift + h / 2 - 7, 14, 14));
        }
        g.setColor(new Color(0x5A3216));
        g.drawString(texto, tx, (int) (y - lift + h / 2 + fm.getAscent() * 0.36));
    }

    /** Tarjeta tipo sticker: borde blanco, contorno marron, cuerpo crema. Devuelve la forma. */
    static Shape sticker(Graphics2D g, double x, double y, double w, double h, double r, Color cuerpo) {
        RoundRectangle2D f = new RoundRectangle2D.Double(x, y, w, h, r, r);
        g.setColor(new Color(70, 35, 12, 60));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 10, w, h, r, r));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(x - 6, y - 6, w + 12, h + 12, r + 12, r + 12));
        g.setColor(cuerpo);
        g.fill(f);
        g.setColor(MARRON);
        g.setStroke(new BasicStroke(4f));
        g.draw(f);
        return f;
    }

    static void flor(Graphics2D g, double x, double y, double r, Color petalo, double rot) {
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        for (int i = 0; i < 5; i++) {
            g.rotate(Math.PI * 2 / 5);
            g.setColor(petalo);
            g.fill(new Ellipse2D.Double(-r * 0.32, -r * 1.0, r * 0.64, r * 0.9));
            g.setColor(new Color(0, 0, 0, 25));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Ellipse2D.Double(-r * 0.32, -r * 1.0, r * 0.64, r * 0.9));
        }
        g.setColor(AMARILLO);
        g.fill(new Ellipse2D.Double(-r * 0.3, -r * 0.3, r * 0.6, r * 0.6));
        g.setTransform(at);
    }

    static void mariposa(Graphics2D g, double x, double y, double s, double t) {
        double ala = 0.65 + 0.35 * Math.abs(Math.sin(t * 5));
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.scale(s, s);
        g.setColor(AMARILLO);
        g.fill(new Ellipse2D.Double(-12 * ala, -10, 12 * ala, 12));
        g.fill(new Ellipse2D.Double(0, -10, 12 * ala, 12));
        g.setColor(new Color(0xFBE08F));
        g.fill(new Ellipse2D.Double(-9 * ala, 0, 9 * ala, 8));
        g.fill(new Ellipse2D.Double(0, 0, 9 * ala, 8));
        g.setColor(new Color(0xC98A12));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(-12 * ala, -10, 12 * ala, 12));
        g.draw(new Ellipse2D.Double(0, -10, 12 * ala, 12));
        g.setColor(MARRON);
        g.fill(new RoundRectangle2D.Double(-1.2, -9, 2.4, 17, 2, 2));
        g.setTransform(at);
    }

    static void estrellita(Graphics2D g, double cx, double cy, double r, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - r);
        p.quadTo(cx, cy, cx + r, cy);
        p.quadTo(cx, cy, cx, cy + r);
        p.quadTo(cx, cy, cx - r, cy);
        p.quadTo(cx, cy, cx, cy - r);
        g.setColor(c);
        g.fill(p);
    }

    /** Foto redonda con marco de flores. Si la imagen no esta, una inicial bonita (sin romper nada). */
    static void foto(Graphics2D g, String[] rutas, double cx, double cy, double r, Color aro, String inicial, double t) {
        g.setColor(new Color(70, 35, 12, 60));
        g.fill(new Ellipse2D.Double(cx - r - 2, cy - r + 8, (r + 8) * 2, (r + 8) * 2));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2));
        Ellipse2D c = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        String ruta = null;
        for (String x : rutas) {
            if (Recursos.existe(x)) {
                ruta = x;
                break;
            }
        }
        if (ruta != null) {
            BufferedImage im = Recursos.imagen(ruta, 300, 300);
            double esc = Math.max(r * 2 / im.getWidth(), r * 2 / im.getHeight());
            int iw = (int) (im.getWidth() * esc), ih = (int) (im.getHeight() * esc);
            Shape antes = g.getClip();
            g.clip(c);
            g.drawImage(im, (int) (cx - iw / 2.0), (int) (cy - ih / 2.0), iw, ih, null);
            g.setClip(antes);
        } else {
            g.setPaint(new GradientPaint(0, (float) (cy - r), aro.brighter(), 0, (float) (cy + r), aro));
            g.fill(c);
            g.setColor(new Color(255, 255, 255, 230));
            g.setFont(Estilo.fuente(Font.BOLD, (float) (r * 1.05)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(inicial, (float) (cx - fm.stringWidth(inicial) / 2.0), (float) (cy + fm.getAscent() * 0.36));
        }
        g.setColor(aro);
        g.setStroke(new BasicStroke(6f));
        g.draw(c);
        flor(g, cx - r * 0.78, cy - r * 0.78, r * 0.24, Color.WHITE, t * 0.4);
        flor(g, cx + r * 0.85, cy + r * 0.62, r * 0.18, ROSA, -t * 0.4);
        mariposa(g, cx + r * 0.9, cy - r * 0.75 + Math.sin(t * 2 + cx) * 4, 1.2, t + cx);
    }

    static void centrado(Graphics2D g, String s, double cx, double y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) y);
    }

    static void alfa(Graphics2D g, double a) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, a))));
    }
}
