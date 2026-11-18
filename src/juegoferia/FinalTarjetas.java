package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pantalla final "Así jugó cada quien": una tarjeta por jugador que entra volando, con su carita,
 * un TITULO segun en que se destaco, sus barritas y sus mariposas. El mejor de la casa lleva corona.
 * Se dibuja como capa de la Cinematica (no necesita imagenes nuevas).
 */
final class FinalTarjetas implements EfectosCine.Capa {

    private static final class Ficha {
        String nombre, rol, carpeta, titulo, emoji;
        Color color;
        int verifico, participo, confianza, bienestar, mariposas, total;
        /** Modo feria: la mision secreta (null si no hubo) y si la cumplio. */
        String mision;
        boolean misionOk;
    }

    private final List<Ficha> fichas = new ArrayList<>();
    /** Con cuanto empieza cada indicador (el titulo sale de lo que se gano, no de lo que ya se tenia). */
    private static final Map<String, Integer> BASE = new EstadoJuego(new java.util.Random(0)).getIndicadores();
    private int mejor = -1;

    FinalTarjetas(Partida p) {
        Rol[] orden = {Rol.CIUDADANO, Rol.INFLUENCER, Rol.PERIODISTA, Rol.CANDIDATO};
        int mejorTotal = Integer.MIN_VALUE;
        for (Rol r : orden) {
            Jugador j = p.jugador(r);
            if (j == null) {
                continue;
            }
            Map<String, Integer> e = j.estado.getIndicadores();
            Ficha f = new Ficha();
            f.nombre = j.nombre;
            f.rol = r.nombre;
            f.carpeta = r.carpeta;
            f.color = r.color;
            f.verifico = e.getOrDefault(EstadoJuego.INFO_VERIFICADA, 0);
            f.participo = e.getOrDefault(EstadoJuego.PARTICIPACION, 0);
            f.confianza = e.getOrDefault(EstadoJuego.CONFIANZA, 0);
            f.bienestar = e.getOrDefault(EstadoJuego.BIENESTAR, 0);
            f.mariposas = j.mariposas;
            f.mision = j.misionNombre;
            f.misionOk = j.tiene("mision_ok");
            f.total = f.verifico + f.participo + f.confianza + e.getOrDefault(EstadoJuego.CONVIVENCIA, 0) + f.mariposas * 2;
            String[][] titulos = {
                {EstadoJuego.INFO_VERIFICADA, "Detective de la verdad"},
                {EstadoJuego.PARTICIPACION, "Alma de la Junta"},
                {EstadoJuego.CONFIANZA, "Corazón del barrio"},
                {EstadoJuego.CONVIVENCIA, "Pegamento de la casa"},
                {EstadoJuego.ALCANCE, "Voz del malecón"},
                {EstadoJuego.BIENESTAR, "Paz y tinto"}
            };
            int top = 0;
            f.titulo = "Recién llegado";
            for (String[] t : titulos) {
                int v = e.getOrDefault(t[0], 0) - BASE.getOrDefault(t[0], 0);   // lo que gano en la partida
                if (v > top) {
                    top = v;
                    f.titulo = t[1];
                }
            }
            if (f.mariposas >= 4 && f.mariposas * 2 >= top) {
                f.titulo = "Imán de mariposas";
            }
            if (f.total > mejorTotal) {
                mejorTotal = f.total;
                mejor = fichas.size();
            }
            fichas.add(f);
        }
    }

    private static final int CW = 370, CH = 560, GAP = 36, Y0 = 150;
    private static final Color CREMA = new Color(0xFFFAF0);
    private static final Color TEXTO = new Color(0x5A3B1F);
    private static final Color SUAVE = new Color(0xA88B6A);

    @Override
    public void pintar(Graphics2D g0, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // velo para que las tarjetas resalten
        g.setColor(new Color(30, 18, 10, (int) (150 * Math.min(1, t / 0.6))));
        g.fillRect(0, 0, PanelDiseno.W, PanelDiseno.H);
        int n = fichas.size();
        int x0 = (PanelDiseno.W - (n * CW + (n - 1) * GAP)) / 2;
        for (int i = 0; i < n; i++) {
            double ti = t - 0.3 - i * 0.28;
            if (ti <= 0) {
                continue;
            }
            tarjeta(g, fichas.get(i), x0 + i * (CW + GAP), Y0, ti, i == mejor, i);
        }
        g.dispose();
    }

    private static double rebote(double k) {
        k = Math.max(0, Math.min(1, k));
        return 1 - Math.pow(1 - k, 3) * Math.cos(k * Math.PI * 1.6);
    }

    private void tarjeta(Graphics2D g0, Ficha f, int x, int y, double t, boolean corona, int i) {
        Graphics2D g = (Graphics2D) g0.create();
        double k = rebote(t / 0.7);
        double dy = (1 - k) * 420;
        double rot = (1 - Math.min(1, t / 0.7)) * (i % 2 == 0 ? -0.25 : 0.25) + Math.sin(t * 1.3 + i) * 0.012;
        g.translate(x + CW / 2.0, y + CH / 2.0 + dy + Math.sin(t * 1.7 + i) * 4);
        g.rotate(rot);
        g.translate(-CW / 2.0, -CH / 2.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, t / 0.25)));

        // cuerpo
        g.setColor(new Color(30, 15, 5, 70));
        g.fill(new RoundRectangle2D.Double(3, 10, CW, CH, 36, 36));
        g.setColor(CREMA);
        g.fill(new RoundRectangle2D.Double(0, 0, CW, CH, 36, 36));
        Shape antes = g.getClip();
        g.clip(new RoundRectangle2D.Double(0, 0, CW, CH, 36, 36));
        g.setPaint(new GradientPaint(0, 0, f.color, 0, 150, f.color.brighter()));
        g.fillRect(0, 0, CW, 130);
        // puntitos de adorno en la banda
        g.setColor(new Color(255, 255, 255, 50));
        for (int j = 0; j < 12; j++) {
            g.fillOval(20 + (j * 67) % CW, 18 + (j * 37) % 90, 10 + j % 3 * 6, 10 + j % 3 * 6);
        }
        g.setClip(antes);
        g.setColor(new Color(0xEAD7AE));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(0, 0, CW, CH, 36, 36));

        // carita
        int cx = CW / 2, cy = 130, r = 74;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2));
        Ellipse2D circ = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(0xFFF1DC));
        g.fill(circ);
        String ruta = "/imagenes/personajes/" + f.carpeta + "/feliz.png";
        if (!Recursos.existe(ruta)) {
            ruta = "/imagenes/personajes/" + f.carpeta + "/quieto.png";
        }
        if (Recursos.existe(ruta)) {
            BufferedImage im = Recursos.imagen(ruta, 205, 250);
            double esc = (r * 2.4) / im.getWidth();
            int iw = (int) (im.getWidth() * esc), ih = (int) (im.getHeight() * esc);
            Shape a2 = g.getClip();
            g.clip(circ);
            g.drawImage(im, cx - iw / 2, (int) (cy - r * 1.02), iw, ih, null);
            g.setClip(a2);
        }
        g.setColor(f.color);
        g.setStroke(new BasicStroke(5f));
        g.draw(circ);
        if (corona) {
            coronita(g, cx + 46, cy - r - 4, t);
        }

        // nombre y rol
        g.setColor(TEXTO);
        g.setFont(Estilo.fuente(Font.BOLD, 34f));
        centrar(g, f.nombre, CW / 2, 255);
        g.setFont(Estilo.fuente(Font.BOLD, 16f));
        g.setColor(SUAVE);
        centrar(g, f.rol.toUpperCase(), CW / 2, 280);

        // el titulo: un sello que cae
        double ts = t - 0.9;
        if (ts > 0) {
            double s = 1 + 0.6 * Math.max(0, 1 - ts / 0.25) - 0.08 * Math.sin(Math.min(1, ts / 0.4) * Math.PI);
            Graphics2D gs = (Graphics2D) g.create();
            gs.translate(CW / 2.0, 322);
            gs.scale(s, s);
            gs.rotate(-0.03);
            gs.setFont(Estilo.fuente(Font.BOLD, 20f));
            FontMetrics fm = gs.getFontMetrics();
            int w = fm.stringWidth(f.titulo) + 40;
            gs.setPaint(new GradientPaint(0, -20, Estilo.DORADO_CLARO, 0, 20, Estilo.DORADO));
            gs.fill(new RoundRectangle2D.Double(-w / 2.0, -20, w, 40, 40, 40));
            gs.setColor(new Color(0xC98A12));
            gs.setStroke(new BasicStroke(2f));
            gs.draw(new RoundRectangle2D.Double(-w / 2.0, -20, w, 40, 40, 40));
            gs.setColor(TEXTO);
            gs.drawString(f.titulo, -fm.stringWidth(f.titulo) / 2f, 7);
            gs.dispose();
        }

        // barritas (se llenan)
        double tb = Math.max(0, Math.min(1, (t - 1.1) / 0.8));
        barrita(g, "Verificó", f.verifico, 380, new Color(0x7FB2E0), tb);
        barrita(g, "Participó", f.participo, 420, new Color(0xF0A85C), tb);
        barrita(g, "Confianza", f.confianza, 460, new Color(0x9CC08A), tb);

        // la mision secreta (modo feria): se revela despues de las barritas
        if (f.mision != null && t > 1.9) {
            double km = Math.min(1, (t - 1.9) / 0.3);
            String txt = (f.misionOk ? "Misión cumplida: " : "Misión fallida: ") + f.mision;
            g.setFont(Estilo.fuente(Font.BOLD, 15f));
            FontMetrics fmm = g.getFontMetrics();
            double mw = fmm.stringWidth(txt) + 30, mx = CW / 2.0 - mw / 2, my = 478;
            Graphics2D gm = (Graphics2D) g.create();
            gm.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) km));
            gm.setColor(f.misionOk ? new Color(0x9CC08A) : new Color(0xE3D6C2));
            gm.fill(new RoundRectangle2D.Double(mx, my, mw, 28, 28, 28));
            gm.setColor(f.misionOk ? Color.WHITE : SUAVE);
            gm.drawString(txt, (float) (mx + 15), (float) (my + 19));
            gm.dispose();
        }

        // mariposas
        int nm = Math.min(8, f.mariposas);
        double tm = t - 1.6;
        g.setFont(Estilo.fuente(Font.BOLD, 18f));
        g.setColor(TEXTO);
        String txt = f.mariposas == 1 ? "1 mariposa" : f.mariposas + " mariposas";
        if (nm == 0) {
            g.setColor(SUAVE);
            centrar(g, "sin mariposas… ¡la próxima!", CW / 2, 530);
        } else {
            for (int j = 0; j < nm; j++) {
                if (tm - j * 0.12 > 0) {
                    double pop = Math.min(1, (tm - j * 0.12) / 0.2);
                    mariposa(g, CW / 2.0 - (nm - 1) * 17 + j * 34, 506 + Math.sin(t * 4 + j) * 3, pop, t + j);
                }
            }
            centrar(g, txt, CW / 2, 545);
        }
        g.dispose();
    }

    private void barrita(Graphics2D g, String nombre, int v, int y, Color c, double k) {
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        g.setColor(SUAVE);
        g.drawString(nombre, 34, y + 5);
        int bx = 140, bw = 160;
        g.setColor(new Color(0xF1E6D0));
        g.fill(new RoundRectangle2D.Double(bx, y - 6, bw, 12, 12, 12));
        double p = Math.max(0, Math.min(1, v / 10.0)) * k;
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(bx, y - 6, Math.max(12, bw * p), 12, 12, 12));
        g.setColor(TEXTO);
        g.drawString(String.valueOf((int) Math.round(v * k)), bx + bw + 12, y + 5);
    }

    private static void mariposa(Graphics2D g0, double x, double y, double pop, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.scale(pop, pop);
        double ala = 0.75 + 0.25 * Math.abs(Math.sin(t * 6));
        g.setColor(new Color(0xF6C445));
        g.fill(new Ellipse2D.Double(-14 * ala, -12, 14 * ala, 14));
        g.fill(new Ellipse2D.Double(0, -12, 14 * ala, 14));
        g.setColor(new Color(0xFBE08F));
        g.fill(new Ellipse2D.Double(-10 * ala, 0, 10 * ala, 9));
        g.fill(new Ellipse2D.Double(0, 0, 10 * ala, 9));
        g.setColor(TEXTO);
        g.fill(new RoundRectangle2D.Double(-1.5, -11, 3, 20, 3, 3));
        g.dispose();
    }

    private static void coronita(Graphics2D g0, double x, double y, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        double s = Math.min(1, Math.max(0, (t - 1.4) / 0.3));
        if (s <= 0) {
            g.dispose();
            return;
        }
        AffineTransform at = g.getTransform();
        g.translate(x, y - (1 - s) * 60);
        g.rotate(0.35 + Math.sin(t * 2) * 0.05);
        Path2D p = new Path2D.Double();
        p.moveTo(-26, 14);
        p.lineTo(-30, -14);
        p.lineTo(-13, 2);
        p.lineTo(0, -20);
        p.lineTo(13, 2);
        p.lineTo(30, -14);
        p.lineTo(26, 14);
        p.closePath();
        g.setPaint(new GradientPaint(0, -20, Estilo.DORADO_CLARO, 0, 14, Estilo.DORADO));
        g.fill(p);
        g.setColor(new Color(0xC98A12));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(new Color(0xF2A7B8));
        g.fill(new Ellipse2D.Double(-5, 2, 10, 10));
        g.setTransform(at);
        g.dispose();
    }

    private static void centrar(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, y);
    }
}
