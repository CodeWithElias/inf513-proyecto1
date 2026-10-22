package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de red_social y su superclase canal_difusion.
 * red_social ES UN canal_difusion (generalización con id_canal como PK y FK).
 * Fila: id_canal, nombre, plataforma, url, responsable, unidad, seguidores, activo, fecha_alta, fecha_baja.
 */
public class DRedSocial {

    private static final String SELECT =
            "SELECT c.id_canal, c.nombre, p.nombre AS plataforma, r.url,"
            + " u.nombre_completo AS responsable, un.nombre AS unidad,"
            + " COALESCE((SELECT sr.seguidores FROM seguidores_red sr"
            + "           WHERE sr.id_canal = c.id_canal"
            + "           ORDER BY sr.fecha DESC, sr.id_seguidores DESC LIMIT 1), 0) AS seguidores,"
            + " c.activo, c.fecha_alta, c.fecha_baja"
            + " FROM red_social r"
            + " JOIN canal_difusion c ON c.id_canal = r.id_canal"
            + " JOIN plataforma p ON p.id_plataforma = r.id_plataforma"
            + " LEFT JOIN usuario u ON u.id_usuario = r.id_usuario"
            + " LEFT JOIN unidad_universitaria un ON un.id_unidad = r.id_unidad";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY c.id_canal");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE c.nombre ILIKE ? OR p.nombre ILIKE ? OR r.url ILIKE ?"
                + " OR u.nombre_completo ILIKE ? OR un.nombre ILIKE ? ORDER BY c.id_canal", like, like, like, like, like);
    }

    public String[] obtener(int idCanal) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE r.id_canal = ?", idCanal);
    }

    /** null = no existe, true/false = activo. */
    public Boolean estaActivo(int idCanal) throws SQLException {
        String v = Conexion.valor("SELECT c.activo FROM red_social r JOIN canal_difusion c ON c.id_canal = r.id_canal"
                + " WHERE r.id_canal = ?", idCanal);
        return v == null ? null : v.equals("t");
    }

    public boolean urlEnUso(String url, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM red_social WHERE lower(url) = lower(?) AND id_canal <> ?",
                url, excluirId) > 0;
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM canal_difusion WHERE lower(nombre) = lower(?) AND tipo = 'red_social'"
                + " AND id_canal <> ?", nombre, excluirId) > 0;
    }

    /** Inserta en canal_difusion y luego en red_social dentro de una transacción. */
    public int insertar(String nombre, String url, int idPlataforma, Integer idUsuario, Integer idUnidad)
            throws SQLException {
        return Conexion.enTransaccion(c -> {
            int idCanal = Conexion.insertar(c, "INSERT INTO canal_difusion (tipo, nombre) VALUES ('red_social', ?)"
                    + " RETURNING id_canal", nombre);
            Conexion.ejecutar(c, "INSERT INTO red_social (id_canal, url, id_plataforma, id_usuario, id_unidad)"
                    + " VALUES (?, ?, ?, ?, ?)", idCanal, url, idPlataforma, idUsuario, idUnidad);
            return idCanal;
        });
    }

    /** Modifica los datos del canal y de la red social en una transacción. */
    public int modificar(int idCanal, String nombre, String url, int idPlataforma, Integer idUsuario, Integer idUnidad)
            throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "UPDATE canal_difusion SET nombre = ? WHERE id_canal = ? AND tipo = 'red_social'",
                    nombre, idCanal);
            if (n > 0) {
                Conexion.ejecutar(c, "UPDATE red_social SET url = ?, id_plataforma = ?, id_usuario = ?, id_unidad = ?"
                        + " WHERE id_canal = ?", url, idPlataforma, idUsuario, idUnidad, idCanal);
            }
            return n;
        });
    }

    /** Baja lógica: inactiva el canal y registra fecha_baja. */
    public int eliminar(int idCanal) throws SQLException {
        return Conexion.ejecutar("UPDATE canal_difusion SET activo = FALSE, fecha_baja = CURRENT_DATE"
                + " WHERE id_canal = ? AND tipo = 'red_social'", idCanal);
    }

    /** Reactiva el canal y limpia la fecha_baja. */
    public int activar(int idCanal) throws SQLException {
        return Conexion.ejecutar("UPDATE canal_difusion SET activo = TRUE, fecha_baja = NULL"
                + " WHERE id_canal = ? AND tipo = 'red_social'", idCanal);
    }

    /** Borrado físico en cascada (para limpieza de datos en pruebas). */
    public int eliminarFisico(int idCanal) throws SQLException {
        return Conexion.ejecutar("DELETE FROM canal_difusion WHERE id_canal = ? AND tipo = 'red_social'", idCanal);
    }
}
