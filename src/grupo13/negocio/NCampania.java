package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DCampania;
import grupo13.datos.DProforma;
import grupo13.util.Html;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** CU3: campañas contratadas, generadas desde una proforma aprobada de una empresa (base para CU4 y CU5). */
public class NCampania {

    private static final Set<String> ESTADOS = Set.of("activa", "pausada", "finalizada", "cancelada");

    private static final String[] TITULOS = {"ID", "Nombre", "Inicio", "Fin", "Estado", "Proforma", "Empresa", "Total (Bs)"};

    private final DCampania datos = new DCampania();
    private final DProforma proforma = new DProforma();

    /** lisCam["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Campañas",
                Html.pagina("Listando las Campañas", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** genCam["id_proforma","nombre","fecha_inicio","fecha_fin"] */
    public Respuesta generar(List<String> p) throws Exception {
        int idProforma = Validar.id(p.get(0), "id_proforma");
        String nombre = Validar.texto(p.get(1), "nombre", 200);
        LocalDate inicio = Validar.fecha(p.get(2), "fecha_inicio");
        LocalDate fin = Validar.fecha(p.get(3), "fecha_fin");
        Validar.rangoFechas(inicio, fin, "fecha_inicio", "fecha_fin");

        String[] e = proforma.estado(idProforma);
        if (e == null) {
            throw new ComandoException("No existe una proforma con ID " + idProforma + ".");
        }
        if (!e[0].equals("aprobada")) {
            throw new ComandoException("La proforma " + idProforma + " está " + Html.esc(e[0])
                    + "; solo se generan campañas de proformas aprobadas (aprPro).");
        }
        if (e[2] == null) {
            throw new ComandoException("La proforma " + idProforma + " es de un visitante; las campañas son solo para empresas registradas.");
        }
        if (proforma.tieneCampania(idProforma)) {
            throw new ComandoException("La proforma " + idProforma + " ya tiene una campaña generada (lisCam).");
        }
        int id = datos.insertar(idProforma, nombre, inicio, fin);
        return exito("La campaña fue generada con ID " + id + ". El cliente ya puede cargar su material publicitario (CU4).", id);
    }

    /** modCam["id","nombre","fecha_inicio","fecha_fin","estado"]  (estado: activa, pausada, finalizada, cancelada) */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_campania");
        String nombre = Validar.texto(p.get(1), "nombre", 200);
        LocalDate inicio = Validar.fecha(p.get(2), "fecha_inicio");
        LocalDate fin = Validar.fecha(p.get(3), "fecha_fin");
        String estado = p.get(4).toLowerCase();
        Validar.rangoFechas(inicio, fin, "fecha_inicio", "fecha_fin");
        if (!ESTADOS.contains(estado)) {
            throw new ComandoException("Estado inválido '" + Html.esc(p.get(4)) + "'. Use: activa, pausada, finalizada o cancelada.");
        }
        buscar(id);
        datos.modificar(id, nombre, inicio, fin, estado);
        return exito("La campaña " + id + " fue modificada.", id);
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe una campaña con ID " + id + ".");
        }
        return fila;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
