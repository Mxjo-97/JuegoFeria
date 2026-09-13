package juegoferia;


import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class Recorridos {


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
        dfsAuxiliar(raiz, resultado);
        return resultado;
    }

    private static void dfsAuxiliar(Nodo nodo, List<Nodo> resultado) {
        resultado.add(nodo);
        for (Nodo hijo : nodo.hijos) {
            dfsAuxiliar(hijo, resultado);
        }
    }

    private static String describir(Nodo n) {
        String texto = n.id + " [" + n.tipo + "]: " + n.descripcion;
        if (n.tipo.equals("hoja")) {
            texto += "  -> efecto: " + n.efecto.getCambios();
            if (n.coleccionable != null) {
                texto += "  (coleccionable: " + n.coleccionable.nombre + ")";
            }
        }
        return texto;
    }

    public static void main(String[] args) {
      
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        Nodo raizEscena1 = ArbolPublicacion.construirArbolEscena1();

        System.out.println("=== BFS (por niveles) — Escena 1 ===");
        for (Nodo n : recorridoBFS(raizEscena1)) {
            System.out.println(describir(n));
        }

        System.out.println("\n=== DFS (preorden) — Escena 1 ===");
        for (Nodo n : recorridoDFS(raizEscena1)) {
            System.out.println(describir(n));
        }

        System.out.println("\n=== Solo el menu (hijos directos de la raiz) ===");
        for (Nodo hijo : raizEscena1.hijos) {
            System.out.println(" -> " + describir(hijo));
        }

        System.out.println("\n=== DFS (preorden) — Escena 4 (viral, 5 hijos) ===");
        Nodo raizEscena4 = ArbolPublicacion.construirArbolEscena4();
        for (Nodo n : recorridoDFS(raizEscena4)) {
            System.out.println(describir(n));
        }
    }
}