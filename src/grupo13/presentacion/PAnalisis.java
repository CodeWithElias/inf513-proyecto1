package grupo13.presentacion;

import grupo13.negocio.Analisis;
import grupo13.negocio.NAnalisis;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.ArrayList;
import java.util.List;

/** Presentación del CU7 – Análisis de impacto: anaPau, anaCam, anaRed. */
public class PAnalisis {

    private static final String[] COL_INDICADORES = {"Indicador", "Valor", "Cómo se calcula"};
    private static final String[] COL_PAUTA = {"ID", "Campaña", "Empresa", "Canal", "Tipo de canal", "Fecha y hora",
            "Material", "Tipo de material", "Estado", "URL publicación"};
    private static final String[] COL_MEDICIONES = {"ID", "Fecha", "Alcance", "Impresiones", "Interacciones", "Clics", "Compartidos"};
    private static final String[] COL_CAMPANIA = {"ID", "Nombre", "Inicio", "Fin", "Estado", "Proforma", "Empresa", "Inversión (Bs)"};
    private static final String[] COL_ULTIMAS = {"Pauta", "Canal", "Tipo de canal", "Fecha y hora", "Material", "Alcance",
            "Impresiones", "Interacciones", "Clics", "Compartidos", "Medido el"};
    private static final String[] COL_POR_CANAL = {"Canal", "Pautas", "Alcance", "Impresiones", "Interacciones", "Clics",
            "Compartidos", "Tasa de interacción (%)", "CTR (%)"};
    private static final String[] COL_RED = {"ID", "Red social", "Plataforma", "URL", "Responsable", "Unidad", "Seguidores"};
    private static final String[] COL_SEGUIDORES = {"ID", "Red social", "Fecha", "Seguidores"};

    private final NAnalisis negocio = new NAnalisis();

    /** anaPau["id_programacion"] */
    public Respuesta pauta(List<String> p) throws NegocioException {
        Analisis a = negocio.analizarPauta(p.get(0));
        String html = Html.tabla(COL_PAUTA, List.<String[]>of(a.getCabecera()), false)
                + Html.subtitulo("Indicadores (última medición)") + indicadores(a)
                + Html.subtitulo("Evolución de las mediciones") + Html.tabla(COL_MEDICIONES, a.getDetalle());
        return Respuesta.exito("Análisis de la pauta " + p.get(0), Html.pagina("Análisis de impacto - Pauta " + p.get(0), html));
    }

    /** anaCam["id_campania"]: rendimiento de la campaña (vista para Canal 11 y para el cliente). */
    public Respuesta campania(List<String> p) throws NegocioException {
        Analisis a = negocio.analizarCampania(p.get(0));
        String html = Html.tabla(COL_CAMPANIA, List.<String[]>of(a.getCabecera()), false)
                + Html.subtitulo("Indicadores de la campaña") + indicadores(a)
                + Html.subtitulo("Rendimiento por canal") + Html.tabla(COL_POR_CANAL, a.getResumen(), false)
                + Html.subtitulo("Detalle por pauta (última medición)") + Html.tabla(COL_ULTIMAS, a.getDetalle());
        return Respuesta.exito("Análisis de la campaña " + p.get(0),
                Html.pagina("Análisis de impacto - Campaña '" + a.getCabecera()[1] + "'", html));
    }

    /** anaRed["id_canal"]: crecimiento de la red social y rendimiento de las pautas publicadas en ella. */
    public Respuesta red(List<String> p) throws NegocioException {
        Analisis a = negocio.analizarRed(p.get(0));
        String html = Html.tabla(COL_RED, List.<String[]>of(a.getCabecera()), false)
                + Html.subtitulo("Indicadores de la red") + indicadores(a)
                + Html.subtitulo("Pautas publicadas en la red (última medición)") + Html.tabla(COL_ULTIMAS, a.getResumen())
                + Html.subtitulo("Historial de seguidores") + Html.tabla(COL_SEGUIDORES, a.getDetalle());
        return Respuesta.exito("Análisis de la red " + p.get(0),
                Html.pagina("Análisis de impacto - Red social '" + a.getCabecera()[1] + "'", html));
    }

    /** Valor + unidad en una sola columna; "-" si no hay datos para calcularlo. */
    private static String indicadores(Analisis a) {
        List<String[]> filas = new ArrayList<>();
        for (String[] i : a.getIndicadores()) {
            String valor = i[1] == null ? "sin datos" : i[1] + (i[2].isEmpty() ? "" : " " + i[2]);
            filas.add(new String[]{i[0], valor, i[3]});
        }
        return Html.tabla(COL_INDICADORES, filas, false);
    }
}
