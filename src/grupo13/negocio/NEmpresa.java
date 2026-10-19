package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DEmpresa;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** CU3 / RF04: registro y perfil de empresas anunciantes (clientes). */
public class NEmpresa {

    public static final String[] PARAMS =
            {"razon_social", "nit", "nombre_comercial", "direccion_facturacion", "email_facturacion", "telefono"};

    private static final String[] TITULOS = {"ID", "Razón social", "NIT", "Nombre comercial", "Dirección facturación",
            "Email facturación", "Teléfono", "Activo", "Fecha registro", "Representantes"};
    private static final String[] TITULOS_REP = {"ID usuario", "Nombre", "Email", "CI", "Cargo", "Activo"};

    private final DEmpresa datos = new DEmpresa();

    /** lisEmp["*"]: busca por razón social, NIT o nombre comercial. */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Empresas",
                Html.pagina("Listando las Empresas Anunciantes", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosEmp["id"]: la empresa y sus representantes. */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_empresa");
        String html = Html.tabla(TITULOS, List.<String[]>of(buscar(id)))
                + "<h3>Representantes (clientes)</h3>" + Html.tabla(TITULOS_REP, datos.representantes(id));
        return Respuesta.exito("Empresa " + id, Html.pagina("Empresa " + id, html));
    }

    /** insEmp["razon_social","nit","nombre_comercial","direccion","email","telefono"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String[] d = validar(p, 0);
        if (datos.nitEnUso(d[1], 0)) {
            throw new ComandoException("Ya existe una empresa con el NIT " + Html.esc(d[1]) + ".");
        }
        int id = datos.insertar(d[0], d[1], d[2], d[3], d[4], d[5]);
        return exito("La empresa fue registrada correctamente con ID " + id
                + ". Asigne sus representantes con insUsu + insRep.", id);
    }

    /** modEmp["id","razon_social","nit","nombre_comercial","direccion","email","telefono"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_empresa");
        String[] d = validar(p, 1);
        buscar(id);
        if (datos.nitEnUso(d[1], id)) {
            throw new ComandoException("El NIT " + Html.esc(d[1]) + " ya pertenece a otra empresa.");
        }
        datos.modificar(id, d[0], d[1], d[2], d[3], d[4], d[5]);
        return exito("La empresa " + id + " fue modificada correctamente.", id);
    }

    /** eliEmp["id"]: baja lógica; no se permite con proformas pendientes. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_empresa");
        if (!estado(id)) {
            throw new ComandoException("La empresa " + id + " ya está dada de baja.");
        }
        int pendientes = datos.contarProformasPendientes(id);
        if (pendientes > 0) {
            throw new ComandoException("No se puede dar de baja la empresa " + id + ": tiene " + pendientes
                    + " proforma(s) pendiente(s). Apruébelas o recházelas primero (lisPro[\"pendiente\"]).");
        }
        datos.cambiarEstado(id, false);
        return exito("La empresa " + id + " fue dada de baja. Puede reactivarla con actEmp[\"" + id + "\"].", id);
    }

    /** actEmp["id"] */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_empresa");
        if (estado(id)) {
            throw new ComandoException("La empresa " + id + " ya está activa.");
        }
        datos.cambiarEstado(id, true);
        return exito("La empresa " + id + " fue reactivada.", id);
    }

    /** Valida los 6 campos a partir de la posición 'desde'. */
    private String[] validar(List<String> p, int desde) throws ComandoException {
        return new String[]{
                Validar.texto(p.get(desde), "razon_social", 200),
                Validar.nit(p.get(desde + 1)),
                Validar.textoOpcional(p.get(desde + 2), "nombre_comercial", 200),
                Validar.textoOpcional(p.get(desde + 3), "direccion_facturacion", 255),
                Validar.emailOpcional(p.get(desde + 4)),
                Validar.telefono(p.get(desde + 5))
        };
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe una empresa con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe una empresa con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
