package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.RoutineFollowUpRepository;
import com.nutritrackcore.repository.UserAllergyRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import com.nutritrackcore.repository.UserProfileRepository;
import com.nutritrackcore.repository.UserRepository;
import java.util.Objects;

/**
 * Borra la cuenta de un usuario y todo lo que depende de ella.
 */
public class AccountDeletionService {
    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final UserConditionRepository userConditionRepository;
    private final ConsumptionRecordRepository consumptionRecordRepository;
    private final DietPlanRepository dietPlanRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;
    private final RoutineFollowUpRepository followUpRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserExcludedFoodRepository excludedFoodRepository;

    public AccountDeletionService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                                  UserConditionRepository userConditionRepository,
                                  ConsumptionRecordRepository consumptionRecordRepository,
                                  DietPlanRepository dietPlanRepository,
                                  ExerciseRoutineRepository exerciseRoutineRepository,
                                  RoutineFollowUpRepository followUpRepository,
                                  UserProfileRepository userProfileRepository,
                                  UserAllergyRepository userAllergyRepository,
                                  UserExcludedFoodRepository excludedFoodRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.userConditionRepository = Objects.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.consumptionRecordRepository = Objects.requireNonNull(consumptionRecordRepository,
                "repositorio de consumos");
        this.dietPlanRepository = Objects.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.exerciseRoutineRepository = Objects.requireNonNull(exerciseRoutineRepository, "repositorio de rutinas");
        this.followUpRepository = Objects.requireNonNull(followUpRepository, "repositorio de seguimiento");
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository, "repositorio de perfiles");
        this.userAllergyRepository = Objects.requireNonNull(userAllergyRepository, "repositorio de alergias");
        this.excludedFoodRepository = Objects.requireNonNull(excludedFoodRepository,
                "repositorio de alimentos excluidos");
    }

    public void deleteAccount(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }
        for (ExerciseRoutine routine : exerciseRoutineRepository.findByUserId(userId)) {
            followUpRepository.deleteByRoutineId(routine.getRoutineId());
        }
        exerciseRoutineRepository.deleteByUserId(userId);
        dietPlanRepository.deleteByUserId(userId);
        consumptionRecordRepository.deleteByUserId(userId);
        userConditionRepository.deleteByUserId(userId);
        userAllergyRepository.deleteByUserId(userId);
        excludedFoodRepository.deleteByUserId(userId);
        userProfileRepository.deleteByUserId(userId);
        bmiHistoryRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}