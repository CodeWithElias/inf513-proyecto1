package grupo13.presentacion;

import grupo13.negocio.NCampania;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** CU3 – Campañas. */
public class PCampania {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Inicio", "Fin", "Estado", "Proforma", "Empresa", "Total (Bs)"};

    private final NCampania negocio = new NCampania();

    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Campañas", "Listando las Campañas", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    public Respuesta generar(List<String> p) throws NegocioException {
        int id = negocio.generar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("La campaña fue generada con ID " + id + ". El cliente ya puede cargar su material publicitario (CU4).",
                COLUMNAS, negocio.obtener(id));
    }

    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4));
        return Vista.exito("La campaña " + p.get(0) + " fue modificada.", COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }
}
