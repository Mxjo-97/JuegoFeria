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
 *   - EQUIPO: uno camina (el lider) y los otros tres lo siguen. 1-4 cambian quien camina.
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
        /** Texto sobre la barra de tiempo (null = el de siempre). */
        String avisoLimite;
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
    /** Habilidades: el rumor deja de crecer hasta este momento. */
    private double rumorPausaHasta = -1;
    /** Vecinos con los que ya se hablo en este lugar (Ver vinculos marca a los que faltan). */
    private final java.util.Set<String> hablados = new java.util.HashSet<>();

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
    /** NPC que son solo una voz (no se dibujan): la tele. */
    private static final java.util.Set<String> VOCES_SIN_SPRITE = java.util.Set.of("vlado");

    public Actor npc(String id, String nombre, String nombreNiebla, double x, double y, Color ropa) {
        Actor a = new Actor(id, x, y, 150, 250)
                .colores(new Color(0xE8B88A), ropa, new Color(0x4A2E1A))
                .imagen(Rutas.spriteVecino(id))
                .nombre(nombre, nombreNiebla);
        if (VOCES_SIN_SPRITE.contains(id)) {
            // la tele ya esta dibujada en el fondo: Vlado no tiene muñeco, solo se le habla
            a.invisible = true;
            a.solido(false);
        }
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
        mostrarMisionesUnRato(6);
        Musica.ambienteJuego(partida.hayNiebla(id));
        if (hora >= 0) {
            partida.reloj.llegar(hora);
        }
        if (rumor != null && partida.pasoUmbral(id) && !partida.hayNiebla(id) && nieblaPantalla != null) {
            activarNiebla();
        }
        if (primeraEntrada) {
            primeraEntrada = false;
            // votacion: quien dijo "Voy a mi ritmo" no puede pedir ayuda en la escena que sigue
            for (Jugador j : partida.jugadores()) {
                j.quitar("a_su_ritmo");
                if (j.tiene("a_su_ritmo_pendiente")) {
                    j.quitar("a_su_ritmo_pendiente");
                    j.poner("a_su_ritmo");
                }
            }
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
            if (o.id.equals(objetivoId) && !o.hecho) {
                o.hecho = true;
                mostrarMisionesUnRato(4);
                lluvia(60, 120, 16, 0);
                Ajustes.sfx("/sonidos/todos_listos.wav", "/sonidos/noticia.wav");
            }
            todos &= o.hecho;
        }
        if (todos && !completadoTodo) {
            completadoTodo = true;
            lluvia(W / 2.0, 120, 40, 0);
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
        mj.juega = quien;      // para los globos de consejo de los otros tres
        mj.nombres = new java.util.EnumMap<>(Rol.class);
        for (Jugador jj : partida.jugadores()) {
            mj.nombres.put(jj.rol, jj.nombre);
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
        // primero se ve hasta donde llego el audio, y despues sigue el guion
        mostrarPropagacion(elecciones, () -> seguirDigital(elecciones));
    }

    private void seguirDigital(Map<Rol, Integer> elecciones) {
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

    /** Pone tiempo limite a la decision que esta abierta (votaciones de la casa: 30 segundos). */
    void limiteDecision(double segundos, Runnable alAgotar, String aviso) {
        if (decision != null) {
            decision.limite = segundos;
            decision.restante = segundos;
            decision.alAgotar = alAgotar;
            decision.avisoLimite = aviso;
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
    protected boolean conPlaca(Actor a) {
        return !cine && !a.invisible && !equipo.containsValue(a);   // los cuatro ya estan abajo en el HUD
    }

    @Override
    protected void alInteractuarCon(Actor a) {
        if (a != null && a.id != null) {
            hablados.add(a.id);
        }
        Ajustes.sfx("/sonidos/hover.wav", "/sonidos/tecla.wav");
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

    /**
     * Las habilidades ahora SIRVEN: la primera vez en cada lugar tienen un efecto fuerte
     * (bajan el rumor y lo frenan un rato); despues solo hacen lo visual.
     *   Ver vinculos  -> lineas entre vecinos + aro dorado sobre los que aun no te han hablado; rumor -2 y quieto 12 s.
     *   Lupa          -> pista en el minijuego + muestra el rumor tal cual; rumor -3 y quieto 15 s.
     *   Megafono      -> aviso a todo el lugar; rumor -6 y quieto 20 s.
     *   Convocatoria  -> los vecinos hacen circulo a tu lado (hablas rapidito con todos); animo +1 y rumor quieto 10 s.
     */
    public void usarHabilidad(Jugador j, Habilidad h) {
        Actor yo = equipo.get(j.rol);
        burbuja(yo, h.linea, 4);
        String clave = "hab_" + id + "_" + j.rol.name();
        boolean primera = !partida.bandera(clave);
        partida.poner(clave);
        Color oro = new Color(0xFFD35C);
        switch (h) {
            case VER_VINCULOS:
                vinculosHasta = tiempo + 10;
                Actor a = actor(partida.barrio.masConectado());
                if (a != null) {
                    burbuja(a, "(todos me escriben a mí)", 4);
                }
                int faltan = 0;
                for (Actor x : getActores()) {
                    if (!equipo.containsValue(x) && x.nombre != null && !x.invisible && !hablados.contains(x.id)) {
                        faltan++;
                    }
                }
                toast(faltan == 0 ? "Ya hablaste con todos los de aquí" : "Aro dorado: " + faltan + " vecinos que aún no te han contado nada", oro);
                if (primera && rumor != null) {
                    efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -2), "Ver vínculos");
                    rumorPausaHasta = Math.max(rumorPausaHasta, tiempo + 12);
                }
                break;
            case LUPA:
                partida.poner("lupa_" + id);   // los minijuegos la pueden revisar para dar pistas
                if (rumor != null) {
                    toast("La lupa lo dejó claro: «" + recortar(rumor, 60) + "»", oro);
                } else {
                    toast("Lupa lista: el minijuego de aquí marcará lo que cambió", oro);
                }
                if (primera && rumor != null) {
                    efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -3), "Lupa");
                    rumorPausaHasta = Math.max(rumorPausaHasta, tiempo + 15);
                }
                break;
            case MEGAFONO:
                if (primera && rumor != null) {
                    efectoCompartido(Efecto.de(Partida.DESINFO_LUGAR, -6), "Megáfono");
                    rumorPausaHasta = Math.max(rumorPausaHasta, tiempo + 20);
                    int k = 0;
                    for (Actor x : getActores()) {
                        if (!equipo.containsValue(x) && x.nombre != null && !x.invisible && k < 3) {
                            burbuja(x, k == 0 ? "¡Ah, era eso!" : k == 1 ? "Ya lo oí." : "Menos mal.", 3);
                            k++;
                        }
                    }
                } else if (!primera) {
                    toast("El megáfono ya sonó aquí: en el próximo lugar vuelve a servir", new Color(0xF2A7B8));
                }
                break;
            case CONVOCATORIA:
                convocar(yo);
                if (primera) {
                    efectoCompartido(Efecto.de(Partida.ANIMO, 1), "Convocatoria");
                    if (rumor != null) {
                        rumorPausaHasta = Math.max(rumorPausaHasta, tiempo + 10);
                    }
                    toast("Los vecinos se acercaron: ahora hablas con todos sin caminar", oro);
                }
                break;
            default:
                break;
        }
        if (primera && rumor != null && h != Habilidad.CONVOCATORIA && tiempo < rumorPausaHasta) {
            toast("El rumor se quedó quieto " + (int) Math.round(rumorPausaHasta - tiempo) + " segundos", new Color(0x9CC08A));
        }
        if (j.prestada == h) {
            dialogo(null, j.rol.name() + ": Gracias. La uso y te la devuelvo.");
            j.devolverPrestada();
        }
    }

    private static String recortar(String t, int n) {
        return t.length() <= n ? t : t.substring(0, n - 1) + "…";
    }

    /** Los vecinos del lugar se acercan y forman un circulo alrededor de quien convoca. */
    private void convocar(Actor centro) {
        List<Actor> vecinos = new ArrayList<>();
        for (Actor a : getActores()) {
            if (!equipo.containsValue(a) && a.nombre != null && !a.invisible) {
                vecinos.add(a);
            }
        }
        for (int i = 0; i < vecinos.size(); i++) {
            double ang = Math.PI * 2 * i / vecinos.size();
            vecinos.get(i).x = Math.max(90, Math.min(W - 90, centro.x + StrictMath.cos(ang) * 260));
            vecinos.get(i).y = Math.max(300, Math.min(H - 40, centro.y + StrictMath.sin(ang) * 90));
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
        if (redActiva()) {
            if (pres && (k == KeyEvent.VK_E || k == KeyEvent.VK_ENTER) && redT > 0.8) {
                terminarPropagacion();
            }
            return true;
        }
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
        if (cine && k != KeyEvent.VK_E && k != KeyEvent.VK_ENTER && k != KeyEvent.VK_F1) {
            return true;   // en la cena nadie camina ni usa habilidades
        }
        if (!pres || dialogoActivo() || estaBloqueado()) {
            return false;
        }
        switch (k) {
            case KeyEvent.VK_Q:
                abrirCelular();
                return true;
            case KeyEvent.VK_TAB:
            case KeyEvent.VK_M:
                alternarMisiones();
                return true;
            case KeyEvent.VK_1:
            case KeyEvent.VK_NUMPAD1:
                elegirLider(Rol.CIUDADANO);
                return true;
            case KeyEvent.VK_2:
            case KeyEvent.VK_NUMPAD2:
                elegirLider(Rol.INFLUENCER);
                return true;
            case KeyEvent.VK_3:
            case KeyEvent.VK_NUMPAD3:
                elegirLider(Rol.PERIODISTA);
                return true;
            case KeyEvent.VK_4:
            case KeyEvent.VK_NUMPAD4:
                elegirLider(Rol.CANDIDATO);
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

    /** 1 Ciudadano, 2 Influencer, 3 Periodista, 4 Candidato: ese pasa a caminar (en red no se cambia). */
    private void elegirLider(Rol r) {
        if (Sincronia.activa() || partida.jugador(r) == null || partida.lider().rol == r) {
            return;
        }
        Actor viejo = getJugador();
        partida.setLider(r);
        Jugador nuevo = partida.lider();
        Actor a = equipo.get(nuevo.rol);
        if (viejo != null && a != null && viejo != a) {
            double x = a.x, y = a.y;
            a.x = viejo.x;
            a.y = viejo.y;
            viejo.x = x;
            viejo.y = y;
            jugador(a);
            rastro.clear();
            lluvia(a.x, a.y - a.alto * 0.6, 14, 2);
            Ajustes.sfx("/sonidos/elegir_rol.wav", "/sonidos/tecla.wav");
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
        boolean misAbiertas = (misionesFijas || tiempo < misionesHasta) && !dialogoActivo() && decision == null;
        misionesAnim += ((misAbiertas ? 1 : 0) - misionesAnim) * Math.min(1, dt * 10);
        super.actualizar(dt);
        actualizarJugo(dt);
        actualizarPropagacion(dt);
        if (cine) {
            cineT += dt;
        }

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

        boolean ocupado = dialogoActivo() || decision != null || redActiva();
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

        if (rumor != null && !rumorResuelto && !dialogoActivo() && ritmo > 0 && tiempo >= rumorPausaHasta && !redActiva()) {
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

    // =====================================================================================
    // JUGO: particulas, numeritos que saltan de los anillos, sonidos chiquitos
    // =====================================================================================

    private static final class Particula {
        double x, y, vx, vy, t, vida, giro, tam;
        int tipo;          // 0 petalo, 1 mariposa, 2 destello, 3 mariposa viajera (va de (x,y) a (vx,vy))
        Color c;
    }

    private static final class Numerito {
        String s;
        Color c;
        double x, y, t;
    }

    private final List<Particula> particulas = new ArrayList<>();
    private final List<Numerito> numeritos = new ArrayList<>();
    private final double[] anilloX = new double[3];
    private final double[] anilloPop = new double[3];
    private double anilloY = 60;
    private int ultAnimo = Integer.MIN_VALUE, ultConf = Integer.MIN_VALUE, ultMariposas = -1;
    private double ultDesinfo = Double.NaN;

    private static final Color[] PETALOS = {new Color(0xF6C445), new Color(0xF2A7B8), new Color(0xFFFFFF),
        new Color(0x9CC08A), new Color(0xFBE08F)};

    /** Lluvia de petalos / mariposas / destellos desde (x, y). */
    void lluvia(double x, double y, int n, int tipo) {
        for (int i = 0; i < n; i++) {
            Particula p = new Particula();
            double a = Math.random() * Math.PI * 2;
            double v = 120 + Math.random() * 260;
            p.x = x;
            p.y = y;
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v - 160;
            p.vida = 1.1 + Math.random() * 0.9;
            p.giro = Math.random() * 6;
            p.tam = 0.7 + Math.random() * 0.7;
            p.tipo = tipo == 0 && i % 4 == 0 ? 1 : tipo;
            p.c = PETALOS[i % PETALOS.length];
            particulas.add(p);
        }
    }

    /** Una mariposa que vuela de (x0,y0) a (x1,y1). */
    private void mariposaViajera(double x0, double y0, double x1, double y1) {
        Particula p = new Particula();
        p.tipo = 3;
        p.x = x0;
        p.y = y0;
        p.vx = x1;
        p.vy = y1;
        p.vida = 1.3;
        p.tam = 1.6;
        particulas.add(p);
    }

    private void numerito(int anillo, String s, boolean bueno) {
        if (anilloX[anillo] <= 0) {
            return;   // los anillos todavia no se han dibujado
        }
        Numerito n = new Numerito();
        n.s = s;
        n.c = bueno ? new Color(0x5E9E4F) : new Color(0xD9604A);
        n.x = anilloX[anillo];
        n.y = anilloY + 50;
        numeritos.add(n);
        anilloPop[anillo] = 1;
        Ajustes.sfx(bueno ? "/sonidos/mariposa.wav" : "/sonidos/cancelar.wav", "/sonidos/tecla.wav");
    }

    private void actualizarJugo(double dt) {
        // cambios en los anillos -> numerito y saltico
        int an = partida.animo;
        if (ultAnimo != Integer.MIN_VALUE && an != ultAnimo) {
            numerito(0, (an > ultAnimo ? "+" : "") + (an - ultAnimo), an > ultAnimo);
        }
        ultAnimo = an;
        int co = partida.confianza.promedio();
        if (ultConf != Integer.MIN_VALUE && co != ultConf) {
            numerito(1, (co > ultConf ? "+" : "") + (co - ultConf), co > ultConf);
        }
        ultConf = co;
        if (rumor != null) {
            double ds = partida.desinfo(id);
            if (!Double.isNaN(ultDesinfo) && Math.abs(ds - ultDesinfo) >= 1.5) {   // saltos, no el goteo normal
                int d = (int) Math.round(ds - ultDesinfo);
                numerito(2, (d > 0 ? "+" : "") + d, d < 0);
                if (d < 0) {
                    lluvia(anilloX[2], anilloY, 10, 2);
                }
            }
            ultDesinfo = ds;
        }
        Jugador lider = Sincronia.activa() ? partida.jugador(Sincronia.miRol()) : partida.lider();
        if (lider != null) {
            if (ultMariposas >= 0 && lider.mariposas > ultMariposas) {
                Actor yo = getJugador();
                if (yo != null) {
                    mariposaViajera(yo.x, yo.y - yo.alto, W / 2.0 - 60, H - 52);
                    lluvia(yo.x, yo.y - yo.alto * 0.8, 10, 0);
                }
                Ajustes.sfx("/sonidos/mariposa.wav", "/sonidos/noticia.wav");
            }
            ultMariposas = lider.mariposas;
        }
        for (int i = 0; i < 3; i++) {
            anilloPop[i] = Math.max(0, anilloPop[i] - dt * 2.5);
        }
        for (Iterator<Particula> it = particulas.iterator(); it.hasNext();) {
            Particula p = it.next();
            p.t += dt;
            if (p.t > p.vida) {
                it.remove();
                continue;
            }
            if (p.tipo != 3) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.vy += 420 * dt;
                p.vx *= 1 - dt * 1.5;
                p.giro += dt * 6;
            }
        }
        for (Iterator<Numerito> it = numeritos.iterator(); it.hasNext();) {
            Numerito n = it.next();
            n.t += dt;
            if (n.t > 1.6) {
                it.remove();
            }
        }
    }

    private void dibujarJugo(Graphics2D g) {
        for (Particula p : particulas) {
            double k = p.t / p.vida;
            float a = (float) Math.max(0, Math.min(1, (1 - k) * 1.6));
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            if (p.tipo == 3) {
                double e = k * k * (3 - 2 * k);
                double x = p.x + (p.vx - p.x) * e;
                double y = p.y + (p.vy - p.y) * e - Math.sin(e * Math.PI) * 160;
                gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, (1 - k) * 4)));
                mariposita(gg, x, y, p.tam);
            } else if (p.tipo == 1) {
                mariposita(gg, p.x, p.y, p.tam);
            } else if (p.tipo == 2) {
                dibujarDestello(gg, p.x, p.y, 9 * p.tam, new Color(0xFFD35C));
            } else {
                gg.translate(p.x, p.y);
                gg.rotate(p.giro);
                gg.setColor(p.c);
                gg.fill(new Ellipse2D.Double(-7 * p.tam, -4 * p.tam, 14 * p.tam, 8 * p.tam));
            }
            gg.dispose();
        }
        for (Numerito n : numeritos) {
            double k = n.t / 1.6;
            float a = (float) Math.max(0, Math.min(1, k < 0.15 ? k / 0.15 : (1 - k) / 0.5));
            double esc = k < 0.15 ? 0.6 + k / 0.15 * 0.6 : 1.2 - Math.min(0.2, (k - 0.15));
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            gg.setFont(Estilo.fuente(Font.BOLD, (float) (30 * esc)));
            FontMetrics fm = gg.getFontMetrics();
            int x = (int) (n.x - fm.stringWidth(n.s) / 2.0), y = (int) (n.y + 30 + k * 40);
            gg.setColor(Color.WHITE);
            for (int dx = -2; dx <= 2; dx += 2) {
                for (int dy = -2; dy <= 2; dy += 2) {
                    gg.drawString(n.s, x + dx, y + dy);
                }
            }
            gg.setColor(n.c);
            gg.drawString(n.s, x, y);
            gg.dispose();
        }
    }

    // =====================================================================================
    // "!" SOBRE LOS VECINOS CON PRUEBA PENDIENTE  +  FLECHA HACIA LA SALIDA
    // =====================================================================================

    private final Map<String, java.util.EnumSet<Rol>> pruebasNpc = new java.util.HashMap<>();
    private boolean marcasActivas;

    /** El guion avisa: este vecino tiene la prueba de este rol. */
    public void marcarPrueba(String npc, Rol r) {
        pruebasNpc.computeIfAbsent(npc, k -> java.util.EnumSet.noneOf(Rol.class)).add(r);
    }

    /** Ese rol ya hizo su prueba. */
    public void pruebaHecha(Rol r) {
        for (java.util.EnumSet<Rol> s : pruebasNpc.values()) {
            s.remove(r);
        }
    }

    public void mostrarMarcas(boolean b) {
        marcasActivas = b;
    }

    private static int numeroDe(Rol r) {
        return r == Rol.CIUDADANO ? 1 : r == Rol.INFLUENCER ? 2 : r == Rol.PERIODISTA ? 3 : 4;
    }

    private void dibujarMarcas(Graphics2D g) {
        if (!marcasActivas || dialogoActivo() || decision != null) {
            return;
        }
        Jugador yo = Sincronia.activa() ? partida.jugador(Sincronia.miRol()) : partida.lider();
        if (yo == null) {
            return;
        }
        for (Map.Entry<String, java.util.EnumSet<Rol>> e : pruebasNpc.entrySet()) {
            Actor a = actor(e.getKey());
            if (a == null || e.getValue().isEmpty() || a == cercano()) {
                continue;   // si ya estas al lado, manda el letrerito "E Hablar con..."
            }
            boolean mia = e.getValue().contains(yo.rol);
            double bob = Math.sin(tiempo * 4 + a.x * 0.02) * 6;
            double cx = a.x, cy = a.y - a.alto - 46 + bob;
            if (mia) {
                double p = 0.5 + 0.5 * Math.sin(tiempo * 5);
                g.setColor(new Color(255, 211, 92, (int) (70 + 60 * p)));
                g.fill(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
                g.setPaint(new GradientPaint((float) cx, (float) (cy - 22), Estilo.DORADO_CLARO, (float) cx, (float) (cy + 22), Estilo.DORADO));
                g.fill(new Ellipse2D.Double(cx - 22, cy - 22, 44, 44));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3f));
                g.draw(new Ellipse2D.Double(cx - 22, cy - 22, 44, 44));
                g.setColor(HUD_TEXTO);
                g.setFont(Estilo.fuente(Font.BOLD, 30f));
                centrado(g, "!", (int) cx, (int) cy + 11);
            } else {
                // de otro: circulito crema con el numero de quien lo tiene que hacer
                StringBuilder sb = new StringBuilder();
                for (Rol r : e.getValue()) {
                    sb.append(sb.length() > 0 ? " " : "").append(numeroDe(r));
                }
                String t = sb.toString();
                g.setFont(Estilo.fuente(Font.BOLD, 16f));
                int w = Math.max(30, g.getFontMetrics().stringWidth(t) + 18);
                g.setColor(new Color(255, 250, 240, 200));
                g.fill(new RoundRectangle2D.Double(cx - w / 2.0, cy - 15, w, 30, 30, 30));
                g.setColor(HUD_BORDE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Double(cx - w / 2.0, cy - 15, w, 30, 30, 30));
                g.setColor(HUD_SUAVE);
                centrado(g, t, (int) cx, (int) cy + 6);
            }
        }
    }

    private void dibujarSalidas(Graphics2D g) {
        if (!completadoTodo || cine) {
            return;
        }
        for (Rectangle r : salidas) {
            double cx = r.getCenterX(), cy = r.getCenterY();
            // hacia donde apunta: hacia el borde por donde se sale
            double dx = 0, dy = -1;
            if (r.x < 200) {
                dx = -1;
                dy = 0;
            } else if (r.x + r.width > W - 200) {
                dx = 1;
                dy = 0;
            } else if (r.y + r.height > H - 120) {
                dy = 1;
            }
            double p = 0.5 + 0.5 * Math.sin(tiempo * 4);
            // brillo en el piso
            g.setPaint(new RadialGradientPaint((float) cx, (float) (r.y + r.height * 0.7), 160f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 211, 92, (int) (90 + 60 * p)), new Color(255, 211, 92, 0)}));
            g.fill(new Ellipse2D.Double(cx - 160, r.y + r.height * 0.7 - 70, 320, 140));
            // tres flechitas que avanzan
            for (int i = 0; i < 3; i++) {
                double k = ((tiempo * 1.2 + i / 3.0) % 1.0);
                double ax = cx - dx * 70 + dx * k * 140, ay = cy - dy * 70 + dy * k * 140;
                float al = (float) Math.sin(k * Math.PI);
                chevron(g, ax, ay, dx, dy, new Color(255, 255, 255, (int) (230 * al)), new Color(201, 138, 18, (int) (230 * al)));
            }
        }
        // pastillita "Sigue por aqui"
        if (!salidas.isEmpty() && !dialogoActivo() && decision == null) {
            Rectangle r = salidas.get(0);
            String t = "¡Sigue por aquí!";
            g.setFont(Estilo.fuente(Font.BOLD, 18f));
            int w = g.getFontMetrics().stringWidth(t) + 30;
            double x = Math.max(20, Math.min(W - w - 20, r.getCenterX() - w / 2.0));
            double y = Math.max(140, Math.min(H - 160, r.y - 50 + Math.sin(tiempo * 3) * 5));
            pastilla(g, x, y, w, 34, 34);
            g.setColor(HUD_TEXTO);
            g.drawString(t, (float) (x + 15), (float) (y + 23));
        }
    }

    private void chevron(Graphics2D g, double x, double y, double dx, double dy, Color relleno, Color borde) {
        AffineTransform at = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.atan2(dy, dx));
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        p.moveTo(-16, -22);
        p.lineTo(8, 0);
        p.lineTo(-16, 22);
        p.lineTo(-6, 0);
        p.closePath();
        g.setColor(borde);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(relleno);
        g.fill(p);
        g.setTransform(at);
    }

    // =====================================================================================
    // ¿HASTA DONDE LLEGO? (despues del celular): la red de vecinos se ilumina paso a paso
    // =====================================================================================

    private double redT = -1;
    private Runnable redFin;
    private final List<String> redNodos = new ArrayList<>();
    private final Map<String, double[]> redPos = new java.util.HashMap<>();
    private Map<String, Integer> redDist;
    private int redRojo, redVerde;
    private String redTitulo, redFrase, redOrigen;
    private static final double RED_PASO = 0.6, RED_DURA = 6.5;

    private boolean redActiva() {
        return redT >= 0;
    }

    /** Arma la animacion segun lo que eligio cada quien en el celular y al final corre 'fin'. */
    private void mostrarPropagacion(Map<Rol, Integer> el, Runnable fin) {
        GrafoRelaciones b = partida.barrio;
        // origen: el vecino mas conectado de este lugar (o el del barrio)
        String origen = null;
        int mejor = -1;
        for (Actor a : getActores()) {
            if (a.id != null && b.existe(a.id) && b.grado(a.id) > mejor) {
                mejor = b.grado(a.id);
                origen = a.id;
            }
        }
        if (origen == null) {
            origen = b.masConectado();
        }
        if (origen == null) {
            fin.run();
            return;
        }
        int nE = 0, nV = 0;
        for (int op : el.values()) {
            if (op == E) {
                nE++;
            } else if (op == A || op == B) {
                nV++;
            }
        }
        redOrigen = origen;
        redDist = b.distanciasDesde(origen);
        if (nE > 0 && nV > 0) {          // carrera: la verdad cubre lo cercano y el rumor se escapa mas lejos
            redVerde = 2 * nV;
            redRojo = redVerde + 2 * nE;
        } else if (nE > 0) {
            redVerde = -1;
            redRojo = 1 + 2 * nE;
        } else if (nV > 0) {
            redVerde = 1 + 2 * nV;
            redRojo = 0;
        } else {
            redVerde = -1;
            redRojo = 2;
        }
        if (nE > 0 && nV == 0) {
            redTitulo = "¡Se regó!";
            redFrase = "Cada reenvío lo lleva un paso más lejos.";
        } else if (nV > 0 && nE == 0) {
            redTitulo = "¡La verdad corrió primero!";
            redFrase = "Verificar a tiempo frena el rumor.";
        } else if (nV > 0) {
            redTitulo = "Carrera: verdad contra rumor";
            redFrase = "Unos verificaron y otros reenviaron.";
        } else {
            redTitulo = "Nadie hizo nada…";
            redFrase = "…y el rumor siguió caminando solito.";
        }
        // posiciones: anillos por distancia alrededor del centro
        redNodos.clear();
        redPos.clear();
        Map<Integer, List<String>> porNivel = new java.util.TreeMap<>();
        for (Map.Entry<String, Integer> e : redDist.entrySet()) {
            porNivel.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        double cx = W / 2.0, cy = 560;
        for (Map.Entry<Integer, List<String>> e : porNivel.entrySet()) {
            List<String> ns = e.getValue();
            java.util.Collections.sort(ns);
            int nivel = e.getKey();
            double rx = nivel * 170, ry = nivel * 78;
            for (int i = 0; i < ns.size(); i++) {
                double ang = -Math.PI / 2 + (Math.PI * 2 * i) / ns.size() + nivel * 0.45;
                double px = Math.max(90, Math.min(W - 90, cx + Math.cos(ang) * rx));
                double py = Math.max(200, Math.min(910, cy + Math.sin(ang) * ry));
                redPos.put(ns.get(i), new double[]{px, py});
                redNodos.add(ns.get(i));
            }
        }
        redFin = fin;
        redT = 0;
        bloquear(true);
        Ajustes.sfx("/sonidos/noticia.wav");
    }

    private void actualizarPropagacion(double dt) {
        if (redT < 0) {
            return;
        }
        double antes = redT;
        redT += dt;
        // un "tic" por cada paso de la onda
        if ((int) (antes / RED_PASO) != (int) (redT / RED_PASO) && redT / RED_PASO <= Math.max(redRojo, redVerde) + 1) {
            Ajustes.sfx("/sonidos/hover.wav", "/sonidos/tecla.wav");
        }
        if (redT > RED_DURA) {
            terminarPropagacion();
        }
    }

    private void terminarPropagacion() {
        if (redT < 0) {
            return;
        }
        redT = -1;
        if (decision == null) {
            bloquear(false);
        }
        Runnable f = redFin;
        redFin = null;
        if (f != null) {
            f.run();
        }
    }

    /** 0 = nada, 1 = rumor, 2 = verdad (en el momento t). */
    private int estadoNodo(String n, double nivelOnda) {
        Integer d = redDist.get(n);
        if (d == null || d > nivelOnda) {
            return 0;
        }
        if (redVerde >= 0 && d <= redVerde) {
            return 2;
        }
        return d <= redRojo ? 1 : 0;
    }

    private void dibujarPropagacion(Graphics2D g) {
        if (redT < 0) {
            return;
        }
        float entrada = (float) Math.min(1, redT / 0.35);
        float salida = (float) Math.min(1, (RED_DURA - redT) / 0.4);
        float a = Math.max(0, Math.min(entrada, salida));
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        gg.setColor(new Color(36, 24, 16, 232));
        gg.fillRect(0, 0, W, H);
        double onda = redT / RED_PASO;
        Color rojo = new Color(0xE0735E), verde = new Color(0x7CC47A), apagado = new Color(255, 250, 240, 70);

        // aristas
        GrafoRelaciones b = partida.barrio;
        for (String n : redNodos) {
            double[] p = redPos.get(n);
            for (String v : b.vecinos(n)) {
                double[] q = redPos.get(v);
                if (q == null || n.compareTo(v) > 0) {
                    continue;
                }
                int en = estadoNodo(n, onda), ev = estadoNodo(v, onda);
                gg.setStroke(new BasicStroke(en != 0 && ev != 0 ? 3.5f : 1.5f));
                gg.setColor(en != 0 && ev != 0 ? (en == 2 || ev == 2 ? verde : rojo) : new Color(255, 250, 240, 50));
                gg.draw(new Line2D.Double(p[0], p[1], q[0], q[1]));
                // puntico que viaja por la arista mientras la onda pasa
                int dn = redDist.getOrDefault(n, 99), dv = redDist.getOrDefault(v, 99);
                int lejos = Math.max(dn, dv);
                double k = onda - (lejos - 1);
                int est = estadoNodo(dn > dv ? n : v, lejos);
                if (Math.abs(dn - dv) == 1 && k > 0 && k < 1 && est != 0) {
                    double[] o = dn < dv ? p : q, d = dn < dv ? q : p;
                    double x = o[0] + (d[0] - o[0]) * k, y = o[1] + (d[1] - o[1]) * k;
                    gg.setColor(est == 2 ? verde : rojo);
                    gg.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
                }
            }
        }
        // nodos
        int nRojo = 0, nVerde = 0;
        for (String n : redNodos) {
            double[] p = redPos.get(n);
            int est = estadoNodo(n, onda);
            if (est == 1) {
                nRojo++;
            } else if (est == 2) {
                nVerde++;
            }
            Integer d = redDist.get(n);
            double pop = d != null ? Math.max(0, 1 - Math.abs(onda - d) * 3) : 0;
            double r = 20 + (est != 0 ? pop * 8 : 0);
            gg.setColor(est == 0 ? apagado : est == 1 ? rojo : verde);
            gg.fill(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
            gg.setColor(est == 0 ? new Color(255, 250, 240, 90) : Color.WHITE);
            gg.setStroke(new BasicStroke(n.equals(redOrigen) ? 4f : 2f));
            gg.draw(new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2));
            Actor ac = actor(n);
            String nom = ac != null && ac.nombre != null ? ac.nombre : Character.toUpperCase(n.charAt(0)) + n.substring(1).replace('_', ' ');
            gg.setFont(Estilo.fuente(Font.BOLD, 18f));
            gg.setColor(Color.WHITE);
            centrado(gg, nom.substring(0, 1).toUpperCase(), (int) p[0], (int) p[1] + 7);
            gg.setFont(Estilo.fuente(Font.BOLD, 14f));
            gg.setColor(est == 0 ? new Color(255, 250, 240, 110) : new Color(0xFFF3D6));
            centrado(gg, nom, (int) p[0], (int) (p[1] + r + 18));
        }
        // titulo y contadores
        gg.setFont(Estilo.fuente(Font.BOLD, 46f));
        gg.setColor(new Color(0xFFF3D6));
        centrado(gg, redTitulo, W / 2, 92);
        gg.setFont(Estilo.fuente(Font.BOLD, 22f));
        gg.setColor(new Color(0xE6D09B));
        centrado(gg, "¿Hasta dónde llegó el audio? " + redFrase, W / 2, 132);
        int px = W / 2 - 230;
        contador(gg, px, 985, rojo, "Rumor: " + nRojo + " vecinos");
        contador(gg, px + 250, 985, verde, "Verdad: " + nVerde + " vecinos");
        gg.setFont(Estilo.fuente(Font.BOLD, 15f));
        gg.setColor(new Color(255, 250, 240, 140));
        centrado(gg, "E para seguir", W / 2, 1050);
        gg.dispose();
    }

    private void contador(Graphics2D g, int x, int y, Color c, String t) {
        g.setColor(new Color(255, 250, 240, 235));
        g.fill(new RoundRectangle2D.Double(x, y - 26, 220, 40, 40, 40));
        g.setColor(c);
        g.fill(new Ellipse2D.Double(x + 14, y - 14, 16, 16));
        g.setFont(Estilo.fuente(Font.BOLD, 18f));
        g.setColor(HUD_TEXTO);
        g.drawString(t, x + 40, y);
    }

    // =====================================================================================
    // MODO CINEMATICA (la cena): nadie camina, barras negras, luz calida, sin HUD
    // =====================================================================================

    private boolean cine;
    private double cineT;

    /**
     * La escena se ve como cinematica: los cuatro quedan quietos en sus puestos (asientos en orden
     * Ciudadano, Influencer, Periodista, Candidato: {x, y} de los pies), barras negras arriba y abajo
     * y sin HUD. Los dialogos, decisiones y minijuegos funcionan igual que siempre.
     */
    public Lugar modoCinematica(int[][] asientos) {
        cine = true;
        quieto(true);
        Rol[] orden = {Rol.CIUDADANO, Rol.INFLUENCER, Rol.PERIODISTA, Rol.CANDIDATO};
        for (int i = 0; i < orden.length && i < asientos.length; i++) {
            Actor a = equipo.get(orden[i]);
            if (a != null) {
                a.x = asientos[i][0];
                a.y = asientos[i][1];
                a.mirandoIzquierda = a.x > W / 2.0;
            }
        }
        return this;
    }

    private void dibujarCine(Graphics2D g) {
        double k = Math.min(1, cineT / 1.2);
        k = k * k * (3 - 2 * k);
        // luz calida que respira sobre la mesa
        double p = 0.5 + 0.5 * Math.sin(tiempo * 1.6) + 0.15 * Math.sin(tiempo * 7.3);
        g.setPaint(new RadialGradientPaint(W / 2f, 520f, 760f, new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(255, 214, 140, (int) (40 + 18 * p)), new Color(255, 190, 110, 0), new Color(30, 15, 5, 150)}));
        g.fillRect(0, 0, W, H);
        // motitas de polvo
        for (int i = 0; i < 26; i++) {
            double sx = (i * 0.37 % 1.0) * W + Math.sin(tiempo * 0.3 + i) * 40;
            double sy = ((i * 0.61 % 1.0) * H - tiempo * (8 + i % 5 * 3)) % H;
            if (sy < 0) {
                sy += H;
            }
            g.setColor(new Color(255, 236, 190, 40 + (i % 4) * 20));
            g.fill(new Ellipse2D.Double(sx, sy, 3 + i % 3, 3 + i % 3));
        }
        // barras negras
        int barra = (int) (78 * k);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, barra);
        g.fillRect(0, H - barra, W, barra);
        if (cineT < 4) {
            float a = (float) Math.max(0, Math.min(1, Math.min(cineT - 0.6, 4 - cineT)));
            Graphics2D gg = (Graphics2D) g.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            gg.setFont(Estilo.fuente(Font.BOLD, 22f));
            gg.setColor(new Color(0xFFF3D6));
            centrado(gg, titulo.toUpperCase() + "  ·  " + partida.reloj.texto(), W / 2, 50);
            gg.dispose();
        }
    }

    // ===================== DIBUJO =====================

    private static final Rectangle RECT_CELULAR = new Rectangle(1792, 22, 92, 100);

    @Override
    protected void dibujarCapas(Graphics2D g) {
        if (partida.hayNiebla(id)) {
            dibujarNiebla(g);
        }
        if (tiempo < vinculosHasta) {
            dibujarVinculos(g);
        }
        if (cine) {
            dibujarCine(g);
        }
        dibujarMariposas(g);
        dibujarMariposasDeVecinos(g);
        dibujarSalidas(g);
        dibujarMarcas(g);
        dibujarBurbujas(g);
        if (!cine) {
            dibujarHud(g);
            if (!dialogoActivo() && decision == null) {
                dibujarChat(g);
            }
        }
        dibujarJugo(g);
        dibujarToasts(g);
        if (decision != null) {
            dibujarDecision(g, decision);
        }
        dibujarPropagacion(g);
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

    /** Habilidad del Ciudadano: lineas entre los vecinos que se hablan (GrafoRelaciones). */
    private void dibujarVinculos(Graphics2D g) {
        double a = Math.min(1, (vinculosHasta - tiempo) / 1.0);
        // aro dorado sobre los vecinos que aun no te han hablado
        for (Actor x : getActores()) {
            if (equipo.containsValue(x) || x.nombre == null || x.invisible || hablados.contains(x.id)) {
                continue;
            }
            double p = 0.5 + 0.5 * Math.sin(tiempo * 5 + x.x * 0.01);
            g.setColor(new Color(255, 211, 92, (int) ((90 + 90 * p) * a)));
            g.setStroke(new BasicStroke(4f));
            g.draw(new Ellipse2D.Double(x.x - x.ancho * 0.5, x.y - 16, x.ancho, 32));
            dibujarDestello(g, x.x, x.y - x.alto - 26 - 6 * p, 12, new Color(255, 211, 92, (int) (230 * a)));
        }
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

    /** Vecinos que SIEMPRE andan con mariposas amarillas alrededor (capricho bonito). */
    private static final java.util.Set<String> CON_MARIPOSAS = java.util.Set.of("mauricio");

    private void dibujarMariposasDeVecinos(Graphics2D g) {
        for (Actor a : getActores()) {
            if (a.invisible || a.id == null || !CON_MARIPOSAS.contains(a.id)) {
                continue;
            }
            int n = 5;
            for (int i = 0; i < n; i++) {
                double fase = i * Math.PI * 2 / n + a.x * 0.01;
                double ang = tiempo * (0.9 + i * 0.07) + fase;
                double rx = 125 + (i % 2) * 30, ry = 85 + (i % 3) * 20;
                double x = a.x + Math.cos(ang) * rx + Math.sin(tiempo * 2.3 + i) * 8;
                double y = a.y - a.alto * 0.55 + Math.sin(ang * 1.3) * ry + Math.cos(tiempo * 3.1 + i) * 6;
                // estela de brillitos
                g.setColor(new Color(255, 214, 92, 70));
                g.fill(new Ellipse2D.Double(x - Math.cos(ang) * 14 - 3, y - 3, 6, 6));
                mariposita(g, x, y, 1.35 + (i % 3) * 0.2);
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

    // =====================================================================================
    // HUD "AESTHETIC": pastillas crema translucidas, poca cosa en pantalla.
    //   arriba izq.  -> dia, hora y lugar (una sola pastilla) + chip de misiones (TAB / M la abre)
    //   arriba der.  -> tres anillos (animo, confianza, desinformacion) + boton del celular
    //   abajo centro -> los cuatro personajes (1-4) y las teclas
    //   abajo der.   -> el chat de la casa, solo unos segundos cuando llega un mensaje
    // =====================================================================================

    private static final Color HUD_CREMA = new Color(255, 250, 240, 228);
    private static final Color HUD_BORDE = new Color(0xEAD7AE);
    private static final Color HUD_TEXTO = new Color(0x5A3B1F);
    private static final Color HUD_SUAVE = new Color(0xA88B6A);
    private static final Color HUD_ROSA = new Color(0xF2A7B8);
    private static final Color HUD_SALVIA = new Color(0x9CC08A);

    /** Misiones: abiertas a mano (TAB o M) o un ratico solas (al entrar y al cumplir una). */
    private boolean misionesFijas = false;
    private double misionesHasta = -1;
    private double misionesAnim = 0;

    /** Chat: se muestra unos segundos cuando llega un mensaje nuevo. */
    private ChatCasa.Mensaje ultimoChat;
    private double chatHasta = -1;

    void alternarMisiones() {
        misionesFijas = !misionesFijas;
        misionesHasta = -1;
    }

    void mostrarMisionesUnRato(double segundos) {
        misionesHasta = Math.max(misionesHasta, tiempo + segundos);
    }

    /** Pastilla crema con sombrita (la base de todo el HUD). */
    private static void pastilla(Graphics2D g, double x, double y, double w, double h, double r) {
        g.setColor(new Color(60, 35, 15, 38));
        g.fill(new RoundRectangle2D.Double(x + 1, y + 4, w, h, r, r));
        g.setColor(HUD_CREMA);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
        g.setColor(HUD_BORDE);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    /** Teclita redonda (E, H, 1...). */
    private static void teclita(Graphics2D g, String t, double cx, double cy, double r, boolean dorada) {
        if (dorada) {
            g.setPaint(new GradientPaint((float) cx, (float) (cy - r), Estilo.DORADO_CLARO, (float) cx, (float) (cy + r), Estilo.DORADO));
        } else {
            g.setColor(new Color(0xFFFDF7));
        }
        g.fill(new RoundRectangle2D.Double(cx - Math.max(r, t.length() * r * 0.45 + r * 0.4), cy - r,
                Math.max(r, t.length() * r * 0.45 + r * 0.4) * 2, r * 2, r * 2, r * 2));
        g.setColor(dorado(dorada));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(cx - Math.max(r, t.length() * r * 0.45 + r * 0.4), cy - r,
                Math.max(r, t.length() * r * 0.45 + r * 0.4) * 2, r * 2, r * 2, r * 2));
        g.setColor(HUD_TEXTO);
        g.setFont(Estilo.fuente(Font.BOLD, (float) (r * 1.05)));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, (float) (cx - fm.stringWidth(t) / 2.0), (float) (cy + fm.getAscent() * 0.36));
    }

    private static Color dorado(boolean d) {
        return d ? new Color(0xC98A12) : HUD_BORDE;
    }

    private static double anchoTeclita(String t, double r) {
        return Math.max(r, t.length() * r * 0.45 + r * 0.4) * 2;
    }

    private void dibujarHud(Graphics2D g) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        dibujarRelojHud(gg);
        dibujarMisiones(gg);
        dibujarAnillos(gg);
        dibujarBotonCelular(gg);
        dibujarBarraInferior(gg);
        gg.dispose();
    }

    // ---- arriba a la izquierda: dia, hora y lugar ----
    private void dibujarRelojHud(Graphics2D g) {
        String hora = partida.reloj.texto();
        String dia = "DÍA " + partida.dia + " DE " + Partida.DIAS;
        String lugar = titulo.toUpperCase();
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        int wHora = g.getFontMetrics().stringWidth(hora);
        g.setFont(Estilo.fuente(Font.BOLD, 14f));
        int wLugar = g.getFontMetrics().stringWidth(lugar);
        int w = Math.max(250, Math.max(wHora + 200, wLugar + 110));
        int x = 24, y = 22, h = 68;
        pastilla(g, x, y, w, h, h);

        // relojito
        int cx = x + 36, cy = y + h / 2, r = 22;
        g.setPaint(new GradientPaint(cx, cy - r, new Color(0xFFE7A6), cx, cy + r, new Color(0xF3BE55)));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(0xFFFBF1));
        g.fillOval(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8);
        int min = partida.reloj.minutos();
        double am = (min % 60) * Math.PI / 30 - Math.PI / 2;
        double ah = (min % 720) * Math.PI / 360 - Math.PI / 2;
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(HUD_TEXTO);
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(ah) * 9, cy + Math.sin(ah) * 9));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xD9774F));
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(am) * 13, cy + Math.sin(am) * 13));
        g.setColor(HUD_TEXTO);
        g.fillOval(cx - 2, cy - 2, 5, 5);

        // hora grande + dia chiquito
        g.setFont(Estilo.fuente(Font.BOLD, 26f));
        g.setColor(HUD_TEXTO);
        g.drawString(hora, x + 70, y + 35);
        g.setFont(Estilo.fuente(Font.BOLD, 13f));
        g.setColor(HUD_SUAVE);
        g.drawString(dia, x + 80 + wHora, y + 33);
        // lugar
        g.setColor(HUD_ROSA);
        g.fillOval(x + 71, y + 46, 7, 7);
        g.setFont(Estilo.fuente(Font.BOLD, 14f));
        g.setColor(HUD_SUAVE);
        g.drawString(lugar, x + 85, y + 54);
    }

    // ---- misiones: chip pequeñito; la lista solo cuando se pide (o un ratico) ----
    private void dibujarMisiones(Graphics2D g) {
        if (objetivos.isEmpty()) {
            return;
        }
        int hechas = 0;
        for (Objetivo o : objetivos) {
            if (o.hecho) {
                hechas++;
            }
        }

        int x = 24, y = 102;
        String t = hechas == objetivos.size() ? "Misiones listas" : "Misiones " + hechas + "/" + objetivos.size();
        g.setFont(Estilo.fuente(Font.BOLD, 16f));
        int wt = g.getFontMetrics().stringWidth(t);
        int w = wt + 52 + (int) anchoTeclita("TAB", 11) + 10, h = 36;
        pastilla(g, x, y, w, h, h);
        // estrellita
        dibujarDestello(g, x + 20, y + h / 2, 7, hechas == objetivos.size() ? HUD_SALVIA : Estilo.DORADO);
        g.setColor(HUD_TEXTO);
        g.drawString(t, x + 36, y + 24);
        teclita(g, "TAB", x + 36 + wt + 12 + anchoTeclita("TAB", 11) / 2, y + h / 2, 11, false);

        if (misionesAnim < 0.03) {
            return;
        }
        // tarjeta con la lista
        g.setFont(Estilo.fuente(Font.BOLD, 18f));
        FontMetrics fm = g.getFontMetrics();
        int wl = 0;
        for (Objetivo o : objetivos) {
            wl = Math.max(wl, fm.stringWidth(o.texto));
        }
        int cw = wl + 76, ch = 22 + objetivos.size() * 36;
        Graphics2D c = (Graphics2D) g.create();
        c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, misionesAnim)));
        int cy = y + h + 8 - (int) ((1 - misionesAnim) * 12);
        pastilla(c, x, cy, cw, ch, 26);
        int ly = cy + 37;
        for (Objetivo o : objetivos) {
            int bx = x + 20, by = ly - 16;
            if (o.hecho) {
                c.setColor(HUD_SALVIA);
                c.fillOval(bx, by, 20, 20);
                c.setColor(Color.WHITE);
                c.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                c.drawLine(bx + 5, by + 10, bx + 9, by + 14);
                c.drawLine(bx + 9, by + 14, bx + 15, by + 6);
            } else {
                c.setColor(new Color(0xFFFDF7));
                c.fillOval(bx, by, 20, 20);
                c.setColor(HUD_ROSA);
                c.setStroke(new BasicStroke(2f));
                c.drawOval(bx, by, 20, 20);
            }
            c.setFont(Estilo.fuente(Font.BOLD, 18f));
            c.setColor(o.hecho ? HUD_SUAVE : HUD_TEXTO);
            c.drawString(o.texto, x + 52, ly);
            if (o.hecho) {
                c.setColor(new Color(0xC9B08A));
                c.setStroke(new BasicStroke(1.5f));
                c.drawLine(x + 52, ly - 6, x + 52 + fm.stringWidth(o.texto), ly - 6);
            }
            ly += 36;
        }
        c.dispose();
    }

    /** Destello de cuatro puntas (adorno). */
    private void dibujarDestello(Graphics2D g, double cx, double cy, double r, Color col) {
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        p.moveTo(cx, cy - r);
        p.quadTo(cx, cy, cx + r, cy);
        p.quadTo(cx, cy, cx, cy + r);
        p.quadTo(cx, cy, cx - r, cy);
        p.quadTo(cx, cy, cx, cy - r);
        g.setColor(col);
        g.fill(p);
    }

    // ---- arriba a la derecha: tres anillos ----
    private void dibujarAnillos(Graphics2D g) {
        int animo = partida.animo;
        int confianza = partida.confianza.promedio();
        boolean conRumor = rumor != null;
        int n = conRumor ? 3 : 2;
        int paso = 112, h = 92;
        int w = n * paso + 16;
        int x = 1780 - w - 14, y = 22;
        pastilla(g, x, y, w, h, 40);
        int cx = x + 8 + paso / 2, cy = y + 38;
        anilloY = cy;
        anilloX[0] = cx;
        anilloX[1] = cx + paso;
        anilloX[2] = cx + paso * 2;
        anillo(g, cx, cy, animo / 10.0, new Color(0xF0A85C), 0, "ÁNIMO", animo + "/10", -1, false);
        cx += paso;
        anillo(g, cx, cy, confianza / 10.0, new Color(0x7FB2E0), 1, "CONFIANZA", confianza + "/10", -1, false);
        if (conRumor) {
            cx += paso;
            double ds = partida.desinfo(id);
            Color c = ds > Partida.UMBRAL_NIEBLA ? new Color(0xE0735E) : ds > 50 ? new Color(0xEDB45A) : HUD_SALVIA;
            anillo(g, cx, cy, ds / 100.0, c, 2, tiempo < rumorPausaHasta ? "EN PAUSA" : "RUMOR",
                    (int) ds + "%", Partida.UMBRAL_NIEBLA / 100.0, tiempo < rumorPausaHasta);
        }
    }

    private void anillo(Graphics2D g, int cx, int cy, double p, Color col, int icono, String nombre,
            String valor, double umbral, boolean pausa) {
        p = Math.max(0, Math.min(1, p));
        int r = (int) (25 * (1 + 0.28 * Math.sin(anilloPop[Math.min(2, icono)] * Math.PI)));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xF1E6D0));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(col);
        if (p > 0.001) {
            g.draw(new java.awt.geom.Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 90, -360 * p, java.awt.geom.Arc2D.OPEN));
        }
        if (umbral >= 0) {
            // puntico que marca donde empieza la niebla
            double a = -Math.PI / 2 + umbral * Math.PI * 2;
            double px = cx + Math.cos(a) * r, py = cy + Math.sin(a) * r;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
            g.setColor(new Color(0xE0735E));
            g.fill(new Ellipse2D.Double(px - 3, py - 3, 6, 6));
        }
        // icono en el centro
        g.setColor(col.darker());
        if (icono == 0) {          // corazon
            g.fillOval(cx - 9, cy - 7, 10, 10);
            g.fillOval(cx - 1, cy - 7, 10, 10);
            g.fillPolygon(new int[]{cx - 9, cx + 9, cx}, new int[]{cy - 1, cy - 1, cy + 9}, 3);
        } else if (icono == 1) {   // dos manitos / personas
            g.fillOval(cx - 9, cy - 8, 8, 8);
            g.fillOval(cx + 1, cy - 8, 8, 8);
            g.fillRoundRect(cx - 11, cy + 1, 11, 9, 6, 6);
            g.fillRoundRect(cx, cy + 1, 11, 9, 6, 6);
        } else if (pausa) {        // pausa
            g.fillRoundRect(cx - 7, cy - 8, 5, 16, 3, 3);
            g.fillRoundRect(cx + 2, cy - 8, 5, 16, 3, 3);
        } else {                   // megafono chiquito
            g.fillPolygon(new int[]{cx - 9, cx - 2, cx + 8, cx + 8, cx - 2, cx - 9},
                    new int[]{cy - 4, cy - 4, cy - 10, cy + 10, cy + 4, cy + 4}, 6);
        }
        g.setFont(Estilo.fuente(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        String t = nombre + " · " + valor;
        g.setColor(HUD_SUAVE);
        g.drawString(t, cx - fm.stringWidth(t) / 2, cy + r + 22);
    }

    // ---- el celular: boton redondo ----
    private void dibujarBotonCelular(Graphics2D g) {
        boolean pendiente = !digitalHecho && digOpciones != null;
        int cx = RECT_CELULAR.x + RECT_CELULAR.width / 2, cy = RECT_CELULAR.y + 46;
        double s = pendiente ? 1 + 0.06 * Math.sin(tiempo * 6) : 1;
        int r = (int) (40 * s);
        g.setColor(new Color(60, 35, 15, 38));
        g.fillOval(cx - r + 1, cy - r + 4, r * 2, r * 2);
        g.setColor(HUD_CREMA);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(pendiente ? HUD_ROSA : HUD_BORDE);
        g.setStroke(new BasicStroke(pendiente ? 3f : 1.6f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        // telefonito
        AffineTransform at = g.getTransform();
        g.translate(cx, cy);
        if (pendiente && (tiempo % 2.5) < 0.35) {
            g.rotate(Math.sin(tiempo * 60) * 0.1);
        }
        g.setColor(HUD_TEXTO);
        g.fill(new RoundRectangle2D.Double(-13, -21, 26, 42, 9, 9));
        g.setPaint(new GradientPaint(0, -16, new Color(0xBFE6F7), 0, 14, new Color(0x8EC5E8)));
        g.fill(new RoundRectangle2D.Double(-10, -16, 20, 30, 5, 5));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(-7, -12, 14, 4, 3, 3));
        g.fill(new RoundRectangle2D.Double(-7, -5, 10, 4, 3, 3));
        g.setTransform(at);
        if (pendiente) {
            g.setColor(new Color(0xE8607A));
            g.fillOval(cx + r - 22, cy - r - 4, 24, 24);
            g.setColor(Color.WHITE);
            g.setFont(Estilo.fuente(Font.BOLD, 16f));
            centrado(g, "!", cx + r - 10, cy - r + 14);
        }
        teclita(g, "Q", cx, cy + r + 4, 11, pendiente);
    }

    // ---- abajo: los cuatro personajes (1-4) y las teclas ----
    private void dibujarBarraInferior(Graphics2D g) {
        boolean red = Sincronia.activa();
        Jugador lider = red ? partida.jugador(Sincronia.miRol()) : partida.lider();
        if (lider == null || dialogoActivo()) {
            return;
        }
        Rol[] orden = {Rol.CIUDADANO, Rol.INFLUENCER, Rol.PERIODISTA, Rol.CANDIDATO};
        List<Jugador> js = new ArrayList<>();
        for (Rol r : orden) {
            Jugador j = partida.jugador(r);
            if (j != null && (!red || j == lider)) {
                js.add(j);
            }
        }
        boolean listo = !partida.bandera("hab_" + id + "_" + lider.rol.name());
        List<String[]> teclas = new ArrayList<>();
        teclas.add(new String[]{"H", lider.rol.habilidad.nombre});
        if (!red) {
            teclas.add(new String[]{"P", "Prestar"});
        }
        teclas.add(new String[]{"F", "Frases"});

        // medidas
        int caraR = 22, caraActivaR = 28, hueco = 12;
        int wCaras = 0;
        for (Jugador j : js) {
            wCaras += (j == lider ? caraActivaR : caraR) * 2 + hueco;
        }
        g.setFont(Estilo.fuente(Font.BOLD, 18f));
        int wNombre = g.getFontMetrics().stringWidth(lider.nombre) + 10;
        g.setFont(Estilo.fuente(Font.BOLD, 15f));
        FontMetrics fm = g.getFontMetrics();
        int wTeclas = 0;
        for (String[] t : teclas) {
            wTeclas += (int) anchoTeclita(t[0], 12) + 8 + fm.stringWidth(t[1]) + 20;
        }
        int h = 66;
        int w = 24 + wCaras + wNombre + 70 + 26 + wTeclas + 10;
        int x = (W - w) / 2, y = H - h - 18;
        pastilla(g, x, y, w, h, h);

        // caras
        int cx = x + 22;
        int cy = y + h / 2;
        for (int i = 0; i < js.size(); i++) {
            Jugador j = js.get(i);
            boolean act = j == lider;
            int r = act ? caraActivaR : caraR;
            cx += r;
            retrato(g, j, cx, cy, r, act);
            if (!red) {
                int num = java.util.Arrays.asList(orden).indexOf(j.rol) + 1;
                g.setColor(act ? j.rol.color : new Color(0xFFFDF7));
                g.fillOval(cx + r - 14, cy + r - 16, 18, 18);
                g.setColor(act ? Color.WHITE : HUD_TEXTO);
                g.setFont(Estilo.fuente(Font.BOLD, 12f));
                centrado(g, String.valueOf(num), cx + r - 5, cy + r - 3);
            }
            cx += r + hueco;
        }
        // nombre + mariposas
        g.setFont(Estilo.fuente(Font.BOLD, 18f));
        g.setColor(HUD_TEXTO);
        g.drawString(lider.nombre, cx, cy - 2);
        g.setFont(Estilo.fuente(Font.BOLD, 12f));
        g.setColor(HUD_SUAVE);
        g.drawString(lider.rol.nombre.toUpperCase(), cx, cy + 16);
        int mx = cx + wNombre + 14;
        mariposita(g, mx, cy, 1.0);
        g.setFont(Estilo.fuente(Font.BOLD, 17f));
        g.setColor(HUD_TEXTO);
        g.drawString(String.valueOf(lider.mariposas), mx + 16, cy + 6);
        cx = mx + 56;

        // separador
        g.setColor(HUD_BORDE);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(cx, y + 16, cx, y + h - 16);
        cx += 18;

        // teclas
        for (String[] t : teclas) {
            double kw = anchoTeclita(t[0], 12);
            boolean brilla = "H".equals(t[0]) && listo;
            if (brilla) {
                double a = 0.35 + 0.35 * Math.sin(tiempo * 3);
                g.setColor(new Color(242, 176, 30, (int) (90 * a)));
                g.fill(new RoundRectangle2D.Double(cx - 5, cy - 17, kw + 8 + fm.stringWidth(t[1]) + 12, 34, 34, 34));
            }
            teclita(g, t[0], cx + kw / 2, cy, 12, brilla);
            g.setFont(Estilo.fuente(Font.BOLD, 15f));
            g.setColor(brilla ? HUD_TEXTO : HUD_SUAVE);
            g.drawString(t[1], (float) (cx + kw + 8), cy + 5);
            cx += (int) kw + 8 + fm.stringWidth(t[1]) + 20;
        }
    }

    /** Carita redonda del personaje (recorta la cabeza de su sprite "quieto"). */
    private void retrato(Graphics2D g, Jugador j, int cx, int cy, int r, boolean activo) {
        if (activo) {
            double p = 0.5 + 0.5 * Math.sin(tiempo * 3);
            g.setColor(new Color(j.rol.color.getRed(), j.rol.color.getGreen(), j.rol.color.getBlue(), (int) (60 + 50 * p)));
            g.fillOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);
        }
        Ellipse2D c = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(0xFFF6E6));
        g.fill(c);
        String ruta = "/imagenes/personajes/" + j.rol.carpeta + "/quieto.png";
        if (Recursos.existe(ruta)) {
            java.awt.image.BufferedImage im = Recursos.imagen(ruta, 205, 250);
            double esc = (r * 2.5) / im.getWidth();
            int iw = (int) (im.getWidth() * esc), ih = (int) (im.getHeight() * esc);
            java.awt.Shape antes = g.getClip();
            g.clip(c);
            g.drawImage(im, cx - iw / 2, (int) (cy - r * 1.05), iw, ih, null);
            g.setClip(antes);
        } else {
            g.setColor(j.rol.color);
            g.setFont(Estilo.fuente(Font.BOLD, r * 0.9f));
            centrado(g, j.rol.nombre.substring(0, 1), cx, cy + (int) (r * 0.32));
        }
        g.setColor(activo ? j.rol.color : HUD_BORDE);
        g.setStroke(new BasicStroke(activo ? 3f : 1.6f));
        g.draw(c);
        if (!activo) {
            g.setColor(new Color(255, 250, 240, 70));
            g.fill(c);
        }
    }

    private void mariposita(Graphics2D g, double x, double y, double esc) {
        double ala = 0.8 + 0.2 * Math.abs(Math.sin(tiempo * 5));
        Color a1 = new Color(0xF6C445), a2 = new Color(0xFBE08F);
        g.setColor(a1);
        g.fill(new Ellipse2D.Double(x - 15 * esc * ala, y - 12 * esc, 15 * esc * ala, 14 * esc));
        g.fill(new Ellipse2D.Double(x, y - 12 * esc, 15 * esc * ala, 14 * esc));
        g.setColor(a2);
        g.fill(new Ellipse2D.Double(x - 11 * esc * ala, y, 11 * esc * ala, 10 * esc));
        g.fill(new Ellipse2D.Double(x, y, 11 * esc * ala, 10 * esc));
        g.setColor(new Color(0xC98A12));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(x - 15 * esc * ala, y - 12 * esc, 15 * esc * ala, 14 * esc));
        g.draw(new Ellipse2D.Double(x, y - 12 * esc, 15 * esc * ala, 14 * esc));
        g.setColor(HUD_TEXTO);
        g.fill(new RoundRectangle2D.Double(x - 1.5, y - 11 * esc, 3, 21 * esc, 3, 3));
    }

    /** Chat de la casa: aparece unos segundos cuando llega algo nuevo y se va solito. */
    private void dibujarChat(Graphics2D g) {
        List<ChatCasa.Mensaje> ms = partida.chat.ultimos(2);
        if (ms.isEmpty()) {
            return;
        }
        ChatCasa.Mensaje u = ms.get(ms.size() - 1);
        if (u != ultimoChat) {
            if (ultimoChat != null) {
                chatHasta = tiempo + 7;
            }
            ultimoChat = u;
        }
        if (tiempo > chatHasta) {
            return;
        }
        float a = (float) Math.max(0, Math.min(1, Math.min((chatHasta - tiempo) / 0.8, (tiempo - (chatHasta - 7)) / 0.3)));
        Graphics2D gg = (Graphics2D) g.create();
        gg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        int w = 470, x = W - w - 24;
        int h = 46 + ms.size() * 34;
        int y = H - 100 - h + (int) ((1 - a) * 16);
        pastilla(gg, x, y, w, h, 26);
        gg.setColor(new Color(0x7CC47A));
        gg.fillOval(x + 18, y + 18, 10, 10);
        gg.setFont(Estilo.fuente(Font.BOLD, 15f));
        gg.setColor(HUD_SUAVE);
        gg.drawString(ChatCasa.NOMBRE.toUpperCase(), x + 36, y + 28);
        gg.setFont(Estilo.fuente(Font.PLAIN, 17f));
        FontMetrics fm = gg.getFontMetrics();
        int ty = y + 60;
        for (ChatCasa.Mensaje m : ms) {
            gg.setColor(HUD_SUAVE);
            gg.drawString(m.hora, x + 18, ty);
            gg.setColor(HUD_TEXTO);
            String t = m.texto;
            while (fm.stringWidth(t) > w - 100 && t.length() > 4) {
                t = t.substring(0, t.length() - 4) + "…";
            }
            gg.drawString(t, x + 78, ty);
            ty += 34;
        }
        gg.dispose();
    }

    /** Avisos: pastillitas centradas arriba, con un puntico del color del aviso. */
    private void dibujarToasts(Graphics2D g) {
        Graphics2D gg0 = (Graphics2D) g.create();
        gg0.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        gg0.setFont(Estilo.fuente(Font.BOLD, 20f));
        FontMetrics fm = gg0.getFontMetrics();
        int y = 24;
        for (Toast t : toasts) {
            double a = t.t < 0.25 ? t.t / 0.25 : (t.t > 2.6 ? Math.max(0, (3.2 - t.t) / 0.6) : 1);
            int w = fm.stringWidth(t.s) + 58, h = 40;
            int x = (W - w) / 2;
            int yy = y - (int) ((1 - Math.min(1, t.t / 0.25)) * 14);
            Graphics2D gg = (Graphics2D) gg0.create();
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) a));
            pastilla(gg, x, yy, w, h, h);
            gg.setColor(t.c);
            gg.fillOval(x + 16, yy + h / 2 - 6, 12, 12);
            gg.setColor(HUD_TEXTO);
            gg.drawString(t.s, x + 38, yy + 27);
            gg.dispose();
            y += 48;
        }
        gg0.dispose();
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
            g.drawString(d.avisoLimite != null ? d.avisoLimite : "Si no decides, te quedas en silencio...", 760, 1000);
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

        // contadores que suben solos (no en las votaciones de la casa: es el chat de la casa)
        if (d.avisoLimite == null) {
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
        }

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
