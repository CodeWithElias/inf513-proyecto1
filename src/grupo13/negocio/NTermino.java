package grupo13.negocio;

import grupo13.datos.DTermino;

import java.sql.SQLException;
import java.util.List;

/**
 * CU6 / RF12: términos de monitoreo (lo que se busca en las plataformas, p. ej. "UAGRM", "Canal 11").
 * Fila: id_termino, termino, tipo, activo, detecciones.
 */
public class NTermino {

    private final DTermino datos = new DTermino();

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

    /** tipo es opcional (p. ej. sigla, nombre, marca). */
    public int insertar(String termino, String tipo) throws NegocioException {
        try {
            String t = Validar.texto(termino.trim(), "termino", 200);
            if (t.length() < 3) {
                throw new NegocioException("El término debe tener al menos 3 caracteres para evitar resultados irrelevantes.");
            }
            String tp = Validar.textoOpcional(tipo, "tipo", 50);
            if (datos.existe(t, 0)) {
                throw new NegocioException("El término '" + t + "' ya está registrado.");
            }
            return datos.insertar(t, tp);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Desactiva el término (deja de buscarse; sus detecciones se conservan). */
    public void eliminar(String idTermino) throws NegocioException {
        cambiar(idTermino, false, "ya está desactivado");
    }

    public void activar(String idTermino) throws NegocioException {
        cambiar(idTermino, true, "ya está activo");
    }

    private void cambiar(String idTermino, boolean activo, String yaEsta) throws NegocioException {
        try {
            int id = Validar.id(idTermino, "id_termino");
            if ("t".equals(buscar(id)[3]) == activo) {
                throw new NegocioException("El término " + id + " " + yaEsta + ".");
            }
            datos.cambiarEstado(id, activo);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un término con ID " + id + ".");
        }
        return fila;
    }
}
