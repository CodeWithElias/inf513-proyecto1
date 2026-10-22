package grupo13.datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Capa de datos de tarifa (precio de un servicio, opcionalmente por unidad universitaria).
 * Fila: id_tarifa, servicio, unidad, monto, unidad_cobro, duracion_seg, vigente_desde, vigente_hasta, activo.
 */
public class DTarifa {

    private static final String SELECT =
            "SELECT t.id_tarifa, s.nombre AS servicio, uu.nombre AS unidad, t.monto, t.unidad_cobro, t.duracion_seg,"
            + " t.vigente_desde, t.vigente_hasta, t.activo"
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

    public int insertar(int idServicio, Integer idUnidad, BigDecimal monto, String unidadCobro, Integer duracionSeg,
                        LocalDate desde, LocalDate hasta) throws SQLException {
        return Conexion.insertar("INSERT INTO tarifa (id_servicio, id_unidad, monto, unidad_cobro, duracion_seg,"
                        + " vigente_desde, vigente_hasta) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_tarifa",
                idServicio, idUnidad, monto, Conexion.nulo(unidadCobro), duracionSeg, desde, hasta);
    }

    /**
     * Modifica la tarifa y guarda, en la misma transacción, los nuevos precios de los paquetes que la incluyen
     * (calculados por la capa de negocio): id_paquete → precio_total.
     */
    public int modificar(int id, int idServicio, Integer idUnidad, BigDecimal monto, String unidadCobro,
                         Integer duracionSeg, LocalDate desde, LocalDate hasta,
                         Map<Integer, BigDecimal> preciosPaquetes) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "UPDATE tarifa SET id_servicio = ?, id_unidad = ?, monto = ?, unidad_cobro = ?,"
                            + " duracion_seg = ?, vigente_desde = ?, vigente_hasta = ? WHERE id_tarifa = ?",
                    idServicio, idUnidad, monto, Conexion.nulo(unidadCobro), duracionSeg, desde, hasta, id);
            for (Map.Entry<Integer, BigDecimal> e : preciosPaquetes.entrySet()) {
                Conexion.ejecutar(c, "UPDATE paquete SET precio_total = ? WHERE id_paquete = ?", e.getValue(), e.getKey());
            }
            return n;
        });
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE tarifa SET activo = ? WHERE id_tarifa = ?", activo, id);
    }

    /** IDs de todos los paquetes (activos o no) que incluyen la tarifa. */
    public List<Integer> paquetesQueLaIncluyen(int id) throws SQLException {
        return Conexion.ids("SELECT id_paquete FROM paquete_item WHERE id_tarifa = ? ORDER BY id_paquete", id);
    }

    /** Paquetes activos que incluyen la tarifa. Fila: id_paquete, nombre. */
    public List<String[]> paquetesActivosQueLaIncluyen(int id) throws SQLException {
        return Conexion.consultar("SELECT p.id_paquete, p.nombre FROM paquete_item i JOIN paquete p ON p.id_paquete = i.id_paquete"
                + " WHERE i.id_tarifa = ? AND p.activo ORDER BY p.id_paquete", id);
    }
}
