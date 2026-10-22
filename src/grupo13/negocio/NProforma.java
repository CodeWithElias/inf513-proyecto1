package grupo13.negocio;

import grupo13.datos.DEmpresa;
import grupo13.datos.DPaquete;
import grupo13.datos.DProforma;
import grupo13.datos.DTarifa;
import grupo13.datos.DVisitante;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * CU3 / RF07: proformas y cotizaciones.
 * Reglas:
 *  - Estados: pendiente → aprobada | rechazada. Una pendiente cuya validez ya pasó se informa como "vencida".
 *  - Solo las pendientes se editan; solo las pendientes vigentes reciben ítems o se aprueban.
 *  - subtotal = Σ subtotal de los ítems; total = max(subtotal − descuento, 0).
 *  - Cada ítem congela su precio unitario (si luego cambia la tarifa, la proforma no cambia).
 *  - Número: PRO-AAAA-NNNNN.
 * Fila de proforma (la que devuelve esta clase): id_proforma, nro_proforma, fecha_emision, fecha_validez,
 *   razon_social_empresa, nombre_visitante, subtotal, descuento, total, estado, items, id_empresa.
 * Fila de ítem: id_detalle, id_tarifa, servicio, unidad, unidad_cobro, id_paquete, paquete, cantidad, frecuencia,
 *   precio_unitario, subtotal.
 */
public class NProforma {

    public static final String PENDIENTE = "pendiente";
    public static final String APROBADA = "aprobada";
    public static final String RECHAZADA = "rechazada";
    public static final String VENCIDA = "vencida";

    /** Días de validez cuando no se indican. */
    private static final int DIAS_VALIDEZ = 15;
    private static final int DIAS_VALIDEZ_MAX = 365;

    // Índices de la fila de proforma
    private static final int VALIDEZ = 3;
    private static final int SUBTOTAL = 6;
    private static final int DESCUENTO = 7;
    private static final int ESTADO = 9;
    private static final int ITEMS = 10;
    private static final int ID_EMPRESA = 11;

    private final DProforma datos = new DProforma();
    private final DEmpresa empresa = new DEmpresa();
    private final DVisitante visitante = new DVisitante();
    private final DTarifa tarifa = new DTarifa();
    private final DPaquete paquete = new DPaquete();

    /** Busca por número, estado, empresa, NIT o visitante. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            List<String[]> filas = new ArrayList<>();
            for (String[] f : datos.listar(filtro)) {
                filas.add(conEstadoActual(f));
            }
            return filas;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idProforma) throws NegocioException {
        try {
            return conEstadoActual(buscar(Validar.id(idProforma, "id_proforma")));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public List<String[]> listarItems(String idProforma) throws NegocioException {
        try {
            int id = Validar.id(idProforma, "id_proforma");
            buscar(id);
            return datos.detalles(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Exactamente uno de empresa / visitante. diasValidez vacío = 15; descuento en Bs (vacío = 0). */
    public int insertar(String idEmpresa, String idVisitante, String diasValidez, String descuento)
            throws NegocioException {
        try {
            Integer emp = Validar.idOpcional(idEmpresa, "id_empresa");
            Integer vis = Validar.idOpcional(idVisitante, "id_visitante");
            int dias = diasValidez.isEmpty() ? DIAS_VALIDEZ : Validar.id(diasValidez, "dias_validez");
            BigDecimal desc = descuento.isEmpty() ? BigDecimal.ZERO : Validar.monto(descuento, "descuento");

            if ((emp == null) == (vis == null)) {
                throw new NegocioException("Indique una empresa o un visitante (uno de los dos; el otro vacío).");
            }
            if (emp != null) {
                Boolean activa = empresa.estaActivo(emp);
                if (activa == null) {
                    throw new NegocioException("No existe la empresa con ID " + emp + ". Consulte la lista de empresas.");
                }
                if (!activa) {
                    throw new NegocioException("La empresa con ID " + emp + " está dada de baja.");
                }
            } else if (visitante.obtener(vis) == null) {
                throw new NegocioException("No existe el visitante con ID " + vis + ". Regístrelo primero.");
            }
            if (dias > DIAS_VALIDEZ_MAX) {
                throw new NegocioException("La validez máxima de una proforma es de " + DIAS_VALIDEZ_MAX + " días.");
            }
            int anio = LocalDate.now().getYear();
            return datos.insertar(emp, vis, LocalDate.now().plusDays(dias), desc,
                    id -> String.format("PRO-%d-%05d", anio, id));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Cambia el descuento (Bs) y la fecha de validez (DD/MM/AAAA, no anterior a hoy) de una pendiente. */
    public void modificar(String idProforma, String descuento, String fechaValidez) throws NegocioException {
        try {
            int id = Validar.id(idProforma, "id_proforma");
            BigDecimal desc = descuento.isEmpty() ? BigDecimal.ZERO : Validar.monto(descuento, "descuento");
            LocalDate validez = Validar.fecha(fechaValidez, "fecha_validez");
            String[] p = exigirPendiente(id, false);
            if (validez.isBefore(LocalDate.now())) {
                throw new NegocioException("La fecha de validez no puede ser anterior a hoy.");
            }
            datos.modificar(id, desc, validez, total(new BigDecimal(p[SUBTOTAL]), desc));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Agrega una tarifa vigente o un paquete activo con tarifas (uno de los dos). */
    public void agregarItem(String idProforma, String idTarifa, String idPaquete, String cantidad, String frecuencia)
            throws NegocioException {
        try {
            int id = Validar.id(idProforma, "id_proforma");
            Integer tar = Validar.idOpcional(idTarifa, "id_tarifa");
            Integer paq = Validar.idOpcional(idPaquete, "id_paquete");
            int cant = Validar.id(cantidad, "cantidad");
            String frec = Validar.textoOpcional(frecuencia, "frecuencia", 50);
            String[] p = exigirPendiente(id, true);

            if ((tar == null) == (paq == null)) {
                throw new NegocioException("Indique una tarifa o un paquete (uno de los dos; el otro vacío).");
            }
            BigDecimal precio;
            if (tar != null) {
                String[] t = tarifa.obtener(tar);
                if (t == null) {
                    throw new NegocioException("No existe la tarifa con ID " + tar + ". Consulte el tarifario.");
                }
                if (!NTarifa.esVigente(t)) {
                    throw new NegocioException("La tarifa " + tar + " no está vigente hoy (deshabilitada o fuera de su periodo).");
                }
                precio = new BigDecimal(t[3]);
            } else {
                String[] pq = paquete.obtener(paq);
                if (pq == null) {
                    throw new NegocioException("No existe el paquete con ID " + paq + ". Consulte la lista de paquetes.");
                }
                if (!"t".equals(pq[6])) {
                    throw new NegocioException("El paquete " + paq + " está dado de baja.");
                }
                if (Integer.parseInt(pq[5]) == 0) {
                    throw new NegocioException("El paquete " + paq + " no tiene tarifas; agrégueselas primero.");
                }
                precio = new BigDecimal(pq[4]);
            }
            BigDecimal subtotalItem = precio.multiply(BigDecimal.valueOf(cant));
            BigDecimal subtotal = new BigDecimal(p[SUBTOTAL]).add(subtotalItem);
            datos.agregarDetalle(id, tar, paq, cant, frec, precio, subtotalItem,
                    subtotal, total(subtotal, new BigDecimal(p[DESCUENTO])));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Quita un ítem de una proforma pendiente. Devuelve el ID de la proforma. */
    public int quitarItem(String idDetalle) throws NegocioException {
        try {
            int idDet = Validar.id(idDetalle, "id_detalle");
            String[] det = datos.obtenerDetalle(idDet);
            if (det == null) {
                throw new NegocioException("No existe un ítem de proforma con ID " + idDet + ".");
            }
            int id = Integer.parseInt(det[0]);
            String[] p = exigirPendiente(id, false);
            BigDecimal subtotal = new BigDecimal(p[SUBTOTAL]).subtract(new BigDecimal(det[1]));
            datos.quitarDetalle(idDet, id, subtotal, total(subtotal, new BigDecimal(p[DESCUENTO])));
            return id;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** El cliente acepta la cotización. Devuelve true si es de una empresa (puede generar campaña). */
    public boolean aprobar(String idProforma) throws NegocioException {
        try {
            int id = Validar.id(idProforma, "id_proforma");
            String[] p = exigirPendiente(id, true);
            if (Integer.parseInt(p[ITEMS]) == 0) {
                throw new NegocioException("La proforma no tiene ítems; agrégueselos antes de aprobarla.");
            }
            datos.cambiarEstado(id, APROBADA);
            return p[ID_EMPRESA] != null;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void rechazar(String idProforma) throws NegocioException {
        try {
            int id = Validar.id(idProforma, "id_proforma");
            exigirPendiente(id, false);
            datos.cambiarEstado(id, RECHAZADA);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    // ===== Reglas =====

    static BigDecimal total(BigDecimal subtotal, BigDecimal descuento) {
        return subtotal.subtract(descuento).max(BigDecimal.ZERO);
    }

    static boolean estaVencida(String[] fila) {
        return PENDIENTE.equals(fila[ESTADO]) && LocalDate.parse(fila[VALIDEZ]).isBefore(LocalDate.now());
    }

    /** Estado que se informa: "vencida" si es pendiente y ya pasó su validez. */
    private static String[] conEstadoActual(String[] f) {
        if (estaVencida(f)) {
            f[ESTADO] = VENCIDA;
        }
        return f;
    }

    /** Fila cruda (estado guardado) de una proforma existente. */
    String[] filaCruda(int id) throws SQLException, NegocioException {
        return buscar(id);
    }

    boolean tieneCampania(int id) throws SQLException {
        return datos.tieneCampania(id);
    }

    /** La proforma debe estar pendiente (y vigente si 'vigente'). Devuelve su fila cruda. */
    private String[] exigirPendiente(int id, boolean vigente) throws SQLException, NegocioException {
        String[] p = buscar(id);
        if (!PENDIENTE.equals(p[ESTADO])) {
            throw new NegocioException("La proforma " + id + " está " + p[ESTADO] + "; solo se pueden cambiar las pendientes.");
        }
        if (vigente && estaVencida(p)) {
            throw new NegocioException("La proforma " + id + " está vencida. Extienda primero su fecha de validez.");
        }
        return p;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una proforma con ID " + id + ".");
        }
        return fila;
    }
}
