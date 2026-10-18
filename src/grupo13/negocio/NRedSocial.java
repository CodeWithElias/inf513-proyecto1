package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DPlataforma;
import grupo13.datos.DRedSocial;
import grupo13.datos.DUnidad;
import grupo13.datos.DUsuario;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: gestión de cuentas oficiales de redes sociales institucionales por unidad y responsable.
 */
public class NRedSocial {

    private static final String[] TITULOS = {
            "ID", "Nombre de la Red", "Plataforma", "URL", "Responsable", "Unidad Universitaria",
            "Seguidores", "Activo", "Fecha Alta", "Fecha Baja"
    };

    private final DRedSocial datos = new DRedSocial();
    private final DPlataforma plataforma = new DPlataforma();
    private final DUsuario usuario = new DUsuario();
    private final DUnidad unidad = new DUnidad();

    /** lisRed["*"] o lisRed["texto"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Redes Sociales",
                Html.pagina("Listando las Redes Sociales Institucionales", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** mosRed["id_canal"] */
    public Respuesta mostrar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_canal");
        return Respuesta.exito("Red Social " + id,
                Html.pagina("Red Social Institucional " + id, Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }

    /** insRed["nombre","url","id_plataforma","id_usuario","id_unidad"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 150);
        String url = Validar.url(p.get(1), "url", 500);
        int idPlataforma = Validar.id(p.get(2), "id_plataforma");
        Integer idUsuario = Validar.idOpcional(p.get(3), "id_usuario");
        Integer idUnidad = Validar.idOpcional(p.get(4), "id_unidad");

        validarRelaciones(idPlataforma, idUsuario, idUnidad);

        if (datos.nombreEnUso(nombre, 0)) {
            throw new ComandoException("Ya existe una red social con el nombre '" + Html.esc(nombre) + "'.");
        }
        if (datos.urlEnUso(url, 0)) {
            throw new ComandoException("Ya existe una red social con la URL '" + Html.esc(url) + "'.");
        }

        int id = datos.insertar(nombre, url, idPlataforma, idUsuario, idUnidad);
        return exito("La red social institucional fue registrada correctamente con ID " + id + ".", id);
    }

    /** modRed["id_canal","nombre","url","id_plataforma","id_usuario","id_unidad"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_canal");
        String nombre = Validar.texto(p.get(1), "nombre", 150);
        String url = Validar.url(p.get(2), "url", 500);
        int idPlataforma = Validar.id(p.get(3), "id_plataforma");
        Integer idUsuario = Validar.idOpcional(p.get(4), "id_usuario");
        Integer idUnidad = Validar.idOpcional(p.get(5), "id_unidad");

        buscar(id);
        validarRelaciones(idPlataforma, idUsuario, idUnidad);

        if (datos.nombreEnUso(nombre, id)) {
            throw new ComandoException("Ya existe otra red social con el nombre '" + Html.esc(nombre) + "'.");
        }
        if (datos.urlEnUso(url, id)) {
            throw new ComandoException("Ya existe otra red social con la URL '" + Html.esc(url) + "'.");
        }

        datos.modificar(id, nombre, url, idPlataforma, idUsuario, idUnidad);
        return exito("La red social " + id + " fue modificada correctamente.", id);
    }

    /** eliRed["id_canal"]: baja lógica */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_canal");
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe una red social con ID " + id + ".");
        }
        if (!activo) {
            throw new ComandoException("La red social " + id + " ya está dada de baja.");
        }
        datos.eliminar(id);
        return exito("La red social " + id + " fue dada de baja (inactivada).", id);
    }

    /** actRed["id_canal"]: reactiva el canal */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_canal");
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe una red social con ID " + id + ".");
        }
        if (activo) {
            throw new ComandoException("La red social " + id + " ya se encuentra activa.");
        }
        datos.activar(id);
        return exito("La red social " + id + " fue reactivada exitosamente.", id);
    }

    private void validarRelaciones(int idPlataforma, Integer idUsuario, Integer idUnidad) throws Exception {
        if (!plataforma.existe(idPlataforma)) {
            throw new ComandoException("No existe una plataforma con ID " + idPlataforma + ". Consulte con lisPla[\"*\"].");
        }
        if (idUsuario != null) {
            Boolean usuActivo = usuario.estaActivo(idUsuario);
            if (usuActivo == null) {
                throw new ComandoException("No existe un usuario con ID " + idUsuario + ".");
            }
            if (!usuActivo) {
                throw new ComandoException("El usuario con ID " + idUsuario + " está inactivo. Debe estar activo para ser responsable de una red social.");
            }
        }
        if (idUnidad != null) {
            Boolean uniActiva = unidad.estaActivo(idUnidad);
            if (uniActiva == null) {
                throw new ComandoException("No existe una unidad universitaria con ID " + idUnidad + ".");
            }
            if (!uniActiva) {
                throw new ComandoException("La unidad universitaria con ID " + idUnidad + " está inactiva.");
            }
        }
    }

    private String[] buscar(int id) throws Exception {
        String[] f = datos.obtener(id);
        if (f == null) {
            throw new ComandoException("No existe una red social con ID " + id + ".");
        }
        return f;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        String html = Html.pagina(mensaje,
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id))));
        return Respuesta.exito(mensaje, html);
    }
}
