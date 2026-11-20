package grupo13.negocio;

/**
 * Actor del comando en curso. La capa de presentación lo inicia al recibir un correo y lo cierra al responder;
 * las clases N* lo consultan para aplicar las reglas de propiedad ("solo lo suyo").
 * Sin sesión (null) = proceso interno del sistema (scraping o captura periódica): acceso completo.
 */
public final class Sesion {

    private static final ThreadLocal<Actor> ACTUAL = new ThreadLocal<>();

    private Sesion() {
    }

    public static void iniciar(Actor actor) {
        ACTUAL.set(actor);
    }

    public static void cerrar() {
        ACTUAL.remove();
    }

    public static Actor actual() {
        return ACTUAL.get();
    }

    /** ¿Ve y modifica todos los datos? Proceso interno o personal de Canal 11. */
    static boolean accesoTotal() {
        Actor a = actual();
        return a == null || a.esPersonal();
    }

    /** Usuario registrado que ejecuta la acción (para registrar quién revisa, programa, carga…). */
    static Actor usuario() throws NegocioException {
        Actor a = actual();
        if (a == null || a.getIdUsuario() == null) {
            throw new NegocioException("Esta acción debe realizarla un usuario registrado del sistema.");
        }
        return a;
    }

    /** El recurso debe ser de la empresa del cliente (el personal de Canal 11 accede a todo). */
    static void exigirEmpresa(String idEmpresaDelRecurso, String recurso) throws NegocioException {
        if (accesoTotal()) {
            return;
        }
        Actor a = actual();
        if (a.getIdEmpresa() == null || idEmpresaDelRecurso == null
                || !String.valueOf(a.getIdEmpresa()).equals(idEmpresaDelRecurso)) {
            throw new NegocioException("No tiene acceso: " + recurso + " no pertenece a su empresa.");
        }
    }

    /** ¿El recurso es de la empresa del cliente? (para filtrar listados). */
    static boolean esDeSuEmpresa(String idEmpresaDelRecurso) {
        if (accesoTotal()) {
            return true;
        }
        Actor a = actual();
        return a.getIdEmpresa() != null && String.valueOf(a.getIdEmpresa()).equals(idEmpresaDelRecurso);
    }
}
