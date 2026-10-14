package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DUnidad;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** CU1: unidades universitarias (facultades, carreras, institutos…) a las que pertenecen los encargados. */
public class NUnidad {

    private static final String[] TITULOS = {"ID", "Nombre", "Tipo", "Unidad padre", "Activo", "Encargados activos"};

    private final DUnidad datos = new DUnidad();

    /** lisUni["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Unidades",
                Html.pagina("Listando las Unidades Universitarias", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosUni["id"]: la unidad y sus subunidades directas. */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_unidad");
        String[] fila = buscar(id);
        String html = Html.tabla(TITULOS, List.<String[]>of(fila))
                + "<h3>Subunidades</h3>" + Html.tabla(TITULOS, datos.listarHijas(id));
        return Respuesta.exito("Unidad " + id, Html.pagina("Unidad " + id, html));
    }

    /** insUni["nombre","tipo","id_unidad_padre"]  (tipo e id_unidad_padre pueden ir vacíos) */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 150);
        String tipo = Validar.textoOpcional(p.get(1), "tipo", 50);
        Integer idPadre = Validar.idOpcional(p.get(2), "id_unidad_padre");
        validarPadre(idPadre);
        int id = datos.insertar(nombre, tipo, idPadre);
        return exito("La unidad fue registrada correctamente con ID " + id + ".", id);
    }

    /** modUni["id","nombre","tipo","id_unidad_padre"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_unidad");
        String nombre = Validar.texto(p.get(1), "nombre", 150);
        String tipo = Validar.textoOpcional(p.get(2), "tipo", 50);
        Integer idPadre = Validar.idOpcional(p.get(3), "id_unidad_padre");
        buscar(id);
        validarPadre(idPadre);
        if (idPadre != null && datos.esDescendiente(idPadre, id)) {
            throw new ComandoException("La unidad " + idPadre + " no puede ser padre de la unidad " + id
                    + " porque es ella misma o una de sus subunidades.");
        }
        datos.modificar(id, nombre, tipo, idPadre);
        return exito("La unidad " + id + " fue modificada correctamente.", id);
    }

    /** eliUni["id"]: baja lógica; no se permite con encargados o subunidades activas. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_unidad");
        if (!estado(id)) {
            throw new ComandoException("La unidad " + id + " ya está dada de baja.");
        }
        int usuarios = datos.contarUsuariosActivos(id);
        if (usuarios > 0) {
            throw new ComandoException("No se puede dar de baja la unidad " + id + ": tiene " + usuarios
                    + " usuario(s) activo(s) asignado(s). Reasígnelos con modUsu primero.");
        }
        int hijas = datos.contarHijasActivas(id);
        if (hijas > 0) {
            throw new ComandoException("No se puede dar de baja la unidad " + id + ": tiene " + hijas
                    + " subunidad(es) activa(s). Délas de baja primero (vea mosUni[\"" + id + "\"]).");
        }
        datos.cambiarEstado(id, false);
        return exito("La unidad " + id + " fue dada de baja. Puede reactivarla con actUni[\"" + id + "\"].", id);
    }

    /** actUni["id"]: no se permite si su unidad padre está inactiva. */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_unidad");
        if (estado(id)) {
            throw new ComandoException("La unidad " + id + " ya está activa.");
        }
        Integer padre = datos.obtenerPadre(id);
        if (padre != null && !Boolean.TRUE.equals(datos.estaActivo(padre))) {
            throw new ComandoException("No se puede reactivar la unidad " + id + ": su unidad padre (" + padre
                    + ") está inactiva. Reactívela primero con actUni[\"" + padre + "\"].");
        }
        datos.cambiarEstado(id, true);
        return exito("La unidad " + id + " fue reactivada.", id);
    }

    private void validarPadre(Integer idPadre) throws Exception {
        if (idPadre == null) {
            return;
        }
        Boolean activa = datos.estaActivo(idPadre);
        if (activa == null) {
            throw new ComandoException("No existe la unidad padre con ID " + idPadre + ".");
        }
        if (!activa) {
            throw new ComandoException("La unidad padre con ID " + idPadre + " está inactiva.");
        }
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe una unidad con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe una unidad con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
