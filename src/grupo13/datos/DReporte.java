package grupo13.datos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Capa de datos del CU8: extrae las filas de un periodo [desde, hasta] para que la capa de negocio las agrupe y calcule.
 * No agrupa ni calcula indicadores; solo filtra por fechas (y por empresa en la vista del cliente).
 * Cada método documenta el orden de sus columnas.
 */
public class DReporte {

    /**
     * Proformas emitidas en el periodo (idEmpresa null = todas).
     * Fila: id_proforma, nro_proforma, fecha_emision, fecha_validez, estado, total, razon_social, nombre_visitante, id_empresa.
     */
    public List<String[]> proformas(LocalDate desde, LocalDate hasta, Integer idEmpresa) throws SQLException {
        String sql = "SELECT p.id_proforma, p.nro_proforma, p.fecha_emision, p.fecha_validez, p.estado, p.total,"
                + " e.razon_social, v.nombre, p.id_empresa"
                + " FROM proforma p"
                + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
                + " LEFT JOIN visitante v ON v.id_visitante = p.id_visitante"
                + " WHERE p.fecha_emision BETWEEN ? AND ?";
        return idEmpresa == null
                ? Conexion.consultar(sql + " ORDER BY p.fecha_emision", desde, hasta)
                : Conexion.consultar(sql + " AND p.id_empresa = ? ORDER BY p.fecha_emision", desde, hasta, idEmpresa);
    }

    /**
     * Ítems de las proformas en el estado indicado emitidas en el periodo.
     * Fila: tipo ("tarifa" | "paquete"), nombre (servicio o paquete), cantidad, subtotal.
     */
    public List<String[]> itemsVendidos(LocalDate desde, LocalDate hasta, String estadoProforma) throws SQLException {
        return Conexion.consultar("SELECT CASE WHEN d.id_tarifa IS NOT NULL THEN 'tarifa' ELSE 'paquete' END,"
                + " COALESCE(s.nombre, pq.nombre), d.cantidad, d.subtotal"
                + " FROM detalle_proforma d"
                + " JOIN proforma p ON p.id_proforma = d.id_proforma"
                + " LEFT JOIN tarifa t ON t.id_tarifa = d.id_tarifa"
                + " LEFT JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + " LEFT JOIN paquete pq ON pq.id_paquete = d.id_paquete"
                + " WHERE p.estado = ? AND p.fecha_emision BETWEEN ? AND ?", estadoProforma, desde, hasta);
    }

    /**
     * Campañas cuyo periodo se superpone con [desde, hasta] (idEmpresa null = todas).
     * Fila: id_campania, nombre, estado, razon_social, total_proforma, fecha_inicio, fecha_fin.
     */
    public List<String[]> campanias(LocalDate desde, LocalDate hasta, Integer idEmpresa) throws SQLException {
        String sql = "SELECT c.id_campania, c.nombre, c.estado, e.razon_social, p.total, c.fecha_inicio, c.fecha_fin"
                + " FROM campania c JOIN proforma p ON p.id_proforma = c.id_proforma"
                + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
                + " WHERE COALESCE(c.fecha_inicio, ?) <= ? AND COALESCE(c.fecha_fin, ?) >= ?";
        return idEmpresa == null
                ? Conexion.consultar(sql + " ORDER BY c.id_campania", desde, hasta, hasta, desde)
                : Conexion.consultar(sql + " AND p.id_empresa = ? ORDER BY c.id_campania", desde, hasta, hasta, desde, idEmpresa);
    }

    /** Espacios del calendario en el periodo. Fila: canal, tipo_canal, estado. */
    public List<String[]> espacios(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT c.nombre, c.tipo, e.estado FROM espacio_publicitario e"
                + " JOIN canal_difusion c ON c.id_canal = e.id_canal WHERE e.fecha BETWEEN ? AND ?", desde, hasta);
    }

    /**
     * Pautas con fecha de programación en el periodo (incluye canceladas, que ya no tienen canal).
     * Fila: id_programacion, estado, canal (o null), tipo_canal (o null), campania.
     */
    public List<String[]> pautas(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT pp.id_programacion, pp.estado, c.nombre, c.tipo, cam.nombre"
                + " FROM programacion_pauta pp"
                + " JOIN material_publicitario m ON m.id_material = pp.id_material"
                + " JOIN campania cam ON cam.id_campania = m.id_campania"
                + " LEFT JOIN espacio_publicitario e ON e.id_espacio = pp.id_espacio"
                + " LEFT JOIN canal_difusion c ON c.id_canal = e.id_canal"
                + " WHERE pp.fecha_programacion::date BETWEEN ? AND ?", desde, hasta);
    }

    /** Material cargado en el periodo. Fila: id_material, tipo, estado, version. */
    public List<String[]> materiales(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT id_material, tipo, estado, version FROM material_publicitario"
                + " WHERE fecha_carga::date BETWEEN ? AND ?", desde, hasta);
    }

    /** Revisiones de material del periodo. Fila: resultado, revisor. */
    public List<String[]> revisiones(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT r.resultado, u.nombre_completo FROM revision_material r"
                + " LEFT JOIN usuario u ON u.id_usuario = r.id_usuario WHERE r.fecha::date BETWEEN ? AND ?", desde, hasta);
    }

    /**
     * Última medición de cada pauta en el estado indicado con fecha de programación en el periodo (idEmpresa null = todas).
     * Fila: id_programacion, campania, razon_social, canal, archivo, alcance, impresiones, interacciones, clics,
     *   compartidos (NULL si la pauta no tiene mediciones), id_campania.
     */
    public List<String[]> impacto(LocalDate desde, LocalDate hasta, String estadoPauta, Integer idEmpresa)
            throws SQLException {
        String sql = "SELECT pp.id_programacion, cam.nombre, e.razon_social, c.nombre, m.nombre_archivo,"
                + " lm.alcance, lm.impresiones, lm.interacciones, lm.clics, lm.compartidos, cam.id_campania"
                + " FROM programacion_pauta pp"
                + " JOIN material_publicitario m ON m.id_material = pp.id_material"
                + " JOIN campania cam ON cam.id_campania = m.id_campania"
                + " JOIN proforma p ON p.id_proforma = cam.id_proforma"
                + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
                + " JOIN espacio_publicitario es ON es.id_espacio = pp.id_espacio"
                + " JOIN canal_difusion c ON c.id_canal = es.id_canal"
                + " LEFT JOIN LATERAL (SELECT * FROM metrica_publicacion mp WHERE mp.id_programacion = pp.id_programacion"
                + "   ORDER BY mp.fecha_medicion DESC, mp.id_metrica DESC LIMIT 1) lm ON TRUE"
                + " WHERE pp.estado = ? AND pp.fecha_programacion::date BETWEEN ? AND ?";
        return idEmpresa == null
                ? Conexion.consultar(sql + " ORDER BY pp.id_programacion", estadoPauta, desde, hasta)
                : Conexion.consultar(sql + " AND p.id_empresa = ? ORDER BY pp.id_programacion", estadoPauta, desde, hasta, idEmpresa);
    }

    /** Mediciones de seguidores del periodo. Fila: id_canal, red_social, plataforma, fecha, seguidores (por red y fecha). */
    public List<String[]> seguidores(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT s.id_canal, c.nombre, pl.nombre, s.fecha, s.seguidores FROM seguidores_red s"
                + " JOIN red_social r ON r.id_canal = s.id_canal"
                + " JOIN canal_difusion c ON c.id_canal = r.id_canal"
                + " JOIN plataforma pl ON pl.id_plataforma = r.id_plataforma"
                + " WHERE s.fecha BETWEEN ? AND ? ORDER BY s.id_canal, s.fecha, s.id_seguidores", desde, hasta);
    }

    /** Ejecuciones de scraping del periodo. Fila: plataforma, estado. */
    public List<String[]> ejecuciones(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT pl.nombre, e.estado FROM ejecucion_scraping e"
                + " JOIN plataforma pl ON pl.id_plataforma = e.id_plataforma WHERE e.fecha_inicio::date BETWEEN ? AND ?",
                desde, hasta);
    }

    /**
     * Detecciones del periodo, indicando si es la primera detección de la página (página nueva).
     * Fila: id_pagina, url, nombre_pagina, estado_pagina, plataforma, termino, fecha_deteccion, es_primera (t/f).
     */
    public List<String[]> detecciones(LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.consultar("SELECT p.id_pagina, p.url, p.nombre_pagina, p.estado, pl.nombre, t.termino, d.fecha_deteccion,"
                + " NOT EXISTS (SELECT 1 FROM deteccion_pagina d2 WHERE d2.id_pagina = d.id_pagina AND d2.id_deteccion < d.id_deteccion)"
                + " FROM deteccion_pagina d"
                + " JOIN pagina_no_autorizada p ON p.id_pagina = d.id_pagina"
                + " JOIN plataforma pl ON pl.id_plataforma = p.id_plataforma"
                + " LEFT JOIN termino_monitoreo t ON t.id_termino = d.id_termino"
                + " WHERE d.fecha_deteccion::date BETWEEN ? AND ? ORDER BY d.fecha_deteccion DESC", desde, hasta);
    }

    /** Material pendiente de revisión (cualquier fecha). */
    public int contarMaterialEnEstado(String estado) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM material_publicitario WHERE estado = ?", estado);
    }
}
