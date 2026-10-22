package grupo13.presentacion;

import grupo13.negocio.NNotificacion;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU5 – Notificaciones: lisNot, leeNot. */
public class PNotificacion {

    private static final String[] COLUMNAS = {"ID", "Fecha", "Medio", "Título", "Mensaje", "Leída", "Pauta"};

    private final NNotificacion negocio = new NNotificacion();

    /** lisNot["id_usuario"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        String titulo = "Notificaciones del usuario " + p.get(0);
        return Vista.listado(titulo, titulo, COLUMNAS, negocio.listar(p.get(0)));
    }

    /** leeNot["id_notificacion"] */
    public Respuesta marcarLeida(List<String> p) throws NegocioException {
        int idUsuario = negocio.marcarLeida(p.get(0));
        return Vista.exito("La notificación " + p.get(0) + " fue marcada como leída.",
                Html.tabla(COLUMNAS, negocio.listar(String.valueOf(idUsuario))));
    }
}
