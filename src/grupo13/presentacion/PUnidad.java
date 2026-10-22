package grupo13.presentacion;

import grupo13.negocio.NUnidad;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU1 – Unidades universitarias: lisUni, mosUni, insUni, modUni, eliUni, actUni. */
public class PUnidad {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Tipo", "Unidad padre", "Activo", "Encargados activos"};

    private final NUnidad negocio = new NUnidad();

    /** lisUni["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Unidades", "Listando las Unidades Universitarias", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosUni["id"]: la unidad y sus subunidades directas. */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        String html = Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(p.get(0))))
                + Html.subtitulo("Subunidades") + Html.tabla(COLUMNAS, negocio.listarSubunidades(p.get(0)));
        return Respuesta.exito("Unidad " + p.get(0), Html.pagina("Unidad " + p.get(0), html));
    }

    /** insUni["nombre","tipo","id_unidad_padre"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("La unidad fue registrada correctamente con ID " + id + ".", COLUMNAS,
                negocio.obtener(String.valueOf(id)));
    }

    /** modUni["id","nombre","tipo","id_unidad_padre"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("La unidad " + p.get(0) + " fue modificada correctamente.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** eliUni["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("La unidad " + p.get(0) + " fue dada de baja. Puede reactivarla con actUni[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actUni["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("La unidad " + p.get(0) + " fue reactivada.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
