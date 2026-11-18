package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * EMOCIONES (emotes): cada jugador puede mostrar como se siente en cualquier momento.
 *
 *   R          abre / cierra la ruedita de emociones encima de tu personaje
 *   1 2 3 4    Feliz, Triste, Enojado, Sorprendido (o clic en la burbuja)
 *
 * Sale una burbujita encima de la cabeza con la carita de esa emocion: tus dibujos
 * personajes/<rol>/feliz.png, triste.png, enojado.png y sorprendido.png (si alguno falta, se
 * usa la cara del personaje con un simbolito dibujado).
 *
 * En red viaja como cualquier tecla: los cuatro ven la emocion de todos al mismo tiempo, y cada
 * quien solo ve SU ruedita. Las burbujas se dibujan una sola vez y se guardan (cuesta casi nada).
 */
final class Emociones {

    static final String[] IDS = {"feliz", "triste", "enojado", "sorprendido"};
    static final String[] NOMBRES = {"Feliz", "Triste", "Enojado", "Sorprendido"};
    private static final double DURA = 3.2;

    private final int n = Rol.values().length;
    private final boolean[] selector = new boolean[n];
    private final double[] tSelector = new double[n];
    private final int[] actual = new int[n];
    private final double[] t = new double[n];

    Emociones() {
        java.util.Arrays.fill(actual, -1);
    }

    boolean abierto(Rol r) {
        return r != null && selector[r.ordinal()];
    }

    void alternar(Rol r) {
        selector[r.ordinal()] = !selector[r.ordinal()];
        tSelector[r.ordinal()] = 0;
    }

    void cerrar(Rol r) {
        if (r != null) {
            selector[r.ordinal()] = false;
        }
    }

    /** Pone la emocion i (0-3) a ese jugador. */
    void poner(Rol r, int i) {
        actual[r.ordinal()] = i;
        t[r.ordinal()] = 0;
        selector[r.ordinal()] = false;
        Ajustes.sfx("/sonidos/hover.wav", "/sonidos/tecla.wav");
    }

    /** Emocion que muestra ahora (null = ninguna). */
    String de(Rol r) {
        int i = actual[r.ordinal()];
        return i >= 0 ? IDS[i] : null;
    }

    void actualizar(double dt, Map<Rol, Actor> equipo) {
        for (Rol r : Rol.values()) {
            int k = r.ordinal();
            if (selector[k]) {
                tSelector[k] += dt;
            }
            if (actual[k] >= 0) {
                t[k] += dt;
                if (t[k] > DURA) {
                    actual[k] = -1;
                }
            }
        }
    }

    // ===================== RUEDITA =====================

    private static final int[] OFF_X = {-171, -57, 57, 171};

    private static double centroX(Actor a, int i) {
        return a.x + OFF_X[i];
    }

    private static double centroY(Actor a) {
        return Math.max(110, a.y - a.alto - 96);
    }

    /** Que burbuja de la ruedita hay en ese punto (-1 = ninguna). */
    int opcionEn(Actor a, Point p) {
        if (a == null) {
            return -1;
        }
        for (int i = 0; i < 4; i++) {
            double dx = p.x - centroX(a, i), dy = p.y - centroY(a);
            if (dx * dx + dy * dy < 56 * 56) {
                return i;
            }
        }
        return -1;
    }

    // ===================== DIBUJO =====================

    /** Burbujas de todos + la ruedita de 'mio' (en un solo PC, la del que camina). */
    void dibujar(Graphics2D g0, Map<Rol, Actor> equipo, Rol mio, double tiempo) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (Rol r : Rol.values()) {
            int k = r.ordinal();
            Actor a = equipo.get(r);
            if (a == null || actual[k] < 0) {
                continue;
            }
            double tt = t[k];
            double s = tt < 0.22 ? rebote(tt / 0.22) : 1.0;
            float alfa = (float) Math.max(0, Math.min(1, (DURA - tt) / 0.35));
            double bx = a.x + 30, by = a.y - a.alto + 4 + Math.sin(tiempo * 3 + k) * 4;
            BufferedImage im = burbuja(r, actual[k]);
            AffineTransform at = g.getTransform();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
            g.translate(bx, by);
            g.scale(s, s);
            // la colita queda abajo a la izquierda, apuntando a la cabeza
            g.drawImage(im, -26, -im.getHeight(), null);
            g.setTransform(at);
        }
        g.setComposite(AlphaComposite.SrcOver);
        if (mio != null && selector[mio.ordinal()] && equipo.get(mio) != null) {
            Actor a = equipo.get(mio);
            double ts = tSelector[mio.ordinal()];
            for (int i = 0; i < 4; i++) {
                double ti = Math.max(0, ts - i * 0.04);
                double s = ti < 0.2 ? rebote(ti / 0.2) : 1.0;
                if (s <= 0.01) {
                    continue;
                }
                double cx = centroX(a, i), cy = centroY(a) + Math.sin(tiempo * 3 + i) * 3;
                BufferedImage im = opcion(mio, i);
                AffineTransform at = g.getTransform();
                g.translate(cx, cy);
                g.scale(s, s);
                g.drawImage(im, -im.getWidth() / 2, -im.getHeight() / 2, null);
                g.setTransform(at);
            }
            // "R para cerrar"
            g.setFont(Estilo.fuente(Font.BOLD, 15f));
            String txt = "1-4 o clic  ·  R cerrar";
            int w = g.getFontMetrics().stringWidth(txt) + 26;
            int x = (int) (a.x - w / 2.0), y = (int) (centroY(a) + 62);
            g.setColor(new Color(255, 250, 240, 225));
            g.fill(new RoundRectangle2D.Double(x, y, w, 28, 28, 28));
            g.setColor(new Color(0xA88B6A));
            g.drawString(txt, x + 13, y + 19);
        }
        g.dispose();
    }

    /** easeOutBack: crece pasandose un poquito y vuelve (efecto "pop"). */
    private static double rebote(double x) {
        double c1 = 1.70158, c3 = c1 + 1;
        x = Math.max(0, Math.min(1, x));
        return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
    }

    // ===================== IMAGENES (se hacen una vez) =====================

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Tu dibujo de esa emocion (personajes/<rol>/feliz.png...), o null si no esta. */
    private static String dibujo(Rol r, int i) {
        String e = r.sprites() + IDS[i] + ".png";
        return Recursos.existe(e) ? e : null;
    }

    /** El sprite normal (para la carita de repuesto). */
    private static String quieto(Rol r) {
        String q = Rutas.spriteQuieto(r);
        return q != null ? q : ArteMj.spriteOficial(r);
    }

    /** El dibujo sin los bordes transparentes (asi llena bien la burbuja). */
    private static BufferedImage recortado(String ruta) {
        BufferedImage src = Recursos.imagen(ruta, 560, 680);
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                if ((src.getRGB(x, y) >>> 24) > 20) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return src;
        }
        return src.getSubimage(minX, minY, Math.min(w - minX, maxX - minX + 2), Math.min(h - minY, maxY - minY + 2));
    }

    /** Dibuja la emocion dentro de un cuadro (centrada, sin deformar). */
    private static void dentro(Graphics2D g, Rol r, int i, double cx, double cy, double lado) {
        String d = dibujo(r, i);
        if (d == null) {
            ArteMj.caraDe(g, cx, cy, lado * 0.42, quieto(r), r.name(), r.color);
            simbolo(g, i, cx + lado * 0.4, cy - lado * 0.36, lado * 0.13);
            return;
        }
        BufferedImage im = recortado(d);
        double esc = Math.min(lado / im.getWidth(), lado / im.getHeight());
        int w = (int) Math.round(im.getWidth() * esc), h = (int) Math.round(im.getHeight() * esc);
        g.drawImage(im, (int) Math.round(cx - w / 2.0), (int) Math.round(cy - h / 2.0), w, h, null);
    }

    /** Burbuja de dialogo blanca con la carita de la emocion. */
    static BufferedImage burbuja(Rol r, int i) {
        return CACHE.computeIfAbsent("b" + r.ordinal() + i, k -> {
            int w = 150, h = 158;
            BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = im.createGraphics();
            suave(g);
            Path2D globo = new Path2D.Double();
            globo.append(new RoundRectangle2D.Double(5, 4, w - 10, h - 36, 50, 50), false);
            Path2D cola = new Path2D.Double();
            cola.moveTo(18, h - 44);
            cola.lineTo(24, h - 5);
            cola.lineTo(50, h - 38);
            cola.closePath();
            java.awt.geom.Area a = new java.awt.geom.Area(globo);
            a.add(new java.awt.geom.Area(cola));
            g.setColor(new Color(60, 34, 14, 50));
            g.translate(0, 3);
            g.fill(a);
            g.translate(0, -3);
            g.setColor(Color.WHITE);
            g.fill(a);
            dentro(g, r, i, w / 2.0, 4 + (h - 36) / 2.0, 108);
            g.setColor(new Color(0x5A3B1F));
            g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(a);
            g.dispose();
            return Recursos.compatible(im);
        });
    }

    /** Una opcion de la ruedita: circulo crema con la carita, el simbolo y su numero. */
    private static BufferedImage opcion(Rol r, int i) {
        return CACHE.computeIfAbsent("o" + r.ordinal() + i, k -> {
            int s = 112;
            BufferedImage im = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = im.createGraphics();
            suave(g);
            g.setColor(new Color(60, 34, 14, 55));
            g.fill(new Ellipse2D.Double(8, 11, s - 16, s - 16));
            g.setColor(new Color(0xFFFAF0));
            g.fill(new Ellipse2D.Double(6, 6, s - 16, s - 16));
            g.setColor(new Color(0xEAD7AE));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(6, 6, s - 16, s - 16));
            java.awt.Shape antes = g.getClip();
            g.clip(new Ellipse2D.Double(8, 8, s - 20, s - 20));
            dentro(g, r, i, s / 2.0 - 5, s / 2.0 - 3, 92);
            g.setClip(antes);
            // numero
            g.setColor(r.color);
            g.fill(new Ellipse2D.Double(4, s - 38, 30, 30));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.4f));
            g.draw(new Ellipse2D.Double(4, s - 38, 30, 30));
            g.setFont(Estilo.fuente(Font.BOLD, 18f));
            String num = String.valueOf(i + 1);
            g.drawString(num, 19 - g.getFontMetrics().stringWidth(num) / 2, s - 16);
            g.dispose();
            return Recursos.compatible(im);
        });
    }

    private static void suave(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Corazon (feliz), gota (triste), "venita" (enojado) o signo de exclamacion (sorprendido). */
    private static void simbolo(Graphics2D g, int i, double cx, double cy, double r) {
        Color borde = new Color(0x5A3B1F);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (i) {
            case 0: {
                Path2D p = new Path2D.Double();
                p.moveTo(cx, cy + r * 0.9);
                p.curveTo(cx - r * 1.4, cy - r * 0.1, cx - r * 0.7, cy - r * 1.1, cx, cy - r * 0.35);
                p.curveTo(cx + r * 0.7, cy - r * 1.1, cx + r * 1.4, cy - r * 0.1, cx, cy + r * 0.9);
                p.closePath();
                g.setColor(new Color(0xF27B9B));
                g.fill(p);
                g.setColor(borde);
                g.draw(p);
                break;
            }
            case 1: {
                Path2D p = new Path2D.Double();
                p.moveTo(cx, cy - r);
                p.curveTo(cx + r * 0.9, cy, cx + r * 0.8, cy + r, cx, cy + r);
                p.curveTo(cx - r * 0.8, cy + r, cx - r * 0.9, cy, cx, cy - r);
                p.closePath();
                g.setColor(new Color(0x7FB2E0));
                g.fill(p);
                g.setColor(borde);
                g.draw(p);
                g.setColor(new Color(255, 255, 255, 200));
                g.fill(new Ellipse2D.Double(cx - r * 0.35, cy, r * 0.3, r * 0.4));
                break;
            }
            case 2: {
                g.setColor(new Color(0xE0574A));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int q = 0; q < 4; q++) {
                    double ang = q * Math.PI / 2 + Math.PI / 4;
                    double ux = Math.cos(ang), uy = Math.sin(ang);
                    Path2D p = new Path2D.Double();
                    double ax = cx + ux * r * 0.95, ay = cy + uy * r * 0.95;
                    double bx = cx + ux * r * 0.3, by = cy + uy * r * 0.3;
                    p.moveTo(ax - uy * r * 0.45, ay + ux * r * 0.45);
                    p.quadTo(bx, by, ax + uy * r * 0.45, ay - ux * r * 0.45);
                    g.draw(p);
                }
                break;
            }
            default: {
                Path2D estrella = new Path2D.Double();
                for (int q = 0; q < 16; q++) {
                    double ang = q * Math.PI / 8 - Math.PI / 2;
                    double rr = q % 2 == 0 ? r * 1.15 : r * 0.7;
                    if (q == 0) {
                        estrella.moveTo(cx + Math.cos(ang) * rr, cy + Math.sin(ang) * rr);
                    } else {
                        estrella.lineTo(cx + Math.cos(ang) * rr, cy + Math.sin(ang) * rr);
                    }
                }
                estrella.closePath();
                g.setColor(new Color(0xFFD35C));
                g.fill(estrella);
                g.setColor(borde);
                g.draw(estrella);
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine((int) cx, (int) (cy - r * 0.45), (int) cx, (int) (cy + r * 0.1));
                g.fill(new Ellipse2D.Double(cx - 2.2, cy + r * 0.3, 4.4, 4.4));
                break;
            }
        }
    }
}
