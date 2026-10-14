package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/** Capa de datos de la tabla rol. */
public class DRol {

    private static final String SELECT =
            "SELECT r.id_rol, r.nombre, r.descripcion, CASE WHEN r.activo THEN 'Sí' ELSE 'No' END,"
            + " (SELECT count(*) FROM usuario u WHERE u.id_rol = r.id_rol AND u.activo) AS usuarios_activos"
            + " FROM rol r";

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE r.nombre ILIKE ? OR r.descripcion ILIKE ?")
                + " ORDER BY r.id_rol";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                ps.setString(1, "%" + filtro + "%");
                ps.setString(2, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    /** Devuelve la fila del rol o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE r.id_rol = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT activo FROM rol WHERE id_rol = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    /** ¿Otro rol (distinto de excluirId) ya usa este nombre? */
    public boolean existeNombre(String nombre, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM rol WHERE lower(nombre) = lower(?) AND id_rol <> ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarUsuariosActivos(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM usuario WHERE id_rol = ? AND activo")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int insertar(String nombre, String descripcion) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("INSERT INTO rol (nombre, descripcion) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            setTextoONull(ps, 2, descripcion);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int modificar(int id, String nombre, String descripcion) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("UPDATE rol SET nombre = ?, descripcion = ? WHERE id_rol = ?")) {
            ps.setString(1, nombre);
            setTextoONull(ps, 2, descripcion);
            ps.setInt(3, id);
            return ps.executeUpdate();
        }
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("UPDATE rol SET activo = ? WHERE id_rol = ?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    private static void setTextoONull(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isEmpty()) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, v);
        }
    }
}
