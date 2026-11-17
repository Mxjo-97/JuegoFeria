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
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * CREDITOS: las cuatro creadoras (con su fotito y lo que hizo cada una), la profe Katherine
 * y los ayudantes sin sueldo. Hecho con amor.
 *
 * Las fotos van en /imagenes/creditos/ (o directo en /imagenes/):
 *   isa.png, mary.png, valen.png, majo.png y katherine.png
 * Si una foto no esta, sale un circulito con la inicial (no se rompe nada).
 */
public class PantallaCreditos extends JPanel {

    private static final int W = 1920, H = 1080;

    private static final class Persona {
        final String apodo, nombre, lema, archivo;
        final Color color;
        final String[] hizo;

        Persona(String apodo, String nombre, String lema, String archivo, Color color, String... hizo) {
            this.apodo = apodo;
            this.nombre = nombre;
            this.lema = lema;
            this.archivo = archivo;
            this.color = color;
            this.hizo = hizo;
        }
    }

    private static final Persona[] EQUIPO = {
        new Persona("Isa", "Isabella", "Guion y minijuegos", "isa", new Color(0xC3A6EE),
                "Complicidad y relaciones entre jugadores",
                "Minijuegos fijos y esporádicos",
                "Guiones día 1 y 2",
                "Guion de la escena inicial",
                "animación de personajes"),
        new Persona("Mary", "Mary", "Arte y mundo", "mary", new Color(0xF28B82),
                "El mapa de Macondo",
                "Fondos nuevos (con Majo)",
                "Diseño de personajes",
                "Directora creativa",
                "Efectos de sonido"),
        new Persona("Valen", "Valentina", "Código y magia", "valen", new Color(0x8EC5F0),
                "Reloj global y niebla del olvido",
                "sprites",
                "Mariposas que te siguen (y las de Mauricio)",
                "Votaciones y escena inicial en código"),
        new Persona("Majo", "Majo", "Arte y emociones", "majo", new Color(0xF6A9C8),
                "Emociones de los personajes",
                "Fondos nuevos",
                "Nuevas colisiones",
                "Efectos de sonido")
    };

    private static final String[] AYUDANTES = {"ChatGPT", "Bocas De Ceniza","Gemini", "Claude", "DeepSeek", "canva", "el nequi de isabella", "powershel"};

    private final Runnable alVolver;
    private final Timer reloj = new Timer(16, e -> repaint());
    private long inicioNs = System.nanoTime();
    private Point mouse = new Point(-100, -100);
    private final double[] hover = new double[EQUIPO.length + 1];

    public PantallaCreditos(Runnable alVolver) {
        this.alVolver = alVolver;
        setOpaque(true);
        setBackground(new Color(0xF6D9C0));
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (zonaVolver().contains(aDiseno(e.getPoint()))) {
                    Ajustes.sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
                    alVolver.run();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = aDiseno(e.getPoint());
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    inicioNs = System.nanoTime();
                    reloj.start();
                } else {
                    reloj.stop();
                }
            }
        });
    }

    private Rectangle zonaVolver() {
        return new Rectangle(40, 40, 210, 66);
    }

    private Point aDiseno(Point p) {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double s = Math.min(w / (double) W, h / (double) H);
        double ox = (w - W * s) / 2, oy = (h - H * s) / 2;
        return new Point((int) ((p.x - ox) / s), (int) ((p.y - oy) / s));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        int w = getWidth(), h = getHeight();
        double s = Math.min(w / (double) W, h / (double) H);
        g.translate((w - W * s) / 2, (h - H * s) / 2);
        g.scale(s, s);
        g.setClip(0, 0, W, H);
        dibujar(g, (System.nanoTime() - inicioNs) / 1e9);
        g.dispose();
    }

    void dibujar(Graphics2D g, double t) {
        Estilo.suave(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Acogedor.fondo(g, "/imagenes/fondoinicio.png", 0.7f);
        Acogedor.petalos(g, t, 30);

        Acogedor.cartel(g, W / 2.0, 22, "Créditos", "Hecho con muchísimo amor", t);
        Rectangle rv = zonaVolver();
        Acogedor.pastilla(g, rv.x, rv.y, rv.width, rv.height, "Volver", rv.contains(mouse) ? 1 : 0, Acogedor.ROSA);

        // ---- las cuatro ----
        int cw = 400, ch = 516, gap = 28, y0 = 212;
        int x0 = (W - (EQUIPO.length * cw + (EQUIPO.length - 1) * gap)) / 2;
        for (int i = 0; i < EQUIPO.length; i++) {
            Rectangle r = new Rectangle(x0 + i * (cw + gap), y0, cw, ch);
            hover[i] += ((r.contains(mouse) ? 1 : 0) - hover[i]) * 0.2;
            tarjeta(g, EQUIPO[i], r, t, i);
        }

        // ---- entre todas ----
        String todas = "Y entre todas: la máquina de escribir de las narraciones, los diálogos y los momentos serios.";
        g.setFont(Estilo.fuente(Font.BOLD, 19f));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(todas) + 70;
        int tx = (W - tw) / 2, ty = 742;
        g.setColor(new Color(255, 249, 238, 235));
        g.fill(new RoundRectangle2D.Double(tx, ty, tw, 40, 40, 40));
        g.setColor(new Color(0xEAD7AE));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(tx, ty, tw, 40, 40, 40));
        Acogedor.estrellita(g, tx + 22, ty + 20, 9, Acogedor.AMARILLO);
        Acogedor.estrellita(g, tx + tw - 22, ty + 20, 9, Acogedor.AMARILLO);
        g.setColor(new Color(0x6E4A2A));
        g.drawString(todas, tx + 35, ty + 27);

        // ---- Katherine ----
        katherine(g, new Rectangle(118, 806, 900, 236), t);

        // ---- ayudantes sin sueldo ----
        ayudantes(g, 1050, 806, 470, 236, t);

        // ---- Ringo ----
        ringo(g, t);
    }

    private void tarjeta(Graphics2D g0, Persona p, Rectangle r, double t, int i) {
        Graphics2D g = (Graphics2D) g0.create();
        double entra = Math.max(0, Math.min(1, (t - 0.15 - i * 0.12) / 0.55));
        double e = 1 - Math.pow(1 - entra, 3);
        double lift = hover[i] * 10;
        double x = r.x, y = r.y + (1 - e) * 60 - lift + Math.sin(t * 1.4 + i * 1.3) * 3;
        double w = r.width, h = r.height;
        Acogedor.alfa(g, entra);
        // inclinacion juguetona
        g.rotate((i % 2 == 0 ? -1 : 1) * 0.012 * (1 - hover[i]), x + w / 2, y + h / 2);
        if (hover[i] > 0.02) {
            g.setColor(new Color(246, 196, 69, (int) (100 * hover[i])));
            g.fill(new RoundRectangle2D.Double(x - 14, y - 14, w + 28, h + 28, 56, 56));
        }
        Shape cuerpo = Acogedor.sticker(g, x, y, w, h, 44, Acogedor.CREMA);
        Shape antes = g.getClip();
        g.clip(cuerpo);
        g.setPaint(new GradientPaint(0, (float) y, p.color.brighter(), 0, (float) (y + 140), p.color));
        g.fill(new Rectangle2D.Double(x, y, w, 128));
        g.setColor(new Color(255, 255, 255, 70));
        for (int k = 0; k < 10; k++) {
            g.fill(new Ellipse2D.Double(x + 20 + (k * 71) % (w - 40), y + 14 + (k * 31) % 90, 10 + k % 3 * 6, 10 + k % 3 * 6));
        }
        g.setClip(antes);
        g.setColor(Acogedor.MARRON);
        g.setStroke(new BasicStroke(4f));
        g.draw(cuerpo);

        // foto
        Acogedor.foto(g, new String[]{"/imagenes/creditos/" + p.archivo + ".png", "/imagenes/" + p.archivo + ".png"},
                x + w / 2, y + 128, 84, p.color.darker(), p.apodo.substring(0, 1), t + i);

        // nombre
        g.setFont(Estilo.fuente(Font.BOLD, 42f));
        g.setColor(new Color(0x5A3216));
        Acogedor.centrado(g, p.apodo, x + w / 2, y + 268);
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        FontMetrics fm = g.getFontMetrics();
        String lema = p.lema.toUpperCase();
        double lw = fm.stringWidth(lema) + 30;
        g.setColor(p.color);
        g.fill(new RoundRectangle2D.Double(x + w / 2 - lw / 2, y + 284, lw, 30, 30, 30));
        g.setColor(new Color(0x5A3216));
        Acogedor.centrado(g, lema, x + w / 2, y + 305);

        // lo que hizo
        g.setFont(Estilo.fuente(Font.PLAIN, 18f));
        fm = g.getFontMetrics();
        double ly = y + 352;
        for (int k = 0; k < p.hizo.length; k++) {
            List<String> ls = envolver(p.hizo[k], fm, (int) w - 96);
            Acogedor.mariposa(g, x + 40, ly - 7, 1.0, t * 0.6 + k + i);
            g.setColor(new Color(0x6E4A2A));
            for (String l : ls) {
                g.drawString(l, (float) (x + 62), (float) ly);
                ly += fm.getHeight() - 2;
            }
            ly += 8;
        }
        g.dispose();
    }

    private void katherine(Graphics2D g0, Rectangle r, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        double entra = Math.max(0, Math.min(1, (t - 0.8) / 0.6));
        Acogedor.alfa(g, entra);
        double y = r.y + (1 - entra) * 40;
        Shape cuerpo = Acogedor.sticker(g, r.x, y, r.width, r.height, 48, new Color(0xFFF4DC));
        // borde dorado interior
        g.setColor(new Color(0xE9C46A));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8, 8}, 0));
        g.draw(new RoundRectangle2D.Double(r.x + 12, y + 12, r.width - 24, r.height - 24, 38, 38));
        Acogedor.foto(g, new String[]{"/imagenes/creditos/katherine.png", "/imagenes/katherine.png"},
                r.x + 138, y + r.height / 2.0, 86, new Color(0xC98A12), "K", t);
        coronita(g, r.x + 138, y + r.height / 2.0 - 104, t);
        double tx = r.x + 262;
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        g.setColor(Acogedor.MARRON_SUAVE);
        g.drawString("Y TAMBIEN CREDITOS A NUESTRA PROFE", (float) tx, (float) (y + 70));
        g.setFont(Estilo.fuente(Font.BOLD, 44f));
        g.setColor(new Color(0x5A3216));
        g.drawString("Katherine, la estimada", (float) tx, (float) (y + 120));
        g.setFont(Estilo.fuente(Font.PLAIN, 20f));
        g.setColor(new Color(0x6E4A2A));
        FontMetrics fm = g.getFontMetrics();
        double ly = y + 160;
        for (String l : envolver("Por su paciencia infinita y darnos plazo hasta el martes",
                fm, r.width - 300)) {
            g.drawString(l, (float) tx, (float) ly);
            ly += fm.getHeight();
        }
        Acogedor.flor(g, r.x + r.width - 30, y + 26, 22, Color.WHITE, t * 0.3);
        Acogedor.flor(g, r.x + r.width - 62, y + 18, 14, Acogedor.ROSA, -t * 0.3);
        g.dispose();
    }

    private static void coronita(Graphics2D g, double x, double y, double t) {
        AffineTransform at = g.getTransform();
        g.translate(x, y + Math.sin(t * 2) * 3);
        Path2D p = new Path2D.Double();
        p.moveTo(-30, 14);
        p.lineTo(-34, -14);
        p.lineTo(-15, 2);
        p.lineTo(0, -22);
        p.lineTo(15, 2);
        p.lineTo(34, -14);
        p.lineTo(30, 14);
        p.closePath();
        g.setPaint(new GradientPaint(0, -22, Estilo.DORADO_CLARO, 0, 14, Estilo.DORADO));
        g.fill(p);
        g.setColor(new Color(0xC98A12));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(Acogedor.ROSA);
        g.fill(new Ellipse2D.Double(-5, 1, 10, 10));
        g.setTransform(at);
    }

    /** Notita amarilla pegada con cinta: los ayudantes que trabajaron sin sueldo. */
    private void ayudantes(Graphics2D g0, double x, double y, double w, double h, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        double entra = Math.max(0, Math.min(1, (t - 1.0) / 0.6));
        Acogedor.alfa(g, entra);
        g.rotate(-0.025, x + w / 2, y + h / 2);
        g.setColor(new Color(70, 35, 12, 50));
        g.fill(new Rectangle2D.Double(x + 6, y + 10, w, h));
        g.setPaint(new GradientPaint(0, (float) y, new Color(0xFFF2A8), 0, (float) (y + h), new Color(0xFBE07C)));
        g.fill(new Rectangle2D.Double(x, y, w, h));
        // cinta
        g.setColor(new Color(255, 255, 255, 150));
        g.fill(new Rectangle2D.Double(x + w / 2 - 60, y - 14, 120, 30));
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(new Color(0x5A3216));
        g.drawString("Ayudantes sin sueldo", (float) (x + 28), (float) (y + 54));
        g.setFont(Estilo.fuente(Font.PLAIN, 16f));
        g.setColor(new Color(0x8A6A3A));
        g.drawString("(trabajaron de madrugada y nunca se quejaron)", (float) (x + 28), (float) (y + 80));
        double cx = x + 28, cy = y + 104;
        g.setFont(Estilo.fuente(Font.BOLD, 19f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < AYUDANTES.length; i++) {
            String a = AYUDANTES[i];
            double cwid = fm.stringWidth(a) + 34;
            if (cx + cwid > x + w - 20) {
                cx = x + 28;
                cy += 50;
            }
            g.setColor(new Color(255, 255, 255, 200));
            g.fill(new RoundRectangle2D.Double(cx, cy, cwid, 38, 38, 38));
            g.setColor(new Color(0xC9A24A));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(cx, cy, cwid, 38, 38, 38));
            g.setColor(new Color(0x5A3216));
            g.drawString(a, (float) (cx + 17), (float) (cy + 26));
            cx += cwid + 12;
        }
        g.dispose();
    }

    private void ringo(Graphics2D g0, double t) {
        if (!Recursos.existe("/imagenes/ringo.png")) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        double entra = Math.max(0, Math.min(1, (t - 1.2) / 0.6));
        int rw = 300, rh = 364;
        double x = W - rw - 30, y = H - rh + 30 + (1 - entra) * 200 + Math.sin(t * 2) * 4;
        g.setColor(new Color(70, 35, 12, 50));
        g.fill(new Ellipse2D.Double(x + 50, H - 26, rw - 100, 26));
        g.drawImage(Recursos.imagen("/imagenes/ringo.png", 560, 680), (int) x, (int) y, rw, rh, null);
        // globito
        if (entra >= 1) {
            String s = "¡Gracias por jugar!";
            g.setFont(Estilo.fuente(Font.BOLD, 20f));
            FontMetrics fm = g.getFontMetrics();
            double bw = fm.stringWidth(s) + 40, bh = 46;
            double bx = x + rw / 2.0 - bw / 2 - 30, by = y - 20 + Math.sin(t * 3) * 3;
            g.setColor(Acogedor.CREMA);
            g.fill(new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh));
            g.fillPolygon(new int[]{(int) (bx + bw / 2), (int) (bx + bw / 2 + 22), (int) (bx + bw / 2 + 4)},
                    new int[]{(int) (by + bh - 2), (int) (by + bh - 2), (int) (by + bh + 18)}, 3);
            g.setColor(Acogedor.MARRON);
            g.setStroke(new BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh));
            g.setColor(new Color(0x5A3216));
            g.drawString(s, (float) (bx + 20), (float) (by + 30));
        }
        g.dispose();
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
