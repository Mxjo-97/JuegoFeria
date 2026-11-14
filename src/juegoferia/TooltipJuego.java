package juegoferia;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Mini-cartel de texto que aparece junto al cursor. */
public final class TooltipJuego {

    private static final Color BORDE = new Color(0x8A4F25);
    private static final Color TITULO = new Color(0x5A2D1A);
    private static final Color CUERPO = new Color(0x6B4A2E);

    private TooltipJuego() {
    }

    /**
     * @param mx, my  posición del cursor (en píxeles del panel)
     * @param pw, ph  tamaño del panel (para que el cartel no se salga)
     * @param alfa    0..1 (se usa el "hover" del botón para que aparezca con fundido)
     */
    public static void dibujar(Graphics2D g, String titulo, String texto,
            int mx, int my, int pw, int ph, float alfa) {

        if (texto == null || alfa <= 0.02f) {
            return;
        }
        alfa = Math.min(1f, alfa);

        Font fTitulo = new Font("Trebuchet MS", Font.BOLD, 20);
        Font fCuerpo = new Font("Trebuchet MS", Font.PLAIN, 16);
        FontMetrics mt = g.getFontMetrics(fTitulo);
        FontMetrics mc = g.getFontMetrics(fCuerpo);

        int pad = 14;
        List<String> lineas = envolver(texto, mc, 270);

        int ancho = mt.stringWidth(titulo);
        for (String l : lineas) {
            ancho = Math.max(ancho, mc.stringWidth(l));
        }
        ancho += pad * 2;
        int alto = pad + mt.getHeight() + 4 + lineas.size() * mc.getHeight() + pad;

        int x = mx + 20;
        int y = my + 28 + (int) ((1 - alfa) * 8);   // sube un poquito al aparecer
        if (x + ancho > pw - 8) {
            x = mx - ancho - 16;                    // si no cabe a la derecha, va a la izquierda
        }
        if (y + alto > ph - 8) {
            y = my - alto - 16;                     // si no cabe abajo, va arriba
        }
        x = Math.max(8, x);
        y = Math.max(8, y);

        java.awt.Composite viejo = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alfa));

        RoundRectangle2D caja = new RoundRectangle2D.Double(x, y, ancho, alto, 22, 22);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 6, ancho, alto, 22, 22));
        g.setPaint(new GradientPaint(0, y, new Color(0xFFF4DC), 0, y + alto, new Color(0xF3D9A8)));
        g.fill(caja);
        g.setColor(BORDE);
        g.setStroke(new BasicStroke(3f));
        g.draw(caja);

        int ty = y + pad + mt.getAscent();
        g.setFont(fTitulo);
        g.setColor(TITULO);
        g.drawString(titulo, x + pad, ty);

        g.setFont(fCuerpo);
        g.setColor(CUERPO);
        ty += 6 + mc.getAscent();
        for (String l : lineas) {
            g.drawString(l, x + pad, ty);
            ty += mc.getHeight();
        }

        g.setComposite(viejo);
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
}