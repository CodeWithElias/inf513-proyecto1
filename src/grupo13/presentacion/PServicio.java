package grupo13.presentacion;

import grupo13.negocio.NServicio;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU3 – Servicios publicitarios: lisSer, insSer, modSer, eliSer, actSer. */
public class PServicio {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Tipo", "Descripción", "Activo", "Tarifas activas"};

    private final NServicio negocio = new NServicio();

    /** lisSer["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Servicios", "Listando los Servicios Publicitarios", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** insSer["nombre","tipo","descripcion"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("El servicio fue registrado con ID " + id + ". Defina su precio con insTar.", COLUMNAS,
                negocio.obtener(id));
    }

    /** modSer["id","nombre","tipo","descripcion"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("El servicio " + p.get(0) + " fue modificado correctamente.", COLUMNAS,
                negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** eliSer["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El servicio " + p.get(0) + " fue dado de baja. Puede reactivarlo con actSer[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** actSer["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El servicio " + p.get(0) + " fue reactivado.", COLUMNAS,
                negocio.obtener(Integer.parseInt(p.get(0))));
    }
}
