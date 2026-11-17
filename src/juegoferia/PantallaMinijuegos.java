package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * La pantalla de MINIJUEGOS (modo prueba): tarjetas tipo sticker por lugar y dia, pestañas para
 * filtrar y Ringo, el capibara, que cuenta de que se trata cada uno. Al elegir uno lo corre
 * y al terminar aplica el resultado al EstadoJuego y vuelve aqui.
 */
public class PantallaMinijuegos extends JPanel {

    private static final int W = 1920, H = 1080;

    private static final class Tarjeta {
        final String titulo, subtitulo;
        final Color color;
        final int indice;
        Rectangle rect = new Rectangle();
        double hover = 0;
        boolean visible = true;
        String tipo = "";
        Tarjeta(String t, String s, Color c, int i) {
            titulo = t; subtitulo = s; color = c; indice = i;
        }
    }

    private final java.awt.CardLayout card = new java.awt.CardLayout();
    private final JPanel contenedor = new JPanel(card);
    private final List<Tarjeta> tarjetas = new ArrayList<>();

    private final Runnable alVolver;
    private final Runnable alTerminarTodo;
    private final EstadoJuego estado;

    private Minijuego actual;
    private final Timer reloj = new Timer(16, e -> repaint());
    private final long inicioNs = System.nanoTime();
    private Point mouse = new Point(-100, -100);
    private int filtro = 0;
    private static final String[] PESTANAS = {"Todos", "Día 1", "Día 2", "Extras"};

    public PantallaMinijuegos(Runnable alVolver, Runnable alTerminarTodo, EstadoJuego estado) {
        this.alVolver = alVolver;
        this.alTerminarTodo = alTerminarTodo;
        this.estado = estado;

        setLayout(card);
        setOpaque(true);
        setBackground(Color.BLACK);

        add(hubPanel(), "hub");
        contenedor.setOpaque(false);
        add(contenedor, "minijuego");
        card.show(this, "hub");

        Color[] colores = {new Color(0x5B3FD8), new Color(0x3F8EDC), new Color(0xE0A030), new Color(0x5BA843),
            new Color(0xD9304F), new Color(0x8A3A5A), new Color(0xC9A020)};
        for (int i = 0; i < Minijuegos.LISTA.length; i++) {
            Minijuegos.Entrada m = Minijuegos.LISTA[i];
            Tarjeta t = new Tarjeta(m.titulo, m.descripcion, colores[i % colores.length], i);
            t.tipo = m.tipo;
            tarjetas.add(t);
        }

        MouseAdapter raton = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (actual != null) return;   // no estamos en el hub
                Point p = aDiseno(e.getPoint());
                if (zonaVolver().contains(p) && SwingUtilities.isLeftMouseButton(e)) {
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    alVolver.run();
                    return;
                }
                for (int k = 0; k < PESTANAS.length; k++) {
                    if (zonaPestana(k).contains(p)) {
                        filtro = k;
                        Ajustes.sfx("/sonidos/click.wav", "/sonidos/tecla.wav");
                        return;
                    }
                }
                for (Tarjeta t : tarjetas) {
                    if (t.visible && t.rect.contains(p)) {
                        lanzar(t.indice);
                        return;
                    }
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                if (actual != null) return;
                mouse = aDiseno(e.getPoint());
                repaint();
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);

        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) reloj.start();
                else reloj.stop();
            }
        });
    }

    // ===================== NAVEGACIÓN =====================

    private void lanzar(int indice) {
        Funciones.reproducirSonido("/sonidos/click.wav");
        actual = Minijuegos.crear(indice);
        if (actual == null) return;

        contenedor.removeAll();
        contenedor.add(actual, "mj");
        card.show(this, "minijuego");
        actual.requestFocusInWindow();

        actual.iniciar(resultado -> {
            estado.aplicarEfecto(resultado.efecto, "Prueba de minijuego");
            estado.imprimir("Prueba: " + resultado.resumen);
            contenedor.removeAll();
            actual = null;
            card.show(this, "hub");
            if (alTerminarTodo != null) alTerminarTodo.run();
        });
    }

    private Rectangle zonaVolver() {
        return new Rectangle(40, 40, 210, 66);
    }

    // ===================== ESCALA =====================

    private Point aDiseno(Point p) {
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double s = Math.min(w / (double) W, h / (double) H);
        double ox = (w - W * s) / 2, oy = (h - H * s) / 2;
        return new Point((int) ((p.x - ox) / s), (int) ((p.y - oy) / s));
    }

    // ===================== DIBUJO DEL HUB =====================

    private JPanel hubPanel() {
        JPanel hub = new JPanel() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                dibujarHub(g, getWidth(), getHeight());
                g.dispose();
            }
        };
        hub.setOpaque(false);
        return hub;
    }

    // ---- datos que se leen del "tipo" de cada minijuego ("Fijo · Día 2 · Panadería · Periodista") ----

    private static String lugarDe(String tipo) {
        for (String l : new String[]{"Panadería", "Malecón", "Junta", "Alcaldía", "Cena", "Apagón"}) {
            if (tipo.contains(l)) {
                return l;
            }
        }
        return "Extra";
    }

    private static int diaDe(String tipo) {
        if (tipo.startsWith("Esporádico")) {
            return 0;
        }
        return tipo.contains("Día 2") ? 2 : 1;
    }

    private static String clase(String tipo) {
        int i = tipo.indexOf(" · ");
        return i > 0 ? tipo.substring(0, i) : tipo;
    }

    private static Rol rolDe(String tipo) {
        for (Rol r : Rol.values()) {
            if (tipo.contains(r.nombre)) {
                return r;
            }
        }
        return null;
    }

    private static Color colorLugar(String lugar) {
        switch (lugar) {
            case "Panadería": return new Color(0xF7C79A);
            case "Malecón": return new Color(0x9FD3EC);
            case "Junta": return new Color(0xB9DC9E);
            case "Alcaldía": return new Color(0xD3C0F0);
            case "Cena": return new Color(0xF6B5C3);
            case "Apagón": return new Color(0xB4B9E6);
            default: return new Color(0xF8DE8C);
        }
    }

    private Rectangle zonaPestana(int k) {
        int w = 150, gap = 14;
        int x0 = 1175 - (PESTANAS.length * w + (PESTANAS.length - 1) * gap) / 2;
        return new Rectangle(x0 + k * (w + gap), 990, w, 52);
    }

    private boolean pasaFiltro(Tarjeta t) {
        int d = diaDe(t.tipo);
        switch (filtro) {
            case 1: return d == 1;
            case 2: return d == 2;
            case 3: return d == 0;
            default: return true;
        }
    }

    private void dibujarHub(Graphics2D g, int w, int h) {
        g.setColor(new Color(0xF6D9C0));
        g.fillRect(0, 0, w, h);
        double s = Math.min(w / (double) W, h / (double) H);
        g.translate((w - W * s) / 2, (h - H * s) / 2);
        g.scale(s, s);
        g.setClip(0, 0, W, H);
        Estilo.suave(g);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double t = (System.nanoTime() - inicioNs) / 1e9;

        Acogedor.fondo(g, "/imagenes/malecon.png", 0.74f);
        Acogedor.petalos(g, t, 26);

        // ---- titulo ----
        Acogedor.cartel(g, 1175, 26, "Minijuegos", "Practica sin jugar todo el día", t);

        // ---- volver ----
        Rectangle rv = zonaVolver();
        Acogedor.pastilla(g, rv.x, rv.y, rv.width, rv.height, "Volver", rv.contains(mouse) ? 1 : 0, Acogedor.ROSA);

        // ---- tarjetas (con el filtro de la pestaña) ----
        int cw = 325, ch = 222, gx = 30, gy = 30, x0 = 480, y0 = 236;
        int k = 0;
        Tarjeta encima = null;
        for (Tarjeta tj : tarjetas) {
            tj.visible = pasaFiltro(tj);
            if (!tj.visible) {
                tj.rect = new Rectangle();
                continue;
            }
            tj.rect = new Rectangle(x0 + (k % 4) * (cw + gx), y0 + (k / 4) * (ch + gy), cw, ch);
            boolean sobre = tj.rect.contains(mouse);
            tj.hover += ((sobre ? 1 : 0) - tj.hover) * 0.2;
            if (sobre) {
                encima = tj;
            }
            k++;
        }
        k = 0;
        for (Tarjeta tj : tarjetas) {
            if (tj.visible) {
                dibujarTarjeta(g, tj, t, k++);
            }
        }

        // ---- pestañas ----
        for (int i = 0; i < PESTANAS.length; i++) {
            Rectangle r = zonaPestana(i);
            boolean on = filtro == i;
            double hv = r.contains(mouse) ? 1 : 0;
            if (on) {
                g.setColor(new Color(60, 30, 10, 60));
                g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 6, r.width, r.height, r.height, r.height));
                g.setPaint(new GradientPaint(0, r.y, Estilo.DORADO_CLARO, 0, r.y + r.height, Estilo.DORADO));
                g.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, r.height, r.height));
                g.setColor(Acogedor.MARRON);
                g.setStroke(new BasicStroke(3.5f));
                g.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, r.height, r.height));
            } else {
                Acogedor.pastilla(g, r.x, r.y + 2, r.width, r.height - 4, "", hv, null);
            }
            g.setFont(Estilo.fuente(Font.BOLD, 21f));
            g.setColor(new Color(0x5A3216));
            Acogedor.centrado(g, PESTANAS[i], r.getCenterX(), r.y + 34 - (on ? 0 : hv * 4));
        }

        // ---- Ringo y su globo ----
        dibujarRingo(g, t, encima);
    }

    private void dibujarRingo(Graphics2D g, double t, Tarjeta encima) {
        double bob = Math.sin(t * 2.0) * 5;
        int rx = 40, rw = 400, rh = 486, ry = H - rh + 14;
        if (Recursos.existe("/imagenes/ringo.png")) {
            // sombrita
            g.setColor(new Color(70, 35, 12, 50));
            g.fill(new java.awt.geom.Ellipse2D.Double(rx + 70, H - 40, rw - 140, 36));
            java.awt.geom.AffineTransform at = g.getTransform();
            g.translate(rx + rw / 2.0, ry + rh);
            g.scale(1 + Math.sin(t * 2.0) * 0.01, 1 - Math.sin(t * 2.0) * 0.012);
            g.drawImage(Recursos.imagen("/imagenes/ringo.png", 560, 680), -rw / 2, -rh + (int) bob, rw, rh, null);
            g.setTransform(at);
        }
        // globo
        int bx = 36, by = 250, bw = 420, bh = 300;
        String titulo, cuerpo, pie;
        if (encima == null) {
            titulo = "¡Hola! Soy Ringo";
            cuerpo = "Escoge un minijuego para practicar. Aquí nada cuenta para la partida, así que equivócate sin miedo.";
            pie = "Pasa el mouse por una tarjeta.";
        } else {
            titulo = encima.titulo;
            cuerpo = encima.subtitulo;
            pie = encima.tipo;
        }
        g.setColor(new Color(70, 35, 12, 55));
        g.fill(new RoundRectangle2D.Double(bx + 3, by + 9, bw, bh, 44, 44));
        g.setColor(Acogedor.CREMA);
        g.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 44, 44));
        java.awt.geom.Path2D cola = new java.awt.geom.Path2D.Double();
        cola.moveTo(bx + 190, by + bh - 2);
        cola.lineTo(bx + 250, by + bh - 2);
        cola.lineTo(bx + 200, by + bh + 46);
        cola.closePath();
        g.fill(cola);
        g.setColor(Acogedor.MARRON);
        g.setStroke(new BasicStroke(4f));
        java.awt.geom.Area forma = new java.awt.geom.Area(new RoundRectangle2D.Double(bx, by, bw, bh, 44, 44));
        forma.add(new java.awt.geom.Area(cola));
        g.draw(forma);
        Acogedor.flor(g, bx + 10, by + 8, 18, Color.WHITE, t * 0.4);
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(new Color(0x5A3216));
        FontMetrics fm = g.getFontMetrics();
        int ty = by + 56;
        for (String l : envolver(titulo, fm, bw - 70)) {
            g.drawString(l, bx + 36, ty);
            ty += fm.getHeight();
        }
        g.setFont(Estilo.fuente(Font.PLAIN, 21f));
        fm = g.getFontMetrics();
        g.setColor(new Color(0x6E4A2A));
        ty += 4;
        int lineas = 0;
        for (String l : envolver(cuerpo, fm, bw - 70)) {
            if (lineas++ >= 5) {
                break;
            }
            g.drawString(l, bx + 36, ty);
            ty += fm.getHeight();
        }
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        g.setColor(Acogedor.MARRON_SUAVE);
        g.drawString(pie.toUpperCase(), bx + 36, by + bh - 24);
    }

    private void dibujarTarjeta(Graphics2D g0, Tarjeta tj, double t, int orden) {
        Graphics2D g = (Graphics2D) g0.create();
        Rectangle r = tj.rect;
        String lugar = lugarDe(tj.tipo);
        Color col = colorLugar(lugar);
        double hv = tj.hover;
        double lift = hv * 10;
        double x = r.x, y = r.y - lift, w = r.width, h = r.height;
        // entrada suave al abrir la pantalla
        double entra = Math.max(0, Math.min(1, (t - orden * 0.04) / 0.5));
        Acogedor.alfa(g, entra);
        y += (1 - entra) * 30;

        if (hv > 0.02) {
            g.setColor(new Color(246, 196, 69, (int) (110 * hv)));
            g.fill(new RoundRectangle2D.Double(x - 14, y - 14, w + 28, h + 28, 52, 52));
        }
        java.awt.Shape cuerpo = Acogedor.sticker(g, x, y, w, h, 40, Acogedor.CREMA);
        // banda de color con puntitos
        java.awt.Shape antes = g.getClip();
        g.clip(cuerpo);
        g.setPaint(new GradientPaint(0, (float) y, col.brighter(), 0, (float) (y + 104), col));
        g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, 104));
        g.setColor(new Color(255, 255, 255, 70));
        for (int i = 0; i < 9; i++) {
            double px = x + 18 + (i * 61) % (w - 30), py = y + 14 + (i * 29) % 72;
            g.fill(new java.awt.geom.Ellipse2D.Double(px, py, 9 + i % 3 * 5, 9 + i % 3 * 5));
        }
        g.setColor(new Color(110, 63, 30, 60));
        g.fill(new java.awt.geom.Rectangle2D.Double(x, y + 102, w, 3));
        g.setClip(antes);
        g.setColor(Acogedor.MARRON);
        g.setStroke(new BasicStroke(4f));
        g.draw(cuerpo);

        // emblema
        double ex = x + w / 2, ey = y + 60 + Math.sin(t * 2 + orden) * 2 * hv;
        g.setColor(new Color(70, 35, 12, 45));
        g.fill(new java.awt.geom.Ellipse2D.Double(ex - 40, ey - 36, 80, 80));
        g.setColor(Color.WHITE);
        g.fill(new java.awt.geom.Ellipse2D.Double(ex - 40, ey - 40, 80, 80));
        g.setColor(Acogedor.MARRON);
        g.setStroke(new BasicStroke(3f));
        g.draw(new java.awt.geom.Ellipse2D.Double(ex - 40, ey - 40, 80, 80));
        icono(g, lugar, ex, ey, col.darker(), t);

        // chips de arriba: dia y clase
        int dia = diaDe(tj.tipo);
        chip(g, x + 14, y + 14, dia == 0 ? "EXTRA" : "DÍA " + dia, new Color(255, 255, 255, 225), new Color(0x5A3216));
        String cl = clase(tj.tipo).toUpperCase();
        g.setFont(Estilo.fuente(Font.BOLD, 13f));
        int cwid = g.getFontMetrics().stringWidth(cl) + 22;
        chip(g, x + w - 14 - cwid, y + 14, cl, new Color(110, 63, 30, 170), Color.WHITE);

        // titulo
        g.setFont(Estilo.fuente(Font.BOLD, 23f));
        FontMetrics fm = g.getFontMetrics();
        List<String> ls = envolver(tj.titulo, fm, (int) w - 40);
        if (ls.size() > 1) {
            g.setFont(Estilo.fuente(Font.BOLD, 20f));
            fm = g.getFontMetrics();
            ls = envolver(tj.titulo, fm, (int) w - 40);
        }
        g.setColor(new Color(0x5A3216));
        double ty = y + (ls.size() > 1 ? 134 : 146);
        for (String l : ls) {
            Acogedor.centrado(g, l, x + w / 2, ty);
            ty += fm.getHeight() - 2;
        }

        // pie: lugar y rol
        Rol rol = rolDe(tj.tipo);
        String pie = (lugar.equals("Extra") ? "Si pasa algo en la partida" : lugar)
                + (rol != null ? " · " + rol.nombre : tj.tipo.contains("Los cuatro") ? " · Los cuatro" : "");
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        fm = g.getFontMetrics();
        double pw = fm.stringWidth(pie) + (rol != null ? 18 : 0);
        double px = x + w / 2 - pw / 2;
        if (rol != null) {
            g.setColor(new Color(rol.color.getRed(), rol.color.getGreen(), rol.color.getBlue(), (int) (255 * (1 - hv))));
            g.fill(new java.awt.geom.Ellipse2D.Double(px, y + h - 32, 11, 11));
            px += 18;
        }
        Graphics2D gp = (Graphics2D) g.create();
        Acogedor.alfa(gp, (1 - hv) * entra);
        gp.setColor(Acogedor.MARRON_SUAVE);
        gp.drawString(pie, (float) px, (float) (y + h - 22));
        gp.dispose();

        // "¡Jugar!" al pasar por encima
        if (hv > 0.05) {
            Graphics2D gj = (Graphics2D) g.create();
            Acogedor.alfa(gj, hv * entra);
            double jw = 150, jh = 44, jx = x + w / 2 - jw / 2, jy = y + h - jh - 8 - hv * 4;
            gj.setColor(new Color(70, 35, 12, 70));
            gj.fill(new RoundRectangle2D.Double(jx + 2, jy + 6, jw, jh, jh, jh));
            gj.setPaint(new GradientPaint(0, (float) jy, Estilo.DORADO_CLARO, 0, (float) (jy + jh), Estilo.DORADO));
            gj.fill(new RoundRectangle2D.Double(jx, jy, jw, jh, jh, jh));
            gj.setColor(Acogedor.MARRON);
            gj.setStroke(new BasicStroke(3.5f));
            gj.draw(new RoundRectangle2D.Double(jx, jy, jw, jh, jh, jh));
            gj.setFont(Estilo.fuente(Font.BOLD, 21f));
            gj.setColor(new Color(0x5A3216));
            Acogedor.centrado(gj, "¡Jugar!", jx + jw / 2, jy + 30);
            gj.dispose();
        }
        g.dispose();
    }

    private static void chip(Graphics2D g, double x, double y, String t, Color fondo, Color texto) {
        g.setFont(Estilo.fuente(Font.BOLD, 13f));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(t) + 22, h = 26;
        g.setColor(fondo);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(texto);
        g.drawString(t, (float) (x + 11), (float) (y + 18));
    }

    /** Iconito del lugar, dibujado a mano. */
    private static void icono(Graphics2D g, String lugar, double cx, double cy, Color c, double t) {
        g.setColor(c);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (lugar) {
            case "Panadería": {   // pan
                g.fill(new RoundRectangle2D.Double(cx - 26, cy - 14, 52, 30, 28, 28));
                g.setColor(Color.WHITE);
                for (int i = -1; i <= 1; i++) {
                    g.draw(new java.awt.geom.Line2D.Double(cx + i * 13 - 4, cy - 6, cx + i * 13 + 4, cy + 8));
                }
                break;
            }
            case "Malecón": {     // sol y olas
                g.setColor(Acogedor.AMARILLO);
                g.fill(new java.awt.geom.Ellipse2D.Double(cx - 12, cy - 24, 24, 24));
                g.setColor(c);
                for (int k = 0; k < 2; k++) {
                    java.awt.geom.Path2D o = new java.awt.geom.Path2D.Double();
                    double yy = cy + 6 + k * 12;
                    o.moveTo(cx - 26, yy);
                    for (int i = 0; i < 4; i++) {
                        o.quadTo(cx - 26 + i * 13 + 6.5, yy - 7 + Math.sin(t * 2 + i) * 1.5, cx - 26 + (i + 1) * 13, yy);
                    }
                    g.draw(o);
                }
                break;
            }
            case "Junta": {       // casita
                java.awt.geom.Path2D techo = new java.awt.geom.Path2D.Double();
                techo.moveTo(cx - 28, cy - 2);
                techo.lineTo(cx, cy - 26);
                techo.lineTo(cx + 28, cy - 2);
                techo.closePath();
                g.fill(techo);
                g.fill(new java.awt.geom.Rectangle2D.Double(cx - 20, cy - 4, 40, 28));
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(cx - 6, cy + 6, 12, 18, 6, 6));
                break;
            }
            case "Alcaldía": {    // edificio con columnas
                java.awt.geom.Path2D f = new java.awt.geom.Path2D.Double();
                f.moveTo(cx - 28, cy - 10);
                f.lineTo(cx, cy - 26);
                f.lineTo(cx + 28, cy - 10);
                f.closePath();
                g.fill(f);
                for (int i = -2; i <= 2; i++) {
                    g.fill(new java.awt.geom.Rectangle2D.Double(cx + i * 11 - 3, cy - 8, 6, 26));
                }
                g.fill(new java.awt.geom.Rectangle2D.Double(cx - 30, cy + 18, 60, 6));
                break;
            }
            case "Cena": {        // tacita con vapor
                g.fill(new RoundRectangle2D.Double(cx - 20, cy - 6, 34, 28, 12, 12));
                g.draw(new java.awt.geom.Ellipse2D.Double(cx + 8, cy - 1, 14, 14));
                for (int i = 0; i < 2; i++) {
                    java.awt.geom.Path2D v = new java.awt.geom.Path2D.Double();
                    double vx = cx - 10 + i * 12;
                    v.moveTo(vx, cy - 10);
                    v.quadTo(vx - 6 + Math.sin(t * 3 + i) * 2, cy - 18, vx, cy - 26);
                    g.draw(v);
                }
                break;
            }
            case "Apagón": {      // farol
                g.fill(new java.awt.geom.Rectangle2D.Double(cx - 3, cy + 6, 6, 20));
                g.fill(new RoundRectangle2D.Double(cx - 14, cy - 22, 28, 30, 8, 8));
                g.setColor(Acogedor.AMARILLO);
                g.fill(new RoundRectangle2D.Double(cx - 9, cy - 16, 18, 18, 6, 6));
                break;
            }
            default:              // mariposa
                Acogedor.mariposa(g, cx, cy, 2.2, t);
                break;
        }
    }

    private static List<String> envolver(String texto, FontMetrics fm, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) > maxW && actual.length() > 0) {
                out.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        out.add(actual.toString());
        return out;
    }
}