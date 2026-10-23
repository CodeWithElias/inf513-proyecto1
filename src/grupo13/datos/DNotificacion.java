package grupo13.datos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de notificacion.
 * Fila: id_notificacion, fecha_envio, medio, titulo, mensaje, leida, id_programacion.
 */
public class DNotificacion {

    private static final String SELECT =
            "SELECT id_notificacion, fecha_envio, medio, titulo, mensaje, leida, id_programacion FROM notificacion";

    public List<String[]> listarPorUsuario(int idUsuario) throws SQLException {
        return Conexion.consultar(SELECT + " WHERE id_usuario = ? ORDER BY fecha_envio DESC, id_notificacion DESC", idUsuario);
    }

    /** {id_usuario, leida} o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno("SELECT id_usuario, leida FROM notificacion WHERE id_notificacion = ?", id);
    }

    public int marcarLeida(int id) throws SQLException {
        return Conexion.ejecutar("UPDATE notificacion SET leida = TRUE WHERE id_notificacion = ?", id);
    }

    /** Registra avisos ({id_usuario, titulo, mensaje}) ligados a una detección de página no autorizada (CU6). */
    static void insertarPorDeteccion(Connection c, String medio, int idDeteccion, List<String[]> avisos)
            throws SQLException {
        for (String[] a : avisos) {
            Conexion.ejecutar(c, "INSERT INTO notificacion (medio, titulo, mensaje, id_usuario, id_deteccion)"
                    + " VALUES (?, ?, ?, ?, ?)", medio, a[1], a[2], Integer.parseInt(a[0]), idDeteccion);
        }
    }

    /**
     * Registra los avisos de una programación dentro de la transacción en curso.
     * Cada aviso: {id_usuario, titulo, mensaje}.
     */
    static void insertar(Connection c, String medio, int idProgramacion, List<String[]> avisos) throws SQLException {
        for (String[] a : avisos) {
            Conexion.ejecutar(c, "INSERT INTO notificacion (medio, titulo, mensaje, id_usuario, id_programacion)"
                    + " VALUES (?, ?, ?, ?, ?)", medio, a[1], a[2], Integer.parseInt(a[0]), idProgramacion);
        }
    }
}
