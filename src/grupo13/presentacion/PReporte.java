package grupo13.presentacion;

import grupo13.negocio.NReporte;
import grupo13.negocio.NegocioException;
import grupo13.negocio.Reporte;
import grupo13.presentacion.comandos.Respuesta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Presentación del CU8 – Reportes y estadísticas: tabGen, repCom, repSer, repOcu, repMat, repImp, repRed, repMon, repCli.
 * Todos reciben fecha_desde y fecha_hasta (DD/MM/AAAA o "*" = sin límite). Cada reporte combina indicadores,
 * tablas y gráficos de barras.
 */
public class PReporte {

    public static final String[] PARAMS_PERIODO = {"fecha_desde", "fecha_hasta"};

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COL_IND = {"Indicador", "Valor", "Cómo se calcula"};

    private final NReporte negocio = new NReporte();

    /** tabGen["desde","hasta"]: tablero general. */
    public Respuesta tablero(List<String> p) throws NegocioException {
        Reporte r = negocio.tablero(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.grafico("Ingresos por mes (Bs)", r.getTabla("porMes"), 0, 3, "Bs")
                + Html.grafico("Pautas publicadas por canal", r.getTabla("pautasPorCanal"), 0, 1, "")
                + Html.grafico("Impresiones por canal", r.getTabla("pautasPorCanal"), 0, 2, "");
        return respuesta("Tablero general", r, html);
    }

    /** repCom["desde","hasta"]: reporte comercial. */
    public Respuesta comercial(List<String> p) throws NegocioException {
        Reporte r = negocio.comercial(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.subtitulo("Proformas por estado")
                + Html.tabla(new String[]{"Estado", "Cantidad", "Monto (Bs)", "% del monto"}, r.getTabla("porEstado"), false)
                + Html.grafico("Monto por estado (Bs)", r.getTabla("porEstado"), 0, 2, "Bs")
                + Html.subtitulo("Evolución mensual")
                + Html.tabla(new String[]{"Mes", "Emitidas", "Aprobadas", "Ingresos (Bs)"}, r.getTabla("porMes"), false)
                + Html.grafico("Ingresos por mes (Bs)", r.getTabla("porMes"), 0, 3, "Bs")
                + Html.subtitulo("Principales clientes (por ingresos)")
                + Html.tabla(new String[]{"Tipo", "Cliente", "Proformas aprobadas", "Ingresos (Bs)"}, r.getTabla("topClientes"), false)
                + Html.grafico("Ingresos por cliente (Bs)", r.getTabla("topClientes"), 1, 3, "Bs");
        return respuesta("Reporte comercial", r, html);
    }

    /** repSer["desde","hasta"]: servicios y paquetes más vendidos. */
    public Respuesta servicios(List<String> p) throws NegocioException {
        Reporte r = negocio.servicios(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String[] col = {"Nombre", "Cantidad", "Monto (Bs)", "% del total"};
        String html = indicadores(r)
                + Html.subtitulo("Servicios") + Html.tabla(col, r.getTabla("servicios"), false)
                + Html.grafico("Ventas por servicio (Bs)", r.getTabla("servicios"), 0, 2, "Bs")
                + Html.subtitulo("Paquetes") + Html.tabla(col, r.getTabla("paquetes"), false)
                + Html.grafico("Ventas por paquete (Bs)", r.getTabla("paquetes"), 0, 2, "Bs");
        return respuesta("Servicios y paquetes vendidos", r, html);
    }

    /** repOcu["desde","hasta"]: ocupación de la pauta por canal. */
    public Respuesta ocupacion(List<String> p) throws NegocioException {
        Reporte r = negocio.ocupacion(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.subtitulo("Ocupación por canal")
                + Html.tabla(new String[]{"Canal", "Tipo", "Espacios", "Ocupados", "Disponibles", "Bloqueados", "Ocupación (%)",
                        "Pautas programadas", "Pautas publicadas"}, r.getTabla("porCanal"), false)
                + Html.grafico("Ocupación por canal (%)", r.getTabla("porCanal"), 0, 6, "%");
        return respuesta("Ocupación de la pauta", r, html);
    }

    /** repMat["desde","hasta"]: material publicitario y revisiones. */
    public Respuesta material(List<String> p) throws NegocioException {
        Reporte r = negocio.material(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.grafico("Material por estado", r.getTabla("porEstado"), 0, 1, "")
                + Html.subtitulo("Por tipo de material")
                + Html.tabla(new String[]{"Tipo", "Cargados", "Aprobados", "Rechazados", "En corrección", "Pendientes"},
                        r.getTabla("porTipo"), false)
                + Html.subtitulo("Revisiones por revisor")
                + Html.tabla(new String[]{"Revisor", "Revisiones", "Aprobó", "Rechazó", "Pidió corrección"},
                        r.getTabla("revisores"), false);
        return respuesta("Material publicitario", r, html);
    }

    /** repImp["desde","hasta"]: impacto y alcance. */
    public Respuesta impacto(List<String> p) throws NegocioException {
        Reporte r = negocio.impacto(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String[] col = {"Nombre", "Pautas", "Alcance", "Impresiones", "Interacciones", "Clics", "Tasa de interacción (%)"};
        String html = indicadores(r)
                + Html.subtitulo("Por canal") + Html.tabla(col, r.getTabla("porCanal"), false)
                + Html.grafico("Impresiones por canal", r.getTabla("porCanal"), 0, 3, "")
                + Html.grafico("Tasa de interacción por canal (%)", r.getTabla("porCanal"), 0, 6, "%")
                + Html.subtitulo("Por campaña") + Html.tabla(col, r.getTabla("porCampania"), false)
                + Html.subtitulo("Pautas con más interacción")
                + Html.tabla(new String[]{"Pauta", "Campaña", "Canal", "Material", "Impresiones", "Interacciones",
                        "Tasa de interacción (%)"}, r.getTabla("topPautas"), false);
        return respuesta("Impacto y alcance", r, html);
    }

    /** repRed["desde","hasta"]: crecimiento de las redes sociales. */
    public Respuesta redes(List<String> p) throws NegocioException {
        Reporte r = negocio.redes(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.subtitulo("Crecimiento por red social")
                + Html.tabla(new String[]{"Red social", "Plataforma", "Seguidores al inicio", "Seguidores al final",
                        "Crecimiento", "Crecimiento (%)", "Mediciones"}, r.getTabla("redes"), false)
                + Html.grafico("Seguidores actuales por red", r.getTabla("redes"), 0, 3, "")
                + Html.grafico("Crecimiento en el periodo", r.getTabla("redes"), 0, 4, "");
        return respuesta("Redes sociales", r, html);
    }

    /** repMon["desde","hasta"]: monitoreo de páginas no autorizadas. */
    public Respuesta monitoreo(List<String> p) throws NegocioException {
        Reporte r = negocio.monitoreo(Vista.filtro(p.get(0)), Vista.filtro(p.get(1)));
        String html = indicadores(r)
                + Html.subtitulo("Por plataforma")
                + Html.tabla(new String[]{"Plataforma", "Detecciones", "Páginas nuevas"}, r.getTabla("porPlataforma"), false)
                + Html.grafico("Detecciones por término", r.getTabla("porTermino"), 0, 1, "")
                + Html.grafico("Páginas por estado de seguimiento", r.getTabla("porEstado"), 0, 1, "")
                + Html.subtitulo("Páginas nuevas más recientes")
                + Html.tabla(new String[]{"ID", "Nombre", "URL", "Plataforma", "Detectada"}, r.getTabla("nuevas"), false);
        return respuesta("Monitoreo de páginas no autorizadas", r, html);
    }

    /** repCli["id_empresa","desde","hasta"]: vista del cliente. */
    public Respuesta cliente(List<String> p) throws NegocioException {
        Reporte r = negocio.cliente(p.get(0), Vista.filtro(p.get(1)), Vista.filtro(p.get(2)));
        String html = indicadores(r)
                + Html.subtitulo("Sus campañas")
                + Html.tabla(new String[]{"Campaña", "Estado", "Inversión (Bs)", "Pautas publicadas", "Impresiones",
                        "Interacciones", "Tasa de interacción (%)"}, r.getTabla("campanias"), false)
                + Html.grafico("Impresiones por campaña", r.getTabla("campanias"), 0, 4, "")
                + Html.grafico("Interacciones por campaña", r.getTabla("campanias"), 0, 5, "");
        return respuesta("Reporte para el cliente", r, html);
    }

    private static Respuesta respuesta(String titulo, Reporte r, String contenido) {
        String periodo = r.getDesde() == null && r.getHasta() == null ? "todo el historial"
                : (r.getDesde() == null ? "inicio" : r.getDesde().format(FECHA)) + " al "
                + (r.getHasta() == null ? "hoy" : r.getHasta().format(FECHA));
        return Respuesta.exito(titulo, Html.pagina(titulo, Html.parrafo("Periodo: " + periodo) + contenido));
    }

    private static String indicadores(Reporte r) {
        List<String[]> filas = new ArrayList<>();
        for (String[] i : r.getIndicadores()) {
            String valor = i[1] == null ? "sin datos" : i[1] + (i[2].isEmpty() ? "" : " " + i[2]);
            filas.add(new String[]{i[0], valor, i[3]});
        }
        return Html.tabla(COL_IND, filas, false);
    }
}
