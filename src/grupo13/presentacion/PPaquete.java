package grupo13.presentacion;

import grupo13.negocio.NPaquete;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU3 – Paquetes: lisPaq, mosPaq, insPaq, modPaq, addPaq, quiPaq, eliPaq, actPaq. */
public class PPaquete {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Descripción", "Descuento (%)", "Precio total (Bs)",
            "Tarifas", "Activo"};
    private static final String[] COLUMNAS_ITEM = {"ID tarifa", "Servicio", "Unidad", "Unidad de cobro",
            "Monto (Bs)", "Cantidad", "Subtotal (Bs)"};

    private final NPaquete negocio = new NPaquete();

    /** lisPaq["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Paquetes", "Paquetes Publicitarios", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosPaq["id"]: el paquete con el detalle de sus tarifas. */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Respuesta.exito("Paquete " + p.get(0), Html.pagina("Paquete " + p.get(0), detalle(p.get(0))));
    }

    /** insPaq["nombre","descripcion","descuento_%"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("El paquete fue registrado con ID " + id + ". Agregue tarifas con addPaq[\"" + id
                + "\",\"id_tarifa\",\"cantidad\"].", COLUMNAS, negocio.obtener(String.valueOf(id)));
    }

    /** modPaq["id","nombre","descripcion","descuento_%"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("El paquete " + p.get(0) + " fue modificado y su precio recalculado.", COLUMNAS,
                negocio.obtener(p.get(0)));
    }

    /** addPaq["id_paquete","id_tarifa","cantidad"] */
    public Respuesta agregarTarifa(List<String> p) throws NegocioException {
        boolean yaEstaba = negocio.agregarTarifa(p.get(0), p.get(1), p.get(2));
        String mensaje = (yaEstaba ? "Se actualizó la cantidad de la tarifa " : "Se agregó la tarifa ")
                + p.get(1) + " al paquete " + p.get(0) + ".";
        return Vista.exito(mensaje, detalle(p.get(0)));
    }

    /** quiPaq["id_paquete","id_tarifa"] */
    public Respuesta quitarTarifa(List<String> p) throws NegocioException {
        negocio.quitarTarifa(p.get(0), p.get(1));
        return Vista.exito("Se quitó la tarifa " + p.get(1) + " del paquete " + p.get(0) + ".", detalle(p.get(0)));
    }

    /** eliPaq["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El paquete " + p.get(0) + " fue dado de baja. Puede reactivarlo con actPaq[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actPaq["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El paquete " + p.get(0) + " fue reactivado.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    private String detalle(String id) throws NegocioException {
        return Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(id)))
                + Html.subtitulo("Tarifas incluidas") + Html.tabla(COLUMNAS_ITEM, negocio.listarItems(id));
    }
}
