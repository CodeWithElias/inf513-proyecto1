package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DRol;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** Consulta y registro de roles (necesarios para asignar a los usuarios). */
public class NRol {

    private static final String[] TITULOS = {"ID", "Nombre", "Descripción", "Activo"};

    private final DRol datos = new DRol();

    /** lisRol["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Roles",
                Html.pagina("Listando los Roles", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** insRol["nombre","descripcion"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 100);
        String descripcion = Validar.textoOpcional(p.get(1), "descripcion", 255);
        if (datos.existeNombre(nombre)) {
            throw new ComandoException("Ya existe un rol llamado " + Html.esc(nombre) + ".");
        }
        int id = datos.insertar(nombre, descripcion);
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>El rol <b>" + Html.esc(nombre) + "</b> fue registrado con ID " + id + ".</p>"));
    }
}
