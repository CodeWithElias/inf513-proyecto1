package grupo13.util;

import java.util.List;

/** Genera el HTML de los correos de respuesta. */
public final class Html {

    private Html() {
    }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String pagina(String titulo, String contenido) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;color:#222'>"
                + "<h2 style='color:#1a4d8f'>" + esc(titulo) + "</h2>"
                + contenido
                + "<hr><small>Sistema vía correo - Grupo 13 SA - Tecnología Web</small>"
                + "</body></html>";
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
            for (String v : f) {
                sb.append("<td>").append(esc(v)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        if (mostrarTotal) {
            sb.append("<p>Total: ").append(filas.size()).append(" registro(s)</p>");
        }
        return sb.toString();
    }
}
