package grupo13.presentacion;

import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Respuestas típicas que arman las clases P* (listado, detalle, éxito). */
final class Vista {

    private Vista() {
    }

    /** En el protocolo de comandos "*" significa "todos"; la capa de negocio recibe null. */
    static String filtro(String valor) {
        return valor.equals("*") ? null : valor;
    }

    static Respuesta listado(String asunto, String titulo, String[] columnas, List<String[]> filas) {
        return Respuesta.exito(asunto, Html.pagina(titulo, Html.tabla(columnas, filas)));
    }

    static Respuesta registro(String titulo, String[] columnas, String[] fila) {
        return Respuesta.exito(titulo, Html.pagina(titulo, Html.tabla(columnas, List.<String[]>of(fila))));
    }

    /** "Éxito" con un mensaje y el registro afectado. */
    static Respuesta exito(String mensaje, String[] columnas, String[] fila) {
        return exito(mensaje, Html.tabla(columnas, List.<String[]>of(fila)));
    }

    /** "Éxito" con un mensaje y HTML adicional ya armado. */
    static Respuesta exito(String mensaje, String htmlAdicional) {
        return Respuesta.exito("Éxito", Html.pagina("Éxito", Html.parrafo(mensaje) + htmlAdicional));
    }
}
