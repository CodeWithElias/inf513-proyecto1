package grupo13.presentacion.comandos;

/** Resultado de ejecutar un comando: asunto y cuerpo HTML del correo de respuesta. */
public class Respuesta {

    private final boolean exito;
    private final String titulo;
    private final String html;

    private Respuesta(boolean exito, String titulo, String html) {
        this.exito = exito;
        this.titulo = titulo;
        this.html = html;
    }

    public static Respuesta exito(String titulo, String html) {
        return new Respuesta(true, titulo, html);
    }

    public static Respuesta error(String titulo, String html) {
        return new Respuesta(false, titulo, html);
    }

    public boolean isExito() {
        return exito;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getHtml() {
        return html;
    }
}
