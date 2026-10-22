package grupo13.negocio;

import java.sql.SQLException;

/**
 * Única excepción que la capa de negocio entrega a la capa de presentación.
 * El mensaje es texto plano en español (sin HTML ni nombres de comandos): la presentación decide cómo mostrarlo.
 */
public class NegocioException extends Exception {

    public NegocioException(String mensaje) {
        super(mensaje);
    }

    /** Traduce un error de la capa de datos para que la presentación no dependa de JDBC. */
    static NegocioException bd(SQLException e) {
        return new NegocioException("Error en la base de datos: " + e.getMessage());
    }
}
