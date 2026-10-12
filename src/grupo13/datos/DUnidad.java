package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Capa de datos de la tabla unidad_universitaria (solo consulta, para usuario). */
public class DUnidad {

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = "SELECT u.id_unidad, u.nombre, u.tipo, p.nombre AS padre,"
                + " CASE WHEN u.activo THEN 'Sí' ELSE 'No' END"
                + " FROM unidad_universitaria u"
                + " LEFT JOIN unidad_universitaria p ON p.id_unidad = u.id_unidad_padre"
                + (filtro == null ? "" : " WHERE u.nombre ILIKE ? OR u.tipo ILIKE ?")
                + " ORDER BY u.id_unidad";
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
}
