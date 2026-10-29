package grupo13.negocio;

import grupo13.datos.DEmpresa;

import java.sql.SQLException;
import java.util.List;

/**
 * CU3 / RF04: empresas anunciantes (clientes).
 * Fila de empresa: id_empresa, razon_social, nit, nombre_comercial, direccion_facturacion, email_facturacion,
 *                  telefono, activo, fecha_registro, representantes.
 * Fila de representante: id_usuario, nombre_completo, email, ci, cargo, activo.
 */
public class NEmpresa {

    private final DEmpresa datos = new DEmpresa();

    /** Busca por razón social, NIT o nombre comercial. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idEmpresa) throws NegocioException {
        try {
            return buscar(Validar.id(idEmpresa, "id_empresa"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public List<String[]> listarRepresentantes(String idEmpresa) throws NegocioException {
        try {
            int id = Validar.id(idEmpresa, "id_empresa");
            buscar(id);
            return datos.representantes(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** nombreComercial, direccion, email y telefono pueden ir vacíos. */
    public int insertar(String razonSocial, String nit, String nombreComercial, String direccion, String email,
                        String telefono) throws NegocioException {
        try {
            String[] d = validar(razonSocial, nit, nombreComercial, direccion, email, telefono);
            if (datos.nitEnUso(d[1], 0)) {
                throw new NegocioException("Ya existe una empresa con el NIT " + d[1] + ".");
            }
            return datos.insertar(d[0], d[1], d[2], d[3], d[4], d[5]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idEmpresa, String razonSocial, String nit, String nombreComercial, String direccion,
                          String email, String telefono) throws NegocioException {
        try {
            int id = Validar.id(idEmpresa, "id_empresa");
            String[] d = validar(razonSocial, nit, nombreComercial, direccion, email, telefono);
            buscar(id);
            if (datos.nitEnUso(d[1], id)) {
                throw new NegocioException("El NIT " + d[1] + " ya pertenece a otra empresa.");
            }
            datos.modificar(id, d[0], d[1], d[2], d[3], d[4], d[5]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica; no se permite con proformas pendientes. */
    public void eliminar(String idEmpresa) throws NegocioException {
        try {
            int id = Validar.id(idEmpresa, "id_empresa");
            if (!estado(id)) {
                throw new NegocioException("La empresa " + id + " ya está dada de baja.");
            }
            int pendientes = datos.contarProformasEnEstado(id, NProforma.PENDIENTE);
            if (pendientes > 0) {
                throw new NegocioException("No se puede dar de baja la empresa " + id + ": tiene " + pendientes
                        + " proforma(s) pendiente(s). Apruébelas o recházelas primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idEmpresa) throws NegocioException {
        try {
            int id = Validar.id(idEmpresa, "id_empresa");
            if (estado(id)) {
                throw new NegocioException("La empresa " + id + " ya está activa.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] validar(String razonSocial, String nit, String nombreComercial, String direccion, String email,
                             String telefono) throws NegocioException {
        return new String[]{
                Validar.texto(razonSocial, "razon_social", 200),
                Validar.nit(nit),
                Validar.textoOpcional(nombreComercial, "nombre_comercial", 200),
                Validar.textoOpcional(direccion, "direccion_facturacion", 255),
                Validar.emailOpcional(email),
                Validar.telefono(telefono)
        };
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una empresa con ID " + id + ".");
        }
        Sesion.exigirEmpresa(String.valueOf(id), "la empresa " + id);
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe una empresa con ID " + id + ".");
        }
        return activo;
    }
}
