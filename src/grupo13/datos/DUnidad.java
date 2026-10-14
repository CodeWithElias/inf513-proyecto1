package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

/** Capa de datos de la tabla unidad_universitaria (estructura jerárquica: facultad → carrera…). */
public class DUnidad {

    private static final String SELECT =
            "SELECT u.id_unidad, u.nombre, u.tipo, p.nombre AS padre,"
            + " CASE WHEN u.activo THEN 'Sí' ELSE 'No' END,"
            + " (SELECT string_agg(e.nombre_completo, ', ' ORDER BY e.nombre_completo) FROM usuario e"
            + "   WHERE e.id_unidad = u.id_unidad AND e.activo) AS encargados"
            + " FROM unidad_universitaria u"
            + " LEFT JOIN unidad_universitaria p ON p.id_unidad = u.id_unidad_padre";

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE u.nombre ILIKE ? OR u.tipo ILIKE ? OR p.nombre ILIKE ?")
                + " ORDER BY u.id_unidad";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, "%" + filtro + "%");
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    /** Devuelve la fila de la unidad o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE u.id_unidad = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    /** Subunidades directas de una unidad. */
    public List<String[]> listarHijas(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE u.id_unidad_padre = ? ORDER BY u.id_unidad")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT activo FROM unidad_universitaria WHERE id_unidad = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    /** ¿La unidad 'posibleDescendiente' está dentro del árbol de 'id' (o es ella misma)? Evita ciclos. */
    public boolean esDescendiente(int posibleDescendiente, int id) throws SQLException {
        String sql = "WITH RECURSIVE arbol AS ("
                + " SELECT id_unidad FROM unidad_universitaria WHERE id_unidad = ?"
                + " UNION SELECT u.id_unidad FROM unidad_universitaria u JOIN arbol a ON u.id_unidad_padre = a.id_unidad)"
                + " SELECT 1 FROM arbol WHERE id_unidad = ?";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, posibleDescendiente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarUsuariosActivos(int id) throws SQLException {
        return contar("SELECT count(*) FROM usuario WHERE id_unidad = ? AND activo", id);
    }

    public int contarHijasActivas(int id) throws SQLException {
        return contar("SELECT count(*) FROM unidad_universitaria WHERE id_unidad_padre = ? AND activo", id);
    }

    public int insertar(String nombre, String tipo, Integer idPadre) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO unidad_universitaria (nombre, tipo, id_unidad_padre) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            setTextoONull(ps, 2, tipo);
            setEnteroONull(ps, 3, idPadre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int modificar(int id, String nombre, String tipo, Integer idPadre) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE unidad_universitaria SET nombre = ?, tipo = ?, id_unidad_padre = ? WHERE id_unidad = ?")) {
            ps.setString(1, nombre);
            setTextoONull(ps, 2, tipo);
            setEnteroONull(ps, 3, idPadre);
            ps.setInt(4, id);
            return ps.executeUpdate();
        }
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("UPDATE unidad_universitaria SET activo = ? WHERE id_unidad = ?")) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    /** id del padre, o null si no tiene. */
    public Integer obtenerPadre(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT id_unidad_padre FROM unidad_universitaria WHERE id_unidad = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int padre = rs.getInt(1);
                return rs.wasNull() ? null : padre;
            }
        }
    }

    private int contar(String sql, int id) throws SQLException {
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
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
