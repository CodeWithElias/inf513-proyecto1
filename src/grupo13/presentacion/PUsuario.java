package grupo13.presentacion;

import grupo13.negocio.NUsuario;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU1 – Usuarios: lisUsu, mosUsu, insUsu, modUsu, eliUsu, actUsu. */
public class PUsuario {

    public static final String[] PARAMS_INSERTAR = {"nombre_completo", "email", "telefono", "password", "id_rol", "id_unidad"};
    public static final String[] PARAMS_MODIFICAR = {"id_usuario", "nombre_completo", "email", "telefono", "password", "id_rol", "id_unidad"};

    private static final String[] COLUMNAS =
            {"ID", "Nombre completo", "Email", "Teléfono", "Rol", "Unidad", "Activo", "Fecha registro", "Último acceso"};

    private final NUsuario negocio = new NUsuario();

    /** lisUsu["*"] o lisUsu["texto"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        String filtro = Vista.filtro(p.get(0));
        String titulo = filtro == null ? "Listando los Usuarios" : "Usuarios que coinciden con \"" + filtro + "\"";
        return Vista.listado("Listando los Usuarios", titulo, COLUMNAS, negocio.listar(filtro));
    }

    /** mosUsu["id"] */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Usuario " + p.get(0), COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** insUsu["nombre","email","telefono","password","id_rol","id_unidad"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("El usuario fue registrado correctamente con ID " + id + ".", COLUMNAS,
                negocio.obtener(String.valueOf(id)));
    }

    /** modUsu["id","nombre","email","telefono","password","id_rol","id_unidad"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5), p.get(6));
        String clave = p.get(4).isEmpty() ? " (la contraseña no cambió)." : " (incluida la contraseña).";
        return Vista.exito("El usuario " + p.get(0) + " fue modificado correctamente" + clave, COLUMNAS,
                negocio.obtener(p.get(0)));
    }

    /** eliUsu["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El usuario " + p.get(0) + " fue dado de baja (inactivo). Puede reactivarlo con actUsu[\""
                + p.get(0) + "\"].", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actUsu["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El usuario " + p.get(0) + " fue reactivado.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
