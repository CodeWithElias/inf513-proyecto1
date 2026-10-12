package grupo13.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/** Cliente SMTP implementado con sockets (RFC 5321). */
public class ClienteSMTP {

    private final String host;
    private final int puerto;
    private final String remitente;

    public ClienteSMTP(String host, int puerto, String remitente) {
        this.host = host;
        this.puerto = puerto;
        this.remitente = remitente;
    }

    /** Envía un correo HTML. */
    public void enviar(String destino, String asunto, String html) throws IOException {
        try (Socket socket = new Socket(host, puerto)) {
            socket.setSoTimeout(30000);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);

            esperar(in, 220);
            enviarComando(out, in, "HELO " + host, 250);
            enviarComando(out, in, "MAIL FROM:<" + remitente + ">", 250);
            enviarComando(out, in, "RCPT TO:<" + destino + ">", 250, 251);
            enviarComando(out, in, "DATA", 354);

            String dominio = remitente.substring(remitente.indexOf('@') + 1);
            // El cuerpo va en Base64: evita problemas con tildes, líneas largas y puntos al inicio
            String cuerpo = Base64.getMimeEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
            String mensaje =
                    "From: " + remitente + "\r\n"
                    + "To: " + destino + "\r\n"
                    + "Subject: " + MimeUtil.codificarCabecera(asunto) + "\r\n"
                    + "Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "\r\n"
                    + "Message-ID: <" + UUID.randomUUID() + "@" + dominio + ">\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/html; charset=UTF-8\r\n"
                    + "Content-Transfer-Encoding: base64\r\n"
                    + "\r\n"
                    + cuerpo + "\r\n"
                    + ".";
            enviarComando(out, in, mensaje, 250);
            enviarComando(out, in, "QUIT", 221);
        }
    }

    private void enviarComando(PrintWriter out, BufferedReader in, String cmd, int... esperados) throws IOException {
        out.print(cmd + "\r\n");
        out.flush();
        esperar(in, esperados);
    }

    /** Lee la respuesta (incluidas las multilínea "250-...") y valida el código. */
    private void esperar(BufferedReader in, int... esperados) throws IOException {
        String linea;
        do {
            linea = in.readLine();
            if (linea == null) {
                throw new IOException("SMTP: conexión cerrada por el servidor");
            }
        } while (linea.length() > 3 && linea.charAt(3) == '-');

        int codigo = Integer.parseInt(linea.substring(0, 3));
        for (int e : esperados) {
            if (codigo == e) {
                return;
            }
        }
        throw new IOException("SMTP: " + linea);
    }
}
