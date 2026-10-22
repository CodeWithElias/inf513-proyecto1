package grupo13.presentacion.correo;

import grupo13.config.Config;
import grupo13.presentacion.Html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Envía por correo los avisos que la capa de negocio ya registró en el sistema (RF11).
 * En modo prueba (--probar) no se envía nada: solo se informa a quién se habría enviado.
 */
public final class Notificador {

    private static boolean activo = true;

    private Notificador() {
    }

    public static void desactivar() {
        activo = false;
    }

    /**
     * avisos: {email, nombre, titulo, mensaje}. Devuelve una línea de resultado por aviso
     * (el fallo de un correo no deshace la operación: el aviso ya está guardado en el sistema).
     */
    public static List<String> enviar(List<String[]> avisos) {
        List<String> resultado = new ArrayList<>();
        ClienteSMTP smtp = new ClienteSMTP(Config.get("smtp.host"), Config.getInt("smtp.puerto"), Config.get("mail.direccion"));
        for (String[] a : avisos) {
            String destino = a[1] + " <" + a[0] + ">";
            if (!activo) {
                resultado.add(destino + ": registrado (modo prueba, correo no enviado)");
                continue;
            }
            try {
                smtp.enviar(a[0], a[2], Html.pagina(a[2], Html.parrafo("Estimado(a) " + a[1] + ":") + Html.parrafo(a[3])));
                resultado.add(destino + ": correo enviado");
            } catch (IOException e) {
                resultado.add(destino + ": registrado en el sistema, pero el correo falló (" + e.getMessage() + ")");
            }
        }
        return resultado;
    }
}
