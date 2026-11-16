package juegoferia;

/**
 * Las cuatro habilidades. Cada una es de un rol, pero se puede PRESTAR a otro jugador
 * (eso sube 1 la confianza entre los dos). Lo que hace cada una en pantalla esta en
 * Lugar.usarHabilidad().
 */
public enum Habilidad {

    VER_VINCULOS("Ver vínculos", "Se iluminan las líneas entre los vecinos que se hablan.",
            "Miren, estos dos se hablan todos los días. Si el rumor llega ahí, llega a todos."),

    LUPA("Lupa", "Las palabras que cambiaron se marcan en amarillo.",
            "Aquí cambió 'se rumora' por 'dicen'. Es una sola palabra, pero cambia todo."),

    MEGAFONO("Megáfono", "Sale un aviso corto que llega a todo el lugar.",
            "Va el aviso. Corto, claro y sin gritar."),

    CONVOCATORIA("Convocatoria", "Los vecinos se acercan y forman un círculo.",
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
