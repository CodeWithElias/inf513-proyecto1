package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DPaquete;
import grupo13.datos.DTarifa;
import grupo13.util.Html;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** CU3 / RF06: paquetes publicitarios (varias tarifas agrupadas con un descuento). */
public class NPaquete {

    private static final String[] TITULOS = {"ID", "Nombre", "Descripción", "Descuento (%)", "Precio total (Bs)",
            "Tarifas", "Activo"};
    private static final String[] TITULOS_ITEM = {"ID tarifa", "Servicio", "Unidad", "Unidad de cobro",
            "Monto (Bs)", "Cantidad", "Subtotal (Bs)"};

    private final DPaquete datos = new DPaquete();
    private final DTarifa tarifa = new DTarifa();

    /** lisPaq["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Paquetes",
                Html.pagina("Paquetes Publicitarios", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosPaq["id"]: el paquete con el detalle de sus tarifas. */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        return Respuesta.exito("Paquete " + id, Html.pagina("Paquete " + id, detalle(id)));
    }

    /** insPaq["nombre","descripcion","descuento_%"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 150);
        String descripcion = Validar.textoOpcional(p.get(1), "descripcion", 500);
        BigDecimal descuento = p.get(2).isEmpty() ? BigDecimal.ZERO : Validar.porcentaje(p.get(2), "descuento");
        int id = datos.insertar(nombre, descripcion, descuento);
        return exito("El paquete fue registrado con ID " + id + ". Agregue tarifas con addPaq[\"" + id
                + "\",\"id_tarifa\",\"cantidad\"].", id);
    }

    /** modPaq["id","nombre","descripcion","descuento_%"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        String nombre = Validar.texto(p.get(1), "nombre", 150);
        String descripcion = Validar.textoOpcional(p.get(2), "descripcion", 500);
        BigDecimal descuento = p.get(3).isEmpty() ? BigDecimal.ZERO : Validar.porcentaje(p.get(3), "descuento");
        buscar(id);
        datos.modificar(id, nombre, descripcion, descuento);
        return exito("El paquete " + id + " fue modificado y su precio recalculado.", id);
    }

    /** addPaq["id_paquete","id_tarifa","cantidad"]: agrega la tarifa o actualiza su cantidad. */
    public Respuesta agregarTarifa(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        int idTarifa = Validar.id(p.get(1), "id_tarifa");
        int cantidad = Validar.id(p.get(2), "cantidad");
        buscar(id);
        Boolean tarifaActiva = tarifa.estaActivo(idTarifa);
        if (tarifaActiva == null) {
            throw new ComandoException("No existe la tarifa con ID " + idTarifa + ". Consulte lisTar[\"*\"].");
        }
        if (!tarifaActiva) {
            throw new ComandoException("La tarifa con ID " + idTarifa + " está deshabilitada.");
        }
        boolean yaEstaba = datos.contieneTarifa(id, idTarifa);
        datos.agregarItem(id, idTarifa, cantidad);
        return exitoDetalle((yaEstaba ? "Se actualizó la cantidad de la tarifa " : "Se agregó la tarifa ")
                + idTarifa + " al paquete " + id + ".", id);
    }

    /** quiPaq["id_paquete","id_tarifa"] */
    public Respuesta quitarTarifa(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        int idTarifa = Validar.id(p.get(1), "id_tarifa");
        buscar(id);
        if (datos.quitarItem(id, idTarifa) == 0) {
            throw new ComandoException("La tarifa " + idTarifa + " no forma parte del paquete " + id + ".");
        }
        return exitoDetalle("Se quitó la tarifa " + idTarifa + " del paquete " + id + ".", id);
    }

    /** eliPaq["id"]: baja lógica (ya no se puede cotizar). */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        if (!estado(id)) {
            throw new ComandoException("El paquete " + id + " ya está dado de baja.");
        }
        datos.cambiarEstado(id, false);
        return exito("El paquete " + id + " fue dado de baja. Puede reactivarlo con actPaq[\"" + id + "\"].", id);
    }

    /** actPaq["id"] */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_paquete");
        if (estado(id)) {
            throw new ComandoException("El paquete " + id + " ya está activo.");
        }
        datos.cambiarEstado(id, true);
        return exito("El paquete " + id + " fue reactivado.", id);
    }

    private String detalle(int id) throws Exception {
        return Html.tabla(TITULOS, List.<String[]>of(buscar(id)))
                + "<h3>Tarifas incluidas</h3>" + Html.tabla(TITULOS_ITEM, datos.items(id));
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe un paquete con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe un paquete con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }

    private Respuesta exitoDetalle(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito", "<p>" + Html.esc(mensaje) + "</p>" + detalle(id)));
    }
}
