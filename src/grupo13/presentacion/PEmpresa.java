package grupo13.presentacion;

import grupo13.negocio.NEmpresa;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU3 – Empresas anunciantes: lisEmp, mosEmp, insEmp, modEmp, eliEmp, actEmp. */
public class PEmpresa {

    public static final String[] PARAMS =
            {"razon_social", "nit", "nombre_comercial", "direccion_facturacion", "email_facturacion", "telefono"};

    private static final String[] COLUMNAS = {"ID", "Razón social", "NIT", "Nombre comercial", "Dirección facturación",
            "Email facturación", "Teléfono", "Activo", "Fecha registro", "Representantes"};
    private static final String[] COLUMNAS_REP = {"ID usuario", "Nombre", "Email", "CI", "Cargo", "Activo"};

    private final NEmpresa negocio = new NEmpresa();

    /** lisEmp["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando las Empresas", "Listando las Empresas Anunciantes", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosEmp["id"]: la empresa y sus representantes. */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        String html = Html.tabla(COLUMNAS, List.<String[]>of(negocio.obtener(p.get(0))))
                + Html.subtitulo("Representantes (clientes)") + Html.tabla(COLUMNAS_REP, negocio.listarRepresentantes(p.get(0)));
        return Respuesta.exito("Empresa " + p.get(0), Html.pagina("Empresa " + p.get(0), html));
    }

    /** insEmp["razon_social","nit","nombre_comercial","direccion","email","telefono"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5));
        return Vista.exito("La empresa fue registrada correctamente con ID " + id
                + ". Asigne sus representantes con insUsu + insRep.", COLUMNAS, negocio.obtener(String.valueOf(id)));
    }

    /** modEmp["id","razon_social","nit","nombre_comercial","direccion","email","telefono"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(5), p.get(6));
        return Vista.exito("La empresa " + p.get(0) + " fue modificada correctamente.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** eliEmp["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("La empresa " + p.get(0) + " fue dada de baja. Puede reactivarla con actEmp[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actEmp["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("La empresa " + p.get(0) + " fue reactivada.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
