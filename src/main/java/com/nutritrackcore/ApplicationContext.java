package com.nutritrackcore;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.MedicalConditionRepository;
import com.nutritrackcore.repository.Repositories;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.repository.jdbc.ConnectionPool;
import com.nutritrackcore.repository.jdbc.DatabaseConfig;
import com.nutritrackcore.repository.jdbc.JdbcRepositories;
import com.nutritrackcore.repository.txt.TxtRepositories;
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

/**
 * Raíz de composición: el único lugar donde se crean las implementaciones
 * concretas (repositorios .txt, cifrador, reglas, servicios) y se conectan
 * entre sí.
 * <p>
 * Los repositorios salen de {@code db.properties}: con {@code persistencia=jdbc}
 * se abre el pool de conexiones y se usan los repositorios JDBC; si no, los
 * archivos .txt. Nada más del proyecto se entera del cambio. Al salir hay que
 * llamar a {@link #close()} para cerrar el pool.
 */
public class ApplicationContext implements AutoCloseable {

    private final UserSession userSession;

    /** Pool de conexiones; {@code null} cuando se usan archivos .txt. */
    private final ConnectionPool connectionPool;

    /** Los 17 repositorios, de la implementación elegida. */
    private final Repositories repositories;
    private final AppServices services;

    /**
     * Crea todas las piezas de la aplicación.
     *
     * @param dataFolder carpeta donde viven los archivos .txt (se crea si no existe)
     * @throws PersistenceExceptions si no se pueden crear o abrir los archivos de datos
     */
    public ApplicationContext(String dataFolder) throws PersistenceExceptions {
        // 1. Repositorios: PostgreSQL (con pool) o archivos .txt, según db.properties
        DatabaseConfig config = DatabaseConfig.load();
        Repositories repos;
        if (config.useJdbc()) {
            this.connectionPool = new ConnectionPool(config);
            repos = JdbcRepositories.create(connectionPool.getDataSource());
        } else {
            this.connectionPool = null;
            repos = TxtRepositories.create(dataFolder);
        }
        this.repositories = repos;

        UserRepository userRepository = repos.users();
        BMIHistoryRepository bmiHistoryRepository = repos.bmiHistory();
        MedicalConditionRepository conditionRepository = repos.medicalConditions();
        UserConditionRepository userConditionRepository = repos.userConditions();
        FoodRepository foodRepository = repos.foods();
        ExerciseRepository exerciseRepository = repos.exercises();
        ConsumptionRecordRepository consumptionRepository = repos.consumptionRecords();
        DietPlanRepository dietPlanRepository = repos.dietPlans();
        ExerciseRoutineRepository routineRepository = repos.exerciseRoutines();
        // Los demás (alergias, perfil, reglas, seguimiento, detalle de consumo)
        // quedan en getRepositories() para los servicios que los vayan a usar.

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

    /** @return los 17 repositorios (.txt o JDBC, según la configuración) */
    public Repositories getRepositories() {
        return repositories;
    }

    /**
     * Cierra el pool de conexiones, si se está usando la base de datos. Se
     * llama una vez, al salir de la aplicación.
     */
    @Override
    public void close() {
        if (connectionPool != null) {
            connectionPool.close();
        }
    }
}
