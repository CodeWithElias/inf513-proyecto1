package grupo13.negocio;

import grupo13.datos.DPlataforma;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: catálogo de plataformas de redes sociales (Facebook, TikTok, etc.).
 * Fila: id_plataforma, nombre, redes_activas.
 */
public class NPlataforma {

    private final DPlataforma datos = new DPlataforma();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(int id) throws NegocioException {
        try {
            String[] f = datos.obtener(id);
            if (f == null) {
                throw new NegocioException("No existe una plataforma con ID " + id + ".");
            }
            return f;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public int insertar(String nombre) throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 100);
            if (datos.nombreEnUso(n, 0)) {
                throw new NegocioException("Ya existe una plataforma llamada '" + n + "'.");
            }
            return datos.insertar(n);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }
}
