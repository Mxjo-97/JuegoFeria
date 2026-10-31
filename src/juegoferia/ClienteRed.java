package juegoferia;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.SwingUtilities;

public class ClienteRed {

    public interface Escucha {
        void jugadorEntra(int id, String nombre);
        void jugadorMueve(int id, int x, int y, String escena);
        void jugadorSale(int id);
        void desconectado();
    }

    private final Escucha escucha;
    private PrintWriter out;

    public ClienteRed(Escucha escucha) {
        this.escucha = escucha;
    }

    public boolean conectar(String ip, int puerto, String nombre) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, puerto), 3000);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out.println("HOLA|" + nombre.replace("|", ""));

            Thread hilo = new Thread(() -> escuchar(in));
            hilo.setDaemon(true);
            hilo.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void enviarPosicion(int x, int y, String escena) {
        if (out != null) {
            out.println("POS|" + x + "|" + y + "|" + escena);
        }
    }

    private void escuchar(BufferedReader in) {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                procesar(linea);
            }
        } catch (IOException e) {
            // conexión perdida
        }
        SwingUtilities.invokeLater(escucha::desconectado);
    }

    private void procesar(String linea) {
        String[] p = linea.split("\\|");
        try {
            switch (p[0]) {
                case "ENTRA": {
                    int id = Integer.parseInt(p[1]);
                    String nombre = p[2];
                    SwingUtilities.invokeLater(() -> escucha.jugadorEntra(id, nombre));
                    break;
                }
                case "POS": {
                    int id = Integer.parseInt(p[1]);
                    int x = Integer.parseInt(p[2]);
                    int y = Integer.parseInt(p[3]);
                    String escena = p[4];
                    SwingUtilities.invokeLater(() -> escucha.jugadorMueve(id, x, y, escena));
                    break;
                }
                case "SALE": {
                    int id = Integer.parseInt(p[1]);
                    SwingUtilities.invokeLater(() -> escucha.jugadorSale(id));
                    break;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // mensaje mal formado: se ignora
        }
    }
}