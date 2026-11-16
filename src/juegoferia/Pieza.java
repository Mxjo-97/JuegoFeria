package juegoferia;

/**
 * Una pieza del Letrero (reemplaza a Coleccionable).
 * Cada rol consigue una distinta al final del rumor de cada lugar:
 *   Ciudadano = material, Periodista = frase verdadera, Influencer = pintura, Candidato = firma.
 *
 * Imagen opcional para mostrarla en la mesa: /imagenes/piezas/<id>.png
 */
public class Pieza {

    public final String id;
    public final String nombre;     // "Cartón de Doña Rosa"
    public final Rol rol;
    public final String lugar;      // id del lugar (GrafoLocaciones.PANADERIA...)

    public Pieza(String id, String nombre, Rol rol, String lugar) {
        this.id = id;
        this.nombre = nombre;
        this.rol = rol;
        this.lugar = lugar;
    }

    public String imagen() {
        return "/imagenes/piezas/" + id + ".png";
    }

    @Override
    public String toString() {
        return nombre + " (" + rol.pieza + ")";
    }
}
