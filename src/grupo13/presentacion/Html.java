package grupo13.presentacion;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera el HTML de los correos de respuesta y da formato a los valores que entrega la capa de negocio
 * (booleanos t/f → Sí/No, fechas ISO → DD/MM/AAAA, meses AAAA-MM → MM/AAAA, horas HH:MM:SS → HH:MM, NULL → "-").
 */
public final class Html {

    private static final Pattern FECHA = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern HORA = Pattern.compile("^(\\d{2}):(\\d{2}):\\d{2}$");
    private static final Pattern MES = Pattern.compile("^(\\d{4})-(\\d{2})$");
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
        m = MES.matcher(v);
        if (m.matches()) {
            return m.group(2) + "/" + m.group(1);
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

    /**
     * Gráfico de barras horizontales hecho con una tabla y estilos en línea (se ve en clientes de correo, sin imágenes
     * ni JavaScript). Usa la columna 'colEtiqueta' como rótulo y 'colValor' como valor numérico.
     */
    public static String grafico(String titulo, List<String[]> filas, int colEtiqueta, int colValor, String unidad) {
        double max = 0;
        for (String[] f : filas) {
            max = Math.max(max, numero(f[colValor]));
        }
        if (filas.isEmpty() || max <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<p style='margin:12px 0 4px'><b>").append(esc(titulo))
                .append("</b></p><table cellpadding='3' cellspacing='0' style='border-collapse:collapse'>");
        for (String[] f : filas) {
            double v = numero(f[colValor]);
            long ancho = v <= 0 ? 0 : Math.max(1, Math.round(v * 100 / max));
            sb.append("<tr><td style='padding-right:8px;white-space:nowrap'>").append(esc(formato(f[colEtiqueta])))
              .append("</td><td style='width:320px'><div style='background:#1a4d8f;height:14px;width:").append(ancho)
              .append("%'></div></td><td style='padding-left:8px;white-space:nowrap'>")
              .append(esc(formato(f[colValor]))).append(unidad.isEmpty() ? "" : " " + esc(unidad)).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    private static double numero(String v) {
        try {
            return v == null ? 0 : Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
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
