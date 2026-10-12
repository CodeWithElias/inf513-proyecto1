package grupo13.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cliente POP3 implementado con sockets (RFC 1939). */
public class ClientePOP3 implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader entrada;
    private final PrintWriter salida;

    public ClientePOP3(String host, int puerto) throws IOException {
        socket = new Socket(host, puerto);
        socket.setSoTimeout(30000);
        // ISO-8859-1 conserva los bytes tal cual; luego se decodifican las cabeceras
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
        salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1), true);
        leerOk();
    }

    public void login(String usuario, String password) throws IOException {
        comando("USER " + usuario);
        comando("PASS " + password);
    }

    /** Devuelve la cantidad de mensajes en el buzón. */
    public int cantidadMensajes() throws IOException {
        String r = comando("STAT"); // +OK <cantidad> <tamaño>
        return Integer.parseInt(r.split("\\s+")[1]);
    }

    /** Descarga el mensaje n y devuelve solo remitente y asunto. */
    public Correo obtener(int n) throws IOException {
        comando("RETR " + n);
        List<String> lineas = leerMultilinea();

        // Unir cabeceras partidas en varias líneas (folding)
        List<String> cabeceras = new ArrayList<>();
        for (String l : lineas) {
            if (l.isEmpty()) {
                break; // fin de cabeceras
            }
            if ((l.startsWith(" ") || l.startsWith("\t")) && !cabeceras.isEmpty()) {
                int i = cabeceras.size() - 1;
                cabeceras.set(i, cabeceras.get(i) + " " + l.trim());
            } else {
                cabeceras.add(l);
            }
        }

        String from = "";
        String asunto = "";
        for (String c : cabeceras) {
            String lower = c.toLowerCase();
            if (lower.startsWith("from:")) {
                from = MimeUtil.decodificarCabecera(c.substring(5).trim());
            } else if (lower.startsWith("subject:")) {
                asunto = MimeUtil.decodificarCabecera(c.substring(8).trim());
            }
        }
        return new Correo(MimeUtil.extraerEmail(from), asunto.trim());
    }

    public void eliminar(int n) throws IOException {
        comando("DELE " + n);
    }

    @Override
    public void close() {
        try {
            comando("QUIT"); // aquí el servidor aplica los DELE
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String comando(String cmd) throws IOException {
        salida.print(cmd + "\r\n");
        salida.flush();
        return leerOk();
    }

    private String leerOk() throws IOException {
        String r = entrada.readLine();
        if (r == null || !r.startsWith("+OK")) {
            throw new IOException("POP3: " + r);
        }
        return r;
    }

    private List<String> leerMultilinea() throws IOException {
        List<String> lineas = new ArrayList<>();
        String l;
        while ((l = entrada.readLine()) != null) {
            if (l.equals(".")) {
                break;
            }
            lineas.add(l.startsWith("..") ? l.substring(1) : l);
        }
        return lineas;
    }
}
