package grupo13.negocio;

import grupo13.datos.DPlataforma;
import grupo13.datos.DRedSocial;
import grupo13.datos.DUnidad;
import grupo13.datos.DUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: cuentas oficiales de redes sociales institucionales por unidad y responsable.
 * Fila: id_canal, nombre, plataforma, url, responsable, unidad, seguidores, activo, fecha_alta, fecha_baja.
 */
public class NRedSocial {

    private final DRedSocial datos = new DRedSocial();
    private final DPlataforma plataforma = new DPlataforma();
    private final DUsuario usuario = new DUsuario();
    private final DUnidad unidad = new DUnidad();

    /** Busca por nombre, plataforma, URL, responsable o unidad. */
    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idCanal) throws NegocioException {
        try {
            return buscar(Validar.id(idCanal, "id_canal"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** idUsuario e idUnidad pueden ir vacíos. */
    public int insertar(String nombre, String url, String idPlataforma, String idUsuario, String idUnidad)
            throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 150);
            String u = Validar.url(url, "url", 500);
            int pla = Validar.id(idPlataforma, "id_plataforma");
            Integer usu = Validar.idOpcional(idUsuario, "id_usuario");
            Integer uni = Validar.idOpcional(idUnidad, "id_unidad");

            validarRelaciones(pla, usu, uni);
            if (datos.nombreEnUso(n, 0)) {
                throw new NegocioException("Ya existe una red social con el nombre '" + n + "'.");
            }
            if (datos.urlEnUso(u, 0)) {
                throw new NegocioException("Ya existe una red social con la URL '" + u + "'.");
            }
            return datos.insertar(n, u, pla, usu, uni);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idCanal, String nombre, String url, String idPlataforma, String idUsuario,
                          String idUnidad) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            String n = Validar.texto(nombre, "nombre", 150);
            String u = Validar.url(url, "url", 500);
            int pla = Validar.id(idPlataforma, "id_plataforma");
            Integer usu = Validar.idOpcional(idUsuario, "id_usuario");
            Integer uni = Validar.idOpcional(idUnidad, "id_unidad");

            buscar(id);
            validarRelaciones(pla, usu, uni);
            if (datos.nombreEnUso(n, id)) {
                throw new NegocioException("Ya existe otra red social con el nombre '" + n + "'.");
            }
            if (datos.urlEnUso(u, id)) {
                throw new NegocioException("Ya existe otra red social con la URL '" + u + "'.");
            }
            datos.modificar(id, n, u, pla, usu, uni);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica (registra fecha_baja). */
    public void eliminar(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            if (!estado(id)) {
                throw new NegocioException("La red social " + id + " ya está dada de baja.");
            }
            datos.eliminar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idCanal) throws NegocioException {
        try {
            int id = Validar.id(idCanal, "id_canal");
            if (estado(id)) {
                throw new NegocioException("La red social " + id + " ya se encuentra activa.");
            }
            datos.activar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private void validarRelaciones(int idPlataforma, Integer idUsuario, Integer idUnidad)
            throws SQLException, NegocioException {
        if (!plataforma.existe(idPlataforma)) {
            throw new NegocioException("No existe una plataforma con ID " + idPlataforma + ". Consulte la lista de plataformas.");
        }
        if (idUsuario != null) {
            Boolean usuActivo = usuario.estaActivo(idUsuario);
            if (usuActivo == null) {
                throw new NegocioException("No existe un usuario con ID " + idUsuario + ".");
            }
            if (!usuActivo) {
                throw new NegocioException("El usuario con ID " + idUsuario
                        + " está inactivo. Debe estar activo para ser responsable de una red social.");
            }
        }
        if (idUnidad != null) {
            Boolean uniActiva = unidad.estaActivo(idUnidad);
            if (uniActiva == null) {
                throw new NegocioException("No existe una unidad universitaria con ID " + idUnidad + ".");
            }
            if (!uniActiva) {
                throw new NegocioException("La unidad universitaria con ID " + idUnidad + " está inactiva.");
            }
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] f = datos.obtener(id);
        if (f == null) {
            throw new NegocioException("No existe una red social con ID " + id + ".");
        }
        return f;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe una red social con ID " + id + ".");
        }
        return activo;
    }
}
