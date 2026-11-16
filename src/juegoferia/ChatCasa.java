package juegoferia;

import java.util.ArrayList;
import java.util.List;

/**
 * El chat de la casa: "Los de Doña Rosa". Aqui se anuncia lo que cada uno hizo con la
 * decision digital ("Periodista se puso a verificar.") y avisos como "Hueco vacío: falta pieza".
 * Lugar dibuja los ultimos mensajes en una esquina.
 */
public class ChatCasa {

    public static final String NOMBRE = "Los de Doña Rosa";

    public static final class Mensaje {

        public final String hora, autor, texto;

        Mensaje(String hora, String autor, String texto) {
            this.hora = hora;
            this.autor = autor;
            this.texto = texto;
        }
    }

    /**
     * Plantillas por defecto de la decision digital (A-E). {rol} se cambia por el rol.
     * Cada lugar puede dar las suyas (ej: "{rol} se puso a verificar de dónde salió el audio.").
     */
    public static final String[] ANUNCIOS_DIGITAL = {
        "{rol} se puso a verificar.",
        "{rol} reportó la publicación.",
        "{rol} se tomó un tinto.",
        "{rol} leyó y no dijo nada.",
        "{rol} difundió lo que sea."
    };

    private final List<Mensaje> mensajes = new ArrayList<>();

    public void escribir(String hora, String autor, String texto) {
        mensajes.add(new Mensaje(hora, autor, texto));
    }

    /** Anuncio de la decision digital. opcion = 0..4 (A..E). plantillas puede ser null. */
    public void anunciarDigital(String hora, Rol rol, int opcion, String[] plantillas) {
        String[] p = plantillas != null ? plantillas : ANUNCIOS_DIGITAL;
        escribir(hora, ChatCasa.NOMBRE, p[opcion].replace("{rol}", rol.nombre));
    }

    public List<Mensaje> ultimos(int n) {
        return new ArrayList<>(mensajes.subList(Math.max(0, mensajes.size() - n), mensajes.size()));
    }

    public List<Mensaje> todos() {
        return new ArrayList<>(mensajes);
    }

    public void limpiar() {
        mensajes.clear();
    }
}
