package grupo13.presentacion;

import grupo13.negocio.NMaterial;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU4 – Material publicitario: lisMat, mosMat, insMat, modMat, subMat, eliMat, aprMat, recMat, corMat, lisRev. */
public class PMaterial {

    public static final String[] PARAMS_INSERTAR =
            {"id_campania", "tipo", "nombre_archivo", "url_archivo", "duracion_seg", "texto_publicacion"};
    public static final String[] PARAMS_MODIFICAR =
            {"id_material", "tipo", "nombre_archivo", "url_archivo", "duracion_seg", "texto_publicacion"};
    public static final String[] PARAMS_VERSION =
            {"id_material", "nombre_archivo", "url_archivo", "duracion_seg", "texto_publicacion"};
    public static final String[] PARAMS_REVISION = {"id_material", "observaciones"};

    private static final String[] COLUMNAS = {"ID", "Campaña", "Empresa", "Tipo", "Archivo", "Formato", "URL",
            "Duración (s)", "Versión", "Estado", "Fecha de carga", "Subido por"};
    private static final String[] COLUMNAS_REV = {"ID", "Fecha", "Resultado", "Observaciones", "Revisor"};

    private final NMaterial negocio = new NMaterial();

    /** lisMat["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando el Material", "Material Publicitario", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosMat["id"]: ficha del material, texto de la publicación e historial de revisiones. */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Respuesta.exito("Material " + p.get(0), Html.pagina("Material " + p.get(0), ficha(p.get(0), true)));
    }

    /** insMat["id_campania","tipo","nombre_archivo","url_archivo","duracion_seg","texto_publicacion"] (carga el remitente) */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("El material fue cargado con ID " + id + " y quedó pendiente de revisión por Canal 11.",
                ficha(String.valueOf(id), false));
    }

    /** modMat["id","tipo","nombre_archivo","url_archivo","duracion_seg","texto_publicacion"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("El material " + p.get(0) + " fue modificado.", ficha(p.get(0), false));
    }

    /** subMat["id","nombre_archivo","url_archivo","duracion_seg","texto_publicacion"] */
    public Respuesta subirVersion(List<String> p) throws NegocioException {
        int version = negocio.subirVersion(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4));
        return Vista.exito("Se cargó la versión " + version + " del material " + p.get(0)
                + "; quedó nuevamente pendiente de revisión.", ficha(p.get(0), true));
    }

    /** eliMat["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El material " + p.get(0) + " fue retirado.", ficha(p.get(0), false));
    }

    /** aprMat["id","observaciones"] (revisa el remitente) */
    public Respuesta aprobar(List<String> p) throws NegocioException {
        negocio.aprobar(p.get(0), p.get(1));
        return Vista.exito("El material " + p.get(0) + " fue aprobado. Ya puede programarse en la pauta (CU5).",
                ficha(p.get(0), true));
    }

    /** recMat["id","observaciones"] (revisa el remitente) */
    public Respuesta rechazar(List<String> p) throws NegocioException {
        negocio.rechazar(p.get(0), p.get(1));
        return Vista.exito("El material " + p.get(0) + " fue rechazado.", ficha(p.get(0), true));
    }

    /** corMat["id","observaciones"] (revisa el remitente) */
    public Respuesta solicitarCorreccion(List<String> p) throws NegocioException {
        negocio.solicitarCorreccion(p.get(0), p.get(1));
        return Vista.exito("Se solicitaron correcciones para el material " + p.get(0) + ". El cliente debe subir una nueva"
                + " versión con subMat[\"" + p.get(0) + "\",\"nombre_archivo\",\"url\",\"duracion\",\"texto\"].", ficha(p.get(0), true));
    }

    /** lisRev["id_material"] */
    public Respuesta listarRevisiones(List<String> p) throws NegocioException {
        String titulo = "Revisiones del material " + p.get(0);
        return Vista.listado(titulo, titulo, COLUMNAS_REV, negocio.listarRevisiones(p.get(0)));
    }

    /** Ficha del material; con 'conRevisiones' agrega el historial. */
    private String ficha(String id, boolean conRevisiones) throws NegocioException {
        String[] f = negocio.obtener(id);
        String html = Html.ficha(new String[][]{
                {"ID", f[0]}, {"Campaña", f[1]}, {"Empresa", f[2]}, {"Tipo", f[3]}, {"Archivo", f[4]},
                {"Formato", f[5]}, {"URL", f[6]}, {"Duración (s)", f[7]}, {"Versión", f[8]}, {"Estado", f[9]},
                {"Fecha de carga", f[10]}, {"Subido por", f[11]}, {"Texto de la publicación", f[12]}});
        if (conRevisiones) {
            html += Html.subtitulo("Historial de revisiones") + Html.tabla(COLUMNAS_REV, negocio.listarRevisiones(id));
        }
        return html;
    }
}
