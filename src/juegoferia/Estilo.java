package juegoferia;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Timer;

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
            // si el proyecto trae la letra en /fuentes/, se registra (asi se ve igual en todos los PC)
            for (String ttf : new String[]{"/fuentes/Fredoka-SemiBold.ttf", "/fuentes/Fredoka-Bold.ttf",
                "/fuentes/Fredoka.ttf", "/fuentes/Baloo2-Bold.ttf"}) {
                try (java.io.InputStream in = Estilo.class.getResourceAsStream(ttf)) {
                    if (in != null) {
                        GraphicsEnvironment.getLocalGraphicsEnvironment()
                                .registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
                    }
                } catch (Exception e) {
                    System.err.println("[Estilo] No se pudo cargar la letra " + ttf + ": " + e.getMessage());
                }
            }
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
        // (las letras se guardan: crear un Font nuevo en cada cuadro era de lo mas lento del HUD)
        long clave = ((long) estilo << 32) | Math.round(tam);
        Font f = FUENTES.get(clave);
        if (f == null) {
            f = new Font(familia, estilo, Math.round(tam));
            FUENTES.put(clave, f);
        }
        return f;
    }

    private static final java.util.Map<Long, Font> FUENTES = new java.util.HashMap<>();

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

    // =====================================================================
    //  HUD: indicadores, dia, objetivo, cuenta regresiva y notificaciones
    // =====================================================================
    /**
     * Capa transparente que se pone ENCIMA de una pantalla (el mundo caminable o las escenas).
     * Dibuja: dia de campana, barras de indicadores que se animan solas, numeros flotantes
     * (+2, -4...), objetivo actual, cuenta regresiva del rumor y avisos de notificacion del celular.
     * No recibe clics (deja pasar el mouse a lo que tenga debajo).
     */
    public static class Hud extends JComponent {

        private static final String[] CLAVES = {
            EstadoJuego.CONFIANZA, EstadoJuego.INFO_VERIFICADA,
            EstadoJuego.BIENESTAR, EstadoJuego.CONVIVENCIA};
        private static final String[] NOMBRES = {
            "Confianza", "Info verificada", "Bienestar", "Convivencia"};
        private static final Color[] COLORES = {
            HOJA, new Color(0x4D9BF0), ROJO, new Color(0xB07CE0)};
        private static final boolean[] MALO_SI_SUBE = {false, false, true, false};
        private static final int N = CLAVES.length;

        // medidas del panel (en pixeles de pantalla)
        private static final int PX = 16, PY = 14, PW = 330, FILA = 28;

        private final Supplier<EstadoJuego> fuente;
        private Supplier<String> objetivo;
        private int dia = 1, diasTotales = 2;
        private boolean siempreVisible = true;     // false = el panel aparece solo cuando algo cambia

        private EstadoJuego ultimoEstado;
        private final double[] mostrado = new double[N];
        private final int[] meta = new int[N];
        private final float[] destello = new float[N];

        private static class Flotante {
            String texto;
            Color color;
            double x, y, edad;
        }
        private final List<Flotante> flotantes = new ArrayList<>();

        private double visibilidad = 1;
        private double reposo = 0;
        private String ultimoObjetivo = "";
        private double objetivoPulso = 0;

        // cuenta regresiva (rumor que se esta regando)
        private boolean cuentaActiva;
        private double cuentaFrac, cuentaSegundos, cuentaAlfa;
        private String cuentaTitulo = "";

        // notificacion del celular
        private String notiTitulo, notiTexto;
        private double notiEdad = 999, notiDuracion;

        private final Timer reloj;
        private long ultimo = System.nanoTime();
        private double t;

        private final Font fNombre = fuente(Font.BOLD, 14f);
        private final Font fNum = fuente(Font.BOLD, 13f);
        private final Font fChip = fuente(Font.BOLD, 15f);
        private final Font fObjT = fuente(Font.BOLD, 11f);
        private final Font fObj = fuente(Font.PLAIN, 14f);
        private final Font fFlot = fuente(Font.BOLD, 22f);
        private final Font fBanner = fuente(Font.BOLD, 19f);
        private final Font fSeg = fuente(Font.BOLD, 26f);
        private final Font fNoti1 = fuente(Font.BOLD, 15f);
        private final Font fNoti2 = fuente(Font.PLAIN, 13f);

        public Hud(Supplier<EstadoJuego> fuente) {
            this.fuente = fuente;
            setOpaque(false);
            reloj = new Timer(33, e -> tic());
            addHierarchyListener(ev -> {
                if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        ultimo = System.nanoTime();
                        sincronizar();
                        reloj.start();
                    } else {
                        reloj.stop();
                    }
                }
            });
        }

        /** Para que los clics pasen de largo hacia lo que esta debajo. */
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        // ---------------- API ----------------
        public void setObjetivo(Supplier<String> s) {
            this.objetivo = s;
        }

        public void setDia(int dia, int total) {
            this.dia = dia;
            this.diasTotales = total;
            repaint();
        }

        /** true = panel siempre visible (mundo caminable). false = aparece al cambiar un indicador (escenas). */
        public void setSiempreVisible(boolean v) {
            siempreVisible = v;
            reposo = 0;
            if (v) {
                visibilidad = 1;
            }
            repaint();
        }

        public void setCuenta(boolean activa, double fraccion, double segundosRestantes, String titulo) {
            cuentaActiva = activa;
            cuentaFrac = Math.max(0, Math.min(1, fraccion));
            cuentaSegundos = Math.max(0, segundosRestantes);
            if (titulo != null) {
                cuentaTitulo = titulo;
            }
        }

        public void notificar(String titulo, String texto, int ms) {
            notiTitulo = titulo;
            notiTexto = texto;
            notiEdad = 0;
            notiDuracion = ms / 1000.0;
            repaint();
        }

        /** Vuelve a leer los indicadores sin animaciones ni numeros flotantes. */
        private void sincronizar() {
            EstadoJuego e = fuente.get();
            ultimoEstado = e;
            for (int i = 0; i < N; i++) {
                int v = (e == null) ? 0 : e.get(CLAVES[i]);
                meta[i] = v;
                mostrado[i] = v;
                destello[i] = 0;
            }
            flotantes.clear();
            reposo = 0;
            visibilidad = siempreVisible ? 1 : 0;
            ultimoObjetivo = objetivoActual();
        }

        private String objetivoActual() {
            String s = (objetivo == null) ? null : objetivo.get();
            return s == null ? "" : s;
        }

        // ---------------- animacion ----------------
        private void tic() {
            long ahora = System.nanoTime();
            double dt = Math.min(0.1, (ahora - ultimo) / 1e9);
            ultimo = ahora;
            t += dt;
            boolean anima = false;

            EstadoJuego e = fuente.get();
            if (e != ultimoEstado) {
                sincronizar();            // partida nueva: sin numeros flotantes
                anima = true;
            } else if (e != null) {
                for (int i = 0; i < N; i++) {
                    int v = e.get(CLAVES[i]);
                    if (v != meta[i]) {
                        int d = v - meta[i];
                        meta[i] = v;
                        destello[i] = 1f;
                        reposo = 0;
                        Flotante f = new Flotante();
                        f.texto = (d > 0 ? "+" : "") + d;
                        f.color = ((d > 0) != MALO_SI_SUBE[i]) ? new Color(0x8BE86A) : new Color(0xFF6B5A);
                        f.x = PX + PW + 10;
                        f.y = PY + 20 + 30 + i * FILA + 14;
                        flotantes.add(f);
                    }
                }
            }

            for (int i = 0; i < N; i++) {
                double dif = meta[i] - mostrado[i];
                if (Math.abs(dif) > 0.05) {
                    mostrado[i] += dif * (1 - Math.exp(-7 * dt));
                    anima = true;
                } else {
                    mostrado[i] = meta[i];
                }
                if (destello[i] > 0) {
                    destello[i] = (float) Math.max(0, destello[i] - dt * 1.6);
                    anima = true;
                }
            }

            for (int i = flotantes.size() - 1; i >= 0; i--) {
                Flotante f = flotantes.get(i);
                f.edad += dt;
                if (f.edad > 1.7) {
                    flotantes.remove(i);
                }
                anima = true;
            }

            if (siempreVisible) {
                visibilidad = 1;
            } else {
                reposo += dt;
                double meta2 = (reposo < 3.5) ? 1 : 0;
                double ant = visibilidad;
                visibilidad += (meta2 - visibilidad) * (1 - Math.exp(-6 * dt));
                if (Math.abs(visibilidad - ant) > 0.002 || visibilidad > 0.02) {
                    anima = true;
                }
            }

            String obj = objetivoActual();
            if (!obj.equals(ultimoObjetivo)) {
                ultimoObjetivo = obj;
                objetivoPulso = 1;
            }
            if (objetivoPulso > 0) {
                objetivoPulso = Math.max(0, objetivoPulso - dt * 0.7);
                anima = true;
            }

            cuentaAlfa += ((cuentaActiva ? 1 : 0) - cuentaAlfa) * (1 - Math.exp(-10 * dt));
            if (cuentaAlfa > 0.01 || cuentaActiva) {
                anima = true;
            }

            if (notiEdad < notiDuracion + 0.7) {
                notiEdad += dt;
                anima = true;
            }

            if (anima) {
                repaint(0, 0, getWidth(), Math.min(getHeight(), 340));
            }
        }

        // ---------------- dibujo ----------------
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            suave(g);
            dibujarPanel(g);
            dibujarCuenta(g);
            dibujarNotificacion(g);
            dibujarFlotantes(g);
            g.dispose();
        }

        private static double clamp(double v) {
            return Math.max(0, Math.min(1, v));
        }

        private static double easeOutBack(double p) {
            double c1 = 1.70158, c3 = c1 + 1;
            return 1 + c3 * Math.pow(p - 1, 3) + c1 * Math.pow(p - 1, 2);
        }

        private static List<String> envolver(String texto, FontMetrics fm, int ancho) {
            List<String> lineas = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            for (String palabra : texto.split(" ")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (fm.stringWidth(prueba) > ancho && actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual = new StringBuilder(palabra);
                } else {
                    actual = new StringBuilder(prueba);
                }
            }
            if (actual.length() > 0) {
                lineas.add(actual.toString());
            }
            return lineas;
        }

        private void dibujarPanel(Graphics2D g) {
            if (visibilidad < 0.02) {
                return;
            }
            Composite viejo = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, visibilidad)));

            String obj = objetivoActual();
            List<String> lineas = obj.isEmpty()
                    ? new ArrayList<String>() : envolver(obj, g.getFontMetrics(fObj), PW - 28);

            int panelY = PY + 20;
            int filaY0 = panelY + 30;
            int fin = filaY0 + N * FILA;
            int bottom = lineas.isEmpty() ? fin + 2 : fin + 8 + 14 + 6 + lineas.size() * 18 + 8;
            int alto = bottom - panelY;

            g.setColor(new Color(0, 0, 0, 55));
            g.fillRoundRect(PX + 2, panelY + 4, PW, alto, 22, 22);
            pergamino(g, PX, panelY, PW, alto, 22);
            if (objetivoPulso > 0) {
                g.setColor(new Color(255, 211, 92, (int) (220 * objetivoPulso)));
                g.setStroke(new BasicStroke(4f));
                g.drawRoundRect(PX, panelY, PW, alto, 22, 22);
            }

            // etiqueta del dia
            madera(g, PX + 10, PY, 150, 30, 14);
            g.setFont(fChip);
            String d = "DÍA " + dia + " DE " + diasTotales;
            FontMetrics fc = g.getFontMetrics();
            int dx = PX + 10 + (150 - fc.stringWidth(d)) / 2;
            g.setColor(new Color(0x4A2A10));
            g.drawString(d, dx + 1, PY + 20 + 1);
            g.setColor(new Color(0xFFF3D6));
            g.drawString(d, dx, PY + 20);

            // barras
            int bx = PX + 136, bw = PW - 136 - 48;
            for (int i = 0; i < N; i++) {
                int y = filaY0 + i * FILA;
                g.setFont(fNombre);
                g.setColor(TEXTO);
                g.drawString(NOMBRES[i], PX + 14, y + 13);
                dibujarBarra(g, bx, y, bw, 16, mostrado[i], COLORES[i], destello[i]);
                g.setFont(fNum);
                String v = String.valueOf(meta[i]);
                g.setColor(TEXTO);
                g.drawString(v, PX + PW - 14 - g.getFontMetrics().stringWidth(v), y + 13);
            }

            // objetivo
            if (!lineas.isEmpty()) {
                int sepY = fin + 2;
                g.setColor(new Color(0xE6D09B));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        10f, new float[]{2f, 6f}, 0f));
                g.drawLine(PX + 14, sepY, PX + PW - 14, sepY);
                g.setFont(fObjT);
                g.setColor(HOJA_OSCURA);
                g.drawString("OBJETIVO", PX + 14, fin + 8 + 11);
                g.setFont(fObj);
                g.setColor(TEXTO);
                int ty = fin + 8 + 14 + 6;
                for (String l : lineas) {
                    ty += 18;
                    g.drawString(l, PX + 14, ty - 4);
                }
            }
            g.setComposite(viejo);
        }

        private void dibujarBarra(Graphics2D g, int x, int y, int w, int h, double valor, Color c, float flash) {
            RoundRectangle2D.Float fondo = new RoundRectangle2D.Float(x, y, w, h, h, h);
            g.setColor(new Color(90, 59, 31, 70));
            g.fill(fondo);
            int fw = (int) Math.round(w * clamp(valor / 100.0));
            if (fw > 0) {
                Shape antes = g.getClip();
                g.clip(fondo);
                g.setPaint(new GradientPaint(0, y, c.brighter(), 0, y + h, c.darker()));
                g.fillRect(x, y, fw, h);
                g.setColor(new Color(255, 255, 255, 70));
                g.fillRect(x, y + 2, fw, h / 3);
                if (flash > 0) {
                    g.setColor(new Color(255, 255, 255, (int) (150 * flash)));
                    g.fillRect(x, y, fw, h);
                }
                g.setClip(antes);
            }
            g.setColor(MADERA_OSCURA);
            g.setStroke(new BasicStroke(2f));
            g.draw(fondo);
        }

        private void dibujarCuenta(Graphics2D g) {
            if (cuentaAlfa < 0.02) {
                return;
            }
            Composite viejo = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, cuentaAlfa)));

            boolean urgente = cuentaSegundos <= 4;
            int bw = 560, bh = 66;
            int tem = urgente ? (int) Math.round(Math.sin(t * 50) * 2.5) : 0;
            // centrado, pero sin tapar los numeros flotantes que salen junto al panel
            int bx = Math.max((getWidth() - bw) / 2, PX + PW + 56) + tem;
            int by = 14 - (int) ((1 - cuentaAlfa) * 80);

            madera(g, bx, by, bw, bh, 26);

            g.setFont(fBanner);
            textoContorno(g, cuentaTitulo, bx + 24, by + 28, new Color(0xFFF3D6), new Color(0x6A2A1C));

            g.setFont(fSeg);
            String s = (int) Math.ceil(cuentaSegundos) + " s";
            Color cs = urgente && ((int) (t * 6) % 2 == 0) ? new Color(0xFF8A7A) : new Color(0xFFD35C);
            int sw = g.getFontMetrics().stringWidth(s);
            textoContorno(g, s, bx + bw - 24 - sw, by + 32, cs, new Color(0x6A2A1C));

            // barra: del amarillo al rojo a medida que el rumor se riega
            Color c = mezclar(DORADO, ROJO, (float) cuentaFrac);
            float flash = urgente ? (float) (0.35 + 0.35 * Math.sin(t * 14)) : 0f;
            dibujarBarra(g, bx + 24, by + 40, bw - 48, 16, cuentaFrac * 100, c, Math.max(0f, flash));
            g.setComposite(viejo);
        }

        private static Color mezclar(Color a, Color b, float p) {
            return new Color(
                    Math.round(a.getRed() + (b.getRed() - a.getRed()) * p),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * p),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * p));
        }

        private void dibujarNotificacion(Graphics2D g) {
            if (notiTitulo == null || notiEdad > notiDuracion + 0.6) {
                return;
            }
            double entra = easeOutBack(clamp(notiEdad / 0.45));
            double sale = notiEdad > notiDuracion ? clamp((notiEdad - notiDuracion) / 0.5) : 0;
            int tw = 440, th = 64;
            double vibra = notiEdad < 0.7 ? Math.sin(notiEdad * 70) * 5 * (1 - notiEdad / 0.7) : 0;
            int tx = (getWidth() - tw) / 2 + (int) Math.round(vibra);
            int ty = (int) (-th + (th + 24) * entra - sale * (th + 40)) + (cuentaAlfa > 0.1 ? 80 : 0);

            g.setColor(new Color(0, 0, 0, 60));
            g.fillRoundRect(tx + 2, ty + 5, tw, th, 22, 22);
            pergamino(g, tx, ty, tw, th, 22);

            // iconito de celular con globito rojo
            int ix = tx + 22, iy = ty + 14;
            g.setColor(new Color(0x3A3F55));
            g.fill(new RoundRectangle2D.Float(ix, iy, 24, 38, 8, 8));
            g.setColor(new Color(0xBFE3FF));
            g.fill(new RoundRectangle2D.Float(ix + 3, iy + 5, 18, 26, 4, 4));
            double lat = 1 + 0.15 * Math.sin(notiEdad * 12);
            g.setColor(new Color(0xE8443F));
            g.fill(new java.awt.geom.Ellipse2D.Double(ix + 24 - 9 * lat, iy - 6 - 0 * lat, 18 * lat, 18 * lat));

            g.setFont(fNoti1);
            g.setColor(TEXTO);
            g.drawString(notiTitulo, tx + 64, ty + 26);
            g.setFont(fNoti2);
            g.setColor(TEXTO_SUAVE);
            String cuerpo = notiTexto == null ? "" : notiTexto;
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(cuerpo) > tw - 84 && cuerpo.length() > 4) {
                cuerpo = cuerpo.substring(0, cuerpo.length() - 2);
            }
            g.drawString(cuerpo, tx + 64, ty + 48);
        }

        private void dibujarFlotantes(Graphics2D g) {
            Composite viejo = g.getComposite();
            for (Flotante f : flotantes) {
                double p = f.edad / 1.7;
                float a = (float) clamp(1 - p * p);
                g.setComposite(AlphaComposite.SrcOver.derive(a));
                g.setFont(fFlot);
                double pop = Math.max(0, 1 - f.edad * 5);
                int yy = (int) (f.y - 46 * (1 - Math.pow(1 - p, 2)) - 6 * pop);
                textoContorno(g, f.texto, (int) f.x, yy, f.color, new Color(0x3A2412));
            }
            g.setComposite(viejo);
        }
    }
}
