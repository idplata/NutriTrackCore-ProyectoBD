package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.RoutineFollowUp;
import com.nutritrackcore.repository.RoutineFollowUpRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link RoutineFollowUpRepository} sobre la tabla
 * {@code SeguimientoRutina}. Si la fecha de sesión es nula se usa
 * {@code current_date}.
 */
public class RoutineFollowUpRepositoryJdbc extends JdbcRepository implements RoutineFollowUpRepository {

    private static final String COLUMNS = "id_seguimiento, id_rutina, fecha_sesion, duracion_min, completada, observaciones";

    /** @param dataSource fuente de conexiones (el pool) */
    public RoutineFollowUpRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public RoutineFollowUp save(RoutineFollowUp f) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO SeguimientoRutina (id_rutina, fecha_sesion, duracion_min, completada, observaciones)
                VALUES (?, COALESCE(?, current_date), ?, ?, ?)
                RETURNING id_seguimiento, fecha_sesion
                """, ps -> {
                    ps.setInt(1, f.getRoutineId());
                    setDate(ps, 2, f.getSessionDate());
                    ps.setInt(3, f.getDurationMinutes());
                    ps.setBoolean(4, f.isCompleted());
                    ps.setString(5, f.getObservations());
                }, rs -> {
                    f.setFollowUpId(rs.getInt("id_seguimiento"));
                    f.setSessionDate(getDate(rs, "fecha_sesion"));
                    return f;
                });
    }

    @Override
    public void update(RoutineFollowUp f) throws PersistenceExceptions {
        updateExisting("""
                UPDATE SeguimientoRutina SET id_rutina = ?, fecha_sesion = COALESCE(?, current_date),
                       duracion_min = ?, completada = ?, observaciones = ?
                WHERE id_seguimiento = ?
                """, ps -> {
                    ps.setInt(1, f.getRoutineId());
                    setDate(ps, 2, f.getSessionDate());
                    ps.setInt(3, f.getDurationMinutes());
                    ps.setBoolean(4, f.isCompleted());
                    ps.setString(5, f.getObservations());
                    ps.setInt(6, f.getFollowUpId());
                }, "No existe el seguimiento con id " + f.getFollowUpId());
    }

    @Override
    public Optional<RoutineFollowUp> findById(int followUpId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM SeguimientoRutina WHERE id_seguimiento = ?",
                ps -> ps.setInt(1, followUpId), this::map);
    }

    @Override
    public List<RoutineFollowUp> findByRoutineId(int routineId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM SeguimientoRutina WHERE id_rutina = ?"
                        + " ORDER BY fecha_sesion, id_seguimiento",
                ps -> ps.setInt(1, routineId), this::map);
    }

    @Override
    public void deleteByRoutineId(int routineId) throws PersistenceExceptions {
        update("DELETE FROM SeguimientoRutina WHERE id_rutina = ?", ps -> ps.setInt(1, routineId));
    }

    /** Convierte una fila de {@code SeguimientoRutina} en un {@link RoutineFollowUp}. */
    private RoutineFollowUp map(ResultSet rs) throws SQLException {
        RoutineFollowUp f = new RoutineFollowUp(rs.getInt("id_rutina"), getDate(rs, "fecha_sesion"),
                rs.getInt("duracion_min"), rs.getBoolean("completada"), rs.getString("observaciones"));
        f.setFollowUpId(rs.getInt("id_seguimiento"));
        return f;
    }
}
