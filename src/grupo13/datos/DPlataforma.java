package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla plataforma (catálogo de redes sociales: Facebook, TikTok, etc.).
 */
public class DPlataforma {

    private static final String SELECT =
            "SELECT p.id_plataforma, p.nombre,"
            + " (SELECT count(*) FROM red_social r JOIN canal_difusion c ON c.id_canal = r.id_canal"
            + "   WHERE r.id_plataforma = p.id_plataforma AND c.activo = TRUE) AS redes_activas"
            + " FROM plataforma p";

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE p.nombre ILIKE ?")
                + " ORDER BY p.id_plataforma";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                ps.setString(1, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    public String[] obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.id_plataforma = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    public boolean existe(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM plataforma WHERE id_plataforma = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM plataforma WHERE lower(nombre) = lower(?) AND id_plataforma <> ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(String nombre) throws SQLException {
        String sql = "INSERT INTO plataforma (nombre) VALUES (?) RETURNING id_plataforma";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Borrado físico (útil para pruebas y limpieza de datos). */
    public int eliminar(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM plataforma WHERE id_plataforma = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }
}
