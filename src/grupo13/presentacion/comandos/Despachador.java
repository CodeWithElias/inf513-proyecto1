package grupo13.presentacion.comandos;

import grupo13.negocio.Actor;
import grupo13.negocio.NAutenticacion;
import grupo13.negocio.NegocioException;
import grupo13.negocio.Sesion;
import grupo13.presentacion.Html;
import grupo13.presentacion.PAnalisis;
import grupo13.presentacion.PCampania;
import grupo13.presentacion.PCanal;
import grupo13.presentacion.PEmpresa;
import grupo13.presentacion.PEspacio;
import grupo13.presentacion.PMaterial;
import grupo13.presentacion.PMetrica;
import grupo13.presentacion.PNotificacion;
import grupo13.presentacion.PPagina;
import grupo13.presentacion.PPaquete;
import grupo13.presentacion.PPauta;
import grupo13.presentacion.PPlataforma;
import grupo13.presentacion.PProforma;
import grupo13.presentacion.PRedSocial;
import grupo13.presentacion.PReporte;
import grupo13.presentacion.PRepresentante;
import grupo13.presentacion.PRol;
import grupo13.presentacion.PScraping;
import grupo13.presentacion.PSeguidoresRed;
import grupo13.presentacion.PServicio;
import grupo13.presentacion.PTarifa;
import grupo13.presentacion.PTermino;
import grupo13.presentacion.PUnidad;
import grupo13.presentacion.PUsuario;
import grupo13.presentacion.PVisitante;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

    /** Minutos de validez de un código de confirmación. */
    private static final int MINUTOS_CONFIRMACION = 15;

    private final Map<String, Definicion> comandos = new LinkedHashMap<>();
    private final Map<String, Pendiente> pendientes = new HashMap<>();
    private final SecureRandom azar = new SecureRandom();
    private final NAutenticacion autenticacion = new NAutenticacion();
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
        registrar("help", "Lista los comandos que su rol puede usar", new String[]{}, "help", p -> ayuda());
        registrar("conf", "Confirma una operación crítica con el código recibido por correo",
                new String[]{"codigo"}, "conf[\"482913\"]", p -> ayuda()); // lo atiende ejecutar() antes de llegar aquí

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

        registrarCu4();
        registrarCu5();
        registrarCu6();
        registrarCu7();
        registrarCu8();
    }

    /** CU8: Reportes y Estadísticas (RF13). Periodo: fecha_desde y fecha_hasta en DD/MM/AAAA, "*" = sin límite. */
    private void registrarCu8() {
        PReporte reporte = new PReporte();
        String[] periodo = PReporte.PARAMS_PERIODO;

        grupo("CU8: Reportes y estadísticas (RF13) - periodo DD/MM/AAAA o \"*\" = sin límite");
        registrar("tabGen", "Tablero general: ingresos, conversión, campañas, pautas, impacto y alertas",
                periodo, "tabGen[\"01/01/2026\",\"31/12/2026\"]", reporte::tablero);
        registrar("repCom", "Reporte comercial: proformas por estado, ingresos por mes, conversión y principales clientes",
                periodo, "repCom[\"01/01/2026\",\"31/12/2026\"]", reporte::comercial);
        registrar("repSer", "Servicios y paquetes más vendidos",
                periodo, "repSer[\"*\",\"*\"]", reporte::servicios);
        registrar("repPau", "Ocupación de la pauta por canal y tasa de cancelación",
                periodo, "repPau[\"01/12/2026\",\"31/12/2026\"]", reporte::ocupacion);
        registrar("repMat", "Material publicitario: estados, tasa de aprobación y revisiones",
                periodo, "repMat[\"*\",\"*\"]", reporte::material);
        registrar("repImp", "Impacto y alcance: por canal, por campaña y pautas con más interacción",
                periodo, "repImp[\"01/01/2026\",\"31/12/2026\"]", reporte::impacto);
        registrar("repRed", "Crecimiento de seguidores de las redes sociales",
                periodo, "repRed[\"01/01/2026\",\"31/12/2026\"]", reporte::redes);
        registrar("repMon", "Monitoreo de páginas no autorizadas: detecciones por plataforma, término y estado",
                periodo, "repMon[\"*\",\"*\"]", reporte::monitoreo);
        registrar("repCli", "Vista del cliente: su inversión, sus campañas y su impacto (CPM, costo por interacción)",
                new String[]{"id_empresa", "fecha_desde", "fecha_hasta"}, "repCli[\"1\",\"*\",\"*\"]", reporte::cliente);
    }

    /** CU7: Gestión de Análisis de Métricas de Impacto (RF13). */
    private void registrarCu7() {
        PMetrica metrica = new PMetrica();
        PAnalisis analisis = new PAnalisis();

        grupo("CU7: Métricas de las pautas publicadas (RF13) - mediciones acumuladas");
        registrar("insMet", "Registra una medición manual de una pauta publicada (cualquier canal)",
                new String[]{"id_programacion", "alcance", "impresiones", "interacciones", "clics", "compartidos"},
                "insMet[\"5\",\"1200\",\"3400\",\"210\",\"45\",\"12\"]", metrica::registrar);
        registrar("modMet", "Corrige una medición",
                new String[]{"id_metrica", "alcance", "impresiones", "interacciones", "clics", "compartidos"},
                "modMet[\"1\",\"1250\",\"3400\",\"210\",\"45\",\"12\"]", metrica::modificar);
        registrar("eliMet", "Elimina una medición registrada por error",
                new String[]{"id_metrica"}, "eliMet[\"1\"]", metrica::eliminar);
        registrar("lisMet", "Historial de mediciones de una pauta",
                new String[]{"id_programacion"}, "lisMet[\"5\"]", metrica::listar);
        registrar("capMet", "Captura automática desde YouTube: vistas y me gusta/comentarios (\"*\" = todas las pautas de YouTube)",
                new String[]{"id_programacion"}, "capMet[\"*\"]", metrica::capturar);

        grupo("CU7: Análisis de impacto (RF13)");
        registrar("anaPau", "Indicadores de una pauta: tasa de interacción, CTR, frecuencia, viralidad y crecimiento",
                new String[]{"id_programacion"}, "anaPau[\"5\"]", analisis::pauta);
        registrar("anaCam", "Rendimiento de una campaña: totales, por canal y costo por resultado (CPM, CPC)",
                new String[]{"id_campania"}, "anaCam[\"2\"]", analisis::campania);
        registrar("anaRed", "Crecimiento de seguidores de una red social y rendimiento de sus pautas",
                new String[]{"id_canal"}, "anaRed[\"1\"]", analisis::red);
    }

    /** CU6: Monitoreo y detección de páginas no autorizadas mediante scraping (RF12). */
    private void registrarCu6() {
        PTermino termino = new PTermino();
        PScraping scraping = new PScraping();
        PPagina pagina = new PPagina();

        grupo("CU6: Términos de monitoreo (RF12)");
        registrar("lisTer", "Lista los términos de monitoreo",
                new String[]{"filtro"}, "lisTer[\"*\"]", termino::listar);
        registrar("insTer", "Registra un término a buscar (tipo opcional: sigla, nombre, marca…)",
                new String[]{"termino", "tipo"}, "insTer[\"UAGRM\",\"sigla\"]", termino::insertar);
        registrar("eliTer", "Desactiva un término (deja de buscarse)",
                new String[]{"id_termino"}, "eliTer[\"1\"]", termino::eliminar);
        registrar("actTer", "Reactiva un término",
                new String[]{"id_termino"}, "actTer[\"1\"]", termino::activar);

        grupo("CU6: Scraping (por ahora YouTube; también se ejecuta solo cada N horas)");
        registrar("ejeScr", "Ejecuta ahora el scraping: un término de termino_monitoreo o \"*\" = todos los activos",
                new String[]{"id_plataforma", "id_termino"}, "ejeScr[\"1\",\"*\"]", scraping::ejecutar);
        registrar("lisEje", "Historial de ejecuciones (\"*\" = todas las plataformas)",
                new String[]{"id_plataforma"}, "lisEje[\"*\"]", scraping::listar);

        grupo("CU6: Páginas no autorizadas (todo lo que no está en red_social) - estados: detectada → confirmada → denunciada → eliminada");
        registrar("lisPag", "Lista las páginas no autorizadas (busca por URL, nombre, estado o plataforma)",
                new String[]{"filtro"}, "lisPag[\"detectada\"]", pagina::listar);
        registrar("mosPag", "Muestra una página con sus detecciones y evidencias",
                new String[]{"id_pagina"}, "mosPag[\"1\"]", pagina::mostrar);
        registrar("modPag", "Cambia el estado de una página (lo revisa el personal de Canal 11)",
                new String[]{"id_pagina", "estado"}, "modPag[\"1\",\"confirmada\"]", pagina::revisar);
    }

    /** CU5: Gestión de Programar Pauta Multi-Canal (RF10 y RF11). */
    private void registrarCu5() {
        PCanal canal = new PCanal();
        PEspacio espacio = new PEspacio();
        PPauta pauta = new PPauta();
        PNotificacion notificacion = new PNotificacion();

        grupo("CU5: Canales de difusión (las redes sociales se registran en el CU2)");
        registrar("lisCan", "Lista los canales (TV, radio, web y redes sociales)",
                new String[]{"filtro"}, "lisCan[\"*\"]", canal::listar);
        registrar("insCan", "Registra un canal (tipo: television, radio o web)",
                new String[]{"tipo", "nombre"}, "insCan[\"television\",\"Canal 11 UAGRM TV\"]", canal::insertar);
        registrar("eliCan", "Da de baja un canal (sin pautas programadas)",
                new String[]{"id_canal"}, "eliCan[\"1\"]", canal::eliminar);
        registrar("actCan", "Reactiva un canal",
                new String[]{"id_canal"}, "actCan[\"1\"]", canal::activar);

        grupo("CU5: Calendario de espacios (RF10) - fechas DD/MM/AAAA, horas HH:MM");
        registrar("lisEsp", "Calendario de espacios (\"*\" = todos los canales / todas las fechas)",
                new String[]{"id_canal", "fecha"}, "lisEsp[\"1\",\"01/12/2026\"]", espacio::listar);
        registrar("disEsp", "Espacios disponibles en un rango (máximo 92 días)",
                new String[]{"id_canal", "fecha_desde", "fecha_hasta"}, "disEsp[\"*\",\"01/12/2026\",\"07/12/2026\"]",
                espacio::disponibles);
        registrar("insEsp", "Crea un espacio en el calendario de un canal",
                new String[]{"id_canal", "fecha", "hora_inicio", "hora_fin"}, "insEsp[\"1\",\"01/12/2026\",\"20:00\",\"20:01\"]",
                espacio::insertar);
        registrar("genEsp", "Crea el mismo espacio todos los días de un rango (omite superposiciones)",
                new String[]{"id_canal", "fecha_desde", "fecha_hasta", "hora_inicio", "hora_fin"},
                "genEsp[\"1\",\"01/12/2026\",\"31/12/2026\",\"20:00\",\"20:01\"]", espacio::generar);
        registrar("eliEsp", "Bloquea un espacio disponible",
                new String[]{"id_espacio"}, "eliEsp[\"1\"]", espacio::bloquear);
        registrar("actEsp", "Desbloquea un espacio",
                new String[]{"id_espacio"}, "actEsp[\"1\"]", espacio::desbloquear);

        grupo("CU5: Programación de pauta (RF10) y notificación a unidades (RF11)");
        registrar("lisPau", "Lista la pauta (busca por campaña, empresa, canal o estado)",
                new String[]{"filtro"}, "lisPau[\"programada\"]", pauta::listar);
        registrar("mosPau", "Muestra una pauta",
                new String[]{"id_programacion"}, "mosPau[\"1\"]", pauta::mostrar);
        registrar("insPau", "Programa material aprobado en un espacio libre, con cargo a un ítem contratado",
                PPauta.PARAMS_PROGRAMAR, "insPau[\"1\",\"1\",\"1\"]", pauta::programar);
        registrar("repPau", "Mueve una pauta programada a otro espacio libre",
                new String[]{"id_programacion", "id_espacio_nuevo"}, "repPau[\"1\",\"2\"]", pauta::reprogramar);
        registrar("canPau", "Cancela una pauta programada y libera su espacio",
                new String[]{"id_programacion"}, "canPau[\"1\"]", pauta::cancelar);
        registrar("pubPau", "Marca la pauta como publicada (URL obligatoria en redes sociales)",
                new String[]{"id_programacion", "url_publicacion"},
                "pubPau[\"1\",\"https://facebook.com/canal11uagrm/posts/123\"]", pauta::publicar);

        grupo("CU5: Notificaciones (RF11)");
        registrar("lisNot", "Mis notificaciones (\"*\") o las de un usuario (solo personal de Canal 11)",
                new String[]{"id_usuario"}, "lisNot[\"*\"]", notificacion::listar);
        registrar("leeNot", "Marca una notificación como leída",
                new String[]{"id_notificacion"}, "leeNot[\"1\"]", notificacion::marcarLeida);
    }

    /** CU4: Gestión de Cargas, Validar Material Publicitario (RF08 y RF09). */
    private void registrarCu4() {
        PMaterial material = new PMaterial();

        grupo("CU4: Carga de material publicitario (RF08) - tipos: video, audio, imagen, banner, texto");
        registrar("lisMat", "Lista el material (busca por archivo, tipo, estado, campaña o empresa)",
                new String[]{"filtro"}, "lisMat[\"pendiente\"]", material::listar);
        registrar("mosMat", "Muestra un material con su historial de revisiones",
                new String[]{"id_material"}, "mosMat[\"1\"]", material::mostrar);
        registrar("insMat", "El representante carga material a una campaña de su empresa (duración solo video/audio)",
                PMaterial.PARAMS_INSERTAR,
                "insMat[\"1\",\"video\",\"spot_navidad.mp4\",\"https://drive.google.com/spot_navidad.mp4\",\"30\",\"\"]",
                material::insertar);
        registrar("modMat", "Corrige los datos de un material pendiente de revisión",
                PMaterial.PARAMS_MODIFICAR,
                "modMat[\"1\",\"video\",\"spot_navidad_v2.mp4\",\"https://drive.google.com/spot_navidad_v2.mp4\",\"30\",\"\"]",
                material::modificar);
        registrar("subMat", "Sube una nueva versión de un material al que se le pidieron correcciones",
                PMaterial.PARAMS_VERSION,
                "subMat[\"1\",\"spot_navidad_v2.mp4\",\"https://drive.google.com/spot_navidad_v2.mp4\",\"28\",\"\"]",
                material::subirVersion);
        registrar("eliMat", "Retira un material (si no está programado en una pauta)",
                new String[]{"id_material"}, "eliMat[\"1\"]", material::eliminar);

        grupo("CU4: Validación de material (RF09) - revisa personal de Canal 11");
        registrar("aprMat", "Aprueba el material (observaciones opcionales)",
                PMaterial.PARAMS_REVISION, "aprMat[\"1\",\"Cumple los estándares técnicos\"]", material::aprobar);
        registrar("recMat", "Rechaza el material (observaciones obligatorias)",
                PMaterial.PARAMS_REVISION, "recMat[\"1\",\"Contenido no permitido por la normativa\"]", material::rechazar);
        registrar("corMat", "Solicita correcciones al cliente (observaciones obligatorias)",
                PMaterial.PARAMS_REVISION, "corMat[\"1\",\"El audio está saturado, reduzca el volumen\"]",
                material::solicitarCorreccion);
        registrar("lisRev", "Lista el historial de revisiones de un material",
                new String[]{"id_material"}, "lisRev[\"1\"]", material::listarRevisiones);
    }

    private void grupo(String nombre) {
        grupoActual = nombre;
    }

    private void registrar(String nombre, String descripcion, String[] parametros, String ejemplo, Accion accion) {
        comandos.put(nombre.toLowerCase(), new Definicion(grupoActual, nombre, descripcion, parametros, ejemplo, accion));
    }

    /**
     * RF02: identifica al remitente, verifica que su rol pueda usar el comando, pide confirmación en las operaciones
     * críticas y ejecuta. Nunca lanza excepción: siempre devuelve una respuesta.
     */
    public Respuesta procesar(String remitente, String asunto) {
        Actor actor;
        try {
            actor = autenticacion.identificar(remitente);
        } catch (NegocioException e) {
            return error(asunto, Html.esc(e.getMessage()));
        }
        Sesion.iniciar(actor);
        try {
            return ejecutar(actor, asunto, false);
        } finally {
            Sesion.cerrar();
        }
    }

    /** Correo con el que se ejecuta el modo de prueba si no se indica otro (primer administrador activo). */
    public String remitentePorDefecto() throws NegocioException {
        return autenticacion.correoAdministrador();
    }

    private Respuesta ejecutar(Actor actor, String asunto, boolean confirmado) {
        try {
            Comando cmd = Comando.parsear(asunto);
            String nombre = cmd.getNombre().toLowerCase();
            if (actor.es(Actor.INACTIVO)) {
                throw new ComandoException("Su cuenta (" + Html.esc(actor.getEmail()) + ") está dada de baja; comuníquese con"
                        + " Canal 11 para reactivarla.");
            }
            if (nombre.equals("conf")) {
                return confirmar(actor, cmd);
            }
            Definicion def = comandos.get(nombre);
            if (def == null) {
                throw new ComandoException("El comando '" + Html.esc(cmd.getNombre()) + "' no existe.");
            }
            if (!Permisos.permitido(actor.getRol(), def.nombre)) {
                throw new ComandoException("Su rol (" + Html.esc(actor.getRol()) + ") no tiene permiso para usar "
                        + def.nombre + ".");
            }
            if (cmd.getParametros().size() != def.parametros.length) {
                throw new ComandoException("El comando " + def.nombre + " requiere " + def.parametros.length
                        + " parámetro(s) " + Html.esc(firma(def)) + " y se recibieron " + cmd.getParametros().size()
                        + ".<br>Ejemplo: <code>" + Html.esc(def.ejemplo) + "</code>");
            }
            if (!confirmado && Permisos.critico(def.nombre)) {
                return pedirConfirmacion(actor, asunto);
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

    // ===== Confirmación de operaciones críticas =====

    /** Orden crítica en espera: quién la pidió, qué pidió y hasta cuándo vale el código. */
    private static final class Pendiente {
        final String email;
        final String asunto;
        final LocalDateTime vence;

        Pendiente(String email, String asunto, LocalDateTime vence) {
            this.email = email;
            this.asunto = asunto;
            this.vence = vence;
        }
    }

    /** Guarda la orden y responde (al correo real del remitente) con el código para confirmarla. */
    private Respuesta pedirConfirmacion(Actor actor, String asunto) {
        pendientes.values().removeIf(p -> p.vence.isBefore(LocalDateTime.now()));
        String codigo;
        do {
            codigo = String.format("%06d", azar.nextInt(1_000_000));
        } while (pendientes.containsKey(codigo));
        LocalDateTime vence = LocalDateTime.now().plusMinutes(MINUTOS_CONFIRMACION);
        pendientes.put(codigo, new Pendiente(actor.getEmail(), asunto, vence));
        String html = Html.parrafo("Para ejecutar la orden " + asunto + " envíe un correo con este asunto:")
                + "<p style='font-size:20px'><b><code>conf[\"" + codigo + "\"]</code></b></p>"
                + Html.parrafo("El código vence a las " + vence.format(DateTimeFormatter.ofPattern("HH:mm")) + " ("
                + MINUTOS_CONFIRMACION + " minutos) y solo puede usarlo " + actor.getEmail() + ".")
                + Html.parrafo("Si usted no envió esta orden, ignore este correo: no se realizará ningún cambio.");
        return Respuesta.exito("Confirmación requerida", Html.pagina("Confirmación requerida", html));
    }

    /** conf["codigo"]: ejecuta la orden pendiente si el código es del mismo remitente y no venció. */
    private Respuesta confirmar(Actor actor, Comando cmd) throws ComandoException {
        if (cmd.getParametros().size() != 1) {
            throw new ComandoException("Use conf[\"código\"] con el código de 6 dígitos que recibió.");
        }
        Pendiente p = pendientes.get(cmd.getParametros().get(0));
        if (p == null || !p.email.equalsIgnoreCase(actor.getEmail())) {
            throw new ComandoException("El código de confirmación no existe, ya fue usado o no corresponde a su correo.");
        }
        pendientes.remove(cmd.getParametros().get(0));
        if (p.vence.isBefore(LocalDateTime.now())) {
            throw new ComandoException("El código venció; envíe nuevamente la orden " + Html.esc(p.asunto) + ".");
        }
        return ejecutar(actor, p.asunto, true);
    }

    private Respuesta error(String asunto, String detalle) {
        String html = Html.pagina("Error",
                "<p>No se pudo realizar la orden <code>" + Html.esc(asunto) + "</code>.</p>"
                        + "<p style='color:#b00020'>" + detalle + "</p>"
                        + "<p>Envíe <code>help</code> en el asunto para ver los comandos disponibles.</p>");
        return Respuesta.error("Error", html);
    }

    /** Arma una tabla por cada grupo, solo con los comandos que el rol del remitente puede usar. */
    private Respuesta ayuda() {
        Actor actor = Sesion.actual();
        Map<String, List<String[]>> porGrupo = new LinkedHashMap<>();
        for (Definicion d : comandos.values()) {
            if (actor != null && !Permisos.permitido(actor.getRol(), d.nombre)) {
                continue;
            }
            String descripcion = d.descripcion + (Permisos.critico(d.nombre) ? " ✱" : "");
            porGrupo.computeIfAbsent(d.grupo, g -> new ArrayList<>())
                    .add(new String[]{d.nombre, descripcion, firma(d), d.ejemplo});
        }
        StringBuilder sb = new StringBuilder();
        if (actor != null) {
            sb.append(Html.parrafo("Usted está identificado como " + actor.getEmail() + " — rol: " + actor.getRol() + "."));
        }
        sb.append("<p>Escriba el comando en el <b>asunto</b> del correo. "
                + "Los parámetros van entre comillas dobles y separados por comas. "
                + "Los comandos marcados con ✱ piden un código de confirmación que llega a su correo.</p>");
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
