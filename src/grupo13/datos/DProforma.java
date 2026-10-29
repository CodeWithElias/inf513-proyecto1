package grupo13.datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Capa de datos de proforma y detalle_proforma.
 * Los totales y el número de proforma los calcula la capa de negocio; aquí solo se guardan (en transacción).
 * Fila: id_proforma, nro_proforma, fecha_emision, fecha_validez, razon_social_empresa, nombre_visitante,
 *       subtotal, descuento, total, estado, items, id_empresa, id_visitante.
 */
public class DProforma {

    private static final String SELECT =
            "SELECT p.id_proforma, p.nro_proforma, p.fecha_emision, p.fecha_validez, e.razon_social, v.nombre,"
            + " p.subtotal, p.descuento, p.total, p.estado,"
            + " (SELECT count(*) FROM detalle_proforma d WHERE d.id_proforma = p.id_proforma) AS items, p.id_empresa,"
            + " p.id_visitante"
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

    /**
     * Ítems de la proforma.
     * Fila: id_detalle, id_tarifa, servicio, unidad, unidad_cobro, id_paquete, paquete, cantidad, frecuencia,
     *       precio_unitario, subtotal.
     */
    public List<String[]> detalles(int id) throws SQLException {
        return Conexion.consultar("SELECT d.id_detalle, d.id_tarifa, s.nombre, uu.nombre, t.unidad_cobro,"
                + " d.id_paquete, pq.nombre, d.cantidad, d.frecuencia, d.precio_unitario, d.subtotal"
                + " FROM detalle_proforma d"
                + " LEFT JOIN tarifa t ON t.id_tarifa = d.id_tarifa"
                + " LEFT JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = t.id_unidad"
                + " LEFT JOIN paquete pq ON pq.id_paquete = d.id_paquete"
                + " WHERE d.id_proforma = ? ORDER BY d.id_detalle", id);
    }

    /**
     * Datos de un ítem para programar pautas:
     * {id_proforma, cantidad, id_tarifa, id_paquete, id_unidad_de_la_tarifa, tarifas_en_el_paquete (Σ cantidades)}.
     */
    public String[] detalleParaPauta(int idDetalle) throws SQLException {
        return Conexion.consultarUno("SELECT d.id_proforma, d.cantidad, d.id_tarifa, d.id_paquete, t.id_unidad,"
                + " (SELECT COALESCE(sum(i.cantidad), 0) FROM paquete_item i WHERE i.id_paquete = d.id_paquete)"
                + " FROM detalle_proforma d LEFT JOIN tarifa t ON t.id_tarifa = d.id_tarifa WHERE d.id_detalle = ?", idDetalle);
    }

    /** Fila del detalle: id_proforma, subtotal; o null si no existe. */
    public String[] obtenerDetalle(int idDetalle) throws SQLException {
        return Conexion.consultarUno("SELECT id_proforma, subtotal FROM detalle_proforma WHERE id_detalle = ?", idDetalle);
    }

    /**
     * Crea la proforma y le asigna el número que devuelve 'numerador' a partir del id generado
     * (la regla del formato la define la capa de negocio).
     */
    public int insertar(Integer idEmpresa, Integer idVisitante, LocalDate fechaValidez, BigDecimal descuento,
                        IntFunction<String> numerador) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int id = Conexion.insertar(c, "INSERT INTO proforma (nro_proforma, fecha_validez, descuento, id_empresa, id_visitante)"
                            + " VALUES ('TMP-' || substr(md5(random()::text), 1, 20), ?, ?, ?, ?) RETURNING id_proforma",
                    fechaValidez, descuento, idEmpresa, idVisitante);
            Conexion.ejecutar(c, "UPDATE proforma SET nro_proforma = ? WHERE id_proforma = ?", numerador.apply(id), id);
            return id;
        });
    }

    public int modificar(int id, BigDecimal descuento, LocalDate fechaValidez, BigDecimal total) throws SQLException {
        return Conexion.ejecutar("UPDATE proforma SET descuento = ?, fecha_validez = ?, total = ? WHERE id_proforma = ?",
                descuento, fechaValidez, total, id);
    }

    /** Inserta el ítem y guarda los nuevos totales de la proforma. Exactamente uno de idTarifa / idPaquete. */
    public int agregarDetalle(int idProforma, Integer idTarifa, Integer idPaquete, int cantidad, String frecuencia,
                              BigDecimal precioUnitario, BigDecimal subtotalItem,
                              BigDecimal subtotal, BigDecimal total) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int id = Conexion.insertar(c, "INSERT INTO detalle_proforma (id_proforma, id_tarifa, id_paquete, cantidad,"
                            + " frecuencia, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_detalle",
                    idProforma, idTarifa, idPaquete, cantidad, Conexion.nulo(frecuencia), precioUnitario, subtotalItem);
            guardarTotales(c, idProforma, subtotal, total);
            return id;
        });
    }

    /** Elimina el ítem y guarda los nuevos totales de la proforma. */
    public int quitarDetalle(int idDetalle, int idProforma, BigDecimal subtotal, BigDecimal total) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "DELETE FROM detalle_proforma WHERE id_detalle = ?", idDetalle);
            guardarTotales(c, idProforma, subtotal, total);
            return n;
        });
    }

    public int cambiarEstado(int id, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE proforma SET estado = ? WHERE id_proforma = ?", estado, id);
    }

    public boolean tieneCampania(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM campania WHERE id_proforma = ?", id) > 0;
    }

    private static void guardarTotales(java.sql.Connection c, int id, BigDecimal subtotal, BigDecimal total)
            throws SQLException {
        Conexion.ejecutar(c, "UPDATE proforma SET subtotal = ?, total = ? WHERE id_proforma = ?", subtotal, total, id);
    }
}
