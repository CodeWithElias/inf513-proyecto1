package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DServicio;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/** CU3 / RF05: catálogo de servicios publicitarios (spot TV, mención en vivo, cintillo, publicación en redes…). */
public class NServicio {

    private static final String[] TITULOS = {"ID", "Nombre", "Tipo", "Descripción", "Activo", "Tarifas activas"};

    private final DServicio datos = new DServicio();

    /** lisSer["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando los Servicios",
                Html.pagina("Listando los Servicios Publicitarios", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** insSer["nombre","tipo","descripcion"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 150);
        String tipo = Validar.textoOpcional(p.get(1), "tipo", 50);
        String descripcion = Validar.textoOpcional(p.get(2), "descripcion", 500);
        if (datos.nombreEnUso(nombre, 0)) {
            throw new ComandoException("Ya existe un servicio llamado " + Html.esc(nombre) + ".");
        }
        int id = datos.insertar(nombre, tipo, descripcion);
        return exito("El servicio fue registrado con ID " + id + ". Defina su precio con insTar.", id);
    }

    /** modSer["id","nombre","tipo","descripcion"] */
    public Respuesta modificar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_servicio");
        String nombre = Validar.texto(p.get(1), "nombre", 150);
        String tipo = Validar.textoOpcional(p.get(2), "tipo", 50);
        String descripcion = Validar.textoOpcional(p.get(3), "descripcion", 500);
        buscar(id);
        if (datos.nombreEnUso(nombre, id)) {
            throw new ComandoException("Ya existe otro servicio llamado " + Html.esc(nombre) + ".");
        }
        datos.modificar(id, nombre, tipo, descripcion);
        return exito("El servicio " + id + " fue modificado correctamente.", id);
    }

    /** eliSer["id"]: baja lógica; no se permite con tarifas activas. */
    public Respuesta eliminar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_servicio");
        if (!estado(id)) {
            throw new ComandoException("El servicio " + id + " ya está dado de baja.");
        }
        int tarifas = datos.contarTarifasActivas(id);
        if (tarifas > 0) {
            throw new ComandoException("No se puede dar de baja el servicio " + id + ": tiene " + tarifas
                    + " tarifa(s) activa(s). Deshabilítelas primero con eliTar.");
        }
        datos.cambiarEstado(id, false);
        return exito("El servicio " + id + " fue dado de baja. Puede reactivarlo con actSer[\"" + id + "\"].", id);
    }

    /** actSer["id"] */
    public Respuesta activar(List<String> p) throws Exception {
        int id = Validar.id(p.get(0), "id_servicio");
        if (estado(id)) {
            throw new ComandoException("El servicio " + id + " ya está activo.");
        }
        datos.cambiarEstado(id, true);
        return exito("El servicio " + id + " fue reactivado.", id);
    }

    private String[] buscar(int id) throws Exception {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new ComandoException("No existe un servicio con ID " + id + ".");
        }
        return fila;
    }

    private boolean estado(int id) throws Exception {
        Boolean activo = datos.estaActivo(id);
        if (activo == null) {
            throw new ComandoException("No existe un servicio con ID " + id + ".");
        }
        return activo;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        return Respuesta.exito("Éxito", Html.pagina("Éxito",
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id)))));
    }
}
