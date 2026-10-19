package grupo13.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Capa de datos de red_social y su superclase canal_difusion.
 * red_social ES UN canal_difusion (generalización con id_canal como PK y FK).
 */
public class DRedSocial {

    private static final String SELECT =
            "SELECT c.id_canal, c.nombre, p.nombre AS plataforma, r.url,"
            + " COALESCE(u.nombre_completo, 'Sin asignar') AS responsable,"
            + " COALESCE(un.nombre, 'Sin asignar') AS unidad,"
            + " COALESCE((SELECT sr.seguidores::text FROM seguidores_red sr"
            + "           WHERE sr.id_canal = c.id_canal"
            + "           ORDER BY sr.fecha DESC, sr.id_seguidores DESC LIMIT 1), '0') AS seguidores,"
            + " CASE WHEN c.activo THEN 'Sí' ELSE 'No' END AS activo,"
            + " to_char(c.fecha_alta, 'DD/MM/YYYY') AS fecha_alta,"
            + " COALESCE(to_char(c.fecha_baja, 'DD/MM/YYYY'), '-') AS fecha_baja"
            + " FROM red_social r"
            + " JOIN canal_difusion c ON c.id_canal = r.id_canal"
            + " JOIN plataforma p ON p.id_plataforma = r.id_plataforma"
            + " LEFT JOIN usuario u ON u.id_usuario = r.id_usuario"
            + " LEFT JOIN unidad_universitaria un ON un.id_unidad = r.id_unidad";

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = SELECT
                + (filtro == null ? "" : " WHERE c.nombre ILIKE ? OR p.nombre ILIKE ? OR r.url ILIKE ?"
                        + " OR u.nombre_completo ILIKE ? OR un.nombre ILIKE ?")
                + " ORDER BY c.id_canal";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                for (int i = 1; i <= 5; i++) {
                    ps.setString(i, "%" + filtro + "%");
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }

    public String[] obtener(int idCanal) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(SELECT + " WHERE r.id_canal = ?")) {
            ps.setInt(1, idCanal);
            try (ResultSet rs = ps.executeQuery()) {
                List<String[]> filas = Conexion.leerFilas(rs);
                return filas.isEmpty() ? null : filas.get(0);
            }
        }
    }

    /** null = no existe, true/false = activo. */
    public Boolean estaActivo(int idCanal) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT c.activo FROM red_social r JOIN canal_difusion c ON c.id_canal = r.id_canal WHERE r.id_canal = ?")) {
            ps.setInt(1, idCanal);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBoolean(1) : null;
            }
        }
    }

    public boolean urlEnUso(String url, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM red_social WHERE lower(url) = lower(?) AND id_canal <> ?")) {
            ps.setString(1, url);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        try (Connection c = Conexion.abrir();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM canal_difusion WHERE lower(nombre) = lower(?) AND tipo = 'red_social' AND id_canal <> ?")) {
            ps.setString(1, nombre);
            ps.setInt(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta en canal_difusion y luego en red_social dentro de una transacción. */
    public int insertar(String nombre, String url, int idPlataforma, Integer idUsuario, Integer idUnidad)
            throws SQLException {
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try {
                int idCanal;
                String sqlCanal = "INSERT INTO canal_difusion (tipo, nombre) VALUES ('red_social', ?) RETURNING id_canal";
                try (PreparedStatement ps = c.prepareStatement(sqlCanal)) {
                    ps.setString(1, nombre);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idCanal = rs.getInt(1);
                    }
                }

                String sqlRed = "INSERT INTO red_social (id_canal, url, id_plataforma, id_usuario, id_unidad) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sqlRed)) {
                    ps.setInt(1, idCanal);
                    ps.setString(2, url);
                    ps.setInt(3, idPlataforma);
                    setEnteroONull(ps, 4, idUsuario);
                    setEnteroONull(ps, 5, idUnidad);
                    ps.executeUpdate();
                }

                c.commit();
                return idCanal;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Modifica los datos del canal y de la red social en una transacción. */
    public int modificar(int idCanal, String nombre, String url, int idPlataforma, Integer idUsuario, Integer idUnidad)
            throws SQLException {
        try (Connection c = Conexion.abrir()) {
            c.setAutoCommit(false);
            try {
                int n;
                String sqlCanal = "UPDATE canal_difusion SET nombre = ? WHERE id_canal = ? AND tipo = 'red_social'";
                try (PreparedStatement ps = c.prepareStatement(sqlCanal)) {
                    ps.setString(1, nombre);
                    ps.setInt(2, idCanal);
                    n = ps.executeUpdate();
                }

                if (n > 0) {
                    String sqlRed = "UPDATE red_social SET url = ?, id_plataforma = ?, id_usuario = ?, id_unidad = ? WHERE id_canal = ?";
                    try (PreparedStatement ps = c.prepareStatement(sqlRed)) {
                        ps.setString(1, url);
                        ps.setInt(2, idPlataforma);
                        setEnteroONull(ps, 3, idUsuario);
                        setEnteroONull(ps, 4, idUnidad);
                        ps.setInt(5, idCanal);
                        ps.executeUpdate();
                    }
                }

                c.commit();
                return n;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Baja lógica: inactiva el canal y registra fecha_baja. */
    public int eliminar(int idCanal) throws SQLException {
        String sql = "UPDATE canal_difusion SET activo = FALSE, fecha_baja = CURRENT_DATE WHERE id_canal = ? AND tipo = 'red_social'";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            return ps.executeUpdate();
        }
    }

    /** Reactiva el canal y limpia la fecha_baja. */
    public int activar(int idCanal) throws SQLException {
        String sql = "UPDATE canal_difusion SET activo = TRUE, fecha_baja = NULL WHERE id_canal = ? AND tipo = 'red_social'";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            return ps.executeUpdate();
        }
    }

    /** Borrado físico en cascada (para limpieza de datos en pruebas). */
    public int eliminarFisico(int idCanal) throws SQLException {
        String sql = "DELETE FROM canal_difusion WHERE id_canal = ? AND tipo = 'red_social'";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idCanal);
            return ps.executeUpdate();
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
