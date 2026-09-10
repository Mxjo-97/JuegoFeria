package juegoferia;

    import java.util.*;

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
}