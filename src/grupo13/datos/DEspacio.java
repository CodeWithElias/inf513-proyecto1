package grupo13.datos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Capa de datos de espacio_publicitario (calendario de espacios por canal).
 * Fila: id_espacio, canal, tipo_canal, fecha, hora_inicio, hora_fin, estado, campania_que_lo_ocupa, id_canal.
 */
public class DEspacio {

    private static final String SELECT =
            "SELECT e.id_espacio, c.nombre AS canal, c.tipo, e.fecha, e.hora_inicio, e.hora_fin, e.estado,"
            + " cam.nombre AS campania, e.id_canal"
            + " FROM espacio_publicitario e"
            + " JOIN canal_difusion c ON c.id_canal = e.id_canal"
            + " LEFT JOIN programacion_pauta pp ON pp.id_espacio = e.id_espacio"
            + " LEFT JOIN material_publicitario m ON m.id_material = pp.id_material"
            + " LEFT JOIN campania cam ON cam.id_campania = m.id_campania";

    private static final String ORDEN = " ORDER BY e.fecha, e.hora_inicio, c.nombre";

    /** Calendario: idCanal y fecha opcionales (null = todos). */
    public List<String[]> listar(Integer idCanal, LocalDate fecha) throws SQLException {
        List<String> condiciones = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (idCanal != null) {
            condiciones.add("e.id_canal = ?");
            params.add(idCanal);
        }
        if (fecha != null) {
            condiciones.add("e.fecha = ?");
            params.add(fecha);
        }
        String where = condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones);
        return Conexion.consultar(SELECT + where + ORDEN, params.toArray());
    }

    /** Espacios en el estado indicado entre dos fechas (idCanal null = todos los canales). */
    public List<String[]> listarPorEstado(Integer idCanal, LocalDate desde, LocalDate hasta, String estado)
            throws SQLException {
        if (idCanal == null) {
            return Conexion.consultar(SELECT + " WHERE e.estado = ? AND e.fecha BETWEEN ? AND ?" + ORDEN, estado, desde, hasta);
        }
        return Conexion.consultar(SELECT + " WHERE e.estado = ? AND e.fecha BETWEEN ? AND ? AND e.id_canal = ?" + ORDEN,
                estado, desde, hasta, idCanal);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE e.id_espacio = ?", id);
    }

    /** Horarios ya registrados de un canal en una fecha. Fila: hora_inicio, hora_fin. */
    public List<String[]> horariosDelDia(int idCanal, LocalDate fecha) throws SQLException {
        return Conexion.consultar("SELECT hora_inicio, hora_fin FROM espacio_publicitario WHERE id_canal = ? AND fecha = ?",
                idCanal, fecha);
    }

    public int insertar(int idCanal, LocalDate fecha, LocalTime inicio, LocalTime fin) throws SQLException {
        return Conexion.insertar("INSERT INTO espacio_publicitario (id_canal, fecha, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)"
                + " RETURNING id_espacio", idCanal, fecha, inicio, fin);
    }

    /** Inserta el mismo horario en varias fechas, en una transacción. */
    public int insertarVarios(int idCanal, List<LocalDate> fechas, LocalTime inicio, LocalTime fin) throws SQLException {
        return Conexion.enTransaccion(c -> {
            for (LocalDate f : fechas) {
                Conexion.ejecutar(c, "INSERT INTO espacio_publicitario (id_canal, fecha, hora_inicio, hora_fin)"
                        + " VALUES (?, ?, ?, ?)", idCanal, f, inicio, fin);
            }
            return fechas.size();
        });
    }

    public int cambiarEstado(int id, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE espacio_publicitario SET estado = ? WHERE id_espacio = ?", estado, id);
    }
}
