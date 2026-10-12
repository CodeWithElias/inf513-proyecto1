package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/** Capa de datos de la tabla usuario. Nunca devuelve password_hash. */
public class DUsuario {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_completo, u.email, u.telefono, r.nombre AS rol,"
            + " uu.nombre AS unidad, CASE WHEN u.activo THEN 'Sí' ELSE 'No' END AS activo,"
            + " to_char(u.fecha_registro, 'DD/MM/YYYY HH24:MI') AS fecha_registro,"
            + " to_char(u.ultimo_acceso, 'DD/MM/YYYY HH24:MI') AS ultimo_acceso"
            + " FROM usuario u"
            + " JOIN rol r ON r.id_rol = u.id_rol"
            + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = u.id_unidad";

    /** filtro null = todos. */
    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE u.nombre_completo ILIKE ? OR u.email ILIKE ? OR r.nombre ILIKE ?")
                + " ORDER BY u.id_usuario";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                String like = "%" + filtro + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    /** Devuelve la fila del usuario o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE u.id_usuario = ?")) {
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
             PreparedStatement ps = c.prepareStatement("SELECT activo FROM usuario WHERE id_usuario = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    /** ¿Otro usuario (distinto de excluirId) ya usa este email? */
    public boolean emailEnUso(String email, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM usuario WHERE lower(email) = lower(?) AND id_usuario <> ?")) {
            ps.setString(1, email);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Devuelve el id generado. */
    public int insertar(String nombre, String email, String telefono, String passwordHash,
                        int idRol, Integer idUnidad) throws SQLException {
        String sql = "INSERT INTO usuario (nombre_completo, email, telefono, password_hash, id_rol, id_unidad)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, email);
            setTextoONull(ps, 3, telefono);
            ps.setString(4, passwordHash);
            ps.setInt(5, idRol);
            setEnteroONull(ps, 6, idUnidad);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** passwordHash null = no se cambia la contraseña. */
    public int modificar(int id, String nombre, String email, String telefono, String passwordHash,
                         int idRol, Integer idUnidad) throws SQLException {
        String sql = "UPDATE usuario SET nombre_completo = ?, email = ?, telefono = ?, id_rol = ?, id_unidad = ?"
                + (passwordHash == null ? "" : ", password_hash = ?")
                + " WHERE id_usuario = ?";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, nombre);
            ps.setString(i++, email);
            setTextoONull(ps, i++, telefono);
            ps.setInt(i++, idRol);
            setEnteroONull(ps, i++, idUnidad);
            if (passwordHash != null) {
                ps.setString(i++, passwordHash);
            }
            ps.setInt(i, id);
            return ps.executeUpdate();
        }
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("UPDATE usuario SET activo = ? WHERE id_usuario = ?")) {
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

    private static void setEnteroONull(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }
}
