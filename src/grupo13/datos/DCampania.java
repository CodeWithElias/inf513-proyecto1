package grupo13.datos;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Capa de datos de campania (se genera a partir de una proforma aprobada: proforma 1 - 0..1 campania). */
public class DCampania {

    private static final String SELECT =
            "SELECT c.id_campania, c.nombre, to_char(c.fecha_inicio, 'DD/MM/YYYY'), to_char(c.fecha_fin, 'DD/MM/YYYY'),"
            + " c.estado, p.nro_proforma, e.razon_social, p.total"
            + " FROM campania c"
            + " JOIN proforma p ON p.id_proforma = c.id_proforma"
            + " LEFT JOIN empresa_anunciante e ON e.id_empresa = p.id_empresa";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY c.id_campania");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE c.nombre ILIKE ? OR c.estado ILIKE ? OR p.nro_proforma ILIKE ?"
                + " OR e.razon_social ILIKE ? ORDER BY c.id_campania", like, like, like, like);
    }

    public String[] obtener(int id) throws SQLException {
        return Conexion.consultarUno(SELECT + " WHERE c.id_campania = ?", id);
    }

    public int insertar(int idProforma, String nombre, LocalDate inicio, LocalDate fin) throws SQLException {
        return Conexion.insertar("INSERT INTO campania (id_proforma, nombre, fecha_inicio, fecha_fin) VALUES (?, ?, ?, ?)"
                + " RETURNING id_campania", idProforma, nombre, inicio, fin);
    }

    public int modificar(int id, String nombre, LocalDate inicio, LocalDate fin, String estado) throws SQLException {
        return Conexion.ejecutar("UPDATE campania SET nombre = ?, fecha_inicio = ?, fecha_fin = ?, estado = ?"
                + " WHERE id_campania = ?", nombre, inicio, fin, estado, id);
    }
}
