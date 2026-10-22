package grupo13.negocio;

import grupo13.datos.DRol;
import grupo13.datos.DUnidad;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * CU1 / RF01: gestión de usuarios (Director, Encargado de Unidad, Cliente…).
 * Fila de usuario: id_usuario, nombre_completo, email, telefono, rol, unidad, activo, fecha_registro, ultimo_acceso.
 */
public class NUsuario {

    private final DUsuario datos = new DUsuario();
    private final DRol rol = new DRol();
    private final DUnidad unidad = new DUnidad();

    /** filtro null = todos; si no, busca por nombre, email, rol o unidad. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idUsuario) throws NegocioException {
        try {
            return buscar(Validar.id(idUsuario, "id_usuario"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** idUnidad puede ser "" (sin unidad). Devuelve el ID generado. */
    public int insertar(String nombre, String email, String telefono, String password, String idRol, String idUnidad)
            throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre_completo", 200);
            String e = Validar.email(email);
            String t = Validar.telefono(telefono);
            String p = Validar.password(password);
            int r = Validar.id(idRol, "id_rol");
            Integer u = Validar.idOpcional(idUnidad, "id_unidad");

            if (datos.emailEnUso(e, 0)) {
                throw new NegocioException("Ya existe un usuario con el email " + e + ".");
            }
            validarRolYUnidad(r, u);
            return datos.insertar(n, e, t, Seguridad.hashPassword(p), r, u);
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    /** password "" = no se cambia la contraseña; idUnidad "" = sin unidad. */
    public void modificar(String idUsuario, String nombre, String email, String telefono, String password,
                          String idRol, String idUnidad) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            String n = Validar.texto(nombre, "nombre_completo", 200);
            String e = Validar.email(email);
            String t = Validar.telefono(telefono);
            String p = password.isEmpty() ? null : Validar.password(password);
            int r = Validar.id(idRol, "id_rol");
            Integer u = Validar.idOpcional(idUnidad, "id_unidad");

            buscar(id);
            if (datos.emailEnUso(e, id)) {
                throw new NegocioException("El email " + e + " ya pertenece a otro usuario.");
            }
            validarRolYUnidad(r, u);
            datos.modificar(id, n, e, t, p == null ? null : Seguridad.hashPassword(p), r, u);
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    /** Baja lógica (activo = false): el usuario puede tener historial en otras tablas. */
    public void eliminar(String idUsuario) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            if (!estado(id)) {
                throw new NegocioException("El usuario " + id + " ya está dado de baja.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idUsuario) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            if (estado(id)) {
                throw new NegocioException("El usuario " + id + " ya está activo.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private void validarRolYUnidad(int idRol, Integer idUnidad) throws SQLException, NegocioException {
        Boolean rolActivo = rol.estaActivo(idRol);
        if (rolActivo == null) {
            throw new NegocioException("No existe el rol con ID " + idRol + ". Consulte la lista de roles.");
        }
        if (!rolActivo) {
            throw new NegocioException("El rol con ID " + idRol + " está inactivo.");
        }
        if (idUnidad != null) {
            Boolean uniActiva = unidad.estaActivo(idUnidad);
            if (uniActiva == null) {
                throw new NegocioException("No existe la unidad con ID " + idUnidad
                        + ". Consulte la lista de unidades o deje el campo vacío.");
            }
            if (!uniActiva) {
                throw new NegocioException("La unidad con ID " + idUnidad + " está inactiva.");
            }
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un usuario con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe un usuario con ID " + id + ".");
        }
        return activo;
    }
}
