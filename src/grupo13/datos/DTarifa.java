package grupo13.datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Capa de datos de tarifa (precio de un servicio, opcionalmente por unidad universitaria). RF05. */
public class DTarifa {

    private static final String SELECT =
            "SELECT t.id_tarifa, s.nombre AS servicio, uu.nombre AS unidad, t.monto, t.unidad_cobro, t.duracion_seg,"
            + " to_char(t.vigente_desde, 'DD/MM/YYYY'), to_char(t.vigente_hasta, 'DD/MM/YYYY'),"
            + " CASE WHEN t.activo THEN 'Sí' ELSE 'No' END,"
            + " CASE WHEN t.activo AND (t.vigente_desde IS NULL OR t.vigente_desde <= CURRENT_DATE)"
            + "       AND (t.vigente_hasta IS NULL OR t.vigente_hasta >= CURRENT_DATE) THEN 'Sí' ELSE 'No' END AS vigente"
            + " FROM tarifa t"
            + " JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
            + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = t.id_unidad";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY t.id_tarifa");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE s.nombre ILIKE ? OR s.tipo ILIKE ? OR uu.nombre ILIKE ? OR t.unidad_cobro ILIKE ?"
                + " ORDER BY t.id_tarifa", like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE t.id_tarifa = ?", id);
    }

    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("tarifa", "id_tarifa", id);
    }

    /** Activa y dentro de su periodo de vigencia (hoy). */
    public boolean esVigente(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM tarifa WHERE id_tarifa = ? AND activo"
                + " AND (vigente_desde IS NULL OR vigente_desde <= CURRENT_DATE)"
                + " AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE)", id) > 0;
    }

    public BigDecimal monto(int id) throws SQLException {
        return new BigDecimal(Conexion.valor("SELECT monto FROM tarifa WHERE id_tarifa = ?", id));
    }

    public int insertar(int idServicio, Integer idUnidad, BigDecimal monto, String unidadCobro, Integer duracionSeg,
                        LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.insertar("INSERT INTO tarifa (id_servicio, id_unidad, monto, unidad_cobro, duracion_seg,"
                        + " vigente_desde, vigente_hasta) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_tarifa",
                idServicio, idUnidad, monto, nulo(unidadCobro), duracionSeg, desde, hasta);
    }

    /** Modifica la tarifa y recalcula el precio de los paquetes que la incluyen (las proformas ya emitidas no cambian). */
    public int modificar(int id, int idServicio, Integer idUnidad, BigDecimal monto, String unidadCobro,
                         Integer duracionSeg, LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "UPDATE tarifa SET id_servicio = ?, id_unidad = ?, monto = ?, unidad_cobro = ?,"
                            + " duracion_seg = ?, vigente_desde = ?, vigente_hasta = ? WHERE id_tarifa = ?",
                    idServicio, idUnidad, monto, nulo(unidadCobro), duracionSeg, desde, hasta, id);
            DPaquete.recalcularPorTarifa(c, id);
            return n;
        });
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE tarifa SET activo = ? WHERE id_tarifa = ?", activo, id);
    }

    /** Paquetes activos que incluyen esta tarifa (nombres separados por coma) o null. */
    public String paquetesActivos(int id) throws SQLException {
        return Conexion.valor("SELECT string_agg(p.nombre || ' (ID ' || p.id_paquete || ')', ', ')"
                + " FROM paquete_item i JOIN paquete p ON p.id_paquete = i.id_paquete"
                + " WHERE i.id_tarifa = ? AND p.activo", id);
    }

    private static String nulo(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}
