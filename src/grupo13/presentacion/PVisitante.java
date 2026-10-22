package grupo13.presentacion;

import grupo13.negocio.NVisitante;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU3 – Visitantes: lisVis, insVis, modVis, eliVis. */
public class PVisitante {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Email", "Teléfono", "Fecha registro", "Proformas"};

    private final NVisitante negocio = new NVisitante();

    /** lisVis["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Visitantes", "Listando los Visitantes", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** insVis["nombre","email","telefono"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("El visitante fue registrado con ID " + id + ". Ya puede generarle una cotización con insPro.",
                COLUMNAS, negocio.obtener(id));
    }

    /** modVis["id","nombre","email","telefono"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("El visitante " + p.get(0) + " fue modificado correctamente.", COLUMNAS,
                negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** eliVis["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El visitante " + p.get(0) + " fue eliminado.", "");
    }
}
