package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Minijuego fijo de la ALCALDIA del Dia 2: "El recorrido de la circular". Lo juega Influencer.
 * (guion 2.3 y prompt del generador 6.6 · fondo: interioralcaldia.png · arte: /imagenes/minijuegos/circular/)
 *
 * Es un GRAFO: 8 oficinas en 3 pisos, unidas por PASILLOS (mismo piso) y ESCALERAS (entre pisos).
 * El audio empieza en Atencion al publico y salta un paso por turno por cualquier pasillo o escalera.
 * En cada turno se pone UNA copia de la circular: tapa esa oficina y las que le quedan al lado
 * en el mismo piso. Luego el audio salta. Con las 3 copias puestas, el audio sigue hasta donde pueda.
 *
 * BIEN: quedan limpias 6 de 8 o mas (tres cuartas partes). REGULAR: la mitad o mas. MAL: menos.
 */
public class MjRecorridoCircular extends Minijuego {

    public static final String[] OFICINAS = {
        "Atención al público", "Ventanilla de Marisol", "Archivo",
        "Contratación", "Tesorería", "Secretaría",
        "Despacho de Quiroga", "Sala de juntas"
    };
    private static final int[] PISO = {1, 1, 1, 2, 2, 2, 3, 3};
    /** Pasillos: mismo piso. Una copia tapa la oficina y sus vecinas de pasillo. */
    public static final int[][] PASILLOS = {{0, 1}, {1, 2}, {3, 4}, {4, 5}, {6, 7}};
    /** Escaleras: entre pisos. Por aqui el audio "salta de piso en piso". */
    public static final int[][] ESCALERAS = {{0, 3}, {1, 4}, {2, 5}, {4, 6}, {5, 7}};
    public static final int INICIO = 0;
    private static final int COPIAS = 3;

    private final boolean pista;   // alguien verifico: ya hay una copia puesta en el primer piso

    private final Set<Integer> contagiada = new HashSet<>();
    private final Set<Integer> protegida = new HashSet<>();
    private final List<Integer> copias = new ArrayList<>();
    private int quedan;
    private int turno;
    private double espera;          // segundos hasta el proximo salto del audio
    private boolean soltando;       // ya no quedan copias: el audio sigue solo
    private final List<int[]> saltos = new ArrayList<>();   // los ultimos saltos (para dibujarlos)
    private double tSaltos;
    private int consejo;
    private String aviso = "";

    public MjRecorridoCircular(boolean pista) {
        this.pista = pista;
    }

    @Override
    public String nombre() {
        return "El recorrido de la circular";
    }

    @Override
    public String instrucciones() {
        return "Cada copia tapa una oficina y las que le quedan al lado en el mismo piso. El audio salta de piso en piso, "
                + "un paso por turno. Tienes 3 copias: decide dónde ponerlas para cortar el rumor.";
    }

    @Override
    protected String carpetaArte() {
        return "circular";
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "circular", "circular_edificio_alcaldia", cx, cy, 150)) {
            copiaCircular(g, cx - 60, cy - 80, 120, 160, 1);
        } else {
            ArteMj.pegar(g, "circular", "circular_documento", cx + 60, cy + 90, 110);
        }
    }

    @Override
    protected void prepararPartida() {
        contagiada.clear();
        protegida.clear();
        copias.clear();
        contagiada.add(INICIO);
        quedan = COPIAS;
        turno = 1;
        espera = 0;
        soltando = false;
        saltos.clear();
        consejo = 0;
        aviso = "El audio sonó en Atención al público. Pon tu primera copia.";
        if (pista) {
            tapar(1);     // "una copia ya colocada en el primer piso" (Ventanilla de Marisol)
        }
    }

    public static List<Integer> vecinos(int o, boolean conEscaleras) {
        List<Integer> r = new ArrayList<>();
        for (int[] e : PASILLOS) {
            if (e[0] == o) {
                r.add(e[1]);
            } else if (e[1] == o) {
                r.add(e[0]);
            }
        }
        if (conEscaleras) {
            for (int[] e : ESCALERAS) {
                if (e[0] == o) {
                    r.add(e[1]);
                } else if (e[1] == o) {
                    r.add(e[0]);
                }
            }
        }
        return r;
    }

    private void tapar(int o) {
        copias.add(o);
        protegida.add(o);
        for (int v : vecinos(o, false)) {
            if (!contagiada.contains(v)) {
                protegida.add(v);
            }
        }
    }

    /** Un salto del audio: cada oficina contagiada pasa el rumor a sus vecinas no protegidas. */
    private boolean saltar() {
        List<int[]> nuevos = new ArrayList<>();
        Set<Integer> nuevas = new HashSet<>();
        for (int o : contagiada) {
            for (int v : vecinos(o, true)) {
                if (!contagiada.contains(v) && !protegida.contains(v) && nuevas.add(v)) {
                    nuevos.add(new int[]{o, v});
                }
            }
        }
        contagiada.addAll(nuevas);
        if (!nuevos.isEmpty()) {
            saltos.clear();
            saltos.addAll(nuevos);
            tSaltos = 0;
        }
        return !nuevas.isEmpty();
    }

    // ---- zonas fijas del plano ----

    private static Rectangle oficina(int o) {
        int w = 270, h = 160;
        switch (o) {
            case 0: return new Rectangle(260, 750, w, h);
            case 1: return new Rectangle(640, 750, w, h);
            case 2: return new Rectangle(1020, 750, w, h);
            case 3: return new Rectangle(260, 480, w, h);
            case 4: return new Rectangle(640, 480, w, h);
            case 5: return new Rectangle(1020, 480, w, h);
            case 6: return new Rectangle(450, 210, w, h);
            default: return new Rectangle(830, 210, w, h);
        }
    }

    // ---- juego ----

    @Override
    protected void actualizarJuego(double dt) {
        tSaltos += dt;
        if (espera > 0) {
            espera -= dt;
            if (espera <= 0) {
                boolean salto = saltar();
                if (soltando) {
                    if (salto) {
                        espera = 0.7;
                    } else {
                        terminar();
                    }
                } else {
                    turno++;
                    aviso = salto ? "El audio saltó. Turno " + turno + ": pon otra copia." : "El audio no pudo saltar. Turno " + turno + ".";
                }
            }
        }
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        if (espera > 0 || soltando) {
            return;
        }
        for (int o = 0; o < OFICINAS.length; o++) {
            if (oficina(o).contains(p)) {
                if (contagiada.contains(o)) {
                    aviso = "Ahí ya llegó el audio. Busca una oficina limpia.";
                    return;
                }
                if (copias.contains(o)) {
                    aviso = "Ahí ya hay una copia.";
                    return;
                }
                tapar(o);
                quedan--;
                consejo++;
                Funciones.reproducirSonido("/sonidos/click.wav");
                if (quedan == 0) {
                    soltando = true;
                    aviso = "Sin copias. El audio sigue solo...";
                } else {
                    aviso = "Copia puesta en " + OFICINAS[o] + ". El audio va a saltar...";
                }
                espera = 0.8;
                return;
            }
        }
    }

    private void terminar() {
        int limpias = OFICINAS.length - contagiada.size();
        int nv = limpias >= 6 ? BIEN : limpias >= 4 ? REGULAR : MAL;
        String r = nv == BIEN
                ? "Influencer: La circular corta el rumor donde se cruzan los pasillos. Mauricio: Eso es leerla antes de repartirla."
                : nv == REGULAR ? "Mauricio: Casi. Faltó cubrir el segundo piso."
                : "Mauricio: Ya se llenó el edificio. Se lo explico yo.";
        terminarJuego(nv != MAL, nv,
                nv == BIEN ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1, EstadoJuego.ALCANCE, 1, Partida.DESINFO_LUGAR, -3)
                        : nv == REGULAR ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1) : new Efecto(),
                r + " (" + limpias + " de 8 oficinas sin el rumor)");
    }

    // ---- dibujo ----

    @Override
    protected void dibujarJuego(Graphics2D g) {
        ArteMj.fondoOficial(g, Rutas.ALCALDIA_DIA2, 100);
        String[] cs = {"Pon una en la escalera.", "Esa oficina conecta con tres.", "Falta el segundo piso."};
        cabecera(g, nombre(), cs[consejo % cs.length]);

        // el plano de la Alcaldia
        ArteMj.panel(g, 70, 140, 1270, 800, new Color(255, 246, 226, 225));
        for (int piso = 1; piso <= 3; piso++) {
            int cy = piso == 1 ? 830 : piso == 2 ? 560 : 290;
            if (!ArteMj.pegar(g, "circular", "circular_piso_" + piso, 160, cy, 150)) {
                g.setFont(ArteMj.letra(28f));
                ArteMj.centrado(g, "Piso " + piso, 160, cy, ArteMj.TEXTO);
            }
        }
        // pasillos (mismo piso) y escaleras (entre pisos)
        for (int[] e : PASILLOS) {
            Rectangle a = oficina(e[0]), b = oficina(e[1]);
            ArteMj.pildora(g, a.x + a.width - 10, (int) a.getCenterY() - 14, b.x - a.x - a.width + 20, 28, ArteMj.CREMA_OSCURA, 3.5f);
        }
        for (int[] e : ESCALERAS) {
            Rectangle a = oficina(e[0]), b = oficina(e[1]);
            double x1 = a.getCenterX(), y1 = a.y, x2 = b.getCenterX(), y2 = b.getMaxY();
            java.awt.geom.Line2D l = new java.awt.geom.Line2D.Double(x1, y1, x2, y2);
            ArteMj.sticker(g, new BasicStroke(26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(l),
                    new Color(0xE9C99A), 3.5f);
            if (!ArteMj.pegar(g, "circular", "circular_escaleras", (x1 + x2) / 2, (y1 + y2) / 2, 74)) {
                g.setColor(ArteMj.TINTA);
                g.drawString("escalera", (int) ((x1 + x2) / 2) - 40, (int) ((y1 + y2) / 2));
            }
        }
        if (tSaltos < 1.2) {
            for (int[] sa : saltos) {
                Rectangle a = oficina(sa[0]), b = oficina(sa[1]);
                ArteMj.flecha(g, a.getCenterX(), a.getCenterY(), b.getCenterX(), b.getCenterY(), ArteMj.CORAL, 16, 30);
            }
        }
        for (int o = 0; o < OFICINAS.length; o++) {
            dibujarOficina(g, o);
        }

        // panel derecho: copias y avisos
        ArteMj.panel(g, 1380, 140, 500, 800, ArteMj.CREMA);
        g.setFont(ArteMj.letra(34f));
        ArteMj.centrado(g, "Copias: " + quedan + "/" + COPIAS, 1630, 200, ArteMj.TEXTO);
        for (int i = 0; i < COPIAS; i++) {
            int cx = 1470 + i * 160;
            if (i < quedan) {
                copiaCircular(g, cx - 60, 225, 120, 150, i + 2);
            } else {
                ArteMj.hueco(g, new Rectangle(cx - 55, 230, 110, 140), false);
            }
        }
        ArteMj.pildora(g, 1420, 400, 420, 56, ArteMj.AMARILLO_CLARO, 4f);
        g.setFont(ArteMj.letra(28f));
        ArteMj.centrado(g, soltando ? "El audio sigue solo" : "Turno " + turno, 1630, 438, ArteMj.TEXTO);
        g.setFont(Estilo.fuente(Font.PLAIN, 28f));
        ArteMj.parrafo(g, aviso, 1420, 510, 420, ArteMj.TEXTO);
        int limpias = OFICINAS.length - contagiada.size();
        ArteMj.pegar(g, "circular", limpias >= 6 ? "circular_lista_completada" : "circular_lista_pendiente", 1480, 760, 110);
        g.setFont(ArteMj.letra(30f));
        g.setColor(ArteMj.VERDE_OSCURO);
        g.drawString("Sin el rumor:", 1550, 745);
        g.drawString(limpias + " de 8", 1550, 785);
        g.setFont(Estilo.fuente(Font.PLAIN, 24f));
        ArteMj.parrafo(g, "Toca una oficina limpia para poner la copia.", 1420, 870, 420, new Color(0x7A5A3A));
    }

    private void dibujarOficina(Graphics2D g, int o) {
        Rectangle r = oficina(o);
        boolean c = contagiada.contains(o), p = protegida.contains(o), copia = copias.contains(o);
        Color fondo = c ? ArteMj.CORAL_CLARO : p ? ArteMj.VERDE_CLARO : ArteMj.CREMA;
        ArteMj.sticker(g, ArteMj.redondo(r.x, r.y, r.width, r.height, 26), fondo, 5f);
        // placa de la oficina (recurso) arriba
        ArteMj.pegarEn(g, "circular", "circular_puerta_oficina", new Rectangle(r.x + 12, r.y + 14, 70, 90));
        g.setFont(ArteMj.letra(25f));
        ArteMj.parrafo(g, OFICINAS[o], r.x + 92, r.y + 44, r.width - 104, ArteMj.TEXTO);
        if (c) {
            if (!ArteMj.pegar(g, "rastrea", "rastrea_megafono_rumor", r.x + r.width - 60, r.y + r.height - 50, 80)) {
                ArteMj.tachita(g, r.x + r.width - 50, r.y + r.height - 44, 50);
            }
            g.setFont(ArteMj.letra(20f));
            g.setColor(new Color(0xB0412E));
            g.drawString("le llegó el audio", r.x + 20, r.y + r.height - 18);
        } else if (copia) {
            copiaCircular(g, r.x + r.width - 92, r.y + r.height - 104, 74, 92, o);
        } else if (p) {
            ArteMj.palomita(g, r.x + r.width - 46, r.y + r.height - 42, 52);
        }
        if (o == INICIO) {
            ArteMj.pegar(g, "rastrea", "rastrea_pin_origen_megafono", r.x + 10, r.y + 10, 78);
        }
    }

    /** Copia de la circular (recurso circular_documento). */
    static void copiaCircular(Graphics2D g, int x, int y, int w, int h, long semilla) {
        if (ArteMj.pegarEn(g, "circular", "circular_documento", new Rectangle(x, y, w, h))) {
            return;
        }
        ArteMj.hojaPapel(g, x, y, w, h, ArteMj.CREMA);
        ArteMj.renglones(g, x + w / 6, y + h / 4, w * 2 / 3, 3, h / 6);
    }
}
