package grupo13.mail;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades para cabeceras MIME (RFC 2047), p. ej. asuntos con tildes. */
public final class MimeUtil {

    private static final Pattern ENCODED_WORD =
            Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=");
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private MimeUtil() {
    }

    /**
     * Decodifica una cabecera. La línea viene leída como ISO-8859-1 (bytes crudos),
     * así que el texto sin codificar se reinterpreta como UTF-8.
     */
    public static String decodificarCabecera(String crudo) {
        // Espacios entre dos encoded-words consecutivos se ignoran (RFC 2047)
        String valor = crudo.replaceAll("\\?=\\s+=\\?", "?==?");
        Matcher m = ENCODED_WORD.matcher(valor);
        StringBuilder sb = new StringBuilder();
        int ultimo = 0;
        while (m.find()) {
            sb.append(aUtf8(valor.substring(ultimo, m.start())));
            sb.append(decodificarPalabra(m.group(1), m.group(2), m.group(3)));
            ultimo = m.end();
        }
        sb.append(aUtf8(valor.substring(ultimo)));
        return sb.toString();
    }

    private static String aUtf8(String latin1) {
        return new String(latin1.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    private static String decodificarPalabra(String charset, String tipo, String texto) {
        try {
            Charset cs = Charset.forName(charset);
            if (tipo.equalsIgnoreCase("B")) {
                return new String(Base64.getDecoder().decode(texto), cs);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i < texto.length(); i++) {
                char c = texto.charAt(i);
                if (c == '_') {
                    out.write(' ');
                } else if (c == '=' && i + 2 < texto.length()) {
                    out.write(Integer.parseInt(texto.substring(i + 1, i + 3), 16));
                    i += 2;
                } else {
                    out.write(c);
                }
            }
            return out.toString(cs);
        } catch (Exception e) {
            return texto;
        }
    }

    /** Codifica un asunto en UTF-8/Base64 para que las tildes lleguen bien. */
    public static String codificarCabecera(String texto) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    /** Extrae la dirección de correo de "Nombre <correo@x.com>". */
    public static String extraerEmail(String from) {
        Matcher m = EMAIL.matcher(from);
        return m.find() ? m.group() : from.trim();
    }
}
