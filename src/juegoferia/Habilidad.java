package juegoferia;

/**
 * Las cuatro habilidades. Cada una es de un rol, pero se puede PRESTAR a otro jugador
 * (eso sube 1 la confianza entre los dos). Lo que hace cada una en pantalla esta en
 * Lugar.usarHabilidad(). La primera vez en cada lugar tienen efecto fuerte (bajan y frenan el rumor).
 */
public enum Habilidad {

    VER_VINCULOS("Ver vínculos", "Ves quién habla con quién y a quién te falta escuchar. Frena el rumor.",
            "Miren, estos dos se hablan todos los días. Si el rumor llega ahí, llega a todos."),

    LUPA("Lupa", "Muestra el rumor tal cual y da pista en el minijuego. Baja y frena el rumor.",
            "Aquí cambió 'se rumora' por 'dicen'. Es una sola palabra, pero cambia todo."),

    MEGAFONO("Megáfono", "Un aviso a todo el lugar: el rumor baja mucho y se queda quieto 20 s.",
            "Va el aviso. Corto, claro y sin gritar."),

    CONVOCATORIA("Convocatoria", "Los vecinos hacen círculo a tu lado y sube el ánimo de la casa.",
            "Vecinos, cinco minutos. Hablemos de esto de frente.");

    public final String nombre;
    public final String efecto;
    public final String linea;

    Habilidad(String nombre, String efecto, String linea) {
        this.nombre = nombre;
        this.efecto = efecto;
        this.linea = linea;
    }
}
