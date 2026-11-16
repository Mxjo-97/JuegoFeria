package juegoferia;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Para escribir el guion como una lista de pasos, sin lambdas metidas dentro de lambdas.
 * Cada paso recibe "siguiente" y lo llama cuando termina (por ejemplo, al final de un dialogo).
 *
 *   new Secuencia()
 *       .paso(sig -> l.dialogo(sig, "DOÑA ROSA: Siéntense."))
 *       .paso(sig -> l.minijuegoFijo(Rol.PERIODISTA, new MjPergaminos(), r -> sig.run()))
 *       .paso(sig -> l.anfitrion().terminarDia())
 *       .iniciar();
 */
public class Secuencia {

    private final List<Consumer<Runnable>> pasos = new ArrayList<>();
    private int i = -1;

    public Secuencia paso(Consumer<Runnable> p) {
        pasos.add(p);
        return this;
    }

    /** Paso que no espera nada (ej: aplicar un efecto). */
    public Secuencia hacer(Runnable r) {
        pasos.add(sig -> {
            r.run();
            sig.run();
        });
        return this;
    }

    public void iniciar() {
        i = -1;
        siguiente();
    }

    private void siguiente() {
        i++;
        if (i < pasos.size()) {
            pasos.get(i).accept(this::siguiente);
        }
    }
}
