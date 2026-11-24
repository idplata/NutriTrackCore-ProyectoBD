package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Sex;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link UserRepository} sobre la tabla {@code Usuario}.
 * <p>
 * El id lo genera la base ({@code identity}) y se recupera con
 * {@code RETURNING}. Si {@code registerDate} es nulo se usa {@code now()}. El
 * sexo se guarda como su código de una letra ({@code M}, {@code F}, {@code O}).
 * Borrar un usuario borra en cascada todos sus datos ({@code ON DELETE CASCADE}).
 */
public class UserRepositoryJdbc extends JdbcRepository implements UserRepository {

    private static final String COLUMNS =
            "id_usuario, nombre, email, contrasena_hash, peso_kg, altura_cm, fecha_nacimiento, sexo, fecha_registro";

    /** @param dataSource fuente de conexiones (el pool) */
    public UserRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public User save(User user) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO Usuario (nombre, email, contrasena_hash, peso_kg, altura_cm,
                                     fecha_nacimiento, sexo, fecha_registro)
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, now()))
                RETURNING id_usuario, fecha_registro
                """, ps -> {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPasswordHash());
                    ps.setDouble(4, user.getWeightKg());
                    ps.setDouble(5, user.getHeightCm());
                    setDate(ps, 6, user.getBirthDate());
                    ps.setString(7, user.getSex() == null ? null : user.getSex().getCode());
                    setDateTime(ps, 8, user.getRegisterDate());
                }, rs -> {
                    user.setIdUser(rs.getInt("id_usuario"));
                    user.setRegisterDate(getDateTime(rs, "fecha_registro"));
                    return user;
                });
    }

    /** {@inheritDoc} La fecha de registro no se modifica. */
    @Override
    public void update(User user) throws PersistenceExceptions {
        updateExisting("""
                UPDATE Usuario SET nombre = ?, email = ?, contrasena_hash = ?, peso_kg = ?,
                                   altura_cm = ?, fecha_nacimiento = ?, sexo = ?
                WHERE id_usuario = ?
                """, ps -> {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPasswordHash());
                    ps.setDouble(4, user.getWeightKg());
                    ps.setDouble(5, user.getHeightCm());
                    setDate(ps, 6, user.getBirthDate());
                    ps.setString(7, user.getSex() == null ? null : user.getSex().getCode());
                    ps.setInt(8, user.getIdUser());
                }, "No existe el usuario con id " + user.getIdUser());
    }

    @Override
    public Optional<User> findById(int idUser) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Usuario WHERE id_usuario = ?",
                ps -> ps.setInt(1, idUser), this::map);
    }

    /** {@inheritDoc} La comparación no distingue mayúsculas. */
    @Override
    public Optional<User> findByEmail(String email) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Usuario WHERE lower(email) = lower(?)",
                ps -> ps.setString(1, email), this::map);
    }

    @Override
    public List<User> findAll() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Usuario ORDER BY id_usuario", NO_PARAMS, this::map);
    }

    /** {@inheritDoc} La base borra en cascada perfil, planes, rutinas, consumos, etc. */
    @Override
    public void deleteById(int idUser) throws PersistenceExceptions {
        update("DELETE FROM Usuario WHERE id_usuario = ?", ps -> ps.setInt(1, idUser));
    }

    /** Convierte una fila de {@code Usuario} en un {@link User}. */
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setIdUser(rs.getInt("id_usuario"));
        u.setName(rs.getString("nombre"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("contrasena_hash"));
        u.setWeightKg(rs.getDouble("peso_kg"));
        u.setHeightCm(rs.getDouble("altura_cm"));
        u.setBirthDate(getDate(rs, "fecha_nacimiento"));
        String sex = rs.getString("sexo");
        u.setSex(sex == null ? null : Sex.fromCode(sex.trim()));   // char(1)
        u.setRegisterDate(getDateTime(rs, "fecha_registro"));
        return u;
    }
}
