package juegoferia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ESPORADICO (aparece si alguien se tomo un TINTO): "Las mariposas escriben".
 * Mariposas amarillas vuelan con una letra cada una; hay que tocarlas en orden para escribir
 * la palabra de la ronda. Sin temporizador. Tres rondas.
 *
 * ARTE: si existe /imagenes/minijuegos/mariposas/ronda_1.png (2, 3) se usa como fondo de cada ronda,
 * y /imagenes/minijuegos/mariposas/mariposa.png como sprite de cada mariposa.
 * (Aqui encaja el visual de mariposas que ya esta hecho: una imagen por ronda.)
 */
public class MjMariposasEscriben extends Minijuego {

    private static final String[] PALABRAS = {"PREGUNTA", "VERIFICA", "RESPETA"};
    private static final String SPRITE = "/imagenes/minijuegos/mariposas/mariposa.png";

    private static final class Mariposa {

        char letra;
        double x, y, fase, vel;
        boolean tomada;
    }

    private final List<Mariposa> mariposas = new ArrayList<>();
    private final Random azar = Sincronia.nuevoRandom();
    private int ronda, siguiente, errores;

    @Override
    public String nombre() {
        return "Las mariposas escriben";
    }

    @Override
    public String instrucciones() {
        return "Con el tinto en la mano, ves mariposas amarillas con letras. Tócalas en orden para escribir "
                + "la palabra de cada ronda. No hay prisa.";
    }

    @Override
    protected void prepararPartida() {
        ronda = 0;
        errores = 0;
        nuevaRonda();
    }

    private void nuevaRonda() {
        mariposas.clear();
        siguiente = 0;
        // cada mariposa en una casilla distinta de una cuadricula de 5x3 (asi no se tapan)
        java.util.List<Integer> casillas = new java.util.ArrayList<>();
        for (int k = 0; k < 15; k++) {
            casillas.add(k);
        }
        java.util.Collections.shuffle(casillas, azar);
        int idx = 0;
        for (char c : PALABRAS[ronda].toCharArray()) {
            Mariposa m = new Mariposa();
            m.letra = c;
            int cas = casillas.get(idx++);
            m.x = 260 + (cas % 5) * 350 + azar.nextInt(80);
            m.y = 320 + (cas / 5) * 230 + azar.nextInt(60);
            m.fase = azar.nextDouble() * 6;
            m.vel = 0.5 + azar.nextDouble();
            mariposas.add(m);
        }
    }

    @Override
    protected void actualizarJuego(double dt) {
        for (Mariposa m : mariposas) {
            m.x += Math.cos(tiempo * m.vel + m.fase) * 40 * dt;
            m.y += Math.sin(tiempo * m.vel * 1.3 + m.fase) * 30 * dt;
        }
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        String palabra = PALABRAS[ronda];
        for (Mariposa m : mariposas) {
            if (!m.tomada && Math.hypot(m.x - p.x, m.y - p.y) < 60) {
                if (m.letra == palabra.charAt(siguiente)) {
                    m.tomada = true;
                    siguiente++;
                    if (siguiente == palabra.length()) {
                        ronda++;
                        if (ronda == PALABRAS.length) {
                            int nv = errores == 0 ? BIEN : errores <= 3 ? REGULAR : MAL;
                            terminarJuego(true, nv, Efecto.de(EstadoJuego.BIENESTAR, nv == BIEN ? 2 : 1),
                                    "Las mariposas escribieron: PREGUNTA, VERIFICA, RESPETA.");
                        } else {
                            nuevaRonda();
                        }
                    }
                } else {
                    errores++;
                }
                return;
            }
        }
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        String f = "/imagenes/minijuegos/mariposas/ronda_" + (Math.min(ronda, 2) + 1) + ".png";
        fondo(g, f);
        cabecera(g, nombre(), null);
        String palabra = PALABRAS[Math.min(ronda, PALABRAS.length - 1)];
        g.setFont(Estilo.fuente(Font.BOLD, 64f));
        g.setColor(Estilo.TEXTO);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < palabra.length(); i++) {
            sb.append(i < siguiente ? palabra.charAt(i) : '_').append(' ');
        }
        centrado(g, sb.toString(), W / 2, 190);
        boolean conSprite = Recursos.existe(SPRITE);
        for (Mariposa m : mariposas) {
            if (m.tomada) {
                continue;
            }
            double ala = 0.4 + 0.6 * Math.abs(Math.sin(tiempo * 9 + m.fase));
            if (conSprite) {
                g.drawImage(Recursos.imagen(SPRITE, 120, 120), (int) m.x - 60, (int) m.y - 60, 120, 120, null);
            } else {
                g.setColor(new Color(0xFFD35C));
                g.fill(new Ellipse2D.Double(m.x - 55 * ala, m.y - 40, 55 * ala, 80));
                g.fill(new Ellipse2D.Double(m.x, m.y - 40, 55 * ala, 80));
            }
            g.setColor(Estilo.MADERA_OSCURA);
            g.setFont(Estilo.fuente(Font.BOLD, 40f));
            centrado(g, String.valueOf(m.letra), (int) m.x, (int) m.y + 14);
        }
    }
}
