-- =====================================================================
-- 001: tablas sin dependencias
-- =====================================================================

INSERT INTO Usuario (nombre, email, contrasena_hash, peso_kg, altura_cm,
                     fecha_nacimiento, sexo, fecha_aceptacion_datos, version_politica) VALUES
('Laura Martínez', 'laura.martinez@correo-demo.test', 'hash_ficticio_0001', 68.50, 165.00, '1990-04-12', 'F', current_date - 120, 'v1.0'),
('Carlos Pérez',   'carlos.perez@correo-demo.test',   'hash_ficticio_0002', 92.00, 175.00, '1978-09-03', 'M', current_date - 100, 'v1.0'),
('Andrea Gómez',   'andrea.gomez@correo-demo.test',   'hash_ficticio_0003', 55.00, 160.00, '1995-01-25', 'F', current_date - 90,  'v1.0'),
('Julián Rojas',   'julian.rojas@correo-demo.test',   'hash_ficticio_0004', 78.20, 180.00, '2001-06-30', 'M', current_date - 60,  'v1.1'),
('Marta Ríos',     'marta.rios@correo-demo.test',     'hash_ficticio_0005', 81.40, 158.00, '1962-11-18', 'F', current_date - 45,  'v1.1'),
('Álex Vargas',    'alex.vargas@correo-demo.test',    'hash_ficticio_0006', 70.00, 172.00, '1999-03-09', 'O', current_date - 1,   'v1.1');

INSERT INTO CondicionMedica (nombre, descripcion) VALUES
('Diabetes tipo 2',        'Alteración del metabolismo de la glucosa'),
('Hipertensión arterial',  'Presión arterial elevada de forma sostenida'),
('Dislipidemia',           'Niveles anormales de colesterol o triglicéridos'),
('Artrosis de rodilla',    'Desgaste articular que limita el ejercicio de impacto'),
('Insuficiencia renal',    'Reducción de la función renal');

INSERT INTO Alergia (nombre, descripcion) VALUES
('Lactosa',        'Intolerancia a los lácteos'),
('Gluten',         'Reacción al gluten del trigo, cebada y centeno'),
('Frutos secos',   'Reacción a nueces, almendras y similares'),
('Huevo',          'Alergia a las proteínas del huevo'),
('Mariscos',       'Alergia a crustáceos y moluscos');

INSERT INTO Alimento (nombre, densidad_g_cm3, calorias_100g, proteinas_100g, carbohidratos_100g, grasas_100g) VALUES
('Arroz blanco cocido',        0.850, 130.00,  2.70, 28.00,  0.30),
('Pechuga de pollo a la plancha', 1.050, 165.00, 31.00,  0.00,  3.60),
('Huevo cocido',               1.030, 155.00, 13.00,  1.10, 11.00),
('Leche entera',               1.030,  61.00,  3.20,  4.80,  3.30),
('Pan de trigo',               0.300, 265.00,  9.00, 49.00,  3.20),
('Lentejas cocidas',           0.800, 116.00,  9.00, 20.00,  0.40),
('Aguacate',                   0.950, 160.00,  2.00,  9.00, 15.00),
('Manzana',                    0.800,  52.00,  0.30, 14.00,  0.20),
('Avena en hojuelas',          0.350, 389.00, 17.00, 66.00,  7.00),
('Almendras',                  0.600, 579.00, 21.00, 22.00, 50.00),
('Salmón al horno',            1.050, 206.00, 22.00,  0.00, 12.00),
('Brócoli cocido',             0.600,  35.00,  2.40,  7.00,  0.40),
('Tofu firme',                 NULL,  144.00, 17.00,  3.00,  9.00),
('Yuca cocida',                0.900, 160.00,  1.40, 38.00,  0.30);

INSERT INTO TipoComida (nombre, orden_dia) VALUES
('Desayuno',       1),
('Media mañana',   2),
('Almuerzo',       3),
('Merienda',       4),
('Cena',           5);

INSERT INTO Ejercicio (nombre, grupo_muscular, bajo_impacto) VALUES
('Caminata rápida',            'cardio',          true),
('Sentadilla',                 'piernas',         false),
('Flexiones de brazos',        'pecho',           false),
('Natación',                   'cuerpo completo', true),
('Bicicleta estática',         'piernas',         true),
('Plancha abdominal',          'core',            true),
('Press de banca',             'pecho',           false),
('Remo con mancuerna',         'espalda',         true),
('Burpees',                    'cuerpo completo', false);

-- =====================================================================
-- 002: tablas dependientes
-- =====================================================================

INSERT INTO PerfilUsuario (id_usuario, nivel_actividad, experiencia_ejercicio, objetivo_peso,
                           dias_entrenamiento_semana, minutos_por_sesion, lugar_entrenamiento,
                           tipo_dieta, comidas_al_dia, presupuesto) VALUES
(1, 'ligero',     'principiante', 'bajar',    3, 45, 'casa',     'normal',      4, 'medio'),
(2, 'sedentario', 'principiante', 'bajar',    3, 30, 'casa',     'normal',      3, 'bajo'),
(3, 'moderado',   'intermedio',   'mantener', 4, 60, 'gimnasio', 'vegetariana', 5, 'medio'),
(4, 'muy_activo', 'avanzado',     'subir',    6, 90, 'gimnasio', 'normal',      5, 'alto'),
(5, 'ligero',     'principiante', 'mantener', 2, 30, 'casa',     'normal',      4, 'bajo');

INSERT INTO ReglaCondicion (id_condicion, tipo_regla, nutriente, valor_limite, descripcion) VALUES
(1, 'maximo_nutriente',  'carbohidratos', 200.00, 'Limitar carbohidratos a 200 g diarios'),
(1, 'minimo_nutriente',  'proteinas',      60.00, 'Asegurar al menos 60 g de proteína al día'),
(2, 'maximo_nutriente',  'grasas',         70.00, 'Limitar grasas a 70 g diarios'),
(3, 'maximo_nutriente',  'grasas',         60.00, 'Limitar grasas a 60 g diarios'),
(4, 'solo_bajo_impacto',  NULL,             NULL, 'Solo ejercicios de bajo impacto articular'),
(5, 'maximo_nutriente',  'proteinas',      50.00, 'Limitar proteínas a 50 g diarios');

INSERT INTO PlanDieta (id_usuario, fecha_generacion, calorias_objetivo, activo) VALUES
(1, current_date - 30, 1700.00, false),
(1, current_date - 2,  1650.00, true),
(2, current_date - 5,  1800.00, true),
(3, current_date - 7,  2000.00, true),
(4, current_date - 3,  3000.00, true),
(5, current_date - 10, 1700.00, true);

INSERT INTO RutinaEjercicio (id_usuario, fecha_generacion, nivel_intensidad, activa) VALUES
(1, current_date - 2,  'bajo',     true),
(2, current_date - 5,  'bajo',     true),
(3, current_date - 7,  'moderado', true),
(4, current_date - 3,  'alto',     true),
(5, current_date - 10, 'bajo',     true),
(1, current_date - 60, 'bajo',     false);

INSERT INTO RegistroConsumo (id_usuario, id_alimento, imagen, nivel_estimacion, gramos_estimados, fecha_hora) VALUES
(1, 1, 'img/u1_almuerzo_0001.jpg', 92.50, 180.00, current_date - 3),
(1, 2, 'img/u1_almuerzo_0001.jpg', 88.00, 150.00, current_date - 3),
(1, 8, 'img/u1_snack_0002.jpg',    95.00, 120.00, current_date - 2),
(2, 1, 'img/u2_cena_0001.jpg',     85.40, 250.00, current_date - 4),
(2, 6, 'img/u2_cena_0001.jpg',     80.00, 200.00, current_date - 4),
(3, 13,'img/u3_almuerzo_0001.jpg', 90.00, 150.00, current_date - 1),
(4, 2, 'img/u4_cena_0001.jpg',     93.00, 300.00, current_date - 1),
(4, 9, 'img/u4_desayuno_0001.jpg', 87.00,  80.00, current_date - 2),
(5, 3, 'img/u5_desayuno_0001.jpg', 91.00, 100.00, current_date - 6);

INSERT INTO Historial_IMC (id_usuario, peso_kg, altura_cm, fecha_registro) VALUES
(1, 72.00, 165.00, current_date - 90),
(1, 70.20, 165.00, current_date - 45),
(1, 68.50, 165.00, current_date - 2),
(2, 97.00, 175.00, current_date - 80),
(2, 94.50, 175.00, current_date - 40),
(2, 92.00, 175.00, current_date - 5),
(3, 55.00, 160.00, current_date - 30),
(4, 74.00, 180.00, current_date - 60),
(4, 78.20, 180.00, current_date - 3),
(5, 84.00, 158.00, current_date - 40),
(5, 81.40, 158.00, current_date - 10);

-- =====================================================================
-- 003: tablas asociativas y seguimiento
-- =====================================================================

INSERT INTO Usuario_Condicion (id_usuario, id_condicion, fecha_diagnostico, observaciones) VALUES
(1, 3, '2022-05-10', 'Colesterol LDL elevado'),
(2, 1, '2020-02-14', 'Controlada con dieta'),
(2, 2, '2021-08-01', NULL),
(5, 2, '2015-03-22', 'En tratamiento farmacológico'),
(5, 4, '2019-11-30', 'Rodilla derecha'),
(5, 1, '2023-01-17', NULL);

INSERT INTO Usuario_Alergia (id_usuario, id_alergia, severidad, observaciones) VALUES
(1, 1, 'leve',     NULL),
(3, 1, 'moderada', NULL),
(3, 3, 'severa',   'Evitar trazas'),
(2, 3, 'leve',     NULL),
(4, 4, 'leve',     NULL),
(5, 2, 'moderada', NULL);

INSERT INTO Alergia_Alimento (id_alergia, id_alimento) VALUES
(1, 4),  
(2, 5),  
(2, 9),   
(3, 10),  
(4, 3); 

INSERT INTO Usuario_AlimentoExcluido (id_usuario, id_alimento) VALUES
(1, 7),   
(2, 14),  
(3, 2),   
(3, 11),   
(4, 12), 
(5, 5);    

INSERT INTO PlanDieta_Alimento (id_plan, id_alimento, id_tipo_comida, cantidad_g) VALUES
(1, 1, 3, 120.00), (1, 2, 3, 120.00),
(2, 9, 1,  60.00), (2, 8, 1, 150.00), (2, 8, 2, 100.00),
(2, 2, 3, 150.00), (2, 1, 3, 150.00), (2, 12, 3, 100.00), (2, 3, 5, 100.00),
(3, 9, 1,  50.00), (3, 8, 2, 150.00), (3, 6, 3, 200.00),
(3, 2, 3, 150.00), (3, 12, 3, 100.00), (3, 11, 5, 150.00),
(4, 9, 1,  60.00), (4, 3, 1, 100.00), (4, 8, 2, 150.00),
(4, 13, 3, 150.00), (4, 6, 3, 200.00), (4, 1, 3, 150.00), (4, 7, 5, 80.00),
(5, 9, 1, 100.00), (5, 4, 1, 250.00), (5, 2, 3, 250.00),
(5, 1, 3, 250.00), (5, 11, 5, 200.00), (5, 14, 5, 200.00),
(6, 3, 1, 100.00), (6, 8, 2, 150.00), (6, 1, 3, 150.00),
(6, 2, 3, 120.00), (6, 12, 3, 100.00);

INSERT INTO RutinaEjercicio_Ejercicio (id_rutina, id_ejercicio, series, repeticiones) VALUES
(1, 1, 3, 10), (1, 2, 3, 12), (1, 6, 3, 20),
(2, 1, 3, 15), (2, 5, 3, 15), (2, 6, 2, 20),
(3, 2, 4, 12), (3, 3, 3, 12), (3, 6, 3, 30), (3, 8, 3, 10),
(4, 7, 5,  5), (4, 2, 5,  8), (4, 3, 4, 15), (4, 8, 4, 10), (4, 6, 4, 45),
(5, 1, 3, 15), (5, 4, 3, 10), (5, 5, 3, 15), (5, 6, 2, 20), (5, 8, 3, 10),
(6, 1, 2, 10);

INSERT INTO SeguimientoRutina (id_rutina, fecha_sesion, duracion_min, completada, observaciones) VALUES
(1, current_date - 1, 40, true,  NULL),
(1, current_date - 0, 25, false, 'Interrumpida por falta de tiempo'),
(2, current_date - 3, 30, true,  NULL),
(2, current_date - 1, 30, true,  'Sin molestias'),
(3, current_date - 4, 60, true,  NULL),
(3, current_date - 2, 55, true,  NULL),
(4, current_date - 2, 90, true,  'Subió peso en press'),
(5, current_date - 5, 30, true,  'Leve dolor de rodilla'),
(5, current_date - 2, 20, false, 'Se detuvo por dolor');

-- =====================================================================
-- PRUEBAS DE RECHAZO (ejecutar de a una, en la sustentación).
-- Cada una debe fallar con el mensaje que se indica.
-- =====================================================================

-- 1) CHECK: correo sin arroba
INSERT INTO Usuario (nombre, email, contrasena_hash, peso_kg, altura_cm, fecha_nacimiento, sexo, fecha_aceptacion_datos, version_politica) VALUES 
('Prueba', 'correo-sin-arroba', 'x', 70, 170, '1990-01-01', 'M', current_date, 'v1.1');
-- ERROR: new row for relation "usuario" violates check constraint "usuario_email_formato_arroba"

-- 2) UNIQUE: correo repetido
INSERT INTO Usuario (nombre, email, contrasena_hash, peso_kg, altura_cm, fecha_nacimiento, sexo, fecha_aceptacion_datos, version_politica) VALUES 
('Otra Laura', 'laura.martinez@correo-demo.test', 'x', 70, 170, '1990-01-01', 'F', current_date, 'v1.1');
-- ERROR: duplicate key value violates unique constraint "usuario_email_unico"

-- 3) CHECK: severidad fuera del dominio
INSERT INTO Usuario_Alergia (id_usuario, id_alergia, severidad) VALUES 
(4, 1, 'extrema');
-- ERROR: new row for relation "usuario_alergia" violates check constraint "usuarioalergia_severidad"

-- 4) CHECK: cantidad no positiva en un plan
INSERT INTO PlanDieta_Alimento (id_plan, id_alimento, id_tipo_comida, cantidad_g) VALUES 
(2, 10, 4, 0);
-- ERROR: new row for relation "plandieta_alimento" violates check constraint "plandietaalimento_cantidad_positiva"

-- 5) PRIMARY KEY: misma condición dos veces al mismo usuario
INSERT INTO Usuario_Condicion (id_usuario, id_condicion) VALUES 
(2, 1);
-- ERROR: duplicate key value violates unique constraint "usuario_condicion_pkey"

-- 6) ON DELETE RESTRICT: borrar un alimento que está en un plan
DELETE FROM Alimento WHERE id_alimento = 1;
-- ERROR: update or delete on table "alimento" violates foreign key constraint "registroconsumo_id_alimento_fkey" on table "registroconsumo"