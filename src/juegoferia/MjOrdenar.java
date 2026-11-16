package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Base para los minijuegos de ORDENAR TARJETAS ("La fecha del agua", "Los pergaminos").
 * Las tarjetas salen desordenadas; se hace clic en una y luego en otra para cambiarlas de puesto.
 * Con "Listo" se cuentan las que quedaron en su sitio y el nivel sale de los umbrales.
 *
 * Una subclase solo pone: nombre, instrucciones, las tarjetas EN ORDEN CORRECTO,
 * los umbrales, los consejos de los otros y el resultado de cada nivel.
 */
public abstract class MjOrdenar extends Minijuego {

    /** Las tarjetas en el orden correcto. */
    protected abstract String[] tarjetasEnOrden();

    /** Aciertos para BIEN y para REGULAR. */
    protected abstract int paraBien();

    protected abstract int paraRegular();

    /** Frases de los otros tres ("Empieza por la más vieja"). */
    protected abstract String[] consejos();

    /** Resumen y efecto segun el nivel (BIEN/REGULAR/MAL). */
    protected abstract String resumen(int nivel);

    protected abstract Efecto efecto(int nivel);

    /** Fondo de Canva (opcional). */
    protected String rutaFondo() {
        return null;
    }

    /** Cuantas tarjetas empiezan ya en su sitio (pista por haber verificado antes). */
    protected int fijasAlInicio() {
        return 0;
    }

    private final List<Integer> orden = new ArrayList<>();   // orden[i] = indice correcto de la tarjeta en el puesto i
    private int elegida = -1;
    private int consejo = 0;
    private final Rectangle listo = new Rectangle(1560, 960, 300, 90);
    private final Rectangle pedirConsejo = new Rectangle(1290, 1000, 250, 56);

    @Override
    protected void prepararPartida() {
        orden.clear();
        int n = tarjetasEnOrden().length;
        for (int i = 0; i < n; i++) {
            orden.add(i);
        }
        int fijas = Math.min(fijasAlInicio(), n);
        List<Integer> resto = new ArrayList<>(orden.subList(fijas, n));
        Random r = Sincronia.nuevoRandom();
        do {
            Collections.shuffle(resto, r);
        } while (resto.size() > 1 && enOrden(resto, fijas));
        for (int i = fijas; i < n; i++) {
            orden.set(i, resto.get(i - fijas));
        }
        elegida = -1;
        consejo = 0;
    }

    private static boolean enOrden(List<Integer> l, int desde) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) != i + desde) {
                return false;
            }
        }
        return true;
    }

    private Rectangle rect(int i) {
        int n = orden.size();
        int alto = Math.min(105, 760 / n);
        return new Rectangle(160, 130 + i * (alto + 8), 1600, alto);
    }

    @Override
    protected void actualizarJuego(double dt) {
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        if (listo.contains(p)) {
            revisar();
            return;
        }
        if (pedirConsejo.contains(p)) {
            consejo++;
            return;
        }
        for (int i = 0; i < orden.size(); i++) {
            if (rect(i).contains(p)) {
                if (i < fijasAlInicio()) {
                    return;     // las de la pista no se mueven
                }
                if (elegida < 0) {
                    elegida = i;
                } else if (elegida == i) {
                    elegida = -1;
                } else {
                    Collections.swap(orden, elegida, i);
                    elegida = -1;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                }
                return;
            }
        }
    }

    private void revisar() {
        int aciertos = 0;
        for (int i = 0; i < orden.size(); i++) {
            if (orden.get(i) == i) {
                aciertos++;
            }
        }
        int nv = nivel(aciertos, paraBien(), paraRegular());
        terminarJuego(nv != MAL, nv, efecto(nv), resumen(nv) + " (" + aciertos + " de " + orden.size() + " en su sitio)");
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        fondo(g, rutaFondo());
        String[] c = consejos();
        cabecera(g, nombre(), c.length == 0 ? null : c[consejo % c.length]);
        String[] t = tarjetasEnOrden();
        for (int i = 0; i < orden.size(); i++) {
            Rectangle r = rect(i);
            Estilo.pergamino(g, r.x, r.y, r.width, r.height, 26);
            g.setStroke(new BasicStroke(i == elegida ? 7f : 3f));
            g.setColor(i == elegida ? Estilo.DORADO : i < fijasAlInicio() ? Estilo.HOJA : Estilo.MADERA_OSCURA);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 26, 26);
            g.setFont(Estilo.fuente(Font.BOLD, 30f));
            g.setColor(Estilo.TEXTO_SUAVE);
            g.drawString(String.valueOf(i + 1), r.x + 24, r.y + r.height / 2 + 11);
            g.setFont(Estilo.fuente(Font.PLAIN, 30f));
            g.setColor(Estilo.TEXTO);
            FontMetrics fm = g.getFontMetrics();
            List<String> ls = envolver(t[orden.get(i)], fm, r.width - 120);
            int ty = r.y + r.height / 2 - (ls.size() - 1) * fm.getHeight() / 2 + 10;
            for (String l : ls) {
                g.drawString(l, r.x + 80, ty);
                ty += fm.getHeight();
            }
        }
        boton(g, listo, "Listo", true);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(pedirConsejo.x, pedirConsejo.y, pedirConsejo.width, pedirConsejo.height, 28, 28);
        g.setColor(Color.WHITE);
        g.setFont(Estilo.fuente(Font.BOLD, 24f));
        centrado(g, "Otro consejo", pedirConsejo.x + pedirConsejo.width / 2, pedirConsejo.y + 37);
    }
}
