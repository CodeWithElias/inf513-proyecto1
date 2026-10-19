package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DVisitante;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** CU3 / RF07: visitantes (interesados sin cuenta que solicitan cotizaciones). */
public class NVisitante {

    private static final String[] TITULOS = {"ID", "Nombre", "Email", "Teléfono", "Fecha registro", "Proformas"};

    private final DVisitante datos = new DVisitante();

    /** lisVis["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Visitantes",
                Html.pagina("Listando los Visitantes", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** insVis["nombre","email","telefono"]  (se exige email o teléfono para poder contactarlo) */
    public Respuesta insertar(List<String> p) throws Exception {
        String[] d = validar(p, 0);
        int id = datos.insertar(d[0], d[1], d[2]);
        return exito("El visitante fue registrado con ID " + id + ". Ya puede generarle una cotización con insPro.", id);
    }

    /** modVis["id","nombre","email","telefono"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_visitante");
        String[] d = validar(p, 1);
        buscar(id);
        datos.modificar(id, d[0], d[1], d[2]);
        return exito("El visitante " + id + " fue modificado correctamente.", id);
    }

    /** eliVis["id"]: borrado físico, solo si no tiene proformas (la tabla no tiene columna activo). */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_visitante");
        buscar(id);
        int proformas = datos.contarProformas(id);
        if (proformas > 0) {
            throw new ComandoException("No se puede eliminar el visitante " + id + ": tiene " + proformas
                    + " proforma(s) registrada(s).");
        }
        datos.eliminar(id);
        return Respuesta.exito("Éxito", Html.pagina("Éxito", "<p>El visitante " + id + " fue eliminado.</p>"));
    }

    private String[] validar(List<String> p, int desde) throws ComandoException {
        String nombre = Validar.texto(p.get(desde), "nombre", 150);
        String email = Validar.emailOpcional(p.get(desde + 1));
        String telefono = Validar.telefono(p.get(desde + 2));
        if (email.isEmpty() && telefono.isEmpty()) {
            throw new ComandoException("Debe indicar al menos un email o un teléfono para contactar al visitante.");
        }
        return new String[]{nombre, email, telefono};
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe un visitante con ID " + id + ".");
        }
        return fila;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
