package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/** Capa de datos de visitante (persona no registrada que pide una cotización, RF07). */
public class DVisitante {

    private static final String SELECT =
            "SELECT v.id_visitante, v.nombre, v.email, v.telefono, to_char(v.fecha_registro, 'DD/MM/YYYY'),"
            + " (SELECT count(*) FROM proforma p WHERE p.id_visitante = v.id_visitante) AS proformas"
            + " FROM visitante v";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY v.id_visitante");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE v.nombre ILIKE ? OR v.email ILIKE ? ORDER BY v.id_visitante", like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE v.id_visitante = ?", id);
    }

    public int insertar(String nombre, String email, String telefono) throws SQLException {
        return Conexion.insertar("INSERT INTO visitante (nombre, email, telefono) VALUES (?, ?, ?) RETURNING id_visitante",
                nombre, nulo(email), nulo(telefono));
    }

    public int modificar(int id, String nombre, String email, String telefono) throws SQLException {
        return Conexion.ejecutar("UPDATE visitante SET nombre = ?, email = ?, telefono = ? WHERE id_visitante = ?",
                nombre, nulo(email), nulo(telefono), id);
    }

    /** Borrado físico (la tabla no tiene columna activo); solo si no tiene proformas. */
    public int eliminar(int id) throws SQLException {
        return Conexion.ejecutar("DELETE FROM visitante WHERE id_visitante = ?", id);
    }

    public int contarProformas(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM proforma WHERE id_visitante = ?", id);
    }

    private static String nulo(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}
