package juegoferia;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/**
 * Red del modo en red (4 jugadores). Sockets TCP y mensajes de texto, una línea por mensaje:
 *     TIPO|campo1|campo2|...      (cada campo va codificado con URLEncoder, así que puede llevar | y acentos)
 * Se puede leer en la consola mientras pruebas, a propósito.
 */
public final class RedMacondo {

    public static final int PUERTO = 5555;

    private RedMacondo() {
    }

    // ===================== FORMATO DE LOS MENSAJES =====================

    public static String armar(Object... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(URLEncoder.encode(String.valueOf(campos[i]), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public static String[] partir(String linea) {
        String[] t = linea.split("\\|", -1);
        for (int i = 0; i < t.length; i++) {
            t[i] = URLDecoder.decode(t[i], StandardCharsets.UTF_8);
        }
        return t;
    }

    /** Quien recibe mensajes del otro lado. */
    public interface Oyente {

        void recibir(String[] m);

        void cerrado(String motivo);
    }

    // ===================== CLIENTE =====================

    public static final class Cliente implements Closeable {

        private final Socket socket;
        private final PrintWriter salida;
        private final boolean enEDT;
        private volatile Oyente oyente;
        private volatile boolean cerrado;

        /** enEDT = true: los mensajes llegan en el hilo de Swing (lo normal). false: en el hilo de red (pruebas). */
        public Cliente(String host, int puerto, boolean enEDT, Oyente oyente) throws IOException {
            this.enEDT = enEDT;
            this.oyente = oyente;
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, puerto), 4000);
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Thread t = new Thread(() -> {
                try {
                    String l;
                    while ((l = in.readLine()) != null) {
                        final String[] m = partir(l);
                        entregar(() -> this.oyente.recibir(m));
                    }
                } catch (IOException e) {
                    // se cerró la conexión
                } finally {
                    if (!cerrado) {
                        entregar(() -> this.oyente.cerrado("Se perdió la conexión con el anfitrión."));
                    }
                }
            }, "macondo-cliente");
            t.setDaemon(true);
            t.start();
        }

        private void entregar(Runnable r) {
            if (enEDT) {
                SwingUtilities.invokeLater(r);
            } else {
                r.run();
            }
        }

        /** Cambia quién escucha (el lobby se lo pasa al juego cuando empieza la partida). */
        public void setOyente(Oyente o) {
            this.oyente = o;
        }

        public synchronized void enviar(Object... campos) {
            if (!cerrado) {
                salida.println(armar(campos));
            }
        }

        @Override
        public void close() {
            cerrado = true;
            try {
                socket.close();
            } catch (IOException e) {
                // nada
            }
        }
    }

    // ===================== SERVIDOR =====================

    public static final class Servidor implements Closeable {

        public interface Manejador {

            void conectado(int id);

            void mensaje(int id, String[] m);

            void desconectado(int id);
        }

        private final ServerSocket ss;
        private final Manejador manejador;
        private final Map<Integer, PrintWriter> salidas = new ConcurrentHashMap<>();
        private final Map<Integer, Socket> sockets = new ConcurrentHashMap<>();
        private final AtomicInteger siguiente = new AtomicInteger(1);
        private volatile boolean cerrado;

        public Servidor(int puerto, Manejador manejador) throws IOException {
            this.manejador = manejador;
            ss = new ServerSocket(puerto);
            Thread aceptar = new Thread(this::aceptar, "macondo-servidor");
            aceptar.setDaemon(true);
            aceptar.start();
        }

        public int puerto() {
            return ss.getLocalPort();
        }

        private void aceptar() {
            while (!cerrado) {
                try {
                    Socket s = ss.accept();
                    s.setTcpNoDelay(true);
                    int id = siguiente.getAndIncrement();
                    Thread t = new Thread(() -> atender(id, s), "macondo-conexion-" + id);
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    return;
                }
            }
        }

        private void atender(int id, Socket s) {
            try {
                PrintWriter w = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                salidas.put(id, w);
                sockets.put(id, s);
                manejador.conectado(id);
                String l;
                while ((l = in.readLine()) != null) {
                    manejador.mensaje(id, partir(l));
                }
            } catch (IOException e) {
                // se cayó el cliente
            } finally {
                boolean estaba = salidas.remove(id) != null;
                sockets.remove(id);
                try {
                    s.close();
                } catch (IOException e) {
                    // nada
                }
                if (estaba) {
                    manejador.desconectado(id);
                }
            }
        }

        public void enviar(int id, Object... campos) {
            PrintWriter w = salidas.get(id);
            if (w != null) {
                synchronized (w) {
                    w.println(armar(campos));
                }
            }
        }

        public void difundir(Object... campos) {
            for (int id : salidas.keySet()) {
                enviar(id, campos);
            }
        }

        public void difundirMenos(int excepto, Object... campos) {
            for (int id : salidas.keySet()) {
                if (id != excepto) {
                    enviar(id, campos);
                }
            }
        }

        public void expulsar(int id) {
            Socket s = sockets.get(id);
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    // nada
                }
            }
        }

        @Override
        public void close() {
            cerrado = true;
            try {
                ss.close();
            } catch (IOException e) {
                // nada
            }
            for (Socket s : sockets.values()) {
                try {
                    s.close();
                } catch (IOException e) {
                    // nada
                }
            }
        }
    }
}
