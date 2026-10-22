package grupo13.presentacion;

import grupo13.negocio.NTarifa;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU3 – Tarifario: lisTar, mosTar, insTar, modTar, eliTar, actTar. */
public class PTarifa {

    public static final String[] PARAMS = {"id_servicio", "id_unidad", "monto", "unidad_cobro", "duracion_seg",
            "vigente_desde", "vigente_hasta"};

    private static final String[] COLUMNAS = {"ID", "Servicio", "Unidad", "Monto (Bs)", "Unidad de cobro",
            "Duración (s)", "Vigente desde", "Vigente hasta", "Activo", "Vigente hoy"};

    private final NTarifa negocio = new NTarifa();

    /** lisTar["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Tarifas", "Tarifario Publicitario", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosTar["id"] */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Tarifa " + p.get(0), COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** insTar["id_servicio","id_unidad","monto","unidad_cobro","duracion_seg","vigente_desde","vigente_hasta"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5), p.get(6));
        return Vista.exito("La tarifa fue registrada con ID " + id + ".", COLUMNAS, negocio.obtener(String.valueOf(id)));
    }

    /** modTar["id", ...mismos 7 campos...] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5), p.get(6), p.get(7));
        return Vista.exito("La tarifa " + p.get(0) + " fue modificada. Los paquetes que la incluyen fueron recalculados;"
                + " las proformas ya emitidas conservan su precio.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** eliTar["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("La tarifa " + p.get(0) + " fue deshabilitada. Puede reactivarla con actTar[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actTar["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("La tarifa " + p.get(0) + " fue reactivada.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
