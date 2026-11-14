package juegoferia;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Imagen quieta + cuadros de caminata. Escala cada uno una sola vez por tamano y lo guarda. */
public final class AnimacionPersonaje {

    private static final String QUIETO = "/imagenes/personajecivil.png";
    private static final String PATRON = "/imagenes/personaje/caminar_%02d.png";
    private static final int MS_POR_CUADRO = 90;   // mas bajo = camina mas rapido

    private final BufferedImage quieto;
    private final List<BufferedImage> pasos = new ArrayList<>();
    private final Map<String, ImageIcon[]> cache = new HashMap<>();
    private boolean caminabaAntes = false;
    private long t0 = System.nanoTime();

    public AnimacionPersonaje() {
        quieto = leer(QUIETO);
        for (int i = 1; i < 100; i++) {
            BufferedImage f = leer(String.format(PATRON, i));
            if (f == null) {
                break;
            }
            pasos.add(f);
        }
        System.out.println("Animacion del personaje: " + pasos.size() + " cuadros");
    }

    /** Devuelve el icono que toca ahora: la imagen quieta o el cuadro actual de la caminata. */
    public ImageIcon icono(boolean caminando, int w, int h, boolean espejo) {
        boolean anima = caminando && !pasos.isEmpty();
        if (anima && !caminabaAntes) {
            t0 = System.nanoTime();   // cada caminata empieza en el cuadro 1
        }
        caminabaAntes = anima;

        String clave = w + "x" + h + (espejo ? "e" : "n");
        ImageIcon[] set = cache.computeIfAbsent(clave, k -> construir(w, h, espejo));
        if (!anima) {
            return set[0];
        }
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        return set[1 + (int) ((ms / MS_POR_CUADRO) % pasos.size())];
    }

    private ImageIcon[] construir(int w, int h, boolean espejo) {
        ImageIcon[] r = new ImageIcon[1 + pasos.size()];
        r[0] = escalar(quieto, w, h, espejo);
        for (int i = 0; i < pasos.size(); i++) {
            r[1 + i] = escalar(pasos.get(i), w, h, espejo);
        }
        return r;
    }

    private static BufferedImage leer(String ruta) {
        try {
            URL u = AnimacionPersonaje.class.getResource(ruta);
            return (u == null) ? null : ImageIO.read(u);
        } catch (Exception e) {
            return null;
        }
    }

    /** Reduce a la mitad varias veces y luego ajusta: queda mucho mas nitido. */
    private static ImageIcon escalar(BufferedImage src, int w, int h, boolean espejo) {
        if (src == null) {
            return new ImageIcon(new BufferedImage(Math.max(1, w), Math.max(1, h),
                    BufferedImage.TYPE_INT_ARGB));
        }
        BufferedImage a = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            a = pasar(a, cw, ch, false);
        }
        return new ImageIcon(pasar(a, w, h, espejo));
    }

    private static BufferedImage pasar(BufferedImage s, int w, int h, boolean espejo) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (espejo) {
            g.drawImage(s, w, 0, 0, h, 0, 0, s.getWidth(), s.getHeight(), null);
        } else {
            g.drawImage(s, 0, 0, w, h, null);
        }
        g.dispose();
        return o;
    }
}