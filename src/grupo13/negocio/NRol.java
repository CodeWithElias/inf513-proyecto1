package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DRol;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** CU1 / RF02: gestión de roles (Director, Encargado de Unidad, Cliente, …). */
public class NRol {

    private static final String[] TITULOS = {"ID", "Nombre", "Descripción", "Activo", "Usuarios activos"};

    private final DRol datos = new DRol();

    /** lisRol["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Roles",
                Html.pagina("Listando los Roles", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosRol["id"] */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_rol");
        return Respuesta.exito("Rol " + id, Html.pagina("Rol " + id, Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }

    /** insRol["nombre","descripcion"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 100);
        String descripcion = Validar.textoOpcional(p.get(1), "descripcion", 255);
        if (datos.existeNombre(nombre, 0)) {
            throw new ComandoException("Ya existe un rol llamado " + Html.esc(nombre) + ".");
        }
        int id = datos.insertar(nombre, descripcion);
        return exito("El rol fue registrado correctamente con ID " + id + ".", id);
    }

    /** modRol["id","nombre","descripcion"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_rol");
        String nombre = Validar.texto(p.get(1), "nombre", 100);
        String descripcion = Validar.textoOpcional(p.get(2), "descripcion", 255);
        buscar(id);
        if (datos.existeNombre(nombre, id)) {
            throw new ComandoException("Ya existe otro rol llamado " + Html.esc(nombre) + ".");
        }
        datos.modificar(id, nombre, descripcion);
        return exito("El rol " + id + " fue modificado correctamente.", id);
    }

    /** eliRol["id"]: baja lógica; no se permite si hay usuarios activos con ese rol. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_rol");
        if (!estado(id)) {
            throw new ComandoException("El rol " + id + " ya está dado de baja.");
        }
        int usuarios = datos.contarUsuariosActivos(id);
        if (usuarios > 0) {
            throw new ComandoException("No se puede dar de baja el rol " + id + ": tiene " + usuarios
                    + " usuario(s) activo(s). Reasígnelos con modUsu o déles de baja con eliUsu primero.");
        }
        datos.cambiarEstado(id, false);
        return exito("El rol " + id + " fue dado de baja. Puede reactivarlo con actRol[\"" + id + "\"].", id);
    }

    /** actRol["id"] */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_rol");
        if (estado(id)) {
            throw new ComandoException("El rol " + id + " ya está activo.");
        }
        datos.cambiarEstado(id, true);
        return exito("El rol " + id + " fue reactivado.", id);
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe un rol con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe un rol con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
