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

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY u.id_usuario");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE u.nombre_completo ILIKE ? OR u.email ILIKE ? OR r.nombre ILIKE ?"
                + " OR uu.nombre ILIKE ? ORDER BY u.id_usuario", like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE u.id_usuario = ?", id);
    }

    /**
     * Usuario por correo (sin distinguir mayúsculas), o null.
     * Fila: id_usuario, nombre_completo, email, rol, usuario_activo, id_unidad, rol_activo.
     */
    public String[] porEmail(String email) throws SQLException {
        return Conexion.consultarUno("SELECT u.id_usuario, u.nombre_completo, u.email, r.nombre, u.activo, u.id_unidad, r.activo"
                + " FROM usuario u JOIN rol r ON r.id_rol = u.id_rol WHERE lower(u.email) = lower(?)", email);
    }

    public int registrarAcceso(int id) throws SQLException {
        return Conexion.ejecutar("UPDATE usuario SET ultimo_acceso = CURRENT_TIMESTAMP WHERE id_usuario = ?", id);
    }

    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("usuario", "id_usuario", id);
    }

    /** Usuarios activos de un rol (por nombre, sin distinguir mayúsculas). Fila: id_usuario, nombre_completo, email. */
    public List<String[]> activosDeRol(String nombreRol) throws SQLException {
        return Conexion.consultar("SELECT u.id_usuario, u.nombre_completo, u.email FROM usuario u"
                + " JOIN rol r ON r.id_rol = u.id_rol WHERE lower(r.nombre) = lower(?) AND u.activo AND r.activo"
                + " ORDER BY u.id_usuario", nombreRol);
    }

    /** Usuarios activos asignados a una unidad. Fila: id_usuario, nombre_completo, email. */
    public List<String[]> activosDeUnidad(int idUnidad) throws SQLException {
        return Conexion.consultar("SELECT id_usuario, nombre_completo, email FROM usuario WHERE id_unidad = ? AND activo"
                + " ORDER BY id_usuario", idUnidad);
    }

    public boolean emailEnUso(String email, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM usuario WHERE lower(email) = lower(?) AND id_usuario <> ?",
                email, excluirId) > 0;
    }

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

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE usuario SET activo = ? WHERE id_usuario = ?", activo, id);
    }
}
