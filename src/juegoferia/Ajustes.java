package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * AJUSTES DEL JUEGO (sonido y texto) + el botoncito del parlante que esta en TODA la partida
 * (lugares, minijuegos y cinematicas), abajo a la izquierda.
 *
 *   Ajustes.musica   0..1  volumen de la musica (Musica y la musica de AudioCine)
 *   Ajustes.efectos  0..1  volumen de los efectos (Funciones.reproducirSonido y efectos de AudioCine)
 *   Ajustes.textoRapido    los dialogos se escriben el doble de rapido
 *
 * Al tocar el parlante se abre una tarjetita con dos barritas, "Silenciar" y "Texto rápido".
 * Los clics sobre el boton o la tarjeta NO le llegan al juego (los intercepta PanelDiseno).
 */
public final class Ajustes {

    private Ajustes() {
    }

    public static double musica = 0.8;
    public static double efectos = 0.9;
    public static boolean silencio = false;
    public static boolean textoRapido = false;

    /** Factor real de la musica (0 si esta en silencio). */
    public static double fMusica() {
        return silencio ? 0 : musica;
    }

    /** Factor real de los efectos (0 si esta en silencio). */
    public static double fEfectos() {
        return silencio ? 0 : efectos;
    }

    /** Multiplicador de la velocidad de los dialogos. */
    public static double velocidadTexto() {
        return textoRapido ? 2.2 : 1.0;
    }

    /** Pasa un factor 0..1 a decibeles (para MASTER_GAIN). */
    public static float db(double f) {
        return f <= 0.0001 ? -80f : (float) (20 * Math.log10(f));
    }

    /** Efecto de sonido que solo suena si el archivo existe (no ensucia la consola si falta). */
    public static void sfx(String ruta) {
        if (fEfectos() > 0 && Recursos.existe(ruta)) {
            Funciones.reproducirSonido(ruta);
        }
    }

    /** Prueba varias rutas y toca la primera que exista. */
    public static void sfx(String... rutas) {
        for (String r : rutas) {
            if (Recursos.existe(r)) {
                sfx(r);
                return;
            }
        }
    }

    private static void cambio() {
        Musica.reaplicarVolumen();
        AudioCine.reaplicarVolumen();
    }

    // ===================== EL BOTON Y LA TARJETA =====================

    private static final int BX = 22, BY = 1010, BR = 26;           // centro del boton = (BX+BR, BY+BR)
    private static final Rectangle BOTON = new Rectangle(BX, BY, BR * 2, BR * 2);
    private static final Rectangle TARJETA = new Rectangle(22, 760, 380, 236);
    private static final Rectangle BARRA_MUSICA = new Rectangle(60, 838, 300, 26);
    private static final Rectangle BARRA_EFECTOS = new Rectangle(60, 902, 300, 26);
    private static final Rectangle BOTON_SILENCIO = new Rectangle(44, 946, 160, 36);
    private static final Rectangle BOTON_TEXTO = new Rectangle(214, 946, 166, 36);

    private static boolean abierto = false;
    private static int arrastrando = 0;   // 1 musica, 2 efectos
    private static boolean encima = false;
    private static double tAbierto = 0;

    /** true = el evento fue del boton o de la tarjeta (el juego no lo recibe). */
    public static boolean raton(int id, Point p) {
        boolean sobreBoton = BOTON.contains(p);
        boolean sobreTarjeta = abierto && TARJETA.contains(p);
        if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
            encima = sobreBoton;
            if (arrastrando != 0) {
                mover(p);
                return true;
            }
            return sobreBoton || sobreTarjeta;
        }
        if (id == MouseEvent.MOUSE_PRESSED) {
            if (sobreBoton) {
                abierto = !abierto;
                tAbierto = 0;
                sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
                return true;
            }
            if (sobreTarjeta) {
                if (agrandar(BARRA_MUSICA).contains(p)) {
                    arrastrando = 1;
                    mover(p);
                } else if (agrandar(BARRA_EFECTOS).contains(p)) {
                    arrastrando = 2;
                    mover(p);
                } else if (BOTON_SILENCIO.contains(p)) {
                    silencio = !silencio;
                    cambio();
                } else if (BOTON_TEXTO.contains(p)) {
                    textoRapido = !textoRapido;
                }
                return true;
            }
            if (abierto) {           // clic afuera: se cierra y ese clic no cuenta para el juego
                abierto = false;
                return true;
            }
            return false;
        }
        if (id == MouseEvent.MOUSE_RELEASED) {
            boolean era = arrastrando != 0;
            if (era && arrastrando == 2) {
                sfx("/sonidos/hover.wav", "/sonidos/tecla.wav");   // para oir como quedo
            }
            arrastrando = 0;
            return era || sobreBoton || sobreTarjeta;
        }
        return sobreBoton || sobreTarjeta;
    }

    private static Rectangle agrandar(Rectangle r) {
        return new Rectangle(r.x - 10, r.y - 12, r.width + 20, r.height + 24);
    }

    private static void mover(Point p) {
        Rectangle b = arrastrando == 1 ? BARRA_MUSICA : BARRA_EFECTOS;
        double v = Math.max(0, Math.min(1, (p.x - b.x) / (double) b.width));
        if (arrastrando == 1) {
            musica = v;
        } else {
            efectos = v;
        }
        if (v > 0) {
            silencio = false;
        }
        cambio();
    }

    private static final Color CREMA = new Color(255, 250, 240, 236);
    private static final Color BORDE = new Color(0xEAD7AE);
    private static final Color TEXTO = new Color(0x5A3B1F);
    private static final Color SUAVE = new Color(0xA88B6A);

    /** Lo dibuja PanelDiseno encima de todo (solo en las pantallas de la partida). */
    public static void dibujar(Graphics2D g0, double dt) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tAbierto += dt;
        int cx = BX + BR, cy = BY + BR;
        double s = encima || abierto ? 1.08 : 1.0;
        int r = (int) (BR * s);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, encima || abierto ? 1f : 0.82f));
        g.setColor(new Color(60, 35, 15, 40));
        g.fillOval(cx - r + 1, cy - r + 3, r * 2, r * 2);
        g.setColor(CREMA);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(abierto ? Estilo.DORADO : BORDE);
        g.setStroke(new BasicStroke(abierto ? 2.4f : 1.6f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        parlante(g, cx - 3, cy, silencio || (musica <= 0 && efectos <= 0) ? 0 : Math.max(musica, efectos));
        g.setComposite(AlphaComposite.SrcOver);

        if (abierto) {
            float a = (float) Math.min(1, tAbierto / 0.15);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            int ty = TARJETA.y + (int) ((1 - a) * 14);
            Rectangle t = TARJETA;
            g.setColor(new Color(60, 35, 15, 45));
            g.fill(new RoundRectangle2D.Double(t.x + 1, ty + 5, t.width, t.height, 28, 28));
            g.setColor(CREMA);
            g.fill(new RoundRectangle2D.Double(t.x, ty, t.width, t.height, 28, 28));
            g.setColor(BORDE);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(t.x, ty, t.width, t.height, 28, 28));
            int dy = ty - TARJETA.y;
            g.setFont(Estilo.fuente(Font.BOLD, 18f));
            g.setColor(TEXTO);
            g.drawString("Sonido y texto", t.x + 24, ty + 36);
            barra(g, "Música", musica, BARRA_MUSICA, dy, new Color(0xF0A85C));
            barra(g, "Efectos", efectos, BARRA_EFECTOS, dy, new Color(0x7FB2E0));
            boton(g, BOTON_SILENCIO, dy, silencio ? "Sin sonido" : "Silenciar", silencio);
            boton(g, BOTON_TEXTO, dy, textoRapido ? "Texto rápido ✓" : "Texto rápido", textoRapido);
        }
        g.dispose();
    }

    private static void barra(Graphics2D g, String nombre, double v, Rectangle b, int dy, Color col) {
        int y = b.y + dy;
        g.setFont(Estilo.fuente(Font.BOLD, 14f));
        g.setColor(SUAVE);
        g.drawString(nombre.toUpperCase(), b.x, y - 8);
        String pct = Math.round(v * 100) + "%";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(pct, b.x + b.width - fm.stringWidth(pct), y - 8);
        int my = y + b.height / 2;
        g.setColor(new Color(0xF1E6D0));
        g.fill(new RoundRectangle2D.Double(b.x, my - 5, b.width, 10, 10, 10));
        g.setColor(silencio ? new Color(0xD8CDBA) : col);
        g.fill(new RoundRectangle2D.Double(b.x, my - 5, b.width * v, 10, 10, 10));
        double hx = b.x + b.width * v;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(hx - 11, my - 11, 22, 22));
        g.setColor(silencio ? new Color(0xD8CDBA) : col);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(hx - 11, my - 11, 22, 22));
    }

    private static void boton(Graphics2D g, Rectangle b, int dy, String t, boolean on) {
        int y = b.y + dy;
        if (on) {
            g.setPaint(new GradientPaint(b.x, y, Estilo.DORADO_CLARO, b.x, y + b.height, Estilo.DORADO));
        } else {
            g.setColor(new Color(0xFFFDF7));
        }
        g.fill(new RoundRectangle2D.Double(b.x, y, b.width, b.height, b.height, b.height));
        g.setColor(on ? new Color(0xC98A12) : BORDE);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(b.x, y, b.width, b.height, b.height, b.height));
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        g.setColor(TEXTO);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, b.x + (b.width - fm.stringWidth(t)) / 2, y + 24);
    }

    /** Parlante con 0-3 ondas segun el volumen (con una X si esta en silencio). */
    private static void parlante(Graphics2D g, int cx, int cy, double v) {
        g.setColor(TEXTO);
        Path2D p = new Path2D.Double();
        p.moveTo(cx - 12, cy - 5);
        p.lineTo(cx - 5, cy - 5);
        p.lineTo(cx + 3, cy - 12);
        p.lineTo(cx + 3, cy + 12);
        p.lineTo(cx - 5, cy + 5);
        p.lineTo(cx - 12, cy + 5);
        p.closePath();
        g.fill(p);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (v <= 0) {
            g.setColor(new Color(0xE0735E));
            g.drawLine(cx + 8, cy - 6, cx + 18, cy + 6);
            g.drawLine(cx + 18, cy - 6, cx + 8, cy + 6);
            return;
        }
        int ondas = v > 0.66 ? 3 : v > 0.33 ? 2 : 1;
        for (int i = 0; i < ondas; i++) {
            int rr = 7 + i * 5;
            g.drawArc(cx + 3 - rr, cy - rr, rr * 2, rr * 2, -45, 90);
        }
    }
}
