package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla rol.
 * Fila: id_rol, nombre, descripcion, activo, usuarios_activos.
 */
public class DRol {

    private static final String SELECT =
            "SELECT r.id_rol, r.nombre, r.descripcion, r.activo,"
            + " (SELECT count(*) FROM usuario u WHERE u.id_rol = r.id_rol AND u.activo) AS usuarios_activos"
            + " FROM rol r";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY r.id_rol");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE r.nombre ILIKE ? OR r.descripcion ILIKE ? ORDER BY r.id_rol", like, like);
    }

    /** Devuelve la fila del rol o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE r.id_rol = ?", id);
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("rol", "id_rol", id);
    }

    /** ¿Otro rol (distinto de excluirId) ya usa este nombre? */
    public boolean existeNombre(String nombre, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM rol WHERE lower(nombre) = lower(?) AND id_rol <> ?",
                nombre, excluirId) > 0;
    }

    public int contarUsuariosActivos(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM usuario WHERE id_rol = ? AND activo", id);
    }

    public int insertar(String nombre, String descripcion) throws SQLException {
        return Conexion.insertar("INSERT INTO rol (nombre, descripcion) VALUES (?, ?) RETURNING id_rol",
                nombre, Conexion.nulo(descripcion));
    }

    public int modificar(int id, String nombre, String descripcion) throws SQLException {
        return Conexion.ejecutar("UPDATE rol SET nombre = ?, descripcion = ? WHERE id_rol = ?",
                nombre, Conexion.nulo(descripcion), id);
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE rol SET activo = ? WHERE id_rol = ?", activo, id);
    }
}
