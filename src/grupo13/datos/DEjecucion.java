package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de ejecucion_scraping.
 * Fila: id_ejecucion, plataforma, fecha_inicio, fecha_fin, estado, mensaje_error, detecciones, paginas_nuevas.
 */
public class DEjecucion {

    private static final String SELECT =
            "SELECT e.id_ejecucion, p.nombre AS plataforma, e.fecha_inicio, e.fecha_fin, e.estado, e.mensaje_error,"
            + " (SELECT count(*) FROM deteccion_pagina d WHERE d.id_ejecucion = e.id_ejecucion) AS detecciones,"
            + " (SELECT count(*) FROM deteccion_pagina d WHERE d.id_ejecucion = e.id_ejecucion AND NOT EXISTS"
            + "   (SELECT 1 FROM deteccion_pagina d2 WHERE d2.id_pagina = d.id_pagina AND d2.id_deteccion < d.id_deteccion))"
            + "   AS paginas_nuevas"
            + " FROM ejecucion_scraping e JOIN plataforma p ON p.id_plataforma = e.id_plataforma";

    /** idPlataforma null = todas (las más recientes primero). */
    public List<String[]> listar(Integer idPlataforma) throws SQLException {
        if (idPlataforma == null) {
            return Conexion.consultar(SELECT + " ORDER BY e.id_ejecucion DESC");
        }
        return Conexion.consultar(SELECT + " WHERE e.id_plataforma = ? ORDER BY e.id_ejecucion DESC", idPlataforma);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE e.id_ejecucion = ?", id);
    }

    /** Ejecuciones de la plataforma en el estado indicado. Fila: id_ejecucion, minutos_desde_el_inicio. */
    public List<String[]> listarEnEstado(int idPlataforma, String estado) throws SQLException {
        return Conexion.consultar("SELECT id_ejecucion, floor(extract(epoch FROM (CURRENT_TIMESTAMP - fecha_inicio)) / 60)"
                + " FROM ejecucion_scraping WHERE id_plataforma = ? AND estado = ? ORDER BY id_ejecucion", idPlataforma, estado);
    }

    public int iniciar(int idPlataforma, String estado) throws SQLException {
        return Conexion.insertar("INSERT INTO ejecucion_scraping (id_plataforma, estado) VALUES (?, ?) RETURNING id_ejecucion",
                idPlataforma, estado);
    }

    public int finalizar(int id, String estado, String mensajeError) throws SQLException {
        return Conexion.ejecutar("UPDATE ejecucion_scraping SET estado = ?, mensaje_error = ?, fecha_fin = CURRENT_TIMESTAMP"
                + " WHERE id_ejecucion = ?", estado, Conexion.nulo(mensajeError), id);
    }
}
