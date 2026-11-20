package grupo13.presentacion;

import grupo13.negocio.NRedSocial;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.ArrayList;
import java.util.List;

/** CU2 – Redes sociales institucionales. */
public class PRedSocial {

    private static final String[] COLUMNAS = {"ID", "Nombre de la Red", "Plataforma", "URL", "Responsable",
            "Unidad Universitaria", "Seguidores", "Activo", "Fecha Alta", "Fecha Baja"};
    private static final int RESPONSABLE = 4;
    private static final int UNIDAD = 5;

    private final NRedSocial negocio = new NRedSocial();

    public Respuesta listar(List<String> p) throws NegocioException {
        List<String[]> filas = new ArrayList<>();
        for (String[] f : negocio.listar(Vista.filtro(p.get(0)))) {
            filas.add(vista(f));
        }
        return Vista.listado("Listando las Redes Sociales", "Listando las Redes Sociales Institucionales", COLUMNAS, filas);
    }

    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Red Social Institucional " + p.get(0), COLUMNAS, vista(negocio.obtener(p.get(0))));
    }

    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4));
        return Vista.exito("La red social institucional fue registrada correctamente con ID " + id + ".", COLUMNAS,
                vista(negocio.obtener(String.valueOf(id))));
    }

    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("La red social " + p.get(0) + " fue modificada correctamente.", COLUMNAS,
                vista(negocio.obtener(p.get(0))));
    }

    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("La red social " + p.get(0) + " fue dada de baja (inactivada).", COLUMNAS,
                vista(negocio.obtener(p.get(0))));
    }

    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("La red social " + p.get(0) + " fue reactivada exitosamente.", COLUMNAS,
                vista(negocio.obtener(p.get(0))));
    }

    private static String[] vista(String[] f) {
        String[] r = f.clone();
        if (r[RESPONSABLE] == null) {
            r[RESPONSABLE] = "Sin asignar";
        }
        if (r[UNIDAD] == null) {
            r[UNIDAD] = "Sin asignar";
        }
        return r;
    }
}
