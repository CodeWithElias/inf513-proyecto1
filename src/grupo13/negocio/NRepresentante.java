package grupo13.negocio;

import grupo13.datos.DEmpresa;
import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * CU1: usuarios de tipo Cliente (representante de una empresa anunciante).
 * Primero se crea el usuario y luego se le asigna el perfil de cliente.
 * Fila: id_usuario, nombre_completo, email, telefono, ci, cargo, razon_social, nit, activo.
 */
public class NRepresentante {

    private final DRepresentante datos = new DRepresentante();
    private final DUsuario usuario = new DUsuario();
    private final DEmpresa empresa = new DEmpresa();

    /** Busca por nombre, email, CI, razón social o NIT. */
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

    /** Convierte un usuario activo existente en cliente de una empresa. */
    public void insertar(String idUsuario, String ci, String cargo, String idEmpresa) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            String c = Validar.ci(ci);
            String cg = Validar.textoOpcional(cargo, "cargo", 100);
            int emp = Validar.id(idEmpresa, "id_empresa");

            Boolean activo = usuario.estaActivo(id);
            if (activo == null) {
                throw new NegocioException("No existe un usuario con ID " + id + ". Regístrelo primero como usuario.");
            }
            if (!activo) {
                throw new NegocioException("El usuario " + id + " está inactivo. Reactívelo primero.");
            }
            if (datos.obtener(id) != null) {
                throw new NegocioException("El usuario " + id + " ya es cliente. Modifique sus datos de cliente en su lugar.");
            }
            validarCiYEmpresa(c, emp, id);
            datos.insertar(id, c, cg, emp);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idUsuario, String ci, String cargo, String idEmpresa) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            String c = Validar.ci(ci);
            String cg = Validar.textoOpcional(cargo, "cargo", 100);
            int emp = Validar.id(idEmpresa, "id_empresa");
            buscar(id);
            validarCiYEmpresa(c, emp, id);
            datos.modificar(id, c, cg, emp);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Quita el perfil de cliente; el usuario sigue existiendo. */
    public void eliminar(String idUsuario) throws NegocioException {
        try {
            int id = Validar.id(idUsuario, "id_usuario");
            buscar(id);
            datos.eliminar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private void validarCiYEmpresa(String ci, int idEmpresa, int idUsuario) throws SQLException, NegocioException {
        if (datos.ciEnUso(ci, idUsuario)) {
            throw new NegocioException("El CI " + ci + " ya está registrado para otro cliente.");
        }
        Boolean activa = empresa.estaActivo(idEmpresa);
        if (activa == null) {
            throw new NegocioException("No existe la empresa con ID " + idEmpresa + ". Consulte la lista de empresas.");
        }
        if (!activa) {
            throw new NegocioException("La empresa con ID " + idEmpresa + " está inactiva.");
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("El usuario " + id + " no está registrado como cliente.");
        }
        return fila;
    }
}
