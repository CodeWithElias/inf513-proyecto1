package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DRol;
import grupo13.datos.DUnidad;
import grupo13.datos.DUsuario;
import grupo13.util.Html;
import grupo13.util.Seguridad;

import java.sql.SQLException;
import java.util.List;

/** Reglas de negocio del caso de uso Gestionar Usuarios. */
public class NUsuario {

    public static final String[] PARAMS_INSERTAR = {"nombre_completo", "email", "telefono", "password", "id_rol", "id_unidad"};
    public static final String[] PARAMS_MODIFICAR = {"id_usuario", "nombre_completo", "email", "telefono", "password", "id_rol", "id_unidad"};

    private static final String[] TITULOS =
            {"ID", "Nombre completo", "Email", "Teléfono", "Rol", "Unidad", "Activo", "Fecha registro", "Último acceso"};

    private final DUsuario datos = new DUsuario();
    private final DRol rol = new DRol();
    private final DUnidad unidad = new DUnidad();

    /** lisUsu["*"] lista todos; lisUsu["texto"] busca por nombre, email, rol o unidad. */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        List<String[]> filas = datos.listar(filtro);
        String titulo = filtro == null ? "Listando los Usuarios" : "Usuarios que coinciden con \"" + filtro + "\"";
        return Respuesta.exito("Listando los Usuarios", Html.pagina(titulo, Html.tabla(TITULOS, filas)));
    }

    /** mosUsu["id"] */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        String[] fila = buscar(id);
        return Respuesta.exito("Usuario " + id, Html.pagina("Usuario " + id, Html.tabla(TITULOS, List.<String[]>of(fila))));
    }

    /** insUsu["nombre","email","telefono","password","id_rol","id_unidad"]  (id_unidad puede ser "") */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre_completo", 200);
        String email = Validar.email(p.get(1));
        String telefono = Validar.telefono(p.get(2));
        String password = Validar.password(p.get(3));
        int idRol = Validar.id(p.get(4), "id_rol");
        Integer idUnidad = Validar.idOpcional(p.get(5), "id_unidad");

        if (datos.emailEnUso(email, 0)) {
            throw new ComandoException("Ya existe un usuario con el email " + Html.esc(email) + ".");
        }
        validarRolYUnidad(idRol, idUnidad);

        int id = datos.insertar(nombre, email, telefono, Seguridad.hashPassword(password), idRol, idUnidad);
        return exito("El usuario fue registrado correctamente con ID " + id + ".", id);
    }

    /** modUsu["id","nombre","email","telefono","password","id_rol","id_unidad"]  (password "" = no cambia) */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        String nombre = Validar.texto(p.get(1), "nombre_completo", 200);
        String email = Validar.email(p.get(2));
        String telefono = Validar.telefono(p.get(3));
        String password = p.get(4).isEmpty() ? null : Validar.password(p.get(4));
        int idRol = Validar.id(p.get(5), "id_rol");
        Integer idUnidad = Validar.idOpcional(p.get(6), "id_unidad");

        buscar(id);
        if (datos.emailEnUso(email, id)) {
            throw new ComandoException("El email " + Html.esc(email) + " ya pertenece a otro usuario.");
        }
        validarRolYUnidad(idRol, idUnidad);

        String hash = password == null ? null : Seguridad.hashPassword(password);
        datos.modificar(id, nombre, email, telefono, hash, idRol, idUnidad);
        return exito("El usuario " + id + " fue modificado correctamente"
                + (password == null ? " (la contraseña no cambió)." : " (incluida la contraseña)."), id);
    }

    /** eliUsu["id"]: baja lógica (activo = false), porque el usuario puede tener historial en otras tablas. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        if (!estado(id)) {
            throw new ComandoException("El usuario " + id + " ya está dado de baja.");
        }
        datos.cambiarEstado(id, false);
        return exito("El usuario " + id + " fue dado de baja (inactivo). Puede reactivarlo con actUsu[\"" + id + "\"].", id);
    }

    /** actUsu["id"]: reactiva un usuario dado de baja. */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        if (estado(id)) {
            throw new ComandoException("El usuario " + id + " ya está activo.");
        }
        datos.cambiarEstado(id, true);
        return exito("El usuario " + id + " fue reactivado.", id);
    }

    private void validarRolYUnidad(int idRol, Integer idUnidad) throws Exception {
        Boolean rolActivo = rol.estaActivo(idRol);
        if (rolActivo == null) {
            throw new ComandoException("No existe el rol con ID " + idRol + ". Consulte los roles con lisRol[\"*\"].");
        }
        if (!rolActivo) {
            throw new ComandoException("El rol con ID " + idRol + " está inactivo.");
        }
        if (idUnidad != null) {
            Boolean uniActiva = unidad.estaActivo(idUnidad);
            if (uniActiva == null) {
                throw new ComandoException("No existe la unidad con ID " + idUnidad
                        + ". Consulte las unidades con lisUni[\"*\"] o deje el campo vacío (\"\").");
            }
            if (!uniActiva) {
                throw new ComandoException("La unidad con ID " + idUnidad + " está inactiva.");
            }
        }
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe un usuario con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe un usuario con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
