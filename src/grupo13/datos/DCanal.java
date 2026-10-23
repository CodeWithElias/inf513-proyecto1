package grupo13.datos;

import java.sql.SQLException;
import java.util.List;

/**
 * Capa de datos de canal_difusion (TV, radio, web… y las redes sociales del CU2, que son su especialización).
 * Fila: id_canal, tipo, nombre, activo, fecha_alta, fecha_baja, espacios_disponibles.
 */
public class DCanal {

    private static final String SELECT =
            "SELECT c.id_canal, c.tipo, c.nombre, c.activo, c.fecha_alta, c.fecha_baja,"
            + " (SELECT count(*) FROM espacio_publicitario e WHERE e.id_canal = c.id_canal"
            + "   AND e.estado = 'disponible' AND e.fecha >= CURRENT_DATE) AS espacios_disponibles"
            + " FROM canal_difusion c";

    /** Busca por nombre o tipo. */
    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY c.id_canal");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE c.nombre ILIKE ? OR c.tipo ILIKE ? ORDER BY c.id_canal", like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE c.id_canal = ?", id);
    }

    /** Si el canal es una red social: {id_unidad, id_usuario responsable, id_plataforma}; si no, null. */
    public String[] datosRedSocial(int idCanal) throws SQLException {
        return Conexion.consultarUno("SELECT id_unidad, id_usuario, id_plataforma FROM red_social WHERE id_canal = ?", idCanal);
    }

    public boolean nombreEnUso(String nombre, int excluirId) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM canal_difusion WHERE lower(nombre) = lower(?) AND id_canal <> ?",
                nombre, excluirId) > 0;
    }

    public int insertar(String tipo, String nombre) throws SQLException {
        return Conexion.insertar("INSERT INTO canal_difusion (tipo, nombre) VALUES (?, ?) RETURNING id_canal", tipo, nombre);
    }

    /** Baja (registra fecha_baja) o alta (la limpia). */
    public int cambiarEstado(int id, boolean activo) throws SQLException {
        return Conexion.ejecutar("UPDATE canal_difusion SET activo = ?,"
                + " fecha_baja = CASE WHEN ? THEN NULL ELSE CURRENT_DATE END WHERE id_canal = ?", activo, activo, id);
    }

    /** Programaciones en el estado indicado en espacios de este canal. */
    public int contarProgramaciones(int idCanal, String estado) throws SQLException {
        return Conexion.contar("SELECT count(*) FROM programacion_pauta pp"
                + " JOIN espacio_publicitario e ON e.id_espacio = pp.id_espacio"
                + " WHERE e.id_canal = ? AND pp.estado = ?", idCanal, estado);
    }
}
