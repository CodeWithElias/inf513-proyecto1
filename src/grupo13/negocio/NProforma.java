package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DEmpresa;
import grupo13.datos.DPaquete;
import grupo13.datos.DProforma;
import grupo13.datos.DTarifa;
import grupo13.datos.DVisitante;
import grupo13.util.Html;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * CU3 / RF07: proformas y cotizaciones.
 * Estados: pendiente → aprobada | rechazada. Una pendiente con fecha de validez pasada se muestra como "vencida".
 * Solo las proformas pendientes y vigentes se pueden editar o aprobar.
 */
public class NProforma {

    /** Días de validez por defecto cuando insPro recibe "". */
    private static final int DIAS_VALIDEZ = 15;

    private static final String[] TITULOS = {"ID", "Nro", "Emisión", "Válida hasta", "Cliente",
            "Subtotal (Bs)", "Descuento (Bs)", "Total (Bs)", "Estado", "Ítems"};
    private static final String[] TITULOS_DET = {"ID detalle", "Tipo", "Descripción", "Cantidad", "Frecuencia",
            "P. unitario (Bs)", "Subtotal (Bs)"};

    private final DProforma datos = new DProforma();
    private final DEmpresa empresa = new DEmpresa();
    private final DVisitante visitante = new DVisitante();
    private final DTarifa tarifa = new DTarifa();
    private final DPaquete paquete = new DPaquete();

    /** lisPro["*"]: busca por número, estado, empresa, NIT o visitante. */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Proformas",
                Html.pagina("Listando las Proformas", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosPro["id"]: la cotización completa (es el documento que se "descarga" por correo). */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_proforma");
        String[] f = buscar(id);
        return Respuesta.exito("Cotización " + f[1], Html.pagina("Cotización " + f[1], cotizacion(id)));
    }

    /** insPro["id_empresa","id_visitante","dias_validez","descuento_Bs"]: exactamente uno de empresa / visitante. */
    public Respuesta insertar(List<String> p) throws Exception {
        Integer idEmpresa = Validar.idOpcional(p.get(0), "id_empresa");
        Integer idVisitante = Validar.idOpcional(p.get(1), "id_visitante");
        int dias = p.get(2).isEmpty() ? DIAS_VALIDEZ : Validar.id(p.get(2), "dias_validez");
        BigDecimal descuento = p.get(3).isEmpty() ? BigDecimal.ZERO : Validar.monto(p.get(3), "descuento");

        if ((idEmpresa == null) == (idVisitante == null)) {
            throw new ComandoException("Indique una empresa o un visitante (uno de los dos, el otro vacío \"\").");
        }
        if (idEmpresa != null) {
            Boolean activa = empresa.estaActivo(idEmpresa);
            if (activa == null) {
                throw new ComandoException("No existe la empresa con ID " + idEmpresa + ". Consulte lisEmp[\"*\"].");
            }
            if (!activa) {
                throw new ComandoException("La empresa con ID " + idEmpresa + " está dada de baja.");
            }
        } else if (visitante.obtener(idVisitante) == null) {
            throw new ComandoException("No existe el visitante con ID " + idVisitante + ". Regístrelo con insVis.");
        }
        if (dias > 365) {
            throw new ComandoException("La validez máxima de una proforma es de 365 días.");
        }

        int id = datos.insertar(idEmpresa, idVisitante, LocalDate.now().plusDays(dias), descuento);
        return exitoCotizacion("Proforma creada. Agregue servicios con addDet[\"" + id
                + "\",\"id_tarifa\",\"\",\"cantidad\",\"frecuencia\"] o paquetes con addDet[\"" + id
                + "\",\"\",\"id_paquete\",\"cantidad\",\"frecuencia\"].", id);
    }

    /** modPro["id","descuento_Bs","fecha_validez"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_proforma");
        BigDecimal descuento = p.get(1).isEmpty() ? BigDecimal.ZERO : Validar.monto(p.get(1), "descuento");
        LocalDate validez = Validar.fecha(p.get(2), "fecha_validez");
        exigirPendiente(id, false);
        if (validez.isBefore(LocalDate.now())) {
            throw new ComandoException("La fecha de validez no puede ser anterior a hoy.");
        }
        datos.modificar(id, descuento, validez);
        return exitoCotizacion("La proforma fue modificada y sus totales recalculados.", id);
    }

    /** addDet["id_proforma","id_tarifa","id_paquete","cantidad","frecuencia"]: uno de tarifa / paquete. */
    public Respuesta agregarDetalle(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_proforma");
        Integer idTarifa = Validar.idOpcional(p.get(1), "id_tarifa");
        Integer idPaquete = Validar.idOpcional(p.get(2), "id_paquete");
        int cantidad = Validar.id(p.get(3), "cantidad");
        String frecuencia = Validar.textoOpcional(p.get(4), "frecuencia", 50);
        exigirPendiente(id, true);

        if ((idTarifa == null) == (idPaquete == null)) {
            throw new ComandoException("Indique una tarifa o un paquete (uno de los dos, el otro vacío \"\").");
        }
        BigDecimal precio;
        if (idTarifa != null) {
            if (tarifa.estaActivo(idTarifa) == null) {
                throw new ComandoException("No existe la tarifa con ID " + idTarifa + ". Consulte lisTar[\"*\"].");
            }
            if (!tarifa.esVigente(idTarifa)) {
                throw new ComandoException("La tarifa " + idTarifa + " no está vigente hoy (deshabilitada o fuera de su periodo).");
            }
            precio = tarifa.monto(idTarifa);
        } else {
            Boolean activo = paquete.estaActivo(idPaquete);
            if (activo == null) {
                throw new ComandoException("No existe el paquete con ID " + idPaquete + ". Consulte lisPaq[\"*\"].");
            }
            if (!activo) {
                throw new ComandoException("El paquete " + idPaquete + " está dado de baja.");
            }
            if (paquete.contarItems(idPaquete) == 0) {
                throw new ComandoException("El paquete " + idPaquete + " no tiene tarifas; agréguelas con addPaq.");
            }
            precio = paquete.precio(idPaquete);
        }
        datos.agregarDetalle(id, idTarifa, idPaquete, cantidad, frecuencia, precio);
        return exitoCotizacion("Se agregó el ítem a la proforma.", id);
    }

    /** quiDet["id_detalle"] */
    public Respuesta quitarDetalle(List<String> p) throws Exception {
        int idDetalle = Validar.id(p.get(0), "id_detalle");
        Integer id = datos.proformaDeDetalle(idDetalle);
        if (id == null) {
            throw new ComandoException("No existe un detalle de proforma con ID " + idDetalle + ".");
        }
        exigirPendiente(id, false);
        datos.quitarDetalle(idDetalle, id);
        return exitoCotizacion("Se quitó el ítem " + idDetalle + " de la proforma.", id);
    }

    /** aprPro["id"]: el cliente acepta la cotización (requisito para generar la campaña). */
    public Respuesta aprobar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_proforma");
        String[] e = exigirPendiente(id, true);
        if (Integer.parseInt(e[3]) == 0) {
            throw new ComandoException("La proforma no tiene ítems; agréguelos con addDet antes de aprobarla.");
        }
        datos.cambiarEstado(id, "aprobada");
        String sig = e[2] != null
                ? " Genere la campaña con genCam[\"" + id + "\",\"nombre\",\"fecha_inicio\",\"fecha_fin\"]."
                : " Al ser de un visitante, para contratar debe registrarse como empresa (insEmp) y cotizar de nuevo.";
        return exitoCotizacion("La proforma fue aprobada." + sig, id);
    }

    /** recPro["id"] */
    public Respuesta rechazar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_proforma");
        exigirPendiente(id, false);
        datos.cambiarEstado(id, "rechazada");
        return exitoCotizacion("La proforma fue rechazada.", id);
    }

    /**
     * Verifica que la proforma exista y esté pendiente (y vigente si 'vigente').
     * Devuelve {estado, fecha_validez, id_empresa, items}.
     */
    private String[] exigirPendiente(int id, boolean vigente) throws Exception {
        String[] e = datos.estado(id);
        if (e == null) {
            throw new ComandoException("No existe una proforma con ID " + id + ".");
        }
        if (!e[0].equals("pendiente")) {
            throw new ComandoException("La proforma " + id + " está " + Html.esc(e[0]) + "; solo se pueden cambiar las pendientes.");
        }
        if (vigente && LocalDate.parse(e[1]).isBefore(LocalDate.now())) {
            throw new ComandoException("La proforma " + id + " está vencida. Extienda su validez con modPro[\"" + id
                    + "\",\"descuento\",\"DD/MM/AAAA\"].");
        }
        return e;
    }

    private String cotizacion(int id) throws Exception {
        String[] f = buscar(id);
        return "<table cellpadding='4'>"
                + fila("Nro. proforma", f[1]) + fila("Fecha de emisión", f[2]) + fila("Válida hasta", f[3])
                + fila("Cliente", f[4]) + fila("Estado", f[8]) + "</table>"
                + "<h3>Detalle</h3>" + Html.tabla(TITULOS_DET, datos.detalles(id), false)
                + "<table cellpadding='4' style='margin-top:8px'>"
                + fila("Subtotal (Bs)", f[5]) + fila("Descuento (Bs)", f[6])
                + "<tr><td><b>TOTAL (Bs)</b></td><td><b>" + Html.esc(f[7]) + "</b></td></tr></table>"
                + "<p><small>Canal 11 UAGRM — Cotización generada automáticamente. Precios en bolivianos.</small></p>";
    }

    private static String fila(String etiqueta, String valor) {
        return "<tr><td><b>" + Html.esc(etiqueta) + "</b></td><td>" + Html.esc(valor) + "</td></tr>";
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe una proforma con ID " + id + ".");
        }
        return fila;
    }

    private Respuesta exitoCotizacion(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito", "<p>" + Html.esc(mensaje) + "</p>" + cotizacion(id)));
    }
}
