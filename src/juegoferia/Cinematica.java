package juegoferia;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Cinematicas con fondos y sprites independientes.
 * Los sprites se declaran por cuadro con .sprite(ruta, x, y, ancho, alto).
 * Si un PNG aun no existe, se dibuja un marcador provisional con el nombre del recurso.
 */
public class Cinematica extends PanelDiseno {

    /** El boton del parlante (volumen) tambien esta aqui. */
    @Override
    protected boolean pausaConEscape() {
        return false;          // aqui ESC salta la cinematica; se pausa con el boton ||
    }

    @Override
    protected boolean conAjustes() {
        return true;
    }

    private static final class SpriteEscena {
        final String ruta;
        final int x, y, ancho, alto;
        final double rotacion;
        boolean respira;   // opcional: leve movimiento de respiracion (modo cine)

        SpriteEscena(String ruta, int x, int y, int ancho, int alto, double rotacion) {
            this.ruta = ruta;
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.rotacion = rotacion;
        }
    }

    private static final class Cuadro {
        String fondo, acotacion, sonido, ambiente;
        String tituloGrande, tituloPequeno;
        final List<String[]> lineas = new ArrayList<>();
        final List<SpriteEscena> sprites = new ArrayList<>();
        // ---- extras de modo cine (todos opcionales) ----
        final List<EfectosCine.Efecto> efectos = new ArrayList<>();
        double[] camara;          // {zoomIni, zoomFin, dx, dy, duracion}
        double duracion;          // segundos; avanza solo (solo cuadros sin dialogo)
        // ---- audio controlable (AudioCine) ----
        String musica;
        double musicaVol = 0.7, musicaFade = 1.5;
        boolean silencio;
        double silencioFade = 1.5;
        final List<SonidoEscena> sonidos = new ArrayList<>();
    }

    private static final class SonidoEscena {
        final String ruta;
        final double retraso, volumen, maxSeg;
        boolean disparado;

        SonidoEscena(String ruta, double retraso, double volumen, double maxSeg) {
            this.ruta = ruta;
            this.retraso = retraso;
            this.volumen = volumen;
            this.maxSeg = maxSeg;
        }
    }

    private static final double LETRAS_POR_SEG = 38;
    private static final double FUNDIDO_SALIDA = 0.45;   // segundos del fundido a negro entre cuadros
    private static final double FUNDIDO_ENTRADA = 1.0;   // segundos del fundido desde negro
    private boolean mantenerMusica;    // true: la musica sigue sonando al terminar la cinematica
    private boolean cine;              // modo cine: camara, efectos, vineta, grano, fundidos
    private double tEscena;            // tiempo desde que empezo el cuadro (no se reinicia por linea)
    private double salida = -1;        // >= 0 mientras hace el fundido a negro de salida
    private final Partida partida;
    private final List<Cuadro> cuadros = new ArrayList<>();
    private int iCuadro = -1, iLinea = -1;
    private double letras, tCuadro;
    private boolean saltable = true;
    private Runnable alTerminar;
    private boolean terminada;

    private static final java.awt.Rectangle BOTON_SALTAR = new java.awt.Rectangle(1660, 30, 230, 64);

    public Cinematica(Partida partida) {
        this.partida = partida;
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && saltable) {
                    terminar();
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    avanzar();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (saltable && BOTON_SALTAR.contains(aDiseno(e.getPoint()))) {
                    terminar();
                } else {
                    avanzar();
                }
            }
        });
    }

    // ===================== ARMAR =====================

    public Cinematica cuadro(String fondo, String acotacion, String... lineas) {
        Cuadro c = new Cuadro();
        c.fondo = fondo;
        c.acotacion = acotacion;
        for (String l : lineas) {
            String t = partida != null ? partida.reemplazar(l) : l;
            int i = t.indexOf(": ");
            String quien = i > 0 && i < 28 ? t.substring(0, i) : null;
            String texto = quien != null ? t.substring(i + 2) : t;
            if (quien != null && partida != null) {
                for (Rol r : Rol.values()) {
                    if (quien.equals(r.name()) && partida.jugador(r) != null) {
                        quien = partida.jugador(r).nombre;
                    }
                }
            }
            c.lineas.add(new String[]{quien == null ? null : bonito(quien), texto});
        }
        cuadros.add(c);
        return this;
    }

    /** Agrega un efecto de una sola reproducción al último cuadro. */
    public Cinematica sonido(String ruta) {
        if (!cuadros.isEmpty()) cuadros.get(cuadros.size() - 1).sonido = ruta;
        return this;
    }

    /** Agrega un ambiente en bucle al último cuadro. */
    public Cinematica ambiente(String ruta) {
        if (!cuadros.isEmpty()) cuadros.get(cuadros.size() - 1).ambiente = ruta;
        return this;
    }

    /**
     * Agrega un PNG transparente sobre el fondo del cuadro actual.
     * x/y/ancho/alto se expresan en el lienzo de diseño 1920x1080.
     */
    public Cinematica sprite(String ruta, int x, int y, int ancho, int alto) {
        return sprite(ruta, x, y, ancho, alto, 0);
    }

    /** Igual que sprite(...), pero permite girarlo; util para reutilizar el sprite acostado. */
    public Cinematica sprite(String ruta, int x, int y, int ancho, int alto, double rotacionGrados) {
        if (!cuadros.isEmpty() && ruta != null && ancho > 0 && alto > 0) {
            cuadros.get(cuadros.size() - 1).sprites.add(
                    new SpriteEscena(ruta, x, y, ancho, alto, rotacionGrados));
        }
        return this;
    }

    public Cinematica titulo(String grande, String pequeno) {
        Cuadro c = new Cuadro();
        c.tituloGrande = grande;
        c.tituloPequeno = pequeno;
        cuadros.add(c);
        return this;
    }

    public Cinematica saltable(boolean s) {
        this.saltable = s;
        return this;
    }

    // ---------- MODO CINE (todo opcional: si no lo usas, la cinematica se ve igual que antes) ----------

    /** Activa el look de pelicula: fundidos a negro, vineta, grano, camara y efectos. */
    public Cinematica cine() {
        this.cine = true;
        return this;
    }

    private Cuadro ultimo() {
        return cuadros.isEmpty() ? null : cuadros.get(cuadros.size() - 1);
    }

    /** Una capa dibujada a mano (en pantalla, no se mueve con la camara). */
    public Cinematica capa(EfectosCine.Capa c) {
        return efecto(new EfectosCine.Efecto(false, c));
    }

    private Cinematica efecto(EfectosCine.Efecto e) {
        Cuadro c = ultimo();
        if (c != null) c.efectos.add(e);
        return this;
    }

    /**
     * Movimiento de camara (zoom + paneo) sobre fondo y sprites. El zoom se ajusta solo para que
     * nunca se vean bordes negros. dx/dy = cuantos pixeles recorre el paneo en total.
     */
    public Cinematica camara(double zoomIni, double zoomFin, int dx, int dy) {
        Cuadro c = ultimo();
        if (c != null) c.camara = new double[]{zoomIni, zoomFin, dx, dy, 22};
        return this;
    }

    /** El cuadro avanza solo despues de N segundos (solo cuadros sin dialogo). */
    public Cinematica duracion(double segundos) {
        Cuadro c = ultimo();
        if (c != null) c.duracion = segundos;
        return this;
    }

    /** Hace que el ultimo sprite agregado "respire" (ideal para personajes dormidos). */
    public Cinematica respirar() {
        Cuadro c = ultimo();
        if (c != null && !c.sprites.isEmpty()) c.sprites.get(c.sprites.size() - 1).respira = true;
        return this;
    }

    // ---------- AUDIO CONTROLABLE ----------

    /** Musica de fondo en bucle desde este cuadro (volumen 0.7, entra con fundido de 1.5 s). */
    public Cinematica musica(String ruta) {
        return musica(ruta, 0.7, 1.5);
    }

    /**
     * Musica de fondo en bucle desde este cuadro. Sigue sonando en los cuadros siguientes
     * hasta que uno use .silencio(...) u otra .musica(...).
     * volumen 0..1; fadeSeg = segundos de entrada gradual (0 = de golpe).
     */
    public Cinematica musica(String ruta, double volumen, double fadeSeg) {
        Cuadro c = ultimo();
        if (c != null) {
            c.musica = ruta;
            c.musicaVol = volumen;
            c.musicaFade = fadeSeg;
        }
        return this;
    }

    /** Apaga la musica al empezar este cuadro, con un fundido de N segundos (0 = corte seco). */
    public Cinematica silencio(double fadeSeg) {
        Cuadro c = ultimo();
        if (c != null) {
            c.silencio = true;
            c.silencioFade = fadeSeg;
        }
        return this;
    }

    /**
     * Efecto de sonido de este cuadro.
     *
     * @param retraso  segundos despues de que empieza el cuadro (0 = al entrar)
     * @param volumen  0..1
     * @param maxSeg   lo corta a esos segundos aunque dure mas (0 = hasta que termine solo)
     */
    public Cinematica efectoSonido(String ruta, double retraso, double volumen, double maxSeg) {
        Cuadro c = ultimo();
        if (c != null && ruta != null) c.sonidos.add(new SonidoEscena(ruta, retraso, volumen, maxSeg));
        return this;
    }

    public Cinematica efectoSonido(String ruta, double retraso) {
        return efectoSonido(ruta, retraso, 1.0, 0);
    }

    /** Por defecto la musica se apaga con fundido al terminar/saltar. Con true sigue sonando (ej. hacia el menu). */
    public Cinematica mantenerMusicaAlTerminar(boolean mantener) {
        this.mantenerMusica = mantener;
        return this;
    }

    public Cinematica estrellas() {
        return efecto(EfectosCine.estrellas());
    }

    /** Olas con espuma. yOrilla: linea donde llega el agua (mar arriba, arena abajo). */
    public Cinematica olas(int yOrilla) {
        return efecto(EfectosCine.olas(yOrilla));
    }

    /** fuerza: 1 = normal, 0.5 = brisa suave, 1.6 = ventarron. */
    public Cinematica viento(double fuerza) {
        return efecto(EfectosCine.viento(fuerza));
    }

    /** Niebla que se va espesando. */
    public Cinematica niebla() {
        return efecto(EfectosCine.bruma(0xDCE3E6, 0.55, 26, 120, 980, true, 16));
    }

    /** Nubes que cruzan suavemente (para tomas aereas). */
    public Cinematica nubes() {
        return efecto(EfectosCine.bruma(0xFFFFFF, 0.30, 34, 0, 1000, false, 9));
    }

    /** Motas de polvo/luz flotando. rgb = color, ej. 0xFFD78A. */
    public Cinematica polvo(int rgb, int cantidad) {
        return efecto(EfectosCine.polvo(rgb, cantidad));
    }

    public Cinematica rayos(int rgb) {
        return efecto(EfectosCine.rayos(rgb));
    }

    /** Luz pulsante (pantalla de celular, farol...). */
    public Cinematica resplandor(int x, int y, int radio, int rgb, double fase) {
        return efecto(EfectosCine.resplandor(x, y, radio, rgb, fase));
    }

    /** Mariposa amarilla animada: sale de (x0,y0), serpentea hasta (x1,y1) y revolotea. */
    public Cinematica mariposa(int x0, int y0, int x1, int y1) {
        return efecto(EfectosCine.mariposa(x0, y0, x1, y1));
    }

    /** El rumor: linea de luz que salta por los puntos (x,y,x,y,...) y se rompe en pedazos. */
    public Cinematica rumor(int... puntos) {
        return efecto(EfectosCine.rumor(puntos));
    }

    /** Celulares que se encienden y dejan una sola raya de senal (con corte a negro al final). */
    public Cinematica senal(int cx, int cy, int... telefonos) {
        return efecto(EfectosCine.senal(cx, cy, telefonos));
    }

    private static String bonito(String quien) {
        if (!quien.equals(quien.toUpperCase())) return quien;
        StringBuilder sb = new StringBuilder();
        for (String p : quien.toLowerCase().split(" ")) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    // ===================== CICLO =====================

    public void iniciar(Runnable alTerminar) {
        this.alTerminar = alTerminar;
        terminada = false;
        iCuadro = -1;
        salida = -1;
        siguienteCuadro();
    }

    private void siguienteCuadro() {
        if (cine && iCuadro >= 0 && iCuadro < cuadros.size() && !terminada) {
            // en modo cine primero se funde a negro; actualizar() hace el cambio real
            if (salida < 0) salida = 0;
            return;
        }
        irACuadro();
    }

    private void irACuadro() {
        detenerAmbienteActual();
        iCuadro++;
        iLinea = -1;
        tCuadro = 0;
        tEscena = 0;
        letras = 0;
        if (iCuadro >= cuadros.size()) {
            terminar();
            return;
        }
        Cuadro c = cuadros.get(iCuadro);
        if (c.sonido != null) Funciones.reproducirSonido(c.sonido);
        if (c.ambiente != null) Musica.tocar(c.ambiente);
        if (c.silencio) AudioCine.detenerMusica(c.silencioFade);
        if (c.musica != null) AudioCine.musica(c.musica, c.musicaVol, c.musicaFade);
        for (SonidoEscena so : c.sonidos) so.disparado = false;
        siguienteLinea();
    }

    private void siguienteLinea() {
        if (terminada || iCuadro < 0 || iCuadro >= cuadros.size()) return;
        Cuadro c = cuadros.get(iCuadro);
        iLinea++;
        letras = 0;
        tCuadro = 0;
        // Un cuadro sin dialogo (solo imagen) ahora espera un clic (o su .duracion) en vez de saltarse solo.
    }

    private void detenerAmbienteActual() {
        if (iCuadro >= 0 && iCuadro < cuadros.size()) {
            Cuadro actual = cuadros.get(iCuadro);
            if (actual.ambiente != null) Musica.parar();
        }
    }

    @Override
    protected void alOcultar() {
        // Evita que un ambiente siga sonando si otra pantalla reemplaza esta cinematica.
        detenerAmbienteActual();
        cortarAudioCine();
    }

    private void cortarAudioCine() {
        AudioCine.detenerEfectos();
        if (!mantenerMusica) AudioCine.detenerMusica(1.2);
    }

    private void avanzar() {
        if (terminada || salida >= 0 || iCuadro < 0 || iCuadro >= cuadros.size()) return;
        Cuadro c = cuadros.get(iCuadro);
        if (iLinea >= 0 && iLinea < c.lineas.size()) {
            String t = c.lineas.get(iLinea)[1];
            if (letras < t.length()) {
                letras = t.length();
                return;
            }
        }
        if (iLinea + 1 < c.lineas.size()) {
            siguienteLinea();
        } else if (tCuadro > 0.4) {
            siguienteCuadro();
        }
    }

    private void terminar() {
        if (terminada) return;
        detenerAmbienteActual();
        cortarAudioCine();
        terminada = true;
        if (alTerminar != null) {
            Runnable r = alTerminar;
            alTerminar = null;
            r.run();
        }
    }

    @Override
    protected void actualizar(double dt) {
        tCuadro += dt;
        tEscena += dt;
        letras += dt * LETRAS_POR_SEG * Ajustes.velocidadTexto();
        if (terminada) return;
        if (salida >= 0) {
            salida += dt;
            if (salida >= FUNDIDO_SALIDA) {
                salida = -1;
                irACuadro();
            }
            return;
        }
        if (iCuadro >= 0 && iCuadro < cuadros.size()) {
            for (SonidoEscena so : cuadros.get(iCuadro).sonidos) {
                if (!so.disparado && tEscena >= so.retraso) {
                    so.disparado = true;
                    AudioCine.efecto(so.ruta, so.volumen, so.maxSeg);
                }
            }
        }
        if (cine && iCuadro >= 0 && iCuadro < cuadros.size()) {
            Cuadro c = cuadros.get(iCuadro);
            if (c.duracion > 0 && c.lineas.isEmpty() && tEscena >= c.duracion) siguienteCuadro();
        }
    }

    // ===================== DIBUJO =====================

    @Override
    protected void dibujar(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        if (iCuadro < 0 || iCuadro >= cuadros.size()) return;
        Cuadro c = cuadros.get(iCuadro);
        float entrada = (float) Math.min(1, tCuadro / 0.6);

        if (c.tituloGrande != null && cine) {
            EfectosCine.titulo(g, c.tituloGrande, c.tituloPequeno, tEscena,
                    Estilo.fuente(Font.BOLD, 96f), Estilo.fuente(Font.PLAIN, 34f));
            pista(g);
            capasDePelicula(g);
            return;
        }

        if (c.tituloGrande != null) {
            g.setColor(new Color(255, 211, 92, (int) (255 * entrada)));
            g.setFont(Estilo.fuente(Font.BOLD, 96f));
            centrado(g, c.tituloGrande, W / 2, H / 2);
            if (c.tituloPequeno != null) {
                g.setFont(Estilo.fuente(Font.PLAIN, 36f));
                g.setColor(new Color(255, 255, 255, (int) (220 * entrada)));
                centrado(g, c.tituloPequeno, W / 2, H / 2 + 80);
            }
            pista(g);
            return;
        }

        // "mundo" = fondo + sprites + efectos anclados al fondo; en modo cine se mueve con la camara.
        Graphics2D mundo = (Graphics2D) g.create();
        if (cine) aplicarCamara(mundo, c);

        if (c.fondo != null && Recursos.existe(c.fondo)) {
            // Las cinematicas usan lienzo 1920x1080; no reutilizar el escalado 1591x989 del mundo.
            BufferedImage fondo = Recursos.imagen(c.fondo, W, H);
            mundo.drawImage(fondo, 0, 0, W, H, null);
        } else if (c.acotacion != null) {
            mundo.setFont(Estilo.fuente(Font.PLAIN, 34f));
            mundo.setColor(new Color(200, 190, 170));
            FontMetrics fm = mundo.getFontMetrics();
            int y = 300;
            for (String l : envolver("(" + c.acotacion + ")", fm, 1400)) {
                centrado(mundo, l, W / 2, y);
                y += fm.getHeight() + 6;
            }
            if (c.fondo != null) {
                mundo.setFont(Estilo.fuente(Font.PLAIN, 18f));
                mundo.setColor(new Color(120, 120, 120));
                centrado(mundo, "falta la imagen: " + c.fondo, W / 2, 1050);
            }
        }

        // Los sprites se dibujan encima del fondo y debajo de la caja de diálogo.
        for (SpriteEscena sprite : c.sprites) dibujarSprite(mundo, sprite);

        if (cine) {
            for (EfectosCine.Efecto e : c.efectos) {
                if (e.mundo) {
                    Graphics2D q = (Graphics2D) mundo.create();
                    e.capa.pintar(q, tEscena);
                    q.dispose();
                }
            }
        }
        mundo.dispose();

        if (cine) {
            for (EfectosCine.Efecto e : c.efectos) {
                if (!e.mundo) {
                    Graphics2D q = (Graphics2D) g.create();
                    e.capa.pintar(q, tEscena);
                    q.dispose();
                }
            }
            EfectosCine.vineta(g);
        }

        if (iLinea >= 0 && iLinea < c.lineas.size()) {
            if (cine) {
                Graphics2D q = (Graphics2D) g.create();
                double a = Math.min(1, Math.max(0, (tEscena - 0.6) / 0.5));
                q.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) a));
                q.translate(0, (1 - a) * 24);
                dibujarLinea(q, c.lineas.get(iLinea));
                q.dispose();
                indicadorContinuar(g, c.lineas.get(iLinea));
            } else {
                dibujarLinea(g, c.lineas.get(iLinea));
            }
        }
        if (cine) capasDePelicula(g);
        if (saltable) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(BOTON_SALTAR.x, BOTON_SALTAR.y, BOTON_SALTAR.width, BOTON_SALTAR.height, 32, 32);
            g.setColor(Color.WHITE);
            g.setFont(Estilo.fuente(Font.BOLD, 26f));
            centrado(g, "Saltar (Esc)", BOTON_SALTAR.x + BOTON_SALTAR.width / 2, BOTON_SALTAR.y + 42);
        }
    }

    // ---------- ayudas del modo cine ----------

    /** Zoom + paneo lentos, con el zoom ajustado para que nunca asomen bordes negros. */
    private void aplicarCamara(Graphics2D g, Cuadro c) {
        if (c.camara == null) return;
        double p = Math.min(1, tEscena / c.camara[4]);
        p = p * p * (3 - 2 * p) * 0.35 + p * 0.65;           // casi lineal, con arranque suave
        double z = c.camara[0] + (c.camara[1] - c.camara[0]) * p;
        double maxX = (z - 1) * W / 2, maxY = (z - 1) * H / 2;
        double ox = Math.max(-maxX, Math.min(maxX, (p - 0.5) * c.camara[2]));
        double oy = Math.max(-maxY, Math.min(maxY, (p - 0.5) * c.camara[3]));
        g.translate(W / 2.0 + ox, H / 2.0 + oy);
        g.scale(z, z);
        g.translate(-W / 2.0, -H / 2.0);
    }

    /** Grano, parpadeo de proyector y los fundidos a negro (entrada y salida). */
    private void capasDePelicula(Graphics2D g) {
        EfectosCine.grano(g, tiempo);
        double negro = 0;
        if (salida >= 0) {
            negro = Math.min(1, salida / FUNDIDO_SALIDA);
        } else if (tEscena < FUNDIDO_ENTRADA) {
            double k = 1 - tEscena / FUNDIDO_ENTRADA;
            negro = k * k;
        }
        if (negro > 0) {
            g.setColor(new Color(0, 0, 0, (int) (255 * negro)));
            g.fillRect(0, 0, W, H);
        }
    }

    /** Triangulito que late cuando el texto ya termino de escribirse. */
    private void indicadorContinuar(Graphics2D g, String[] l) {
        if (letras < l[1].length() || tEscena < 0.8) return;
        double pulso = 0.5 + 0.5 * Math.sin(tEscena * 5);
        int x = 260 + 1400 - 90, y = 790 + 230 - 62 + (int) (pulso * 6);
        g.setColor(new Color(Estilo.MADERA_OSCURA.getRed(), Estilo.MADERA_OSCURA.getGreen(),
                Estilo.MADERA_OSCURA.getBlue(), (int) (120 + 120 * pulso)));
        g.fillPolygon(new int[]{x, x + 28, x + 14}, new int[]{y, y, y + 16}, 3);
    }

    private void dibujarSprite(Graphics2D g, SpriteEscena s) {
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        p.translate(s.x + s.ancho / 2.0, s.y + s.alto / 2.0);
        p.rotate(Math.toRadians(s.rotacion));
        if (s.respira && cine) {
            double r = Math.sin(tEscena * 1.9 + s.x * 0.013);
            p.scale(1 + 0.006 * r, 1 + 0.018 * r);
        }
        p.translate(-s.ancho / 2.0, -s.alto / 2.0);

        if (Recursos.existe(s.ruta)) {
            BufferedImage img = Recursos.imagen(s.ruta, s.ancho, s.alto);
            p.drawImage(img, 0, 0, s.ancho, s.alto, null);
            p.dispose();
            return;
        }

        // Marcador visual temporal: permite programar y ajustar posiciones antes de tener los PNG.
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(new Color(12, 25, 48, 75));
        p.fill(new Ellipse2D.Double(s.ancho * 0.12, s.alto * 0.88, s.ancho * 0.76, s.alto * 0.10));
        p.setColor(new Color(255, 239, 205, 220));
        p.setStroke(new BasicStroke(4f));
        p.draw(new RoundRectangle2D.Double(0, 0, s.ancho, s.alto, 30, 30));
        p.setColor(new Color(255, 244, 220, 35));
        p.fill(new RoundRectangle2D.Double(3, 3, s.ancho - 6, s.alto - 6, 26, 26));
        p.setColor(new Color(255, 239, 205, 230));
        p.setFont(Estilo.fuente(Font.BOLD, 20f));
        String nombre = nombreArchivo(s.ruta);
        FontMetrics fm = p.getFontMetrics();
        for (String linea : envolver(nombre, fm, s.ancho - 18)) {
            centrado(p, linea, s.ancho / 2, s.alto / 2);
        }
        p.dispose();
    }

    private static String nombreArchivo(String ruta) {
        int barra = ruta.lastIndexOf('/');
        String nombre = barra >= 0 ? ruta.substring(barra + 1) : ruta;
        int punto = nombre.lastIndexOf('.');
        return punto > 0 ? nombre.substring(0, punto) : nombre;
    }

    private void dibujarLinea(Graphics2D g, String[] l) {
        int bx = 260, by = 790, bw = 1400, bh = 230;
        if (Recursos.existe(EscenaMundo.MARCO_BURBUJA)) {
            g.drawImage(Recursos.imagen(EscenaMundo.MARCO_BURBUJA, bw, bh), bx, by, bw, bh, null);
        } else {
            Estilo.pergamino(g, bx, by, bw, bh, 44);
            g.setColor(Estilo.MADERA_OSCURA);
            g.setStroke(new BasicStroke(6f));
            g.drawRoundRect(bx, by, bw, bh, 44, 44);
        }
        if (l[0] != null) {
            Estilo.madera(g, bx + 50, by - 34, 330, 64, 28);
            g.setFont(Estilo.fuente(Font.BOLD, 34f));
            g.setColor(new Color(0xFFF3D6));
            centrado(g, l[0], bx + 50 + 165, by + 10);
        }
        g.setFont(Estilo.fuente(Font.PLAIN, 40f));
        FontMetrics fm = g.getFontMetrics();
        int visibles = (int) Math.min(l[1].length(), letras);
        int usados = 0, y = by + 82;
        g.setColor(Estilo.TEXTO);
        for (String s : envolver(l[1], fm, bw - 150)) {
            if (usados >= visibles) break;
            int n = Math.min(s.length(), visibles - usados);
            g.drawString(s.substring(0, n), bx + 70, y);
            usados += s.length() + 1;
            y += fm.getHeight() + 6;
        }
    }

    private void pista(Graphics2D g) {
        if (tCuadro > 1.2) {
            g.setFont(Estilo.fuente(Font.PLAIN, 22f));
            g.setColor(new Color(255, 255, 255, 150));
            centrado(g, "clic para continuar", W / 2, H - 60);
        }
    }
}
