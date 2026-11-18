package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Un personaje (o cosa) del mundo: tiene posicion, tamano, caja de colision propia
 * y se dibuja solo. Si le das un PNG (imagen) usa ese sprite; si no, dibuja una
 * figurita provisional con codigo, para poder armar escenas sin esperar el arte.
 *
 * (x, y) es el punto de los PIES (centro de abajo). La colision es una caja chica
 * en los pies, asi se puede pasar "por detras" de las cosas sin pegarse con la cabeza.
 *
 * Uso:
 *   Actor dani = new Actor("dani", 500, 700, 150, 260)
 *           .colores(piel, ropa, pelo)
 *           .interactuar("Hablar", () -> ...);
 */
public class Actor {

    public final String id;
    public double x, y;
    public int ancho, alto;
    public int hitAncho = 56, hitAlto = 30;
    public boolean solido = true;
    public String etiqueta;
    /** true = no se dibuja (ej: Vlado, que es la voz de la tele dibujada en el fondo), pero si se puede hablar. */
    public boolean invisible;
    public Runnable alInteractuar;
    public String nombre, nombreNiebla;   // placa con el nombre (y como se llama cuando hay niebla)
    public boolean mirandoIzquierda, caminando;

    private String ruta;
    private boolean tieneImagen;
    // animacion por carpeta (ver sprites())
    private String carpeta;
    private final java.util.List<String> cuadros = new java.util.ArrayList<>();
    private String expresion;
    private static final double SEG_POR_CUADRO = 0.11;
    private Color piel = new Color(0xF2C9A0);
    private Color ropa = new Color(0x4C8BD9);
    private Color pelo = new Color(0x4A2E1A);
    private final double fase = Math.random() * 6.28;

    public Actor(String id, double x, double y, int ancho, int alto) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
    }

    // ---- configuracion encadenable ----

    public Actor colores(Color piel, Color ropa, Color pelo) {
        this.piel = piel;
        this.ropa = ropa;
        this.pelo = pelo;
        return this;
    }

    /** Usa un PNG como sprite (ej: "/imagenes/personajes/dani.png"). */
    public Actor imagen(String ruta) {
        this.ruta = ruta;
        this.tieneImagen = Recursos.existe(ruta);
        return this;
    }

    /**
     * Sprites por carpeta (para los 4 jugadores y para quien tenga expresiones):
     *   carpeta + "quieto.png"                  (obligatoria para que se use la carpeta)
     *   carpeta + "caminar_01.png" ... "_99"    (cuadros de caminata, opcionales)
     *   carpeta + "<expresion>.png"             (ej: "triste.png", opcionales)
     * Si falta quieto.png se dibuja la figurita provisional.
     */
    public Actor sprites(String carpeta) {
        this.carpeta = carpeta;
        cuadros.clear();
        for (int i = 1; i < 100; i++) {
            String r = carpeta + String.format("caminar_%02d.png", i);
            if (!Recursos.existe(r)) {
                break;
            }
            cuadros.add(r);
        }
        return imagen(carpeta + "quieto.png");
    }

    /**
     * Sprite de un rol jugable. Acepta tus rutas de siempre: personajes/ciudadano/quieto.png (con
     * animacion), personajes/ciudadano.png o, para el Ciudadano, personajecivil.png. Ver Rutas.spriteQuieto.
     */
    public Actor spritesDeRol(Rol r) {
        String q = Rutas.spriteQuieto(r);
        if (q != null && q.equals(r.sprites() + "quieto.png")) {
            return sprites(r.sprites());
        }
        this.carpeta = null;
        cuadros.clear();
        // cuadros de caminata en cualquiera de tus ubicaciones de siempre
        String P = "/imagenes/personajes/";
        String[] patrones = {
            r.sprites() + "caminar_%02d.png",
            P + r.carpeta + "_caminar_%02d.png",
            P + r.carpeta + "/caminar%02d.png",
            r == Rol.CIUDADANO ? "/imagenes/personaje/caminar_%02d.png" : "/imagenes/" + r.carpeta + "/caminar_%02d.png",
            "/imagenes/personaje" + r.carpeta + "/caminar_%02d.png"
        };
        for (String pat : patrones) {
            for (int ini = 1; ini >= 0; ini--) {
                java.util.List<String> lista = new java.util.ArrayList<>();
                for (int i = ini; i < 100; i++) {
                    String f = String.format(pat, i);
                    if (!Recursos.existe(f)) {
                        break;
                    }
                    lista.add(f);
                }
                if (!lista.isEmpty()) {
                    cuadros.addAll(lista);
                    return imagen(q != null ? q : r.sprites() + "quieto.png");
                }
            }
        }
        return imagen(q != null ? q : r.sprites() + "quieto.png");
    }

    /**
     * Cambia la cara: busca carpeta/<e>.png (si uso sprites) o <ruta>_<e>.png (si uso imagen).
     * null vuelve a la normal. Si la imagen no existe, no cambia nada.
     */
    public Actor expresion(String e) {
        this.expresion = e;
        return this;
    }

    private String rutaActual(double t) {
        if (expresion != null) {
            String r = carpeta != null ? carpeta + expresion + ".png"
                    : ruta.replace(".png", "_" + expresion + ".png");
            if (Recursos.existe(r)) {
                return r;
            }
        }
        if (caminando && !cuadros.isEmpty()) {
            return cuadros.get((int) (t / SEG_POR_CUADRO) % cuadros.size());
        }
        return ruta;
    }

    /** Placa con el nombre. En la niebla se muestra nombreNiebla (ej: "la senora"). */
    public Actor nombre(String nombre, String nombreNiebla) {
        this.nombre = nombre;
        this.nombreNiebla = nombreNiebla;
        return this;
    }

    public Actor hitbox(int ancho, int alto) {
        this.hitAncho = ancho;
        this.hitAlto = alto;
        return this;
    }

    public Actor solido(boolean solido) {
        this.solido = solido;
        return this;
    }

    public Actor interactuar(String etiqueta, Runnable accion) {
        this.etiqueta = etiqueta;
        this.alInteractuar = accion;
        return this;
    }

    public Rectangle2D.Double hitbox() {
        return new Rectangle2D.Double(x - hitAncho / 2.0, y - hitAlto, hitAncho, hitAlto);
    }

    // ---- dibujo ----

    public void dibujar(Graphics2D g, double t) {
        if (invisible) {
            return;
        }
        AffineTransform guardado = g.getTransform();

        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x - ancho * 0.38, y - 10, ancho * 0.76, 20));

        double bob = !cuadros.isEmpty() && caminando ? 0
                : caminando ? Math.abs(Math.sin(t * 10 + fase)) * 7 : Math.sin(t * 2 + fase) * 1.5;

        g.translate(x, y);
        if (mirandoIzquierda) {
            g.scale(-1, 1);
        }
        if (tieneImagen) {
            g.drawImage(Recursos.escalada(rutaActual(t), ancho, alto), -ancho / 2, (int) (-alto - bob), ancho, alto, null);
        } else {
            figura(g, t, bob);
        }
        g.setTransform(guardado);
    }

    private void figura(Graphics2D g, double t, double bob) {
        double a = ancho, h = alto;
        double r = a * 0.36;
        double cy = -h + r + 6 - bob;
        double cuerpoY = cy + r * 0.8;
        double cuerpoH = h * 0.40;
        double paso = caminando ? Math.sin(t * 10 + fase) * 7 : 0;
        Color linea = new Color(0x3B2210);
        BasicStroke borde = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(borde);

        // piernas
        g.setColor(ropa.darker());
        g.fill(new RoundRectangle2D.Double(-a * 0.22 + paso, -h * 0.17, a * 0.17, h * 0.17, 8, 8));
        g.fill(new RoundRectangle2D.Double(a * 0.05 - paso, -h * 0.17, a * 0.17, h * 0.17, 8, 8));

        // cuerpo
        Shape cuerpo = new RoundRectangle2D.Double(-a * 0.30, cuerpoY, a * 0.60, cuerpoH, 22, 22);
        g.setColor(ropa);
        g.fill(cuerpo);
        g.setColor(linea);
        g.draw(cuerpo);

        // brazos
        g.setColor(piel);
        g.fill(new Ellipse2D.Double(-a * 0.44, cuerpoY + cuerpoH * 0.25 + paso * 0.3, a * 0.15, a * 0.15));
        g.fill(new Ellipse2D.Double(a * 0.29, cuerpoY + cuerpoH * 0.25 - paso * 0.3, a * 0.15, a * 0.15));

        // pelo de atras, cabeza y flequillo
        g.setColor(pelo);
        g.fill(new Ellipse2D.Double(-r * 1.1, cy - r * 1.05, r * 2.2, r * 1.8));
        Shape cabeza = new Ellipse2D.Double(-r, cy - r * 0.9, r * 2, r * 1.9);
        g.setColor(piel);
        g.fill(cabeza);
        g.setColor(linea);
        g.draw(cabeza);
        g.setColor(pelo);
        g.fill(new Arc2D.Double(-r * 1.04, cy - r * 1.0, r * 2.08, r * 1.3, 0, 180, Arc2D.CHORD));

        // ojos (parpadean de vez en cuando)
        g.setColor(linea);
        boolean parpadeo = ((t + fase) % 4.0) < 0.12;
        if (parpadeo) {
            g.drawLine((int) (-r * 0.48), (int) (cy + r * 0.05), (int) (-r * 0.22), (int) (cy + r * 0.05));
            g.drawLine((int) (r * 0.22), (int) (cy + r * 0.05), (int) (r * 0.48), (int) (cy + r * 0.05));
        } else {
            g.fill(new Ellipse2D.Double(-r * 0.48, cy - r * 0.12, r * 0.24, r * 0.34));
            g.fill(new Ellipse2D.Double(r * 0.24, cy - r * 0.12, r * 0.24, r * 0.34));
        }
        g.setColor(new Color(255, 130, 130, 110));
        g.fill(new Ellipse2D.Double(-r * 0.78, cy + r * 0.28, r * 0.32, r * 0.2));
        g.fill(new Ellipse2D.Double(r * 0.46, cy + r * 0.28, r * 0.32, r * 0.2));
        g.setColor(linea);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(-r * 0.18, cy + r * 0.34, r * 0.36, r * 0.26, 200, 140, Arc2D.OPEN));
    }
}
