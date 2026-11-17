package juegoferia;

import java.awt.CardLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JPanel;

/**
 * La parte JUGABLE (reemplaza a JuegoInfluencer): muestra un lugar a la vez, viaja por el mapa,
 * lanza minijuegos y cinematicas, y pasa de un dia al otro.
 *
 *   JuegoMacondo juego = new JuegoMacondo(partida, () -> volverAlMenu());
 *   juego.empezar();   // prologo -> Dia 1 -> Dia 2 -> final
 */
public class JuegoMacondo extends JPanel implements Lugar.Anfitrion {

    private final CardLayout cartas = new CardLayout();
    private final Partida partida;
    private final Runnable alTerminarPartida;
    private final Map<String, Lugar> lugares = new HashMap<>();
    private Lugar actual;
    /** La pantalla que se esta viendo (lugar, minijuego o cinematica): Sincronia le da el tiempo y las teclas. */
    private PanelDiseno visible;

    public JuegoMacondo(Partida partida, Runnable alTerminarPartida) {
        this.partida = partida;
        this.alTerminarPartida = alTerminarPartida;
        setLayout(cartas);
        setBackground(Color.BLACK);
    }

    public Partida partida() {
        return partida;
    }

    /** La pantalla que esta a la vista ahora mismo. */
    public PanelDiseno panelActivo() {
        return visible;
    }

    private void mostrar(String clave, PanelDiseno p) {
        visible = p;
        cartas.show(this, clave);
    }

    /**
     * Resumen del estado para comparar entre computadoras en red: si dos huellas difieren,
     * el juego se desincronizo. Incluye lo importante (dia, hora, medidores, posiciones).
     */
    public String huella() {
        StringBuilder sb = new StringBuilder();
        sb.append(partida.dia).append('|').append(partida.reloj.texto()).append('|').append(partida.animo)
                .append('|').append(partida.confianza.promedio()).append('|');
        for (Jugador j : partida.jugadores()) {
            sb.append(j.rol.name()).append(j.estado.getIndicadores()).append(j.mariposas).append(j.piezas().size()).append(';');
        }
        sb.append('|').append(visible == null ? "-" : visible.getClass().getSimpleName());
        if (actual != null && visible == actual) {
            sb.append('|').append(actual.huellaLugar());
        }
        return sb.toString();
    }

    /** Prologo (la playa, la casa, tres semanas despues) y luego el Dia 1. */
    public void empezar() {
        cinematica(GuionPrologo.llegada(partida), () -> {
            partida.reloj.reiniciar();
            ir(GuionDia1.PRIMER_LUGAR);
        });
    }

    @Override
    public void ir(String destino) {
        String clave = partida.dia + ":" + destino;
        Lugar l = lugares.get(clave);
        if (l == null) {
            l = partida.dia == 1 ? GuionDia1.crear(destino, partida, this) : GuionDia2.crear(destino, partida, this);
            lugares.put(clave, l);
            add(l, clave);
            revalidate();
        }
        actual = l;
        mostrar(clave, l);
        l.entrar();
        l.requestFocusInWindow();
        System.out.println("[Juego] Día " + partida.dia + " · " + partida.reloj.texto() + " · " + l.titulo
                + (partida.reloj.retraso() > 0 ? " (llegaron " + partida.reloj.retraso() + " min tarde)" : ""));
    }

    @Override
    public void minijuego(Minijuego mj, Consumer<Minijuego.Resultado> fin) {
        final Lugar volver = actual;
        add(mj, "mj");
        revalidate();
        mostrar("mj", mj);
        mj.iniciar(res -> {
            remove(mj);
            revalidate();
            repaint();
            if (volver != null) {
                mostrar(partida.dia + ":" + volver.id, volver);
                volver.requestFocusInWindow();
            }
            fin.accept(res);
        });
        mj.requestFocusInWindow();
    }

    @Override
    public void cinematica(Cinematica c, Runnable fin) {
        final Lugar volver = actual;
        Musica.parar();   // la cinematica trae su propia musica
        add(c, "cine");
        revalidate();
        mostrar("cine", c);
        c.iniciar(() -> {
            remove(c);
            revalidate();
            repaint();
            if (volver != null) {
                mostrar(partida.dia + ":" + volver.id, volver);
                volver.requestFocusInWindow();
                Musica.ambienteJuego(partida.hayNiebla(volver.id));
            }
            if (fin != null) {
                fin.run();
            }
        });
        c.requestFocusInWindow();
    }

    @Override
    public void terminarDia() {
        partida.imprimir();
        if (partida.dia < Partida.DIAS) {
            partida.avanzarDia();
            actual = null;
            ir(GuionDia2.PRIMER_LUGAR);
        } else {
            actual = null;
            cinematica(GuionDia2.finalDelJuego(partida), alTerminarPartida);
        }
    }
}
