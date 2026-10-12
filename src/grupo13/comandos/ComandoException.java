package grupo13.comandos;

/** Error de validación de un comando (mensaje legible para el usuario). */
public class ComandoException extends Exception {

    public ComandoException(String mensaje) {
        super(mensaje);
    }
}
