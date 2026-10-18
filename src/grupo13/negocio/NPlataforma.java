package grupo13.negocio;

import grupo13.comandos.ComandoException;
import grupo13.comandos.Respuesta;
import grupo13.datos.DPlataforma;
import grupo13.util.Html;

import java.sql.SQLException;
import java.util.List;

/**
 * CU2 / RF03: gestión del catálogo de plataformas de redes sociales (Facebook, TikTok, etc.).
 */
public class NPlataforma {

    private static final String[] TITULOS = {"ID", "Plataforma", "Redes activas asociadas"};

    private final DPlataforma datos = new DPlataforma();

    /** lisPla["*"] o lisPla["texto"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Plataformas",
                Html.pagina("Listando las Plataformas", Html.tabla(TITULOS, datos.listar(filtro))));
    }

    /** insPla["nombre"] */
    public Respuesta insertar(List<String> p) throws Exception {
        String nombre = Validar.texto(p.get(0), "nombre", 100);
        if (datos.nombreEnUso(nombre, 0)) {
            throw new ComandoException("Ya existe una plataforma llamada '" + Html.esc(nombre) + "'.");
        }
        int id = datos.insertar(nombre);
        return exito("La plataforma fue registrada correctamente con ID " + id + ".", id);
    }

    private String[] buscar(int id) throws Exception {
        String[] f = datos.obtener(id);
        if (f == null) {
            throw new ComandoException("No existe una plataforma con ID " + id + ".");
        }
        return f;
    }

    private Respuesta exito(String mensaje, int id) throws Exception {
        String html = Html.pagina(mensaje,
                "<p>" + Html.esc(mensaje) + "</p>" + Html.tabla(TITULOS, List.<String[]>of(buscar(id))));
        return Respuesta.exito(mensaje, html);
    }
}
