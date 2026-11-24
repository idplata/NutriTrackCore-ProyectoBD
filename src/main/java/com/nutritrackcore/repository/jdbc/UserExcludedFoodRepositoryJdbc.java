package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import java.util.List;
import javax.sql.DataSource;

/**
 * Implementación de {@link UserExcludedFoodRepository} sobre la tabla
 * {@code Usuario_AlimentoExcluido} (clave compuesta usuario + alimento).
 */
public class UserExcludedFoodRepositoryJdbc extends JdbcRepository implements UserExcludedFoodRepository {

    /** @param dataSource fuente de conexiones (el pool) */
    public UserExcludedFoodRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(UserExcludedFood excluded) throws PersistenceExceptions {
        update("INSERT INTO Usuario_AlimentoExcluido (id_usuario, id_alimento) VALUES (?, ?)", ps -> {
            ps.setInt(1, excluded.getUserId());
            ps.setInt(2, excluded.getFoodId());
        });
    }

    @Override
    public boolean exists(int userId, int foodId) throws PersistenceExceptions {
        return queryOne("SELECT 1 FROM Usuario_AlimentoExcluido WHERE id_usuario = ? AND id_alimento = ?", ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, foodId);
        }, rs -> Boolean.TRUE).isPresent();
    }

    @Override
    public List<UserExcludedFood> findByUserId(int userId) throws PersistenceExceptions {
        return query("SELECT id_usuario, id_alimento FROM Usuario_AlimentoExcluido WHERE id_usuario = ?"
                        + " ORDER BY id_alimento",
                ps -> ps.setInt(1, userId),
                rs -> new UserExcludedFood(rs.getInt("id_usuario"), rs.getInt("id_alimento")));
    }

    @Override
    public void delete(int userId, int foodId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_AlimentoExcluido WHERE id_usuario = ? AND id_alimento = ?", ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, foodId);
        });
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_AlimentoExcluido WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }
}
