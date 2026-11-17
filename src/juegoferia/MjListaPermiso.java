package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Path2D;

/**
 * Minijuego fijo de la JUNTA del Dia 2: "La lista con permiso". Lo juega Ciudadano.
 * (guion 2.5 y prompt del generador 6.8 · fondo: junta_piso1.png · arte: /imagenes/minijuegos/lista_permiso/)
 *
 * Amaranta entrega la lista de afiliados (8 nombres) y la hoja de permisos. Si el nombre no esta
 * en la hoja CON SU FIRMA, no dio permiso. Cada nombre se marca: "firmó la hoja de permiso" o "no figura".
 * Correcto: firmaron Doña Petra, Wilson y Emiro. No figuran los otros cinco.
 *
 * BIEN: los ocho bien.  REGULAR: seis o siete.  MAL: cinco o menos.
 */
public class MjListaPermiso extends Minijuego {

    public static final String[] NOMBRES = {"Don Alirio", "Doña Petra", "Wilson", "Don Gustavo", "Santa Sofía", "Emiro", "Chela", "Daniela"};
    /** id del sprite oficial para el marco circular de cada renglon (Rutas.spriteVecino). */
    private static final String[] SPRITE = {"alirio", "petra", "wilson", "gustavo", "sofia", "emiro", "chela", "daniela"};
    private static final boolean[] FIRMO = {false, true, true, false, false, true, false, false};

    /** La hoja de permisos: nombre, ¿tiene firma?, fecha. Hay gente que no esta en la lista y renglones sin firma. */
    private static final String[][] HOJA = {
        {"Doña Petra", "si", "2 de marzo"},
        {"Wilson", "si", "2 de marzo"},
        {"Don Gustavo", "no", ""},
        {"Emiro", "si", "3 de marzo"},
        {"Rebeca", "si", "3 de marzo"},
        {"Santa Sofía", "no", ""},
        {"Pilar", "si", "4 de marzo"}
    };

    private final boolean pista;     // alguien verifico el grupo: los dos primeros ya vienen marcados
    private final int[] marca = new int[8];    // -1 nada, 0 firmo, 1 no figura
    private int consejo;

    public MjListaPermiso(boolean pista) {
        this.pista = pista;
    }

    @Override
    public String nombre() {
        return "La lista con permiso";
    }

    @Override
    public String instrucciones() {
        return "Mira el nombre y búscalo en la hoja de permisos. Si el nombre no está, o está sin firma, no dio permiso. "
                + "Clasifica a los 8 de la lista: «firmó la hoja de permiso» o «no figura».";
    }

    @Override
    protected String carpetaArte() {
        return "lista_permiso";
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "lista_permiso", "permiso_registro_de_firmas", cx, cy, 210)) {
            ArteMj.hojaPapel(g, cx - 70, cy - 90, 140, 180, ArteMj.CREMA);
            firma(g, cx - 45, cy - 10, 90, 3);
        }
        ArteMj.lupa(g, cx + 75, cy + 80, 100);
    }

    @Override
    protected void prepararPartida() {
        for (int i = 0; i < 8; i++) {
            marca[i] = -1;
        }
        if (pista) {
            marca[0] = 1;
            marca[1] = 0;
        }
        consejo = 0;
    }

    private static Rectangle rBoton(int fila, int op) {
        return new Rectangle(500 + op * 230, 205 + fila * 92, 215, 74);
    }

    private final Rectangle verificar = new Rectangle(1500, 975, 360, 88);

    @Override
    protected void actualizarJuego(double dt) {
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        for (int f = 0; f < 8; f++) {
            if (pista && f < 2) {
                continue;
            }
            for (int op = 0; op < 2; op++) {
                if (rBoton(f, op).contains(p)) {
                    marca[f] = op;
                    consejo++;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    return;
                }
            }
        }
        if (verificar.contains(p) && completa()) {
            int bien = 0;
            for (int f = 0; f < 8; f++) {
                if ((marca[f] == 0) == FIRMO[f]) {
                    bien++;
                }
            }
            int nv = bien == 8 ? BIEN : bien >= 6 ? REGULAR : MAL;
            String r = nv == BIEN
                    ? "Ciudadano: Tres dieron permiso y cinco no. El grupo se armó antes de preguntar. Amaranta: Gracias por leerlo con cuidado."
                    : nv == REGULAR ? "Amaranta: Casi. Revise los dos últimos."
                    : "Amaranta: Se lo muestro yo.";
            terminarJuego(nv != MAL, nv,
                    nv == BIEN ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1, EstadoJuego.CONFIANZA, 2, Partida.DESINFO_LUGAR, -3)
                            : nv == REGULAR ? Efecto.de(EstadoJuego.CONFIANZA, 1) : new Efecto(),
                    r + " (" + bien + " de 8)");
        }
    }

    private boolean completa() {
        for (int m : marca) {
            if (m < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        ArteMj.fondoOficial(g, Rutas.JUNTA_COCINA, 100);
        String[] cs = {"Busca la firma.", "Fíjate en la fecha.", "Si no figura, márcalo.", "Un renglón sin firma no es un permiso."};
        cabecera(g, nombre(), cs[consejo % cs.length]);

        // ---- lista de afiliados ----
        ArteMj.hojaPapel(g, 50, 140, 950, 830, ArteMj.CREMA);
        ArteMj.pegar(g, "lista_permiso", "permiso_lista_de_acceso", 930, 170, 110);
        g.setFont(ArteMj.letra(30f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("Lista de afiliados · Junta de Acción Comunal", 90, 186);
        for (int f = 0; f < 8; f++) {
            int y = 205 + f * 92;
            g.setColor(new Color(214, 190, 150));
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(90, y + 84, 970, y + 84);
            ArteMj.cara(g, 124, y + 38, 36, NOMBRES[f], null);
            g.setFont(ArteMj.letra(31f));
            g.setColor(ArteMj.TEXTO);
            g.drawString(NOMBRES[f], 176, y + 50);
            for (int op = 0; op < 2; op++) {
                Rectangle r = rBoton(f, op);
                Rectangle rr = new Rectangle(r.x, r.y + 6, r.width, r.height - 10);
                if (marca[f] == op) {
                    ArteMj.estado(g, rr, op == 0 ? "Firmó" : "No figura", op == 0 ? 0 : 1);
                } else {
                    ArteMj.opcion(g, rr, op == 0 ? "firmó" : "no figura", false);
                }
            }
        }

        // ---- hoja de permisos ----
        ArteMj.hojaPapel(g, 1050, 140, 820, 790, new Color(0xFFF8E6));
        ArteMj.pegar(g, "lista_permiso", "permiso_hoja_firma", 1790, 210, 110);
        g.setFont(ArteMj.letra(34f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("Hoja de permiso", 1090, 200);
        g.setFont(Estilo.fuente(Font.ITALIC, 24f));
        g.setColor(new Color(0x7A5A3A));
        g.drawString("«Autorizo que me agreguen al grupo de la Junta.»", 1090, 240);
        ArteMj.pildora(g, 1080, 262, 760, 48, ArteMj.AMARILLO_CLARO, 3.5f);
        g.setFont(ArteMj.letra(24f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("Nombre", 1110, 295);
        g.drawString("Firma", 1410, 295);
        g.drawString("Fecha", 1670, 295);
        for (int i = 0; i < HOJA.length; i++) {
            int y = 360 + i * 80;
            g.setColor(new Color(214, 190, 150));
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(1090, y + 22, 1840, y + 22);
            g.setFont(Estilo.fuente(Font.PLAIN, 30f));
            g.setColor(ArteMj.TEXTO);
            g.drawString(HOJA[i][0], 1100, y);
            if (HOJA[i][1].equals("si")) {
                firma(g, 1400, y - 26, 200, i + 7);
            } else {
                g.setColor(new Color(0xB89A72));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 9f}, 0f));
                g.drawLine(1400, y, 1600, y);
            }
            g.setFont(Estilo.fuente(Font.PLAIN, 26f));
            g.setColor(ArteMj.TEXTO);
            g.drawString(HOJA[i][2], 1670, y);
        }
        ArteMj.lupa(g, 1110, 880, 100);
        ArteMj.boton(g, verificar.x, verificar.y, verificar.width, verificar.height, "Verificar", completa() ? 0 : 2);
    }

    /** Firma a mano (un garabato distinto para cada renglon). */
    private static void firma(Graphics2D g, int x, int y, int w, long semilla) {
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + 20);
        int n = 6;
        for (int i = 1; i <= n; i++) {
            double px = x + w * i / (double) n;
            p.curveTo(px - w / 9.0, y - 4 + ArteMj.ruido(semilla + i) * 10, px - w / 18.0, y + 36 + ArteMj.ruido(semilla + 2 * i) * 8,
                    px, y + 18 + ArteMj.ruido(semilla + 3 * i) * 10);
        }
        g.setColor(new Color(0x2B3F7A));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }
}
