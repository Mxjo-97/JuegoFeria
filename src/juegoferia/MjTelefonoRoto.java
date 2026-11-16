package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * ESPORADICO (aparece si alguien DIFUNDIO): "Teléfono roto".
 *
 * El rumor sale de un vecino y corre por el barrio (GrafoRelaciones). El jugador tiene DOS
 * aclaraciones: escoge a que dos vecinos contarle la verdad primero. Luego se simulan los dos
 * mensajes a la vez con un BFS de varias fuentes: cada vecino se queda con lo que le llega primero
 * (si le llegan los dos en el mismo paso, gana la verdad).
 *
 * Es BFS puro: sirve para mostrarle a la profe como se usa el grafo en el juego.
 */
public class MjTelefonoRoto extends Minijuego {

    private final GrafoRelaciones barrio;
    private final String origen;
    private final List<String> nodos = new ArrayList<>();
    private final Map<String, Point> pos = new HashMap<>();
    private final Set<String> elegidos = new HashSet<>();
    /** Cuantos vecinos entran al juego (los mas cercanos al origen, por BFS). */
    private static final int MAX_VECINOS = 12;
    private Map<String, Integer> resultado;   // 1 = verdad, 2 = rumor
    private double tAnim;
    private final Rectangle soltar = new Rectangle(1500, 940, 360, 90);

    public MjTelefonoRoto(GrafoRelaciones barrio, String origen) {
        this.barrio = barrio;
        this.origen = origen;
    }

    @Override
    public String nombre() {
        return "Teléfono roto";
    }

    @Override
    public String instrucciones() {
        return "El rumor ya salió de " + origen + ". Tienes dos aclaraciones: toca a los dos vecinos a quienes "
                + "les contarías la verdad primero. Pista: los que más gente conocen llegan más lejos.";
    }

    @Override
    protected void prepararPartida() {
        nodos.clear();
        // solo el vecindario del origen: los primeros MAX_VECINOS del recorrido BFS
        for (String v : barrio.recorridoBFS(origen)) {
            if (nodos.size() < MAX_VECINOS) {
                nodos.add(v);
            }
        }
        pos.clear();
        int n = nodos.size();
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n - Math.PI / 2;
            pos.put(nodos.get(i), new Point((int) (960 + Math.cos(a) * 700), (int) (560 + Math.sin(a) * 340)));
        }
        elegidos.clear();
        resultado = null;
        tAnim = 0;
    }

    /** BFS de varias fuentes: verdad (desde los elegidos) contra rumor (desde el origen). */
    private Map<String, Integer> simular() {
        Map<String, Integer> quien = new HashMap<>();
        Map<String, Integer> paso = new HashMap<>();
        Queue<String> cola = new LinkedList<>();
        for (String e : elegidos) {
            quien.put(e, 1);
            paso.put(e, 0);
            cola.add(e);
        }
        if (!quien.containsKey(origen)) {
            quien.put(origen, 2);
            paso.put(origen, 0);
            cola.add(origen);
        }
        while (!cola.isEmpty()) {
            String u = cola.poll();
            for (String v : barrio.vecinos(u)) {
                if (!nodos.contains(v)) {
                    continue;
                }
                int pv = paso.get(u) + 1;
                if (!quien.containsKey(v)) {
                    quien.put(v, quien.get(u));
                    paso.put(v, pv);
                    cola.add(v);
                } else if (paso.get(v) == pv && quien.get(u) == 1) {
                    quien.put(v, 1);   // empate: gana la verdad
                }
            }
        }
        return quien;
    }

    @Override
    protected void actualizarJuego(double dt) {
        if (resultado != null) {
            tAnim += dt;
            if (tAnim > 2.5) {
                int verdad = 0;
                for (int v : resultado.values()) {
                    if (v == 1) {
                        verdad++;
                    }
                }
                double f = verdad / (double) nodos.size();
                int nv = f >= 0.7 ? BIEN : f >= 0.4 ? REGULAR : MAL;
                terminarJuego(nv != MAL, nv,
                        nv == BIEN ? Efecto.de(Partida.DESINFO_LUGAR, -5, EstadoJuego.INFO_VERIFICADA, 1)
                                : nv == MAL ? Efecto.de(Partida.DESINFO_LUGAR, 3) : new Efecto(),
                        "La verdad le llegó primero a " + verdad + " de " + nodos.size() + " vecinos.");
            }
        }
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        if (resultado != null) {
            return;
        }
        if (soltar.contains(p) && elegidos.size() == 2) {
            resultado = simular();
            return;
        }
        for (String n : nodos) {
            if (n.equals(origen)) {
                continue;
            }
            if (pos.get(n).distance(p) < 50) {
                if (!elegidos.remove(n) && elegidos.size() < 2) {
                    elegidos.add(n);
                }
                return;
            }
        }
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        fondo(g, "/imagenes/minijuegos/telefono_roto.png");
        cabecera(g, nombre(), null);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(90, 60, 30, 120));
        for (String a : nodos) {
            for (String b : barrio.vecinos(a)) {
                if (nodos.contains(b) && a.compareTo(b) < 0) {
                    Point pa = pos.get(a), pb = pos.get(b);
                    g.drawLine(pa.x, pa.y, pb.x, pb.y);
                }
            }
        }
        g.setFont(Estilo.fuente(Font.BOLD, 20f));
        for (String n : nodos) {
            Point p = pos.get(n);
            Color c = Estilo.PERGAMINO;
            if (resultado != null && tAnim > 0.5) {
                c = resultado.getOrDefault(n, 0) == 1 ? new Color(0x8BE07A) : new Color(0xFF8A7A);
            } else if (n.equals(origen)) {
                c = new Color(0xFF8A7A);
            } else if (elegidos.contains(n)) {
                c = Estilo.DORADO;
            }
            g.setColor(c);
            g.fillOval(p.x - 44, p.y - 44, 88, 88);
            g.setColor(Estilo.MADERA_OSCURA);
            g.drawOval(p.x - 44, p.y - 44, 88, 88);
            centrado(g, n, p.x, p.y + 7);
            g.setColor(Estilo.TEXTO_SUAVE);
            centrado(g, barrio.grado(n) + " contactos", p.x, p.y + 66);
        }
        boton(g, soltar, "Contar la verdad", elegidos.size() == 2 && resultado == null);
    }
}
