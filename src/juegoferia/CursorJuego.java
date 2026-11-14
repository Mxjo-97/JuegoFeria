package juegoferia;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Cursor dibujado por el juego (sin limite de tamano). Vive en el glass pane de la ventana
 * y deja pasar todos los clics a lo que haya debajo.
 */
public final class CursorJuego extends JComponent {

    /** Alto del cursor en pixeles. Sube o baja este numero a tu gusto. */
    private static final int ALTO = 56;

    /** Punto exacto del clic como fraccion de la imagen: {x, y} de 0.0 a 1.0. */
    private static final double[] PUNTA_NORMAL = {0.10, 0.08};
    private static final double[] PUNTA_MANO = {0.35, 0.08};

    private final JFrame frame;
    private final BufferedImage imgNormal;
    private final BufferedImage imgMano;
    private Point pos;
    private boolean mano;

    public static void instalar(JFrame frame) {
        new CursorJuego(frame);
    }

    private CursorJuego(JFrame frame) {
        this.frame = frame;
        imgNormal = Cursores.cargar("/imagenes/cursores/normal.png", ALTO);
        imgMano = Cursores.cargar("/imagenes/cursores/mano.png", ALTO);

        setOpaque(false);
        frame.setGlassPane(this);
        setVisible(true);

        Toolkit.getDefaultToolkit().addAWTEventListener(this::alEvento,
                AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
    }

    /** No intercepta el mouse: todo pasa a los componentes de abajo. */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    private void alEvento(AWTEvent ev) {
        if (!(ev instanceof MouseEvent)) {
            return;
        }
        MouseEvent me = (MouseEvent) ev;
        if (!(me.getSource() instanceof Component)) {
            return;
        }
        Component src = (Component) me.getSource();
        if (SwingUtilities.getWindowAncestor(src) != frame && src != frame) {
            return;
        }

        Point nuevo = SwingUtilities.convertPoint(src, me.getPoint(), this);
        Cursor c = src.getCursor();
        boolean esMano = (c == Cursores.MANO) || (c != null && c.getType() == Cursor.HAND_CURSOR);

        Point anterior = pos;
        pos = nuevo;
        mano = esMano;

        if (anterior != null) {
            repaint(zona(anterior));
        }
        repaint(zona(nuevo));
    }

    private java.awt.Rectangle zona(Point p) {
        return new java.awt.Rectangle(p.x - ALTO, p.y - ALTO, ALTO * 3, ALTO * 3);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (pos == null) {
            return;
        }
        BufferedImage img = mano ? imgMano : imgNormal;
        if (img == null) {
            return;
        }
        double[] punta = mano ? PUNTA_MANO : PUNTA_NORMAL;
        int x = pos.x - (int) (img.getWidth() * punta[0]);
        int y = pos.y - (int) (img.getHeight() * punta[1]);

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(img, x, y, null);
        g.dispose();
    }
}