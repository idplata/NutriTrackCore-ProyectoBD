package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.repository.MedicalConditionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link MedicalConditionRepository} sobre la tabla
 * {@code CondicionMedica}.
 */
public class MedicalConditionRepositoryJdbc extends JdbcRepository implements MedicalConditionRepository {

    private static final String COLUMNS = "id_condicion, nombre, descripcion, activo";

    /** @param dataSource fuente de conexiones (el pool) */
    public MedicalConditionRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public MedicalCondition save(MedicalCondition condition) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO CondicionMedica (nombre, descripcion, activo) VALUES (?, ?, ?)
                RETURNING id_condicion
                """, ps -> {
                    ps.setString(1, condition.getName());
                    ps.setString(2, condition.getDescription());
                    ps.setBoolean(3, condition.isActive());
                }, rs -> {
                    condition.setConditionId(rs.getInt("id_condicion"));
                    return condition;
                });
    }

    @Override
    public void update(MedicalCondition condition) throws PersistenceExceptions {
        updateExisting("UPDATE CondicionMedica SET nombre = ?, descripcion = ?, activo = ? WHERE id_condicion = ?",
                ps -> {
                    ps.setString(1, condition.getName());
                    ps.setString(2, condition.getDescription());
                    ps.setBoolean(3, condition.isActive());
                    ps.setInt(4, condition.getConditionId());
                }, "No existe la condición con id " + condition.getConditionId());
    }

    @Override
    public Optional<MedicalCondition> findById(int conditionId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM CondicionMedica WHERE id_condicion = ?",
                ps -> ps.setInt(1, conditionId), this::map);
    }

    @Override
    public List<MedicalCondition> findAllActive() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM CondicionMedica WHERE activo ORDER BY id_condicion",
                NO_PARAMS, this::map);
    }

    @Override
    public List<MedicalCondition> findAll() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM CondicionMedica ORDER BY id_condicion", NO_PARAMS, this::map);
    }

    /** Convierte una fila de {@code CondicionMedica} en un {@link MedicalCondition}. */
    private MedicalCondition map(ResultSet rs) throws SQLException {
        MedicalCondition m = new MedicalCondition(rs.getString("nombre"), rs.getString("descripcion"));
        m.setConditionId(rs.getInt("id_condicion"));
        m.setActive(rs.getBoolean("activo"));
        return m;
    }
}
