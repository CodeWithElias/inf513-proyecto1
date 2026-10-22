package grupo13.negocio;

import grupo13.datos.DVisitante;

import java.sql.SQLException;
import java.util.List;

/**
 * CU3 / RF07: visitantes (interesados sin cuenta que solicitan cotizaciones).
 * Fila: id_visitante, nombre, email, telefono, fecha_registro, proformas.
 */
public class NVisitante {

    private final DVisitante datos = new DVisitante();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(int id) throws NegocioException {
        try {
            return buscar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Se exige email o teléfono para poder contactarlo. */
    public int insertar(String nombre, String email, String telefono) throws NegocioException {
        try {
            String[] d = validar(nombre, email, telefono);
            return datos.insertar(d[0], d[1], d[2]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idVisitante, String nombre, String email, String telefono) throws NegocioException {
        try {
            int id = Validar.id(idVisitante, "id_visitante");
            String[] d = validar(nombre, email, telefono);
            buscar(id);
            datos.modificar(id, d[0], d[1], d[2]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Borrado físico, solo si no tiene proformas (la tabla no tiene columna activo). */
    public void eliminar(String idVisitante) throws NegocioException {
        try {
            int id = Validar.id(idVisitante, "id_visitante");
            buscar(id);
            int proformas = datos.contarProformas(id);
            if (proformas > 0) {
                throw new NegocioException("No se puede eliminar el visitante " + id + ": tiene " + proformas
                        + " proforma(s) registrada(s).");
            }
            datos.eliminar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] validar(String nombre, String email, String telefono) throws NegocioException {
        String n = Validar.texto(nombre, "nombre", 150);
        String e = Validar.emailOpcional(email);
        String t = Validar.telefono(telefono);
        if (e.isEmpty() && t.isEmpty()) {
            throw new NegocioException("Debe indicar al menos un email o un teléfono para contactar al visitante.");
        }
        return new String[]{n, e, t};
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un visitante con ID " + id + ".");
        }
        return fila;
    }
}
