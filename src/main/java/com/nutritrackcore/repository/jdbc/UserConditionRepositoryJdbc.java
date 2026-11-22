package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserCondition;
import com.nutritrackcore.repository.UserConditionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link UserConditionRepository} sobre la tabla
 * {@code Usuario_Condicion} (clave compuesta usuario + condición). Si el par ya
 * existe, la clave primaria lo rechaza.
 */
public class UserConditionRepositoryJdbc extends JdbcRepository implements UserConditionRepository {

    private static final String COLUMNS = "id_usuario, id_condicion, fecha_diagnostico, observaciones";

    /** @param dataSource fuente de conexiones (el pool) */
    public UserConditionRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(UserCondition uc) throws PersistenceExceptions {
        update("""
                INSERT INTO Usuario_Condicion (id_usuario, id_condicion, fecha_diagnostico, observaciones)
                VALUES (?, ?, ?, ?)
                """, ps -> {
                    ps.setInt(1, uc.getUserId());
                    ps.setInt(2, uc.getConditionId());
                    setDate(ps, 3, uc.getDiagnosisDate());
                    ps.setString(4, uc.getObservations());
                });
    }

    @Override
    public void update(UserCondition uc) throws PersistenceExceptions {
        updateExisting("""
                UPDATE Usuario_Condicion SET fecha_diagnostico = ?, observaciones = ?
                WHERE id_usuario = ? AND id_condicion = ?
                """, ps -> {
                    setDate(ps, 1, uc.getDiagnosisDate());
                    ps.setString(2, uc.getObservations());
                    ps.setInt(3, uc.getUserId());
                    ps.setInt(4, uc.getConditionId());
                }, "El usuario " + uc.getUserId() + " no tiene la condición " + uc.getConditionId());
    }

    @Override
    public Optional<UserCondition> find(int userId, int conditionId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Usuario_Condicion WHERE id_usuario = ? AND id_condicion = ?",
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, conditionId);
                }, this::map);
    }

    @Override
    public List<UserCondition> findByUserId(int userId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Usuario_Condicion WHERE id_usuario = ? ORDER BY id_condicion",
                ps -> ps.setInt(1, userId), this::map);
    }

    @Override
    public void delete(int userId, int conditionId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_Condicion WHERE id_usuario = ? AND id_condicion = ?", ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, conditionId);
        });
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_Condicion WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    /** Convierte una fila de {@code Usuario_Condicion} en un {@link UserCondition}. */
    private UserCondition map(ResultSet rs) throws SQLException {
        return new UserCondition(rs.getInt("id_usuario"), rs.getInt("id_condicion"),
                getDate(rs, "fecha_diagnostico"), rs.getString("observaciones"));
    }
}
