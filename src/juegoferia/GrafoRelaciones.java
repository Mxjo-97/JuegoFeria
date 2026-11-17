package juegoferia;

import java.util.*;

/**
 * Grafo de relaciones del barrio (NO dirigido).
 *
 * - Cada nodo es un ciudadano (se identifica por su nombre).
 * - Cada arista significa "se conocen / se hablan".
 *
 * Es solo la estructura: no tiene reglas de ningun minijuego. Quien lo use
 * (por ejemplo el minijuego de rumores) puede guardar aparte lo que necesite
 * de cada ciudadano, como un Map<String, ...> con su estado.
 */
public class GrafoRelaciones {

    private final Map<String, Set<String>> conexiones = new LinkedHashMap<>();

    //constuir o modificar

    public void agregarCiudadano(String nombre) {
        conexiones.putIfAbsent(nombre, new LinkedHashSet<>());
    }

    /** Quita al ciudadano y todas sus conexiones. */
    public boolean quitarCiudadano(String nombre) {
        Set<String> vecinos = conexiones.remove(nombre);
        if (vecinos == null) {
            return false;
        }
        for (String v : vecinos) {
            conexiones.get(v).remove(nombre);
        }
        return true;
    }

    /** Une a dos ciudadanos (en los dos sentidos). Si no existen, los crea. */
    public void conectar(String a, String b) {
        if (a.equals(b)) {
            return;
        }
        agregarCiudadano(a);
        agregarCiudadano(b);
        conexiones.get(a).add(b);
        conexiones.get(b).add(a);
    }

    /** Corta la relacion entre dos ciudadanos. Devuelve false si no estaban conectados. */
    public boolean desconectar(String a, String b) {
        if (!estanConectados(a, b)) {
            return false;
        }
        conexiones.get(a).remove(b);
        conexiones.get(b).remove(a);
        return true;
    }

    //CONSULTAS

    public boolean existe(String nombre) {
        return conexiones.containsKey(nombre);
    }

    public boolean estanConectados(String a, String b) {
        Set<String> v = conexiones.get(a);
        return v != null && v.contains(b);
    }

    public Set<String> getCiudadanos() {
        return Collections.unmodifiableSet(conexiones.keySet());
    }

    public int cantidadCiudadanos() {
        return conexiones.size();
    }

    /** Con quienes esta conectado un ciudadano (vacio si no existe). */
    public Set<String> vecinos(String nombre) {
        Set<String> v = conexiones.get(nombre);
        return v == null ? Collections.emptySet() : Collections.unmodifiableSet(v);
    }

    /** Cuantas conexiones tiene un ciudadano. */
    public int grado(String nombre) {
        return vecinos(nombre).size();
    }

    // RECORRIDOS

    /** Recorrido por niveles (BFS) desde un ciudadano. */
    public List<String> recorridoBFS(String inicio) {
        return new ArrayList<>(distanciasDesde(inicio).keySet());
    }

    /** Recorrido en profundidad (DFS) desde un ciudadano. */
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
        for (String v : conexiones.get(actual)) {
            if (!visitados.contains(v)) {
                dfs(v, visitados, resultado);
            }
        }
    }

    /**
     * A cuantos "saltos" esta cada ciudadano alcanzable desde el inicio (BFS).
     * 0 = el mismo, 1 = sus vecinos, 2 = los vecinos de sus vecinos, etc.
     * Sirve para ver "olas" que se expanden desde un punto.
     */
    public Map<String, Integer> distanciasDesde(String inicio) {
        Map<String, Integer> distancia = new LinkedHashMap<>();
        if (!existe(inicio)) {
            return distancia;
        }
        Queue<String> cola = new LinkedList<>();
        cola.add(inicio);
        distancia.put(inicio, 0);

        while (!cola.isEmpty()) {
            String actual = cola.poll();
            for (String v : conexiones.get(actual)) {
                if (!distancia.containsKey(v)) {
                    distancia.put(v, distancia.get(actual) + 1);
                    cola.add(v);
                }
            }
        }
        return distancia;
    }

    /** Camino con menos saltos entre dos ciudadanos. Lista vacia si no hay camino. */
    public List<String> caminoMasCorto(String origen, String destino) {
        List<String> camino = new ArrayList<>();
        if (!existe(origen) || !existe(destino)) {
            return camino;
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
            for (String v : conexiones.get(actual)) {
                if (!padre.containsKey(v)) {
                    padre.put(v, actual);
                    cola.add(v);
                }
            }
        }

        if (!padre.containsKey(destino)) {
            return camino;
        }
        for (String n = destino; n != null; n = padre.get(n)) {
            camino.add(0, n);
        }
        return camino;
    }
  
    /** El vecino con mas conexiones: si el rumor llega ahi, llega a todos (habilidad del Ciudadano). */
    public String masConectado() {
        String mejor = null;
        for (String c : conexiones.keySet()) {
            if (mejor == null || grado(c) > grado(mejor)) {
                mejor = c;
            }
        }
        return mejor;
    }

    /** Cuantos vecinos alcanza un mensaje que sale de 'inicio' en a lo sumo 'pasos' reenvios (BFS). */
    public int alcanceEn(String inicio, int pasos) {
        int n = 0;
        for (int d : distanciasDesde(inicio).values()) {
            if (d > 0 && d <= pasos) {
                n++;
            }
        }
        return n;
    }

    /**
     * El barrio de Macondo. Los nombres son los IDS de los Actor (asi la habilidad
     * "ver vinculos" puede dibujar lineas entre los personajes que estan en pantalla).
     * AJUSTAR cuando el guion agregue vecinos.
     */
    public static GrafoRelaciones crearBarrioMacondo() {
        GrafoRelaciones g = new GrafoRelaciones();
        // panaderia
        g.conectar("rosa", "samuel");
        g.conectar("rosa", "ursula");
        g.conectar("rosa", "nubia");
        g.conectar("nubia", "yolanda");
        g.conectar("nubia", "eliecer");
        g.conectar("rosa", "mateo");
        g.conectar("mateo", "esperanza");
        g.conectar("ximena", "lorena");
        g.conectar("ximena", "salome");
        g.conectar("lorena", "salome");
        g.conectar("samuel", "salome");
        g.conectar("vlado", "visitacion");
        g.conectar("rosa", "vlado");
        // malecon
        g.conectar("chela", "emiro");
        g.conectar("chela", "daniela");
        g.conectar("emiro", "daniela");
        g.conectar("julian", "beatriz");
        g.conectar("rosa", "chela");
        // junta
        g.conectar("alirio", "gustavo");
        g.conectar("petra", "wilson");
        g.conectar("petra", "amaranta");
        g.conectar("petra", "alirio");
        g.conectar("sofia", "petra");
        g.conectar("wilson", "julian");
        g.conectar("mauricio", "petra");
        g.conectar("nubia", "wilson");
        return g;
    }

    public void imprimir() {
        for (Map.Entry<String, Set<String>> e : conexiones.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }
    }
}