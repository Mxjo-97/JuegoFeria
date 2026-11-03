package juegoferia;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Variables de la partida. Cada hoja del árbol de decisión trae un Efecto;
 * aplicarEfecto() lo suma a los indicadores, respetando los límites
 *
 * Los nombres de los indicadores son los mismos que se usan en ArbolPublicacion.
 */
public class EstadoJuego {

    public static final String CONFIANZA = "confianza";
    public static final String INFO_VERIFICADA = "info_verificada";
    public static final String DESINFORMACION = "desinformacion";
    public static final String CONFLICTOS = "conflictos";
    public static final String CONVIVENCIA = "convivencia";
    public static final String REPUTACION = "reputacion_jugador";

    
    private static final int MINIMO = 0;
    private static final int MAXIMO = 100;
    private static final int INICIAL_CON_BASE = 50;   // confianza, convivencia y reputación
    private static final int INICIAL_EN_CERO = 0;     // info verificada, desinformación, conflictos
    // =================================================================

    private final Map<String, Integer> indicadores = new LinkedHashMap<>();
    private final List<String> historial = new ArrayList<>();
    private final Random azar;

    public EstadoJuego() {
        this(new Random());
    }


    public EstadoJuego(Random azar) {
        this.azar = azar;
        reiniciar();
    }

    public void reiniciar() {
        indicadores.clear();
        indicadores.put(CONFIANZA, INICIAL_CON_BASE);
        indicadores.put(INFO_VERIFICADA, INICIAL_EN_CERO);
        indicadores.put(DESINFORMACION, INICIAL_EN_CERO);
        indicadores.put(CONFLICTOS, INICIAL_EN_CERO);
        indicadores.put(CONVIVENCIA, INICIAL_CON_BASE);
        indicadores.put(REPUTACION, INICIAL_CON_BASE);
        historial.clear();
    }

    public int get(String indicador) {
        Integer valor = indicadores.get(indicador);
        return valor == null ? 0 : valor;
    }

    public Map<String, Integer> getIndicadores() {
        return Collections.unmodifiableMap(indicadores);
    }

    public List<String> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    // es la que ya llama partvisual: estadoJuego.aplicarEfecto(nodoActual.efecto)
    public void aplicarEfecto(Efecto efecto) {
        aplicarEfecto(efecto, "sin etiqueta");
    }

    // deja anotado en el historial a qué decisión corresponde
    public void aplicarEfecto(Efecto efecto, String etiqueta) {
        if (efecto == null) {
            return;
        }

        Map<String, Integer> cambios = new LinkedHashMap<>();
        resolver(efecto, cambios);

        StringBuilder resumen = new StringBuilder();
        for (Map.Entry<String, Integer> cambio : cambios.entrySet()) {
            String indicador = cambio.getKey();
            int valor = cambio.getValue();

            if (!indicadores.containsKey(indicador)) {
                if (!indicador.equals("neutro")) {
                    System.out.println("[EstadoJuego] Indicador desconocido, se ignora: " + indicador);
                }
                continue;
            }

            int nuevo = indicadores.get(indicador) + valor;
            nuevo = Math.max(MINIMO, Math.min(MAXIMO, nuevo));
            indicadores.put(indicador, nuevo);

            if (resumen.length() > 0) {
                resumen.append(", ");
            }
            resumen.append(indicador).append(valor >= 0 ? " +" : " ").append(valor);
        }

        historial.add(etiqueta + ": " + (resumen.length() == 0 ? "sin cambios" : resumen));
    }

    // Junta los cambios fijos y, si el efecto tiene alternativas, elige UNA al azar.
    private void resolver(Efecto efecto, Map<String, Integer> cambios) {
        for (Map.Entry<String, Integer> e : efecto.getCambios().entrySet()) {
            cambios.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        List<Efecto> alternativas = efecto.getAlternativas();
        if (!alternativas.isEmpty()) {
            resolver(alternativas.get(azar.nextInt(alternativas.size())), cambios);
        }
    }

    public void imprimirEstadoPartida() {
        System.out.println("========== ESTADO DE LA PARTIDA ==========");
        for (Map.Entry<String, Integer> e : indicadores.entrySet()) {
            System.out.println(String.format("%-24s %3d", nombreBonito(e.getKey()) + ":", e.getValue()));
        }
        if (!historial.isEmpty()) {
            System.out.println("Última decisión -> " + historial.get(historial.size() - 1));
        }
        System.out.println("==========================================");
    }

    private String nombreBonito(String indicador) {
        switch (indicador) {
            case CONFIANZA: return "Confianza";
            case INFO_VERIFICADA: return "Información verificada";
            case DESINFORMACION: return "Desinformación";
            case CONFLICTOS: return "Conflictos";
            case CONVIVENCIA: return "Convivencia";
            case REPUTACION: return "Reputación del jugador";
            default: return indicador;
        }
    }
}
