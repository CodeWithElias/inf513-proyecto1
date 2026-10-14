package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.util.Html;

/** Validaciones comunes de parámetros. Lanzan ComandoException con un mensaje para el usuario. */
public final class Validar {

    private Validar() {
    }

    public static int id(String valor, String campo) throws ComandoException {
        if (!valor.matches("\\d{1,9}") || Integer.parseInt(valor) == 0) {
            throw new ComandoException("El campo " + campo + " debe ser un número entero positivo (recibido: '"
                    + Html.esc(valor) + "').");
        }
        return Integer.parseInt(valor);
    }

    /** "" = null (campo opcional). */
    public static Integer idOpcional(String valor, String campo) throws ComandoException {
        return valor.isEmpty() ? null : id(valor, campo);
    }

    public static String texto(String valor, String campo, int max) throws ComandoException {
        if (valor.isEmpty()) {
            throw new ComandoException("El campo " + campo + " es obligatorio.");
        }
        return textoOpcional(valor, campo, max);
    }

    public static String textoOpcional(String valor, String campo, int max) throws ComandoException {
        if (valor.length() > max) {
            throw new ComandoException("El campo " + campo + " admite máximo " + max + " caracteres.");
        }
        return valor;
    }

    public static String email(String valor) throws ComandoException {
        texto(valor, "email", 150);
        if (!valor.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) {
            throw new ComandoException("El email '" + Html.esc(valor) + "' no es válido.");
        }
        return valor.toLowerCase();
    }

    public static String telefono(String valor) throws ComandoException {
        if (!valor.matches("[0-9+\\- ]{0,30}")) {
            throw new ComandoException("El teléfono solo puede contener números, espacios, + y -.");
        }
        return valor;
    }

    /** Cédula de identidad: números y opcionalmente complemento/extensión (ej. 4715292, 4715292-1B, 4715292 SC). */
    public static String ci(String valor) throws ComandoException {
        if (!valor.matches("[0-9]{4,12}([ \\-][0-9A-Za-z]{1,4})?")) {
            throw new ComandoException("El CI '" + Html.esc(valor) + "' no es válido (ej. 4715292 o 4715292-1B).");
        }
        return valor.toUpperCase();
    }

    public static String password(String valor) throws ComandoException {
        if (valor.length() < 6 || valor.length() > 100) {
            throw new ComandoException("La contraseña debe tener entre 6 y 100 caracteres.");
        }
        return valor;
    }
}
