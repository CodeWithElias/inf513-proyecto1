package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/** Capa de datos de empresa_anunciante (CU3 / RF04). */
public class DEmpresa {

    private static final String SELECT =
            "SELECT e.id_empresa, e.razon_social, e.nit, e.nombre_comercial, e.direccion_facturacion,"
            + " e.email_facturacion, e.telefono, CASE WHEN e.activo THEN 'Sí' ELSE 'No' END,"
            + " to_char(e.fecha_registro, 'DD/MM/YYYY'),"
            + " (SELECT count(*) FROM representante r WHERE r.id_empresa = e.id_empresa) AS representantes"
            + " FROM empresa_anunciante e";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY e.id_empresa");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE e.razon_social ILIKE ? OR e.nit ILIKE ? OR e.nombre_comercial ILIKE ?"
                + " ORDER BY e.id_empresa", like, like, like);
    }

    /** Devuelve la fila de la empresa o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE e.id_empresa = ?", id);
    }

    /** Representantes (clientes) de la empresa. */
    public List<String[]> representantes(int id) throws SQLException {
        return Conexion.consultar("SELECT u.id_usuario, u.nombre_completo, u.email, r.ci, r.cargo,"
                + " CASE WHEN u.activo THEN 'Sí' ELSE 'No' END"
                + " FROM representante r JOIN usuario u ON u.id_usuario = r.id_usuario"
                + " WHERE r.id_empresa = ? ORDER BY u.id_usuario", id);
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("empresa_anunciante", "id_empresa", id);
    }

    /** ¿Otra empresa (distinta de excluirId) ya usa este NIT? */
    public boolean nitEnUso(String nit, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM empresa_anunciante WHERE nit = ? AND id_empresa <> ?", nit, excluirId) > 0;
    }

    public int insertar(String razonSocial, String nit, String nombreComercial, String direccion,
                        String email, String telefono) throws SQLException {
        return Conexion.insertar("INSERT INTO empresa_anunciante"
                        + " (razon_social, nit, nombre_comercial, direccion_facturacion, email_facturacion, telefono)"
                        + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id_empresa",
                razonSocial, nit, nulo(nombreComercial), nulo(direccion), nulo(email), nulo(telefono));
    }

    public int modificar(int id, String razonSocial, String nit, String nombreComercial, String direccion,
                         String email, String telefono) throws SQLException {
        return Conexion.ejecutar("UPDATE empresa_anunciante SET razon_social = ?, nit = ?, nombre_comercial = ?,"
                        + " direccion_facturacion = ?, email_facturacion = ?, telefono = ? WHERE id_empresa = ?",
                razonSocial, nit, nulo(nombreComercial), nulo(direccion), nulo(email), nulo(telefono), id);
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE empresa_anunciante SET activo = ? WHERE id_empresa = ?", activo, id);
    }

    public int contarProformasPendientes(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM proforma WHERE id_empresa = ? AND estado = 'pendiente'", id);
    }

    private static String nulo(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}
