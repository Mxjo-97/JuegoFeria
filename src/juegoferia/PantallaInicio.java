package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Pantalla de inicio: imagen de fondo + viento (mariposas y hojas) +
 * cartel de madera con el título + rueda de 4 botones, todo dibujado por código.
 * Diseño lógico 1920 x 1080.
 *
 * Uso en partvisual:
 *   iniciojuego = new PantallaInicio(
 *           () -> card.show(Panel_cambiante, "roles"),
 *           () -> card.show(Panel_cambiante, "inst"),
 *           () -> System.exit(0),
 *           () -> card.show(Panel_cambiante, "minijuegos"));
 */
public class PantallaInicio extends JPanel {

    private static final int W = 1920;
    private static final int H = 1080;

    /** Ruta de la imagen de fondo dentro de los recursos (carpeta imagenes). */
    private static final String RUTA_FONDO = "/imagenes/fondoinicio.png";

    // ---------- Rueda de botones (coordenadas lógicas) ----------
    private static final double RUEDA_X = 1415;     // centro de la rueda
    private static final double RUEDA_Y = 590;
    private static final double R_CUARTO = 330;     // radio de cada cuarto
    private static final double GAP = 14;           // mitad de la separación entre cuartos
    private static final double PROF = 12;          // profundidad (lo que se hunde al presionar)
    private static final double LADO = 780;         // lado del cuadro que ocupa cada botón
    private static final double R_ARO = 356;        // radio del aro de madera

    private static final Color BORDE = new Color(0x8A4F25);
    private static final Color TEXTO = new Color(0x5A2D1A);

    /** Un cuarto de círculo con esquinas redondeadas (esquina inferior derecha). */
    private static final Shape CUARTO = crearCuarto();

    private static Shape crearCuarto() {
        Area disco = new Area(new Ellipse2D.Double(-R_CUARTO, -R_CUARTO, 2 * R_CUARTO, 2 * R_CUARTO));
        disco.intersect(new Area(new Rectangle2D.Double(GAP, GAP, R_CUARTO, R_CUARTO)));

        // redondear esquinas: se "encoge" y se "infla" la forma
        double rc = 38;
        BasicStroke banda = new BasicStroke((float) (2 * rc), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Area erosion = new Area(disco);
        erosion.subtract(new Area(banda.createStrokedShape(disco)));
        Area res = new Area(erosion);
        res.add(new Area(banda.createStrokedShape(erosion)));
        return res;
    }

    // ---------- Fuentes ----------
    private static final String FUENTE = elegirFuente(
            new String[]{"Arial Black", "Segoe UI Black", "Impact", "Verdana"});
    private static final int ESTILO = (FUENTE.equals("Arial Black")
            || FUENTE.equals("Segoe UI Black")
            || FUENTE.equals("Impact")) ? Font.PLAIN : Font.BOLD;
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

    // ---------- Estado ----------
    private final BotonCuarto[] botones;
    private final Timer reloj;
    private long inicio = System.nanoTime();
    private long ultimo = inicio;
    private double t = 0;
    private double flotarRueda = 0;
    private final Random azar = new Random(7);

    private BufferedImage fondoOriginal;
    private BufferedImage fondoEscalado;

    // Hojas
    private static final int N_HOJAS = 18;
    private final float[] hx = new float[N_HOJAS];
    private final float[] hy = new float[N_HOJAS];
    private final float[] hs = new float[N_HOJAS];
    private final float[] hf = new float[N_HOJAS];
    private final float[] hr = new float[N_HOJAS];
    private final float[] hw = new float[N_HOJAS];
    private final Color[] hc = new Color[N_HOJAS];

    // Mariposas
    private static final int N_MARIP = 9;
    private final float[] mx = new float[N_MARIP];
    private final float[] mb = new float[N_MARIP];      // altura base
    private final float[] ms = new float[N_MARIP];      // tamaño
    private final float[] mph = new float[N_MARIP];     // fase
    private final float[] msp = new float[N_MARIP];     // velocidad de aleteo

    // Destellos
    private static final int N_BRILLOS = 16;
    private final float[] brx = new float[N_BRILLOS];
    private final float[] bry = new float[N_BRILLOS];
    private final float[] brf = new float[N_BRILLOS];
    private final float[] brs = new float[N_BRILLOS];

    public PantallaInicio(Runnable alJugar, Runnable alInstrucciones, Runnable alSalir, Runnable alMinijuegos) {

        setLayout(null);
        setOpaque(true);
        setBackground(new Color(0x2AA9E0));

        cargarFondo();

        // cuadrante: sx/sy = -1 izquierda/arriba, +1 derecha/abajo
        BotonCuarto btJugar = new BotonCuarto("JUGAR", -1, 1,
                new Color(0xFFF0CC), new Color(0x9A3A2A), 0, 0);
        BotonCuarto btInst = new BotonCuarto("INSTRUCCIONES", -1, -1,
                new Color(0xF9A3BF), new Color(0x8A3A5A), 1, 1);
        BotonCuarto btMini = new BotonCuarto("MINIJUEGOS", 1, -1,
                new Color(0xFFD56B), new Color(0x8A5A12), 3, 2);
        BotonCuarto btSalir = new BotonCuarto("SALIR", 1, 1,
                new Color(0xD9A5EC), new Color(0x5E2E80), 2, 3);

        btJugar.addActionListener(e -> alJugar.run());
        btInst.addActionListener(e -> alInstrucciones.run());
        btMini.addActionListener(e -> alMinijuegos.run());
        btSalir.addActionListener(e -> alSalir.run());

        // el orden es el orden en que aparecen
        botones = new BotonCuarto[]{btJugar, btInst, btMini, btSalir};
        for (BotonCuarto b : botones) {
            add(b);
        }

        iniciarElementos();

        reloj = new Timer(16, e -> {
            long ahora = System.nanoTime();
            double dt = Math.min(0.05, (ahora - ultimo) / 1e9);
            ultimo = ahora;
            t = (ahora - inicio) / 1e9;
            actualizar(dt);
            acomodarBotones();
            repaint();
        });

        // La animación solo corre mientras la pantalla se ve (CardLayout)
        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    inicio = System.nanoTime();   // reinicia la entrada animada
                    ultimo = inicio;
                    reloj.start();
                } else {
                    reloj.stop();
                }
            }
        });
    }

    private void cargarFondo() {
        URL url = PantallaInicio.class.getResource(RUTA_FONDO);
        if (url == null) {
            System.out.println("No se encontró la imagen de fondo: " + RUTA_FONDO);
            return;
        }
        try {
            fondoOriginal = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Ajusta la imagen para que CUBRA toda la ventana sin deformarse. */
    private BufferedImage escalarFondo(int w, int h) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (fondoOriginal == null) {
            g.setPaint(new GradientPaint(0, 0, new Color(0x8A6FE0), 0, h, new Color(0xFFB27A)));
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

    // ================================================================
    //  VIENTO, MARIPOSAS, HOJAS Y DESTELLOS
    // ================================================================
    /** Fuerza del viento (0.1 a ~1.1): sube y baja como ráfagas. */
    private double viento() {
        double v = 0.55 + 0.35 * Math.sin(t * 0.45) + 0.20 * Math.sin(t * 1.3 + 1);
        return Math.max(0.1, v);
    }

    private void iniciarElementos() {
        Color[] verdes = {
            new Color(0x5FA83A), new Color(0x7DBB3F), new Color(0x3E9A4F), new Color(0xA5D64B)
        };
        for (int i = 0; i < N_HOJAS; i++) {
            hx[i] = azar.nextInt(W);
            hy[i] = azar.nextInt(H);
            hs[i] = 18 + azar.nextFloat() * 22;
            hf[i] = azar.nextFloat() * 6.28f;
            hr[i] = azar.nextFloat() * 6.28f;
            hw[i] = 1.5f + azar.nextFloat() * 3f;
            hc[i] = verdes[azar.nextInt(verdes.length)];
        }
        for (int i = 0; i < N_MARIP; i++) {
            mx[i] = azar.nextInt(W);
            mb[i] = 120 + azar.nextInt(760);
            ms[i] = 26 + azar.nextFloat() * 34;
            mph[i] = azar.nextFloat() * 6.28f;
            msp[i] = 7 + azar.nextFloat() * 5;
        }
        for (int i = 0; i < N_BRILLOS; i++) {
            brx[i] = azar.nextInt(W);
            bry[i] = 60 + azar.nextInt(700);
            brf[i] = azar.nextFloat() * 6.28f;
            brs[i] = 6 + azar.nextFloat() * 8;
        }
    }

    private void actualizar(double dt) {
        double v = viento();

        for (int i = 0; i < N_HOJAS; i++) {
            double prof = 0.6 + hs[i] / 40.0;
            hx[i] += (90 + 260 * v) * prof * dt;
            hy[i] += (22 + Math.sin(t * 1.4 + hf[i]) * 55 * v) * dt;
            hr[i] += hw[i] * (0.4 + v) * dt;

            if (hx[i] > W + 80) {
                hx[i] = -80;
                hy[i] = azar.nextInt(H);
            }
            if (hy[i] > H + 80) {
                hy[i] = -80;
            } else if (hy[i] < -120) {
                hy[i] = H + 40;
            }
        }

        // el viento empuja a las mariposas hacia la derecha
        for (int i = 0; i < N_MARIP; i++) {
            mx[i] += (28 + 75 * v) * (0.6 + ms[i] / 60.0) * dt;
            if (mx[i] > W + 100) {
                mx[i] = -100;
                mb[i] = 120 + azar.nextInt(760);
            }
        }

        for (int i = 0; i < N_BRILLOS; i++) {
            brx[i] += 6 * dt;
            if (brx[i] > W + 20) {
                brx[i] = -20;
            }
        }
    }

    /** Resplandores del sol y de la lámpara, y destellos en el agua. */
    private void dibujarLuces(Graphics2D g) {
        // sol
        double pulso = 1 + 0.05 * Math.sin(t * 1.2);
        float R = (float) (320 * pulso);
        g.setPaint(new RadialGradientPaint(835f, 545f, R, new float[]{0f, 1f},
                new Color[]{new Color(255, 240, 170, 90), new Color(255, 240, 170, 0)}));
        g.fill(new Ellipse2D.Double(835 - R, 545 - R, 2 * R, 2 * R));

        // lámpara (parpadea suave)
        double fl = 0.85 + 0.15 * Math.sin(t * 5) * Math.sin(t * 1.7);
        float R2 = (float) (170 * fl);
        g.setPaint(new RadialGradientPaint(255f, 565f, R2, new float[]{0f, 1f},
                new Color[]{new Color(255, 210, 110, (int) (110 * fl)), new Color(255, 210, 110, 0)}));
        g.fill(new Ellipse2D.Double(255 - R2, 565 - R2, 2 * R2, 2 * R2));

        // destellos sobre el agua, bajo el sol
        for (int i = 0; i < 9; i++) {
            double w = 150 - i * 11;
            double x = 835 + Math.sin(t * 1.5 + i) * 16;
            double y = 602 + i * 24;
            int alfa = (int) (50 + 35 * Math.sin(t * 2.3 + i * 1.3));
            g.setColor(new Color(255, 236, 170, Math.max(0, Math.min(255, alfa))));
            g.fill(new RoundRectangle2D.Double(x - w / 2, y, w, 5, 5, 5));
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

            g.setColor(hc[i]);
            g.fill(hoja);
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) (-L / 2), 0, (int) (L / 2), 0);

            g.setTransform(old);
        }
    }

    /** Rayitas claras que cruzan la pantalla cuando sopla una ráfaga. */
    private void dibujarRafagas(Graphics2D g) {
        double v = Math.min(1, viento());
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double fase = (t * 0.25 * (0.6 + v) + i / 6.0) % 1.0;
            double x0 = -300 + fase * (W + 600);
            double y = 160 + i * 120 + Math.sin(t + i * 1.7) * 20;
            int alfa = (int) (60 * Math.sin(fase * Math.PI) * v);
            if (alfa <= 0) {
                continue;
            }
            g.setColor(new Color(255, 255, 255, alfa));
            Path2D p = new Path2D.Double();
            p.moveTo(x0, y);
            p.curveTo(x0 + 90, y - 28, x0 + 170, y + 28, x0 + 260, y - 6);
            g.draw(p);
        }
    }

    private void dibujarBrillos(Graphics2D g) {
        AffineTransform old = g.getTransform();
        for (int i = 0; i < N_BRILLOS; i++) {
            double tw = (Math.sin(t * 2 + brf[i]) + 1) / 2;
            double r = brs[i] * (0.5 + 0.7 * tw);
            g.translate(brx[i], bry[i] + Math.sin(t + brf[i]) * 8);
            g.setColor(new Color(255, 246, 190, (int) (60 + 190 * tw)));
            Path2D s = new Path2D.Double();
            s.moveTo(0, -r);
            s.quadTo(0, 0, r, 0);
            s.quadTo(0, 0, 0, r);
            s.quadTo(0, 0, -r, 0);
            s.quadTo(0, 0, 0, -r);
            g.fill(s);
            g.setTransform(old);
        }
    }

    private void dibujarMariposas(Graphics2D g) {
        for (int i = 0; i < N_MARIP; i++) {
            double y = mb[i] + Math.sin(t * 0.9 + mph[i]) * 45 + Math.cos(t * 0.5 + mph[i] * 2) * 25;
            double rot = Math.sin(t * 0.9 + mph[i]) * 0.35;
            dibujarMariposa(g, mx[i], y, ms[i], t * msp[i] + mph[i], rot);
        }
    }

    private void dibujarMariposa(Graphics2D g, double x, double y, double s, double fase, double rot) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);

        // resplandor
        float rg = (float) (s * 1.5);
        g.setPaint(new RadialGradientPaint(0f, 0f, rg, new float[]{0f, 1f},
                new Color[]{new Color(255, 235, 120, 110), new Color(255, 235, 120, 0)}));
        g.fill(new Ellipse2D.Double(-rg, -rg, rg * 2, rg * 2));

        double f = 0.25 + 0.75 * Math.abs(Math.sin(fase));     // aleteo
        for (int lado = -1; lado <= 1; lado += 2) {
            AffineTransform sv = g.getTransform();
            g.scale(lado * f, 1);

            Path2D sup = new Path2D.Double();
            sup.moveTo(0, -s * 0.05);
            sup.curveTo(s * 0.2, -s * 1.0, s * 1.0, -s * 0.95, s * 0.95, -s * 0.25);
            sup.curveTo(s * 0.9, s * 0.05, s * 0.4, s * 0.1, 0, s * 0.05);

            Path2D inf = new Path2D.Double();
            inf.moveTo(0, s * 0.05);
            inf.curveTo(s * 0.5, 0, s * 0.8, s * 0.3, s * 0.6, s * 0.65);
            inf.curveTo(s * 0.4, s * 0.9, s * 0.1, s * 0.6, 0, s * 0.1);

            g.setColor(new Color(0xFFE94D));
            g.fill(sup);
            g.fill(inf);
            g.setColor(new Color(0xE8A317));
            g.setStroke(new BasicStroke((float) Math.max(2, s * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sup);
            g.draw(inf);
            g.setColor(new Color(255, 255, 255, 130));
            g.fill(new Ellipse2D.Double(s * 0.35, -s * 0.78, s * 0.3, s * 0.15));

            g.setTransform(sv);
        }

        // cuerpo y antenas
        g.setColor(new Color(0x7A4A1E));
        g.fill(new Ellipse2D.Double(-s * 0.05, -s * 0.28, s * 0.1, s * 0.62));
        g.setStroke(new BasicStroke((float) Math.max(1.5, s * 0.03), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, -s * 0.28, -s * 0.15, -s * 0.5));
        g.draw(new Line2D.Double(0, -s * 0.28, s * 0.15, -s * 0.5));

        g.setTransform(old);
    }

    // ================================================================
    //  BOTONES: la rueda aparece y flota
    // ================================================================
    private double escalaUI() {
        return Math.min(getWidth() / (double) W, getHeight() / (double) H);
    }

    private void acomodarBotones() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double us = escalaUI();
        double ox = (getWidth() - W * us) / 2.0;
        double oy = (getHeight() - H * us) / 2.0;

        int lado = (int) Math.round(LADO * us);
        Rectangle r = new Rectangle(
                (int) Math.round(ox + (RUEDA_X - LADO / 2) * us),
                (int) Math.round(oy + (RUEDA_Y - LADO / 2) * us),
                lado, lado);

        flotarRueda = Math.sin(t * 1.3) * 6;

        for (int i = 0; i < botones.length; i++) {
            double p = clamp((t - 0.5 - i * 0.12) / 0.6);
            botones[i].aparicion = easeOutBack(p);
            botones[i].flotar = flotarRueda;
            botones[i].setEnabled(p >= 1);
            if (!r.equals(botones[i].getBounds())) {
                botones[i].setBounds(r);
            }
        }
    }

    // ================================================================
    //  DIBUJO
    // ================================================================
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

        // imagen de fondo
        Graphics2D base = (Graphics2D) g0.create();
        activarCalidad(base);
        base.drawImage(fondoEscalado, 0, 0, null);
        base.dispose();

        // luces, viento, hojas y destellos
        Graphics2D g = graficosFondo(g0);
        dibujarLuces(g);
        dibujarRafagas(g);
        dibujarHojas(g);
        dibujarBrillos(g);
        g.dispose();

        // aro de madera de la rueda y cartel del título
        Graphics2D u = graficosUI(g0);
        dibujarAro(u);
        dibujarCartel(u);
        u.dispose();
    }

    /** Lo que va ENCIMA de los botones: flores, hojitas, flor central y mariposas. */
    @Override
    protected void paintChildren(Graphics g0) {
        super.paintChildren(g0);

        Graphics2D u = graficosUI(g0);
        dibujarDecoracionRueda(u);
        u.dispose();

        Graphics2D g = graficosFondo(g0);
        dibujarMariposas(g);
        g.dispose();
    }

    private static void activarCalidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // ---------------- ARO DE LA RUEDA ----------------
    private void dibujarAro(Graphics2D g) {
        double e = easeOutBack(clamp((t - 0.3) / 0.7));
        if (e <= 0.01) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(RUEDA_X, RUEDA_Y + flotarRueda);
        g.scale(e, e);

        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(-R_ARO + 8, -R_ARO + 18, 2 * R_ARO, 2 * R_ARO));

        Ellipse2D aro = new Ellipse2D.Double(-R_ARO, -R_ARO, 2 * R_ARO, 2 * R_ARO);
        g.setPaint(new GradientPaint(0, (float) -R_ARO, new Color(0xD9954F), 0, (float) R_ARO, new Color(0xA5622E)));
        g.fill(aro);
        g.setColor(new Color(0x6B3A1E));
        g.setStroke(new BasicStroke(9f));
        g.draw(aro);

        g.setColor(new Color(0xF2C98A));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Ellipse2D.Double(-R_ARO + 12, -R_ARO + 12, 2 * R_ARO - 24, 2 * R_ARO - 24));

        // fondo oscuro que se ve por las separaciones entre cuartos
        g.setColor(new Color(0x8E5226));
        double rf = R_CUARTO + 6;
        g.fill(new Ellipse2D.Double(-rf, -rf, 2 * rf, 2 * rf));

        g.setTransform(old);
    }

    private void dibujarDecoracionRueda(Graphics2D g) {
        double e = easeOutBack(clamp((t - 0.9) / 0.6));
        if (e <= 0.01) {
            return;
        }
        g.translate(0, flotarRueda);

        racimo(g, 1285, 262, 1.0 * e, 0.0, true);
        racimo(g, 1535, 248, 0.9 * e, 1.7, false);
        racimo(g, 1194, 861, 0.8 * e, 3.1, true);

        double pulso = 1 + 0.04 * Math.sin(t * 2.2);
        dibujarFlor(g, RUEDA_X, RUEDA_Y, 52 * e * pulso, t * 0.25,
                new Color(0xFFF4E2), new Color(0xFFD34D));
    }

    // ---------------- CARTEL DEL TÍTULO ----------------
    private void dibujarCartel(Graphics2D g) {
        double pop = easeOutBack(clamp(t / 0.8));
        if (pop <= 0.01) {
            return;
        }
        double bob = Math.sin(t * 1.6) * 5;
        double giro = Math.sin(t * 1.1) * 0.012;

        AffineTransform old = g.getTransform();
        g.translate(590, 100 + bob);        // cuelga del borde de arriba
        g.rotate(giro);
        g.translate(0, 160);
        g.scale(pop, pop);
        g.translate(-590, -260);

        dibujarTabla(g);
        racimo(g, 215, 150, 1.0, 0.5, true);
        racimo(g, 200, 335, 1.1, 2.0, true);
        racimo(g, 975, 295, 1.0, 3.5, true);
        racimo(g, 725, 100, 1.1, 4.8, true);

        dibujarLetras(g, "TODOS HABLAN", 590, 235, 150, 720,
                new Color(0xFFF8E1), new Color(0xFFD9A0), 0.5);
        dibujarLetras(g, "EN MACONDO", 590, 372, 140, 680,
                new Color(0xFFC2D6), new Color(0xFF6FA0), 0.9);

        dibujarCinta(g);
        dibujarCorona(g);

        g.setTransform(old);
    }

    private void dibujarTabla(Graphics2D g) {
        RoundRectangle2D tabla = new RoundRectangle2D.Double(190, 90, 800, 330, 70, 70);

        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(190, 104, 800, 330, 70, 70));

        g.setPaint(new GradientPaint(0, 90, new Color(0xBC7A48), 0, 420, new Color(0x8A5129)));
        g.fill(tabla);

        Shape clipViejo = g.getClip();
        g.clip(tabla);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x5E3216).getRed() == 0 ? Color.BLACK : new Color(94, 50, 22, 110));
        for (int i = 0; i < 4; i++) {
            Path2D v = new Path2D.Double();
            double y0 = 150 + i * 75;
            v.moveTo(190, y0);
            for (int x = 190; x <= 990; x += 20) {
                v.lineTo(x, y0 + Math.sin(x * 0.02 + i) * 5);
            }
            g.draw(v);
        }
        g.setColor(new Color(255, 255, 255, 45));
        g.fillRect(190, 90, 800, 90);
        g.setClip(clipViejo);

        g.setColor(new Color(0x4B2812));
        g.setStroke(new BasicStroke(8f));
        g.draw(tabla);

        // clavos
        double[][] clavos = {{235, 135}, {945, 135}, {235, 375}, {945, 375}};
        for (double[] c : clavos) {
            g.setColor(new Color(0x4B2812));
            g.fill(new Ellipse2D.Double(c[0] - 9, c[1] - 9, 18, 18));
            g.setColor(new Color(0xD9A06A));
            g.fill(new Ellipse2D.Double(c[0] - 5, c[1] - 6, 8, 8));
        }
    }

    private void dibujarCorona(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.translate(520, 92);
        double esc = 1 + 0.03 * Math.sin(t * 3);
        g.scale(esc, esc);
        g.rotate(Math.sin(t * 1.4) * 0.04);

        Path2D c = new Path2D.Double();
        c.moveTo(-70, 30);
        c.lineTo(-78, -22);
        c.lineTo(-38, 6);
        c.lineTo(0, -46);
        c.lineTo(38, 6);
        c.lineTo(78, -22);
        c.lineTo(70, 30);
        c.closePath();

        g.setPaint(new GradientPaint(0, -46, new Color(0xFFE680), 0, 30, new Color(0xF2B21C)));
        g.fill(c);
        g.setColor(new Color(0x7A4E10));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(c);

        g.setColor(new Color(0xE0A010));
        g.fill(new RoundRectangle2D.Double(-68, 14, 136, 16, 8, 8));
        g.setColor(new Color(0x7A4E10));
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(-68, 14, 136, 16, 8, 8));

        double[][] joyas = {{-78, -22}, {0, -46}, {78, -22}};
        for (double[] j : joyas) {
            g.setColor(new Color(0xFFF2A8));
            g.fill(new Ellipse2D.Double(j[0] - 8, j[1] - 8, 16, 16));
            g.setColor(new Color(0x7A4E10));
            g.setStroke(new BasicStroke(4f));
            g.draw(new Ellipse2D.Double(j[0] - 8, j[1] - 8, 16, 16));
        }
        g.setTransform(old);
    }

    /** Letras que aparecen una por una y después se mecen cada una por su lado. */
    private void dibujarLetras(Graphics2D g, String txt, double cx, double baseY, int tam,
            double maxW, Color c1, Color c2, double demora) {

        Font f = new Font(FUENTE, ESTILO, tam);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), txt);
        double ancho = gv.getVisualBounds().getWidth();
        if (ancho > maxW) {
            f = f.deriveFont((float) (tam * maxW / ancho));
            gv = f.createGlyphVector(g.getFontRenderContext(), txt);
        }
        Rectangle2D vb = gv.getVisualBounds();
        double x0 = cx - vb.getCenterX();

        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            Shape gs = gv.getGlyphOutline(i);
            Rectangle2D b = gs.getBounds2D();
            if (b.isEmpty()) {
                continue;
            }
            double p = clamp((t - demora - i * 0.07) / 0.5);
            if (p <= 0) {
                continue;
            }
            double e = easeOutBack(p);
            double dy = Math.sin(t * 2.6 + i * 0.7 + demora * 5) * 7;
            double rot = Math.sin(t * 2.0 + i) * 0.03;

            AffineTransform at = new AffineTransform();
            at.translate(x0, baseY + dy);
            at.rotate(rot, b.getCenterX(), b.getCenterY());
            at.translate(b.getCenterX(), b.getCenterY());
            at.scale(e, e);
            at.translate(-b.getCenterX(), -b.getCenterY());
            Shape sh = at.createTransformedShape(gs);

            // sombra
            Shape sombra = AffineTransform.getTranslateInstance(0, 10).createTransformedShape(sh);
            g.setColor(new Color(0, 0, 0, 70));
            g.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sombra);
            g.fill(sombra);

            // contorno
            g.setColor(new Color(0x5A2D1A));
            g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sh);

            // relleno
            Rectangle2D bb = sh.getBounds2D();
            g.setPaint(new GradientPaint(0, (float) bb.getMinY(), c1, 0, (float) bb.getMaxY(), c2));
            g.fill(sh);

            // brillo
            Shape clipViejo = g.getClip();
            g.clip(sh);
            g.setColor(new Color(255, 255, 255, 80));
            g.fill(new Rectangle2D.Double(bb.getX(), bb.getY(), bb.getWidth(), bb.getHeight() * 0.40));
            g.setClip(clipViejo);
        }
    }

    private void dibujarCinta(Graphics2D g) {
        double e = easeOutBack(clamp((t - 1.3) / 0.5));
        if (e <= 0.01) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(590, 418);
        g.scale(e, e);
        g.rotate(Math.sin(t * 1.8) * 0.01);

        RoundRectangle2D cinta = new RoundRectangle2D.Double(-260, -30, 520, 60, 30, 30);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(-260, -20, 520, 60, 30, 30));
        g.setPaint(new GradientPaint(0, -30, new Color(0xF3C88A), 0, 30, new Color(0xD79A58)));
        g.fill(cinta);
        g.setColor(new Color(0x7A4524));
        g.setStroke(new BasicStroke(6f));
        g.draw(cinta);

        // estrellitas en los extremos
        for (int lado = -1; lado <= 1; lado += 2) {
            estrella(g, lado * 222, 0, 15, 7, t * lado * 0.8,
                    new Color(0xFFD34D), new Color(0xB5760F));
        }

        Font f = new Font(FUENTE, ESTILO, 40);
        Shape s = f.createGlyphVector(g.getFontRenderContext(), "USO SALUDABLE DE LAS REDES").getOutline();
        Rectangle2D b = s.getBounds2D();
        double esc = Math.min(1.0, 360 / b.getWidth());
        AffineTransform at = new AffineTransform();
        at.scale(esc, esc);
        at.translate(-b.getCenterX(), -b.getCenterY());
        g.setColor(new Color(0x4A2410));
        g.fill(at.createTransformedShape(s));

        g.setTransform(old);
    }

    // ---------------- DECORACIÓN: flores, hojas, estrellas ----------------
    private void estrella(Graphics2D g, double x, double y, double R, double r, double rot,
            Color relleno, Color borde) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double ang = rot + i * Math.PI / 5 - Math.PI / 2;
            double rad = (i % 2 == 0) ? R : r;
            double px = x + Math.cos(ang) * rad;
            double py = y + Math.sin(ang) * rad;
            if (i == 0) {
                p.moveTo(px, py);
            } else {
                p.lineTo(px, py);
            }
        }
        p.closePath();
        g.setColor(relleno);
        g.fill(p);
        g.setColor(borde);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }

    private void dibujarFlor(Graphics2D g, double x, double y, double r, double rot,
            Color petalo, Color centro) {
        if (r <= 1) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        for (int i = 0; i < 5; i++) {
            g.rotate(2 * Math.PI / 5);
            Ellipse2D p = new Ellipse2D.Double(-r * 0.38, -r, r * 0.76, r * 0.9);
            g.setColor(petalo);
            g.fill(p);
            g.setColor(new Color(0xD9A27A));
            g.setStroke(new BasicStroke((float) Math.max(2, r * 0.07)));
            g.draw(p);
        }
        g.setColor(centro);
        g.fill(new Ellipse2D.Double(-r * 0.34, -r * 0.34, r * 0.68, r * 0.68));
        g.setColor(new Color(0xD98A1A));
        g.setStroke(new BasicStroke((float) Math.max(2, r * 0.07)));
        g.draw(new Ellipse2D.Double(-r * 0.34, -r * 0.34, r * 0.68, r * 0.68));
        g.setTransform(old);
    }

    private void dibujarHojaDeco(Graphics2D g, double len) {
        Path2D h = new Path2D.Double();
        h.moveTo(0, 0);
        h.quadTo(len * 0.5, -len * 0.42, len, 0);
        h.quadTo(len * 0.5, len * 0.42, 0, 0);
        g.setColor(new Color(0x5BAA2E));
        g.fill(h);
        g.setColor(new Color(0x2F6A18));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(h);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(len * 0.1, 0, len * 0.85, 0));
    }

    /** Un grupito de hojas (y a veces una flor) que se mece con el viento. */
    private void racimo(Graphics2D g, double x, double y, double esc, double fase, boolean flor) {
        if (esc <= 0.01) {
            return;
        }
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.scale(esc, esc);
        double[] ang = {-150, -100, -40, 10};
        for (int i = 0; i < ang.length; i++) {
            AffineTransform sv = g.getTransform();
            g.rotate(Math.toRadians(ang[i]) + Math.sin(t * 1.5 + fase + i) * 0.08);
            dibujarHojaDeco(g, 62 - i * 4);
            g.setTransform(sv);
        }
        if (flor) {
            dibujarFlor(g, 0, 0, 30 * (1 + 0.04 * Math.sin(t * 2 + fase)),
                    Math.sin(t * 1.2 + fase) * 0.2, new Color(0xFFF4E2), new Color(0xFFD34D));
        }
        g.setTransform(old);
    }

    // ================================================================
    //  UTILIDADES
    // ================================================================
    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double easeOutBack(double p) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2);
    }

    private static Color mezclar(Color a, Color b, float p) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * p),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * p),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * p));
    }

    // ================================================================
    //  BOTÓN EN FORMA DE CUARTO DE RUEDA (con efecto hundido)
    // ================================================================
    private class BotonCuarto extends JButton {

        private final int sx;               // -1 izquierda, 1 derecha
        private final int sy;               // -1 arriba, 1 abajo
        private final int icono;            // 0 play, 1 libro, 2 X, 3 dado
        private final int numero;
        private final Color color;
        private final Color tinta;
        private final Shape forma;

        private boolean sobre = false;
        private boolean presionado = false;
        private float hover = 0f;
        private float hundido = 0f;
        private final Timer anim;

        double aparicion = 0;               // 0..1: lo controla el panel (animación de entrada)
        double flotar = 0;                  // lo controla el panel (la rueda sube y baja)

        BotonCuarto(String texto, int sx, int sy, Color color, Color tinta, int icono, int numero) {
            super(texto);
            this.sx = sx;
            this.sy = sy;
            this.color = color;
            this.tinta = tinta;
            this.icono = icono;
            this.numero = numero;
            this.forma = AffineTransform.getScaleInstance(sx, sy).createTransformedShape(CUARTO);

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursores.MANO);

            anim = new Timer(16, e -> {
                float th = sobre ? 1f : 0f;
                float tp = presionado ? 1f : 0f;
                hover += (th - hover) * 0.25f;
                hundido += (tp - hundido) * 0.45f;
                if (Math.abs(th - hover) < 0.01f && Math.abs(tp - hundido) < 0.01f) {
                    hover = th;
                    hundido = tp;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    sobre = true;
                    anim.start();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    sobre = false;
                    presionado = false;
                    anim.start();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        presionado = true;
                        anim.start();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    presionado = false;
                    anim.start();
                }
            });
        }

        /** Solo cuenta como "dentro del botón" el cuarto de círculo, no todo el cuadro. */
        @Override
        public boolean contains(int x, int y) {
            if (aparicion < 0.3 || getWidth() <= 0) {
                return false;
            }
            double k = getWidth() / LADO;
            double lx = (x / k - LADO / 2) / aparicion;
            double ly = (y / k - LADO / 2 - flotar) / aparicion;
            return forma.contains(lx, ly);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            double a = aparicion;
            if (a <= 0.01 || getWidth() <= 0) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            activarCalidad(g);

            double k = getWidth() / LADO;
            g.scale(k, k);
            g.translate(LADO / 2, LADO / 2 + flotar);
            g.scale(a, a);

            double desp = PROF * hundido;                       // cuánto baja la cara
            double alza = -5 * hover * (1 - hundido);           // sube un poquito con el mouse
            double popX = sx * 6 * hover * (1 - hundido);       // se asoma hacia afuera
            double popY = sy * 6 * hover * (1 - hundido);

            // sombra
            g.setColor(new Color(0, 0, 0, Math.round(55 * (1 - hundido * 0.7f))));
            g.fill(AffineTransform.getTranslateInstance(popX + 4, popY + PROF + 10).createTransformedShape(forma));

            // base (lo que se ve debajo de la cara cuando no está presionado)
            Shape base = AffineTransform.getTranslateInstance(popX, popY + PROF).createTransformedShape(forma);
            g.setColor(mezclar(color, TEXTO, 0.45f));
            g.fill(base);
            g.setColor(BORDE);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(base);

            // cara
            double fx = popX;
            double fy = popY + desp + alza;
            Shape cara = AffineTransform.getTranslateInstance(fx, fy).createTransformedShape(forma);
            Rectangle2D bb = cara.getBounds2D();
            Color c1 = mezclar(color, Color.WHITE, 0.40f + 0.12f * hover);
            Color c2 = mezclar(color, tinta, 0.10f);
            g.setPaint(new GradientPaint(0, (float) bb.getMinY(), c1, 0, (float) bb.getMaxY(), c2));
            g.fill(cara);

            // brillo interior y migajitas de "galleta"
            Shape clipViejo = g.getClip();
            g.clip(cara);
            g.setColor(new Color(255, 255, 255, 90));
            g.setStroke(new BasicStroke(14f));
            g.draw(cara);
            g.setColor(new Color(tinta.getRed(), tinta.getGreen(), tinta.getBlue(), 60));
            double[][] migas = {{60, 80}, {250, 60}, {120, 255}, {215, 150}, {290, 250}};
            for (double[] m : migas) {
                g.fill(new Ellipse2D.Double(fx + sx * m[0] - 5, fy + sy * m[1] - 5, 10, 10));
            }
            g.setClip(clipViejo);

            // se oscurece al presionar
            if (hundido > 0.01f) {
                g.setColor(new Color(0, 0, 0, Math.round(40 * hundido)));
                g.fill(cara);
            }

            g.setColor(BORDE);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(cara);

            // contenido (se mueve junto con la cara)
            boolean arriba = sy < 0;
            double tx = sx * (arriba ? 170 : 140);
            double iy = arriba ? -185 : 120;
            double ty = arriba ? -118 : 202;
            double maxW = arriba ? 270 : 230;

            AffineTransform sv = g.getTransform();
            g.translate(fx, fy);

            // icono con un balanceo suave
            g.translate(tx, iy);
            g.rotate(Math.sin(t * 2 + numero) * 0.08);
            double esc = 1 + 0.04 * Math.sin(t * 3 + numero);
            g.scale(esc, esc);
            dibujarIcono(g, 88);
            g.setTransform(sv);

            // texto
            g.translate(fx, fy);
            Font f = new Font(FUENTE_BTN, Font.BOLD, 36);
            Shape s0 = f.createGlyphVector(g.getFontRenderContext(), getText()).getOutline();
            Rectangle2D b = s0.getBounds2D();
            double escT = Math.min(1.0, maxW / b.getWidth());
            AffineTransform at = new AffineTransform();
            at.translate(tx, ty);
            at.scale(escT, escT);
            at.translate(-b.getCenterX(), -b.getCenterY());
            g.setColor(TEXTO);
            g.fill(at.createTransformedShape(s0));

            g.dispose();
        }

        /** Icono centrado en (0,0). */
        private void dibujarIcono(Graphics2D g, double s) {
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (icono == 0) {                                   // play
                Path2D tri = new Path2D.Double();
                tri.moveTo(-0.28 * s, -0.5 * s);
                tri.lineTo(-0.28 * s, 0.5 * s);
                tri.lineTo(0.5 * s, 0);
                tri.closePath();
                g.setColor(new Color(0xF57C6E));
                g.fill(tri);
                g.setColor(tinta);
                g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(tri);

            } else if (icono == 1) {                            // libro
                for (int lado = -1; lado <= 1; lado += 2) {
                    Path2D pag = new Path2D.Double();
                    pag.moveTo(lado * 0.50 * s, -0.30 * s);
                    pag.lineTo(lado * 0.02 * s, -0.18 * s);
                    pag.lineTo(lado * 0.02 * s, 0.40 * s);
                    pag.lineTo(lado * 0.50 * s, 0.28 * s);
                    pag.closePath();
                    g.setColor(new Color(0xFFE8EE));
                    g.fill(pag);
                    g.setColor(tinta);
                    g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(pag);
                    g.setColor(new Color(tinta.getRed(), tinta.getGreen(), tinta.getBlue(), 150));
                    g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 3; i++) {
                        double yy = -0.12 * s + i * 0.15 * s;
                        g.draw(new Line2D.Double(lado * 0.12 * s, yy, lado * 0.40 * s, yy + 0.04 * s));
                    }
                }

            } else if (icono == 3) {                            // dado
                g.rotate(-0.17);
                RoundRectangle2D d = new RoundRectangle2D.Double(-0.4 * s, -0.4 * s, 0.8 * s, 0.8 * s, 0.2 * s, 0.2 * s);
                g.setColor(new Color(0xFFF8F0));
                g.fill(d);
                g.setColor(new Color(0x6B3A1E));
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(d);
                double r = 0.07 * s;
                double[][] puntos = {{-0.2, -0.2}, {0.2, -0.2}, {0, 0}, {-0.2, 0.2}, {0.2, 0.2}};
                for (double[] p : puntos) {
                    g.fill(new Ellipse2D.Double(p[0] * s - r, p[1] * s - r, r * 2, r * 2));
                }

            } else {                                            // X
                double m = 0.34 * s;
                g.setColor(tinta);
                g.setStroke(new BasicStroke((float) (0.34 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(-m, -m, m, m));
                g.draw(new Line2D.Double(m, -m, -m, m));
                g.setColor(new Color(0xC27FE6));
                g.setStroke(new BasicStroke((float) (0.20 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(-m, -m, m, m));
                g.draw(new Line2D.Double(m, -m, -m, m));
            }
        }
    }
}