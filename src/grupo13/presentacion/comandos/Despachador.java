package grupo13.presentacion.comandos;

import grupo13.negocio.NegocioException;
import grupo13.presentacion.Html;
import grupo13.presentacion.PCampania;
import grupo13.presentacion.PEmpresa;
import grupo13.presentacion.PPaquete;
import grupo13.presentacion.PPlataforma;
import grupo13.presentacion.PProforma;
import grupo13.presentacion.PRedSocial;
import grupo13.presentacion.PRepresentante;
import grupo13.presentacion.PRol;
import grupo13.presentacion.PSeguidoresRed;
import grupo13.presentacion.PServicio;
import grupo13.presentacion.PTarifa;
import grupo13.presentacion.PUnidad;
import grupo13.presentacion.PUsuario;
import grupo13.presentacion.PVisitante;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de todos los comandos del sistema.
 * Capa de presentación: registra cada comando con su clase P* (que a su vez usa la capa de negocio).
 * Para agregar un caso de uso: crear D* (datos), N* (negocio) y P* (presentación), y registrar(...) aquí.
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
        PUsuario usuario = new PUsuario();
        PRol rol = new PRol();
        PUnidad unidad = new PUnidad();
        PRepresentante cliente = new PRepresentante();
        PEmpresa empresa = new PEmpresa();
        PPlataforma plataforma = new PPlataforma();
        PRedSocial redSocial = new PRedSocial();
        PSeguidoresRed seguidores = new PSeguidoresRed();

        grupo("Ayuda");
        registrar("help", "Lista todos los comandos existentes", new String[]{}, "help", p -> ayuda());

        grupo("CU1: Gestión de Usuarios");
        registrar("lisUsu", "Lista los usuarios (\"*\" = todos, o un texto para buscar por nombre/email/rol/unidad)",
                new String[]{"filtro"}, "lisUsu[\"*\"]", usuario::listar);
        registrar("mosUsu", "Muestra un usuario por su ID",
                new String[]{"id_usuario"}, "mosUsu[\"1\"]", usuario::mostrar);
        registrar("insUsu", "Registra un usuario (id_unidad puede ir vacío \"\")",
                PUsuario.PARAMS_INSERTAR,
                "insUsu[\"Juan Carlos Perez\",\"juanperez@uagrm.edu.bo\",\"71055123\",\"clave123\",\"1\",\"\"]",
                usuario::insertar);
        registrar("modUsu", "Modifica un usuario (password vacío \"\" = no se cambia)",
                PUsuario.PARAMS_MODIFICAR,
                "modUsu[\"1\",\"Juan Carlos Perez\",\"jperez@uagrm.edu.bo\",\"71055123\",\"\",\"1\",\"\"]",
                usuario::modificar);
        registrar("eliUsu", "Da de baja a un usuario (queda inactivo)",
                new String[]{"id_usuario"}, "eliUsu[\"1\"]", usuario::eliminar);
        registrar("actUsu", "Reactiva un usuario dado de baja",
                new String[]{"id_usuario"}, "actUsu[\"1\"]", usuario::activar);

        grupo("CU1: Roles (Director, Encargado de Unidad, Cliente…)");
        registrar("lisRol", "Lista los roles (\"*\" = todos)",
                new String[]{"filtro"}, "lisRol[\"*\"]", rol::listar);
        registrar("mosRol", "Muestra un rol por su ID",
                new String[]{"id_rol"}, "mosRol[\"1\"]", rol::mostrar);
        registrar("insRol", "Registra un rol",
                new String[]{"nombre", "descripcion"}, "insRol[\"Director\",\"Director de Canal 11\"]", rol::insertar);
        registrar("modRol", "Modifica un rol",
                new String[]{"id_rol", "nombre", "descripcion"}, "modRol[\"2\",\"Director\",\"Dirección general de Canal 11\"]",
                rol::modificar);
        registrar("eliRol", "Da de baja un rol (solo si no tiene usuarios activos)",
                new String[]{"id_rol"}, "eliRol[\"2\"]", rol::eliminar);
        registrar("actRol", "Reactiva un rol dado de baja",
                new String[]{"id_rol"}, "actRol[\"2\"]", rol::activar);

        grupo("CU1: Unidades Universitarias (para Encargados de Unidad)");
        registrar("lisUni", "Lista las unidades universitarias (\"*\" = todas)",
                new String[]{"filtro"}, "lisUni[\"*\"]", unidad::listar);
        registrar("mosUni", "Muestra una unidad y sus subunidades",
                new String[]{"id_unidad"}, "mosUni[\"1\"]", unidad::mostrar);
        registrar("insUni", "Registra una unidad (tipo e id_unidad_padre pueden ir vacíos \"\")",
                new String[]{"nombre", "tipo", "id_unidad_padre"},
                "insUni[\"Facultad de Ciencias de la Computación\",\"Facultad\",\"\"]", unidad::insertar);
        registrar("modUni", "Modifica una unidad",
                new String[]{"id_unidad", "nombre", "tipo", "id_unidad_padre"},
                "modUni[\"2\",\"Ingeniería Informática\",\"Carrera\",\"1\"]", unidad::modificar);
        registrar("eliUni", "Da de baja una unidad (sin usuarios ni subunidades activas)",
                new String[]{"id_unidad"}, "eliUni[\"2\"]", unidad::eliminar);
        registrar("actUni", "Reactiva una unidad dada de baja",
                new String[]{"id_unidad"}, "actUni[\"2\"]", unidad::activar);

        grupo("CU1: Clientes (representantes de empresas anunciantes)");
        registrar("lisRep", "Lista los clientes (busca por nombre, email, CI, empresa o NIT)",
                new String[]{"filtro"}, "lisRep[\"*\"]", cliente::listar);
        registrar("mosRep", "Muestra un cliente por su ID de usuario",
                new String[]{"id_usuario"}, "mosRep[\"3\"]", cliente::mostrar);
        registrar("insRep", "Convierte un usuario existente en cliente de una empresa",
                new String[]{"id_usuario", "ci", "cargo", "id_empresa"},
                "insRep[\"3\",\"4715292\",\"Gerente comercial\",\"1\"]", cliente::insertar);
        registrar("modRep", "Modifica los datos de cliente de un usuario",
                new String[]{"id_usuario", "ci", "cargo", "id_empresa"},
                "modRep[\"3\",\"4715292\",\"Representante legal\",\"1\"]", cliente::modificar);
        registrar("eliRep", "Quita el perfil de cliente (el usuario se mantiene)",
                new String[]{"id_usuario"}, "eliRep[\"3\"]", cliente::eliminar);

        grupo("CU2: Gestión de Cuentas de Redes Sociales Institucionales");
        registrar("lisPla", "Lista las plataformas de redes sociales (\"*\" = todas)",
                new String[]{"filtro"}, "lisPla[\"*\"]", plataforma::listar);
        registrar("insPla", "Registra una plataforma de red social",
                new String[]{"nombre"}, "insPla[\"TikTok\"]", plataforma::insertar);

        registrar("lisRed", "Lista las redes sociales (\"*\" = todas, o busca por nombre/plataforma/url/responsable/unidad)",
                new String[]{"filtro"}, "lisRed[\"*\"]", redSocial::listar);
        registrar("mosRed", "Muestra una red social por su ID de canal",
                new String[]{"id_canal"}, "mosRed[\"1\"]", redSocial::mostrar);
        registrar("insRed", "Registra una red social institucional (id_usuario e id_unidad pueden ir vacíos \"\")",
                new String[]{"nombre", "url", "id_plataforma", "id_usuario", "id_unidad"},
                "insRed[\"Canal 11 Oficial Facebook\",\"https://facebook.com/canal11uagrm\",\"1\",\"1\",\"1\"]",
                redSocial::insertar);
        registrar("modRed", "Modifica una red social institucional",
                new String[]{"id_canal", "nombre", "url", "id_plataforma", "id_usuario", "id_unidad"},
                "modRed[\"1\",\"Canal 11 Oficial FB\",\"https://facebook.com/canal11oficial\",\"1\",\"1\",\"1\"]",
                redSocial::modificar);
        registrar("eliRed", "Da de baja una red social (inactivación lógica)",
                new String[]{"id_canal"}, "eliRed[\"1\"]", redSocial::eliminar);
        registrar("actRed", "Reactiva una red social dada de baja",
                new String[]{"id_canal"}, "actRed[\"1\"]", redSocial::activar);

        registrar("insSeg", "Registra el conteo de seguidores para la fecha actual",
                new String[]{"id_canal", "seguidores"}, "insSeg[\"1\",\"15400\"]", seguidores::insertar);
        registrar("lisSeg", "Lista el historial de mediciones de seguidores de una red social",
                new String[]{"id_canal"}, "lisSeg[\"1\"]", seguidores::listar);

        registrarCu3(empresa);
    }

    /** CU3: Gestión de Oferta Comercial, Clientes y Generación de Cotizaciones (RF04 a RF07). */
    private void registrarCu3(PEmpresa empresa) {
        PVisitante visitante = new PVisitante();
        PServicio servicio = new PServicio();
        PTarifa tarifa = new PTarifa();
        PPaquete paquete = new PPaquete();
        PProforma proforma = new PProforma();
        PCampania campania = new PCampania();

        grupo("CU3: Empresas anunciantes (RF04)");
        registrar("lisEmp", "Lista las empresas (busca por razón social, NIT o nombre comercial)",
                new String[]{"filtro"},
                "lisEmp[\"*\"]", empresa::listar);
        registrar("mosEmp", "Muestra una empresa y sus representantes",
                new String[]{"id_empresa"},
                "mosEmp[\"1\"]", empresa::mostrar);
        registrar("insEmp", "Registra una empresa (nombre_comercial, dirección, email y teléfono pueden ir vacíos)",
                PEmpresa.PARAMS,
                "insEmp[\"Importadora Santa Cruz SRL\",\"1023456789\",\"ImporSCZ\",\"Av. Banzer 123\",\"facturas@imporscz.com\",\"33445566\"]", empresa::insertar);
        registrar("modEmp", "Modifica una empresa",
                new String[]{"id_empresa", "razon_social", "nit", "nombre_comercial", "direccion_facturacion", "email_facturacion", "telefono"},
                "modEmp[\"1\",\"Importadora Santa Cruz SRL\",\"1023456789\",\"ImporSCZ\",\"Av. Banzer 456\",\"facturas@imporscz.com\",\"33445566\"]", empresa::modificar);
        registrar("eliEmp", "Da de baja una empresa (sin proformas pendientes)",
                new String[]{"id_empresa"},
                "eliEmp[\"1\"]", empresa::eliminar);
        registrar("actEmp", "Reactiva una empresa dada de baja",
                new String[]{"id_empresa"},
                "actEmp[\"1\"]", empresa::activar);

        grupo("CU3: Visitantes (cotizan sin cuenta)");
        registrar("lisVis", "Lista los visitantes",
                new String[]{"filtro"},
                "lisVis[\"*\"]", visitante::listar);
        registrar("insVis", "Registra un visitante (email o teléfono obligatorio)",
                new String[]{"nombre", "email", "telefono"},
                "insVis[\"María López\",\"maria@gmail.com\",\"70011223\"]", visitante::insertar);
        registrar("modVis", "Modifica un visitante",
                new String[]{"id_visitante", "nombre", "email", "telefono"},
                "modVis[\"1\",\"María López\",\"maria.lopez@gmail.com\",\"70011223\"]", visitante::modificar);
        registrar("eliVis", "Elimina un visitante (solo si no tiene proformas)",
                new String[]{"id_visitante"},
                "eliVis[\"1\"]", visitante::eliminar);

        grupo("CU3: Servicios publicitarios (RF05)");
        registrar("lisSer", "Lista los servicios publicitarios",
                new String[]{"filtro"},
                "lisSer[\"*\"]", servicio::listar);
        registrar("insSer", "Registra un servicio (tipo y descripción pueden ir vacíos)",
                new String[]{"nombre", "tipo", "descripcion"},
                "insSer[\"Spot en pausa comercial\",\"TV\",\"Spot de 30 s en tanda comercial\"]", servicio::insertar);
        registrar("modSer", "Modifica un servicio",
                new String[]{"id_servicio", "nombre", "tipo", "descripcion"},
                "modSer[\"1\",\"Spot en pausa comercial\",\"TV\",\"Spot de 30 s en horario estelar\"]", servicio::modificar);
        registrar("eliSer", "Da de baja un servicio (sin tarifas activas)",
                new String[]{"id_servicio"},
                "eliSer[\"1\"]", servicio::eliminar);
        registrar("actSer", "Reactiva un servicio",
                new String[]{"id_servicio"},
                "actSer[\"1\"]", servicio::activar);

        grupo("CU3: Tarifario (RF05) - fechas DD/MM/AAAA; id_unidad, unidad_cobro, duración y vigencia pueden ir vacíos");
        registrar("lisTar", "Lista las tarifas (busca por servicio, tipo, unidad o unidad de cobro)",
                new String[]{"filtro"},
                "lisTar[\"*\"]", tarifa::listar);
        registrar("mosTar", "Muestra una tarifa",
                new String[]{"id_tarifa"},
                "mosTar[\"1\"]", tarifa::mostrar);
        registrar("insTar", "Registra una tarifa para un servicio",
                PTarifa.PARAMS,
                "insTar[\"1\",\"\",\"350.00\",\"por spot\",\"30\",\"01/01/2026\",\"31/12/2026\"]", tarifa::insertar);
        registrar("modTar", "Modifica una tarifa (recalcula los paquetes que la usan)",
                new String[]{"id_tarifa", "id_servicio", "id_unidad", "monto", "unidad_cobro", "duracion_seg", "vigente_desde", "vigente_hasta"},
                "modTar[\"1\",\"1\",\"\",\"380.00\",\"por spot\",\"30\",\"01/01/2026\",\"31/12/2026\"]", tarifa::modificar);
        registrar("eliTar", "Deshabilita una tarifa (no debe estar en paquetes activos)",
                new String[]{"id_tarifa"},
                "eliTar[\"1\"]", tarifa::eliminar);
        registrar("actTar", "Reactiva una tarifa",
                new String[]{"id_tarifa"},
                "actTar[\"1\"]", tarifa::activar);

        grupo("CU3: Paquetes publicitarios (RF06)");
        registrar("lisPaq", "Lista los paquetes",
                new String[]{"filtro"},
                "lisPaq[\"*\"]", paquete::listar);
        registrar("mosPaq", "Muestra un paquete con sus tarifas",
                new String[]{"id_paquete"},
                "mosPaq[\"1\"]", paquete::mostrar);
        registrar("insPaq", "Registra un paquete (descuento en %; vacío = 0)",
                new String[]{"nombre", "descripcion", "descuento"},
                "insPaq[\"Paquete TV + Redes\",\"Spots en TV y publicaciones en redes de facultades\",\"10\"]", paquete::insertar);
        registrar("modPaq", "Modifica un paquete (recalcula su precio)",
                new String[]{"id_paquete", "nombre", "descripcion", "descuento"},
                "modPaq[\"1\",\"Paquete TV + Redes\",\"Combo mensual\",\"15\"]", paquete::modificar);
        registrar("addPaq", "Agrega una tarifa al paquete (o cambia su cantidad)",
                new String[]{"id_paquete", "id_tarifa", "cantidad"},
                "addPaq[\"1\",\"1\",\"10\"]", paquete::agregarTarifa);
        registrar("quiPaq", "Quita una tarifa del paquete",
                new String[]{"id_paquete", "id_tarifa"},
                "quiPaq[\"1\",\"1\"]", paquete::quitarTarifa);
        registrar("eliPaq", "Da de baja un paquete",
                new String[]{"id_paquete"},
                "eliPaq[\"1\"]", paquete::eliminar);
        registrar("actPaq", "Reactiva un paquete",
                new String[]{"id_paquete"},
                "actPaq[\"1\"]", paquete::activar);

        grupo("CU3: Proformas y cotizaciones (RF07)");
        registrar("lisPro", "Lista las proformas (busca por número, estado, empresa, NIT o visitante)",
                new String[]{"filtro"},
                "lisPro[\"pendiente\"]", proforma::listar);
        registrar("mosPro", "Muestra la cotización completa con su detalle y totales",
                new String[]{"id_proforma"},
                "mosPro[\"1\"]", proforma::mostrar);
        registrar("insPro", "Crea una proforma para una empresa o un visitante (dias_validez vacío = 15; descuento en Bs)",
                new String[]{"id_empresa", "id_visitante", "dias_validez", "descuento"},
                "insPro[\"1\",\"\",\"15\",\"0\"]", proforma::insertar);
        registrar("modPro", "Modifica el descuento (Bs) y la fecha de validez de una proforma pendiente",
                new String[]{"id_proforma", "descuento", "fecha_validez"},
                "modPro[\"1\",\"100\",\"30/11/2026\"]", proforma::modificar);
        registrar("addDet", "Agrega una tarifa o un paquete a la proforma (uno de los dos vacío)",
                new String[]{"id_proforma", "id_tarifa", "id_paquete", "cantidad", "frecuencia"},
                "addDet[\"1\",\"1\",\"\",\"20\",\"2 por día\"]", proforma::agregarDetalle);
        registrar("quiDet", "Quita un ítem de la proforma",
                new String[]{"id_detalle"},
                "quiDet[\"1\"]", proforma::quitarDetalle);
        registrar("aprPro", "Aprueba la proforma (el cliente acepta la cotización)",
                new String[]{"id_proforma"},
                "aprPro[\"1\"]", proforma::aprobar);
        registrar("recPro", "Rechaza la proforma",
                new String[]{"id_proforma"},
                "recPro[\"1\"]", proforma::rechazar);

        grupo("CU3: Campañas (desde una proforma aprobada)");
        registrar("lisCam", "Lista las campañas",
                new String[]{"filtro"},
                "lisCam[\"*\"]", campania::listar);
        registrar("genCam", "Genera la campaña de una proforma aprobada de una empresa",
                new String[]{"id_proforma", "nombre", "fecha_inicio", "fecha_fin"},
                "genCam[\"1\",\"Campaña Navidad 2026\",\"01/12/2026\",\"31/12/2026\"]", campania::generar);
        registrar("modCam", "Modifica una campaña (estado: activa, pausada, finalizada, cancelada)",
                new String[]{"id_campania", "nombre", "fecha_inicio", "fecha_fin", "estado"},
                "modCam[\"1\",\"Campaña Navidad 2026\",\"01/12/2026\",\"31/12/2026\",\"pausada\"]", campania::modificar);
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
            return error(asunto, e.getMessage()); // mensaje propio de la presentación (puede traer HTML)
        } catch (NegocioException e) {
            return error(asunto, Html.esc(e.getMessage())); // texto plano de la capa de negocio
        } catch (RuntimeException e) {
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
