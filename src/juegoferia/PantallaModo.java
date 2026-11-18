package juegoferia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Despues de JUGAR: como quieren jugar.
 *   EN RED        cuatro computadores (cada quien con su personaje y su teclado)
 *   EN UN SOLO PC los cuatro en el mismo computador (1-4 cambian quien camina)
 * Los botones se dibujan con codigo y se hunden al presionar.
 */
public class PantallaModo extends PanelDiseno {

    private final Runnable enRed, unSoloPc, volver;
    private final Rectangle[] r = {
        new Rectangle(460, 300, 1000, 170),
        new Rectangle(460, 500, 1000, 170),
        new Rectangle(710, 870, 500, 100),
        new Rectangle(510, 715, 900, 110)
    };

    /** Que partida se juega: true = MODO FERIA (corta, ~15 min); false = la completa (2 dias). */
    public static boolean feria = true;
    private final String[] titulo = {"EN RED", "EN UN SOLO PC", "VOLVER"};
    private final String[] detalle = {
        "4 jugadores, cada uno en su computador y con su personaje",
        "Los cuatro en este computador · 1, 2, 3 y 4 cambian quién camina",
        null
    };
    private int encima = -1, pulsado = -1;

    public PantallaModo(Runnable enRed, Runnable unSoloPc, Runnable volver) {
        this.enRed = enRed;
        this.unSoloPc = unSoloPc;
        this.volver = volver;
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                encima = boton(aDiseno(e.getPoint()));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pulsado = boton(aDiseno(e.getPoint()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int b = boton(aDiseno(e.getPoint()));
                int p = pulsado;
                pulsado = -1;
                if (b == 3 && p == 3) {
                    feria = !feria;
                    Ajustes.sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
                    return;
                }
                if (b >= 0 && b == p) {
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    (b == 0 ? PantallaModo.this.enRed : b == 1 ? PantallaModo.this.unSoloPc : PantallaModo.this.volver).run();
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    private int boton(Point p) {
        for (int i = 0; i < r.length; i++) {
            if (r[i].contains(p)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void actualizar(double dt) {
    }

    @Override
    protected void dibujar(Graphics2D g) {
        if (Recursos.existe(Rutas.FONDO_INICIO)) {
            g.drawImage(Recursos.fondo(Rutas.FONDO_INICIO), 0, 0, W, H, null);
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(0, 0, W, H);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(0xF6E3C0), 0, H, new Color(0xD9A05B)));
            g.fillRect(0, 0, W, H);
        }
        g.setFont(Estilo.fuente(Font.BOLD, 100f));
        Estilo.textoContorno(g, "¿CÓMO QUIEREN JUGAR?",
                W / 2 - g.getFontMetrics().stringWidth("¿CÓMO QUIEREN JUGAR?") / 2, 230,
                Estilo.DORADO_CLARO, Estilo.MADERA_OSCURA);

        for (int i = 0; i < r.length; i++) {
            if (i == 3) {
                dibujarTipoPartida(g);
                continue;
            }
            boolean hov = encima == i, pres = pulsado == i && hov;
            int off = pres ? 8 : (hov ? -4 : 0);
            Rectangle b = r[i];
            Estilo.madera(g, b.x, b.y + off, b.width, b.height, 50);
            if (!pres) {
                g.setColor(new Color(255, 255, 255, hov ? 50 : 0));
                g.fillRoundRect(b.x + 6, b.y + off + 6, b.width - 12, b.height / 2 - 6, 40, 40);
            }
            g.setFont(Estilo.fuente(Font.BOLD, i == 2 ? 52f : 72f));
            g.setColor(new Color(0xFFF3D6));
            centrado(g, titulo[i], b.x + b.width / 2, b.y + off + (detalle[i] != null ? 90 : 72));
            if (detalle[i] != null) {
                g.setFont(Estilo.fuente(Font.PLAIN, 34f));
                g.setColor(new Color(0xFFE8BD));
                centrado(g, detalle[i], b.x + b.width / 2, b.y + off + 140);
            }
        }
    }

    /** El selector de partida: CORTA (modo feria) o COMPLETA. Se cambia con un clic. */
    private void dibujarTipoPartida(Graphics2D g) {
        Rectangle b = r[3];
        boolean hov = encima == 3;
        int off = hov ? -3 : 0;
        Color fondo = feria ? new Color(0xFFE3A3) : new Color(0xF4E6CF);
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(b.x + 4, b.y + 10 + off, b.width, b.height, b.height, b.height);
        g.setColor(fondo);
        g.fillRoundRect(b.x, b.y + off, b.width, b.height, b.height, b.height);
        g.setColor(Estilo.MADERA_OSCURA);
        g.setStroke(new java.awt.BasicStroke(5f));
        g.drawRoundRect(b.x, b.y + off, b.width, b.height, b.height, b.height);
        // flechitas
        int cy = b.y + off + b.height / 2;
        g.fillPolygon(new int[]{b.x + 40, b.x + 62, b.x + 62}, new int[]{cy, cy - 16, cy + 16}, 3);
        g.fillPolygon(new int[]{b.x + b.width - 40, b.x + b.width - 62, b.x + b.width - 62}, new int[]{cy, cy - 16, cy + 16}, 3);
        g.setFont(Estilo.fuente(Font.BOLD, 40f));
        g.setColor(Estilo.MADERA_OSCURA);
        centrado(g, feria ? "PARTIDA CORTA · MODO FERIA" : "PARTIDA COMPLETA", b.x + b.width / 2, b.y + off + 52);
        g.setFont(Estilo.fuente(Font.PLAIN, 26f));
        g.setColor(new Color(0x7A4A24));
        centrado(g, feria ? "unos 15 minutos · toca para cambiar" : "dos días completos (más de una hora) · toca para cambiar",
                b.x + b.width / 2, b.y + off + 88);
    }
}
