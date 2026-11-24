package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.repository.AllergyRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link AllergyRepository} sobre la tabla {@code Alergia}.
 */
public class AllergyRepositoryJdbc extends JdbcRepository implements AllergyRepository {

    private static final String COLUMNS = "id_alergia, nombre, descripcion";

    /** @param dataSource fuente de conexiones (el pool) */
    public AllergyRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Allergy save(Allergy allergy) throws PersistenceExceptions {
        return insertReturning("INSERT INTO Alergia (nombre, descripcion) VALUES (?, ?) RETURNING id_alergia",
                ps -> {
                    ps.setString(1, allergy.getName());
                    ps.setString(2, allergy.getDescription());
                }, rs -> {
                    allergy.setAllergyId(rs.getInt("id_alergia"));
                    return allergy;
                });
    }

    @Override
    public void update(Allergy allergy) throws PersistenceExceptions {
        updateExisting("UPDATE Alergia SET nombre = ?, descripcion = ? WHERE id_alergia = ?", ps -> {
            ps.setString(1, allergy.getName());
            ps.setString(2, allergy.getDescription());
            ps.setInt(3, allergy.getAllergyId());
        }, "No existe la alergia con id " + allergy.getAllergyId());
    }

    @Override
    public Optional<Allergy> findById(int allergyId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Alergia WHERE id_alergia = ?",
                ps -> ps.setInt(1, allergyId), this::map);
    }

    /** {@inheritDoc} La comparación no distingue mayúsculas. */
    @Override
    public Optional<Allergy> findByName(String name) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Alergia WHERE lower(nombre) = lower(?)",
                ps -> ps.setString(1, name), this::map);
    }

    @Override
    public List<Allergy> findAll() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Alergia ORDER BY id_alergia", NO_PARAMS, this::map);
    }

    /** Convierte una fila de {@code Alergia} en un {@link Allergy}. */
    private Allergy map(ResultSet rs) throws SQLException {
        Allergy a = new Allergy(rs.getString("nombre"), rs.getString("descripcion"));
        a.setAllergyId(rs.getInt("id_alergia"));
        return a;
    }
}
