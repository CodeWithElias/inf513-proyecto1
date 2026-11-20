package grupo13.negocio;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de un reporte del CU8. Solo datos: la presentación decide títulos, columnas y gráficos.
 * indicadores: {indicador, valor (o null si no hay datos), unidad, explicación}.
 * tablas: por código (p. ej. "porMes"); el orden de columnas de cada una lo documenta el método de NReporte.
 */
public final class Reporte {

    private final LocalDate desde;
    private final LocalDate hasta;
    private final List<String[]> indicadores;
    private final Map<String, List<String[]>> tablas = new LinkedHashMap<>();

    Reporte(LocalDate desde, LocalDate hasta, List<String[]> indicadores) {
        this.desde = desde;
        this.hasta = hasta;
        this.indicadores = indicadores;
    }

    Reporte tabla(String codigo, List<String[]> filas) {
        tablas.put(codigo, filas);
        return this;
    }

    /** Inicio del periodo (null = sin límite). */
    public LocalDate getDesde() {
        return desde;
    }

    /** Fin del periodo (null = sin límite). */
    public LocalDate getHasta() {
        return hasta;
    }

    public List<String[]> getIndicadores() {
        return indicadores;
    }

    public List<String[]> getTabla(String codigo) {
        return tablas.getOrDefault(codigo, List.of());
    }
}
