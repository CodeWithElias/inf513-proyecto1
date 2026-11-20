package grupo13.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Lee la configuración desde config.properties (en la carpeta desde donde se ejecuta).
 * Las contraseñas (mail.password, db.password) solo están en ese archivo, que no se sube al repositorio
 * (plantilla: config.properties.example). El resto tiene valores por defecto del Grupo 13.
 */
public final class Config {

    private static final Properties PROPS = new Properties();

    static {
        PROPS.setProperty("mail.usuario", "grupo13sa");
        PROPS.setProperty("mail.direccion", "grupo13sa@tecnoweb.org.bo");
        PROPS.setProperty("pop3.host", "mail.tecnoweb.org.bo");
        PROPS.setProperty("pop3.puerto", "110");
        PROPS.setProperty("smtp.host", "mail.tecnoweb.org.bo");
        PROPS.setProperty("smtp.puerto", "25");
        PROPS.setProperty("db.host", "db.tecnoweb.org.bo");
        PROPS.setProperty("db.puerto", "5432");
        PROPS.setProperty("db.nombre", "db_grupo13sa");
        PROPS.setProperty("db.usuario", "grupo13sa");
        PROPS.setProperty("app.intervalo", "10");
        PROPS.setProperty("log.carpeta", "logs");
        PROPS.setProperty("scraping.intervalo.horas", "6");
        PROPS.setProperty("scraping.paginas", "3");
        PROPS.setProperty("alertas.rol", "Administrador");

        try (InputStreamReader in = new InputStreamReader(
                new FileInputStream("config.properties"), StandardCharsets.UTF_8)) {
            PROPS.load(in);
        } catch (IOException e) {
            System.out.println("[Config] config.properties no encontrado: copie config.properties.example y complete"
                    + " las contraseñas.");
        }
    }

    private Config() {
    }

    public static String get(String clave) {
        String valor = PROPS.getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta '" + clave + "' en config.properties"
                    + " (vea config.properties.example).");
        }
        return valor.trim();
    }

    public static int getInt(String clave) {
        return Integer.parseInt(get(clave));
    }
}
