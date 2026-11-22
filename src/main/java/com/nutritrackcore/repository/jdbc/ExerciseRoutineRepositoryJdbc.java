package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ExerciseByReps;
import com.nutritrackcore.model.ExerciseByTime;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link ExerciseRoutineRepository} sobre
 * {@code RutinaEjercicio}, {@code RutinaEjercicio_Repeticiones},
 * {@code RutinaEjercicio_Tiempo} y {@code RutinaEjercicio_Condicion}.
 * <p>
 * La rutina, sus ejercicios y sus condiciones se guardan en una sola
 * transacción. Cada ejercicio va a la tabla de su subclase
 * ({@link ExerciseByReps} o {@link ExerciseByTime}). La intensidad se traduce
 * entre el enum ({@code LOW}, {@code MODERATE}, {@code HIGH}) y los valores de
 * la base ({@code bajo}, {@code moderado}, {@code alto}).
 */
public class ExerciseRoutineRepositoryJdbc extends JdbcRepository implements ExerciseRoutineRepository {

    private static final String COLUMNS =
            "id_rutina, id_usuario, fecha_generacion, nivel_intensidad, activa, id_historial_base, id_plan_dieta";

    /** @param dataSource fuente de conexiones (el pool) */
    public ExerciseRoutineRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    /** {@inheritDoc} Si la fecha es nula se usa {@code current_date}. */
    @Override
    public ExerciseRoutine save(ExerciseRoutine routine) throws PersistenceExceptions {
        return inTransaction(con -> {
            insertReturning(con, """
                    INSERT INTO RutinaEjercicio (id_usuario, fecha_generacion, nivel_intensidad, activa,
                                                 id_historial_base, id_plan_dieta)
                    VALUES (?, COALESCE(?, current_date), ?, ?, ?, ?)
                    RETURNING id_rutina, fecha_generacion
                    """, ps -> {
                        ps.setInt(1, routine.getUserId());
                        setDate(ps, 2, routine.getGenerationDate());
                        ps.setString(3, toDb(routine.getIntensityLevel()));
                        ps.setBoolean(4, routine.isActive());
                        ps.setInt(5, routine.getBaseHistoryId());
                        setNullableInt(ps, 6, routine.getDietPlanId());
                    }, rs -> {
                        routine.setRoutineId(rs.getInt("id_rutina"));
                        routine.setGenerationDate(getDate(rs, "fecha_generacion"));
                        return routine;
                    });
            insertChildren(con, routine);
            return routine;
        });
    }

    /** {@inheritDoc} Reemplaza ejercicios y condiciones: borra los anteriores e inserta los actuales. */
    @Override
    public void update(ExerciseRoutine routine) throws PersistenceExceptions {
        inTransaction(con -> {
            int rows = update(con, """
                    UPDATE RutinaEjercicio SET id_usuario = ?, fecha_generacion = COALESCE(?, fecha_generacion),
                           nivel_intensidad = ?, activa = ?, id_historial_base = ?, id_plan_dieta = ?
                    WHERE id_rutina = ?
                    """, ps -> {
                        ps.setInt(1, routine.getUserId());
                        setDate(ps, 2, routine.getGenerationDate());
                        ps.setString(3, toDb(routine.getIntensityLevel()));
                        ps.setBoolean(4, routine.isActive());
                        ps.setInt(5, routine.getBaseHistoryId());
                        setNullableInt(ps, 6, routine.getDietPlanId());
                        ps.setInt(7, routine.getRoutineId());
                    });
            if (rows == 0) {
                throw new PersistenceExceptions("No existe la rutina con id " + routine.getRoutineId());
            }
            for (String table : new String[] {"RutinaEjercicio_Repeticiones", "RutinaEjercicio_Tiempo",
                                              "RutinaEjercicio_Condicion"}) {
                update(con, "DELETE FROM " + table + " WHERE id_rutina = ?",   // nombres fijos, no vienen del usuario
                        ps -> ps.setInt(1, routine.getRoutineId()));
            }
            insertChildren(con, routine);
            return null;
        });
    }

    @Override
    public Optional<ExerciseRoutine> findById(int routineId) throws PersistenceExceptions {
        List<ExerciseRoutine> list = loadRoutines("SELECT " + COLUMNS + " FROM RutinaEjercicio WHERE id_rutina = ?",
                routineId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** {@inheritDoc} Ordenadas por fecha de generación. */
    @Override
    public List<ExerciseRoutine> findByUserId(int userId) throws PersistenceExceptions {
        return loadRoutines("SELECT " + COLUMNS + " FROM RutinaEjercicio WHERE id_usuario = ?"
                + " ORDER BY fecha_generacion, id_rutina", userId);
    }

    /** {@inheritDoc} Si hay varias activas, devuelve la más reciente (igual que en .txt). */
    @Override
    public Optional<ExerciseRoutine> findActiveByUserId(int userId) throws PersistenceExceptions {
        List<ExerciseRoutine> list = loadRoutines("SELECT " + COLUMNS + " FROM RutinaEjercicio"
                + " WHERE id_usuario = ? AND activa ORDER BY fecha_generacion DESC, id_rutina DESC LIMIT 1", userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** {@inheritDoc} La cascada borra ejercicios, condiciones y seguimientos de cada rutina. */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM RutinaEjercicio WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    // ---------- apoyo ----------

    /** Inserta ejercicios (cada uno en la tabla de su subclase) y condiciones de la rutina. */
    private static void insertChildren(Connection con, ExerciseRoutine routine)
            throws SQLException, PersistenceExceptions {
        int id = routine.getRoutineId();
        if (routine.getLines() != null) {
            for (ExerciseRoutineExercise e : routine.getLines()) {
                e.setRoutineId(id);
                if (e instanceof ExerciseByReps reps) {
                    update(con, """
                            INSERT INTO RutinaEjercicio_Repeticiones (id_rutina, id_ejercicio, series, repeticiones)
                            VALUES (?, ?, ?, ?)
                            """, ps -> {
                                ps.setInt(1, id);
                                ps.setInt(2, reps.getExerciseId());
                                ps.setInt(3, reps.getSets());
                                ps.setInt(4, reps.getReps());
                            });
                } else if (e instanceof ExerciseByTime time) {
                    if (time.getDuration() == null) {
                        throw new PersistenceExceptions("El ejercicio " + time.getExerciseId() + " no tiene duración");
                    }
                    update(con, """
                            INSERT INTO RutinaEjercicio_Tiempo (id_rutina, id_ejercicio, series, duracion_segundos)
                            VALUES (?, ?, ?, ?)
                            """, ps -> {
                                ps.setInt(1, id);
                                ps.setInt(2, time.getExerciseId());
                                ps.setInt(3, time.getSets());
                                ps.setInt(4, Math.toIntExact(time.getDuration().getSeconds()));
                            });
                } else {
                    throw new PersistenceExceptions("Tipo de ejercicio de rutina no soportado: "
                            + e.getClass().getSimpleName());
                }
            }
        }
        if (routine.getConsideredConditions() != null) {
            for (ExerciseRoutineCondition c : routine.getConsideredConditions()) {
                c.setRoutineId(id);
                update(con, """
                        INSERT INTO RutinaEjercicio_Condicion (id_rutina, id_condicion, restriccion_aplicada)
                        VALUES (?, ?, ?)
                        """, ps -> {
                            ps.setInt(1, id);
                            ps.setInt(2, c.getConditionId());
                            ps.setString(3, c.getAppliedRestriction());
                        });
            }
        }
    }

    /**
     * Ejecuta la consulta de rutinas (con un único parámetro entero) y carga
     * ejercicios y condiciones de cada una, todo con la misma conexión.
     */
    private List<ExerciseRoutine> loadRoutines(String sql, int param) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            List<ExerciseRoutine> routines = query(con, sql, ps -> ps.setInt(1, param),
                    ExerciseRoutineRepositoryJdbc::mapRoutine);
            for (ExerciseRoutine r : routines) {
                List<ExerciseRoutineExercise> lines = new ArrayList<>();
                lines.addAll(query(con, """
                        SELECT id_rutina, id_ejercicio, series, repeticiones
                        FROM RutinaEjercicio_Repeticiones WHERE id_rutina = ? ORDER BY id_ejercicio
                        """, ps -> ps.setInt(1, r.getRoutineId()),
                        rs -> new ExerciseByReps(rs.getInt("id_rutina"), rs.getInt("id_ejercicio"),
                                rs.getInt("series"), rs.getInt("repeticiones"))));
                lines.addAll(query(con, """
                        SELECT id_rutina, id_ejercicio, series, duracion_segundos
                        FROM RutinaEjercicio_Tiempo WHERE id_rutina = ? ORDER BY id_ejercicio
                        """, ps -> ps.setInt(1, r.getRoutineId()),
                        rs -> new ExerciseByTime(rs.getInt("id_rutina"), rs.getInt("id_ejercicio"),
                                rs.getInt("series"), Duration.ofSeconds(rs.getInt("duracion_segundos")))));
                r.setLines(lines);
                r.setConsideredConditions(query(con, """
                        SELECT id_rutina, id_condicion, restriccion_aplicada
                        FROM RutinaEjercicio_Condicion WHERE id_rutina = ? ORDER BY id_condicion
                        """, ps -> ps.setInt(1, r.getRoutineId()),
                        rs -> new ExerciseRoutineCondition(rs.getInt("id_rutina"), rs.getInt("id_condicion"),
                                rs.getString("restriccion_aplicada"))));
            }
            return routines;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Convierte una fila de {@code RutinaEjercicio} en un {@link ExerciseRoutine} (sin hijos). */
    private static ExerciseRoutine mapRoutine(ResultSet rs) throws SQLException {
        ExerciseRoutine r = new ExerciseRoutine(rs.getInt("id_usuario"), getDate(rs, "fecha_generacion"),
                fromDb(rs.getString("nivel_intensidad")), rs.getInt("id_historial_base"),
                getNullableInt(rs, "id_plan_dieta"));
        r.setRoutineId(rs.getInt("id_rutina"));
        r.setActive(rs.getBoolean("activa"));
        return r;
    }

    /** Traduce la intensidad del enum al valor de la base. */
    static String toDb(IntensityLevel level) {
        if (level == null) {
            return null;                                     // la base lo rechaza: es obligatorio
        }
        return switch (level) {
            case LOW -> "bajo";
            case MODERATE -> "moderado";
            case HIGH -> "alto";
        };
    }

    /** Traduce el valor de la base al enum de intensidad. */
    static IntensityLevel fromDb(String value) {
        return switch (value) {
            case "bajo" -> IntensityLevel.LOW;
            case "moderado" -> IntensityLevel.MODERATE;
            case "alto" -> IntensityLevel.HIGH;
            default -> throw new IllegalArgumentException("Nivel de intensidad desconocido: " + value);
        };
    }
}
