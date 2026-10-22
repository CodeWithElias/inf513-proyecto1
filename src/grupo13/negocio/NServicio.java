package grupo13.negocio;

import grupo13.datos.DServicio;

import java.sql.SQLException;
import java.util.List;

/**
 * CU3 / RF05: catálogo de servicios publicitarios (spot TV, mención en vivo, cintillo, publicación en redes…).
 * Fila: id_servicio, nombre, tipo, descripcion, activo, tarifas_activas.
 */
public class NServicio {

    private final DServicio datos = new DServicio();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public String[] obtener(int id) throws NegocioException {
        try {
            return buscar(id);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public int insertar(String nombre, String tipo, String descripcion) throws NegocioException {
        try {
            String n = Validar.texto(nombre, "nombre", 150);
            String t = Validar.textoOpcional(tipo, "tipo", 50);
            String d = Validar.textoOpcional(descripcion, "descripcion", 500);
            if (datos.nombreEnUso(n, 0)) {
                throw new NegocioException("Ya existe un servicio llamado " + n + ".");
            }
            return datos.insertar(n, t, d);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void modificar(String idServicio, String nombre, String tipo, String descripcion) throws NegocioException {
        try {
            int id = Validar.id(idServicio, "id_servicio");
            String n = Validar.texto(nombre, "nombre", 150);
            String t = Validar.textoOpcional(tipo, "tipo", 50);
            String d = Validar.textoOpcional(descripcion, "descripcion", 500);
            buscar(id);
            if (datos.nombreEnUso(n, id)) {
                throw new NegocioException("Ya existe otro servicio llamado " + n + ".");
            }
            datos.modificar(id, n, t, d);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Baja lógica; no se permite con tarifas activas. */
    public void eliminar(String idServicio) throws NegocioException {
        try {
            int id = Validar.id(idServicio, "id_servicio");
            if (!estado(id)) {
                throw new NegocioException("El servicio " + id + " ya está dado de baja.");
            }
            int tarifas = datos.contarTarifasActivas(id);
            if (tarifas > 0) {
                throw new NegocioException("No se puede dar de baja el servicio " + id + ": tiene " + tarifas
                        + " tarifa(s) activa(s). Deshabilítelas primero.");
            }
            datos.cambiarEstado(id, false);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    public void activar(String idServicio) throws NegocioException {
        try {
            int id = Validar.id(idServicio, "id_servicio");
            if (estado(id)) {
                throw new NegocioException("El servicio " + id + " ya está activo.");
            }
            datos.cambiarEstado(id, true);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un servicio con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws SQLException, NegocioException {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new NegocioException("No existe un servicio con ID " + id + ".");
        }
        return activo;
    }
}
