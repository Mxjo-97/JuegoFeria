package juegoferia;

/**
 * El reloj del dia: corre de 7:00 a 19:00, se detiene cuando alguien habla o hay una decision
 * (Lugar llama pausar/reanudar) y lleva la hora de llegada de cada lugar.
 *
 * Si el grupo se demora (por ejemplo peleando), llega tarde al lugar siguiente y pierde esos
 * minutos: retraso() lo dice y el guion decide que se pierde (menos tiempo para el rumor, etc.).
 */
public class Reloj {

    public static final int INICIO = 7 * 60;     // 7:00
    public static final int FIN = 19 * 60;       // 19:00

    /** Cuantos segundos REALES dura un minuto del juego. Ajustar para que el dia no sea eterno. */
    public static double SEGUNDOS_POR_MINUTO = 1.0;

    private double minutos = INICIO;
    private boolean pausado;
    private int retraso;

    public void reiniciar() {
        minutos = INICIO;
        pausado = false;
        retraso = 0;
    }

    /** Lo llama el lugar en cada cuadro. */
    public void avanzar(double segundosReales) {
        if (!pausado && minutos < FIN) {
            minutos = Math.min(FIN, minutos + segundosReales / SEGUNDOS_POR_MINUTO);
        }
    }

    public void pausar() {
        pausado = true;
    }

    public void reanudar() {
        pausado = false;
    }

    public boolean pausado() {
        return pausado;
    }

    /** Suma minutos de golpe (el camino entre dos lugares, por ejemplo). */
    public void sumarMinutos(int m) {
        minutos = Math.min(FIN, minutos + m);
    }

    /**
     * Llegar a un lugar que tiene hora fija ("1.3 Parque y malecon (11:00)").
     * Si se llega antes, el reloj salta a esa hora. Si se llega despues, queda anotado el retraso.
     */
    public void llegar(int horaProgramada) {
        if (minutos <= horaProgramada) {
            minutos = horaProgramada;
            retraso = 0;
        } else {
            retraso = (int) (minutos - horaProgramada);
        }
    }

    /** Minutos de retraso con que se llego al ultimo lugar. */
    public int retraso() {
        return retraso;
    }

    public int minutos() {
        return (int) minutos;
    }

    public boolean terminoElDia() {
        return minutos >= FIN;
    }

    /** "07:10". */
    public String texto() {
        int m = (int) minutos;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    /** Atajo para escribir horas en el guion: Reloj.h(11, 0). */
    public static int h(int hora, int minuto) {
        return hora * 60 + minuto;
    }
}
