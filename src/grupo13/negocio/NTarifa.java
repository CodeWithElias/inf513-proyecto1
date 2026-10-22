package grupo13.negocio;

import grupo13.datos.DPaquete;
import grupo13.datos.DServicio;
import grupo13.datos.DTarifa;
import grupo13.datos.DUnidad;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CU3 / RF05: tarifario publicitario.
 * Fila de tarifa (la que devuelve esta clase): id_tarifa, servicio, unidad, monto, unidad_cobro, duracion_seg,
 *   vigente_desde, vigente_hasta, activo, vigente_hoy ("t"/"f", calculado aquí).
 */
public class NTarifa {

    private final DTarifa datos = new DTarifa();
    private final DServicio servicio = new DServicio();
    private final DUnidad unidad = new DUnidad();
    private final DPaquete paquete = new DPaquete();

    /** Busca por servicio, tipo de servicio, unidad o unidad de cobro. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            List<String[]> filas = new ArrayList<>();
            for (String[] f : datos.listar(filtro)) {
                filas.add(conVigencia(f));
            }
            return filas;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idTarifa) throws NegocioException {
        try {
            return conVigencia(buscar(Validar.id(idTarifa, "id_tarifa")));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** idUnidad, unidadCobro, duracionSeg, desde y hasta pueden ir vacíos; fechas DD/MM/AAAA. */
    public int insertar(String idServicio, String idUnidad, String monto, String unidadCobro, String duracionSeg,
                        String desde, String hasta) throws NegocioException {
        try {
            Datos d = validar(idServicio, idUnidad, monto, unidadCobro, duracionSeg, desde, hasta);
            return datos.insertar(d.idServicio, d.idUnidad, d.monto, d.unidadCobro, d.duracion, d.desde, d.hasta);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Modifica la tarifa y recalcula el precio de los paquetes que la incluyen (las proformas emitidas no cambian). */
    public void modificar(String idTarifa, String idServicio, String idUnidad, String monto, String unidadCobro,
                          String duracionSeg, String desde, String hasta) throws NegocioException {
        try {
            int id = Validar.id(idTarifa, "id_tarifa");
            Datos d = validar(idServicio, idUnidad, monto, unidadCobro, duracionSeg, desde, hasta);
            buscar(id);

            Map<Integer, BigDecimal> precios = new LinkedHashMap<>();
            for (int idPaquete : datos.paquetesQueLaIncluyen(id)) {
                List<String[]> items = paquete.items(idPaquete);
                for (String[] item : items) {
                    if (Integer.parseInt(item[0]) == id) {
                        item[4] = d.monto.toPlainString(); // nuevo monto de esta tarifa
                    }
                }
                BigDecimal descuento = new BigDecimal(paquete.obtener(idPaquete)[3]);
                precios.put(idPaquete, NPaquete.calcularPrecio(items, descuento));
            }
            datos.modificar(id, d.idServicio, d.idUnidad, d.monto, d.unidadCobro, d.duracion, d.desde, d.hasta, precios);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Deshabilita; no se permite si forma parte de un paquete activo. */
    public void eliminar(String idTarifa) throws NegocioException {
        try {
            int id = Validar.id(idTarifa, "id_tarifa");
            if (!estado(id)) {
                throw new NegocioException("La tarifa " + id + " ya está deshabilitada.");
            }
            List<String[]> paquetes = datos.paquetesActivosQueLaIncluyen(id);
            if (!paquetes.isEmpty()) {
                String nombres = paquetes.stream().map(p -> p[1] + " (ID " + p[0] + ")").collect(Collectors.joining(", "));
                throw new NegocioException("No se puede deshabilitar la tarifa " + id + ": forma parte de los paquetes activos "
                        + nombres + ". Quítela de esos paquetes o délos de baja primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idTarifa) throws NegocioException {
        try {
            int id = Validar.id(idTarifa, "id_tarifa");
            if (estado(id)) {
                throw new NegocioException("La tarifa " + id + " ya está activa.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas usadas también por NPaquete y NProforma =====

    /** Regla de vigencia: activa y hoy dentro de [vigente_desde, vigente_hasta] (extremos opcionales). */
    static boolean esVigente(String[] fila) {
        LocalDate hoy = LocalDate.now();
        boolean activa = "t".equals(fila[8]);
        boolean desdeOk = fila[6] == null || !LocalDate.parse(fila[6]).isAfter(hoy);
        boolean hastaOk = fila[7] == null || !LocalDate.parse(fila[7]).isBefore(hoy);
        return activa && desdeOk && hastaOk;
    }

    private static String[] conVigencia(String[] f) {
        String[] r = Arrays.copyOf(f, f.length + 1);
        r[f.length] = esVigente(f) ? "t" : "f";
        return r;
    }

    /** Campos validados de una tarifa. */
    private static class Datos {
        int idServicio;
        Integer idUnidad;
        BigDecimal monto;
        String unidadCobro;
        Integer duracion;
        LocalDate desde;
        LocalDate hasta;
    }

    private Datos validar(String idServicio, String idUnidad, String monto, String unidadCobro, String duracionSeg,
                          String desde, String hasta) throws SQLException, NegocioException {
        Datos d = new Datos();
        d.idServicio = Validar.id(idServicio, "id_servicio");
        d.idUnidad = Validar.idOpcional(idUnidad, "id_unidad");
        d.monto = Validar.montoPositivo(monto, "monto");
        d.unidadCobro = Validar.textoOpcional(unidadCobro, "unidad_cobro", 50);
        d.duracion = Validar.enteroOpcional(duracionSeg, "duracion_seg");
        d.desde = Validar.fechaOpcional(desde, "vigente_desde");
        d.hasta = Validar.fechaOpcional(hasta, "vigente_hasta");
        Validar.rangoFechas(d.desde, d.hasta, "vigente_desde", "vigente_hasta");

        Boolean servicioActivo = servicio.estaActivo(d.idServicio);
        if (servicioActivo == null) {
            throw new NegocioException("No existe el servicio con ID " + d.idServicio + ". Consulte la lista de servicios.");
        }
        if (!servicioActivo) {
            throw new NegocioException("El servicio con ID " + d.idServicio + " está dado de baja.");
        }
        if (d.idUnidad != null) {
            Boolean unidadActiva = unidad.estaActivo(d.idUnidad);
            if (unidadActiva == null) {
                throw new NegocioException("No existe la unidad con ID " + d.idUnidad
                        + ". Consulte la lista de unidades o deje el campo vacío.");
            }
            if (!unidadActiva) {
                throw new NegocioException("La unidad con ID " + d.idUnidad + " está dada de baja.");
            }
        }
        return d;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una tarifa con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe una tarifa con ID " + id + ".");
        }
        return activo;
    }
}
