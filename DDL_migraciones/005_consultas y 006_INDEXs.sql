-- 005_banco_consultas.sql
-- Banco de consultas de NutriTrack Core (punto 5 de la guía).
-- Cada consulta indica la pregunta de negocio que responde y el requisito que cubre.




-- Q1. REUNIÓN DE 4 TABLAS
-- Pregunta: ¿qué alimentos, en qué cantidad y con cuántas kcal tiene cada
--           comida del plan de dieta activo de un usuario?
-- Tablas: PlanDieta, PlanDieta_Alimento, Alimento, TipoComida

SELECT p.id_plan,
       tc.orden_dia,
       tc.nombre                                          AS comida,
       a.nombre                                           AS alimento,
       pda.cantidad_g,
       ROUND(pda.cantidad_g * a.calorias_100g / 100, 1)   AS kcal
FROM PlanDieta p
JOIN PlanDieta_Alimento pda ON pda.id_plan = p.id_plan
JOIN Alimento a             ON a.id_alimento = pda.id_alimento
JOIN TipoComida tc          ON tc.id_tipo_comida = pda.id_tipo_comida
WHERE p.activo
  AND p.id_usuario = 1
ORDER BY tc.orden_dia, a.nombre;



-- Q2. REUNIÓN DE 4 TABLAS
-- Pregunta: ¿qué reglas clínicas (límites de nutrientes, solo bajo impacto)
--           aplican a cada usuario según sus condiciones médicas?
-- Tablas: Usuario, Usuario_Condicion, CondicionMedica, ReglaCondicion

SELECT u.nombre              AS usuario,
       cm.nombre             AS condicion,
       r.tipo_regla,
       r.nutriente,
       r.valor_limite,
       r.descripcion
FROM Usuario u
JOIN Usuario_Condicion uc ON uc.id_usuario = u.id_usuario
JOIN CondicionMedica cm   ON cm.id_condicion = uc.id_condicion
JOIN ReglaCondicion r     ON r.id_condicion = cm.id_condicion
ORDER BY u.nombre, cm.nombre, r.tipo_regla;



-- Q3. REUNIÓN EXTERNA, patrón "LOS QUE NO TIENEN"
-- Pregunta: ¿qué usuarios no han registrado ningún consumo de alimentos?
--           (candidatos a recordatorio de la app)
-- Tablas: Usuario, RegistroConsumo
-- 
SELECT u.id_usuario, u.nombre, u.email
FROM Usuario u
LEFT JOIN RegistroConsumo r ON r.id_usuario = u.id_usuario
WHERE r.id_registro IS NULL
ORDER BY u.nombre;

-- Variante: ¿qué usuarios no tienen una rutina de ejercicio activa?
-- (la condición "activa" va en el ON; si va en el WHERE, la reunión externa
--  se comporta como interna y se pierden los usuarios sin ninguna rutina)
SELECT u.id_usuario, u.nombre
FROM Usuario u
LEFT JOIN RutinaEjercicio re ON re.id_usuario = u.id_usuario AND re.activa
WHERE re.id_rutina IS NULL
ORDER BY u.nombre;


-- 
-- Q4. AGREGACIÓN CON GROUP BY Y HAVING
-- Pregunta: ¿qué rutinas tienen un cumplimiento menor al 50 % de sus sesiones
--           completadas? (el usuario las abandona; hay que ajustarlas)
-- Tablas: SeguimientoRutina, RutinaEjercicio, Usuario
-- 
SELECT u.nombre                                              AS usuario,
       s.id_rutina,
       COUNT(*)                                              AS sesiones,
       COUNT(*) FILTER (WHERE s.completada)                  AS completadas,
       ROUND(100.0 * COUNT(*) FILTER (WHERE s.completada)
                   / COUNT(*), 1)                            AS pct_cumplimiento
FROM SeguimientoRutina s
JOIN RutinaEjercicio re ON re.id_rutina = s.id_rutina
JOIN Usuario u          ON u.id_usuario = re.id_usuario
GROUP BY u.id_usuario, u.nombre, s.id_rutina
HAVING 100.0 * COUNT(*) FILTER (WHERE s.completada) / COUNT(*) < 50
ORDER BY pct_cumplimiento;


-- 
-- Q5. CTE
-- Pregunta: ¿cuánto cambió el peso de cada usuario entre su primera y su
--           última medición? (solo usuarios con 2 o más mediciones)
-- Tablas: Historial_IMC, Usuario
-- Nota: fecha_registro es date; id_historial desempata mediciones del mismo día.
-- 
WITH ordenado AS (
    SELECT id_usuario,
           peso_kg,
           ROW_NUMBER() OVER (PARTITION BY id_usuario
                              ORDER BY fecha_registro, id_historial)           AS rn_asc,
           ROW_NUMBER() OVER (PARTITION BY id_usuario
                              ORDER BY fecha_registro DESC, id_historial DESC) AS rn_desc
    FROM Historial_IMC
)
SELECT u.nombre,
       MAX(o.peso_kg) FILTER (WHERE o.rn_asc  = 1)   AS peso_inicial,
       MAX(o.peso_kg) FILTER (WHERE o.rn_desc = 1)   AS peso_final,
       MAX(o.peso_kg) FILTER (WHERE o.rn_desc = 1)
         - MAX(o.peso_kg) FILTER (WHERE o.rn_asc = 1) AS cambio_kg
FROM ordenado o
JOIN Usuario u ON u.id_usuario = o.id_usuario
GROUP BY u.id_usuario, u.nombre
HAVING COUNT(*) > 1
ORDER BY cambio_kg;


-- 
-- Q6. REUNIÓN DE 6 TABLAS (consulta de seguridad)
-- Pregunta: ¿algún plan de dieta activo incluye un alimento que provoca una
--           alergia que el usuario declaró? El resultado esperado es 0 filas;
--           cualquier fila es un riesgo de salud.
-- Tablas: PlanDieta, Usuario, PlanDieta_Alimento, Alergia_Alimento,
--         Usuario_Alergia, Alergia, Alimento
-- 
SELECT u.id_usuario,
       u.nombre      AS usuario,
       al.nombre     AS alergia,
       ua.severidad,
       a.nombre      AS alimento_peligroso
FROM PlanDieta p
JOIN Usuario u              ON u.id_usuario = p.id_usuario
JOIN PlanDieta_Alimento pda ON pda.id_plan = p.id_plan
JOIN Alergia_Alimento aa    ON aa.id_alimento = pda.id_alimento
JOIN Usuario_Alergia ua     ON ua.id_alergia = aa.id_alergia
                           AND ua.id_usuario = p.id_usuario
JOIN Alergia al             ON al.id_alergia = aa.id_alergia
JOIN Alimento a             ON a.id_alimento = pda.id_alimento
WHERE p.activo
ORDER BY u.nombre, al.nombre;


-- 
-- Q7. SUBCONSULTA
-- Pregunta: ¿qué usuarios con una condición que exige "solo bajo impacto"
--           tienen en su rutina activa ejercicios que NO son de bajo impacto?
--           (la personalización no está respetando la regla clínica)
-- Tablas: RutinaEjercicio, Usuario, RutinaEjercicio_Ejercicio, Ejercicio,
--         y subconsulta sobre Usuario_Condicion y ReglaCondicion
-- 
SELECT DISTINCT u.nombre      AS usuario,
                re.id_rutina,
                e.nombre      AS ejercicio_no_apto
FROM RutinaEjercicio re
JOIN Usuario u                    ON u.id_usuario = re.id_usuario
JOIN RutinaEjercicio_Ejercicio ree ON ree.id_rutina = re.id_rutina
JOIN Ejercicio e                  ON e.id_ejercicio = ree.id_ejercicio
WHERE re.activa
  AND NOT e.bajo_impacto
  AND re.id_usuario IN (
        SELECT uc.id_usuario
        FROM Usuario_Condicion uc
        JOIN ReglaCondicion r ON r.id_condicion = uc.id_condicion
        WHERE r.tipo_regla = 'solo_bajo_impacto'
  )
ORDER BY u.nombre, re.id_rutina;


-- 
-- Q8. AGREGACIÓN CON REUNIÓN
-- Pregunta: ¿cuántas kcal consumió cada usuario hoy frente a la meta
--           calórica de su plan activo?
-- Tablas: RegistroConsumo, Alimento, Usuario, PlanDieta
-- Nota: fecha_hora es date, por eso se compara por día.
-- 
SELECT u.nombre,
       ROUND(SUM(r.gramos_estimados * a.calorias_100g / 100), 1)   AS kcal_consumidas,
       p.calorias_objetivo,
       ROUND(SUM(r.gramos_estimados * a.calorias_100g / 100)
             - p.calorias_objetivo, 1)                             AS diferencia
FROM RegistroConsumo r
JOIN Alimento a  ON a.id_alimento = r.id_alimento
JOIN Usuario u   ON u.id_usuario = r.id_usuario
JOIN PlanDieta p ON p.id_usuario = u.id_usuario AND p.activo
WHERE r.fecha_hora = current_date
GROUP BY u.id_usuario, u.nombre, p.calorias_objetivo
ORDER BY diferencia DESC;

-- Q8-bis. Misma consulta para UN usuario y UN día. Es la que se usa en el
-- punto 6 (índices) con EXPLAIN ANALYZE antes y después de crear
-- idx_consumo_usuario_fecha ON RegistroConsumo (id_usuario, fecha_hora).
EXPLAIN ANALYZE
SELECT r.id_registro, a.nombre,
       r.gramos_estimados,
       ROUND(r.gramos_estimados * a.calorias_100g / 100, 1) AS kcal
FROM RegistroConsumo r
JOIN Alimento a ON a.id_alimento = r.id_alimento
WHERE r.id_usuario = 42
  AND r.fecha_hora = DATE '2026-10-28';


-- 
-- Q9. VISTA CREADA Y CONSULTADA
-- Pregunta: ¿cuál es el IMC actual y la clasificación de cada usuario?
-- El IMC no se almacena (se calcula): la vista concentra la fórmula en un
-- solo lugar y las demás consultas la reutilizan.
-- 
CREATE OR REPLACE VIEW v_imc_actual AS
SELECT u.id_usuario,
       u.nombre,
       u.peso_kg,
       u.altura_cm,
       ROUND(u.peso_kg / POWER(u.altura_cm / 100, 2), 2) AS imc,
       CASE
           WHEN u.peso_kg / POWER(u.altura_cm / 100, 2) < 18.5 THEN 'Bajo peso'
           WHEN u.peso_kg / POWER(u.altura_cm / 100, 2) < 25   THEN 'Normal'
           WHEN u.peso_kg / POWER(u.altura_cm / 100, 2) < 30   THEN 'Sobrepeso'
           ELSE 'Obesidad'
       END AS clasificacion
FROM Usuario u;

-- Consulta sobre la vista: usuarios con sobrepeso u obesidad que además
-- reportaron una condición médica (prioridad para planes personalizados).
SELECT v.nombre, v.imc, v.clasificacion, cm.nombre AS condicion
FROM v_imc_actual v
JOIN Usuario_Condicion uc ON uc.id_usuario = v.id_usuario
JOIN CondicionMedica cm   ON cm.id_condicion = uc.id_condicion
WHERE v.clasificacion IN ('Sobrepeso', 'Obesidad')
ORDER BY v.imc DESC;




-- 006_indices.sql

-- RegistroConsumo: la que más crece. Cubre Q3, Q8 y Q8-bis
CREATE INDEX IF NOT EXISTS idx_consumo_usuario_fecha
ON RegistroConsumo (id_usuario, fecha_hora);

-- SeguimientoRutina: Q4 (agrupa y une por rutina)
CREATE INDEX IF NOT EXISTS idx_seguimiento_rutina
ON SeguimientoRutina (id_rutina);

-- Historial_IMC: Q5 (particiona por usuario y ordena por fecha)
CREATE INDEX IF NOT EXISTS idx_historial_usuario_fecha
ON Historial_IMC (id_usuario, fecha_registro, id_historial);

-- PlanDieta_Alimento: Q1 y Q6 (unir plan con sus alimentos)
CREATE INDEX IF NOT EXISTS idx_pda_plan
ON PlanDieta_Alimento (id_plan);