package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minijuego fijo del MALECON del Dia 2: "Quién mira". Lo juega Candidato.
 * (guion 2.4 y prompt del generador 6.7 · fondo: malecon.png · arte: /imagenes/minijuegos/quien_mira/)
 *
 * Hay dos camaras (la del poste y la del muro del conjunto) y cada una tiene su recorrido:
 *   CÁMARA -> DISCO -> TREINTA DÍAS -> ( ? ) -> QUIÉN LA PIDE -> QUIÉN LA VE
 * Se toca una ficha de la bandeja y luego la ranura de su camara. Entre ficha y ficha hay una flecha;
 * tocandola se cambia por el marcador "falta una flecha". La flecha que falta es la de en medio:
 * nadie dice quien pide ver la grabacion. Cada camara trae ademas una ficha que es puro rumor
 * ("RECONOCE CARAS"): esa no va en ningun recorrido.
 *
 * BIEN: los dos recorridos correctos.  REGULAR: uno.  MAL: ninguno.
 */
public class MjQuienMira extends Minijuego {

    public static final String[] FICHAS = {"CÁMARA", "DISCO", "TREINTA DÍAS", "QUIÉN LA PIDE", "QUIÉN LA VE", "RECONOCE CARAS"};
    private static final String[] CLAVE = {"camara", "disco", "treinta_dias", "quien_pide", "quien_ve", "reconoce_caras"};
    private static final String[] CAMARA = {"Cámara del poste", "Cámara del muro del conjunto"};
    private static final Color[] COLOR = {ArteMj.AZUL, ArteMj.CORAL};
    /** Lo correcto entre cada par de ranuras: true = flecha, false = "falta una flecha". */
    private static final boolean[] FLECHA_OK = {true, true, false, true};
    private static final int RANURAS = 5;

    private final boolean pista;    // alguien verifico la foto: la camara del poste ya viene puesta

    /** La bandeja: cada ficha es {camara, tipo}. null = ya se uso. */
    private final List<int[]> bandeja = new ArrayList<>();
    private final int[][] ranura = new int[2][RANURAS];      // tipo de ficha o -1
    private final boolean[][] flecha = new boolean[2][RANURAS - 1];
    private int elegida = -1;
    private int consejo;
    private String aviso = "";

    public MjQuienMira(boolean pista) {
        this.pista = pista;
    }

    @Override
    public String nombre() {
        return "Quién mira";
    }

    @Override
    public String instrucciones() {
        return "Arma el camino de la grabación: cámara, disco, treinta días, quién la pide y quién la ve. "
                + "Hay dos cámaras y cada una tiene su recorrido. Toca una flecha si crees que esa flecha falta.";
    }

    @Override
    protected String carpetaArte() {
        return "quien_mira";
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "quien_mira", "mira_camara_seguridad", cx, cy - 20, 170)) {
            icono(g, 0, cx, cy, 2.4);
        }
        ArteMj.pegar(g, "quien_mira", "mira_icono_ojo", cx + 70, cy + 90, 90);
    }

    @Override
    protected void prepararPartida() {
        bandeja.clear();
        for (int c = 0; c < 2; c++) {
            for (int t = 0; t < FICHAS.length; t++) {
                bandeja.add(new int[]{c, t});
            }
        }
        Collections.shuffle(bandeja, Sincronia.nuevoRandom());
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < RANURAS; i++) {
                ranura[c][i] = -1;
            }
            for (int i = 0; i < RANURAS - 1; i++) {
                flecha[c][i] = true;
            }
        }
        if (pista) {
            for (int k = 0; k < bandeja.size(); k++) {
                int[] f = bandeja.get(k);
                if (f != null && f[0] == 0 && f[1] == 0) {
                    ranura[0][0] = 0;
                    bandeja.set(k, null);
                }
            }
        }
        elegida = -1;
        consejo = 0;
        aviso = "";
    }

    // ---- zonas fijas ----

    private static Rectangle rRanura(int c, int i) {
        return new Rectangle(300 + i * 320, 190 + c * 300, 230, 150);
    }

    private static Rectangle rFlecha(int c, int i) {
        Rectangle a = rRanura(c, i);
        return new Rectangle(a.x + a.width + 4, a.y + 30, 82, 90);
    }

    private static Rectangle rBandeja(int k) {
        return new Rectangle(220 + (k % 6) * 270, 725 + (k / 6) * 110, 250, 100);
    }

    private final Rectangle verificar = new Rectangle(1520, 975, 340, 88);

    // ---- juego ----

    @Override
    protected void actualizarJuego(double dt) {
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        for (int k = 0; k < bandeja.size(); k++) {
            if (bandeja.get(k) != null && rBandeja(k).contains(p)) {
                elegida = elegida == k ? -1 : k;
                Funciones.reproducirSonido("/sonidos/click.wav");
                return;
            }
        }
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < RANURAS; i++) {
                if (rRanura(c, i).contains(p)) {
                    clicRanura(c, i);
                    return;
                }
            }
            for (int i = 0; i < RANURAS - 1; i++) {
                if (rFlecha(c, i).contains(p)) {
                    flecha[c][i] = !flecha[c][i];
                    consejo++;
                    Funciones.reproducirSonido("/sonidos/click.wav");
                    return;
                }
            }
        }
        if (verificar.contains(p) && completo()) {
            int bien = 0;
            for (int c = 0; c < 2; c++) {
                if (recorridoBien(c)) {
                    bien++;
                }
            }
            int nv = bien == 2 ? BIEN : bien == 1 ? REGULAR : MAL;
            String r = nv == BIEN
                    ? "Candidato: La cámara graba, el disco guarda treinta días. Y aquí falta una flecha: nadie dice quién pide ver ni quién ve. "
                    + "Orlando: Esa es la que nadie ha dibujado."
                    : nv == REGULAR ? "Orlando: Uno bien. Revise el de la otra cámara."
                    : "Orlando: Se lo cuento yo.";
            terminarJuego(nv != MAL, nv,
                    nv == BIEN ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, EstadoJuego.PARTICIPACION, 1, Partida.DESINFO_LUGAR, -3)
                            : nv == REGULAR ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1) : new Efecto(), r);
        }
    }

    private void clicRanura(int c, int i) {
        if (ranura[c][i] >= 0) {
            if (pista && c == 0 && i == 0) {
                return;     // la de la pista no se mueve
            }
            // devolver la ficha a la bandeja
            for (int k = 0; k < bandeja.size(); k++) {
                if (bandeja.get(k) == null) {
                    bandeja.set(k, new int[]{c, ranura[c][i]});
                    break;
                }
            }
            ranura[c][i] = -1;
            return;
        }
        if (elegida < 0) {
            aviso = "Primero toca una ficha de la bandeja.";
            return;
        }
        int[] f = bandeja.get(elegida);
        if (f[0] != c) {
            aviso = "Esa ficha es de la otra cámara (mira el color).";
            return;
        }
        ranura[c][i] = f[1];
        bandeja.set(elegida, null);
        elegida = -1;
        consejo++;
        aviso = "";
        Funciones.reproducirSonido("/sonidos/click.wav");
    }

    private boolean completo() {
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < RANURAS; i++) {
                if (ranura[c][i] < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean recorridoBien(int c) {
        for (int i = 0; i < RANURAS; i++) {
            if (ranura[c][i] != i) {
                return false;
            }
        }
        for (int i = 0; i < RANURAS - 1; i++) {
            if (flecha[c][i] != FLECHA_OK[i]) {
                return false;
            }
        }
        return true;
    }

    // ---- dibujo ----

    /** Recurso de cada ficha: camara de poste, disco, calendario de 30, registro, ojo, y la del rumor. */
    private static final String[] ICONO = {"mira_camara_seguridad", "mira_disco_almacenamiento", "mira_calendario_30_dias",
        "mira_registro_acceso", "mira_icono_ojo", "mira_alerta_sirena"};

    @Override
    protected void dibujarJuego(Graphics2D g) {
        ArteMj.fondoOficial(g, Rutas.MALECON_DIA2, 95);
        String[] cs = {"La cámara va primero.", "Después del disco va el tiempo.", "Falta quién la pide.",
            "¿Esa ficha es verdad o es rumor?"};
        cabecera(g, nombre(), cs[consejo % cs.length]);

        for (int c = 0; c < 2; c++) {
            Rectangle a = rRanura(c, 0);
            ArteMj.panel(g, 30, a.y - 30, 1860, a.height + 60, new Color(255, 246, 226, 205));
            if (!ArteMj.pegar(g, "quien_mira", "mira_etiqueta_camara_" + (c + 1), 160, a.getCenterY() - 22, 62)) {
                ArteMj.pildora(g, 70, (int) a.getCenterY() - 50, 180, 56, ArteMj.CREMA, 4f);
            }
            g.setFont(ArteMj.letra(21f));
            ArteMj.centrado(g, c == 0 ? "la del poste" : "la del muro", 160, a.getCenterY() + 40, COLOR[c].darker());
            for (int i = 0; i < RANURAS; i++) {
                Rectangle r = rRanura(c, i);
                if (ranura[c][i] < 0) {
                    ArteMj.hueco(g, r, elegida >= 0);
                    g.setFont(ArteMj.letra(44f));
                    ArteMj.centrado(g, String.valueOf(i + 1), r.getCenterX(), r.getCenterY() + 16, new Color(0xC9A57A));
                } else {
                    ficha(g, c, ranura[c][i], r, false, 31 * c + i);
                }
            }
            for (int i = 0; i < RANURAS - 1; i++) {
                Rectangle r = rFlecha(c, i);
                if (flecha[c][i]) {
                    ArteMj.flecha(g, r.x + 4, r.getCenterY(), r.getMaxX() - 2, r.getCenterY(), COLOR[c], 14, 0);
                } else if (!ArteMj.pegar(g, "pergaminos", "pergamino_etiqueta_pregunta", r.getCenterX(), r.getCenterY(), 58)) {
                    ArteMj.pregunta(g, r.getCenterX(), r.getCenterY(), 54);
                }
            }
        }

        // bandeja de fichas
        ArteMj.panel(g, 190, 680, 1690, 268, ArteMj.CREMA);
        g.setFont(ArteMj.letra(24f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("Fichas de Orlando: toca una y luego su lugar. Toca una flecha si crees que falta.", 230, 712);
        for (int k = 0; k < bandeja.size(); k++) {
            int[] f = bandeja.get(k);
            if (f == null) {
                continue;
            }
            Rectangle r = rBandeja(k);
            ficha(g, f[0], f[1], new Rectangle(r.x, r.y + 6, r.width, r.height - 6), k == elegida, k);
        }
        ArteMj.aviso(g, aviso, 1180, 118);
        ArteMj.boton(g, verificar.x, verificar.y, verificar.width, verificar.height, "Verificar", completo() ? 0 : 2);
    }

    private void ficha(Graphics2D g, int c, int t, Rectangle r, boolean levantada, long semilla) {
        int off = levantada ? -12 : 0;
        Rectangle rr = new Rectangle(r.x, r.y + off, r.width, r.height);
        ArteMj.sticker(g, ArteMj.redondo(rr.x, rr.y, rr.width, rr.height, 22), levantada ? ArteMj.AMARILLO_CLARO : ArteMj.CREMA, 4.5f);
        // franja del color de su camara
        g.setColor(COLOR[c]);
        g.fill(ArteMj.redondo(rr.x + 10, rr.y + 8, 14, rr.height - 16, 7));
        int ico = (int) Math.min(rr.height * 0.74, rr.width * 0.34);
        if (!ArteMj.pegar(g, "quien_mira", ICONO[t], rr.x + 30 + ico / 2.0, rr.getCenterY(), ico)) {
            icono(g, t, rr.x + 30 + ico / 2.0, rr.getCenterY(), rr.height / 150.0);
        }
        g.setFont(ArteMj.letra(rr.height > 120 ? 26f : 22f));
        ArteMj.textoEnCaja(g, FICHAS[t], new Rectangle(rr.x + 36 + ico, rr.y + 4, rr.width - 46 - ico, rr.height - 8), ArteMj.TEXTO);
    }

    /** Iconos de respaldo si faltan los recursos. */
    private static void icono(Graphics2D g, int t, double cx, double cy, double s) {
        double d = 56 * s;
        switch (t) {
            case 0:
            case 1:
            case 4:
                g.setColor(ArteMj.GRIS);
                g.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
                break;
            case 2:
                ArteMj.sticker(g, ArteMj.redondo(cx - d / 2, cy - d / 2, d, d, 8), ArteMj.CREMA, 3f);
                g.setFont(ArteMj.letra((float) (d * 0.45)));
                ArteMj.centrado(g, "30", cx, cy + d * 0.16, ArteMj.TEXTO);
                break;
            case 3:
                ArteMj.pregunta(g, cx, cy, d);
                break;
            default:
                ArteMj.tachita(g, cx, cy, d);
        }
    }
}
