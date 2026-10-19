package grupo13.datos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Capa de datos de proforma y detalle_proforma (cotizaciones, RF07).
 * subtotal = Σ subtotal de los detalles; total = max(subtotal − descuento, 0). Se recalcula en cada cambio.
 * Cada detalle guarda el precio_unitario del momento: si luego cambia la tarifa, la proforma emitida no cambia.
 */
public class DProforma {

    private static final String SELECT =
            "SELECT p.id_proforma, p.nro_proforma, to_char(p.fecha_emision, 'DD/MM/YYYY'),"
            + " to_char(p.fecha_validez, 'DD/MM/YYYY'),"
            + " COALESCE('Empresa: ' || e.razon_social, 'Visitante: ' || v.nombre) AS cliente,"
            + " p.subtotal, p.descuento, p.total,"
            + " CASE WHEN p.estado = 'pendiente' AND p.fecha_validez < CURRENT_DATE THEN 'vencida' ELSE p.estado END,"
            + " (SELECT count(*) FROM detalle_proforma d WHERE d.id_proforma = p.id_proforma) AS items"
            + " FROM proforma p"
            + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa"
            + " LEFT JOIN visitante v ON v.id_visitante = p.id_visitante";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY p.id_proforma");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE p.nro_proforma ILIKE ? OR p.estado ILIKE ? OR e.razon_social ILIKE ?"
                + " OR e.nit ILIKE ? OR v.nombre ILIKE ? ORDER BY p.id_proforma", like, like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE p.id_proforma = ?", id);
    }

    public List<String[]> detalles(int id) throws SQLException {
        return Conexion.consultar("SELECT d.id_detalle,"
                + " CASE WHEN d.id_tarifa IS NOT NULL THEN 'Tarifa' ELSE 'Paquete' END,"
                + " COALESCE(s.nombre || COALESCE(' - ' || uu.nombre, '') || COALESCE(' (' || t.unidad_cobro || ')', ''), pq.nombre),"
                + " d.cantidad, d.frecuencia, d.precio_unitario, d.subtotal"
                + " FROM detalle_proforma d"
                + " LEFT JOIN tarifa t ON t.id_tarifa = d.id_tarifa"
                + " LEFT JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = t.id_unidad"
                + " LEFT JOIN paquete pq ON pq.id_paquete = d.id_paquete"
                + " WHERE d.id_proforma = ? ORDER BY d.id_detalle", id);
    }

    /** {estado, fecha_validez (yyyy-MM-dd), id_empresa, cantidad de detalles} o null si no existe. */
    public String[] estado(int id) throws SQLException {
        return Conexion.consultarUno("SELECT p.estado, p.fecha_validez, p.id_empresa,"
                + " (SELECT count(*) FROM detalle_proforma d WHERE d.id_proforma = p.id_proforma)"
                + " FROM proforma p WHERE p.id_proforma = ?", id);
    }

    /** Crea la proforma y le asigna el número PRO-AAAA-NNNNN. Devuelve el id. */
    public int insertar(Integer idEmpresa, Integer idVisitante, LocalDate fechaValidez, BigDecimal descuento)
            throws SQLException {
        return Conexion.enTransaccion(c -> {
            int id = Conexion.insertar(c, "INSERT INTO proforma (nro_proforma, fecha_validez, descuento, id_empresa, id_visitante)"
                            + " VALUES ('TMP-' || substr(md5(random()::text), 1, 20), ?, ?, ?, ?) RETURNING id_proforma",
                    fechaValidez, descuento, idEmpresa, idVisitante);
            Conexion.ejecutar(c, "UPDATE proforma SET nro_proforma = 'PRO-' || to_char(fecha_emision, 'YYYY') || '-'"
                    + " || lpad(id_proforma::text, 5, '0') WHERE id_proforma = ?", id);
            recalcular(c, id);
            return id;
        });
    }

    public int modificar(int id, BigDecimal descuento, LocalDate fechaValidez) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "UPDATE proforma SET descuento = ?, fecha_validez = ? WHERE id_proforma = ?",
                    descuento, fechaValidez, id);
            recalcular(c, id);
            return n;
        });
    }

    /** Exactamente uno de idTarifa / idPaquete debe venir lleno. */
    public int agregarDetalle(int idProforma, Integer idTarifa, Integer idPaquete, int cantidad, String frecuencia,
                              BigDecimal precioUnitario) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int id = Conexion.insertar(c, "INSERT INTO detalle_proforma (id_proforma, id_tarifa, id_paquete, cantidad,"
                            + " frecuencia, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_detalle",
                    idProforma, idTarifa, idPaquete, cantidad, frecuencia.isEmpty() ? null : frecuencia,
                    precioUnitario, precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
            recalcular(c, idProforma);
            return id;
        });
    }

    /** Devuelve el id de la proforma del detalle, o null si el detalle no existe. */
    public Integer proformaDeDetalle(int idDetalle) throws SQLException {
        String v = Conexion.valor("SELECT id_proforma FROM detalle_proforma WHERE id_detalle = ?", idDetalle);
        return v == null ? null : Integer.parseInt(v);
    }

    public int quitarDetalle(int idDetalle, int idProforma) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "DELETE FROM detalle_proforma WHERE id_detalle = ?", idDetalle);
            recalcular(c, idProforma);
            return n;
        });
    }

    public int cambiarEstado(int id, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE proforma SET estado = ? WHERE id_proforma = ?", estado, id);
    }

    public boolean tieneCampania(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM campania WHERE id_proforma = ?", id) > 0;
    }

    private static void recalcular(Connection c, int id) throws SQLException {
        Conexion.ejecutar(c, "UPDATE proforma p SET subtotal = s.suma, total = GREATEST(s.suma - p.descuento, 0)"
                + " FROM (SELECT COALESCE(sum(subtotal), 0) AS suma FROM detalle_proforma WHERE id_proforma = ?) s"
                + " WHERE p.id_proforma = ?", id, id);
    }
}
