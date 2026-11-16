package juegoferia;

import java.util.*;

/**
 * Grafo NO dirigido y con peso: cada nodo es una locacion (isla) del juego
 * y cada arista es un camino entre dos locaciones (el peso es la distancia).
 * Usa lista de adyacencia.
 */
public class GrafoLocaciones {

    private final Map<String, Map<String, Integer>> adyacencia = new LinkedHashMap<>();

    public void agregarLocacion(String nombre) {
        adyacencia.putIfAbsent(nombre, new LinkedHashMap<>());
    }

    public void conectar(String a, String b, int distancia) {
        agregarLocacion(a);
        agregarLocacion(b);
        adyacencia.get(a).put(b, distancia);
        adyacencia.get(b).put(a, distancia);
    }

    public boolean existe(String nombre) {
        return adyacencia.containsKey(nombre);
    }

    public Set<String> getLocaciones() {
        return adyacencia.keySet();
    }

    public Set<String> vecinos(String nombre) {
        Map<String, Integer> v = adyacencia.get(nombre);
        return v == null ? Collections.emptySet() : v.keySet();
    }

    public int distancia(String a, String b) {
        Map<String, Integer> v = adyacencia.get(a);
        return (v != null && v.containsKey(b)) ? v.get(b) : -1;
    }

    /** Camino con menos saltos entre dos locaciones (BFS). Lista vacia si no hay camino. */
    public List<String> caminoMasCorto(String origen, String destino) {
        if (!existe(origen) || !existe(destino)) {
            return new ArrayList<>();
        }

        Map<String, String> padre = new HashMap<>();
        Queue<String> cola = new LinkedList<>();
        cola.add(origen);
        padre.put(origen, null);

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            if (actual.equals(destino)) {
                break;
            }
            for (String v : vecinos(actual)) {
                if (!padre.containsKey(v)) {
                    padre.put(v, actual);
                    cola.add(v);
                }
            }
        }

        List<String> camino = new ArrayList<>();
        if (!padre.containsKey(destino)) {
            return camino;
        }
        for (String n = destino; n != null; n = padre.get(n)) {
            camino.add(0, n);
        }
        return camino;
    }

    /** Suma de distancias a lo largo de un camino. */
    public int distanciaTotal(List<String> camino) {
        int total = 0;
        for (int i = 0; i < camino.size() - 1; i++) {
            total += distancia(camino.get(i), camino.get(i + 1));
        }
        return total;
    }

    /** Recorrido en profundidad desde una locacion. */
    public List<String> recorridoDFS(String inicio) {
        List<String> resultado = new ArrayList<>();
        if (existe(inicio)) {
            dfs(inicio, new HashSet<>(), resultado);
        }
        return resultado;
    }

    private void dfs(String actual, Set<String> visitados, List<String> resultado) {
        visitados.add(actual);
        resultado.add(actual);
        for (String v : vecinos(actual)) {
            if (!visitados.contains(v)) {
                dfs(v, visitados, resultado);
            }
        }
    }

    /**
     * Camino de MENOS MINUTOS entre dos locaciones (Dijkstra con los pesos).
     * Es el que usa el Reloj para saber cuanto tarda el grupo en llegar.
     */
    public List<String> caminoMasRapido(String origen, String destino) {
        List<String> camino = new ArrayList<>();
        if (!existe(origen) || !existe(destino)) {
            return camino;
        }
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> padre = new HashMap<>();
        PriorityQueue<String> cola = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        for (String n : adyacencia.keySet()) {
            dist.put(n, Integer.MAX_VALUE);
        }
        dist.put(origen, 0);
        cola.add(origen);
        Set<String> listos = new HashSet<>();
        while (!cola.isEmpty()) {
            String u = cola.poll();
            if (!listos.add(u)) {
                continue;
            }
            for (Map.Entry<String, Integer> e : adyacencia.get(u).entrySet()) {
                int nd = dist.get(u) + e.getValue();
                if (nd < dist.get(e.getKey())) {
                    dist.put(e.getKey(), nd);
                    padre.put(e.getKey(), u);
                    cola.add(e.getKey());
                }
            }
        }
        if (dist.get(destino) == Integer.MAX_VALUE) {
            return camino;
        }
        for (String n = destino; n != null; n = padre.get(n)) {
            camino.add(0, n);
        }
        return camino;
    }

    /** Minutos de camino entre dos locaciones (0 si es la misma, -1 si no hay camino). */
    public int minutos(String origen, String destino) {
        if (origen.equals(destino)) {
            return 0;
        }
        List<String> c = caminoMasRapido(origen, destino);
        return c.isEmpty() ? -1 : distanciaTotal(c);
    }

    // ===================== MAPA DE LA ISLA =====================
    // Ids de las locaciones. Los usan Lugar, GuionDia1/2 y el Reloj.
    public static final String CASA = "Casa";              // casa de huespedes de Dona Rosa (sobre la panaderia)
    public static final String PANADERIA = "Panaderia";
    public static final String MALECON = "Malecon";
    public static final String JUNTA = "Junta_Comunal";
    public static final String PLAZA = "Plaza";            // OJO: decision pendiente (el apagon pasa aqui)
    public static final String ALCALDIA = "Alcaldia";

    /**
     * La isla. El peso de cada arista son MINUTOS de camino (los usa el Reloj).
     *   Casa - Panaderia (la casa queda encima: 1 min)
     *   Panaderia - Malecon (el "camino al malecon")
     *   Malecon - Junta
     *   Panaderia - Plaza - Junta
     *   Plaza - Alcaldia
     * AJUSTAR a como quede el mapa dibujado.
     */
    public static GrafoLocaciones crearMapaIsla() {
        GrafoLocaciones g = new GrafoLocaciones();
        g.conectar(CASA, PANADERIA, 1);
        g.conectar(PANADERIA, MALECON, 20);
        g.conectar(MALECON, JUNTA, 25);
        g.conectar(PANADERIA, PLAZA, 10);
        g.conectar(PLAZA, JUNTA, 15);
        g.conectar(PLAZA, ALCALDIA, 10);
        return g;
    }

    public void imprimir() {
        for (Map.Entry<String, Map<String, Integer>> e : adyacencia.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
    }
}

