# Capa repository completa - NutriTrackCore

Incluye las 17 interfaces, las implementaciones .txt, las implementaciones JDBC y el connection pool (HikariCP).

## Cómo instalarlo
1. **Copia los archivos encima del proyecto:** `src/`, `pom.xml` y `.gitignore`. Se reemplazan los que tienen el mismo nombre.
2. **Borra estas clases del modelo:**
   - `model/ConsumptionRecorByFood.java`: está duplicada y con el nombre mal escrito.
   - `model/MealType.java`: la reemplaza el enum `MealTime`.
3. **Borra los .txt viejos de `data/`, si existen:** `consumption_records.txt` y `routine_exercises.txt`. Tienen el formato anterior.
4. **Crea la base:** ejecuta los scripts 001 → 005 en PostgreSQL.
5. **Configura la conexión:** copia `db.properties.example` como `db.properties`, junto al `pom.xml`, y pon tus datos.
   - Con `persistencia=jdbc` la app usa PostgreSQL con el pool.
   - Con `persistencia=txt`, o si no existe el archivo, usa la carpeta `data`.
6. **Descarga las dependencias:** en NetBeans, clic derecho → *Build with Dependencies*. Así Maven descarga el driver, HikariCP y slf4j-simple.

## Estructura
```
repository/
├── 17 interfaces (UserRepository, FoodRepository, ...)
├── Repositories.java           agrupa los 17 repositorios
├── txt/
│   ├── TxtFile, TxtFormat, IdGenerator, AbstractTxtRepository
│   ├── 17 *RepositoryTxt
│   └── TxtRepositories.java    fábrica .txt
└── jdbc/
    ├── DatabaseConfig.java     lee db.properties
    ├── ConnectionPool.java     pool HikariCP (una sola instancia)
    ├── JdbcRepository.java     base: conexiones, transacciones, errores
    ├── 17 *RepositoryJdbc
    └── JdbcRepositories.java   fábrica JDBC
```

## Cómo funciona el pool
- **Creación:** `ApplicationContext` crea un solo `ConnectionPool`. Si la base no responde, falla al arrancar, no a mitad de uso.
- **Uso por los repos:** cada repo JDBC recibe el `DataSource` del pool. En cada operación pide una conexión con `getConnection()` y la devuelve con try-with-resources: la conexión no se cierra, vuelve al pool.
- **Cierre:** al salir de la app, el shutdown hook de `NutriTrackCore` llama a `context.close()`, que cierra el pool.

## Detalles de la implementación JDBC
- **Seguridad:** todas las consultas usan `PreparedStatement` con `?`, nunca texto concatenado. Eso evita la inyección SQL.
- **Ids y valores por defecto:** los ids generados y las fechas por defecto (`now()`, `current_date`) se recuperan con `RETURNING`.
- **Transacciones:** guardar o actualizar un plan de dieta, una rutina, o un consumo con sus alimentos se hace en una sola transacción. Si algo falla, se hace rollback y no queda nada a medias.
- **Errores:** cuando la base rechaza un dato (UNIQUE, CHECK, FK, NOT NULL), el error llega como `PersistenceExceptions` con un mensaje entendible.
- **Traducciones entre el modelo y la base:**
  - `MealTime` ↔ `id_tipo_comida` usando `orden_dia`.
  - `IntensityLevel` ↔ `'bajo'/'moderado'/'alto'`.
  - `Sex` ↔ `'M'/'F'/'O'`.
- **Índice:** `findByUserIdAndDate` filtra por rango de `fecha_hora`, así que PostgreSQL usa `idx_consumo_usuario_fecha`.

## Fuera de repository (necesario para que funcione)
- **ApplicationContext:** elige .txt o JDBC según `db.properties` y cierra el pool con `close()`.
- **NutriTrackCore:** registra el cierre del pool al salir.
- **pom.xml:** agrega `postgresql`, `HikariCP` y `slf4j-simple`.
- **Modelo:** `Food`, `ConsumptionRecord`, `ConsumptionRecordByFood`, `UserProfile`, `UserExcludedFood` y `MealTime`.

## Pendiente (capa services/ui)
`ConsumptionService`, `QueryService`, `ExerciseRoutineService` y `RoutinePanel` todavía usan la API vieja de `ConsumptionRecord` y de `ExerciseRoutineExercise`.
