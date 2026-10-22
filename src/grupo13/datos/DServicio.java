package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de servicio_publicitario.
 * Fila: id_servicio, nombre, tipo, descripcion, activo, tarifas_activas.
 */
public class DServicio {

    private static final String SELECT =
            "SELECT s.id_servicio, s.nombre, s.tipo, s.descripcion, s.activo,"
            + " (SELECT count(*) FROM tarifa t WHERE t.id_servicio = s.id_servicio AND t.activo) AS tarifas_activas"
            + " FROM servicio_publicitario s";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY s.id_servicio");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE s.nombre ILIKE ? OR s.tipo ILIKE ? OR s.descripcion ILIKE ?"
                + " ORDER BY s.id_servicio", like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE s.id_servicio = ?", id);
    }

    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("servicio_publicitario", "id_servicio", id);
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM servicio_publicitario WHERE lower(nombre) = lower(?) AND id_servicio <> ?",
                nombre, excluirId) > 0;
    }

    public int insertar(String nombre, String tipo, String descripcion) throws SQLException {
        return Conexion.insertar("INSERT INTO servicio_publicitario (nombre, tipo, descripcion) VALUES (?, ?, ?)"
                + " RETURNING id_servicio", nombre, Conexion.nulo(tipo), Conexion.nulo(descripcion));
    }

    public int modificar(int id, String nombre, String tipo, String descripcion) throws SQLException {
        return Conexion.ejecutar("UPDATE servicio_publicitario SET nombre = ?, tipo = ?, descripcion = ? WHERE id_servicio = ?",
                nombre, Conexion.nulo(tipo), Conexion.nulo(descripcion), id);
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE servicio_publicitario SET activo = ? WHERE id_servicio = ?", activo, id);
    }

    public int contarTarifasActivas(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM tarifa WHERE id_servicio = ? AND activo", id);
    }
}
