package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.repository.UserProfileRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link UserProfileRepository} sobre la tabla
 * {@code PerfilUsuario} (1:1 con {@code Usuario}: {@code id_usuario} es la
 * clave). Si {@code updatedAt} es nulo se usa {@code current_date}.
 */
public class UserProfileRepositoryJdbc extends JdbcRepository implements UserProfileRepository {

    private static final String COLUMNS = "id_usuario, nivel_actividad, experiencia_ejercicio, objetivo_peso,"
            + " dias_entrenamiento_semana, minutos_por_sesion, lugar_entrenamiento, tipo_dieta, comidas_al_dia,"
            + " fecha_actualizacion";

    /** @param dataSource fuente de conexiones (el pool) */
    public UserProfileRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void save(UserProfile profile) throws PersistenceExceptions {
        insertReturning("""
                INSERT INTO PerfilUsuario (nivel_actividad, experiencia_ejercicio, objetivo_peso,
                        dias_entrenamiento_semana, minutos_por_sesion, lugar_entrenamiento, tipo_dieta,
                        comidas_al_dia, fecha_actualizacion, id_usuario)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, current_date), ?)
                RETURNING fecha_actualizacion
                """, ps -> bindFields(ps, profile), rs -> {
                    profile.setUpdatedAt(getDate(rs, "fecha_actualizacion"));
                    return profile;
                });
    }

    @Override
    public void update(UserProfile profile) throws PersistenceExceptions {
        updateExisting("""
                UPDATE PerfilUsuario SET nivel_actividad = ?, experiencia_ejercicio = ?, objetivo_peso = ?,
                       dias_entrenamiento_semana = ?, minutos_por_sesion = ?, lugar_entrenamiento = ?,
                       tipo_dieta = ?, comidas_al_dia = ?, fecha_actualizacion = COALESCE(?, current_date)
                WHERE id_usuario = ?
                """, ps -> bindFields(ps, profile), "El usuario " + profile.getUserId() + " no tiene perfil");
    }

    @Override
    public Optional<UserProfile> findByUserId(int userId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM PerfilUsuario WHERE id_usuario = ?",
                ps -> ps.setInt(1, userId), this::map);
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM PerfilUsuario WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    /** Asigna los parámetros 1 a 9 con los datos y el 10 con el id del usuario. */
    private static void bindFields(PreparedStatement ps, UserProfile p) throws SQLException {
        ps.setString(1, p.getActivityLevel());
        ps.setString(2, p.getExerciseExperience());
        ps.setString(3, p.getWeightGoal());
        ps.setInt(4, p.getTrainingDaysPerWeek());
        ps.setInt(5, p.getMinutesPerSession());
        ps.setString(6, p.getTrainingLocation());
        ps.setString(7, p.getDietType());
        ps.setInt(8, p.getMealsPerDay());
        setDate(ps, 9, p.getUpdatedAt());
        ps.setInt(10, p.getUserId());
    }

    /** Convierte una fila de {@code PerfilUsuario} en un {@link UserProfile}. */
    private UserProfile map(ResultSet rs) throws SQLException {
        UserProfile p = new UserProfile();
        p.setUserId(rs.getInt("id_usuario"));
        p.setActivityLevel(rs.getString("nivel_actividad"));
        p.setExerciseExperience(rs.getString("experiencia_ejercicio"));
        p.setWeightGoal(rs.getString("objetivo_peso"));
        p.setTrainingDaysPerWeek(rs.getInt("dias_entrenamiento_semana"));
        p.setMinutesPerSession(rs.getInt("minutos_por_sesion"));
        p.setTrainingLocation(rs.getString("lugar_entrenamiento"));
        p.setDietType(rs.getString("tipo_dieta"));
        p.setMealsPerDay(rs.getInt("comidas_al_dia"));
        p.setUpdatedAt(getDate(rs, "fecha_actualizacion"));
        return p;
    }
}
