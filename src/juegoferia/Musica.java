package juegoferia;

import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Musica de fondo en bucle (una sola a la vez). Solo .wav (Java no lee mp3/ogg sin librerias).
 *
 *   Musica.tocar("/musica/elegir_rol.wav");   // si ya suena esa, no la reinicia
 *   Musica.parar();
 *
 * Si el archivo no existe no pasa nada (el juego sigue en silencio).
 */
public final class Musica {

    private static Clip actual;
    /** Volumen "de diseno" de la musica que suena (sin contar el boton del parlante). */
    private static float dbBase = -8f;
    private static String rutaActual;
    /** Volumen de la musica en decibeles (0 = original, -10 = mas bajito). */
    public static float volumenDb = -8f;

    private Musica() {
    }

    public static synchronized void tocar(String ruta) {
        if (ruta.equals(rutaActual) && actual != null && actual.isRunning()) {
            return;
        }
        parar();
        URL url = Musica.class.getResource(ruta);
        if (url == null) {
            return;
        }
        try {
            AudioInputStream original = AudioSystem.getAudioInputStream(url);
            AudioFormat base = original.getFormat();
            AudioInputStream in = original;
            if (base.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || base.getSampleSizeInBits() != 16) {
                AudioFormat destino = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(),
                        16, base.getChannels(), base.getChannels() * 2, base.getSampleRate(), false);
                in = AudioSystem.getAudioInputStream(destino, original);
            }
            Clip c = AudioSystem.getClip();
            c.open(in);
            if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl v = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                v.setValue(Math.max(v.getMinimum(), Math.min(v.getMaximum(), volumenDb + Ajustes.db(Ajustes.fMusica()))));
            }
            c.loop(Clip.LOOP_CONTINUOUSLY);
            actual = c;
            rutaActual = ruta;
            dbBase = volumenDb;
        } catch (Exception e) {
            System.err.println("[Musica] No se pudo tocar " + ruta + ": " + e.getMessage());
        }
    }

    public static synchronized void parar() {
        if (actual != null) {
            actual.stop();
            actual.close();
        }
        actual = null;
        rutaActual = null;
    }

    /** Musica de fondo mientras se juega: la primera que exista de estas (todas ya estan en el proyecto). */
    private static final String[] JUEGO = {"/sonidos/musica_isla.wav", "/musica/elegir_rol.wav", "/sonidos/musica_titulo.wav"};

    /** Volumen de la musica mientras se juega (mas bajita para que se oigan los dialogos). */
    public static float volumenJuegoDb = -15f;

    /** Pone la musica del juego (no la reinicia si ya suena). En la niebla suena mas bajito. */
    public static synchronized void ambienteJuego(boolean niebla) {
        for (String r : JUEGO) {
            if (Musica.class.getResource(r) != null) {
                float antes = volumenDb;
                volumenDb = niebla ? volumenJuegoDb - 7 : volumenJuegoDb;
                tocar(r);
                volumenDb = antes;
                ajustarVolumen(niebla ? volumenJuegoDb - 7 : volumenJuegoDb);
                return;
            }
        }
    }

    /** Lo llama el boton del parlante: vuelve a poner el volumen de lo que esta sonando. */
    public static synchronized void reaplicarVolumen() {
        ajustarVolumen(dbBase);
    }

    private static void ajustarVolumen(float db) {
        dbBase = db;
        db += Ajustes.db(Ajustes.fMusica());
        if (actual != null && actual.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl v = (FloatControl) actual.getControl(FloatControl.Type.MASTER_GAIN);
            v.setValue(Math.max(v.getMinimum(), Math.min(v.getMaximum(), db)));
        }
    }
}
