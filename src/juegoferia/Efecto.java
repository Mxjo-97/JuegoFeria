package juegoferia;

import java.util.*;
public class Efecto {
private Map<String, Integer> cambios;
    public Efecto() {
        this.cambios = new HashMap<>();
    }
    
    public void agregar(String indicador, int valor) {
        cambios.put(indicador, valor);
    }
    
    public Map<String, Integer> getCambios() {
        return cambios;
    }
}