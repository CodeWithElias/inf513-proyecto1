package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de termino_monitoreo.
 * Fila: id_termino, termino, tipo, activo, detecciones.
 */
public class DTermino {

    private static final String SELECT =
            "SELECT t.id_termino, t.termino, t.tipo, t.activo,"
            + " (SELECT count(*) FROM deteccion_pagina d WHERE d.id_termino = t.id_termino) AS detecciones"
            + " FROM termino_monitoreo t";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY t.id_termino");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE t.termino ILIKE ? OR t.tipo ILIKE ? ORDER BY t.id_termino", like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE t.id_termino = ?", id);
    }

    /** Términos activos. Fila: id_termino, termino. */
    public List<String[]> activos() throws SQLException {
        return Conexion.consultar("SELECT id_termino, termino FROM termino_monitoreo WHERE activo ORDER BY id_termino");
    }

    public boolean existe(String termino, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM termino_monitoreo WHERE lower(termino) = lower(?) AND id_termino <> ?",
                termino, excluirId) > 0;
    }

    public int insertar(String termino, String tipo) throws SQLException {
        return Conexion.insertar("INSERT INTO termino_monitoreo (termino, tipo) VALUES (?, ?) RETURNING id_termino",
                termino, Conexion.nulo(tipo));
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE termino_monitoreo SET activo = ? WHERE id_termino = ?", activo, id);
    }
}
