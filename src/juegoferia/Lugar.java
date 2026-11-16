package juegoferia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Un lugar jugable (panaderia, malecon, junta, plaza, casa). Es un EscenaMundo (se camina y se
 * habla con los vecinos) mas las reglas del guion, ahora para CUATRO jugadores juntos:
 *
 *   - EQUIPO: uno camina (el lider) y los otros tres lo siguen. TAB cambia quien camina.
 *   - DECISION DIGITAL (Q): las cinco opciones A-E son siempre las mismas; eligen los cuatro,
 *     uno por uno, y el chat "Los de Dona Rosa" anuncia lo que hizo cada uno.
 *   - DECISION DE RESPETO: responde quien quiera primero; si elige A, los otros pueden "Yo tambien".
 *   - PIEZAS del Letrero, MINIJUEGO FIJO y ESPORADICOS (opcionales).
 *   - HABILIDADES (H), PRESTAR habilidad (P) y FRASES RAPIDAS (F).
 *   - DESINFORMACION del lugar (empieza en 40); si pasa de 70 entra la NIEBLA.
 *   - RELOJ: corre mientras caminan y se detiene con dialogos y decisiones.
 *
 * Los textos y posiciones de cada lugar se arman en GuionDia1 / GuionDia2.
 */
public class Lugar extends EscenaMundo {

    /** Lo que Lugar necesita del juego que lo contiene (JuegoMacondo). */
    public interface Anfitrion {

        void ir(String destino);

        void minijuego(Minijuego mj, Consumer<Minijuego.Resultado> fin);

        void cinematica(Cinematica c, Runnable fin);

        /** Se acabo el dia (despues de la noche): pasa al siguiente o al final. */
        void terminarDia();
    }

    public static final class Opcion {

        final String texto;
        final Runnable accion;
        /** En red: si no es null, solo ese rol puede escoger esta opcion. */
        Rol solo;

        Opcion(String texto, Runnable accion) {
            this.texto = texto;
            this.accion = accion;
        }

        public Opcion soloPara(Rol r) {
            this.solo = r;
            return this;
        }
    }

    /** Crea una opcion para decidir(...). La letra (A, B, C...) se asigna sola. */
    public static Opcion op(String texto, Runnable accion) {
        return new Opcion(texto, accion);
    }

    static final class Decision {

        String titulo = "", autor = "", situacion = "", contexto = "";
        Color turno;
        /** En red: si no es null, solo este rol puede responder (los demas ven la pantalla y esperan). */
        Rol soloRol;
        boolean celular;
        final List<Opcion> opciones = new ArrayList<>();
        double limite, restante, t;
        Runnable alAgotar;
        int hover = -1, pulsada = -1;
    }

    private static final class Toast {

        String s;
        Color c;
        double t;
    }

    private static final class Objetivo {

        final String id, texto;
        boolean hecho;

        Objetivo(String id, String texto) {
            this.id = id;
            this.texto = texto;
        }
    }

    /** Burbuja corta encima de un personaje (frases rapidas, reacciones). */
    private static final class Burbuja {

        Actor a;
        String texto;
        double t, dura;
    }

    private static final Color[] COLOR_LETRA = {
        new Color(0x4CB04A), new Color(0x3F8EDC), new Color(0xE0A030), new Color(0x8A8FA3), new Color(0xDD3B2E)
    };

    // ===================== DECISION DIGITAL: SIEMPRE LAS MISMAS =====================

    /** Efectos de A-E (guion: "Las cinco opciones de la decision digital"). */
    public static final Efecto[] EFECTO_DIGITAL = {
        Efecto.de(EstadoJuego.INFO_VERIFICADA, 2, EstadoJuego.PARTICIPACION, 1, Partida.DESINFO_LUGAR, -5, Partida.ANIMO, 1),
        Efecto.de(EstadoJuego.CONFIANZA, 2, EstadoJuego.INFO_VERIFICADA, 1, Partida.DESINFO_LUGAR, -3),
        Efecto.de(EstadoJuego.BIENESTAR, 2, EstadoJuego.ALCANCE, -1, Partida.ANIMO, 1),
        new Efecto(),
        Efecto.de(EstadoJuego.ALCANCE, 3, EstadoJuego.INFO_VERIFICADA, -3, Partida.DESINFO_LUGAR, 10, Partida.ANIMO, -1)
    };
    public static final String[] NOMBRE_DIGITAL = {"Verificar", "Reportar", "Tomarte un tinto", "Guardar silencio", "Difundir"};
    public static final int A = 0, B = 1, C = 2, D = 3, E = 4;

    // ===================== ESTADO =====================

    public final String id, titulo;
    public final int hora;
    final Partida partida;
    private final Anfitrion anfitrion;

    private final List<Objetivo> objetivos = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();
    private final List<Burbuja> burbujas = new ArrayList<>();
    private final List<Rectangle> salidas = new ArrayList<>();

    // equipo
    private final Map<Rol, Actor> equipo = new EnumMap<>(Rol.class);
    private final LinkedList<double[]> rastro = new LinkedList<>();
    private static final int PASOS_ENTRE_SEGUIDORES = 14;

    // rumor
    private String rumor;
    private double ritmo;
    private boolean rumorResuelto;
    private String nieblaPantalla, nieblaLinea;
    private Letrero letrero;

    // celular
    private String digAutor, digContexto, digPublicacion;
    private String[] digOpciones, digAnuncios;
    private Consumer<Map<Rol, Integer>> digAlTerminar;
    private Supplier<Minijuego> esporadicoTinto, esporadicoDifundir;
    private String nombreTinto, nombreDifundir;
    private boolean digitalHecho;

    // habilidades en pantalla
    private double vinculosHasta = -1;

    private Decision decision;
    private boolean primeraEntrada = true, completadoTodo;
    private Runnable alPrimeraEntrada, alCompletar;

    /** hora = hora de llegada del guion (Reloj.h(11, 0)); -1 si no tiene. */
    public Lugar(Partida partida, Anfitrion anfitrion, String id, String titulo, int hora) {
        this.partida = partida;
        this.anfitrion = anfitrion;
        this.id = id;
        this.titulo = titulo;
        this.hora = hora;
    }

    public Partida partida() {
        return partida;
    }

    public Anfitrion anfitrion() {
        return anfitrion;
    }

    // ===================== ARMAR EL LUGAR =====================

    /**
     * Pone a los cuatro jugadores en (x, y). El lider camina y los otros lo siguen.
     * Cada uno usa los sprites de su rol (/imagenes/personajes/<rol>/quieto.png ...).
     */
    public Lugar equipoEn(int x, int y) {
        int i = 0;
        for (Jugador j : partida.jugadores()) {
            Actor a = new Actor(j.rol.carpeta, x - i * 70, y + (i % 2) * 20, 150, 260)
                    .colores(new Color(0xF2C9A0), j.rol.color, new Color(0x3A2416))
                    .spritesDeRol(j.rol)
                    .nombre(j.nombre, j.rol.nombre)
                    .solido(false);
            equipo.put(j.rol, a);
            agregar(a);
            i++;
        }
        if (Sincronia.activa()) {
            // en red: cada jugador maneja su propio personaje y nadie sigue a nadie
            for (Map.Entry<Rol, Actor> e : equipo.entrySet()) {
                controlar(e.getKey(), e.getValue());
            }
            jugador(equipo.get(Sincronia.miRol()));
        } else {
            Jugador lider = partida.lider();
            if (lider != null) {
                jugador(equipo.get(lider.rol));
            }
        }
        return this;
    }

    /** Resumen del estado de este lugar (posiciones, dialogo, decision) para detectar desincronizacion en red. */
    String huellaLugar() {
        StringBuilder sb = new StringBuilder(id);
        for (Actor a : getActores()) {
            sb.append(',').append(a.id).append(Math.round(a.x)).append(':').append(Math.round(a.y));
        }
        sb.append('|').append(dialogoActivo()).append(decision == null ? "-" : decision.titulo)
                .append(completadoTodo).append((int) partida.desinfo(id)).append(partida.hayNiebla(id));
        return sb.toString();
    }

    /** Para las pruebas automaticas: que esta pasando en este lugar. */
    String resumenPrueba() {
        return decision != null ? "DEC" + decision.opciones.size() : dialogoActivo() ? "DLG" : "LIBRE";
    }

    public Actor actorDe(Rol r) {
        return equipo.get(r);
    }

    /** NPC con sprite /imagenes/personajes/<id>.png (o figurita provisional si no existe). */
    public Actor npc(String id, String nombre, String nombreNiebla, double x, double y, Color ropa) {
        Actor a = new Actor(id, x, y, 150, 250)
                .colores(new Color(0xE8B88A), ropa, new Color(0x4A2E1A))
                .imagen("/imagenes/personajes/" + id + ".png")
                .nombre(nombre, nombreNiebla);
        agregar(a);
        return a;
    }

    /** El rumor de este lugar. ritmo = puntos de desinformacion por segundo (0 = no crece solo). */
    public Lugar rumor(String texto, double ritmo) {
        this.rumor = texto;
        this.ritmo = ritmo;
        return this;
    }

    /**
     * La decision digital del lugar.
     * @param autor        quien publico (ej: "Grupo del barrio")
     * @param publicacion  lo que dice la publicacion
     * @param contexto     la situacion ("Ya tiene cuarenta pulgares arriba...")
     * @param opcionesAE   los 5 textos de A-E de este lugar
     * @param anunciosAE   los 5 anuncios del chat con {rol} (null = los de por defecto)
     * @param alTerminar   recibe lo que eligio cada rol (0..4) para las reacciones del grupo
     */
    public Lugar digital(String autor, String publicacion, String contexto, String[] opcionesAE,
            String[] anunciosAE, Consumer<Map<Rol, Integer>> alTerminar) {
        this.digAutor = autor;
        this.digPublicacion = publicacion;
        this.digContexto = contexto;
        this.digOpciones = opcionesAE;
        this.digAnuncios = anunciosAE;
        this.digAlTerminar = alTerminar;
        return this;
    }

    /** Esporadicos que se OFRECEN (el jugador decide si juega) despues de la decision digital. */
    public Lugar esporadicos(String nombreTinto, Supplier<Minijuego> siTinto,
            String nombreDifundir, Supplier<Minijuego> siDifunde) {
        this.nombreTinto = nombreTinto;
        this.esporadicoTinto = siTinto;
        this.nombreDifundir = nombreDifundir;
        this.esporadicoDifundir = siDifunde;
        return this;
    }

    public Lugar objetivo(String id, String texto) {
        objetivos.add(new Objetivo(id, texto));
        return this;
    }

    /** El Letrero de este lugar (se arma en la cena con las piezas). */
    public Lugar letrero(String texto) {
        this.letrero = partida.letrero(id, texto);
        return this;
    }

    public Letrero getLetrero() {
        return letrero;
    }

    public Lugar niebla(String textoPantalla, String lineaNpc) {
        this.nieblaPantalla = textoPantalla;
        this.nieblaLinea = lineaNpc;
        return this;
    }

    public Lugar alPrimeraEntrada(Runnable r) {
        this.alPrimeraEntrada = r;
        return this;
    }

    /** Se llama cuando se completan todos los objetivos (ej: pasar al siguiente lugar). */
    public Lugar alCompletar(Runnable r) {
        this.alCompletar = r;
        return this;
    }

    /** Puerta hacia otro lugar (se usa con E). */
    public Lugar salida(int x, int y, int w, int h, String destino, String etiqueta) {
        salidas.add(new Rectangle(x, y, w, h));
        zona(x, y, w, h, etiqueta, () -> salir(destino));
        return this;
    }

    // ===================== DIALOGOS =====================

    /**
     * Igual que EscenaMundo.dialogo, pero entiende el guion:
     *   "CIUDADANO: ..."  -> muestra el nombre de quien juega ese rol
     *   {CANDIDATO}       -> nombre de quien juega ese rol (para "nombre del personaje")
     * y pausa el reloj mientras se habla.
     */
    @Override
    public void dialogo(Runnable fin, String... lineas) {
        String[] l = new String[lineas.length];
        for (int i = 0; i < lineas.length; i++) {
            String s = partida.reemplazar(lineas[i]);
            boolean esRol = false;
            for (Rol r : Rol.values()) {
                if (s.startsWith(r.name() + ": ")) {
                    Jugador j = partida.jugador(r);
                    s = (j != null ? j.nombre : r.nombre) + s.substring(r.name().length());
                    esRol = true;
                }
            }
            int dp = s.indexOf(": ");
            if (!esRol && dp > 0 && dp < 24) {
                String quien = s.substring(0, dp);
                if (quien.equals(quien.toUpperCase())) {     // "DOÑA ROSA" -> "Doña Rosa"
                    StringBuilder sb = new StringBuilder();
                    for (String w : quien.toLowerCase().split(" ")) {
                        if (!w.isEmpty()) {
                            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
                        }
                    }
                    s = sb.toString().trim() + s.substring(dp);
                }
            }
            l[i] = s;
        }
        super.dialogo(fin, l);
    }

    // ===================== LO QUE PASA DURANTE EL JUEGO =====================

    /** El anfitrion lo llama cada vez que el grupo llega a este lugar. */
    public void entrar() {
        if (hora >= 0) {
            partida.reloj.llegar(hora);
        }
        if (rumor != null && partida.pasoUmbral(id) && !partida.hayNiebla(id) && nieblaPantalla != null) {
            activarNiebla();
        }
        if (primeraEntrada) {
            primeraEntrada = false;
            if (alPrimeraEntrada != null) {
                alPrimeraEntrada.run();
            }
        } else if (partida.hayNiebla(id)) {
            toast("Hay niebla aquí. Esta noche hace falta un Letrero.", new Color(0xFFE08A));
        }
    }

    private void salir(String destino) {
        if (!objetivos.isEmpty() && !completadoTodo) {
            for (Objetivo o : objetivos) {
                if (!o.hecho) {
                    toast("Todavía falta: " + o.texto, new Color(0xFFE08A));
                    return;
                }
            }
        }
        int m = partida.mapa.minutos(id, destino);
        if (m > 0) {
            partida.reloj.sumarMinutos(m);
        }
        anfitrion.ir(destino);
    }

    public void completar(String objetivoId) {
        boolean todos = true;
        for (Objetivo o : objetivos) {
            if (o.id.equals(objetivoId)) {
                o.hecho = true;
            }
            todos &= o.hecho;
        }
        if (todos && !completadoTodo) {
            completadoTodo = true;
            toast("¡Todo listo en " + titulo + "!", new Color(0x8BE07A));
            if (alCompletar != null) {
                alCompletar.run();
            }
        }
    }

    public boolean completado() {
        return completadoTodo;
    }

    /** Aplica un efecto a UN jugador (null = solo lo compartido) y lo muestra en pantalla. */
    public void efecto(Jugador j, Efecto ef, String etiqueta) {
        Map<String, Integer> real = partida.aplicar(j, ef, etiqueta, id);
        String quien = j == null ? "" : j.rol.nombre + ": ";
        for (Map.Entry<String, Integer> c : real.entrySet()) {
            boolean bueno = c.getKey().equals(Partida.DESINFO_LUGAR) ? c.getValue() < 0 : c.getValue() > 0;
            toast(quien + (c.getValue() > 0 ? "+" : "") + c.getValue() + " " + EstadoJuego.nombreBonito(c.getKey()),
                    bueno ? new Color(0x8BE07A) : new Color(0xFF8A7A));
        }
        if (partida.hayNiebla(id) && !real.isEmpty()) {
            toast("(la niebla reduce lo bueno)", new Color(0xCFCFE0));
        }
        revisarNiebla();
    }

    /** Lo compartido (animo, desinformacion del lugar) sin tocar a ningun jugador. */
    public void efectoCompartido(Efecto ef, String etiqueta) {
        efecto(null, ef, etiqueta);
    }

    /** Mariposa amarilla: solo la ve quien la gano. */
    public void mariposa(Jugador j) {
        j.mariposas++;
        toast(j.nombre + ": una mariposa amarilla se posa en tu hombro", new Color(0xFFD35C));
    }

    /** Entrega una pieza del Letrero a un rol. */
    public void darPieza(Rol r, String idPieza, String nombre) {
        Jugador j = partida.jugador(r);
        if (j == null) {
            return;
        }
        j.darPieza(new Pieza(idPieza, nombre, r, id));
        toast(j.nombre + " recibe: " + nombre, new Color(0xFFD35C));
    }

    public void toast(String s, Color c) {
        Toast t = new Toast();
        t.s = s;
        t.c = c;
        toasts.add(t);
        if (toasts.size() > 6) {
            toasts.remove(0);
        }
    }

    /** Burbuja encima de un personaje durante unos segundos. */
    public void burbuja(Actor a, String texto, double segundos) {
        if (a == null) {
            return;
        }
        Burbuja b = new Burbuja();
        b.a = a;
        b.texto = partida.reemplazar(texto);
        b.dura = segundos;
        burbujas.removeIf(x -> x.a == a);
        burbujas.add(b);
    }

    /** Lanza un minijuego cualquiera. */
    public void lanzar(Minijuego mj, Consumer<Minijuego.Resultado> fin) {
        anfitrion.minijuego(mj, res -> {
            if (fin != null) {
                fin.accept(res);
            }
        });
    }

    /**
     * El MINIJUEGO FIJO del lugar. Lo juega 'quien' (los otros miran y aconsejan).
     * Si sale BIEN, el Letrero del lugar suma +3 de proteccion.
     */
    public void minijuegoFijo(Rol quien, Minijuego mj, Consumer<Minijuego.Resultado> fin) {
        Jugador j = partida.jugador(quien);
        toast("Juega " + (j != null ? j.titulo() : quien.nombre) + ". Los demás aconsejan.", new Color(0xFFD35C));
        if (Sincronia.activa()) {
            mj.soloRol = quien;
        }
        lanzar(mj, res -> {
            if (res.puntos == Minijuego.BIEN && letrero != null) {
                letrero.minijuegoBien = true;
                toast("Letrero del lugar: +3 de protección", new Color(0xFFD35C));
            }
            efecto(j, res.efecto, mj.nombre() + ": " + res.resumen);
            if (fin != null) {
                fin.accept(res);
            }
        });
    }

    /** Ofrece un esporadico: el jugador decide si lo juega. */
    public void ofrecerEsporadico(String nombre, Supplier<Minijuego> crear, Runnable despues) {
        decidir("ALGO INESPERADO", "Aparece: " + nombre + ". ¿Quieren jugarlo? Es opcional.", 0, null,
                op("Sí, juguemos", () -> {
                    Minijuego mj = crear.get();
                    if (Sincronia.activa()) {
                        mj.soloRol = Sincronia.rolActual();     // lo juega quien dijo que si
                    }
                    lanzar(mj, res -> {
                        efectoCompartido(res.efecto, nombre);
                        if (despues != null) {
                            despues.run();
                        }
                    });
                }),
                op("Ahora no", despues));
    }

    // ===================== CELULAR (DECISION DIGITAL) =====================

    public void abrirCelular() {
        if (decision != null || dialogoActivo() || estaBloqueado()) {
            return;
        }
        if (digOpciones == null || digitalHecho) {
            toast("Nada nuevo en el celular", new Color(0xCFCFE0));
            return;
        }
        digitalHecho = true;
        DecisionGrupal.digital(this, digAutor, digPublicacion, digContexto, digOpciones,
                (j, op) -> resolverDigital(j, op),
                elecciones -> terminarDigital(elecciones));
    }

    private void resolverDigital(Jugador j, int op) {
        efecto(j, EFECTO_DIGITAL[op], "Decisión digital (" + titulo + "): " + NOMBRE_DIGITAL[op]);
        partida.chat.anunciarDigital(partida.reloj.texto(), j.rol, op, digAnuncios);
        if (op == B) {
            j.poner("reporte_en_perfil");
        } else if (op == E) {
            j.poner("difundio_sin_verificar");
        }
    }

    private void terminarDigital(Map<Rol, Integer> elecciones) {
        boolean alguienVerifico = elecciones.containsValue(A) || elecciones.containsValue(B);
        if (alguienVerifico) {
            rumorResuelto = true;
        }
        completar("digital");
        Runnable esporadicos = () -> {
            if (elecciones.containsValue(E) && esporadicoDifundir != null) {
                ofrecerEsporadico(nombreDifundir, esporadicoDifundir, null);
            } else if (elecciones.containsValue(C) && esporadicoTinto != null) {
                ofrecerEsporadico(nombreTinto, esporadicoTinto, null);
            }
        };
        if (digAlTerminar != null) {
            digAlTerminar.accept(elecciones);
        }
        // los esporadicos se ofrecen cuando termine lo que dispare el guion
        esperarYLuego(esporadicos);
    }

    /** Ejecuta r cuando no haya dialogo ni decision abiertos. */
    public void esperarYLuego(Runnable r) {
        pendientes.add(r);
    }

    private final List<Runnable> pendientes = new ArrayList<>();

    // ===================== DECISIONES =====================

    /**
     * Abre una decision. Si limite > 0 y se acaba, se ejecuta alAgotar.
     * Las opciones se eligen con 1-9 o con clic.
     */
    public void decidir(String titulo, String situacion, double limite, Runnable alAgotar, Opcion... ops) {
        Decision d = new Decision();
        d.titulo = titulo;
        d.situacion = partida.reemplazar(situacion);
        d.limite = limite;
        d.alAgotar = alAgotar;
        for (Opcion o : ops) {
            d.opciones.add(o);
        }
        abrir(d);
    }

    /** Decision de un jugador en particular (se ve su color y "Turno de ..."). */
    public void decidirTurno(Jugador j, String situacion, Opcion... ops) {
        decidir("TURNO DE " + j.nombre.toUpperCase(), situacion, 0, null, ops);
        decision.turno = j.rol.color;
        decision.soloRol = j.rol;
    }

    /** Decision con estilo de celular (un mensaje o una publicacion). */
    public void decidirCelular(String titulo, String autor, String mensaje, String contexto, Opcion... ops) {
        Decision d = new Decision();
        d.celular = true;
        d.titulo = titulo;
        d.autor = autor;
        d.situacion = partida.reemplazar(mensaje);
        d.contexto = contexto == null ? "" : partida.reemplazar(contexto);
        for (Opcion o : ops) {
            d.opciones.add(o);
        }
        abrir(d);
    }

    void colorTurno(Color c) {
        if (decision != null) {
            decision.turno = c;
        }
    }

    /** En red: solo este rol puede responder la decision abierta. */
    void soloPara(Rol r) {
        if (decision != null) {
            decision.soloRol = r;
        }
    }

    @Override
    protected boolean aceptaEntradaDe(Rol r) {
        Decision d = decision;
        return d == null || d.soloRol == null || d.soloRol == r;
    }

    @Override
    protected void alActuarAuto(Rol r) {
        partida.setLider(r);
    }

    private void abrir(Decision d) {
        d.restante = d.limite;
        decision = d;
        bloquear(true);
    }

    private void cerrarDecision() {
        decision = null;
        bloquear(false);
    }

    private void elegir(int i) {
        Decision d = decision;
        if (d == null || i < 0 || i >= d.opciones.size()) {
            return;
        }
        Opcion elegida = d.opciones.get(i);
        if (Sincronia.activa() && elegida.solo != null && elegida.solo != Sincronia.rolActual()) {
            return;      // esa opcion es de otro jugador
        }
        Runnable accion = elegida.accion;
        cerrarDecision();
        Funciones.reproducirSonido("/sonidos/click.wav");
        if (accion != null) {
            accion.run();
        }
    }

    // ===================== NIEBLA =====================

    private void revisarNiebla() {
        if (rumor != null && !rumorResuelto && partida.pasoUmbral(id) && !partida.hayNiebla(id) && nieblaPantalla != null) {
            activarNiebla();
        }
    }

    private void activarNiebla() {
        partida.nieblaOn(id);
        Funciones.reproducirSonido("/sonidos/noticia.wav");
        dialogo(null, "Narrador: " + nieblaPantalla, nieblaLinea);
    }

    @Override
    protected boolean hayNiebla() {
        return partida.hayNiebla(id);
    }

    // ===================== HABILIDADES Y FRASES =====================

    /** H: el lider usa su habilidad (o elige entre la suya y la prestada). */
    private void elegirHabilidad() {
        Jugador j = partida.lider();
        List<Habilidad> hs = j.habilidades();
        if (hs.size() == 1) {
            usarHabilidad(j, hs.get(0));
            return;
        }
        Opcion[] ops = new Opcion[hs.size()];
        for (int i = 0; i < hs.size(); i++) {
            Habilidad h = hs.get(i);
            ops[i] = op(h.nombre + ": " + h.efecto, () -> usarHabilidad(j, h));
        }
        decidirTurno(j, "¿Qué habilidad usas?", ops);
    }

    public void usarHabilidad(Jugador j, Habilidad h) {
        Actor yo = equipo.get(j.rol);
        burbuja(yo, h.linea, 4);
        switch (h) {
            case VER_VINCULOS:
                vinculosHasta = tiempo + 6;
                String clave = partida.barrio.masConectado();
                Actor a = actor(clave);
                if (a != null) {
                    burbuja(a, "(todos me escriben a mí)", 4);
                }
                break;
            case LUPA:
                partida.poner("lupa_" + id);   // los minijuegos la pueden revisar para dar pistas
                toast("Lupa activa: el minijuego de aquí marcará las palabras que cambiaron", new Color(0xFFD35C));
                break;
            case MEGAFONO:
                if (!partida.bandera("megafono_" + id)) {
                    partida.poner("megafono_" + id);
                    efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -3), "Megáfono");
                }
                break;
            case CONVOCATORIA:
                convocar(yo);
                break;
            default:
                break;
        }
        if (j.prestada == h) {
            dialogo(null, j.rol.name() + ": Gracias. La uso y te la devuelvo.");
            j.devolverPrestada();
        }
    }

    /** Los vecinos del lugar se acercan y forman un circulo alrededor de quien convoca. */
    private void convocar(Actor centro) {
        List<Actor> vecinos = new ArrayList<>();
        for (Actor a : getActores()) {
            if (!equipo.containsValue(a) && a.nombre != null) {
                vecinos.add(a);
            }
        }
        for (int i = 0; i < vecinos.size(); i++) {
            double ang = Math.PI * 2 * i / vecinos.size();
            vecinos.get(i).x = centro.x + StrictMath.cos(ang) * 260;
            vecinos.get(i).y = centro.y + StrictMath.sin(ang) * 90;
        }
    }

    /** P: el lider le presta su habilidad a otro. */
    private void prestarHabilidad() {
        Jugador de = partida.lider();
        List<Opcion> ops = new ArrayList<>();
        for (Jugador a : partida.otros(de.rol)) {
            boolean bloq = partida.confianza.estaBloqueada(de.rol, a.rol);
            ops.add(op(a.titulo() + (bloq ? " (hoy no se pueden prestar)" : ""), () -> {
                if (partida.prestar(de.rol, a.rol)) {
                    dialogo(null, de.rol.name() + ": Te presto mi habilidad.",
                            a.rol.name() + ": Gracias. La uso y te la devuelvo.");
                    toast("Confianza " + de.rol.nombre + "-" + a.rol.nombre + " +1", new Color(0x8BE07A));
                } else {
                    toast("Hoy no se pueden prestar habilidades entre ustedes.", new Color(0xFF8A7A));
                }
            }));
        }
        ops.add(op("Mejor no", null));
        decidirTurno(de, "¿A quién le prestas " + de.rol.habilidad.nombre + "?", ops.toArray(new Opcion[0]));
    }

    // ===================== ENTRADA =====================

    @Override
    protected boolean teclaExtra(int k, boolean pres) {
        Decision d = decision;
        if (d != null) {
            if (pres) {
                int i = -1;
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) {
                    i = k - KeyEvent.VK_1;
                } else if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) {
                    i = k - KeyEvent.VK_NUMPAD1;
                } else if (k == KeyEvent.VK_0) {
                    i = 9;
                }
                if (i >= 0 && i < d.opciones.size()) {
                    elegir(i);
                }
            }
            return true;
        }
        if (!pres || dialogoActivo() || estaBloqueado()) {
            return false;
        }
        switch (k) {
            case KeyEvent.VK_Q:
                abrirCelular();
                return true;
            case KeyEvent.VK_TAB:
                if (!Sincronia.activa()) {
                    cambiarLider();
                }
                return true;
            case KeyEvent.VK_H:
                elegirHabilidad();
                return true;
            case KeyEvent.VK_P:
                prestarHabilidad();
                return true;
            case KeyEvent.VK_F:
                FrasesRapidas.abrir(this);
                return true;
            default:
                return false;
        }
    }

    /** TAB: camina otro. El nuevo lider toma el lugar del anterior. */
    private void cambiarLider() {
        Actor viejo = getJugador();
        Jugador nuevo = partida.siguienteLider();
        Actor a = equipo.get(nuevo.rol);
        if (viejo != null && a != null && viejo != a) {
            double x = a.x, y = a.y;
            a.x = viejo.x;
            a.y = viejo.y;
            viejo.x = x;
            viejo.y = y;
            jugador(a);
            rastro.clear();
            toast("Ahora camina " + nuevo.titulo(), nuevo.rol.color);
        }
    }

    @Override
    protected boolean ratonExtra(int tipo, Point p) {
        Decision d = decision;
        if (d == null) {
            if (tipo == 1 && RECT_CELULAR.contains(p)) {
                abrirCelular();
                return true;
            }
            return false;
        }
        int idx = opcionEn(d, p);
        if (tipo == 0) {
            d.hover = idx;
        } else if (tipo == 1) {
            d.pulsada = idx;
        } else {
            int pulsada = d.pulsada;
            d.pulsada = -1;
            if (pulsada >= 0 && pulsada == idx) {
                elegir(idx);
            }
        }
        return true;
    }

    private int opcionEn(Decision d, Point p) {
        for (int i = 0; i < d.opciones.size(); i++) {
            if (rectOpcion(i, d.opciones.size()).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    private static Rectangle2D.Double rectOpcion(int i, int n) {
        double gap = 16;
        double h = Math.min(140, (760 - gap * (n - 1)) / n);
        return new Rectangle2D.Double(740, 210 + i * (h + gap), 1090, h);
    }

    // ===================== LOGICA =====================

    @Override
    protected void actualizar(double dt) {
        super.actualizar(dt);

        for (Iterator<Toast> it = toasts.iterator(); it.hasNext();) {
            Toast t = it.next();
            t.t += dt;
            if (t.t > 3.2) {
                it.remove();
            }
        }
        for (Iterator<Burbuja> it = burbujas.iterator(); it.hasNext();) {
            Burbuja b = it.next();
            b.t += dt;
            if (b.t > b.dura) {
                it.remove();
            }
        }

        moverSeguidores();

        boolean ocupado = dialogoActivo() || decision != null;
        if (ocupado) {
            partida.reloj.pausar();
        } else {
            partida.reloj.reanudar();
            partida.reloj.avanzar(dt);
            if (!pendientes.isEmpty()) {
                Runnable r = pendientes.remove(0);
                r.run();
            }
        }

        Decision d = decision;
        if (d != null) {
            d.t += dt;
            if (d.limite > 0) {
                d.restante -= dt;
                if (d.restante <= 0) {
                    Runnable f = d.alAgotar;
                    cerrarDecision();
                    if (f != null) {
                        f.run();
                    }
                }
            }
            return;
        }

        if (rumor != null && !rumorResuelto && !dialogoActivo() && ritmo > 0) {
            partida.sumarDesinfo(id, ritmo * dt);
            revisarNiebla();
        }
    }

    /** Los que no caminan siguen el rastro del lider, cada uno un poco mas atras. */
    private void moverSeguidores() {
        Actor lider = getJugador();
        if (lider == null || Sincronia.activa()) {
            return;
        }
        double[] ultimo = rastro.isEmpty() ? null : rastro.getFirst();
        if (ultimo == null || Math.hypot(ultimo[0] - lider.x, ultimo[1] - lider.y) > 6) {
            rastro.addFirst(new double[]{lider.x, lider.y});
            if (rastro.size() > PASOS_ENTRE_SEGUIDORES * 4) {
                rastro.removeLast();
            }
        }
        int k = 1;
        for (Actor a : equipo.values()) {
            if (a == lider) {
                continue;
            }
            int idx = PASOS_ENTRE_SEGUIDORES * k;
            if (idx < rastro.size()) {
                double[] p = rastro.get(idx);
                a.caminando = Math.hypot(p[0] - a.x, p[1] - a.y) > 1;
                if (Math.abs(p[0] - a.x) > 1) {
                    a.mirandoIzquierda = p[0] < a.x;
                }
                a.x = p[0];
                a.y = p[1];
            } else {
                a.caminando = false;
            }
            k++;
        }
    }

    // ===================== DIBUJO =====================

    private static final Rectangle RECT_CELULAR = new Rectangle(1770, 20, 110, 140);

    @Override
    protected void dibujarCapas(Graphics2D g) {
        if (partida.hayNiebla(id)) {
            dibujarNiebla(g);
        }
        if (tiempo < vinculosHasta) {
            dibujarVinculos(g);
        }
        dibujarMariposas(g);
        dibujarSalidas(g);
        dibujarBurbujas(g);
        dibujarHud(g);
        if (!dialogoActivo() && decision == null) {
            dibujarChat(g);
        }
        dibujarToasts(g);
        if (decision != null) {
            dibujarDecision(g, decision);
        }
    }

    private void dibujarNiebla(Graphics2D g) {
        g.setColor(new Color(225, 228, 235, 105));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 9; i++) {
            double cx = ((i * 0.31) % 1.0) * W + Math.sin(tiempo * 0.25 + i * 1.7) * 160;
            double cy = H * (0.2 + 0.6 * ((i * 0.47) % 1.0)) + Math.cos(tiempo * 0.2 + i) * 60;
            float r = 330 + (i % 3) * 120;
            g.setPaint(new RadialGradientPaint((float) cx, (float) cy, r, new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 130), new Color(255, 255, 255, 0)}));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
        double p = 0.6 + 0.4 * Math.sin(tiempo * 3);
        g.setColor(new Color(90, 70, 120, (int) (170 + 85 * p)));
        g.setFont(Estilo.fuente(Font.BOLD, 44f));
        centrado(g, "LA NIEBLA DEL OLVIDO", 1000, 250);
        g.setFont(Estilo.fuente(Font.BOLD, 28f));
        centrado(g, "esta noche hace falta un Letrero", 1000, 292);
    }

    private void dibujarSalidas(Graphics2D g) {
        if (!completadoTodo) {
            return;
        }
        double p = 0.5 + 0.5 * Math.sin(tiempo * 4);
        g.setStroke(new BasicStroke(6f));
        g.setColor(new Color(255, 211, 92, (int) (110 + 140 * p)));
        for (Rectangle r : salidas) {
            g.draw(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 40, 40));
            g.setFont(Estilo.fuente(Font.BOLD, 60f));
            centrado(g, "→", r.x + r.width / 2, r.y + r.height / 2 + 20);
        }
    }

    /** Habilidad del Ciudadano: lineas entre los vecinos que se hablan (GrafoRelaciones). */
    private void dibujarVinculos(Graphics2D g) {
        double a = Math.min(1, (vinculosHasta - tiempo) / 1.0);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Actor x : getActores()) {
            if (!partida.barrio.existe(x.id)) {
                continue;
            }
            for (String v : partida.barrio.vecinos(x.id)) {
                Actor y = actor(v);
                if (y != null && x.id.compareTo(y.id) < 0) {
                    g.setColor(new Color(255, 211, 92, (int) (200 * a)));
                    g.draw(new Line2D.Double(x.x, x.y - x.alto * 0.6, y.x, y.y - y.alto * 0.6));
                }
            }
        }
    }

    /** Mariposas del lider (solo las ve quien las gano: se muestran las de quien camina). */
    private void dibujarMariposas(Graphics2D g) {
        Actor j = getJugador();
        Jugador lider = Sincronia.activa() ? partida.jugador(Sincronia.miRol()) : partida.lider();
        if (j == null || lider == null) {
            return;
        }
        int n = Math.min(5, lider.mariposas);
        for (int i = 0; i < n; i++) {
            double a = tiempo * 1.3 + i * 1.26;
            double x = j.x + Math.cos(a) * (70 + i * 6);
            double y = j.y - j.alto * 0.9 + Math.sin(a * 1.4) * 26 - i * 10;
            double ala = 0.35 + 0.65 * Math.abs(Math.sin(tiempo * 14 + i));
            AffineTransform at = g.getTransform();
            g.translate(x, y);
            g.setColor(new Color(0xFFD35C));
            g.fill(new Ellipse2D.Double(-17, -12, 17, 24 * ala + 4));
            g.fill(new Ellipse2D.Double(0, -12, 17, 24 * ala + 4));
            g.setColor(new Color(0x7A4A24));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(-17, -12, 17, 24 * ala + 4));
            g.draw(new Ellipse2D.Double(0, -12, 17, 24 * ala + 4));
            g.fill(new Ellipse2D.Double(-2, -10, 4, 22));
            g.setTransform(at);
        }
    }

    private void dibujarBurbujas(Graphics2D g) {
        g.setFont(Estilo.fuente(Font.BOLD, 24f));
        FontMetrics fm = g.getFontMetrics();
        for (Burbuja b : burbujas) {
            List<String> ls = envolver(b.texto, fm, 380);
            int w = 0;
            for (String l : ls) {
                w = Math.max(w, fm.stringWidth(l));
            }
            w += 36;
            int h = ls.size() * (fm.getHeight()) + 24;
            int x = (int) (b.a.x - w / 2.0), y = (int) (b.a.y - b.a.alto - h - 30);
            float alfa = (float) Math.min(1, Math.min(b.t / 0.2, (b.dura - b.t) / 0.4));
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alfa)));
            gg.setColor(Color.WHITE);
            gg.fill(new RoundRectangle2D.Double(x, y, w, h, 26, 26));
            gg.fillPolygon(new int[]{(int) b.a.x - 12, (int) b.a.x + 12, (int) b.a.x},
                    new int[]{y + h - 2, y + h - 2, y + h + 22}, 3);
            gg.setColor(Estilo.MADERA_OSCURA);
            gg.setStroke(new BasicStroke(3f));
            gg.draw(new RoundRectangle2D.Double(x, y, w, h, 26, 26));
            gg.setColor(Estilo.TEXTO);
            int ty = y + 12 + fm.getAscent();
            for (String l : ls) {
                gg.drawString(l, x + 18, ty);
                ty += fm.getHeight();
            }
            gg.dispose();
        }
    }

    private void dibujarHud(Graphics2D g) {
        // ---- reloj y dia ----
        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new RoundRectangle2D.Double(24, 20, 330, 70, 30, 30));
        g.setFont(Estilo.fuente(Font.BOLD, 40f));
        g.setColor(Estilo.DORADO_CLARO);
        g.drawString(partida.reloj.texto(), 44, 70);
        g.setFont(Estilo.fuente(Font.BOLD, 22f));
        g.setColor(Color.WHITE);
        g.drawString("Día " + partida.dia + " de " + Partida.DIAS, 200, 64);

        // ---- titulo del lugar y objetivos ----
        int oy = 104;
        int h = 54 + objetivos.size() * 40;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Double(24, oy, 640, h, 24, 24));
        g.setFont(Estilo.fuente(Font.BOLD, 22f));
        g.setColor(Estilo.DORADO_CLARO);
        g.drawString(titulo.toUpperCase(), 46, oy + 36);
        g.setFont(Estilo.fuente(Font.PLAIN, 24f));
        int ly = oy + 76;
        for (Objetivo o : objetivos) {
            g.setColor(o.hecho ? new Color(0x8BE07A) : Color.WHITE);
            g.drawRoundRect(46, ly - 20, 22, 22, 6, 6);
            if (o.hecho) {
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(51, ly - 9, 56, ly - 3);
                g.drawLine(56, ly - 3, 66, ly - 19);
            }
            g.drawString(o.texto, 82, ly);
            ly += 40;
        }

        // ---- medidores compartidos ----
        int bx = 1000, by = 24;
        g.setColor(new Color(0, 0, 0, 130));
        g.fill(new RoundRectangle2D.Double(bx, by, 740, 132, 26, 26));
        g.setFont(Estilo.fuente(Font.BOLD, 20f));
        g.setColor(Color.WHITE);
        g.drawString("Ánimo de la casa " + partida.animo + "/10", bx + 20, by + 32);
        barra(g, bx + 20, by + 40, 330, 14, partida.animo / 10.0, new Color(0xF0A020));
        g.setColor(Color.WHITE);
        g.drawString("Confianza entre roles " + partida.confianza.promedio() + "/10", bx + 380, by + 32);
        barra(g, bx + 380, by + 40, 330, 14, partida.confianza.promedio() / 10.0, new Color(0x4C8BD9));
        double ds = partida.desinfo(id);
        g.setColor(Color.WHITE);
        if (rumor != null) {
        g.drawString("Desinformación aquí " + (int) ds + "/100" + (rumorResuelto ? "  (controlada)" : ""),
                bx + 20, by + 88);
        barra(g, bx + 20, by + 96, 690, 16, ds / 100.0,
                ds > Partida.UMBRAL_NIEBLA ? new Color(0xDD3B2E) : ds > 50 ? new Color(0xF0A020) : new Color(0x5BA843));
        int ux = bx + 20 + (int) (690 * Partida.UMBRAL_NIEBLA / 100.0);
        g.setColor(Color.WHITE);
        g.fillRect(ux - 1, by + 92, 3, 24);
        }

        // ---- celular ----
        boolean pendiente = !digitalHecho && digOpciones != null;
        AffineTransform at = g.getTransform();
        g.translate(RECT_CELULAR.x + 55, RECT_CELULAR.y + 70);
        if (pendiente && (tiempo % 2.5) < 0.35) {
            g.rotate(Math.sin(tiempo * 60) * 0.08);
        }
        g.setColor(Estilo.MADERA_OSCURA);
        g.fill(new RoundRectangle2D.Double(-46, -62, 92, 124, 22, 22));
        g.setColor(new Color(0x7FD3F0));
        g.fill(new RoundRectangle2D.Double(-37, -50, 74, 98, 12, 12));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(-30, -42, 60, 12, 6, 6));
        g.fill(new RoundRectangle2D.Double(-30, -24, 44, 10, 5, 5));
        g.setTransform(at);
        if (pendiente) {
            g.setColor(new Color(0xE0302A));
            g.fillOval(RECT_CELULAR.x + 82, RECT_CELULAR.y - 8, 40, 40);
            g.setColor(Color.WHITE);
            g.setFont(Estilo.fuente(Font.BOLD, 24f));
            centrado(g, "!", RECT_CELULAR.x + 102, RECT_CELULAR.y + 21);
        }
        g.setColor(Estilo.DORADO);
        g.fillRoundRect(RECT_CELULAR.x + 35, RECT_CELULAR.y + 130, 40, 36, 10, 10);
        g.setColor(Estilo.MADERA_OSCURA);
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        centrado(g, "Q", RECT_CELULAR.x + 55, RECT_CELULAR.y + 158);

        // ---- quien camina y teclas ----
        boolean red = Sincronia.activa();
        Jugador lider = red ? partida.jugador(Sincronia.miRol()) : partida.lider();
        if (lider != null) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fill(new RoundRectangle2D.Double(24, 1000, red ? 1100 : 1180, 60, 30, 30));
            g.setColor(lider.rol.color);
            g.fillOval(40, 1012, 36, 36);
            g.setFont(Estilo.fuente(Font.BOLD, 22f));
            g.setColor(Color.WHITE);
            g.drawString((red ? "Eres: " : "Camina: ") + lider.titulo() + "   Mariposas: " + lider.mariposas
                    + (red ? "      E hablar · H habilidad · P prestar · F frases · Q celular"
                            : "      TAB cambiar · H habilidad · P prestar · F frases · Q celular"), 90, 1038);
        }
    }

    /** Ultimos mensajes del chat de la casa. */
    private void dibujarChat(Graphics2D g) {
        List<ChatCasa.Mensaje> ms = partida.chat.ultimos(3);
        if (ms.isEmpty()) {
            return;
        }
        int x = 1340, y = 860, w = 556;
        g.setColor(new Color(0x1F2B22));
        g.fill(new RoundRectangle2D.Double(x, y - 46, w, 46 + ms.size() * 42, 22, 22));
        g.setFont(Estilo.fuente(Font.BOLD, 20f));
        g.setColor(new Color(0x8BE07A));
        g.drawString(ChatCasa.NOMBRE, x + 18, y - 16);
        g.setFont(Estilo.fuente(Font.PLAIN, 20f));
        int ty = y + 28;
        for (ChatCasa.Mensaje m : ms) {
            g.setColor(new Color(0xBFD8C4));
            g.drawString(m.hora, x + 18, ty);
            g.setColor(Color.WHITE);
            String t = m.texto;
            FontMetrics fm = g.getFontMetrics();
            while (fm.stringWidth(t) > w - 110 && t.length() > 4) {
                t = t.substring(0, t.length() - 4) + "…";
            }
            g.drawString(t, x + 90, ty);
            ty += 42;
        }
    }

    private void dibujarToasts(Graphics2D g) {
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        FontMetrics fm = g.getFontMetrics();
        int y = 200;
        for (Toast t : toasts) {
            double a = t.t < 0.25 ? t.t / 0.25 : (t.t > 2.6 ? Math.max(0, (3.2 - t.t) / 0.6) : 1);
            int w = fm.stringWidth(t.s) + 40;
            int x = W - w - 24 + (int) ((1 - Math.min(1, t.t / 0.25)) * 80);
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a));
            gg.setColor(new Color(0, 0, 0, 170));
            gg.fill(new RoundRectangle2D.Double(x, y, w, 44, 22, 22));
            gg.setColor(t.c);
            gg.drawString(t.s, x + 20, y + 31);
            gg.dispose();
            y += 52;
        }
    }

    private void dibujarDecision(Graphics2D g, Decision d) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, W, H);

        // ---- panel izquierdo: celular o tarjeta de situacion ----
        if (d.celular) {
            dibujarCelular(g, d);
        } else {
            Estilo.madera(g, 90, 160, 580, 720, 44);
            Estilo.pergamino(g, 110, 180, 540, 680, 30);
            g.setFont(Estilo.fuente(Font.BOLD, 26f));
            g.setColor(Estilo.TEXTO_SUAVE);
            g.drawString("SITUACIÓN", 140, 230);
            g.setFont(Estilo.fuente(Font.BOLD, 36f));
            g.setColor(Estilo.TEXTO);
            FontMetrics fm = g.getFontMetrics();
            int ty = 290;
            for (String l : envolver(d.situacion, fm, 480)) {
                g.drawString(l, 140, ty);
                ty += fm.getHeight() + 4;
            }
        }

        // ---- titulo y opciones ----
        g.setFont(Estilo.fuente(Font.BOLD, 56f));
        if (d.turno != null) {
            g.setColor(d.turno);
            g.fill(new RoundRectangle2D.Double(730, 100, 1110, 80, 40, 40));
        }
        Estilo.textoContorno(g, d.titulo, 750, 160, Estilo.DORADO_CLARO, Estilo.MADERA_OSCURA);

        int n = d.opciones.size();
        for (int i = 0; i < n; i++) {
            double ap = Math.max(0, Math.min(1, (d.t - i * 0.05) / 0.25));
            if (ap <= 0) {
                continue;
            }
            Rectangle2D.Double r = rectOpcion(i, n);
            boolean hov = d.hover == i;
            boolean pres = d.pulsada == i && hov;
            double off = pres ? 8 : (hov ? -5 : 0);
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ap));
            gg.translate((1 - ap) * 80, 0);

            gg.setColor(new Color(0, 0, 0, pres ? 40 : 80));
            gg.fill(new RoundRectangle2D.Double(r.x + 4, r.y + 10 + (pres ? 0 : 4), r.width, r.height, 36, 36));
            Estilo.pergamino(gg, (int) r.x, (int) (r.y + off), (int) r.width, (int) r.height, 36);
            gg.setColor(hov ? Estilo.DORADO : Estilo.MADERA_OSCURA);
            gg.setStroke(new BasicStroke(hov ? 6f : 4f));
            gg.draw(new RoundRectangle2D.Double(r.x, r.y + off, r.width, r.height, 36, 36));

            Color cl = COLOR_LETRA[i % COLOR_LETRA.length];
            double by = r.y + off + r.height / 2 - 34;
            gg.setColor(cl);
            gg.fill(new Ellipse2D.Double(r.x + 26, by, 68, 68));
            gg.setColor(Estilo.MADERA_OSCURA);
            gg.setStroke(new BasicStroke(3f));
            gg.draw(new Ellipse2D.Double(r.x + 26, by, 68, 68));
            gg.setColor(Color.WHITE);
            gg.setFont(Estilo.fuente(Font.BOLD, 42f));
            centrado(gg, String.valueOf((char) ('A' + i)), (int) r.x + 60, (int) (by + 49));

            gg.setFont(Estilo.fuente(Font.BOLD, 29f));
            FontMetrics fm = gg.getFontMetrics();
            List<String> ls = envolver(d.opciones.get(i).texto, fm, (int) r.width - 220);
            int lines = Math.min(3, ls.size());
            double ty = r.y + off + r.height / 2 - (lines * (fm.getHeight() - 2)) / 2.0 + fm.getAscent() - 2;
            gg.setColor(Estilo.TEXTO);
            for (int k = 0; k < lines; k++) {
                gg.drawString(ls.get(k), (int) r.x + 118, (int) ty);
                ty += fm.getHeight() - 2;
            }
            gg.setFont(Estilo.fuente(Font.BOLD, 24f));
            gg.setColor(Estilo.TEXTO_SUAVE);
            gg.drawString("[" + (i + 1) + "]", (int) (r.x + r.width - 62), (int) (r.y + off + 36));
            gg.dispose();
        }

        // ---- en red: de quien es el turno ----
        if (Sincronia.activa() && d.soloRol != null) {
            Jugador dueno = partida.jugador(d.soloRol);
            boolean mio = d.soloRol == Sincronia.miRol();
            String txt = mio ? "¡Es tu turno! Escoge con el clic o con las teclas 1 a "
                    + d.opciones.size() + "."
                    : "Está escogiendo " + (dueno != null ? dueno.titulo() : d.soloRol.nombre) + ". Tú miras y aconsejas.";
            g.setFont(Estilo.fuente(Font.BOLD, 28f));
            int w = g.getFontMetrics().stringWidth(txt) + 60;
            g.setColor(new Color(0, 0, 0, 170));
            g.fill(new RoundRectangle2D.Double(740, 936, Math.min(1090, w), 52, 26, 26));
            g.setColor(mio ? Estilo.DORADO_CLARO : Color.WHITE);
            g.drawString(txt, 770, 972);
        }

        // ---- temporizador ----
        if (d.limite > 0) {
            double f = Math.max(0, d.restante / d.limite);
            Color c = f > 0.35 ? new Color(0x4C8BD9) : new Color(0xDD3B2E);
            barra(g, 740, 1010, 1090, 28, f, c);
            g.setFont(Estilo.fuente(Font.BOLD, 22f));
            g.setColor(Color.WHITE);
            g.drawString("Si no decides, te quedas en silencio...", 760, 1000);
        }
    }

    private void dibujarCelular(Graphics2D g, Decision d) {
        Estilo.madera(g, 90, 100, 580, 880, 56);
        Graphics2D gg = (Graphics2D) g.create();
        gg.clip(new RoundRectangle2D.Double(112, 128, 536, 824, 36, 36));
        gg.setPaint(new GradientPaint(0, 128, new Color(0x2B2350), 0, 952, new Color(0x4B3A80)));
        gg.fillRect(112, 128, 536, 824);

        // barra de estado
        gg.setColor(Color.WHITE);
        gg.setFont(Estilo.fuente(Font.BOLD, 24f));
        gg.drawString(partida.reloj.texto(), 140, 162);
        for (int i = 0; i < 4; i++) {
            gg.setColor(i == 0 ? Color.WHITE : new Color(255, 255, 255, 70));
            gg.fillRect(556 + i * 14, 160 - 6 - i * 5, 9, 6 + i * 5);
        }

        // publicacion
        gg.setColor(new Color(0, 0, 0, 80));
        gg.fill(new RoundRectangle2D.Double(139, 214, 490, 480, 30, 30));
        gg.setColor(new Color(0xFBF4FF));
        gg.fill(new RoundRectangle2D.Double(135, 205, 490, 480, 30, 30));
        gg.setColor(new Color(0x9A8FB5));
        gg.fill(new Ellipse2D.Double(155, 225, 64, 64));
        gg.setColor(Color.WHITE);
        gg.setFont(Estilo.fuente(Font.BOLD, 40f));
        centrado(gg, "?", 187, 270);
        gg.setColor(Estilo.TEXTO);
        gg.setFont(Estilo.fuente(Font.BOLD, 28f));
        gg.drawString(d.autor, 236, 254);
        gg.setFont(Estilo.fuente(Font.PLAIN, 20f));
        gg.setColor(Estilo.TEXTO_SUAVE);
        gg.drawString("ahora", 236, 280);

        gg.setFont(Estilo.fuente(Font.BOLD, 33f));
        gg.setColor(Estilo.TEXTO);
        FontMetrics fm = gg.getFontMetrics();
        int ty = 350;
        for (String l : envolver(d.situacion, fm, 450)) {
            gg.drawString(l, 155, ty);
            ty += fm.getHeight();
        }

        // contadores que suben solos
        double ds = partida.desinfo(id) - Partida.DESINFO_INICIAL;
        int likes = 120 + (int) (ds * 4 + d.t * 7);
        int coms = 40 + (int) (ds * 1.5 + d.t * 3);
        int shares = 9 + (int) (ds * 1.2 + d.t * 2);
        gg.setFont(Estilo.fuente(Font.BOLD, 24f));
        gg.setColor(new Color(0xD9304F));
        gg.fill(corazon(175, 640, 13));
        gg.setColor(Estilo.TEXTO);
        gg.drawString(String.valueOf(likes), 198, 650);
        gg.setColor(new Color(0x4C8BD9));
        gg.fill(new RoundRectangle2D.Double(300, 624, 30, 22, 8, 8));
        gg.setColor(Estilo.TEXTO);
        gg.drawString(String.valueOf(coms), 340, 650);
        gg.setColor(new Color(0x5BA843));
        gg.fillPolygon(new int[]{450, 450, 478}, new int[]{626, 650, 638}, 3);
        gg.setColor(Estilo.TEXTO);
        gg.drawString(String.valueOf(shares), 490, 650);

        // contexto
        if (d.contexto != null && !d.contexto.isEmpty()) {
            gg.setFont(Estilo.fuente(Font.PLAIN, 25f));
            gg.setColor(new Color(0xE8E0FF));
            FontMetrics f2 = gg.getFontMetrics();
            int cy = 735;
            int n = 0;
            for (String l : envolver(d.contexto, f2, 480)) {
                if (n++ >= 6) {
                    break;
                }
                gg.drawString(l, 140, cy);
                cy += f2.getHeight();
            }
        }
        gg.dispose();
    }
}
