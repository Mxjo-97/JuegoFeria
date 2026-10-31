package juegoferia;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorJuego {

    private static class Jugador {
        final int id;
        String nombre;
        final PrintWriter out;
        int x = 5000, y = 5000;
        String escena = "EXT";

        Jugador(int id, PrintWriter out) {
            this.id = id;
            this.out = out;
        }
    }

    private static final Object LOCK = new Object();
    private static final Map<Integer, Jugador> jugadores = new LinkedHashMap<>();
    private static int siguienteId = 1;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(5000);
        System.out.println("Servidor listo en puerto 5000");
        while (true) {
            Socket socket = server.accept();
            new Thread(() -> atender(socket)).start();
        }
    }

    private static void atender(Socket socket) {
        Jugador yo = null;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String linea;
            while ((linea = in.readLine()) != null) {
                String[] p = linea.split("\\|");
                try {
                    if (p[0].equals("HOLA") && yo == null) {
                        synchronized (LOCK) {
                            yo = new Jugador(siguienteId++, out);
                            yo.nombre = (p.length > 1 && !p[1].trim().isEmpty())
                                    ? p[1] : "Jugador" + yo.id;

                            // Contarle al nuevo quién ya está conectado
                            for (Jugador otro : jugadores.values()) {
                                out.println("ENTRA|" + otro.id + "|" + otro.nombre);
                                out.println("POS|" + otro.id + "|" + otro.x + "|"
                                        + otro.y + "|" + otro.escena);
                            }
                            jugadores.put(yo.id, yo);
                            enviarAOtros(yo, "ENTRA|" + yo.id + "|" + yo.nombre);
                            System.out.println(yo.nombre + " se conectó (id " + yo.id + ")");
                        }

                    } else if (p[0].equals("POS") && yo != null && p.length >= 4) {
                        synchronized (LOCK) {
                            yo.x = Integer.parseInt(p[1]);
                            yo.y = Integer.parseInt(p[2]);
                            yo.escena = p[3];
                            enviarAOtros(yo, "POS|" + yo.id + "|" + yo.x + "|"
                                    + yo.y + "|" + yo.escena);
                        }
                    }
                } catch (NumberFormatException e) {
                    // mensaje mal formado: se ignora
                }
            }
        } catch (IOException e) {
            // se cayó la conexión
        } finally {
            if (yo != null) {
                synchronized (LOCK) {
                    jugadores.remove(yo.id);
                    enviarAOtros(yo, "SALE|" + yo.id);
                    System.out.println(yo.nombre + " se desconectó");
                }
            }
        }
    }

    // Siempre se llama dentro de synchronized (LOCK)
    private static void enviarAOtros(Jugador yo, String msg) {
        for (Jugador j : jugadores.values()) {
            if (j != yo) {
                j.out.println(msg);
            }
        }
    }
}