# Sistema de Gestión Publicitaria vía Correo Electrónico — Grupo 13 SA

Proyecto de la materia **Tecnología Web (INF513)** — UAGRM.
Sistema para **Canal 11 / UAGRM** que gestiona usuarios, redes sociales institucionales, oferta comercial
(tarifas, paquetes, proformas), material publicitario, programación de pautas, monitoreo de páginas no
autorizadas y métricas de impacto.

**La particularidad:** no hay interfaz web ni de escritorio. El cliente envía un **correo electrónico** cuyo
**asunto** es un comando (por ejemplo `lisUsu["*"]`). El sistema lee el buzón, ejecuta el comando contra
PostgreSQL y **responde por correo** con el resultado en HTML ("Éxito", un listado o "Error" con el motivo).

> Este documento es la referencia del proyecto para los integrantes del grupo y para agentes de IA.
> Antes de escribir código, lea al menos las secciones **4 (Arquitectura)**, **7 (Convenciones)** y **8 (Cómo agregar un comando)**.

---

## Índice
1. [Restricciones del proyecto](#1-restricciones-del-proyecto)
2. [Herramientas y servidores](#2-herramientas-y-servidores)
3. [Casos de uso y estado de avance](#3-casos-de-uso-y-estado-de-avance)
4. [Arquitectura](#4-arquitectura)
5. [Flujo de funcionamiento](#5-flujo-de-funcionamiento)
6. [Protocolo de comandos](#6-protocolo-de-comandos)
7. [Convenciones de código](#7-convenciones-de-código)
8. [Cómo agregar un comando (paso a paso)](#8-cómo-agregar-un-comando-paso-a-paso)
9. [Modelo de datos](#9-modelo-de-datos)
10. [Hoja de ruta: comandos por caso de uso](#10-hoja-de-ruta-comandos-por-caso-de-uso)
11. [Compilar, ejecutar y probar](#11-compilar-ejecutar-y-probar)
12. [Forma de trabajo en equipo (Git)](#12-forma-de-trabajo-en-equipo-git)
13. [Limitaciones y problemas conocidos](#13-limitaciones-y-problemas-conocidos)
14. [Reglas para agentes de IA](#14-reglas-para-agentes-de-ia)

---

## 1. Restricciones del proyecto

| Restricción | Consecuencia práctica |
|---|---|
| **Java sin frameworks** | Nada de Spring, Jakarta Mail, Hibernate, Maven/Gradle obligatorios, etc. Solo el JDK. |
| Única librería externa: **driver JDBC de PostgreSQL** | `lib/postgresql-42.7.4.jar`. Es un driver, no un framework. |
| **Correo implementado a mano** | POP3 y SMTP se hablan directamente con `java.net.Socket` (RFC 1939 / RFC 5321). |
| **Interfaz = asunto del correo** | Toda la funcionalidad se expone como comandos `nombre["p1","p2",...]`. |
| **Respuesta = correo HTML** | Tablas y mensajes se generan como HTML (`util/Html.java`). |
| Servidores de la facultad | Correo, base de datos y web viven en `tecnoweb.org.bo` (ver sección 2). |

---

## 2. Herramientas y servidores

### Herramientas
| Herramienta | Versión / detalle | Uso |
|---|---|---|
| JDK | **17** (Temurin) | Compilar y ejecutar (se usan APIs de Java 11+ como `List.of` y `String.isBlank`). |
| PostgreSQL JDBC | 42.7.4 (`lib/`) | Acceso a la base de datos. |
| PostgreSQL | servidor de la facultad | Base de datos `db_grupo13sa`. |
| Dovecot (POP3) / SMTP | servidor de la facultad | Buzón `grupo13sa@tecnoweb.org.bo`. |
| Git | — | Control de versiones (ver sección 12). |
| Editor | VS Code (recomendado) con *Extension Pack for Java* | Opcional; se puede compilar con `javac` desde la terminal. |
| psql / pgAdmin | — | Administrar la base de datos (las tablas ya están creadas). |

### Servidores (Grupo 13 SA)
| Servicio | Host | Puerto | Credencial |
|---|---|---|---|
| POP3 (leer correos) | `mail.tecnoweb.org.bo` | 110 | usuario `grupo13sa` |
| SMTP (enviar respuestas) | `mail.tecnoweb.org.bo` | 25 | sin autenticación, remitente `grupo13sa@tecnoweb.org.bo` |
| PostgreSQL | `db.tecnoweb.org.bo` | 5432 | usuario `grupo13sa`, BD `db_grupo13sa` |
| Web (no se usa aún) | `https://www.tecnoweb.org.bo/inf513/grupo13sa` | — | — |

La contraseña es la del grupo y vive **solo** en `config.properties` (ignorado por Git, ver sección 11).

---

## 3. Casos de uso y estado de avance

### Requisitos funcionales → casos de uso
| CU | Nombre | Requisitos | Tablas principales | Estado |
|---|---|---|---|---|
| **CU1** | Gestión de Usuarios (Director, Encargado de Unidad Universitaria, Cliente) | RF01, RF02 | `usuario`, `rol`, `unidad_universitaria`, `representante` | ✅ **Completo**: usuarios, roles, unidades (encargados) y clientes (representantes). Pendiente opcional: permisos por rol (ver nota RF02) |
| **CU2** | Gestión de Cuentas de Redes Sociales Institucionales | RF03 | `canal_difusion`, `red_social`, `plataforma`, `seguidores_red` | ⚪ Pendiente |
| **CU3** | Gestión de Oferta Comercial, Clientes y Generación de Cotizaciones | RF04, RF05, RF06, RF07 | `empresa_anunciante`, `representante`, `visitante`, `servicio_publicitario`, `tarifa`, `paquete`, `paquete_item`, `proforma`, `detalle_proforma`, `campania` | ⚪ Pendiente |
| **CU4** | Gestión de Cargas, Validar Material Publicitario | RF08, RF09 | `material_publicitario`, `revision_material` | ⚪ Pendiente |
| **CU5** | Gestión de Programar Pauta Multi-Canal | RF10, RF11 | `espacio_publicitario`, `programacion_pauta`, `notificacion` | ⚪ Pendiente |
| **CU6** | Gestión de Monitoreo y Detectar Páginas No Autorizadas mediante Scraping | RF12 | `termino_monitoreo`, `ejecucion_scraping`, `pagina_no_autorizada`, `deteccion_pagina`, `notificacion` | ⚪ Pendiente |
| **CU7** | Gestión de Análisis Métricas de Impacto | RF13 | `metrica_publicacion`, `seguidores_red` | ⚪ Pendiente |
| **CU8** | Reportes y Estadísticas | RF13 | consultas agregadas sobre todas las tablas | ⚪ Pendiente |

### Resumen de los requisitos
- **RF01** Registrar, consultar, modificar, activar e inactivar usuarios (incluye encargados de unidades).
- **RF02** Roles y privilegios por perfil (Administrador de Canal 11, Empresa/Cliente anunciante, Encargado de Unidad…).
  *Nota:* el esquema actual tiene la tabla `rol` pero **no** una tabla de permisos; si se necesitan privilegios finos hay que acordar el cambio de esquema con el grupo.
- **RF03** Registro, actualización y baja de redes sociales oficiales por unidad, con un responsable de contenido.
- **RF04** Registro de empresas anunciantes (razón social, facturación, representante legal/comercial).
- **RF05** Tarifario: registrar, actualizar, consultar y deshabilitar tarifas por servicio (spot TV, mención, cintillo, redes…).
- **RF06** Paquetes que agrupan varias tarifas/servicios.
- **RF07** Proformas/cotizaciones calculadas automáticamente para clientes y visitantes.
- **RF08** Carga de material publicitario (video, audio, arte, banner, texto) asociado a una campaña.
- **RF09** Revisión del material: aprobar, rechazar o solicitar corrección.
- **RF10** Programación de pauta en calendario, validando disponibilidad de espacios.
- **RF11** Notificación automática (correo) al encargado de la unidad cuando se programa una pauta en sus redes.
- **RF12** Scraping periódico para detectar páginas que usan indebidamente la imagen de UAGRM/Canal 11.
- **RF13** Reportes de impacto comercial y alcance de publicaciones.

---

## 4. Arquitectura

### Vista general
```
 ┌────────────┐   correo (asunto = comando)   ┌───────────────────────────┐
 │  Cliente   │ ─────────────────────────────▶│ grupo13sa@tecnoweb.org.bo │
 │ (Gmail...) │ ◀───────────────────────────── │   (servidor de correo)    │
 └────────────┘   respuesta HTML               └───────────┬───────────────┘
                                                POP3 :110  │  ▲ SMTP :25
                                                           ▼  │
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                         App (bucle cada N segundos)                      │
 │                                                                          │
 │  mail/ClientePOP3 ──▶ comandos/Despachador ──▶ negocio/N*  ──▶ datos/D*  │──▶ PostgreSQL
 │        │                 │  Comando.parsear      (valida)     (SQL)      │   db_grupo13sa
 │        │                 ▼                                               │
 │        │            Respuesta (titulo + HTML con util/Html)              │
 │        └──────────────────────────────────▶ mail/ClienteSMTP ────────────│
 └─────────────────────────────────────────────────────────────────────────┘
```

### Capas
El proyecto sigue una **arquitectura en capas** (similar a la de 3 capas que se usa en la materia):

| Capa | Paquete | Responsabilidad | No debe… |
|---|---|---|---|
| **Presentación / transporte** | `mail`, `App` | Leer correos (POP3), enviar respuestas (SMTP), bucle principal. | conocer SQL ni reglas de negocio. |
| **Comandos** | `comandos` | Interpretar el asunto, verificar que el comando exista y que el número de parámetros sea correcto, capturar errores y convertirlos en respuesta "Error". | validar datos de negocio. |
| **Negocio** | `negocio` (`N*`) | Validar parámetros (tipos, longitudes, existencia de FKs, unicidad), aplicar reglas (baja lógica, hash de contraseña) y armar la `Respuesta`. | escribir SQL. |
| **Datos** | `datos` (`D*`) | SQL con `PreparedStatement`. Una clase por tabla principal. | lanzar mensajes para el usuario ni generar HTML. |
| **Utilidades** | `util`, `config` | HTML, seguridad (hash), configuración. | — |

### Estructura de carpetas
```
proyecto-mail/
├── README.md
├── config.properties          ← credenciales (NO se sube a Git)
├── lib/postgresql-42.7.4.jar  ← único JAR externo
├── sql/schema.sql             ← esquema completo (ya aplicado en db_grupo13sa)
├── out/                       ← .class compilados (generado)
└── src/grupo13/
    ├── App.java               ← main: bucle POP3 → procesar → SMTP; modo --probar
    ├── config/Config.java     ← lee config.properties (con valores por defecto)
    ├── mail/
    │   ├── ClientePOP3.java   ← USER/PASS/STAT/RETR/DELE/QUIT; extrae From y Subject
    │   ├── ClienteSMTP.java   ← HELO/MAIL FROM/RCPT TO/DATA; cuerpo HTML en Base64
    │   ├── Correo.java        ← remitente + asunto
    │   └── MimeUtil.java      ← decodifica asuntos =?UTF-8?B?...?=, extrae email del From
    ├── comandos/
    │   ├── Comando.java       ← parser del asunto: nombre + lista de parámetros
    │   ├── Despachador.java   ← REGISTRO DE TODOS LOS COMANDOS + help + manejo de errores
    │   ├── Accion.java        ← interfaz funcional: Respuesta ejecutar(List<String>)
    │   ├── Respuesta.java     ← exito/error + título + HTML
    │   └── ComandoException   ← error de validación con mensaje para el usuario
    ├── negocio/
    │   ├── NUsuario.java      ← CU1 usuarios: listar, mostrar, insertar, modificar, eliminar, activar
    │   ├── NRol.java          ← CU1 roles: CRUD + baja/alta lógica
    │   ├── NUnidad.java       ← CU1 unidades: CRUD jerárquico (evita ciclos) + baja/alta lógica
    │   ├── NRepresentante.java← CU1 clientes: perfil de representante de un usuario
    │   ├── NEmpresa.java      ← consulta de empresas (apoyo; el CRUD es del CU3)
    │   └── Validar.java       ← validaciones reutilizables (id, texto, email, teléfono, ci, password)
    ├── datos/
    │   ├── Conexion.java      ← abre conexión JDBC; leerFilas(ResultSet)
    │   ├── DUsuario.java
    │   ├── DRol.java
    │   ├── DUnidad.java       ← incluye consulta recursiva (WITH RECURSIVE) para la jerarquía
    │   ├── DRepresentante.java
    │   └── DEmpresa.java
    └── util/
        ├── Html.java          ← pagina(), tabla(), esc() (escapar HTML)
        └── Seguridad.java     ← hash de contraseñas PBKDF2-SHA256 con sal
```

---

## 5. Flujo de funcionamiento

### Ciclo principal (`App.main`)
1. Lee `app.intervalo` (por defecto 10 s) e inicia un bucle infinito.
2. En cada vuelta llama a `revisarBuzon()`:
   1. Abre `ClientePOP3`, hace `USER` / `PASS` y `STAT` para saber cuántos correos hay.
   2. Para cada mensaje `i`:
      - `RETR i` → descarga el mensaje, une cabeceras partidas en varias líneas y obtiene **From** (solo el email) y **Subject** (decodificado si viene en `=?UTF-8?B?...?=`).
      - `procesar(correo)`:
        - Si el remitente es el propio sistema, `mailer-daemon` o `postmaster` → se **ignora** (evita bucles de respuestas).
        - `Despachador.procesar(asunto)` devuelve una `Respuesta` (nunca lanza excepción).
        - `ClienteSMTP.enviar(remitente, titulo + " - " + asunto, html)`.
      - `DELE i` → marca el correo para borrar.
   3. `QUIT` → el servidor elimina definitivamente los correos marcados.
3. Si falla la conexión, se registra en consola y se reintenta en la siguiente vuelta.

### Procesamiento de un comando (`Despachador.procesar`)
```
asunto "insUsu[\"Ana\",\"ana@x.com\",\"\",\"clave123\",\"1\",\"\"]"
   │
   ▼ Comando.parsear ─────────── formato inválido ──────────────▶ Respuesta "Error"
   │   nombre = insUsu, params = [Ana, ana@x.com, "", clave123, 1, ""]
   ▼ buscar en el registro (sin distinguir mayúsculas) ─ no existe ─▶ "Error: el comando no existe"
   ▼ ¿cantidad de parámetros correcta? ─────────── no ──────────────▶ "Error: requiere N parámetros + ejemplo"
   ▼ NUsuario.insertar(params)
   │   Validar.* (email válido, password ≥ 6, id_rol numérico...) ─ falla ─▶ ComandoException → "Error"
   │   reglas: email único, rol existe y activo, unidad existe...      ─ falla ─▶ "Error"
   ▼ DUsuario.insertar(...)  ──────────── SQLException ────────────▶ "Error en la base de datos: ..."
   ▼
 Respuesta.exito("Éxito", HTML con el registro creado)
```

### Ejemplo de conversación
```
De:      juan@gmail.com
Para:    grupo13sa@tecnoweb.org.bo
Asunto:  lisUsu["*"]

           ⟶ (≈10 s después)

De:      grupo13sa@tecnoweb.org.bo
Para:    juan@gmail.com
Asunto:  Listando los Usuarios - lisUsu["*"]
Cuerpo:  tabla HTML con ID, nombre, email, teléfono, rol, unidad, activo, fechas
```

---

## 6. Protocolo de comandos

### Sintaxis
```
nombreComando["parametro1","parametro2",...]
```
- Cada parámetro va **entre comillas dobles** y se separan con **comas**. Se permiten espacios alrededor.
- Las comas dentro de comillas son parte del valor: `"Perez, Juan"` es un solo parámetro.
- Parámetro vacío: `""` (se usa para campos opcionales, p. ej. `id_unidad` o "no cambiar la contraseña").
- Comillas tipográficas `“ ”` (las que ponen algunos celulares) se convierten a `"`.
- Comandos sin parámetros pueden ir sin corchetes: `help`.
- Filtro de listados: `"*"` = todos; cualquier otro texto = búsqueda parcial (`ILIKE '%texto%'`).

### Nomenclatura de comandos
**3 letras de la acción + 3 letras de la entidad, en camelCase**: `lisUsu`, `insRol`, `modTar`.
La búsqueda **no distingue mayúsculas** (`LISUSU`, `lisusu` también funcionan), pero en código, ayuda y
documentación se escribe siempre en camelCase.

| Acción | Significado |
|---|---|
| `lis` | listar / buscar (`"*"` o texto) |
| `mos` | mostrar un registro por ID |
| `ins` | insertar / registrar |
| `mod` | modificar |
| `eli` | eliminar — **baja lógica** cuando la tabla tiene `activo` o es referenciada por otras |
| `act` | reactivar (deshacer una baja lógica) |
| otras | se pueden crear verbos específicos de 3 letras: `apr` aprobar, `rec` rechazar, `gen` generar, `eje` ejecutar, `rep` reporte… (ver sección 10) |

### Respuestas
| Caso | Asunto de la respuesta | Cuerpo |
|---|---|---|
| Listado | `Listando los <Entidad> - <asunto original>` | Tabla HTML + total de registros |
| Operación correcta | `Éxito - <asunto original>` | Mensaje + tabla con el registro afectado |
| Cualquier error | `Error - <asunto original>` | Motivo concreto + sugerencia de enviar `help` |
| `help` | `Comandos disponibles - help` | Una tabla por caso de uso con parámetros y ejemplo |

### Comandos implementados
| Comando | Parámetros | Ejemplo |
|---|---|---|
| `help` | — | `help` |
| **Usuarios** | | |
| `lisUsu` | filtro (`*` = todos; busca por nombre, email, rol o unidad) | `lisUsu["*"]` |
| `mosUsu` | id_usuario | `mosUsu["1"]` |
| `insUsu` | nombre_completo, email, telefono, password, id_rol, id_unidad (`""` = sin unidad) | `insUsu["Juan Carlos Perez","juanperez@uagrm.edu.bo","71055123","clave123","1",""]` |
| `modUsu` | id_usuario, nombre_completo, email, telefono, password (`""` = no cambia), id_rol, id_unidad | `modUsu["1","Juan Carlos Perez","jperez@uagrm.edu.bo","71055123","","1",""]` |
| `eliUsu` | id_usuario — baja lógica (`activo = false`) | `eliUsu["1"]` |
| `actUsu` | id_usuario — reactiva | `actUsu["1"]` |
| **Roles** (Director, Encargado de Unidad, Cliente…) | | |
| `lisRol` | filtro (muestra cuántos usuarios activos tiene cada rol) | `lisRol["*"]` |
| `mosRol` | id_rol | `mosRol["1"]` |
| `insRol` | nombre, descripcion | `insRol["Director","Director de Canal 11"]` |
| `modRol` | id_rol, nombre, descripcion | `modRol["2","Director","Dirección general de Canal 11"]` |
| `eliRol` | id_rol — baja lógica; **rechazada si el rol tiene usuarios activos** | `eliRol["2"]` |
| `actRol` | id_rol — reactiva | `actRol["2"]` |
| **Unidades universitarias** (a las que pertenecen los Encargados de Unidad) | | |
| `lisUni` | filtro (nombre, tipo o unidad padre; muestra los encargados activos) | `lisUni["*"]` |
| `mosUni` | id_unidad — la unidad y sus subunidades directas | `mosUni["1"]` |
| `insUni` | nombre, tipo, id_unidad_padre (`""` = unidad raíz) | `insUni["Facultad de Ciencias de la Computación","Facultad",""]` |
| `modUni` | id_unidad, nombre, tipo, id_unidad_padre — **rechaza ciclos** (ser padre de sí misma o de un ancestro) | `modUni["2","Ingeniería Informática","Carrera","1"]` |
| `eliUni` | id_unidad — baja lógica; **rechazada con usuarios o subunidades activas** | `eliUni["2"]` |
| `actUni` | id_unidad — reactiva; **rechazada si su unidad padre está inactiva** | `actUni["2"]` |
| **Clientes** (usuario + perfil `representante` de una empresa) | | |
| `lisRep` | filtro (nombre, email, CI, razón social o NIT) | `lisRep["*"]` |
| `mosRep` | id_usuario | `mosRep["3"]` |
| `insRep` | id_usuario, ci, cargo, id_empresa — convierte un usuario **activo** existente en cliente | `insRep["3","4715292","Gerente comercial","1"]` |
| `modRep` | id_usuario, ci, cargo, id_empresa | `modRep["3","4715292","Representante legal","1"]` |
| `eliRep` | id_usuario — quita el perfil de cliente (borrado físico de `representante`; el usuario se mantiene) | `eliRep["3"]` |
| `lisEmp` | filtro — empresas anunciantes (solo consulta, apoyo para `insRep`) | `lisEmp["*"]` |

**Flujo típico CU1:**
1. `insRol["Director",""]`, `insRol["Encargado de Unidad",""]`, `insRol["Cliente",""]` → crear los perfiles.
2. `insUni["Facultad de Ciencias de la Computación","Facultad",""]` → `insUni["Ingeniería Informática","Carrera","1"]`.
3. **Director:** `insUsu[..., id_rol_director, ""]`.
4. **Encargado de Unidad:** `insUsu[..., id_rol_encargado, id_unidad]`.
5. **Cliente:** `insUsu[..., id_rol_cliente, ""]` y luego `insRep[id_usuario, ci, cargo, id_empresa]` (la empresa debe existir; su registro es parte del CU3).

---

## 7. Convenciones de código

### Nombres
| Elemento | Convención | Ejemplo |
|---|---|---|
| Paquete raíz | `grupo13` | `grupo13.negocio` |
| Clase de datos | `D` + Entidad | `DUsuario`, `DTarifa` |
| Clase de negocio | `N` + Entidad | `NUsuario`, `NTarifa` |
| Métodos de negocio | verbo en español | `listar`, `mostrar`, `insertar`, `modificar`, `eliminar`, `activar` |
| Comandos | acción(3) + Entidad(3) camelCase | `lisTar`, `insPaq` |
| Idioma | **español** en clases, métodos, variables, mensajes y comentarios | — |

### Reglas
1. **Siempre `PreparedStatement`** con `?`. Nunca concatenar valores del usuario en SQL.
2. **Nunca devolver `password_hash`** ni datos sensibles en una respuesta.
3. **Todo texto que vaya al HTML pasa por `Html.esc()`** (los parámetros vienen de cualquier remitente).
4. Errores para el usuario → `throw new ComandoException("mensaje claro en español")`. El `Despachador` lo convierte en respuesta "Error".
5. Validaciones de formato con `negocio/Validar` (`id`, `idOpcional`, `texto`, `textoOpcional`, `email`, `telefono`, `password`). Si falta una validación genérica, se agrega ahí.
6. Antes de insertar/modificar, **verificar en negocio** unicidad y existencia de claves foráneas, para dar un mensaje claro en vez de un error SQL.
7. **Baja lógica** (`activo = false`) cuando la tabla tiene columna `activo` y/o es referenciada por otras tablas; borrado físico solo en tablas "hoja" o detalle.
8. Contraseñas → `Seguridad.hashPassword()` (PBKDF2-SHA256, 120 000 iteraciones, sal aleatoria). Verificar con `Seguridad.verificarPassword()`.
9. Fechas en respuestas formateadas en SQL: `to_char(fecha, 'DD/MM/YYYY HH24:MI')`.
10. Las clases `D*` abren y cierran su conexión con *try-with-resources* (`Conexion.abrir()`); no se comparten conexiones.
11. Mismo estilo que el código existente: llaves en la misma línea, 4 espacios, comentarios `/** */` breves por método público.
12. **Sin dependencias nuevas.** Si algo parece necesitar una librería (JSON, HTTP, scraping), usar lo que trae el JDK (`java.net.http.HttpClient`, `java.util.regex`, etc.).

---

## 8. Cómo agregar un comando (paso a paso)

Ejemplo: **CU3 – Tarifas** (`lisTar`, `insTar`).

**1. Datos** — `src/grupo13/datos/DTarifa.java`
```java
public class DTarifa {

    public List<String[]> listar(String filtro) throws SQLException {
        String sql = "SELECT t.id_tarifa, s.nombre, t.monto, t.unidad_cobro,"
                + " to_char(t.vigente_desde, 'DD/MM/YYYY'), to_char(t.vigente_hasta, 'DD/MM/YYYY'),"
                + " CASE WHEN t.activo THEN 'Sí' ELSE 'No' END"
                + " FROM tarifa t JOIN servicio_publicitario s ON s.id_servicio = t.id_servicio"
                + (filtro == null ? "" : " WHERE s.nombre ILIKE ?")
                + " ORDER BY t.id_tarifa";
        try (Connection c = Conexion.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtro != null) {
                ps.setString(1, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                return Conexion.leerFilas(rs);
            }
        }
    }
}
```

**2. Negocio** — `src/grupo13/negocio/NTarifa.java`
```java
public class NTarifa {

    private static final String[] TITULOS = {"ID", "Servicio", "Monto", "Unidad cobro", "Desde", "Hasta", "Activo"};
    private final DTarifa datos = new DTarifa();

    /** lisTar["*"] */
    public Respuesta listar(List<String> p) throws SQLException {
        String filtro = p.get(0).equals("*") ? null : p.get(0);
        return Respuesta.exito("Listando las Tarifas",
                Html.pagina("Listando las Tarifas", Html.tabla(TITULOS, datos.listar(filtro))));
    }
}
```

**3. Registro** — en el constructor de `comandos/Despachador.java`
```java
NTarifa tarifa = new NTarifa();

grupo("CU3: Oferta Comercial, Clientes y Cotizaciones");
registrar("lisTar", "Lista las tarifas (\"*\" = todas)",
        new String[]{"filtro"}, "lisTar[\"*\"]", tarifa::listar);
```
`registrar(nombre, descripción, nombresDeParámetros, ejemplo, método)`. Con esto el comando ya:
- aparece en `help` dentro de su grupo,
- valida la cantidad de parámetros y muestra el ejemplo si está mal,
- convierte cualquier excepción en una respuesta "Error".

**4. Probar sin correo**
```
java -cp "out;lib/*" grupo13.App --probar "lisTar[\"*\"]"
```
Probar el caso correcto **y** los casos de error (parámetros de menos, ID inexistente, FK inexistente, duplicado).

**5. Documentar** — agregar el comando a la tabla de la sección 6 y actualizar el estado en la sección 3.

---

## 9. Modelo de datos

Script completo: [`sql/schema.sql`](sql/schema.sql) — **ya está aplicado** en `db_grupo13sa`; no volver a ejecutarlo.
Todas las PK son `INTEGER GENERATED BY DEFAULT AS IDENTITY`.

| Bloque | Tablas | Relaciones clave |
|---|---|---|
| Catálogos | `rol`, `unidad_universitaria` (reflexiva: `id_unidad_padre`), `plataforma`, `servicio_publicitario`, `termino_monitoreo` | — |
| Usuarios | `usuario` → `rol` (1), `unidad_universitaria` (0..1); `representante` **ES UN** `usuario` y pertenece a `empresa_anunciante` | `representante.id_usuario` = PK y FK |
| Clientes | `empresa_anunciante`, `visitante` | una proforma la realiza una empresa **o** un visitante |
| Oferta | `tarifa` → `servicio_publicitario`, `unidad_universitaria`; `paquete` ↔ `tarifa` vía `paquete_item` (cantidad) | — |
| Cotización | `proforma` 1 → N `detalle_proforma` (tarifa **o** paquete); `proforma` 1 → 0..1 `campania` | `campania.id_proforma` UNIQUE |
| Material | `material_publicitario` → `campania`, `usuario`; 1 → N `revision_material` | `estado` default `'pendiente'`, `version` |
| Canales | `canal_difusion`; `red_social` **ES UN** `canal_difusion` → `plataforma`, `usuario` (responsable), `unidad`; `seguidores_red`; `espacio_publicitario` → `canal_difusion` | `red_social.id_canal` = PK y FK |
| Pautas | `programacion_pauta` → `detalle_proforma`, `material_publicitario`, `espacio_publicitario` (UNIQUE), `usuario`; 1 → N `metrica_publicacion` | un espacio solo puede usarse una vez |
| Monitoreo | `ejecucion_scraping` → `plataforma`; 1 → N `deteccion_pagina` → `pagina_no_autorizada`, `termino_monitoreo` | — |
| Notificaciones | `notificacion` → `usuario`, `deteccion_pagina` (0..1), `programacion_pauta` (0..1) | — |

**Importante para las bajas:** `usuario` es referenciado por `material_publicitario`, `revision_material`,
`red_social`, `programacion_pauta` sin `ON DELETE CASCADE`, por eso `eliUsu` es baja lógica.
Las generalizaciones (`representante`, `red_social`) se insertan en **dos pasos dentro de una transacción**
(primero el padre, luego el hijo con el mismo ID).

---

## 10. Hoja de ruta: comandos por caso de uso

Propuesta de comandos **pendientes** siguiendo la nomenclatura. Antes de implementar un CU, confirmar con el grupo
los parámetros finales; al terminarlo, moverlo a la tabla de la sección 6.

| CU | Comandos propuestos | Notas |
|---|---|---|
| **CU1** ✅ | Implementado (ver sección 6). Opcional: control de permisos por rol según el remitente del correo. | Requiere decidir si se agrega una tabla de permisos al esquema. |
| **CU2** | `lisPla`, `insPla` · `lisRed`, `mosRed`, `insRed`, `modRed`, `eliRed`, `actRed` · `insSeg`, `lisSeg` | `insRed` crea `canal_difusion` + `red_social`; baja = `activo=false` + `fecha_baja`. Responsable = `id_usuario`. |
| **CU3** | `insEmp`, `modEmp`, `eliEmp`, `actEmp` (`lisEmp` ya existe) · `lisVis`, `insVis` · `lisSer`, `insSer`, `modSer`, `eliSer` · `lisTar`, `insTar`, `modTar`, `eliTar` · `lisPaq`, `insPaq`, `modPaq`, `eliPaq`, `addPaq` (agregar tarifa) · `insPro`, `addDet` (agregar ítem), `mosPro`, `lisPro`, `genCam` (generar campaña desde proforma) | Totales de proforma se **calculan** en negocio: subtotal = Σ detalle; total = subtotal − descuento. `mosPro` es la cotización "descargable" (HTML en el correo). |
| **CU4** | `insMat`, `lisMat`, `mosMat` · `aprMat`, `recMat`, `corMat` (aprobar / rechazar / pedir corrección) · `lisRev` | El archivo se referencia por URL (`url_archivo`); cada revisión inserta en `revision_material` y cambia `estado`. Opcional: leer adjuntos del correo. |
| **CU5** | `lisEsp`, `insEsp`, `dispEsp` (disponibles por fecha/canal) · `insPau`, `lisPau`, `modPau`, `eliPau` · `lisNot` | Validar que el espacio esté `disponible` y el material `aprobado`. Al programar en una red social: insertar `notificacion` y **enviar correo** al encargado (`ClienteSMTP`). |
| **CU6** | `lisTer`, `insTer`, `eliTer` · `ejeScr` (ejecutar scraping ahora) · `lisPag`, `mosPag`, `modPag` (cambiar estado) · `lisDet` | Scraping con `java.net.http.HttpClient` + regex (sin librerías). Ejecución periódica: agregar una tarea en el bucle de `App` (p. ej. cada N vueltas). Registrar cada corrida en `ejecucion_scraping`. |
| **CU7** | `insMet`, `lisMet` (por programación) · `resMet` (resumen por campaña) | Métricas ingresadas por comando o importadas; agregaciones con `SUM`/`AVG` en SQL. |
| **CU8** | `repIng` (ingresos por proformas/periodo) · `repCam` (campañas por estado) · `repMat` (material aprobado/rechazado) · `repPau` (pautas por canal) · `repMet` (impacto) · `repPag` (páginas detectadas) | Reportes = consultas agregadas → tablas HTML. Parámetros típicos: fecha_inicio, fecha_fin (`DD/MM/YYYY`). |

---

## 11. Compilar, ejecutar y probar

### Configuración (primera vez)
`config.properties` **no está en Git**. Crearlo en la raíz del proyecto:
```properties
mail.usuario=grupo13sa
mail.password=********
mail.direccion=grupo13sa@tecnoweb.org.bo
pop3.host=mail.tecnoweb.org.bo
pop3.puerto=110
smtp.host=mail.tecnoweb.org.bo
smtp.puerto=25
db.host=db.tecnoweb.org.bo
db.puerto=5432
db.nombre=db_grupo13sa
db.usuario=grupo13sa
db.password=********
app.intervalo=10
```
El programa se ejecuta **desde la raíz del proyecto** (ahí busca `config.properties`).

### Compilar
```powershell
# Windows (PowerShell)
javac -encoding UTF-8 -d out -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
```
```bash
# Linux / Git Bash
javac -encoding UTF-8 -d out -cp "lib/*" $(find src -name "*.java")
```

### Ejecutar el servidor de correo
```powershell
java -cp "out;lib/*" grupo13.App        # Windows   (separador ;)
java -cp "out:lib/*" grupo13.App        # Linux     (separador :)
```
Consola esperada:
```
[15:30:00] Escuchando grupo13sa@tecnoweb.org.bo cada 10 s (Ctrl+C para salir)
[15:30:10] Recibido -> De: juan@gmail.com | Asunto: lisUsu["*"]
[15:30:11]   Respondido: Listando los Usuarios
```

### Probar un comando sin correo (modo `--probar`)
Ejecuta el comando contra la base de datos real e imprime el HTML de la respuesta. **No lee ni envía correos.**
```powershell
java -cp "out;lib/*" grupo13.App --probar "lisUsu[\"*\"]"
```
Útil para desarrollar cada comando antes de probarlo por correo.

### Checklist de prueba de un comando
- [ ] Caso correcto (`Éxito` / listado con datos).
- [ ] Listado vacío y con filtro.
- [ ] Cantidad de parámetros incorrecta → muestra el ejemplo.
- [ ] Tipos inválidos (ID no numérico, email mal formado).
- [ ] ID / FK inexistente → mensaje claro (no error SQL crudo).
- [ ] Duplicados (campos `UNIQUE`).
- [ ] Prueba real por correo desde Gmail.
- [ ] **Borrar los datos de prueba** de la base de datos compartida.

---

## 12. Forma de trabajo en equipo (Git)

- Rama principal: `main` (o `master`). Cada CU en su rama: `cu3-tarifas`, `cu5-pautas`, etc.
- Commits pequeños y en español, describiendo el comando: `CU3: agrega lisTar e insTar`.
- Antes de hacer *merge*: compilar sin errores, pasar el checklist de la sección 11 y actualizar este README.
- **No subir:** `config.properties` (ya en `.gitignore`), contraseñas, ni datos personales reales.
- `out/` contiene archivos generados; se recomienda agregarlo a `.gitignore` y quitarlo del repositorio
  (`git rm -r --cached out`) para evitar conflictos entre integrantes.
- `lib/postgresql-42.7.4.jar` **sí** se versiona (no hay Maven/Gradle).
- **Solo una instancia** de `App` debe estar corriendo contra el buzón a la vez (ver sección 13).

---

## 13. Limitaciones y problemas conocidos

| Tema | Detalle | Recomendación |
|---|---|---|
| **El buzón se vacía** | Cada correo leído se procesa y se borra (`DELE` + `QUIT`). Si dos integrantes ejecutan `App` a la vez, se "roban" los correos. | Coordinar quién tiene el servidor corriendo; para desarrollo usar `--probar`. |
| **Contraseña en el código** | `Config.java` tiene la contraseña del grupo como valor por defecto, así que queda versionada aunque `config.properties` esté en `.gitignore`. | Quitar los valores por defecto de `mail.password` y `db.password` en `Config.java` y depender solo de `config.properties`. |
| **Sin autenticación de remitente** | Cualquiera que escriba a `grupo13sa@tecnoweb.org.bo` puede ejecutar comandos. | Pendiente: permitir solo remitentes registrados como usuarios activos y controlar permisos por rol (RF02). |
| Contraseña en el asunto | `insUsu` lleva la contraseña en texto plano en el asunto del correo. | Aceptado para el alcance académico; en la BD se guarda hasheada. |
| Spam | Gmail puede mandar las respuestas a *Spam* (el servidor no firma DKIM/SPF). | Revisar la carpeta Spam y marcar "No es spam". |
| Puerto 25 | Algunas redes/ISP bloquean el puerto 25 de salida. | Probar desde la red de la facultad o con otra conexión. |
| Si falla el SMTP | El correo igual se borra del buzón y la respuesta se pierde (queda en el log). | Revisar la consola. |
| Datos compartidos | Todos usan la misma `db_grupo13sa`. | Usar datos de prueba identificables y borrarlos al terminar. |
| Archivos (CU4) | El material se registra por URL; aún no se leen adjuntos MIME. | Extensión futura en `ClientePOP3` (multipart). |
| Scraping (CU6) | Las redes sociales bloquean bots y requieren login. | Usar búsquedas públicas/URLs conocidas con `HttpClient`; registrar errores en `ejecucion_scraping.mensaje_error`. |

---

## 14. Reglas para agentes de IA

Si eres un agente trabajando en este repositorio:

1. **No agregues frameworks ni dependencias.** Solo JDK 17 + `lib/postgresql-42.7.4.jar`.
2. **No modifiques el esquema de la base de datos** ni ejecutes `sql/schema.sql`: las tablas ya existen. Si un CU requiere cambiarlo, propón el cambio y espera confirmación.
3. **No ejecutes `grupo13.App` sin `--probar`** salvo que te lo pidan: lee y **borra** los correos reales del buzón y envía respuestas.
4. Sigue las convenciones de la sección 7 y el procedimiento de la sección 8 (D* → N* → registrar en `Despachador`).
5. Nombres de comandos: acción(3) + Entidad(3) en camelCase. Confirma con el usuario los nombres nuevos si no están en la sección 10.
6. Prueba cada comando con `--probar`, incluidos los casos de error, y **elimina los datos de prueba** que insertes (y reinicia la identidad si la tabla quedó vacía: `ALTER TABLE t ALTER COLUMN id RESTART WITH 1`).
7. Mensajes al usuario en **español**, claros y escapados con `Html.esc()`.
8. Al terminar, actualiza este README: tabla de comandos (sección 6), estado del CU (sección 3) y hoja de ruta (sección 10).
9. Nunca escribas credenciales en archivos versionados; usa `config.properties`.
