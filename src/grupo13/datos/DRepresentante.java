package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla representante (cliente).
 * representante ES UN usuario: su PK id_usuario es también FK a usuario.
 * Fila: id_usuario, nombre_completo, email, telefono, ci, cargo, razon_social, nit, activo.
 */
public class DRepresentante {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_completo, u.email, u.telefono, r.ci, r.cargo,"
            + " e.razon_social, e.nit, u.activo"
            + " FROM representante r"
            + " JOIN usuario u ON u.id_usuario = r.id_usuario"
            + " JOIN empresa_anunciante e ON e.id_empresa = r.id_empresa";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY u.id_usuario");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE u.nombre_completo ILIKE ? OR u.email ILIKE ? OR r.ci ILIKE ?"
                + " OR e.razon_social ILIKE ? OR e.nit ILIKE ? ORDER BY u.id_usuario", like, like, like, like, like);
    }

    /** Devuelve la fila del representante o null si el usuario no es representante. */
    public String[] obtener(int idUsuario) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE r.id_usuario = ?", idUsuario);
    }

    /** ¿Otro representante (distinto de excluirId) ya usa este CI? */
    public boolean ciEnUso(String ci, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM representante WHERE lower(ci) = lower(?) AND id_usuario <> ?",
                ci, excluirId) > 0;
    }

    public void insertar(int idUsuario, String ci, String cargo, int idEmpresa) throws SQLException {
        Conexion.ejecutar("INSERT INTO representante (id_usuario, ci, cargo, id_empresa) VALUES (?, ?, ?, ?)",
                idUsuario, ci, Conexion.nulo(cargo), idEmpresa);
    }

    public int modificar(int idUsuario, String ci, String cargo, int idEmpresa) throws SQLException {
        return Conexion.ejecutar("UPDATE representante SET ci = ?, cargo = ?, id_empresa = ? WHERE id_usuario = ?",
                ci, Conexion.nulo(cargo), idEmpresa, idUsuario);
    }

    /** Quita el perfil de representante; el usuario sigue existiendo. */
    public int eliminar(int idUsuario) throws SQLException {
        return Conexion.ejecutar("DELETE FROM representante WHERE id_usuario = ?", idUsuario);
    }
}
