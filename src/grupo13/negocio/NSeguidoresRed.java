package grupo13.negocio;

import grupo13.datos.DRedSocial;
import grupo13.datos.DSeguidoresRed;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: histórico de seguidores de redes sociales oficiales.
 * Fila: id_seguidores, red_social, fecha, seguidores.
 */
public class NSeguidoresRed {

    private final DSeguidoresRed datos = new DSeguidoresRed();
    private final DRedSocial redSocial = new DRedSocial();

    /** Historial de mediciones de una red social (la más reciente primero). */
    public List<String[]> listar(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            if (redSocial.estaActivo(id) == null) {
                throw new NegocioException("No existe una red social con ID " + id + ".");
            }
            return datos.listarPorCanal(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Registra la medición de hoy; devuelve la fila registrada. La red debe estar activa. */
    public String[] insertar(String idCanal, String seguidores) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            int cantidad = Validar.enteroNoNegativo(seguidores, "seguidores");
            Boolean activo = redSocial.estaActivo(id);
            if (activo == null) {
                throw new NegocioException("No existe una red social con ID " + id + ".");
            }
            if (!activo) {
                throw new NegocioException("La red social " + id
                        + " está inactiva. Reactívela antes de registrar métricas de seguidores.");
            }
            datos.insertar(id, cantidad);
            return datos.obtenerUltimo(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }
}
