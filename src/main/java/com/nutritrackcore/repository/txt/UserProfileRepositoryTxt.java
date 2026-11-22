package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link UserProfileRepository} sobre
 * {@code user_profiles.txt} (tabla {@code PerfilUsuario}).
 * <p>
 * Formato:
 * {@code id_usuario|nivel_actividad|experiencia_ejercicio|objetivo_peso|dias_entrenamiento_semana|minutos_por_sesion|lugar_entrenamiento|tipo_dieta|comidas_al_dia|fecha_actualizacion}.
 */
public class UserProfileRepositoryTxt extends AbstractTxtRepository<UserProfile> implements UserProfileRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_usuario|nivel_actividad|experiencia_ejercicio|objetivo_peso"
            + "|dias_entrenamiento_semana|minutos_por_sesion|lugar_entrenamiento|tipo_dieta|comidas_al_dia"
            + "|fecha_actualizacion";

    /**
     * @param file archivo {@code user_profiles.txt}
     */
    public UserProfileRepositoryTxt(TxtFile file) {
        super(file, "user_profiles.txt", 10);
    }

    @Override
    public void save(UserProfile profile) throws PersistenceExceptions {
        List<UserProfile> all = readAll();
        for (UserProfile p : all) {                      // relación 1:1 con Usuario
            if (p.getUserId() == profile.getUserId()) {
                throw new PersistenceExceptions("El usuario " + profile.getUserId() + " ya tiene perfil");
            }
        }
        all.add(profile);
        writeAll(all);
    }

    @Override
    public void update(UserProfile profile) throws PersistenceExceptions {
        List<UserProfile> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUserId() == profile.getUserId()) {
                all.set(i, profile);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("El usuario " + profile.getUserId() + " no tiene perfil");
    }

    @Override
    public Optional<UserProfile> findByUserId(int userId) throws PersistenceExceptions {
        for (UserProfile p : readAll()) {
            if (p.getUserId() == userId) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<UserProfile> all = readAll();
        if (all.removeIf(p -> p.getUserId() == userId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(UserProfile p) {
        return TxtFormat.join(
                String.valueOf(p.getUserId()),
                p.getActivityLevel(),
                p.getExerciseExperience(),
                p.getWeightGoal(),
                String.valueOf(p.getTrainingDaysPerWeek()),
                String.valueOf(p.getMinutesPerSession()),
                p.getTrainingLocation(),
                p.getDietType(),
                String.valueOf(p.getMealsPerDay()),
                TxtFormat.text(p.getUpdatedAt()));
    }

    @Override
    protected UserProfile fromFields(String[] c) {
        UserProfile p = new UserProfile();
        p.setUserId(Integer.parseInt(c[0]));
        p.setActivityLevel(TxtFormat.nullIfEmpty(c[1]));
        p.setExerciseExperience(TxtFormat.nullIfEmpty(c[2]));
        p.setWeightGoal(TxtFormat.nullIfEmpty(c[3]));
        p.setTrainingDaysPerWeek(Integer.parseInt(c[4]));
        p.setMinutesPerSession(Integer.parseInt(c[5]));
        p.setTrainingLocation(TxtFormat.nullIfEmpty(c[6]));
        p.setDietType(TxtFormat.nullIfEmpty(c[7]));
        p.setMealsPerDay(Integer.parseInt(c[8]));
        p.setUpdatedAt(TxtFormat.parseDate(c[9]));
        return p;
    }
}
