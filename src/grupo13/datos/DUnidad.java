package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de la tabla unidad_universitaria (estructura jerárquica: facultad → carrera…).
 * Fila: id_unidad, nombre, tipo, unidad_padre, activo, encargados_activos.
 */
public class DUnidad {

    private static final String SELECT =
            "SELECT u.id_unidad, u.nombre, u.tipo, p.nombre AS padre, u.activo,"
            + " (SELECT string_agg(e.nombre_completo, ', ' ORDER BY e.nombre_completo) FROM usuario e"
            + "   WHERE e.id_unidad = u.id_unidad AND e.activo) AS encargados"
            + " FROM unidad_universitaria u"
            + " LEFT JOIN unidad_universitaria p ON p.id_unidad = u.id_unidad_padre";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY u.id_unidad");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE u.nombre ILIKE ? OR u.tipo ILIKE ? OR p.nombre ILIKE ?"
                + " ORDER BY u.id_unidad", like, like, like);
    }

    /** Devuelve la fila de la unidad o null si no existe. */
    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE u.id_unidad = ?", id);
    }

    /** Subunidades directas de una unidad. */
    public List<String[]> listarHijas(int id) throws SQLException {
        return Conexion.consultar(SELECT + " WHERE u.id_unidad_padre = ? ORDER BY u.id_unidad", id);
    }

    /** null = no existe, true/false = valor de activo. */
    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("unidad_universitaria", "id_unidad", id);
    }

    /** IDs de la unidad y de todo su subárbol (hijas, nietas…). */
    public List<Integer> idsDelSubarbol(int id) throws SQLException {
        return Conexion.ids("WITH RECURSIVE arbol AS ("
                + " SELECT id_unidad FROM unidad_universitaria WHERE id_unidad = ?"
                + " UNION SELECT u.id_unidad FROM unidad_universitaria u JOIN arbol a ON u.id_unidad_padre = a.id_unidad)"
                + " SELECT id_unidad FROM arbol", id);
    }

    public int contarUsuariosActivos(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM usuario WHERE id_unidad = ? AND activo", id);
    }

    public int contarHijasActivas(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM unidad_universitaria WHERE id_unidad_padre = ? AND activo", id);
    }

    public int insertar(String nombre, String tipo, Integer idPadre) throws SQLException {
        return Conexion.insertar("INSERT INTO unidad_universitaria (nombre, tipo, id_unidad_padre) VALUES (?, ?, ?)"
                + " RETURNING id_unidad", nombre, Conexion.nulo(tipo), idPadre);
    }

    public int modificar(int id, String nombre, String tipo, Integer idPadre) throws SQLException {
        return Conexion.ejecutar("UPDATE unidad_universitaria SET nombre = ?, tipo = ?, id_unidad_padre = ?"
                + " WHERE id_unidad = ?", nombre, Conexion.nulo(tipo), idPadre, id);
    }

    /** Baja / alta lógica. */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE unidad_universitaria SET activo = ? WHERE id_unidad = ?", activo, id);
    }

    /** id del padre, o null si no tiene (o si la unidad no existe). */
    public Integer obtenerPadre(int id) throws SQLException {
        String v = Conexion.valor("SELECT id_unidad_padre FROM unidad_universitaria WHERE id_unidad = ?", id);
        return v == null ? null : Integer.parseInt(v);
    }
}
