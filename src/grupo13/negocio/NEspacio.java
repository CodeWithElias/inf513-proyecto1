package grupo13.negocio;

import grupo13.datos.DCanal;
import grupo13.datos.DEspacio;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * CU5 / RF10: calendario de espacios publicitarios por canal.
 * Reglas: el canal debe estar activo; no se crean espacios en el pasado; hora_inicio < hora_fin; los espacios de un
 * mismo canal y día no se superponen. Estados: disponible → ocupado (al programar) | bloqueado (manual).
 * Fila: id_espacio, canal, tipo_canal, fecha, hora_inicio, hora_fin, estado, campania_que_lo_ocupa, id_canal.
 */
public class NEspacio {

    public static final String DISPONIBLE = "disponible";
    public static final String OCUPADO = "ocupado";
    public static final String BLOQUEADO = "bloqueado";

    /** Máximo de días en una consulta de disponibilidad o en una generación masiva. */
    public static final int MAX_DIAS = 92;

    private static final int ESTADO = 6;

    private final DEspacio datos = new DEspacio();
    private final DCanal canal = new DCanal();

    /** Calendario: idCanal y fecha pueden ser null (= todos). */
    public List<String[]> listar(String idCanal, String fecha) throws NegocioException {
        try {
            Integer c = idCanal == null ? null : Validar.id(idCanal, "id_canal");
            LocalDate f = fecha == null ? null : Validar.fecha(fecha, "fecha");
            if (c != null) {
                existeCanal(c);
            }
            return datos.listar(c, f);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Espacios libres entre dos fechas (idCanal null = todos los canales). */
    public List<String[]> disponibles(String idCanal, String desde, String hasta) throws NegocioException {
        try {
            Integer c = idCanal == null ? null : Validar.id(idCanal, "id_canal");
            LocalDate d = Validar.fecha(desde, "fecha_desde");
            LocalDate h = Validar.fecha(hasta, "fecha_hasta");
            validarRango(d, h);
            if (c != null) {
                existeCanal(c);
            }
            return datos.listarPorEstado(c, d, h, DISPONIBLE);
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

    /** Fecha DD/MM/AAAA y horas HH:MM. Devuelve el ID creado. */
    public int insertar(String idCanal, String fecha, String horaInicio, String horaFin) throws NegocioException {
        try {
            int c = Validar.id(idCanal, "id_canal");
            LocalDate f = Validar.fecha(fecha, "fecha");
            LocalTime ini = Validar.hora(horaInicio, "hora_inicio");
            LocalTime fin = Validar.hora(horaFin, "hora_fin");
            validarCanalActivo(c);
            validarHorario(ini, fin);
            if (f.isBefore(LocalDate.now())) {
                throw new NegocioException("No se pueden crear espacios en fechas pasadas.");
            }
            if (seSuperpone(c, f, ini, fin)) {
                throw new NegocioException("El horario " + horaInicio + "-" + horaFin + " se superpone con otro espacio del canal "
                        + c + " en esa fecha.");
            }
            return datos.insertar(c, f, ini, fin);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /**
     * Crea el mismo horario todos los días entre dos fechas (máximo 92 días). Omite los días en que se superpone.
     * Devuelve {creados, omitidos}.
     */
    public int[] generar(String idCanal, String desde, String hasta, String horaInicio, String horaFin)
            throws NegocioException {
        try {
            int c = Validar.id(idCanal, "id_canal");
            LocalDate d = Validar.fecha(desde, "fecha_desde");
            LocalDate h = Validar.fecha(hasta, "fecha_hasta");
            LocalTime ini = Validar.hora(horaInicio, "hora_inicio");
            LocalTime fin = Validar.hora(horaFin, "hora_fin");
            validarRango(d, h);
            validarCanalActivo(c);
            validarHorario(ini, fin);
            if (d.isBefore(LocalDate.now())) {
                throw new NegocioException("No se pueden crear espacios en fechas pasadas.");
            }
            List<LocalDate> fechas = new ArrayList<>();
            int omitidos = 0;
            for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) {
                if (seSuperpone(c, f, ini, fin)) {
                    omitidos++;
                } else {
                    fechas.add(f);
                }
            }
            if (fechas.isEmpty()) {
                throw new NegocioException("No se creó ningún espacio: el horario se superpone en todos los días del rango.");
            }
            datos.insertarVarios(c, fechas, ini, fin);
            return new int[]{fechas.size(), omitidos};
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }

    /** Bloquea un espacio disponible (no se podrá programar). */
    public void bloquear(String idEspacio) throws NegocioException {
        cambiar(idEspacio, DISPONIBLE, BLOQUEADO, "solo se bloquean espacios disponibles");
    }

    /** Libera un espacio bloqueado. */
    public void desbloquear(String idEspacio) throws NegocioException {
        cambiar(idEspacio, BLOQUEADO, DISPONIBLE, "solo se desbloquean espacios bloqueados");
    }

    private void cambiar(String idEspacio, String desde, String hacia, String explicacion) throws NegocioException {
        try {
            int id = Validar.id(idEspacio, "id_espacio");
            String[] e = buscar(id);
            if (!desde.equals(e[ESTADO])) {
                throw new NegocioException("El espacio " + id + " está " + e[ESTADO] + "; " + explicacion + ".");
            }
            datos.cambiarEstado(id, hacia);
        } catch (SQLException ex) {
            throw NegocioException.bd(ex);
        }
    }

    // ===== Reglas =====

    private static void validarHorario(LocalTime ini, LocalTime fin) throws NegocioException {
        if (!ini.isBefore(fin)) {
            throw new NegocioException("La hora de inicio debe ser anterior a la hora de fin.");
        }
    }

    private static void validarRango(LocalDate d, LocalDate h) throws NegocioException {
        Validar.rangoFechas(d, h, "fecha_desde", "fecha_hasta");
        if (ChronoUnit.DAYS.between(d, h) + 1 > MAX_DIAS) {
            throw new NegocioException("El rango de fechas no puede superar " + MAX_DIAS + " días.");
        }
    }

    /** Dos horarios [a, b) y [c, d) se superponen si a < d y c < b. */
    private boolean seSuperpone(int idCanal, LocalDate fecha, LocalTime ini, LocalTime fin) throws SQLException {
        for (String[] h : datos.horariosDelDia(idCanal, fecha)) {
            if (ini.isBefore(LocalTime.parse(h[1])) && LocalTime.parse(h[0]).isBefore(fin)) {
                return true;
            }
        }
        return false;
    }

    private void validarCanalActivo(int idCanal) throws SQLException, NegocioException {
        String[] c = existeCanal(idCanal);
        if (!"t".equals(c[3])) {
            throw new NegocioException("El canal " + idCanal + " está dado de baja.");
        }
    }

    private String[] existeCanal(int idCanal) throws SQLException, NegocioException {
        String[] c = canal.obtener(idCanal);
        if (c == null) {
            throw new NegocioException("No existe un canal con ID " + idCanal + ".");
        }
        return c;
    }

    private String[] buscar(int id) throws SQLException, NegocioException {
        String[] fila = datos.obtener(id);
        if (fila == null) {
            throw new NegocioException("No existe un espacio con ID " + id + ".");
        }
        return fila;
    }
}
