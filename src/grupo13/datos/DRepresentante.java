package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Capa de datos de la tabla representante (cliente).
 * representante ES UN usuario: su PK id_usuario es también FK a usuario.
 */
public class DRepresentante {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_completo, u.email, u.telefono, r.ci, r.cargo,"
            + " e.razon_social, e.nit, CASE WHEN u.activo THEN 'Sí' ELSE 'No' END"
            + " FROM representante r"
            + " JOIN usuario u ON u.id_usuario = r.id_usuario"
            + " JOIN empresa_anunciante e ON e.id_empresa = r.id_empresa";

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE u.nombre_completo ILIKE ? OR u.email ILIKE ? OR r.ci ILIKE ?"
                        + " OR e.razon_social ILIKE ? OR e.nit ILIKE ?")
                + " ORDER BY u.id_usuario";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, "%" + filtro + "%");
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    /** Devuelve la fila del representante o null si el usuario no es representante. */
    public String[] obtener(int idUsuario) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE r.id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    /** ¿Otro representante (distinto de excluirId) ya usa este CI? */
    public boolean ciEnUso(String ci, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM representante WHERE lower(ci) = lower(?) AND id_usuario <> ?")) {
            ps.setString(1, ci);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertar(int idUsuario, String ci, String cargo, int idEmpresa) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO representante (id_usuario, ci, cargo, id_empresa) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, idUsuario);
            ps.setString(2, ci);
            setTextoONull(ps, 3, cargo);
            ps.setInt(4, idEmpresa);
            ps.executeUpdate();
        }
    }

    public int modificar(int idUsuario, String ci, String cargo, int idEmpresa) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE representante SET ci = ?, cargo = ?, id_empresa = ? WHERE id_usuario = ?")) {
            ps.setString(1, ci);
            setTextoONull(ps, 2, cargo);
            ps.setInt(3, idEmpresa);
            ps.setInt(4, idUsuario);
            return ps.executeUpdate();
        }
    }

    /** Quita el perfil de representante; el usuario sigue existiendo. */
    public int eliminar(int idUsuario) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM representante WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
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
