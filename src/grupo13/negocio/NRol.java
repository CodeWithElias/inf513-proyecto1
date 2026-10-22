package grupo13.negocio;

import grupo13.datos.DRol;

import java.sql.SQLException;
import java.util.List;

/**
 * CU1 / RF02: gestión de roles.
 * Fila de rol: id_rol, nombre, descripcion, activo, usuarios_activos.
 */
public class NRol {

    private final DRol datos = new DRol();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idRol) throws NegocioException {
        try {
            return buscar(Validar.id(idRol, "id_rol"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public int insertar(String nombre, String descripcion) throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 100);
            String d = Validar.textoOpcional(descripcion, "descripcion", 255);
            if (datos.existeNombre(n, 0)) {
                throw new NegocioException("Ya existe un rol llamado " + n + ".");
            }
            return datos.insertar(n, d);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idRol, String nombre, String descripcion) throws NegocioException {
        try {
            int id = Validar.id(idRol, "id_rol");
            String n = Validar.texto(nombre, "nombre", 100);
            String d = Validar.textoOpcional(descripcion, "descripcion", 255);
            buscar(id);
            if (datos.existeNombre(n, id)) {
                throw new NegocioException("Ya existe otro rol llamado " + n + ".");
            }
            datos.modificar(id, n, d);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica; no se permite si hay usuarios activos con ese rol. */
    public void eliminar(String idRol) throws NegocioException {
        try {
            int id = Validar.id(idRol, "id_rol");
            if (!estado(id)) {
                throw new NegocioException("El rol " + id + " ya está dado de baja.");
            }
            int usuarios = datos.contarUsuariosActivos(id);
            if (usuarios > 0) {
                throw new NegocioException("No se puede dar de baja el rol " + id + ": tiene " + usuarios
                        + " usuario(s) activo(s). Reasígnelos o déles de baja primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idRol) throws NegocioException {
        try {
            int id = Validar.id(idRol, "id_rol");
            if (estado(id)) {
                throw new NegocioException("El rol " + id + " ya está activo.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un rol con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe un rol con ID " + id + ".");
        }
        return activo;
    }
}
