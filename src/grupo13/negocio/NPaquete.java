package grupo13.negocio;

import grupo13.datos.DPaquete;
import grupo13.datos.DTarifa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CU3 / RF06: paquetes publicitarios (varias tarifas agrupadas con un descuento %).
 * Regla de precio: precio_total = Σ(monto × cantidad) × (1 − descuento / 100), redondeado a 2 decimales.
 * Fila de paquete: id_paquete, nombre, descripcion, descuento, precio_total, items, activo.
 * Fila de ítem (la que devuelve esta clase): id_tarifa, servicio, unidad, unidad_cobro, monto, cantidad, subtotal.
 */
public class NPaquete {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final DPaquete datos = new DPaquete();
    private final DTarifa tarifa = new DTarifa();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idPaquete) throws NegocioException {
        try {
            return buscar(Validar.id(idPaquete, "id_paquete"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Tarifas incluidas, con el subtotal (monto × cantidad) de cada una. */
    public List<String[]> listarItems(String idPaquete) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            buscar(id);
            List<String[]> filas = new ArrayList<>();
            for (String[] item : datos.items(id)) {
                String[] r = Arrays.copyOf(item, item.length + 1);
                r[item.length] = subtotal(item).toPlainString();
                filas.add(r);
            }
            return filas;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** descuento en % (0 a 100; vacío = 0). */
    public int insertar(String nombre, String descripcion, String descuento) throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 150);
            String d = Validar.textoOpcional(descripcion, "descripcion", 500);
            BigDecimal desc = descuento.isEmpty() ? BigDecimal.ZERO : Validar.porcentaje(descuento, "descuento");
            return datos.insertar(n, d, desc);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Recalcula el precio con el nuevo descuento. */
    public void modificar(String idPaquete, String nombre, String descripcion, String descuento) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            String n = Validar.texto(nombre, "nombre", 150);
            String d = Validar.textoOpcional(descripcion, "descripcion", 500);
            BigDecimal desc = descuento.isEmpty() ? BigDecimal.ZERO : Validar.porcentaje(descuento, "descuento");
            buscar(id);
            datos.modificar(id, n, d, desc, calcularPrecio(datos.items(id), desc));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Agrega la tarifa (activa) o actualiza su cantidad. Devuelve true si ya estaba en el paquete. */
    public boolean agregarTarifa(String idPaquete, String idTarifa, String cantidad) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            int idTar = Validar.id(idTarifa, "id_tarifa");
            int cant = Validar.id(cantidad, "cantidad");
            String[] paq = buscar(id);
            String[] tar = tarifa.obtener(idTar);
            if (tar == null) {
                throw new NegocioException("No existe la tarifa con ID " + idTar + ". Consulte el tarifario.");
            }
            if (!"t".equals(tar[8])) {
                throw new NegocioException("La tarifa con ID " + idTar + " está deshabilitada.");
            }

            List<String[]> items = datos.items(id);
            boolean yaEstaba = false;
            for (String[] item : items) {
                if (Integer.parseInt(item[0]) == idTar) {
                    item[5] = String.valueOf(cant);
                    yaEstaba = true;
                }
            }
            if (!yaEstaba) {
                items.add(new String[]{String.valueOf(idTar), tar[1], tar[2], tar[4], tar[3], String.valueOf(cant)});
            }
            datos.agregarItem(id, idTar, cant, calcularPrecio(items, new BigDecimal(paq[3])));
            return yaEstaba;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void quitarTarifa(String idPaquete, String idTarifa) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            int idTar = Validar.id(idTarifa, "id_tarifa");
            String[] paq = buscar(id);
            List<String[]> items = datos.items(id);
            boolean quitada = items.removeIf(item -> Integer.parseInt(item[0]) == idTar);
            if (!quitada) {
                throw new NegocioException("La tarifa " + idTar + " no forma parte del paquete " + id + ".");
            }
            datos.quitarItem(id, idTar, calcularPrecio(items, new BigDecimal(paq[3])));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica (ya no se puede cotizar). */
    public void eliminar(String idPaquete) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            if (!estado(id)) {
                throw new NegocioException("El paquete " + id + " ya está dado de baja.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idPaquete) throws NegocioException {
        try {
            int id = Validar.id(idPaquete, "id_paquete");
            if (estado(id)) {
                throw new NegocioException("El paquete " + id + " ya está activo.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas de cálculo (usadas también por NTarifa) =====

    /** items: filas con monto en [4] y cantidad en [5]. */
    static BigDecimal calcularPrecio(List<String[]> items, BigDecimal descuentoPorcentaje) {
        BigDecimal suma = BigDecimal.ZERO;
        for (String[] item : items) {
            suma = suma.add(subtotal(item));
        }
        BigDecimal factor = BigDecimal.ONE.subtract(descuentoPorcentaje.divide(CIEN));
        return suma.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal subtotal(String[] item) {
        return new BigDecimal(item[4]).multiply(new BigDecimal(item[5]));
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un paquete con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe un paquete con ID " + id + ".");
        }
        return activo;
    }
}
