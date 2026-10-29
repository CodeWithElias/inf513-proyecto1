package grupo13.negocio;

import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * RF02: identificación por el correo del remitente (el correo es la única "sesión" posible).
 *  - Correo registrado en usuario, con usuario y rol activos → su rol (y su empresa, si es representante).
 *  - Usuario o rol dado de baja → "Cuenta inactiva".
 *  - Correo no registrado → Visitante.
 * Cada identificación de un usuario registrado actualiza su último acceso.
 */
public class NAutenticacion {

    private final DUsuario usuario = new DUsuario();
    private final DRepresentante representante = new DRepresentante();

    public Actor identificar(String email) throws NegocioException {
        try {
            String correo = email == null ? "" : email.trim().toLowerCase();
            String[] u = usuario.porEmail(correo);
            if (u == null) {
                return new Actor(null, correo, correo, Actor.VISITANTE, null, null);
            }
            int id = Integer.parseInt(u[0]);
            if (!"t".equals(u[4]) || !"t".equals(u[6])) {
                return new Actor(id, u[1], correo, Actor.INACTIVO, null, null);
            }
            usuario.registrarAcceso(id);
            return new Actor(id, u[1], correo, Actor.rolCanonico(u[3]), representante.empresaDe(id),
                    u[5] == null ? null : Integer.parseInt(u[5]));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Correo del primer administrador activo (lo usa el modo de prueba cuando no se indica remitente), o null. */
    public String correoAdministrador() throws NegocioException {
        try {
            List<String[]> admins = usuario.activosDeRol(Actor.ADMINISTRADOR);
            return admins.isEmpty() ? null : admins.get(0)[2];
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }
}
