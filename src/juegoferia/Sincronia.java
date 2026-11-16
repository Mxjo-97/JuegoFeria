package juegoferia;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Timer;

/**
 * El modo EN RED funciona asi (se llama "lockstep"): las cuatro computadoras corren EXACTAMENTE el
 * mismo juego, con la misma semilla, y lo unico que viaja por la red son las teclas y los clics
 * de cada jugador. El anfitrion les pone un numero de cuadro (tick) a todos los eventos y los
 * reenvia; cada computadora aplica los mismos eventos en el mismo cuadro, asi que el juego queda
 * identico en las cuatro sin mandar posiciones ni estados.
 *
 * Reglas para que esto no se rompa al programar:
 *   - La logica del juego NO usa el reloj real ni Math.random(); usa dt (siempre 1/60 en red)
 *     y Sincronia.nuevoRandom() cuando necesite azar.
 *   - Todo lo que cambie el estado del juego sale de una tecla/clic recibido (Sincronia lo entrega
 *     con rolActual() = quien lo hizo) o del paso del tiempo.
 *   - Lo que es solo de tu pantalla (F1, tamaño de la ventana) no viaja: ver teclaSoloLocal.
 *
 * En modo "un solo PC" Sincronia no esta activa y nada de esto se usa.
 */
public final class Sincronia {

    public static final double DT = 1.0 / 60.0;

    /** Cada cuantos cuadros se compara el estado entre computadoras (para detectar desincronizacion). */
    private static final int CADA_CHEQUEO = 120;
    private static final int MAX_PASOS_POR_TIMER = 12;

    private static boolean activa, preparada;
    private static Rol miRol;
    private static long semilla;
    private static int contadorAzar;
    private static RedMacondo.Cliente red;
    private static Supplier<PanelDiseno> panelActivo = () -> null;
    private static Supplier<Partida> partida = () -> null;
    private static Supplier<String> huella = () -> "";
    private static Consumer<String> alFin = m -> {
    };

    private static boolean inyectando;
    private static Rol rolActual;
    private static final Point punto = new Point();

    private static final Set<Integer> abajo = new HashSet<>();
    private static long ultimoMov;
    private static Point ultimaPos = new Point(-100, -100);

    private static final LinkedList<String[]> cola = new LinkedList<>();
    private static Timer timer;
    private static int cuadro;
    private static String ultimaHuella = "";
    /** Solo para pruebas: recibe "cuadro|hash|texto" cada vez que se calcula la huella. */
    static Consumer<String> sonda;
    static Consumer<String> sondaEv;

    private Sincronia() {
    }

    // ===================== ESTADO =====================

    public static boolean activa() {
        return activa;
    }

    public static boolean inyectando() {
        return inyectando;
    }

    /** Quien hizo la tecla/clic que se esta procesando (solo vale mientras inyectando()). */
    public static Rol rolActual() {
        return rolActual;
    }

    public static Rol miRol() {
        return miRol;
    }

    static Point puntoDiseno() {
        return punto;
    }

    public static int cuadro() {
        return cuadro;
    }

    public static String ultimaHuella() {
        return ultimaHuella;
    }

    /**
     * Dado un jugador, indica si lo manejo yo en esta computadora. En un solo PC siempre es true
     * (un teclado maneja a todos); en red solo es true para mi propio rol.
     */
    public static boolean esMio(Rol r) {
        return !activa || r == miRol;
    }

    // ===================== AZAR COMPARTIDO =====================

    /** Debe llamarse ANTES de crear la Partida: asi todo el azar sale de la misma semilla. */
    public static void preparar(long semillaCompartida) {
        semilla = semillaCompartida;
        contadorAzar = 0;
        preparada = true;
    }

    /** Un Random que da lo mismo en las cuatro computadoras (si hay semilla compartida). */
    public static Random nuevoRandom() {
        if (!preparada && !activa) {
            return new Random();
        }
        return new Random(semilla * 1000003L + (contadorAzar++));
    }

    // ===================== ARRANQUE Y FIN =====================

    /** Los oyentes de red del juego: reciben los cuadros del anfitrion. */
    public static final RedMacondo.Oyente OYENTE = new RedMacondo.Oyente() {
        @Override
        public void recibir(String[] m) {
            Sincronia.recibir(m);
        }

        @Override
        public void cerrado(String motivo) {
            Sincronia.terminarPorRed(motivo);
        }
    };

    public static void activar(Rol mi, RedMacondo.Cliente cliente, Supplier<PanelDiseno> panel,
            Supplier<Partida> pa, Supplier<String> hue, Consumer<String> cuandoTermina) {
        desactivar();
        miRol = mi;
        red = cliente;
        panelActivo = panel;
        partida = pa;
        huella = hue;
        alFin = cuandoTermina;
        cuadro = 0;
        activa = true;
        preparada = true;
        red.setOyente(OYENTE);
        timer = new Timer(8, e -> pasos());
        timer.start();
    }

    public static void desactivar() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        activa = false;
        preparada = false;
        cola.clear();
        abajo.clear();
        miRol = null;
        red = null;
        panelActivo = () -> null;
        partida = () -> null;
    }

    private static void terminarPorRed(String motivo) {
        if (!activa) {
            return;
        }
        Consumer<String> f = alFin;
        desactivar();
        f.accept(motivo);
    }

    // ===================== LO QUE LLEGA DEL ANFITRION =====================

    static void recibir(String[] m) {
        if (m.length == 0) {
            return;
        }
        switch (m[0]) {
            case "T":
                cola.add(m);
                break;
            case "FIN":
                terminarPorRed(m.length > 1 ? m[1] : "La partida terminó.");
                break;
            case "DESINC":
                System.err.println("[Sincronia] OJO: las computadoras se desincronizaron cerca del cuadro "
                        + (m.length > 1 ? m[1] : "?") + ". Revisa que usen la misma versión del juego y las mismas imágenes.");
                break;
            default:
                break;
        }
    }

    private static void pasos() {
        int n = 0;
        while (activa && !cola.isEmpty() && n < MAX_PASOS_POR_TIMER) {
            // si nos atrasamos mucho, damos mas pasos por vuelta para alcanzar
            avanzarUno(cola.poll());
            n++;
        }
    }

    /** Aplica los eventos del cuadro y avanza el juego un paso fijo. Visible para las pruebas. */
    static void avanzarUno(String[] frame) {
        cuadro++;
        for (int i = 2; i < frame.length; i++) {
            aplicar(frame[i]);
        }
        PanelDiseno p = panelActivo.get();
        if (p != null) {
            p.avanzar(DT);
        }
        if (cuadro % CADA_CHEQUEO == 0 && red != null) {
            String texto = huella.get();
            ultimaHuella = Integer.toHexString(texto.hashCode());
            red.enviar("CHK", cuadro, ultimaHuella);
            if (sonda != null) {
                sonda.accept(cuadro + "|" + ultimaHuella + "|" + texto);
            }
        }
    }

    private static void aplicar(String campo) {
        String[] f = campo.split(",");
        if (f.length < 2) {
            return;
        }
        Rol r = Rol.values()[Integer.parseInt(f[0])];
        String tipo = f[1];
        PanelDiseno p = panelActivo.get();
        if (sondaEv != null) {
            sondaEv.accept(cuadro + "|" + campo + "|" + (p == null ? "-" : p.getClass().getSimpleName() + p.aceptaEntradaDe(r)));
        }
        if (p == null) {
            return;
        }
        boolean suelta = tipo.equals("KU") || tipo.equals("MU");
        if (!suelta && !p.aceptaEntradaDe(r)) {
            return;
        }
        Partida pa = partida.get();
        if (pa != null) {
            pa.setLider(r);      // "quien actua ahora": el guion lo lee con partida.lider()
        }
        rolActual = r;
        inyectando = true;
        try {
            switch (tipo) {
                case "KD":
                    p.inyectarTecla(true, Integer.parseInt(f[2]));
                    break;
                case "KU":
                    p.inyectarTecla(false, Integer.parseInt(f[2]));
                    break;
                case "MD":
                    punto.setLocation(Integer.parseInt(f[2]), Integer.parseInt(f[3]));
                    p.inyectarRaton(MouseEvent.MOUSE_PRESSED, Integer.parseInt(f[4]));
                    break;
                case "MU":
                    punto.setLocation(Integer.parseInt(f[2]), Integer.parseInt(f[3]));
                    p.inyectarRaton(MouseEvent.MOUSE_RELEASED, Integer.parseInt(f[4]));
                    break;
                case "MM":
                    punto.setLocation(Integer.parseInt(f[2]), Integer.parseInt(f[3]));
                    p.inyectarRaton(MouseEvent.MOUSE_MOVED, 0);
                    break;
                default:
                    break;
            }
        } finally {
            inyectando = false;
        }
    }

    // ===================== LO QUE HACE ESTE JUGADOR =====================

    static void teclaLocal(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (!abajo.add(code)) {
                return;                // repeticion automatica del teclado: se ignora
            }
            enviar("KD", code, 0, 0);
        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            if (abajo.remove(code)) {
                enviar("KU", code, 0, 0);
            }
        }
    }

    static void ratonLocal(MouseEvent e, Point d) {
        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                enviar("MD", d.x, d.y, e.getButton());
                break;
            case MouseEvent.MOUSE_RELEASED:
                enviar("MU", d.x, d.y, e.getButton());
                break;
            case MouseEvent.MOUSE_MOVED:
            case MouseEvent.MOUSE_DRAGGED:
                long ahora = System.nanoTime();
                if (ahora - ultimoMov > 40_000_000L && Math.abs(d.x - ultimaPos.x) + Math.abs(d.y - ultimaPos.y) >= 6) {
                    ultimoMov = ahora;
                    ultimaPos = d;
                    enviar("MM", d.x, d.y, 0);
                }
                break;
            default:
                break;
        }
    }

    /** Si la ventana pierde el foco, se sueltan todas las teclas para que nadie quede caminando solo. */
    static void soltarTodas() {
        if (!activa || abajo.isEmpty()) {
            return;
        }
        for (int code : new HashSet<>(abajo)) {
            enviar("KU", code, 0, 0);
        }
        abajo.clear();
    }

    private static void enviar(String tipo, int a, int b, int c) {
        if (red != null) {
            red.enviar("IN", tipo, a, b, c);
        }
    }
}
