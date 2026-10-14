package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Capa de datos de empresa_anunciante (solo consulta; el CRUD completo pertenece al CU3). */
public class DEmpresa {

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = "SELECT e.id_empresa, e.razon_social, e.nit, e.nombre_comercial, e.email_facturacion, e.telefono,"
                + " CASE WHEN e.activo THEN 'Sí' ELSE 'No' END,"
                + " (SELECT count(*) FROM representante r WHERE r.id_empresa = e.id_empresa) AS representantes"
                + " FROM empresa_anunciante e"
                + (filtro == null ? "" : " WHERE e.razon_social ILIKE ? OR e.nit ILIKE ? OR e.nombre_comercial ILIKE ?")
                + " ORDER BY e.id_empresa";
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

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT activo FROM empresa_anunciante WHERE id_empresa = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }
}
