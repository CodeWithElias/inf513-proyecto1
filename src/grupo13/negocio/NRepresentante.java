package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DEmpresa;
import grupo13.datos.DRepresentante;
import grupo13.datos.DUsuario;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/**
 * CU1: usuarios de tipo Cliente (representante de una empresa anunciante).
 * Flujo: primero se crea el usuario con insUsu y luego se le asigna el perfil de cliente con insRep.
 */
public class NRepresentante {

    private static final String[] TITULOS =
            {"ID usuario", "Nombre completo", "Email", "Teléfono", "CI", "Cargo", "Empresa", "NIT", "Activo"};

    private final DRepresentante datos = new DRepresentante();
    private final DUsuario usuario = new DUsuario();
    private final DEmpresa empresa = new DEmpresa();

    /** lisRep["*"]: busca por nombre, email, CI, razón social o NIT. */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Clientes",
                Html.pagina("Listando los Clientes (representantes)", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosRep["id_usuario"] */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        return Respuesta.exito("Cliente " + id,
                Html.pagina("Cliente " + id, Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }

    /** insRep["id_usuario","ci","cargo","id_empresa"] */
    public Respuesta insertar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        String ci = Validar.ci(p.get(1));
        String cargo = Validar.textoOpcional(p.get(2), "cargo", 100);
        int idEmpresa = Validar.id(p.get(3), "id_empresa");

        Boolean activo = usuario.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe un usuario con ID " + id + ". Regístrelo primero con insUsu.");
        }
        if (!activo) {
            throw new ComandoException("El usuario " + id + " está inactivo. Reactívelo con actUsu[\"" + id + "\"].");
        }
        if (datos.obtener(id) != null) {
            throw new ComandoException("El usuario " + id + " ya es cliente. Use modRep para cambiar sus datos.");
        }
        validarCiYEmpresa(ci, idEmpresa, id);

        datos.insertar(id, ci, cargo, idEmpresa);
        return exito("El usuario " + id + " fue registrado como cliente.", id);
    }

    /** modRep["id_usuario","ci","cargo","id_empresa"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        String ci = Validar.ci(p.get(1));
        String cargo = Validar.textoOpcional(p.get(2), "cargo", 100);
        int idEmpresa = Validar.id(p.get(3), "id_empresa");

        buscar(id);
        validarCiYEmpresa(ci, idEmpresa, id);

        datos.modificar(id, ci, cargo, idEmpresa);
        return exito("Los datos de cliente del usuario " + id + " fueron modificados.", id);
    }

    /** eliRep["id_usuario"]: quita el perfil de cliente; el usuario sigue existiendo. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_usuario");
        buscar(id);
        datos.eliminar(id);
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>El usuario " + id + " ya no es cliente de ninguna empresa. Su cuenta de usuario se mantiene"
                        + " (para darla de baja use eliUsu[\"" + id + "\"]).</p>"));
    }

    private void validarCiYEmpresa(String ci, int idEmpresa, int idUsuario) throws Exception {
        if (datos.ciEnUso(ci, idUsuario)) {
            throw new ComandoException("El CI " + Html.esc(ci) + " ya está registrado para otro cliente.");
        }
        Boolean activa = empresa.estaActivo(idEmpresa);
        if (activa == null) {
            throw new ComandoException("No existe la empresa con ID " + idEmpresa + ". Consulte las empresas con lisEmp[\"*\"].");
        }
        if (!activa) {
            throw new ComandoException("La empresa con ID " + idEmpresa + " está inactiva.");
        }
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("El usuario " + id + " no está registrado como cliente.");
        }
        return fila;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
