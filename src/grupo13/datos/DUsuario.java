package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla usuario. Nunca devuelve password_hash.
 * Fila: id_usuario, nombre_completo, email, telefono, rol, unidad, activo, fecha_registro, ultimo_acceso.
 */
public class DUsuario {

    private static final String SELECT =
            "SELECT u.id_usuario, u.nombre_completo, u.email, u.telefono, r.nombre AS rol,"
            + " uu.nombre AS unidad, u.activo, u.fecha_registro, u.ultimo_acceso"
            + " FROM usuario u"
            + " JOIN rol r ON r.id_rol = u.id_rol"
            + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = u.id_unidad";

    /** filtro null = todos; si no, busca por nombre, email, rol o unidad. */
    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY u.id_usuario");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE u.nombre_completo ILIKE ? OR u.email ILIKE ? OR r.nombre ILIKE ?"
                + " OR uu.nombre ILIKE ? ORDER BY u.id_usuario", like, like, like, like);
    }

    /** Devuelve la fila del usuario o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE u.id_usuario = ?", id);
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("usuario", "id_usuario", id);
    }

    /** ¿Otro usuario (distinto de excluirId) ya usa este email? */
    public boolean emailEnUso(String email, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM usuario WHERE lower(email) = lower(?) AND id_usuario <> ?",
                email, excluirId) > 0;
    }

    /** Devuelve el id generado. */
    public int insertar(String nombre, String email, String telefono, String passwordHash,
                        int idRol, Integer idUnidad) throws SQLException {
        return Conexion.insertar("INSERT INTO usuario (nombre_completo, email, telefono, password_hash, id_rol, id_unidad)"
                        + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id_usuario",
                nombre, email, Conexion.nulo(telefono), passwordHash, idRol, idUnidad);
    }

    /** passwordHash null = no se cambia la contraseña. */
    public int modificar(int id, String nombre, String email, String telefono, String passwordHash,
                         int idRol, Integer idUnidad) throws SQLException {
        if (passwordHash == null) {
            return Conexion.ejecutar("UPDATE usuario SET nombre_completo = ?, email = ?, telefono = ?, id_rol = ?,"
                    + " id_unidad = ? WHERE id_usuario = ?", nombre, email, Conexion.nulo(telefono), idRol, idUnidad, id);
        }
        return Conexion.ejecutar("UPDATE usuario SET nombre_completo = ?, email = ?, telefono = ?, id_rol = ?,"
                        + " id_unidad = ?, password_hash = ? WHERE id_usuario = ?",
                nombre, email, Conexion.nulo(telefono), idRol, idUnidad, passwordHash, id);
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE usuario SET activo = ? WHERE id_usuario = ?", activo, id);
    }
}
