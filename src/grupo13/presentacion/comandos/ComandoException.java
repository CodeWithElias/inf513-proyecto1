package grupo13.presentacion.comandos;

/** Error del protocolo de comandos (formato, comando, permiso). El mensaje puede traer HTML ya escapado. */
public class ComandoException extends Exception {

    public ComandoException(String mensaje) {
        super(mensaje);
    }
}
