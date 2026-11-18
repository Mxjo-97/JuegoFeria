package juegoferia;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todo lo que recuerda la partida (reemplaza a PartidaInfluencer):
 *   - los cuatro jugadores (cada uno con sus medidores personales);
 *   - lo COMPARTIDO: animo de la casa, confianza entre roles, desinformacion de cada lugar, niebla;
 *   - el dia, el reloj, el chat de la casa, los Letreros y las banderas generales del guion.
 */
public class Partida {

    // ---- claves COMPARTIDAS que se pueden poner en un Efecto (junto a las personales) ----
    public static final String ANIMO = "animo_casa";               // 0 a 10, empieza en 5
    public static final String CONFIANZA_ROLES = "confianza_roles"; // sube/baja TODAS las aristas
    public static final String DESINFO_LUGAR = "desinfo_lugar";     // desinformacion del lugar actual

    public static final int ANIMO_INICIAL = 5, ANIMO_MAX = 10;
    public static final int CONFIANZA_INICIAL = 2;
    public static final int DESINFO_INICIAL = 40, DESINFO_MAX = 100;
    /** Si la desinformacion de un lugar pasa de aqui sin corregirse, la niebla entra. */
    public static final int UMBRAL_NIEBLA = 70;
    public static final int DIAS = 2;

    private final Map<Rol, Jugador> jugadores = new EnumMap<>(Rol.class);
    private Rol lider = Rol.CIUDADANO;

    public int dia = 1;
    /** true = MODO FERIA (partida corta: panaderia, malecon, junta y final). */
    public boolean feria;
    public int animo = ANIMO_INICIAL;
    public final GrafoConfianza confianza = new GrafoConfianza(CONFIANZA_INICIAL);
    public final Reloj reloj = new Reloj();
    public final ChatCasa chat = new ChatCasa();
    public final GrafoLocaciones mapa = GrafoLocaciones.crearMapaIsla();
    public final GrafoRelaciones barrio = GrafoRelaciones.crearBarrioMacondo();

    private final Map<String, Double> desinfo = new HashMap<>();
    private final Set<String> niebla = new HashSet<>();
    private final Set<String> banderas = new HashSet<>();
    private final Map<String, Letrero> letreros = new LinkedHashMap<>();

    // ===================== JUGADORES =====================

    /** Lo llama la pantalla de elegir personaje. Cada rol se elige una sola vez. */
    public boolean agregarJugador(String nombre, Rol rol) {
        if (jugadores.containsKey(rol)) {
            return false;
        }
        jugadores.put(rol, new Jugador(nombre, rol));
        return true;
    }

    /** Suelta un rol (la pantalla de elegir personaje deja cambiar de opinion). */
    public void quitarJugador(Rol rol) {
        jugadores.remove(rol);
    }

    public boolean estaCompleta() {
        return jugadores.size() == Rol.values().length;
    }

    public boolean ocupado(Rol r) {
        return jugadores.containsKey(r);
    }

    public Jugador jugador(Rol r) {
        return jugadores.get(r);
    }

    /** En el orden Ciudadano, Periodista, Influencer, Candidato. */
    public List<Jugador> jugadores() {
        return new ArrayList<>(jugadores.values());
    }

    /** Todos menos los que se pasan. */
    public List<Jugador> otros(Rol... menos) {
        List<Jugador> r = new ArrayList<>();
        for (Jugador j : jugadores.values()) {
            boolean fuera = false;
            for (Rol m : menos) {
                fuera |= j.rol == m;
            }
            if (!fuera) {
                r.add(j);
            }
        }
        return r;
    }

    /** El jugador que camina ahora (los otros lo siguen). Las teclas 1-4 lo cambian. */
    public Jugador lider() {
        Jugador j = jugadores.get(lider);
        return j != null ? j : (jugadores.isEmpty() ? null : jugadores.values().iterator().next());
    }

    public void setLider(Rol r) {
        if (jugadores.containsKey(r)) {
            lider = r;
        }
    }

    /** Pasa el turno de caminar al siguiente rol. */
    public Jugador siguienteLider() {
        Rol[] r = Rol.values();
        for (int i = 1; i <= r.length; i++) {
            Rol s = r[(lider.ordinal() + i) % r.length];
            if (jugadores.containsKey(s)) {
                lider = s;
                break;
            }
        }
        return lider();
    }

    // ===================== EFECTOS =====================

    /**
     * Aplica un efecto a un jugador. Las claves personales van a su EstadoJuego y las compartidas
     * (ANIMO, CONFIANZA_ROLES, DESINFO_LUGAR) a la partida. En la niebla lo bueno vale la mitad.
     * Devuelve los cambios que de verdad se aplicaron (para mostrarlos en pantalla).
     */
    public Map<String, Integer> aplicar(Jugador j, Efecto ef, String etiqueta, String lugar) {
        Map<String, Integer> real = new LinkedHashMap<>();
        if (ef == null) {
            return real;
        }
        boolean enNiebla = lugar != null && hayNiebla(lugar);
        Efecto personal = new Efecto();
        for (Map.Entry<String, Integer> c : ef.getCambios().entrySet()) {
            String k = c.getKey();
            int v = c.getValue();
            boolean bueno = k.equals(DESINFO_LUGAR) ? v < 0 : v > 0;
            if (enNiebla && bueno) {
                v = v / 2 == 0 ? (v > 0 ? 1 : -1) : v / 2;
            }
            if (v == 0) {
                continue;
            }
            switch (k) {
                case ANIMO:
                    sumarAnimo(v);
                    break;
                case CONFIANZA_ROLES:
                    confianza.sumarTodas(v);
                    break;
                case DESINFO_LUGAR:
                    if (lugar != null) {
                        sumarDesinfo(lugar, v);
                    }
                    break;
                default:
                    if (!EstadoJuego.esPersonal(k)) {
                        System.out.println("[Partida] Indicador desconocido, se ignora: " + k);
                        continue;
                    }
                    personal.agregar(k, v);
            }
            real.put(k, v);
        }
        if (j != null) {
            j.estado.aplicarEfecto(personal, etiqueta);
        }
        return real;
    }

    public void sumarAnimo(int v) {
        animo = Math.max(0, Math.min(ANIMO_MAX, animo + v));
    }

    /** Lo que dice Dona Rosa en la cena segun el animo. */
    public String nivelAnimo() {
        return animo >= 7 ? "alto" : animo >= 4 ? "medio" : "bajo";
    }

    // ===================== DESINFORMACION Y NIEBLA =====================

    public double desinfo(String lugar) {
        return desinfo.getOrDefault(lugar, (double) DESINFO_INICIAL);
    }

    public void sumarDesinfo(String lugar, double v) {
        desinfo.put(lugar, Math.max(0, Math.min(DESINFO_MAX, desinfo(lugar) + v)));
    }

    public void fijarDesinfo(String lugar, double v) {
        desinfo.put(lugar, Math.max(0, Math.min(DESINFO_MAX, v)));
    }

    /** true si la desinformacion paso el umbral (la niebla puede entrar esa noche o en el acto). */
    public boolean pasoUmbral(String lugar) {
        return desinfo(lugar) > UMBRAL_NIEBLA;
    }

    public boolean hayNiebla(String lugar) {
        return niebla.contains(lugar);
    }

    public void nieblaOn(String lugar) {
        niebla.add(lugar);
    }

    public void nieblaOff(String lugar) {
        niebla.remove(lugar);
    }

    // ===================== BANDERAS GENERALES =====================
    // (las personales, como "difundio_sin_verificar", estan en cada Jugador)

    public boolean bandera(String b) {
        return banderas.contains(b);
    }

    public void poner(String b) {
        banderas.add(b);
    }

    /** true si ALGUN jugador tiene esa bandera personal. */
    public boolean alguien(String bandera) {
        for (Jugador j : jugadores.values()) {
            if (j.tiene(bandera)) {
                return true;
            }
        }
        return false;
    }

    // ===================== LETREROS =====================

    public Letrero letrero(String lugar, String texto) {
        return letreros.computeIfAbsent(lugar, k -> new Letrero(k, texto));
    }

    public Letrero letrero(String lugar) {
        return letreros.get(lugar);
    }

    public List<Letrero> letreros() {
        return new ArrayList<>(letreros.values());
    }

    // ===================== HABILIDADES =====================

    /** Presta la habilidad de 'de' a 'a'. Sube 1 la confianza entre los dos. */
    public boolean prestar(Rol de, Rol a) {
        Jugador recibe = jugador(a);
        if (recibe == null || de == a || confianza.estaBloqueada(de, a)) {
            return false;
        }
        recibe.prestada = de.habilidad;
        recibe.prestadaPor = de;
        confianza.sumar(de, a, 1);
        return true;
    }

    // ===================== DIAS =====================

    public int totalMariposas() {
        int t = 0;
        for (Jugador j : jugadores.values()) {
            t += j.mariposas;
        }
        return t;
    }

    public void avanzarDia() {
        dia++;
        reloj.reiniciar();
        letreros.clear();
        for (Jugador j : jugadores.values()) {
            j.devolverPrestada();
        }
        // los bloqueos de "irse de la mesa" duran el dia siguiente; se levantan al empezar el otro
    }

    /** Reemplaza {CIUDADANO}, {PERIODISTA}... por el nombre de quien juega ese rol. */
    public String reemplazar(String texto) {
        String t = texto;
        for (Rol r : Rol.values()) {
            Jugador j = jugadores.get(r);
            t = t.replace("{" + r.name() + "}", j != null ? j.nombre : r.nombre);
        }
        return t;
    }

    public void imprimir() {
        System.out.println("===== PARTIDA · Día " + dia + " · " + reloj.texto() + " =====");
        System.out.println("Ánimo de la casa: " + animo + "   Confianza entre roles: " + confianza.promedio());
        for (Jugador j : jugadores.values()) {
            j.estado.imprimir(j.titulo() + "  mariposas=" + j.mariposas);
        }
    }
}
