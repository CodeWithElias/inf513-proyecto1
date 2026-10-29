package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de material_publicitario y su composición revision_material.
 * Fila de material: id_material, campania, razon_social, tipo, nombre_archivo, formato, url_archivo, duracion_seg,
 *   version, estado, fecha_carga, subido_por, texto_publicacion, id_campania, id_empresa.
 * Fila de revisión: id_revision, fecha, resultado, observaciones, revisor.
 */
public class DMaterial {

    private static final String SELECT =
            "SELECT m.id_material, c.nombre AS campania, e.razon_social, m.tipo, m.nombre_archivo, m.formato,"
            + " m.url_archivo, m.duracion_seg, m.version, m.estado, m.fecha_carga, u.nombre_completo AS subido_por,"
            + " m.texto_publicacion, m.id_campania, p.id_empresa"
            + " FROM material_publicitario m"
            + " JOIN campania c ON c.id_campania = m.id_campania"
            + " JOIN proforma p ON p.id_proforma = c.id_proforma"
            + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
            + " LEFT JOIN usuario u ON u.id_usuario = m.id_usuario";

    /** Busca por nombre de archivo, tipo, estado, campaña o empresa. */
    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY m.id_material");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE m.nombre_archivo ILIKE ? OR m.tipo ILIKE ? OR m.estado ILIKE ?"
                + " OR c.nombre ILIKE ? OR e.razon_social ILIKE ? ORDER BY m.id_material", like, like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE m.id_material = ?", id);
    }

    /** Historial de revisiones del material (la más reciente primero). */
    public List<String[]> revisiones(int id) throws SQLException {
        return Conexion.consultar("SELECT r.id_revision, r.fecha, r.resultado, r.observaciones, u.nombre_completo"
                + " FROM revision_material r LEFT JOIN usuario u ON u.id_usuario = r.id_usuario"
                + " WHERE r.id_material = ? ORDER BY r.fecha DESC, r.id_revision DESC", id);
    }

    /**
     * Mayor duración (segundos) entre las tarifas contratadas por la campaña: las tarifas directas de su proforma
     * y las tarifas de sus paquetes. null si ninguna define duración.
     */
    public Integer duracionMaximaContratada(int idCampania) throws SQLException {
        String v = Conexion.valor("SELECT max(dur) FROM ("
                + " SELECT t.duracion_seg AS dur FROM campania c"
                + "   JOIN detalle_proforma d ON d.id_proforma = c.id_proforma"
                + "   JOIN tarifa t ON t.id_tarifa = d.id_tarifa WHERE c.id_campania = ?"
                + " UNION ALL"
                + " SELECT t.duracion_seg FROM campania c"
                + "   JOIN detalle_proforma d ON d.id_proforma = c.id_proforma"
                + "   JOIN paquete_item i ON i.id_paquete = d.id_paquete"
                + "   JOIN tarifa t ON t.id_tarifa = i.id_tarifa WHERE c.id_campania = ?) x", idCampania, idCampania);
        return v == null ? null : Integer.parseInt(v);
    }

    /** Veces que el material está usado en la programación de pautas. */
    public int contarProgramaciones(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM programacion_pauta WHERE id_material = ?", id);
    }

    public int insertar(int idCampania, int idUsuario, String tipo, String nombreArchivo, String url, String formato,
                        Integer duracionSeg, String texto) throws SQLException {
        return Conexion.insertar("INSERT INTO material_publicitario (id_campania, id_usuario, tipo, nombre_archivo,"
                        + " url_archivo, formato, duracion_seg, texto_publicacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        + " RETURNING id_material",
                idCampania, idUsuario, tipo, nombreArchivo, Conexion.nulo(url), formato, duracionSeg, Conexion.nulo(texto));
    }

    /** Corrige los datos sin cambiar la versión. */
    public int modificar(int id, String tipo, String nombreArchivo, String url, String formato, Integer duracionSeg,
                         String texto) throws SQLException {
        return Conexion.ejecutar("UPDATE material_publicitario SET tipo = ?, nombre_archivo = ?, url_archivo = ?,"
                        + " formato = ?, duracion_seg = ?, texto_publicacion = ? WHERE id_material = ?",
                tipo, nombreArchivo, Conexion.nulo(url), formato, duracionSeg, Conexion.nulo(texto), id);
    }

    /** Guarda una nueva versión del archivo con el número de versión y estado que decide la capa de negocio. */
    public int nuevaVersion(int id, String nombreArchivo, String url, String formato, Integer duracionSeg, String texto,
                            int version, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE material_publicitario SET nombre_archivo = ?, url_archivo = ?, formato = ?,"
                        + " duracion_seg = ?, texto_publicacion = ?, version = ?, estado = ?, fecha_carga = CURRENT_TIMESTAMP"
                        + " WHERE id_material = ?",
                nombreArchivo, Conexion.nulo(url), formato, duracionSeg, Conexion.nulo(texto), version, estado, id);
    }

    /** Cambia el estado del material y registra la revisión, en una transacción. */
    public void registrarRevision(int id, String estado, String resultado, String observaciones, int idRevisor)
            throws SQLException {
        Conexion.enTransaccion(c -> {
            Conexion.ejecutar(c, "UPDATE material_publicitario SET estado = ? WHERE id_material = ?", estado, id);
            Conexion.ejecutar(c, "INSERT INTO revision_material (id_material, resultado, observaciones, id_usuario)"
                    + " VALUES (?, ?, ?, ?)", id, resultado, Conexion.nulo(observaciones), idRevisor);
            return null;
        });
    }

    public int cambiarEstado(int id, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE material_publicitario SET estado = ? WHERE id_material = ?", estado, id);
    }
}
