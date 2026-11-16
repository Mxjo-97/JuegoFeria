package juegoferia;

/**
 * Confianza ENTRE ROLES como grafo completo, no dirigido y con peso (matriz de adyacencia 4x4).
 * El peso de la arista Ciudadano-Periodista es cuanto confian el uno en el otro (0 a 10).
 *
 * El guion muestra un solo numero ("confianza entre roles", empieza en 2): ese es promedio().
 * Pero hay cosas que solo suben entre dos (prestarse una habilidad, tomar partido en una pelea),
 * y por eso se guarda por parejas.
 *
 * Ademas cada arista puede quedar BLOQUEADA un dia: despues de "irse de la mesa" esa pareja
 * no se puede prestar habilidades al dia siguiente.
 */
public class GrafoConfianza {

    public static final int MIN = 0, MAX = 10;

    private final int n = Rol.values().length;
    private final int[][] peso = new int[n][n];
    private final boolean[][] bloqueada = new boolean[n][n];

    public GrafoConfianza(int inicial) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                peso[i][j] = i == j ? 0 : inicial;
            }
        }
    }

    public int get(Rol a, Rol b) {
        return peso[a.ordinal()][b.ordinal()];
    }

    /** Sube o baja la confianza entre dos roles (la arista es la misma en los dos sentidos). */
    public void sumar(Rol a, Rol b, int v) {
        if (a == b) {
            return;
        }
        int nuevo = Math.max(MIN, Math.min(MAX, get(a, b) + v));
        peso[a.ordinal()][b.ordinal()] = nuevo;
        peso[b.ordinal()][a.ordinal()] = nuevo;
    }

    /** "Confianza entre roles +1" del guion: sube todas las aristas. */
    public void sumarTodas(int v) {
        Rol[] r = Rol.values();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sumar(r[i], r[j], v);
            }
        }
    }

    /** El numero que se muestra en pantalla (promedio de las 6 aristas, redondeado). */
    public int promedio() {
        int suma = 0, aristas = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                suma += peso[i][j];
                aristas++;
            }
        }
        return Math.round(suma / (float) aristas);
    }

    /** El rol en quien mas confia 'a' (su vecino de mayor peso). */
    public Rol masConfiable(Rol a) {
        Rol mejor = null;
        for (Rol b : Rol.values()) {
            if (b != a && (mejor == null || get(a, b) > get(a, mejor))) {
                mejor = b;
            }
        }
        return mejor;
    }

    public void bloquear(Rol a, Rol b, boolean v) {
        bloqueada[a.ordinal()][b.ordinal()] = v;
        bloqueada[b.ordinal()][a.ordinal()] = v;
    }

    public boolean estaBloqueada(Rol a, Rol b) {
        return bloqueada[a.ordinal()][b.ordinal()];
    }

    /** Al empezar un dia nuevo se levantan los bloqueos del dia anterior. */
    public void desbloquearTodo() {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                bloqueada[i][j] = false;
            }
        }
    }

    public void imprimir() {
        Rol[] r = Rol.values();
        StringBuilder sb = new StringBuilder("            ");
        for (Rol c : r) {
            sb.append(String.format("%-11s", c.nombre));
        }
        System.out.println(sb);
        for (int i = 0; i < n; i++) {
            sb = new StringBuilder(String.format("%-12s", r[i].nombre));
            for (int j = 0; j < n; j++) {
                sb.append(String.format("%-11s", i == j ? "-" : peso[i][j] + (bloqueada[i][j] ? "x" : "")));
            }
            System.out.println(sb);
        }
        System.out.println("Promedio (lo que ve el jugador): " + promedio());
    }
}
