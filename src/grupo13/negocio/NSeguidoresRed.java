package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DRedSocial;
import grupo13.datos.DSeguidoresRed;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: registro y consulta del histórico de seguidores de redes sociales oficiales.
 */
public class NSeguidoresRed {

    private static final String[] TITULOS = {"ID", "Red Social", "Fecha de Medición", "Seguidores"};

    private final DSeguidoresRed datos = new DSeguidoresRed();
    private final DRedSocial redSocial = new DRedSocial();

    /** lisSeg["id_canal"]: historial de mediciones de seguidores de una red social */
    public Respuesta listar(List<String> p) throws SQLException, ComandoException {
        int idCanal = Validar.id(p.get(0), "id_canal");
        if (redSocial.estaActivo(idCanal) == null) {
            throw new ComandoException("No existe una red social con ID " + idCanal + ".");
        }
        List<String[]> filas = datos.listarPorCanal(idCanal);
        return Respuesta.exito("Historial de Seguidores - Red Social " + idCanal,
                Html.pagina("Historial de Seguidores - Red Social " + idCanal, Html.tabla(TITULOS, filas)));
    }

    /** insSeg["id_canal","seguidores"]: registra una medición de seguidores para la fecha actual */
    public Respuesta insertar(List<String> p) throws Exception {
        int idCanal = Validar.id(p.get(0), "id_canal");
        int seguidores = Validar.enteroNoNegativo(p.get(1), "seguidores");

        Boolean activo = redSocial.estaActivo(idCanal);
        if (activo == null) {
            throw new ComandoException("No existe una red social con ID " + idCanal + ".");
        }
        if (!activo) {
            throw new ComandoException("La red social " + idCanal + " está inactiva. Reactívela con actRed[\""
                    + idCanal + "\"] antes de registrar métricas de seguidores.");
        }

        datos.insertar(idCanal, seguidores);
        String[] ultimo = datos.obtenerUltimo(idCanal);
        String mensaje = "Se registró el conteo de " + seguidores + " seguidores para la red social " + idCanal + ".";
        String html = Html.pagina(mensaje,
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(ultimo)));
        return Respuesta.exito(mensaje, html);
    }
}
