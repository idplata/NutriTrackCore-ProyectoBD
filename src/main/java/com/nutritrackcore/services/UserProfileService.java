package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.User;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserProfileRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Perfil de hábitos, solo para usuarios sin condiciones médicas.
 * Los textos se guardan en minúsculas y el tipo de dieta en mayúsculas, como los pide la base de datos.
 */
public class UserProfileService {
    private final UserProfileRepository profileRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository,
                              UserConditionRepository userConditionRepository, UserRepository userRepository) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "repositorio de perfiles");
        this.userConditionRepository = Objects.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
    }

    /** Crea o actualiza el perfil del usuario. */
    public UserProfile save(UserProfile profile) throws NutriTrackException {
        ParameterChecks.requireNonNull(profile, "perfil de usuario");
        int userId = profile.getUserId();
        ParameterChecks.requireId(userId, "identificador del usuario");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundEntityException("No existe un usuario con el identificador " + userId + "."));
        if (!userConditionRepository.findByUserId(userId).isEmpty()) {
            throw new BussinesRuleException(
                    "Los usuarios con condiciones médicas no llenan el perfil de hábitos: el sistema los asigna.");
        }
        profile.setActivityLevel(ParameterChecks.requireLowerOption(
                profile.getActivityLevel(), ParameterChecks.ACTIVITY_LEVELS, "nivel de actividad"));
        profile.setExerciseExperience(ParameterChecks.requireLowerOption(
                profile.getExerciseExperience(), ParameterChecks.EXPERIENCE_LEVELS, "experiencia"));
        profile.setWeightGoal(ParameterChecks.requireLowerOption(
                profile.getWeightGoal(), ParameterChecks.WEIGHT_GOALS, "objetivo de peso"));
        if ("subir".equals(profile.getWeightGoal()) && BmiResult.calculate(user.getWeightKg(), user.getHeightCm())
                .getValue() >= BmiCategory.OVERWEIGHT.getMin()) {
            throw new BussinesRuleException("Con sobrepeso u obesidad no se puede elegir el objetivo de subir de peso.");
        }
        profile.setTrainingLocation(ParameterChecks.requireLowerOption(
                profile.getTrainingLocation(), ParameterChecks.TRAINING_LOCATIONS, "lugar de entrenamiento"));
        profile.setDietType(ParameterChecks.requireDietType(profile.getDietType()).name());
        ParameterChecks.requireMealsPerDay(profile.getMealsPerDay());
        ParameterChecks.requireRange(profile.getTrainingDaysPerWeek(), 0, ParameterChecks.MAX_TRAINING_DAYS,
                "días de entrenamiento por semana");
        ParameterChecks.requirePositive(profile.getMinutesPerSession(), "minutos por sesión");
        profile.setUpdatedAt(LocalDate.now());

        if (profileRepository.findByUserId(userId).isPresent()) {
            profileRepository.update(profile);
        } else {
            profileRepository.save(profile);
        }
        return profile;
    }

    public Optional<UserProfile> find(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return profileRepository.findByUserId(userId);
    }

    /** Indica si el nivel de actividad del perfil es bajo (sedentario o ligero). */
    public static boolean isLowActivity(UserProfile profile) {
        return profile != null && profile.getActivityLevel() != null
                && ParameterChecks.LOW_ACTIVITY_LEVELS.contains(profile.getActivityLevel().toLowerCase());
    }
}