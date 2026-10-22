package grupo13.negocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Validaciones comunes de parámetros. Lanzan NegocioException con un mensaje para el usuario. */
public final class Validar {

    private Validar() {
    }

    public static int id(String valor, String campo) throws NegocioException {
        if (!valor.matches("\\d{1,9}") || Integer.parseInt(valor) == 0) {
            throw new NegocioException("El campo " + campo + " debe ser un número entero positivo (recibido: '"
                    + valor + "').");
        }
        return Integer.parseInt(valor);
    }

    /** "" = null (campo opcional). */
    public static Integer idOpcional(String valor, String campo) throws NegocioException {
        return valor.isEmpty() ? null : id(valor, campo);
    }

    public static String texto(String valor, String campo, int max) throws NegocioException {
        if (valor.isEmpty()) {
            throw new NegocioException("El campo " + campo + " es obligatorio.");
        }
        return textoOpcional(valor, campo, max);
    }

    public static String textoOpcional(String valor, String campo, int max) throws NegocioException {
        if (valor.length() > max) {
            throw new NegocioException("El campo " + campo + " admite máximo " + max + " caracteres.");
        }
        return valor;
    }

    public static String email(String valor) throws NegocioException {
        texto(valor, "email", 150);
        if (!valor.matches("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) {
            throw new NegocioException("El email '" + valor + "' no es válido.");
        }
        return valor.toLowerCase();
    }

    public static String telefono(String valor) throws NegocioException {
        if (!valor.matches("[0-9+\\- ]{0,30}")) {
            throw new NegocioException("El teléfono solo puede contener números, espacios, + y -.");
        }
        return valor;
    }

    /** Cédula de identidad: números y opcionalmente complemento/extensión (ej. 4715292, 4715292-1B, 4715292 SC). */
    public static String ci(String valor) throws NegocioException {
        if (!valor.matches("[0-9]{4,12}([ \\-][0-9A-Za-z]{1,4})?")) {
            throw new NegocioException("El CI '" + valor + "' no es válido (ej. 4715292 o 4715292-1B).");
        }
        return valor.toUpperCase();
    }

    public static String password(String valor) throws NegocioException {
        if (valor.length() < 6 || valor.length() > 100) {
            throw new NegocioException("La contraseña debe tener entre 6 y 100 caracteres.");
        }
        return valor;
    }

    public static String url(String valor, String campo, int max) throws NegocioException {
        texto(valor, campo, max);
        if (!valor.matches("^(https?://)?[A-Za-z0-9.\\-]+(:[0-9]+)?(/.*)?$")) {
            throw new NegocioException("El campo " + campo + " no contiene una URL válida (recibido: '"
                    + valor + "').");
        }
        return valor;
    }

    public static int enteroNoNegativo(String valor, String campo) throws NegocioException {
        if (!valor.matches("\\d{1,9}")) {
            throw new NegocioException("El campo " + campo + " debe ser un número entero mayor o igual a 0 (recibido: '"
                    + valor + "').");
        }
        return Integer.parseInt(valor);
    }

    // ===== CU3: montos, porcentajes y fechas =====

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Monto con hasta 2 decimales; acepta punto o coma (150.50 / 150,50). */
    public static BigDecimal monto(String valor, String campo) throws NegocioException {
        if (!valor.matches("\\d{1,10}([.,]\\d{1,2})?")) {
            throw new NegocioException("El campo " + campo + " debe ser un monto válido, ej. 150 o 150.50 (recibido: '"
                    + valor + "').");
        }
        return new BigDecimal(valor.replace(',', '.'));
    }

    public static BigDecimal montoPositivo(String valor, String campo) throws NegocioException {
        BigDecimal m = monto(valor, campo);
        if (m.signum() <= 0) {
            throw new NegocioException("El campo " + campo + " debe ser mayor a 0.");
        }
        return m;
    }

    /** Porcentaje entre 0 y 100. */
    public static BigDecimal porcentaje(String valor, String campo) throws NegocioException {
        BigDecimal p = monto(valor, campo);
        if (p.compareTo(new BigDecimal("100")) > 0) {
            throw new NegocioException("El campo " + campo + " es un porcentaje y no puede ser mayor a 100.");
        }
        return p;
    }

    /** Fecha en formato DD/MM/AAAA. */
    public static LocalDate fecha(String valor, String campo) throws NegocioException {
        try {
            return LocalDate.parse(valor, FECHA);
        } catch (DateTimeParseException e) {
            throw new NegocioException("El campo " + campo + " debe ser una fecha válida en formato DD/MM/AAAA (recibido: '"
                    + valor + "').");
        }
    }

    /** "" = null (campo opcional). */
    public static LocalDate fechaOpcional(String valor, String campo) throws NegocioException {
        return valor.isEmpty() ? null : fecha(valor, campo);
    }

    /** Verifica que desde <= hasta cuando ambas están presentes. */
    public static void rangoFechas(LocalDate desde, LocalDate hasta, String campoDesde, String campoHasta)
            throws NegocioException {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("El campo " + campoDesde + " no puede ser posterior a " + campoHasta + ".");
        }
    }

    /** Entero positivo opcional ("" = null). */
    public static Integer enteroOpcional(String valor, String campo) throws NegocioException {
        return valor.isEmpty() ? null : id(valor, campo);
    }

    /** NIT boliviano: solo dígitos (5 a 20). */
    public static String nit(String valor) throws NegocioException {
        if (!valor.matches("\\d{5,20}")) {
            throw new NegocioException("El NIT '" + valor + "' no es válido (solo números, 5 a 20 dígitos).");
        }
        return valor;
    }

    /** Email opcional ("" = sin email). */
    public static String emailOpcional(String valor) throws NegocioException {
        return valor.isEmpty() ? "" : email(valor);
    }
}
