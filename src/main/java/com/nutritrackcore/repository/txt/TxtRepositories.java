package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import com.nutritrackcore.repository.Repositories;
import java.nio.file.Paths;

/**
 * Fábrica de los 17 repositorios .txt. Crea (si no existen) los archivos de la
 * carpeta de datos, cada uno con su cabecera.
 */
public final class TxtRepositories {

    private TxtRepositories() {
    }

    /**
     * Crea los repositorios sobre archivos .txt.
     *
     * @param dataFolder carpeta de los archivos, por ejemplo {@code data}
     * @return los 17 repositorios agrupados
     * @throws PersistenceExceptions si no se puede preparar algún archivo
     */
    public static Repositories create(String dataFolder) throws PersistenceExceptions {
        ConsumptionRecordFoodRepository consumptionFoods = new ConsumptionRecordFoodRepositoryTxt(
                txt(dataFolder, "consumption_record_foods.txt", ConsumptionRecordFoodRepositoryTxt.HEADER));
        return new Repositories(
                new UserRepositoryTxt(txt(dataFolder, "users.txt", UserRepositoryTxt.HEADER)),
                new BMIHistoryRepositoryTxt(txt(dataFolder, "bmi_records.txt", BMIHistoryRepositoryTxt.HEADER)),
                new UserProfileRepositoryTxt(txt(dataFolder, "user_profiles.txt", UserProfileRepositoryTxt.HEADER)),
                new MedicalConditionRepositoryTxt(
                        txt(dataFolder, "medical_conditions.txt", MedicalConditionRepositoryTxt.HEADER)),
                new MedicalConditionRuleRepositoryTxt(
                        txt(dataFolder, "condition_rules.txt", MedicalConditionRuleRepositoryTxt.HEADER)),
                new UserConditionRepositoryTxt(
                        txt(dataFolder, "user_conditions.txt", UserConditionRepositoryTxt.HEADER)),
                new AllergyRepositoryTxt(txt(dataFolder, "allergies.txt", AllergyRepositoryTxt.HEADER)),
                new UserAllergyRepositoryTxt(txt(dataFolder, "user_allergies.txt", UserAllergyRepositoryTxt.HEADER)),
                new AllergyFoodRepositoryTxt(txt(dataFolder, "allergy_foods.txt", AllergyFoodRepositoryTxt.HEADER)),
                new FoodRepositoryTxt(txt(dataFolder, "foods.txt", FoodRepositoryTxt.HEADER)),
                new UserExcludedFoodRepositoryTxt(
                        txt(dataFolder, "user_excluded_foods.txt", UserExcludedFoodRepositoryTxt.HEADER)),
                new ExerciseRepositoryTxt(txt(dataFolder, "exercises.txt", ExerciseRepositoryTxt.HEADER)),
                new ConsumptionRecordRepositoryTxt(
                        txt(dataFolder, "consumption_records.txt", ConsumptionRecordRepositoryTxt.HEADER),
                        consumptionFoods),                   // guarda comidas completas y borra en cascada
                consumptionFoods,
                new DietPlanRepositoryTxt(
                        txt(dataFolder, "diet_plans.txt", DietPlanRepositoryTxt.PLANS_HEADER),
                        txt(dataFolder, "diet_plan_items.txt", DietPlanRepositoryTxt.LINES_HEADER)),
                new ExerciseRoutineRepositoryTxt(
                        txt(dataFolder, "exercise_routines.txt", ExerciseRoutineRepositoryTxt.ROUTINES_HEADER),
                        txt(dataFolder, "routine_exercises_reps.txt", ExerciseRoutineRepositoryTxt.REPS_HEADER),
                        txt(dataFolder, "routine_exercises_time.txt", ExerciseRoutineRepositoryTxt.TIME_HEADER),
                        txt(dataFolder, "routine_conditions.txt", ExerciseRoutineRepositoryTxt.CONDITIONS_HEADER)),
                new RoutineFollowUpRepositoryTxt(
                        txt(dataFolder, "routine_follow_ups.txt", RoutineFollowUpRepositoryTxt.HEADER)));
    }

    /** Prepara un archivo de la carpeta de datos con su cabecera. */
    private static TxtFile txt(String folder, String name, String header) throws PersistenceExceptions {
        return new TxtFile(Paths.get(folder, name).toString(), header);
    }
}
