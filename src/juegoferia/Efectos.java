package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Biblioteca de efectos para las cinematicas. Todos son procedurales (sin PNG):
 * olas, viento, niebla, particulas/luciernagas, mariposa, rumor, destello y focos de luz.
 * Se usan con Cinematica.efecto(Efectos.xxx(...)).
 */
public final class Efectos {

    private static final int W = PanelDiseno.W;
    private static final int H = PanelDiseno.H;

    private Efectos() {
    }

    // ===================== UTILIDADES =====================

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    static double suave(double x) {
        x = clamp(x);
        return x * x * (3 - 2 * x);
    }

    private static int alfa(double a) {
        return (int) Math.max(0, Math.min(255, a));
    }

    private static double mod(double a, double b) {
        double r = a % b;
        return r < 0 ? r + b : r;
    }

    private static void calidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    /** Mancha de luz suave: centro opaco, borde transparente. */
    static BufferedImage brillo(Color c, int tam) {
        BufferedImage img = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        float r = tam / 2f;
        g.setPaint(new RadialGradientPaint(r, r, r, new float[]{0f, 0.35f, 1f},
                new Color[]{
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 255),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 90),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        g.fillRect(0, 0, tam, tam);
        g.dispose();
        return img;
    }

    /** Dibuja una mancha de luz centrada en (cx, cy) con opacidad a. */
    static void mancha(Graphics2D g, BufferedImage img, double cx, double cy, double w, double h, double a) {
        if (a <= 0.004) {
            return;
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, a)));
        g.drawImage(img, (int) (cx - w / 2), (int) (cy - h / 2), (int) w, (int) h, null);
        g.setComposite(old);
    }

    // ===================== OLAS =====================

    /**
     * Olas de espuma que avanzan desde el mar (yMar) hasta la orilla (yOrilla) con
     * destellos de luz sobre el agua. Si el mar queda arriba, yMar &lt; yOrilla; si queda abajo, al reves.
     */
    public static Efecto1 olas(final int yMar, final int yOrilla) {
        final int BRILLOS = 46;
        final Random r = new Random(11);
        final double[] bx = new double[BRILLOS], by = new double[BRILLOS],
                bf = new double[BRILLOS], bl = new double[BRILLOS];
        for (int i = 0; i < BRILLOS; i++) {
            bx[i] = r.nextDouble() * W;
            by[i] = Math.min(yMar, yOrilla) + r.nextDouble() * Math.max(1, Math.abs(yOrilla - yMar));
            bf[i] = r.nextDouble() * 6.28;
            bl[i] = 6 + r.nextDouble() * 20;
        }
        return (g0, t) -> {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double entra = suave(t / 2.0);

            // destellos que titilan sobre el agua
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < BRILLOS; i++) {
                double tw = Math.sin(t * 1.8 + bf[i]);
                tw = tw > 0 ? Math.pow(tw, 6) : 0;
                if (tw < 0.02) {
                    continue;
                }
                g.setColor(new Color(235, 248, 255, alfa(150 * tw * entra)));
                g.draw(new Line2D.Double(bx[i] - bl[i] / 2, by[i], bx[i] + bl[i] / 2, by[i]));
            }

            // frentes de espuma
            final int N = 5;
            final double periodo = 8.0;
            for (int k = 0; k < N; k++) {
                double u = mod(t / periodo + k / (double) N, 1.0);
                double avance = 1 - (1 - u) * (1 - u);
                double y0 = yMar + (yOrilla - yMar) * avance;
                double vis = suave(u / 0.25) * (1 - suave((u - 0.6) / 0.4)) * entra;
                if (vis < 0.01) {
                    continue;
                }
                double amp = 8 + 14 * (1 - u);
                Path2D.Double path = new Path2D.Double();
                for (int x = -20; x <= W + 20; x += 24) {
                    double y = y0 + amp * Math.sin(x * 0.006 + t * 0.5 + k * 2.1)
                            + amp * 0.45 * Math.sin(x * 0.017 - t * 0.8 + k);
                    if (x == -20) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                }
                double grosor = 2 + 5 * u;
                g.setStroke(new BasicStroke((float) (grosor * 5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(220, 240, 255, alfa(28 * vis)));
                g.draw(path);
                g.setStroke(new BasicStroke((float) (grosor * 2.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(230, 245, 255, alfa(70 * vis)));
                g.draw(path);
                g.setStroke(new BasicStroke((float) grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(250, 253, 255, alfa(150 * vis)));
                g.draw(path);
            }
            g.dispose();
        };
    }

    // ===================== VIENTO =====================

    /** Rachas de viento (lineas de luz que cruzan la pantalla) y motas llevadas por el aire. */
    public static Efecto1 viento(final int rachas) {
        final Random r = new Random(5);
        final double[] y = new double[rachas], vel = new double[rachas], len = new double[rachas],
                fase = new double[rachas], amp = new double[rachas], al = new double[rachas];
        for (int i = 0; i < rachas; i++) {
            y[i] = 80 + r.nextDouble() * (H - 160);
            vel[i] = 260 + r.nextDouble() * 380;
            len[i] = 220 + r.nextDouble() * 420;
            fase[i] = r.nextDouble();
            amp[i] = 8 + r.nextDouble() * 26;
            al[i] = 30 + r.nextDouble() * 50;
        }
        final int MOTAS = 30;
        final double[] mx = new double[MOTAS], my = new double[MOTAS], mv = new double[MOTAS];
        for (int i = 0; i < MOTAS; i++) {
            mx[i] = r.nextDouble() * W;
            my[i] = r.nextDouble() * H;
            mv[i] = 500 + r.nextDouble() * 450;
        }
        return (g0, t) -> {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double entra = suave(t / 1.5);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < rachas; i++) {
                double ciclo = W + 2 * len[i];
                double x0 = mod(t * vel[i] + fase[i] * ciclo, ciclo) - len[i];
                Path2D.Double p = new Path2D.Double();
                int n = 14;
                for (int j = 0; j <= n; j++) {
                    double x = x0 + len[i] * j / n;
                    double yy = y[i] + amp[i] * Math.sin(j * 0.55 + t * 1.4 + i);
                    if (j == 0) {
                        p.moveTo(x, yy);
                    } else {
                        p.lineTo(x, yy);
                    }
                }
                Color c0 = new Color(255, 255, 255, 0);
                Color c1 = new Color(255, 255, 255, alfa(al[i] * entra));
                g.setPaint(new LinearGradientPaint(new Point2D.Double(x0, 0), new Point2D.Double(x0 + len[i], 0),
                        new float[]{0f, 0.5f, 1f}, new Color[]{c0, c1, c0}));
                g.draw(p);
            }
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < MOTAS; i++) {
                double x = mod(mx[i] + t * mv[i], W + 100) - 50;
                double yy = my[i] + 18 * Math.sin(t * 1.1 + i);
                g.setColor(new Color(255, 250, 235, alfa(110 * entra)));
                g.draw(new Line2D.Double(x - 14, yy, x, yy));
            }
            g.dispose();
        };
    }

    // ===================== NIEBLA =====================

    /** Bancos de niebla que derivan lentamente. intensidad: 0 a 1 (1 = muy cerrada). */
    public static Efecto1 niebla(final double intensidad) {
        final BufferedImage nube = brillo(new Color(232, 238, 244), 256);
        final Random r = new Random(3);
        final int N = 9;
        final double[] x0 = new double[N], yy = new double[N], tam = new double[N],
                vel = new double[N], fase = new double[N];
        for (int i = 0; i < N; i++) {
            x0[i] = r.nextDouble() * W;
            yy[i] = H * (0.35 + r.nextDouble() * 0.65);
            tam[i] = 800 + r.nextDouble() * 500;
            vel[i] = 10 + r.nextDouble() * 18;
            fase[i] = r.nextDouble() * 6.28;
        }
        return (g0, t) -> {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double entra = suave(t / 2.5);
            for (int i = 0; i < N; i++) {
                double cx = mod(x0[i] + t * vel[i], W + tam[i]) - tam[i] / 2;
                double pulso = 0.75 + 0.25 * Math.sin(t * 0.4 + fase[i]);
                mancha(g, nube, cx, yy[i], tam[i], tam[i] * 0.5, intensidad * 0.30 * pulso * entra);
            }
            g.dispose();
        };
    }

    // ===================== PARTICULAS / LUCIERNAGAS =====================

    /**
     * Motas de luz flotando (polvo, luciernagas, chispas).
     * sube: 0 = quedan casi quietas, 1 = suben con calma.
     */
    public static Efecto1 particulas(final Color color, final int n, final double sube) {
        final BufferedImage luz = brillo(color, 64);
        final Color nucleo = new Color(Math.min(255, color.getRed() + 70), Math.min(255, color.getGreen() + 70),
                Math.min(255, color.getBlue() + 70));
        final Random r = new Random(17);
        final double[] x0 = new double[n], y0 = new double[n], rad = new double[n],
                ph = new double[n], vx = new double[n], vy = new double[n];
        for (int i = 0; i < n; i++) {
            x0[i] = r.nextDouble() * W;
            y0[i] = r.nextDouble() * H;
            rad[i] = 1.2 + r.nextDouble() * 2.6;
            ph[i] = r.nextDouble() * 6.28;
            vx[i] = 4 + r.nextDouble() * 12;
            vy[i] = 6 + r.nextDouble() * 18;
        }
        return (g0, t) -> {
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double entra = suave(t / 2.0);
            for (int i = 0; i < n; i++) {
                double x = mod(x0[i] + vx[i] * t + 22 * Math.sin(t * 0.6 + ph[i]), W);
                double y = mod(y0[i] - sube * vy[i] * t + 14 * Math.cos(t * 0.5 + ph[i]), H);
                double tw = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * 1.7 + ph[i] * 3));
                mancha(g, luz, x, y, rad[i] * 9, rad[i] * 9, 0.55 * tw * entra);
                g.setColor(new Color(nucleo.getRed(), nucleo.getGreen(), nucleo.getBlue(), alfa(210 * tw * entra)));
                g.fill(new Ellipse2D.Double(x - rad[i] * 0.45, y - rad[i] * 0.45, rad[i] * 0.9, rad[i] * 0.9));
            }
            g.dispose();
        };
    }

    // ===================== MARIPOSA =====================

    private static final Path2D ALA_ARRIBA = new Path2D.Double();
    private static final Path2D ALA_ABAJO = new Path2D.Double();

    static {
        ALA_ARRIBA.moveTo(3, -4);
        ALA_ARRIBA.curveTo(16, -62, 74, -84, 78, -34);
        ALA_ARRIBA.curveTo(80, -12, 42, -2, 3, 4);
        ALA_ARRIBA.closePath();
        ALA_ABAJO.moveTo(3, 2);
        ALA_ABAJO.curveTo(44, 0, 68, 16, 54, 44);
        ALA_ABAJO.curveTo(44, 64, 14, 54, 3, 18);
        ALA_ABAJO.closePath();
    }

    private static double[] rutaMariposa(double u, double x0, double y0, double x1, double y1) {
        double x = x0 + (x1 - x0) * u + 70 * Math.sin(u * 13) * (0.3 + 0.7 * u);
        double y = y0 + (y1 - y0) * u - 120 * Math.sin(Math.PI * u) + 40 * Math.sin(u * 19);
        return new double[]{x, y};
    }

    /**
     * Mariposa amarilla que nace en (x0, y0), vuela aleteando en una ruta sinuosa hasta (x1, y1)
     * y se pierde a lo lejos, dejando una estela de chispas.
     *
     * @param x0
     * @param y0
     * @param x1
     * @param retardo  segundos desde el inicio del cuadro hasta que nace
     * @param y1
     * @param duracion segundos que dura el vuelo
     * @param escala   1.0 = tamano normal (unos 160 px de ala a ala)
     */
    public static Efecto1 mariposa(final int x0, final int y0, final int x1, final int y1,
            final double retardo, final double duracion, final double escala) {
        final BufferedImage luz = brillo(new Color(255, 222, 80), 128);
        return (g0, t) -> {
            double u = (t - retardo) / duracion;
            if (u < 0 || u > 1) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);

            double vis = suave(u / 0.05) * (1 - suave((u - 0.86) / 0.14));
            double sc = escala * (0.35 + 0.65 * suave(u / 0.25)) * (1 - 0.35 * suave((u - 0.7) / 0.3));

            // anillo de luz al nacer
            if (u < 0.1) {
                double q = u / 0.1;
                g.setStroke(new BasicStroke(3f));
                g.setColor(new Color(255, 235, 140, alfa(140 * (1 - q))));
                double rr = 20 + 110 * q;
                g.draw(new Ellipse2D.Double(x0 - rr, y0 - rr, rr * 2, rr * 2));
            }

            // estela de chispas
            for (int k = 1; k <= 16; k++) {
                double uk = u - k * 0.006;
                if (uk < 0) {
                    break;
                }
                double[] pk = rutaMariposa(uk, x0, y0, x1, y1);
                double f = 1 - k / 17.0;
                double jx = 7 * Math.sin(k * 2.3 + t * 3);
                double jy = 7 * Math.cos(k * 1.7 + t * 3);
                mancha(g, luz, pk[0] + jx, pk[1] + jy, 34 * f * escala + 8, 34 * f * escala + 8, 0.7 * f * vis);
            }

            double[] p = rutaMariposa(u, x0, y0, x1, y1);
            double flap = 0.14 + 0.86 * Math.abs(Math.cos(t * Math.PI * 2.6));
            double tilt = 0.3 * Math.sin(u * 14 + 0.8) + 0.2;

            mancha(g, luz, p[0], p[1], 260 * sc, 260 * sc, 0.55 * vis);

            Graphics2D b = (Graphics2D) g.create();
            b.translate(p[0], p[1]);
            b.rotate(tilt);
            b.scale(sc, sc);
            b.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp(vis)));
            dibujarMariposa(b, flap);
            b.dispose();
            g.dispose();
        };
    }

    private static void dibujarMariposa(Graphics2D g, double flap) {
        for (int lado = 1; lado >= -1; lado -= 2) {
            Graphics2D w = (Graphics2D) g.create();
            w.scale(lado * flap, 1);
            w.setPaint(new GradientPaint(0, 0, new Color(255, 240, 130, 245),
                    80, -40, new Color(255, 172, 24, 245)));
            w.fill(ALA_ARRIBA);
            w.fill(ALA_ABAJO);
            w.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            w.setColor(new Color(120, 70, 10, 230));
            w.draw(ALA_ARRIBA);
            w.draw(ALA_ABAJO);
            w.setStroke(new BasicStroke(1.4f));
            w.setColor(new Color(140, 80, 12, 110));
            w.draw(new Line2D.Double(4, -1, 68, -42));
            w.draw(new Line2D.Double(4, 0, 62, -14));
            w.draw(new Line2D.Double(4, 6, 46, 36));
            w.setColor(new Color(110, 60, 10, 200));
            w.fill(new Ellipse2D.Double(58, -62, 12, 12));
            w.setColor(new Color(255, 120, 20, 220));
            w.fill(new Ellipse2D.Double(32, 30, 9, 9));
            w.dispose();
        }
        g.setColor(new Color(70, 40, 10));
        g.fill(new Ellipse2D.Double(-3.5, -30, 7, 60));
        g.fill(new Ellipse2D.Double(-5, -40, 10, 10));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double ant = new Path2D.Double();
        ant.moveTo(-2, -37);
        ant.quadTo(-8, -58, -20, -66);
        ant.moveTo(2, -37);
        ant.quadTo(8, -58, 20, -66);
        g.draw(ant);
        g.fill(new Ellipse2D.Double(-22.5, -68.5, 5, 5));
        g.fill(new Ellipse2D.Double(17.5, -68.5, 5, 5));
    }

    // ===================== RUMOR =====================

    private static double[] puntoRumor(double tau, double seg, int[] pts, int n, double duracion) {
        double tt = Math.max(0, Math.min(duracion, tau));
        int i = Math.min(n - 1, (int) (tt / seg));
        double v = Math.min(1, (tt - i * seg) / seg);
        double ax = pts[2 * i], ay = pts[2 * i + 1], bx = pts[2 * i + 2], by = pts[2 * i + 3];
        double dist = Math.hypot(bx - ax, by - ay);
        double x = ax + (bx - ax) * v;
        double y = ay + (by - ay) * v - dist * 0.28 * 4 * v * (1 - v);
        return new double[]{x, y};
    }

    /**
     * Una linea de luz que salta de punto en punto (casa en casa) y al final se rompe en pedazos.
     *
     * @param puntos pares x, y (minimo dos puntos)
     */
    public static Efecto1 rumor(final double retardo, final double duracion, final int... puntos) {
        final int n = puntos.length / 2 - 1;
        final BufferedImage luz = brillo(new Color(170, 235, 255), 96);
        final Random r = new Random(23);
        final int FRAG = 18;
        final double[] ang = new double[FRAG], vel = new double[FRAG], rot = new double[FRAG];
        for (int i = 0; i < FRAG; i++) {
            ang[i] = r.nextDouble() * 6.28;
            vel[i] = 120 + r.nextDouble() * 320;
            rot[i] = r.nextDouble() * 6.28;
        }
        return (g0, t) -> {
            double tau = t - retardo;
            if (tau < 0 || n < 1) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double seg = duracion / n;

            // pulsos en cada casa que toca
            g.setStroke(new BasicStroke(3f));
            for (int j = 0; j < n; j++) {
                double dt = tau - j * seg;
                if (dt > 0 && dt < 0.7) {
                    double q = dt / 0.7;
                    double rr = 12 + 80 * q;
                    g.setColor(new Color(190, 240, 255, alfa(170 * (1 - q))));
                    g.draw(new Ellipse2D.Double(puntos[2 * j] - rr, puntos[2 * j + 1] - rr, rr * 2, rr * 2));
                }
            }

            if (tau < duracion) {
                // estela
                double[] prev = puntoRumor(tau, seg, puntos, n, duracion);
                final int K = 36;
                for (int k = 1; k <= K; k++) {
                    double tk = tau - k * 0.03;
                    if (tk < 0) {
                        break;
                    }
                    double[] pk = puntoRumor(tk, seg, puntos, n, duracion);
                    double f = 1 - k / (double) K;
                    g.setStroke(new BasicStroke((float) (1 + 5 * f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(185, 238, 255, alfa(230 * f)));
                    g.draw(new Line2D.Double(prev[0], prev[1], pk[0], pk[1]));
                    prev = pk;
                }
                double[] h = puntoRumor(tau, seg, puntos, n, duracion);
                mancha(g, luz, h[0], h[1], 120, 120, 1.0);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(h[0] - 6, h[1] - 6, 12, 12));
            } else {
                // se rompe en pedazos
                double dtS = tau - duracion;
                double[] e = puntoRumor(duracion, seg, puntos, n, duracion);
                if (dtS < 1.6) {
                    double q = dtS / 1.6;
                    g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < FRAG; i++) {
                        double px = e[0] + Math.cos(ang[i]) * vel[i] * dtS;
                        double py = e[1] + Math.sin(ang[i]) * vel[i] * dtS + 260 * dtS * dtS;
                        double rr = rot[i] + dtS * 6;
                        mancha(g, luz, px, py, 40, 40, 0.8 * (1 - q));
                        g.setColor(new Color(215, 245, 255, alfa(255 * (1 - q))));
                        g.draw(new Line2D.Double(px, py, px + Math.cos(rr) * 14, py + Math.sin(rr) * 14));
                    }
                }
            }
            g.dispose();
        };
    }

    // ===================== DESTELLO Y FOCOS =====================

    /** Fogonazo blanco-azulado de toda la pantalla en el segundo t0 que se apaga rapido. */
    public static Efecto1 destello(final double t0) {
        return (g0, t) -> {
            double x = t - t0;
            if (x < 0) {
                return;
            }
            double a = Math.min(1, x / 0.07) * Math.exp(-x * 2.2);
            if (a < 0.01) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(new Color(215, 238, 255, alfa(235 * a)));
            g.fillRect(-300, -300, W + 600, H + 600);
            g.dispose();
        };
    }

    /**
     * Foco de luz que respira (por ejemplo la pantalla de un celular) en (x, y).
     *
     * @param t0 segundo en que se enciende (aparece suavemente)
     */
    public static Efecto1 foco(final int x, final int y, final Color color, final int radio,
            final double periodo, final double t0) {
        final BufferedImage luz = brillo(color, 192);
        return (g0, t) -> {
            double e = suave((t - t0) / 0.9);
            if (e <= 0) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            calidad(g);
            double pulso = 0.8 + 0.2 * Math.sin((t - t0) * 2 * Math.PI / periodo + x * 0.01);
            mancha(g, luz, x, y, radio * 2 * pulso, radio * 2 * pulso, 0.75 * e);
            g.dispose();
        };
    }
}
