package grupo13.datos;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Capa de datos de programacion_pauta. Las operaciones que tocan el espacio y las notificaciones van en una
 * transacción; los estados y el contenido de los avisos los decide la capa de negocio.
 * Fila: id_programacion, campania, empresa, canal, tipo_canal, fecha_programacion, archivo, tipo_material, estado,
 *   url_publicacion, programado_por, id_espacio, id_detalle, id_material, id_canal.
 */
public class DPauta {

    private static final String SELECT =
            "SELECT pp.id_programacion, cam.nombre AS campania, e.razon_social, c.nombre AS canal, c.tipo,"
            + " pp.fecha_programacion, m.nombre_archivo, m.tipo, pp.estado, pp.url_publicacion,"
            + " u.nombre_completo AS programado_por, pp.id_espacio, pp.id_detalle, pp.id_material, esp.id_canal"
            + " FROM programacion_pauta pp"
            + " JOIN material_publicitario m ON m.id_material = pp.id_material"
            + " JOIN campania cam ON cam.id_campania = m.id_campania"
            + " JOIN proforma p ON p.id_proforma = cam.id_proforma"
            + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
            + " LEFT JOIN espacio_publicitario esp ON esp.id_espacio = pp.id_espacio"
            + " LEFT JOIN canal_difusion c ON c.id_canal = esp.id_canal"
            + " LEFT JOIN usuario u ON u.id_usuario = pp.id_usuario";

    /** Busca por campaña, empresa, canal o estado. */
    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY pp.fecha_programacion, pp.id_programacion");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE cam.nombre ILIKE ? OR e.razon_social ILIKE ? OR c.nombre ILIKE ?"
                + " OR pp.estado ILIKE ? ORDER BY pp.fecha_programacion, pp.id_programacion", like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE pp.id_programacion = ?", id);
    }

    /** Programaciones de un ítem contratado que no están en el estado indicado (p. ej. no canceladas). */
    public int contarPorDetalleExcepto(int idDetalle, String estadoExcluido) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM programacion_pauta WHERE id_detalle = ? AND estado <> ?",
                idDetalle, estadoExcluido);
    }

    /** Registra la programación, ocupa el espacio y guarda los avisos ({id_usuario, titulo, mensaje}). */
    public int programar(int idDetalle, int idMaterial, int idEspacio, int idUsuario, LocalDateTime fecha,
                         String estadoEspacio, String medio, List<String[]> avisos) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int id = Conexion.insertar(c, "INSERT INTO programacion_pauta (id_detalle, id_material, id_espacio, id_usuario,"
                    + " fecha_programacion) VALUES (?, ?, ?, ?, ?) RETURNING id_programacion",
                    idDetalle, idMaterial, idEspacio, idUsuario, fecha);
            Conexion.ejecutar(c, "UPDATE espacio_publicitario SET estado = ? WHERE id_espacio = ?", estadoEspacio, idEspacio);
            DNotificacion.insertar(c, medio, id, avisos);
            return id;
        });
    }

    /** Mueve la programación a otro espacio: libera el anterior, ocupa el nuevo y guarda los avisos. */
    public void reprogramar(int id, int idEspacioAnterior, int idEspacioNuevo, LocalDateTime fecha,
                            String estadoLibre, String estadoOcupado, String medio, List<String[]> avisos)
            throws SQLException {
        Conexion.enTransaccion(c -> {
            Conexion.ejecutar(c, "UPDATE programacion_pauta SET id_espacio = ?, fecha_programacion = ? WHERE id_programacion = ?",
                    idEspacioNuevo, fecha, id);
            Conexion.ejecutar(c, "UPDATE espacio_publicitario SET estado = ? WHERE id_espacio = ?", estadoLibre, idEspacioAnterior);
            Conexion.ejecutar(c, "UPDATE espacio_publicitario SET estado = ? WHERE id_espacio = ?", estadoOcupado, idEspacioNuevo);
            DNotificacion.insertar(c, medio, id, avisos);
            return null;
        });
    }

    /** Cancela: cambia el estado, libera el espacio (id_espacio es UNIQUE) y guarda los avisos. */
    public void cancelar(int id, int idEspacio, String estado, String estadoLibre, String medio, List<String[]> avisos)
            throws SQLException {
        Conexion.enTransaccion(c -> {
            Conexion.ejecutar(c, "UPDATE programacion_pauta SET estado = ?, id_espacio = NULL WHERE id_programacion = ?",
                    estado, id);
            Conexion.ejecutar(c, "UPDATE espacio_publicitario SET estado = ? WHERE id_espacio = ?", estadoLibre, idEspacio);
            DNotificacion.insertar(c, medio, id, avisos);
            return null;
        });
    }

    public int publicar(int id, String estado, String url) throws SQLException {
        return Conexion.ejecutar("UPDATE programacion_pauta SET estado = ?, url_publicacion = ? WHERE id_programacion = ?",
                estado, Conexion.nulo(url), id);
    }
}
