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
