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

    /** Locaciones que todavia no se resolvieron en el dia actual. */
    public List<String> pendientes(ControladorDePartidas partida) {
        List<String> lista = new ArrayList<>();
        for (String isla : adyacencia.keySet()) {
            if (!partida.laIslaResuelta(isla)) {
                lista.add(isla);
            }
        }
        return lista;
    }

    /** Ciudad del juego. AJUSTAR las conexiones y distancias a como quede el mapa real. */
    public static GrafoLocaciones crearCiudadPorDefecto() {
        GrafoLocaciones g = new GrafoLocaciones();
        g.conectar("Panaderia", "Plaza", 2);
        g.conectar("Plaza", "Colegio", 3);
        g.conectar("Plaza", "Parque", 2);
        g.conectar("Plaza", "Junta_Comunal", 4);
        g.conectar("Parque", "Colegio", 3);
        return g;
    }

    public void imprimir() {
        for (Map.Entry<String, Map<String, Integer>> e : adyacencia.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
    }
}
