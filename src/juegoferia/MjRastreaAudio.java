package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Minijuego fijo de la PANADERIA: "Rastrea el audio". Lo juega Ciudadano con el celular de Dona Rosa.
 *
 * El rumor es un ARBOL de reenvios (Nodo): la raiz es el origen y cada hijo es un reenvio.
 * Se empieza en una hoja (el mensaje que le llego a Dona Rosa) y hay que SUBIR hasta la raiz:
 *   1. Elegir de quien le llego el mensaje (el padre) entre tres chats. Tres errores = queda a medio camino.
 *   2. Comparar con el de arriba y marcar las palabras que cambiaron.
 *
 * BIEN: llega al origen y marca 3 o mas palabras que cambiaron.  REGULAR: llega sin marcarlas.
 * MAL: se queda a medio camino.
 */
public class MjRastreaAudio extends Minijuego {

    private enum Paso { ELEGIR_PADRE, MARCAR }

    private final boolean pista;   // alguien verifico: el primer eslabon ya viene marcado
    private final boolean lupa;    // Periodista presto la lupa: las palabras cambiadas se ven en amarillo

    private Nodo raiz, actual;
    private Paso paso;
    private final List<Nodo> candidatos = new ArrayList<>();
    private final Set<Integer> marcadas = new HashSet<>();
    private int errores, buenas, malas;
    private String aviso = "";
    private final Rectangle subir = new Rectangle(1500, 940, 360, 90);
    private int consejo;

    public MjRastreaAudio(boolean pista, boolean lupa) {
        this.pista = pista;
        this.lupa = lupa;
    }

    @Override
    public String nombre() {
        return "Rastrea el audio";
    }

    @Override
    public String instrucciones() {
        return "Doña Rosa te pasa su celular. Sube por los reenvíos hasta encontrar de dónde salió el audio. "
                + "En cada paso elige de quién llegó el mensaje y marca las palabras que cambiaron.";
    }

    /** El arbol de reenvios del Dia 1 (10 a 12 chats). La raiz es el origen. */
    public static Nodo arbolDelAudio() {
        Nodo raiz = new Nodo("vlado", "Noticiero de Vlado", "Se rumora que Aguado cerraría el colegio si gana.");
        Nodo yolanda = raiz.agregarHijo(new Nodo("yolanda", "Yolanda", "Dicen que Aguado cerraría el colegio si gana."));
        raiz.agregarHijo(new Nodo("ursula", "Doña Úrsula", "Esto ya lo viví, aunque no sabría decir cuándo."));
        yolanda.agregarHijo(new Nodo("mateo", "Mateo", "¿Aguado cerraría el colegio? ¿De dónde sacan eso?"));
        Nodo nubia = yolanda.agregarHijo(new Nodo("nubia", "Nubia (audio)", "Dicen que Aguado cierra el colegio en enero."));
        nubia.agregarHijo(new Nodo("lorena", "Lorena", "¿Es verdad lo del colegio?"));
        nubia.agregarHijo(new Nodo("wilson", "Wilson", "Ojo: Aguado cierra el colegio en enero."));
        Nodo barrio = nubia.agregarHijo(new Nodo("barrio", "Grupo del barrio", "Dicen que cierran el colegio en enero."));
        barrio.agregarHijo(new Nodo("eliecer", "Don Eliécer", "Pues si lo cierran, por algo será."));
        barrio.agregarHijo(new Nodo("camila", "Camila", "¿Mi colegio?"));
        barrio.agregarHijo(new Nodo("rosa", "Grupo de la panadería", "Cierran el colegio en enero. Pásalo."));
        return raiz;
    }

    @Override
    protected void prepararPartida() {
        raiz = arbolDelAudio();
        actual = null;
        for (Nodo n : Recorridos.recorridoDFS(raiz)) {
            if (n.id.equals("rosa")) {
                actual = n;
            }
        }
        errores = buenas = malas = consejo = 0;
        aviso = "";
        marcadas.clear();
        if (pista) {
            paso = Paso.MARCAR;   // el primer eslabon ya esta encontrado
        } else {
            prepararCandidatos();
        }
    }

    private void prepararCandidatos() {
        paso = Paso.ELEGIR_PADRE;
        candidatos.clear();
        candidatos.add(actual.padre);
        List<Nodo> todos = Recorridos.recorridoBFS(raiz);
        Collections.shuffle(todos, Sincronia.nuevoRandom());
        for (Nodo n : todos) {
            if (candidatos.size() >= 3) {
                break;
            }
            if (n != actual && n != actual.padre && !candidatos.contains(n)) {
                candidatos.add(n);
            }
        }
        Collections.shuffle(candidatos, Sincronia.nuevoRandom());
    }

    // ---- palabras ----

    private static String[] palabras(String s) {
        return s.split(" ");
    }

    private static String normal(String w) {
        return w.toLowerCase().replaceAll("[^\\p{L}]", "");
    }

    /** Indices de las palabras del hijo que no estan en el padre. */
    private static Set<Integer> cambiadas(Nodo hijo) {
        Set<String> delPadre = new HashSet<>();
        for (String w : palabras(hijo.padre.descripcion)) {
            delPadre.add(normal(w));
        }
        Set<Integer> r = new HashSet<>();
        String[] ws = palabras(hijo.descripcion);
        for (int i = 0; i < ws.length; i++) {
            if (!normal(ws[i]).isEmpty() && !delPadre.contains(normal(ws[i]))) {
                r.add(i);
            }
        }
        return r;
    }

    // ---- juego ----

    @Override
    protected void actualizarJuego(double dt) {
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        if (paso == Paso.ELEGIR_PADRE) {
            for (int i = 0; i < candidatos.size(); i++) {
                if (rectCandidato(i).contains(p)) {
                    if (candidatos.get(i) == actual.padre) {
                        aviso = "¡Ese es! Ahora compara los dos mensajes.";
                        paso = Paso.MARCAR;
                        marcadas.clear();
                    } else {
                        errores++;
                        aviso = "Ese no. Te quedan " + (3 - errores) + " intentos.";
                        if (errores >= 3) {
                            terminarJuego(false, MAL, new Efecto(),
                                    "Quedaste a medio camino. Vlado lo aclara desde la tele: "
                                    + "«Yo dije 'se rumora'. No dije 'se confirma'».");
                        }
                    }
                    return;
                }
            }
            return;
        }
        // MARCAR
        List<Rectangle> rectPalabras = disponer(actual, 160, 520, 1600);
        for (int i = 0; i < rectPalabras.size(); i++) {
            if (rectPalabras.get(i).contains(p)) {
                if (!marcadas.remove(i)) {
                    marcadas.add(i);
                }
                return;
            }
        }
        if (subir.contains(p)) {
            Set<Integer> cambio = cambiadas(actual);
            for (int i : marcadas) {
                if (cambio.contains(i)) {
                    buenas++;
                } else {
                    malas++;
                }
            }
            actual = actual.padre;
            consejo++;
            if (actual.esRaiz()) {
                boolean bien = buenas >= 3 && malas <= 2;
                terminarJuego(true, bien ? BIEN : REGULAR,
                        bien ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, Partida.DESINFO_LUGAR, -5)
                                : Efecto.de(EstadoJuego.INFO_VERIFICADA, 1),
                        bien ? "La tele dijo «se rumora». El segundo dijo «dicen». El tercero ya dice «cierran en enero»."
                                : "Llegaste al origen. Falta ver qué cambió por el camino.");
            } else {
                aviso = "";
                prepararCandidatos();
            }
        }
    }

    private Rectangle rectCandidato(int i) {
        return new Rectangle(160 + i * 560, 560, 520, 260);
    }

    // ---- dibujo ----

    @Override
    protected void dibujarJuego(Graphics2D g) {
        fondo(g, "/imagenes/minijuegos/rastrea_audio.png");
        String[] cs = {"Sube un nivel.", "Mira la palabra «dicen».", "Compara con el de arriba."};
        cabecera(g, nombre(), cs[consejo % cs.length]);
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(Estilo.TEXTO);
        g.drawString("Nivel " + actual.nivel() + " · palabras bien marcadas: " + buenas + " · intentos fallidos: " + errores,
                60, 140);

        if (paso == Paso.ELEGIR_PADRE) {
            mensaje(g, actual, 160, 200, 1600, false);
            g.setFont(Estilo.fuente(Font.BOLD, 40f));
            g.setColor(Estilo.TEXTO);
            g.drawString("¿De quién le llegó?", 160, 520);
            for (int i = 0; i < candidatos.size(); i++) {
                Rectangle r = rectCandidato(i);
                mensajeCaja(g, candidatos.get(i), r);
            }
        } else {
            g.setFont(Estilo.fuente(Font.BOLD, 30f));
            g.setColor(Estilo.TEXTO_SUAVE);
            g.drawString("De arriba (de quien lo recibió):", 160, 200);
            mensaje(g, actual.padre, 160, 220, 1600, false);
            g.drawString("Este mensaje. Toca las palabras que cambiaron:", 160, 500);
            mensaje(g, actual, 160, 520, 1600, true);
            boton(g, subir, actual.padre.esRaiz() ? "Llegar al origen" : "Subir un nivel", true);
        }
        if (!aviso.isEmpty()) {
            g.setFont(Estilo.fuente(Font.BOLD, 30f));
            g.setColor(Estilo.ROJO);
            g.drawString(aviso, 160, 900);
        }
    }

    private void mensajeCaja(Graphics2D g, Nodo n, Rectangle r) {
        Estilo.pergamino(g, r.x, r.y, r.width, r.height, 26);
        g.setColor(Estilo.MADERA_OSCURA);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 26, 26);
        g.setFont(Estilo.fuente(Font.BOLD, 28f));
        g.drawString(n.tipo, r.x + 24, r.y + 44);
        g.setFont(Estilo.fuente(Font.PLAIN, 26f));
        g.setColor(Estilo.TEXTO);
        FontMetrics fm = g.getFontMetrics();
        int y = r.y + 90;
        for (String l : envolver(n.descripcion, fm, r.width - 48)) {
            g.drawString(l, r.x + 24, y);
            y += fm.getHeight();
        }
    }

    /**
     * Donde va cada palabra. Se calcula con un ancho fijo por letra (NO con la fuente) para que en red las
     * cuatro computadoras tengan exactamente las mismas zonas de clic, sin importar su fuente ni su pantalla.
     */
    private static List<Rectangle> disponer(Nodo n, int x, int y, int w) {
        List<Rectangle> r = new ArrayList<>();
        int cx = x + 30, cy = y + 110;
        for (String pal : palabras(n.descripcion)) {
            int ww = pal.length() * 22;
            if (cx + ww > x + w - 30) {
                cx = x + 30;
                cy += 56;
            }
            r.add(new Rectangle(cx - 6, cy - 36, ww + 12, 46));
            cx += ww + 18;
        }
        return r;
    }

    /** Dibuja un mensaje palabra por palabra. Si clicable, marca las cambiadas (lupa) y las elegidas. */
    private void mensaje(Graphics2D g, Nodo n, int x, int y, int w, boolean clicable) {
        Estilo.pergamino(g, x, y, w, 220, 30);
        g.setFont(Estilo.fuente(Font.BOLD, 28f));
        g.setColor(Estilo.TEXTO_SUAVE);
        g.drawString(n.tipo, x + 30, y + 44);
        g.setFont(Estilo.fuente(Font.PLAIN, 40f));
        Set<Integer> cambio = clicable && lupa ? cambiadas(n) : Collections.emptySet();
        String[] ws = palabras(n.descripcion);
        List<Rectangle> rs = disponer(n, x, y, w);
        for (int i = 0; i < ws.length; i++) {
            Rectangle r = rs.get(i);
            if (clicable) {
                if (cambio.contains(i)) {
                    g.setColor(new Color(255, 230, 90, 140));
                    g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                }
                if (marcadas.contains(i)) {
                    g.setColor(Estilo.ROJO);
                    g.setStroke(new BasicStroke(4f));
                    g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
                }
            }
            g.setColor(Estilo.TEXTO);
            g.drawString(ws[i], r.x + 6, r.y + 36);
        }
    }
}
