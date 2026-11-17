package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base de todos los minijuegos. Cada uno pasa por 3 fases, con la misma presentacion:
 *   INTRO (tarjeta con instrucciones) -> JUGANDO -> FIN (tarjeta con el resultado).
 *
 * Para crear uno nuevo, hereda de esta clase e implementa:
 *   nombre(), instrucciones(), prepararPartida(), actualizarJuego(dt), dibujarJuego(g)
 *   y (opcional) clicJuego(p, boton). Cuando acabe, llama a terminarJuego(...).
 * Despues registralo en Minijuegos.
 *
 * Uso desde el juego:
 *   Minijuego mj = Minijuegos.crear(Rol.INFLUENCER, 0);
 *   mj.iniciar(resultado -> { Minijuegos.aplicar(resultado, estado); ...volver al mapa... });
 */
public abstract class Minijuego extends PanelDiseno {

    /** El boton del parlante (volumen) tambien esta aqui. */
    @Override
    protected boolean conAjustes() {
        return true;
    }

    /** Niveles del guion (van en Resultado.puntos): bien / regular / mal. */
    public static final int BIEN = 2, REGULAR = 1, MAL = 0;

    /** Lo que devuelve un minijuego cuando termina. puntos = BIEN, REGULAR o MAL. */
    public static final class Resultado {

        public final boolean exito;
        public final int puntos;
        public final Efecto efecto;
        public final String resumen;

        Resultado(boolean exito, int puntos, Efecto efecto, String resumen) {
            this.exito = exito;
            this.puntos = puntos;
            this.efecto = efecto;
            this.resumen = resumen;
        }
    }

    protected enum Fase { INTRO, JUGANDO, FIN }

    /** En red: si no es null, solo ese jugador puede hacer clic/teclas en el minijuego (los demas miran). */
    Rol soloRol;

    /** Quien lo juega (lo pone Lugar.minijuegoFijo). Los otros tres aconsejan con globos. null = prueba suelta. */
    Rol juega;

    /** true = los otros tres no pueden aconsejar (ej: la votacion "cada uno por su lado"). */
    protected boolean sinConsejos;

    public Minijuego sinConsejos(boolean b) {
        this.sinConsejos = b;
        return this;
    }

    /** Nombre de cada jugador (lo pone Lugar.minijuegoFijo) para las burbujas del final. */
    Map<Rol, String> nombres;

    /**
     * Lo que dicen los personajes AL FINAL, segun el nivel (BIEN / REGULAR / MAL), con el formato del guion:
     * "CIUDADANO: texto", "RECTORA VISITACIÓN: texto", "Narrador: texto". Se muestra en burbujas con la cara
     * de cada personaje (sprites de definitiva_560x680). Si no se pone, se usa el resumen del minijuego.
     */
    private java.util.function.IntFunction<String[]> dialogoFinal;

    public Minijuego dialogoFinal(java.util.function.IntFunction<String[]> f) {
        this.dialogoFinal = f;
        return this;
    }

    protected Fase fase = Fase.INTRO;
    protected double tFase;
    private Consumer<Resultado> alTerminar;
    private Resultado resultado;

    protected Minijuego() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                procesarClic(aDiseno(e.getPoint()), e.getButton());
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                procesarTecla(e.getKeyCode());
            }
        });
    }

    @Override
    protected boolean aceptaEntradaDe(Rol r) {
        return soloRol == null || soloRol == r;
    }

    // ===================== LO QUE IMPLEMENTA CADA MINIJUEGO =====================

    public abstract String nombre();

    /** Texto de la tarjeta de inicio. Usa \n para separar lineas. */
    public abstract String instrucciones();

    /** Deja todo en su estado inicial (se llama en iniciar()). */
    protected abstract void prepararPartida();

    protected abstract void actualizarJuego(double dt);

    protected abstract void dibujarJuego(Graphics2D g);

    protected void clicJuego(Point p, int boton) {
    }

    protected void teclaJuego(int codigo) {
    }

    /**
     * Carpeta de arte del minijuego (prompt del generador, seccion 7): kit, rastrea, fecha_agua, papel,
     * pergaminos, cinco_noticia, circular, quien_mira, lista_permiso. null = solo el kit general.
     */
    protected String carpetaArte() {
        return null;
    }

    /** Icono grande de la accion en el aviso "Cómo se juega" (opcional). */
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        ArteMj.mariposa(g, cx, cy, 150, -10 + Math.sin(tiempo * 3) * 6);
    }

    // ===================== AYUDAS PARA DIBUJAR =====================

    /** Fondo del minijuego: imagen de Canva si existe; si no, un degradado calido. */
    protected void fondo(Graphics2D g, String ruta) {
        if (ruta != null && Recursos.existe(ruta)) {
            g.drawImage(Recursos.fondo(ruta), 0, 0, W, H, null);
        } else {
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0xFBE7C2), 0, H, new Color(0xF0C58A)));
            g.fillRect(0, 0, W, H);
        }
    }

    /** Boton del kit (pildora crema tipo sticker). */
    protected static void boton(Graphics2D g, java.awt.Rectangle r, String texto, boolean activo) {
        ArteMj.boton(g, r.x, r.y, r.width, r.height, texto, activo ? 0 : 2);
    }

    /**
     * Titulo (el banner de cada minijuego) y el consejo de uno de los otros tres jugadores,
     * en una burbuja con su cara (sprite oficial de definitiva_560x680).
     */
    protected void cabecera(Graphics2D g, String titulo, String consejo) {
        ArteMj.titulo(g, carpetaArte(), titulo, 26, 8, 118);
        if (consejo == null) {
            return;
        }
        if (sinConsejos) {
            g.setFont(ArteMj.letra(24f));
            int w = g.getFontMetrics().stringWidth("Entraron cada uno por su lado: nadie aconseja.") + 60;
            ArteMj.pildora(g, 40, 1000, w, 54, ArteMj.CREMA, 4f);
            ArteMj.centrado(g, "Entraron cada uno por su lado: nadie aconseja.", 40 + w / 2.0, 1036, ArteMj.TEXTO);
            return;
        }
        Rol quien = consejero(consejo);
        g.setFont(ArteMj.letra(26f));
        String txt = consejo;
        int w = Math.min(1000, g.getFontMetrics().stringWidth(txt) + 60);
        int gx = 136, gy = 990, gh = 70;
        ArteMj.globo(g, gx, gy, w, gh, -1, null);
        g.setFont(ArteMj.letra(20f));
        g.setColor(quien.color.darker());
        g.drawString(nombreVisible(quien), gx + 26, gy + 26);
        g.setFont(ArteMj.letra(26f));
        g.setColor(ArteMj.TEXTO);
        g.drawString(txt, gx + 26, gy + 56);
        ArteMj.cara(g, 76, 1018, 50, quien.name(), quien.color);
    }

    /** Uno de los otros tres, segun el consejo (asi cada consejo "lo dice" alguien distinto). */
    private Rol consejero(String consejo) {
        java.util.List<Rol> otros = new java.util.ArrayList<>();
        for (Rol r : Rol.values()) {
            if (r != juega) {
                otros.add(r);
            }
        }
        if (juega == null) {
            otros.remove(Rol.CIUDADANO);
        }
        return otros.get(Math.floorMod(consejo.hashCode(), otros.size()));
    }

    private String nombreVisible(Rol r) {
        String n = nombres == null ? null : nombres.get(r);
        return n == null || n.isEmpty() || n.equalsIgnoreCase(r.nombre) ? r.nombre : n + " · " + r.nombre;
    }

    /** Nivel segun cuantos aciertos (umbrales del guion). */
    protected static int nivel(int aciertos, int paraBien, int paraRegular) {
        return aciertos >= paraBien ? BIEN : aciertos >= paraRegular ? REGULAR : MAL;
    }

    // ===================== CICLO DE VIDA =====================

    /** Empieza el minijuego. fin recibe el resultado cuando el jugador cierra la tarjeta final. */
    public void iniciar(Consumer<Resultado> fin) {
        alTerminar = fin;
        resultado = null;
        fase = Fase.INTRO;
        tFase = 0;
        prepararPartida();
        repaint();
    }

    protected void terminarJuego(boolean exito, int puntos, Efecto efecto, String resumen) {
        if (fase == Fase.FIN) {
            return;
        }
        resultado = new Resultado(exito, puntos, efecto == null ? new Efecto() : efecto, resumen);
        fase = Fase.FIN;
        tFase = 0;
        Funciones.reproducirSonido(exito ? "/sonidos/noticia.wav" : "/sonidos/click.wav");
    }

    void procesarClic(Point p, int boton) {
        switch (fase) {
            case INTRO:
                if (tFase > 0.25) {
                    fase = Fase.JUGANDO;
                    tFase = 0;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                }
                break;
            case JUGANDO:
                clicJuego(p, boton);
                break;
            case FIN:
                if (tFase > 0.7 && alTerminar != null) {
                    Consumer<Resultado> f = alTerminar;
                    alTerminar = null;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    f.accept(resultado);
                }
                break;
            default:
                break;
        }
    }

    void procesarTecla(int k) {
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) {
            if (fase != Fase.JUGANDO) {
                procesarClic(new Point(W / 2, H / 2), MouseEvent.BUTTON1);
                return;
            }
        }
        if (fase == Fase.JUGANDO) {
            teclaJuego(k);
        }
    }

    @Override
    protected void actualizar(double dt) {
        tFase += dt;
        if (fase == Fase.JUGANDO) {
            actualizarJuego(dt);
        }
    }

    @Override
    protected void dibujar(Graphics2D g) {
        dibujarJuego(g);
        if (fase == Fase.INTRO) {
            tarjetaAviso(g);
        } else if (fase == Fase.FIN && resultado != null) {
            tarjetaResultado(g, resultado);
        }
    }

    // ===================== TARJETAS (kit: aviso "Cómo se juega" y banners) =====================

    private Graphics2D entrada(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);
        double t = Math.min(1, tFase / 0.35);
        double e = 1 + 2.70158 * Math.pow(t - 1, 3) + 1.70158 * Math.pow(t - 1, 2);
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, t * 2.5)));
        gg.translate(W / 2.0, H / 2.0);
        gg.scale(0.8 + 0.2 * e, 0.8 + 0.2 * e);
        gg.translate(-W / 2.0, -H / 2.0);
        return gg;
    }

    /** AVISO "Cómo se juega": panel crema tipo sticker, banner del minijuego, icono, texto y "Entendido". */
    private void tarjetaAviso(Graphics2D g) {
        Graphics2D gg = entrada(g);
        int x = 340, y = 200, w = 1240, h = 700;
        ArteMj.panel(gg, x, y, w, h, ArteMj.CREMA);
        ArteMj.ramito(gg, x + 34, y + h - 40, 74, true);
        ArteMj.ramito(gg, x + w - 40, y + 46, 66, false);
        ArteMj.mariposa(gg, x + w - 70, y + h - 70, 64, 12 + Math.sin(tiempo * 3) * 8);
        ArteMj.tituloCentrado(gg, carpetaArte(), nombre(), W / 2, y - 92, 190);

        gg.setFont(ArteMj.letra(30f));
        ArteMj.pildora(gg, W / 2 - 150, y + 106, 300, 54, ArteMj.AMARILLO_CLARO, 4f);
        ArteMj.centrado(gg, "Cómo se juega", W / 2.0, y + 143, ArteMj.TEXTO);

        iconoAviso(gg, x + 190, y + 370);
        gg.setFont(Estilo.fuente(Font.PLAIN, 34f));
        FontMetrics fm = gg.getFontMetrics();
        java.util.List<String> ls = envolver(instrucciones(), fm, w - 470);
        int ty = y + 360 - (ls.size() * (fm.getHeight() + 6)) / 2 + fm.getAscent();
        gg.setColor(ArteMj.TEXTO);
        for (String parrafo : ls) {
            gg.drawString(parrafo, x + 360, ty);
            ty += fm.getHeight() + 6;
        }
        double pulso = 0.5 + 0.5 * Math.sin(tiempo * 4);
        ArteMj.boton(gg, W / 2 - 180, y + h - 130, 360, 90, "Entendido", pulso > 0.5 ? 1 : 0);
        gg.dispose();
    }

    /** RESULTADO: el banner de su minijuego (¡Muy bien! / ¡Casi! / Inténtalo de nuevo), lo que dicen
     *  los personajes en burbujas con su cara, los efectos y el boton "Continuar". */
    private void tarjetaResultado(Graphics2D g, Resultado r) {
        Graphics2D gg = entrada(g);
        // ---- lo que dicen los personajes ----
        String dato = null;
        String resumen = r.resumen == null ? "" : r.resumen.trim();
        java.util.regex.Matcher md = java.util.regex.Pattern.compile("\\(([^()]*)\\)\\s*$").matcher(resumen);
        if (md.find() && md.start() > 0) {
            dato = md.group(1);
            resumen = resumen.substring(0, md.start()).trim();
        }
        String[] lineas = dialogoFinal != null ? dialogoFinal.apply(r.puntos) : partirEnLineas(resumen);
        String extra = lineaEfectos(r);
        if (dato != null) {
            extra = extra.isEmpty() ? dato : dato + "   ·   " + extra;
        }

        // ---- medir para que la tarjeta quede justa ----
        int x = 300, w = 1320;
        int contenido = 0;
        for (String linea : lineas) {
            contenido += altoLinea(gg, linea, w) + 22;
        }
        int h = Math.max(470, Math.min(820, 120 + contenido + (extra.isEmpty() ? 0 : 76) + 140));
        int y = Math.max(110, (H - h) / 2 + 30);

        ArteMj.panel(gg, x, y, w, h, ArteMj.CREMA);
        ArteMj.ramito(gg, x + 30, y + h - 36, 70, r.puntos != MAL);
        ArteMj.ramito(gg, x + w - 36, y + 40, 62, false);
        ArteMj.resultado(gg, carpetaArte(), r.puntos, W / 2, y - 6, 176);
        if (r.puntos == BIEN) {
            for (int i = 0; i < 3; i++) {
                double a = tiempo * 0.9 + i * 2.1;
                ArteMj.mariposa(gg, x + 160 + i * 500 + Math.sin(a) * 30, y + 100 - ((tFase * 40 + i * 30) % 50), 46, Math.sin(a) * 15);
            }
        }
        int by = y + 110;
        int lado = -1;
        for (String linea : lineas) {
            if (by > y + h - 230) {
                break;
            }
            int dos = linea.indexOf(": ");
            String quien = dos > 0 && dos < 34 ? linea.substring(0, dos).trim() : null;
            String texto = quien != null ? linea.substring(dos + 2).trim() : linea.trim();
            if (quien != null && quien.equalsIgnoreCase("Narrador")) {
                quien = null;
            }
            if (quien == null) {
                gg.setFont(Estilo.fuente(Font.ITALIC, 28f));
                FontMetrics fm = gg.getFontMetrics();
                java.util.List<String> ls = envolver(texto, fm, w - 260);
                int gh = ls.size() * fm.getHeight() + 34;
                ArteMj.globo(gg, x + 110, by, w - 220, gh, 0, ArteMj.AMARILLO_CLARO);
                int ty = by + 20 + fm.getAscent();
                gg.setColor(ArteMj.TEXTO);
                for (String l : ls) {
                    gg.drawString(l, x + 140, ty);
                    ty += fm.getHeight();
                }
                by += gh + 22;
            } else {
                Rol rol = rolDe(quien);
                String visible = rol != null ? nombreVisible(rol) : bonitoNombre(quien);
                by += ArteMj.burbujaConCara(gg, x + 70, by, w - 140, quien, visible, texto, lado,
                        rol != null ? rol.color : null) + 22;
                lado = -lado;
            }
        }
        if (!extra.isEmpty()) {
            gg.setFont(ArteMj.letra(26f));
            int ew = Math.min(w - 120, gg.getFontMetrics().stringWidth(extra) + 70);
            ArteMj.pildora(gg, W / 2 - ew / 2, y + h - 210, ew, 56, ArteMj.VERDE_CLARO, 4f);
            ArteMj.centrado(gg, extra, W / 2.0, y + h - 173, ArteMj.VERDE_OSCURO);
        }
        ArteMj.boton(gg, W / 2 - 180, y + h - 128, 360, 90, "Continuar", tFase > 0.7 ? 1 : 2);
        gg.dispose();
    }

    private int altoLinea(Graphics2D g, String linea, int w) {
        int dos = linea.indexOf(": ");
        boolean narr = dos <= 0 || dos >= 34 || linea.substring(0, dos).trim().equalsIgnoreCase("Narrador");
        String texto = dos > 0 && dos < 34 ? linea.substring(dos + 2) : linea;
        if (narr) {
            g.setFont(Estilo.fuente(Font.ITALIC, 28f));
            FontMetrics fm = g.getFontMetrics();
            return envolver(texto, fm, w - 260).size() * fm.getHeight() + 34;
        }
        return ArteMj.altoBurbuja(g, w - 140, texto);
    }

    /** "Rectora Visitación: Exacto. Periodista: ..." -> una linea por cada quien habla. */
    static String[] partirEnLineas(String resumen) {
        java.util.List<String> r = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:^|(?<=[.!?»)…]\\s))([A-ZÁÉÍÓÚÑ][\\p{L}]+(?: [\\p{L}]+){0,2}): ").matcher(resumen);
        int desde = 0;
        String quien = null;
        while (m.find()) {
            String antes = resumen.substring(desde, m.start()).trim();
            if (!antes.isEmpty()) {
                r.add(quien == null ? antes : quien + ": " + antes);
            }
            quien = m.group(1);
            desde = m.end();
        }
        String resto = resumen.substring(desde).trim();
        if (!resto.isEmpty()) {
            r.add(quien == null ? resto : quien + ": " + resto);
        }
        return r.toArray(new String[0]);
    }

    private static Rol rolDe(String quien) {
        String k = ArteMj.clave(quien);
        for (Rol rr : Rol.values()) {
            if (rr.name().equals(k)) {
                return rr;
            }
        }
        return null;
    }

    /** "RECTORA VISITACIÓN" -> "Rectora Visitación". */
    private static String bonitoNombre(String s) {
        StringBuilder b = new StringBuilder();
        for (String p : s.toLowerCase().split(" ")) {
            if (!p.isEmpty()) {
                b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return b.toString();
    }

    private static String lineaEfectos(Resultado r) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> c : r.efecto.getCambios().entrySet()) {
            if (c.getValue() == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("    ");
            }
            sb.append(bonito(c.getKey())).append(c.getValue() > 0 ? " +" : " ").append(c.getValue());
        }
        return sb.toString();
    }

    private static String bonito(String indicador) {
        return EstadoJuego.nombreBonito(indicador);
    }
}
