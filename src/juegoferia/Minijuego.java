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

    // ===================== AYUDAS PARA DIBUJAR =====================

    /** Fondo del minijuego: imagen de Canva si existe; si no, un degradado suave. */
    protected void fondo(Graphics2D g, String ruta) {
        if (ruta != null && Recursos.existe(ruta)) {
            g.drawImage(Recursos.fondo(ruta), 0, 0, W, H, null);
        } else {
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0xF6E3C0), 0, H, new Color(0xD9A05B)));
            g.fillRect(0, 0, W, H);
        }
    }

    /** Boton de madera con texto. */
    protected static void boton(Graphics2D g, java.awt.Rectangle r, String texto, boolean activo) {
        Estilo.madera(g, r.x, r.y, r.width, r.height, 30);
        g.setFont(Estilo.fuente(Font.BOLD, 30f));
        g.setColor(activo ? new Color(0xFFF3D6) : new Color(0xC9B79A));
        centrado(g, texto, r.x + r.width / 2, r.y + r.height / 2 + 11);
    }

    /** Titulo y consejo de los otros jugadores ("Los otros tres miran y aconsejan"). */
    protected void cabecera(Graphics2D g, String titulo, String consejo) {
        g.setFont(Estilo.fuente(Font.BOLD, 52f));
        Estilo.textoContorno(g, titulo, 60, 90, Estilo.DORADO_CLARO, Estilo.MADERA_OSCURA);
        if (consejo != null) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(60, 1000, 1200, 56, 28, 28);
            g.setFont(Estilo.fuente(Font.BOLD, 26f));
            g.setColor(Color.WHITE);
            g.drawString("Los demás aconsejan: " + consejo, 84, 1037);
        }
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
            tarjeta(g, nombre(), instrucciones(), "Haz clic para empezar", null);
        } else if (fase == Fase.FIN && resultado != null) {
            String titulo = resultado.puntos == BIEN ? "¡Lo lograste!"
                    : resultado.puntos == REGULAR ? "Casi..." : "Esta vez no";
            tarjeta(g, titulo, resultado.resumen, "Haz clic para continuar", lineaEfectos(resultado));
        }
    }

    // ===================== TARJETAS =====================

    private void tarjeta(Graphics2D g, String titulo, String cuerpo, String pie, String extra) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);

        double t = Math.min(1, tFase / 0.35);
        double e = 1 + 2.70158 * Math.pow(t - 1, 3) + 1.70158 * Math.pow(t - 1, 2);
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, t * 2.5)));
        gg.translate(W / 2.0, H / 2.0);
        gg.scale(0.8 + 0.2 * e, 0.8 + 0.2 * e);
        gg.translate(-W / 2.0, -H / 2.0);

        int x = 360, y = 220, w = 1200, h = 640;
        Estilo.madera(gg, x - 18, y - 18, w + 36, h + 36, 60);
        Estilo.pergamino(gg, x, y, w, h, 46);
        Estilo.hojas(gg, x + 6, y + 6, 1, 1, 70);
        Estilo.hojas(gg, x + w - 6, y + h - 6, -1, -1, 70);

        gg.setFont(Estilo.fuente(Font.BOLD, 92f));
        Estilo.textoContorno(gg, titulo, W / 2 - gg.getFontMetrics().stringWidth(titulo) / 2, y + 125,
                Estilo.DORADO_CLARO, Estilo.MADERA_OSCURA);

        gg.setFont(Estilo.fuente(Font.PLAIN, 42f));
        FontMetrics fm = gg.getFontMetrics();
        gg.setColor(Estilo.TEXTO);
        int ty = y + 220;
        for (String parrafo : envolver(cuerpo, fm, w - 160)) {
            centrado(gg, parrafo, W / 2, ty);
            ty += fm.getHeight() + 8;
        }
        if (extra != null) {
            gg.setFont(Estilo.fuente(Font.BOLD, 38f));
            gg.setColor(Estilo.HOJA_OSCURA);
            centrado(gg, extra, W / 2, y + h - 150);
        }

        double pulso = 0.5 + 0.5 * Math.sin(tiempo * 4);
        gg.setFont(Estilo.fuente(Font.BOLD, 40f));
        gg.setColor(new Color(0xC9553A));
        gg.setColor(new Color(Estilo.ROJO.getRed(), Estilo.ROJO.getGreen(), Estilo.ROJO.getBlue(),
                (int) (150 + 105 * pulso)));
        centrado(gg, pie, W / 2, y + h - 60);
        gg.setStroke(new BasicStroke(1f));
        gg.dispose();
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
