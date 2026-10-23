package grupo13.presentacion;

import grupo13.negocio.NScraping;
import grupo13.negocio.NegocioException;
import grupo13.negocio.ResultadoScraping;
import grupo13.presentacion.comandos.Respuesta;
import grupo13.presentacion.correo.Notificador;

import java.util.ArrayList;
import java.util.List;

/** Presentación del CU6 – Ejecución del scraping: ejeScr, lisEje y la ejecución periódica que dispara App. */
public class PScraping {

    private static final String[] COLUMNAS = {"ID", "Plataforma", "Inicio", "Fin", "Estado", "Errores",
            "Detecciones", "Páginas nuevas"};

    private final NScraping negocio = new NScraping();

    /** ejeScr["id_plataforma","id_termino" o "*"]: ejecución manual con un término o con todos los activos. */
    public Respuesta ejecutar(List<String> p) throws NegocioException {
        ResultadoScraping r = negocio.ejecutar(p.get(0), Vista.filtro(p.get(1)));
        String html = Html.ficha(new String[][]{
                {"Ejecución", String.valueOf(r.getIdEjecucion())}, {"Plataforma", r.getPlataforma()},
                {"Estado", r.getEstado()}, {"Términos buscados", r.getTerminos()},
                {"Cuentas con los términos", String.valueOf(r.getCuentasRevisadas())},
                {"Redes oficiales de red_social (autorizadas)", String.valueOf(r.getOficiales())},
                {"Páginas no autorizadas nuevas", String.valueOf(r.getNuevas())},
                {"Ya registradas (nueva detección)", String.valueOf(r.getYaRegistradas())},
                {"Errores", r.getErrores()}});
        html += Html.subtitulo("Alertas al administrador");
        if (r.getAvisos().isEmpty()) {
            html += Html.parrafo(r.getNuevas() == 0 ? "No hubo páginas nuevas: no se envían alertas."
                    : "No hay usuarios activos con el rol de alertas configurado (las páginas igual quedaron registradas).");
        } else {
            html += lista(Notificador.enviar(r.getAvisos()));
        }
        html += Html.parrafo("Revise las páginas detectadas con lisPag[\"detectada\"] y mosPag[\"id\"].");
        return Vista.exito("Scraping de " + r.getPlataforma() + " finalizado (" + r.getEstado() + ").", html);
    }

    /** lisEje["id_plataforma" o "*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Ejecuciones de scraping", "Historial de ejecuciones de scraping", COLUMNAS,
                negocio.listarEjecuciones(Vista.filtro(p.get(0))));
    }

    /** Ejecución periódica (la llama App): ejecuta, envía las alertas y devuelve líneas para el registro de consola. */
    public List<String> ejecutarProgramado() {
        List<String> log = new ArrayList<>();
        try {
            List<ResultadoScraping> resultados = negocio.ejecutarProgramadas();
            if (resultados.isEmpty()) {
                log.add("Scraping: no hay plataformas con scraping disponible registradas (registre YouTube con insPla).");
            }
            for (ResultadoScraping r : resultados) {
                log.add("Scraping " + r.getPlataforma() + " (ejecución " + r.getIdEjecucion() + ", " + r.getEstado()
                        + ", términos: " + r.getTerminos() + "): "
                        + r.getCuentasRevisadas() + " cuentas, " + r.getOficiales() + " oficiales, " + r.getNuevas()
                        + " nuevas, " + r.getYaRegistradas() + " ya registradas"
                        + (r.getErrores() == null ? "" : ". Errores: " + r.getErrores()));
                log.addAll(Notificador.enviar(r.getAvisos()));
            }
        } catch (NegocioException e) {
            log.add("Scraping no ejecutado: " + e.getMessage());
        }
        return log;
    }

    private static String lista(List<String> lineas) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (String l : lineas) {
            sb.append("<li>").append(Html.esc(l)).append("</li>");
        }
        return sb.append("</ul>").toString();
    }
}
