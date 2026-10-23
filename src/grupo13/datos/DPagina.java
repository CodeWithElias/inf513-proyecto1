package grupo13.datos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de pagina_no_autorizada y su composición deteccion_pagina.
 * Fila de página: id_pagina, url, nombre_pagina, plataforma, estado, detecciones, ultima_deteccion, fecha_revision,
 *   revisado_por, id_plataforma.
 * Fila de detección: id_deteccion, fecha_deteccion, termino, evidencia, id_ejecucion.
 * Los resultados de una ejecución se guardan juntos con registrarLote (ver Registro).
 */
public class DPagina {

    private static final String SELECT =
            "SELECT p.id_pagina, p.url, p.nombre_pagina, pl.nombre AS plataforma, p.estado,"
            + " (SELECT count(*) FROM deteccion_pagina d WHERE d.id_pagina = p.id_pagina) AS detecciones,"
            + " (SELECT max(d.fecha_deteccion) FROM deteccion_pagina d WHERE d.id_pagina = p.id_pagina) AS ultima,"
            + " p.fecha_revision, u.nombre_completo AS revisado_por, p.id_plataforma"
            + " FROM pagina_no_autorizada p"
            + " JOIN plataforma pl ON pl.id_plataforma = p.id_plataforma"
            + " LEFT JOIN usuario u ON u.id_usuario = p.id_usuario";

    /** Busca por URL, nombre, estado o plataforma. */
    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY p.id_pagina");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE p.url ILIKE ? OR p.nombre_pagina ILIKE ? OR p.estado ILIKE ?"
                + " OR pl.nombre ILIKE ? ORDER BY p.id_pagina", like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE p.id_pagina = ?", id);
    }

    public List<String[]> detecciones(int idPagina) throws SQLException {
        return Conexion.consultar("SELECT d.id_deteccion, d.fecha_deteccion, t.termino, d.evidencia, d.id_ejecucion"
                + " FROM deteccion_pagina d LEFT JOIN termino_monitoreo t ON t.id_termino = d.id_termino"
                + " WHERE d.id_pagina = ? ORDER BY d.fecha_deteccion DESC, d.id_deteccion DESC", idPagina);
    }

    /** Todas las páginas registradas de una plataforma, para compararlas en memoria. Fila: id_pagina, url_en_minusculas, estado. */
    public List<String[]> indice(int idPlataforma) throws SQLException {
        return Conexion.consultar("SELECT id_pagina, lower(url), estado FROM pagina_no_autorizada WHERE id_plataforma = ?",
                idPlataforma);
    }

    /**
     * Resultado de una ejecución a guardar: página nueva (idPagina null) o detecciones de una existente
     * (nuevoEstado no null = la reabre). detecciones: {id_termino, evidencia}; avisos: {id_usuario, titulo, mensaje}.
     */
    public static final class Registro {
        final Integer idPagina;
        final String url;
        final String nombre;
        final String estado;
        final List<String[]> detecciones;
        final List<String[]> avisos;

        private Registro(Integer idPagina, String url, String nombre, String estado, List<String[]> detecciones,
                         List<String[]> avisos) {
            this.idPagina = idPagina;
            this.url = url;
            this.nombre = nombre;
            this.estado = estado;
            this.detecciones = detecciones;
            this.avisos = avisos;
        }

        public static Registro nueva(String url, String nombre, String estado, List<String[]> detecciones,
                                     List<String[]> avisos) {
            return new Registro(null, url, nombre, estado, detecciones, avisos);
        }

        public static Registro existente(int idPagina, String nuevoEstado, List<String[]> detecciones,
                                         List<String[]> avisos) {
            return new Registro(idPagina, null, null, nuevoEstado, detecciones, avisos);
        }
    }

    /** Guarda todos los resultados de una ejecución en una sola transacción (una sola conexión). */
    public void registrarLote(int idPlataforma, int idEjecucion, List<Registro> registros, String medio)
            throws SQLException {
        Conexion.enTransaccion(c -> {
            for (Registro r : registros) {
                int id;
                if (r.idPagina == null) {
                    id = Conexion.insertar(c, "INSERT INTO pagina_no_autorizada (url, nombre_pagina, estado, id_plataforma)"
                            + " VALUES (?, ?, ?, ?) RETURNING id_pagina", r.url, Conexion.nulo(r.nombre), r.estado, idPlataforma);
                } else {
                    id = r.idPagina;
                    if (r.estado != null) {
                        Conexion.ejecutar(c, "UPDATE pagina_no_autorizada SET estado = ? WHERE id_pagina = ?", r.estado, id);
                    }
                }
                int primera = insertarDetecciones(c, id, idEjecucion, r.detecciones);
                DNotificacion.insertarPorDeteccion(c, medio, primera, r.avisos);
            }
            return null;
        });
    }

    /** Revisión del personal: nuevo estado, quién revisó y cuándo. */
    public int revisar(int id, String estado, int idUsuario) throws SQLException {
        return Conexion.ejecutar("UPDATE pagina_no_autorizada SET estado = ?, id_usuario = ?, fecha_revision = CURRENT_TIMESTAMP"
                + " WHERE id_pagina = ?", estado, idUsuario, id);
    }

    private static int insertarDetecciones(Connection c, int idPagina, int idEjecucion, List<String[]> detecciones)
            throws SQLException {
        int primera = 0;
        for (String[] d : detecciones) {
            int id = Conexion.insertar(c, "INSERT INTO deteccion_pagina (id_pagina, id_termino, id_ejecucion, evidencia)"
                    + " VALUES (?, ?, ?, ?) RETURNING id_deteccion", idPagina, Integer.parseInt(d[0]), idEjecucion, d[1]);
            if (primera == 0) {
                primera = id;
            }
        }
        return primera;
    }
}
