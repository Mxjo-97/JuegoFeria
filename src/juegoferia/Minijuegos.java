package juegoferia;

import java.util.function.Supplier;

/**
 * Registro de TODOS los minijuegos (lo usa la pantalla de pruebas PantallaMinijuegos).
 * NO confundir con Minijuego (la clase base, en singular).
 *
 * Para agregar uno: crea la clase (hereda de Minijuego o de MjOrdenar) y agregala a LISTA.
 */
public final class Minijuegos {

    private Minijuegos() {
    }

    public static final class Entrada {

        public final String titulo, descripcion, tipo;
        public final Supplier<Minijuego> crear;

        Entrada(String titulo, String tipo, String descripcion, Supplier<Minijuego> crear) {
            this.titulo = titulo;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.crear = crear;
        }
    }

    public static final Entrada[] LISTA = {
        new Entrada("Rastrea el audio", "Fijo · Panadería · Ciudadano",
                "Sube por el árbol de reenvíos hasta el origen.", () -> new MjRastreaAudio(false, false)),
        new Entrada("La fecha del agua", "Fijo · Malecón · Influencer",
                "Ordena las siete tarjetas por fecha.", () -> new MjFechaDelAgua(false)),
        new Entrada("El papel que nadie leyó", "Fijo · Junta · Candidato",
                "Coincide, no coincide o no aparece.", () -> new MjPapelQueNadieLeyo(false)),
        new Entrada("Los faroles", "Cooperativo · Apagón · Los cuatro",
                "Cada quien su tecla: A, F, J, L.", MjFaroles::new),
        new Entrada("Los pergaminos", "Fijo · Cena · Periodista",
                "Arma el pergamino de Melquíades.", MjPergaminos::new),
        new Entrada("Las cinco de una noticia", "Fijo · Día 2 · Panadería · Periodista",
                "Arma la citación rota y marca qué le falta al audio.", () -> new MjCincoNoticia(false, false)),
        new Entrada("El recorrido de la circular", "Fijo · Día 2 · Alcaldía · Influencer",
                "Tres copias para cortar el audio en la Alcaldía.", () -> new MjRecorridoCircular(false)),
        new Entrada("Quién mira", "Fijo · Día 2 · Malecón · Candidato",
                "El camino de la grabación de las dos cámaras.", () -> new MjQuienMira(false)),
        new Entrada("La lista con permiso", "Fijo · Día 2 · Junta · Ciudadano",
                "¿Firmó la hoja de permiso o no figura?", () -> new MjListaPermiso(false)),
        new Entrada("Los pergaminos (Día 2)", "Fijo · Día 2 · Cena · Ciudadano",
                "El pergamino de la segunda noche.", () -> new MjPergaminos(2)),
        new Entrada("Teléfono roto", "Esporádico · si alguien difundió",
                "¿A quién le cuentas la verdad primero?",
                () -> new MjTelefonoRoto(GrafoRelaciones.crearBarrioMacondo(), "nubia")),
        new Entrada("Las mariposas escriben", "Esporádico · si alguien se tomó un tinto",
                "Toca las mariposas en orden.", MjMariposasEscriben::new)
    };

    public static Minijuego crear(int indice) {
        return indice >= 0 && indice < LISTA.length ? LISTA[indice].crear.get() : null;
    }

    public static int cantidad() {
        return LISTA.length;
    }
}
