package juegoferia;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo de un arbol general. En el juego se usa para el ARBOL DE REENVIOS de un rumor
 * (minijuego "Rastrea el audio"): la raiz es el origen y cada hijo es un reenvio.
 *
 *   tipo        = quien lo mando (ej: "Grupo de la panadería", "Nubia")
 *   descripcion = el texto tal como llego a ese chat
 *   padre       = de quien lo recibio (null en la raiz)
 */
public class Nodo {

    String id;
    String tipo;
    String descripcion;
    List<Nodo> hijos;
    Nodo padre;
    Efecto efecto;

    public Nodo(String id, String tipo, String descripcion) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.hijos = new ArrayList<>();
        this.efecto = new Efecto();
    }

    public Nodo agregarHijo(Nodo hijo) {
        hijo.padre = this;
        this.hijos.add(hijo);
        return hijo;
    }

    public boolean esHoja() {
        return hijos.isEmpty();
    }

    public boolean esRaiz() {
        return padre == null;
    }

    /** Profundidad: la raiz es 0. */
    public int nivel() {
        int n = 0;
        for (Nodo p = padre; p != null; p = p.padre) {
            n++;
        }
        return n;
    }

    /** Camino desde este nodo hasta la raiz (incluidos los dos). */
    public List<Nodo> caminoALaRaiz() {
        List<Nodo> c = new ArrayList<>();
        for (Nodo p = this; p != null; p = p.padre) {
            c.add(p);
        }
        return c;
    }

    public Nodo buscarHijo(String id) {
        for (Nodo hijo : hijos) {
            if (hijo.id.equals(id) || hijo.id.endsWith("_" + id)) {
                return hijo;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return tipo + ": " + descripcion;
    }
}
