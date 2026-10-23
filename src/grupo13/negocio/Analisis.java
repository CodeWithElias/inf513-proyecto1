package grupo13.negocio;

import java.util.List;

/**
 * Resultado de un análisis de impacto (CU7). Solo datos; la presentación decide títulos y formato.
 * indicadores: {indicador, valor (número o null si no hay datos), unidad, explicación}.
 * detalle y resumen: filas cuyo contenido documenta cada método de NAnalisis.
 */
public final class Analisis {

    private final String[] cabecera;
    private final List<String[]> indicadores;
    private final List<String[]> detalle;
    private final List<String[]> resumen;

    Analisis(String[] cabecera, List<String[]> indicadores, List<String[]> detalle, List<String[]> resumen) {
        this.cabecera = cabecera;
        this.indicadores = indicadores;
        this.detalle = detalle;
        this.resumen = resumen;
    }

    /** Fila del objeto analizado (pauta, campaña o red social). */
    public String[] getCabecera() {
        return cabecera;
    }

    public List<String[]> getIndicadores() {
        return indicadores;
    }

    public List<String[]> getDetalle() {
        return detalle;
    }

    public List<String[]> getResumen() {
        return resumen;
    }
}
