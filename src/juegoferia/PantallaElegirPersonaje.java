package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * "Elige tu rol" (modo un solo PC).
 *
 * - Cuatro insignias como nubecitas; al pasar el mouse crecen, brillan, sueltan chispas y el
 *   personaje dice su frase. Las demas se apagan un poquito.
 * - Al tocar una insignia libre se abre una tarjeta con la info del rol y una caja para escribir
 *   el nombre ("¡Soy yo!" o Enter confirma, Esc o "Cancelar" cierra).
 * - Tocar una insignia ya tomada deja soltarla (por si alguien se equivoco).
 * - Mariposas amarillas vuelan por toda la pantalla cargando lucecitas.
 * - Teclas 1-4 abren cada rol (orden de las insignias en pantalla: arriba, izquierda, derecha, abajo).
 *
 * ARTE (todo opcional; si falta, se dibuja con codigo):
 *   /imagenes/fondos/elegir_personaje.png      fondo 1920x1080 SIN botones ni letreros (se oscurece solo)
 *   /imagenes/personajes/<rol>/quieto.png      sprite de cada rol (ya lo usa el resto del juego)
 *   /imagenes/ui/rol_<rol>.png                 si quieres hacer tu la insignia completa (440x380, PNG transparente)
 * SONIDOS (opcionales, .wav): ver lista en LEEME_ELEGIR_ROL.txt
 *
 * Esta pantalla solo se usa en "un solo PC" (no pasa por la red), por eso puede usar su propio Random.
 */
public class PantallaElegirPersonaje extends PanelDiseno {

    // ---------- sonidos y musica ----------
    private static final String S_HOVER = "/sonidos/hover.wav";
    private static final String S_ABRIR = "/sonidos/abrir_tarjeta.wav";
    private static final String S_ELEGIR = "/sonidos/elegir_rol.wav";
    private static final String S_CANCELAR = "/sonidos/cancelar.wav";
    private static final String S_TECLA = "/sonidos/tecla.wav";
    private static final String S_SOLTAR = "/sonidos/soltar_rol.wav";
    private static final String S_TODOS = "/sonidos/todos_listos.wav";
    private static final String S_CLICK = "/sonidos/click.wav";
    private static final String MUSICA = "/musica/elegir_rol.wav";

    // ---------- colores ----------
    private static final Color CREMA = new Color(0xFFF6DE);
    private static final Color CREMA_2 = new Color(0xF6E2B8);
    private static final Color CAFE = new Color(0x4A2A14);
    private static final Color AMARILLO = new Color(0xFFD84A);
    private static final Color NOCHE = new Color(14, 8, 34);

    /** Donde va cada rol en pantalla (como en tu boceto: arriba, izquierda, derecha, abajo). */
    private static final Rol[] ORDEN = {Rol.CANDIDATO, Rol.PERIODISTA, Rol.CIUDADANO, Rol.INFLUENCER};
    private static final int[][] CENTROS = {{960, 400}, {585, 610}, {1335, 610}, {960, 828}};
    private static final int RX = 190, RY = 165;

    private final Consumer<Partida> alListo;
    private final Runnable alVolver;
    private Partida partida;
    private final StringBuilder nombre = new StringBuilder();
    private final Actor[] figuras = new Actor[4];
    private final Random rnd = new Random(1967);

    // ---------- estado de la interfaz ----------
    private int hover = -1;
    private final double[] brillo = new double[4];   // 0..1 animacion de hover de cada insignia
    private final double[] pop = new double[4];      // salto cuando se elige
    private int abierta = -1;                        // tarjeta abierta (indice en ORDEN) o -1
    private double tarjeta;                          // 0..1 animacion de la tarjeta
    private String botonEncima;                      // "volver", "empezar", "cancelar", "ok"
    private double listoDesde = -1;

    private final Rectangle volver = new Rectangle(40, 962, 250, 82);
    private final Rectangle empezar = new Rectangle(1520, 948, 360, 98);
    private static final Rectangle CARD = new Rectangle(440, 250, 1040, 620);
    private final Rectangle cajaNombre = new Rectangle(CARD.x + 450, CARD.y + 410, 540, 74);
    private final Rectangle cancelar = new Rectangle(CARD.x + 450, CARD.y + 528, 230, 70);
    private final Rectangle ok = new Rectangle(CARD.x + 700, CARD.y + 528, 290, 70);

    // ---------- mariposas y lucecitas ----------
    private final List<Mariposa> mariposas = new ArrayList<>();
    private final List<Chispa> chispas = new ArrayList<>();
    private BufferedImage glow;
    private BufferedImage vineta;

    public PantallaElegirPersonaje(Consumer<Partida> alListo, Runnable alVolver) {
        this.alListo = alListo;
        this.alVolver = alVolver;
        for (int i = 0; i < 4; i++) {
            Rol r = ORDEN[i];
            figuras[i] = new Actor(r.carpeta, 0, 0, 165, 235)
                    .colores(new Color(0xF2C9A0), r.color, new Color(0x3A2416))
                    .sprites(r.sprites());
        }
        for (int i = 0; i < 11; i++) {
            mariposas.add(new Mariposa(i < 4));
        }
        reiniciar();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (abierta < 0) {
                    return;
                }
                char ch = e.getKeyChar();
                if (ch == '\b') {
                    if (nombre.length() > 0) {
                        nombre.setLength(nombre.length() - 1);
                        sonar(S_TECLA);
                    }
                } else if (!Character.isISOControl(ch) && nombre.length() < 14) {
                    nombre.append(ch);
                    sonar(S_TECLA);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (abierta >= 0) {
                    if (k == KeyEvent.VK_ENTER) {
                        confirmar();
                    } else if (k == KeyEvent.VK_ESCAPE) {
                        cerrar();
                    }
                    return;
                }
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_4) {
                    tocarInsignia(k - KeyEvent.VK_1);
                } else if (k == KeyEvent.VK_ENTER && partida.estaCompleta()) {
                    arrancar();
                } else if (k == KeyEvent.VK_ESCAPE) {
                    alVolver.run();
                }
            }
        });

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                apuntar(aDiseno(e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                apuntar(aDiseno(e.getPoint()));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point p = aDiseno(e.getPoint());
                if (abierta >= 0) {
                    if (ok.contains(p)) {
                        confirmar();
                    } else if (cancelar.contains(p) || !CARD.contains(p)) {
                        cerrar();
                    }
                    return;
                }
                if (volver.contains(p)) {
                    sonar(S_CLICK);
                    alVolver.run();
                    return;
                }
                if (partida.estaCompleta() && empezar.contains(p)) {
                    arrancar();
                    return;
                }
                int i = insigniaEn(p);
                if (i >= 0) {
                    tocarInsignia(i);
                }
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
    }

    /** Deja la pantalla lista para una partida nueva. */
    public final void reiniciar() {
        partida = new Partida();
        nombre.setLength(0);
        abierta = -1;
        tarjeta = 0;
        listoDesde = -1;
    }

    @Override
    protected void alMostrar() {
        Musica.tocar(MUSICA);
    }

    @Override
    protected void alOcultar() {
        Musica.parar();
    }

    // ======================================================================
    //  LOGICA
    // ======================================================================

    private int turno() {
        return partida.jugadores().size() + 1;
    }

    private void tocarInsignia(int i) {
        Rol r = ORDEN[i];
        if (partida.ocupado(r)) {
            // soltar el rol (cambio de opinion)
            partida.quitarJugador(r);
            listoDesde = -1;
            pop[i] = 1;
            sonar(S_SOLTAR, S_CLICK);
            return;
        }
        abierta = i;
        tarjeta = 0;
        nombre.setLength(0);
        sonar(S_ABRIR, S_CLICK);
    }

    private void confirmar() {
        if (abierta < 0) {
            return;
        }
        int i = abierta;
        String n = nombre.toString().trim();
        partida.agregarJugador(n.isEmpty() ? "Jugador " + turno() : n, ORDEN[i]);
        nombre.setLength(0);
        abierta = -1;
        pop[i] = 1;
        explosion(CENTROS[i][0], CENTROS[i][1], ORDEN[i].color, 60);
        if (partida.estaCompleta()) {
            listoDesde = tiempo;
            sonar(S_TODOS, S_ELEGIR);
            for (int k = 0; k < 4; k++) {
                explosion(CENTROS[k][0], CENTROS[k][1], AMARILLO, 30);
            }
        } else {
            sonar(S_ELEGIR, S_CLICK);
        }
    }

    private void cerrar() {
        abierta = -1;
        nombre.setLength(0);
        sonar(S_CANCELAR, S_CLICK);
    }

    private void arrancar() {
        sonar(S_CLICK);
        alListo.accept(partida);
    }

    private void apuntar(Point p) {
        String antesBoton = botonEncima;
        botonEncima = null;
        int nuevo = -1;
        if (abierta >= 0) {
            if (ok.contains(p)) {
                botonEncima = "ok";
            } else if (cancelar.contains(p)) {
                botonEncima = "cancelar";
            }
        } else {
            if (volver.contains(p)) {
                botonEncima = "volver";
            } else if (partida.estaCompleta() && empezar.contains(p)) {
                botonEncima = "empezar";
            } else {
                nuevo = insigniaEn(p);
            }
        }
        if (nuevo != hover && nuevo >= 0 && !partida.ocupado(ORDEN[nuevo])) {
            sonar(S_HOVER);
        }
        if (botonEncima != null && !botonEncima.equals(antesBoton)) {
            sonar(S_HOVER);
        }
        hover = nuevo;
        setCursor(hover >= 0 || botonEncima != null ? Cursores.MANO : Cursores.NORMAL);
    }

    /** La insignia bajo el punto (si se solapan gana la mas cercana a su centro). */
    private int insigniaEn(Point p) {
        int mejor = -1;
        double dMejor = 1.0;
        for (int i = 0; i < 4; i++) {
            double dx = (p.x - CENTROS[i][0]) / (double) RX;
            double dy = (p.y - CENTROS[i][1]) / (double) RY;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < dMejor) {
                dMejor = d;
                mejor = i;
            }
        }
        return mejor;
    }

    private static void sonar(String ruta, String... alternativas) {
        if (Recursos.existe(ruta)) {
            Funciones.reproducirSonido(ruta);
            return;
        }
        for (String a : alternativas) {
            if (Recursos.existe(a)) {
                Funciones.reproducirSonido(a);
                return;
            }
        }
    }

    @Override
    protected void actualizar(double dt) {
        double k = Math.min(1, dt * 11);
        for (int i = 0; i < 4; i++) {
            double meta = (i == hover && abierta < 0) ? 1 : 0;
            brillo[i] += (meta - brillo[i]) * k;
            pop[i] = Math.max(0, pop[i] - dt * 2.2);
        }
        if (abierta >= 0) {
            tarjeta = Math.min(1, tarjeta + dt * 4.5);
        }

        for (Mariposa m : mariposas) {
            m.mover(dt);
        }
        // polvito dorado que flota solo
        if (rnd.nextDouble() < dt * 9) {
            Chispa c = new Chispa(rnd.nextDouble() * W, H + 10, AMARILLO);
            c.vx = (rnd.nextDouble() - 0.5) * 20;
            c.vy = -20 - rnd.nextDouble() * 30;
            c.vida = c.max = 5 + rnd.nextDouble() * 4;
            c.tam = 3 + rnd.nextDouble() * 4;
            chispas.add(c);
        }
        // chispas alrededor de la insignia con hover
        if (hover >= 0 && abierta < 0 && !partida.ocupado(ORDEN[hover]) && rnd.nextDouble() < dt * 30) {
            double a = rnd.nextDouble() * Math.PI * 2;
            Chispa c = new Chispa(CENTROS[hover][0] + Math.cos(a) * RX * 1.05,
                    CENTROS[hover][1] + Math.sin(a) * RY * 1.05, rnd.nextBoolean() ? AMARILLO : Color.WHITE);
            c.vx = Math.cos(a) * 30;
            c.vy = Math.sin(a) * 30 - 20;
            c.vida = c.max = 0.6 + rnd.nextDouble() * 0.5;
            c.tam = 5 + rnd.nextDouble() * 6;
            c.estrella = true;
            chispas.add(c);
        }
        for (Iterator<Chispa> it = chispas.iterator(); it.hasNext();) {
            Chispa c = it.next();
            c.vida -= dt;
            c.x += c.vx * dt;
            c.y += c.vy * dt;
            c.vx *= 0.985;
            if (c.vida <= 0) {
                it.remove();
            }
        }
        while (chispas.size() > 600) {
            chispas.remove(0);
        }
    }

    private void explosion(double x, double y, Color color, int n) {
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double v = 120 + rnd.nextDouble() * 380;
            Chispa c = new Chispa(x, y, rnd.nextInt(3) == 0 ? Color.WHITE : (rnd.nextBoolean() ? color : AMARILLO));
            c.vx = Math.cos(a) * v;
            c.vy = Math.sin(a) * v - 60;
            c.vida = c.max = 0.7 + rnd.nextDouble() * 0.7;
            c.tam = 6 + rnd.nextDouble() * 8;
            c.estrella = rnd.nextBoolean();
            chispas.add(c);
        }
    }

    // ======================================================================
    //  DIBUJO
    // ======================================================================

    @Override
    protected void dibujar(Graphics2D g) {
        prepararImagenes();
        fondo(g);

        // mariposas de atras (mas chiquitas, detras de las insignias)
        for (Mariposa m : mariposas) {
            if (m.atras) {
                m.dibujar(g);
            }
        }

        letrero(g);

        double algunHover = 0;
        for (int i = 0; i < 4; i++) {
            algunHover = Math.max(algunHover, partida.ocupado(ORDEN[i]) ? 0 : brillo[i]);
        }
        // primero las que no tienen hover, al final la que si (para que quede encima)
        int[] orden = {0, 1, 2, 3};
        for (int a = 0; a < 4; a++) {
            for (int b = a + 1; b < 4; b++) {
                if (brillo[orden[b]] < brillo[orden[a]]) {
                    int t = orden[a];
                    orden[a] = orden[b];
                    orden[b] = t;
                }
            }
        }
        for (int i : orden) {
            insignia(g, i, algunHover);
        }

        for (Mariposa m : mariposas) {
            if (!m.atras) {
                m.dibujar(g);
            }
        }
        dibujarChispas(g);

        if (hover >= 0 && abierta < 0) {
            burbuja(g, hover);
        }

        botonPildora(g, volver, "Volver", CREMA, CAFE, "volver".equals(botonEncima), true, 0);
        progreso(g);

        if (abierta >= 0) {
            tarjetaRol(g, abierta);
        }
    }

    private void prepararImagenes() {
        if (glow == null) {
            glow = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = glow.createGraphics();
            gg.setPaint(new RadialGradientPaint(64, 64, 64, new float[]{0f, 0.18f, 0.45f, 1f},
                    new Color[]{new Color(255, 252, 225, 255), new Color(255, 225, 120, 210),
                        new Color(255, 190, 60, 70), new Color(255, 170, 40, 0)}));
            gg.fillRect(0, 0, 128, 128);
            gg.dispose();
        }
        if (vineta == null) {
            vineta = new BufferedImage(W / 2, H / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = vineta.createGraphics();
            gg.setPaint(new RadialGradientPaint(new Point2D.Double(W / 4.0, H / 4.0 + 20), W / 2.6f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(10, 5, 25, 0), new Color(10, 5, 25, 70), new Color(6, 2, 16, 215)}));
            gg.fillRect(0, 0, W / 2, H / 2);
            gg.dispose();
        }
    }

    private void fondo(Graphics2D g) {
        String ruta = Rutas.primera(Rutas.ELEGIR_PERSONAJE);
        if (Recursos.existe(ruta)) {
            g.drawImage(Recursos.imagen(ruta, W, H), 0, 0, W, H, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(0x3B1F5C), 0, H, new Color(0x1F3A2A)));
            g.fillRect(0, 0, W, H);
        }
        // oscurecer + vineta (los bordes mas oscuros que el centro)
        g.setColor(new Color(NOCHE.getRed(), NOCHE.getGreen(), NOCHE.getBlue(), 105));
        g.fillRect(0, 0, W, H);
        g.drawImage(vineta, 0, 0, W, H, null);
        // cuando hay tarjeta abierta, todavia mas oscuro
        if (abierta >= 0) {
            g.setColor(new Color(6, 3, 16, (int) (150 * tarjeta)));
            g.fillRect(0, 0, W, H);
        }
    }

    // ---------------------------- letrero ----------------------------

    private void letrero(Graphics2D g) {
        int w = 760, h = 168, x = W / 2 - w / 2, y = 44;
        // cintas que cuelgan
        for (int lado = -1; lado <= 1; lado += 2) {
            int bx = W / 2 + lado * (w / 2 - 40);
            for (int k = 0; k < 2; k++) {
                Path2D cinta = new Path2D.Double();
                double ox = lado * (k * 26);
                cinta.moveTo(bx + ox, y + h - 30);
                cinta.lineTo(bx + ox + lado * 34, y + h - 30);
                cinta.lineTo(bx + ox + lado * 70, y + h + 70 + k * 10);
                cinta.lineTo(bx + ox + lado * 52, y + h + 54 + k * 10);
                cinta.lineTo(bx + ox + lado * 40, y + h + 82 + k * 10);
                cinta.closePath();
                g.setColor(k == 0 ? new Color(0xF4F1E8) : new Color(0x7FA8D9));
                g.fill(cinta);
                g.setColor(new Color(0x3B4A66));
                g.setStroke(new BasicStroke(2.5f));
                g.draw(cinta);
            }
        }
        Estilo.madera(g, x, y, w, h, 70);
        g.setColor(new Color(0x8C5A2B));
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(x + 14, y + 14, w - 28, h - 28, 50, 50));

        // hojas y flores en las puntas
        Estilo.hojas(g, x + 16, y + h / 2, -1, 1, 70);
        Estilo.hojas(g, x + w - 16, y + h / 2, 1, 1, 70);
        flor(g, x + 6, y + h / 2 - 6, 30);
        flor(g, x + w - 6, y + h / 2 - 6, 30);
        flor(g, x + 52, y + h / 2 + 46, 18);
        flor(g, x + w - 52, y + h / 2 + 46, 18);

        // corona
        corona(g, W / 2, y - 6 + Math.sin(tiempo * 2) * 3);
        // destellos de la corona
        for (int k = -1; k <= 1; k += 2) {
            double a = 0.6 + 0.4 * Math.sin(tiempo * 3 + k);
            g.setColor(new Color(255, 220, 90, (int) (220 * a)));
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(W / 2 + k * 70, y - 18, W / 2 + k * 92, y - 34);
            g.drawLine(W / 2 + k * 100, y + 4, W / 2 + k * 126, y - 2);
        }

        tituloGordo(g, "Elige tu rol", W / 2, y + 112, 96f);

        String sub = partida.estaCompleta() ? "¡Ya están los cuatro! Que empiece la historia."
                : "Jugador " + turno() + " · toca un personaje libre";
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(new Color(0xFFF0C8));
        centrado(g, sub, W / 2, y + 150);
    }

    private void tituloGordo(Graphics2D g, String s, int cx, int base, float tam) {
        Font f = Estilo.fuente(Font.BOLD, tam);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), s);
        double ancho = gv.getVisualBounds().getWidth();
        Shape forma = gv.getOutline((float) (cx - ancho / 2), base);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(AffineTransform.getTranslateInstance(0, 6).createTransformedShape(forma));
        g.setColor(CAFE);
        g.setStroke(new BasicStroke(13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(forma);
        g.setPaint(new GradientPaint(0, base - tam * 0.7f, new Color(0xFFFDF2), 0, base, new Color(0xFFE09A)));
        g.fill(forma);
    }

    private static void flor(Graphics2D g, double cx, double cy, double r) {
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5 - Math.PI / 2;
            Shape p = new Ellipse2D.Double(cx + Math.cos(a) * r * 0.55 - r * 0.45, cy + Math.sin(a) * r * 0.55 - r * 0.45,
                    r * 0.9, r * 0.9);
            g.setColor(new Color(0xFFFBF0));
            g.fill(p);
            g.setColor(new Color(0xC9A27A));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(p);
        }
        g.setColor(new Color(0xF5B82E));
        g.fill(new Ellipse2D.Double(cx - r * 0.32, cy - r * 0.32, r * 0.64, r * 0.64));
    }

    private static void corona(Graphics2D g, double cx, double y) {
        Path2D c = new Path2D.Double();
        c.moveTo(cx - 46, y + 30);
        c.lineTo(cx - 50, y - 12);
        c.lineTo(cx - 26, y + 8);
        c.lineTo(cx, y - 22);
        c.lineTo(cx + 26, y + 8);
        c.lineTo(cx + 50, y - 12);
        c.lineTo(cx + 46, y + 30);
        c.closePath();
        g.setPaint(new GradientPaint(0, (float) y - 20, new Color(0xFFE680), 0, (float) y + 30, new Color(0xF0A818)));
        g.fill(c);
        g.setColor(new Color(0x8A5A10));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(c);
        g.setColor(new Color(0xE0424F));
        g.fill(new Ellipse2D.Double(cx - 7, y + 10, 14, 14));
        for (int k = -1; k <= 1; k += 2) {
            g.setColor(new Color(0x4FA3E0));
            g.fill(new Ellipse2D.Double(cx + k * 28 - 5, y + 14, 10, 10));
        }
    }

    // ---------------------------- insignias ----------------------------

    /** Contorno de nube con borde ondulado (las insignias de tu boceto). */
    private static Path2D nube(double cx, double cy, double rx, double ry, double fase) {
        Path2D p = new Path2D.Double();
        int n = 120;
        for (int i = 0; i <= n; i++) {
            double a = i * Math.PI * 2 / n;
            double f = 1 + 0.035 * Math.sin(7 * a + fase) + 0.02 * Math.sin(3 * a + 1.3 + fase * 0.5);
            double x = cx + Math.cos(a) * rx * f;
            double y = cy + Math.sin(a) * ry * f;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        return p;
    }

    private void insignia(Graphics2D g, int i, double algunHover) {
        Rol r = ORDEN[i];
        boolean ocupado = partida.ocupado(r);
        double b = ocupado ? brillo[i] * 0.3 : brillo[i];
        double rebote = Math.sin(pop[i] * Math.PI) * 0.12;
        double s = 1 + 0.08 * b + rebote;
        double cx = CENTROS[i][0];
        double cy = CENTROS[i][1] - 12 * b + Math.sin(tiempo * 1.6 + i * 1.7) * 4;

        AffineTransform guardado = g.getTransform();
        Composite compAntes = g.getComposite();
        // las demas se apagan un poquito cuando hay una con hover
        double apagado = Math.max(0, (algunHover - b) * 0.5);
        if (apagado > 0.01) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - apagado)));
        }
        g.translate(cx, cy);
        g.scale(s, s);

        // halo de luz detras
        if (b > 0.01) {
            float rad = (float) (RX * 1.55);
            Color c = r.color;
            g.setPaint(new RadialGradientPaint(0, 0, rad, new float[]{0.45f, 1f},
                    new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (150 * b)),
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g.fill(new Ellipse2D.Double(-rad, -rad, rad * 2, rad * 2));
            // rayitos amarillos girando (como en tu boceto)
            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI * 2 / 10 + tiempo * 0.5;
                double r1 = RX * 1.12 + Math.sin(tiempo * 6 + k) * 6;
                double r2 = r1 + 26 * b;
                g.setColor(new Color(255, 214, 74, (int) (230 * b)));
                g.drawLine((int) (Math.cos(a) * r1), (int) (Math.sin(a) * r1 * RY / RX),
                        (int) (Math.cos(a) * r2), (int) (Math.sin(a) * r2 * RY / RX));
            }
        }

        String propia = "/imagenes/ui/rol_" + r.carpeta + ".png";
        if (Recursos.existe(propia)) {
            g.drawImage(Recursos.imagen(propia, 440, 380), -220, -190, 440, 380, null);
        } else {
            dibujarNube(g, r, i, b);
        }

        // etiqueta con icono y nombre
        etiqueta(g, r, 0, RY - 34, b);

        // tomado: velo, cinta con el nombre y sello
        if (ocupado) {
            g.setColor(new Color(20, 12, 30, 135));
            g.fill(nube(0, 0, RX, RY, i));
            cintaNombre(g, partida.jugador(r).nombre, r.color);
            sello(g, RX - 50, -RY + 40);
        }

        g.setComposite(compAntes);
        g.setTransform(guardado);
    }

    private void dibujarNube(Graphics2D g, Rol r, int i, double b) {
        double fase = i + tiempo * 0.8 * b;
        Color c = r.color;
        Color claro = mezclar(c, Color.WHITE, 0.55);
        // sombra
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(nube(6, 14, RX + 10, RY + 10, fase));
        // borde crema grueso
        g.setColor(CAFE);
        g.fill(nube(0, 0, RX + 14, RY + 14, fase));
        g.setColor(CREMA);
        g.fill(nube(0, 0, RX + 9, RY + 9, fase));
        // relleno del color del rol
        Path2D adentro = nube(0, 0, RX - 6, RY - 6, fase);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0, -RY * 0.35), RX * 1.2f,
                new float[]{0f, 0.6f, 1f}, new Color[]{mezclar(claro, Color.WHITE, 0.3), claro, mezclar(c, Color.WHITE, 0.15)}));
        g.fill(adentro);
        // circulitos de fondo (textura suave)
        Shape clipAntes = g.getClip();
        g.clip(adentro);
        g.setColor(new Color(255, 255, 255, 45));
        for (int k = 0; k < 9; k++) {
            double ax = Math.cos(k * 2.4 + i) * RX * 0.7, ay = Math.sin(k * 1.9 + i) * RY * 0.6;
            double rr = 18 + (k * 13) % 30;
            g.fill(new Ellipse2D.Double(ax - rr, ay - rr, rr * 2, rr * 2));
        }
        // hojitas de laurel abajo
        g.setColor(new Color(0x4E9A3A));
        for (int lado = -1; lado <= 1; lado += 2) {
            for (int k = 0; k < 3; k++) {
                Estilo.hoja(g, lado * (RX * 0.62 + k * 16), RY * 0.42 - k * 24, lado > 0 ? -60 - k * 15 : 240 + k * 15, 36);
            }
        }
        // el personaje
        Actor f = figuras[i];
        f.x = 0;
        f.y = RY * 0.58;
        f.caminando = false;
        f.dibujar(g, tiempo * (1 + b));
        g.setClip(clipAntes);
        // brillo arriba
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Arc2D.Double(-RX * 0.8, -RY * 0.92, RX * 1.6, RY * 1.4, 110, 50, java.awt.geom.Arc2D.OPEN));
        g.setColor(CAFE);
        g.setStroke(new BasicStroke(3.5f));
        g.draw(adentro);
    }

    private void etiqueta(Graphics2D g, Rol r, double cx, double cy, double b) {
        g.setFont(Estilo.fuente(Font.BOLD, 38f));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(r.nombre);
        double h = 70, w = tw + 120;
        double x = cx - w / 2 + 20, y = cy - h / 2;
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 6, w, h, h, h));
        g.setPaint(new GradientPaint(0, (float) y, new Color(0xFFFCF0), 0, (float) (y + h), CREMA_2));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(CAFE);
        g.setStroke(new BasicStroke(4f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
        // circulo del icono
        double ic = h + 10;
        double ix = x - 22, iy = cy - ic / 2;
        g.setColor(new Color(0xFFF3D0));
        g.fill(new Ellipse2D.Double(ix, iy, ic, ic));
        g.setColor(CAFE);
        g.draw(new Ellipse2D.Double(ix, iy, ic, ic));
        icono(g, r, ix + ic / 2, cy, ic * 0.32);
        g.setColor(mezclar(CAFE, r.color, b * 0.4));
        g.drawString(r.nombre, (float) (x + 74), (float) (cy + fm.getAscent() * 0.36));
    }

    /** Iconos dibujados con codigo (alcaldia, periodico, play, personitas). */
    private static void icono(Graphics2D g, Rol r, double cx, double cy, double s) {
        Stroke borde = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(borde);
        switch (r) {
            case CANDIDATO: {
                g.setColor(new Color(0xF2B01E));
                g.fill(new Ellipse2D.Double(cx - s * 1.25, cy - s * 1.25, s * 2.5, s * 2.5));
                Path2D techo = new Path2D.Double();
                techo.moveTo(cx - s * 0.85, cy - s * 0.3);
                techo.lineTo(cx, cy - s * 0.85);
                techo.lineTo(cx + s * 0.85, cy - s * 0.3);
                techo.closePath();
                g.setColor(new Color(0x7A4A12));
                g.fill(techo);
                for (int k = -1; k <= 1; k++) {
                    g.fillRect((int) (cx + k * s * 0.5 - s * 0.1), (int) (cy - s * 0.25), (int) (s * 0.22), (int) (s * 0.75));
                }
                g.fillRect((int) (cx - s * 0.9), (int) (cy + s * 0.5), (int) (s * 1.8), (int) (s * 0.2));
                break;
            }
            case PERIODISTA: {
                Shape hoja = new RoundRectangle2D.Double(cx - s, cy - s * 0.8, s * 2, s * 1.6, 6, 6);
                g.setColor(Color.WHITE);
                g.fill(hoja);
                g.setColor(new Color(0x4A4A4A));
                g.draw(hoja);
                g.fillRect((int) (cx - s * 0.75), (int) (cy - s * 0.55), (int) (s * 0.6), (int) (s * 0.5));
                for (int k = 0; k < 4; k++) {
                    double ly = cy - s * 0.45 + k * s * 0.32;
                    g.drawLine((int) (cx + (k < 2 ? s * 0.0 : -s * 0.75)), (int) ly, (int) (cx + s * 0.75), (int) ly);
                }
                break;
            }
            case INFLUENCER: {
                Shape caja = new RoundRectangle2D.Double(cx - s * 1.1, cy - s * 0.8, s * 2.2, s * 1.6, s * 0.7, s * 0.7);
                g.setColor(new Color(0xE53935));
                g.fill(caja);
                Path2D play = new Path2D.Double();
                play.moveTo(cx - s * 0.3, cy - s * 0.45);
                play.lineTo(cx + s * 0.5, cy);
                play.lineTo(cx - s * 0.3, cy + s * 0.45);
                play.closePath();
                g.setColor(Color.WHITE);
                g.fill(play);
                break;
            }
            default: {   // CIUDADANO
                Color azul = new Color(0x3F8EDC);
                for (int k = -1; k <= 1; k += 2) {
                    double px = cx + k * s * 0.42;
                    g.setColor(mezclar(azul, Color.WHITE, k < 0 ? 0.35 : 0));
                    g.fill(new Ellipse2D.Double(px - s * 0.3, cy - s * 0.8, s * 0.6, s * 0.6));
                    g.fill(new java.awt.geom.Arc2D.Double(px - s * 0.6, cy - s * 0.15, s * 1.2, s * 1.4, 0, 180,
                            java.awt.geom.Arc2D.CHORD));
                }
                g.setColor(azul.darker());
                g.fill(new Ellipse2D.Double(cx - s * 0.32, cy - s * 0.95, s * 0.64, s * 0.64));
                g.fill(new java.awt.geom.Arc2D.Double(cx - s * 0.65, cy - s * 0.25, s * 1.3, s * 1.5, 0, 180,
                        java.awt.geom.Arc2D.CHORD));
                break;
            }
        }
    }

    private void cintaNombre(Graphics2D g, String texto, Color color) {
        AffineTransform t = g.getTransform();
        g.rotate(Math.toRadians(-7));
        g.setFont(Estilo.fuente(Font.BOLD, 36f));
        int tw = g.getFontMetrics().stringWidth(texto);
        double w = Math.max(240, tw + 80), h = 64, x = -w / 2, y = -h / 2 - 10;
        Color oscuro = mezclar(color, Color.BLACK, 0.35);
        for (int lado = -1; lado <= 1; lado += 2) {
            Path2D punta = new Path2D.Double();
            double bx = lado * (w / 2 - 10);
            punta.moveTo(bx, y + 12);
            punta.lineTo(bx + lado * 50, y + 12);
            punta.lineTo(bx + lado * 34, y + 12 + h / 2);
            punta.lineTo(bx + lado * 50, y + 12 + h);
            punta.lineTo(bx, y + 12 + h);
            punta.closePath();
            g.setColor(oscuro);
            g.fill(punta);
        }
        g.setColor(color);
        g.fill(new Rectangle((int) x, (int) y, (int) w, (int) h));
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8, 6}, 0));
        g.drawRect((int) x + 6, (int) y + 6, (int) w - 12, (int) h - 12);
        g.setColor(Color.WHITE);
        centrado(g, texto, 0, (int) (y + h / 2 + 13));
        g.setTransform(t);
    }

    private static void sello(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(0x2E9E4F));
        g.fill(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5f));
        g.draw(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D v = new Path2D.Double();
        v.moveTo(cx - 14, cy + 1);
        v.lineTo(cx - 3, cy + 12);
        v.lineTo(cx + 15, cy - 10);
        g.draw(v);
    }

    /** Globito de dialogo con la frase del personaje. */
    private void burbuja(Graphics2D g, int i) {
        Rol r = ORDEN[i];
        boolean ocupado = partida.ocupado(r);
        String linea1 = ocupado ? "Toca para soltar este rol" : "«" + r.frase + "»";
        String linea2 = ocupado ? "(si alguien se equivocó)" : "Habilidad: " + r.habilidad.nombre;
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        int w1 = g.getFontMetrics().stringWidth(linea1);
        g.setFont(Estilo.fuente(Font.PLAIN, 21f));
        int w2 = g.getFontMetrics().stringWidth(linea2);
        int w = Math.max(w1, w2) + 56, h = 92;
        double a = brillo[i];
        boolean alLado = r == Rol.CANDIDATO || r == Rol.INFLUENCER;
        double bx, by, px, py;
        if (alLado) {
            bx = CENTROS[i][0] + RX + 40;
            by = CENTROS[i][1] - h / 2.0 - 30;
            px = bx - 26;
            py = by + h / 2.0 + 8;
        } else {
            bx = CENTROS[i][0] - w / 2.0;
            by = CENTROS[i][1] - RY - h - 52;
            px = CENTROS[i][0];
            py = by + h + 26;
        }
        bx = Math.max(16, Math.min(W - w - 16, bx));
        by += (1 - a) * 12;
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, a * 1.2)));
        Path2D cola = new Path2D.Double();
        if (alLado) {
            cola.moveTo(bx + 4, by + h / 2.0 - 14);
            cola.lineTo(px, py);
            cola.lineTo(bx + 4, by + h / 2.0 + 14);
        } else {
            cola.moveTo(px - 16, by + h - 4);
            cola.lineTo(px, py);
            cola.lineTo(px + 16, by + h - 4);
        }
        cola.closePath();
        Shape caja = new RoundRectangle2D.Double(bx, by, w, h, 40, 40);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(AffineTransform.getTranslateInstance(3, 6).createTransformedShape(caja));
        g.setColor(Color.WHITE);
        g.fill(caja);
        g.fill(cola);
        g.setColor(CAFE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(caja);
        g.setColor(Color.WHITE);
        g.fill(cola);
        g.setColor(CAFE);
        if (alLado) {
            g.drawLine((int) bx + 2, (int) (by + h / 2.0 - 14), (int) px, (int) py);
            g.drawLine((int) px, (int) py, (int) bx + 2, (int) (by + h / 2.0 + 14));
        } else {
            g.drawLine((int) px - 16, (int) (by + h - 2), (int) px, (int) py);
            g.drawLine((int) px, (int) py, (int) px + 16, (int) (by + h - 2));
        }
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(CAFE);
        centrado(g, linea1, (int) (bx + w / 2.0), (int) by + 40);
        g.setFont(Estilo.fuente(Font.PLAIN, 21f));
        g.setColor(mezclar(r.color, Color.BLACK, 0.25));
        centrado(g, linea2, (int) (bx + w / 2.0), (int) by + 72);
        g.setComposite(antes);
    }

    // ---------------------------- abajo: progreso y empezar ----------------------------

    private void progreso(Graphics2D g) {
        int listos = partida.jugadores().size();
        if (partida.estaCompleta()) {
            double pulso = 0.5 + 0.5 * Math.sin((tiempo - listoDesde) * 4);
            double entra = Math.min(1, (tiempo - listoDesde) * 3);
            Rectangle r = new Rectangle(empezar);
            r.y += (int) ((1 - entra) * 140);
            // halo
            float rad = 230f;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(r.getCenterX(), r.getCenterY()), rad,
                    new float[]{0f, 1f}, new Color[]{new Color(255, 214, 74, (int) (60 + 50 * pulso)), new Color(255, 214, 74, 0)}));
            g.fill(new Ellipse2D.Double(r.getCenterX() - rad, r.getCenterY() - rad, rad * 2, rad * 2));
            botonPildora(g, r, "¡Empezar!", new Color(0x6CC04A), Color.WHITE, "empezar".equals(botonEncima), false, pulso);
            return;
        }
        // "2 de 4 listos" con puntitos de colores
        int x = 1560, y = 975, w = 320, h = 64;
        g.setColor(new Color(10, 6, 24, 150));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(new Color(255, 240, 200, 120));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
        for (int i = 0; i < 4; i++) {
            Rol r = ORDEN[i];
            double cx = x + 34 + i * 34, cy = y + h / 2.0;
            boolean ok = partida.ocupado(r);
            g.setColor(ok ? r.color : new Color(255, 255, 255, 50));
            g.fill(new Ellipse2D.Double(cx - 12, cy - 12, 24, 24));
            g.setColor(new Color(255, 255, 255, ok ? 230 : 90));
            g.draw(new Ellipse2D.Double(cx - 12, cy - 12, 24, 24));
        }
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(new Color(0xFFF0C8));
        g.drawString(listos + " de 4 listos", x + 170, y + 42);
    }

    /** Boton redondeado tipo pildora (Volver, Empezar, Cancelar...). */
    private void botonPildora(Graphics2D g, Rectangle r0, String texto, Color fondo, Color tinta,
            boolean encima, boolean flecha, double pulso) {
        double lift = encima ? -4 : 0;
        double grow = encima ? 4 : 0;
        double x = r0.x - grow, y = r0.y - grow + lift, w = r0.width + grow * 2, h = r0.height + grow * 2;
        double arco = h;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 8 - lift, w, h, arco, arco));
        g.setColor(CAFE);
        g.fill(new RoundRectangle2D.Double(x - 4, y - 4, w + 8, h + 8, arco + 8, arco + 8));
        Color arriba = mezclar(fondo, Color.WHITE, encima ? 0.35 : 0.2 + 0.15 * pulso);
        g.setPaint(new GradientPaint(0, (float) y, arriba, 0, (float) (y + h), fondo));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arco, arco));
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new RoundRectangle2D.Double(x + 18, y + 5, w - 36, h * 0.2, h * 0.2, h * 0.2));
        g.setFont(Estilo.fuente(Font.BOLD, (float) (h * 0.42)));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(texto);
        double tx = x + w / 2 - tw / 2.0 + (flecha ? 24 : 0);
        double ty = y + h / 2 + fm.getAscent() * 0.36;
        if (flecha) {
            double ax = tx - 50, ay = y + h / 2;
            g.setColor(tinta);
            g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) ax, (int) ay, (int) (ax + 32), (int) ay);
            Path2D p = new Path2D.Double();
            p.moveTo(ax + 14, ay - 16);
            p.lineTo(ax, ay);
            p.lineTo(ax + 14, ay + 16);
            g.draw(p);
        }
        if (tinta.equals(Color.WHITE)) {
            g.setColor(mezclar(fondo, Color.BLACK, 0.45));
            g.drawString(texto, (float) tx + 2, (float) ty + 3);
        }
        g.setColor(tinta);
        g.drawString(texto, (float) tx, (float) ty);
    }

    // ---------------------------- tarjeta del rol + nombre ----------------------------

    private void tarjetaRol(Graphics2D g, int i) {
        Rol r = ORDEN[i];
        double t = tarjeta;
        double s = easeOutBack(t);
        AffineTransform guardado = g.getTransform();
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, t * 2)));
        double cx = CARD.getCenterX(), cy = CARD.getCenterY();
        g.translate(cx, cy);
        g.scale(0.6 + 0.4 * s, 0.6 + 0.4 * s);
        g.translate(-cx, -cy);

        Estilo.madera(g, CARD.x, CARD.y, CARD.width, CARD.height, 60);
        Estilo.pergamino(g, CARD.x + 20, CARD.y + 20, CARD.width - 40, CARD.height - 40, 44);
        Estilo.hojas(g, CARD.x + 10, CARD.y + 14, 1, 1, 70);
        Estilo.hojas(g, CARD.x + CARD.width - 10, CARD.y + CARD.height - 14, -1, -1, 70);
        flor(g, CARD.x + 22, CARD.y + 22, 26);
        flor(g, CARD.x + CARD.width - 22, CARD.y + CARD.height - 22, 26);

        // insignia grande a la izquierda
        AffineTransform t2 = g.getTransform();
        g.translate(CARD.x + 230, CARD.y + 300 + Math.sin(tiempo * 2) * 5);
        g.scale(0.98, 0.98);
        String propia = "/imagenes/ui/rol_" + r.carpeta + ".png";
        if (Recursos.existe(propia)) {
            g.drawImage(Recursos.imagen(propia, 440, 380), -220, -190, 440, 380, null);
        } else {
            dibujarNube(g, r, i, 0.6);
        }
        g.setTransform(t2);

        // textos a la derecha
        int tx = CARD.x + 450, ancho = CARD.width - 450 - 60;
        g.setFont(Estilo.fuente(Font.BOLD, 64f));
        g.setColor(new Color(0, 0, 0, 40));
        g.drawString(r.nombre, tx + 2, CARD.y + 116);
        g.setColor(mezclar(r.color, Color.BLACK, 0.3));
        g.drawString(r.nombre, tx, CARD.y + 112);
        g.setFont(Estilo.fuente(Font.PLAIN, 24f));
        g.setColor(Estilo.TEXTO_SUAVE);
        int y = CARD.y + 152;
        for (String l : envolver(r.descripcion, g.getFontMetrics(), ancho)) {
            g.drawString(l, tx, y);
            y += 30;
        }
        y += 10;
        String[][] info = {{"Habilidad", r.habilidad.nombre}, {"Lo bueno", r.fortaleza}, {"Le falta", r.carencia}};
        Color[] puntos = {r.color, new Color(0x5BA843), new Color(0xC9553A)};
        for (int k = 0; k < info.length; k++) {
            g.setColor(puntos[k]);
            g.fill(new Ellipse2D.Double(tx, y - 16, 16, 16));
            g.setFont(Estilo.fuente(Font.BOLD, 23f));
            g.setColor(Estilo.TEXTO);
            String et = info[k][0] + ": ";
            g.drawString(et, tx + 26, y);
            int ew = g.getFontMetrics().stringWidth(et);
            g.setFont(Estilo.fuente(Font.PLAIN, 23f));
            List<String> lineas = envolver(info[k][1], g.getFontMetrics(), ancho - 26 - ew);
            for (String l : lineas) {
                g.drawString(l, tx + 26 + ew, y);
                y += 29;
            }
            y += 6;
        }

        // nombre
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(Estilo.TEXTO);
        g.drawString("¿Cómo te llamas?", cajaNombre.x, cajaNombre.y - 14);
        Shape caja = new RoundRectangle2D.Double(cajaNombre.x, cajaNombre.y, cajaNombre.width, cajaNombre.height, 30, 30);
        g.setColor(Color.WHITE);
        g.fill(caja);
        g.setColor(r.color);
        g.setStroke(new BasicStroke(4f));
        g.draw(caja);
        g.setFont(Estilo.fuente(Font.BOLD, 36f));
        boolean cursor = (tiempo % 1.0) < 0.55;
        int baseTexto = cajaNombre.y + 50;
        if (nombre.length() == 0) {
            g.setColor(new Color(0xBFAE95));
            g.drawString("Jugador " + turno(), cajaNombre.x + 24, baseTexto);
            if (cursor) {
                g.setColor(Estilo.TEXTO);
                g.fillRect(cajaNombre.x + 20, cajaNombre.y + 16, 3, 42);
            }
        } else {
            g.setColor(Estilo.TEXTO);
            g.drawString(nombre.toString(), cajaNombre.x + 24, baseTexto);
            if (cursor) {
                int cw = g.getFontMetrics().stringWidth(nombre.toString());
                g.fillRect(cajaNombre.x + 28 + cw, cajaNombre.y + 16, 3, 42);
            }
        }
        g.setFont(Estilo.fuente(Font.PLAIN, 18f));
        g.setColor(Estilo.TEXTO_SUAVE);
        g.drawString("Enter = listo  ·  Esc = cancelar", cajaNombre.x + 6, cajaNombre.y + cajaNombre.height + 26);

        botonPildora(g, cancelar, "Cancelar", CREMA, CAFE, "cancelar".equals(botonEncima), false, 0);
        botonPildora(g, ok, "¡Soy yo!", r.color, Color.WHITE, "ok".equals(botonEncima), false,
                0.5 + 0.5 * Math.sin(tiempo * 4));

        g.setComposite(antes);
        g.setTransform(guardado);
    }

    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    // ---------------------------- chispas ----------------------------

    private void dibujarChispas(Graphics2D g) {
        Composite antes = g.getComposite();
        for (Chispa c : chispas) {
            double a = Math.max(0, Math.min(1, c.vida / c.max));
            // las de ambiente titilan
            if (!c.estrella) {
                a *= 0.55 + 0.45 * Math.sin(c.vida * 7 + c.x);
                a = Math.min(a, (c.max - c.vida) * 2);   // aparecen suave
            }
            if (a <= 0.01) {
                continue;
            }
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a));
            double t = c.tam * (c.estrella ? 3.2 : 4);
            g.drawImage(glow, (int) (c.x - t / 2), (int) (c.y - t / 2), (int) t, (int) t, null);
            if (c.estrella) {
                g.setColor(c.color);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double l = c.tam * 0.9;
                g.drawLine((int) (c.x - l), (int) c.y, (int) (c.x + l), (int) c.y);
                g.drawLine((int) c.x, (int) (c.y - l), (int) c.x, (int) (c.y + l));
            }
        }
        g.setComposite(antes);
    }

    private static class Chispa {

        double x, y, vx, vy, vida, max, tam;
        boolean estrella;
        final Color color;

        Chispa(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    // ---------------------------- mariposas amarillas ----------------------------

    private class Mariposa {

        double x, y, ang, giro, vel, tam, fase, aleteo, rastro;
        final boolean atras;

        Mariposa(boolean atras) {
            this.atras = atras;
            x = rnd.nextDouble() * W;
            y = 120 + rnd.nextDouble() * (H - 240);
            ang = rnd.nextDouble() * Math.PI * 2;
            vel = atras ? 45 + rnd.nextDouble() * 30 : 70 + rnd.nextDouble() * 50;
            tam = atras ? 12 + rnd.nextDouble() * 5 : 18 + rnd.nextDouble() * 9;
            fase = rnd.nextDouble() * 10;
            aleteo = 11 + rnd.nextDouble() * 6;
        }

        void mover(double dt) {
            giro += rnd.nextGaussian() * 3.2 * dt;
            giro *= Math.pow(0.4, dt);
            giro = Math.max(-2.2, Math.min(2.2, giro));
            // si se acerca al borde, gira de vuelta hacia el centro
            double margen = 90;
            if (x < margen || x > W - margen || y < margen || y > H - margen) {
                double hacia = Math.atan2(H / 2.0 - y, W / 2.0 - x);
                double dif = Math.atan2(Math.sin(hacia - ang), Math.cos(hacia - ang));
                ang += dif * Math.min(1, dt * 2.2);
            }
            ang += giro * dt;
            double v = vel * (0.8 + 0.35 * Math.abs(Math.sin(tiempo * aleteo * 0.5 + fase)));
            x += Math.cos(ang) * v * dt;
            y += Math.sin(ang) * v * dt + Math.sin(tiempo * 3 + fase) * 18 * dt;

            // la lucecita que cargan va dejando polvo dorado
            rastro -= dt;
            if (rastro <= 0) {
                rastro = atras ? 0.12 : 0.06;
                double[] l = luz();
                Chispa c = new Chispa(l[0] + (rnd.nextDouble() - 0.5) * 6, l[1], AMARILLO);
                c.vx = (rnd.nextDouble() - 0.5) * 14;
                c.vy = 12 + rnd.nextDouble() * 22;
                c.vida = c.max = 0.7 + rnd.nextDouble() * 0.8;
                c.tam = (atras ? 2.2 : 3.2) + rnd.nextDouble() * 2;
                c.estrella = rnd.nextInt(5) == 0;
                chispas.add(c);
            }
        }

        /** Posicion de la lucecita (cuelga un poquito debajo de la mariposa y se mece). */
        double[] luz() {
            return new double[]{x + Math.sin(tiempo * 2.3 + fase) * tam * 0.35, y + tam * 1.25};
        }

        void dibujar(Graphics2D g) {
            float alfa = atras ? 0.7f : 1f;
            Composite antes = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
            double[] l = luz();
            double pulso = 0.8 + 0.2 * Math.sin(tiempo * 5 + fase);

            // halo de la luz (grande y suave)
            double gt = tam * 5.5 * pulso;
            g.drawImage(glow, (int) (l[0] - gt / 2), (int) (l[1] - gt / 2), (int) gt, (int) gt, null);

            // hilito y luz
            g.setColor(new Color(255, 235, 170, 140));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine((int) x, (int) (y + tam * 0.3), (int) l[0], (int) l[1]);
            double lt = tam * 0.55;
            g.setColor(new Color(255, 250, 225));
            g.fill(new Ellipse2D.Double(l[0] - lt / 2, l[1] - lt / 2, lt, lt));

            AffineTransform t = g.getTransform();
            g.translate(x, y);
            g.rotate(ang + Math.PI / 2);
            double abre = 0.18 + 0.82 * Math.abs(Math.sin(tiempo * aleteo + fase));
            for (int lado = -1; lado <= 1; lado += 2) {
                AffineTransform t3 = g.getTransform();
                g.scale(lado * abre, 1);
                ala(g, tam);
                g.setTransform(t3);
            }
            // cuerpo y antenas
            g.setColor(new Color(0x3A2A10));
            g.fill(new Ellipse2D.Double(-tam * 0.09, -tam * 0.55, tam * 0.18, tam * 1.1));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(0, (int) (-tam * 0.5), (int) (-tam * 0.3), (int) (-tam * 0.95));
            g.drawLine(0, (int) (-tam * 0.5), (int) (tam * 0.3), (int) (-tam * 0.95));
            g.setTransform(t);
            g.setComposite(antes);
        }

        private void ala(Graphics2D g, double s) {
            Path2D arriba = new Path2D.Double();
            arriba.moveTo(0, -s * 0.15);
            arriba.curveTo(s * 0.35, -s * 1.05, s * 1.15, -s * 1.0, s * 1.0, -s * 0.35);
            arriba.curveTo(s * 0.95, -s * 0.05, s * 0.5, s * 0.08, 0, s * 0.05);
            arriba.closePath();
            Path2D abajo = new Path2D.Double();
            abajo.moveTo(0, s * 0.02);
            abajo.curveTo(s * 0.55, s * 0.05, s * 0.85, s * 0.45, s * 0.6, s * 0.75);
            abajo.curveTo(s * 0.4, s * 0.95, s * 0.1, s * 0.6, 0, s * 0.25);
            abajo.closePath();
            g.setPaint(new GradientPaint(0, 0, new Color(0xFFF07A), (float) s, (float) -s * 0.4f, new Color(0xF5B800)));
            g.fill(abajo);
            g.fill(arriba);
            g.setColor(new Color(0xB07A00));
            g.setStroke(new BasicStroke(1.3f));
            g.draw(arriba);
            g.draw(abajo);
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(new Ellipse2D.Double(s * 0.55, -s * 0.7, s * 0.18, s * 0.18));
        }
    }

    // ---------------------------- util ----------------------------

    private static Color mezclar(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
