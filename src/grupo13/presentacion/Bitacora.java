package grupo13.presentacion;

import grupo13.config.Config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bitácora para la auditoría: archivos locales en la carpeta log.carpeta (por defecto "logs").
 *   access.log -> cada correo procesado: fecha | remitente | rol | asunto | resultado | detalle | duración
 *   error.log  -> cada excepción capturada: fecha | tipo | remitente | asunto | mensaje (+ traza si es inesperada)
 * Nunca lanza excepción: si no puede escribir, avisa por la consola y el sistema sigue funcionando.
 */
public final class Bitacora {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path CARPETA = Path.of(Config.get("log.carpeta"));
    private static final Path ACCESO = CARPETA.resolve("access.log");
    private static final Path ERROR = CARPETA.resolve("error.log");

    private Bitacora() {
    }

    public static void acceso(String remitente, String rol, String asunto, String resultado, String detalle, long ms) {
        escribir(ACCESO, campo(remitente) + " | " + campo(rol) + " | " + campo(asunto) + " | " + campo(resultado)
                + " | " + campo(detalle) + " | " + ms + " ms");
    }

    /** Excepción capturada. Con causa se agrega su traza completa (errores inesperados y de base de datos). */
    public static void error(String tipo, String remitente, String asunto, String mensaje, Throwable causa) {
        String linea = campo(tipo) + " | " + campo(remitente) + " | " + campo(asunto) + " | " + campo(mensaje);
        if (causa != null) {
            StringWriter traza = new StringWriter();
            causa.printStackTrace(new PrintWriter(traza));
            linea += System.lineSeparator() + traza.toString().stripTrailing();
        }
        escribir(ERROR, linea);
    }

    public static void error(String tipo, String mensaje, Throwable causa) {
        error(tipo, "-", "-", mensaje, causa);
    }

    private static String campo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor.replaceAll("<[^>]*>", " ").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&").replace('|', '/').replaceAll("\\s+", " ").trim();
    }

    private static synchronized void escribir(Path archivo, String linea) {
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(archivo, LocalDateTime.now().format(FECHA) + " | " + linea + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("[Bitacora] No se pudo escribir en " + archivo + ": " + e.getMessage());
        }
    }
}
