package juegoferia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Minijuego fijo de la JUNTA: "El papel que nadie leyó". Lo juega Candidato.
 * Hay una hoja de cuentas de 12 lineas y una acusacion con tres cifras. Para cada cifra
 * se marca: coincide, no coincide o no aparece.
 */
public class MjPapelQueNadieLeyo extends Minijuego {

    private static final String[] HOJA = {
        "1. Saldo inicial de la Junta .................. $ 650.000",
        "2. Cuotas de afiliados (enero) ................ $ 320.000",
        "3. Bazar comunal ............................. $ 410.000",
        "4. Pago de servicios del salón ............... $ 180.000",
        "5. Ingreso del alcantarillado (cuenta Junta) . $ 4.800.000",
        "6. Papelería y fotocopias .................... $ 45.000",
        "7. Refrigerios asamblea ...................... $ 90.000",
        "8. Arreglo de la escalera .................... $ 260.000",
        "9. Pago contratista Rivera, factura 112 ...... $ 1.200.000",
        "10. Cuotas de afiliados (febrero) ............ $ 300.000",
        "11. Transporte a la gobernación .............. $ 60.000",
        "12. Saldo en cuenta .......................... $ 4.645.000"
    };

    private static final String[] ACUSACIONES = {
        "«Se perdieron $4.800.000 del alcantarillado»",
        "«Pagaron $1.200.000 a un contratista que no existe»",
        "«Hay $2.000.000 sin soporte»"
    };

    private static final String[] OPCIONES = {"Coincide", "No coincide", "No aparece"};

    /** Respuesta correcta de cada acusacion (indice en OPCIONES). */
    private static final int[] CORRECTA = {0, 0, 2};

    private final boolean pista;
    private final int[] marca = new int[3];
    private final Rectangle revisar = new Rectangle(1500, 940, 360, 90);
    private int consejo;

    /** pista = alguien verifico la foto: la primera cifra ya viene marcada. */
    public MjPapelQueNadieLeyo(boolean pista) {
        this.pista = pista;
    }

    @Override
    public String nombre() {
        return "El papel que nadie leyó";
    }

    @Override
    protected String carpetaArte() {
        return "papel";
    }

    @Override
    protected void iconoAviso(Graphics2D g, int cx, int cy) {
        if (!ArteMj.pegar(g, "papel", "papel_documento_oficial", cx - 20, cy, 190)) {
            ArteMj.hojaPapel(g, cx - 70, cy - 90, 140, 180, ArteMj.CREMA);
        }
        ArteMj.lupa(g, cx + 70, cy + 70, 110);
    }

    @Override
    public String instrucciones() {
        return "Amaranta te pasa la tercera carpeta. Busca cada cifra de la acusación en la hoja de cuentas "
                + "y marca si coincide, si no coincide o si no aparece.";
    }

    @Override
    protected void prepararPartida() {
        for (int i = 0; i < 3; i++) {
            marca[i] = -1;
        }
        if (pista) {
            marca[0] = CORRECTA[0];
        }
        consejo = 0;
    }

    private Rectangle rectOpcion(int acusacion, int op) {
        return new Rectangle(1080 + op * 260, 250 + acusacion * 220 + 70, 240, 70);
    }

    @Override
    protected void actualizarJuego(double dt) {
    }

    @Override
    protected void clicJuego(Point p, int boton) {
        for (int a = 0; a < 3; a++) {
            for (int o = 0; o < 3; o++) {
                if (rectOpcion(a, o).contains(p) && !(pista && a == 0)) {
                    marca[a] = o;
                    consejo++;
                    return;
                }
            }
        }
        if (revisar.contains(p)) {
            for (int m : marca) {
                if (m < 0) {
                    return;
                }
            }
            int aciertos = 0;
            for (int i = 0; i < 3; i++) {
                if (marca[i] == CORRECTA[i]) {
                    aciertos++;
                }
            }
            int nv = nivel(aciertos, 3, 2);
            String r = nv == BIEN
                    ? "El dinero no se perdió: está depositado. El contratista existe: figura con factura. "
                    + "Y los dos millones no aparecen en ningún papel."
                    : nv == REGULAR ? "Amaranta: Casi. Revise la de los dos millones."
                    : "Amaranta: Siéntese. Yo se lo explico con el papel en la mano.";
            terminarJuego(nv != MAL, nv,
                    nv == BIEN ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, EstadoJuego.CONFIANZA, 1, Partida.DESINFO_LUGAR, -3)
                            : nv == REGULAR ? Efecto.de(EstadoJuego.INFO_VERIFICADA, 1) : new Efecto(), r);
        }
    }

    @Override
    protected void dibujarJuego(Graphics2D g) {
        ArteMj.fondoOficial(g, Rutas.JUNTA, 100);
        String[] cs = {"Busca el monto exacto.", "Mira la línea del contratista.", "Si no aparece, márcalo."};
        cabecera(g, nombre(), cs[consejo % cs.length]);

        // hoja de cuentas
        ArteMj.hojaPapel(g, 50, 140, 980, 830, ArteMj.CREMA);
        ArteMj.pegar(g, "papel", "papel_tabla_datos", 940, 190, 100);
        g.setFont(ArteMj.letra(28f));
        g.setColor(ArteMj.TEXTO);
        g.drawString("Hoja de cuentas de la Junta", 90, 190);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
        for (int i = 0; i < HOJA.length; i++) {
            if (i % 2 == 0) {
                g.setColor(new Color(246, 226, 180, 140));
                g.fillRect(80, 222 + i * 60, 920, 54);
            }
            g.setColor(ArteMj.TEXTO);
            g.drawString(HOJA[i], 90, 258 + i * 60);
        }

        // acusaciones
        String[] tipo = {"Coincide", "No coincide", "No aparece"};
        int[] color = {0, 1, 2};
        for (int a = 0; a < 3; a++) {
            int y = 250 + a * 220;
            ArteMj.panel(g, 1060, y - 30, 820, 196, ArteMj.CREMA);
            ArteMj.pegar(g, "papel", "papel_etiqueta_cifra", 1130, y + 8, 64);
            g.setFont(ArteMj.letra(26f));
            ArteMj.parrafo(g, ACUSACIONES[a], 1200, y + 18, 660, ArteMj.TEXTO);
            for (int o = 0; o < 3; o++) {
                Rectangle r = rectOpcion(a, o);
                Rectangle rr = new Rectangle(r.x, r.y + 6, r.width, r.height - 8);
                if (marca[a] == o) {
                    ArteMj.estado(g, rr, tipo[o], color[o]);
                } else {
                    ArteMj.opcion(g, rr, OPCIONES[o], false);
                }
            }
        }
        boolean completo = marca[0] >= 0 && marca[1] >= 0 && marca[2] >= 0;
        boton(g, revisar, "Revisar", completo);
    }
}
