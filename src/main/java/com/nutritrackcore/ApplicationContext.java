package com.nutritrackcore;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.MedicalConditionRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.repository.txt.BMIHistoryRepositoryTxt;
import com.nutritrackcore.repository.txt.ConsumptionRecordRepositoryTxt;
import com.nutritrackcore.repository.txt.DietPlanRepositoryTxt;
import com.nutritrackcore.repository.txt.ExerciseRepositoryTxt;
import com.nutritrackcore.repository.txt.ExerciseRoutineRepositoryTxt;
import com.nutritrackcore.repository.txt.FoodRepositoryTxt;
import com.nutritrackcore.repository.txt.MedicalConditionRepositoryTxt;
import com.nutritrackcore.repository.txt.TxtFile;
import com.nutritrackcore.repository.txt.UserConditionRepositoryTxt;
import com.nutritrackcore.repository.txt.UserRepositoryTxt;
import com.nutritrackcore.services.AuthenticationService;
import com.nutritrackcore.services.BmiService;
import com.nutritrackcore.services.ConsumptionService;
import com.nutritrackcore.services.DietPlanService;
import com.nutritrackcore.services.ExerciseRoutineService;
import com.nutritrackcore.services.ExerciseService;
import com.nutritrackcore.services.FoodService;
import com.nutritrackcore.services.MedicalConditionService;
import com.nutritrackcore.services.PasswordHasher;
import com.nutritrackcore.services.Sha256PasswordHasher;
import com.nutritrackcore.services.UserService;
import com.nutritrackcore.services.UserSession;
import com.nutritrackcore.services.calculation.CalorieRequirementCalculator;
import com.nutritrackcore.services.calculation.ConditionRuleCatalog;
import com.nutritrackcore.services.calculation.MifflinStJeorCalculator;
import com.nutritrackcore.ui.AppServices;
import com.nutritrackcore.ui.MainWindow;
import java.nio.file.Paths;

/**
 * Raíz de composición: el único lugar donde se crean las implementaciones
 * concretas (repositorios .txt, cifrador, reglas, servicios) y se conectan
 * entre sí.
 * <p>
 * Para pasar a base de datos solo se cambian aquí los {@code ...RepositoryTxt}
 * por sus versiones JDBC; nada más del proyecto se entera.
 */
public class ApplicationContext {

    private final UserSession userSession;
    private final AppServices services;

    /**
     * Crea todas las piezas de la aplicación.
     *
     * @param dataFolder carpeta donde viven los archivos .txt (se crea si no existe)
     * @throws PersistenceExceptions si no se pueden crear o abrir los archivos de datos
     */
    public ApplicationContext(String dataFolder) throws PersistenceExceptions {
        // 1. Repositorios (.txt)
        UserRepository userRepository = new UserRepositoryTxt(
                txt(dataFolder, "users.txt", UserRepositoryTxt.HEADER));
        BMIHistoryRepository bmiHistoryRepository = new BMIHistoryRepositoryTxt(
                txt(dataFolder, "bmi_records.txt", BMIHistoryRepositoryTxt.HEADER));
        MedicalConditionRepository conditionRepository = new MedicalConditionRepositoryTxt(
                txt(dataFolder, "medical_conditions.txt", MedicalConditionRepositoryTxt.HEADER));
        UserConditionRepository userConditionRepository = new UserConditionRepositoryTxt(
                txt(dataFolder, "user_conditions.txt", UserConditionRepositoryTxt.HEADER));
        FoodRepository foodRepository = new FoodRepositoryTxt(
                txt(dataFolder, "foods.txt", FoodRepositoryTxt.HEADER));
        ExerciseRepository exerciseRepository = new ExerciseRepositoryTxt(
                txt(dataFolder, "exercises.txt", ExerciseRepositoryTxt.HEADER));
        ConsumptionRecordRepository consumptionRepository = new ConsumptionRecordRepositoryTxt(
                txt(dataFolder, "consumption_records.txt", ConsumptionRecordRepositoryTxt.HEADER));
        DietPlanRepository dietPlanRepository = new DietPlanRepositoryTxt(
                txt(dataFolder, "diet_plans.txt", DietPlanRepositoryTxt.PLANS_HEADER),
                txt(dataFolder, "diet_plan_items.txt", DietPlanRepositoryTxt.LINES_HEADER));
        ExerciseRoutineRepository routineRepository = new ExerciseRoutineRepositoryTxt(
                txt(dataFolder, "exercise_routines.txt", ExerciseRoutineRepositoryTxt.ROUTINES_HEADER),
                txt(dataFolder, "routine_exercises.txt", ExerciseRoutineRepositoryTxt.EXERCISES_HEADER),
                txt(dataFolder, "routine_conditions.txt", ExerciseRoutineRepositoryTxt.CONDITIONS_HEADER));

        // 2. Seguridad, sesión y cálculo
        PasswordHasher passwordHasher = new Sha256PasswordHasher();
        this.userSession = new UserSession();
        CalorieRequirementCalculator calorieCalculator = new MifflinStJeorCalculator();
        ConditionRuleCatalog ruleCatalog = ConditionRuleCatalog.withDefaultRules();

        // 3. Servicios
        AuthenticationService authenticationService = new AuthenticationService(
                userRepository, bmiHistoryRepository, passwordHasher, userSession);
        UserService userService = new UserService(userRepository, bmiHistoryRepository, passwordHasher);
        BmiService bmiService = new BmiService(userRepository, bmiHistoryRepository);
        MedicalConditionService conditionService = new MedicalConditionService(
                conditionRepository, userConditionRepository, userRepository);
        FoodService foodService = new FoodService(foodRepository);
        ExerciseService exerciseService = new ExerciseService(exerciseRepository);
        ConsumptionService consumptionService = new ConsumptionService(
                consumptionRepository, foodRepository, userRepository);
        DietPlanService dietPlanService = new DietPlanService(dietPlanRepository, userRepository,
                foodRepository, conditionService, calorieCalculator, ruleCatalog);
        ExerciseRoutineService routineService = new ExerciseRoutineService(routineRepository,
                exerciseRepository, bmiHistoryRepository, dietPlanRepository, userRepository,
                conditionService, ruleCatalog);

        this.services = new AppServices(userSession, authenticationService, userService, bmiService,
                conditionService, foodService, exerciseService, consumptionService, dietPlanService,
                routineService);
    }

    /**
     * Crea la ventana principal con los servicios ya conectados.
     *
     * @return ventana lista para mostrarse
     */
    public MainWindow createMainWindow() {
        return new MainWindow(services);
    }

    /** @return la sesión compartida por servicios y pantallas */
    public UserSession getUserSession() {
        return userSession;
    }

    /** @return todos los servicios que usa la interfaz */
    public AppServices getServices() {
        return services;
    }

    /** @return el servicio de registro e inicio de sesión */
    public AuthenticationService getAuthenticationService() {
        return services.authentication();
    }

    private static TxtFile txt(String folder, String name, String header) throws PersistenceExceptions {
        return new TxtFile(Paths.get(folder, name).toString(), header);
    }
}
