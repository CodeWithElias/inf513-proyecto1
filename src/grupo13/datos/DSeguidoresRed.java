package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos para la tabla seguidores_red (historial y registro de seguidores por red social).
 * Fila: id_seguidores, red_social, fecha, seguidores.
 */
public class DSeguidoresRed {

    private static final String SELECT =
            "SELECT s.id_seguidores, c.nombre AS red_social, s.fecha, s.seguidores"
            + " FROM seguidores_red s"
            + " JOIN red_social r ON r.id_canal = s.id_canal"
            + " JOIN canal_difusion c ON c.id_canal = r.id_canal";

    public List<String[]> listarPorCanal(int idCanal) throws SQLException {
        return Conexion.consultar(SELECT + " WHERE s.id_canal = ? ORDER BY s.fecha DESC, s.id_seguidores DESC", idCanal);
    }

    public String[] obtenerUltimo(int idCanal) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE s.id_canal = ? ORDER BY s.fecha DESC, s.id_seguidores DESC LIMIT 1",
                idCanal);
    }

    public int insertar(int idCanal, int seguidores) throws SQLException {
        return Conexion.insertar("INSERT INTO seguidores_red (id_canal, seguidores, fecha) VALUES (?, ?, CURRENT_DATE)"
                + " RETURNING id_seguidores", idCanal, seguidores);
    }

    public int eliminar(int idSeguidores) throws SQLException {
        return Conexion.ejecutar("DELETE FROM seguidores_red WHERE id_seguidores = ?", idSeguidores);
    }
}
