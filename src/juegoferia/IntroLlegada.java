package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Prólogo en una sola clase: transición desde la pantalla de nombre,
 * noche en la isla, Ringo narrando con efecto máquina de escribir y
 * la protagonista cayendo a la playa. Diseño lógico 1920 x 1080.
 *
 * Uso:
 *   introLlegada.iniciar(nombreJugador, fotoPantallaAnterior, () -> ...lo que sigue...);
 */
public class IntroLlegada extends JPanel {

    private static final int W = 1920;
    private static final int H = 1080;

    // ===== TEXTOS DE RINGO (edítalos a tu gusto). {nombre} = nombre del jugador =====
    private static final String NARRADOR = "Ringo";
    private static final String[] LINEAS = {
        "Dicen que esta isla no aparece en ningún mapa.",
        "Me llamo Ringo, y llevo aquí más años que los faroles.",
        "Esta noche, {nombre}, el mar te trajo a ti.",
        "Levántate. Aquí las noticias viajan rápido… y no todas dicen la verdad."
    };
    private static final int LINEA_CAIDA = 2;       // en qué línea cae la protagonista
    private static final double VELOCIDAD = 34;     // letras por segundo

    private static final String RUTA_PERSONAJE = "/imagenes/personajecivil.png";
    private static final String RUTA_RINGO = "/imagenes/ringo.png";   // opcional: PNG del chigüiro

    private static final Color MARRON = new Color(0x8A4F25);
    private static final Color TINTA = new Color(0x5A2D1A);
    private static final Font FUENTE_TEXTO = new Font("Trebuchet MS", Font.PLAIN, 28);
    private static final Font FUENTE_NOMBRE = new Font("Trebuchet MS", Font.BOLD, 26);
    private static final Font FUENTE_HINT = new Font("Trebuchet MS", Font.PLAIN, 16);

    // ---------- Estado ----------
    private final Timer reloj;
    private long inicio;
    private double ahora;
    private boolean activo;
    private int fase;                 // 0 foto->negro, 1 aparece la noche, 2 narración, 3 salida
    private double inicioFase;
    private String nombre = "viajero";
    private BufferedImage foto;
    private Runnable alTerminar;

    private int linea = -1;
    private double inicioLinea;
    private double inicioCaja;
    private String[] renglones = new String[0];
    private double[] tiempos = new double[0];
    private String textoLinea = "";
    private double tCaida = -1;

    private BufferedImage personaje;
    private BufferedImage ringo;

    private static final int N_ESTRELLAS = 90;
    private final float[] ex = new float[N_ESTRELLAS];
    private final float[] ey = new float[N_ESTRELLAS];
    private final float[] ef = new float[N_ESTRELLAS];
    private final float[] es = new float[N_ESTRELLAS];

    public IntroLlegada() {
        setOpaque(true);
        setBackground(Color.BLACK);

        personaje = cargar(RUTA_PERSONAJE, true);
        ringo = cargar(RUTA_RINGO, false);

        Random azar = new Random(5);
        for (int i = 0; i < N_ESTRELLAS; i++) {
            ex[i] = azar.nextInt(W);
            ey[i] = azar.nextInt(430);
            ef[i] = azar.nextFloat() * 6.28f;
            es[i] = 1.5f + azar.nextFloat() * 2.5f;
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                avanzar();
            }
        });

        AbstractAction accionAvanzar = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                avanzar();
            }
        };
        AbstractAction accionSaltar = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saltar();
            }
        };
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "avanzar");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "avanzar");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "saltar");
        getActionMap().put("avanzar", accionAvanzar);
        getActionMap().put("saltar", accionSaltar);

        reloj = new Timer(16, e -> {
            actualizar();
            repaint();
        });

        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                reloj.stop();
            }
        });
    }

    private BufferedImage cargar(String ruta, boolean avisar) {
        URL url = IntroLlegada.class.getResource(ruta);
        if (url == null) {
            if (avisar) {
                System.out.println("No se encontró la imagen: " + ruta);
            }
            return null;
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Arranca el prólogo.
     *
     * @param foto "foto" de la pantalla anterior para fundirla a negro (puede ser null)
     */
    public void iniciar(String nombreJugador, BufferedImage foto, Runnable alTerminar) {
        this.nombre = (nombreJugador == null || nombreJugador.trim().isEmpty()) ? "viajero" : nombreJugador.trim();
        this.foto = foto;
        this.alTerminar = alTerminar;
        inicio = System.nanoTime();
        ahora = 0;
        fase = 0;
        inicioFase = 0;
        linea = -1;
        tCaida = -1;
        renglones = new String[0];
        tiempos = new double[0];
        activo = true;
        requestFocusInWindow();
        reloj.start();
    }

    // ================================================================
    //  LÓGICA
    // ================================================================
    private void actualizar() {
        ahora = (System.nanoTime() - inicio) / 1e9;
        double d = ahora - inicioFase;

        if (fase == 0 && d >= 0.7) {
            fase = 1;
            inicioFase = ahora;
        } else if (fase == 1 && d >= 1.6) {
            fase = 2;
            inicioFase = ahora;
            inicioCaja = ahora;
            empezarLinea(0);
        } else if (fase == 3 && d >= 0.9) {
            terminar();
        }
    }

    private void empezarLinea(int i) {
        linea = i;
        inicioLinea = ahora;
        textoLinea = LINEAS[i].replace("{nombre}", nombre);
        renglones = envolver(textoLinea, FUENTE_TEXTO, 520);

        tiempos = new double[textoLinea.length()];
        double acum = 0;
        for (int k = 0; k < textoLinea.length(); k++) {
            acum += 1.0 / VELOCIDAD;
            tiempos[k] = acum;
            char c = textoLinea.charAt(k);
            if (c == ',' || c == ';') {
                acum += 0.16;
            } else if (c == '.' || c == '?' || c == '!' || c == '…') {
                acum += 0.32;
            }
        }
        if (i == LINEA_CAIDA) {
            tCaida = ahora;
        }
    }

    private boolean lineaCompleta() {
        return tiempos.length == 0 || ahora - inicioLinea >= tiempos[tiempos.length - 1];
    }

    private void avanzar() {
        if (!activo || fase != 2 || ahora - inicioCaja < 0.5) {
            return;
        }
        if (!lineaCompleta()) {
            inicioLinea = ahora - 999;          // muestra toda la línea de golpe
            return;
        }
        if (linea + 1 >= LINEAS.length) {
            fase = 3;
            inicioFase = ahora;
        } else {
            empezarLinea(linea + 1);
        }
    }

    private void saltar() {
        if (activo && fase < 3) {
            fase = 3;
            inicioFase = ahora;
        }
    }

    private void terminar() {
        if (!activo) {
            return;
        }
        activo = false;
        reloj.stop();
        foto = null;
        Runnable r = alTerminar;
        alTerminar = null;
        if (r != null) {
            r.run();
        }
    }

    private static String[] envolver(String texto, Font f, int ancho) {
        BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        FontMetrics fm = g.getFontMetrics(f);
        List<String> lineas = new ArrayList<>();
        String actual = "";
        for (String palabra : texto.split(" ")) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) > ancho && !actual.isEmpty()) {
                lineas.add(actual);
                actual = palabra;
            } else {
                actual = prueba;
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual);
        }
        g.dispose();
        return lineas.toArray(new String[0]);
    }

    // ================================================================
    //  DIBUJO
    // ================================================================
    private static void activarCalidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private Graphics2D graficos(Graphics g0, boolean cubrir) {
        int w = getWidth();
        int h = getHeight();
        double esc = cubrir ? Math.max(w / (double) W, h / (double) H)
                : Math.min(w / (double) W, h / (double) H);
        Graphics2D g = (Graphics2D) g0.create();
        activarCalidad(g);
        g.translate((w - W * esc) / 2.0, (h - H * esc) / 2.0);
        g.scale(esc, esc);
        return g;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g0.create();
        activarCalidad(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        // Fase 0: la pantalla anterior se funde a negro
        if (fase == 0) {
            if (foto != null) {
                float a = (float) (1 - clamp((ahora - inicioFase) / 0.7));
                g.setComposite(AlphaComposite.SrcOver.derive(a));
                g.drawImage(foto, 0, 0, w, h, null);
            }
            g.dispose();
            return;
        }

        // temblor al aterrizar
        double aterr = tCaida < 0 ? -1 : ahora - (tCaida + 1.25);
        double temblor = (aterr >= 0 && aterr < 0.5) ? 10 * Math.exp(-9 * aterr) * Math.sin(aterr * 70) : 0;

        Graphics2D s = graficos(g0, true);
        s.translate(0, temblor);
        dibujarEscena(s);
        dibujarPersonaje(s);
        s.dispose();

        if (fase >= 2) {
            Graphics2D u = graficos(g0, false);
            dibujarCaja(u);
            u.dispose();
        }

        // cortina negra (entrada y salida)
        double d = ahora - inicioFase;
        double negro = 0;
        if (fase == 1) {
            negro = 1 - clamp(d / 1.6);
        } else if (fase == 3) {
            negro = clamp(d / 0.9);
        }
        if (negro > 0.001) {
            g.setColor(new Color(0, 0, 0, (int) Math.round(255 * negro)));
            g.fillRect(0, 0, w, h);
        }
        g.dispose();
    }

    // ---------------- Escena nocturna ----------------
    private void dibujarEscena(Graphics2D g) {
        // cielo
        g.setPaint(new GradientPaint(0, 0, new Color(0x0A1038), 0, 430, new Color(0x23469C)));
        g.fill(new Rectangle2D.Double(-80, -80, W + 160, 540));

        // estrellas
        for (int i = 0; i < N_ESTRELLAS; i++) {
            double tw = (Math.sin(ahora * 1.6 + ef[i]) + 1) / 2;
            double r = es[i] * (0.5 + 0.5 * tw);
            g.setColor(new Color(255, 255, 255, (int) (80 + 150 * tw)));
            g.fill(new Ellipse2D.Double(ex[i] - r, ey[i] - r, r * 2, r * 2));
        }

        // luna
        g.setPaint(new RadialGradientPaint(1570f, 75f, 190f, new float[]{0f, 1f},
                new Color[]{new Color(255, 243, 192, 120), new Color(255, 243, 192, 0)}));
        g.fill(new Ellipse2D.Double(1570 - 190, 75 - 190, 380, 380));
        g.setColor(new Color(0xFFF3C0));
        g.fill(new Ellipse2D.Double(1570 - 58, 75 - 58, 116, 116));
        g.setColor(new Color(0xF0DC9C));
        g.fill(new Ellipse2D.Double(1545, 55, 22, 22));
        g.fill(new Ellipse2D.Double(1585, 92, 16, 16));

        // nubes lentas
        g.setColor(new Color(0x1B2F7A, false));
        for (int i = 0; i < 3; i++) {
            double x = ((i * 700 + ahora * 12) % (W + 400)) - 200;
            double y = 120 + i * 70;
            g.setColor(new Color(27, 47, 122, 150));
            g.fill(new Ellipse2D.Double(x, y, 220, 46));
            g.fill(new Ellipse2D.Double(x + 50, y - 22, 120, 56));
        }

        // mar
        g.setPaint(new GradientPaint(0, 430, new Color(0x2B6CC4), 0, 600, new Color(0x174A99)));
        g.fill(new Rectangle2D.Double(-80, 430, W + 160, 200));

        // reflejo de la luna
        for (int i = 0; i < 8; i++) {
            double wd = 150 - i * 14;
            double x = 1570 + Math.sin(ahora * 1.5 + i) * 12;
            int alfa = (int) (70 + 50 * Math.sin(ahora * 2.2 + i * 1.3));
            g.setColor(new Color(255, 243, 192, Math.max(0, Math.min(255, alfa))));
            g.fill(new RoundRectangle2D.Double(x - wd / 2, 445 + i * 15, wd, 5, 5, 5));
        }

        // olas
        for (int k = 0; k < 3; k++) {
            Path2D ola = new Path2D.Double();
            double base = 470 + k * 42;
            ola.moveTo(-80, 640);
            for (int x = -80; x <= W + 80; x += 20) {
                ola.lineTo(x, base + Math.sin(x * 0.012 + ahora * (0.8 + k * 0.3) + k * 2) * (7 + k * 2));
            }
            ola.lineTo(W + 80, 640);
            ola.closePath();
            g.setColor(new Color(120 + k * 30, 180 + k * 20, 255, 55));
            g.fill(ola);
        }

        // palmeras (siluetas)
        palmera(g, 170, 640, 330, 1, 0.0);
        palmera(g, 1760, 640, 300, -1, 2.0);

        // arena con borde de espuma
        Path2D arena = new Path2D.Double();
        Path2D borde = new Path2D.Double();
        arena.moveTo(-80, H + 80);
        for (int x = -80; x <= W + 80; x += 20) {
            double y = 600 + Math.sin(x * 0.01 + ahora * 0.9) * 9;
            arena.lineTo(x, y);
            if (x == -80) {
                borde.moveTo(x, y);
            } else {
                borde.lineTo(x, y);
            }
        }
        arena.lineTo(W + 80, H + 80);
        arena.closePath();
        g.setPaint(new GradientPaint(0, 600, new Color(0xD9AD72), 0, H, new Color(0xA5764A)));
        g.fill(arena);
        g.setColor(new Color(255, 255, 255, 150));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(borde);
    }

    private void palmera(Graphics2D g, double x, double suelo, double alto, int dir, double fase) {
        double balanceo = Math.sin(ahora * 0.9 + fase) * 8;
        double tx = x + dir * 40 + balanceo;
        double ty = suelo - alto;

        Path2D tronco = new Path2D.Double();
        tronco.moveTo(x, suelo);
        tronco.quadTo(x + dir * 70, suelo - alto * 0.5, tx, ty);
        g.setColor(new Color(0x08122E));
        g.setStroke(new BasicStroke(26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(tronco);

        for (int i = 0; i < 7; i++) {
            double a = Math.toRadians(-170 + i * 26) + Math.sin(ahora * 1.3 + i + fase) * 0.05;
            double len = 150;
            Path2D hoja = new Path2D.Double();
            hoja.moveTo(tx, ty);
            hoja.quadTo(tx + Math.cos(a) * len * 0.5, ty + Math.sin(a) * len * 0.9 - 30,
                    tx + Math.cos(a) * len, ty + Math.sin(a) * len * 0.6 + 45);
            g.setColor(new Color(0x0B1E3A));
            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(hoja);
        }
    }

    // ---------------- Protagonista cayendo ----------------
    private void dibujarPersonaje(Graphics2D g) {
        if (personaje == null || tCaida < 0) {
            return;
        }
        double ini = tCaida + 0.25;
        if (ahora < ini) {
            return;
        }
        double alto = 230;
        double ancho = alto * personaje.getWidth() / personaje.getHeight();
        double x = 520;
        double suelo = 700;
        double dur = 1.0;

        double p = clamp((ahora - ini) / dur);
        double y = -300 + (suelo + 300) * p * p;
        double rot = (1 - p) * (1 - p) * 6.0;
        double aterr = ahora - (ini + dur);

        // sombra en la arena (crece al acercarse)
        g.setColor(new Color(0, 0, 0, (int) (70 * p)));
        g.fill(new Ellipse2D.Double(x - ancho * 0.45 * p, suelo - 12, ancho * 0.9 * p, 26 * p));

        // estela luminosa mientras cae
        if (p > 0 && p < 1) {
            for (int k = 1; k <= 7; k++) {
                double pk = Math.max(0, p - k * 0.04);
                double yk = -300 + (suelo + 300) * pk * pk;
                double r = 38 - k * 4;
                g.setColor(new Color(255, 233, 168, Math.max(0, 80 - k * 10)));
                g.fill(new Ellipse2D.Double(x - r, yk - alto / 2 - r, r * 2, r * 2));
            }
        }

        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(rot, 0, -alto / 2);
        double sq = aterr >= 0 ? Math.exp(-5 * aterr) * Math.cos(14 * aterr) * 0.22 : 0;
        g.scale(1 + sq, 1 - sq);
        g.drawImage(personaje, (int) (-ancho / 2), (int) -alto, (int) ancho, (int) alto, null);
        g.setTransform(old);

        // polvo de arena
        if (aterr >= 0 && aterr < 0.9) {
            for (int i = 0; i < 10; i++) {
                double sg = (i % 2 == 0) ? -1 : 1;
                double px = x + sg * (20 + aterr * (90 + i * 14));
                double py = suelo - aterr * (30 + i * 5) + aterr * aterr * 60;
                double r = 8 + aterr * 26;
                g.setColor(new Color(232, 207, 160, (int) (150 * (1 - aterr / 0.9))));
                g.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
            }
        }
    }

    // ---------------- Caja de diálogo de Ringo ----------------
    private void dibujarCaja(Graphics2D g) {
        if (linea < 0) {
            return;
        }
        double ap = clamp((ahora - inicioCaja) / 0.5);
        AffineTransform old = g.getTransform();
        g.translate(0, (1 - ap) * 80);

        // chigüiro asomado (si existe el PNG)
        if (ringo != null) {
            double h = 130;
            double w = h * ringo.getWidth() / ringo.getHeight();
            double bob = Math.sin(ahora * 2.2) * 3;
            g.drawImage(ringo, (int) (740 - w / 2), (int) (900 - h + bob), (int) w, (int) h, null);
        }

        RoundRectangle2D caja = new RoundRectangle2D.Double(660, 880, 600, 150, 70, 70);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(660, 890, 600, 150, 70, 70));
        g.setPaint(new GradientPaint(0, 880, new Color(0xFFF6E8), 0, 1030, new Color(0xF8E6C6)));
        g.fill(caja);
        g.setColor(MARRON);
        g.setStroke(new BasicStroke(8f));
        g.draw(caja);
        g.setColor(new Color(0xC9A07A));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{9f, 7f}, 0f));
        g.draw(new RoundRectangle2D.Double(674, 894, 572, 122, 56, 56));

        // etiqueta con el nombre
        RoundRectangle2D tag = new RoundRectangle2D.Double(850, 860, 255, 46, 46, 46);
        g.setColor(new Color(0xFBE3B0));
        g.fill(tag);
        g.setColor(MARRON);
        g.setStroke(new BasicStroke(4f));
        g.draw(tag);
        g.setColor(new Color(0xF2B21C));
        g.fill(new Ellipse2D.Double(862, 877, 12, 12));
        g.fill(new Ellipse2D.Double(1081, 877, 12, 12));
        flor(g, 832, 884, 20);

        g.setFont(FUENTE_NOMBRE);
        FontMetrics fn = g.getFontMetrics();
        g.setColor(TINTA);
        g.drawString(NARRADOR, (float) (977.5 - fn.stringWidth(NARRADOR) / 2.0), (float) (883 + fn.getAscent() / 2.0 - 3));

        // texto con efecto máquina de escribir
        int visibles = 0;
        double el = ahora - inicioLinea;
        while (visibles < tiempos.length && tiempos[visibles] <= el) {
            visibles++;
        }
        g.setFont(FUENTE_TEXTO);
        FontMetrics fm = g.getFontMetrics();
        int altoLinea = fm.getHeight();
        double y = 968 - (renglones.length - 1) * altoLinea / 2.0 + fm.getAscent() / 2.0 - 4;
        int restante = visibles;
        g.setColor(TINTA);
        for (String r : renglones) {
            int mostrar = Math.min(r.length(), Math.max(0, restante));
            double anchoCompleto = fm.stringWidth(r);          // centrado según la línea completa: no "salta" al escribir
            g.drawString(r.substring(0, mostrar), (float) (960 - anchoCompleto / 2.0), (float) y);
            restante -= r.length() + 1;
            y += altoLinea;
        }

        // flechita "continuar"
        if (lineaCompleta()) {
            double bob = Math.sin(ahora * 5) * 4;
            Path2D tri = new Path2D.Double();
            tri.moveTo(1199, 990 + bob);
            tri.lineTo(1231, 990 + bob);
            tri.lineTo(1215, 1010 + bob);
            tri.closePath();
            g.setColor(new Color(0xF2B21C));
            g.fill(tri);
            g.setColor(new Color(0x7A4E10));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(tri);
        }
        g.setTransform(old);

        // ayuda pequeñita
        g.setFont(FUENTE_HINT);
        FontMetrics fh = g.getFontMetrics();
        String hint = "ESPACIO / clic: continuar   ·   ESC: saltar";
        g.setColor(new Color(255, 255, 255, 140));
        g.drawString(hint, W - 30 - fh.stringWidth(hint), H - 20);
    }

    private void flor(Graphics2D g, double x, double y, double r) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.sin(ahora * 1.2) * 0.2);
        for (int i = 0; i < 5; i++) {
            g.rotate(2 * Math.PI / 5);
            Ellipse2D p = new Ellipse2D.Double(-r * 0.38, -r, r * 0.76, r * 0.9);
            g.setColor(new Color(0xFFF4E2));
            g.fill(p);
            g.setColor(new Color(0xD9A27A));
            g.setStroke(new BasicStroke(2f));
            g.draw(p);
        }
        g.setColor(new Color(0xFFD34D));
        g.fill(new Ellipse2D.Double(-r * 0.34, -r * 0.34, r * 0.68, r * 0.68));
        g.setTransform(old);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}