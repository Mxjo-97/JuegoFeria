/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package juegoferia;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Isabella Sandoval
 */
public class ControladorDePartidas {
    private int diasRestantes;
    private Map<String, Boolean> islasResueltasActual;
    
    public ControladorDePartidas() {
        this.diasRestantes = 7;
        this.islasResueltasActual = new HashMap<>();
        inicializarIslas();
    }
    
    private void inicializarIslas() {
        islasResueltasActual.put("Panaderia", false);
        islasResueltasActual.put("Colegio", false);
        islasResueltasActual.put("Plaza", false);
        islasResueltasActual.put("Parque", false);
        islasResueltasActual.put("Junta_Comunal", false);
    }
    
    public void checkIslaCompletada(String isla) {
        if (islasResueltasActual.containsKey(isla)) {
            islasResueltasActual.put(isla, true);
        }
    }

    public boolean laIslaResuelta(String isla) {
        return islasResueltasActual.getOrDefault(isla, false);
    }
    
    public void avanzarDia() {
        if (diasRestantes > 0) {
            diasRestantes--;
            
            for (String isla : islasResueltasActual.keySet()) {
                islasResueltasActual.put(isla, false);
            }
        }
    }

    public int getDiasRestantes() { 
        return diasRestantes; 
    }
}
