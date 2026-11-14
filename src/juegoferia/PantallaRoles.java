package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Pantalla "Elige tu rol", dibujada por código y animada.
 * Diseño lógico 1920 x 1080 (igual que PantallaInicio).
 *
 * Uso en partvisual:
 *   new PantallaRoles(
 *           () -> card.show(Panel_cambiante, "inicio"),
 *           () -> card.show(Panel_cambiante, "nombre"));
 */
public class PantallaRoles extends JPanel {

    private static final int W = 1920;
    private static final int H = 1080;

    private static final String RUTA_FONDO = "/imagenes/fondoinicio.png";   // cámbialo si tienes otro fondo
    private static final String RUTA_PERSONAJE = "/imagenes/personajecivil.png";

    private static final Color BORDE = new Color(0x8A4F25);
    private static final Color TEXTO = new Color(0x5A2D1A);

    private static final double MEDIA_DIAG = 205;                 // "radio" del rombo
    private static final double LADO = MEDIA_DIAG * Math.sqrt(2);
    private static final Shape ROMBO = crearRombo();

    private static Shape crearRombo() {
        RoundRectangle2D rr = new RoundRectangle2D.Double(-LADO / 2, -LADO / 2, LADO, LADO, 70, 70);
        return AffineTransform.getRotateInstance(Math.PI / 4).createTransformedShape(rr);
    }

    // ---------- Fuentes ----------
    private static final String FUENTE = elegirFuente(
            new String[]{"Arial Black", "Segoe UI Black", "Impact", "Verdana"});
    private static final int ESTILO = (FUENTE.equals("Arial Black")
            || FUENTE.equals("Segoe UI Black") || FUENTE.equals("Impact")) ? Font.PLAIN : Font.BOLD;
    private static final String FUENTE_BTN = elegirFuente(
            new String[]{"Arial Rounded MT Bold", "Trebuchet MS", "Segoe UI", "Verdana"});

    private static String elegirFuente(String[] preferidas) {
        Set<String> disponibles = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : preferidas) {
            if (disponibles.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    // ---------- Datos de cada rol ----------
    private static class Rol {

        final String nombre;
        final String descripcion;
        final double cx, cy;
        final Color color, tinta;
        final boolean bloqueado;
        final int icono;          // 0 casa, 1 periódico, 2 gente, 3 celular
        float hover = 0f;
        float presion = 0f;
        double sacudida = 0;      // segundos que le quedan al temblor

        Rol(String nombre, String descripcion, double cx, double cy, Color color, Color tinta,
                boolean bloqueado, int icono) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.cx = cx;
            this.cy = cy;
            this.color = color;
            this.tinta = tinta;
            this.bloqueado = bloqueado;
            this.icono = icono;
        }
    }

    private final Rol[] roles = {
        new Rol("Alcalde",
        "Dirige la isla: toma decisiones de gobierno y enfrenta las crisis de desinformación.",
        960, 340, new Color(0xF2D27A), new Color(0x8A5A12), true, 0),
        new Rol("Periodista",
        "Investiga, contrasta fuentes y publica noticias verificadas.",
        689, 574, new Color(0xF4928A), new Color(0x8A2F2A), true, 1),
        new Rol("Civil",
        "Vive el día a día de la isla y decide si verificas, difundes o reportas lo que ves en redes.",
        1229, 574, new Color(0x4D9BF0), new Color(0x1F4F9A), false, 2),
        new Rol("Influencer",
        "Tus publicaciones llegan a miles: elige con cuidado qué compartes.",
        960, 810, new Color(0x8FD65A), new Color(0x3E7A1E), true, 3)
    };

    // ---------- Estado ----------
    private final Runnable alVolver;
    private final Runnable alCivil;
    private final Timer reloj;
    private long inicio = System.nanoTime();
    private long ultimo = inicio;
    private double t = 0;
    private final Random azar = new Random(11);

    private int sobre = -1;           // rombo bajo el cursor
    private int presionado = -1;      // rombo que se está presionando
    private boolean sobreVolver = false;
    private boolean presionVolver = false;
    private float hoverVolver = 0f;
    private float hundidoVolver = 0f;
    private int ratonX, ratonY;       // posición del cursor en píxeles del panel

    private BufferedImage fondoOriginal;
    private BufferedImage fondoEscalado;
    private BufferedImage personaje;

    // Hojas y luciérnagas
    private static final int N_HOJAS = 14;
    private final float[] hx = new float[N_HOJAS], hy = new float[N_HOJAS], hs = new float[N_HOJAS],
            hf = new float[N_HOJAS], hr = new float[N_HOJAS], hw = new float[N_HOJAS];
    private static final int N_LUC = 26;
    private final float[] lx = new float[N_LUC], ly = new float[N_LUC], lf = new float[N_LUC],
            ls = new float[N_LUC];

    public PantallaRoles(Runnable alVolver, Runnable alCivil) {
        this.alVolver = alVolver;
        this.alCivil = alCivil;

        setLayout(null);
        setOpaque(true);
        setBackground(new Color(0x1B2A1B));

        fondoOriginal = cargar(RUTA_FONDO);
        personaje = cargar(RUTA_PERSONAJE);
        iniciarElementos();

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                actualizarRaton(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                actualizarRaton(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sobre = -1;
                sobreVolver = false;
                presionado = -1;
                presionVolver = false;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                actualizarRaton(e);
                presionado = sobre;
                presionVolver = sobreVolver;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                actualizarRaton(e);
                int idx = presionado;
                boolean volver = presionVolver;
                presionado = -1;
                presionVolver = false;

                if (volver && sobreVolver) {
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    alVolver.run();
                } else if (idx >= 0 && idx == sobre) {
    if (!roles[idx].bloqueado) {
        Funciones.reproducirSonido("/sonidos/click.wav");
        alCivil.run();
    }
}
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        reloj = new Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimo) / 1e9);
            ultimo = ahora;
            t = (ahora - inicio) / 1e9;
            actualizar(dt);
            repaint();
        });

        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    inicio = System.nanoTime();   // reinicia la entrada animada
                    ultimo = inicio;
                    reloj.start();
                } else {
                    reloj.stop();
                    sobre = -1;
                    sobreVolver = false;
                }
            }
        });
    }

    private BufferedImage cargar(String ruta) {
        URL url = PantallaRoles.class.getResource(ruta);
        if (url == null) {
            System.out.println("No se encontró la imagen: " + ruta);
            return null;
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ================================================================
    //  LÓGICA
    // ================================================================
    private double escalaUI() {
        return Math.min(getWidth() / (double) W, getHeight() / (double) H);
    }

    private void actualizarRaton(MouseEvent e) {
        ratonX = e.getX();
        ratonY = e.getY();

        double us = escalaUI();
        double px = (e.getX() - (getWidth() - W * us) / 2.0) / us;
        double py = (e.getY() - (getHeight() - H * us) / 2.0) / us;

        sobre = -1;
        for (int i = 0; i < roles.length; i++) {
            Rol r = roles[i];
            double e0 = aparicion(i);
            if (e0 < 0.9) {
                continue;
            }
            double dx = Math.abs(px - r.cx);
            double dy = Math.abs(py - (r.cy + flotar(i)));
            if (dx + dy <= MEDIA_DIAG - 8) {
                sobre = i;
                break;
            }
        }
        sobreVolver = aparicionVolver() >= 0.9 && px >= 50 && px <= 315 && py >= 965 && py <= 1035;

        setCursor((sobre >= 0 || sobreVolver) ? Cursores.MANO : Cursores.NORMAL);
    }

    private double aparicion(int i) {
        return easeOutBack(clamp((t - 0.35 - i * 0.12) / 0.6));
    }

    private double aparicionVolver() {
        return easeOutBack(clamp((t - 0.9) / 0.5));
    }

    private double flotar(int i) {
        return Math.sin(t * 1.0 + i * 1.3) * 2.5;
    }

    private void iniciarElementos() {
        for (int i = 0; i < N_HOJAS; i++) {
            hx[i] = azar.nextInt(W);
            hy[i] = azar.nextInt(H);
            hs[i] = 18 + azar.nextFloat() * 20;
            hf[i] = azar.nextFloat() * 6.28f;
            hr[i] = azar.nextFloat() * 6.28f;
            hw[i] = 1.5f + azar.nextFloat() * 2.5f;
        }
        for (int i = 0; i < N_LUC; i++) {
            lx[i] = azar.nextInt(W);
            ly[i] = 80 + azar.nextInt(H - 160);
            lf[i] = azar.nextFloat() * 6.28f;
            ls[i] = 5 + azar.nextFloat() * 7;
        }
    }

    private void actualizar(double dt) {
        for (int i = 0; i < N_HOJAS; i++) {
            hx[i] += (60 + 90 * hs[i] / 40.0) * dt;
            hy[i] += (18 + Math.sin(t * 1.2 + hf[i]) * 40) * dt;
            hr[i] += hw[i] * dt;
            if (hx[i] > W + 80) {
                hx[i] = -80;
                hy[i] = azar.nextInt(H);
            }
            if (hy[i] > H + 80) {
                hy[i] = -80;
            }
        }
        for (int i = 0; i < N_LUC; i++) {
            lx[i] += 8 * dt;
            if (lx[i] > W + 20) {
                lx[i] = -20;
            }
        }

        for (int i = 0; i < roles.length; i++) {
            Rol r = roles[i];
            float th = (sobre == i) ? 1f : 0f;
            float tp = (presionado == i && sobre == i) ? 1f : 0f;
            r.hover += (th - r.hover) * 0.22f;
            r.presion += (tp - r.presion) * 0.45f;
            if (Math.abs(r.hover - th) < 0.01f) {
                r.hover = th;
            }
            if (Math.abs(r.presion - tp) < 0.01f) {
                r.presion = tp;
            }
            if (r.sacudida > 0) {
                r.sacudida = Math.max(0, r.sacudida - dt);
            }
        }
        float thv = sobreVolver ? 1f : 0f;
        float tpv = (presionVolver && sobreVolver) ? 1f : 0f;
        hoverVolver += (thv - hoverVolver) * 0.22f;
        hundidoVolver += (tpv - hundidoVolver) * 0.45f;
    }

    // ================================================================
    //  DIBUJO
    // ================================================================
    private static void activarCalidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private Graphics2D graficosFondo(Graphics g0) {
        int w = getWidth();
        int h = getHeight();
        double esc = Math.max(w / (double) W, h / (double) H);
        Graphics2D g = (Graphics2D) g0.create();
        activarCalidad(g);
        g.translate((w - W * esc) / 2.0, (h - H * esc) / 2.0);
        g.scale(esc, esc);
        return g;
    }

    private Graphics2D graficosUI(Graphics g0) {
        int w = getWidth();
        int h = getHeight();
        double us = escalaUI();
        Graphics2D u = (Graphics2D) g0.create();
        activarCalidad(u);
        u.translate((w - W * us) / 2.0, (h - H * us) / 2.0);
        u.scale(us, us);
        return u;
    }

    private BufferedImage escalarFondo(int w, int h) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        activarCalidad(g);
        if (fondoOriginal == null) {
            g.setPaint(new GradientPaint(0, 0, new Color(0x1E3A2A), 0, h, new Color(0x0F1A14)));
            g.fillRect(0, 0, w, h);
        } else {
            double esc = Math.max(w / (double) fondoOriginal.getWidth(),
                    h / (double) fondoOriginal.getHeight());
            int iw = (int) Math.ceil(fondoOriginal.getWidth() * esc);
            int ih = (int) Math.ceil(fondoOriginal.getHeight() * esc);
            g.drawImage(fondoOriginal, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
        }
        g.dispose();
        return im;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (fondoEscalado == null || fondoEscalado.getWidth() != w || fondoEscalado.getHeight() != h) {
            fondoEscalado = escalarFondo(w, h);
        }

        // fondo oscurecido (como en tu imagen de Canva)
        Graphics2D base = (Graphics2D) g0.create();
        activarCalidad(base);
        base.drawImage(fondoEscalado, 0, 0, null);
        base.setColor(new Color(8, 14, 10, 165));
        base.fillRect(0, 0, w, h);
        base.dispose();

        Graphics2D g = graficosFondo(g0);
        dibujarLuciernagas(g);
        dibujarHojas(g);
        g.dispose();

        Graphics2D u = graficosUI(g0);
        for (int i = 0; i < roles.length; i++) {
            dibujarTarjeta(u, roles[i], i);
        }
        dibujarTitulo(u);
        dibujarVolver(u);
        u.dispose();

        // tooltip (en píxeles reales, sin escalar)
        Graphics2D tg = (Graphics2D) g0.create();
        activarCalidad(tg);
        if (sobre >= 0) {
            Rol r = roles[sobre];
            String titulo = r.bloqueado ? r.nombre + " (bloqueado)" : r.nombre;
            TooltipJuego.dibujar(tg, titulo, r.descripcion, ratonX, ratonY, w, h, r.hover);
        } else if (hoverVolver > 0.02f) {
            TooltipJuego.dibujar(tg, "Volver", "Regresa al menú de inicio.", ratonX, ratonY, w, h, hoverVolver);
        }
        tg.dispose();
    }

    // ---------------- Ambiente ----------------
    private void dibujarLuciernagas(Graphics2D g) {
        for (int i = 0; i < N_LUC; i++) {
            double tw = (Math.sin(t * 1.8 + lf[i]) + 1) / 2;
            double x = lx[i] + Math.sin(t * 0.7 + lf[i]) * 30;
            double y = ly[i] + Math.cos(t * 0.9 + lf[i] * 2) * 24;
            float r = (float) (ls[i] * (1.2 + 1.6 * tw));
            g.setPaint(new RadialGradientPaint((float) x, (float) y, r * 3, new float[]{0f, 1f},
                    new Color[]{new Color(255, 240, 150, (int) (40 + 150 * tw)), new Color(255, 240, 150, 0)}));
            g.fill(new Ellipse2D.Double(x - r * 3, y - r * 3, r * 6, r * 6));
            g.setColor(new Color(255, 250, 200, (int) (120 + 135 * tw)));
            g.fill(new Ellipse2D.Double(x - r * 0.4, y - r * 0.4, r * 0.8, r * 0.8));
        }
    }

    private void dibujarHojas(Graphics2D g) {
        AffineTransform old = g.getTransform();
        for (int i = 0; i < N_HOJAS; i++) {
            double L = hs[i];
            g.translate(hx[i], hy[i]);
            g.rotate(hr[i]);
            g.scale(1, 0.35 + 0.65 * Math.abs(Math.cos(t * 1.5 + hf[i])));
            Path2D hoja = new Path2D.Double();
            hoja.moveTo(-L / 2, 0);
            hoja.quadTo(0, -L * 0.42, L / 2, 0);
            hoja.quadTo(0, L * 0.42, -L / 2, 0);
            g.setColor(new Color(0x4C9A35));
            g.fill(hoja);
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(-L / 2, 0, L / 2, 0));
            g.setTransform(old);
        }
    }

    // ---------------- Título ----------------
    private void dibujarTitulo(Graphics2D g) {
        double pop = easeOutBack(clamp(t / 0.8));
        if (pop <= 0.01) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(960, 105 + Math.sin(t * 1.6) * 4);
        g.rotate(Math.sin(t * 1.1) * 0.01);
        g.scale(pop, pop);

        RoundRectangle2D tabla = new RoundRectangle2D.Double(-380, -62, 760, 124, 50, 50);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(-380, -48, 760, 124, 50, 50));
        g.setPaint(new GradientPaint(0, -62, new Color(0xE0A262), 0, 62, new Color(0xB8743A)));
        g.fill(tabla);
        g.setColor(new Color(255, 255, 255, 45));
        Shape clip = g.getClip();
        g.clip(tabla);
        g.fillRect(-380, -62, 760, 42);
        g.setClip(clip);
        g.setColor(new Color(0x7A4524));
        g.setStroke(new BasicStroke(7f));
        g.draw(tabla);

        racimo(g, -395, 0, 1.0, 0.0);
        racimo(g, 395, 0, 1.0, 2.0);

        // letras que se mecen una por una
        AffineTransform sv = g.getTransform();
        Font f = new Font(FUENTE, ESTILO, 84);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), "Elige tu rol");
        Rectangle2D vb = gv.getVisualBounds();
        double esc = Math.min(1.0, 560 / vb.getWidth());
        g.translate(0, 28);
        g.scale(esc, esc);
        g.translate(-vb.getCenterX(), 0);
        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            Shape gs = gv.getGlyphOutline(i);
            Rectangle2D b = gs.getBounds2D();
            if (b.isEmpty()) {
                continue;
            }
            double dy = Math.sin(t * 2.6 + i * 0.7) * 5;
            Shape sh = AffineTransform.getTranslateInstance(0, dy).createTransformedShape(gs);
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(AffineTransform.getTranslateInstance(0, 6).createTransformedShape(sh));
            g.setColor(TEXTO);
            g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sh);
            Rectangle2D bb = sh.getBounds2D();
            g.setPaint(new GradientPaint(0, (float) bb.getMinY(), new Color(0xFFF8E1),
                    0, (float) bb.getMaxY(), new Color(0xFFD9A0)));
            g.fill(sh);
        }
        g.setTransform(sv);
        g.setTransform(old);
    }

    // ---------------- Tarjetas (rombos) ----------------
    private void dibujarTarjeta(Graphics2D g, Rol r, int i) {
        double e = aparicion(i);
        if (e <= 0.01) {
            return;
        }
        double hv = r.hover;
        double pr = r.presion;
        double sac = 0;

        AffineTransform old = g.getTransform();
        g.translate(r.cx + sac, r.cy + flotar(i));
        double esc = e * (1 + 0.025 * hv);
        g.scale(esc, esc);

        double hund = 12 * pr;
        double alza = -4 * hv * (1 - pr);

        // sombra
        g.setColor(new Color(0, 0, 0, 75));
        g.fill(AffineTransform.getTranslateInstance(6, 28).createTransformedShape(ROMBO));

        // base (grosor del botón)
        Shape base = AffineTransform.getTranslateInstance(0, 14).createTransformedShape(ROMBO);
        g.setColor(mezclar(r.color, TEXTO, 0.45f));
        g.fill(base);
        g.setColor(BORDE);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(base);

        // cara
        g.translate(0, alza + hund);
        g.setPaint(new GradientPaint(0, (float) -MEDIA_DIAG, mezclar(r.color, Color.WHITE, 0.35f + 0.12f * (float) hv),
                0, (float) MEDIA_DIAG, mezclar(r.color, r.tinta, 0.10f)));
        g.fill(ROMBO);

        Shape clip = g.getClip();
        g.clip(ROMBO);
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(16f));
        g.draw(ROMBO);
        g.setClip(clip);

        g.setColor(BORDE);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(ROMBO);

        // etiqueta
        dibujarEtiqueta(g, r.nombre, 0, -68);

        // icono pequeño arriba
        AffineTransform sv = g.getTransform();
        g.translate(0, -132);
        g.rotate(Math.sin(t * 1.5 + i) * 0.03);
        dibujarIcono(g, r.icono, 76);
        g.setTransform(sv);

        // parte de abajo: candado o personaje
        if (r.bloqueado) {
           dibujarCandado(g, 0, 62, 0.85, 0);
        } else {
            dibujarPersonaje(g, 0, 52, 190);
        }

        if (pr > 0.01) {
            g.setColor(new Color(0, 0, 0, (int) Math.round(40 * pr)));
            g.fill(ROMBO);
        }

        g.setTransform(old);
    }

    private void dibujarEtiqueta(Graphics2D g, String texto, double x, double y) {
        Font f = new Font(FUENTE_BTN, Font.BOLD, 26);
        FontMetrics fm = g.getFontMetrics(f);
        double ancho = fm.stringWidth(texto) + 44;
        RoundRectangle2D pill = new RoundRectangle2D.Double(x - ancho / 2, y - 21, ancho, 42, 42, 42);
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new RoundRectangle2D.Double(x - ancho / 2, y - 15, ancho, 42, 42, 42));
        g.setColor(new Color(0xFFF6E6));
        g.fill(pill);
        g.setColor(new Color(0x7A4524));
        g.setStroke(new BasicStroke(4f));
        g.draw(pill);
        g.setFont(f);
        g.setColor(TEXTO);
        g.drawString(texto, (float) (x - fm.stringWidth(texto) / 2.0), (float) (y + fm.getAscent() / 2.0 - 3));
    }

    private void dibujarPersonaje(Graphics2D g, double x, double y, double alto) {
        if (personaje == null) {
            return;
        }
        double ancho = alto * personaje.getWidth() / personaje.getHeight();
        AffineTransform old = g.getTransform();
        g.translate(x, y + alto / 2);                       // pivote en los pies
        g.rotate(Math.sin(t * 2.0) * 0.03);
        g.drawImage(personaje, (int) (-ancho / 2), (int) (-alto), (int) ancho, (int) alto, null);
        g.setTransform(old);
    }

    private void dibujarCandado(Graphics2D g, double x, double y, double esc, double giro) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.scale(esc, esc);
        g.rotate(giro, 0, 60);

        g.setStroke(new BasicStroke(22f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x9A9AA0));
        g.draw(new Arc2D.Double(-42, -95, 84, 100, 0, 180, Arc2D.OPEN));
        g.draw(new Line2D.Double(-42, -45, -42, -8));
        g.draw(new Line2D.Double(42, -45, 42, -8));

        RoundRectangle2D cuerpo = new RoundRectangle2D.Double(-62, -20, 124, 100, 28, 28);
        g.setPaint(new GradientPaint(0, -20, new Color(0xF7CB6A), 0, 80, new Color(0xE0A23C)));
        g.fill(cuerpo);
        g.setColor(new Color(0x7A4E10));
        g.setStroke(new BasicStroke(6f));
        g.draw(cuerpo);

        g.setColor(new Color(0x4B2812));
        g.fill(new Ellipse2D.Double(-14, 12, 28, 28));
        g.fill(new RoundRectangle2D.Double(-6, 28, 12, 34, 6, 6));
        g.setTransform(old);
    }

    /** Iconos pequeños dibujados con formas, centrados en (0,0). */
    private void dibujarIcono(Graphics2D g, int tipo, double s) {
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Color borde = new Color(0x6B3A1E);

        if (tipo == 0) {                                    // casa del alcalde
            Path2D techo = new Path2D.Double();
            techo.moveTo(-0.55 * s, -0.05 * s);
            techo.lineTo(0, -0.55 * s);
            techo.lineTo(0.55 * s, -0.05 * s);
            techo.closePath();
            RoundRectangle2D cuerpo = new RoundRectangle2D.Double(-0.42 * s, -0.05 * s, 0.84 * s, 0.55 * s, 8, 8);
            g.setColor(new Color(0xFFF1D6));
            g.fill(cuerpo);
            g.setColor(borde);
            g.draw(cuerpo);
            g.setColor(new Color(0xE0584A));
            g.fill(techo);
            g.setColor(borde);
            g.draw(techo);
            g.setColor(new Color(0x4D6FB5));
            g.fill(new RoundRectangle2D.Double(-0.1 * s, 0.2 * s, 0.2 * s, 0.3 * s, 8, 8));
            g.setColor(new Color(0xE0584A));
            g.draw(new Line2D.Double(0, -0.55 * s, 0, -0.78 * s));
            g.fill(new Rectangle2D.Double(0, -0.78 * s, 0.22 * s, 0.14 * s));

        } else if (tipo == 1) {                             // periódico
            RoundRectangle2D hoja = new RoundRectangle2D.Double(-0.55 * s, -0.4 * s, 1.1 * s, 0.8 * s, 10, 10);
            g.setColor(new Color(0xF2F2F2));
            g.fill(hoja);
            g.setColor(borde);
            g.draw(hoja);
            g.setColor(new Color(0x8A8A8A));
            g.fill(new Rectangle2D.Double(-0.45 * s, -0.3 * s, 0.4 * s, 0.25 * s));
            for (int i = 0; i < 4; i++) {
                g.draw(new Line2D.Double(0.02 * s, (-0.28 + i * 0.16) * s, 0.45 * s, (-0.28 + i * 0.16) * s));
            }
            for (int i = 0; i < 2; i++) {
                g.draw(new Line2D.Double(-0.45 * s, (0.1 + i * 0.16) * s, -0.05 * s, (0.1 + i * 0.16) * s));
            }

        } else if (tipo == 2) {                             // gente (civil)
            double[][] p = {{-0.4, 0.05, 0.22, 0xE8C9A0}, {0.4, 0.05, 0.22, 0xF4A9A0}, {0, -0.05, 0.3, 0x3B82E0}};
            for (double[] c : p) {
                Color col = new Color((int) c[3]);
                g.setColor(col);
                g.fill(new Ellipse2D.Double((c[0] - c[2]) * s, (c[1] - c[2] - 0.2) * s, c[2] * 2 * s, c[2] * 2 * s));
                g.fill(new Arc2D.Double((c[0] - c[2] * 1.3) * s, (c[1] + 0.12) * s, c[2] * 2.6 * s, c[2] * 2.4 * s, 0, 180, Arc2D.PIE));
                g.setColor(borde);
                g.draw(new Ellipse2D.Double((c[0] - c[2]) * s, (c[1] - c[2] - 0.2) * s, c[2] * 2 * s, c[2] * 2 * s));
            }

        } else {                                            // celular + corazón
            RoundRectangle2D cel = new RoundRectangle2D.Double(-0.5 * s, -0.4 * s, 0.5 * s, 0.9 * s, 12, 12);
            g.setColor(new Color(0x3C6FD1));
            g.fill(cel);
            g.setColor(borde);
            g.draw(cel);
            RoundRectangle2D burbuja = new RoundRectangle2D.Double(0.0, -0.55 * s, 0.6 * s, 0.45 * s, 14, 14);
            g.setColor(Color.WHITE);
            g.fill(burbuja);
            g.setColor(borde);
            g.draw(burbuja);
            double r = 0.09 * s;
            Path2D h = new Path2D.Double();
            h.moveTo(0.3 * s, -0.27 * s + r * 0.9);
            h.curveTo(0.3 * s - r * 1.6, -0.27 * s - r * 0.1, 0.3 * s - r * 0.7, -0.27 * s - r * 1.3, 0.3 * s, -0.27 * s - r * 0.4);
            h.curveTo(0.3 * s + r * 0.7, -0.27 * s - r * 1.3, 0.3 * s + r * 1.6, -0.27 * s - r * 0.1, 0.3 * s, -0.27 * s + r * 0.9);
            g.setColor(new Color(0xE8443F));
            g.fill(h);
        }
    }

    // ---------------- Botón Volver ----------------
    private void dibujarVolver(Graphics2D g) {
        double e = aparicionVolver();
        if (e <= 0.01) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(182 - (1 - e) * 200, 1000);
        double esc = 1 + 0.06 * hoverVolver;
        g.scale(esc, esc);
        double hund = 6 * hundidoVolver;

        RoundRectangle2D pill = new RoundRectangle2D.Double(-132, -35 + hund, 265, 70, 70, 70);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(-132, -35 + 12, 265, 70, 70, 70));
        g.setPaint(new GradientPaint(0, -35, new Color(0xFFF6E6), 0, 35, new Color(0xF3D9A8)));
        g.fill(pill);
        g.setColor(BORDE);
        g.setStroke(new BasicStroke(5f));
        g.draw(pill);

        // flecha
        g.translate(0, hund);
        g.setColor(TEXTO);
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double fx = -70 - Math.sin(t * 4) * 3 * hoverVolver;
        g.draw(new Line2D.Double(fx - 22, 0, fx + 22, 0));
        g.draw(new Line2D.Double(fx - 22, 0, fx - 5, -17));
        g.draw(new Line2D.Double(fx - 22, 0, fx - 5, 17));

        Font f = new Font(FUENTE_BTN, Font.BOLD, 34);
        FontMetrics fm = g.getFontMetrics(f);
        g.setFont(f);
        g.drawString("Volver", 12, (float) (fm.getAscent() / 2.0 - 4));
        g.setTransform(old);
    }

    // ---------------- Decoración ----------------
    private void dibujarFlor(Graphics2D g, double x, double y, double r, double rot) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        for (int i = 0; i < 5; i++) {
            g.rotate(2 * Math.PI / 5);
            Ellipse2D p = new Ellipse2D.Double(-r * 0.38, -r, r * 0.76, r * 0.9);
            g.setColor(new Color(0xFFF4E2));
            g.fill(p);
            g.setColor(new Color(0xD9A27A));
            g.setStroke(new BasicStroke((float) Math.max(2, r * 0.07)));
            g.draw(p);
        }
        g.setColor(new Color(0xFFD34D));
        g.fill(new Ellipse2D.Double(-r * 0.34, -r * 0.34, r * 0.68, r * 0.68));
        g.setColor(new Color(0xD98A1A));
        g.draw(new Ellipse2D.Double(-r * 0.34, -r * 0.34, r * 0.68, r * 0.68));
        g.setTransform(old);
    }

    private void racimo(Graphics2D g, double x, double y, double esc, double fase) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.scale(esc, esc);
        double[] ang = {-150, -100, -40, 10, 60, 120};
        for (int i = 0; i < ang.length; i++) {
            AffineTransform sv = g.getTransform();
            g.rotate(Math.toRadians(ang[i]) + Math.sin(t * 1.5 + fase + i) * 0.08);
            Path2D h = new Path2D.Double();
            double len = 58 - i * 2;
            h.moveTo(0, 0);
            h.quadTo(len * 0.5, -len * 0.42, len, 0);
            h.quadTo(len * 0.5, len * 0.42, 0, 0);
            g.setColor(new Color(0x5BAA2E));
            g.fill(h);
            g.setColor(new Color(0x2F6A18));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(h);
            g.setTransform(sv);
        }
        dibujarFlor(g, 0, 0, 30 * (1 + 0.04 * Math.sin(t * 2 + fase)), Math.sin(t * 1.2 + fase) * 0.2);
        g.setTransform(old);
    }

    // ---------------- Utilidades ----------------
    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double easeOutBack(double p) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2);
    }

    private static Color mezclar(Color a, Color b, float p) {
        p = Math.max(0f, Math.min(1f, p));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * p),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * p),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * p));
    }
}