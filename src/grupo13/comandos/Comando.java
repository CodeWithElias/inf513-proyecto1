package grupo13.comandos;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comando leído del asunto del correo.
 * Formato: NOMBRE["param1","param2",...]   (help también se acepta sin corchetes)
 */
public class Comando {

    private static final Pattern FORMATO = Pattern.compile("^([A-Za-z]+)\\s*(?:\\[(.*)\\])?\\s*$", Pattern.DOTALL);

    private final String nombre;
    private final List<String> parametros;

    private Comando(String nombre, List<String> parametros) {
        this.nombre = nombre;
        this.parametros = parametros;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getParametros() {
        return parametros;
    }

    public static Comando parsear(String asunto) throws ComandoException {
        if (asunto == null || asunto.isBlank()) {
            throw new ComandoException("El asunto está vacío. Envíe help para ver los comandos disponibles.");
        }
        // Los celulares a veces cambian " por comillas tipográficas
        String texto = asunto.trim().replace('“', '"').replace('”', '"');

        Matcher m = FORMATO.matcher(texto);
        if (!m.matches()) {
            throw new ComandoException("Formato inválido: " + grupo13.util.Html.esc(asunto)
                    + ". Use COMANDO[\"param1\",\"param2\",...]");
        }
        String nombre = m.group(1);
        List<String> params = m.group(2) == null ? new ArrayList<>() : parsearParametros(m.group(2));
        return new Comando(nombre, params);
    }

    /** Separa "a","b , c","d" respetando comas dentro de las comillas. */
    private static List<String> parsearParametros(String texto) throws ComandoException {
        List<String> lista = new ArrayList<>();
        int i = 0;
        int n = texto.length();
        i = saltarEspacios(texto, i);
        if (i == n) {
            return lista; // NOMBRE[]
        }
        while (true) {
            if (texto.charAt(i) != '"') {
                throw new ComandoException("Cada parámetro debe ir entre comillas dobles (\"...\").");
            }
            int cierre = texto.indexOf('"', i + 1);
            if (cierre < 0) {
                throw new ComandoException("Falta cerrar comillas en los parámetros.");
            }
            lista.add(texto.substring(i + 1, cierre).trim());
            i = saltarEspacios(texto, cierre + 1);
            if (i == n) {
                return lista;
            }
            if (texto.charAt(i) != ',') {
                throw new ComandoException("Los parámetros deben separarse con comas.");
            }
            i = saltarEspacios(texto, i + 1);
            if (i == n) {
                throw new ComandoException("Hay una coma de más al final de los parámetros.");
            }
        }
    }

    private static int saltarEspacios(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }
}
