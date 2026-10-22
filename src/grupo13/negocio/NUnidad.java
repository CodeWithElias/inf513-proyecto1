package grupo13.negocio;

import grupo13.datos.DUnidad;

import java.sql.SQLException;
import java.util.List;

/**
 * CU1: unidades universitarias (jerarquía facultad → carrera…), a las que pertenecen los encargados.
 * Fila de unidad: id_unidad, nombre, tipo, unidad_padre, activo, encargados_activos.
 */
public class NUnidad {

    private final DUnidad datos = new DUnidad();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(String idUnidad) throws NegocioException {
        try {
            return buscar(Validar.id(idUnidad, "id_unidad"));
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Subunidades directas. */
    public List<String[]> listarSubunidades(String idUnidad) throws NegocioException {
        try {
            int id = Validar.id(idUnidad, "id_unidad");
            buscar(id);
            return datos.listarHijas(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** tipo e idPadre pueden ir vacíos. */
    public int insertar(String nombre, String tipo, String idPadre) throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 150);
            String t = Validar.textoOpcional(tipo, "tipo", 50);
            Integer padre = Validar.idOpcional(idPadre, "id_unidad_padre");
            validarPadre(padre);
            return datos.insertar(n, t, padre);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Rechaza ciclos: la nueva unidad padre no puede ser la misma unidad ni una de sus subunidades. */
    public void modificar(String idUnidad, String nombre, String tipo, String idPadre) throws NegocioException {
        try {
            int id = Validar.id(idUnidad, "id_unidad");
            String n = Validar.texto(nombre, "nombre", 150);
            String t = Validar.textoOpcional(tipo, "tipo", 50);
            Integer padre = Validar.idOpcional(idPadre, "id_unidad_padre");
            buscar(id);
            validarPadre(padre);
            if (padre != null && datos.idsDelSubarbol(id).contains(padre)) {
                throw new NegocioException("La unidad " + padre + " no puede ser padre de la unidad " + id
                        + " porque es ella misma o una de sus subunidades.");
            }
            datos.modificar(id, n, t, padre);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica; no se permite con usuarios o subunidades activas. */
    public void eliminar(String idUnidad) throws NegocioException {
        try {
            int id = Validar.id(idUnidad, "id_unidad");
            if (!estado(id)) {
                throw new NegocioException("La unidad " + id + " ya está dada de baja.");
            }
            int usuarios = datos.contarUsuariosActivos(id);
            if (usuarios > 0) {
                throw new NegocioException("No se puede dar de baja la unidad " + id + ": tiene " + usuarios
                        + " usuario(s) activo(s) asignado(s). Reasígnelos primero.");
            }
            int hijas = datos.contarHijasActivas(id);
            if (hijas > 0) {
                throw new NegocioException("No se puede dar de baja la unidad " + id + ": tiene " + hijas
                        + " subunidad(es) activa(s). Délas de baja primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** No se permite si su unidad padre está inactiva. */
    public void activar(String idUnidad) throws NegocioException {
        try {
            int id = Validar.id(idUnidad, "id_unidad");
            if (estado(id)) {
                throw new NegocioException("La unidad " + id + " ya está activa.");
            }
            Integer padre = datos.obtenerPadre(id);
            if (padre != null && !Boolean.TRUE.equals(datos.estaActivo(padre))) {
                throw new NegocioException("No se puede reactivar la unidad " + id + ": su unidad padre (" + padre
                        + ") está inactiva. Reactívela primero.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private void validarPadre(Integer idPadre) throws SQLException, NegocioException {
        if (idPadre == null) {
            return;
        }
        Boolean activa = datos.estaActivo(idPadre);
        if (activa == null) {
            throw new NegocioException("No existe la unidad padre con ID " + idPadre + ".");
        }
        if (!activa) {
            throw new NegocioException("La unidad padre con ID " + idPadre + " está inactiva.");
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe una unidad con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe una unidad con ID " + id + ".");
        }
        return activo;
    }
}
