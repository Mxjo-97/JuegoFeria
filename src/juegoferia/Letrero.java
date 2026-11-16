package juegoferia;

import java.util.EnumMap;
import java.util.Map;

/**
 * El Letrero de un lugar: cuatro huecos, uno por rol. Se arma en la mesa de la cena.
 *
 * Reglas del guion:
 *   4 piezas: baja la desinformacion del lugar 15 y frena la niebla.
 *   3 piezas: baja 10 y frena la niebla UNA noche (vuelve manana).
 *   2 piezas: baja 5 y no frena la niebla.
 *   1 o 0:    no se puede colgar.
 *   Si el minijuego fijo del lugar salio bien, suma 3 mas (si se pudo colgar).
 */
public class Letrero {

    public enum Resultado { IMPECABLE, CON_HUECO, NO_RESISTE }

    public final String lugar;
    public final String texto;
    private final Map<Rol, Pieza> huecos = new EnumMap<>(Rol.class);
    public boolean minijuegoBien;

    public Letrero(String lugar, String texto) {
        this.lugar = lugar;
        this.texto = texto;
    }

    public void poner(Pieza p) {
        if (p != null) {
            huecos.put(p.rol, p);
        }
    }

    public boolean tiene(Rol r) {
        return huecos.containsKey(r);
    }

    public int piezas() {
        return huecos.size();
    }

    public Resultado resultado() {
        int n = piezas();
        return n == 4 ? Resultado.IMPECABLE : n == 3 ? Resultado.CON_HUECO : Resultado.NO_RESISTE;
    }

    public boolean sePuedeColgar() {
        return piezas() >= 2;
    }

    /** Cuanto baja la desinformacion del lugar al colgarlo. */
    public int reduccion() {
        int n = piezas();
        int base = n == 4 ? 15 : n == 3 ? 10 : n == 2 ? 5 : 0;
        return base == 0 ? 0 : base + (minijuegoBien ? 3 : 0);
    }

    /** true si frena la niebla de esta noche. */
    public boolean frenaNiebla() {
        return piezas() >= 3;
    }

    /** true si la frena para siempre (con 3 vuelve manana). */
    public boolean frenaNieblaDelTodo() {
        return piezas() == 4;
    }

    /** El rol cuya pieza falta (solo tiene sentido con 3 piezas). */
    public Rol faltante() {
        for (Rol r : Rol.values()) {
            if (!huecos.containsKey(r)) {
                return r;
            }
        }
        return null;
    }

    /** Lo que se ve cuando falta una pieza (guion: "Resultados segun las piezas"). */
    public static String textoFalta(Rol r) {
        switch (r) {
            case CIUDADANO: return "Falta el material: el Letrero se pinta sobre una hoja suelta y se vuela.";
            case PERIODISTA: return "Falta la frase: la frase sale con huecos y no se entiende.";
            case INFLUENCER: return "Falta la pintura: la letra sale pequeña y borrosa.";
            default: return "Falta la firma: queda sin nombre debajo y los vecinos preguntan quién lo puso.";
        }
    }
}
