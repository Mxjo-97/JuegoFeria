package juegoferia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


 // Cambios que una decisión produce en las variables de la partida.
 
 
public class Efecto {

    private final Map<String, Integer> cambios = new LinkedHashMap<>();
    private final List<Efecto> alternativas = new ArrayList<>();

    public Efecto() {
    }

    /**
     * Atajo para escribir efectos como en el guion:
     *   Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, Partida.DESINFO_LUGAR, -5, Partida.ANIMO, 1)
     * Las claves personales van al jugador; las compartidas (Partida.ANIMO, ...) a la partida.
     */
    public static Efecto de(Object... pares) {
        Efecto e = new Efecto();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            e.agregar((String) pares[i], (Integer) pares[i + 1]);
        }
        return e;
    }

    // Devuelve this para poder encadenar: new Efecto().agregar("a", 1).agregar("b", 2)
    public Efecto agregar(String indicador, int valor) {
        cambios.put(indicador, valor);
        return this;
    }

    public Efecto agregarAlternativa(Efecto alternativa) {
        alternativas.add(alternativa);
        return this;
    }

    public Map<String, Integer> getCambios() {
        return cambios;
    }

    public List<Efecto> getAlternativas() {
        return alternativas;
    }
}
