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

    /** Fondo oficial del lugar (lista de Rutas); si no es null, gana sobre rutaFondo(). */
    protected String[] fondoLugar() {
        return null;
    }

    /**
     * Dibuja una tarjeta en su puesto (las subclases la cambian: papel con fecha, pedazo de pergamino...).
     * puesto empieza en 1. elegida = levantada; fija = la de la pista.
     */
    protected void tarjeta(Graphics2D g, int puesto, Rectangle r, String texto, boolean elegida, boolean fija) {
        int off = elegida ? -8 : 0;
        Rectangle rr = new Rectangle(r.x, r.y + off, r.width, r.height);
        ArteMj.sticker(g, ArteMj.redondo(rr.x, rr.y, rr.width, rr.height, 24),
                elegida ? ArteMj.AMARILLO_CLARO : fija ? ArteMj.VERDE_CLARO : ArteMj.CREMA, 5f);
        ArteMj.numero(g, puesto, rr.x + 52, rr.getCenterY(), rr.height * 0.82, elegida);
        g.setFont(Estilo.fuente(Font.PLAIN, 30f));
        ArteMj.textoEnCaja(g, texto, new Rectangle(rr.x + 110, rr.y + 4, rr.width - 150, rr.height - 8), ArteMj.TEXTO);
    }

    /** Adornos propios (iconos de los recursos) despues de las tarjetas. */
    protected void adornos(Graphics2D g) {
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
        if (fondoLugar() != null) {
            ArteMj.fondoOficial(g, fondoLugar(), 100);
        } else {
            fondo(g, rutaFondo());
        }
        String[] c = consejos();
        cabecera(g, nombre(), c.length == 0 ? null : c[consejo % c.length]);
        String[] t = tarjetasEnOrden();
        for (int i = 0; i < orden.size(); i++) {
            tarjeta(g, i + 1, rect(i), t[orden.get(i)], i == elegida, i < fijasAlInicio());
        }
        adornos(g);
        boton(g, listo, "Listo", true);
        ArteMj.opcion(g, pedirConsejo, "Otro consejo", false);
    }
}
