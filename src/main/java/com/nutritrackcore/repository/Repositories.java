package com.nutritrackcore.repository;

/**
 * Agrupa los 17 repositorios de la aplicación.
 * <p>
 * Lo arma una fábrica ({@code TxtRepositories} o {@code JdbcRepositories}) y
 * lo recibe el {@code ApplicationContext}, que reparte cada repositorio a los
 * servicios. Como los campos son interfaces, los servicios no saben si los
 * datos están en archivos .txt o en PostgreSQL.
 */
public record Repositories(
        UserRepository users,
        BMIHistoryRepository bmiHistory,
        UserProfileRepository userProfiles,
        MedicalConditionRepository medicalConditions,
        MedicalConditionRuleRepository conditionRules,
        UserConditionRepository userConditions,
        AllergyRepository allergies,
        UserAllergyRepository userAllergies,
        AllergyFoodRepository allergyFoods,
        FoodRepository foods,
        UserExcludedFoodRepository excludedFoods,
        ExerciseRepository exercises,
        ConsumptionRecordRepository consumptionRecords,
        ConsumptionRecordFoodRepository consumptionRecordFoods,
        DietPlanRepository dietPlans,
        ExerciseRoutineRepository exerciseRoutines,
        RoutineFollowUpRepository routineFollowUps) {
}
