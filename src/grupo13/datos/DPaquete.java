package grupo13.datos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de paquete y paquete_item (agrupación de tarifas, RF06).
 * precio_total = Σ(monto tarifa × cantidad) × (1 − descuento / 100); se recalcula en cada cambio.
 */
public class DPaquete {

    private static final String SELECT =
            "SELECT p.id_paquete, p.nombre, p.descripcion, p.descuento, p.precio_total,"
            + " (SELECT count(*) FROM paquete_item i WHERE i.id_paquete = p.id_paquete) AS items,"
            + " CASE WHEN p.activo THEN 'Sí' ELSE 'No' END"
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

    /** Tarifas incluidas en el paquete. */
    public List<String[]> items(int id) throws SQLException {
        return Conexion.consultar("SELECT t.id_tarifa, s.nombre, uu.nombre, t.unidad_cobro, t.monto, i.cantidad,"
                + " t.monto * i.cantidad AS subtotal"
                + " FROM paquete_item i"
                + " JOIN tarifa t ON t.id_tarifa = i.id_tarifa"
                + " JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + " LEFT JOIN unidad_universitaria uu ON uu.id_unidad = t.id_unidad"
                + " WHERE i.id_paquete = ? ORDER BY t.id_tarifa", id);
    }

    public Boolean estaActivo(int id) throws SQLException {
        return Conexion.estaActivo("paquete", "id_paquete", id);
    }

    public BigDecimal precio(int id) throws SQLException {
        return new BigDecimal(Conexion.valor("SELECT precio_total FROM paquete WHERE id_paquete = ?", id));
    }

    public int contarItems(int id) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM paquete_item WHERE id_paquete = ?", id);
    }

    public boolean contieneTarifa(int idPaquete, int idTarifa) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM paquete_item WHERE id_paquete = ? AND id_tarifa = ?",
                idPaquete, idTarifa) > 0;
    }

    public int insertar(String nombre, String descripcion, BigDecimal descuento) throws SQLException {
        return Conexion.insertar("INSERT INTO paquete (nombre, descripcion, descuento) VALUES (?, ?, ?) RETURNING id_paquete",
                nombre, nulo(descripcion), descuento);
    }

    public int modificar(int id, String nombre, String descripcion, BigDecimal descuento) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "UPDATE paquete SET nombre = ?, descripcion = ?, descuento = ? WHERE id_paquete = ?",
                    nombre, nulo(descripcion), descuento, id);
            recalcular(c, id);
            return n;
        });
    }

    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE paquete SET activo = ? WHERE id_paquete = ?", activo, id);
    }

    /** Agrega la tarifa al paquete o actualiza su cantidad si ya estaba. */
    public void agregarItem(int idPaquete, int idTarifa, int cantidad) throws SQLException {
        Conexion.enTransaccion(c -> {
            Conexion.ejecutar(c, "INSERT INTO paquete_item (id_paquete, id_tarifa, cantidad) VALUES (?, ?, ?)"
                    + " ON CONFLICT (id_paquete, id_tarifa) DO UPDATE SET cantidad = EXCLUDED.cantidad",
                    idPaquete, idTarifa, cantidad);
            recalcular(c, idPaquete);
            return null;
        });
    }

    public int quitarItem(int idPaquete, int idTarifa) throws SQLException {
        return Conexion.enTransaccion(c -> {
            int n = Conexion.ejecutar(c, "DELETE FROM paquete_item WHERE id_paquete = ? AND id_tarifa = ?", idPaquete, idTarifa);
            recalcular(c, idPaquete);
            return n;
        });
    }

    static void recalcular(Connection c, int idPaquete) throws SQLException {
        Conexion.ejecutar(c, "UPDATE paquete p SET precio_total = round(COALESCE((SELECT sum(t.monto * i.cantidad)"
                + " FROM paquete_item i JOIN tarifa t ON t.id_tarifa = i.id_tarifa WHERE i.id_paquete = p.id_paquete), 0)"
                + " * (1 - p.descuento / 100), 2) WHERE p.id_paquete = ?", idPaquete);
    }

    /** Recalcula todos los paquetes que contienen la tarifa (cuando cambia su monto). */
    static void recalcularPorTarifa(Connection c, int idTarifa) throws SQLException {
        for (String[] f : Conexion.consultar(c, "SELECT id_paquete FROM paquete_item WHERE id_tarifa = ?", idTarifa)) {
            recalcular(c, Integer.parseInt(f[0]));
        }
    }

    private static String nulo(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}
