package grupo13.mail;

/** Correo recibido (solo lo que necesitamos: remitente y asunto). */
public class Correo {

    private final String remitente;
    private final String asunto;

    public Correo(String remitente, String asunto) {
        this.remitente = remitente;
        this.asunto = asunto;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getAsunto() {
        return asunto;
    }

    @Override
    public String toString() {
        return "De: " + remitente + " | Asunto: " + asunto;
    }
}
