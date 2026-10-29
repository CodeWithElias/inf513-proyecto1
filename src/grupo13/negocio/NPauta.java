package grupo13.negocio;

import grupo13.datos.DCampania;
import grupo13.datos.DCanal;
import grupo13.datos.DEspacio;
import grupo13.datos.DMaterial;
import grupo13.datos.DPauta;
import grupo13.datos.DProforma;
import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU5 / RF10 y RF11: programación de la pauta multicanal y notificación a las unidades.
 * Reglas para programar (y reprogramar):
 *  - Programa el personal de Canal 11 (usuario activo que no es representante de una empresa).
 *  - El material debe estar aprobado (CU4) y su campaña activa.
 *  - El ítem contratado (detalle de proforma) debe pertenecer a la proforma de esa campaña, y no se puede programar
 *    más veces de lo contratado: cantidad del ítem (tarifa) o cantidad × tarifas del paquete.
 *  - El espacio debe estar disponible, no haber pasado y caer dentro de las fechas de la campaña.
 *  - El canal debe estar activo y aceptar el tipo de material; si la tarifa es de una unidad, el canal debe ser una
 *    red social de esa unidad.
 *  - RF11: si el canal es una red social, se avisa a su responsable (o, si no tiene, a los usuarios activos de su
 *    unidad). El aviso queda registrado en la misma transacción.
 * Estados: programada → publicada | cancelada.
 * Fila: id_programacion, campania, empresa, canal, tipo_canal, fecha_programacion, archivo, tipo_material, estado,
 *   url_publicacion, programado_por, id_espacio, id_detalle, id_material, id_canal.
 */
public class NPauta {

    public static final String PROGRAMADA = "programada";
    public static final String PUBLICADA = "publicada";
    public static final String CANCELADA = "cancelada";

    /** Medio con el que se registra el aviso (además queda en el sistema). */
    public static final String MEDIO = "correo";

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Índices de las filas
    private static final int PP_ESTADO = 8;
    private static final int PP_ESPACIO = 11;
    private static final int PP_DETALLE = 12;
    private static final int PP_MATERIAL = 13;
    private static final int PP_FECHA = 5;
    private static final int ESP_FECHA = 3;
    private static final int ESP_INICIO = 4;
    private static final int ESP_ESTADO = 6;
    private static final int ESP_CANAL = 8;

    private final DPauta datos = new DPauta();
    private final DMaterial material = new DMaterial();
    private final DCampania campania = new DCampania();
    private final DProforma proforma = new DProforma();
    private final DEspacio espacio = new DEspacio();
    private final DCanal canal = new DCanal();
    private final DUsuario usuario = new DUsuario();
    private final DRepresentante representante = new DRepresentante();

    /** Busca por campaña, empresa, canal o estado. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            List<String[]> filas = new ArrayList<>();
            for (String[] f : datos.listar(filtro)) {
                if (Acceso.puedeVerPauta(f)) {
                    filas.add(f);
                }
            }
            return filas;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idProgramacion) throws NegocioException {
        try {
            String[] pp = buscar(Validar.id(idProgramacion, "id_programacion"));
            Acceso.exigirPauta(pp);
            return pp;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** RF10: el remitente (personal de Canal 11) programa material aprobado en un espacio libre, con cargo a un ítem contratado. */
    public ResultadoPauta programar(String idDetalle, String idMaterial, String idEspacio)
            throws NegocioException {
        try {
            int det = Validar.id(idDetalle, "id_detalle");
            int mat = Validar.id(idMaterial, "id_material");
            int esp = Validar.id(idEspacio, "id_espacio");
            int usu = Sesion.usuario().getIdUsuario();

            validarPersonal(usu);
            String[] m = material.obtener(mat);
            if (m == null) {
                throw new NegocioException("No existe un material con ID " + mat + ".");
            }
            if (!NMaterial.APROBADO.equals(m[9])) {
                throw new NegocioException("El material " + mat + " está " + m[9] + "; solo se programa material aprobado.");
            }
            String[] cam = campania.datosParaPauta(Integer.parseInt(m[13]));
            if (!"activa".equals(cam[0])) {
                throw new NegocioException("La campaña '" + cam[4] + "' está " + cam[0] + "; solo se programan campañas activas.");
            }
            String[] d = proforma.detalleParaPauta(det);
            if (d == null) {
                throw new NegocioException("No existe un ítem de proforma con ID " + det + ".");
            }
            if (!d[0].equals(cam[1])) {
                throw new NegocioException("El ítem " + det + " no pertenece a la proforma de la campaña '" + cam[4] + "'.");
            }
            int contratadas = Integer.parseInt(d[1]) * (d[2] != null ? 1 : Integer.parseInt(d[5]));
            int usadas = datos.contarPorDetalleExcepto(det, CANCELADA);
            if (usadas >= contratadas) {
                throw new NegocioException("El ítem " + det + " ya tiene " + usadas + " de " + contratadas
                        + " emisión(es) contratada(s) programadas.");
            }
            String[] e = validarEspacio(esp, cam, m[3], d[4]);

            LocalDateTime fecha = fechaDe(e);
            List<String[]> avisos = avisos(Integer.parseInt(e[ESP_CANAL]), "Nueva pauta programada en " + e[1],
                    "Se programó la publicación del material '" + m[4] + "' de la campaña '" + cam[4] + "' ("
                            + m[2] + ") para el " + fecha.format(FORMATO) + " en " + e[1] + ".");
            int id = datos.programar(det, mat, esp, usu, fecha, NEspacio.OCUPADO, MEDIO, soloRegistro(avisos));
            return new ResultadoPauta(id, soloCorreo(avisos));
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    /** Mueve una programación pendiente a otro espacio libre (se avisa al canal anterior y al nuevo). */
    public ResultadoPauta reprogramar(String idProgramacion, String idEspacioNuevo) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            int nuevo = Validar.id(idEspacioNuevo, "id_espacio");
            String[] pp = exigirProgramada(id);
            int anterior = Integer.parseInt(pp[PP_ESPACIO]);
            if (anterior == nuevo) {
                throw new NegocioException("La pauta " + id + " ya está programada en el espacio " + nuevo + ".");
            }
            String[] m = material.obtener(Integer.parseInt(pp[PP_MATERIAL]));
            String[] cam = campania.datosParaPauta(Integer.parseInt(m[13]));
            String[] d = proforma.detalleParaPauta(Integer.parseInt(pp[PP_DETALLE]));
            String[] e = validarEspacio(nuevo, cam, m[3], d[4]);
            String[] eAnterior = espacio.obtener(anterior);

            LocalDateTime fecha = fechaDe(e);
            String mensaje = "La publicación del material '" + m[4] + "' de la campaña '" + cam[4] + "' se movió del "
                    + fechaDe(eAnterior).format(FORMATO) + " en " + eAnterior[1] + " al " + fecha.format(FORMATO)
                    + " en " + e[1] + ".";
            Map<String, String[]> unicos = new LinkedHashMap<>();
            for (String[] a : avisos(Integer.parseInt(eAnterior[ESP_CANAL]), "Pauta reprogramada", mensaje)) {
                unicos.put(a[0], a);
            }
            for (String[] a : avisos(Integer.parseInt(e[ESP_CANAL]), "Pauta reprogramada", mensaje)) {
                unicos.put(a[0], a);
            }
            List<String[]> avisos = new ArrayList<>(unicos.values());
            datos.reprogramar(id, anterior, nuevo, fecha, NEspacio.DISPONIBLE, NEspacio.OCUPADO, MEDIO, soloRegistro(avisos));
            return new ResultadoPauta(id, soloCorreo(avisos));
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    /** Cancela una programación pendiente y libera su espacio. */
    public ResultadoPauta cancelar(String idProgramacion) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            String[] pp = exigirProgramada(id);
            int esp = Integer.parseInt(pp[PP_ESPACIO]);
            String[] e = espacio.obtener(esp);
            List<String[]> avisos = avisos(Integer.parseInt(e[ESP_CANAL]), "Pauta cancelada en " + e[1],
                    "Se canceló la publicación del material '" + pp[6] + "' de la campaña '" + pp[1] + "' prevista para el "
                            + fechaDe(e).format(FORMATO) + " en " + e[1] + ".");
            datos.cancelar(id, esp, CANCELADA, NEspacio.DISPONIBLE, MEDIO, soloRegistro(avisos));
            return new ResultadoPauta(id, soloCorreo(avisos));
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    /** Marca como publicada una pauta cuya hora ya llegó; en redes sociales exige el enlace de la publicación. */
    public void publicar(String idProgramacion, String url) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            String[] pp = exigirProgramada(id);
            LocalDateTime fecha = LocalDateTime.parse(pp[PP_FECHA].replace(' ', 'T'));
            if (fecha.isAfter(LocalDateTime.now())) {
                throw new NegocioException("La pauta " + id + " está programada para el " + fecha.format(FORMATO)
                        + "; aún no puede marcarse como publicada.");
            }
            boolean esRed = NCanal.RED_SOCIAL.equals(pp[4]);
            String u = esRed || !url.isEmpty() ? Validar.url(url, "url_publicacion", 500) : "";
            datos.publicar(id, PUBLICADA, u);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas =====

    /** Espacio libre, futuro, dentro de la campaña y en un canal activo compatible. Devuelve la fila del espacio. */
    private String[] validarEspacio(int idEspacio, String[] cam, String tipoMaterial, String unidadTarifa)
            throws SQLException, NegocioException {
        String[] e = espacio.obtener(idEspacio);
        if (e == null) {
            throw new NegocioException("No existe un espacio con ID " + idEspacio + ".");
        }
        if (!NEspacio.DISPONIBLE.equals(e[ESP_ESTADO])) {
            throw new NegocioException("El espacio " + idEspacio + " está " + e[ESP_ESTADO] + "; elija un espacio disponible.");
        }
        LocalDateTime fecha = fechaDe(e);
        if (!fecha.isAfter(LocalDateTime.now())) {
            throw new NegocioException("El espacio " + idEspacio + " ya pasó (" + fecha.format(FORMATO) + ").");
        }
        LocalDate dia = fecha.toLocalDate();
        if ((cam[2] != null && dia.isBefore(LocalDate.parse(cam[2]))) || (cam[3] != null && dia.isAfter(LocalDate.parse(cam[3])))) {
            throw new NegocioException("El espacio " + idEspacio + " está fuera de las fechas de la campaña '" + cam[4] + "'.");
        }
        int idCanal = Integer.parseInt(e[ESP_CANAL]);
        String[] c = canal.obtener(idCanal);
        if (!"t".equals(c[3])) {
            throw new NegocioException("El canal '" + c[2] + "' está dado de baja.");
        }
        if (!NCanal.MATERIAL_ACEPTADO.getOrDefault(c[1], Set.of()).contains(tipoMaterial)) {
            throw new NegocioException("El canal '" + c[2] + "' (" + c[1] + ") no acepta material de tipo " + tipoMaterial + ".");
        }
        if (unidadTarifa != null) {
            String[] red = canal.datosRedSocial(idCanal);
            if (red == null || !unidadTarifa.equals(red[0])) {
                throw new NegocioException("La tarifa contratada es para las redes de la unidad " + unidadTarifa
                        + "; el canal '" + c[2] + "' no es una red social de esa unidad.");
            }
        }
        return e;
    }

    /**
     * RF11: destinatarios del aviso para un canal. Red social → su responsable activo o, si no tiene, los usuarios
     * activos de su unidad. Otros canales → nadie. Cada aviso: {id_usuario, email, nombre, titulo, mensaje}.
     */
    private List<String[]> avisos(int idCanal, String titulo, String mensaje) throws SQLException {
        List<String[]> avisos = new ArrayList<>();
        String[] red = canal.datosRedSocial(idCanal);
        if (red == null) {
            return avisos;
        }
        if (red[1] != null) {
            String[] u = usuario.obtener(Integer.parseInt(red[1]));
            if (u != null && "t".equals(u[6])) {
                avisos.add(new String[]{u[0], u[2], u[1], titulo, mensaje});
                return avisos;
            }
        }
        if (red[0] != null) {
            for (String[] u : usuario.activosDeUnidad(Integer.parseInt(red[0]))) {
                avisos.add(new String[]{u[0], u[2], u[1], titulo, mensaje});
            }
        }
        return avisos;
    }

    /** {id_usuario, titulo, mensaje} para registrar en notificacion. */
    private static List<String[]> soloRegistro(List<String[]> avisos) {
        List<String[]> r = new ArrayList<>();
        for (String[] a : avisos) {
            r.add(new String[]{a[0], a[3], a[4]});
        }
        return r;
    }

    /** {email, nombre, titulo, mensaje} para que la presentación los envíe. */
    private static List<String[]> soloCorreo(List<String[]> avisos) {
        List<String[]> r = new ArrayList<>();
        for (String[] a : avisos) {
            r.add(new String[]{a[1], a[2], a[3], a[4]});
        }
        return r;
    }

    private static LocalDateTime fechaDe(String[] espacio) {
        return LocalDateTime.of(LocalDate.parse(espacio[ESP_FECHA]), LocalTime.parse(espacio[ESP_INICIO]));
    }

    private void validarPersonal(int idUsuario) throws SQLException, NegocioException {
        Boolean activo = usuario.estaActivo(idUsuario);
        if (activo == null) {
            throw new NegocioException("No existe un usuario con ID " + idUsuario + ".");
        }
        if (!activo) {
            throw new NegocioException("El usuario " + idUsuario + " está inactivo.");
        }
        if (representante.empresaDe(idUsuario) != null) {
            throw new NegocioException("El usuario " + idUsuario + " es representante de una empresa; la pauta la programa el personal de Canal 11.");
        }
    }

    private String[] exigirProgramada(int id) throws SQLException, NegocioException {
        String[] pp = buscar(id);
        if (!PROGRAMADA.equals(pp[PP_ESTADO])) {
            throw new NegocioException("La pauta " + id + " está " + pp[PP_ESTADO] + "; solo se cambian las programadas.");
        }
        return pp;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una pauta programada con ID " + id + ".");
        }
        return fila;
    }
}
