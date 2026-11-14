package juegoferia;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * NORMAL y MANO son cursores INVISIBLES: sirven solo como "etiqueta" de cual esta activo.
 * El dibujo real lo hace CursorJuego.
 */
public final class Cursores {

    public static final Cursor NORMAL = invisible("normal");
    public static final Cursor MANO = invisible("mano");

    private Cursores() {
    }

    private static Cursor invisible(String nombre) {
        try {
            BufferedImage vacio = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            return Toolkit.getDefaultToolkit().createCustomCursor(vacio, new Point(0, 0), nombre);
        } catch (Exception e) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }

    /** Carga un PNG, recorta el borde transparente y lo escala al alto pedido. */
    static BufferedImage cargar(String ruta, int alto) {
        try {
            URL url = Cursores.class.getResource(ruta);
            if (url == null) {
                System.out.println("Cursor no encontrado: " + ruta);
                return null;
            }
            BufferedImage origen = recortarTransparente(ImageIO.read(url));
            int w = Math.max(1, (int) Math.round(origen.getWidth() * (alto / (double) origen.getHeight())));
            return escalarSuave(origen, w, alto);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage recortarTransparente(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 10) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return img;
        }
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage escalarSuave(BufferedImage src, int w, int h) {
        BufferedImage actual = src;
        int cw = src.getWidth();
        int ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            actual = pasar(actual, cw, ch);
        }
        return pasar(actual, w, h);
    }

    private static BufferedImage pasar(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}