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
    }
}