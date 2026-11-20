package grupo13.negocio;

import java.util.List;

/**
 * Resultado de una operación sobre la pauta: el ID de la programación y los avisos (RF11) que ya quedaron
 * registrados en el sistema. La capa de presentación decide si además los envía por correo.
 * Cada aviso: {email, nombre, titulo, mensaje}.
 */
public final class ResultadoPauta {

    private final int idProgramacion;
    private final List<String[]> avisos;

    ResultadoPauta(int idProgramacion, List<String[]> avisos) {
        this.idProgramacion = idProgramacion;
        this.avisos = avisos;
    }

    public int getIdProgramacion() {
        return idProgramacion;
    }

    public List<String[]> getAvisos() {
        return avisos;
    }
}
