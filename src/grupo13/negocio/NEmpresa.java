package grupo13.negocio;

import grupo13.comandos.Respuesta;
import grupo13.datos.DEmpresa;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** Consulta de empresas anunciantes (para asignar clientes en CU1; el CRUD completo es del CU3). */
public class NEmpresa {

    private static final String[] TITULOS =
            {"ID", "Razón social", "NIT", "Nombre comercial", "Email facturación", "Teléfono", "Activo", "Representantes"};

    private final DEmpresa datos = new DEmpresa();

    /** lisEmp["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Empresas",
                Html.pagina("Listando las Empresas Anunciantes", Html.tabla(TITULOS, datos.listar(filtro))));
    }
}
