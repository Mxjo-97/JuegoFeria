package juegoferia;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** Recorridos de arboles (Nodo): BFS por niveles y DFS en preorden. */
public final class Recorridos {

    private Recorridos() {
    }

    public static List<Nodo> recorridoBFS(Nodo raiz) {
        List<Nodo> resultado = new ArrayList<>();
        Queue<Nodo> cola = new LinkedList<>();
        cola.add(raiz);
        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();
            resultado.add(actual);
            cola.addAll(actual.hijos);
        }
        return resultado;
    }

    public static List<Nodo> recorridoDFS(Nodo raiz) {
        List<Nodo> resultado = new ArrayList<>();
        dfs(raiz, resultado);
        return resultado;
    }

    private static void dfs(Nodo nodo, List<Nodo> resultado) {
        resultado.add(nodo);
        for (Nodo hijo : nodo.hijos) {
            dfs(hijo, resultado);
        }
    }

    /** Altura del arbol (un solo nodo = 0). */
    public static int altura(Nodo raiz) {
        int max = -1;
        for (Nodo h : raiz.hijos) {
            max = Math.max(max, altura(h));
        }
        return max + 1;
    }

    /** Cuantos reenvios hay en total (todos los nodos menos la raiz). */
    public static int reenvios(Nodo raiz) {
        return recorridoBFS(raiz).size() - 1;
    }

    /** Las hojas: los ultimos que recibieron el rumor. */
    public static List<Nodo> hojas(Nodo raiz) {
        List<Nodo> r = new ArrayList<>();
        for (Nodo n : recorridoDFS(raiz)) {
            if (n.esHoja()) {
                r.add(n);
            }
        }
        return r;
    }
}
