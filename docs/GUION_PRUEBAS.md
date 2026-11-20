# Guion de pruebas por correo

Prueba de punta a punta del sistema, como en producción: cada paso es **un correo** enviado a
`grupo13sa@tecnoweb.org.bo` con el comando en el **asunto** (el cuerpo puede ir vacío). El sistema responde al
remitente con *Éxito*, un listado, *Confirmación requerida* o *Error*.

Parte de los datos cargados con [sql/datos.sql](../sql/datos.sql). Los IDs de este guion suponen que, después de
cargarlos, todavía no se creó nada desde el correo; si ya se crearon registros, use el ID que devuelva cada respuesta.

## Preparación

1. `App` corriendo en una red desde la que el SMTP de la facultad acepte enviar (red de la facultad o el servidor);
   desde una red de casa las respuestas fallan con *550 Relaying denied*.
2. Seis buzones reales, uno por actor. En las tablas se nombran así:

   | Clave | Quién | Usuario en la base |
   |---|---|---|
   | **ADM** | Administrador | 1 – Elias Puma Claure (`eliaspumaclaure48@gmail.com`) |
   | **DIR** | Director | 3 – Carlos Alberto Mendoza Rojas |
   | **ENC** | Encargada de la FICCT (responsable de la red 7, Facebook FICCT) | 4 – Ana Gabriela Vaca Suárez |
   | **CLI1** | Cliente de *Panificadora La Espiga Dorada* (empresa 1, campaña 1) | 7 – Roberto Carlos Saucedo Ortiz |
   | **CLI2** | Cliente de *Constructora Piraí Norte* (empresa 4, proforma 4 pendiente) | 10 – Daniela Ribera Cuéllar |
   | **VIS** | Visitante: un correo que **no** esté registrado | — |

3. **Confirmaciones (✱):** las operaciones críticas no se ejecutan al primer correo. El sistema responde
   *Confirmación requerida* con un código de 6 dígitos; se responde con otro correo con asunto `conf["código"]`
   desde **el mismo buzón**, dentro de 15 minutos. En las tablas, `conf["…"]` es ese paso.

Columna **Esperado**: lo que debe decir la respuesta. Si algo no coincide, anote el número de paso y la respuesta.

---

## 0. Correos reales para cada rol (CU1, `modUsu` ✱)

Formato: `modUsu["id","nombre","email","teléfono","password","id_rol","id_unidad"]`; password vacío = no se cambia.

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 0.1 | ADM | `modUsu["3","Carlos Alberto Mendoza Rojas","<correo DIR>","71234567","","2","3"]` | Confirmación requerida con código |
| 0.2 | ADM | `conf["…"]` | Éxito: usuario 3 con el correo nuevo |
| 0.3 | ADM | `modUsu["4","Ana Gabriela Vaca Suárez","<correo ENC>","72145896","","3","1"]` → `conf["…"]` | Éxito |
| 0.4 | ADM | `modUsu["7","Roberto Carlos Saucedo Ortiz","<correo CLI1>","75478963","","4",""]` → `conf["…"]` | Éxito |
| 0.5 | ADM | `modUsu["10","Daniela Ribera Cuéllar","<correo CLI2>","78741236","","4",""]` → `conf["…"]` | Éxito |
| 0.6 | ADM | `conf["…"]` (repetir un código ya usado) | Error: el código no existe, ya fue usado o no corresponde a su correo |

## 1. Identificación y permisos (RF02)

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 1.1 | ADM | `help` | Todos los comandos, agrupados por CU; los críticos marcados con ✱ |
| 1.2 | DIR | `help` | Todo menos administrar usuarios y roles (no aparecen `insUsu`, `modUsu`, `eliUsu`, `actUsu`, `insRol`…) |
| 1.3 | ENC | `help` | Solo `lisRed`, `mosRed`, `insSeg`, `lisSeg`, `lisPau`, `mosPau`, `anaRed`, `lisEsp`, `disEsp`, `lisNot`, `leeNot` |
| 1.4 | CLI1 | `help` | Catálogo, cotizar, su empresa, campañas, material, pautas, métricas, `repCli` y notificaciones |
| 1.5 | VIS | `help` | Solo catálogo y cotizar |
| 1.6 | VIS | `lisUsu["*"]` | Error: su rol (Visitante) no tiene permiso para usar lisUsu |
| 1.7 | DIR | `insRol["Prueba","No debe crearse"]` | Error: su rol (Director) no tiene permiso |
| 1.8 | ENC | `lisPro["*"]` | Error: sin permiso |
| 1.9 | CLI1 | `tabGen["*","*"]` | Error: sin permiso |
| 1.10 | ADM | `xyzAbc["1"]` | Error: comando no reconocido, sugiere enviar `help` |

## 2. CU1 – Usuarios, roles, unidades y clientes

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 2.1 | ADM | `lisUsu["*"]` | 10 usuarios con su rol y unidad |
| 2.2 | ADM | `lisUsu["Encargado"]` | Solo los 3 encargados de unidad |
| 2.3 | ADM | `mosUsu["4"]` | Ana Gabriela Vaca Suárez, Encargado de Unidad, Facultad de Ciencias de la Computación |
| 2.4 | ADM | `lisRol["*"]` | 4 roles: Administrador, Director, Encargado de Unidad, Cliente |
| 2.5 | DIR | `lisUni["*"]` | 6 unidades con su unidad superior |
| 2.6 | ADM | `insUni["Facultad de Ciencias Económicas","Facultad",""]` | Éxito, ID 7 |
| 2.7 | ADM | `modUni["7","Facultad de Ciencias Económicas, Administrativas y Financieras","Facultad",""]` | Éxito |
| 2.8 | ADM | `modUni["2","Rectorado","Rectorado","3"]` | Error: crearía un ciclo (Canal 11 TVU depende del Rectorado) |
| 2.9 | ADM | `eliUni["7"]` → `conf["…"]` | Éxito: unidad 7 inactiva |
| 2.10 | ADM | `actUni["7"]` → `conf["…"]` | Éxito: unidad 7 activa |
| 2.11 | DIR | `lisRep["*"]` | 4 representantes con su empresa |
| 2.12 | DIR | `mosRep["10"]` | Daniela Ribera Cuéllar, Encargada de Ventas, Constructora Piraí Norte S.A. |

## 3. CU2 – Redes sociales institucionales

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 3.1 | ADM | `lisPla["*"]` | YouTube, Facebook, TikTok, Instagram |
| 3.2 | ADM | `lisRed["*"]` | 5 redes oficiales (4 de YouTube, 1 de Facebook) con responsable y unidad |
| 3.3 | ENC | `lisRed["*"]` | Solo la red 7, FICCT UAGRM (Facebook) |
| 3.4 | ENC | `mosRed["7"]` | Ficha de la red con su último conteo de seguidores (25870) |
| 3.5 | ENC | `mosRed["5"]` | Error: no tiene acceso a esa red (es de otra unidad) |
| 3.6 | ENC | `insSeg["7","26100"]` | Éxito |
| 3.7 | ENC | `insSeg["5","3300"]` | Error: no tiene acceso a esa red |
| 3.8 | ENC | `lisSeg["7"]` | Historial desde julio, el último con 26100 |
| 3.9 | ADM | `insRed["Canal 11 TVU (TikTok) - prueba","https://www.tiktok.com/@canal11tvu.prueba","3","3","3"]` | Éxito, ID 8 |
| 3.10 | ADM | `eliRed["8"]` → `conf["…"]` | Éxito: red 8 dada de baja |
| 3.11 | ADM | `lisRed["*"]` | La red 8 aparece inactiva |

## 4. CU3 – Oferta comercial y cotizaciones

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 4.1 | VIS | `lisSer["*"]` | 6 servicios publicitarios |
| 4.2 | VIS | `lisTar["*"]` | Tarifario con monto, unidad de cobro, duración y vigencia |
| 4.3 | VIS | `mosPaq["2"]` | Paquete Campaña Universitaria: 3 ítems, 15 % de descuento, precio 3825,00 Bs |
| 4.4 | VIS | `insPro["","","","50"]` | Error: solo el personal de Canal 11 puede aplicar descuentos |
| 4.5 | VIS | `insPro["","","",""]` | Éxito: proforma 8 (PRO-2026-00008) a nombre de su correo |
| 4.6 | VIS | `addDet["8","1","","2","semanal"]` | Éxito: 2 spots de 30 s, subtotal 700,00 |
| 4.7 | VIS | `addDet["8","","1","1",""]` | Éxito: + Paquete Emprendedor 1476,00 → total 2176,00 |
| 4.8 | VIS | `mosPro["8"]` | 2 ítems, subtotal 2176,00, total 2176,00, pendiente |
| 4.9 | VIS | `lisPro["*"]` | Solo su proforma 8 |
| 4.10 | VIS | `mosPro["1"]` | Error: no tiene acceso a esa proforma |
| 4.11 | CLI2 | `lisPro["*"]` | Solo la proforma 4 de su empresa |
| 4.12 | CLI2 | `mosPro["1"]` | Error: no pertenece a su empresa |
| 4.13 | CLI2 | `aprPro["4"]` → `conf["…"]` | Éxito: proforma 4 aprobada |
| 4.14 | ADM | `lisPro["*"]` | 8 proformas; la 6 figura **vencida** (validez 16/10/2026) |
| 4.15 | ADM | `modPro["8","176","15/11/2026"]` | Éxito: total 2000,00 (2176 − 176) |
| 4.16 | ADM | `genCam["6","Prueba","01/11/2026","30/11/2026"]` → `conf["…"]` | Error: la proforma 6 no está aprobada |
| 4.17 | ADM | `genCam["4","Lanzamiento Condominio Piraí Norte","05/11/2026","30/11/2026"]` → `conf["…"]` | Éxito: campaña 4 activa |
| 4.18 | CLI1 | `lisCam["*"]` | Solo la campaña 1 (Todos Santos 2026) |
| 4.19 | CLI1 | `mosEmp["1"]` | Su empresa con sus representantes |
| 4.20 | CLI1 | `mosEmp["2"]` | Error: no pertenece a su empresa |
| 4.21 | ADM | `lisEmp["*"]` | 4 empresas anunciantes |

## 5. CU4 – Carga y validación de material

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 5.1 | CLI2 | `insMat["4","video","spot_condominio.mp4","https://drive.google.com/file/d/pirai-spot","90",""]` | Error: 90 s supera el máximo contratado en la campaña (60 s) |
| 5.2 | CLI2 | `insMat["4","video","spot_condominio.wav","https://drive.google.com/file/d/pirai-spot","60",""]` | Error: formato no válido para video (mp4, mov, avi, mkv) |
| 5.3 | CLI2 | `insMat["4","video","spot_condominio_60s.mp4","https://drive.google.com/file/d/pirai-spot-60s","60",""]` | Éxito: material 10, pendiente, versión 1 |
| 5.4 | CLI1 | `mosMat["10"]` | Error: no pertenece a su empresa |
| 5.5 | DIR | `lisMat["pendiente"]` | Materiales 3, 8 y 10 |
| 5.6 | DIR | `corMat["10","Agregue el logotipo de la empresa al cierre del spot"]` → `conf["…"]` | Éxito: material 10 en corrección |
| 5.7 | CLI2 | `subMat["10","spot_condominio_60s_v2.mp4","https://drive.google.com/file/d/pirai-spot-60s-v2","58",""]` | Éxito: versión 2, pendiente |
| 5.8 | DIR | `aprMat["10","Cumple los estándares técnicos"]` → `conf["…"]` | Éxito: material 10 aprobado |
| 5.9 | CLI2 | `lisRev["10"]` | 2 revisiones: corrección y aprobado, con revisor y fecha |
| 5.10 | DIR | `aprMat["3","Texto aprobado para redes"]` → `conf["…"]` | Éxito: material 3 (texto) aprobado |
| 5.11 | DIR | `aprMat["9",""]` → `conf["…"]` | Error: el material 9 está rechazado (rechazo definitivo) |

## 6. CU5 – Programación de pauta y notificaciones

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 6.1 | ADM | `lisCan["*"]` | 7 canales: señal abierta, portal web y las redes sociales |
| 6.2 | DIR | `disEsp["1","05/11/2026","30/11/2026"]` | Espacios libres de la señal abierta: 14, 15, 16, 17, 19 y 20 (el 18 está bloqueado) |
| 6.3 | ENC | `disEsp["*","01/11/2026","30/11/2026"]` | Calendario libre de noviembre |
| 6.4 | DIR | `insPau["7","10","18"]` | Error: el espacio 18 está bloqueado |
| 6.5 | DIR | `insPau["7","10","1"]` | Error: el espacio 1 está ocupado / ya pasó |
| 6.6 | DIR | `insPau["7","2","15"]` | Error: el ítem 7 no pertenece a la proforma de esa campaña |
| 6.7 | DIR | `insPau["7","10","15"]` | Éxito: pauta 14 el 06/11/2026 20:00 en la señal abierta |
| 6.8 | DIR | `repPau["14","16"]` → `conf["…"]` | Éxito: pauta 14 pasa al 12/11/2026; el espacio 15 queda libre |
| 6.9 | DIR | `insPau["5","7","23"]` | Éxito: pauta 15 el 06/11/2026 12:00 en FICCT UAGRM (Facebook) |
| 6.10 | ENC | *(revisar su bandeja)* | Llega el correo "Nueva pauta programada en FICCT UAGRM (Facebook)" (RF11) |
| 6.11 | ENC | `lisNot["*"]` | 4 avisos; los 2 últimos sin leer |
| 6.12 | ENC | `leeNot["4"]` | Éxito: aviso 4 marcado como leído |
| 6.13 | ENC | `lisPau["*"]` | Solo las pautas de su red: 3, 9, 11 y 15 |
| 6.14 | ENC | `mosPau["1"]` | Error: no tiene acceso (es de la señal abierta) |
| 6.15 | CLI1 | `lisPau["*"]` | Solo las de su campaña: 7, 8, 9, 10 y 11 |
| 6.16 | DIR | `pubPau["10",""]` | Error: la pauta 10 es del 03/11/2026; aún no puede marcarse como publicada |
| 6.17 | DIR | `canPau["13"]` → `conf["…"]` | Éxito: pauta 13 cancelada y espacio 13 libre |
| 6.18 | ADM | `genEsp["2","23/11/2026","27/11/2026","08:00","23:59"]` | Éxito: 5 espacios en el portal web |
| 6.19 | ADM | `eliEsp["19"]` → `conf["…"]` | Éxito: espacio 19 bloqueado |
| 6.20 | ADM | `actEsp["19"]` → `conf["…"]` | Éxito: espacio 19 disponible otra vez |

## 7. CU6 – Monitoreo y páginas no autorizadas (YouTube)

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 7.1 | ADM | `lisTer["*"]` | 5 términos activos |
| 7.2 | ADM | `insTer["Canal 11 Santa Cruz","Nombre"]` | Éxito, ID 6 |
| 7.3 | ADM | `ejeScr["1","3"]` | Resumen de la ejecución con el término "Canal 11 TVU": cuentas revisadas, oficiales omitidas y páginas no autorizadas nuevas (tarda hasta un minuto) |
| 7.4 | ADM | *(revisar su bandeja)* | Si hubo páginas nuevas, llega la alerta de páginas no autorizadas |
| 7.5 | ADM | `lisEje["*"]` | La ejecución anterior como completada |
| 7.6 | ADM | `lisPag["detectada"]` | Páginas detectadas. Ninguna de las 5 redes oficiales debe aparecer |
| 7.7 | ADM | `mosPag["<id de 7.6>"]` | Ficha con URL, plataforma y evidencias (término y texto donde apareció) |
| 7.8 | ADM | `modPag["<id>","eliminada"]` → `conf["…"]` | Error: de detectada solo puede pasar a confirmada |
| 7.9 | ADM | `modPag["<id>","confirmada"]` → `conf["…"]` | Éxito: página confirmada, con revisor y fecha |
| 7.10 | ADM | `modPag["<id>","denunciada"]` → `conf["…"]` | Éxito |
| 7.11 | ADM | `ejeScr["1","*"]` | Ejecución con todos los términos activos |

## 8. CU7 – Métricas e impacto

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 8.1 | ADM | `lisMet["3"]` | 2 mediciones de la pauta 3 (Facebook) |
| 8.2 | ADM | `insMet["12","40500","40500","0","0","0"]` | Éxito: nueva medición de la pauta 12 |
| 8.3 | ADM | `insMet["10","100","100","0","0","0"]` | Error: la pauta 10 está programada; solo se miden pautas publicadas |
| 8.4 | ADM | `anaPau["9"]` | Indicadores de la pauta 9: alcance, impresiones, tasa de interacción, CTR |
| 8.5 | ADM | `anaCam["2"]` | Expo Cupesí 2026: totales por canal y costo por resultado |
| 8.6 | ENC | `anaRed["7"]` | Crecimiento de seguidores de la red 7 y rendimiento de sus pautas |
| 8.7 | ENC | `anaRed["3"]` | Error: no tiene acceso a esa red |
| 8.8 | CLI1 | `anaCam["1"]` | Análisis de su campaña Todos Santos 2026 |
| 8.9 | CLI1 | `anaCam["2"]` | Error: no pertenece a su empresa |
| 8.10 | ADM | `capMet["*"]` | No hay pautas publicadas en YouTube para capturar (todas las publicadas son de TV o Facebook) |

## 9. CU8 – Reportes y estadísticas

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 9.1 | ADM | `tabGen["01/07/2026","31/12/2026"]` | Tablero: ingresos, conversión, campañas activas, pautas, impacto y alertas |
| 9.2 | DIR | `repCom["01/06/2026","31/12/2026"]` | Proformas por estado (aprobadas, pendientes, vencida, rechazada), ingresos por mes y principales clientes |
| 9.3 | DIR | `repSer["*","*"]` | Servicios y paquetes más vendidos con gráfico de barras |
| 9.4 | DIR | `repOcu["01/11/2026","30/11/2026"]` | Ocupación de noviembre por canal |
| 9.5 | DIR | `repMat["*","*"]` | Material por estado y tipo, tasa de aprobación y revisiones por revisor |
| 9.6 | DIR | `repImp["01/08/2026","31/10/2026"]` | Impacto por canal y campaña; las pautas con más interacción |
| 9.7 | DIR | `repRed["01/07/2026","31/10/2026"]` | Crecimiento de seguidores de las 5 redes |
| 9.8 | DIR | `repMon["*","*"]` | Detecciones por plataforma, término y estado (incluye las del punto 7) |
| 9.9 | CLI1 | `repCli["1","*","*"]` | Vista de su empresa: inversión, campañas e impacto (CPM, costo por interacción) |
| 9.10 | CLI1 | `repCli["2","*","*"]` | Error: no pertenece a su empresa |
| 9.11 | ENC | `tabGen["*","*"]` | Error: sin permiso |

## 10. El visitante se vuelve cliente (CU1, `insUsu` ✱)

| # | Envía | Asunto | Esperado |
|---|---|---|---|
| 10.1 | ADM | `insUsu["Nombre del visitante","<correo VIS>","70011223","Clave.2026","4",""]` → `conf["…"]` | Éxito: usuario 11 con rol Cliente |
| 10.2 | ADM | `insRep["11","5566778 SC","Gerente de Proyectos","4"]` → `conf["…"]` | Éxito: representante de Constructora Piraí Norte |
| 10.3 | VIS | `help` | Ahora muestra los comandos de Cliente |
| 10.4 | VIS | `lisPro["*"]` | La proforma 4 de su nueva empresa (su proforma de visitante 8 ya no la ve: era a nombre del visitante) |
| 10.5 | ADM | `eliUsu["11"]` → `conf["…"]` | Éxito: usuario 11 inactivo |
| 10.6 | VIS | `help` | Cuenta inactiva: no puede usar ningún comando |
| 10.7 | ADM | `actUsu["11"]` → `conf["…"]` | Éxito: vuelve a estar activo |

---

## Resultado

| Sección | Pasos | OK | Observaciones |
|---|---|---|---|
| 0. Correos reales | 6 | | |
| 1. Permisos (RF02) | 10 | | |
| 2. CU1 | 12 | | |
| 3. CU2 | 11 | | |
| 4. CU3 | 21 | | |
| 5. CU4 | 11 | | |
| 6. CU5 | 20 | | |
| 7. CU6 | 11 | | |
| 8. CU7 | 10 | | |
| 9. CU8 | 11 | | |
| 10. Visitante → cliente | 7 | | |

**Nota:** el guion cambia los datos (nuevas proformas, pautas, material, usuarios). Para repetirlo desde cero hay
que volver a dejar la base como estaba después de `sql/datos.sql`.
