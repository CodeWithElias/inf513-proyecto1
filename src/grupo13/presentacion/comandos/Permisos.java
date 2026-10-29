package grupo13.presentacion.comandos;

import grupo13.negocio.Actor;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RF02: qué comandos puede usar cada rol (matriz fija en el código, por nombre de rol).
 *  - Administrador: todos.
 *  - Director: todos menos administrar usuarios y roles.
 *  - Encargado de Unidad: sus redes sociales, la pauta de sus redes, su análisis y sus notificaciones.
 *  - Cliente: catálogo y lo de su empresa (proformas, campañas, material, pauta, métricas, análisis y reportes).
 *  - Visitante (correo no registrado): catálogo y cotizar su propia proforma.
 * "help" y "conf" están disponibles para todos. Las reglas de propiedad ("solo lo suyo") las aplica la capa de negocio.
 * Las operaciones críticas piden un código de confirmación que llega al correo real del remitente.
 */
final class Permisos {

    private static final Set<String> SIEMPRE = minusculas("help", "conf");

    private static final Set<String> SOLO_ADMINISTRADOR = minusculas(
            "insUsu", "modUsu", "eliUsu", "actUsu", "insRol", "modRol", "eliRol", "actRol");

    private static final Set<String> CATALOGO = minusculas("lisSer", "lisTar", "mosTar", "lisPaq", "mosPaq");
    private static final Set<String> COTIZAR = minusculas("lisPro", "mosPro", "insPro", "addDet", "quiDet", "aprPro", "recPro");

    private static final Map<String, Set<String>> POR_ROL = Map.of(
            Actor.ENCARGADO, minusculas(
                    "lisRed", "mosRed", "insSeg", "lisSeg", "lisPau", "mosPau", "anaRed", "lisEsp", "disEsp",
                    "lisNot", "leeNot"),
            Actor.CLIENTE, union(CATALOGO, COTIZAR, minusculas(
                    "mosEmp", "lisCam", "lisMat", "mosMat", "insMat", "modMat", "subMat", "eliMat", "lisRev",
                    "lisPau", "mosPau", "lisMet", "anaPau", "anaCam", "repCli", "lisNot", "leeNot")),
            Actor.VISITANTE, union(CATALOGO, COTIZAR));

    /** Operaciones que cambian datos sensibles: se confirman con un código enviado al correo real. */
    private static final Set<String> CRITICOS = minusculas(
            "insUsu", "modUsu", "insRol", "modRol", "insRep", "modRep", "aprPro", "recPro", "genCam",
            "aprMat", "recMat", "corMat", "repPau", "canPau", "modPag");

    private Permisos() {
    }

    static boolean permitido(String rol, String comando) {
        String c = comando.toLowerCase(Locale.ROOT);
        if (SIEMPRE.contains(c) || rol.equals(Actor.ADMINISTRADOR)) {
            return true;
        }
        if (rol.equals(Actor.DIRECTOR)) {
            return !SOLO_ADMINISTRADOR.contains(c);
        }
        Set<String> s = POR_ROL.get(rol);
        return s != null && s.contains(c);
    }

    /** Todas las bajas y reactivaciones (eli*, act*) y las operaciones de la lista CRITICOS. */
    static boolean critico(String comando) {
        String c = comando.toLowerCase(Locale.ROOT);
        return c.startsWith("eli") || c.startsWith("act") || CRITICOS.contains(c);
    }

    private static Set<String> minusculas(String... comandos) {
        return Stream.of(comandos).map(c -> c.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @SafeVarargs
    private static Set<String> union(Set<String>... conjuntos) {
        return Stream.of(conjuntos).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());
    }
}
