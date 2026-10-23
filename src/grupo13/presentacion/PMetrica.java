package grupo13.presentacion;

import grupo13.negocio.NMetrica;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU7 – Métricas: insMet, modMet, eliMet, lisMet, capMet. */
public class PMetrica {

    public static final String[] PARAMS_VALORES = {"alcance", "impresiones", "interacciones", "clics", "compartidos"};

    private static final String[] COLUMNAS = {"ID", "Fecha de medición", "Alcance", "Impresiones", "Interacciones",
            "Clics", "Compartidos", "Pauta"};

    private final NMetrica negocio = new NMetrica();

    /** lisMet["id_programacion"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        String titulo = "Mediciones de la pauta " + p.get(0);
        return Vista.listado(titulo, titulo, COLUMNAS, negocio.listar(p.get(0)));
    }

    /** insMet["id_programacion","alcance","impresiones","interacciones","clics","compartidos"] */
    public Respuesta registrar(List<String> p) throws NegocioException {
        int id = negocio.registrar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("Se registró la medición " + id + " de la pauta " + p.get(0) + ". Vea el análisis con anaPau[\""
                + p.get(0) + "\"].", COLUMNAS, negocio.obtener(id));
    }

    /** modMet["id_metrica","alcance","impresiones","interacciones","clics","compartidos"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("La medición " + p.get(0) + " fue corregida.", COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** eliMet["id_metrica"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        int idPauta = negocio.eliminar(p.get(0));
        return Vista.exito("La medición " + p.get(0) + " fue eliminada.",
                Html.subtitulo("Mediciones de la pauta " + idPauta) + Html.tabla(COLUMNAS, negocio.listar(String.valueOf(idPauta))));
    }

    /** capMet["id_programacion" o "*"]: captura automática desde YouTube. */
    public Respuesta capturar(List<String> p) throws NegocioException {
        if (p.get(0).equals("*")) {
            List<String> r = negocio.capturarTodas();
            String html = r.isEmpty()
                    ? Html.parrafo("No hay pautas publicadas en redes de YouTube.")
                    : lista(r);
            return Vista.exito("Captura de métricas desde YouTube (" + r.size() + " pauta(s)).", html);
        }
        String[] r = negocio.capturar(p.get(0));
        String detalle = r[1] + " vistas, " + (r[2] == null ? "me gusta ocultos" : r[2] + " me gusta") + ", "
                + (r[3] == null ? "comentarios no disponibles" : r[3] + " comentarios");
        return Vista.exito("Se capturaron las métricas de la pauta " + p.get(0) + " desde YouTube: " + detalle + ".",
                Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(Integer.parseInt(r[0]))))
                        + Html.parrafo("YouTube no publica alcance, clics ni compartidos: se mantienen de la última medición"
                        + " manual. Para completarlos use modMet[\"" + r[0] + "\",...]."));
    }

    /** Captura periódica (la llama App junto con el scraping): líneas para el registro de consola. */
    public List<String> capturarProgramado() {
        try {
            return negocio.capturarTodas();
        } catch (NegocioException e) {
            return List.of("Captura de métricas no ejecutada: " + e.getMessage());
        }
    }

    private static String lista(List<String> lineas) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (String l : lineas) {
            sb.append("<li>").append(Html.esc(l)).append("</li>");
        }
        return sb.append("</ul>").toString();
    }
}
