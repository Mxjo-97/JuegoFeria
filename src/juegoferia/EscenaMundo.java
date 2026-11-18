package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Un lugar del juego armado CON CODIGO: fondo (una imagen sin personajes, o uno
 * provisional), actores con su propia colision, obstaculos, zonas y dialogos de texto.
 * No necesita mapas de colision pintados en Canva.
 *
 * Controles: WASD / flechas = caminar, E / ENTER / ESPACIO = interactuar o avanzar el dialogo,
 * F1 = ver las cajas de colision (para acomodarlas).
 *
 * Ejemplo:
 *   EscenaMundo m = new EscenaMundo();
 *   m.fondo("/imagenes/influencer/estudio.png");     // opcional
 *   m.caminable(80, 520, 1760, 520);                  // donde pueden pisar los pies
 *   m.obstaculo(1300, 520, 420, 140, "Escritorio");
 *   m.jugador(new Actor("yo", 900, 800, 150, 260));
 *   m.agregar(new Actor("dani", 500, 700, 150, 260).interactuar("Hablar", () -> m.dialogo(null, "Dani: Hola!")));
 */
public class EscenaMundo extends PanelDiseno {

    private static final class Zona {
        final Rectangle r;
        final String etiqueta;
        final Runnable accion;
        final boolean auto;
        boolean usada;

        Zona(Rectangle r, String etiqueta, Runnable accion, boolean auto) {
            this.r = r;
            this.etiqueta = etiqueta;
            this.accion = accion;
            this.auto = auto;
        }
    }

    private static final class Obstaculo {
        final Rectangle r;
        final String nombre;

        Obstaculo(Rectangle r, String nombre) {
            this.r = r;
            this.nombre = nombre;
        }
    }

    /** Marco de burbuja de dialogo hecho en Canva. Si no existe se dibuja el pergamino. */
    public static final String MARCO_BURBUJA = "/imagenes/ui/burbuja.png";

    private BufferedImage fondo;
    private final List<Actor> actores = new ArrayList<>();
    private final List<Obstaculo> obstaculos = new ArrayList<>();
    private final List<Zona> zonas = new ArrayList<>();
    private Actor jugador;
    private Rectangle caminable = new Rectangle(0, 0, W, H);
    private double velocidad = 340;

    /** Todo lo que se necesita para manejar UN personaje: sus teclas, y lo que tiene cerca. */
    private static final class Ctl {

        Actor actor;
        boolean izq, der, arr, abj, acomodado;
        Actor cerca;
        Zona zonaCerca;

        void soltar() {
            izq = der = arr = abj = false;
            if (actor != null) {
                actor.caminando = false;
            }
        }
    }

    private boolean verCajas, bloqueado;
    /** true = nadie camina (modo cinematica), pero si se puede hablar y decidir. */
    private boolean quieto;

    public void quieto(boolean q) {
        quieto = q;
    }
    /** Un solo PC: un teclado maneja a un personaje a la vez (el que camina). */
    private final Ctl principal = new Ctl();
    /** En red: cada jugador maneja el suyo (se llena con controlar()). */
    private final Ctl[] porRol = new Ctl[Rol.values().length];
    private boolean enRed;

    // ===================== DIALOGOS (en red: uno por jugador) =====================
    //
    // Un solo PC: hay un dialogo ('comun') y se ve en la unica pantalla, como siempre.
    // En red: cada rol tiene SU dialogo. Lo que pasa cuando un jugador habla con alguien
    // (o elige algo suyo) le sale solo a el; lo que es de la escena (sin dueño) le sale a
    // los cuatro, cada quien lo lee a su ritmo, y lo que sigue pasa cuando TODOS terminaron.
    // Todo esto se simula igual en las cuatro computadoras (lockstep); cada una solo DIBUJA el suyo.

    /** Un dialogo encolado: sus lineas, que pasa al final, de quien es y (si es de todos) su grupo. */
    private static final class Bloque {
        final List<String[]> lineas;
        final Runnable fin;
        final Rol de;
        final Grupo grupo;

        Bloque(List<String[]> lineas, Runnable fin, Rol de, Grupo grupo) {
            this.lineas = lineas;
            this.fin = fin;
            this.de = de;
            this.grupo = grupo;
        }
    }

    /** Un dialogo de la escena repartido a todos: 'fin' corre una vez, cuando ya nadie lo esta leyendo. */
    private static final class Grupo {
        final java.util.EnumSet<Rol> faltan = java.util.EnumSet.noneOf(Rol.class);
        final Runnable fin;

        Grupo(Runnable fin) {
            this.fin = fin;
        }
    }

    /** El dialogo de una pantalla. */
    private static final class Dlg {
        final LinkedList<String[]> cola = new LinkedList<>();
        final LinkedList<Bloque> espera = new LinkedList<>();
        Bloque actual;
        String quien, texto;
        double chars, adelantoT;
        double sinInteraccionHasta = -1;

        void vaciar() {
            cola.clear();
            espera.clear();
            actual = null;
            quien = texto = null;
        }
    }

    private final Dlg comun = new Dlg();
    private final Dlg[] dlgRol = new Dlg[Rol.values().length];
    private final List<Grupo> grupos = new ArrayList<>();
    /** De quien es lo que se esta procesando ahora (null = de la escena / de todos). Solo cuenta en red. */
    private Rol audiencia;

    {
        for (int i = 0; i < dlgRol.length; i++) {
            dlgRol[i] = new Dlg();
        }
    }

    public EscenaMundo() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tecla(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                tecla(e.getKeyCode(), false);
            }
        });
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point p = aDiseno(e.getPoint());
                if (ratonExtra(1, p)) {
                    return;
                }
                Dlg d = dlgDe(ctlDeEntrada());
                if (d.texto != null) {
                    avanzar(d);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ratonExtra(2, aDiseno(e.getPoint()));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                ratonExtra(0, aDiseno(e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                ratonExtra(0, aDiseno(e.getPoint()));
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
    }

    @Override
    protected boolean conAjustes() {
        return true;
    }

    // ===================== GANCHOS PARA SUBCLASES =====================

    /** tipo: 0 = mover, 1 = presionar, 2 = soltar. Devuelve true si lo consumio. */
    protected boolean ratonExtra(int tipo, Point p) {
        return false;
    }

    @Override
    protected boolean teclaSoloLocal(int codigo) {
        return codigo == KeyEvent.VK_F1;
    }

    /** Devuelve true si la tecla fue consumida (no mueve ni interactua). */
    protected boolean teclaExtra(int k, boolean pres) {
        return false;
    }

    /** Se dibuja encima del mundo y debajo del dialogo (HUD, niebla, menus). */
    protected void dibujarCapas(Graphics2D g) {
    }

    /** Si hay niebla, las placas muestran nombreNiebla. */
    protected boolean hayNiebla() {
        return false;
    }

    /** Hay ALGUN dialogo abierto (de cualquiera): el reloj y lo pendiente esperan. */
    protected boolean dialogoActivo() {
        if (comun.texto != null) {
            return true;
        }
        for (Dlg d : dlgRol) {
            if (d.texto != null) {
                return true;
            }
        }
        return false;
    }

    /** Hay un dialogo DE LA ESCENA abierto (el de todos). En red no cuentan las charlas personales. */
    protected boolean dialogoDeEscenaActivo() {
        return enRed ? !grupos.isEmpty() : comun.texto != null;
    }

    /** Hay dialogo en ESTA pantalla (para dibujar y esconder el HUD). */
    protected boolean dialogoVisible() {
        return dlgVista().texto != null;
    }

    /** Ese jugador esta leyendo algo (en un solo PC: el dialogo comun). */
    protected boolean dialogoActivoDe(Rol r) {
        return enRed && r != null ? dlgRol[r.ordinal()].texto != null : comun.texto != null;
    }

    /** De quien es lo que se esta procesando (null = de todos). Lugar lo usa para decisiones, burbujas y avisos. */
    protected Rol audiencia() {
        return enRed ? audiencia : null;
    }

    /** Corre 'r' como si fuera de ese jugador (null = de todos): sus dialogos y decisiones le saldran solo a el. */
    public void conAudiencia(Rol rol, Runnable r) {
        Rol antes = audiencia;
        audiencia = rol;
        try {
            r.run();
        } finally {
            audiencia = antes;
        }
    }

    /** En red: ese jugador no se puede mover (Lugar: tiene una decision suya abierta). */
    protected boolean congelado(Rol r) {
        return false;
    }

    /** Nombres de los que todavia leen un dialogo de la escena que YO ya termine (o null). */
    protected List<Rol> esperandoA() {
        if (!enRed) {
            return null;
        }
        Rol mi = Sincronia.miRol();
        for (Grupo gr : grupos) {
            if (!gr.faltan.isEmpty() && !gr.faltan.contains(mi) && dlgVista().texto == null) {
                return new ArrayList<>(gr.faltan);
            }
        }
        return null;
    }

    protected boolean estaBloqueado() {
        return bloqueado;
    }

    // ===================== ARMAR LA ESCENA =====================

    /** Fondo del lugar: usa la primera ruta que exista (ver Rutas). Si ninguna existe, se dibuja uno provisional. */
    public EscenaMundo fondo(String... rutas) {
        fondo = null;
        for (String r : rutas) {
            if (Recursos.existe(r)) {
                fondo = Recursos.fondo(r);
                break;
            }
        }
        return this;
    }

    /** Rectangulo por donde pueden pisar los pies del jugador. */
    public EscenaMundo caminable(int x, int y, int w, int h) {
        caminable = new Rectangle(x, y, w, h);
        return this;
    }

    public EscenaMundo obstaculo(int x, int y, int w, int h, String nombre) {
        obstaculos.add(new Obstaculo(new Rectangle(x, y, w, h), nombre));
        return this;
    }

    public EscenaMundo agregar(Actor a) {
        actores.add(a);
        return this;
    }

    public EscenaMundo jugador(Actor a) {
        jugador = a;
        principal.actor = a;
        principal.acomodado = false;
        if (!actores.contains(a)) {
            actores.add(a);
        }
        return this;
    }

    /** En red: este actor lo maneja el jugador con ese rol (todos se mueven a la vez, cada quien con su teclado). */
    public EscenaMundo controlar(Rol r, Actor a) {
        enRed = true;
        Ctl c = new Ctl();
        c.actor = a;
        porRol[r.ordinal()] = c;
        if (!actores.contains(a)) {
            actores.add(a);
        }
        return this;
    }

    /** true si en esta escena cada jugador maneja su propio personaje. */
    protected boolean modoRed() {
        return enRed;
    }

    private List<Ctl> ctls() {
        List<Ctl> r = new ArrayList<>(4);
        if (enRed) {
            for (Ctl c : porRol) {
                if (c != null) {
                    r.add(c);
                }
            }
        } else if (principal.actor != null) {
            r.add(principal);
        }
        return r;
    }

    /** El control del jugador de ESTA pantalla (para el cartel de "E" y lo que tiene cerca). */
    private Ctl vista() {
        if (enRed) {
            Rol mi = Sincronia.miRol();
            return mi != null ? porRol[mi.ordinal()] : null;
        }
        return principal;
    }

    /** El control al que va la tecla que se esta procesando. */
    private Ctl ctlDeEntrada() {
        if (enRed) {
            Rol r = Sincronia.inyectando() ? Sincronia.rolActual() : Sincronia.miRol();
            return r != null ? porRol[r.ordinal()] : null;
        }
        return principal;
    }

    /** Zona donde se pulsa E (ej: la puerta, el computador). */
    public EscenaMundo zona(int x, int y, int w, int h, String etiqueta, Runnable accion) {
        zonas.add(new Zona(new Rectangle(x, y, w, h), etiqueta, accion, false));
        return this;
    }

    /** Zona que se activa sola, una vez, cuando el jugador la pisa. */
    public EscenaMundo zonaAuto(int x, int y, int w, int h, Runnable accion) {
        zonas.add(new Zona(new Rectangle(x, y, w, h), null, accion, true));
        return this;
    }

    public void limpiar() {
        actores.clear();
        obstaculos.clear();
        zonas.clear();
        jugador = null;
        principal.actor = null;
        enRed = false;
        java.util.Arrays.fill(porRol, null);
        comun.vaciar();
        for (Dlg d : dlgRol) {
            d.vaciar();
        }
        grupos.clear();
        audiencia = null;
    }

    public void bloquear(boolean b) {
        bloqueado = b;
        for (Ctl c : ctls()) {
            c.soltar();
        }
    }

    public List<Actor> getActores() {
        return actores;
    }

    /** Busca un actor por su id ("rosa", "nubia"...). */
    public Actor actor(String id) {
        for (Actor a : actores) {
            if (a.id.equals(id)) {
                return a;
            }
        }
        return null;
    }

    public void quitar(Actor a) {
        actores.remove(a);
    }

    public Actor getJugador() {
        return jugador;
    }

    /**
     * Dialogo de texto. Cada linea es "Quien: texto". Al terminar llama a fin (puede ser null).
     * Ej: m.dialogo(() -> ..., "Dani: Hola!", "Yo: Que hay de nuevo?");
     * En red: si lo provoco un jugador (audiencia) le sale solo a el; si no, a todos.
     */
    public void dialogo(Runnable fin, String... lineas) {
        List<String[]> ls = new ArrayList<>();
        for (String l : lineas) {
            int i = l.indexOf(": ");
            if (l.startsWith("img:")) {
                ls.add(new String[]{"@img", l.substring(4)});
            } else if (i > 0 && i < 24) {
                ls.add(new String[]{l.substring(0, i), l.substring(i + 2)});
            } else {
                ls.add(new String[]{null, l});
            }
        }
        if (!enRed) {
            // un solo PC: como siempre (un dialogo nuevo reemplaza al que estaba)
            comun.espera.clear();
            empezar(comun, new Bloque(ls, fin, null, null));
            return;
        }
        Rol a = audiencia;
        if (a != null && porRol[a.ordinal()] != null) {
            encolar(dlgRol[a.ordinal()], new Bloque(ls, fin, a, null));
            return;
        }
        // de la escena: a los cuatro; 'fin' cuando todos terminen
        Grupo gr = new Grupo(fin);
        for (Rol r : Rol.values()) {
            if (porRol[r.ordinal()] != null) {
                gr.faltan.add(r);
            }
        }
        if (gr.faltan.isEmpty()) {
            if (fin != null) {
                fin.run();
            }
            return;
        }
        grupos.add(gr);
        for (Rol r : gr.faltan.toArray(new Rol[0])) {
            encolar(dlgRol[r.ordinal()], new Bloque(ls, null, r, gr));
        }
    }

    private void encolar(Dlg d, Bloque b) {
        if (d.actual == null && d.texto == null) {
            empezar(d, b);
        } else {
            d.espera.add(b);
        }
    }

    private void empezar(Dlg d, Bloque b) {
        d.actual = b;
        d.cola.clear();
        d.cola.addAll(b.lineas);
        siguienteLinea(d);
    }

    private void siguienteLinea(Dlg d) {
        if (d.cola.isEmpty()) {
            d.texto = null;
            d.quien = null;
            // pausa cortita: la misma tecla que cierra el dialogo no lo vuelve a abrir
            d.sinInteraccionHasta = tiempo + 0.45;
            Bloque b = d.actual;
            d.actual = null;
            terminarBloque(b);
            if (d.actual == null && d.texto == null && !d.espera.isEmpty()) {
                empezar(d, d.espera.poll());
            }
            return;
        }
        String[] l = d.cola.poll();
        d.quien = l[0];
        d.texto = l[1];
        d.chars = "@img".equals(l[0]) ? l[1].length() : 0;
        if (d == comun) {
            for (Ctl c : ctls()) {
                c.soltar();
            }
        } else {
            for (int i = 0; i < dlgRol.length; i++) {
                if (dlgRol[i] == d && porRol[i] != null) {
                    porRol[i].soltar();
                }
            }
        }
    }

    private void terminarBloque(Bloque b) {
        if (b == null) {
            return;
        }
        if (b.grupo != null) {
            b.grupo.faltan.remove(b.de);
            if (b.grupo.faltan.isEmpty()) {
                grupos.remove(b.grupo);
                if (b.grupo.fin != null) {
                    conAudiencia(null, b.grupo.fin);
                }
            }
        } else if (b.fin != null) {
            conAudiencia(b.de, b.fin);
        }
    }

    private void avanzar(Dlg d) {
        if (d.texto == null) {
            return;
        }
        if (d.chars < d.texto.length()) {
            d.chars = d.texto.length();
        } else {
            siguienteLinea(d);
        }
    }

    private Rol rolDe(Ctl c) {
        if (c != null) {
            for (int i = 0; i < porRol.length; i++) {
                if (porRol[i] == c) {
                    return Rol.values()[i];
                }
            }
        }
        return null;
    }

    private Dlg dlgDe(Ctl c) {
        if (!enRed) {
            return comun;
        }
        Rol r = rolDe(c);
        return r == null ? comun : dlgRol[r.ordinal()];
    }

    private Dlg dlgVista() {
        return dlgDe(vista());
    }

    // ===================== ENTRADA =====================

    /** E (o Enter) sostenida: adelanta el dialogo rapidito. Las repeticiones del teclado no vuelven a hablar. */
    // (en red cada rol lleva su propia E: indice = ordinal del rol; en un solo PC se usa el 0)
    private final boolean[] eAbajoDe = new boolean[5];
    private final double[] eAbajoTDe = new double[5];
    private boolean eRepeticion;

    private int quienE() {
        return Sincronia.activa() && Sincronia.rolActual() != null ? Sincronia.rolActual().ordinal() : 0;
    }

    /** true mientras ESTA pantalla esta adelantando su dialogo con la E sostenida. */
    protected boolean adelantando() {
        int i = 0;
        if (enRed) {
            Rol mi = Sincronia.miRol();
            i = mi != null ? mi.ordinal() : 0;
        }
        return eAbajoDe[i] && eAbajoTDe[i] > 0.35 && dlgVista().texto != null;
    }

    void tecla(int k, boolean pres) {
        if (k == KeyEvent.VK_E || k == KeyEvent.VK_ENTER) {
            int q = quienE();
            eRepeticion = pres && eAbajoDe[q];
            if (pres && !eAbajoDe[q]) {
                eAbajoTDe[q] = 0;
            }
            eAbajoDe[q] = pres;
        }
        if (teclaExtra(k, pres)) {
            return;
        }
        Ctl c = ctlDeEntrada();
        switch (k) {
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                if (c != null) {
                    c.izq = pres;
                }
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                if (c != null) {
                    c.der = pres;
                }
                break;
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                if (c != null) {
                    c.arr = pres;
                }
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                if (c != null) {
                    c.abj = pres;
                }
                break;
            case KeyEvent.VK_E:
            case KeyEvent.VK_ENTER:
                // (ESPACIO ya no interactua: volvia a abrir el dialogo sin querer)
                if (pres && !eRepeticion) {
                    interactuar(c);
                }
                break;
            case KeyEvent.VK_F1:
                if (pres) {
                    verCajas = !verCajas;
                }
                break;
            default:
                break;
        }
    }

    private void interactuar(Ctl c) {
        Dlg d = dlgDe(c);
        if (d.texto != null) {
            avanzar(d);
            return;
        }
        Rol r = rolDe(c);
        if (bloqueado || c == null || tiempo < d.sinInteraccionHasta || (enRed && congelado(r))) {
            return;
        }
        if (c.cerca != null && c.cerca.alInteractuar != null) {
            if (c.actor != null) {
                c.cerca.mirandoIzquierda = c.actor.x < c.cerca.x;
            }
            Actor quien = c.cerca;
            conAudiencia(r, () -> {
                alInteractuarCon(quien);
                quien.alInteractuar.run();
            });
        } else if (c.zonaCerca != null) {
            Zona z = c.zonaCerca;
            conAudiencia(r, z.accion);
        }
    }

    @Override
    protected void alOcultar() {
        for (Ctl c : ctls()) {
            c.soltar();
        }
    }

    // ===================== LOGICA =====================

    @Override
    protected void actualizar(double dt) {
        for (int i = 0; i < eAbajoDe.length; i++) {
            if (eAbajoDe[i]) {
                eAbajoTDe[i] += dt;
            }
        }
        if (enRed) {
            for (int i = 0; i < dlgRol.length; i++) {
                escribir(dlgRol[i], i, dt);
            }
        } else if (comun.texto != null) {
            escribir(comun, 0, dt);
            return;          // un solo PC: mientras se lee, nadie camina (como siempre)
        }
        List<Ctl> todos = ctls();
        if (todos.isEmpty()) {
            return;
        }
        for (Ctl c : todos) {
            if (!c.acomodado) {
                c.acomodado = true;
                acomodar(c.actor);
            }
        }
        if (bloqueado || quieto) {
            for (Ctl c : todos) {
                c.actor.caminando = false;
            }
            return;
        }

        for (Ctl c : todos) {
            Actor a = c.actor;
            if (parado(c)) {          // en red: este jugador esta leyendo o decidiendo (los demas siguen)
                a.caminando = false;
                continue;
            }
            double dx = (c.der ? 1 : 0) - (c.izq ? 1 : 0);
            double dy = (c.abj ? 1 : 0) - (c.arr ? 1 : 0);
            a.caminando = dx != 0 || dy != 0;
            if (a.caminando) {
                double len = StrictMath.hypot(dx, dy);
                dx /= len;
                dy /= len;
                mover(a, dx * velocidad * dt, 0);
                mover(a, 0, dy * velocidad * dt);
                if (dx != 0) {
                    a.mirandoIzquierda = dx < 0;
                }
            }
            buscarCercano(c);
        }

        for (Ctl c : todos) {
            if (parado(c)) {
                continue;
            }
            for (Zona z : zonas) {
                if (z.auto && !z.usada && z.r.contains(c.actor.x, c.actor.y)) {
                    z.usada = true;
                    quienActua(c);
                    z.accion.run();
                    break;
                }
            }
        }
    }

    /** Va escribiendo la linea del dialogo d (y la adelanta si su dueño tiene la E sostenida). */
    private void escribir(Dlg d, int e, double dt) {
        if (d.texto == null) {
            return;
        }
        d.chars += dt * 60 * Ajustes.velocidadTexto();
        if (eAbajoDe[e] && eAbajoTDe[e] > 0.35) {
            d.chars += dt * 500;
            if (d.chars >= d.texto.length()) {
                d.adelantoT += dt;
                if (d.adelantoT > 0.16) {
                    d.adelantoT = 0;
                    avanzar(d);
                }
            }
        }
    }

    private boolean parado(Ctl c) {
        if (!enRed) {
            return false;
        }
        return dlgDe(c).texto != null || congelado(rolDe(c));
    }

    /** Gancho: en red, avisa quien causo la accion automatica (zonaAuto) para que el guion sepa "quien actua". */
    protected void quienActua(Object controlDe) {
        if (enRed) {
            for (int i = 0; i < porRol.length; i++) {
                if (porRol[i] == controlDe) {
                    alActuarAuto(Rol.values()[i]);
                }
            }
        }
    }

    protected void alActuarAuto(Rol r) {
    }

    private void mover(Actor a, double dx, double dy) {
        double ox = a.x, oy = a.y;
        a.x += dx;
        a.y += dy;
        if (!libre(a)) {
            a.x = ox;
            a.y = oy;
        }
    }

    private boolean libre(Actor a) {
        Rectangle2D.Double h = a.hitbox();
        if (!caminable.contains(h)) {
            return false;
        }
        for (Obstaculo o : obstaculos) {
            if (o.r.intersects(h)) {
                return false;
            }
        }
        for (Actor otro : actores) {
            if (otro != a && otro.solido && otro.hitbox().intersects(h)) {
                return false;
            }
        }
        return true;
    }

    /** Si el actor aparecio encima de algo solido, lo mueve al punto libre mas cercano. */
    private void acomodar(Actor a) {
        if (quieto || libre(a)) {
            return;
        }
        double x0 = a.x, y0 = a.y;
        for (int radio = 8; radio <= 700; radio += 8) {
            for (int dx = -radio; dx <= radio; dx += 8) {
                for (int dy = -radio; dy <= radio; dy += 8) {
                    if (Math.abs(dx) != radio && Math.abs(dy) != radio) {
                        continue;
                    }
                    a.x = x0 + dx;
                    a.y = y0 + dy;
                    if (libre(a)) {
                        System.out.println("[EscenaMundo] " + a.id + " estaba atascado, se movio a "
                                + (int) a.x + ", " + (int) a.y);
                        return;
                    }
                }
            }
        }
        a.x = x0;
        a.y = y0;
        System.out.println("[EscenaMundo] OJO: no hay espacio libre cerca de " + a.id
                + ". Revisa caminable() y los obstaculos.");
    }

    private void buscarCercano(Ctl c) {
        c.cerca = null;
        c.zonaCerca = null;
        Actor yo = c.actor;
        double mejor = 150;
        for (Actor a : actores) {
            if (a == yo || a.etiqueta == null) {
                continue;
            }
            double d = StrictMath.hypot(a.x - yo.x, (a.y - yo.y) * 1.4);
            if (d < mejor) {
                mejor = d;
                c.cerca = a;
            }
        }
        if (c.cerca == null) {
            for (Zona z : zonas) {
                if (!z.auto && z.r.contains(yo.x, yo.y)) {
                    c.zonaCerca = z;
                    break;
                }
            }
        }
    }

    // ===================== DIBUJO =====================

    @Override
    protected void dibujar(Graphics2D g) {
        if (fondo != null) {
            dibujarAPantalla(g, fondo, 0, 0, W, H);
        } else {
            fondoProvisional(g);
        }

        List<Actor> orden = new ArrayList<>(actores);
        orden.sort(Comparator.comparingDouble(a -> a.y));
        for (Actor a : orden) {
            a.dibujar(g, tiempo);
        }

        placas(g);
        indicador(g);
        if (verCajas) {
            cajas(g);
        }
        dibujarCapas(g);
        Dlg d = dlgVista();
        if (d.texto != null) {
            dibujarDialogo(g, d);
        } else {
            dibujarEsperando(g);
        }
    }

    /** En red: "Esperando a que X termine de leer..." (ya leiste lo de la escena y faltan otros). */
    private void dibujarEsperando(Graphics2D g) {
        List<Rol> faltan = esperandoA();
        if (faltan == null || faltan.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("Esperando a que ");
        for (int i = 0; i < faltan.size(); i++) {
            if (i > 0) {
                sb.append(i == faltan.size() - 1 ? " y " : ", ");
            }
            sb.append(nombreDe(faltan.get(i)));
        }
        sb.append(faltan.size() == 1 ? " termine de leer" : " terminen de leer");
        int puntos = (int) (tiempo * 2.5) % 4;
        sb.append(".".repeat(puntos));
        String t = sb.toString();
        g.setFont(Estilo.fuente(Font.BOLD, 22f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(t + "...") + 60, h = 48;
        int x = (W - w) / 2, y = H - 190;
        g.setColor(new Color(60, 35, 15, 40));
        g.fillRoundRect(x + 2, y + 5, w, h, h, h);
        g.setColor(new Color(255, 250, 240, 235));
        g.fillRoundRect(x, y, w, h, h, h);
        g.setColor(new Color(0xEAD7AE));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, h, h);
        g.setColor(new Color(0x5A3B1F));
        g.drawString(t, x + 30, y + 31);
    }

    /** Nombre para mostrar de un rol (Lugar lo cambia por el nombre del jugador). */
    protected String nombreDe(Rol r) {
        return r.nombre;
    }

    private void fondoProvisional(Graphics2D g) {
        int horizonte = 430;
        g.setPaint(new GradientPaint(0, 0, new Color(0xF6E3C0), 0, horizonte, new Color(0xE8C99A)));
        g.fillRect(0, 0, W, horizonte);
        // ventanas
        for (int i = 0; i < 3; i++) {
            int wx = 220 + i * 560;
            g.setColor(new Color(0x8FD0F0));
            g.fillRoundRect(wx, 90, 240, 210, 18, 18);
            g.setColor(Estilo.MADERA_OSCURA);
            g.setStroke(new BasicStroke(8f));
            g.drawRoundRect(wx, 90, 240, 210, 18, 18);
            g.drawLine(wx + 120, 90, wx + 120, 300);
            g.drawLine(wx, 195, wx + 240, 195);
        }
        // zocalo y piso
        g.setColor(Estilo.MADERA_OSCURA);
        g.fillRect(0, horizonte - 12, W, 16);
        g.setPaint(new GradientPaint(0, horizonte, new Color(0xD9A05B), 0, H, new Color(0xB5793E)));
        g.fillRect(0, horizonte + 4, W, H - horizonte);
        g.setColor(new Color(0x7A4A24).darker());
        g.setStroke(new BasicStroke(2f));
        for (int y = horizonte + 70; y < H; y += 70) {
            g.drawLine(0, y, W, y);
        }
        // muebles (obstaculos)
        Font f = Estilo.fuente(Font.BOLD, 28f);
        g.setFont(f);
        for (Obstaculo o : obstaculos) {
            g.setColor(new Color(0, 0, 0, 50));
            g.fillRoundRect(o.r.x + 6, o.r.y + 10, o.r.width, o.r.height, 26, 26);
            Estilo.madera(g, o.r.x, o.r.y - 40, o.r.width, o.r.height + 40, 26);
            if (o.nombre != null) {
                g.setColor(new Color(0xFFF3D6));
                centrado(g, o.nombre, o.r.x + o.r.width / 2, o.r.y + o.r.height / 2 - 10);
            }
        }
    }

    /** El actor con quien se puede hablar ahora mismo (el del letrerito "E Hablar con..."), o null. */
    protected Actor cercano() {
        Ctl v = vista();
        return v != null && dlgVista().texto == null && !bloqueado && !parado(v) ? v.cerca : null;
    }

    /** Gancho: se acaba de hablar con este actor (Lugar lleva la cuenta). */
    protected void alInteractuarCon(Actor a) {
    }

    /** Distancia (en pixeles del diseno) a la que aparece la placa con el nombre de un vecino. */
    public static final double DIST_PLACA = 240;

    /** false = esa placa nunca se dibuja (Lugar esconde las de los cuatro principales). */
    protected boolean conPlaca(Actor a) {
        return true;
    }

    /** Placas con el nombre: SOLO de quien esta cerca de ti, y aparecen suavecito. */
    private void placas(Graphics2D g) {
        Ctl v = vista();
        Actor yo = v != null ? v.actor : null;
        if (yo == null || dlgVista().texto != null) {
            return;
        }
        g.setFont(Estilo.fuente(Font.BOLD, 20f));
        FontMetrics fm = g.getFontMetrics();
        for (Actor a : actores) {
            String n = hayNiebla() ? a.nombreNiebla : a.nombre;
            if (n == null || a == yo || !conPlaca(a)) {
                continue;
            }
            double d = StrictMath.hypot(a.x - yo.x, (a.y - yo.y) * 1.4);
            if (d > DIST_PLACA) {
                continue;
            }
            float alfa = (float) Math.max(0, Math.min(1, (DIST_PLACA - d) / 70.0));
            boolean elegido = v.cerca == a;
            int w = fm.stringWidth(n) + 38, h = 30;
            int x = (int) (a.x - w / 2.0), y = (int) a.y + 14;
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
            gg.setColor(new Color(40, 24, 12, 60));
            gg.fill(new RoundRectangle2D.Double(x + 1, y + 3, w, h, h, h));
            gg.setColor(new Color(255, 250, 238, 238));
            gg.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            gg.setColor(elegido ? Estilo.DORADO : new Color(0xE6D09B));
            gg.setStroke(new BasicStroke(elegido ? 2.5f : 1.5f));
            gg.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
            gg.setColor(elegido ? Estilo.DORADO : new Color(0xE9A9B8));
            gg.fillOval(x + 11, y + h / 2 - 4, 8, 8);
            gg.setColor(hayNiebla() ? Estilo.TEXTO_SUAVE : Estilo.TEXTO);
            gg.drawString(n, x + 25, y + 21);
            gg.dispose();
        }
    }

    private void indicador(Graphics2D g) {
        String texto = null;
        double cx = 0, cy = 0;
        Ctl v = vista();
        if (v == null || v.actor == null) {
            return;
        }
        boolean libre = dlgVista().texto == null && !bloqueado && !parado(v);
        if (v.cerca != null && libre) {
            texto = v.cerca.etiqueta;
            cx = v.cerca.x;
            cy = v.cerca.y - v.cerca.alto - 24;
        } else if (v.zonaCerca != null && libre) {
            texto = v.zonaCerca.etiqueta;
            cx = v.actor.x;
            cy = v.actor.y - v.actor.alto - 24;
        }
        if (texto == null) {
            return;
        }
        cy += Math.sin(tiempo * 4) * 4;
        g.setFont(Estilo.fuente(Font.BOLD, 24f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(texto) + 76;
        int h = 46;
        double x0 = cx - w / 2.0, y0 = cy - h;
        g.setColor(new Color(40, 24, 12, 55));
        g.fill(new RoundRectangle2D.Double(x0 + 2, y0 + 4, w, h, h, h));
        g.setColor(new Color(255, 250, 238, 245));
        g.fill(new RoundRectangle2D.Double(x0, y0, w, h, h, h));
        g.setColor(new Color(0xE6D09B));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x0, y0, w, h, h, h));
        // tecla E: circulito dorado
        g.setPaint(new GradientPaint((float) x0, (float) y0, Estilo.DORADO_CLARO, (float) x0, (float) (y0 + h), Estilo.DORADO));
        g.fill(new Ellipse2D.Double(x0 + 7, y0 + 7, 32, 32));
        g.setColor(Estilo.MADERA_OSCURA);
        g.setFont(Estilo.fuente(Font.BOLD, 20f));
        centrado(g, "E", (int) (x0 + 23), (int) (y0 + 30));
        g.setFont(Estilo.fuente(Font.BOLD, 24f));
        g.setColor(Estilo.TEXTO);
        g.drawString(texto, (int) (x0 + 50), (int) (y0 + 31));
        // colita
        g.setColor(new Color(255, 250, 238, 245));
        g.fillPolygon(new int[]{(int) cx - 9, (int) cx + 9, (int) cx}, new int[]{(int) (y0 + h - 1), (int) (y0 + h - 1), (int) (y0 + h + 11)}, 3);
    }

    private void cajas(Graphics2D g) {
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(60, 220, 90));
        g.draw(caminable);
        g.setColor(new Color(255, 60, 60, 110));
        for (Obstaculo o : obstaculos) {
            g.fill(o.r);
        }
        g.setColor(new Color(255, 220, 40, 90));
        for (Zona z : zonas) {
            g.fill(z.r);
        }
        g.setColor(new Color(60, 140, 255));
        for (Actor a : actores) {
            g.draw(a.hitbox());
            g.fillOval((int) a.x - 4, (int) a.y - 4, 8, 8);
        }
    }

    private void dibujarDialogo(Graphics2D g, Dlg d) {
        String dlgQuien = d.quien, dlgTexto = d.texto;
        double dlgChars = d.chars;
        if ("@img".equals(dlgQuien)) {
            BufferedImage img = Recursos.dialogo(dlgTexto);
            double esc = (W * 0.50) / img.getWidth();
            if (img.getHeight() * esc > H * 0.34) {
                esc = (H * 0.34) / img.getHeight();
            }
            int w = (int) (img.getWidth() * esc), h = (int) (img.getHeight() * esc);
            g.drawImage(img, (W - w) / 2, H - (int) (H * 0.015) - h, w, h, null);
            return;
        }
        int bx = 260, by = 790, bw = 1400, bh = 230;
        if (Recursos.existe(MARCO_BURBUJA)) {
            // marco hecho en Canva (1400x230, el texto se escribe encima)
            g.drawImage(Recursos.imagen(MARCO_BURBUJA, bw, bh), bx, by, bw, bh, null);
        } else {
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(bx + 6, by + 10, bw, bh, 44, 44);
            Estilo.pergamino(g, bx, by, bw, bh, 44);
            g.setColor(Estilo.MADERA_OSCURA);
            g.setStroke(new BasicStroke(6f));
            g.drawRoundRect(bx, by, bw, bh, 44, 44);
        }

        if (dlgQuien != null) {
            Estilo.madera(g, bx + 50, by - 34, 330, 64, 28);
            g.setFont(Estilo.fuente(Font.BOLD, 34f));
            g.setColor(new Color(0xFFF3D6));
            centrado(g, dlgQuien, bx + 50 + 165, by + 10);
        }

        g.setFont(Estilo.fuente(Font.PLAIN, 40f));
        FontMetrics fm = g.getFontMetrics();
        List<String> lineas = envolver(dlgTexto, fm, bw - 150);
        int visibles = (int) Math.min(dlgTexto.length(), dlgChars);
        int usados = 0;
        int y = by + 82;
        g.setColor(Estilo.TEXTO);
        for (String l : lineas) {
            if (usados >= visibles) {
                break;
            }
            int n = Math.min(l.length(), visibles - usados);
            g.drawString(l.substring(0, n), bx + 70, y);
            usados += l.length() + 1;
            y += fm.getHeight() + 6;
        }
        // pista: mantener E adelanta
        g.setFont(Estilo.fuente(Font.BOLD, 17f));
        String pista = adelantando() ? "adelantando ▶▶" : "mantén E para adelantar";
        g.setColor(adelantando() ? new Color(0xC98A12) : new Color(156, 123, 85, 150));
        g.drawString(pista, bx + bw - 150 - g.getFontMetrics().stringWidth(pista), by + bh - 34);
        if (dlgChars >= dlgTexto.length()) {
            int tx = bx + bw - 90, ty = by + bh - 70 + (int) (Math.sin(tiempo * 6) * 4);
            g.setColor(Estilo.DORADO);
            g.fillPolygon(new int[]{tx, tx + 40, tx + 20}, new int[]{ty, ty, ty + 28}, 3);
        }
    }
}
