-- =====================================================================
-- DATOS INICIALES - Sistema de gestión publicitaria Canal 11 / UAGRM
-- Grupo 13 SA - Tecnología Web (INF513)
--
-- Uso: sobre una base VACÍA, después de sql/schema.sql:
--   psql -h db.tecnoweb.org.bo -U grupo13sa -d db_grupo13sa -f sql/schema.sql
--   psql -h db.tecnoweb.org.bo -U grupo13sa -d db_grupo13sa -f sql/datos.sql
-- Todo va en una sola transacción: si algo falla, no se guarda nada.
--
-- Las fechas son relativas al día en que se ejecuta (CURRENT_DATE): siempre hay
-- historial de los últimos meses y espacios libres en los próximos días, así
-- que se pueden programar pautas desde el correo apenas se carga.
--
-- Usuarios: 1 y 2 administradores; 3 Director; 4-6 Encargados de Unidad;
-- 7-10 Clientes. Contraseña inicial de todos: Canal11.2026 (el sistema
-- identifica al usuario por su correo; la contraseña no se usa para ingresar).
-- Correos @example.com: dominio reservado que no entrega correo. Antes de
-- ejecutar, reemplácelos por correos reales para probar cada rol.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- CU1: Roles (los nombres deben ser exactamente estos: los usa Permisos.java)
-- ---------------------------------------------------------------------
INSERT INTO rol (id_rol, nombre, descripcion, activo) VALUES
(1, 'Administrador',       'Acceso total al sistema', TRUE),
(2, 'Director',            'Director de Canal 11: gestiona la operación comercial, la pauta, el monitoreo y los reportes', TRUE),
(3, 'Encargado de Unidad', 'Encargado de una unidad universitaria: consulta sus redes sociales, su pauta y sus avisos', TRUE),
(4, 'Cliente',             'Representante de una empresa anunciante: cotiza, carga material y ve el impacto de sus campañas', TRUE);

-- ---------------------------------------------------------------------
-- CU1: Unidades universitarias
-- ---------------------------------------------------------------------
INSERT INTO unidad_universitaria (id_unidad, nombre, tipo, activo, id_unidad_padre) VALUES
(1, 'Facultad de Ciencias de la Computación',        'Facultad',              TRUE, NULL),
(2, 'Rectorado',                                     'Rectorado',             TRUE, NULL),
(3, 'Canal 11 TVU',                                  'Medio de comunicación', TRUE, 2),
(4, 'Carrera de Ingeniería Informática',             'Carrera',               TRUE, 1),
(5, 'Facultad de Humanidades',                       'Facultad',              TRUE, NULL),
(6, 'Carrera de Ciencias de la Comunicación Social', 'Carrera',               TRUE, 5);

-- ---------------------------------------------------------------------
-- CU1: Usuarios
-- ---------------------------------------------------------------------
INSERT INTO usuario (id_usuario, nombre_completo, email, telefono, password_hash, activo, fecha_registro, id_rol, id_unidad) VALUES
(1,  'Elias Puma Claure',            'eliaspumaclaure48@gmail.com', '71055123',
     'pbkdf2$120000$4gYrW70DsxDx3gAWJ+g+EA==$tn6L452GLQCQkG+36afUrd0zW7SzuSgsoDyTwbROOM8=', TRUE, (CURRENT_DATE - 151 + TIME '08:00'), 1, 1),
(2,  'Maykol Villazon',              'lokyamvf15@gmail.com',        '0000000',
     'pbkdf2$120000$qxuKrQdzRxZ6rVEUjIYG0w==$y4shHOPXKnxV38V8XfLul/xYghfJIFTArsqogf9LZf0=', TRUE, (CURRENT_DATE - 151 + TIME '08:00'), 1, NULL),
(3,  'Carlos Alberto Mendoza Rojas', 'carlos.mendoza@example.com',  '71234567',
     'pbkdf2$120000$xZhDIyyr6YWhlMna8Ld7zA==$SuKh2rQTbEy5OOw5em4xKFpJ/1qAi5wGd5a6y0Wi+Ys=', TRUE, (CURRENT_DATE - 121 + TIME '08:00'), 2, 3),
(4,  'Ana Gabriela Vaca Suárez',     'ana.vaca@example.com',        '72145896',
     'pbkdf2$120000$tjFGcfTeQFYnEKWyBxl6nw==$yBM43IwkBEL8gobSH6Ye6Z9UUIjiyvrCbzopHouiQAM=', TRUE, (CURRENT_DATE - 121 + TIME '08:30'), 3, 1),
(5,  'Luis Fernando Rojas Áñez',     'luis.rojas@example.com',      '73258741',
     'pbkdf2$120000$HqSk2LR1Wu/BHLuzbHqxDw==$sy4q9hgyjfisAh+92AioV/swxFJlJTS8ttQAGdwZZTc=', TRUE, (CURRENT_DATE - 120 + TIME '09:00'), 3, 4),
(6,  'Patricia Justiniano Rivero',   'patricia.justiniano@example.com', '74369852',
     'pbkdf2$120000$3smoERB+dH2o0uwdP2WSqw==$LvMNZ3q9kuFY0AS3zTpjMKq0OWEEXY8pB8d78HbwpS0=', TRUE, (CURRENT_DATE - 120 + TIME '09:30'), 3, 6),
(7,  'Roberto Carlos Saucedo Ortiz', 'roberto.saucedo@example.com', '75478963',
     'pbkdf2$120000$E58GkrIZ0BeZphknCC+MZg==$MW7bW41KqXttybSYQFG9RfsBdbN7m+AYLzfycv4Ea+A=', TRUE, (CURRENT_DATE - 147 + TIME '10:00'), 4, NULL),
(8,  'Mariela Durán Paz',            'mariela.duran@example.com',   '76587412',
     'pbkdf2$120000$9cUsKJuFXRO5MY/gLQ079w==$2cwQyC4M5ysQY0DOHo+Pl1IcsIWHjdKSEPDzQSkcEGA=', TRUE, (CURRENT_DATE - 107 + TIME '11:00'), 4, NULL),
(9,  'Javier Ignacio Paz Molina',    'javier.paz@example.com',      '77698523',
     'pbkdf2$120000$UdrkKQPCSl10MCWp/ZVUFA==$4U6JvzQnWvtFvb9NH752oAMWhNnp8TOa70bUWrCQI+I=', TRUE, (CURRENT_DATE - 29 + TIME '15:00'), 4, NULL),
(10, 'Daniela Ribera Cuéllar',       'daniela.ribera@example.com',  '78741236',
     'pbkdf2$120000$vBryGByN4tyD2UirbV8KBQ==$z63FuH1ICy2lmPLEPYXFQMGVnRucP8CIMHFWsOnpRKc=', TRUE, (CURRENT_DATE - 6 + TIME '16:00'), 4, NULL);

-- ---------------------------------------------------------------------
-- CU3: Empresas anunciantes y sus representantes (clientes)
-- ---------------------------------------------------------------------
INSERT INTO empresa_anunciante (id_empresa, razon_social, nit, nombre_comercial, direccion_facturacion, email_facturacion, telefono, activo, fecha_registro) VALUES
(1, 'Panificadora La Espiga Dorada S.R.L.', '1023456021', 'La Espiga Dorada',
    'Av. Cristo Redentor, entre 4to y 5to anillo, Santa Cruz de la Sierra', 'facturacion@espigadorada.example.com', '33456789', TRUE, (CURRENT_DATE - 147 + TIME '10:00')),
(2, 'Agroindustrias Cupesí S.A.',           '2045678015', 'Cupesí',
    'Parque Industrial, Manzana 7, Santa Cruz de la Sierra',               'contabilidad@cupesi.example.com',      '33527841', TRUE, (CURRENT_DATE - 107 + TIME '11:00')),
(3, 'Instituto Técnico Horizonte S.R.L.',   '3078901026', 'Horizonte',
    'Calle Junín N° 245, zona central, Santa Cruz de la Sierra',           'admin@horizonte.example.com',          '33369874', TRUE, (CURRENT_DATE - 29 + TIME '15:00')),
(4, 'Constructora Piraí Norte S.A.',        '4011223019', 'Piraí Norte',
    'Av. Banzer km 7, Santa Cruz de la Sierra',                            'ventas@pirainorte.example.com',        '33985214', TRUE, (CURRENT_DATE - 6 + TIME '16:00'));

INSERT INTO representante (id_usuario, ci, cargo, id_empresa) VALUES
(7,  '7845123 SC', 'Gerente de Marketing',  1),
(8,  '6123987 SC', 'Jefa de Comunicación',  2),
(9,  '8456321 SC', 'Director Académico',    3),
(10, '9012345 SC', 'Encargada de Ventas',   4);

-- Visitantes (cotizaron sin cuenta)
INSERT INTO visitante (id_visitante, nombre, email, telefono, fecha_registro) VALUES
(1, 'María Fernanda Suárez Gil', 'maria.suarez@example.com', '76543210', (CURRENT_DATE - 29 + TIME '09:15')),
(2, 'Jorge Antelo Justiniano',   'jorge.antelo@example.com', '70125896', (CURRENT_DATE - 2 + TIME '17:40'));

-- ---------------------------------------------------------------------
-- CU2: Plataformas, canales de difusión y redes sociales oficiales
-- Las URL de YouTube son canales reales encontrados con el mismo scraping
-- del sistema. Todo lo que el scraping encuentre y NO esté aquí se marca
-- como página no autorizada: confirme que estas son las oficiales.
-- ---------------------------------------------------------------------
INSERT INTO plataforma (id_plataforma, nombre) VALUES
(1, 'YouTube'),
(2, 'Facebook'),
(3, 'TikTok'),
(4, 'Instagram');

INSERT INTO canal_difusion (id_canal, tipo, nombre, fecha_alta, activo) VALUES
(1, 'television', 'Canal 11 TVU - Señal abierta',                (CURRENT_DATE - 2494), TRUE),
(2, 'web',        'Portal web Canal 11 TVU',                     (CURRENT_DATE - 1704), TRUE),
(3, 'red_social', 'Canal 11 TVU - UAGRM (YouTube)',              (CURRENT_DATE - 121), TRUE),
(4, 'red_social', 'UAGRM Oficial (YouTube)',                     (CURRENT_DATE - 121), TRUE),
(5, 'red_social', 'Ingeniería Informática UAGRM (YouTube)',      (CURRENT_DATE - 120), TRUE),
(6, 'red_social', 'Ciencias de la Comunicación UAGRM (YouTube)', (CURRENT_DATE - 120), TRUE),
(7, 'red_social', 'FICCT UAGRM (Facebook)',                      (CURRENT_DATE - 121), TRUE);

INSERT INTO red_social (id_canal, url, id_plataforma, id_usuario, id_unidad) VALUES
(3, 'https://www.youtube.com/@canal11tvuuagrm',                1, 3,    3),
(4, 'https://www.youtube.com/@UAGRMOFICIAL',                   1, NULL, 2),
(5, 'https://www.youtube.com/@ingenieriainformatica-uagr9800', 1, 5,    4),
(6, 'https://www.youtube.com/@Cs.delacomunicacionUAGRM',       1, 6,    6),
(7, 'https://www.facebook.com/ficct.uagrm',                    2, 4,    1);  -- verifique la URL real de la página

-- Seguidores por fecha (valores de ejemplo; actualícelos con insSeg)
INSERT INTO seguidores_red (seguidores, fecha, id_canal) VALUES
(18200, (CURRENT_DATE - 121), 3), (18650, (CURRENT_DATE - 90), 3), (19120, (CURRENT_DATE - 59), 3), (19710, (CURRENT_DATE - 29), 3), (20180, (CURRENT_DATE), 3),
(41500, (CURRENT_DATE - 121), 4), (42300, (CURRENT_DATE - 90), 4), (43050, (CURRENT_DATE - 59), 4), (44400, (CURRENT_DATE - 29), 4), (45020, (CURRENT_DATE), 4),
( 2950, (CURRENT_DATE - 120), 5), ( 3010, (CURRENT_DATE - 90), 5), ( 3090, (CURRENT_DATE - 59), 5), ( 3180, (CURRENT_DATE - 29), 5), ( 3240, (CURRENT_DATE), 5),
( 1820, (CURRENT_DATE - 120), 6), ( 1850, (CURRENT_DATE - 90), 6), ( 1905, (CURRENT_DATE - 59), 6), ( 1960, (CURRENT_DATE - 29), 6), ( 2010, (CURRENT_DATE), 6),
(23400, (CURRENT_DATE - 121), 7), (23950, (CURRENT_DATE - 90), 7), (24600, (CURRENT_DATE - 59), 7), (25300, (CURRENT_DATE - 29), 7), (25870, (CURRENT_DATE), 7);

-- ---------------------------------------------------------------------
-- CU3: Servicios, tarifario y paquetes
-- Una tarifa con id_unidad solo se pauta en las redes de esa unidad.
-- ---------------------------------------------------------------------
INSERT INTO servicio_publicitario (id_servicio, nombre, tipo, descripcion, activo) VALUES
(1, 'Spot televisivo',                    'television', 'Spot en la tanda comercial de la señal abierta de Canal 11 TVU', TRUE),
(2, 'Mención en vivo',                    'television', 'Mención del auspiciante leída por el conductor durante un programa en vivo', TRUE),
(3, 'Cintillo en pantalla',               'television', 'Banner (cintillo) en la parte inferior de la pantalla durante la programación', TRUE),
(4, 'Publicación patrocinada en redes',   'red_social', 'Publicación del anunciante en una red social oficial de una unidad universitaria', TRUE),
(5, 'Video patrocinado en YouTube',       'red_social', 'Video del anunciante publicado en el canal de YouTube de Canal 11 TVU', TRUE),
(6, 'Banner en portal web',               'web',        'Banner en la página principal del portal web de Canal 11 TVU', TRUE);

INSERT INTO tarifa (id_tarifa, monto, unidad_cobro, vigente_desde, vigente_hasta, duracion_seg, activo, id_servicio, id_unidad) VALUES
(1, 350.00, 'por emisión',    date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 30,   TRUE,  1, NULL),
(2, 600.00, 'por emisión',    date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 60,   TRUE,  1, NULL),
(3, 150.00, 'por mención',    date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, TRUE,  2, NULL),
(4, 200.00, 'por día',        date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, TRUE,  3, NULL),
(5, 120.00, 'por publicación',date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, TRUE,  4, 1),   -- redes de la FICCT
(6, 100.00, 'por publicación',date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, TRUE,  4, 6),   -- redes de Comunicación Social
(7, 250.00, 'por publicación',date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, 60,   TRUE,  5, 3),   -- YouTube de Canal 11
(8, 180.00, 'por día',        date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + INTERVAL '1 year - 1 day')::date, NULL, TRUE,  6, NULL),
(9, 300.00, 'por emisión',    (date_trunc('year', CURRENT_DATE) - INTERVAL '1 year')::date, (date_trunc('year', CURRENT_DATE) - INTERVAL '1 day')::date, 30,   FALSE, 1, NULL); -- tarifa del año anterior (histórica)

-- precio_total = Σ(monto × cantidad) × (1 − descuento/100)
INSERT INTO paquete (id_paquete, nombre, descripcion, descuento, activo, precio_total) VALUES
(1, 'Paquete Emprendedor',           '4 spots de 30 s y 2 publicaciones en redes de la FICCT',                 10.00, TRUE, 1476.00), -- 1640 × 0,90
(2, 'Paquete Campaña Universitaria', '5 spots de 60 s, 2 videos en YouTube de Canal 11 y 5 días de cintillo', 15.00, TRUE, 3825.00), -- 4500 × 0,85
(3, 'Paquete Digital',               'Publicaciones en redes de la FICCT y Comunicación, y 2 días de banner web', 12.00, TRUE, 897.60); -- 1020 × 0,88

INSERT INTO paquete_item (id_paquete, id_tarifa, cantidad) VALUES
(1, 1, 4), (1, 5, 2),
(2, 2, 5), (2, 7, 2), (2, 4, 5),
(3, 5, 3), (3, 6, 3), (3, 8, 2);

-- ---------------------------------------------------------------------
-- CU3: Proformas (nro = PRO-AAAA-id) y detalle
-- subtotal = Σ subtotal ítems; total = subtotal − descuento (Bs)
-- La 6 queda pendiente con validez pasada: el sistema la muestra "vencida".
-- ---------------------------------------------------------------------
INSERT INTO proforma (id_proforma, nro_proforma, fecha_emision, fecha_validez, descuento, total, estado, subtotal, id_empresa, id_visitante) VALUES
(1, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00001', (CURRENT_DATE - 45), (CURRENT_DATE - 30), 140.00, 2200.00, 'aprobada',  2340.00, 1,    NULL),
(2, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00002', (CURRENT_DATE - 102), (CURRENT_DATE - 87),   0.00, 4425.00, 'aprobada',  4425.00, 2,    NULL),
(3, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00003', (CURRENT_DATE - 25), (CURRENT_DATE - 10),  36.00, 1800.00, 'aprobada',  1836.00, 3,    NULL),
(4, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00004', (CURRENT_DATE - 5), (CURRENT_DATE + 10),   0.00, 2800.00, 'pendiente', 2800.00, 4,    NULL),
(5, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00005', (CURRENT_DATE - 142), (CURRENT_DATE - 127),   0.00,  700.00, 'rechazada',  700.00, 1,    NULL),
(6, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00006', (CURRENT_DATE - 29), (CURRENT_DATE - 14),   0.00,  400.00, 'pendiente',  400.00, NULL, 1),
(7, 'PRO-' || to_char(CURRENT_DATE, 'YYYY') || '-00007', (CURRENT_DATE - 2), (CURRENT_DATE + 13),   0.00,  897.60, 'pendiente',  897.60, NULL, 2);

INSERT INTO detalle_proforma (id_detalle, cantidad, precio_unitario, subtotal, frecuencia, id_proforma, id_tarifa, id_paquete) VALUES
(1,  6,  350.00, 2100.00, '2 por semana', 1, 1,    NULL),
(2,  2,  120.00,  240.00, 'semanal',      1, 5,    NULL),
(3,  1, 3825.00, 3825.00, NULL,           2, NULL, 2),
(4,  4,  150.00,  600.00, '1 por semana', 2, 3,    NULL),
(5,  1, 1476.00, 1476.00, NULL,           3, NULL, 1),
(6,  2,  180.00,  360.00, 'fin de semana',3, 8,    NULL),
(7,  3,  600.00, 1800.00, 'semanal',      4, 2,    NULL),
(8,  5,  200.00, 1000.00, 'diario',       4, 4,    NULL),
(9,  2,  350.00,  700.00, NULL,           5, 1,    NULL),
(10, 4,  100.00,  400.00, 'semanal',      6, 6,    NULL),
(11, 1,  897.60,  897.60, NULL,           7, NULL, 3);

-- Campañas (una por proforma aprobada)
INSERT INTO campania (id_campania, nombre, fecha_inicio, fecha_fin, estado, id_proforma) VALUES
(1, 'Todos Santos 2026 - La Espiga Dorada',   (CURRENT_DATE - 29), (CURRENT_DATE + 11), 'activa',     1),
(2, 'Expo Cupesí 2026',                       (CURRENT_DATE - 90), (CURRENT_DATE - 30), 'finalizada', 2),
(3, 'Inscripciones Gestión 2027 - Horizonte', (CURRENT_DATE - 15), (CURRENT_DATE + 51), 'activa',     3);

-- ---------------------------------------------------------------------
-- CU4: Material publicitario y revisiones (resultado = estado)
-- Video/audio no superan la duración máxima contratada en su campaña.
-- ---------------------------------------------------------------------
INSERT INTO material_publicitario (id_material, tipo, nombre_archivo, url_archivo, formato, tamano_bytes, duracion_seg, texto_publicacion, version, estado, fecha_carga, id_campania, id_usuario) VALUES
(1, 'video',  'spot_todos_santos_30s.mp4',     'https://drive.google.com/file/d/espiga-todos-santos-30s', 'mp4', 48234112, 30, NULL, 1, 'aprobado',  (CURRENT_DATE - 35 + TIME '10:20'), 1, 7),
(2, 'imagen', 'post_tantawawas.png',           'https://drive.google.com/file/d/espiga-post-tantawawas',  'png',  1843200, NULL, 'Este Todos Santos, las tantawawas de La Espiga Dorada llegan a tu mesa. Pedidos en nuestras sucursales.', 1, 'aprobado', (CURRENT_DATE - 35 + TIME '10:25'), 1, 7),
(3, 'texto',  'publicacion.txt',               NULL,                                                      'txt',  NULL,    NULL, 'Recordamos a nuestros clientes que del 30 de octubre al 2 de noviembre atendemos en horario extendido.', 1, 'pendiente', (CURRENT_DATE - 1 + TIME '18:05'), 1, 7),
(4, 'video',  'spot_expo_cupesi_60s.mp4',      'https://drive.google.com/file/d/cupesi-expo-60s-v2',     'mp4', 96468224, 60, NULL, 2, 'aprobado',  (CURRENT_DATE - 93 + TIME '09:40'), 2, 8),
(5, 'video',  'video_expo_cupesi_redes.mp4',   'https://drive.google.com/file/d/cupesi-expo-redes',      'mp4', 61865984, 55, NULL, 1, 'aprobado',  (CURRENT_DATE - 96 + TIME '15:10'), 2, 8),
(6, 'banner', 'cintillo_expo_cupesi.png',      'https://drive.google.com/file/d/cupesi-cintillo',        'png',   512000, NULL, NULL, 1, 'aprobado',  (CURRENT_DATE - 96 + TIME '15:15'), 2, 8),
(7, 'video',  'spot_inscripciones_2027_30s.mp4','https://drive.google.com/file/d/horizonte-spot-30s',     'mp4', 45088768, 30, NULL, 1, 'aprobado',  (CURRENT_DATE - 20 + TIME '11:00'), 3, 9),
(8, 'banner', 'banner_web_inscripciones.jpg',  'https://drive.google.com/file/d/horizonte-banner-web',    'jpg',   734003, NULL, NULL, 1, 'pendiente', (CURRENT_DATE - 2 + TIME '12:30'), 3, 9),
(9, 'imagen', 'afiche_inscripciones.png',      'https://drive.google.com/file/d/horizonte-afiche',       'png',  2306867, NULL, NULL, 1, 'rechazado', (CURRENT_DATE - 20 + TIME '11:05'), 3, 9);

INSERT INTO revision_material (fecha, resultado, observaciones, id_material, id_usuario) VALUES
((CURRENT_DATE - 95 + TIME '10:00'), 'correccion', 'El audio se satura en los últimos 5 segundos; reduzca el volumen y vuelva a subirlo.', 4, 3),
((CURRENT_DATE - 95 + TIME '10:15'), 'aprobado',   'Cumple los estándares técnicos de Canal 11.', 5, 3),
((CURRENT_DATE - 95 + TIME '10:20'), 'aprobado',   NULL, 6, 3),
((CURRENT_DATE - 92 + TIME '09:00'), 'aprobado',   'Versión 2 corregida: audio en niveles correctos.', 4, 3),
((CURRENT_DATE - 34 + TIME '11:00'), 'aprobado',   'Cumple los estándares técnicos de Canal 11.', 1, 3),
((CURRENT_DATE - 34 + TIME '11:10'), 'aprobado',   NULL, 2, 1),
((CURRENT_DATE - 18 + TIME '16:00'), 'aprobado',   'Cumple los estándares técnicos de Canal 11.', 7, 3),
((CURRENT_DATE - 17 + TIME '09:30'), 'rechazado',  'Usa el escudo de la UAGRM sin autorización; no puede difundirse.', 9, 3);

-- ---------------------------------------------------------------------
-- CU5: Espacios publicitarios (calendario) y programación de pautas
-- Pasado: ocupados con pautas publicadas. Noviembre: espacios libres
-- para probar insPau desde el correo.
-- ---------------------------------------------------------------------
INSERT INTO espacio_publicitario (id_espacio, fecha, hora_inicio, hora_fin, estado, id_canal) VALUES
(1,  (CURRENT_DATE - 86), '20:00', '20:02', 'ocupado',    1),
(2,  (CURRENT_DATE - 79), '20:00', '20:02', 'ocupado',    1),
(3,  (CURRENT_DATE - 76), '18:00', '18:30', 'ocupado',    7),
(4,  (CURRENT_DATE - 71), '13:00', '13:02', 'ocupado',    1),
(5,  (CURRENT_DATE - 58), '20:00', '20:02', 'ocupado',    1),
(6,  (CURRENT_DATE - 50), '20:00', '20:02', 'disponible', 1),   -- liberado por la pauta cancelada 6
(7,  (CURRENT_DATE - 10), '20:00', '20:02', 'ocupado',    1),
(8,  (CURRENT_DATE - 3), '20:00', '20:02', 'ocupado',    1),
(9,  (CURRENT_DATE - 8), '12:00', '12:30', 'ocupado',    7),
(10, (CURRENT_DATE - 1), '20:00', '20:02', 'ocupado',    1),   -- ya pasó: la pauta 10 falta marcarla publicada
(11, (CURRENT_DATE + 5), '12:00', '12:30', 'ocupado',    7),
(12, (CURRENT_DATE - 4), '21:00', '21:02', 'ocupado',    1),
(13, (CURRENT_DATE + 11), '21:00', '21:02', 'ocupado',    1),
(14, (CURRENT_DATE + 6), '20:00', '20:02', 'disponible', 1),
(15, (CURRENT_DATE + 7), '20:00', '20:02', 'disponible', 1),
(16, (CURRENT_DATE + 13), '20:00', '20:02', 'disponible', 1),
(17, (CURRENT_DATE + 14), '21:00', '21:02', 'disponible', 1),
(18, (CURRENT_DATE + 16), '20:00', '20:02', 'bloqueado',  1),   -- transmisión especial
(19, (CURRENT_DATE + 18), '13:00', '13:02', 'disponible', 1),
(20, (CURRENT_DATE + 20), '20:00', '20:02', 'disponible', 1),
(21, (CURRENT_DATE + 10), '08:00', '23:59', 'disponible', 2),
(22, (CURRENT_DATE + 17), '08:00', '23:59', 'disponible', 2),
(23, (CURRENT_DATE + 7), '12:00', '12:30', 'disponible', 7),
(24, (CURRENT_DATE + 12), '12:00', '12:30', 'disponible', 7),
(25, (CURRENT_DATE + 8), '18:00', '18:30', 'disponible', 3),
(26, (CURRENT_DATE + 15), '18:00', '18:30', 'disponible', 3),
(27, (CURRENT_DATE + 9), '19:00', '19:30', 'disponible', 5),
(28, (CURRENT_DATE + 13), '19:00', '19:30', 'disponible', 6);

INSERT INTO programacion_pauta (id_programacion, estado, url_publicacion, fecha_programacion, id_detalle, id_material, id_espacio, id_usuario) VALUES
(1,  'publicada',  NULL, (CURRENT_DATE - 86 + TIME '20:00'), 3, 4, 1,    3),
(2,  'publicada',  NULL, (CURRENT_DATE - 79 + TIME '20:00'), 3, 4, 2,    3),
(3,  'publicada',  'https://www.facebook.com/ficct.uagrm/videos/expo-cupesi-2026', (CURRENT_DATE - 76 + TIME '18:00'), 3, 5, 3, 3),
(4,  'publicada',  NULL, (CURRENT_DATE - 71 + TIME '13:00'), 4, 4, 4,    3),
(5,  'publicada',  NULL, (CURRENT_DATE - 58 + TIME '20:00'), 3, 6, 5,    1),
(6,  'cancelada',  NULL, (CURRENT_DATE - 50 + TIME '20:00'), 3, 4, NULL, 3),
(7,  'publicada',  NULL, (CURRENT_DATE - 10 + TIME '20:00'), 1, 1, 7,    3),
(8,  'publicada',  NULL, (CURRENT_DATE - 3 + TIME '20:00'), 1, 1, 8,    3),
(9,  'publicada',  'https://www.facebook.com/ficct.uagrm/posts/tantawawas-espiga-dorada', (CURRENT_DATE - 8 + TIME '12:00'), 2, 2, 9, 3),
(10, 'programada', NULL, (CURRENT_DATE - 1 + TIME '20:00'), 1, 1, 10,   3),
(11, 'programada', NULL, (CURRENT_DATE + 5 + TIME '12:00'), 2, 2, 11,   3),
(12, 'publicada',  NULL, (CURRENT_DATE - 4 + TIME '21:00'), 5, 7, 12,   3),
(13, 'programada', NULL, (CURRENT_DATE + 11 + TIME '21:00'), 5, 7, 13,   3);

-- Avisos a la responsable de la red de la FICCT (RF11)
INSERT INTO notificacion (medio, titulo, mensaje, fecha_envio, leida, id_usuario, id_programacion) VALUES
('correo', 'Nueva pauta programada en FICCT UAGRM (Facebook)',
 'Se programó la publicación del material ''video_expo_cupesi_redes.mp4'' de la campaña ''Expo Cupesí 2026'' (Agroindustrias Cupesí S.A.) para el ' || to_char(CURRENT_DATE - 76 + TIME '18:00', 'DD/MM/YYYY HH24:MI') || ' en FICCT UAGRM (Facebook).',
 (CURRENT_DATE - 88 + TIME '10:00'), TRUE, 4, 3),
('correo', 'Nueva pauta programada en FICCT UAGRM (Facebook)',
 'Se programó la publicación del material ''post_tantawawas.png'' de la campaña ''Todos Santos 2026 - La Espiga Dorada'' (Panificadora La Espiga Dorada S.R.L.) para el ' || to_char(CURRENT_DATE - 8 + TIME '12:00', 'DD/MM/YYYY HH24:MI') || ' en FICCT UAGRM (Facebook).',
 (CURRENT_DATE - 15 + TIME '09:30'), TRUE, 4, 9),
('correo', 'Nueva pauta programada en FICCT UAGRM (Facebook)',
 'Se programó la publicación del material ''post_tantawawas.png'' de la campaña ''Todos Santos 2026 - La Espiga Dorada'' (Panificadora La Espiga Dorada S.R.L.) para el ' || to_char(CURRENT_DATE + 5 + TIME '12:00', 'DD/MM/YYYY HH24:MI') || ' en FICCT UAGRM (Facebook).',
 (CURRENT_DATE - 1 + TIME '16:45'), FALSE, 4, 11);

-- ---------------------------------------------------------------------
-- CU7: Métricas de las pautas publicadas (mediciones acumuladas)
-- TV: alcance = audiencia estimada. Facebook: datos de la página.
-- ---------------------------------------------------------------------
INSERT INTO metrica_publicacion (fecha_medicion, alcance, impresiones, interacciones, clics, compartidos, id_programacion) VALUES
((CURRENT_DATE - 85 + TIME '10:00'), 42000, 42000,    0,   0,   0, 1),
((CURRENT_DATE - 78 + TIME '10:00'), 39500, 39500,    0,   0,   0, 2),
((CURRENT_DATE - 75 + TIME '18:00'),  8200, 11900,  640, 210,  85, 3),
((CURRENT_DATE - 69 + TIME '18:00'), 12600, 18400, 1020, 344, 131, 3),
((CURRENT_DATE - 70 + TIME '10:00'), 27800, 27800,    0,   0,   0, 4),
((CURRENT_DATE - 57 + TIME '10:00'), 40100, 40100,    0,   0,   0, 5),
((CURRENT_DATE - 9 + TIME '10:00'), 45300, 45300,    0,   0,   0, 7),
((CURRENT_DATE - 7 + TIME '12:00'),  6400,  9100,  512, 158,  64, 9),
((CURRENT_DATE - 1 + TIME '12:00'),  9800, 14200,  830, 262,  97, 9),
((CURRENT_DATE - 3 + TIME '10:00'), 38900, 38900,    0,   0,   0, 12),
((CURRENT_DATE - 2 + TIME '10:00'), 46800, 46800,    0,   0,   0, 8);

-- ---------------------------------------------------------------------
-- CU6: Términos de monitoreo
-- ejecucion_scraping, pagina_no_autorizada y deteccion_pagina quedan
-- vacías a propósito: las llena el scraping real (ejeScr o el automático).
-- ---------------------------------------------------------------------
INSERT INTO termino_monitoreo (id_termino, termino, tipo, activo) VALUES
(1, 'UAGRM',               'Sigla',  TRUE),
(2, 'FICCT',               'Sigla',  TRUE),
(3, 'Canal 11 TVU',        'Nombre', TRUE),
(4, 'Gabriel René Moreno', 'Nombre', TRUE),
(5, 'TVU Santa Cruz',      'Marca',  TRUE);

-- ---------------------------------------------------------------------
-- Ajusta los contadores de identidad para que los próximos registros
-- creados desde el sistema continúen después de estos IDs.
-- ---------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('rol', 'id_rol'),                           (SELECT max(id_rol) FROM rol));
SELECT setval(pg_get_serial_sequence('unidad_universitaria', 'id_unidad'),       (SELECT max(id_unidad) FROM unidad_universitaria));
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'),                   (SELECT max(id_usuario) FROM usuario));
SELECT setval(pg_get_serial_sequence('empresa_anunciante', 'id_empresa'),        (SELECT max(id_empresa) FROM empresa_anunciante));
SELECT setval(pg_get_serial_sequence('visitante', 'id_visitante'),               (SELECT max(id_visitante) FROM visitante));
SELECT setval(pg_get_serial_sequence('plataforma', 'id_plataforma'),             (SELECT max(id_plataforma) FROM plataforma));
SELECT setval(pg_get_serial_sequence('canal_difusion', 'id_canal'),              (SELECT max(id_canal) FROM canal_difusion));
SELECT setval(pg_get_serial_sequence('seguidores_red', 'id_seguidores'),         (SELECT max(id_seguidores) FROM seguidores_red));
SELECT setval(pg_get_serial_sequence('servicio_publicitario', 'id_servicio'),    (SELECT max(id_servicio) FROM servicio_publicitario));
SELECT setval(pg_get_serial_sequence('tarifa', 'id_tarifa'),                     (SELECT max(id_tarifa) FROM tarifa));
SELECT setval(pg_get_serial_sequence('paquete', 'id_paquete'),                   (SELECT max(id_paquete) FROM paquete));
SELECT setval(pg_get_serial_sequence('proforma', 'id_proforma'),                 (SELECT max(id_proforma) FROM proforma));
SELECT setval(pg_get_serial_sequence('detalle_proforma', 'id_detalle'),          (SELECT max(id_detalle) FROM detalle_proforma));
SELECT setval(pg_get_serial_sequence('campania', 'id_campania'),                 (SELECT max(id_campania) FROM campania));
SELECT setval(pg_get_serial_sequence('material_publicitario', 'id_material'),    (SELECT max(id_material) FROM material_publicitario));
SELECT setval(pg_get_serial_sequence('revision_material', 'id_revision'),        (SELECT max(id_revision) FROM revision_material));
SELECT setval(pg_get_serial_sequence('espacio_publicitario', 'id_espacio'),      (SELECT max(id_espacio) FROM espacio_publicitario));
SELECT setval(pg_get_serial_sequence('programacion_pauta', 'id_programacion'),   (SELECT max(id_programacion) FROM programacion_pauta));
SELECT setval(pg_get_serial_sequence('metrica_publicacion', 'id_metrica'),       (SELECT max(id_metrica) FROM metrica_publicacion));
SELECT setval(pg_get_serial_sequence('notificacion', 'id_notificacion'),         (SELECT max(id_notificacion) FROM notificacion));
SELECT setval(pg_get_serial_sequence('termino_monitoreo', 'id_termino'),         (SELECT max(id_termino) FROM termino_monitoreo));

COMMIT;
