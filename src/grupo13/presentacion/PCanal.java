package grupo13.presentacion;

import grupo13.negocio.NCanal;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU5 – Canales de difusión: lisCan, insCan, eliCan, actCan. */
public class PCanal {

    private static final String[] COLUMNAS = {"ID", "Tipo", "Nombre", "Activo", "Fecha alta", "Fecha baja",
            "Espacios disponibles"};

    private final NCanal negocio = new NCanal();

    /** lisCan["*"] (incluye las redes sociales del CU2) */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando los Canales", "Canales de Difusión", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }

    /** insCan["tipo","nombre"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        int id = negocio.insertar(p.get(0), p.get(1));
        return Vista.exito("El canal fue registrado con ID " + id + ". Cree sus espacios con insEsp o genEsp.", COLUMNAS,
                negocio.obtener(String.valueOf(id)));
    }

    /** eliCan["id"] */
    public Respuesta eliminar(List<String> p) throws NegocioException {
        negocio.eliminar(p.get(0));
        return Vista.exito("El canal " + p.get(0) + " fue dado de baja. Puede reactivarlo con actCan[\"" + p.get(0) + "\"].",
                COLUMNAS, negocio.obtener(p.get(0)));
    }

    /** actCan["id"] */
    public Respuesta activar(List<String> p) throws NegocioException {
        negocio.activar(p.get(0));
        return Vista.exito("El canal " + p.get(0) + " fue reactivado.", COLUMNAS, negocio.obtener(p.get(0)));
    }
}
