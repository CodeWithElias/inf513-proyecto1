package grupo13;

import grupo13.comandos.Despachador;
import grupo13.comandos.Respuesta;
import grupo13.config.Config;
import grupo13.mail.ClientePOP3;
import grupo13.mail.ClienteSMTP;
import grupo13.mail.Correo;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Servidor de comandos vía correo electrónico.
 *
 *   java -cp ... grupo13.App                    -> revisa el buzón cada N segundos
 *   java -cp ... grupo13.App --probar "ASUNTO"  -> ejecuta un comando sin correo (pruebas)
 */
public class App {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Despachador despachador = new Despachador();
    private final ClienteSMTP smtp = new ClienteSMTP(
            Config.get("smtp.host"), Config.getInt("smtp.puerto"), Config.get("mail.direccion"));

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        App app = new App();

        if (args.length >= 2 && args[0].equals("--probar")) {
            String comando = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            Respuesta r = app.despachador.procesar(comando);
            System.out.println("Asunto respuesta: " + r.getTitulo());
            System.out.println(r.getHtml());
            return;
        }

        int intervalo = Config.getInt("app.intervalo");
        log("Escuchando " + Config.get("mail.direccion") + " cada " + intervalo + " s (Ctrl+C para salir)");
        while (true) {
            try {
                app.revisarBuzon();
            } catch (IOException e) {
                log("Error de conexión: " + e.getMessage());
            }
            Thread.sleep(intervalo * 1000L);
        }
    }

    /** Descarga y borra los correos nuevos, luego responde cada uno. */
    private void revisarBuzon() throws IOException {
        try (ClientePOP3 pop = new ClientePOP3(Config.get("pop3.host"), Config.getInt("pop3.puerto"))) {
            pop.login(Config.get("mail.usuario"), Config.get("mail.password"));
            int total = pop.cantidadMensajes();
            for (int i = 1; i <= total; i++) {
                //correos.add(pop.obtener(i));
                procesar(pop.obtener(i));
                pop.eliminar(i);
            }
        } // QUIT confirma la eliminación

    }

    private void procesar(Correo correo) {
        log("Recibido -> " + correo);
        String remitente = correo.getRemitente().toLowerCase();
        // Evita bucles: no responder a nosotros mismos ni a rebotes del servidor
        if (remitente.equals(Config.get("mail.direccion").toLowerCase())
                || remitente.contains("mailer-daemon") || remitente.contains("postmaster")) {
            log("  Ignorado (remitente del sistema)");
            return;
        }

        Respuesta asunto = despachador.procesar(correo.getAsunto());
        try {
            smtp.enviar(correo.getRemitente(), asunto.getTitulo() + " - " + correo.getAsunto(), asunto.getHtml());
            log("  Respondido: " + asunto.getTitulo());
        } catch (IOException e) {
            log("  No se pudo enviar la respuesta: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + msg);
    }
}
