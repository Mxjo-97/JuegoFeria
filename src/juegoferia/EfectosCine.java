package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Efectos "de pelicula" para Cinematica: olas, viento, niebla, nubes, estrellas, polvo,
 * rayos de luz, mariposa, rumor, resplandores, vineta, grano y titulo animado.
 *
 * Todos los efectos son SIN ESTADO: se dibujan solo en funcion del tiempo t (segundos desde
 * que empezo el cuadro). Por eso no hay que actualizar nada ni se rompen al saltar cuadros.
 * Coordenadas en el lienzo de diseno 1920x1080.
 */
public final class EfectosCine {

    private EfectosCine() {
    }

    static final int W = PanelDiseno.W;
    static final int H = PanelDiseno.H;

    /** Una capa dibujable. mundo = true: se mueve junto con la camara (anclada al fondo). */
    public interface Capa {
        void pintar(Graphics2D g, double t);
    }

    public static final class Efecto {
        final Capa capa;
        final boolean mundo;

        Efecto(boolean mundo, Capa capa) {
            this.mundo = mundo;
            this.capa = capa;
        }
    }

    // ===================== UTILIDADES =====================

    static double rnd(double semilla) {
        double v = Math.sin(semilla * 12.9898 + 78.233) * 43758.5453;
        return v - Math.floor(v);
    }

    static double clamp01(double x) {
        return x < 0 ? 0 : (x > 1 ? 1 : x);
    }

    static double suave(double x) {
        x = clamp01(x);
        return x * x * (3 - 2 * x);
    }

    static double mezcla(double a, double b, double k) {
        return a + (b - a) * k;
    }

    private static Color con(int rgb, double alfa) {
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (255 * clamp01(alfa)));
    }

    private static void alfa(Graphics2D g, double a) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp01(a)));
    }

    // Manchas suaves (gradiente radial) en cache: baratas de dibujar, escalables, tenibles.
    private static final Map<Integer, BufferedImage> MANCHAS = new HashMap<>();

    private static synchronized BufferedImage mancha(int rgb) {
        BufferedImage b = MANCHAS.get(rgb);
        if (b != null) return b;
        int n = 256;
        b = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.setPaint(new RadialGradientPaint(n / 2f, n / 2f, n / 2f,
                new float[]{0f, 0.35f, 1f},
                new Color[]{con(rgb, 1), con(rgb, 0.38), con(rgb, 0)}));
        g.fillRect(0, 0, n, n);
        g.dispose();
        MANCHAS.put(rgb, b);
        return b;
    }

    private static void brillo(Graphics2D g, int rgb, double cx, double cy, double rx, double ry, double a) {
        if (a <= 0.003) return;
        java.awt.Composite old = g.getComposite();
        alfa(g, a);
        g.drawImage(mancha(rgb), (int) (cx - rx), (int) (cy - ry), (int) (rx * 2), (int) (ry * 2), null);
        g.setComposite(old);
    }

    // ===================== FABRICAS DE EFECTOS =====================

    /** Estrellas que titilan; aparecen poco a poco. */
    public static Efecto estrellas() {
        return new Efecto(false, (g, t) -> {
            double entra = suave(t / 3.5);
            for (int i = 0; i < 90; i++) {
                double x = rnd(i * 3.1) * W;
                double y = Math.pow(rnd(i * 5.7), 1.6) * 640;
                double tam = 1.2 + rnd(i * 9.3) * 2.6;
                double ti = 0.5 + 0.5 * Math.sin(t * (0.8 + rnd(i * 2.2) * 2.2) + i * 7.7);
                double a = entra * (0.25 + 0.75 * ti) * (0.5 + 0.5 * rnd(i * 1.7));
                int rgb = rnd(i * 4.4) > 0.8 ? 0xFFE9B0 : 0xDDEBFF;
                brillo(g, rgb, x, y, tam * 5, tam * 5, a * 0.55);
                g.setColor(con(0xFFFFFF, a));
                g.fill(new Ellipse2D.Double(x - tam / 2, y - tam / 2, tam, tam));
                if (tam > 3.1 && ti > 0.6) {
                    g.setStroke(new BasicStroke(1.2f));
                    g.setColor(con(0xFFFFFF, a * 0.7));
                    g.drawLine((int) (x - tam * 4), (int) y, (int) (x + tam * 4), (int) y);
                    g.drawLine((int) x, (int) (y - tam * 4), (int) x, (int) (y + tam * 4));
                }
            }
        });
    }

    /** Capas de bruma/niebla que derivan. crece: se va espesando con el tiempo. */
    public static Efecto bruma(int rgb, double alfaMax, double velocidad, double yMin, double yMax,
            boolean crece, int cantidad) {
        return new Efecto(false, (g, t) -> {
            double dens = crece ? 0.12 + 0.88 * suave(t / 14.0) : suave(t / 2.0);
            for (int i = 0; i < cantidad; i++) {
                double r = rnd(i * 6.1 + 1);
                double ancho = 900 + rnd(i * 2.9) * 800;
                double alto = 260 + rnd(i * 8.3) * 340;
                double ciclo = W + ancho * 2;
                double x = ((rnd(i * 4.7) * ciclo + t * velocidad * (0.5 + r)) % ciclo) - ancho;
                double y = yMin + rnd(i * 7.9) * (yMax - yMin) + Math.sin(t * 0.25 + i) * 28;
                brillo(g, rgb, x, y, ancho / 2, alto / 2, alfaMax * dens * (0.55 + 0.45 * rnd(i * 3.3)));
            }
        });
    }

    /**
     * Olas que suben y bajan sobre la orilla. yOrilla = linea donde llega la espuma
     * (el mar queda ARRIBA de esa linea y la arena ABAJO).
     */
    public static Efecto olas(int yOrilla) {
        return new Efecto(true, (g, t) -> {
            // reflejos de luna sobre el agua
            for (int i = 0; i < 55; i++) {
                double x = 200 + rnd(i * 2.3) * (W - 400);
                double y = yOrilla - 40 - rnd(i * 5.1) * 330;
                double tw = 0.5 + 0.5 * Math.sin(t * (1.2 + rnd(i * 1.3) * 2) + i * 3.7);
                double largo = 18 + rnd(i * 9.7) * 70 * (1 - (yOrilla - y) / 420.0 * 0.6);
                g.setColor(con(0xDDEBFF, 0.10 + 0.30 * tw));
                g.fill(new Rectangle2D.Double(x - largo / 2 + Math.sin(t * 0.7 + i) * 6, y, largo, 2.2));
            }
            // tres frentes de espuma desfasados: avanzan, se desvanecen y se retiran
            for (int k = 0; k < 3; k++) {
                double ciclo = 7.5;
                double fase = ((t / ciclo) + k / 3.0) % 1.0;
                double avance = Math.sin(fase * Math.PI);          // 0 -> 1 -> 0
                double base = yOrilla - 55 * k + 70 * avance;
                double a = Math.sin(fase * Math.PI);
                Path2D.Double borde = new Path2D.Double();
                Path2D.Double agua = new Path2D.Double();
                agua.moveTo(-30, yOrilla - 420);
                for (int x = -30; x <= W + 30; x += 24) {
                    double y = base + 9 * Math.sin(x / 95.0 + t * 1.2 + k * 2.1)
                            + 5 * Math.sin(x / 41.0 - t * 2.1 + k);
                    if (x == -30) borde.moveTo(x, y);
                    else borde.lineTo(x, y);
                    agua.lineTo(x, y);
                }
                agua.lineTo(W + 30, yOrilla - 420);
                agua.closePath();
                g.setPaint(new GradientPaint(0, (float) (yOrilla - 160), con(0x7FB4E6, 0),
                        0, (float) (base + 14), con(0xB8DCFF, 0.20 * a + 0.04)));
                g.fill(agua);
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(con(0xF2FAFF, 0.50 * a));
                g.draw(borde);
                g.setStroke(new BasicStroke(2f));
                g.setColor(con(0xFFFFFF, 0.70 * a));
                g.draw(borde);
            }
            // arena mojada: oscurece un poco justo donde llego el agua
            double mojado = 0.5 + 0.5 * Math.sin(t / 7.5 * Math.PI * 2 - 1.2);
            g.setPaint(new GradientPaint(0, yOrilla, con(0x0A1630, 0.16 * mojado),
                    0, yOrilla + 90, con(0x0A1630, 0)));
            g.fill(new Rectangle2D.Double(0, yOrilla, W, 90));
        });
    }

    /** Rafagas de viento (estelas) y hojitas/polvo arrastrados de izquierda a derecha. */
    public static Efecto viento(double fuerza) {
        return new Efecto(false, (g, t) -> {
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int estelas = (int) (22 * fuerza);
            for (int i = 0; i < estelas; i++) {
                double vel = 0.10 + rnd(i * 3.3) * 0.12;
                double u = (t * vel + rnd(i * 1.9)) % 1.0;
                double x = -400 + u * (W + 800);
                double y = 80 + rnd(i * 7.1) * (H - 260) + Math.sin(u * 7 + i) * 26;
                double largo = 180 + rnd(i * 5.5) * 260;
                double a = Math.sin(u * Math.PI) * 0.20 * fuerza;
                Path2D.Double p = new Path2D.Double();
                p.moveTo(x - largo, y);
                p.curveTo(x - largo * 0.6, y - 18, x - largo * 0.3, y + 22, x, y - 6);
                g.setColor(con(0xFFFFFF, a));
                g.draw(p);
            }
            int hojas = (int) (34 * fuerza);
            for (int i = 0; i < hojas; i++) {
                double vel = 0.05 + rnd(i * 2.1) * 0.08;
                double u = (t * vel + rnd(i * 8.8)) % 1.0;
                double x = -60 + u * (W + 120);
                double y = rnd(i * 4.2) * (H - 200) + 40 * Math.sin(u * 10 + i * 2) + u * 90;
                double gir = t * (1.5 + rnd(i * 1.1) * 3) + i;
                double tam = 5 + rnd(i * 6.6) * 8;
                Graphics2D p = (Graphics2D) g.create();
                p.translate(x, y);
                p.rotate(gir);
                int rgb = rnd(i * 3.7) > 0.5 ? 0xC9B27A : 0x8FA865;
                p.setColor(con(rgb, 0.65 * Math.sin(u * Math.PI)));
                p.fill(new Ellipse2D.Double(-tam, -tam * 0.4, tam * 2, tam * 0.8));
                p.dispose();
            }
        });
    }

    /** Motas de polvo/luz que flotan hacia arriba. */
    public static Efecto polvo(int rgb, int cantidad) {
        return new Efecto(false, (g, t) -> {
            double entra = suave(t / 1.5);
            for (int i = 0; i < cantidad; i++) {
                double vel = 14 + rnd(i * 2.7) * 30;
                double x = (rnd(i * 3.9) * W + Math.sin(t * 0.5 + i * 1.7) * 40 + t * 10) % W;
                double y = H - ((rnd(i * 6.3) * H + t * vel) % H);
                double tam = 2 + rnd(i * 8.1) * 4.5;
                double tw = 0.5 + 0.5 * Math.sin(t * 2 + i * 5.3);
                brillo(g, rgb, x, y, tam * 4, tam * 4, 0.45 * tw * entra);
                g.setColor(con(0xFFFFFF, 0.65 * tw * entra));
                g.fill(new Ellipse2D.Double(x - tam / 2, y - tam / 2, tam, tam));
            }
        });
    }

    /** Haces de luz que bajan en diagonal desde arriba a la izquierda. */
    public static Efecto rayos(int rgb) {
        return new Efecto(false, (g, t) -> {
            double entra = suave(t / 2.5);
            for (int i = 0; i < 6; i++) {
                double x0 = -200 + i * 360 + Math.sin(t * 0.25 + i * 2) * 50;
                double ancho = 90 + rnd(i * 4.1) * 120;
                double a = (0.05 + 0.05 * (0.5 + 0.5 * Math.sin(t * 0.6 + i * 1.9))) * entra;
                Path2D.Double haz = new Path2D.Double();
                haz.moveTo(x0, -40);
                haz.lineTo(x0 + ancho, -40);
                haz.lineTo(x0 + ancho + 760, H + 40);
                haz.lineTo(x0 + 560, H + 40);
                haz.closePath();
                g.setPaint(new GradientPaint(0, 0, con(rgb, a * 1.8), 0, H, con(rgb, 0)));
                g.fill(haz);
            }
        });
    }

    /** Resplandor pulsante (pantalla de celular, farol, etc.). */
    public static Efecto resplandor(int x, int y, int radio, int rgb, double fase) {
        return new Efecto(true, (g, t) -> {
            double pulso = 0.5 + 0.5 * Math.sin(t * 2.2 + fase);
            double entra = suave((t - 0.3) / 1.2);
            double r = radio * (1 + 0.07 * pulso);
            brillo(g, rgb, x, y, r, r, (0.40 + 0.30 * pulso) * entra);
            brillo(g, 0xFFFFFF, x, y, radio * 0.28, radio * 0.28, 0.45 * entra);
            // la pantallita
            g.setColor(con(0xEFFFFF, 0.9 * entra));
            g.fill(new java.awt.geom.RoundRectangle2D.Double(x - 9, y - 15, 18, 30, 5, 5));
        });
    }

    // ---------- MARIPOSA ----------

    private static Point2D.Double rutaMariposa(double t, double x0, double y0, double x1, double y1) {
        double ini = 0.9, dur = 8.0;
        double u = clamp01((t - ini) / dur);
        double e = suave(u);
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len;
        double env = Math.sin(Math.PI * u);
        double zig = Math.sin(u * Math.PI * 3.4) * 150 * env;
        double x = x0 + dx * e + nx * zig;
        double y = y0 + dy * e + ny * zig + Math.sin(t * 4.2) * 8;
        // al llegar, revolotea en un lazo pequeno
        double lazo = suave((u - 0.75) / 0.25);
        x += Math.cos(t * 1.3) * 55 * lazo;
        y += Math.sin(t * 2.6) * 30 * lazo;
        return new Point2D.Double(x, y);
    }

    /** Mariposa amarilla que sale de (x0,y0), vuela serpenteando hasta (x1,y1) y revolotea. */
    public static Efecto mariposa(int x0, int y0, int x1, int y1) {
        return new Efecto(true, (g, t) -> {
            double ini = 0.9;
            // destello previo en el letrero: aviso de que algo va a salir
            if (t < 2.5) {
                brillo(g, 0xFFE070, x0, y0, 100, 100, 0.55 * suave(t / 0.4) * (1 - suave((t - 1.2) / 1.2)));
            }
            // estela de chispas doradas (muestreo del pasado: la ruta no tiene estado)
            for (int k = 28; k >= 1; k--) {
                double tk = t - k * 0.055;
                if (tk < ini - 0.2) continue;
                Point2D.Double p = rutaMariposa(tk, x0, y0, x1, y1);
                double f = 1 - k / 28.0;
                double ch = 3 + 6 * f;
                double jx = Math.sin(k * 7.3 + t) * 5 * (1 - f);
                double jy = Math.cos(k * 5.1 + t) * 5 * (1 - f) + k * 0.8;
                brillo(g, 0xFFD34A, p.x + jx, p.y + jy, ch * 3, ch * 3, 0.55 * f * f);
                g.setColor(con(0xFFF4C0, 0.8 * f * f));
                g.fill(new Ellipse2D.Double(p.x + jx - ch * 0.2, p.y + jy - ch * 0.2, ch * 0.4, ch * 0.4));
            }
            Point2D.Double p = rutaMariposa(t, x0, y0, x1, y1);
            Point2D.Double q = rutaMariposa(t + 0.03, x0, y0, x1, y1);
            double rumbo = Math.atan2(q.y - p.y, q.x - p.x);
            double nace = suave((t - ini + 0.4) / 0.9);   // crece al salir del letrero
            double aleteo = 0.18 + 0.82 * Math.abs(Math.sin(t * 13));
            brillo(g, 0xFFD34A, p.x, p.y, 120 * nace, 120 * nace, 0.35);
            dibujarMariposa(g, p.x, p.y, rumbo, aleteo, 0.9 * nace);
        });
    }

    /** Dibuja una mariposa vista desde arriba. flap: 0.18 (alas cerradas) a 1 (abiertas). */
    static void dibujarMariposa(Graphics2D g0, double cx, double cy, double rumbo, double flap, double esc) {
        if (esc <= 0.01) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.rotate(rumbo + Math.PI / 2);
        g.scale(esc, esc);
        for (int lado = 0; lado < 2; lado++) {
            Graphics2D w = (Graphics2D) g.create();
            if (lado == 1) w.scale(-1, 1);
            double f = flap;
            Path2D.Double alta = new Path2D.Double();
            alta.moveTo(2, -4);
            alta.curveTo(26 * f, -50, 70 * f, -38, 58 * f, -8);
            alta.curveTo(50 * f, 6, 16 * f, 4, 2, 0);
            alta.closePath();
            Path2D.Double baja = new Path2D.Double();
            baja.moveTo(2, 0);
            baja.curveTo(44 * f, 2, 56 * f, 34, 32 * f, 44);
            baja.curveTo(14 * f, 48, 4 * f, 24, 2, 8);
            baja.closePath();
            GradientPaint gp = new GradientPaint(0, 0, new Color(255, 232, 110), (float) (60 * f), -20,
                    new Color(255, 150, 20));
            w.setPaint(gp);
            w.fill(alta);
            w.fill(baja);
            w.setColor(new Color(120, 70, 10, 190));
            w.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            w.draw(alta);
            w.draw(baja);
            w.setStroke(new BasicStroke(1.2f));
            w.setColor(new Color(150, 90, 15, 120));
            w.draw(new java.awt.geom.Line2D.Double(3, -2, 52 * f, -24));
            w.draw(new java.awt.geom.Line2D.Double(3, 2, 38 * f, 26));
            w.setColor(new Color(255, 250, 220, 200));
            w.fill(new Ellipse2D.Double(44 * f, -26, 7 * f, 7));
            w.fill(new Ellipse2D.Double(28 * f, 28, 6 * f, 6));
            w.dispose();
        }
        g.setColor(new Color(70, 40, 10));
        g.fill(new Ellipse2D.Double(-3.2, -14, 6.4, 38));
        g.fill(new Ellipse2D.Double(-4, -17, 8, 8));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.QuadCurve2D.Double(-1.5, -16, -8, -30, -16, -32));
        g.draw(new java.awt.geom.QuadCurve2D.Double(1.5, -16, 8, -30, 16, -32));
        g.dispose();
    }

    // ---------- RUMOR ----------

    private static final double RUMOR_INI = 1.0, RUMOR_SALTO = 1.05, RUMOR_ALTO = 150;

    private static Point2D.Double posRumor(double t, int[] p) {
        int saltos = p.length / 2 - 1;
        double s = (t - RUMOR_INI) / RUMOR_SALTO;
        if (s <= 0) return new Point2D.Double(p[0], p[1]);
        if (s >= saltos) return new Point2D.Double(p[saltos * 2], p[saltos * 2 + 1]);
        int h = (int) s;
        double u = suave(s - h);
        double x = mezcla(p[h * 2], p[h * 2 + 2], u);
        double y = mezcla(p[h * 2 + 1], p[h * 2 + 3], u) - RUMOR_ALTO * 4 * u * (1 - u);
        return new Point2D.Double(x, y);
    }

    /**
     * El audio-rumor: una linea de luz que salta de casa en casa (pares x,y) y al final se rompe
     * en pedazos. Necesita al menos 2 puntos.
     */
    public static Efecto rumor(int... pts) {
        return new Efecto(true, (g, t) -> {
            if (pts.length < 4) return;
            int saltos = pts.length / 2 - 1;
            double tFin = RUMOR_INI + saltos * RUMOR_SALTO;
            // casas: aros que laten + aro al aterrizar
            for (int i = 0; i <= saltos; i++) {
                double llegada = RUMOR_INI + i * RUMOR_SALTO;
                double dt = t - llegada;
                double visible = i == 0 ? suave((t - 0.2) / 0.6) : suave(dt / 0.2);
                if (visible <= 0) continue;
                brillo(g, 0xBFEFFF, pts[i * 2], pts[i * 2 + 1], 54, 54, 0.4 * visible);
                if (dt > 0 && dt < 1.2) {
                    double k = dt / 1.2;
                    g.setStroke(new BasicStroke((float) (4 * (1 - k) + 1)));
                    g.setColor(con(0xDDF6FF, 0.8 * (1 - k)));
                    double r = 20 + 90 * k;
                    g.draw(new Ellipse2D.Double(pts[i * 2] - r, pts[i * 2 + 1] - r, r * 2, r * 2));
                }
            }
            // la estela (hasta que se rompe)
            double hasta = Math.min(t, tFin);
            if (t < tFin + 0.15) {
                Path2D.Double estela = new Path2D.Double();
                boolean primero = true;
                int n = 26;
                for (int k = n; k >= 0; k--) {
                    double tk = hasta - k * 0.025;
                    if (tk < RUMOR_INI - 0.1) continue;
                    Point2D.Double p = posRumor(tk, pts);
                    if (primero) {
                        estela.moveTo(p.x, p.y);
                        primero = false;
                    } else estela.lineTo(p.x, p.y);
                }
                if (!primero) {
                    g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(con(0x7FD8FF, 0.18));
                    g.draw(estela);
                    g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(con(0xBFEFFF, 0.55));
                    g.draw(estela);
                    g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(con(0xFFFFFF, 0.95));
                    g.draw(estela);
                }
                Point2D.Double c = posRumor(hasta, pts);
                brillo(g, 0xA8E8FF, c.x, c.y, 60, 60, 0.9);
                brillo(g, 0xFFFFFF, c.x, c.y, 22, 22, 1.0);
            }
            // se rompe en pedazos
            double dr = t - tFin;
            if (dr > 0) {
                double lx = pts[saltos * 2], ly = pts[saltos * 2 + 1];
                brillo(g, 0xFFFFFF, lx, ly, 170 * (1 - suave(dr / 0.35)), 170 * (1 - suave(dr / 0.35)), 0.8);
                for (int i = 0; i < 22; i++) {
                    double ang = rnd(i * 3.3) * Math.PI * 2;
                    double vel = 140 + rnd(i * 6.1) * 340;
                    double px = lx + Math.cos(ang) * vel * dr;
                    double py = ly + Math.sin(ang) * vel * dr + 260 * dr * dr;
                    double vida = clamp01(1 - dr / (1.5 + rnd(i * 9.1) * 0.8));
                    if (vida <= 0) continue;
                    Graphics2D q = (Graphics2D) g.create();
                    q.translate(px, py);
                    q.rotate(dr * (4 + rnd(i * 2.5) * 8) + i);
                    double lado = 7 + rnd(i * 1.9) * 14;
                    Path2D.Double tri = new Path2D.Double();
                    tri.moveTo(-lado, -lado * 0.4);
                    tri.lineTo(lado, 0);
                    tri.lineTo(-lado * 0.2, lado * 0.6);
                    tri.closePath();
                    q.setColor(con(0xD8F6FF, 0.9 * vida));
                    q.fill(tri);
                    q.setColor(con(0xFFFFFF, vida));
                    q.setStroke(new BasicStroke(1.5f));
                    q.draw(tri);
                    q.dispose();
                }
            }
        });
    }

    // ---------- SENAL ----------

    /**
     * Cuadro 08: los cuatro celulares se encienden a la vez, la luz se junta y aparece UNA raya de
     * senal. Al final hace el "corte a negro". Dura unos 6 s (usar .duracion(6.2)).
     * (cx,cy) = donde sale el icono; telefonos = pares x,y de los 4 celulares.
     */
    public static Efecto senal(int cx, int cy, int... telefonos) {
        return new Efecto(false, (g, t) -> {
            int n = telefonos.length / 2;
            double encender = suave((t - 0.4) / 0.35);       // los cuatro a la vez
            double converge = suave((t - 1.5) / 1.4);
            double destello = Math.max(0, 1 - Math.abs(t - 2.9) / 0.35);
            for (int i = 0; i < n; i++) {
                double x = telefonos[i * 2], y = telefonos[i * 2 + 1];
                double px = mezcla(x, cx, converge), py = mezcla(y, cy, converge);
                brillo(g, 0x9FE6FF, x, y, 150, 150, 0.7 * encender * (1 - converge));
                if (converge > 0 && converge < 1) {
                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setPaint(new GradientPaint((float) x, (float) y, con(0xBFEFFF, 0.8 * (1 - converge)),
                            (float) px, (float) py, con(0xFFFFFF, 0.9)));
                    g.draw(new java.awt.geom.Line2D.Double(x, y, px, py));
                    brillo(g, 0xFFFFFF, px, py, 34, 34, 0.9);
                }
            }
            // icono: cuatro barras, solo la primera se enciende
            double pop = suave((t - 2.7) / 0.5);
            if (pop > 0) {
                double s = 0.6 + 0.4 * pop + 0.06 * Math.sin(t * 6) * pop;
                Graphics2D q = (Graphics2D) g.create();
                q.translate(cx, cy);
                q.scale(s, s);
                brillo(q, 0xFFE9A0, 0, 0, 260, 200, 0.45 * pop + 0.5 * destello);
                for (int b = 0; b < 4; b++) {
                    double bx = -110 + b * 58, alto = 36 + b * 38;
                    Shape barra = new java.awt.geom.RoundRectangle2D.Double(bx, 76 - alto, 38, alto, 12, 12);
                    if (b == 0) {
                        q.setColor(con(0xFFF1BF, pop));
                        q.fill(barra);
                    } else {
                        q.setColor(con(0xFFFFFF, 0.14 * pop));
                        q.fill(barra);
                        q.setStroke(new BasicStroke(3f));
                        q.setColor(con(0xFFFFFF, 0.35 * pop));
                        q.draw(barra);
                    }
                }
                q.dispose();
            }
            // corte a negro
            double negro = suave((t - 4.9) / 1.1);
            if (negro > 0) {
                g.setColor(con(0x000000, negro));
                g.fillRect(0, 0, W, H);
            }
        });
    }

    // ===================== CAPAS GLOBALES DE "PELICULA" =====================

    private static BufferedImage vineta;
    private static BufferedImage[] granos;
    private static TexturePaint[] granoPintura;

    private static synchronized void preparar() {
        if (vineta != null) return;
        vineta = new BufferedImage(W / 2, H / 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = vineta.createGraphics();
        g.setPaint(new RadialGradientPaint(W / 4f, H / 4f, W / 3.0f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(4, 6, 16, 55), new Color(2, 3, 10, 190)}));
        g.fillRect(0, 0, W / 2, H / 2);
        g.dispose();
        granos = new BufferedImage[4];
        granoPintura = new TexturePaint[4];
        java.util.Random r = new java.util.Random(7);
        for (int k = 0; k < 4; k++) {
            BufferedImage b = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    int v = r.nextInt(256);
                    int a = r.nextInt(100) < 22 ? 5 + r.nextInt(14) : 0;
                    b.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
                }
            }
            granos[k] = b;
            granoPintura[k] = new TexturePaint(b, new Rectangle2D.Double(0, 0, 256, 256));
        }
    }

    /** Oscurece los bordes; da el aire de lente de cine. */
    public static void vineta(Graphics2D g) {
        preparar();
        g.drawImage(vineta, 0, 0, W, H, null);
    }

    /** Grano de pelicula y un leve parpadeo de proyector. */
    public static void grano(Graphics2D g, double t) {
        preparar();
        int paso = (int) (t * 18);
        Graphics2D q = (Graphics2D) g.create();
        q.translate((int) (rnd(paso * 1.3) * 256), (int) (rnd(paso * 2.9) * 256));
        q.setPaint(granoPintura[paso & 3]);
        q.fill(new Rectangle2D.Double(-256, -256, W + 512, H + 512));
        q.dispose();
        double parpadeo = 0.018 * (0.5 + 0.5 * Math.sin(t * 31) * Math.sin(t * 7.3));
        g.setColor(con(0x000000, parpadeo));
        g.fillRect(0, 0, W, H);
    }

    // ===================== TITULO ANIMADO =====================

    /** Titulo letra por letra con destello dorado, linea que se abre y subtitulo. */
    public static void titulo(Graphics2D g0, String grande, String pequeno, double t, Font fGrande, Font fPequena) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double cx = W / 2.0, cy = H / 2.0;
        // halo dorado detras
        double halo = suave((t - 0.2) / 2.5);
        brillo(g, 0xFFB93A, cx, cy - 20, 900, 260, 0.20 * halo);
        // motas doradas subiendo
        for (int i = 0; i < 46; i++) {
            double vel = 16 + rnd(i * 2.1) * 30;
            double x = (rnd(i * 3.7) * W + Math.sin(t * 0.6 + i) * 30) % W;
            double y = H + 20 - ((rnd(i * 5.3) * H + t * vel) % (H + 40));
            double tw = 0.5 + 0.5 * Math.sin(t * 2.4 + i * 4.1);
            double tam = 2 + rnd(i * 7.7) * 4;
            brillo(g, 0xFFD36A, x, y, tam * 4, tam * 4, 0.40 * tw * halo);
            g.setColor(con(0xFFF1C0, 0.7 * tw * halo));
            g.fill(new Ellipse2D.Double(x - tam / 2, y - tam / 2, tam, tam));
        }

        // letras
        g.setFont(fGrande);
        FontMetrics fm = g.getFontMetrics();
        int n = grande.length();
        double tracking = 6 + 10 * suave(t / 8.0);
        double[] ancho = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            ancho[i] = fm.charWidth(grande.charAt(i)) + tracking;
            total += ancho[i];
        }
        total -= tracking;
        double x = cx - total / 2;
        double base = cy + fm.getAscent() * 0.3;
        double barrido = ((t - 3.0) / 3.2) % 1.0;            // brillo que cruza el titulo
        double bx = mezcla(cx - total / 2 - 200, cx + total / 2 + 200, barrido);
        for (int i = 0; i < n; i++) {
            double nace = suave((t - 0.5 - i * 0.07) / 0.7);
            if (nace > 0 && grande.charAt(i) != ' ') {
                GlyphVector gv = fFontGV(fGrande, g, String.valueOf(grande.charAt(i)));
                AffineTransform at = new AffineTransform();
                double esc = mezcla(1.35, 1.0, nace);
                at.translate(x + ancho[i] / 2 - tracking / 2, base + (1 - nace) * 36);
                at.scale(esc, esc);
                at.translate(-fm.charWidth(grande.charAt(i)) / 2.0, 0);
                Shape forma = at.createTransformedShape(gv.getOutline());
                java.awt.Composite old = g.getComposite();
                alfa(g, nace);
                g.setColor(con(0x3A1E00, 0.55));
                g.translate(3, 5);
                g.fill(forma);
                g.translate(-3, -5);
                g.setPaint(new LinearGradientPaint(0, (float) (base - fm.getAscent()), 0, (float) base,
                        new float[]{0f, 1f}, new Color[]{new Color(255, 232, 140), new Color(255, 183, 52)}));
                g.fill(forma);
                double luz = Math.max(0, 1 - Math.abs((x + ancho[i] / 2) - bx) / 140.0);
                if (luz > 0 && barrido > 0) {
                    g.setColor(con(0xFFFFFF, 0.75 * luz));
                    g.fill(forma);
                }
                g.setComposite(old);
            }
            x += ancho[i];
        }

        // linea que se abre con rombo
        double linea = suave((t - 2.3) / 1.0);
        if (linea > 0) {
            double mitad = 360 * linea;
            double ly = cy + fm.getDescent() + 46;
            g.setPaint(new LinearGradientPaint((float) (cx - mitad), 0, (float) (cx + mitad), 0,
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{con(0xFFD36A, 0), con(0xFFD36A, 0.95), con(0xFFD36A, 0)}));
            g.fill(new Rectangle2D.Double(cx - mitad, ly, mitad * 2, 3));
            Path2D.Double rombo = new Path2D.Double();
            rombo.moveTo(cx, ly - 9 + 1.5);
            rombo.lineTo(cx + 9, ly + 1.5);
            rombo.lineTo(cx, ly + 12 + 1.5);
            rombo.lineTo(cx - 9, ly + 1.5);
            rombo.closePath();
            g.setColor(con(0xFFE9A8, linea));
            g.fill(rombo);
        }

        // subtitulo
        double sub = suave((t - 3.0) / 1.2);
        if (pequeno != null && sub > 0) {
            g.setFont(fPequena);
            FontMetrics f2 = g.getFontMetrics();
            double tr = 3 + 3 * sub;
            double tw = 0;
            for (int i = 0; i < pequeno.length(); i++) tw += f2.charWidth(pequeno.charAt(i)) + tr;
            double sx = cx - (tw - tr) / 2;
            double sy = cy + fm.getDescent() + 46 + 64 - (1 - sub) * 10;
            g.setColor(con(0xFFFFFF, 0.88 * sub));
            for (int i = 0; i < pequeno.length(); i++) {
                String c = String.valueOf(pequeno.charAt(i));
                g.drawString(c, (float) sx, (float) sy);
                sx += f2.charWidth(pequeno.charAt(i)) + tr;
            }
        }

        // una mariposita cruza por debajo del titulo
        double v = (t - 5.0) / 9.0;
        if (v > 0 && v < 1) {
            double mx = mezcla(-80, W + 80, v);
            double my = 820 + Math.sin(v * 9) * 70 - v * 180;
            double mx2 = mezcla(-80, W + 80, v + 0.01);
            double my2 = 820 + Math.sin((v + 0.01) * 9) * 70 - (v + 0.01) * 180;
            double rumbo = Math.atan2(my2 - my, mx2 - mx);
            brillo(g, 0xFFD34A, mx, my, 90, 90, 0.35 * Math.sin(v * Math.PI));
            dibujarMariposa(g, mx, my, rumbo, 0.18 + 0.82 * Math.abs(Math.sin(t * 12)), 0.55);
        }
        g.dispose();
    }

    private static GlyphVector fFontGV(Font f, Graphics2D g, String s) {
        return f.createGlyphVector(g.getFontRenderContext(), s);
    }
}
