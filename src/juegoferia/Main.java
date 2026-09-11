package juegoferia;

public class Main {
    public static void main(String[] args) {
        Nodo raiz = ArbolPublicacion.construirArbolEscena1();

        System.out.println("Descripción de la raíz: " + raiz.descripcion);
        System.out.println("Tipo: " + raiz.tipo);
        System.out.println("Número de hijos: " + raiz.hijos.size());

        for (Nodo hijo : raiz.hijos) {
            System.out.println(" - Hijo: " + hijo.descripcion + " (tipo: " + hijo.tipo + ")");
        }

        System.out.println("\n--- Profundizando en cada rama ---");

        for (Nodo hijo : raiz.hijos) {
            System.out.println("\nRama: " + hijo.descripcion);
            if (hijo.hijos.isEmpty()) {
                System.out.println("  (es hoja directa, efecto: " + hijo.efecto.getCambios() + ")");
            } else {
                for (Nodo nieto : hijo.hijos) {
                    System.out.println("  - " + nieto.descripcion + " -> efecto: " + nieto.efecto.getCambios());
                }
            }
        }

        System.out.println("\n--- Probando inserción y eliminación ---");

        ArbolPublicacion.insertarSubArbol("panaderia", raiz);
        System.out.println("¿Hay algo activo en panadería? " + ArbolPublicacion.raicesActivas.containsKey("panaderia"));

        ArbolPublicacion.eliminarSubArbol("panaderia");
        System.out.println("¿Hay algo activo en panadería? " + ArbolPublicacion.raicesActivas.containsKey("panaderia"));

        ArbolPublicacion.eliminarSubArbol("colegio");

        System.out.println("\n--- Verificando las 3 publicaciones nuevas ---");

        Nodo raiz2 = ArbolPublicacion.construirArbolEscena2();
        System.out.println("Escena 2 - hijos: " + raiz2.hijos.size());

        Nodo raiz3 = ArbolPublicacion.construirArbolEscena3();
        System.out.println("Escena 3 - hijos: " + raiz3.hijos.size());

        Nodo raiz4 = ArbolPublicacion.construirArbolEscena4();
        System.out.println("Escena 4 - hijos: " + raiz4.hijos.size() + " (debería ser 5, no 4)");
    }
}