package grupo13.negocio;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Quién envía el comando, identificado por el correo del remitente.
 * Un correo que no está en la tabla usuario es un Visitante. Un usuario o rol dado de baja es una cuenta inactiva.
 */
public final class Actor {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String DIRECTOR = "Director";
    public static final String ENCARGADO = "Encargado de Unidad";
    public static final String CLIENTE = "Cliente";
    public static final String VISITANTE = "Visitante";
    public static final String INACTIVO = "Cuenta inactiva";

    private static final String[] ROLES = {ADMINISTRADOR, DIRECTOR, ENCARGADO, CLIENTE, VISITANTE};

    private final Integer idUsuario;
    private final String nombre;
    private final String email;
    private final String rol;
    private final Integer idEmpresa;
    private final Integer idUnidad;

    Actor(Integer idUsuario, String nombre, String email, String rol, Integer idEmpresa, Integer idUnidad) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.idEmpresa = idEmpresa;
        this.idUnidad = idUnidad;
    }

    /** Nombre del rol conocido que corresponde (sin distinguir mayúsculas ni tildes); si no, el nombre tal cual. */
    static String rolCanonico(String nombreRol) {
        String n = normalizar(nombreRol);
        for (String r : ROLES) {
            if (normalizar(r).equals(n)) {
                return r;
            }
        }
        return nombreRol;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** null si es un visitante (no registrado). */
    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    /** Empresa que representa (solo clientes); null si no corresponde. */
    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    /** Unidad universitaria asignada (encargados); null si no tiene. */
    public Integer getIdUnidad() {
        return idUnidad;
    }

    public boolean es(String nombreRol) {
        return rol.equals(nombreRol);
    }

    /** Personal de Canal 11 con acceso a todos los datos (Administrador o Director). */
    public boolean esPersonal() {
        return es(ADMINISTRADOR) || es(DIRECTOR);
    }
}
