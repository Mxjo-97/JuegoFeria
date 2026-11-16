package juegoferia;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * El SERVIDOR de la sala. Corre solo en la computadora de quien pulsa "Crear sala" (y esa misma
 * persona juega conectada a el, como los demas). Hace tres cosas:
 *
 *   1. SALA: lleva la cuenta de quien esta conectado y que rol escogio cada uno (un rol por persona).
 *      Cuando los CUATRO roles estan escogidos arranca una cuenta regresiva y empieza la partida:
 *      escoger un rol ya no inicia nada por si solo.
 *   2. ARRANQUE: manda a todos "EMPEZAR|semilla|roles y nombres" y espera a que los cuatro avisen LISTO.
 *   3. RELOJ: desde ahi manda 60 cuadros por segundo ("T|n|eventos..."). Los eventos son las teclas y
 *      clics de los jugadores, con el rol de quien los hizo (el rol lo pone el servidor, no el cliente).
 *
 * No calcula reglas del juego: eso lo hace cada computadora con el mismo codigo (ver Sincronia).
 *
 * Mensajes (cliente -> servidor): HOLA|nombre, ROL|ROL o -, LISTO, IN|tipo|a|b|c, CHK|cuadro|huella
 * Mensajes (servidor -> cliente): BIENVENIDA|id, ESTADO|(duenoId|nombre)x4, CUENTA|n, RECHAZO|motivo,
 *                                 EMPEZAR|semilla|(ROL|nombre)x4, T|n|eventos..., FIN|motivo, DESINC|n
 */
public final class SalaMacondo implements RedMacondo.Servidor.Manejador, Closeable {

    private static final class Con {

        String nombre = "Jugador";
        Rol rol;
        boolean listo;
    }

    private final RedMacondo.Servidor servidor;
    private final Map<Integer, Con> cons = new LinkedHashMap<>();
    private final ScheduledExecutorService reloj = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "macondo-sala");
        t.setDaemon(true);
        return t;
    });
    private final List<ScheduledFuture<?>> cuenta = new ArrayList<>();
    private final List<String> pendientes = new ArrayList<>();
    private final Map<Integer, String> huellas = new HashMap<>();
    private final long periodoNs;

    private boolean empezo, corriendo, cerrada;
    private int cuadro;
    private ScheduledFuture<?> ticker;

    /** periodoNs = duracion de un cuadro (16.666.667 = 60 por segundo; las pruebas lo acortan). */
    public SalaMacondo(int puerto, long periodoNs) throws IOException {
        this.periodoNs = periodoNs;
        this.servidor = new RedMacondo.Servidor(puerto, this);
    }

    public SalaMacondo(int puerto) throws IOException {
        this(puerto, 16_666_667L);
    }

    public int puerto() {
        return servidor.puerto();
    }

    // ===================== CONEXIONES =====================

    @Override
    public synchronized void conectado(int id) {
        if (empezo) {
            servidor.enviar(id, "RECHAZO", "La partida ya empezó.");
            servidor.expulsar(id);
            return;
        }
        if (cons.size() >= Rol.values().length) {
            servidor.enviar(id, "RECHAZO", "La sala ya tiene cuatro jugadores.");
            servidor.expulsar(id);
            return;
        }
        cons.put(id, new Con());
        servidor.enviar(id, "BIENVENIDA", id);
        difundirEstado();
    }

    @Override
    public synchronized void desconectado(int id) {
        Con c = cons.remove(id);
        if (c == null || cerrada) {
            return;
        }
        if (empezo) {
            String quien = c.nombre + (c.rol != null ? " (" + c.rol.nombre + ")" : "");
            terminar(quien + " se desconectó. La partida terminó.");
        } else {
            cancelarCuenta();
            difundirEstado();
        }
    }

    @Override
    public synchronized void mensaje(int id, String[] m) {
        Con c = cons.get(id);
        if (c == null || m.length == 0) {
            return;
        }
        switch (m[0]) {
            case "HOLA":
                if (m.length > 1 && !m[1].isBlank()) {
                    String n = m[1].trim();
                    c.nombre = n.length() > 14 ? n.substring(0, 14) : n;
                }
                difundirEstado();
                break;
            case "ROL":
                if (!empezo && m.length > 1) {
                    elegirRol(id, c, Rol.de(m[1]));
                }
                break;
            case "LISTO":
                c.listo = true;
                if (empezo && !corriendo && todosListos()) {
                    arrancarReloj();
                }
                break;
            case "IN":
                if (corriendo && c.rol != null && m.length >= 5) {
                    pendientes.add(c.rol.ordinal() + "," + m[1] + "," + m[2] + "," + m[3] + "," + m[4]);
                }
                break;
            case "CHK":
                if (corriendo && m.length >= 3) {
                    comparar(Integer.parseInt(m[1]), m[2]);
                }
                break;
            default:
                break;
        }
    }

    // ===================== SALA =====================

    private void elegirRol(int id, Con c, Rol r) {
        if (r != null) {
            for (Map.Entry<Integer, Con> e : cons.entrySet()) {
                if (e.getKey() != id && e.getValue().rol == r) {
                    servidor.enviar(id, "RECHAZO", r.nombre + " ya lo tiene " + e.getValue().nombre + ".");
                    return;
                }
            }
        }
        c.rol = r;       // null = soltar el rol
        cancelarCuenta();
        difundirEstado();
        if (rolesCompletos()) {
            programarCuenta();
        }
    }

    private boolean rolesCompletos() {
        for (Rol r : Rol.values()) {
            if (duenoDe(r) < 0) {
                return false;
            }
        }
        return true;
    }

    private int duenoDe(Rol r) {
        for (Map.Entry<Integer, Con> e : cons.entrySet()) {
            if (e.getValue().rol == r) {
                return e.getKey();
            }
        }
        return -1;
    }

    private boolean todosListos() {
        for (Con c : cons.values()) {
            if (!c.listo) {
                return false;
            }
        }
        return cons.size() == Rol.values().length;
    }

    private void difundirEstado() {
        List<Object> campos = new ArrayList<>();
        campos.add("ESTADO");
        for (Rol r : Rol.values()) {
            int dueno = duenoDe(r);
            campos.add(dueno);
            campos.add(dueno < 0 ? "-" : cons.get(dueno).nombre);
        }
        servidor.difundir(campos.toArray());
    }

    private void programarCuenta() {
        for (int n = 3; n >= 1; n--) {
            final int k = n;
            cuenta.add(reloj.schedule(() -> {
                synchronized (SalaMacondo.this) {
                    servidor.difundir("CUENTA", k);
                }
            }, 3 - n, TimeUnit.SECONDS));
        }
        cuenta.add(reloj.schedule(() -> {
            synchronized (SalaMacondo.this) {
                if (rolesCompletos() && !empezo) {
                    empezar();
                }
            }
        }, 3, TimeUnit.SECONDS));
    }

    private void cancelarCuenta() {
        for (ScheduledFuture<?> f : cuenta) {
            f.cancel(false);
        }
        cuenta.clear();
        servidor.difundir("CUENTA", 0);
    }

    // ===================== PARTIDA =====================

    private void empezar() {
        empezo = true;
        long semilla = System.nanoTime() & 0xFFFFFFFFFFL;
        List<Object> campos = new ArrayList<>();
        campos.add("EMPEZAR");
        campos.add(semilla);
        for (Rol r : Rol.values()) {
            campos.add(r.name());
            campos.add(cons.get(duenoDe(r)).nombre);
        }
        servidor.difundir(campos.toArray());
    }

    private void arrancarReloj() {
        corriendo = true;
        cuadro = 0;
        ticker = reloj.scheduleAtFixedRate(this::tic, 0, periodoNs, TimeUnit.NANOSECONDS);
    }

    private void tic() {
        synchronized (this) {
            if (!corriendo) {
                return;
            }
            cuadro++;
            Object[] f = new Object[2 + pendientes.size()];
            f[0] = "T";
            f[1] = cuadro;
            for (int i = 0; i < pendientes.size(); i++) {
                f[2 + i] = pendientes.get(i);
            }
            pendientes.clear();
            servidor.difundir(f);
        }
    }

    private void comparar(int n, String huella) {
        String antes = huellas.putIfAbsent(n, huella);
        if (antes != null && !antes.equals(huella)) {
            System.err.println("[Sala] DESINCRONIZADOS en el cuadro " + n + ": " + antes + " != " + huella);
            servidor.difundir("DESINC", n);
        }
        huellas.remove(n - 1200);
    }

    private void terminar(String motivo) {
        corriendo = false;
        if (ticker != null) {
            ticker.cancel(false);
        }
        servidor.difundir("FIN", motivo);
    }

    @Override
    public synchronized void close() {
        cerrada = true;
        corriendo = false;
        reloj.shutdownNow();
        servidor.close();
    }
}
