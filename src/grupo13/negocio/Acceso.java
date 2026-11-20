package grupo13.negocio;

import grupo13.datos.DCanal;

import java.sql.SQLException;

/**
 * RF02: reglas de propiedad que necesitan consultar datos (redes sociales y pautas).
 *  - Encargado de Unidad: sus redes son las de su unidad o aquellas de las que es responsable.
 *  - Cliente: las pautas de su empresa.  Personal de Canal 11 y procesos internos: todo.
 */
final class Acceso {

    // Índices de la fila de pauta (ver DPauta)
    private static final int PP_CANAL = 14;
    private static final int PP_EMPRESA = 15;

    private static final DCanal CANAL = new DCanal();

    private Acceso() {
    }

    /** ¿La red social (id_canal) es del encargado actual? */
    static boolean esSuRed(String idCanal) throws SQLException {
        if (Sesion.accesoTotal()) {
            return true;
        }
        Actor a = Sesion.actual();
        if (!a.es(Actor.ENCARGADO) || idCanal == null) {
            return false;
        }
        String[] red = CANAL.datosRedSocial(Integer.parseInt(idCanal));
        if (red == null) {
            return false;
        }
        boolean deSuUnidad = a.getIdUnidad() != null && String.valueOf(a.getIdUnidad()).equals(red[0]);
        boolean esResponsable = String.valueOf(a.getIdUsuario()).equals(red[1]);
        return deSuUnidad || esResponsable;
    }

    static void exigirRed(String idCanal) throws SQLException, NegocioException {
        if (!esSuRed(idCanal)) {
            throw new NegocioException("No tiene acceso a la red social " + idCanal + ": no es de su unidad ni está a su cargo.");
        }
    }

    /** ¿Puede ver la pauta? Cliente: de su empresa; Encargado: publicada en sus redes. */
    static boolean puedeVerPauta(String[] pp) throws SQLException {
        if (Sesion.accesoTotal()) {
            return true;
        }
        Actor a = Sesion.actual();
        if (a.es(Actor.CLIENTE)) {
            return Sesion.esDeSuEmpresa(pp[PP_EMPRESA]);
        }
        return a.es(Actor.ENCARGADO) && esSuRed(pp[PP_CANAL]);
    }

    static void exigirPauta(String[] pp) throws SQLException, NegocioException {
        if (!puedeVerPauta(pp)) {
            throw new NegocioException("No tiene acceso a la pauta " + pp[0] + ".");
        }
    }
}
