package grupo13.presentacion;

import grupo13.negocio.NTermino;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU6 – Términos de monitoreo: lisTer, insTer, eliTer, actTer. */
public class PTermino {

    private static final String[] COLUMNAS = {"ID", "Término", "Tipo", "Activo", "Detecciones"};

    private final NTermino negocio = new NTermino();

    /** lisTer["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Términos", "Términos de Monitoreo", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** insTer["termino","tipo"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1));
        return Vista.exito("El término fue registrado con ID " + id + "; se buscará en la próxima ejecución del scraping.",
                COLUMNAS, negocio.obtener(id));
    }

    /** eliTer["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El término " + p.get(0) + " fue desactivado (ya no se buscará). Puede reactivarlo con actTer[\""
                + p.get(0) + "\"].", COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** actTer["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El término " + p.get(0) + " fue reactivado.", COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }
}
