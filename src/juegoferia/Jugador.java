package juegoferia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Una persona que juega: su nombre, el rol que escogio y todo lo PERSONAL.
 * Lo compartido (animo de la casa, desinformacion, confianza entre roles) esta en Partida.
 */
public class Jugador {

    public final String nombre;
    public final Rol rol;
    public final EstadoJuego estado = new EstadoJuego();

    /** Mariposas amarillas: solo las ve quien las gano. */
    public int mariposas;

    /** Piezas ganadas, por lugar (una por lugar). */
    private final Map<String, Pieza> piezas = new LinkedHashMap<>();

    /** Banderas personales del guion: "difundio_sin_verificar", "se_rio_con_eliecer"... */
    private final Set<String> banderas = new HashSet<>();

    /** Objetos pequenos que regala una reconciliacion. */
    public final List<String> objetos = new ArrayList<>();

    /** Habilidad que otro jugador le presto (null si no tiene). */
    public Habilidad prestada;
    public Rol prestadaPor;

    public Jugador(String nombre, Rol rol) {
        this.nombre = nombre == null || nombre.isBlank() ? rol.nombre : nombre.trim();
        this.rol = rol;
    }

    // ---- banderas ----

    public boolean tiene(String bandera) {
        return banderas.contains(bandera);
    }

    public void poner(String bandera) {
        banderas.add(bandera);
    }

    public void quitar(String bandera) {
        banderas.remove(bandera);
    }

    // ---- piezas ----

    public void darPieza(Pieza p) {
        piezas.put(p.lugar, p);
    }

    public Pieza pieza(String lugar) {
        return piezas.get(lugar);
    }

    public Pieza sacarPieza(String lugar) {
        return piezas.remove(lugar);
    }

    public List<Pieza> piezas() {
        return new ArrayList<>(piezas.values());
    }

    // ---- habilidades ----

    /** Las habilidades que puede usar ahora: la suya y, si se la prestaron, otra. */
    public List<Habilidad> habilidades() {
        List<Habilidad> h = new ArrayList<>();
        h.add(rol.habilidad);
        if (prestada != null) {
            h.add(prestada);
        }
        return h;
    }

    public void devolverPrestada() {
        prestada = null;
        prestadaPor = null;
    }

    /** "Ana (Periodista)". */
    public String titulo() {
        return nombre.equals(rol.nombre) ? rol.nombre : nombre + " (" + rol.nombre + ")";
    }

    @Override
    public String toString() {
        return titulo();
    }
}
