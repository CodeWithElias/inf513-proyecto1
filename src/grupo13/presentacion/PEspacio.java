package grupo13.presentacion;

import grupo13.negocio.NEspacio;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU5 – Calendario de espacios: lisEsp, disEsp, insEsp, genEsp, eliEsp, actEsp. */
public class PEspacio {

    private static final String[] COLUMNAS = {"ID", "Canal", "Tipo de canal", "Fecha", "Inicio", "Fin", "Estado", "Campaña"};

    private final NEspacio negocio = new NEspacio();

    /** lisEsp["id_canal" o "*", "fecha" o "*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        String canal = Vista.filtro(p.get(0));
        String fecha = Vista.filtro(p.get(1));
        String titulo = "Calendario de espacios" + (canal == null ? "" : " - canal " + canal) + (fecha == null ? "" : " - " + fecha);
        return Vista.listado("Calendario de espacios", titulo, COLUMNAS, negocio.listar(canal, fecha));
    }

    /** disEsp["id_canal" o "*", "fecha_desde", "fecha_hasta"] */
    public Respuesta disponibles(List<String> p) throws NegocioException {
        String titulo = "Espacios disponibles del " + p.get(1) + " al " + p.get(2);
        return Vista.listado("Espacios disponibles", titulo, COLUMNAS, negocio.disponibles(Vista.filtro(p.get(0)), p.get(1), p.get(2)));
    }

    /** insEsp["id_canal","fecha","hora_inicio","hora_fin"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1), p.get(2), p.get(3));
        return Vista.exito("El espacio fue registrado con ID " + id + ".", COLUMNAS, negocio.obtener(id));
    }

    /** genEsp["id_canal","fecha_desde","fecha_hasta","hora_inicio","hora_fin"] */
    public Respuesta generar(List<String> p) throws NegocioException {
        int[] r = negocio.generar(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4));
        String mensaje = "Se crearon " + r[0] + " espacio(s) de " + p.get(3) + " a " + p.get(4) + " entre el " + p.get(1)
                + " y el " + p.get(2) + (r[1] > 0 ? "; se omitieron " + r[1] + " día(s) por superposición." : ".");
        return Vista.exito(mensaje, Html.tabla(COLUMNAS, negocio.disponibles(p.get(0), p.get(1), p.get(2))));
    }

    /** eliEsp["id"]: bloquea el espacio. */
    public Respuesta bloquear(List<String> p) throws NegocioException {
        negocio.bloquear(p.get(0));
        return Vista.exito("El espacio " + p.get(0) + " fue bloqueado. Puede liberarlo con actEsp[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(Integer.parseInt(p.get(0))));
    }

    /** actEsp["id"]: desbloquea el espacio. */
    public Respuesta desbloquear(List<String> p) throws NegocioException {
        negocio.desbloquear(p.get(0));
        return Vista.exito("El espacio " + p.get(0) + " vuelve a estar disponible.", COLUMNAS,
                negocio.obtener(Integer.parseInt(p.get(0))));
    }
}
