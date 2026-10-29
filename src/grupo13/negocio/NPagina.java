package grupo13.negocio;

import grupo13.datos.DPagina;
import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU6 / RF12: seguimiento de las páginas no autorizadas detectadas.
 * Las únicas cuentas autorizadas son las redes oficiales de red_social (las administra el personal en el CU2):
 * si una página detectada es legítima, se la registra como red oficial y el scraping deja de detectarla.
 * Estados y transiciones (los decide el personal de Canal 11):
 *   detectada → confirmada;  confirmada → denunciada;  denunciada → eliminada.
 *   "eliminada" = la plataforma la dio de baja; si vuelve a aparecer, el scraping la reabre como detectada.
 * Fila de página: id_pagina, url, nombre_pagina, plataforma, estado, detecciones, ultima_deteccion, fecha_revision,
 *   revisado_por, id_plataforma.
 * Fila de detección: id_deteccion, fecha_deteccion, termino, evidencia, id_ejecucion.
 */
public class NPagina {

    public static final String DETECTADA = "detectada";
    public static final String CONFIRMADA = "confirmada";
    public static final String DENUNCIADA = "denunciada";
    public static final String ELIMINADA = "eliminada";

    static final Map<String, Set<String>> TRANSICIONES = Map.of(
            DETECTADA, Set.of(CONFIRMADA),
            CONFIRMADA, Set.of(DENUNCIADA),
            DENUNCIADA, Set.of(ELIMINADA),
            ELIMINADA, Set.of());

    private static final int ESTADO = 4;

    private final DPagina datos = new DPagina();
    private final DUsuario usuario = new DUsuario();
    private final DRepresentante representante = new DRepresentante();

    /** Busca por URL, nombre, estado o plataforma. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idPagina) throws NegocioException {
        try {
            return buscar(Validar.id(idPagina, "id_pagina"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public List<String[]> listarDetecciones(String idPagina) throws NegocioException {
        try {
            int id = Validar.id(idPagina, "id_pagina");
            buscar(id);
            return datos.detecciones(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Cambia el estado según las transiciones permitidas; revisa el personal de Canal 11. */
    public void revisar(String idPagina, String estado) throws NegocioException {
        try {
            int id = Validar.id(idPagina, "id_pagina");
            String nuevo = estado.toLowerCase();
            int usu = Sesion.usuario().getIdUsuario();
            if (!TRANSICIONES.containsKey(nuevo)) {
                throw new NegocioException("Estado inválido '" + estado
                        + "'. Use: confirmada, denunciada o eliminada.");
            }
            String actual = buscar(id)[ESTADO];
            if (!TRANSICIONES.get(actual).contains(nuevo)) {
                throw new NegocioException("La página " + id + " está " + actual + "; no puede pasar a " + nuevo
                        + (TRANSICIONES.get(actual).isEmpty() ? "." : ". Estados posibles: " + String.join(", ", new java.util.TreeSet<>(TRANSICIONES.get(actual))) + "."));
            }
            Boolean activo = usuario.estaActivo(usu);
            if (activo == null) {
                throw new NegocioException("No existe un usuario con ID " + usu + ".");
            }
            if (!activo) {
                throw new NegocioException("El usuario " + usu + " está inactivo.");
            }
            if (representante.empresaDe(usu) != null) {
                throw new NegocioException("El usuario " + usu + " es representante de una empresa; la revisión la hace el personal de Canal 11.");
            }
            datos.revisar(id, nuevo, usu);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una página no autorizada con ID " + id + ".");
        }
        return fila;
    }
}
