package juegoferia;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.swing.Timer;

/**
 * Audio controlable para las cinematicas (no toca Musica ni Funciones).
 *
 * - UNA musica de fondo a la vez, en bucle, con fundido de entrada y de salida.
 * - Efectos de sonido que puedes cortar (por duracion maxima o con detenerEfectos()).
 * - Volumenes de 0.0 (mudo) a 1.0 (maximo). Formato: .wav
 *
 * Si un archivo no existe o falla, solo escribe un aviso en consola y el juego sigue.
 */
public final class AudioCine {

    private AudioCine() {
    }

    private static Clip musica;
    private static String rutaMusica;
    private static final List<Clip> efectos = new ArrayList<>();

    // ===================== MUSICA =====================

    /**
     * Pone musica en bucle. Si ya suena esa misma musica, no la reinicia.
     * Si suena otra, la anterior se desvanece mientras entra la nueva.
     */
    public static synchronized void musica(String ruta, double volumen, double fadeEntradaSeg) {
        if (ruta == null) return;
        if (ruta.equals(rutaMusica) && musica != null && musica.isOpen()) {
            fade(musica, volumen, 0.5, null);
            return;
        }
        detenerMusica(1.0);
        Clip c = abrir(ruta);
        if (c == null) return;
        musica = c;
        rutaMusica = ruta;
        ponerVolumen(c, fadeEntradaSeg > 0 ? 0 : volumen);
        c.loop(Clip.LOOP_CONTINUOUSLY);
        if (fadeEntradaSeg > 0) fade(c, volumen, fadeEntradaSeg, null);
    }

    /** Apaga la musica con un fundido (0 = corte inmediato). */
    public static synchronized void detenerMusica(double fadeSalidaSeg) {
        Clip c = musica;
        musica = null;
        rutaMusica = null;
        if (c == null) return;
        if (fadeSalidaSeg <= 0) {
            cerrar(c);
        } else {
            fade(c, 0, fadeSalidaSeg, () -> cerrar(c));
        }
    }

    // ===================== EFECTOS =====================

    /**
     * Reproduce un efecto una vez.
     *
     * @param maxSeg si es mayor que 0, el efecto se corta (con un fundido corto) a esos segundos
     *               aunque el archivo dure mas. Con 0 suena hasta que termine solo.
     */
    public static synchronized void efecto(String ruta, double volumen, double maxSeg) {
        Clip c = abrir(ruta);
        if (c == null) return;
        ponerVolumen(c, volumen);
        efectos.add(c);
        c.addLineListener(ev -> {
            if (ev.getType() == LineEvent.Type.STOP) {
                synchronized (AudioCine.class) {
                    efectos.remove(c);
                }
                c.close();
            }
        });
        c.start();
        if (maxSeg > 0) {
            Timer t = new Timer((int) (maxSeg * 1000), e -> {
                if (c.isOpen()) fade(c, 0, 0.25, () -> cerrar(c));
            });
            t.setRepeats(false);
            t.start();
        }
    }

    /** Corta ya todos los efectos que esten sonando. */
    public static synchronized void detenerEfectos() {
        for (Clip c : new ArrayList<>(efectos)) cerrar(c);
        efectos.clear();
    }

    /** Corta efectos y apaga la musica con fundido. */
    public static void detenerTodo(double fadeMusicaSeg) {
        detenerEfectos();
        detenerMusica(fadeMusicaSeg);
    }

    // ===================== INTERNO =====================

    private static Clip abrir(String ruta) {
        try {
            URL url = AudioCine.class.getResource(ruta);
            if (url == null) {
                System.err.println("[AudioCine] No se encontro el sonido: " + ruta);
                return null;
            }
            AudioInputStream original = AudioSystem.getAudioInputStream(url);
            AudioFormat base = original.getFormat();
            AudioInputStream ais = original;
            if (base.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || base.getSampleSizeInBits() != 16) {
                AudioFormat destino = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), 16,
                        base.getChannels(), base.getChannels() * 2, base.getSampleRate(), false);
                ais = AudioSystem.getAudioInputStream(destino, original);
            }
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return c;
        } catch (Exception e) {
            System.err.println("[AudioCine] Error con " + ruta + ": " + e);
            return null;
        }
    }

    private static void ponerVolumen(Clip c, double v) {
        try {
            if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl ctl = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            double lineal = Math.max(0, Math.min(1, v));
            float db = lineal <= 0.0001 ? ctl.getMinimum() : (float) (20 * Math.log10(lineal));
            ctl.setValue(Math.max(ctl.getMinimum(), Math.min(ctl.getMaximum(), db)));
        } catch (Exception ignorar) {
            // algunos equipos no dejan cambiar el volumen
        }
    }

    private static double volumenActual(Clip c) {
        try {
            FloatControl ctl = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            return Math.pow(10, ctl.getValue() / 20.0);
        } catch (Exception e) {
            return 1;
        }
    }

    private static void cerrar(Clip c) {
        try {
            c.stop();
            c.close();
        } catch (Exception ignorar) {
        }
    }

    /** Lleva el volumen de c hasta 'destino' en 'seg' segundos; al terminar ejecuta 'fin'. */
    private static void fade(Clip c, double destino, double seg, Runnable fin) {
        final double desde = volumenActual(c);
        final int pasos = Math.max(1, (int) (seg * 1000 / 30));
        final int[] i = {0};
        Timer t = new Timer(30, null);
        t.addActionListener(e -> {
            i[0]++;
            if (!c.isOpen()) {
                t.stop();
                return;
            }
            double k = Math.min(1, i[0] / (double) pasos);
            ponerVolumen(c, desde + (destino - desde) * k);
            if (k >= 1) {
                t.stop();
                if (fin != null) fin.run();
            }
        });
        t.start();
    }
}
