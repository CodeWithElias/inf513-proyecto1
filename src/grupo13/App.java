package grupo13;

import grupo13.presentacion.Bitacora;
import grupo13.presentacion.Html;
import grupo13.presentacion.PMetrica;
import grupo13.presentacion.PScraping;
import grupo13.presentacion.comandos.Despachador;
import grupo13.presentacion.comandos.Respuesta;
import grupo13.config.Config;
import grupo13.presentacion.correo.ClientePOP3;
import grupo13.presentacion.correo.ClienteSMTP;
import grupo13.presentacion.correo.Correo;
import grupo13.presentacion.correo.Notificador;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Servidor de comandos vía correo electrónico.
 *
 *   java -cp ... grupo13.App                    -> revisa el buzón cada N segundos y ejecuta el scraping cada N horas
 *   java -cp ... grupo13.App --probar "ASUNTO"  -> ejecuta un comando sin correo, como el primer administrador activo
 *   java -cp ... grupo13.App --probar --como correo@x.com "ASUNTO"  -> lo ejecuta como ese remitente
 *
 * Bitácora para la auditoría en logs/access.log (cada correo) y logs/error.log (cada excepción capturada).
 */
public class App {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Despachador despachador = new Despachador();
    private final PScraping scraping = new PScraping();
    private final PMetrica metricas = new PMetrica();
    private final ClienteSMTP smtp = new ClienteSMTP(
            Config.get("smtp.host"), Config.getInt("smtp.puerto"), Config.get("mail.direccion"));

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        App app = new App();

        if (args.length >= 2 && args[0].equals("--probar")) {
            Notificador.desactivar(); // en pruebas los avisos (RF11) se registran pero no se envían por correo
            int desde = 1;
            String remitente;
            if (args.length >= 4 && args[1].equals("--como")) {
                remitente = args[2];
                desde = 3;
            } else {
                remitente = app.despachador.remitentePorDefecto();
                if (remitente == null) {
                    System.out.println("No hay un administrador activo; use --probar --como correo@x.com COMANDO");
                    return;
                }
            }
            String comando = String.join(" ", java.util.Arrays.copyOfRange(args, desde, args.length));
            System.out.println("Remitente: " + remitente);
            Respuesta r = app.despachador.procesar(remitente, comando);
            System.out.println("Asunto respuesta: " + r.getTitulo());
            System.out.println(r.getHtml());
            return;
        }

        int intervalo = Config.getInt("app.intervalo");
        long cadaScraping = Config.getInt("scraping.intervalo.horas") * 3600_000L;
        log("Escuchando " + Config.get("mail.direccion") + " cada " + intervalo + " s (Ctrl+C para salir)");
        log(cadaScraping > 0 ? "Scraping automático (CU6) cada " + Config.get("scraping.intervalo.horas") + " h"
                : "Scraping automático desactivado (solo manual con ejeScr)");
        long proximoScraping = System.currentTimeMillis(); // la primera ejecución es al arrancar
        while (true) {
            try {
                app.revisarBuzon();
            } catch (IOException e) {
                log("Error de conexión: " + e.getMessage());
                Bitacora.error("POP3", "No se pudo leer el buzón: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                log("Error inesperado: " + e);
                Bitacora.error("INESPERADO", "Fallo al procesar el buzón: " + e, e);
            }
            if (cadaScraping > 0 && System.currentTimeMillis() >= proximoScraping) {
                app.scrapingProgramado();
                proximoScraping = System.currentTimeMillis() + cadaScraping;
            }
            Thread.sleep(intervalo * 1000L);
        }
    }

    /** CU6 / RF12 y CU7 / RF13: monitoreo de páginas no autorizadas y captura de métricas de YouTube. */
    private void scrapingProgramado() {
        try {
            log("Iniciando scraping programado...");
            for (String linea : scraping.ejecutarProgramado()) {
                log("  " + linea);
            }
            log("Capturando métricas de las pautas publicadas en YouTube (CU7)...");
            for (String linea : metricas.capturarProgramado()) {
                log("  " + linea);
            }
        } catch (RuntimeException e) {
            log("Error en el proceso programado: " + e);
            Bitacora.error("PROGRAMADO", "Fallo en el scraping o la captura de métricas: " + e, e);
        }
    }

    /**
     * Descarga y borra los correos nuevos, luego responde cada uno. Cada correo se procesa aislado: si falla de forma
     * inesperada, se avisa al remitente y se borra igual, para que no bloquee a los siguientes en cada vuelta.
     */
    private void revisarBuzon() throws IOException {
        try (ClientePOP3 pop = new ClientePOP3(Config.get("pop3.host"), Config.getInt("pop3.puerto"))) {
            pop.login(Config.get("mail.usuario"), Config.get("mail.password"));
            int total = pop.cantidadMensajes();
            for (int i = 1; i <= total; i++) {
                Correo correo = pop.obtener(i);
                try {
                    procesar(correo);
                } catch (RuntimeException e) {
                    log("  Error inesperado: " + e);
                    Bitacora.error("INESPERADO", correo.getRemitente(), correo.getAsunto(), String.valueOf(e), e);
                    responderFalla(correo);
                }
                pop.eliminar(i);
            }
        } // QUIT confirma la eliminación
    }

    /** Respuesta de último recurso: el remitente siempre sabe que su correo llegó aunque no se pudo procesar. */
    private void responderFalla(Correo correo) {
        String html = Html.pagina("Error",
                "<p>Ocurrió un error interno al procesar su correo con el asunto <code>" + Html.esc(correo.getAsunto())
                        + "</code>.</p><p style='color:#b00020'>Es posible que la orden no se haya ejecutado: verifíquelo"
                        + " antes de reenviarla. Si el problema continúa, comuníquese con el administrador.</p>");
        try {
            smtp.enviar(correo.getRemitente(), "Error - " + correo.getAsunto(), html);
        } catch (IOException | RuntimeException e) {
            Bitacora.error("SMTP", correo.getRemitente(), correo.getAsunto(),
                    "No se pudo avisar del error interno: " + e.getMessage(), e);
        }
    }

    private void procesar(Correo correo) {
        log("Recibido -> " + correo);
        String remitente = correo.getRemitente().toLowerCase();
        // Evita bucles: no responder a nosotros mismos ni a rebotes del servidor (ni a un correo sin remitente)
        if (remitente.isBlank() || remitente.equals(Config.get("mail.direccion").toLowerCase())
                || remitente.contains("mailer-daemon") || remitente.contains("postmaster")) {
            log("  Ignorado (remitente del sistema o vacío)");
            Bitacora.acceso(remitente, "-", correo.getAsunto(), "Ignorado", "remitente del sistema, rebote o vacío", 0);
            return;
        }

        Respuesta asunto = despachador.procesar(correo.getRemitente(), correo.getAsunto());
        try {
            smtp.enviar(correo.getRemitente(), asunto.getTitulo() + " - " + correo.getAsunto(), asunto.getHtml());
            log("  Respondido: " + asunto.getTitulo());
        } catch (IOException e) {
            log("  No se pudo enviar la respuesta: " + e.getMessage());
            Bitacora.error("SMTP", correo.getRemitente(), correo.getAsunto(),
                    "No se pudo enviar la respuesta: " + e.getMessage(), e);
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + msg);
    }
}
