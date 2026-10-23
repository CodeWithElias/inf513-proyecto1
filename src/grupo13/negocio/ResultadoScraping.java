package grupo13.negocio;

import java.util.List;

/**
 * Resultado de una ejecución de scraping (RF12). Las alertas ya quedaron registradas en el sistema;
 * 'avisos' es el resumen por destinatario para que la presentación lo envíe por correo: {email, nombre, titulo, mensaje}.
 */
public final class ResultadoScraping {

    private final int idEjecucion;
    private final String plataforma;
    private final String estado;
    private final String terminos;
    private final int cuentasRevisadas;
    private final int oficiales;
    private final int nuevas;
    private final int yaRegistradas;
    private final String errores;
    private final List<String[]> avisos;

    ResultadoScraping(int idEjecucion, String plataforma, String estado, String terminos, int cuentasRevisadas, int oficiales,
                      int nuevas, int yaRegistradas, String errores, List<String[]> avisos) {
        this.idEjecucion = idEjecucion;
        this.plataforma = plataforma;
        this.estado = estado;
        this.terminos = terminos;
        this.cuentasRevisadas = cuentasRevisadas;
        this.oficiales = oficiales;
        this.nuevas = nuevas;
        this.yaRegistradas = yaRegistradas;
        this.errores = errores;
        this.avisos = avisos;
    }

    public int getIdEjecucion() {
        return idEjecucion;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getEstado() {
        return estado;
    }

    /** Términos buscados en la ejecución, separados por coma. */
    public String getTerminos() {
        return terminos;
    }

    /** Cuentas distintas que contenían algún término. */
    public int getCuentasRevisadas() {
        return cuentasRevisadas;
    }

    /** Cuentas que coinciden con una red oficial registrada (CU2). */
    public int getOficiales() {
        return oficiales;
    }

    /** Páginas no autorizadas nuevas (o que reaparecieron). */
    public int getNuevas() {
        return nuevas;
    }

    /** Páginas que ya estaban registradas (se les agregó una detección). */
    public int getYaRegistradas() {
        return yaRegistradas;
    }

    /** Errores de búsqueda (null si no hubo). */
    public String getErrores() {
        return errores;
    }

    public List<String[]> getAvisos() {
        return avisos;
    }
}
