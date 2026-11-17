package juegoferia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Tarea cooperativa del APAGON: prender los faroles de las cuatro esquinas de la plaza (2 minutos).
 * Se juega a la vez en el mismo teclado: cada jugador tiene SU tecla y SU esquina.
 *
 *   Ciudadano = A (esquina del río)      Periodista = F (esquina de la panadería)
 *   Influencer = J (esquina de la Junta) Candidato  = L (esquina de la Alcaldía)
 *
 * Cada farol necesita varios golpes de fosforo. A veces a alguien "le falta fosforo": su farol
 * se detiene hasta que OTRO jugador pulse su tecla para pasarle uno ("Te paso uno").
 */
public class MjFaroles extends Minijuego {

    private static final double DURACION = 120;
    private static final int GOLPES = 14;
    private static final int[] TECLAS = {KeyEvent.VK_A, KeyEvent.VK_F, KeyEvent.VK_J, KeyEvent.VK_L};
    private static final String[] LETRAS = {"A", "F", "J", "L"};
    private static final String[] ESQUINAS = {"Esquina del río", "Esquina de la panadería",
        "Esquina de la Junta", "Esquina de la Alcaldía"};
    private static final double[][] POS = {{360, 330}, {1560, 330}, {360, 800}, {1560, 800}};

    private final int[] golpes = new int[4];
    private final boolean[] sinFosforo = new boolean[4];
    private double restante, proximoApuro;
    private final Random azar = Sincronia.nuevoRandom();

    @Override
    public String nombre() {
        return "Los faroles";
    }

    @Override
    public String instrucciones() {
        return "Sin señal toca prender los faroles de las cuatro esquinas. Cada quien tiene su tecla: "
                + "Ciudadano A, Periodista F, Influencer J, Candidato L. Si a alguien le falta fósforo, "
                + "otro le pasa uno pulsando su propia tecla. Tienen dos minutos.";
    }

    @Override
    protected void prepararPartida() {
        for (int i = 0; i < 4; i++) {
            golpes[i] = 0;
            sinFosforo[i] = false;
        }
        restante = DURACION;
        proximoApuro = 12;
    }

    private int encendidos() {
        int n = 0;
        for (int g : golpes) {
            if (g >= GOLPES) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void actualizarJuego(double dt) {
        restante -= dt;
        proximoApuro -= dt;
        if (proximoApuro <= 0) {
            proximoApuro = 10 + azar.nextInt(8);
            int i = azar.nextInt(4);
            if (golpes[i] < GOLPES) {
                sinFosforo[i] = true;
            }
        }
        if (encendidos() == 4 || restante <= 0) {
            int n = encendidos();
            int nv = nivel(n, 4, 3);
            String r = nv == BIEN ? "Los cuatro se quedan juntos bajo la luz. «Gracias por la esquina de allá.»"
                    : nv == REGULAR ? "Una esquina queda a oscuras. Doña Úrsula: Alguien dejó la suya. Eso se nota."
                    : "Dos esquinas a oscuras. La niebla de la noche entra por ahí.";
            Efecto ef = nv == BIEN ? Efecto.de(Partida.ANIMO, 2, Partida.CONFIANZA_ROLES, 1)
                    : nv == MAL ? Efecto.de(Partida.ANIMO, -1) : new Efecto();
            terminarJuego(nv != MAL, nv, ef, r + " (" + n + " de 4 faroles)");
        }
    }

    @Override
    protected void teclaJuego(int k) {
        for (int i = 0; i < 4; i++) {
            if (k != TECLAS[i]) {
                continue;
            }
            // primero: si alguien necesita fosforo, este jugador se lo pasa
            for (int j = 0; j < 4; j++) {
                if (j != i && sinFosforo[j]) {
                    sinFosforo[j] = false;
                    return;
                }
            }
            if (!sinFosforo[i] && golpes[i] < GOLPES) {
                golpes[i]++;
                if (golpes[i] == GOLPES) {
                    Funciones.reproducirSonido("/sonidos/noticia.wav");
                }
            }
        }
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        // el mismo fondo del apagon (o uno propio en /imagenes/minijuegos/plaza_apagon.png)
        String fa = Recursos.existe("/imagenes/minijuegos/plaza_apagon.png") ? "/imagenes/minijuegos/plaza_apagon.png"
                : Rutas.primera(Rutas.PLAZA_APAGON);
        if (Recursos.existe(fa)) {
            fondo(g, fa);
        } else {
            g.setColor(new Color(10, 12, 30));
            g.fillRect(0, 0, W, H);
        }
        // oscuridad que se va levantando
        g.setColor(new Color(0, 0, 20, 150 - encendidos() * 30));
        g.fillRect(0, 0, W, H);

        g.setFont(Estilo.fuente(Font.BOLD, 52f));
        Estilo.textoContorno(g, nombre() + "  ·  " + (int) Math.max(0, restante) + " s", 60, 90,
                Estilo.DORADO_CLARO, Estilo.MADERA_OSCURA);

        Rol[] roles = Rol.values();
        for (int i = 0; i < 4; i++) {
            double x = POS[i][0], y = POS[i][1];
            double f = golpes[i] / (double) GOLPES;
            if (f >= 1) {
                float r = 260;
                g.setPaint(new RadialGradientPaint((float) x, (float) y - 120, r, new float[]{0f, 1f},
                        new Color[]{new Color(255, 220, 120, 200), new Color(255, 220, 120, 0)}));
                g.fill(new Ellipse2D.Double(x - r, y - 120 - r, r * 2, r * 2));
            }
            g.setColor(Estilo.MADERA_OSCURA);
            g.fillRect((int) x - 8, (int) y - 120, 16, 200);
            g.setColor(f >= 1 ? new Color(0xFFE08A) : new Color(80, 80, 100));
            g.fillRoundRect((int) x - 40, (int) y - 200, 80, 90, 20, 20);
            barra(g, (int) x - 120, (int) y + 100, 240, 18, Math.min(1, f), roles[i].color);
            g.setFont(Estilo.fuente(Font.BOLD, 26f));
            g.setColor(Color.WHITE);
            centrado(g, ESQUINAS[i], (int) x, (int) y + 150);
            centrado(g, roles[i].nombre + " · tecla " + LETRAS[i], (int) x, (int) y + 186);
            if (sinFosforo[i]) {
                g.setColor(new Color(0xFF8A7A));
                centrado(g, "¡Me falta fósforo!", (int) x, (int) y - 230);
            }
        }
    }
}
