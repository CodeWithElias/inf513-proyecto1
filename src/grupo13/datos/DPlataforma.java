package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla plataforma (catálogo de redes sociales: Facebook, TikTok, etc.).
 * Fila: id_plataforma, nombre, redes_activas.
 */
public class DPlataforma {

    private static final String SELECT =
            "SELECT p.id_plataforma, p.nombre,"
            + " (SELECT count(*) FROM red_social r JOIN canal_difusion c ON c.id_canal = r.id_canal"
            + "   WHERE r.id_plataforma = p.id_plataforma AND c.activo = TRUE) AS redes_activas"
            + " FROM plataforma p";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY p.id_plataforma");
        }
        return Conexion.consultar(SELECT + " WHERE p.nombre ILIKE ? ORDER BY p.id_plataforma", "%" + filtro + "%");
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE p.id_plataforma = ?", id);
    }

    public boolean existe(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM plataforma WHERE id_plataforma = ?", id) > 0;
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM plataforma WHERE lower(nombre) = lower(?) AND id_plataforma <> ?",
                nombre, excluirId) > 0;
    }

    public int insertar(String nombre) throws SQLException {
        return Conexion.insertar("INSERT INTO plataforma (nombre) VALUES (?) RETURNING id_plataforma", nombre);
    }

    /** Borrado físico (útil para pruebas y limpieza de datos). */
    public int eliminar(int id) throws SQLException {
        return Conexion.ejecutar("DELETE FROM plataforma WHERE id_plataforma = ?", id);
    }
}
