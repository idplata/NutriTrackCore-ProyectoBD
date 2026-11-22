package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.repository.ExerciseRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link ExerciseRepository} sobre la tabla {@code Ejercicio}.
 * <p>
 * El grupo muscular debe venir en el formato del {@code check}
 * ({@code PIERNAS}, {@code CUERPO_COMPLETO}, ...); si no, la base lo rechaza.
 */
public class ExerciseRepositoryJdbc extends JdbcRepository implements ExerciseRepository {

    private static final String COLUMNS = "id_ejercicio, nombre, grupo_muscular, bajo_impacto, requiere_equipo, activo";

    /** @param dataSource fuente de conexiones (el pool) */
    public ExerciseRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Exercise save(Exercise exercise) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO Ejercicio (nombre, grupo_muscular, bajo_impacto, requiere_equipo, activo)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id_ejercicio
                """, ps -> bindFields(ps, exercise), rs -> {
                    exercise.setExerciseId(rs.getInt("id_ejercicio"));
                    return exercise;
                });
    }

    @Override
    public void update(Exercise exercise) throws PersistenceExceptions {
        updateExisting("""
                UPDATE Ejercicio SET nombre = ?, grupo_muscular = ?, bajo_impacto = ?, requiere_equipo = ?, activo = ?
                WHERE id_ejercicio = ?
                """, ps -> {
                    bindFields(ps, exercise);
                    ps.setInt(6, exercise.getExerciseId());
                }, "No existe el ejercicio con id " + exercise.getExerciseId());
    }

    @Override
    public Optional<Exercise> findById(int exerciseId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Ejercicio WHERE id_ejercicio = ?",
                ps -> ps.setInt(1, exerciseId), this::map);
    }

    @Override
    public List<Exercise> findAllActive() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Ejercicio WHERE activo ORDER BY id_ejercicio", NO_PARAMS, this::map);
    }

    @Override
    public List<Exercise> findAll() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Ejercicio ORDER BY id_ejercicio", NO_PARAMS, this::map);
    }

    /** Asigna los parámetros 1 a 5 (todas las columnas menos el id). */
    private static void bindFields(PreparedStatement ps, Exercise e) throws SQLException {
        ps.setString(1, e.getName());
        ps.setString(2, e.getMuscleGroup());
        ps.setBoolean(3, e.isLowImpact());
        ps.setBoolean(4, e.getNeed_Equipment());
        ps.setBoolean(5, e.isActive());
    }

    /** Convierte una fila de {@code Ejercicio} en un {@link Exercise}. */
    private Exercise map(ResultSet rs) throws SQLException {
        Exercise e = new Exercise();
        e.setExerciseId(rs.getInt("id_ejercicio"));
        e.setName(rs.getString("nombre"));
        e.setMuscleGroup(rs.getString("grupo_muscular"));
        e.setLowImpact(rs.getBoolean("bajo_impacto"));
        e.setNeed_Equipment(rs.getBoolean("requiere_equipo"));
        e.setActive(rs.getBoolean("activo"));
        return e;
    }
}
