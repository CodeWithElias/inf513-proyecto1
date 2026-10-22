package grupo13.presentacion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera el HTML de los correos de respuesta y da formato a los valores que entrega la capa de negocio
 * (booleanos t/f → Sí/No, fechas ISO → DD/MM/AAAA, horas HH:MM:SS → HH:MM, NULL → "-").
 */
public final class Html {

    private static final Pattern FECHA = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern HORA = Pattern.compile("^(\\d{2}):(\\d{2}):\\d{2}$");
    private static final Pattern FECHA_HORA = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2})(:\\d{2}(\\.\\d+)?)?$");

    private Html() {
    }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Formato de presentación de un valor crudo. */
    public static String formato(String v) {
        if (v == null) {
            return "-";
        }
        if (v.equals("t")) {
            return "Sí";
        }
        if (v.equals("f")) {
            return "No";
        }
        Matcher m = FECHA.matcher(v);
        if (m.matches()) {
            return m.group(3) + "/" + m.group(2) + "/" + m.group(1);
        }
        m = HORA.matcher(v);
        if (m.matches()) {
            return m.group(1) + ":" + m.group(2);
        }
        m = FECHA_HORA.matcher(v);
        if (m.matches()) {
            return m.group(3) + "/" + m.group(2) + "/" + m.group(1) + " " + m.group(4) + ":" + m.group(5);
        }
        return v;
    }

    public static String pagina(String titulo, String contenido) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;color:#222'>"
                + "<h2 style='color:#1a4d8f'>" + esc(titulo) + "</h2>"
                + contenido
                + "<hr><small>Sistema vía correo - Grupo 13 SA - Tecnología Web</small>"
                + "</body></html>";
    }

    /** Párrafo con texto plano escapado. */
    public static String parrafo(String texto) {
        return "<p>" + esc(texto) + "</p>";
    }

    public static String subtitulo(String texto) {
        return "<h3>" + esc(texto) + "</h3>";
    }

    public static String tabla(String[] columnas, List<String[]> filas) {
        return tabla(columnas, filas, true);
    }

    public static String tabla(String[] columnas, List<String[]> filas, boolean mostrarTotal) {
        if (filas.isEmpty()) {
            return "<p><i>No se encontraron registros.</i></p>";
        }
        StringBuilder sb = new StringBuilder("<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse'>");
        sb.append("<tr style='background:#1a4d8f;color:#fff'>");
        for (String c : columnas) {
            sb.append("<th>").append(esc(c)).append("</th>");
        }
        sb.append("</tr>");
        for (String[] f : filas) {
            sb.append("<tr>");
            for (int i = 0; i < columnas.length && i < f.length; i++) {
                sb.append("<td>").append(esc(formato(f[i]))).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        if (mostrarTotal) {
            sb.append("<p>Total: ").append(filas.size()).append(" registro(s)</p>");
        }
        return sb.toString();
    }

    /** Tabla de dos columnas etiqueta → valor (ficha de un registro). */
    public static String ficha(String[][] pares) {
        StringBuilder sb = new StringBuilder("<table cellpadding='4'>");
        for (String[] par : pares) {
            sb.append("<tr><td><b>").append(esc(par[0])).append("</b></td><td>")
              .append(esc(formato(par[1]))).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }
}
