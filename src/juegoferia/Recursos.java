package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Carga imagenes de los recursos del proyecto y las guarda en cache.
 * Si una imagen falta, devuelve un cuadro provisional (con la ruta escrita)
 * para poder seguir probando el juego sin tener todas las imagenes.
 */
public class Recursos {

    /** Tamanio "de diseno": todas las coordenadas del juego se escriben en esta resolucion. */
    public static final int BASE_W = 1591;
    public static final int BASE_H = 989;

    private static final Map<String, BufferedImage> cache = new HashMap<>();

    public static BufferedImage fondo(String ruta) {
        return imagen(ruta, BASE_W, BASE_H);
    }

    /**
     * Burbuja de dialogo ya recortada: se quitan los margenes transparentes, asi da igual
     * si el PNG viene en un lienzo de 1591x989 o recortado. PantallaJuego la coloca siempre
     * en la misma zona fija (ZONA_DIALOGO).
     */
    public static synchronized BufferedImage dialogo(String ruta) {
        String clave = "recorte:" + ruta;
        BufferedImage hecho = cache.get(clave);
        if (hecho != null) {
            return hecho;
        }
        hecho = recortarTransparencia(imagen(ruta, 900, 220));
        cache.put(clave, hecho);
        return hecho;
    }

    /** true si el archivo existe en los recursos (sirve para usar texto mientras falta la imagen). */
    public static boolean existe(String ruta) {
        return ruta != null && Recursos.class.getResource(ruta) != null;
    }

    private static BufferedImage recortarTransparencia(BufferedImage src) {
        if (!src.getColorModel().hasAlpha()) {
            return src;
        }
        int minX = src.getWidth(), minY = src.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if (((src.getRGB(x, y) >> 24) & 0xFF) > 10) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return src;
        }
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public static BufferedImage sprite(String ruta) {
        return imagen(ruta, 140, 170);
    }

    public static synchronized BufferedImage imagen(String ruta, int phW, int phH) {
        BufferedImage img = cache.get(ruta);
        if (img != null) {
            return img;
        }
        try {
            URL url = Recursos.class.getResource(ruta);
            if (url != null) {
                img = ImageIO.read(url);
            }
        } catch (IOException e) {
            System.err.println("[Recursos] No se pudo leer " + ruta + ": " + e.getMessage());
        }
        if (img == null) {
            System.err.println("[Recursos] FALTA la imagen: " + ruta + " (se usa un cuadro provisional)");
            img = provisional(ruta, phW, phH);
        }
        cache.put(ruta, img);
        return img;
    }

    private static BufferedImage provisional(String ruta, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(120, 90, 140), w, h, new Color(60, 90, 140)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(Math.max(2, w / 200f)));
        g.drawRect(4, 4, w - 8, h - 8);
        g.drawLine(4, 4, w - 4, h - 4);
        g.drawLine(w - 4, 4, 4, h - 4);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, Math.min(w, h) / 14)));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(ruta, Math.max(10, (w - fm.stringWidth(ruta)) / 2), h / 2);
        g.dispose();
        return img;
    }
}
