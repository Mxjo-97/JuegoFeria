package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minijuego fijo de la PANADERIA del Dia 2: "Las cinco de una noticia". Lo juega Periodista.
 * (guion 2.1 y prompt del generador 6.5 · fondo: panaderia_dia2.png · arte: /imagenes/minijuegos/cinco_noticia/)
 *
 * La rectora Visitacion entrega una citacion rota en seis pedazos: QUIEN, QUE, CUANDO, DONDE, POR QUE y REGLA.
 *   FASE 1: armar la citacion. Se toca un pedazo suelto y luego la casilla donde va. Tres errores = MAL.
 *   FASE 2: marcar que campos le FALTAN al audio (esta / falta) y tocar la palabra del audio que choca con la REGLA.
 *
 * BIEN: citacion completa + los tres campos que le faltan al audio + la palabra "revisar".
 * REGULAR: citacion completa.  MAL: se rompio antes de armarla (la verdad la cuenta la rectora).
 *
 * Las zonas de clic son FIJAS (no dependen de la fuente) para que en red las cuatro computadoras coincidan.
 */
public class MjCincoNoticia extends Minijuego {

    public static final String[] CAMPOS = {"QUIÉN", "QUÉ", "CUÁNDO", "DÓNDE", "POR QUÉ", "REGLA"};
    public static final String[] TEXTOS = {
        "La rectoría del colegio.",
        "Se guardarán los celulares en un casillero con llave.",
        "Desde el lunes.",
        "En la entrada del colegio.",
        "Para evitar distracciones en clase.",
        "Nadie abre ni revisa el celular de nadie."
    };
    /** Para los archivos de arte: cinco_noticia_pieza_<clave>_vacio.png */
    private static final String[] CLAVE = {"quien", "que", "cuando", "donde", "por_que", "regla"};

    /** El audio que corre por el grupo del barrio (guion 2.1). */
    public static final String AUDIO = "En el colegio van a revisar todos los celulares a la entrada.";
    /** Fase 2: de los cinco primeros campos, cuales le FALTAN al audio (true = falta). */
    private static final boolean[] FALTA = {true, false, true, false, true};
    private static final String PALABRA_CHOCA = "revisar";

    private static final int ERRORES_MAX = 3;

    private final boolean pistaQuien, pistaOtra;

    private int fase2;                       // 0 = armando, 1 = marcando el audio
    private final int[] colocado = new int[6];   // casilla -> pedazo (o -1)
    private final List<Integer> sueltos = new ArrayList<>();   // pedazos que siguen en la mesa, en su puesto
    private int elegido = -1;
    private int errores;
    private final int[] marca = new int[5];   // -1 sin marcar, 0 esta, 1 falta
    private int palabra = -1;
    private String aviso = "";
    private double tAviso;
    private int consejo;

    /**
     * @param pistaQuien alguien verifico el audio en la panaderia: el campo QUIÉN ya viene puesto
     * @param pistaOtra  anoche se voto investigar sin avisar: un campo mas ya viene puesto (CUÁNDO)
     */
    public MjCincoNoticia(boolean pistaQuien, boolean pistaOtra) {
        this.pistaQuien = pistaQuien;
        this.pistaOtra = pistaOtra;
    }

    @Override
    public String nombre() {
        return "Las cinco de una noticia";
    }

    @Override
    public String instrucciones() {
        return "La rectora te da una citación rota en 6 pedazos: QUIÉN, QUÉ, CUÁNDO, DÓNDE, POR QUÉ y REGLA. "
                + "Ármala: toca un pedazo y luego su casilla. Luego marca qué campos le faltan al audio.";
    }

    @Override
    protected String carpetaArte() {
        return "cinco_noticia";
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "cinco_noticia", "noticia_rompecabezas_completo", cx, cy, 220)) {
            ArteMj.lupa(g, cx, cy, 200);
        }
    }

    @Override
    protected void prepararPartida() {
        fase2 = 0;
        for (int i = 0; i < 6; i++) {
            colocado[i] = -1;
        }
        sueltos.clear();
        for (int i = 0; i < 6; i++) {
            sueltos.add(i);
        }
        Collections.shuffle(sueltos, Sincronia.nuevoRandom());
        if (pistaQuien) {
            poner(0);
        }
        if (pistaOtra) {
            poner(2);
        }
        for (int i = 0; i < 5; i++) {
            marca[i] = -1;
        }
        elegido = -1;
        errores = 0;
        palabra = -1;
        aviso = "";
        consejo = 0;
    }

    private void poner(int pedazo) {
        colocado[pedazo] = pedazo;
        int k = sueltos.indexOf(pedazo);
        if (k >= 0) {
            sueltos.set(k, -1);     // deja el hueco en la mesa (los demas no se mueven)
        }
    }

    // ---- zonas fijas ----

    private static Rectangle casilla(int i) {
        return new Rectangle(330, 272 + i * 106, 610, 92);
    }

    private static Rectangle puestoSuelto(int k) {
        return new Rectangle(1030 + (k % 2) * 420, 190 + (k / 2) * 230, 390, 190);
    }

    private static Rectangle botonMarca(int campo, int op) {
        return new Rectangle(1330 + op * 250, 560 + campo * 82, 230, 66);
    }

    private final Rectangle verificar = new Rectangle(1500, 975, 360, 88);

    /** Donde va cada palabra del audio (ancho fijo por letra, igual en todas las computadoras). */
    private static List<Rectangle> palabrasAudio() {
        List<Rectangle> r = new ArrayList<>();
        int x0 = 1060, cx = x0, cy = 300, maxX = 1840;
        for (String p : AUDIO.split(" ")) {
            int w = p.length() * 21 + 8;
            if (cx + w > maxX) {
                cx = x0;
                cy += 58;
            }
            r.add(new Rectangle(cx - 4, cy - 38, w + 8, 50));
            cx += w + 16;
        }
        return r;
    }

    private static String limpia(String w) {
        return w.replaceAll("[^\\p{L}]", "").toLowerCase();
    }

    // ---- juego ----

    @Override
    protected void actualizarJuego(double dt) {
        if (tAviso > 0) {
            tAviso -= dt;
            if (tAviso <= 0) {
                aviso = "";
            }
        }
    }

    private void decir(String s) {
        aviso = s;
        tAviso = 2.5;
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        if (fase2 == 0) {
            clicArmar(p);
        } else {
            clicAudio(p);
        }
    }

    private void clicArmar(Point p) {
        for (int k = 0; k < sueltos.size(); k++) {
            int pz = sueltos.get(k);
            if (pz >= 0 && puestoSuelto(k).contains(p)) {
                elegido = elegido == pz ? -1 : pz;
                Funciones.reproducirSonido("/sonidos/click.wav");
                return;
            }
        }
        if (elegido < 0) {
            return;
        }
        for (int c = 0; c < 6; c++) {
            if (casilla(c).contains(p) && colocado[c] < 0) {
                if (c == elegido) {
                    poner(elegido);
                    elegido = -1;
                    consejo++;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    if (todoArmado()) {
                        fase2 = 1;
                        decir("¡Citación armada! Ahora mira el audio.");
                    }
                } else {
                    errores++;
                    decir("Ese pedazo no va en " + CAMPOS[c] + ".");
                    elegido = -1;
                    if (errores >= ERRORES_MAX) {
                        terminarJuego(false, MAL, new Efecto(),
                                "Rectora Visitación: (lee ella) Se las explico yo. La citación dice «guardar», no «revisar».");
                    }
                }
                return;
            }
        }
    }

    private boolean todoArmado() {
        for (int c : colocado) {
            if (c < 0) {
                return false;
            }
        }
        return true;
    }

    private void clicAudio(Point p) {
        for (int f = 0; f < 5; f++) {
            for (int op = 0; op < 2; op++) {
                if (botonMarca(f, op).contains(p)) {
                    marca[f] = op;
                    consejo++;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    return;
                }
            }
        }
        List<Rectangle> rs = palabrasAudio();
        for (int i = 0; i < rs.size(); i++) {
            if (rs.get(i).contains(p)) {
                palabra = palabra == i ? -1 : i;
                Funciones.reproducirSonido("/sonidos/click.wav");
                return;
            }
        }
        if (verificar.contains(p) && marcasCompletas()) {
            int bien = 0;
            for (int f = 0; f < 5; f++) {
                if ((marca[f] == 1) == FALTA[f]) {
                    bien++;
                }
            }
            boolean palabraBien = palabra >= 0 && limpia(AUDIO.split(" ")[palabra]).equals(PALABRA_CHOCA);
            if (bien == 5 && palabraBien) {
                terminarJuego(true, BIEN,
                        Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, EstadoJuego.PARTICIPACION, 1, Partida.DESINFO_LUGAR, -3),
                        "Periodista: El audio no dice quién, ni cuándo, ni por qué. Y dice «revisar». La citación dice «guardar». "
                        + "Rectora Visitación: Exacto.");
            } else {
                terminarJuego(true, REGULAR, Efecto.de(EstadoJuego.INFO_VERIFICADA, 1),
                        "Rectora Visitación: Falta ver qué no dice el audio. (" + bien + " de 5 campos bien"
                        + (palabraBien ? "" : "; la palabra que choca es «revisar»") + ")");
            }
        }
    }

    private boolean marcasCompletas() {
        for (int m : marca) {
            if (m < 0) {
                return false;
            }
        }
        return true;
    }

    // ---- dibujo ----

    /** Etiqueta de cada campo: los recursos ¿Quién? ¿Qué? ¿Cuándo? ¿Dónde? ¿Por qué? (REGLA se dibuja igual). */
    private static void etiqueta(Graphics2D g, int campo, int cx, int cy, int alto) {
        String[] archivo = {"noticia_pregunta_quien", "noticia_pregunta_que", "noticia_pregunta_cuando",
            "noticia_pregunta_donde", "noticia_pregunta_por_que", null};
        if (archivo[campo] != null && ArteMj.pegar(g, "cinco_noticia", archivo[campo], cx, cy, alto)) {
            return;
        }
        int w = (int) (alto * 1.7), h = (int) (alto * 0.62);
        ArteMj.pildora(g, cx - w / 2, cy - h / 2 + 6, w, h, ArteMj.AMARILLO_CLARO, 4.5f);
        ArteMj.flor(g, cx - w / 2.0 + 8, cy + h / 2.0 - 2, alto * 0.36, true);
        g.setFont(ArteMj.letra(alto * 0.3f));
        ArteMj.centrado(g, campo == 5 ? "Regla" : CAMPOS[campo], cx + 8, cy + alto * 0.17, ArteMj.TEXTO);
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        ArteMj.fondoOficial(g, Rutas.PANADERIA_DIA2, 95);
        String[] cs = fase2 == 0
                ? new String[]{"Empieza por el QUIÉN.", "¿Cuál dice cuándo empieza?", "La REGLA es lo que nadie puede hacer."}
                : new String[]{"Mira lo que dice «revisar».", "¿El audio dice cuándo?", "¿Quién firma el audio?"};
        cabecera(g, nombre(), cs[consejo % cs.length]);
        ArteMj.intentos(g, 1700, 70, ERRORES_MAX - errores, ERRORES_MAX);
        dibujarCitacion(g);
        if (fase2 == 0) {
            dibujarSueltos(g);
        } else {
            dibujarAudio(g);
        }
        ArteMj.aviso(g, aviso, 1240, 84);
    }

    private void dibujarCitacion(Graphics2D g) {
        int x = 60, y = 140, w = 940, h = 820;
        ArteMj.hojaPapel(g, x, y, w, h, ArteMj.CREMA);
        ArteMj.pegar(g, "cinco_noticia", "noticia_tarjeta_titular", x + w - 110, y + 70, 120);
        g.setFont(ArteMj.letra(24f));
        g.setColor(new Color(0x9C7A55));
        g.drawString("Colegio Distrital de Macondo · Rectoría", x + 40, y + 48);
        g.setFont(ArteMj.letra(50f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("CITACIÓN", x + 40, y + 108);
        for (int c = 0; c < 6; c++) {
            Rectangle r = casilla(c);
            etiqueta(g, c, 205, r.y + r.height / 2, 96);
            if (colocado[c] < 0) {
                ArteMj.hueco(g, r, elegido >= 0);
            } else {
                pedazo(g, colocado[c], r, false, true);
            }
        }
    }

    private void pedazo(Graphics2D g, int i, Rectangle r, boolean levantado, boolean puesto) {
        int off = levantado ? -12 : 0;
        Rectangle rr = new Rectangle(r.x, r.y + off, r.width, r.height);
        ArteMj.puzzle(g, rr, puesto ? new Color(0xEAF6D8) : levantado ? ArteMj.AMARILLO_CLARO : ArteMj.CREMA);
        g.setFont(Estilo.fuente(Font.PLAIN, puesto ? 26f : 28f));
        Rectangle t = new Rectangle(rr.x + 34, rr.y + 6, rr.width - (puesto ? 120 : 80), rr.height - 12);
        FontMetrics fm = g.getFontMetrics();
        List<String> ls = envolver(TEXTOS[i], fm, t.width);
        int ty = t.y + (t.height - ls.size() * fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(ArteMj.TEXTO);
        for (String l : ls) {
            g.drawString(l, t.x, ty);
            ty += fm.getHeight();
        }
        if (puesto) {
            ArteMj.palomita(g, rr.x + rr.width - 64, rr.getCenterY(), 44);
        }
    }

    private void dibujarSueltos(Graphics2D g) {
        ArteMj.panel(g, 1020, 140, 860, 720, new Color(255, 244, 222, 200));
        g.setFont(ArteMj.letra(32f));
        ArteMj.centrado(g, "Pedazos sueltos: toca uno y luego su casilla", 1450, 182, ArteMj.TEXTO);
        Color[] cintas = {ArteMj.ROSA, ArteMj.AMARILLO, ArteMj.VERDE, ArteMj.AZUL, ArteMj.MORADO, ArteMj.CORAL};
        for (int k = 0; k < sueltos.size(); k++) {
            int pz = sueltos.get(k);
            if (pz < 0) {
                continue;
            }
            Rectangle r = puestoSuelto(k);
            Rectangle rr = new Rectangle(r.x + 8, r.y + 26, r.width - 16, r.height - 40);
            pedazo(g, pz, rr, pz == elegido, false);
            ArteMj.cinta(g, rr.getCenterX(), rr.y + (pz == elegido ? -12 : 0), 90, cintas[k % cintas.length], (k % 2 == 0 ? -6 : 5));
        }
        ArteMj.pegar(g, "cinco_noticia", "noticia_nota_adhesiva", 1800, 900, 120);
    }

    private void dibujarAudio(Graphics2D g) {
        int x = 1030, y = 150, w = 840, h = 330;
        ArteMj.globo(g, x + 20, y, w - 20, h, -1, ArteMj.VERDE_CLARO);
        if (!ArteMj.pegar(g, "rastrea", "rastrea_audio_reproducir_015", x + 230, y + 52, 74)) {
            g.setFont(ArteMj.letra(26f));
            g.setColor(ArteMj.TEXTO);
            g.drawString("Audio sin nombre", x + 60, y + 60);
        }
        g.setFont(ArteMj.letra(22f));
        g.setColor(new Color(0x7A5A3A));
        g.drawString("40 pulgares arriba", x + 430, y + 62);
        g.setColor(new Color(0xB0562E));
        g.drawString("Toca la palabra del audio que choca con la REGLA.", x + 60, y + 108);
        String[] ws = AUDIO.split(" ");
        List<Rectangle> rs = palabrasAudio();
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        for (int i = 0; i < ws.length; i++) {
            Rectangle r = rs.get(i);
            if (i == palabra) {
                ArteMj.pildora(g, r.x - 2, r.y + 4, r.width + 4, r.height - 6, ArteMj.AMARILLO, 3f);
            }
            g.setColor(ArteMj.TEXTO);
            g.drawString(ws[i], r.x + 6, r.y + 37);
        }
        ArteMj.pegar(g, "cinco_noticia", "noticia_microfono", x + w - 60, y + h - 40, 120);

        ArteMj.panel(g, 1020, 500, 860, 455, new Color(255, 244, 222, 215));
        g.setFont(ArteMj.letra(30f));
        ArteMj.centrado(g, "¿Qué le falta al audio?", 1450, 540, ArteMj.TEXTO);
        for (int f = 0; f < 5; f++) {
            Rectangle a = botonMarca(f, 0);
            etiqueta(g, f, 1170, a.y + a.height / 2, 76);
            for (int op = 0; op < 2; op++) {
                Rectangle r = botonMarca(f, op);
                if (marca[f] == op) {
                    ArteMj.estado(g, r, op == 0 ? "Está" : "Falta", op == 0 ? 0 : 1);
                } else {
                    ArteMj.opcion(g, r, op == 0 ? "está" : "falta", false);
                }
            }
        }
        ArteMj.boton(g, verificar.x, verificar.y, verificar.width, verificar.height, "Verificar", marcasCompletas() ? 0 : 2);
    }
}
