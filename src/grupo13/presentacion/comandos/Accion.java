package grupo13.presentacion.comandos;

import grupo13.negocio.NegocioException;

import java.util.List;

/** Método de una clase P* que atiende un comando. Los parámetros ya llegan con la cantidad correcta. */
@FunctionalInterface
public interface Accion {

    Respuesta ejecutar(List<String> parametros) throws NegocioException;
}
