package grupo13.datos;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de paquete y paquete_item.
 * El precio_total lo calcula la capa de negocio; aquí solo se guarda junto con cada cambio (en transacción).
 * Fila: id_paquete, nombre, descripcion, descuento, precio_total, items, activo.
 */
public class DPaquete {

    private static final String SELECT =
            "SELECT p.id_paquete, p.nombre, p.descripcion, p.descuento, p.precio_total,"
            + " (SELECT count(*) FROM paquete_item i WHERE i.id_paquete = p.id_paquete) AS items, p.activo"
            + " FROM paquete p";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY p.id_paquete");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE p.nombre ILIKE ? OR p.descripcion ILIKE ? ORDER BY p.id_paquete", like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE p.id_paquete = ?", id);
    }

    /** Tarifas del paquete. Fila: id_tarifa, servicio, unidad, unidad_cobro, monto, cantidad. */
    public List<String[]> items(int id) throws SQLException {
        return Conexion.consultar("SELECT t.id_tarifa, s.nombre, uu.nombre, t.unidad_cobro, t.monto, i.cantidad"
                + " FROM paquete_item i"
                + " JOIN tarifa t ON t.id_tarifa = i.id_tarifa"
                + " JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = t.id_unidad"
                + " WHERE i.id_paquete = ? ORDER BY t.id_tarifa", id);
    }

    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("paquete", "id_paquete", id);
    }

    public int insertar(String nombre, String descripcion, BigDecimal descuento) throws SQLException {
        return Conexion.insertar("INSERT INTO paquete (nombre, descripcion, descuento) VALUES (?, ?, ?) RETURNING id_paquete",
                nombre, Conexion.nulo(descripcion), descuento);
    }

    public int modificar(int id, String nombre, String descripcion, BigDecimal descuento, BigDecimal precioTotal)
            throws SQLException {
        return Conexion.ejecutar("UPDATE paquete SET nombre = ?, descripcion = ?, descuento = ?, precio_total = ?"
                + " WHERE id_paquete = ?", nombre, Conexion.nulo(descripcion), descuento, precioTotal, id);
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE paquete SET activo = ? WHERE id_paquete = ?", activo, id);
    }

    /** Agrega la tarifa (o actualiza su cantidad) y guarda el nuevo precio del paquete. */
    public void agregarItem(int idPaquete, int idTarifa, int cantidad, BigDecimal precioTotal) throws SQLException {
        Conexion.enTransaccion(c -> {
            Conexion.ejecutar(c, "INSERT INTO paquete_item (id_paquete, id_tarifa, cantidad) VALUES (?, ?, ?)"
                    + " ON CONFLICT (id_paquete, id_tarifa) DO UPDATE SET cantidad = EXCLUDED.cantidad",
                    idPaquete, idTarifa, cantidad);
            Conexion.ejecutar(c, "UPDATE paquete SET precio_total = ? WHERE id_paquete = ?", precioTotal, idPaquete);
            return null;
        });
    }

    /** Quita la tarifa y guarda el nuevo precio del paquete. */
    public int quitarItem(int idPaquete, int idTarifa, BigDecimal precioTotal) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "DELETE FROM paquete_item WHERE id_paquete = ? AND id_tarifa = ?", idPaquete, idTarifa);
            Conexion.ejecutar(c, "UPDATE paquete SET precio_total = ? WHERE id_paquete = ?", precioTotal, idPaquete);
            return n;
        });
    }
}
