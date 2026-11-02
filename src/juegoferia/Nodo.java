package juegoferia;

import java.util.ArrayList;
import java.util.List;

public class Nodo {
    String id;
    String tipo;
    String descripcion;
    List<Nodo> hijos;
    Efecto efecto;
    Coleccionable coleccionable;

    public Nodo(String id, String tipo, String descripcion) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.hijos = new ArrayList<>();
        this.efecto = new Efecto();
    }

    public void agregarHijo(Nodo hijo) {
        this.hijos.add(hijo);
    }

    public boolean esHoja() {
        return hijos.isEmpty();
    }

    // Busca entre los hijos el que corresponde a la decisión elegida.
    // "verificar" encuentra "pub1_verificar"; "viral" encuentra "pub1_difundir_viral"
    // pero noo "pub1_difundir_noviral" (el guion bajo evita la confusión).
    public Nodo buscarHijo(String decision) {
        for (Nodo hijo : hijos) {
            if (hijo.id.equals(decision) || hijo.id.endsWith("_" + decision)) {
                return hijo;
            }
        }
        return null;
    }
}
