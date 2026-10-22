package grupo13.presentacion;

import grupo13.negocio.NProforma;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.ArrayList;
import java.util.List;

/** Presentación del CU3 – Proformas y cotizaciones: lisPro, mosPro, insPro, modPro, addDet, quiDet, aprPro, recPro. */
public class PProforma {

    private static final String[] COLUMNAS = {"ID", "Nro", "Emisión", "Válida hasta", "Cliente",
            "Subtotal (Bs)", "Descuento (Bs)", "Total (Bs)", "Estado", "Ítems"};
    private static final String[] COLUMNAS_ITEM = {"ID detalle", "Tipo", "Descripción", "Cantidad", "Frecuencia",
            "P. unitario (Bs)", "Subtotal (Bs)"};

    private final NProforma negocio = new NProforma();

    /** lisPro["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        List<String[]> filas = new ArrayList<>();
        for (String[] f : negocio.listar(Vista.filtro(p.get(0)))) {
            filas.add(new String[]{f[0], f[1], f[2], f[3], cliente(f), f[6], f[7], f[8], f[9], f[10]});
        }
        return Vista.listado("Listando las Proformas", "Listando las Proformas", COLUMNAS, filas);
    }

    /** mosPro["id"]: la cotización completa (es el documento que se "descarga" por correo). */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        String[] f = negocio.obtener(p.get(0));
        return Respuesta.exito("Cotización " + f[1], Html.pagina("Cotización " + f[1], cotizacion(p.get(0))));
    }

    /** insPro["id_empresa","id_visitante","dias_validez","descuento_Bs"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("Proforma creada. Agregue servicios con addDet[\"" + id
                + "\",\"id_tarifa\",\"\",\"cantidad\",\"frecuencia\"] o paquetes con addDet[\"" + id
                + "\",\"\",\"id_paquete\",\"cantidad\",\"frecuencia\"].", cotizacion(String.valueOf(id)));
    }

    /** modPro["id","descuento_Bs","fecha_validez"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("La proforma fue modificada y sus totales recalculados.", cotizacion(p.get(0)));
    }

    /** addDet["id_proforma","id_tarifa","id_paquete","cantidad","frecuencia"] */
    public Respuesta agregarDetalle(List<String> p) throws NegocioException {
        negocio.agregarItem(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4));
        return Vista.exito("Se agregó el ítem a la proforma.", cotizacion(p.get(0)));
    }

    /** quiDet["id_detalle"] */
    public Respuesta quitarDetalle(List<String> p) throws NegocioException {
        int idProforma = negocio.quitarItem(p.get(0));
        return Vista.exito("Se quitó el ítem " + p.get(0) + " de la proforma.", cotizacion(String.valueOf(idProforma)));
    }

    /** aprPro["id"] */
    public Respuesta aprobar(List<String> p) throws NegocioException {
        boolean esDeEmpresa = negocio.aprobar(p.get(0));
        String siguiente = esDeEmpresa
                ? " Genere la campaña con genCam[\"" + p.get(0) + "\",\"nombre\",\"fecha_inicio\",\"fecha_fin\"]."
                : " Al ser de un visitante, para contratar debe registrarse como empresa (insEmp) y cotizar de nuevo.";
        return Vista.exito("La proforma fue aprobada." + siguiente, cotizacion(p.get(0)));
    }

    /** recPro["id"] */
    public Respuesta rechazar(List<String> p) throws NegocioException {
        negocio.rechazar(p.get(0));
        return Vista.exito("La proforma fue rechazada.", cotizacion(p.get(0)));
    }

    /** Documento de cotización: cabecera, ítems y totales. */
    private String cotizacion(String id) throws NegocioException {
        String[] f = negocio.obtener(id);
        List<String[]> items = new ArrayList<>();
        for (String[] d : negocio.listarItems(id)) {
            items.add(new String[]{d[0], tipo(d), descripcion(d), d[7], d[8], d[9], d[10]});
        }
        return Html.ficha(new String[][]{
                        {"Nro. proforma", f[1]}, {"Fecha de emisión", f[2]}, {"Válida hasta", f[3]},
                        {"Cliente", cliente(f)}, {"Estado", f[9]}})
                + Html.subtitulo("Detalle") + Html.tabla(COLUMNAS_ITEM, items, false)
                + Html.ficha(new String[][]{{"Subtotal (Bs)", f[6]}, {"Descuento (Bs)", f[7]}, {"TOTAL (Bs)", f[8]}})
                + "<p><small>Canal 11 UAGRM — Cotización generada automáticamente. Precios en bolivianos.</small></p>";
    }

    private static String cliente(String[] f) {
        return f[4] != null ? "Empresa: " + f[4] : "Visitante: " + f[5];
    }

    private static String tipo(String[] d) {
        return d[1] != null ? "Tarifa" : "Paquete";
    }

    /** Tarifa: "Servicio - Unidad (unidad de cobro)"; paquete: su nombre. */
    private static String descripcion(String[] d) {
        if (d[1] == null) {
            return d[6];
        }
        return d[2] + (d[3] != null ? " - " + d[3] : "") + (d[4] != null ? " (" + d[4] + ")" : "");
    }
}
