package grupo13.datos;

import grupo13.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fuente de datos externa: búsqueda pública de YouTube (mismo enfoque que la librería Python "scrapetube").
 * Descarga la página de resultados, lee el JSON "ytInitialData" y pide las páginas siguientes a la API interna
 * (youtubei/v1/search) con el token de continuación. Solo obtiene datos; no decide nada.
 * Fila de resultado: tipo ("canal" | "video"), channel_id, handle (/@... o null), nombre_canal, texto, url_video.
 *   - canal: texto = descripción del canal.
 *   - video: texto = título del video + fragmento de su descripción.
 */
public class DYoutube {

    private static final String BASE = "https://www.youtube.com";
    private static final String FILTRO_CANALES = "EgIQAg%3D%3D";
    private static final String FILTRO_VIDEOS = "EgIQAQ%3D%3D";
    private static final String AGENTE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
            + " Chrome/124.0 Safari/537.36";
    private static final Pattern DATOS = Pattern.compile("var ytInitialData = (\\{.*?\\});</script>", Pattern.DOTALL);
    private static final Pattern VERSION = Pattern.compile("\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Canales y videos que YouTube devuelve para el término (hasta 'scraping.paginas' páginas por búsqueda). */
    public List<String[]> buscar(String termino) throws IOException {
        int paginas = Integer.parseInt(Config.get("scraping.paginas"));
        List<String[]> resultados = new ArrayList<>();
        buscarCon(termino, FILTRO_CANALES, paginas, resultados);
        buscarCon(termino, FILTRO_VIDEOS, paginas, resultados);
        return resultados;
    }

    private void buscarCon(String termino, String filtro, int paginas, List<String[]> resultados) throws IOException {
        String html = get(BASE + "/results?search_query=" + URLEncoder.encode(termino, StandardCharsets.UTF_8) + "&sp=" + filtro);
        Matcher m = DATOS.matcher(html);
        if (!m.find()) {
            throw new IOException("YouTube no devolvió resultados reconocibles (posible bloqueo o cambio de formato).");
        }
        Matcher v = VERSION.matcher(html);
        String version = v.find() ? v.group(1) : "2.20240101.00.00";

        Object datos = Json.leer(m.group(1));
        for (int pagina = 1; ; pagina++) {
            extraer(datos, resultados);
            String token = continuacion(datos);
            if (token == null || pagina >= paginas) {
                return;
            }
            String cuerpo = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"" + version
                    + "\",\"hl\":\"es\",\"gl\":\"BO\"}},\"continuation\":\"" + token + "\"}";
            datos = Json.leer(post(BASE + "/youtubei/v1/search?prettyPrint=false", cuerpo));
        }
    }

    private static final Pattern VISTAS = Pattern.compile("\"viewCount\":\"(\\d+)\"");
    private static final Pattern ME_GUSTA = Pattern.compile("\"likeCount\":\"(\\d+)\"");
    private static final Pattern COMENTARIOS =
            Pattern.compile("\"contextualInfo\":\\{\"runs\":\\[\\{\"text\":\"([\\d.,]+)\"\\}\\]");

    /**
     * Estadísticas públicas de un video: {vistas, me_gusta, comentarios}. me_gusta y comentarios son null si el
     * video no los muestra (p. ej. "me gusta" ocultos o comentarios desactivados).
     */
    public String[] estadisticasVideo(String idVideo) throws IOException {
        String html = get(BASE + "/watch?v=" + URLEncoder.encode(idVideo, StandardCharsets.UTF_8));
        Matcher v = VISTAS.matcher(html);
        if (!v.find()) {
            throw new IOException("YouTube no mostró las estadísticas del video " + idVideo
                    + " (no existe, es privado o la página cambió de formato).");
        }
        Matcher g = ME_GUSTA.matcher(html);
        Matcher c = COMENTARIOS.matcher(html);
        return new String[]{v.group(1), g.find() ? g.group(1) : null, c.find() ? c.group(1).replaceAll("[.,]", "") : null};
    }

    private static void extraer(Object datos, List<String[]> resultados) {
        List<Map<String, Object>> canales = new ArrayList<>();
        Json.buscar(datos, "channelRenderer", canales);
        for (Map<String, Object> c : canales) {
            resultados.add(new String[]{"canal", (String) c.get("channelId"),
                    (String) Json.ruta(c, "navigationEndpoint", "browseEndpoint", "canonicalBaseUrl"),
                    Json.texto(c.get("title")), Json.texto(c.get("descriptionSnippet")), null});
        }
        List<Map<String, Object>> videos = new ArrayList<>();
        Json.buscar(datos, "videoRenderer", videos);
        for (Map<String, Object> vr : videos) {
            Object dueno = Json.ruta(vr, "ownerText", "runs", 0);
            String fragmento = Json.texto(Json.ruta(vr, "detailedMetadataSnippets", 0, "snippetText"));
            resultados.add(new String[]{"video",
                    (String) Json.ruta(dueno, "navigationEndpoint", "browseEndpoint", "browseId"),
                    (String) Json.ruta(dueno, "navigationEndpoint", "browseEndpoint", "canonicalBaseUrl"),
                    (String) Json.ruta(dueno, "text"),
                    Json.texto(vr.get("title")) + (fragmento == null ? "" : " — " + fragmento),
                    BASE + "/watch?v=" + vr.get("videoId")});
        }
    }

    private static String continuacion(Object datos) {
        List<Map<String, Object>> comandos = new ArrayList<>();
        Json.buscar(datos, "continuationCommand", comandos);
        return comandos.isEmpty() ? null : (String) comandos.get(0).get("token");
    }

    private String get(String url) throws IOException {
        return enviar(HttpRequest.newBuilder(URI.create(url)).GET());
    }

    private String post(String url, String json) throws IOException {
        return enviar(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)));
    }

    private String enviar(HttpRequest.Builder b) throws IOException {
        HttpRequest req = b.header("User-Agent", AGENTE).header("Accept-Language", "es-419,es;q=0.9")
                .timeout(Duration.ofSeconds(30)).build();
        try {
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() != 200) {
                throw new IOException("YouTube respondió HTTP " + r.statusCode() + ".");
            }
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Búsqueda interrumpida.");
        }
    }
}
