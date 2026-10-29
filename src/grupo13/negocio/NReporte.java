package grupo13.negocio;

import grupo13.datos.DEmpresa;
import grupo13.datos.DReporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * CU8 / RF13: reportes y estadísticas por periodo. La capa de datos solo entrega las filas del periodo; aquí se
 * agrupan y se calculan todos los indicadores (incluidas reglas como "proforma vencida" o la tasa de conversión).
 * Periodo: desde y hasta en DD/MM/AAAA; null = sin límite.
 * Definiciones:
 *   tasa de conversión = aprobadas / (aprobadas + rechazadas + vencidas) × 100   (las pendientes vigentes no cuentan)
 *   ticket promedio = ingresos aprobados / proformas aprobadas
 *   ocupación = espacios ocupados / espacios del calendario × 100
 *   tasa de cancelación = pautas canceladas / pautas del periodo × 100
 *   tasa de aprobación de material = aprobados / (aprobados + rechazados) × 100
 *   tasa de interacción = interacciones / audiencia (alcance de cada pauta o, si no lo informa, sus impresiones) × 100
 */
public class NReporte {

    private static final LocalDate MINIMO = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAXIMO = LocalDate.of(2999, 12, 31);
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MIL = new BigDecimal("1000");
    private static final int TOP = 10;

    private final DReporte datos = new DReporte();
    private final DEmpresa empresa = new DEmpresa();

    // =====================================================================================================
    // Comercial

    /**
     * Tablas: "porEstado" {estado, cantidad, monto, porcentaje_del_monto};
     *         "porMes" {mes (AAAA-MM), emitidas, aprobadas, ingresos};
     *         "topClientes" {tipo (empresa|visitante), cliente, aprobadas, ingresos} (los 10 de mayor ingreso).
     */
    public Reporte comercial(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            return comercial(p, datos.proformas(p[0], p[1], null));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private Reporte comercial(LocalDate[] p, List<String[]> proformas) {
        Map<String, BigDecimal[]> porEstado = new LinkedHashMap<>(); // estado → {cantidad, monto}
        for (String e : new String[]{NProforma.APROBADA, NProforma.PENDIENTE, NProforma.VENCIDA, NProforma.RECHAZADA}) {
            porEstado.put(e, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        Map<String, BigDecimal[]> porMes = new TreeMap<>();        // mes → {emitidas, aprobadas, ingresos}
        Map<String, BigDecimal[]> porCliente = new LinkedHashMap<>(); // "tipo|nombre" → {aprobadas, ingresos}
        Set<String> clientes = new HashSet<>();
        BigDecimal cotizado = BigDecimal.ZERO;

        for (String[] f : proformas) {
            BigDecimal total = new BigDecimal(f[5]);
            String estado = estadoEfectivo(f[4], f[3]);
            sumar(porEstado.computeIfAbsent(estado, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}), BigDecimal.ONE, total);
            String mes = f[2].substring(0, 7);
            BigDecimal[] m = porMes.computeIfAbsent(mes, k -> ceros(3));
            m[0] = m[0].add(BigDecimal.ONE);
            String cliente = (f[6] != null ? "empresa|" + f[6] : "visitante|" + f[7]);
            clientes.add(cliente);
            cotizado = cotizado.add(total);
            if (estado.equals(NProforma.APROBADA)) {
                m[1] = m[1].add(BigDecimal.ONE);
                m[2] = m[2].add(total);
                sumar(porCliente.computeIfAbsent(cliente, k -> ceros(2)), BigDecimal.ONE, total);
            }
        }

        BigDecimal[] aprob = porEstado.get(NProforma.APROBADA);
        BigDecimal cerradas = aprob[0].add(porEstado.get(NProforma.RECHAZADA)[0]).add(porEstado.get(NProforma.VENCIDA)[0]);
        List<String[]> ind = new ArrayList<>();
        ind.add(fila("Proformas emitidas", BigDecimal.valueOf(proformas.size()), "", "Emitidas en el periodo."));
        ind.add(fila("Monto cotizado", cotizado, "Bs", "Suma del total de todas las proformas emitidas."));
        ind.add(fila("Ingresos (proformas aprobadas)", aprob[1], "Bs", aprob[0].toPlainString() + " proforma(s) aprobada(s)."));
        ind.add(fila("Tasa de conversión", porcentaje(aprob[0], cerradas), "%",
                "Aprobadas / (aprobadas + rechazadas + vencidas). Las pendientes vigentes aún no cuentan."));
        ind.add(fila("Ticket promedio", dividir(aprob[1], aprob[0]), "Bs", "Ingresos / proformas aprobadas."));
        ind.add(fila("Clientes que cotizaron", BigDecimal.valueOf(clientes.size()), "", "Empresas y visitantes distintos."));

        List<String[]> estados = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : porEstado.entrySet()) {
            estados.add(new String[]{e.getKey(), entero(e.getValue()[0]), dinero(e.getValue()[1]),
                    texto(porcentaje(e.getValue()[1], cotizado))});
        }
        List<String[]> meses = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : porMes.entrySet()) {
            meses.add(new String[]{e.getKey(), entero(e.getValue()[0]), entero(e.getValue()[1]), dinero(e.getValue()[2])});
        }
        List<String[]> top = new ArrayList<>();
        porCliente.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
                .limit(TOP)
                .forEach(e -> {
                    String[] k = e.getKey().split("\\|", 2);
                    top.add(new String[]{k[0], k[1], entero(e.getValue()[0]), dinero(e.getValue()[1])});
                });
        return new Reporte(p[2] == null ? null : p[0], p[3] == null ? null : p[1], ind)
                .tabla("porEstado", estados).tabla("porMes", meses).tabla("topClientes", top);
    }

    // =====================================================================================================
    // Servicios y paquetes vendidos

    /** Tablas: "servicios" y "paquetes" {nombre, cantidad, monto, porcentaje_del_total} ordenadas por monto. */
    public Reporte servicios(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            Map<String, BigDecimal[]> servicios = new LinkedHashMap<>();
            Map<String, BigDecimal[]> paquetes = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (String[] f : datos.itemsVendidos(p[0], p[1], NProforma.APROBADA)) {
                BigDecimal monto = new BigDecimal(f[3]);
                total = total.add(monto);
                sumar((f[0].equals("tarifa") ? servicios : paquetes).computeIfAbsent(f[1], k -> ceros(2)),
                        new BigDecimal(f[2]), monto);
            }
            List<String[]> tServ = ranking(servicios, total);
            List<String[]> tPaq = ranking(paquetes, total);
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Total vendido", total, "Bs", "Ítems de proformas aprobadas emitidas en el periodo."));
            ind.add(new String[]{"Servicio más vendido", tServ.isEmpty() ? null : tServ.get(0)[0], "",
                    tServ.isEmpty() ? "Sin ventas de servicios sueltos." : "Por monto: Bs " + tServ.get(0)[2] + "."});
            ind.add(new String[]{"Paquete más vendido", tPaq.isEmpty() ? null : tPaq.get(0)[0], "",
                    tPaq.isEmpty() ? "Sin ventas de paquetes." : "Por monto: Bs " + tPaq.get(0)[2] + "."});
            return reporte(p, ind).tabla("servicios", tServ).tabla("paquetes", tPaq);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Ocupación de la pauta

    /** Tabla "porCanal": {canal, tipo, espacios, ocupados, disponibles, bloqueados, ocupacion_%, programadas, publicadas}. */
    public Reporte ocupacion(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            Map<String, long[]> canales = new TreeMap<>(); // canal → {espacios, ocupados, disponibles, bloqueados, programadas, publicadas}
            Map<String, String> tipos = new LinkedHashMap<>();
            long[] total = new long[6];
            for (String[] f : datos.espacios(p[0], p[1])) {
                long[] c = canales.computeIfAbsent(f[0], k -> new long[6]);
                tipos.put(f[0], f[1]);
                int i = f[2].equals(NEspacio.OCUPADO) ? 1 : f[2].equals(NEspacio.BLOQUEADO) ? 3 : 2;
                c[0]++;
                c[i]++;
                total[0]++;
                total[i]++;
            }
            int canceladas = 0;
            List<String[]> pautas = datos.pautas(p[0], p[1]);
            for (String[] f : pautas) {
                if (f[1].equals(NPauta.CANCELADA)) {
                    canceladas++;
                    continue;
                }
                long[] c = canales.computeIfAbsent(f[2], k -> new long[6]);
                tipos.putIfAbsent(f[2], f[3]);
                int i = f[1].equals(NPauta.PUBLICADA) ? 5 : 4;
                c[i]++;
                total[i]++;
            }
            List<String[]> filas = new ArrayList<>();
            for (Map.Entry<String, long[]> e : canales.entrySet()) {
                long[] c = e.getValue();
                filas.add(new String[]{e.getKey(), tipos.get(e.getKey()), String.valueOf(c[0]), String.valueOf(c[1]),
                        String.valueOf(c[2]), String.valueOf(c[3]), texto(porcentaje(c[1], c[0])), String.valueOf(c[4]),
                        String.valueOf(c[5])});
            }
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Espacios en el calendario", BigDecimal.valueOf(total[0]), "", total[3] + " bloqueado(s)."));
            ind.add(fila("Ocupación", porcentaje(total[1], total[0]), "%", "Espacios ocupados / espacios del calendario."));
            ind.add(fila("Pautas programadas", BigDecimal.valueOf(total[4]), "", "Pendientes de emisión."));
            ind.add(fila("Pautas publicadas", BigDecimal.valueOf(total[5]), "", ""));
            ind.add(fila("Pautas canceladas", BigDecimal.valueOf(canceladas), "", ""));
            ind.add(fila("Tasa de cancelación", porcentaje(canceladas, pautas.size()), "%", "Canceladas / pautas del periodo."));
            return reporte(p, ind).tabla("porCanal", filas);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Material publicitario

    /**
     * Tablas: "porEstado" {estado, cantidad}; "porTipo" {tipo, cargados, aprobados, rechazados, en_correccion, pendientes};
     *         "revisores" {revisor, revisiones, aprobados, rechazados, correcciones}.
     */
    public Reporte material(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            String[] estados = {NMaterial.PENDIENTE, NMaterial.APROBADO, NMaterial.CORRECCION, NMaterial.RECHAZADO, NMaterial.RETIRADO};
            Map<String, Long> porEstado = new LinkedHashMap<>();
            for (String e : estados) {
                porEstado.put(e, 0L);
            }
            Map<String, long[]> porTipo = new TreeMap<>(); // tipo → {cargados, aprobados, rechazados, correccion, pendientes}
            long versiones = 0;
            List<String[]> materiales = datos.materiales(p[0], p[1]);
            for (String[] f : materiales) {
                porEstado.merge(f[2], 1L, Long::sum);
                long[] t = porTipo.computeIfAbsent(f[1], k -> new long[5]);
                t[0]++;
                if (f[2].equals(NMaterial.APROBADO)) t[1]++;
                if (f[2].equals(NMaterial.RECHAZADO)) t[2]++;
                if (f[2].equals(NMaterial.CORRECCION)) t[3]++;
                if (f[2].equals(NMaterial.PENDIENTE)) t[4]++;
                versiones += Long.parseLong(f[3]);
            }
            Map<String, long[]> revisores = new TreeMap<>(); // revisor → {revisiones, aprobados, rechazados, correcciones}
            for (String[] f : datos.revisiones(p[0], p[1])) {
                long[] r = revisores.computeIfAbsent(f[1] == null ? "(sin revisor)" : f[1], k -> new long[4]);
                r[0]++;
                if (NMaterial.APROBADO.equals(f[0])) r[1]++;
                if (NMaterial.RECHAZADO.equals(f[0])) r[2]++;
                if (NMaterial.CORRECCION.equals(f[0])) r[3]++;
            }
            long aprobados = porEstado.get(NMaterial.APROBADO);
            long rechazados = porEstado.get(NMaterial.RECHAZADO);
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Material cargado", BigDecimal.valueOf(materiales.size()), "", "Cargado en el periodo."));
            ind.add(fila("Tasa de aprobación", porcentaje(aprobados, aprobados + rechazados), "%", "Aprobados / (aprobados + rechazados)."));
            ind.add(fila("Versiones promedio", materiales.isEmpty() ? null : dividir(BigDecimal.valueOf(versiones), materiales.size()),
                    "por material", "Cuántas veces se subió cada material (1 = sin correcciones)."));
            ind.add(fila("Pendientes de revisión", BigDecimal.valueOf(porEstado.get(NMaterial.PENDIENTE)), "", "Del material cargado en el periodo."));
            List<String[]> tEstado = new ArrayList<>();
            porEstado.forEach((k, v) -> tEstado.add(new String[]{k, String.valueOf(v)}));
            List<String[]> tTipo = new ArrayList<>();
            porTipo.forEach((k, v) -> tTipo.add(new String[]{k, String.valueOf(v[0]), String.valueOf(v[1]), String.valueOf(v[2]),
                    String.valueOf(v[3]), String.valueOf(v[4])}));
            List<String[]> tRev = new ArrayList<>();
            revisores.forEach((k, v) -> tRev.add(new String[]{k, String.valueOf(v[0]), String.valueOf(v[1]), String.valueOf(v[2]),
                    String.valueOf(v[3])}));
            return reporte(p, ind).tabla("porEstado", tEstado).tabla("porTipo", tTipo).tabla("revisores", tRev);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Impacto y alcance

    /**
     * Tablas: "porCanal" y "porCampania" {nombre, pautas, alcance, impresiones, interacciones, clics, tasa_interaccion_%};
     *         "topPautas" {id_programacion, campania, canal, archivo, impresiones, interacciones, tasa_interaccion_%} (5 mejores).
     */
    public Reporte impacto(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            return impacto(p, datos.impacto(p[0], p[1], NPauta.PUBLICADA, null));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private Reporte impacto(LocalDate[] p, List<String[]> filas) {
        Suma total = new Suma();
        Map<String, Suma> porCanal = new TreeMap<>();
        Map<String, Suma> porCampania = new LinkedHashMap<>();
        List<String[]> conMedicion = new ArrayList<>();
        for (String[] f : filas) {
            total.sumar(f);
            porCanal.computeIfAbsent(f[3], k -> new Suma()).sumar(f);
            porCampania.computeIfAbsent(f[1], k -> new Suma()).sumar(f);
            if (f[5] != null) {
                conMedicion.add(f);
            }
        }
        conMedicion.sort(Comparator.comparingLong((String[] f) -> Long.parseLong(f[7])).reversed());
        List<String[]> top = new ArrayList<>();
        for (String[] f : conMedicion.subList(0, Math.min(5, conMedicion.size()))) {
            Suma s = new Suma();
            s.sumar(f);
            top.add(new String[]{f[0], f[1], f[3], f[4], f[6], f[7], texto(s.tasa())});
        }
        List<String[]> ind = new ArrayList<>();
        ind.add(fila("Pautas publicadas", BigDecimal.valueOf(total.pautas), "", total.medidas + " con mediciones."));
        ind.add(fila("Alcance", BigDecimal.valueOf(total.v[0]), "personas", "Suma del alcance conocido."));
        ind.add(fila("Impresiones", BigDecimal.valueOf(total.v[1]), "", ""));
        ind.add(fila("Interacciones", BigDecimal.valueOf(total.v[2]), "", ""));
        ind.add(fila("Clics", BigDecimal.valueOf(total.v[3]), "", ""));
        ind.add(fila("Tasa de interacción", total.tasa(), "%",
                "Interacciones / audiencia (alcance de cada pauta o, si no lo informa, sus impresiones)."));
        return reporte(p, ind).tabla("porCanal", sumas(porCanal)).tabla("porCampania", sumas(porCampania)).tabla("topPautas", top);
    }

    // =====================================================================================================
    // Redes sociales

    /** Tabla "redes": {red, plataforma, seguidores_inicio, seguidores_fin, crecimiento, crecimiento_%, mediciones}. */
    public Reporte redes(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            Map<String, String[]> redes = new LinkedHashMap<>(); // id → {red, plataforma, inicio, fin, mediciones}
            for (String[] f : datos.seguidores(p[0], p[1])) {
                String[] r = redes.get(f[0]);
                if (r == null) {
                    redes.put(f[0], new String[]{f[1], f[2], f[4], f[4], "1"});
                } else {
                    r[3] = f[4];
                    r[4] = String.valueOf(Integer.parseInt(r[4]) + 1);
                }
            }
            List<String[]> filas = new ArrayList<>();
            long totalFin = 0;
            long crecimiento = 0;
            String mejor = null;
            BigDecimal mejorPct = null;
            for (String[] r : redes.values()) {
                long ini = Long.parseLong(r[2]);
                long fin = Long.parseLong(r[3]);
                BigDecimal pct = porcentaje(fin - ini, ini);
                totalFin += fin;
                crecimiento += fin - ini;
                if (pct != null && (mejorPct == null || pct.compareTo(mejorPct) > 0)) {
                    mejorPct = pct;
                    mejor = r[0];
                }
                filas.add(new String[]{r[0], r[1], r[2], r[3], String.valueOf(fin - ini), texto(pct), r[4]});
            }
            filas.sort((a, b) -> Long.compare(Long.parseLong(b[4]), Long.parseLong(a[4])));
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Redes con mediciones", BigDecimal.valueOf(redes.size()), "", "Redes con seguidores registrados en el periodo."));
            ind.add(fila("Seguidores (última medición)", BigDecimal.valueOf(totalFin), "", "Suma de todas las redes."));
            ind.add(fila("Crecimiento total", BigDecimal.valueOf(crecimiento), "seguidores", "Primera vs. última medición de cada red."));
            ind.add(new String[]{"Red con mayor crecimiento", mejor, "", mejorPct == null ? "" : "+" + mejorPct.toPlainString() + " %."});
            return reporte(p, ind).tabla("redes", filas);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Monitoreo de páginas no autorizadas (CU6)

    /**
     * Tablas: "porPlataforma" {plataforma, detecciones, paginas_nuevas}; "porTermino" {termino, detecciones};
     *         "porEstado" {estado, paginas}; "nuevas" {id_pagina, nombre, url, plataforma, fecha} (las 15 más recientes).
     */
    public Reporte monitoreo(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            List<String[]> ejecuciones = datos.ejecuciones(p[0], p[1]);
            long conError = ejecuciones.stream().filter(e -> e[1].equals(NScraping.ERROR)).count();
            Map<String, long[]> porPlataforma = new TreeMap<>();
            Map<String, Long> porTermino = new TreeMap<>();
            Map<String, String> estadoPagina = new LinkedHashMap<>();
            List<String[]> nuevas = new ArrayList<>();
            List<String[]> detecciones = datos.detecciones(p[0], p[1]);
            for (String[] f : detecciones) {
                long[] pl = porPlataforma.computeIfAbsent(f[4], k -> new long[2]);
                pl[0]++;
                porTermino.merge(f[5] == null ? "(término eliminado)" : f[5], 1L, Long::sum);
                estadoPagina.put(f[0], f[3]);
                if ("t".equals(f[7])) {
                    pl[1]++;
                    if (nuevas.size() < 15) {
                        nuevas.add(new String[]{f[0], f[2], f[1], f[4], f[6]});
                    }
                }
            }
            Map<String, Long> porEstado = new TreeMap<>();
            estadoPagina.values().forEach(e -> porEstado.merge(e, 1L, Long::sum));
            long totalNuevas = porPlataforma.values().stream().mapToLong(a -> a[1]).sum();
            List<String[]> ind = new ArrayList<>();
            ind.add(fila("Ejecuciones de scraping", BigDecimal.valueOf(ejecuciones.size()), "", conError + " con error."));
            ind.add(fila("Detecciones", BigDecimal.valueOf(detecciones.size()), "", "Veces que se encontró una página no autorizada."));
            ind.add(fila("Páginas no autorizadas nuevas", BigDecimal.valueOf(totalNuevas), "", "Detectadas por primera vez en el periodo."));
            ind.add(fila("Páginas distintas detectadas", BigDecimal.valueOf(estadoPagina.size()), "", ""));
            List<String[]> tPla = new ArrayList<>();
            porPlataforma.forEach((k, v) -> tPla.add(new String[]{k, String.valueOf(v[0]), String.valueOf(v[1])}));
            List<String[]> tTer = new ArrayList<>();
            porTermino.forEach((k, v) -> tTer.add(new String[]{k, String.valueOf(v)}));
            List<String[]> tEst = new ArrayList<>();
            porEstado.forEach((k, v) -> tEst.add(new String[]{k, String.valueOf(v)}));
            return reporte(p, ind).tabla("porPlataforma", tPla).tabla("porTermino", tTer).tabla("porEstado", tEst)
                    .tabla("nuevas", nuevas);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Vista del cliente

    /**
     * Cabecera en el indicador "Empresa". Tabla "campanias":
     *   {campania, estado, inversion, pautas_publicadas, impresiones, interacciones, tasa_interaccion_%}.
     */
    public Reporte cliente(String idEmpresa, String desde, String hasta) throws NegocioException {
        try {
            int id = Validar.id(idEmpresa, "id_empresa");
            String[] emp = empresa.obtener(id);
            if (emp == null) {
                throw new NegocioException("No existe una empresa con ID " + id + ".");
            }
            Sesion.exigirEmpresa(String.valueOf(id), "el reporte de la empresa " + id);
            LocalDate[] p = periodo(desde, hasta);
            List<String[]> proformas = datos.proformas(p[0], p[1], id);
            List<String[]> campanias = datos.campanias(p[0], p[1], id);
            List<String[]> pautas = datos.impacto(p[0], p[1], NPauta.PUBLICADA, id);

            BigDecimal inversion = BigDecimal.ZERO;
            int aprobadas = 0;
            for (String[] f : proformas) {
                if (estadoEfectivo(f[4], f[3]).equals(NProforma.APROBADA)) {
                    aprobadas++;
                    inversion = inversion.add(new BigDecimal(f[5]));
                }
            }
            Suma total = new Suma();
            Map<String, Suma> porCampania = new LinkedHashMap<>();
            for (String[] f : pautas) {
                total.sumar(f);
                porCampania.computeIfAbsent(f[10], k -> new Suma()).sumar(f);
            }
            List<String[]> tCamp = new ArrayList<>();
            for (String[] c : campanias) {
                Suma s = porCampania.getOrDefault(c[0], new Suma());
                tCamp.add(new String[]{c[1], c[2], c[4], String.valueOf(s.pautas), String.valueOf(s.v[1]),
                        String.valueOf(s.v[2]), texto(s.tasa())});
            }
            List<String[]> ind = new ArrayList<>();
            ind.add(new String[]{"Empresa", emp[1], "", "NIT " + emp[2] + "."});
            ind.add(fila("Proformas", BigDecimal.valueOf(proformas.size()), "", aprobadas + " aprobada(s)."));
            ind.add(fila("Inversión", inversion, "Bs", "Total de las proformas aprobadas del periodo."));
            ind.add(fila("Campañas", BigDecimal.valueOf(campanias.size()), "", "Con actividad en el periodo."));
            ind.add(fila("Pautas publicadas", BigDecimal.valueOf(total.pautas), "", total.medidas + " con mediciones."));
            ind.add(fila("Impresiones", BigDecimal.valueOf(total.v[1]), "", ""));
            ind.add(fila("Interacciones", BigDecimal.valueOf(total.v[2]), "", ""));
            ind.add(fila("Tasa de interacción", total.tasa(), "%", "Interacciones / audiencia."));
            ind.add(fila("CPM (costo por mil impresiones)", total.v[1] == 0 ? null : dividir(inversion.multiply(MIL), total.v[1]),
                    "Bs", "Inversión / impresiones × 1000."));
            ind.add(fila("Costo por interacción", total.v[2] == 0 ? null : dividir(inversion, total.v[2]), "Bs",
                    "Inversión / interacciones."));
            return reporte(p, ind).tabla("campanias", tCamp);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Tablero general

    /** Tablas: "porMes" (como en comercial) y "pautasPorCanal" {canal, publicadas, impresiones, interacciones}. */
    public Reporte tablero(String desde, String hasta) throws NegocioException {
        try {
            LocalDate[] p = periodo(desde, hasta);
            Reporte com = comercial(p, datos.proformas(p[0], p[1], null));
            Reporte imp = impacto(p, datos.impacto(p[0], p[1], NPauta.PUBLICADA, null));
            long activas = datos.campanias(p[0], p[1], null).stream().filter(c -> c[2].equals("activa")).count();
            long nuevas = datos.detecciones(p[0], p[1]).stream().filter(d -> "t".equals(d[7])).count();

            List<String[]> ind = new ArrayList<>();
            ind.add(com.getIndicadores().get(2)); // ingresos
            ind.add(com.getIndicadores().get(0)); // proformas emitidas
            ind.add(com.getIndicadores().get(3)); // conversión
            ind.add(fila("Campañas activas", BigDecimal.valueOf(activas), "", "Con actividad en el periodo."));
            ind.add(imp.getIndicadores().get(0)); // pautas publicadas
            ind.add(imp.getIndicadores().get(2)); // impresiones
            ind.add(imp.getIndicadores().get(3)); // interacciones
            ind.add(imp.getIndicadores().get(5)); // tasa de interacción
            ind.add(fila("Material pendiente de revisión", BigDecimal.valueOf(datos.contarMaterialEnEstado(NMaterial.PENDIENTE)),
                    "", "A la fecha (no depende del periodo)."));
            ind.add(fila("Páginas no autorizadas nuevas", BigDecimal.valueOf(nuevas), "", "Detectadas por primera vez en el periodo."));

            List<String[]> porCanal = new ArrayList<>();
            for (String[] c : imp.getTabla("porCanal")) {
                porCanal.add(new String[]{c[0], c[1], c[3], c[4]});
            }
            return reporte(p, ind).tabla("porMes", com.getTabla("porMes")).tabla("pautasPorCanal", porCanal);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // =====================================================================================================
    // Reglas y utilidades

    /** Regla de negocio: una proforma pendiente cuya validez ya pasó cuenta como vencida. */
    private static String estadoEfectivo(String estado, String fechaValidez) {
        if (NProforma.PENDIENTE.equals(estado) && fechaValidez != null && LocalDate.parse(fechaValidez).isBefore(LocalDate.now())) {
            return NProforma.VENCIDA;
        }
        return estado;
    }

    /** {desde, hasta, desde_original o null, hasta_original o null}: null = sin límite. */
    private static LocalDate[] periodo(String desde, String hasta) throws NegocioException {
        LocalDate d = desde == null ? null : Validar.fecha(desde, "fecha_desde");
        LocalDate h = hasta == null ? null : Validar.fecha(hasta, "fecha_hasta");
        Validar.rangoFechas(d, h, "fecha_desde", "fecha_hasta");
        return new LocalDate[]{d == null ? MINIMO : d, h == null ? MAXIMO : h, d, h};
    }

    private static Reporte reporte(LocalDate[] p, List<String[]> indicadores) {
        return new Reporte(p[2], p[3], indicadores);
    }

    /** Suma de la última medición de varias pautas (filas de DReporte.impacto). */
    private static final class Suma {
        final long[] v = new long[5]; // alcance, impresiones, interacciones, clics, compartidos
        long base;
        int pautas;
        int medidas;

        void sumar(String[] f) {
            pautas++;
            if (f[5] == null) {
                return;
            }
            medidas++;
            for (int i = 0; i < 5; i++) {
                v[i] += Long.parseLong(f[5 + i]);
            }
            long alcance = Long.parseLong(f[5]);
            base += alcance > 0 ? alcance : Long.parseLong(f[6]);
        }

        BigDecimal tasa() {
            return porcentaje(v[2], base);
        }
    }

    private static List<String[]> sumas(Map<String, Suma> grupos) {
        List<String[]> filas = new ArrayList<>();
        grupos.forEach((k, s) -> filas.add(new String[]{k, String.valueOf(s.pautas), String.valueOf(s.v[0]),
                String.valueOf(s.v[1]), String.valueOf(s.v[2]), String.valueOf(s.v[3]), texto(s.tasa())}));
        return filas;
    }

    /** {nombre, cantidad, monto, porcentaje_del_total} ordenado por monto descendente. */
    private static List<String[]> ranking(Map<String, BigDecimal[]> grupos, BigDecimal total) {
        List<String[]> filas = new ArrayList<>();
        grupos.entrySet().stream()
                .sorted((a, b) -> b.getValue()[1].compareTo(a.getValue()[1]))
                .forEach(e -> filas.add(new String[]{e.getKey(), entero(e.getValue()[0]), dinero(e.getValue()[1]),
                        texto(porcentaje(e.getValue()[1], total))}));
        return filas;
    }

    private static void sumar(BigDecimal[] acc, BigDecimal cantidad, BigDecimal monto) {
        acc[0] = acc[0].add(cantidad);
        acc[1] = acc[1].add(monto);
    }

    private static BigDecimal[] ceros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static BigDecimal porcentaje(long parte, long total) {
        return porcentaje(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? null : parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal a, BigDecimal b) {
        return b.signum() == 0 ? null : a.divide(b, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal a, long b) {
        return dividir(a, BigDecimal.valueOf(b));
    }

    private static String entero(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String dinero(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String texto(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }

    private static String[] fila(String indicador, BigDecimal valor, String unidad, String explicacion) {
        return new String[]{indicador, texto(valor), unidad, explicacion};
    }
}
