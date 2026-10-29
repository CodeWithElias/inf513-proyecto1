package grupo13.negocio;

import grupo13.datos.DNotificacion;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * CU5 / RF11: notificaciones registradas en el sistema para cada usuario.
 * Fila: id_notificacion, fecha_envio, medio, titulo, mensaje, leida, id_programacion.
 * Propiedad (RF02): cada usuario ve y marca sus notificaciones; el personal de Canal 11 puede ver las de cualquiera.
 */
public class NNotificacion {

    private final DNotificacion datos = new DNotificacion();
    private final DUsuario usuario = new DUsuario();

    /** idUsuario null = las del remitente. */
    public List<String[]> listar(String idUsuario) throws NegocioException {
        try {
            int id = idUsuario == null ? Sesion.usuario().getIdUsuario() : Validar.id(idUsuario, "id_usuario");
            exigirPropia(id);
            if (usuario.estaActivo(id) == null) {
                throw new NegocioException("No existe un usuario con ID " + id + ".");
            }
            return datos.listarPorUsuario(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Marca como leída; devuelve el id del usuario dueño de la notificación. */
    public int marcarLeida(String idNotificacion) throws NegocioException {
        try {
            int id = Validar.id(idNotificacion, "id_notificacion");
            String[] n = datos.obtener(id);
            if (n == null) {
                throw new NegocioException("No existe una notificación con ID " + id + ".");
            }
            exigirPropia(Integer.parseInt(n[0]));
            if ("t".equals(n[1])) {
                throw new NegocioException("La notificación " + id + " ya estaba marcada como leída.");
            }
            datos.marcarLeida(id);
            return Integer.parseInt(n[0]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private static void exigirPropia(int idUsuario) throws NegocioException {
        if (!Sesion.accesoTotal() && Sesion.usuario().getIdUsuario() != idUsuario) {
            throw new NegocioException("Solo puede consultar sus propias notificaciones.");
        }
    }
}
