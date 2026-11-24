-- =====================================================================
-- Semilla de la tabla Ejercicio (reemplaza el INSERT INTO Ejercicio de 004)
-- ---------------------------------------------------------------------
-- Fuentes:
--   * Catálogo: free-exercise-db (yuhonas, dominio público / Unlicense),
--     https://github.com/yuhonas/free-exercise-db  → columna "fuente" del comentario.
--   * Impacto e intensidad: 2024 Adult Compendium of Physical Activities
--     (Herrmann et al., J Sport Health Sci 2024;13:6-12), https://pacompendium.com
--
-- Cómo se adaptó a la tabla:
--   nombre          → traducido al español (máx. 100 caracteres, único)
--   grupo_muscular  → primaryMuscles del dataset:
--                       chest → PECHO | lats, middle back, lower back, traps → ESPALDA
--                       shoulders → HOMBROS | biceps, triceps, forearms → BRAZOS
--                       quadriceps, hamstrings, calves, adductors, abductors → PIERNAS
--                       glutes → GLUTEOS | abdominals → CORE
--                       category = cardio → CARDIO | varios grupos grandes → CUERPO_COMPLETO
--   requiere_equipo → false solo si equipment = "body only" o vacío (se hace en casa)
--   bajo_impacto    → true si no tiene saltos, carrera ni carga pesada sobre la columna,
--                     y su MET en el Compendium es bajo o moderado (< 6 MET).
--                     false: pliometría, carrera, saltar lazo, levantamientos con barra pesada.
--
-- Los 9 primeros conservan nombre y orden (ids 1 a 9) porque las rutinas de
-- la semilla los usan. Solo se corrige requiere_equipo en los que usan equipo.
-- =====================================================================

INSERT INTO Ejercicio (nombre, grupo_muscular, bajo_impacto, requiere_equipo) VALUES
-- ---- ids 1 a 9: los de la semilla original -------------------------
('Caminata rápida',                    'CARDIO',          true,  false),
('Sentadilla',                         'PIERNAS',         false, false),
('Flexiones de brazos',                'PECHO',           false, false),
('Natación',                           'CUERPO_COMPLETO', true,  true),   -- requiere piscina
('Bicicleta estática',                 'PIERNAS',         true,  true),
('Plancha abdominal',                  'CORE',            true,  false),
('Press de banca',                     'PECHO',           false, true),
('Remo con mancuerna',                 'ESPALDA',         true,  true),
('Burpees',                            'CUERPO_COMPLETO', false, false),

-- ---- PECHO -----------------------------------------------------------
('Flexiones inclinadas',               'PECHO',           true,  false),  -- Incline_Push-Up
('Aperturas con mancuernas',           'PECHO',           true,  true),   -- Dumbbell_Flyes

-- ---- ESPALDA ---------------------------------------------------------
('Superman',                           'ESPALDA',         true,  false),  -- Superman
('Dominadas',                          'ESPALDA',         false, true),   -- Pullups (barra)
('Remo inclinado con dos mancuernas',  'ESPALDA',         true,  true),   -- Bent_Over_Two-Dumbbell_Row
('Remo sentado en polea',              'ESPALDA',         true,  true),   -- Seated_Cable_Rows
('Jalón al pecho agarre ancho',        'ESPALDA',         true,  true),   -- Wide-Grip_Lat_Pulldown
('Extensiones lumbares',               'ESPALDA',         true,  true),   -- Hyperextensions_Back_Extensions

-- ---- HOMBROS ---------------------------------------------------------
('Círculos con los brazos',            'HOMBROS',         true,  false),  -- Arm_Circles
('Press de hombros con mancuernas',    'HOMBROS',         true,  true),   -- Dumbbell_Shoulder_Press
('Elevaciones laterales',              'HOMBROS',         true,  true),   -- Side_Lateral_Raise
('Elevaciones frontales',              'HOMBROS',         true,  true),   -- Front_Dumbbell_Raise

-- ---- BRAZOS ----------------------------------------------------------
('Fondos en banco',                    'BRAZOS',          true,  false),  -- Bench_Dips
('Curl de bíceps con mancuernas',      'BRAZOS',          true,  true),   -- Dumbbell_Bicep_Curl
('Curl martillo',                      'BRAZOS',          true,  true),   -- Hammer_Curls
('Extensión de tríceps en polea',      'BRAZOS',          true,  true),   -- Triceps_Pushdown

-- ---- PIERNAS ---------------------------------------------------------
('Sentadilla con peso corporal',       'PIERNAS',         true,  false),  -- Bodyweight_Squat
('Zancadas caminando',                 'PIERNAS',         true,  false),  -- Bodyweight_Walking_Lunge
('Subida al escalón con rodilla',      'PIERNAS',         true,  false),  -- Step-up_with_Knee_Raise
('Sentadilla con barra',               'PIERNAS',         false, true),   -- Barbell_Squat
('Zancadas con mancuernas',            'PIERNAS',         true,  true),   -- Dumbbell_Lunges
('Prensa de piernas',                  'PIERNAS',         true,  true),   -- Leg_Press
('Elevación de talones de pie',        'PIERNAS',         true,  true),   -- Standing_Calf_Raises
('Peso muerto a una pierna con pesa rusa', 'PIERNAS',     false, true),   -- Kettlebell_One-Legged_Deadlift

-- ---- GLUTEOS ---------------------------------------------------------
('Puente de glúteos',                  'GLUTEOS',         true,  false),  -- Butt_Lift_Bridge
('Puente de glúteos a una pierna',     'GLUTEOS',         true,  false),  -- Single_Leg_Glute_Bridge
('Patada de glúteo',                   'GLUTEOS',         true,  false),  -- Glute_Kickback
('Patadas de tijera',                  'GLUTEOS',         true,  false),  -- Flutter_Kicks
('Empuje de cadera con barra',         'GLUTEOS',         false, true),   -- Barbell_Hip_Thrust

-- ---- CORE ------------------------------------------------------------
('Abdominales crunch',                 'CORE',            true,  false),  -- Crunches
('Bicho muerto',                       'CORE',            true,  false),  -- Dead_Bug
('Plancha lateral',                    'CORE',            true,  false),  -- Side_Bridge
('Giro ruso',                          'CORE',            true,  false),  -- Russian_Twist
('Abdominales bicicleta',              'CORE',            true,  false),  -- Air_Bike

-- ---- CARDIO ----------------------------------------------------------
('Escaladores',                        'CARDIO',          false, false),  -- Mountain_Climbers
('Saltar lazo',                        'CARDIO',          false, true),   -- Rope_Jumping
('Trote en caminadora',                'CARDIO',          false, true),   -- Jogging_Treadmill
('Caminata en caminadora',             'CARDIO',          true,  true),   -- Walking_Treadmill
('Bicicleta reclinada',                'CARDIO',          true,  true),   -- Recumbent_Bike
('Elíptica',                           'CARDIO',          true,  true),   -- Elliptical_Trainer
('Remo estático',                      'CARDIO',          true,  true),   -- Rowing_Stationary
('Escaladora',                         'CARDIO',          false, true);   -- Stairmaster
