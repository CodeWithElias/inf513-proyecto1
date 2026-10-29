package grupo13.presentacion;

import grupo13.negocio.NPagina;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU6 – Páginas no autorizadas: lisPag, mosPag, modPag. */
public class PPagina {

    private static final String[] COLUMNAS = {"ID", "URL", "Nombre", "Plataforma", "Estado", "Detecciones",
            "Última detección", "Revisada", "Revisado por"};
    private static final String[] COLUMNAS_DET = {"ID", "Fecha", "Término", "Evidencia", "Ejecución"};

    private final NPagina negocio = new NPagina();

    /** lisPag["*"] o lisPag["detectada"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Páginas no autorizadas", "Páginas no autorizadas", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosPag["id"]: la página y sus detecciones con evidencia. */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Respuesta.exito("Página no autorizada " + p.get(0),
                Html.pagina("Página no autorizada " + p.get(0), detalle(p.get(0))));
    }

    /** modPag["id_pagina","estado"] (revisa el remitente) */
    public Respuesta revisar(List<String> p) throws NegocioException {
        negocio.revisar(p.get(0), p.get(1));
        return Vista.exito("La página " + p.get(0) + " pasó a estado " + p.get(1).toLowerCase() + ".", detalle(p.get(0)));
    }

    private String detalle(String id) throws NegocioException {
        return Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(id)))
                + Html.subtitulo("Detecciones (evidencia)") + Html.tabla(COLUMNAS_DET, negocio.listarDetecciones(id));
    }
}
