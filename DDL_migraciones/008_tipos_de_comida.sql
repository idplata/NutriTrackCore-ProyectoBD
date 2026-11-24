-- 008: catálogo de tipos de comida.
-- El 004 quedó solo con ejercicios y sin estas filas, así que generar la dieta fallaba con
-- "TipoComida no tiene una fila con orden_dia 1". orden_dia debe coincidir con el enum MealTime de Java.
INSERT INTO TipoComida (nombre, orden_dia) VALUES
    ('Desayuno', 1),
    ('Media mañana', 2),
    ('Almuerzo', 3),
    ('Merienda', 4),
    ('Cena', 5)
ON CONFLICT (orden_dia) DO NOTHING;
