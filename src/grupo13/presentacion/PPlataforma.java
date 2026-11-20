package grupo13.presentacion;

import grupo13.negocio.NPlataforma;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** CU2 – Plataformas. */
public class PPlataforma {

    private static final String[] COLUMNAS = {"ID", "Plataforma", "Redes activas asociadas"};

    private final NPlataforma negocio = new NPlataforma();

    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Plataformas", "Listando las Plataformas", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0));
        return Vista.exito("La plataforma fue registrada correctamente con ID " + id + ".", COLUMNAS, negocio.obtener(id));
    }
}
