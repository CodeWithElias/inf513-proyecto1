package grupo13.comandos;

import grupo13.negocio.NRol;
import grupo13.negocio.NUnidad;
import grupo13.negocio.NUsuario;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de todos los comandos del sistema.
 * Para agregar un caso de uso nuevo: crear su clase de negocio, llamar a grupo(...) y registrar(...) en el constructor.
 */
public class Despachador {

    /** Definición de un comando: parámetros esperados, ejemplo y acción. */
    private static class Definicion {
        final String grupo;
        final String nombre;
        final String descripcion;
        final String[] parametros;
        final String ejemplo;
        final Accion accion;

        Definicion(String grupo, String nombre, String descripcion, String[] parametros, String ejemplo, Accion accion) {
            this.grupo = grupo;
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.parametros = parametros;
            this.ejemplo = ejemplo;
            this.accion = accion;
        }
    }

    private final Map<String, Definicion> comandos = new LinkedHashMap<>();
    private String grupoActual = "General";

    public Despachador() {
        NUsuario usuario = new NUsuario();
        NRol rol = new NRol();
        NUnidad unidad = new NUnidad();

        grupo("Ayuda");
        registrar("help", "Lista todos los comandos existentes", new String[]{}, "help", p -> ayuda());

        grupo("CU: Gestionar Usuarios");
        registrar("lisUsu", "Lista los usuarios (\"*\" = todos, o un texto para buscar por nombre/email/rol)",
                new String[]{"filtro"}, "lisUsu[\"*\"]", usuario::listar);
        registrar("mosUsu", "Muestra un usuario por su ID",
                new String[]{"id_usuario"}, "mosUsu[\"1\"]", usuario::mostrar);
        registrar("insUsu", "Registra un usuario (id_unidad puede ir vacío \"\")",
                NUsuario.PARAMS_INSERTAR,
                "insUsu[\"Juan Carlos Perez\",\"juanperez@uagrm.edu.bo\",\"71055123\",\"clave123\",\"1\",\"\"]",
                usuario::insertar);
        registrar("modUsu", "Modifica un usuario (password vacío \"\" = no se cambia)",
                NUsuario.PARAMS_MODIFICAR,
                "modUsu[\"1\",\"Juan Carlos Perez\",\"jperez@uagrm.edu.bo\",\"71055123\",\"\",\"1\",\"\"]",
                usuario::modificar);
        registrar("eliUsu", "Da de baja a un usuario (queda inactivo)",
                new String[]{"id_usuario"}, "eliUsu[\"1\"]", usuario::eliminar);
        registrar("actUsu", "Reactiva un usuario dado de baja",
                new String[]{"id_usuario"}, "actUsu[\"1\"]", usuario::activar);

        grupo("Datos de apoyo para usuarios");
        registrar("lisRol", "Lista los roles (\"*\" = todos)",
                new String[]{"filtro"}, "lisRol[\"*\"]", rol::listar);
        registrar("insRol", "Registra un rol",
                new String[]{"nombre", "descripcion"}, "insRol[\"Administrador\",\"Acceso total al sistema\"]", rol::insertar);
        registrar("lisUni", "Lista las unidades universitarias (\"*\" = todas)",
                new String[]{"filtro"}, "lisUni[\"*\"]", unidad::listar);
    }

    private void grupo(String nombre) {
        grupoActual = nombre;
    }

    private void registrar(String nombre, String descripcion, String[] parametros, String ejemplo, Accion accion) {
        comandos.put(nombre.toLowerCase(), new Definicion(grupoActual, nombre, descripcion, parametros, ejemplo, accion));
    }

    /** Interpreta el asunto, valida y ejecuta. Nunca lanza excepción: siempre devuelve una respuesta. */
    public Respuesta procesar(String asunto) {
        try {
            Comando cmd = Comando.parsear(asunto);
            Definicion def = comandos.get(cmd.getNombre().toLowerCase());
            if (def == null) {
                throw new ComandoException("El comando '" + Html.esc(cmd.getNombre()) + "' no existe.");
            }
            if (cmd.getParametros().size() != def.parametros.length) {
                throw new ComandoException("El comando " + def.nombre + " requiere " + def.parametros.length
                        + " parámetro(s) " + Html.esc(firma(def)) + " y se recibieron " + cmd.getParametros().size()
                        + ".<br>Ejemplo: <code>" + Html.esc(def.ejemplo) + "</code>");
            }
            return def.accion.ejecutar(cmd.getParametros());
        } catch (ComandoException e) {
            return error(asunto, e.getMessage());
        } catch (SQLException e) {
            return error(asunto, "Error en la base de datos: " + Html.esc(e.getMessage()));
        } catch (Exception e) {
            return error(asunto, "Error inesperado: " + Html.esc(String.valueOf(e.getMessage())));
        }
    }

    private Respuesta error(String asunto, String detalle) {
        String html = Html.pagina("Error",
                "<p>No se pudo realizar la orden <code>" + Html.esc(asunto) + "</code>.</p>"
                        + "<p style='color:#b00020'>" + detalle + "</p>"
                        + "<p>Envíe <code>help</code> en el asunto para ver los comandos disponibles.</p>");
        return Respuesta.error("Error", html);
    }

    /** Arma una tabla por cada grupo de comandos. */
    private Respuesta ayuda() {
        Map<String, List<String[]>> porGrupo = new LinkedHashMap<>();
        for (Definicion d : comandos.values()) {
            porGrupo.computeIfAbsent(d.grupo, g -> new ArrayList<>())
                    .add(new String[]{d.nombre, d.descripcion, firma(d), d.ejemplo});
        }
        StringBuilder sb = new StringBuilder("<p>Escriba el comando en el <b>asunto</b> del correo. "
                + "Los parámetros van entre comillas dobles y separados por comas.</p>");
        for (Map.Entry<String, List<String[]>> e : porGrupo.entrySet()) {
            sb.append("<h3>").append(Html.esc(e.getKey())).append("</h3>")
              .append(Html.tabla(new String[]{"Comando", "Descripción", "Parámetros", "Ejemplo"}, e.getValue(), false));
        }
        return Respuesta.exito("Comandos disponibles", Html.pagina("Comandos disponibles", sb.toString()));
    }

    private static String firma(Definicion d) {
        return d.parametros.length == 0 ? "(ninguno)" : "[" + String.join(", ", d.parametros) + "]";
    }
}
