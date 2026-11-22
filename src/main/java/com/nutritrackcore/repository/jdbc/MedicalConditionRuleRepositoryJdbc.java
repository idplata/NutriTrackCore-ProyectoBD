package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalConditionRule;
import com.nutritrackcore.repository.MedicalConditionRuleRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link MedicalConditionRuleRepository} sobre la tabla
 * {@code ReglaCondicion}. La base exige coherencia: las reglas de máximo o
 * mínimo llevan nutriente y valor; {@code solo_bajo_impacto} los deja nulos.
 */
public class MedicalConditionRuleRepositoryJdbc extends JdbcRepository implements MedicalConditionRuleRepository {

    private static final String COLUMNS = "id_regla, id_condicion, tipo_regla, nutriente, valor_limite, descripcion";

    /** @param dataSource fuente de conexiones (el pool) */
    public MedicalConditionRuleRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public MedicalConditionRule save(MedicalConditionRule rule) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO ReglaCondicion (id_condicion, tipo_regla, nutriente, valor_limite, descripcion)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id_regla
                """, ps -> bindFields(ps, rule), rs -> {
                    rule.setRuleId(rs.getInt("id_regla"));
                    return rule;
                });
    }

    @Override
    public void update(MedicalConditionRule rule) throws PersistenceExceptions {
        updateExisting("""
                UPDATE ReglaCondicion SET id_condicion = ?, tipo_regla = ?, nutriente = ?, valor_limite = ?,
                                          descripcion = ?
                WHERE id_regla = ?
                """, ps -> {
                    bindFields(ps, rule);
                    ps.setInt(6, rule.getRuleId());
                }, "No existe la regla con id " + rule.getRuleId());
    }

    @Override
    public Optional<MedicalConditionRule> findById(int ruleId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM ReglaCondicion WHERE id_regla = ?",
                ps -> ps.setInt(1, ruleId), this::map);
    }

    @Override
    public List<MedicalConditionRule> findByConditionId(int conditionId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM ReglaCondicion WHERE id_condicion = ? ORDER BY id_regla",
                ps -> ps.setInt(1, conditionId), this::map);
    }

    @Override
    public void deleteById(int ruleId) throws PersistenceExceptions {
        update("DELETE FROM ReglaCondicion WHERE id_regla = ?", ps -> ps.setInt(1, ruleId));
    }

    /** Asigna los parámetros 1 a 5 (todas las columnas menos el id). */
    private static void bindFields(PreparedStatement ps, MedicalConditionRule r) throws SQLException {
        ps.setInt(1, r.getConditionId());
        ps.setString(2, r.getRuleType());
        ps.setString(3, r.getNutrient());
        setNullableDecimal(ps, 4, r.getLimitValue());
        ps.setString(5, r.getDescription());
    }

    /** Convierte una fila de {@code ReglaCondicion} en un {@link MedicalConditionRule}. */
    private MedicalConditionRule map(ResultSet rs) throws SQLException {
        MedicalConditionRule r = new MedicalConditionRule(rs.getInt("id_condicion"), rs.getString("tipo_regla"),
                rs.getString("nutriente"), rs.getBigDecimal("valor_limite"), rs.getString("descripcion"));
        r.setRuleId(rs.getInt("id_regla"));
        return r;
    }
}
