package grupo13.negocio;

import grupo13.datos.DCampania;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * CU3: campañas contratadas, generadas desde una proforma aprobada de una empresa (base para CU4 y CU5).
 * Fila: id_campania, nombre, fecha_inicio, fecha_fin, estado, nro_proforma, razon_social, total.
 */
public class NCampania {

    public static final Set<String> ESTADOS = Set.of("activa", "pausada", "finalizada", "cancelada");

    private final DCampania datos = new DCampania();
    private final NProforma proforma = new NProforma();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(int id) throws NegocioException {
        try {
            return buscar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Solo desde una proforma aprobada, de empresa y sin campaña previa. Devuelve el ID generado. */
    public int generar(String idProforma, String nombre, String fechaInicio, String fechaFin) throws NegocioException {
        try {
            int idPro = Validar.id(idProforma, "id_proforma");
            String n = Validar.texto(nombre, "nombre", 200);
            LocalDate inicio = Validar.fecha(fechaInicio, "fecha_inicio");
            LocalDate fin = Validar.fecha(fechaFin, "fecha_fin");
            Validar.rangoFechas(inicio, fin, "fecha_inicio", "fecha_fin");

            String[] p = proforma.filaCruda(idPro);
            if (!NProforma.APROBADA.equals(p[9])) {
                throw new NegocioException("La proforma " + idPro + " está " + p[9]
                        + "; solo se generan campañas de proformas aprobadas.");
            }
            if (p[11] == null) {
                throw new NegocioException("La proforma " + idPro
                        + " es de un visitante; las campañas son solo para empresas registradas.");
            }
            if (proforma.tieneCampania(idPro)) {
                throw new NegocioException("La proforma " + idPro + " ya tiene una campaña generada.");
            }
            return datos.insertar(idPro, n, inicio, fin);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** estado: activa, pausada, finalizada o cancelada. */
    public void modificar(String idCampania, String nombre, String fechaInicio, String fechaFin, String estado)
            throws NegocioException {
        try {
            int id = Validar.id(idCampania, "id_campania");
            String n = Validar.texto(nombre, "nombre", 200);
            LocalDate inicio = Validar.fecha(fechaInicio, "fecha_inicio");
            LocalDate fin = Validar.fecha(fechaFin, "fecha_fin");
            String est = estado.toLowerCase();
            Validar.rangoFechas(inicio, fin, "fecha_inicio", "fecha_fin");
            if (!ESTADOS.contains(est)) {
                throw new NegocioException("Estado inválido '" + estado + "'. Use: activa, pausada, finalizada o cancelada.");
            }
            buscar(id);
            datos.modificar(id, n, inicio, fin, est);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una campaña con ID " + id + ".");
        }
        return fila;
    }
}
