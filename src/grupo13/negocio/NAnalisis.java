package grupo13.negocio;

import grupo13.datos.DCampania;
import grupo13.datos.DMetrica;
import grupo13.datos.DPauta;
import grupo13.datos.DRedSocial;
import grupo13.datos.DSeguidoresRed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU7 / RF13: análisis de métricas de impacto. Todos los indicadores se calculan aquí.
 * Definiciones (base = alcance si se conoce; si no, impresiones):
 *   tasa de interacción = interacciones / base × 100      CTR = clics / impresiones × 100
 *   frecuencia = impresiones / alcance                     tasa de viralidad = compartidos / base × 100
 *   CPM = costo / impresiones × 1000                       costo por clic = costo / clics
 *   costo por interacción = costo / interacciones          crecimiento = (actual − anterior) / anterior × 100
 * Si un denominador es 0 el indicador queda sin valor (null).
 * Para campañas y redes se usa la ÚLTIMA medición de cada pauta publicada (las mediciones son acumuladas).
 */
public class NAnalisis {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final String PCT = "%";
    private static final String BS = "Bs";

    // Índices: fila "última medición por pauta" (ver DMetrica)
    private static final int U_CANAL = 1;
    private static final int U_ALC = 5;

    private final DPauta pauta = new DPauta();
    private final DMetrica metrica = new DMetrica();
    private final DCampania campania = new DCampania();
    private final DRedSocial redSocial = new DRedSocial();
    private final DSeguidoresRed seguidores = new DSeguidoresRed();

    /**
     * Análisis de una pauta publicada. Cabecera: fila de la pauta (DPauta).
     * Detalle: historial de mediciones (id_metrica, fecha, alcance, impresiones, interacciones, clics, compartidos).
     */
    public Analisis analizarPauta(String idProgramacion) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            String[] pp = pauta.obtener(id);
            if (pp == null) {
                throw new NegocioException("No existe una pauta con ID " + id + ".");
            }
            List<String[]> ultimas = metrica.ultimasDos(id);
            if (ultimas.isEmpty()) {
                throw new NegocioException("La pauta " + id + " todavía no tiene mediciones registradas.");
            }
            long[] actual = valores(ultimas.get(0), 2);
            List<String[]> ind = indicadoresBasicos(actual);
            if (ultimas.size() == 2) {
                long[] anterior = valores(ultimas.get(1), 2);
                ind.add(fila("Crecimiento de impresiones", crecimiento(actual[1], anterior[1]), PCT,
                        "Respecto a la medición anterior (" + anterior[1] + " → " + actual[1] + ")."));
                ind.add(fila("Crecimiento de interacciones", crecimiento(actual[2], anterior[2]), PCT,
                        "Respecto a la medición anterior (" + anterior[2] + " → " + actual[2] + ")."));
            }
            return new Analisis(pp, ind, metrica.listarPorPauta(id), new ArrayList<>());
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /**
     * Análisis de una campaña. Cabecera: fila de la campaña (DCampania).
     * Detalle: última medición de cada pauta publicada (fila "última medición por pauta" de DMetrica).
     * Resumen por canal: {canal, pautas, alcance, impresiones, interacciones, clics, compartidos, tasa_interaccion, ctr}.
     */
    public Analisis analizarCampania(String idCampania) throws NegocioException {
        try {
            int id = Validar.id(idCampania, "id_campania");
            String[] cam = campania.obtener(id);
            if (cam == null) {
                throw new NegocioException("No existe una campaña con ID " + id + ".");
            }
            List<String[]> detalle = metrica.ultimasPorCampania(id, NPauta.PUBLICADA);
            if (detalle.isEmpty()) {
                throw new NegocioException("La campaña '" + cam[1] + "' todavía no tiene pautas publicadas.");
            }
            Acumulado total = new Acumulado();
            Map<String, Acumulado> porCanal = new LinkedHashMap<>();
            for (String[] f : detalle) {
                total.sumar(f);
                porCanal.computeIfAbsent(f[U_CANAL], k -> new Acumulado()).sumar(f);
            }
            BigDecimal costo = new BigDecimal(cam[7]);
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Pautas publicadas", BigDecimal.valueOf(total.pautas), "", total.medidas + " con mediciones."));
            ind.add(fila("Alcance total", BigDecimal.valueOf(total.v[0]), "personas",
                    "Suma del alcance conocido (" + total.conAlcance + " pauta(s) lo informan)."));
            ind.add(fila("Impresiones totales", BigDecimal.valueOf(total.v[1]), "", "Veces que se mostró la publicidad."));
            ind.add(fila("Interacciones totales", BigDecimal.valueOf(total.v[2]), "", "Me gusta, comentarios, reacciones, etc."));
            ind.add(fila("Clics totales", BigDecimal.valueOf(total.v[3]), "", ""));
            ind.add(fila("Compartidos totales", BigDecimal.valueOf(total.v[4]), "", ""));
            ind.addAll(indicadoresAgregados(total));
            ind.add(fila("Inversión de la campaña", costo, BS, "Total de la proforma aprobada."));
            ind.add(fila("CPM (costo por mil impresiones)", dividir(costo.multiply(MIL), total.v[1]), BS, "Inversión / impresiones × 1000."));
            ind.add(fila("Costo por clic", dividir(costo, total.v[3]), BS, "Inversión / clics."));
            ind.add(fila("Costo por interacción", dividir(costo, total.v[2]), BS, "Inversión / interacciones."));

            List<String[]> resumen = new ArrayList<>();
            for (Map.Entry<String, Acumulado> e : porCanal.entrySet()) {
                Acumulado a = e.getValue();
                resumen.add(new String[]{e.getKey(), String.valueOf(a.pautas), String.valueOf(a.v[0]), String.valueOf(a.v[1]),
                        String.valueOf(a.v[2]), String.valueOf(a.v[3]), String.valueOf(a.v[4]),
                        texto(porcentaje(a.v[2], a.base)), texto(porcentaje(a.v[3], a.v[1]))});
            }
            return new Analisis(cam, ind, detalle, resumen);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /**
     * Análisis de una red social institucional. Cabecera: fila de la red (DRedSocial).
     * Detalle: historial de seguidores (id_seguidores, red_social, fecha, seguidores), el más reciente primero.
     * Resumen: última medición de cada pauta publicada en la red (fila "última medición por pauta" de DMetrica).
     */
    public Analisis analizarRed(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            String[] red = redSocial.obtener(id);
            if (red == null) {
                throw new NegocioException("No existe una red social con ID " + id + ".");
            }
            List<String[]> historial = seguidores.listarPorCanal(id);
            List<String[]> pautas = metrica.ultimasPorCanal(id, NPauta.PUBLICADA);
            List<String[]> ind = new ArrayList<>();

            long actuales = 0;
            if (!historial.isEmpty()) {
                String[] ultimo = historial.get(0);
                String[] primero = historial.get(historial.size() - 1);
                actuales = Long.parseLong(ultimo[3]);
                long iniciales = Long.parseLong(primero[3]);
                long dias = ChronoUnit.DAYS.between(LocalDate.parse(primero[2]), LocalDate.parse(ultimo[2]));
                ind.add(fila("Seguidores actuales", BigDecimal.valueOf(actuales), "", "Última medición (" + fecha(ultimo[2]) + ")."));
                ind.add(fila("Crecimiento de seguidores", BigDecimal.valueOf(actuales - iniciales), "seguidores",
                        "Desde la primera medición (" + fecha(primero[2]) + ", " + iniciales + " seguidores)."));
                ind.add(fila("Crecimiento porcentual", crecimiento(actuales, iniciales), PCT, "Respecto a la primera medición."));
                ind.add(fila("Crecimiento diario promedio", dias == 0 ? null : dividir(BigDecimal.valueOf(actuales - iniciales), dias),
                        "seguidores/día", dias + " día(s) entre la primera y la última medición."));
            } else {
                ind.add(fila("Seguidores actuales", null, "", "Sin mediciones de seguidores (registre con insSeg)."));
            }

            Acumulado total = new Acumulado();
            for (String[] f : pautas) {
                total.sumar(f);
            }
            ind.add(fila("Pautas publicadas en la red", BigDecimal.valueOf(total.pautas), "", total.medidas + " con mediciones."));
            ind.add(fila("Interacciones con las pautas", BigDecimal.valueOf(total.v[2]), "", "Última medición de cada pauta."));
            ind.add(indicadoresAgregados(total).get(0));
            ind.add(fila("Interacción sobre la comunidad", porcentaje(total.v[2], actuales), PCT,
                    "Interacciones de las pautas / seguidores actuales × 100."));
            return new Analisis(red, ind, historial, pautas);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas de cálculo =====

    /** Tasa de interacción, CTR, frecuencia y viralidad. v = {alcance, impresiones, interacciones, clics, compartidos}. */
    private static List<String[]> indicadoresBasicos(long[] v) {
        long base = v[0] > 0 ? v[0] : v[1];
        String sobre = v[0] > 0 ? "alcance" : "impresiones (sin dato de alcance)";
        List<String[]> ind = new ArrayList<>();
        ind.add(fila("Tasa de interacción", porcentaje(v[2], base), PCT, "Interacciones / " + sobre + " × 100."));
        ind.add(fila("CTR (tasa de clics)", porcentaje(v[3], v[1]), PCT, "Clics / impresiones × 100."));
        ind.add(fila("Frecuencia", v[0] > 0 ? dividir(BigDecimal.valueOf(v[1]), v[0]) : null, "veces",
                "Impresiones / alcance: cuántas veces vio la publicidad cada persona."));
        ind.add(fila("Tasa de viralidad", porcentaje(v[4], base), PCT, "Compartidos / " + sobre + " × 100."));
        return ind;
    }

    /**
     * Suma de las últimas mediciones de varias pautas. Como algunas plataformas no informan el alcance (p. ej. YouTube),
     * la base de las tasas es, por pauta, su alcance si se conoce y si no sus impresiones; la frecuencia solo usa las
     * pautas que informan alcance.
     */
    private static final class Acumulado {
        final long[] v = new long[5];   // alcance, impresiones, interacciones, clics, compartidos
        long base;                      // Σ (alcance si > 0, si no impresiones)
        long impresionesConAlcance;     // impresiones de las pautas que informan alcance
        int pautas;
        int medidas;
        int conAlcance;

        void sumar(String[] filaUltima) {
            pautas++;
            if (filaUltima[U_ALC] == null) {
                return; // pauta publicada sin mediciones
            }
            medidas++;
            long[] m = valores(filaUltima, U_ALC);
            for (int i = 0; i < 5; i++) {
                v[i] += m[i];
            }
            if (m[0] > 0) {
                conAlcance++;
                base += m[0];
                impresionesConAlcance += m[1];
            } else {
                base += m[1];
            }
        }
    }

    /** Tasa de interacción, CTR, frecuencia y viralidad de un conjunto de pautas. */
    private static List<String[]> indicadoresAgregados(Acumulado a) {
        String sobre = "audiencia (alcance de cada pauta o, si no lo informa, sus impresiones)";
        List<String[]> ind = new ArrayList<>();
        ind.add(fila("Tasa de interacción", porcentaje(a.v[2], a.base), PCT, "Interacciones / " + sobre + " × 100."));
        ind.add(fila("CTR (tasa de clics)", porcentaje(a.v[3], a.v[1]), PCT, "Clics / impresiones × 100."));
        ind.add(fila("Frecuencia", a.v[0] > 0 ? dividir(BigDecimal.valueOf(a.impresionesConAlcance), a.v[0]) : null, "veces",
                "Impresiones / alcance, solo de las pautas que informan alcance."));
        ind.add(fila("Tasa de viralidad", porcentaje(a.v[4], a.base), PCT, "Compartidos / " + sobre + " × 100."));
        return ind;
    }

    /** Lee 5 valores enteros desde la posición 'desde' (alcance, impresiones, interacciones, clics, compartidos). */
    private static long[] valores(String[] f, int desde) {
        long[] v = new long[5];
        for (int i = 0; i < 5; i++) {
            v[i] = Long.parseLong(f[desde + i]);
        }
        return v;
    }

    private static BigDecimal porcentaje(long parte, long total) {
        return total == 0 ? null : BigDecimal.valueOf(parte).multiply(CIEN).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal crecimiento(long actual, long anterior) {
        return anterior == 0 ? null : BigDecimal.valueOf(actual - anterior).multiply(CIEN)
                .divide(BigDecimal.valueOf(anterior), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal a, long b) {
        return b == 0 ? null : a.divide(BigDecimal.valueOf(b), 2, RoundingMode.HALF_UP);
    }

    private static String fecha(String iso) {
        return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String texto(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }

    private static String[] fila(String indicador, BigDecimal valor, String unidad, String explicacion) {
        return new String[]{indicador, texto(valor), unidad, explicacion};
    }
}
