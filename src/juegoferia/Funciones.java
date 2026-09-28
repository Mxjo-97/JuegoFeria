package juegoferia;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.swing.ImageIcon;

public class Funciones {

    /**
     * Reproduce un sonido .wav ubicado en los recursos del proyecto.
     *
     * @param ruta ruta del sonido (ej: "/sonidos/click.wav")
     */
    public static void reproducirSonido(String ruta) {
        try {
            URL sonidoURL = Funciones.class.getResource(ruta);
            if (sonidoURL == null) {
                System.err.println("No se encontró el archivo de sonido: " + ruta);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(sonidoURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);

            // Algunos equipos no soportan este control y por eso se verifica antes de usarlo
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volumen = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                volumen.setValue(volumen.getMaximum());
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });

            clip.start();

        } catch (Exception e) {
            System.err.println("Error al reproducir el sonido: " + ruta);
            e.printStackTrace();
        }
    }

    /**
     * Crea un cursor personalizado a partir de una imagen y la redimensiona.
     *
     * @param path   ruta de la imagen en recursos (ej: "/images/cursor.png")
     * @param width  ancho deseado del cursor
     * @param height alto deseado del cursor
     * @return Cursor listo para usar (o el cursor por defecto si falla)
     */
    public static Cursor crearCursor(String path, int width, int height) {
        try {
            URL url = Funciones.class.getResource(path);
            if (url == null) {
                System.err.println("No se encontró la imagen del cursor: " + path);
                return Cursor.getDefaultCursor();
            }
            ImageIcon icon = new ImageIcon(url);
            Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return Toolkit.getDefaultToolkit().createCustomCursor(
                    image,
                    new Point(0, 0),
                    "CursorPersonalizado"
            );
        } catch (Exception e) {
            System.err.println("No se pudo cargar el cursor: " + e.getMessage());
            return Cursor.getDefaultCursor();
        }
    }
}