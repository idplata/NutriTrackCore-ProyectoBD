package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.AllergyFood;
import com.nutritrackcore.repository.AllergyFoodRepository;
import java.util.List;
import javax.sql.DataSource;

/**
 * Implementación de {@link AllergyFoodRepository} sobre la tabla
 * {@code Alergia_Alimento} (enlace puro, clave compuesta).
 */
public class AllergyFoodRepositoryJdbc extends JdbcRepository implements AllergyFoodRepository {

    /** @param dataSource fuente de conexiones (el pool) */
    public AllergyFoodRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(AllergyFood link) throws PersistenceExceptions {
        update("INSERT INTO Alergia_Alimento (id_alergia, id_alimento) VALUES (?, ?)", ps -> {
            ps.setInt(1, link.getAllergyId());
            ps.setInt(2, link.getFoodId());
        });
    }

    @Override
    public List<AllergyFood> findByAllergyId(int allergyId) throws PersistenceExceptions {
        return query("SELECT id_alergia, id_alimento FROM Alergia_Alimento WHERE id_alergia = ? ORDER BY id_alimento",
                ps -> ps.setInt(1, allergyId),
                rs -> new AllergyFood(rs.getInt("id_alergia"), rs.getInt("id_alimento")));
    }

    @Override
    public List<AllergyFood> findByFoodId(int foodId) throws PersistenceExceptions {
        return query("SELECT id_alergia, id_alimento FROM Alergia_Alimento WHERE id_alimento = ? ORDER BY id_alergia",
                ps -> ps.setInt(1, foodId),
                rs -> new AllergyFood(rs.getInt("id_alergia"), rs.getInt("id_alimento")));
    }

    @Override
    public void delete(int allergyId, int foodId) throws PersistenceExceptions {
        update("DELETE FROM Alergia_Alimento WHERE id_alergia = ? AND id_alimento = ?", ps -> {
            ps.setInt(1, allergyId);
            ps.setInt(2, foodId);
        });
    }
}
