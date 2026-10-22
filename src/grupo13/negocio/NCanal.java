package grupo13.negocio;

import grupo13.datos.DCanal;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CU5: canales de difusión (televisión, radio, web). Las redes sociales también son canales, pero se
 * registran y dan de baja desde el CU2.
 * Fila: id_canal, tipo, nombre, activo, fecha_alta, fecha_baja, espacios_disponibles.
 */
public class NCanal {

    public static final String RED_SOCIAL = "red_social";

    /** Tipos que se registran en este CU. */
    public static final Set<String> TIPOS = Set.of("television", "radio", "web");

    /** Tipos de material que acepta cada tipo de canal. */
    static final Map<String, Set<String>> MATERIAL_ACEPTADO = Map.of(
            "television", Set.of("video", "texto", "banner"),
            "radio", Set.of("audio", "texto"),
            "web", Set.of("video", "imagen", "banner", "texto"),
            RED_SOCIAL, Set.of("video", "imagen", "banner", "texto"));

    private static final int TIPO = 1;

    private final DCanal datos = new DCanal();

    /** Busca por nombre o tipo. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idCanal) throws NegocioException {
        try {
            return buscar(Validar.id(idCanal, "id_canal"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** tipo: television, radio o web. */
    public int insertar(String tipo, String nombre) throws NegocioException {
        try {
            String t = validarTipo(tipo);
            String n = Validar.texto(nombre, "nombre", 150);
            if (datos.nombreEnUso(n, 0)) {
                throw new NegocioException("Ya existe un canal llamado '" + n + "'.");
            }
            return datos.insertar(t, n);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica; no se permite con pautas programadas pendientes de emisión. */
    public void eliminar(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            String[] c = buscarPropio(id);
            if (!"t".equals(c[3])) {
                throw new NegocioException("El canal " + id + " ya está dado de baja.");
            }
            int programadas = datos.contarProgramaciones(id, NPauta.PROGRAMADA);
            if (programadas > 0) {
                throw new NegocioException("No se puede dar de baja el canal " + id + ": tiene " + programadas
                        + " pauta(s) programada(s). Cancélelas o reprográmelas primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            String[] c = buscarPropio(id);
            if ("t".equals(c[3])) {
                throw new NegocioException("El canal " + id + " ya está activo.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private static String validarTipo(String tipo) throws NegocioException {
        String t = tipo.toLowerCase();
        if (t.equals(RED_SOCIAL)) {
            throw new NegocioException("Las redes sociales se registran como redes sociales institucionales (CU2).");
        }
        if (!TIPOS.contains(t)) {
            throw new NegocioException("Tipo de canal inválido '" + tipo + "'. Use: television, radio o web.");
        }
        return t;
    }

    /** Canal que se administra en este CU (no una red social). */
    private String[] buscarPropio(int id) throws SQLException, NegocioException {
        String[] c = buscar(id);
        if (RED_SOCIAL.equals(c[TIPO])) {
            throw new NegocioException("El canal " + id + " es una red social; adminístrelo como red social institucional (CU2).");
        }
        return c;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un canal con ID " + id + ".");
        }
        return fila;
    }
}
