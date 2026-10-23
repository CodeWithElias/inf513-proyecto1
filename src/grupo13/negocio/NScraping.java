package grupo13.negocio;

import grupo13.config.Config;
import grupo13.datos.DEjecucion;
import grupo13.datos.DPagina;
import grupo13.datos.DPlataforma;
import grupo13.datos.DRedSocial;
import grupo13.datos.DTermino;
import grupo13.datos.DUsuario;
import grupo13.datos.DYoutube;

import java.io.IOException;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CU6 / RF12: detección de páginas no autorizadas mediante scraping.
 * Proceso de una ejecución (por plataforma; por ahora solo YouTube):
 *  1. Por cada término de termino_monitoreo (todos los activos, o uno elegido en la ejecución manual) se buscan
 *     canales (nombre/descripción) y videos (título/descripción → su canal).
 *  2. Solo cuentan los resultados que contienen el término (sin distinguir mayúsculas ni tildes).
 *  3. Cada canal encontrado se compara con las redes oficiales de esa plataforma registradas en el CU2
 *     (por @handle o /channel/ID). Las oficiales se ignoran.
 *  4. Canal no oficial y nuevo → pagina_no_autorizada "detectada" + detección con evidencia + alerta al rol configurado.
 *     Ya registrado → solo se agrega la detección. "eliminada" → se reabre y se alerta.
 *     Lo único autorizado es red_social (lo administra el personal en el CU2); el scraping no autoriza nada.
 *  5. Todo se guarda en una sola transacción y la ejecución queda registrada (completada o error, con el detalle).
 *  6. No se permiten dos ejecuciones simultáneas de la misma plataforma (una "en_proceso" de más de 30 minutos
 *     se cierra como interrumpida).
 */
public class NScraping {

    public static final String EN_PROCESO = "en_proceso";
    public static final String COMPLETADA = "completada";
    public static final String ERROR = "error";

    /** Plataformas con scraping implementado (por nombre, en minúsculas). */
    public static final Set<String> SOPORTADAS = Set.of("youtube");

    private static final String BASE_YOUTUBE = "https://www.youtube.com/";
    private static final int MAX_EVIDENCIAS = 3;
    private static final int MAX_TEXTO = 300;
    /** Una ejecución "en_proceso" más antigua que esto se considera interrumpida. */
    private static final int MINUTOS_EN_CURSO = 30;

    private final DPlataforma plataforma = new DPlataforma();
    private final DTermino termino = new DTermino();
    private final DRedSocial redSocial = new DRedSocial();
    private final DPagina pagina = new DPagina();
    private final DEjecucion ejecucion = new DEjecucion();
    private final DUsuario usuario = new DUsuario();
    private final DYoutube youtube = new DYoutube();

    /** Historial de ejecuciones (idPlataforma null = todas). */
    public List<String[]> listarEjecuciones(String idPlataforma) throws NegocioException {
        try {
            Integer id = idPlataforma == null ? null : Validar.id(idPlataforma, "id_plataforma");
            return ejecucion.listar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Ejecución manual sobre una plataforma: idTermino null = todos los términos activos; si no, solo ese término. */
    public ResultadoScraping ejecutar(String idPlataforma, String idTermino) throws NegocioException {
        try {
            int id = Validar.id(idPlataforma, "id_plataforma");
            List<String[]> terminos = terminosAEjecutar(idTermino);
            String[] p = plataforma.obtener(id);
            if (p == null) {
                throw new NegocioException("No existe una plataforma con ID " + id + ".");
            }
            if (!SOPORTADAS.contains(p[1].toLowerCase(Locale.ROOT))) {
                throw new NegocioException("El scraping todavía no está disponible para " + p[1] + "; por ahora solo para YouTube.");
            }
            return ejecutarEn(id, p[1], terminos);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Ejecución periódica: todas las plataformas registradas que tienen scraping implementado. */
    public List<ResultadoScraping> ejecutarProgramadas() throws NegocioException {
        try {
            List<ResultadoScraping> resultados = new ArrayList<>();
            for (String[] p : plataforma.listar(null)) {
                if (SOPORTADAS.contains(p[1].toLowerCase(Locale.ROOT))) {
                    resultados.add(ejecutarEn(Integer.parseInt(p[0]), p[1], terminosAEjecutar(null)));
                }
            }
            return resultados;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Términos a buscar: todos los activos (idTermino null) o solo el indicado, que debe estar activo. Fila: id, termino. */
    private List<String[]> terminosAEjecutar(String idTermino) throws SQLException, NegocioException {
        if (idTermino == null) {
            List<String[]> activos = termino.activos();
            if (activos.isEmpty()) {
                throw new NegocioException("No hay términos de monitoreo activos; registre al menos uno antes de ejecutar el scraping.");
            }
            return activos;
        }
        int id = Validar.id(idTermino, "id_termino");
        String[] t = termino.obtener(id);
        if (t == null) {
            throw new NegocioException("No existe un término de monitoreo con ID " + id + ".");
        }
        if (!"t".equals(t[3])) {
            throw new NegocioException("El término '" + t[1] + "' está desactivado; reactívelo antes de buscarlo.");
        }
        List<String[]> uno = new ArrayList<>();
        uno.add(new String[]{t[0], t[1]});
        return uno;
    }

    private ResultadoScraping ejecutarEn(int idPlataforma, String nombrePlataforma, List<String[]> terminos)
            throws SQLException, NegocioException {
        for (String[] enCurso : ejecucion.listarEnEstado(idPlataforma, EN_PROCESO)) {
            if (Double.parseDouble(enCurso[1]) < MINUTOS_EN_CURSO) {
                throw new NegocioException("Ya hay una ejecución de scraping en curso para " + nombrePlataforma + " (ejecución "
                        + enCurso[0] + "); espere a que termine.");
            }
        }
        for (String[] enCurso : ejecucion.listarEnEstado(idPlataforma, EN_PROCESO)) {
            ejecucion.finalizar(Integer.parseInt(enCurso[0]), ERROR, "Interrumpida: no terminó en " + MINUTOS_EN_CURSO + " minutos.");
        }

        int idEjecucion = ejecucion.iniciar(idPlataforma, EN_PROCESO);
        try {
            return procesar(idEjecucion, idPlataforma, nombrePlataforma, terminos);
        } catch (SQLException | RuntimeException e) {
            try {
                ejecucion.finalizar(idEjecucion, ERROR, "Falla al procesar los resultados: " + e.getMessage());
            } catch (SQLException ignorada) {
                // la base de datos no responde: la ejecución se cerrará como interrumpida en la próxima corrida
            }
            throw e;
        }
    }

    private ResultadoScraping procesar(int idEjecucion, int idPlataforma, String nombrePlataforma, List<String[]> terminos)
            throws SQLException {
        // 1. Búsqueda en la plataforma (fuente externa)
        List<String> errores = new ArrayList<>();
        Map<String, Cuenta> cuentas = new LinkedHashMap<>();
        for (String[] t : terminos) {
            try {
                for (String[] r : youtube.buscar(t[1])) {
                    agregarCoincidencia(cuentas, r, t);
                }
            } catch (IOException e) {
                errores.add("Término '" + t[1] + "': " + e.getMessage());
            }
        }

        // 2. Comparación en memoria con las redes oficiales y las páginas ya registradas (pocas consultas)
        Set<String> oficiales = new HashSet<>();
        for (String url : redSocial.urlsOficiales(idPlataforma)) {
            oficiales.add(normalizarUrl(url));
        }
        Map<String, String[]> registradas = new HashMap<>();
        for (String[] f : pagina.indice(idPlataforma)) {
            registradas.put(normalizarUrl(f[1]), new String[]{f[0], f[2]});
        }
        List<String[]> administradores = usuario.activosDeRol(Config.get("alertas.rol"));

        int oficialesEncontradas = 0;
        int nuevas = 0;
        int yaRegistradas = 0;
        List<String> paginasNuevas = new ArrayList<>();
        List<DPagina.Registro> registros = new ArrayList<>();
        for (Cuenta c : cuentas.values()) {
            if (esOficial(c, oficiales)) {
                oficialesEncontradas++;
                continue;
            }
            String[] existente = null;
            for (String u : c.urls()) {
                if (existente == null) {
                    existente = registradas.get(normalizarUrl(u));
                }
            }
            boolean reabre = existente != null && NPagina.ELIMINADA.equals(existente[1]);
            if (existente == null || reabre) {
                String titulo = (reabre ? "Reapareció una página no autorizada en " : "Página no autorizada detectada en ")
                        + nombrePlataforma;
                String mensaje = "La cuenta '" + c.nombre + "' (" + c.url() + ") usa los términos " + c.terminos()
                        + " y no es una red oficial registrada. Evidencia: " + c.evidencias.get(0);
                List<String[]> avisos = new ArrayList<>();
                for (String[] a : administradores) {
                    avisos.add(new String[]{a[0], titulo, mensaje});
                }
                registros.add(existente == null
                        ? DPagina.Registro.nueva(c.url(), c.nombre, NPagina.DETECTADA, c.detecciones(), avisos)
                        : DPagina.Registro.existente(Integer.parseInt(existente[0]), NPagina.DETECTADA, c.detecciones(), avisos));
                nuevas++;
                paginasNuevas.add("• " + c.nombre + " — " + c.url());
            } else {
                registros.add(DPagina.Registro.existente(Integer.parseInt(existente[0]), null, c.detecciones(),
                        new ArrayList<>()));
                yaRegistradas++;
            }
        }

        // 3. Todos los resultados en una sola transacción y cierre de la ejecución
        pagina.registrarLote(idPlataforma, idEjecucion, registros, NPauta.MEDIO);
        String estado = errores.size() == terminos.size() ? ERROR : COMPLETADA;
        String detalleErrores = errores.isEmpty() ? null : String.join(" | ", errores);
        ejecucion.finalizar(idEjecucion, estado, detalleErrores);

        // Resumen por correo: uno por administrador y por ejecución (las alertas individuales ya están en el sistema)
        List<String[]> resumen = new ArrayList<>();
        if (nuevas > 0) {
            String titulo = "Monitoreo " + nombrePlataforma + ": " + nuevas + " página(s) no autorizada(s) nueva(s)";
            String mensaje = "La ejecución " + idEjecucion + " encontró " + nuevas + " cuenta(s) que usan los términos de"
                    + " monitoreo y no son redes oficiales: " + String.join(" ", paginasNuevas);
            for (String[] a : administradores) {
                resumen.add(new String[]{a[2], a[1], titulo, mensaje});
            }
        }
        List<String> buscados = new ArrayList<>();
        for (String[] t : terminos) {
            buscados.add(t[1]);
        }
        return new ResultadoScraping(idEjecucion, nombrePlataforma, estado, String.join(", ", buscados), cuentas.size(),
                oficialesEncontradas, nuevas,
                yaRegistradas, detalleErrores, resumen);
    }

    // ===== Reglas =====

    /** Cuenta (canal) encontrada y sus coincidencias, agrupadas por término. */
    private static class Cuenta {
        String channelId;
        String handle;
        String nombre;
        final Map<String, String> idTerminos = new LinkedHashMap<>();       // término → id_termino
        final Map<String, List<String>> porTermino = new LinkedHashMap<>(); // término → evidencias
        final List<String> evidencias = new ArrayList<>();

        String url() {
            return handle != null ? BASE_YOUTUBE + handle.substring(1) : BASE_YOUTUBE + "channel/" + channelId;
        }

        List<String> urls() {
            List<String> u = new ArrayList<>();
            if (handle != null) {
                u.add(BASE_YOUTUBE + handle.substring(1));
            }
            u.add(BASE_YOUTUBE + "channel/" + channelId);
            return u;
        }

        String terminos() {
            return "'" + String.join("', '", porTermino.keySet()) + "'";
        }

        /** {id_termino, evidencia} por cada término que la cuenta contiene. */
        List<String[]> detecciones() {
            List<String[]> d = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : porTermino.entrySet()) {
                d.add(new String[]{idTerminos.get(e.getKey()), String.join("\n", e.getValue())});
            }
            return d;
        }
    }

    /** Agrega el resultado si contiene el término. r: {tipo, channel_id, handle, nombre, texto, url_video}. */
    private static void agregarCoincidencia(Map<String, Cuenta> cuentas, String[] r, String[] t) {
        if (r[1] == null) {
            return;
        }
        String buscado = normalizar(t[1]);
        String nombre = r[3] == null ? "" : r[3];
        String texto = r[4] == null ? "" : r[4];
        if (!normalizar(nombre + " " + texto).contains(buscado)) {
            return;
        }
        Cuenta c = cuentas.computeIfAbsent(r[1], k -> new Cuenta());
        c.channelId = r[1];
        if (r[2] != null && r[2].startsWith("/@")) {
            c.handle = r[2];
        }
        c.nombre = nombre;
        c.idTerminos.put(t[1], t[0]);
        List<String> ev = c.porTermino.computeIfAbsent(t[1], k -> new ArrayList<>());
        if (ev.size() < MAX_EVIDENCIAS) {
            String e = r[0].equals("canal")
                    ? "Canal '" + nombre + "'" + (texto.isEmpty() ? "" : ": " + recortar(texto))
                    : "Video '" + recortar(texto) + "' (" + r[5] + ")";
            ev.add(e);
            c.evidencias.add(e);
        }
    }

    private static boolean esOficial(Cuenta c, Set<String> oficiales) {
        for (String u : c.urls()) {
            if (oficiales.contains(normalizarUrl(u))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Forma comparable de una URL de YouTube: sin protocolo, www/m, parámetros ni barra final, y solo la parte que
     * identifica al canal (youtube.com/@handle, youtube.com/channel/ID, /c/nombre o /user/nombre).
     */
    static String normalizarUrl(String url) {
        String u = url.trim().toLowerCase(Locale.ROOT);
        u = u.replaceFirst("^https?://", "").replaceFirst("^(www\\.|m\\.)", "");
        u = u.replaceAll("[?#].*$", "");
        String[] partes = u.split("/");
        if (partes.length >= 3 && (partes[1].equals("channel") || partes[1].equals("c") || partes[1].equals("user"))) {
            return partes[0] + "/" + partes[1] + "/" + partes[2];
        }
        if (partes.length >= 2) {
            return partes[0] + "/" + partes[1];
        }
        return u;
    }

    /** Minúsculas y sin tildes, para comparar términos. */
    static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String recortar(String s) {
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > MAX_TEXTO ? t.substring(0, MAX_TEXTO) + "…" : t;
    }
}
