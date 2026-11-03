package juegoferia;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static java.lang.Boolean.TRUE;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Pantalla de menu hecha de una imagen de fondo + zonas donde se hace clic.
 * Sirve para inicio, como jugar, roles y nombre. Las zonas se escriben en la
 * resolucion de diseno 1591 x 989 y se escalan solas.
 */
public class PantallaImagen extends JPanel {

    /** Dev: poner en true para ver las zonas de clic (rojo) y poder acomodarlas. */
    public static boolean MOSTRAR_ZONAS = TRUE;

    private static class Zona {
        final Rectangle r;
        final Runnable accion;

        Zona(Rectangle r, Runnable accion) {
            this.r = r;
            this.accion = accion;
        }
    }

    private final String rutaFondo;
    private final List<Zona> zonas = new ArrayList<>();
    private JTextField campo;
    private Rectangle campoBase;

    public PantallaImagen(String rutaFondo) {
        this.rutaFondo = rutaFondo;
        setLayout(null);

        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Zona z = zonaEn(e.getPoint());
                if (z != null) {
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    z.accion.run();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(zonaEn(e.getPoint()) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarCampo();
            }
        });
    }

    /** Agrega una zona de clic (coordenadas de diseno). */
    public void zona(int x, int y, int w, int h, Runnable accion) {
        zonas.add(new Zona(new Rectangle(x, y, w, h), accion));
    }

    /** Agrega un campo de texto transparente encima de la imagen. */
    public JTextField campoTexto(int x, int y, int w, int h) {
        campo = new JTextField();
        campo.setOpaque(false);
        campo.setBorder(null);
        campo.setBackground(new Color(0, 0, 0, 0));
        Color cafe = new Color(80, 60, 40);
        campo.setForeground(cafe);
        campo.setCaretColor(cafe);
        campoBase = new Rectangle(x, y, w, h);
        add(campo);
        ajustarCampo();
        return campo;
    }

    private void ajustarCampo() {
        if (campo == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double sx = getWidth() / (double) Recursos.BASE_W;
        double sy = getHeight() / (double) Recursos.BASE_H;
        campo.setBounds((int) (campoBase.x * sx), (int) (campoBase.y * sy),
                (int) (campoBase.width * sx), (int) (campoBase.height * sy));
        campo.setFont(new Font("SansSerif", Font.PLAIN, Math.max(14, (int) (30 * sy))));
    }

    private Zona zonaEn(Point p) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        Point b = new Point((int) (p.x * (double) Recursos.BASE_W / getWidth()),
                (int) (p.y * (double) Recursos.BASE_H / getHeight()));
        for (Zona z : zonas) {
            if (z.r.contains(b)) {
                return z;
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(Recursos.fondo(rutaFondo), 0, 0, getWidth(), getHeight(), null);
        if (MOSTRAR_ZONAS) {
            double sx = getWidth() / (double) Recursos.BASE_W;
            double sy = getHeight() / (double) Recursos.BASE_H;
            g.setColor(Color.RED);
            for (Zona z : zonas) {
                g.drawRect((int) (z.r.x * sx), (int) (z.r.y * sy), (int) (z.r.width * sx), (int) (z.r.height * sy));
            }
        }
        g.dispose();
    }
}
