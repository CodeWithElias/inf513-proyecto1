# Sistema vía correo - Grupo 13 SA

Java 17 puro (sin frameworks). POP3 y SMTP implementados con `Socket`; base de datos con JDBC (`lib/postgresql-42.7.4.jar`).

## Puesta en marcha
1. Las tablas de `sql/schema.sql` ya están creadas en `db_grupo13sa`.
2. Compilar: `compilar.bat` (Windows) o `sh compilar.sh` (Linux)
3. Ejecutar: `ejecutar.bat` → revisa `grupo13sa@tecnoweb.org.bo` cada 10 s y responde.
4. Probar un comando sin correo: `ejecutar.bat --probar "lisUsu[\"*\"]"`

## Comandos (en el asunto)
Envíe `help` para recibir la lista completa con ejemplos.
Nomenclatura: 3 letras de la acción + 3 de la entidad en camelCase (`lisUsu`). No distingue mayúsculas: `LISUSU` y `lisusu` también funcionan.

| Comando | Parámetros | Ejemplo |
|---|---|---|
| help | — | `help` |
| lisUsu | filtro (`*` = todos) | `lisUsu["*"]` |
| mosUsu | id_usuario | `mosUsu["1"]` |
| insUsu | nombre_completo, email, telefono, password, id_rol, id_unidad (`""` = sin unidad) | `insUsu["Juan Carlos Perez","juanperez@uagrm.edu.bo","71055123","clave123","1",""]` |
| modUsu | id_usuario, nombre_completo, email, telefono, password (`""` = no cambia), id_rol, id_unidad | `modUsu["1","Juan Carlos Perez","jperez@uagrm.edu.bo","71055123","","1",""]` |
| eliUsu | id_usuario (baja lógica: activo = false) | `eliUsu["1"]` |
| actUsu | id_usuario (reactivar) | `actUsu["1"]` |
| lisRol | filtro | `lisRol["*"]` |
| insRol | nombre, descripcion | `insRol["Administrador","Acceso total"]` |
| lisUni | filtro | `lisUni["*"]` |

Las contraseñas se guardan con PBKDF2-SHA256 + sal (`util/Seguridad.java`), nunca en texto plano.

## Estructura
```
src/grupo13/
  App.java              bucle principal: POP3 -> procesar -> SMTP
  config/Config.java    lee config.properties
  mail/                 ClientePOP3, ClienteSMTP, MimeUtil (asuntos con tildes)
  comandos/             Comando (parser), Despachador (registro + validación), Respuesta
  negocio/              NUsuario, NRol, NUnidad, Validar (reglas y validaciones)
  datos/                DUsuario, DRol, DUnidad (SQL con PreparedStatement)
  util/Html             cuerpo HTML de las respuestas
```

## Agregar un caso de uso (ej. Producto)
1. `sql/schema.sql`: crear la tabla.
2. `datos/DProducto.java`: listar/insertar/modificar/eliminar (copiar DUsuario).
3. `negocio/NProducto.java`: validaciones; cada método recibe `List<String>` y devuelve `Respuesta`.
4. `comandos/Despachador.java`: `grupo("CU: Gestionar Productos");` y `registrar("lisPro", "...", new String[]{"filtro"}, "lisPro[\"*\"]", producto::listar);`

El despachador ya valida que el comando exista y que la cantidad de parámetros sea la correcta.
