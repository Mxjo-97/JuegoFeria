package juegoferia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Medidores PERSONALES de un jugador (cada Jugador tiene el suyo).
 * Los medidores COMPARTIDOS (animo de la casa, confianza entre roles, desinformacion
 * de cada lugar) viven en Partida.
 *
 * ESCALA: el guion suma de a 1, 2 o 3, asi que los medidores van de 0 a 20.
 * Si el equipo prefiere otra escala, se cambia MAXIMO y los valores iniciales aqui y listo.
 */
public class EstadoJuego {

    public static final String INFO_VERIFICADA = "info_verificada";
    public static final String PARTICIPACION = "participacion";
    public static final String CONFIANZA = "confianza";
    public static final String BIENESTAR = "bienestar";
    public static final String ALCANCE = "alcance";
    public static final String CONVIVENCIA = "convivencia";

    /** Orden en que se muestran. */
    public static final String[] TODOS = {INFO_VERIFICADA, PARTICIPACION, CONFIANZA, BIENESTAR, ALCANCE, CONVIVENCIA};

    public static final int MINIMO = 0;
    public static final int MAXIMO = 20;

    private final Map<String, Integer> indicadores = new LinkedHashMap<>();
    private final List<String> historial = new ArrayList<>();
    private final Random azar;

    public EstadoJuego() {
        this(Sincronia.nuevoRandom());
    }

    public EstadoJuego(Random azar) {
        this.azar = azar;
        reiniciar();
    }

    public final void reiniciar() {
        indicadores.clear();
        indicadores.put(INFO_VERIFICADA, 0);
        indicadores.put(PARTICIPACION, 0);
        indicadores.put(CONFIANZA, 5);
        indicadores.put(BIENESTAR, 10);
        indicadores.put(ALCANCE, 5);
        indicadores.put(CONVIVENCIA, 5);
        historial.clear();
    }

    public static boolean esPersonal(String indicador) {
        for (String s : TODOS) {
            if (s.equals(indicador)) {
                return true;
            }
        }
        return false;
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

    public void aplicarEfecto(Efecto efecto) {
        aplicarEfecto(efecto, "sin etiqueta");
    }

    /** Suma los cambios PERSONALES del efecto. Los compartidos los ignora (los aplica Partida). */
    public void aplicarEfecto(Efecto efecto, String etiqueta) {
        if (efecto == null) {
            return;
        }
        Map<String, Integer> cambios = new LinkedHashMap<>();
        resolver(efecto, cambios);

        StringBuilder resumen = new StringBuilder();
        for (Map.Entry<String, Integer> c : cambios.entrySet()) {
            if (!indicadores.containsKey(c.getKey())) {
                continue;
            }
            int nuevo = Math.max(MINIMO, Math.min(MAXIMO, indicadores.get(c.getKey()) + c.getValue()));
            indicadores.put(c.getKey(), nuevo);
            if (resumen.length() > 0) {
                resumen.append(", ");
            }
            resumen.append(c.getKey()).append(c.getValue() >= 0 ? " +" : " ").append(c.getValue());
        }
        historial.add(etiqueta + ": " + (resumen.length() == 0 ? "sin cambios" : resumen));
    }

    /** Junta los cambios fijos y, si el efecto tiene alternativas, elige UNA al azar. */
    private void resolver(Efecto efecto, Map<String, Integer> cambios) {
        for (Map.Entry<String, Integer> e : efecto.getCambios().entrySet()) {
            cambios.merge(e.getKey(), e.getValue(), Integer::sum);
        }
        List<Efecto> alternativas = efecto.getAlternativas();
        if (!alternativas.isEmpty()) {
            resolver(alternativas.get(azar.nextInt(alternativas.size())), cambios);
        }
    }

    public static String nombreBonito(String indicador) {
        switch (indicador) {
            case INFO_VERIFICADA: return "Info verificada";
            case PARTICIPACION: return "Participación";
            case CONFIANZA: return "Confianza";
            case BIENESTAR: return "Bienestar";
            case ALCANCE: return "Alcance";
            case CONVIVENCIA: return "Convivencia";
            case Partida.ANIMO: return "Ánimo de la casa";
            case Partida.CONFIANZA_ROLES: return "Confianza entre roles";
            case Partida.DESINFO_LUGAR: return "Desinformación del lugar";
            default: return indicador;
        }
    }

    public void imprimir(String quien) {
        System.out.println("---- " + quien + " ----");
        for (Map.Entry<String, Integer> e : indicadores.entrySet()) {
            System.out.println(String.format("  %-18s %3d", nombreBonito(e.getKey()) + ":", e.getValue()));
        }
    }
}
