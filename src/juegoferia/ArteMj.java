package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KIT VISUAL de los minijuegos, con la estetica de los recursos de los minijuegos
 * (rastrea_, agua_, papel_, pergamino_, noticia_, circular_, mira_, permiso_):
 * piezas tipo STICKER, relleno crema, contorno marron grueso, borde blanco por fuera,
 * esquinas redondas, flores rosadas y amarillas, hojas y mariposas.
 *
 * Los recursos van en /imagenes/minijuegos/<carpeta>/<nombre>.png (tal como vinieron, ya limpios).
 * Si falta alguno, aqui se dibuja una pieza parecida en codigo, asi el juego nunca se rompe.
 *
 * Las CARAS de los personajes salen UNICAMENTE de los sprites de definitiva_560x680,
 * copiados en /imagenes/definitiva/<Nombre>.png. Si un personaje no tiene sprite ahi,
 * se deja el marco vacio con su inicial (no se inventa ninguna cara).
 */
public final class ArteMj {

    private ArteMj() {
    }

    public static final String RAIZ = "/imagenes/minijuegos/";
    public static final String CARAS = "/imagenes/definitiva/";

    // ---- paleta de los recursos ----
    public static final Color TINTA = new Color(0x5A3417);
    public static final Color TEXTO = new Color(0x4A2C14);
    public static final Color CREMA = new Color(0xFDF1D3);
    public static final Color CREMA_OSCURA = new Color(0xF3DDA8);
    public static final Color BLANCO = new Color(0xFFFDF7);
    public static final Color VERDE = new Color(0x8CC152);
    public static final Color VERDE_CLARO = new Color(0xDDEFC2);
    public static final Color VERDE_OSCURO = new Color(0x4E8A2E);
    public static final Color CORAL = new Color(0xEF7B6C);
    public static final Color CORAL_CLARO = new Color(0xFAD3C8);
    public static final Color AMARILLO = new Color(0xF7C843);
    public static final Color AMARILLO_CLARO = new Color(0xFFEBB0);
    public static final Color AZUL = new Color(0x6FB7E9);
    public static final Color AZUL_CLARO = new Color(0xD6ECFA);
    public static final Color ROSA = new Color(0xF4A3A8);
    public static final Color MORADO = new Color(0xB79AD9);
    public static final Color GRIS = new Color(0xC9C1B2);

    // ===================== RECURSOS (PNG) =====================

    public static String ruta(String carpeta, String nombre) {
        return RAIZ + carpeta + "/" + nombre + ".png";
    }

    public static boolean existe(String carpeta, String nombre) {
        return Recursos.existe(ruta(carpeta, nombre));
    }

    private static BufferedImage img(String carpeta, String nombre) {
        String r = ruta(carpeta, nombre);
        return Recursos.existe(r) ? Recursos.imagen(r, 10, 10) : null;
    }

    private static void suave(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** Pega el recurso centrado en (cx, cy) con ese ALTO (conserva la forma). false si no existe. */
    public static boolean pegar(Graphics2D g, String carpeta, String nombre, double cx, double cy, double alto) {
        BufferedImage im = img(carpeta, nombre);
        if (im == null) {
            return false;
        }
        suave(g);
        double w = alto * im.getWidth() / im.getHeight();
        g.drawImage(im, (int) Math.round(cx - w / 2), (int) Math.round(cy - alto / 2), (int) Math.round(w), (int) Math.round(alto), null);
        return true;
    }

    /** Pega el recurso ajustado (sin deformar) dentro de la caja. false si no existe. */
    public static boolean pegarEn(Graphics2D g, String carpeta, String nombre, Rectangle caja) {
        BufferedImage im = img(carpeta, nombre);
        if (im == null) {
            return false;
        }
        suave(g);
        double s = Math.min(caja.width / (double) im.getWidth(), caja.height / (double) im.getHeight());
        int w = (int) (im.getWidth() * s), h = (int) (im.getHeight() * s);
        g.drawImage(im, caja.x + (caja.width - w) / 2, caja.y + (caja.height - h) / 2, w, h, null);
        return true;
    }

    /** Igual que pegar, pero transparente (para "vacia", "desactivada"...). */
    public static boolean pegarTenue(Graphics2D g, String carpeta, String nombre, double cx, double cy, double alto, float alfa) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
        boolean ok = pegar(gg, carpeta, nombre, cx, cy, alto);
        gg.dispose();
        return ok;
    }

    // ===================== FONDO =====================

    /** Fondo oficial del lugar (lista de Rutas) con un velo para que las piezas resalten. */
    public static void fondoOficial(Graphics2D g, String[] rutas, int velo) {
        String r = Rutas.primera(rutas);
        if (r != null && Recursos.existe(r)) {
            suave(g);
            g.drawImage(Recursos.fondo(r), 0, 0, PanelDiseno.W, PanelDiseno.H, null);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(0xFBE7C2), 0, PanelDiseno.H, new Color(0xF0C58A)));
            g.fillRect(0, 0, PanelDiseno.W, PanelDiseno.H);
        }
        g.setColor(new Color(60, 34, 14, velo));
        g.fillRect(0, 0, PanelDiseno.W, PanelDiseno.H);
    }

    // ===================== STICKERS =====================

    /**
     * Forma tipo sticker: borde blanco por fuera, sombrita, relleno, brillo arriba y contorno marron.
     * grosor = contorno marron (en px de la pantalla de 1920x1080).
     */
    public static void sticker(Graphics2D g, Shape s, Paint relleno, float grosor) {
        suave(g);
        g.setColor(new Color(60, 34, 14, 55));
        AffineTransform at = g.getTransform();
        g.translate(0, grosor * 1.4);
        g.setStroke(new BasicStroke(grosor * 3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
        g.fill(s);
        g.setTransform(at);
        g.setColor(BLANCO);
        g.setStroke(new BasicStroke(grosor * 3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
        g.setPaint(relleno);
        g.fill(s);
        // brillo suave arriba (como la acuarela de los recursos)
        java.awt.Rectangle b = s.getBounds();
        Shape antes = g.getClip();
        g.clip(s);
        g.setPaint(new GradientPaint(0, b.y, new Color(255, 255, 255, 70), 0, b.y + b.height * 0.45f, new Color(255, 255, 255, 0)));
        g.fillRect(b.x, b.y, b.width, b.height / 2);
        g.setPaint(new GradientPaint(0, b.y + b.height * 0.6f, new Color(120, 70, 20, 0), 0, b.y + b.height, new Color(120, 70, 20, 28)));
        g.fillRect(b.x, b.y + b.height / 2, b.width, b.height / 2 + 2);
        g.setClip(antes);
        g.setColor(TINTA);
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    public static Shape redondo(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    /** Panel grande crema (papel de los recursos). */
    public static void panel(Graphics2D g, int x, int y, int w, int h, Color relleno) {
        sticker(g, redondo(x, y, w, h, Math.min(44, h / 4.0)), relleno, 6f);
    }

    /** Pildora (etiquetas, botones, sellos). */
    public static void pildora(Graphics2D g, int x, int y, int w, int h, Color relleno, float grosor) {
        sticker(g, redondo(x, y, w, h, h / 2.0), relleno, grosor);
    }

    /** Hoja de papel con esquina doblada (documentos). */
    public static void hojaPapel(Graphics2D g, int x, int y, int w, int h, Color relleno) {
        int d = Math.min(46, Math.min(w, h) / 5);
        Path2D p = new Path2D.Double();
        p.moveTo(x + 18, y);
        p.lineTo(x + w - d, y);
        p.lineTo(x + w, y + d);
        p.lineTo(x + w, y + h - 18);
        p.quadTo(x + w, y + h, x + w - 18, y + h);
        p.lineTo(x + 18, y + h);
        p.quadTo(x, y + h, x, y + h - 18);
        p.lineTo(x, y + 18);
        p.quadTo(x, y, x + 18, y);
        p.closePath();
        sticker(g, p, relleno, 5f);
        Path2D dobl = new Path2D.Double();
        dobl.moveTo(x + w - d, y);
        dobl.lineTo(x + w - d, y + d);
        dobl.lineTo(x + w, y + d);
        dobl.closePath();
        g.setColor(CREMA_OSCURA);
        g.fill(dobl);
        g.setColor(TINTA);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(dobl);
    }

    /** Lineas de "texto" grises (como los papeles de los recursos). */
    public static void renglones(Graphics2D g, int x, int y, int w, int n, int paso) {
        g.setColor(new Color(176, 146, 110));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n; i++) {
            int largo = (i == n - 1) ? w * 3 / 5 : w - (i % 2) * 30;
            g.drawLine(x, y + i * paso, x + largo, y + i * paso);
        }
    }

    /** Cinta adhesiva de color (como la de las fichas de los recursos). */
    public static void cinta(Graphics2D g, double cx, double cy, double w, Color c, double ang) {
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        g.rotate(Math.toRadians(ang));
        Shape s = new RoundRectangle2D.Double(-w / 2, -14, w, 28, 6, 6);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 220));
        g.fill(s);
        g.setColor(TINTA);
        g.setStroke(new BasicStroke(3f));
        g.draw(s);
        g.setTransform(at);
    }

    // ===================== DECORACION =====================

    public static void flor(Graphics2D g, double cx, double cy, double alto, boolean rosada) {
        if (pegar(g, "fecha_agua", rosada ? "agua_flor_rosada" : "agua_flor_amarilla", cx, cy, alto)) {
            return;
        }
        double r = alto / 2;
        g.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(i * 72 - 90);
            Ellipse2D e = new Ellipse2D.Double(cx + Math.cos(a) * r * 0.55 - r * 0.42, cy + Math.sin(a) * r * 0.55 - r * 0.42, r * 0.84, r * 0.84);
            g.setColor(rosada ? ROSA : AMARILLO);
            g.fill(e);
            g.setColor(TINTA);
            g.draw(e);
        }
        g.setColor(rosada ? AMARILLO : new Color(0xE58A2E));
        g.fill(new Ellipse2D.Double(cx - r * 0.25, cy - r * 0.25, r * 0.5, r * 0.5));
    }

    public static void hoja(Graphics2D g, double cx, double cy, double alto, double ang) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.rotate(Math.toRadians(ang), cx, cy);
        if (!pegar(gg, "fecha_agua", "agua_hoja", cx, cy, alto)) {
            Path2D p = new Path2D.Double();
            p.moveTo(cx, cy + alto / 2);
            p.quadTo(cx - alto * 0.45, cy, cx, cy - alto / 2);
            p.quadTo(cx + alto * 0.45, cy, cx, cy + alto / 2);
            gg.setColor(VERDE);
            gg.fill(p);
            gg.setColor(TINTA);
            gg.setStroke(new BasicStroke(2.5f));
            gg.draw(p);
        }
        gg.dispose();
    }

    /** Ramito de flor + hojas en una esquina (como los paneles florales de los recursos). */
    public static void ramito(Graphics2D g, double cx, double cy, double tam, boolean rosada) {
        hoja(g, cx - tam * 0.45, cy - tam * 0.1, tam * 0.75, -60);
        hoja(g, cx + tam * 0.4, cy + tam * 0.15, tam * 0.7, 70);
        flor(g, cx, cy, tam, rosada);
    }

    public static void mariposa(Graphics2D g, double cx, double cy, double alto, double ang) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.rotate(Math.toRadians(ang), cx, cy);
        if (!pegar(gg, "fecha_agua", "agua_mariposa", cx, cy, alto)
                && !pegar(gg, "pergaminos", "pergamino_mariposa", cx, cy, alto)) {
            for (int lado = -1; lado <= 1; lado += 2) {
                Ellipse2D e = new Ellipse2D.Double(cx + (lado < 0 ? -alto * 0.5 : 0), cy - alto * 0.4, alto * 0.5, alto * 0.6);
                gg.setColor(AMARILLO);
                gg.fill(e);
                gg.setColor(TINTA);
                gg.setStroke(new BasicStroke(2.5f));
                gg.draw(e);
            }
        }
        gg.dispose();
    }

    // ===================== TEXTO =====================

    public static Font letra(float tam) {
        return Estilo.fuente(Font.BOLD, tam);
    }

    public static void centrado(Graphics2D g, String s, double cx, double baseY, Color c) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (int) Math.round(cx - fm.stringWidth(s) / 2.0), (int) Math.round(baseY));
    }

    /** Texto envuelto; devuelve la y siguiente. */
    public static int parrafo(Graphics2D g, String texto, int x, int y, int maxW, Color c) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        for (String l : PanelDiseno.envolverTexto(texto, fm, maxW)) {
            g.drawString(l, x, y);
            y += fm.getHeight();
        }
        return y;
    }

    /** Texto envuelto y centrado vertical y horizontalmente en la caja. */
    public static void textoEnCaja(Graphics2D g, String texto, Rectangle r, Color c) {
        FontMetrics fm = g.getFontMetrics();
        List<String> ls = PanelDiseno.envolverTexto(texto, fm, r.width);
        int alto = ls.size() * fm.getHeight();
        int y = r.y + (r.height - alto) / 2 + fm.getAscent();
        g.setColor(c);
        for (String l : ls) {
            g.drawString(l, r.x + (r.width - fm.stringWidth(l)) / 2, y);
            y += fm.getHeight();
        }
    }

    // ===================== ICONOS =====================

    /** Circulo verde con palomita (como el de "¡Correcto!"). */
    public static void palomita(Graphics2D g, double cx, double cy, double d) {
        sticker(g, new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d), VERDE, (float) Math.max(2.5, d / 16));
        Path2D p = new Path2D.Double();
        p.moveTo(cx - d * 0.24, cy + d * 0.01);
        p.lineTo(cx - d * 0.06, cy + d * 0.19);
        p.lineTo(cx + d * 0.25, cy - d * 0.18);
        g.setColor(BLANCO);
        g.setStroke(new BasicStroke((float) (d * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }

    /** Circulo coral con equis (como el de "Incorrecto"). */
    public static void tachita(Graphics2D g, double cx, double cy, double d) {
        sticker(g, new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d), CORAL, (float) Math.max(2.5, d / 16));
        g.setColor(BLANCO);
        g.setStroke(new BasicStroke((float) (d * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double k = d * 0.2;
        g.drawLine((int) (cx - k), (int) (cy - k), (int) (cx + k), (int) (cy + k));
        g.drawLine((int) (cx + k), (int) (cy - k), (int) (cx - k), (int) (cy + k));
    }

    /** Circulo amarillo con signo de pregunta. */
    public static void pregunta(Graphics2D g, double cx, double cy, double d) {
        sticker(g, new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d), AMARILLO, (float) Math.max(2.5, d / 16));
        g.setFont(letra((float) (d * 0.7)));
        centrado(g, "?", cx, cy + d * 0.25, TINTA);
    }

    /**
     * SELLO/ESTADO (pildora de color con su icono, como "Coincide", "Tiene permiso", "Confirmado").
     * tipo: 0 verde (si), 1 coral (no), 2 amarillo (no aparece / falta saber).
     */
    public static void estado(Graphics2D g, Rectangle r, String texto, int tipo) {
        Color fondo = tipo == 0 ? VERDE_CLARO : tipo == 1 ? CORAL_CLARO : AMARILLO_CLARO;
        pildora(g, r.x, r.y, r.width, r.height, fondo, 4.5f);
        double d = r.height * 0.72;
        double cx = r.x + r.height * 0.55;
        if (tipo == 0) {
            palomita(g, cx, r.getCenterY(), d);
        } else if (tipo == 1) {
            tachita(g, cx, r.getCenterY(), d);
        } else {
            pregunta(g, cx, r.getCenterY(), d);
        }
        g.setFont(letra((float) Math.min(r.height * 0.42, 30)));
        Rectangle t = new Rectangle((int) (cx + d / 2 + 6), r.y, (int) (r.x + r.width - (cx + d / 2 + 6) - 12), r.height);
        textoEnCaja(g, texto, t, TEXTO);
    }

    /** Opcion sin elegir: pildora crema con su texto (lo que se puede tocar). */
    public static void opcion(Graphics2D g, Rectangle r, String texto, boolean resaltada) {
        pildora(g, r.x, r.y, r.width, r.height, resaltada ? AMARILLO_CLARO : CREMA, 4f);
        g.setFont(letra((float) Math.min(r.height * 0.4, 28)));
        textoEnCaja(g, texto, new Rectangle(r.x + 12, r.y, r.width - 24, r.height), TEXTO);
    }

    /** Flecha gruesa tipo sticker (como las flechas de los recursos). Curva si curva != 0. */
    public static void flecha(Graphics2D g, double x1, double y1, double x2, double y2, Color c, double grosor, double curva) {
        double mx = (x1 + x2) / 2, my = (y1 + y2) / 2;
        double nx = -(y2 - y1), ny = x2 - x1;
        double len = Math.max(1, Math.hypot(nx, ny));
        double qx = mx + nx / len * curva, qy = my + ny / len * curva;
        double a = Math.atan2(y2 - qy, x2 - qx);
        double punta = grosor * 1.6;
        double ex = x2 - Math.cos(a) * punta * 0.8, ey = y2 - Math.sin(a) * punta * 0.8;
        QuadCurve2D cuerpo = new QuadCurve2D.Double(x1, y1, qx, qy, ex, ey);
        Path2D cabeza = new Path2D.Double();
        cabeza.moveTo(x2, y2);
        cabeza.lineTo(ex + Math.cos(a + Math.PI / 2) * punta, ey + Math.sin(a + Math.PI / 2) * punta);
        cabeza.lineTo(ex + Math.cos(a - Math.PI / 2) * punta, ey + Math.sin(a - Math.PI / 2) * punta);
        cabeza.closePath();
        Area forma = new Area(new BasicStroke((float) grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(cuerpo));
        forma.add(new Area(cabeza));
        sticker(g, forma, c, (float) Math.max(3, grosor / 5));
    }

    /** Lupa (recurso). */
    public static void lupa(Graphics2D g, double cx, double cy, double alto) {
        if (!pegar(g, "cinco_noticia", "noticia_lupa", cx, cy, alto) && !pegar(g, "papel", "papel_lupa", cx, cy, alto)) {
            sticker(g, new Ellipse2D.Double(cx - alto * 0.35, cy - alto * 0.35, alto * 0.5, alto * 0.5), AZUL_CLARO, 4f);
        }
    }

    // ===================== CARAS (solo definitiva_560x680) =====================

    private static final Map<String, String> ARCHIVO = new HashMap<>();

    static {
        String[][] m = {
            {"CIUDADANO", "Ciudadano"}, {"PERIODISTA", "Periodista"}, {"INFLUENCER", "Influencer"}, {"CANDIDATO", "Candidato"},
            {"ROSA", "Rosa"}, {"URSULA", "Ursula"}, {"SAMUEL", "Samuel"}, {"MELQUIADES", "Melquiades"},
            {"RECTORA", "Rectora"}, {"RECTORA VISITACION", "Rectora"}, {"VISITACION", "Rectora"},
            {"MAURICIO", "Mauricio"}, {"ORLANDO", "Orlando"}, {"AMARANTA", "Amaranta"}, {"CHELA", "Chela"},
            {"QUIROGA", "Quiroga"}, {"MARISOL", "Marisol"}, {"PETRA", "Petra"}, {"ALIRIO", "Alirio"},
            {"GUSTAVO", "Gustavo"}, {"SANTA SOFIA", "Santa_sofia"}, {"SOFIA", "Santa_sofia"},
            {"SANTA SOFIA DE LA PIEDAD", "Santa_sofia"}, {"EMIRO", "Emiro"}, {"DANIELA", "Daniela"},
            {"JULIAN", "Julian"}, {"MATEO", "Mateo"}, {"BEATRIZ", "Beatriz"}, {"CLEMENCIA", "Clemencia"},
            {"TEOFILO", "Teofilo"}, {"RAMIRO", "Ramiro"}, {"BRAYAN", "Brayan"}, {"NELLY", "Nelly"},
            {"ELIECER", "Eliecer"}, {"XIMENA", "Ximena"}, {"LORENA", "Lorena"}, {"SALOME", "Salome"},
            {"PILAR", "Pilar"}, {"REBECA", "Rebeca"}, {"AURELIANO", "Aureliano"}, {"REMEDIOS", "Remedios"},
            {"RINGO", "Ringo"}, {"YEISON", "Yeison"}, {"WINSTON", "Winston"}, {"KEVIN", "Kevin_ig"},
            {"ESPERANZA", "Esperanza"}, {"HERMINA", "Hermina"}, {"GLADYS", "Gladys"}, {"CECILIA", "Cecilia"},
            {"TATIANA", "Tatiana"}, {"RODOLFO", "Rodolfo"}, {"OVIDIO", "Ovidio"}, {"MARLENY", "Marleny"},
            {"LEONEL", "Leonel"}, {"PIETRO", "Pietro"}, {"FONCHO", "Foncho"}, {"EFRAIN", "Efrain"},
            {"ESTEBAN", "Esteban"}, {"JOSE ARCADIO", "JoseArcadio"}
        };
        for (String[] p : m) {
            ARCHIVO.put(p[0], p[1]);
        }
    }

    /** "DOÑA ROSA" -> "ROSA", "Rectora Visitación" -> "RECTORA VISITACION". */
    public static String clave(String quien) {
        String s = Normalizer.normalize(quien.trim().toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^A-Z ]", " ").replaceAll(" +", " ").trim();
        for (String pre : new String[]{"DONA ", "DON ", "PROFE ", "SENORA ", "SENOR "}) {
            if (s.startsWith(pre)) {
                s = s.substring(pre.length());
            }
        }
        return s;
    }

    /** Ruta del sprite oficial de ese personaje, o null si no tiene en definitiva_560x680. */
    public static String spriteOficial(String quien) {
        if (quien == null) {
            return null;
        }
        String k = clave(quien);
        String f = ARCHIVO.get(k);
        if (f == null && k.contains(" ")) {
            f = ARCHIVO.get(k.substring(0, k.indexOf(' ')));
        }
        if (f == null) {
            return null;
        }
        String r = CARAS + f + ".png";
        if (Recursos.existe(r)) {
            return r;
        }
        // si no esta en definitiva/, el NPC estatico de /imagenes/personaje/<Nombre>/<Nombre>.png
        return Rutas.spriteEstatico(f, k.replace(' ', '_'));
    }

    public static String spriteOficial(Rol r) {
        return spriteOficial(r.name());
    }

    private static final Map<String, int[]> CABEZA = new HashMap<>();

    /** Donde esta la cabeza en el sprite (se calcula una vez con la transparencia). */
    private static int[] cabeza(String ruta, BufferedImage im) {
        return CABEZA.computeIfAbsent(ruta, k -> {
            int w = im.getWidth(), h = im.getHeight();
            int top = 0;
            buscar:
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x += 2) {
                    if ((im.getRGB(x, y) >>> 24) > 40) {
                        top = y;
                        break buscar;
                    }
                }
            }
            int min = w, max = 0;
            for (int y = top; y < Math.min(h, top + h / 3); y += 2) {
                for (int x = 0; x < w; x += 2) {
                    if ((im.getRGB(x, y) >>> 24) > 40) {
                        min = Math.min(min, x);
                        max = Math.max(max, x);
                    }
                }
            }
            int lado = (int) (w * 0.57);
            return new int[]{(min + max) / 2 - lado / 2, top + (int) (h * 0.02), lado};
        });
    }

    /**
     * Marco redondo tipo sticker con la CARA del sprite oficial. Si no hay sprite en definitiva,
     * el marco queda vacio con la inicial del nombre.
     */
    public static void cara(Graphics2D g, double cx, double cy, double r, String quien, Color aro) {
        caraDe(g, cx, cy, r, spriteOficial(quien), quien, aro);
    }

    /** Igual que cara(...), pero con la ruta del sprite ya elegida (ej: personajes/ciudadano/feliz.png). */
    public static void caraDe(Graphics2D g, double cx, double cy, double r, String ruta, String quien, Color aro) {
        Ellipse2D c = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        sticker(g, c, aro == null ? CREMA : aro, (float) Math.max(3, r / 11));
        double ri = r - Math.max(3, r / 11) * 1.2;
        Ellipse2D dentro = new Ellipse2D.Double(cx - ri, cy - ri, ri * 2, ri * 2);
        g.setColor(CREMA);
        g.fill(dentro);
        if (ruta != null) {
            BufferedImage im = Recursos.imagen(ruta, 560, 680);
            int[] cab = cabeza(ruta, im);
            Shape antes = g.getClip();
            g.clip(dentro);
            suave(g);
            g.drawImage(im, (int) (cx - ri), (int) (cy - ri), (int) (cx + ri), (int) (cy + ri),
                    cab[0], cab[1], cab[0] + cab[2], cab[1] + cab[2], null);
            g.setClip(antes);
        } else if (quien != null && !quien.isEmpty()) {
            g.setFont(letra((float) (r * 0.9)));
            centrado(g, clave(quien).substring(0, 1), cx, cy + r * 0.32, new Color(176, 146, 110));
        }
        g.setColor(TINTA);
        g.setStroke(new BasicStroke((float) Math.max(3, r / 11)));
        g.draw(dentro);
    }

    // ===================== GLOBOS DE DIALOGO =====================

    /**
     * Globo de dialogo tipo sticker (crema, contorno marron, borde blanco) con colita hacia la cara.
     * colita: -1 a la izquierda, 1 a la derecha, 0 sin colita.
     */
    public static void globo(Graphics2D g, int x, int y, int w, int h, int colita, Color relleno) {
        Area a = new Area(redondo(x, y, w, h, Math.min(34, h / 2.4)));
        if (colita != 0) {
            Path2D t = new Path2D.Double();
            int cy = y + Math.min(h / 2, 54);
            if (colita < 0) {
                t.moveTo(x + 6, cy - 16);
                t.lineTo(x - 26, cy + 4);
                t.lineTo(x + 6, cy + 18);
            } else {
                t.moveTo(x + w - 6, cy - 16);
                t.lineTo(x + w + 26, cy + 4);
                t.lineTo(x + w - 6, cy + 18);
            }
            t.closePath();
            a.add(new Area(t));
        }
        sticker(g, a, relleno == null ? CREMA : relleno, 4.5f);
    }

    /** Alto que ocuparia burbujaConCara (para acomodar la tarjeta antes de dibujar). */
    public static int altoBurbuja(Graphics2D g, int ancho, String texto) {
        int r = 46;
        g.setFont(Estilo.fuente(Font.PLAIN, 28f));
        FontMetrics fm = g.getFontMetrics();
        int gw = ancho - (r * 2 + 40);
        List<String> ls = PanelDiseno.envolverTexto(texto, fm, gw - 52);
        return Math.max(r * 2 + 4, 52 + ls.size() * fm.getHeight() + 16);
    }

    /**
     * Burbuja de dialogo completa: cara (sprite oficial) + globo con nombre y texto.
     * Devuelve el alto que ocupo. lado: -1 cara a la izquierda, 1 cara a la derecha.
     */
    public static int burbujaConCara(Graphics2D g, int x, int y, int ancho, String quien, String nombreVisible,
            String texto, int lado, Color aro) {
        int r = 46;
        g.setFont(Estilo.fuente(Font.PLAIN, 28f));
        FontMetrics fm = g.getFontMetrics();
        int gw = ancho - (r * 2 + 40);
        List<String> ls = PanelDiseno.envolverTexto(texto, fm, gw - 52);
        int gh = Math.max(r * 2 + 4, 52 + ls.size() * fm.getHeight() + 16);
        int caraX = lado < 0 ? x + r : x + ancho - r;
        int gx = lado < 0 ? x + r * 2 + 34 : x;
        globo(g, gx, y, gw, gh, lado, null);
        cara(g, caraX, y + Math.min(gh / 2, 52), r, quien, aro);
        g.setFont(letra(24f));
        g.setColor(aro != null ? aro.darker().darker() : new Color(0xB0562E));
        g.drawString(nombreVisible, gx + 26, y + 36);
        g.setFont(Estilo.fuente(Font.PLAIN, 28f));
        g.setColor(TEXTO);
        int ty = y + 36 + fm.getHeight();
        for (String l : ls) {
            g.drawString(l, gx + 26, ty);
            ty += fm.getHeight();
        }
        return gh;
    }

    // ===================== TITULOS, BOTONES Y RESULTADOS =====================

    /** Prefijo de los recursos de cada carpeta. */
    public static String prefijo(String carpeta) {
        if (carpeta == null) {
            return null;
        }
        switch (carpeta) {
            case "fecha_agua": return "agua";
            case "pergaminos": return "pergamino";
            case "cinco_noticia": return "noticia";
            case "quien_mira": return "mira";
            case "lista_permiso": return "permiso";
            default: return carpeta;
        }
    }

    private static String bannerDe(String carpeta) {
        if ("rastrea".equals(carpeta)) {
            return "rastrea_banner_audio_texto";
        }
        if ("fecha_agua".equals(carpeta)) {
            return "agua_banner_fecha_del_agua";
        }
        return prefijo(carpeta) + "_banner_titulo";
    }

    /** Titulo del minijuego: su banner (recurso) o, si no hay, una cinta crema con el nombre. */
    public static void titulo(Graphics2D g, String carpeta, String nombre, int x, int y, int alto) {
        if (carpeta != null && existe(carpeta, bannerDe(carpeta))) {
            BufferedImage im = img(carpeta, bannerDe(carpeta));
            double w = alto * im.getWidth() / (double) im.getHeight();
            pegar(g, carpeta, bannerDe(carpeta), x + w / 2, y + alto / 2.0, alto);
            return;
        }
        g.setFont(letra(alto * 0.34f));
        int w = g.getFontMetrics().stringWidth(nombre) + 110;
        int h = (int) (alto * 0.62);
        pildora(g, x, y + (alto - h) / 2, w, h, CREMA, 5f);
        ramito(g, x + 18, y + (alto - h) / 2 + 12, 46, true);
        centrado(g, nombre, x + w / 2.0 + 14, y + alto / 2.0 + alto * 0.12, TEXTO);
    }

    /** Titulo centrado (para el aviso "Cómo se juega"). */
    public static void tituloCentrado(Graphics2D g, String carpeta, String nombre, int cx, int y, int alto) {
        if (carpeta != null && existe(carpeta, bannerDe(carpeta))) {
            pegar(g, carpeta, bannerDe(carpeta), cx, y + alto / 2.0, alto);
            return;
        }
        g.setFont(letra(alto * 0.3f));
        int w = g.getFontMetrics().stringWidth(nombre) + 120;
        titulo(g, null, nombre, cx - w / 2, y, alto);
    }

    /**
     * Boton tipo sticker. estado: 0 normal, 1 resaltado, 2 desactivado.
     * Pildora crema con un circulo verde con flecha (como "¡Correcto!" de los recursos).
     */
    public static void boton(Graphics2D g, int x, int y, int w, int h, String texto, int estado) {
        Graphics2D gg = (Graphics2D) g.create();
        if (estado == 2) {
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        }
        int off = estado == 1 ? -3 : 0;
        pildora(gg, x, y + off, w, h, estado == 1 ? AMARILLO_CLARO : CREMA, 5f);
        double d = h * 0.66;
        double cx = x + h * 0.52, cy = y + off + h / 2.0;
        sticker(gg, new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d), estado == 2 ? GRIS : VERDE, 3.5f);
        Path2D f = new Path2D.Double();
        f.moveTo(cx - d * 0.2, cy - d * 0.22);
        f.lineTo(cx + d * 0.18, cy);
        f.lineTo(cx - d * 0.2, cy + d * 0.22);
        gg.setColor(BLANCO);
        gg.setStroke(new BasicStroke((float) (d * 0.14), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gg.draw(f);
        gg.setFont(letra(Math.min(36f, h * 0.42f)));
        Rectangle t = new Rectangle((int) (cx + d / 2 + 6), y + off, (int) (x + w - (cx + d / 2 + 6) - 16), h);
        textoEnCaja(gg, texto, t, TEXTO);
        gg.dispose();
    }

    /**
     * Banner de resultado de cada minijuego (sus recursos "¡Muy bien!", "¡Casi!", "Inténtalo de nuevo").
     * Si la carpeta no los trae, se usan los de "La fecha del agua"; si tampoco, uno dibujado.
     */
    public static void resultado(Graphics2D g, String carpeta, int nivel, int cx, int cy, int alto) {
        String p = prefijo(carpeta);
        String[][] pruebas;
        if (nivel == Minijuego.BIEN) {
            pruebas = new String[][]{{carpeta, p + "_feedback_muy_bien"}, {carpeta, p + "_estado_correcto"},
                {"fecha_agua", "agua_feedback_muy_bien"}};
        } else if (nivel == Minijuego.REGULAR) {
            pruebas = new String[][]{{carpeta, p + "_feedback_casi"}, {"fecha_agua", "agua_feedback_casi"}};
        } else {
            pruebas = new String[][]{{carpeta, p + "_feedback_intentalo"}, {carpeta, p + "_feedback_reintentar"},
                {carpeta, p + "_estado_reintentar"}, {"fecha_agua", "agua_feedback_intentalo"}};
        }
        for (String[] pr : pruebas) {
            if (pr[0] != null && pegar(g, pr[0], pr[1], cx, cy, alto)) {
                return;
            }
        }
        String txt = nivel == Minijuego.BIEN ? "¡Muy bien!" : nivel == Minijuego.REGULAR ? "¡Casi!" : "Inténtalo de nuevo";
        g.setFont(letra(alto * 0.36f));
        int w = g.getFontMetrics().stringWidth(txt) + 160;
        int h = (int) (alto * 0.62);
        pildora(g, cx - w / 2, cy - h / 2, w, h, nivel == Minijuego.BIEN ? VERDE_CLARO : nivel == Minijuego.REGULAR ? AMARILLO_CLARO : CORAL_CLARO, 5f);
        if (nivel == Minijuego.BIEN) {
            palomita(g, cx - w / 2.0 + h * 0.6, cy, h * 0.7);
        } else if (nivel == Minijuego.REGULAR) {
            flor(g, cx - w / 2.0 + h * 0.6, cy, h * 0.75, true);
        } else {
            tachita(g, cx - w / 2.0 + h * 0.6, cy, h * 0.7);
        }
        centrado(g, txt, cx + h * 0.35, cy + alto * 0.12, TEXTO);
    }

    /** 3 mariposas de intentos (llena = queda, transparente = se gasto). */
    public static void intentos(Graphics2D g, int x, int y, int quedan, int total) {
        for (int i = 0; i < total; i++) {
            if (i < quedan) {
                mariposa(g, x + i * 66, y, 52, -8 + i * 8);
            } else {
                Graphics2D gg = (Graphics2D) g.create();
                gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                mariposa(gg, x + i * 66, y, 52, 0);
                gg.dispose();
            }
        }
    }

    /** Aviso corto arriba (pildora amarilla). */
    public static void aviso(Graphics2D g, String texto, int cx, int y) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        g.setFont(letra(28f));
        int w = g.getFontMetrics().stringWidth(texto) + 70;
        pildora(g, cx - w / 2, y, w, 58, AMARILLO_CLARO, 4f);
        centrado(g, texto, cx, y + 39, TEXTO);
    }

    /** Ficha numerada (recursos agua_numero_N, o una ficha dibujada). */
    public static void numero(Graphics2D g, int n, double cx, double cy, double alto, boolean activa) {
        if (n >= 1 && n <= 7 && pegar(g, "fecha_agua", "agua_numero_" + n, cx, cy, alto)) {
            return;
        }
        sticker(g, redondo(cx - alto * 0.45, cy - alto * 0.42, alto * 0.9, alto * 0.84, 14), activa ? AMARILLO_CLARO : CREMA, 4f);
        g.setFont(letra((float) (alto * 0.55)));
        centrado(g, String.valueOf(n), cx, cy + alto * 0.2, TEXTO);
    }

    /** Ficha de rompecabezas tipo sticker (como las piezas de "Las cinco de una noticia"). */
    public static Shape formaPuzzle(double x, double y, double w, double h) {
        double k = Math.min(h * 0.34, 38);
        Area a = new Area(redondo(x, y, w - k * 0.6, h, 22));
        a.add(new Area(new Ellipse2D.Double(x + w - k * 1.25, y + h / 2 - k / 2, k, k)));
        a.subtract(new Area(new Ellipse2D.Double(x - k * 0.45, y + h / 2 - k * 0.45, k * 0.9, k * 0.9)));
        return a;
    }

    public static void puzzle(Graphics2D g, Rectangle r, Color relleno) {
        sticker(g, formaPuzzle(r.x, r.y, r.width, r.height), relleno, 5f);
    }

    /** Hueco punteado (donde se pone algo). */
    public static void hueco(Graphics2D g, Rectangle r, boolean invitando) {
        Shape s = redondo(r.x, r.y, r.width, r.height, 22);
        g.setColor(invitando ? new Color(255, 235, 176, 200) : new Color(255, 248, 230, 150));
        g.fill(s);
        g.setColor(new Color(0x9C7A55));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{14f, 11f}, 0f));
        g.draw(s);
    }

    /** Numero entre -1 y 1 que siempre es el mismo para la misma semilla (garabatos que no "tiemblan"). */
    public static double ruido(long semilla) {
        long x = semilla * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        x ^= (x >>> 31);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 29);
        return ((x >>> 11) % 2000) / 1000.0 - 1.0;
    }
}
