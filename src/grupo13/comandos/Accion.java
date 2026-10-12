package grupo13.comandos;

import java.util.List;

/** Lógica de un caso de uso. Los parámetros ya llegan con la cantidad correcta. */
@FunctionalInterface
public interface Accion {

    Respuesta ejecutar(List<String> parametros) throws Exception;
}
