package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.repository.UserAllergyRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link UserAllergyRepository} sobre la tabla
 * {@code Usuario_Alergia} (clave compuesta usuario + alergia). La severidad
 * debe ser {@code leve}, {@code moderada} o {@code severa}.
 */
public class UserAllergyRepositoryJdbc extends JdbcRepository implements UserAllergyRepository {

    private static final String COLUMNS = "id_usuario, id_alergia, severidad, observaciones";

    /** @param dataSource fuente de conexiones (el pool) */
    public UserAllergyRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(UserAllergy ua) throws PersistenceExceptions {
        update("INSERT INTO Usuario_Alergia (id_usuario, id_alergia, severidad, observaciones) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, ua.getUserId());
                    ps.setInt(2, ua.getAllergyId());
                    ps.setString(3, ua.getSeverity());
                    ps.setString(4, ua.getObservations());
                });
    }

    @Override
    public void update(UserAllergy ua) throws PersistenceExceptions {
        updateExisting("""
                UPDATE Usuario_Alergia SET severidad = ?, observaciones = ?
                WHERE id_usuario = ? AND id_alergia = ?
                """, ps -> {
                    ps.setString(1, ua.getSeverity());
                    ps.setString(2, ua.getObservations());
                    ps.setInt(3, ua.getUserId());
                    ps.setInt(4, ua.getAllergyId());
                }, "El usuario " + ua.getUserId() + " no tiene la alergia " + ua.getAllergyId());
    }

    @Override
    public Optional<UserAllergy> find(int userId, int allergyId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Usuario_Alergia WHERE id_usuario = ? AND id_alergia = ?",
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, allergyId);
                }, this::map);
    }

    @Override
    public List<UserAllergy> findByUserId(int userId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Usuario_Alergia WHERE id_usuario = ? ORDER BY id_alergia",
                ps -> ps.setInt(1, userId), this::map);
    }

    @Override
    public void delete(int userId, int allergyId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_Alergia WHERE id_usuario = ? AND id_alergia = ?", ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, allergyId);
        });
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM Usuario_Alergia WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    /** Convierte una fila de {@code Usuario_Alergia} en un {@link UserAllergy}. */
    private UserAllergy map(ResultSet rs) throws SQLException {
        return new UserAllergy(rs.getInt("id_usuario"), rs.getInt("id_alergia"),
                rs.getString("severidad"), rs.getString("observaciones"));
    }
}
