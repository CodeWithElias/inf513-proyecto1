package grupo13.presentacion;

import grupo13.negocio.NRol;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** CU1 – Roles. */
public class PRol {

    private static final String[] COLUMNAS = {"ID", "Nombre", "Descripción", "Activo", "Usuarios activos"};

    private final NRol negocio = new NRol();

    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Roles", "Listando los Roles", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Rol " + p.get(0), COLUMNAS, negocio.obtener(p.get(0)));
    }

    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1));
        return Vista.exito("El rol fue registrado correctamente con ID " + id + ".", COLUMNAS,
                negocio.obtener(String.valueOf(id)));
    }

    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2));
        return Vista.exito("El rol " + p.get(0) + " fue modificado correctamente.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El rol " + p.get(0) + " fue dado de baja. Puede reactivarlo con actRol[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El rol " + p.get(0) + " fue reactivado.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
