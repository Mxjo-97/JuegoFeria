package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cinematicas con fondos, sprites y efectos animados independientes.
 *
 * Por cuadro se puede declarar:
 *   .sprite(ruta, x, y, ancho, alto)    PNG transparente sobre el fondo
 *   .efecto(Efectos.xxx(...))           olas, viento, niebla, mariposa, etc. (ver Efectos.java)
 *   .camara(zIni, zFin, panX, panY)     movimiento de camara (por defecto: acercamiento lento)
 *   .duracion(segundos)                 avance automatico (cuadros sin dialogo y titulos)
 *
 * Look de cine automatico: fundidos, barras negras, vineta, grano de pelicula,
 * camara con respiracion y texto que aparece con retraso.
 * Si un PNG aun no existe, se dibuja un marcador provisional con el nombre del recurso.
 */
public class Cinematica extends PanelDiseno {

    // ---- interruptores del look (ponlos en false si el PC se pone lento) ----
    private static final boolean CINE_BARRAS = true;
    private static final boolean GRANO_ACTIVO = true;
    private static final boolean VINETA_ACTIVA = true;

    private static final int BARRA = 60;               // alto de cada barra negra (px de diseno)
    private static final double DUR_ENTRADA = 1.0;     // fundido desde negro al empezar un cuadro
    private static final double DUR_SALIDA = 0.5;      // fundido a negro al salir de un cuadro
    private static final double DUR_CAMARA = 16.0;     // lo que tarda la camara en completar su recorrido
    private static final double LETRAS_POR_SEG = 38;

    private static final class SpriteEscena {
        final String ruta;
        final int x, y, ancho, alto;
        final double rotacion;

        SpriteEscena(String ruta, int x, int y, int ancho, int alto, double rotacion) {
            this.ruta = ruta;
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.rotacion = rotacion;
        }
    }

    private static final class Cuadro {
        String fondo, acotacion, sonido, ambiente;
        String tituloGrande, tituloPequeno;
        final List<String[]> lineas = new ArrayList<>();
        final List<SpriteEscena> sprites = new ArrayList<>();
        final List<Efecto1> efectos = new ArrayList<>();
        double zIni = 1.04, zFin = 1.11, panX = 36, panY = 14;
        double duracionAuto;
    }

    // ---- recursos del look (se crean una sola vez) ----
    /** Vineta ya dibujada una sola vez (mucho mas barata que recalcular el degradado en cada cuadro). */
    private static final BufferedImage VINETA = crearVineta();

    private static BufferedImage crearVineta() {
        BufferedImage img = new BufferedImage(PanelDiseno.W, PanelDiseno.H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D v = img.createGraphics();
        v.translate(PanelDiseno.W / 2.0, PanelDiseno.H / 2.0);
        v.scale(1, PanelDiseno.H / (double) PanelDiseno.W);
        v.setPaint(new RadialGradientPaint(0f, 0f, PanelDiseno.W * 0.62f,
                new float[]{0.5f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}));
        v.fillRect(-PanelDiseno.W / 2 - 4, -PanelDiseno.W / 2 - 4, PanelDiseno.W + 8, PanelDiseno.W + 8);
        v.dispose();
        return img;
    }

    private static final BufferedImage[] GRANO = new BufferedImage[4];
    private static final BufferedImage HALO = Efectos.brillo(new Color(255, 200, 80), 256);

    static {
        Random r = new Random(99);
        for (int k = 0; k < GRANO.length; k++) {
            BufferedImage img = new BufferedImage(480, 270, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 270; y++) {
                for (int x = 0; x < 480; x++) {
                    int v = r.nextInt(256);
                    img.setRGB(x, y, (14 << 24) | (v << 16) | (v << 8) | v);
                }
            }
            GRANO[k] = img;
        }
    }

    private final Partida partida;
    private final List<Cuadro> cuadros = new ArrayList<>();
    private int iCuadro = -1, iLinea = -1;
    private double letras, tCuadro;
    private double tEscena;            // segundos desde que empezo el cuadro (no se reinicia por linea)
    private double tTotal;             // segundos desde que empezo la cinematica
    private double saliendo = -1;      // >= 0 mientras el cuadro se esta desvaneciendo
    private boolean saltable = true;
    private Runnable alTerminar;
    private boolean terminada;

    private static final java.awt.Rectangle BOTON_SALTAR = new java.awt.Rectangle(1660, 30, 230, 64);

    public Cinematica(Partida partida) {
        this.partida = partida;
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && saltable) {
                    terminar();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    avanzar();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (saltable && BOTON_SALTAR.contains(aDiseno(e.getPoint()))) {
                    terminar();
                } else {
                    avanzar();
                }
            }
        });
    }

    // ===================== ARMAR =====================

    public Cinematica cuadro(String fondo, String acotacion, String... lineas) {
        Cuadro c = new Cuadro();
        c.fondo = fondo;
        c.acotacion = acotacion;
        c.panX = (cuadros.size() % 2 == 0 ? 1 : -1) * 36;
        for (String l : lineas) {
            String t = partida != null ? partida.reemplazar(l) : l;
            int i = t.indexOf(": ");
            String quien = i > 0 && i < 28 ? t.substring(0, i) : null;
            String texto = quien != null ? t.substring(i + 2) : t;
            if (quien != null && partida != null) {
                for (Rol r : Rol.values()) {
                    if (quien.equals(r.name()) && partida.jugador(r) != null) {
                        quien = partida.jugador(r).nombre;
                    }
                }
            }
            c.lineas.add(new String[]{quien == null ? null : bonito(quien), texto});
        }
        // un cuadro sin dialogo avanza solo (si no, se quedaria esperando un clic sobre nada)
        if (c.lineas.isEmpty()) {
            c.duracionAuto = 4.5;
        }
        cuadros.add(c);
        return this;
    }

    /** Agrega un efecto de una sola reproducción al último cuadro. */
    public Cinematica sonido(String ruta) {
        if (!cuadros.isEmpty()) cuadros.get(cuadros.size() - 1).sonido = ruta;
        return this;
    }

    /** Agrega un ambiente en bucle al último cuadro. */
    public Cinematica ambiente(String ruta) {
        if (!cuadros.isEmpty()) cuadros.get(cuadros.size() - 1).ambiente = ruta;
        return this;
    }

    /**
     * Agrega un PNG transparente sobre el fondo del cuadro actual.
     * x/y/ancho/alto se expresan en el lienzo de diseño 1920x1080.
     */
    public Cinematica sprite(String ruta, int x, int y, int ancho, int alto) {
        return sprite(ruta, x, y, ancho, alto, 0);
    }

    /** Igual que sprite(...), pero permite girarlo; util para reutilizar el sprite acostado. */
    public Cinematica sprite(String ruta, int x, int y, int ancho, int alto, double rotacionGrados) {
        if (!cuadros.isEmpty() && ruta != null && ancho > 0 && alto > 0) {
            cuadros.get(cuadros.size() - 1).sprites.add(
                    new SpriteEscena(ruta, x, y, ancho, alto, rotacionGrados));
        }
        return this;
    }

    /** Agrega un efecto animado (ver Efectos) al último cuadro. Se pueden poner varios. */
    public Cinematica efecto1(Efecto1 e) {
        if (!cuadros.isEmpty() && e != null) cuadros.get(cuadros.size() - 1).efectos.add(e);
        return this;
    }

    /**
     * Movimiento de camara del último cuadro. zIni/zFin son el zoom (1.0 = sin zoom; usa al menos 1.03
     * para que no se vean bordes). panX/panY son cuantos pixeles se desliza la camara (maximo ~40 en x, ~20 en y).
     */
    public Cinematica camara(double zIni, double zFin, double panX, double panY) {
        if (!cuadros.isEmpty()) {
            Cuadro c = cuadros.get(cuadros.size() - 1);
            c.zIni = zIni;
            c.zFin = zFin;
            c.panX = panX;
            c.panY = panY;
        }
        return this;
    }

    /** Segundos tras los cuales el último cuadro avanza solo (0 = espera clic). */
    public Cinematica duracion(double segundos) {
        if (!cuadros.isEmpty()) cuadros.get(cuadros.size() - 1).duracionAuto = segundos;
        return this;
    }

    public Cinematica titulo(String grande, String pequeno) {
        Cuadro c = new Cuadro();
        c.tituloGrande = grande;
        c.tituloPequeno = pequeno;
        c.duracionAuto = 7.5;
        cuadros.add(c);
        return this;
    }

    public Cinematica saltable(boolean s) {
        this.saltable = s;
        return this;
    }

    private static String bonito(String quien) {
        if (!quien.equals(quien.toUpperCase())) return quien;
        StringBuilder sb = new StringBuilder();
        for (String p : quien.toLowerCase().split(" ")) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static double curva(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    // ===================== CICLO =====================

    public void iniciar(Runnable alTerminar) {
        this.alTerminar = alTerminar;
        terminada = false;
        iCuadro = -1;
        tTotal = 0;
        saliendo = -1;
        siguienteCuadro();
    }

    private void siguienteCuadro() {
        detenerAmbienteActual();
        iCuadro++;
        iLinea = -1;
        tCuadro = 0;
        tEscena = 0;
        saliendo = -1;
        letras = 0;
        if (iCuadro >= cuadros.size()) {
            terminar();
            return;
        }
        Cuadro c = cuadros.get(iCuadro);
        if (c.sonido != null) Funciones.reproducirSonido(c.sonido);
        if (c.ambiente != null) Musica.tocar(c.ambiente);
        siguienteLinea();
        // el texto espera un momento a que la imagen se asiente antes de escribirse
        letras = -LETRAS_POR_SEG * 0.7;
        tCuadro = 0;
    }

    private void siguienteLinea() {
        if (terminada || iCuadro < 0 || iCuadro >= cuadros.size()) return;
        iLinea++;
        letras = 0;
        tCuadro = 0;
    }

    private void iniciarSalida() {
        if (saliendo < 0) saliendo = 0;
    }

    private void detenerAmbienteActual() {
        if (iCuadro >= 0 && iCuadro < cuadros.size()) {
            Cuadro actual = cuadros.get(iCuadro);
            if (actual.ambiente != null) Musica.parar();
        }
    }

    @Override
    protected void alOcultar() {
        // Evita que un ambiente siga sonando si otra pantalla reemplaza esta cinematica.
        detenerAmbienteActual();
    }

    private void avanzar() {
        if (terminada || iCuadro < 0 || iCuadro >= cuadros.size() || saliendo >= 0) return;
        Cuadro c = cuadros.get(iCuadro);
        if (iLinea >= 0 && iLinea < c.lineas.size()) {
            String t = c.lineas.get(iLinea)[1];
            if (letras < t.length()) {
                letras = t.length();
                return;
            }
        }
        if (iLinea + 1 < c.lineas.size()) {
            siguienteLinea();
        } else if (tCuadro > 0.4 && tEscena > (c.lineas.isEmpty() ? 1.5 : 0.6)) {
            iniciarSalida();
        }
    }

    private void terminar() {
        if (terminada) return;
        detenerAmbienteActual();
        terminada = true;
        if (alTerminar != null) {
            Runnable r = alTerminar;
            alTerminar = null;
            r.run();
        }
    }

    @Override
    protected void actualizar(double dt) {
        if (terminada || iCuadro < 0 || iCuadro >= cuadros.size()) return;
        tCuadro += dt;
        tEscena += dt;
        tTotal += dt;
        letras += dt * LETRAS_POR_SEG;
        if (saliendo >= 0) {
            saliendo += dt;
            if (saliendo >= DUR_SALIDA) {
                siguienteCuadro();
            }
        } else {
            Cuadro c = cuadros.get(iCuadro);
            if (c.duracionAuto > 0 && tEscena >= c.duracionAuto) {
                iniciarSalida();
            }
        }
    }

    // ===================== DIBUJO =====================

    @Override
    protected void dibujar(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        if (iCuadro < 0 || iCuadro >= cuadros.size()) return;
        Cuadro c = cuadros.get(iCuadro);
        double fadeSalida = saliendo >= 0 ? curva(saliendo / DUR_SALIDA) : 0;
        double visible = curva(tEscena / DUR_ENTRADA) * (1 - fadeSalida);
        boolean esTitulo = c.tituloGrande != null;

        if (esTitulo) {
            dibujarTitulo(g, c);
        } else {
            dibujarEscena(g, c);
        }
        postproceso(g, visible);

        if (esTitulo) {
            pista(g);
        } else if (iLinea >= 0 && iLinea < c.lineas.size()) {
            Graphics2D d = (Graphics2D) g.create();
            double a = curva((tEscena - 0.35) / 0.45) * (1 - fadeSalida);
            d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, a))));
            dibujarLinea(d, c.lineas.get(iLinea));
            d.dispose();
        }
        if (saltable) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(BOTON_SALTAR.x, BOTON_SALTAR.y, BOTON_SALTAR.width, BOTON_SALTAR.height, 32, 32);
            g.setColor(Color.WHITE);
            g.setFont(Estilo.fuente(Font.BOLD, 26f));
            centrado(g, "Saltar (Esc)", BOTON_SALTAR.x + BOTON_SALTAR.width / 2, BOTON_SALTAR.y + 42);
        }
    }

    /** Fondo + sprites + efectos, todo bajo la camara. */
    private void dibujarEscena(Graphics2D g, Cuadro c) {
        double p = curva(Math.min(1, tEscena / DUR_CAMARA));
        double z = c.zIni + (c.zFin - c.zIni) * p;
        double mx = W * (z - 1) / 2 - 2;
        double my = H * (z - 1) / 2 - 2;
        // "respiracion" de camara: temblor muy suave para que nada se sienta congelado
        double ox = c.panX * (p - 0.5) + 3.0 * Math.sin(tTotal * 0.7);
        double oy = c.panY * (p - 0.5) + 2.0 * Math.sin(tTotal * 0.9 + 1);
        ox = Math.max(-mx, Math.min(mx, ox));
        oy = Math.max(-my, Math.min(my, oy));

        Graphics2D cam = (Graphics2D) g.create();
        cam.translate(W / 2.0 + ox, H / 2.0 + oy);
        cam.scale(z, z);
        cam.translate(-W / 2.0, -H / 2.0);

        if (c.fondo != null && Recursos.existe(c.fondo)) {
            // Las cinematicas usan lienzo 1920x1080; no reutilizar el escalado 1591x989 del mundo.
            BufferedImage fondo = Recursos.imagen(c.fondo, W, H);
            cam.drawImage(fondo, 0, 0, W, H, null);
        } else if (c.acotacion != null) {
            cam.setFont(Estilo.fuente(Font.PLAIN, 34f));
            cam.setColor(new Color(200, 190, 170));
            FontMetrics fm = cam.getFontMetrics();
            int y = 300;
            for (String l : envolver("(" + c.acotacion + ")", fm, 1400)) {
                centrado(cam, l, W / 2, y);
                y += fm.getHeight() + 6;
            }
            if (c.fondo != null) {
                cam.setFont(Estilo.fuente(Font.PLAIN, 18f));
                cam.setColor(new Color(120, 120, 120));
                centrado(cam, "falta la imagen: " + c.fondo, W / 2, 1050);
            }
        }

        // Los sprites se dibujan encima del fondo y debajo de la caja de diálogo.
        for (SpriteEscena sprite : c.sprites) dibujarSprite(cam, sprite);

        for (Efecto1 e : c.efectos) {
            Graphics2D eg = (Graphics2D) cam.create();
            e.dibujar(eg, tEscena);
            eg.dispose();
        }
        cam.dispose();
    }

    /** Vineta, grano de pelicula, fundido a negro y barras de cine. */
    private void postproceso(Graphics2D g, double visible) {
        if (VINETA_ACTIVA) {
            g.drawImage(VINETA, 0, 0, null);
        }
        if (GRANO_ACTIVO) {
            Graphics2D q = (Graphics2D) g.create();
            q.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            q.drawImage(GRANO[(int) (tTotal * 14) % GRANO.length], 0, 0, W, H, null);
            q.dispose();
        }
        if (visible < 1) {
            g.setColor(new Color(0, 0, 0, (int) Math.max(0, Math.min(255, 255 * (1 - visible)))));
            g.fillRect(0, 0, W, H);
        }
        if (CINE_BARRAS) {
            int b = (int) (BARRA * curva(tTotal / 1.4));
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, W, b);
            g.fillRect(0, H - b, W, b);
        }
    }

    private void dibujarSprite(Graphics2D g, SpriteEscena s) {
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        p.translate(s.x + s.ancho / 2.0, s.y + s.alto / 2.0);
        p.rotate(Math.toRadians(s.rotacion));
        p.translate(-s.ancho / 2.0, -s.alto / 2.0);
        // respiracion: el personaje se infla y desinfla apenas, anclado en los pies
        double resp = Math.sin(tEscena * 1.5 + s.x * 0.01);
        p.translate(s.ancho / 2.0, s.alto);
        p.scale(1 + 0.006 * resp, 1 + 0.012 * resp);
        p.translate(-s.ancho / 2.0, -s.alto);

        if (Recursos.existe(s.ruta)) {
            BufferedImage img = Recursos.imagen(s.ruta, s.ancho, s.alto);
            p.drawImage(img, 0, 0, s.ancho, s.alto, null);
            p.dispose();
            return;
        }

        // Marcador visual temporal: permite programar y ajustar posiciones antes de tener los PNG.
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(new Color(12, 25, 48, 75));
        p.fill(new Ellipse2D.Double(s.ancho * 0.12, s.alto * 0.88, s.ancho * 0.76, s.alto * 0.10));
        p.setColor(new Color(255, 239, 205, 220));
        p.setStroke(new BasicStroke(4f));
        p.draw(new RoundRectangle2D.Double(0, 0, s.ancho, s.alto, 30, 30));
        p.setColor(new Color(255, 244, 220, 35));
        p.fill(new RoundRectangle2D.Double(3, 3, s.ancho - 6, s.alto - 6, 26, 26));
        p.setColor(new Color(255, 239, 205, 230));
        p.setFont(Estilo.fuente(Font.BOLD, 20f));
        String nombre = nombreArchivo(s.ruta);
        FontMetrics fm = p.getFontMetrics();
        for (String linea : envolver(nombre, fm, s.ancho - 18)) {
            centrado(p, linea, s.ancho / 2, s.alto / 2);
        }
        p.dispose();
    }

    private static String nombreArchivo(String ruta) {
        int barra = ruta.lastIndexOf('/');
        String nombre = barra >= 0 ? ruta.substring(barra + 1) : ruta;
        int punto = nombre.lastIndexOf('.');
        return punto > 0 ? nombre.substring(0, punto) : nombre;
    }

    private void dibujarLinea(Graphics2D g, String[] l) {
        int bx = 260, by = 790, bw = 1400, bh = 230;
        if (Recursos.existe(EscenaMundo.MARCO_BURBUJA)) {
            g.drawImage(Recursos.imagen(EscenaMundo.MARCO_BURBUJA, bw, bh), bx, by, bw, bh, null);
        } else {
            Estilo.pergamino(g, bx, by, bw, bh, 44);
            g.setColor(Estilo.MADERA_OSCURA);
            g.setStroke(new BasicStroke(6f));
            g.drawRoundRect(bx, by, bw, bh, 44, 44);
        }
        if (l[0] != null) {
            Estilo.madera(g, bx + 50, by - 34, 330, 64, 28);
            g.setFont(Estilo.fuente(Font.BOLD, 34f));
            g.setColor(new Color(0xFFF3D6));
            centrado(g, l[0], bx + 50 + 165, by + 10);
        }
        g.setFont(Estilo.fuente(Font.PLAIN, 40f));
        FontMetrics fm = g.getFontMetrics();
        int visibles = (int) Math.max(0, Math.min(l[1].length(), letras));
        int usados = 0, y = by + 82;
        g.setColor(Estilo.TEXTO);
        for (String s : envolver(l[1], fm, bw - 150)) {
            if (usados >= visibles) break;
            int n = Math.min(s.length(), visibles - usados);
            g.drawString(s.substring(0, n), bx + 70, y);
            usados += s.length() + 1;
            y += fm.getHeight() + 6;
        }
        // flechita que late cuando la linea ya termino de escribirse
        if (letras >= l[1].length()) {
            double rebote = 6 * Math.sin(tEscena * 4);
            int fx = bx + bw - 80, fy = (int) (by + bh - 52 + rebote);
            g.setColor(Estilo.MADERA_OSCURA);
            g.fillPolygon(new int[]{fx, fx + 28, fx + 14}, new int[]{fy, fy, fy + 20}, 3);
        }
    }

    // ===================== TITULO =====================

    private static double anchoEspaciado(FontMetrics fm, String s, double espacio) {
        double w = 0;
        for (int i = 0; i < s.length(); i++) w += fm.charWidth(s.charAt(i)) + espacio;
        return w - espacio;
    }

    /** Texto centrado en (dx, dy) con separacion extra entre letras. */
    private static void textoEspaciado(Graphics2D g, String s, double dx, double dy, double espacio) {
        FontMetrics fm = g.getFontMetrics();
        double x = dx - anchoEspaciado(fm, s, espacio) / 2;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            g.drawString(String.valueOf(ch), (float) x, (float) dy);
            x += fm.charWidth(ch) + espacio;
        }
    }

    private void dibujarTitulo(Graphics2D g, Cuadro c) {
        double t = tEscena;
        Graphics2D base = (Graphics2D) g.create();
        base.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        base.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // resplandor dorado detras del texto
        Efectos.mancha(base, HALO, W / 2.0, H / 2.0, 1500, 800,
                0.38 * curva(t / 2.5) * (0.9 + 0.1 * Math.sin(t * 1.3)));
        for (Efecto1 e : c.efectos) {
            Graphics2D eg = (Graphics2D) base.create();
            e.dibujar(eg, t);
            eg.dispose();
        }

        double aparece = curva((t - 0.4) / 1.8);
        double asent = curva(t / 4.5);
        double esc = 1.08 - 0.08 * asent;
        double espacio = 22 - 14 * asent;

        Graphics2D tg = (Graphics2D) base.create();
        tg.translate(W / 2.0, H / 2.0);
        tg.scale(esc, esc);
        tg.setFont(Estilo.fuente(Font.BOLD, 96f));
        FontMetrics fm = tg.getFontMetrics();
        double fit = Math.min(1, (W - 240) / anchoEspaciado(fm, c.tituloGrande, espacio));
        tg.scale(fit, fit);

        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            tg.setColor(new Color(255, 200, 70, (int) (34 * aparece)));
            textoEspaciado(tg, c.tituloGrande, Math.cos(ang) * 5, Math.sin(ang) * 5, espacio);
        }
        tg.setColor(new Color(255, 211, 92, (int) (255 * aparece)));
        textoEspaciado(tg, c.tituloGrande, 0, 0, espacio);

        double lw = 560 * curva((t - 1.2) / 1.3);
        tg.setStroke(new BasicStroke(3f));
        tg.setColor(new Color(255, 211, 92, (int) (200 * aparece)));
        tg.draw(new Line2D.Double(-lw / 2, 34, lw / 2, 34));

        if (c.tituloPequeno != null) {
            tg.setFont(Estilo.fuente(Font.PLAIN, 36f));
            tg.setColor(new Color(255, 255, 255, (int) (220 * curva((t - 1.9) / 1.3))));
            textoEspaciado(tg, c.tituloPequeno, 0, 96, 3);
        }
        tg.dispose();
        base.dispose();
    }

    private void pista(Graphics2D g) {
        if (tEscena > 2.5 && saliendo < 0) {
            g.setFont(Estilo.fuente(Font.PLAIN, 22f));
            g.setColor(new Color(255, 255, 255, (int) (150 * curva((tEscena - 2.5) / 1.0))));
            centrado(g, "clic para continuar", W / 2, H - 22);
        }
    }
}
