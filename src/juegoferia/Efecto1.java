package juegoferia;

import java.awt.Graphics2D;

/**
 * Un efecto visual animado que se dibuja encima del fondo de un cuadro de cinematica.
 * Es una funcion pura del tiempo: t = segundos desde que empezo el cuadro.
 * Se dibuja en el lienzo de diseno 1920x1080 (ya con la camara aplicada).
 */
@FunctionalInterface
public interface Efecto1 {
    void dibujar(Graphics2D g, double t);
}
