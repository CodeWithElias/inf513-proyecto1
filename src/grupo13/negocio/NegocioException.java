package grupo13.negocio;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Única excepción que la capa de negocio entrega a la capa de presentación.
 * El mensaje es texto plano en español (sin HTML ni nombres de comandos): la presentación decide cómo mostrarlo.
 * Si viene de la base de datos, la causa original (getCause) se conserva para el error.log.
 */
public class NegocioException extends Exception {

    /** "(columna)=(valor)" del detalle que PostgreSQL da en las violaciones de unicidad y de clave foránea. */
    private static final Pattern CLAVE = Pattern.compile("\\(([^)]*)\\)=\\(([^)]*)\\)");

    public NegocioException(String mensaje) {
        super(mensaje);
    }

    private NegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** Traduce un error de la capa de datos (por su SQLState) para que la presentación no dependa de JDBC. */
    static NegocioException bd(SQLException e) {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        Matcher clave = CLAVE.matcher(String.valueOf(e.getMessage()));
        String dato = clave.find() ? " (" + clave.group(1) + " = " + clave.group(2) + ")" : "";
        String mensaje;
        switch (estado) {
            case "23505":
                mensaje = "Ya existe un registro con ese valor" + dato + "; no se permiten duplicados.";
                break;
            case "23503":
                mensaje = "La operación hace referencia a un registro que no existe o que otros datos todavía usan" + dato + ".";
                break;
            case "23502":
                mensaje = "Falta un dato obligatorio.";
                break;
            case "22001":
                mensaje = "Un texto supera la longitud permitida por la base de datos.";
                break;
            case "22003":
                mensaje = "Un número está fuera del rango permitido por la base de datos.";
                break;
            default:
                mensaje = estado.startsWith("08")
                        ? "No se pudo conectar con la base de datos; intente nuevamente en unos minutos."
                        : "Error en la base de datos: " + e.getMessage();
        }
        return new NegocioException(mensaje, e);
    }
}
