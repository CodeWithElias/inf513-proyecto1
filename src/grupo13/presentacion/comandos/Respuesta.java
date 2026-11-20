package grupo13.presentacion.comandos;

public class Respuesta {

    private final boolean exito;
    private final String titulo;
    private final String html;
    private final String detalle;

    private Respuesta(boolean exito, String titulo, String html, String detalle) {
        this.exito = exito;
        this.titulo = titulo;
        this.html = html;
        this.detalle = detalle;
    }

    public static Respuesta exito(String titulo, String html) {
        return new Respuesta(true, titulo, html, titulo);
    }

    public static Respuesta error(String titulo, String html) {
        return new Respuesta(false, titulo, html, titulo);
    }

    /** detalle: motivo del error en una línea, para la bitácora (access.log). */
    public static Respuesta error(String titulo, String html, String detalle) {
        return new Respuesta(false, titulo, html, detalle);
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

    /** Resumen de una línea: el título si salió bien, el motivo si falló. */
    public String getDetalle() {
        return detalle;
    }
}
