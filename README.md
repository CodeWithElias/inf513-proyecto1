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
| **Respuesta = correo HTML** | Tablas y mensajes se generan como HTML (`presentacion/Html.java`). |
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
| **CU2** | Gestión de Cuentas de Redes Sociales Institucionales | RF03 | `canal_difusion`, `red_social`, `plataforma`, `seguidores_red` | ✅ **Completo**: plataformas, redes oficiales por unidad y responsable, métricas de seguidores, bajas lógicas y reactivación |
| **CU3** | Gestión de Oferta Comercial, Clientes y Generación de Cotizaciones | RF04, RF05, RF06, RF07 | `empresa_anunciante`, `representante`, `visitante`, `servicio_publicitario`, `tarifa`, `paquete`, `paquete_item`, `proforma`, `detalle_proforma`, `campania` | ✅ **Completo**: empresas, visitantes, servicios, tarifario con vigencia, paquetes con descuento, proformas con cálculo automático y campañas |
| **CU4** | Gestión de Cargas, Validar Material Publicitario | RF08, RF09 | `material_publicitario`, `revision_material` | ✅ **Completo**: carga por URL con validación técnica (formato por tipo, duración contratada), revisión por Canal 11 (aprobar / rechazar / pedir corrección), versiones e historial |
| **CU5** | Gestión de Programar Pauta Multi-Canal | RF10, RF11 | `canal_difusion`, `espacio_publicitario`, `programacion_pauta`, `notificacion` | ✅ **Completo**: canales, calendario de espacios sin superposición, programación validada contra lo contratado, reprogramación/cancelación/publicación y avisos a la unidad (sistema + correo) |
| **CU6** | Gestión de Monitoreo y Detectar Páginas No Autorizadas mediante Scraping | RF12 | `termino_monitoreo`, `ejecucion_scraping`, `pagina_no_autorizada`, `deteccion_pagina`, `notificacion` | ✅ **Completo (YouTube)**: búsqueda por términos en canales y videos, comparación con las redes oficiales del CU2, registro con evidencia, seguimiento por estados, ejecución periódica y alertas al administrador. Facebook/TikTok pendientes (anti-scraping) |
| **CU7** | Gestión de Análisis Métricas de Impacto | RF13 | `metrica_publicacion`, `seguidores_red` | ✅ **Completo**: mediciones manuales (cualquier canal) y captura automática de YouTube; indicadores por pauta, campaña (incluye costo por resultado) y red social |
| **CU8** | Reportes y Estadísticas | RF13 | todas (solo lectura) | ✅ **Completo**: tablero general y 8 reportes por periodo (comercial, servicios, ocupación, material, impacto, redes, monitoreo y vista del cliente) con gráficos de barras en el correo |

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

El proyecto aplica **estrictamente la arquitectura de 3 capas**: **Presentación → Negocio → Datos**.
Cada capa solo conoce a la capa inmediatamente inferior; nunca al revés ni saltándose una.

### Vista general
```
 ┌────────────┐   correo (asunto = comando)   ┌───────────────────────────┐
 │  Cliente   │ ─────────────────────────────▶│ grupo13sa@tecnoweb.org.bo │
 │ (Gmail...) │ ◀───────────────────────────── │   (servidor de correo)    │
 └────────────┘   respuesta HTML               └───────────┬───────────────┘
                                                POP3 :110  │  ▲ SMTP :25
                                                           ▼  │
 ┌──────────────────────── CAPA DE PRESENTACIÓN (grupo13.presentacion) ─────────────────────┐
 │  App ─▶ correo/ClientePOP3 ─▶ comandos/Despachador ─▶ P* (PUsuario, PTarifa…) ─▶ Html     │
 │          (lee el correo)      (parsea el asunto,      (arma columnas, mensajes,   (HTML)  │
 │                                valida nº de params)    formato Sí/No y fechas)            │
 │  correo/ClienteSMTP ◀──────────────── Respuesta (título + HTML) ◀────────────────────────│
 └───────────────────────────────────────────┬──────────────────────────────────────────────┘
                     parámetros (String)      │  ▲  filas (String[]), IDs / NegocioException
                                              ▼  │
 ┌──────────────────────────── CAPA DE NEGOCIO (grupo13.negocio) ───────────────────────────┐
 │  N* (NUsuario, NTarifa, NProforma…) + Validar + Seguridad                                 │
 │  valida datos, aplica reglas y hace los CÁLCULOS (precios, totales, vigencia, estados)    │
 └───────────────────────────────────────────┬──────────────────────────────────────────────┘
                     valores ya validados     │  ▲  filas crudas (t/f, fechas ISO, NULL) / SQLException
                                              ▼  │
 ┌───────────────────────────── CAPA DE DATOS (grupo13.datos) ──────────────────────────────┐
 │  D* (DUsuario, DTarifa, DProforma…) + Conexion  —  solo SQL con PreparedStatement         │
 └───────────────────────────────────────────┬──────────────────────────────────────────────┘
                                              ▼
                                   PostgreSQL db_grupo13sa
```

### Responsabilidades y prohibiciones
| Capa | Paquete | Hace | **Nunca** |
|---|---|---|---|
| **Presentación** | `grupo13.presentacion` (+ `correo`, `comandos`) y `App` | Leer/enviar correos; interpretar el asunto (`nombre["p1",...]`, `"*"` = todos); validar que el comando exista y la **cantidad** de parámetros; llamar a la clase `N*`; elegir títulos de columnas, mensajes de éxito y sugerencias de comandos; dar formato (`t/f → Sí/No`, fechas `DD/MM/AAAA`, `NULL → -`); generar el HTML; escapar todo con `Html.esc`. | Importar `grupo13.datos` o `java.sql`; validar reglas de negocio; calcular montos o estados. |
| **Negocio** | `grupo13.negocio` | Validar cada dato (`Validar`); verificar existencia, unicidad y estado de registros relacionados; aplicar reglas (bajas lógicas, transiciones de estado, jerarquías sin ciclos); **calcular** (precio de paquete, subtotal/total de proforma, número `PRO-AAAA-NNNNN`, vigencia de tarifa, proforma vencida); hash de contraseñas; traducir `SQLException` a `NegocioException`. | Importar `grupo13.presentacion`; generar HTML; mencionar nombres de comandos en sus mensajes; escribir SQL. |
| **Datos** | `grupo13.datos` | Ejecutar SQL con `PreparedStatement`; devolver filas **crudas**; agrupar en una transacción las escrituras que deben ser atómicas (guardando los valores que calculó negocio). | Importar `grupo13.negocio` o `grupo13.presentacion`; formatear (`to_char`, `'Sí'/'No'`, etiquetas); decidir reglas o cálculos. |
| Transversal | `grupo13.config` | Leer `config.properties` (usado por datos y por presentación). | — |

### Contratos entre capas
- **Presentación → Negocio:** cada método `P*` recibe la lista de parámetros del comando y llama a un método `N*`
  con **parámetros explícitos** (`negocio.insertar(nombre, email, telefono, ...)`). El filtro `"*"` se convierte en `null`
  en la presentación (`Vista.filtro`), porque es parte del protocolo de comandos, no del negocio.
- **Negocio → Presentación:** devuelve datos (`List<String[]>`, `String[]`, `int` con el ID generado, `boolean`)
  o lanza **`NegocioException`** con un mensaje en texto plano. Nunca devuelve HTML ni `Respuesta`.
- **Negocio → Datos:** llama a métodos `D*` con valores ya validados y tipados (`int`, `BigDecimal`, `LocalDate`…).
  Si una operación toca varias tablas, negocio **calcula** los valores y datos los **guarda juntos en una transacción**
  (p. ej. `DProforma.agregarDetalle(..., subtotal, total)` o `DTarifa.modificar(..., preciosPaquetes)`).
- **Datos → Negocio:** filas crudas (`"t"`/`"f"`, fechas ISO `2026-10-19`, `null`) o `SQLException`.
- Cada clase documenta en su Javadoc el **orden de columnas** de las filas que devuelve.

### Verificación automática de la arquitectura
Estos comandos deben **no imprimir nada**. Ejecútelos antes de cada commit:
```bash
grep -rn "import grupo13\.\(negocio\|presentacion\)" src/grupo13/datos          # datos no conoce capas superiores
grep -rn "import grupo13\.presentacion\|Html" src/grupo13/negocio                # negocio no conoce presentación
grep -rn "import grupo13\.datos\|import java\.sql" src/grupo13/presentacion src/grupo13/App.java   # presentación no conoce datos
grep -rn "to_char\|'Sí'\|'No'" src/grupo13/datos                                 # datos no formatea
```

### Estructura de carpetas
```
proyecto-mail/
├── README.md
├── config.properties              ← credenciales (NO se sube a Git)
├── lib/postgresql-42.7.4.jar      ← único JAR externo
├── sql/schema.sql                 ← esquema completo (ya aplicado en db_grupo13sa)
├── out/                           ← .class compilados (generado)
└── src/grupo13/
    ├── App.java                   ← arranque: bucle POP3 → Despachador → SMTP; modo --probar
    ├── config/Config.java         ← lee config.properties (transversal)
    │
    ├── presentacion/              ══════ CAPA DE PRESENTACIÓN ══════
    │   ├── correo/
    │   │   ├── ClientePOP3.java   ← USER/PASS/STAT/RETR/DELE/QUIT; extrae From y Subject
    │   │   ├── ClienteSMTP.java   ← HELO/MAIL FROM/RCPT TO/DATA; cuerpo HTML en Base64
    │   │   ├── Correo.java        ← remitente + asunto
    │   │   ├── MimeUtil.java      ← decodifica asuntos =?UTF-8?B?...?=, extrae email del From
    │   │   └── Notificador.java   ← envía por correo los avisos del RF11 (desactivado en --probar)
    │   ├── comandos/
    │   │   ├── Comando.java       ← parser del asunto: nombre + lista de parámetros
    │   │   ├── Despachador.java   ← REGISTRO DE TODOS LOS COMANDOS (→ métodos P*) + help + errores
    │   │   ├── Accion.java        ← Respuesta ejecutar(List<String>) throws NegocioException
    │   │   ├── Respuesta.java     ← exito/error + título + HTML
    │   │   └── ComandoException   ← error del protocolo de comandos (formato, comando inexistente)
    │   ├── Html.java              ← pagina, tabla, ficha, esc y formato (t/f, fechas ISO, NULL)
    │   ├── Vista.java             ← respuestas típicas (listado, registro, éxito) y filtro "*"
    │   ├── PUsuario, PRol, PUnidad, PRepresentante            (CU1)
    │   ├── PPlataforma, PRedSocial, PSeguidoresRed            (CU2)
    │   ├── PEmpresa, PVisitante, PServicio, PTarifa, PPaquete, PProforma, PCampania   (CU3)
    │   ├── PMaterial                                                                  (CU4)
    │   ├── PCanal, PEspacio, PPauta, PNotificacion                                    (CU5)
    │   ├── PTermino, PScraping (manual + periódico), PPagina                          (CU6)
    │   ├── PMetrica (registro + captura), PAnalisis (indicadores)                     (CU7)
    │   └── PReporte (tablas + gráficos de barras con Html.grafico)                    (CU8)
    │
    ├── negocio/                   ══════ CAPA DE NEGOCIO ══════
    │   ├── NegocioException.java  ← única excepción que ve la presentación
    │   ├── Validar.java           ← validaciones (id, texto, email, teléfono, ci, nit, password, url, monto, %, fechas)
    │   ├── Seguridad.java         ← hash de contraseñas PBKDF2-SHA256 con sal
    │   ├── NUsuario, NRol, NUnidad (evita ciclos), NRepresentante                   (CU1)
    │   ├── NPlataforma, NRedSocial, NSeguidoresRed                                  (CU2)
    │   ├── NEmpresa, NVisitante, NServicio, NTarifa (vigencia), NPaquete (precio),
    │   │   NProforma (totales, número, estados, vencida), NCampania                 (CU3)
    │   ├── NMaterial (formatos por tipo, duración contratada, estados y revisiones) (CU4)
    │   ├── NCanal, NEspacio (superposición), NPauta (cupo contratado, compatibilidad,
    │   │   destinatarios de avisos), NNotificacion, ResultadoPauta                  (CU5)
    │   ├── NTermino, NScraping (coincidencia de términos, comparación con oficiales,
    │   │   alertas), NPagina (estados), ResultadoScraping                           (CU6)
    │   ├── NMetrica (coherencia, captura YouTube), NAnalisis (cálculo de indicadores),
    │   │   Analisis                                                                 (CU7)
    │   └── NReporte (agrupa y calcula todos los indicadores del periodo), Reporte   (CU8)
    │
    └── datos/                     ══════ CAPA DE DATOS ══════
        ├── Conexion.java          ← JDBC + ayudantes: consultar, consultarUno, valor, ejecutar, insertar,
        │                            ids, estaActivo, contar, enTransaccion
        ├── DUsuario, DRol, DUnidad (WITH RECURSIVE del subárbol), DRepresentante      (CU1)
        ├── DPlataforma, DRedSocial (canal_difusion + red_social), DSeguidoresRed       (CU2)
        ├── DEmpresa, DVisitante, DServicio, DTarifa, DPaquete, DProforma, DCampania    (CU3)
        ├── DMaterial (material_publicitario + revision_material)                       (CU4)
        ├── DCanal, DEspacio, DPauta (pauta + espacio + avisos en transacción), DNotificacion (CU5)
        ├── DTermino, DEjecucion, DPagina (página + detecciones + avisos en transacción)  (CU6)
        ├── DYoutube (fuente externa: búsqueda y estadísticas públicas de YouTube) + Json  (CU6, CU7)
        ├── DMetrica (mediciones; última medición por pauta con LATERAL)                (CU7)
        └── DReporte (solo extrae las filas de un periodo; no agrupa ni calcula)        (CU8)
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
   │ ── PRESENTACIÓN ──────────────────────────────────────────────────────────────────────────
   ▼ Comando.parsear ─────────── formato inválido ──────────────▶ ComandoException → "Error"
   │   nombre = insUsu, params = [Ana, ana@x.com, "", clave123, 1, ""]
   ▼ buscar en el registro (sin distinguir mayúsculas) ─ no existe ─▶ "Error: el comando no existe"
   ▼ ¿cantidad de parámetros correcta? ─────────── no ──────────────▶ "Error: requiere N parámetros + ejemplo"
   ▼ PUsuario.insertar(params)  →  negocio.insertar("Ana", "ana@x.com", "", "clave123", "1", "")
   │
   │ ── NEGOCIO ──────────────────────────────────────────────────────────────────────────────
   ▼ NUsuario.insertar(...)
   │   Validar.* (email válido, password ≥ 6, id_rol numérico...) ─ falla ─▶ NegocioException → "Error"
   │   reglas: email único, rol existe y activo, unidad existe...      ─ falla ─▶ NegocioException → "Error"
   │   Seguridad.hashPassword(password)
   │
   │ ── DATOS ────────────────────────────────────────────────────────────────────────────────
   ▼ DUsuario.insertar(...) → id ─── SQLException ─▶ (negocio la traduce) NegocioException → "Error en la base de datos"
   │
   │ ── de vuelta en PRESENTACIÓN ─────────────────────────────────────────────────────────────
   ▼ PUsuario: negocio.obtener(id) → fila cruda → Vista.exito("El usuario fue registrado...", COLUMNAS, fila)
 Respuesta "Éxito" (HTML con la fila formateada: Sí/No, DD/MM/AAAA)
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
| **Plataformas de Redes Sociales** | | |
| `lisPla` | filtro (`*` = todas; muestra conteo de redes activas) | `lisPla["*"]` |
| `insPla` | nombre | `insPla["TikTok"]` |
| **Redes Sociales Institucionales** | | |
| `lisRed` | filtro (`*` = todas; busca por nombre, plataforma, URL, responsable o unidad) | `lisRed["*"]` |
| `mosRed` | id_canal | `mosRed["1"]` |
| `insRed` | nombre, url, id_plataforma, id_usuario (`""` = sin asignar), id_unidad (`""` = sin unidad) | `insRed["Canal 11 Oficial Facebook","https://facebook.com/canal11uagrm","1","1","1"]` |
| `modRed` | id_canal, nombre, url, id_plataforma, id_usuario, id_unidad | `modRed["1","Canal 11 FB Oficial","https://facebook.com/canal11oficial","1","1","1"]` |
| `eliRed` | id_canal — baja lógica (`activo = false`, registra `fecha_baja`) | `eliRed["1"]` |
| `actRed` | id_canal — reactiva | `actRed["1"]` |
| **Seguidores** | | |
| `insSeg` | id_canal, seguidores — registra recuento para la fecha actual | `insSeg["1","15400"]` |
| `lisSeg` | id_canal — historial de mediciones de seguidores | `lisSeg["1"]` |
| **CU3 · Empresas anunciantes** (RF04) | | |
| `lisEmp` | filtro (razón social, NIT o nombre comercial) | `lisEmp["*"]` |
| `mosEmp` | id_empresa — la empresa y sus representantes | `mosEmp["1"]` |
| `insEmp` | razon_social, nit (solo dígitos, único), nombre_comercial, direccion_facturacion, email_facturacion, telefono (los 4 últimos pueden ir `""`) | `insEmp["Importadora Santa Cruz SRL","1023456789","ImporSCZ","Av. Banzer 123","facturas@imporscz.com","33445566"]` |
| `modEmp` | id_empresa + los 6 campos de `insEmp` | `modEmp["1","Importadora Santa Cruz SRL","1023456789","ImporSCZ","Av. Banzer 456","facturas@imporscz.com","33445566"]` |
| `eliEmp` | id_empresa — baja lógica; **rechazada con proformas pendientes** | `eliEmp["1"]` |
| `actEmp` | id_empresa — reactiva | `actEmp["1"]` |
| **CU3 · Visitantes** (cotizan sin cuenta) | | |
| `lisVis` | filtro | `lisVis["*"]` |
| `insVis` | nombre, email, telefono (al menos uno de los dos contactos) | `insVis["María López","maria@gmail.com","70011223"]` |
| `modVis` | id_visitante, nombre, email, telefono | `modVis["1","María López","maria.lopez@gmail.com","70011223"]` |
| `eliVis` | id_visitante — borrado físico, **solo sin proformas** (la tabla no tiene `activo`) | `eliVis["1"]` |
| **CU3 · Servicios publicitarios** (RF05) | | |
| `lisSer` | filtro | `lisSer["*"]` |
| `insSer` | nombre (único), tipo, descripcion | `insSer["Spot en pausa comercial","TV","Spot de 30 s en tanda comercial"]` |
| `modSer` | id_servicio, nombre, tipo, descripcion | `modSer["1","Spot en pausa comercial","TV","Spot de 30 s en horario estelar"]` |
| `eliSer` | id_servicio — baja lógica; **rechazada con tarifas activas** | `eliSer["1"]` |
| `actSer` | id_servicio | `actSer["1"]` |
| **CU3 · Tarifario** (RF05) — fechas `DD/MM/AAAA`, montos `150` / `150.50` / `150,50` | | |
| `lisTar` | filtro (servicio, tipo, unidad o unidad de cobro); muestra si está **vigente hoy** | `lisTar["*"]` |
| `mosTar` | id_tarifa | `mosTar["1"]` |
| `insTar` | id_servicio, id_unidad (`""` = general), monto, unidad_cobro, duracion_seg, vigente_desde, vigente_hasta (los 5 últimos salvo monto pueden ir `""`) | `insTar["1","","350.00","por spot","30","01/01/2026","31/12/2026"]` |
| `modTar` | id_tarifa + los 7 campos de `insTar` — **recalcula los paquetes** que la usan; las proformas emitidas no cambian | `modTar["1","1","","380.00","por spot","30","01/01/2026","31/12/2026"]` |
| `eliTar` | id_tarifa — deshabilita; **rechazada si está en un paquete activo** | `eliTar["1"]` |
| `actTar` | id_tarifa | `actTar["1"]` |
| **CU3 · Paquetes** (RF06) — `precio_total = Σ(monto × cantidad) × (1 − descuento%/100)` | | |
| `lisPaq` | filtro | `lisPaq["*"]` |
| `mosPaq` | id_paquete — con el detalle de sus tarifas | `mosPaq["1"]` |
| `insPaq` | nombre, descripcion, descuento (% 0–100; `""` = 0) | `insPaq["Paquete TV + Redes","Spots en TV y publicaciones en redes","10"]` |
| `modPaq` | id_paquete, nombre, descripcion, descuento | `modPaq["1","Paquete TV + Redes","Combo mensual","15"]` |
| `addPaq` | id_paquete, id_tarifa, cantidad — agrega o actualiza la cantidad (tarifa activa) | `addPaq["1","1","10"]` |
| `quiPaq` | id_paquete, id_tarifa | `quiPaq["1","1"]` |
| `eliPaq` / `actPaq` | id_paquete — baja / alta lógica | `eliPaq["1"]` |
| **CU3 · Proformas y cotizaciones** (RF07) — estados `pendiente → aprobada \| rechazada` (`vencida` si pasó la validez) | | |
| `lisPro` | filtro (número, estado, empresa, NIT o visitante) | `lisPro["pendiente"]` |
| `mosPro` | id_proforma — **la cotización completa** (cabecera, ítems, subtotal, descuento, total) | `mosPro["1"]` |
| `insPro` | id_empresa, id_visitante (**uno de los dos**), dias_validez (`""` = 15), descuento en Bs (`""` = 0) — genera `PRO-AAAA-NNNNN` | `insPro["1","","15","0"]` |
| `modPro` | id_proforma, descuento (Bs), fecha_validez (no anterior a hoy) | `modPro["1","100","30/11/2026"]` |
| `addDet` | id_proforma, id_tarifa, id_paquete (**uno de los dos**), cantidad, frecuencia — tarifa vigente o paquete activo con ítems; congela el precio unitario | `addDet["1","1","","20","2 por día"]` |
| `quiDet` | id_detalle | `quiDet["1"]` |
| `aprPro` | id_proforma — pendiente, vigente y con ítems | `aprPro["1"]` |
| `recPro` | id_proforma | `recPro["1"]` |
| **CU3 · Campañas** | | |
| `lisCam` | filtro | `lisCam["*"]` |
| `genCam` | id_proforma (aprobada, **de empresa**, sin campaña), nombre, fecha_inicio, fecha_fin | `genCam["1","Campaña Navidad 2026","01/12/2026","31/12/2026"]` |
| `modCam` | id_campania, nombre, fecha_inicio, fecha_fin, estado (`activa`, `pausada`, `finalizada`, `cancelada`) | `modCam["1","Campaña Navidad 2026","01/12/2026","31/12/2026","pausada"]` |
| **CU4 · Carga de material** (RF08) — estados `pendiente → aprobado \| rechazado \| correccion`; `correccion → (nueva versión) pendiente`; cualquiera `→ retirado` | | |
| `lisMat` | filtro (archivo, tipo, estado, campaña o empresa) | `lisMat["pendiente"]` |
| `mosMat` | id_material — ficha, texto de la publicación e historial de revisiones | `mosMat["1"]` |
| `insMat` | id_campania (activa o pausada), id_usuario (**representante de la empresa de la campaña**), tipo (`video`, `audio`, `imagen`, `banner`, `texto`), nombre_archivo (con extensión), url_archivo, duracion_seg (**solo** video/audio, ≤ duración contratada), texto_publicacion (obligatorio en `texto`) | `insMat["1","3","video","spot_navidad.mp4","https://drive.google.com/spot_navidad.mp4","30",""]` |
| `modMat` | id_material + tipo, nombre_archivo, url_archivo, duracion_seg, texto_publicacion — solo **pendiente**, no cambia la versión | `modMat["1","video","spot_v2.mp4","https://drive.google.com/spot_v2.mp4","30",""]` |
| `subMat` | id_material, nombre_archivo, url_archivo, duracion_seg, texto_publicacion — solo en **correccion**; versión + 1 y vuelve a pendiente | `subMat["1","spot_v2.mp4","https://drive.google.com/spot_v2.mp4","28",""]` |
| `eliMat` | id_material — retira (baja lógica); **rechazado si está programado en una pauta** | `eliMat["1"]` |
| **CU4 · Validación de material** (RF09) — revisa **personal de Canal 11** (usuario activo que no es representante); solo material **pendiente** | | |
| `aprMat` | id_material, id_revisor, observaciones (opcional) | `aprMat["1","1","Cumple los estándares técnicos"]` |
| `recMat` | id_material, id_revisor, observaciones (**obligatorio**) — rechazo definitivo | `recMat["1","1","Contenido no permitido por la normativa"]` |
| `corMat` | id_material, id_revisor, observaciones (**obligatorio**) — el cliente debe subir otra versión | `corMat["1","1","El audio está saturado, reduzca el volumen"]` |
| `lisRev` | id_material — historial de revisiones | `lisRev["1"]` |
| **CU5 · Canales de difusión** — tipos `television`, `radio`, `web` (las redes sociales del CU2 también son canales) | | |
| `lisCan` | filtro — todos los canales con sus espacios disponibles | `lisCan["*"]` |
| `insCan` | tipo, nombre (único) | `insCan["television","Canal 11 UAGRM TV"]` |
| `eliCan` / `actCan` | id_canal — baja / alta; **baja rechazada con pautas programadas**; las redes sociales se administran con `eliRed`/`actRed` | `eliCan["3"]` |
| **CU5 · Calendario de espacios** (RF10) — fechas `DD/MM/AAAA`, horas `HH:MM`; estados `disponible`, `ocupado`, `bloqueado` | | |
| `lisEsp` | id_canal (`*` = todos), fecha (`*` = todas) — calendario con la campaña que ocupa cada espacio | `lisEsp["3","01/12/2026"]` |
| `disEsp` | id_canal (`*`), fecha_desde, fecha_hasta (máx. 92 días) — solo espacios libres | `disEsp["*","01/12/2026","07/12/2026"]` |
| `insEsp` | id_canal (activo), fecha (no pasada), hora_inicio, hora_fin — **no puede superponerse** con otro espacio del canal | `insEsp["3","01/12/2026","20:00","20:01"]` |
| `genEsp` | id_canal, fecha_desde, fecha_hasta, hora_inicio, hora_fin — el mismo horario cada día (omite los días con superposición) | `genEsp["3","01/12/2026","31/12/2026","20:00","20:01"]` |
| `eliEsp` / `actEsp` | id_espacio — bloquear (solo disponibles) / desbloquear | `eliEsp["5"]` |
| **CU5 · Programación de pauta** (RF10) — estados `programada → publicada \| cancelada` | | |
| `lisPau` | filtro (campaña, empresa, canal o estado) | `lisPau["programada"]` |
| `mosPau` | id_programacion | `mosPau["1"]` |
| `insPau` | id_detalle (ítem contratado de la proforma), id_material (**aprobado**), id_espacio (**disponible**), id_usuario (**personal de Canal 11**) | `insPau["1","1","5","1"]` |
| `repPau` | id_programacion, id_espacio_nuevo — libera el espacio anterior | `repPau["1","6"]` |
| `canPau` | id_programacion — cancela y libera el espacio (y el cupo contratado) | `canPau["1"]` |
| `pubPau` | id_programacion, url_publicacion (**obligatoria en redes sociales**) — solo cuando ya llegó la hora | `pubPau["1","https://facebook.com/canal11uagrm/posts/123"]` |
| **CU5 · Notificaciones** (RF11) | | |
| `lisNot` | id_usuario — avisos recibidos (fecha, título, mensaje, leída) | `lisNot["4"]` |
| `leeNot` | id_notificacion — marca como leída | `leeNot["1"]` |
| **CU6 · Términos de monitoreo** (RF12) | | |
| `lisTer` | filtro | `lisTer["*"]` |
| `insTer` | termino (mín. 3 caracteres, único), tipo (opcional) | `insTer["UAGRM","sigla"]` |
| `eliTer` / `actTer` | id_termino — desactivar (deja de buscarse) / reactivar | `eliTer["1"]` |
| **CU6 · Scraping** — por ahora solo **YouTube**; además se ejecuta **solo** cada `scraping.intervalo.horas` | | |
| `ejeScr` | id_plataforma, id_termino (`*` = todos los términos activos de `termino_monitoreo`) — ejecuta ahora: busca, compara con `red_social`, registra y alerta | `ejeScr["1","*"]` · `ejeScr["1","2"]` |
| `lisEje` | id_plataforma (`*` = todas) — historial: estado, errores, detecciones y páginas nuevas | `lisEje["*"]` |
| **CU6 · Páginas no autorizadas** — toda cuenta encontrada que **no está en `red_social`**; estados `detectada → confirmada → denunciada → eliminada` | | |
| `lisPag` | filtro (URL, nombre, estado o plataforma) | `lisPag["detectada"]` |
| `mosPag` | id_pagina — la página y sus detecciones con evidencia | `mosPag["1"]` |
| `modPag` | id_pagina, estado (`confirmada`, `denunciada`, `eliminada`), id_usuario (**personal de Canal 11**) | `modPag["1","confirmada","1"]` |
| **CU7 · Métricas** (RF13) — mediciones **acumuladas** de pautas **publicadas**; coherencia: alcance ≤ impresiones, clics ≤ impresiones, compartidos ≤ interacciones | | |
| `insMet` | id_programacion, alcance, impresiones, interacciones, clics, compartidos — registro manual (cualquier canal) | `insMet["5","1200","3400","210","45","12"]` |
| `modMet` | id_metrica + los 5 valores — corrige una medición | `modMet["1","1250","3400","210","45","12"]` |
| `eliMet` | id_metrica — borra una medición registrada por error | `eliMet["1"]` |
| `lisMet` | id_programacion — historial de mediciones | `lisMet["5"]` |
| `capMet` | id_programacion (`*` = todas las de YouTube) — **captura automática**: impresiones = vistas, interacciones = me gusta + comentarios; alcance, clics y compartidos se mantienen de la última medición | `capMet["*"]` |
| **CU7 · Análisis de impacto** (RF13) | | |
| `anaPau` | id_programacion — tasa de interacción, CTR, frecuencia, viralidad y crecimiento respecto a la medición anterior | `anaPau["5"]` |
| `anaCam` | id_campania — totales, indicadores, **rendimiento por canal** y **costo por resultado** (CPM, costo por clic, costo por interacción) sobre la inversión de la proforma | `anaCam["2"]` |
| `anaRed` | id_canal — crecimiento de seguidores (total, %, diario) e interacción de las pautas publicadas en la red | `anaRed["1"]` |
| **CU8 · Reportes y estadísticas** (RF13) — todos reciben fecha_desde y fecha_hasta (`DD/MM/AAAA`; `*` = sin límite) e incluyen indicadores, tablas y **gráficos de barras** | | |
| `tabGen` | Tablero general: ingresos, proformas, conversión, campañas activas, pautas, impresiones, interacción, material pendiente y páginas no autorizadas nuevas | `tabGen["01/01/2026","31/12/2026"]` |
| `repCom` | Comercial: proformas por estado (incluye **vencidas**), ingresos por mes, tasa de conversión, ticket promedio y principales clientes | `repCom["01/10/2026","31/10/2026"]` |
| `repSer` | Servicios y paquetes más vendidos (cantidad, monto y % del total) | `repSer["*","*"]` |
| `repPau` | Ocupación del calendario por canal (ocupados/disponibles/bloqueados), pautas programadas/publicadas y tasa de cancelación | `repPau["01/12/2026","31/12/2026"]` |
| `repMat` | Material por estado y tipo, tasa de aprobación, versiones promedio y revisiones por revisor | `repMat["*","*"]` |
| `repImp` | Impacto y alcance por canal y por campaña, y las 5 pautas con más interacción | `repImp["01/01/2026","31/12/2026"]` |
| `repRed` | Crecimiento de seguidores de cada red social en el periodo | `repRed["01/01/2026","31/12/2026"]` |
| `repMon` | Monitoreo (CU6): ejecuciones, detecciones y páginas nuevas por plataforma, término y estado | `repMon["*","*"]` |
| `repCli` | id_empresa, fecha_desde, fecha_hasta — **vista del cliente**: su inversión, sus campañas y su impacto (CPM, costo por interacción) | `repCli["1","*","*"]` |

**Flujo típico CU1:**
1. `insRol["Director",""]`, `insRol["Encargado de Unidad",""]`, `insRol["Cliente",""]` → crear los perfiles.
2. `insUni["Facultad de Ciencias de la Computación","Facultad",""]` → `insUni["Ingeniería Informática","Carrera","1"]`.
3. **Director:** `insUsu[..., id_rol_director, ""]`.
4. **Encargado de Unidad:** `insUsu[..., id_rol_encargado, id_unidad]`.
5. **Cliente:** `insEmp[...]` (CU3), luego `insUsu[..., id_rol_cliente, ""]` y `insRep[id_usuario, ci, cargo, id_empresa]`.

**Flujo típico CU3 (de la oferta a la campaña):**
1. Oferta: `insSer["Spot en pausa comercial","TV",""]` → `insTar["1","","350","por spot","30","",""]` → `insTar["1","","120","por mención","","",""]`.
2. Paquete (opcional): `insPaq["Combo TV","","10"]` → `addPaq["1","1","10"]` → `addPaq["1","2","5"]` → `mosPaq["1"]`.
3. Cliente: `insEmp[...]` (o `insVis[...]` si solo quiere cotizar).
4. Cotización: `insPro["1","","15","0"]` → `addDet["1","1","","20","2 por día"]` / `addDet["1","","1","1","mensual"]` → `mosPro["1"]` (el correo de respuesta **es** la cotización descargable).
5. Cierre: `aprPro["1"]` (o `recPro["1"]`) → `genCam["1","Campaña Navidad","01/12/2026","31/12/2026"]` → la campaña queda lista para CU4 (material) y CU5 (pauta).

**Flujo típico CU4 (del archivo del cliente a material aprobado):**
1. El cliente sube su archivo a un almacenamiento propio (Drive, Dropbox…) y registra el enlace:
   `insMat["1","3","video","spot.mp4","https://drive.google.com/...","30",""]` → queda **pendiente**.
2. Canal 11 revisa (`lisMat["pendiente"]`, `mosMat["1"]`) y decide:
   - `aprMat["1","1",""]` → **aprobado** (listo para la pauta del CU5),
   - `corMat["1","1","Motivo"]` → **correccion** → el cliente sube `subMat["1","spot_v2.mp4","https://...","28",""]` (versión 2, otra vez pendiente),
   - `recMat["1","1","Motivo"]` → **rechazado** (definitivo).
3. Cada decisión queda en el historial: `lisRev["1"]`.

Formatos aceptados por tipo (según la extensión de `nombre_archivo`): video `mp4 mov avi mkv` · audio `mp3 wav aac ogg` ·
imagen/banner `jpg jpeg png gif webp` · texto `txt pdf docx` (si se omite el nombre se usa `publicacion.txt`).

**Flujo típico CU5 (de material aprobado a pauta publicada):**
1. Canales y calendario: `insCan["television","Canal 11 TV"]` → `genEsp["3","01/12/2026","31/12/2026","20:00","20:01"]`
   (las redes sociales del CU2 ya son canales: solo se les crean espacios con `insEsp`/`genEsp`).
2. Buscar lugar: `disEsp["*","01/12/2026","07/12/2026"]` y el ítem contratado con `mosPro["<id_proforma>"]` (columna *ID detalle*).
3. Programar: `insPau["id_detalle","id_material","id_espacio","id_usuario_canal11"]`.
4. **RF11:** si el canal es una red social, se registra una `notificacion` para su responsable (o, si no tiene, para los
   usuarios activos de su unidad) y se le envía por correo. La respuesta indica a quién se avisó y si el correo salió.
5. Cambios: `repPau` (otro espacio, avisa al canal anterior y al nuevo) o `canPau` (libera espacio y cupo).
6. Emitida la pauta: `pubPau["id","url"]`. Los encargados revisan sus avisos con `lisNot["id_usuario"]` / `leeNot["id"]`.

Reglas de `insPau`/`repPau`: campaña **activa**; espacio dentro de las fechas de la campaña y en el futuro; no exceder lo
contratado (cantidad del ítem, o cantidad × tarifas del paquete); el canal debe aceptar el tipo de material
(TV: video/texto/banner · radio: audio/texto · web y redes: video/imagen/banner/texto); si la tarifa es de una unidad,
el canal debe ser una red social de esa unidad.

**Flujo típico CU6 (monitoreo de páginas no autorizadas en YouTube):**
1. Preparación (una vez): `insPla["YouTube"]`, registrar los canales **oficiales** como redes del CU2
   (`insRed["Canal 11 TVU","https://www.youtube.com/@canal11tvuuagrm","<id YouTube>","",""]`) y los términos
   (`insTer["UAGRM","sigla"]`, `insTer["Canal 11",""]`).
2. Ejecución: automática cada `scraping.intervalo.horas` (y al arrancar `App`) con **todos los términos activos** de
   `termino_monitoreo`, o a pedido con `ejeScr["<id YouTube>","*"]` (todos) o `ejeScr["<id YouTube>","<id_termino>"]` (uno).
   Por cada término se buscan **canales** (nombre y descripción) y **videos** (título y descripción → su canal), leyendo
   hasta `scraping.paginas` páginas de resultados, igual que la librería Python *scrapetube* pero en Java puro.
3. Solo cuentan los resultados que **contienen** el término (sin mayúsculas ni tildes). Cada canal se compara con las
   redes oficiales de esa plataforma (`@handle` o `/channel/ID`); los que no son oficiales se registran como
   `detectada`, con una detección por término y su **evidencia** (texto del canal o título y enlace del video).
4. Alertas: cada página nueva genera una `notificacion` para los usuarios activos del rol `alertas.rol`
   (por defecto *Administrador*), y se envía **un correo resumen por ejecución**.
5. Una sola ejecución a la vez por plataforma: si hay otra *en_proceso* (de menos de 30 min) se rechaza; las más antiguas
   se cierran como *interrumpidas*. Todos los resultados de una ejecución se guardan en **una transacción**; si algo falla,
   la ejecución queda en `error` con el motivo (ver `lisEje`).
6. Seguimiento del personal: `lisPag["detectada"]` → `mosPag["id"]` → `modPag["id","confirmada","id_usuario"]`
   → `denunciada` → `eliminada` (si una eliminada reaparece, el scraping la reabre como detectada).
7. **Lo único autorizado es `red_social`**, que administra el personal en el CU2; el scraping no autoriza nada.
   Si una cuenta detectada resulta legítima, se la registra como red oficial (`insRed`) y deja de detectarse.

> La primera ejecución detecta también canales legítimos no registrados (facultades, carreras, medios que mencionan a la
> UAGRM). Es lo esperado: los que sean de la universidad se registran como redes oficiales en el CU2.

**Flujo típico CU7 (medir y analizar el impacto):**
1. Publicada la pauta (`pubPau`), se registran mediciones a lo largo del tiempo:
   - cualquier canal: `insMet["id_pauta","alcance","impresiones","interacciones","clics","compartidos"]`
     (los datos se copian de Meta Business Suite, YouTube Studio, el rating de TV, etc.);
   - YouTube: `capMet["id_pauta"]` o `capMet["*"]`; además `App` lo hace **solo** junto con el scraping (cada
     `scraping.intervalo.horas`). Completar alcance/clics/compartidos con `modMet` si se conocen.
2. Análisis: `anaPau` (una pauta), `anaCam` (la campaña completa, vista útil para el cliente) y `anaRed` (una red social).

Indicadores (calculados en `NAnalisis`; base = alcance si se conoce, si no impresiones):

| Indicador | Fórmula |
|---|---|
| Tasa de interacción | interacciones / base × 100 |
| CTR | clics / impresiones × 100 |
| Frecuencia | impresiones / alcance (solo pautas que informan alcance) |
| Tasa de viralidad | compartidos / base × 100 |
| Crecimiento | (actual − anterior) / anterior × 100 |
| CPM | inversión / impresiones × 1000 |
| Costo por clic / por interacción | inversión / clics · inversión / interacciones |
| Interacción sobre la comunidad | interacciones de las pautas / seguidores actuales × 100 |

En campañas y redes se usa la **última medición** de cada pauta (las mediciones son acumuladas) y, en el agregado, cada
pauta aporta su alcance o, si no lo informa (YouTube), sus impresiones. Si un denominador es 0 el indicador sale "sin datos".

**CU8 — cómo se arma un reporte:**
- `DReporte` solo **extrae las filas del periodo** (proformas por fecha de emisión, campañas que se superponen con el
  periodo, espacios y pautas por fecha, material por fecha de carga, detecciones por fecha, etc.).
- `NReporte` **agrupa y calcula** todo (incluida la regla "pendiente con validez vencida = vencida"):

| Indicador | Fórmula |
|---|---|
| Tasa de conversión | aprobadas / (aprobadas + rechazadas + vencidas) × 100 (las pendientes vigentes no cuentan) |
| Ticket promedio | ingresos aprobados / proformas aprobadas |
| Ocupación | espacios ocupados / espacios del calendario × 100 |
| Tasa de cancelación | pautas canceladas / pautas del periodo × 100 |
| Tasa de aprobación de material | aprobados / (aprobados + rechazados) × 100 |
| Tasa de interacción | interacciones / audiencia (alcance de cada pauta o, si no lo informa, sus impresiones) × 100 |

- `PReporte` arma tablas y **gráficos de barras** con `Html.grafico` (tablas con estilos en línea: se ven en Gmail sin
  imágenes ni JavaScript). `repCli` es la vista pensada para enviar a cada empresa cliente.

---

## 7. Convenciones de código

### Nombres
| Elemento | Convención | Ejemplo |
|---|---|---|
| Paquete raíz | `grupo13` | `grupo13.negocio` |
| Clase de datos | `D` + Entidad (`grupo13.datos`) | `DUsuario`, `DTarifa` |
| Clase de negocio | `N` + Entidad (`grupo13.negocio`) | `NUsuario`, `NTarifa` |
| Clase de presentación | `P` + Entidad (`grupo13.presentacion`) | `PUsuario`, `PTarifa` |
| Métodos `D*` | verbo + objeto, valores tipados | `listar(filtro)`, `obtener(id)`, `insertar(...)`, `cambiarEstado(id, activo)` |
| Métodos `N*` | verbo de negocio, parámetros `String` explícitos | `listar(filtro)`, `obtener(id)`, `insertar(...)`, `modificar(...)`, `eliminar(id)`, `activar(id)` |
| Métodos `P*` | igual que el comando, reciben `List<String>` y devuelven `Respuesta` | `listar(p)`, `insertar(p)`, `agregarTarifa(p)` |
| Comandos | acción(3) + Entidad(3) camelCase | `lisTar`, `insPaq` |
| Idioma | **español** en clases, métodos, variables, mensajes y comentarios | — |

### Reglas
1. **Respetar las 3 capas** (sección 4): P* solo llama a N*; N* solo llama a D*; D* solo a `Conexion`. Pasar la verificación automática antes de cada commit.
2. **Siempre `PreparedStatement`** con `?`. Nunca concatenar valores del usuario en SQL.
3. **Nunca devolver `password_hash`** ni datos sensibles.
4. **Todo texto que vaya al HTML pasa por `Html.esc()`** en la presentación (los parámetros vienen de cualquier remitente). `Html.tabla` y `Html.ficha` ya lo hacen.
5. Errores de negocio → `throw new NegocioException("mensaje claro en español")`, en texto plano y **sin nombres de comandos**. Las sugerencias del tipo "use `actUsu[...]`" se escriben en la clase `P*`.
   Errores del protocolo de comandos (formato, comando inexistente, nº de parámetros) → `ComandoException` en `presentacion.comandos`.
6. Cada método público de `N*` envuelve su cuerpo en `try { ... } catch (SQLException e) { throw NegocioException.bd(e); }`: la presentación nunca ve `SQLException`.
7. Validaciones de formato con `negocio/Validar` (`id`, `idOpcional`, `texto`, `textoOpcional`, `email`, `emailOpcional`, `telefono`, `ci`, `nit`, `password`, `url`, `enteroNoNegativo`, `enteroOpcional`, `monto`, `montoPositivo`, `porcentaje`, `fecha`, `fechaOpcional`, `rangoFechas`).
8. Antes de insertar/modificar, **verificar en negocio** unicidad y existencia/estado de claves foráneas, para dar un mensaje claro en vez de un error SQL.
9. **Cálculos y reglas derivadas en negocio** (nunca en SQL ni en la presentación). Datos solo guarda los valores calculados, en una transacción si son varias escrituras (`Conexion.enTransaccion`).
10. **Formato en presentación:** datos devuelve `t`/`f`, fechas ISO y `NULL`; `Html.formato` los muestra como `Sí`/`No`, `DD/MM/AAAA` y `-`. Etiquetas compuestas (p. ej. "Empresa: X", "Sin asignar") las arma la clase `P*`.
11. **Baja lógica** (`activo = false`) cuando la tabla tiene columna `activo` y/o es referenciada por otras tablas; borrado físico solo en tablas "hoja" o detalle.
12. Contraseñas → `Seguridad.hashPassword()` (PBKDF2-SHA256, 120 000 iteraciones, sal aleatoria). Verificar con `Seguridad.verificarPassword()`.
13. Las clases `D*` usan los ayudantes de `Conexion` (`consultar`, `consultarUno`, `valor`, `ejecutar`, `insertar` con `RETURNING`, `ids`, `estaActivo`, `contar`, `enTransaccion`, `nulo`), que abren y cierran la conexión; no se comparten conexiones.
14. Mismo estilo que el código existente: llaves en la misma línea, 4 espacios, comentarios `/** */` breves por método público.
15. **Sin dependencias nuevas.** Si algo parece necesitar una librería (JSON, HTTP, scraping), usar lo que trae el JDK (`java.net.http.HttpClient`, `java.util.regex`, etc.).

---

## 8. Cómo agregar un comando (paso a paso)

Ejemplo ilustrativo y simplificado: **CU4 – Material publicitario** (`lisMat`). Siempre en este orden: **D → N → P → registrar**.
> El CU4 ya está implementado: la versión completa está en `DMaterial`, `NMaterial` y `PMaterial` (úsela como referencia de un CU con estados, versiones y revisiones).

**1. Datos** — `src/grupo13/datos/DMaterial.java` (SQL y filas crudas, nada más)
```java
/** Fila: id_material, nombre_archivo, tipo, estado, fecha_carga, campania. */
public class DMaterial {

    private static final String SELECT = "SELECT m.id_material, m.nombre_archivo, m.tipo, m.estado, m.fecha_carga, c.nombre"
            + " FROM material_publicitario m JOIN campania c ON c.id_campania = m.id_campania";

    public List<String[]> listar(String filtro) throws SQLException {
        if (filtro == null) {
            return Conexion.consultar(SELECT + " ORDER BY m.id_material");
        }
        String like = "%" + filtro + "%";
        return Conexion.consultar(SELECT + " WHERE m.nombre_archivo ILIKE ? OR c.nombre ILIKE ? ORDER BY m.id_material", like, like);
    }
}
```

**2. Negocio** — `src/grupo13/negocio/NMaterial.java` (validaciones, reglas y cálculos; devuelve datos)
```java
/** CU4 / RF08. Fila: id_material, nombre_archivo, tipo, estado, fecha_carga, campania. */
public class NMaterial {

    private final DMaterial datos = new DMaterial();

    public List<String[]> listar(String filtro) throws NegocioException {
        try {
            return datos.listar(filtro);
        } catch (SQLException e) {
            throw NegocioException.bd(e);
        }
    }
}
```

**3. Presentación** — `src/grupo13/presentacion/PMaterial.java` (columnas, mensajes y HTML)
```java
/** Presentación del CU4 – Material: lisMat. */
public class PMaterial {

    private static final String[] COLUMNAS = {"ID", "Archivo", "Tipo", "Estado", "Fecha de carga", "Campaña"};
    private final NMaterial negocio = new NMaterial();

    /** lisMat["*"] */
    public Respuesta listar(List<String> p) throws NegocioException {
        return Vista.listado("Listando el Material", "Material Publicitario", COLUMNAS, negocio.listar(Vista.filtro(p.get(0))));
    }
}
```

**4. Registro** — en `presentacion/comandos/Despachador.java`
```java
PMaterial material = new PMaterial();

grupo("CU4: Material publicitario (RF08)");
registrar("lisMat", "Lista el material publicitario (\"*\" = todo)",
        new String[]{"filtro"}, "lisMat[\"*\"]", material::listar);
```
`registrar(nombre, descripción, nombresDeParámetros, ejemplo, métodoP)`. Con esto el comando ya:
- aparece en `help` dentro de su grupo,
- valida la cantidad de parámetros y muestra el ejemplo si está mal,
- convierte `NegocioException` y `ComandoException` en una respuesta "Error".

**5. Verificar y probar**
```
grep ... (verificación automática de la sección 4: no debe imprimir nada)
java -cp "out;lib/*" grupo13.App --probar "lisMat[\"*\"]"
```
Probar el caso correcto **y** los casos de error (parámetros de menos, ID inexistente, FK inexistente, duplicado).

**6. Documentar** — agregar el comando a la tabla de la sección 6 y actualizar el estado en la sección 3.

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
| **CU2** ✅ | Implementado (ver sección 6). | `canal_difusion` + `red_social` en transacción; bajas lógicas y seguidores. |
| **CU3** ✅ | Implementado (ver sección 6). | Totales y precios se recalculan en SQL dentro de transacciones; cada ítem de proforma congela su precio unitario. |
| **CU4** ✅ | Implementado (ver sección 6). | Material por URL; la duración máxima sale de las tarifas contratadas (directas y de paquetes); cada revisión queda en `revision_material`. |
| **CU5** ✅ | Implementado (ver sección 6). | La notificación se guarda en la misma transacción que la pauta; el correo lo envía la presentación (`Notificador`) y si falla el aviso igual queda en el sistema. |
| **CU6** ✅ (YouTube) | Implementado (ver sección 6). Pendiente: Facebook y TikTok. | Agregar otra plataforma = una fuente `D<Plataforma>` que devuelva las mismas filas que `DYoutube` y su nombre en `NScraping.SOPORTADAS`. |
| **CU7** ✅ | Implementado (ver sección 6). | Los indicadores se calculan en negocio (`NAnalisis`); SQL solo trae la última medición de cada pauta. Los reportes globales por periodo quedan para el CU8. |
| **CU8** ✅ | Implementado (ver sección 6). | Datos solo filtra por periodo; negocio agrupa y calcula; presentación arma tablas y gráficos. |

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
scraping.intervalo.horas=6     # 0 = solo manual (ejeScr)
scraping.paginas=3             # páginas de resultados de YouTube por búsqueda
alertas.rol=Administrador      # rol que recibe las alertas del CU6
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
| Avisos del RF11 por correo | Se envían con el mismo SMTP; desde redes no autorizadas fallan (relay denegado), pero el aviso queda en `notificacion` y la respuesta lo indica. En `--probar` no se envían. | Ejecutar desde la red de la facultad o en el servidor. |
| Si falla el SMTP | El correo igual se borra del buzón y la respuesta se pierde (queda en el log). | Revisar la consola. |
| Datos compartidos | Todos usan la misma `db_grupo13sa`. | Usar datos de prueba identificables y borrarlos al terminar. |
| Archivos (CU4) | El material se registra por URL (el esquema no tiene dónde guardar el archivo); no se leen adjuntos MIME. | Extensión futura en `presentacion/correo/ClientePOP3` (multipart). |
| Scraping (CU6) | Facebook y TikTok bloquean bots y exigen login, por eso solo está YouTube. YouTube puede cambiar el formato de su página o limitar consultas automáticas: entonces la ejecución queda en `error` con el motivo en `mensaje_error`. Leer resultados de búsqueda de forma automatizada puede ir contra los términos de uso de la plataforma; mantener volúmenes bajos (`scraping.paginas`, `scraping.intervalo.horas`). | Ajustar `DYoutube` si cambia el formato; revisar `lisEje`. |

---

## 14. Reglas para agentes de IA

Si eres un agente trabajando en este repositorio:

1. **No agregues frameworks ni dependencias.** Solo JDK 17 + `lib/postgresql-42.7.4.jar`.
2. **No modifiques el esquema de la base de datos** ni ejecutes `sql/schema.sql`: las tablas ya existen. Si un CU requiere cambiarlo, propón el cambio y espera confirmación.
3. **No ejecutes `grupo13.App` sin `--probar`** salvo que te lo pidan: lee y **borra** los correos reales del buzón y envía respuestas.
4. **Respeta estrictamente las 3 capas** (sección 4): sigue el procedimiento de la sección 8 (D* → N* → P* → registrar en `Despachador`) y ejecuta la verificación automática; no debe imprimir nada.
5. Nombres de comandos: acción(3) + Entidad(3) en camelCase. Confirma con el usuario los nombres nuevos si no están en la sección 10.
6. Prueba cada comando con `--probar`, incluidos los casos de error, y **elimina los datos de prueba** que insertes (y reinicia la identidad si la tabla quedó vacía: `ALTER TABLE t ALTER COLUMN id RESTART WITH 1`).
7. Mensajes al usuario en **español**: los de negocio en texto plano y sin nombres de comandos; la presentación los escapa con `Html.esc()`.
8. Al terminar, actualiza este README: tabla de comandos (sección 6), estado del CU (sección 3) y hoja de ruta (sección 10).
9. Nunca escribas credenciales en archivos versionados; usa `config.properties`.
