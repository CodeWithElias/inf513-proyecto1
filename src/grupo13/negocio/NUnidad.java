package grupo13.negocio;

import grupo13.comandos.Respuesta;
import grupo13.datos.DUnidad;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** Consulta de unidades universitarias (para asignar a los usuarios). */
public class NUnidad {

    private static final String[] TITULOS = {"ID", "Nombre", "Tipo", "Unidad padre", "Activo"};

    private final DUnidad datos = new DUnidad();

    /** lisUni["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Unidades",
                Html.pagina("Listando las Unidades Universitarias", Html.tabla(TITULOS, datos.listar(filtro))));
    }
}
