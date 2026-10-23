package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de metrica_publicacion (mediciones acumuladas de una pauta publicada).
 * Fila de medición: id_metrica, fecha_medicion, alcance, impresiones, interacciones, clics, compartidos, id_programacion.
 * Fila "última medición por pauta": id_programacion, canal, tipo_canal, fecha_programacion, archivo, alcance, impresiones,
 *   interacciones, clics, compartidos, fecha_medicion (las métricas son NULL si la pauta aún no tiene mediciones).
 */
public class DMetrica {

    private static final String SELECT =
            "SELECT id_metrica, fecha_medicion, alcance, impresiones, interacciones, clics, compartidos, id_programacion"
            + " FROM metrica_publicacion";

    /** Última medición de cada pauta publicada (LATERAL toma la más reciente de cada una). */
    private static final String ULTIMAS =
            "SELECT pp.id_programacion, c.nombre, c.tipo, pp.fecha_programacion, m.nombre_archivo,"
            + " lm.alcance, lm.impresiones, lm.interacciones, lm.clics, lm.compartidos, lm.fecha_medicion"
            + " FROM programacion_pauta pp"
            + " JOIN material_publicitario m ON m.id_material = pp.id_material"
            + " JOIN espacio_publicitario e ON e.id_espacio = pp.id_espacio"
            + " JOIN canal_difusion c ON c.id_canal = e.id_canal"
            + " LEFT JOIN LATERAL (SELECT * FROM metrica_publicacion mp WHERE mp.id_programacion = pp.id_programacion"
            + "   ORDER BY mp.fecha_medicion DESC, mp.id_metrica DESC LIMIT 1) lm ON TRUE"
            + " WHERE pp.estado = ?";

    /** Historial de una pauta, la más reciente primero. */
    public List<String[]> listarPorPauta(int idProgramacion) throws SQLException {
        return Conexion.consultar(SELECT + " WHERE id_programacion = ? ORDER BY fecha_medicion DESC, id_metrica DESC",
                idProgramacion);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE id_metrica = ?", id);
    }

    /** Las dos mediciones más recientes de la pauta (0, 1 o 2 filas; la primera es la última). */
    public List<String[]> ultimasDos(int idProgramacion) throws SQLException {
        return Conexion.consultar(SELECT + " WHERE id_programacion = ? ORDER BY fecha_medicion DESC, id_metrica DESC LIMIT 2",
                idProgramacion);
    }

    /** Última medición de cada pauta (en el estado indicado) de una campaña. */
    public List<String[]> ultimasPorCampania(int idCampania, String estadoPauta) throws SQLException {
        return Conexion.consultar(ULTIMAS + " AND m.id_campania = ? ORDER BY pp.fecha_programacion", estadoPauta, idCampania);
    }

    /** Última medición de cada pauta (en el estado indicado) emitida en un canal. */
    public List<String[]> ultimasPorCanal(int idCanal, String estadoPauta) throws SQLException {
        return Conexion.consultar(ULTIMAS + " AND c.id_canal = ? ORDER BY pp.fecha_programacion", estadoPauta, idCanal);
    }

    /** Pautas en el estado indicado publicadas en redes sociales: id_programacion, url_publicacion, plataforma. */
    public List<String[]> pautasEnRedes(String estadoPauta) throws SQLException {
        return Conexion.consultar("SELECT pp.id_programacion, pp.url_publicacion, pl.nombre FROM programacion_pauta pp"
                + " JOIN espacio_publicitario e ON e.id_espacio = pp.id_espacio"
                + " JOIN red_social r ON r.id_canal = e.id_canal"
                + " JOIN plataforma pl ON pl.id_plataforma = r.id_plataforma"
                + " WHERE pp.estado = ? ORDER BY pp.id_programacion", estadoPauta);
    }

    public int insertar(int idProgramacion, int alcance, int impresiones, int interacciones, int clics, int compartidos)
            throws SQLException {
        return Conexion.insertar("INSERT INTO metrica_publicacion (id_programacion, alcance, impresiones, interacciones, clics,"
                + " compartidos) VALUES (?, ?, ?, ?, ?, ?) RETURNING id_metrica",
                idProgramacion, alcance, impresiones, interacciones, clics, compartidos);
    }

    public int modificar(int id, int alcance, int impresiones, int interacciones, int clics, int compartidos)
            throws SQLException {
        return Conexion.ejecutar("UPDATE metrica_publicacion SET alcance = ?, impresiones = ?, interacciones = ?, clics = ?,"
                + " compartidos = ? WHERE id_metrica = ?", alcance, impresiones, interacciones, clics, compartidos, id);
    }

    /** Borrado físico de una medición registrada por error (tabla hoja). */
    public int eliminar(int id) throws SQLException {
        return Conexion.ejecutar("DELETE FROM metrica_publicacion WHERE id_metrica = ?", id);
    }
}
