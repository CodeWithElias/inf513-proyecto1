package grupo13.presentacion;

import grupo13.negocio.NRepresentante;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU1 – Clientes (representantes): lisRep, mosRep, insRep, modRep, eliRep. */
public class PRepresentante {

    private static final String[] COLUMNAS =
            {"ID usuario", "Nombre completo", "Email", "Teléfono", "CI", "Cargo", "Empresa", "NIT", "Activo"};

    private final NRepresentante negocio = new NRepresentante();

    /** lisRep["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Clientes", "Listando los Clientes (representantes)", COLUMNAS,
                negocio.listar(Vista.filtro(p.get(0))));
    }

    /** mosRep["id_usuario"] */
    public Respuesta mostrar(List<String> p) throws NegocioException {
        return Vista.registro("Cliente " + p.get(0), COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** insRep["id_usuario","ci","cargo","id_empresa"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("El usuario " + p.get(0) + " fue registrado como cliente.", COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** modRep["id_usuario","ci","cargo","id_empresa"] */
    public Respuesta modificar(List<String> p) throws NegocioException {
        negocio.modificar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("Los datos de cliente del usuario " + p.get(0) + " fueron modificados.", COLUMNAS,
                negocio.obtener(p.get(0)));
    }

    /** eliRep["id_usuario"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El usuario " + p.get(0) + " ya no es cliente de ninguna empresa. Su cuenta de usuario se mantiene"
                + " (para darla de baja use eliUsu[\"" + p.get(0) + "\"]).", "");
    }
}
