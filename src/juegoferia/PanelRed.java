package juegoferia;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * El modo EN RED (4 jugadores), antes de empezar: conectar (crear sala o unirse con la IP) y la SALA
 * donde cada quien escoge su rol. Escoger rol NO empieza la partida: empieza sola, con una cuenta
 * regresiva, cuando los cuatro roles estan escogidos por cuatro personas distintas.
 *
 * Una persona crea la sala (su computador hace de servidor: SalaMacondo) y juega como los demas.
 * Las otras tres se unen con la IP que sale en pantalla (misma red Wi-Fi o cable; si Windows
 * pregunta por el firewall, permitir Java).
 */
public class PanelRed extends JPanel implements RedMacondo.Oyente {

    /** Lo que el panel necesita de la ventana. */
    public interface Anfitrion {

        /** Llegaron los cuatro: crear el juego, activar Sincronia y avisar LISTO. */
        void empezar(Partida partida, Rol miRol, RedMacondo.Cliente red);

        void volver();
    }

    private final Anfitrion anfitrion;
    private final CardLayout cartas = new CardLayout();

    // conectar
    private final JTextField campoNombre = new JTextField("Jugador", 14);
    private final JTextField campoIp = new JTextField("192.168.", 14);
    private final JLabel avisoConectar = new JLabel(" ", SwingConstants.CENTER);

    // sala
    private final JLabel infoSala = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel estadoSala = new JLabel(" ", SwingConstants.CENTER);
    private final Map<Rol, JLabel> duenos = new EnumMap<>(Rol.class);
    private final Map<Rol, Estilo.BotonMadera> botones = new EnumMap<>(Rol.class);

    private SalaMacondo sala;
    private RedMacondo.Cliente red;
    private int miId = -1;
    private Rol miRol;
    private final int[] duenoId = new int[Rol.values().length];
    private final String[] duenoNombre = new String[Rol.values().length];
    private int cuenta;
    private boolean empezando;

    public PanelRed(Anfitrion anfitrion) {
        this.anfitrion = anfitrion;
        setLayout(cartas);
        add(panelConectar(), "conectar");
        add(panelSala(), "sala");
    }

    /** Se llama cada vez que se muestra la pantalla. */
    public void reiniciar() {
        cerrarRed();
        avisoConectar.setText(" ");
        cartas.show(this, "conectar");
    }

    // ===================== CONECTAR =====================

    private static class Fondo extends JPanel {

        Fondo() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0xF6E3C0), 0, getHeight(), new Color(0xE2BE82)));
            g.fillRect(0, 0, getWidth(), getHeight());
            Estilo.suave(g);
            Estilo.hojas(g, 0, 0, 1, 1, 150);
            Estilo.hojas(g, getWidth(), getHeight(), -1, -1, 150);
            g.dispose();
        }
    }

    private JPanel panelConectar() {
        JPanel p = new Fondo();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(40, 120, 40, 120));

        JLabel t = titulo("JUGAR EN RED");
        JLabel s = texto("4 jugadores, cada uno en su computador · misma red Wi-Fi o cable", 20f, Estilo.TEXTO_SUAVE);

        JPanel fila1 = fila();
        fila1.add(etiqueta("Tu nombre:"));
        estiloCampo(campoNombre);
        fila1.add(campoNombre);

        Estilo.BotonMadera crear = boton("Crear sala (yo soy el anfitrión)");
        crear.addActionListener(e -> crearSala());
        crear.setAlignmentX(0.5f);

        JLabel o = texto("— o —", 20f, Estilo.TEXTO_SUAVE);

        JPanel fila2 = fila();
        fila2.add(etiqueta("IP del anfitrión:"));
        estiloCampo(campoIp);
        fila2.add(campoIp);
        Estilo.BotonMadera unir = boton("Unirme");
        unir.addActionListener(e -> unirse(campoIp.getText().trim()));
        fila2.add(unir);

        avisoConectar.setForeground(Estilo.ROJO);
        avisoConectar.setFont(Estilo.fuente(Font.BOLD, 18f));
        avisoConectar.setAlignmentX(0.5f);

        Estilo.BotonMadera volver = boton("Volver");
        volver.addActionListener(e -> {
            cerrarRed();
            anfitrion.volver();
        });
        volver.setAlignmentX(0.5f);

        p.add(Box.createVerticalGlue());
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        p.add(s);
        p.add(Box.createVerticalStrut(30));
        p.add(fila1);
        p.add(Box.createVerticalStrut(16));
        p.add(crear);
        p.add(Box.createVerticalStrut(14));
        p.add(o);
        p.add(Box.createVerticalStrut(14));
        p.add(fila2);
        p.add(Box.createVerticalStrut(12));
        p.add(avisoConectar);
        p.add(Box.createVerticalStrut(20));
        p.add(volver);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private static JPanel fila() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        f.setOpaque(false);
        f.setMaximumSize(new Dimension(1000, 60));
        return f;
    }

    private static JLabel titulo(String s) {
        JLabel t = new JLabel(s, SwingConstants.CENTER);
        t.setFont(Estilo.fuente(Font.BOLD, 52f));
        t.setForeground(Estilo.MADERA_OSCURA);
        t.setAlignmentX(0.5f);
        return t;
    }

    private static JLabel texto(String s, float tam, Color c) {
        JLabel l = new JLabel(s, SwingConstants.CENTER);
        l.setFont(Estilo.fuente(Font.PLAIN, tam));
        l.setForeground(c);
        l.setAlignmentX(0.5f);
        return l;
    }

    private static JLabel etiqueta(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Estilo.fuente(Font.BOLD, 20f));
        l.setForeground(Estilo.TEXTO);
        return l;
    }

    private static void estiloCampo(JTextField c) {
        c.setFont(Estilo.fuente(Font.PLAIN, 20f));
        c.setForeground(Estilo.TEXTO);
        c.setBackground(new Color(0xFFF8E3));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.MADERA_OSCURA, 3, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private static Estilo.BotonMadera boton(String s) {
        Estilo.BotonMadera b = new Estilo.BotonMadera(s);
        b.setFont(Estilo.fuente(Font.BOLD, 20f));
        b.setPreferredSize(new Dimension(Math.max(200, s.length() * 13 + 50), 54));
        return b;
    }

    private String nombreEscrito() {
        String n = campoNombre.getText().trim();
        return n.isEmpty() ? "Jugador" : n;
    }

    private void crearSala() {
        try {
            sala = new SalaMacondo(RedMacondo.PUERTO);
        } catch (IOException e) {
            avisoConectar.setText("No pude abrir el puerto " + RedMacondo.PUERTO + " (¿ya hay una sala abierta en este computador?).");
            return;
        }
        if (!conectarA("127.0.0.1")) {
            cerrarRed();
        } else {
            // la anfitriona decide que partida se juega (la misma en las cuatro computadoras)
            red.enviar("MODO", PantallaModo.feria ? "FERIA" : "COMPLETA");
            infoSala.setText("Dile a los otros tres que se unan a:  " + misIps()
                    + (PantallaModo.feria ? "   ·  Partida corta (Modo Feria)" : "   ·  Partida completa"));
        }
    }

    private void unirse(String ip) {
        if (ip.isEmpty()) {
            avisoConectar.setText("Escribe la IP que te dio el anfitrión.");
            return;
        }
        if (conectarA(ip)) {
            infoSala.setText("Conectado a " + ip + ". Escoge tu rol.");
        }
    }

    private boolean conectarA(String host) {
        try {
            red = new RedMacondo.Cliente(host, RedMacondo.PUERTO, true, this);
        } catch (IOException e) {
            avisoConectar.setText("No pude conectar con " + host + ". Revisa la IP y que estén en la misma red.");
            return false;
        }
        avisoConectar.setText(" ");
        miRol = null;
        miId = -1;
        cuenta = 0;
        empezando = false;
        java.util.Arrays.fill(duenoId, -1);
        java.util.Arrays.fill(duenoNombre, "-");
        red.enviar("HOLA", nombreEscrito());
        refrescarSala();
        cartas.show(this, "sala");
        return true;
    }

    static String misIps() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        ips.add(a.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // nada
        }
        return ips.isEmpty() ? "(no encontré tu IP: ejecuta ipconfig y busca \"IPv4\")" : String.join("   o   ", ips);
    }

    // ===================== SALA =====================

    private JPanel panelSala() {
        JPanel p = new Fondo();
        p.setLayout(new BorderLayout(0, 18));
        p.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JPanel arriba = new JPanel();
        arriba.setOpaque(false);
        arriba.setLayout(new BoxLayout(arriba, BoxLayout.Y_AXIS));
        arriba.add(titulo("SALA DE EQUIPO"));
        infoSala.setFont(Estilo.fuente(Font.BOLD, 24f));
        infoSala.setForeground(Estilo.HOJA_OSCURA);
        infoSala.setAlignmentX(0.5f);
        arriba.add(Box.createVerticalStrut(6));
        arriba.add(infoSala);
        arriba.add(Box.createVerticalStrut(4));
        arriba.add(texto("Cada quien escoge un rol distinto. La partida empieza sola cuando estén los cuatro.", 20f, Estilo.TEXTO_SUAVE));
        p.add(arriba, BorderLayout.NORTH);

        JPanel cartasRol = new JPanel(new GridLayout(1, 4, 18, 0));
        cartasRol.setOpaque(false);
        for (Rol r : Rol.values()) {
            cartasRol.add(tarjetaRol(r));
        }
        p.add(cartasRol, BorderLayout.CENTER);

        JPanel abajo = new JPanel();
        abajo.setOpaque(false);
        abajo.setLayout(new BoxLayout(abajo, BoxLayout.Y_AXIS));
        estadoSala.setFont(Estilo.fuente(Font.BOLD, 28f));
        estadoSala.setForeground(Estilo.TEXTO);
        estadoSala.setAlignmentX(0.5f);
        Estilo.BotonMadera salir = boton("Salir de la sala");
        salir.addActionListener(e -> {
            cerrarRed();
            cartas.show(this, "conectar");
        });
        salir.setAlignmentX(0.5f);
        abajo.add(estadoSala);
        abajo.add(Box.createVerticalStrut(14));
        abajo.add(salir);
        p.add(abajo, BorderLayout.SOUTH);
        return p;
    }

    private JPanel tarjetaRol(Rol r) {
        JPanel t = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Estilo.suave(g);
                Estilo.madera(g, 4, 4, getWidth() - 12, getHeight() - 12, 36);
                Estilo.pergamino(g, 16, 16, getWidth() - 36, getHeight() - 36, 26);
                g.setColor(r.color);
                g.fillRoundRect(16, 16, getWidth() - 36, 18, 14, 14);
                g.dispose();
            }
        };
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBorder(BorderFactory.createEmptyBorder(46, 30, 28, 30));

        JLabel nombre = new JLabel(r.nombre.toUpperCase());
        nombre.setFont(Estilo.fuente(Font.BOLD, 26f));
        nombre.setForeground(Estilo.TEXTO);
        nombre.setAlignmentX(0.5f);

        JLabel frase = new JLabel("<html><div style='text-align:center;width:165px'>«" + r.frase + "»</div></html>");
        frase.setFont(Estilo.fuente(Font.ITALIC, 16f));
        frase.setForeground(Estilo.TEXTO_SUAVE);
        frase.setAlignmentX(0.5f);

        JLabel hab = new JLabel("<html><div style='text-align:center;width:165px'>Habilidad: <b>" + r.habilidad.nombre
                + "</b><br>Fortaleza: " + r.fortaleza + "<br>Carencia: " + r.carencia + "</div></html>");
        hab.setFont(Estilo.fuente(Font.PLAIN, 15f));
        hab.setForeground(Estilo.TEXTO);
        hab.setAlignmentX(0.5f);

        JLabel dueno = new JLabel("Libre");
        dueno.setFont(Estilo.fuente(Font.BOLD, 20f));
        dueno.setAlignmentX(0.5f);
        duenos.put(r, dueno);

        Estilo.BotonMadera b = boton("Escoger");
        b.setFont(Estilo.fuente(Font.BOLD, 16f));
        b.setPreferredSize(new Dimension(150, 46));
        b.setMaximumSize(new Dimension(150, 46));
        b.setAlignmentX(0.5f);
        b.addActionListener(e -> {
            if (red == null) {
                return;
            }
            red.enviar("ROL", miRol == r ? "-" : r.name());
        });
        botones.put(r, b);

        t.add(nombre);
        t.add(Box.createVerticalStrut(8));
        t.add(frase);
        t.add(Box.createVerticalStrut(12));
        t.add(hab);
        t.add(Box.createVerticalGlue());
        t.add(dueno);
        t.add(Box.createVerticalStrut(10));
        t.add(b);
        return t;
    }

    private void refrescarSala() {
        int tomados = 0;
        for (Rol r : Rol.values()) {
            int id = duenoId[r.ordinal()];
            String n = duenoNombre[r.ordinal()];
            JLabel l = duenos.get(r);
            Estilo.BotonMadera b = botones.get(r);
            if (id < 0) {
                l.setText("Libre");
                l.setForeground(Estilo.TEXTO_SUAVE);
                b.setText(miRol == null ? "Escoger" : "Cambiarme");
                b.setEnabled(!empezando);
            } else {
                tomados++;
                boolean yo = id == miId;
                l.setText(yo ? n + " (tú)" : n);
                l.setForeground(yo ? Estilo.HOJA_OSCURA : Estilo.TEXTO);
                b.setText(yo ? "Soltar" : "Ocupado");
                b.setEnabled(yo && !empezando);
            }
        }
        if (empezando) {
            estadoSala.setText("Preparando la partida…");
        } else if (cuenta > 0) {
            estadoSala.setText("¡Están los cuatro! Empieza en " + cuenta + "…");
        } else if (tomados == 0) {
            estadoSala.setText("Esperando a que escojan su rol (0 de 4)");
        } else {
            estadoSala.setText("Faltan " + (4 - tomados) + " rol(es) por escoger (" + tomados + " de 4)");
        }
    }

    // ===================== MENSAJES DEL SERVIDOR =====================

    @Override
    public void recibir(String[] m) {
        switch (m[0]) {
            case "BIENVENIDA":
                miId = Integer.parseInt(m[1]);
                break;
            case "ESTADO":
                miRol = null;
                for (Rol r : Rol.values()) {
                    int i = r.ordinal();
                    duenoId[i] = Integer.parseInt(m[1 + i * 2]);
                    duenoNombre[i] = m[2 + i * 2];
                    if (duenoId[i] == miId) {
                        miRol = r;
                    }
                }
                refrescarSala();
                break;
            case "CUENTA":
                cuenta = Integer.parseInt(m[1]);
                refrescarSala();
                break;
            case "RECHAZO":
                if (miId < 0) {
                    avisoConectar.setText(m[1]);
                    cerrarRed();
                    cartas.show(this, "conectar");
                } else {
                    JOptionPane.showMessageDialog(this, m[1], "Sala", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case "EMPEZAR":
                arrancar(m);
                break;
            default:
                break;
        }
    }

    private void arrancar(String[] m) {
        if (miRol == null) {
            return;
        }
        empezando = true;
        refrescarSala();
        // 1) misma semilla en las cuatro computadoras ANTES de crear nada que use azar
        Sincronia.preparar(Long.parseLong(m[1]));
        // 2) misma Partida en las cuatro (los jugadores siempre en el orden de los roles)
        Partida partida = new Partida();
        for (int i = 2; i + 1 < m.length; i += 2) {
            if ("MODO".equals(m[i])) {
                partida.feria = "FERIA".equals(m[i + 1]);
                continue;
            }
            partida.agregarJugador(m[i + 1], Rol.valueOf(m[i]));
        }
        RedMacondo.Cliente c = red;
        Rol mi = miRol;
        anfitrion.empezar(partida, mi, c);
    }

    @Override
    public void cerrado(String motivo) {
        if (empezando) {
            return;
        }
        boolean enSala = miId >= 0;
        cerrarRed();
        avisoConectar.setText(enSala ? "Se perdió la conexión con el anfitrión." : motivo);
        cartas.show(this, "conectar");
    }

    /** Cierra la conexion y, si yo era el anfitrion, la sala. */
    public void cerrarRed() {
        cerrarRed(false);
    }

    /**
     * demorar = true al terminar la partida: el anfitrion deja la sala abierta unos segundos mas para
     * que los otros terminen de ver el final antes de que se corte la conexion.
     */
    public void cerrarRed(boolean demorar) {
        if (red != null) {
            red.close();
            red = null;
        }
        if (sala != null) {
            final SalaMacondo s = sala;
            sala = null;
            if (demorar) {
                javax.swing.Timer t = new javax.swing.Timer(4000, e -> s.close());
                t.setRepeats(false);
                t.start();
            } else {
                s.close();
            }
        }
        miId = -1;
        miRol = null;
        empezando = false;
        cuenta = 0;
    }
}
