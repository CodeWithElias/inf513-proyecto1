package grupo13.presentacion;

import grupo13.negocio.NPauta;
import grupo13.negocio.NegocioException;
import grupo13.negocio.ResultadoPauta;
import grupo13.presentacion.comandos.Respuesta;
import grupo13.presentacion.correo.Notificador;

import java.util.List;

/** Presentación del CU5 – Programación de pauta: lisPau, mosPau, insPau, repPau, canPau, pubPau. */
public class PPauta {

    public static final String[] PARAMS_PROGRAMAR = {"id_detalle", "id_material", "id_espacio"};

    private static final String[] COLUMNAS = {"ID", "Campaña", "Empresa", "Canal", "Tipo de canal", "Fecha y hora",
            "Material", "Tipo de material", "Estado", "URL publicación", "Programado por"};

    private final NPauta negocio = new NPauta();

    /** lisPau["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando la Pauta", "Pauta Multicanal", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosPau["id"] */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Pauta " + p.get(0), COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** insPau["id_detalle","id_material","id_espacio"] (programa el remitente) */
    public Respuesta programar(List<String> p) throws NegocioException {
        ResultadoPauta r = negocio.programar(p.get(0), p.get(1), p.get(2));
        return conAvisos("La pauta fue programada con ID " + r.getIdProgramacion() + ".", r);
    }

    /** repPau["id_programacion","id_espacio_nuevo"] */
    public Respuesta reprogramar(List<String> p) throws NegocioException {
        ResultadoPauta r = negocio.reprogramar(p.get(0), p.get(1));
        return conAvisos("La pauta " + p.get(0) + " fue reprogramada; el espacio anterior quedó disponible.", r);
    }

    /** canPau["id_programacion"] */
    public Respuesta cancelar(List<String> p) throws NegocioException {
        ResultadoPauta r = negocio.cancelar(p.get(0));
        return conAvisos("La pauta " + p.get(0) + " fue cancelada y su espacio quedó disponible.", r);
    }

    /** pubPau["id_programacion","url_publicacion"] */
    public Respuesta publicar(List<String> p) throws NegocioException {
        negocio.publicar(p.get(0), p.get(1));
        return Vista.exito("La pauta " + p.get(0) + " fue marcada como publicada.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** Respuesta de éxito con la pauta y el resultado del envío de los avisos (RF11). */
    private Respuesta conAvisos(String mensaje, ResultadoPauta r) throws NegocioException {
        String html = Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(String.valueOf(r.getIdProgramacion()))));
        html += Html.subtitulo("Notificaciones a la unidad (RF11)");
        if (r.getAvisos().isEmpty()) {
            html += Html.parrafo("El canal no es una red social institucional: no se notifica a ninguna unidad.");
        } else {
            StringBuilder lista = new StringBuilder("<ul>");
            for (String linea : Notificador.enviar(r.getAvisos())) {
                lista.append("<li>").append(Html.esc(linea)).append("</li>");
            }
            html += lista.append("</ul>");
        }
        return Vista.exito(mensaje, html);
    }
}
