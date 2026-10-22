package grupo13.presentacion;

import grupo13.negocio.NSeguidoresRed;
import grupo13.negocio.NegocioException;
import grupo13.presentacion.comandos.Respuesta;

import java.util.List;

/** Presentación del CU2 – Seguidores: insSeg, lisSeg. */
public class PSeguidoresRed {

    private static final String[] COLUMNAS = {"ID", "Red Social", "Fecha de Medición", "Seguidores"};

    private final NSeguidoresRed negocio = new NSeguidoresRed();

    /** lisSeg["id_canal"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        String titulo = "Historial de Seguidores - Red Social " + p.get(0);
        return Vista.listado(titulo, titulo, COLUMNAS, negocio.listar(p.get(0)));
    }

    /** insSeg["id_canal","seguidores"] */
    public Respuesta insertar(List<String> p) throws NegocioException {
        String[] registro = negocio.insertar(p.get(0), p.get(1));
        return Vista.exito("Se registró el conteo de " + p.get(1) + " seguidores para la red social " + p.get(0) + ".",
                COLUMNAS, registro);
    }
}
