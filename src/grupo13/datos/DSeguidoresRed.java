package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos para la tabla seguidores_red (historial y registro de seguidores por red social).
 */
public class DSeguidoresRed {

    private static final String SELECT =
            "SELECT s.id_seguidores, c.nombre AS red_social, to_char(s.fecha, 'DD/MM/YYYY') AS fecha, s.seguidores"
            + " FROM seguidores_red s"
            + " JOIN red_social r ON r.id_canal = s.id_canal"
            + " JOIN canal_difusion c ON c.id_canal = r.id_canal";

    public List<String[]> listarPorCanal(int idCanal) throws SQLException {
        String sql = SELECT
                + " WHERE s.id_canal = ?"
                + " ORDER BY s.fecha DESC, s.id_seguidores DESC";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    public String[] obtenerUltimo(int idCanal) throws SQLException {
        String sql = SELECT
                + " WHERE s.id_canal = ?"
                + " ORDER BY s.fecha DESC, s.id_seguidores DESC LIMIT 1";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    public int insertar(int idCanal, int seguidores) throws SQLException {
        String sql = "INSERT INTO seguidores_red (id_canal, seguidores, fecha) VALUES (?, ?, CURRENT_DATE) RETURNING id_seguidores";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            ps.setInt(2, seguidores);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int eliminar(int idSeguidores) throws SQLException {
        String sql = "DELETE FROM seguidores_red WHERE id_seguidores = ?";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idSeguidores);
            return ps.executeUpdate();
        }
    }
}
