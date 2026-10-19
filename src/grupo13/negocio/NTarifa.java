package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DServicio;
import grupo13.datos.DTarifa;
import grupo13.datos.DUnidad;
import grupo13.util.Html;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** CU3 / RF05: tarifario publicitario. */
public class NTarifa {

    public static final String[] PARAMS = {"id_servicio", "id_unidad", "monto", "unidad_cobro", "duracion_seg",
            "vigente_desde", "vigente_hasta"};

    private static final String[] TITULOS = {"ID", "Servicio", "Unidad", "Monto (Bs)", "Unidad de cobro",
            "Duración (s)", "Vigente desde", "Vigente hasta", "Activo", "Vigente hoy"};

    private final DTarifa datos = new DTarifa();
    private final DServicio servicio = new DServicio();
    private final DUnidad unidad = new DUnidad();

    /** lisTar["*"]: busca por servicio, tipo de servicio, unidad o unidad de cobro. */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Tarifas",
                Html.pagina("Tarifario Publicitario", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosTar["id"] */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_tarifa");
        return Respuesta.exito("Tarifa " + id, Html.pagina("Tarifa " + id, Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }

    /** insTar["id_servicio","id_unidad","monto","unidad_cobro","duracion_seg","vigente_desde","vigente_hasta"] */
    public Respuesta insertar(List<String> p) throws Exception {
        Datos d = validar(p, 0);
        int id = datos.insertar(d.idServicio, d.idUnidad, d.monto, d.unidadCobro, d.duracion, d.desde, d.hasta);
        return exito("La tarifa fue registrada con ID " + id + ".", id);
    }

    /** modTar["id", ...mismos 7 campos...]; recalcula los paquetes que la incluyen. */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_tarifa");
        Datos d = validar(p, 1);
        buscar(id);
        datos.modificar(id, d.idServicio, d.idUnidad, d.monto, d.unidadCobro, d.duracion, d.desde, d.hasta);
        return exito("La tarifa " + id + " fue modificada. Los paquetes que la incluyen fueron recalculados;"
                + " las proformas ya emitidas conservan su precio.", id);
    }

    /** eliTar["id"]: deshabilita; no se permite si forma parte de un paquete activo. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_tarifa");
        if (!estado(id)) {
            throw new ComandoException("La tarifa " + id + " ya está deshabilitada.");
        }
        String paquetes = datos.paquetesActivos(id);
        if (paquetes != null) {
            throw new ComandoException("No se puede deshabilitar la tarifa " + id + ": forma parte de los paquetes activos "
                    + Html.esc(paquetes) + ". Quítela con quiPaq o dé de baja el paquete.");
        }
        datos.cambiarEstado(id, false);
        return exito("La tarifa " + id + " fue deshabilitada. Puede reactivarla con actTar[\"" + id + "\"].", id);
    }

    /** actTar["id"] */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_tarifa");
        if (estado(id)) {
            throw new ComandoException("La tarifa " + id + " ya está activa.");
        }
        datos.cambiarEstado(id, true);
        return exito("La tarifa " + id + " fue reactivada.", id);
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

    private Datos validar(List<String> p, int i) throws Exception {
        Datos d = new Datos();
        d.idServicio = Validar.id(p.get(i), "id_servicio");
        d.idUnidad = Validar.idOpcional(p.get(i + 1), "id_unidad");
        d.monto = Validar.montoPositivo(p.get(i + 2), "monto");
        d.unidadCobro = Validar.textoOpcional(p.get(i + 3), "unidad_cobro", 50);
        d.duracion = Validar.enteroOpcional(p.get(i + 4), "duracion_seg");
        d.desde = Validar.fechaOpcional(p.get(i + 5), "vigente_desde");
        d.hasta = Validar.fechaOpcional(p.get(i + 6), "vigente_hasta");
        Validar.rangoFechas(d.desde, d.hasta, "vigente_desde", "vigente_hasta");

        Boolean servicioActivo = servicio.estaActivo(d.idServicio);
        if (servicioActivo == null) {
            throw new ComandoException("No existe el servicio con ID " + d.idServicio + ". Consulte lisSer[\"*\"].");
        }
        if (!servicioActivo) {
            throw new ComandoException("El servicio con ID " + d.idServicio + " está dado de baja.");
        }
        if (d.idUnidad != null) {
            Boolean unidadActiva = unidad.estaActivo(d.idUnidad);
            if (unidadActiva == null) {
                throw new ComandoException("No existe la unidad con ID " + d.idUnidad + ". Consulte lisUni[\"*\"] o deje el campo vacío.");
            }
            if (!unidadActiva) {
                throw new ComandoException("La unidad con ID " + d.idUnidad + " está dada de baja.");
            }
        }
        return d;
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe una tarifa con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe una tarifa con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
