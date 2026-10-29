package grupo13.negocio;

import grupo13.datos.DCampania;
import grupo13.datos.DMaterial;
import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * CU4 / RF08 y RF09: carga y validación de material publicitario.
 * Reglas:
 *  - Solo un representante (activo) de la empresa de la campaña carga material, y solo en campañas activas o pausadas.
 *    Quien carga y quien revisa es el remitente del correo (RF02). Un cliente solo accede al material de su empresa.
 *  - Tipos: video, audio, imagen, banner, texto. El formato se toma de la extensión del archivo y debe corresponder al tipo.
 *  - Video y audio exigen duración, que no puede superar la duración máxima de las tarifas contratadas en la campaña.
 *  - El tipo texto exige el texto de la publicación.
 *  - Estados: pendiente → aprobado | rechazado | correccion; correccion → (nueva versión) pendiente; cualquiera → retirado.
 *  - Revisa personal de Canal 11: usuario activo que no sea representante de una empresa. Rechazo y corrección
 *    exigen observaciones. Cada revisión queda registrada.
 * Fila de material: id_material, campania, razon_social, tipo, nombre_archivo, formato, url_archivo, duracion_seg,
 *   version, estado, fecha_carga, subido_por, texto_publicacion, id_campania.
 * Fila de revisión: id_revision, fecha, resultado, observaciones, revisor.
 */
public class NMaterial {

    public static final String PENDIENTE = "pendiente";
    public static final String APROBADO = "aprobado";
    public static final String RECHAZADO = "rechazado";
    public static final String CORRECCION = "correccion";
    public static final String RETIRADO = "retirado";

    /** Formatos (extensiones) permitidos por tipo de material. */
    public static final Map<String, Set<String>> FORMATOS = Map.of(
            "video", Set.of("mp4", "mov", "avi", "mkv"),
            "audio", Set.of("mp3", "wav", "aac", "ogg"),
            "imagen", Set.of("jpg", "jpeg", "png", "gif", "webp"),
            "banner", Set.of("jpg", "jpeg", "png", "gif", "webp"),
            "texto", Set.of("txt", "pdf", "docx"));

    private static final Set<String> CON_DURACION = Set.of("video", "audio");
    private static final Set<String> CAMPANIA_ABIERTA = Set.of("activa", "pausada");
    private static final String ARCHIVO_TEXTO = "publicacion.txt";
    private static final int TEXTO_MAX = 2000;

    // Índices de la fila de material
    private static final int TIPO = 3;
    private static final int VERSION = 8;
    private static final int ESTADO = 9;
    private static final int ID_CAMPANIA = 13;
    private static final int ID_EMPRESA = 14;

    private final DMaterial datos = new DMaterial();
    private final DCampania campania = new DCampania();
    private final DUsuario usuario = new DUsuario();
    private final DRepresentante representante = new DRepresentante();

    /** Busca por archivo, tipo, estado, campaña o empresa. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            List<String[]> filas = new ArrayList<>();
            for (String[] f : datos.listar(filtro)) {
                if (Sesion.esDeSuEmpresa(f[ID_EMPRESA])) {
                    filas.add(f);
                }
            }
            return filas;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idMaterial) throws NegocioException {
        try {
            return buscar(Validar.id(idMaterial, "id_material"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public List<String[]> listarRevisiones(String idMaterial) throws NegocioException {
        try {
            int id = Validar.id(idMaterial, "id_material");
            buscar(id);
            return datos.revisiones(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** RF08: el representante (remitente) carga material a una campaña de su empresa. Devuelve el ID generado. */
    public int insertar(String idCampania, String tipo, String nombreArchivo, String url,
                        String duracionSeg, String texto) throws NegocioException {
        try {
            int camp = Validar.id(idCampania, "id_campania");
            int usu = Sesion.usuario().getIdUsuario();
            String t = validarTipo(tipo);

            String[] c = campania.estadoYEmpresa(camp);
            if (c == null) {
                throw new NegocioException("No existe una campaña con ID " + camp + ".");
            }
            if (!CAMPANIA_ABIERTA.contains(c[0])) {
                throw new NegocioException("La campaña " + camp + " está " + c[0] + "; solo se carga material en campañas activas o pausadas.");
            }
            Boolean activo = usuario.estaActivo(usu);
            if (activo == null) {
                throw new NegocioException("No existe un usuario con ID " + usu + ".");
            }
            if (!activo) {
                throw new NegocioException("El usuario " + usu + " está inactivo.");
            }
            Integer empresaUsuario = representante.empresaDe(usu);
            if (empresaUsuario == null || !String.valueOf(empresaUsuario).equals(c[1])) {
                throw new NegocioException("Usted no es representante de la empresa que contrató la campaña "
                        + camp + "; solo sus representantes pueden cargar material.");
            }
            Contenido m = validarContenido(t, nombreArchivo, url, duracionSeg, texto, camp);
            return datos.insertar(camp, usu, t, m.nombre, m.url, m.formato, m.duracion, m.texto);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Corrige los datos de un material que todavía está pendiente de revisión (no cambia la versión). */
    public void modificar(String idMaterial, String tipo, String nombreArchivo, String url, String duracionSeg,
                          String texto) throws NegocioException {
        try {
            int id = Validar.id(idMaterial, "id_material");
            String t = validarTipo(tipo);
            String[] f = buscar(id);
            if (!PENDIENTE.equals(f[ESTADO])) {
                throw new NegocioException("El material " + id + " está " + estadoLegible(f[ESTADO])
                        + "; solo se modifica mientras está pendiente de revisión.");
            }
            Contenido m = validarContenido(t, nombreArchivo, url, duracionSeg, texto, Integer.parseInt(f[ID_CAMPANIA]));
            datos.modificar(id, t, m.nombre, m.url, m.formato, m.duracion, m.texto);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Sube una nueva versión de un material al que se le pidieron correcciones. Devuelve el nuevo número de versión. */
    public int subirVersion(String idMaterial, String nombreArchivo, String url, String duracionSeg, String texto)
            throws NegocioException {
        try {
            int id = Validar.id(idMaterial, "id_material");
            String[] f = buscar(id);
            if (!CORRECCION.equals(f[ESTADO])) {
                throw new NegocioException("El material " + id + " está " + estadoLegible(f[ESTADO])
                        + "; solo se sube una nueva versión cuando se solicitaron correcciones.");
            }
            Contenido m = validarContenido(f[TIPO], nombreArchivo, url, duracionSeg, texto, Integer.parseInt(f[ID_CAMPANIA]));
            int version = Integer.parseInt(f[VERSION]) + 1;
            datos.nuevaVersion(id, m.nombre, m.url, m.formato, m.duracion, m.texto, version, PENDIENTE);
            return version;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Retira el material (baja lógica); no se permite si ya está programado en una pauta. */
    public void eliminar(String idMaterial) throws NegocioException {
        try {
            int id = Validar.id(idMaterial, "id_material");
            String[] f = buscar(id);
            if (RETIRADO.equals(f[ESTADO])) {
                throw new NegocioException("El material " + id + " ya fue retirado.");
            }
            int programado = datos.contarProgramaciones(id);
            if (programado > 0) {
                throw new NegocioException("No se puede retirar el material " + id + ": está programado en " + programado
                        + " pauta(s).");
            }
            datos.cambiarEstado(id, RETIRADO);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** RF09: aprobar (observaciones opcionales). */
    public void aprobar(String idMaterial, String observaciones) throws NegocioException {
        revisar(idMaterial, observaciones, APROBADO, false);
    }

    /** RF09: rechazar definitivamente (observaciones obligatorias). */
    public void rechazar(String idMaterial, String observaciones) throws NegocioException {
        revisar(idMaterial, observaciones, RECHAZADO, true);
    }

    /** RF09: solicitar correcciones (observaciones obligatorias); el cliente luego sube una nueva versión. */
    public void solicitarCorreccion(String idMaterial, String observaciones) throws NegocioException {
        revisar(idMaterial, observaciones, CORRECCION, true);
    }

    /** El revisor es el remitente: personal de Canal 11 (no representante de una empresa). */
    private void revisar(String idMaterial, String observaciones, String resultado,
                         boolean exigeObservaciones) throws NegocioException {
        try {
            int id = Validar.id(idMaterial, "id_material");
            int rev = Sesion.usuario().getIdUsuario();
            String obs = exigeObservaciones
                    ? Validar.texto(observaciones, "observaciones", TEXTO_MAX)
                    : Validar.textoOpcional(observaciones, "observaciones", TEXTO_MAX);
            String[] f = buscar(id);
            if (!PENDIENTE.equals(f[ESTADO])) {
                throw new NegocioException("El material " + id + " está " + estadoLegible(f[ESTADO]) + "; solo se revisa material pendiente.");
            }
            Boolean activo = usuario.estaActivo(rev);
            if (activo == null) {
                throw new NegocioException("No existe un usuario con ID " + rev + ".");
            }
            if (!activo) {
                throw new NegocioException("El usuario " + rev + " está inactivo.");
            }
            if (representante.empresaDe(rev) != null) {
                throw new NegocioException("Usted es representante de una empresa; la revisión la hace el personal de Canal 11.");
            }
            datos.registrarRevision(id, resultado, resultado, obs, rev);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas de contenido =====

    /** Contenido validado de un material. */
    private static class Contenido {
        String nombre;
        String url;
        String formato;
        Integer duracion;
        String texto;
    }

    private static String validarTipo(String tipo) throws NegocioException {
        String t = tipo.toLowerCase();
        if (!FORMATOS.containsKey(t)) {
            throw new NegocioException("Tipo de material inválido '" + tipo + "'. Use: video, audio, imagen, banner o texto.");
        }
        return t;
    }

    private Contenido validarContenido(String tipo, String nombreArchivo, String url, String duracionSeg, String texto,
                                       int idCampania) throws SQLException, NegocioException {
        Contenido m = new Contenido();
        boolean esTexto = tipo.equals("texto");

        m.nombre = esTexto && nombreArchivo.isEmpty() ? ARCHIVO_TEXTO : Validar.texto(nombreArchivo, "nombre_archivo", 255);
        m.formato = extension(m.nombre);
        if (!FORMATOS.get(tipo).contains(m.formato)) {
            throw new NegocioException("El formato '" + m.formato + "' no corresponde a un material de tipo " + tipo
                    + ". Formatos permitidos: " + String.join(", ", new TreeSet<>(FORMATOS.get(tipo))) + ".");
        }

        if (esTexto) {
            m.url = url.isEmpty() ? "" : Validar.url(url, "url_archivo", 500);
            m.texto = Validar.texto(texto, "texto_publicacion", TEXTO_MAX);
        } else {
            m.url = Validar.url(url, "url_archivo", 500);
            m.texto = Validar.textoOpcional(texto, "texto_publicacion", TEXTO_MAX);
        }

        if (CON_DURACION.contains(tipo)) {
            if (duracionSeg.isEmpty()) {
                throw new NegocioException("La duración (en segundos) es obligatoria para material de tipo " + tipo + ".");
            }
            m.duracion = Validar.id(duracionSeg, "duracion_seg");
            Integer maxima = datos.duracionMaximaContratada(idCampania);
            if (maxima != null && m.duracion > maxima) {
                throw new NegocioException("La duración de " + m.duracion + " s supera el máximo contratado en la campaña ("
                        + maxima + " s).");
            }
        } else if (!duracionSeg.isEmpty()) {
            throw new NegocioException("El material de tipo " + tipo + " no lleva duración; deje el campo vacío.");
        }
        return m;
    }

    /** Estado en lenguaje natural para los mensajes ("en corrección" en lugar de "correccion"). */
    private static String estadoLegible(String estado) {
        return CORRECCION.equals(estado) ? "en corrección" : estado;
    }

    /** Extensión del archivo en minúsculas (sin el punto). */
    private static String extension(String nombre) throws NegocioException {
        int punto = nombre.lastIndexOf('.');
        if (punto < 1 || punto == nombre.length() - 1) {
            throw new NegocioException("El nombre de archivo '" + nombre + "' debe incluir su extensión (ej. spot.mp4).");
        }
        return nombre.substring(punto + 1).toLowerCase();
    }

    /** Material existente y accesible: un cliente solo accede al de su empresa. */
    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un material con ID " + id + ".");
        }
        Sesion.exigirEmpresa(fila[ID_EMPRESA], "el material " + id);
        return fila;
    }
}
