package grupo13.negocio;

import grupo13.datos.DCanal;
import grupo13.datos.DMetrica;
import grupo13.datos.DPauta;
import grupo13.datos.DPlataforma;
import grupo13.datos.DYoutube;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CU7 / RF13: registro de métricas de impacto de las pautas publicadas.
 * Reglas:
 *  - Solo se miden pautas publicadas (CU5). Cada registro es una medición acumulada a la fecha.
 *  - Coherencia: alcance ≤ impresiones, clics ≤ impresiones, compartidos ≤ interacciones (todo ≥ 0).
 *  - Captura automática (YouTube): impresiones = vistas, interacciones = "me gusta" + comentarios; alcance, clics y
 *    compartidos (que YouTube no publica) se mantienen de la última medición, ajustados para seguir siendo coherentes.
 * Fila de medición: id_metrica, fecha_medicion, alcance, impresiones, interacciones, clics, compartidos, id_programacion.
 */
public class NMetrica {

    private static final Pattern VIDEO = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|live/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})");

    // Índices de la fila de pauta (ver DPauta)
    private static final int PP_ESTADO = 8;
    private static final int PP_URL = 9;
    private static final int PP_CANAL = 14;

    private final DMetrica datos = new DMetrica();
    private final DPauta pauta = new DPauta();
    private final DCanal canal = new DCanal();
    private final DPlataforma plataforma = new DPlataforma();
    private final DYoutube youtube = new DYoutube();

    /** Historial de mediciones de una pauta (la más reciente primero). */
    public List<String[]> listar(String idProgramacion) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            Acceso.exigirPauta(buscarPauta(id));
            return datos.listarPorPauta(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(int idMetrica) throws NegocioException {
        try {
            return buscar(idMetrica);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Registro manual (cualquier canal). Devuelve el ID de la medición. */
    public int registrar(String idProgramacion, String alcance, String impresiones, String interacciones, String clics,
                         String compartidos) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            int[] v = validarValores(alcance, impresiones, interacciones, clics, compartidos);
            exigirPublicada(id);
            return datos.insertar(id, v[0], v[1], v[2], v[3], v[4]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Corrige una medición registrada. */
    public void modificar(String idMetrica, String alcance, String impresiones, String interacciones, String clics,
                          String compartidos) throws NegocioException {
        try {
            int id = Validar.id(idMetrica, "id_metrica");
            int[] v = validarValores(alcance, impresiones, interacciones, clics, compartidos);
            buscar(id);
            datos.modificar(id, v[0], v[1], v[2], v[3], v[4]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Elimina una medición registrada por error. Devuelve el ID de su pauta. */
    public int eliminar(String idMetrica) throws NegocioException {
        try {
            int id = Validar.id(idMetrica, "id_metrica");
            String[] m = buscar(id);
            datos.eliminar(id);
            return Integer.parseInt(m[7]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /**
     * Captura automática desde YouTube de una pauta publicada en una red de YouTube.
     * Devuelve {id_metrica, vistas, me_gusta, comentarios}.
     */
    public String[] capturar(String idProgramacion) throws NegocioException {
        try {
            int id = Validar.id(idProgramacion, "id_programacion");
            String[] pp = exigirPublicada(id);
            String[] red = pp[PP_CANAL] == null ? null : canal.datosRedSocial(Integer.parseInt(pp[PP_CANAL]));
            if (red == null || !esYoutube(Integer.parseInt(red[2]))) {
                throw new NegocioException("La pauta " + id + " no se publicó en una red social de YouTube; registre sus métricas"
                        + " manualmente.");
            }
            return capturarVideo(id, pp[PP_URL]);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Captura todas las pautas publicadas en YouTube. Devuelve una línea de resultado por pauta. */
    public List<String> capturarTodas() throws NegocioException {
        try {
            List<String> resultado = new ArrayList<>();
            for (String[] p : datos.pautasEnRedes(NPauta.PUBLICADA)) {
                if (!p[2].toLowerCase(Locale.ROOT).equals("youtube")) {
                    continue;
                }
                try {
                    String[] r = capturarVideo(Integer.parseInt(p[0]), p[1]);
                    resultado.add("Pauta " + p[0] + ": " + r[1] + " vistas, " + (r[2] == null ? "-" : r[2]) + " me gusta, "
                            + (r[3] == null ? "-" : r[3]) + " comentarios");
                } catch (NegocioException e) {
                    resultado.add("Pauta " + p[0] + ": " + e.getMessage());
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] capturarVideo(int idProgramacion, String url) throws SQLException, NegocioException {
        Matcher m = url == null ? null : VIDEO.matcher(url);
        if (m == null || !m.find()) {
            throw new NegocioException("La URL de publicación de la pauta " + idProgramacion + " no es un video de YouTube ("
                    + (url == null ? "sin URL" : url) + ").");
        }
        String[] s;
        try {
            s = youtube.estadisticasVideo(m.group(1));
        } catch (IOException e) {
            throw new NegocioException("No se pudieron leer las estadísticas: " + e.getMessage());
        }
        int vistas = Integer.parseInt(s[0]);
        int interacciones = (s[1] == null ? 0 : Integer.parseInt(s[1])) + (s[2] == null ? 0 : Integer.parseInt(s[2]));
        List<String[]> anteriores = datos.ultimasDos(idProgramacion);
        int alcance = 0;
        int clics = 0;
        int compartidos = 0;
        if (!anteriores.isEmpty()) {
            String[] a = anteriores.get(0);
            alcance = Math.min(Integer.parseInt(a[2]), vistas);
            clics = Math.min(Integer.parseInt(a[5]), vistas);
            compartidos = Math.min(Integer.parseInt(a[6]), interacciones);
        }
        int idMetrica = datos.insertar(idProgramacion, alcance, vistas, interacciones, clics, compartidos);
        return new String[]{String.valueOf(idMetrica), s[0], s[1], s[2]};
    }

    // ===== Reglas =====

    /** {alcance, impresiones, interacciones, clics, compartidos} validados y coherentes. */
    private static int[] validarValores(String alcance, String impresiones, String interacciones, String clics,
                                        String compartidos) throws NegocioException {
        int alc = Validar.enteroNoNegativo(alcance, "alcance");
        int imp = Validar.enteroNoNegativo(impresiones, "impresiones");
        int inter = Validar.enteroNoNegativo(interacciones, "interacciones");
        int cli = Validar.enteroNoNegativo(clics, "clics");
        int comp = Validar.enteroNoNegativo(compartidos, "compartidos");
        if (alc > imp) {
            throw new NegocioException("El alcance (personas distintas) no puede ser mayor que las impresiones (veces mostrada).");
        }
        if (cli > imp) {
            throw new NegocioException("Los clics no pueden ser más que las impresiones.");
        }
        if (comp > inter) {
            throw new NegocioException("Los compartidos son un tipo de interacción: no pueden ser más que las interacciones.");
        }
        return new int[]{alc, imp, inter, cli, comp};
    }

    private boolean esYoutube(int idPlataforma) throws SQLException {
        String[] p = plataforma.obtener(idPlataforma);
        return p != null && p[1].toLowerCase(Locale.ROOT).equals("youtube");
    }

    private String[] exigirPublicada(int id) throws SQLException, NegocioException {
        String[] pp = buscarPauta(id);
        if (!NPauta.PUBLICADA.equals(pp[PP_ESTADO])) {
            throw new NegocioException("La pauta " + id + " está " + pp[PP_ESTADO] + "; solo se miden pautas publicadas.");
        }
        return pp;
    }

    private String[] buscarPauta(int id) throws SQLException, NegocioException {
        String[] pp = pauta.obtener(id);
        if (pp == null) {
            throw new NegocioException("No existe una pauta con ID " + id + ".");
        }
        return pp;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] m = datos.obtener(id);
        if (m == null) {
            throw new NegocioException("No existe una medición con ID " + id + ".");
        }
        return m;
    }
}
