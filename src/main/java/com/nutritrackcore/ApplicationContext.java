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
import com.nutritrackcore.services.AccountDeletionService;
import com.nutritrackcore.services.AllergyService;
import com.nutritrackcore.services.AuthenticationService;
import com.nutritrackcore.services.BmiService;
import com.nutritrackcore.services.CalorieRequirementCalculator;
import com.nutritrackcore.services.ConditionRuleCatalog;
import com.nutritrackcore.services.ConsumptionService;
import com.nutritrackcore.services.DietPlanService;
import com.nutritrackcore.services.ExerciseRoutineService;
import com.nutritrackcore.services.ExerciseService;
import com.nutritrackcore.services.FoodService;
import com.nutritrackcore.services.MedicalConditionService;
import com.nutritrackcore.services.MifflinStJeorCalculator;
import com.nutritrackcore.services.PasswordHasher;
import com.nutritrackcore.services.QueryService;
import com.nutritrackcore.services.RoutineFollowUpService;
import com.nutritrackcore.services.Sha256PasswordHasher;
import com.nutritrackcore.services.UserProfileService;
import com.nutritrackcore.services.UserService;
import com.nutritrackcore.services.UserSession;
import com.nutritrackcore.ui.AppServices;
import com.nutritrackcore.ui.MainWindow;

/**
 * Raíz de composición: el único lugar donde se crean las implementaciones
 * concretas (repositorios, cifrador, reglas, servicios) y se conectan
 * entre sí.
 * <p>
 * Los repositorios salen de {@code db.properties}: con {@code persistencia=jdbc}
 * se abre el pool de conexiones y se usan los repositorios JDBC; si no, los
 * archivos .txt. Nada más del proyecto se entera del cambio. Al salir hay que
 * llamar a {@link #close()} para cerrar el pool.
 * <p>
 * Los servicios que todavía no están en {@link AppServices} se exponen con
 * sus propios getters.
 */
public class ApplicationContext implements AutoCloseable {

    private final UserSession userSession;

    /** Pool de conexiones; {@code null} cuando se usan archivos .txt. */
    private final ConnectionPool connectionPool;

    /** Los 17 repositorios, de la implementación elegida. */
    private final Repositories repositories;
    private final AppServices services;

    private final QueryService queryService;
    private final UserProfileService userProfileService;
    private final AllergyService allergyService;
    private final RoutineFollowUpService followUpService;
    private final AccountDeletionService accountDeletionService;

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
        // Los demás repositorios se toman directo de repos al crear cada servicio.

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
        this.userProfileService = new UserProfileService(
                repos.userProfiles(), userConditionRepository, userRepository);

        ConsumptionService consumptionService = new ConsumptionService(
                consumptionRepository, repos.consumptionRecordFoods(), foodRepository, userRepository);
        this.queryService = new QueryService(consumptionRepository, repos.consumptionRecordFoods(),
                foodRepository, userRepository, userConditionRepository, bmiHistoryRepository, routineRepository);

        DietPlanService dietPlanService = new DietPlanService(dietPlanRepository, userRepository,
                foodRepository, conditionService, calorieCalculator, ruleCatalog, repos.userProfiles(),
                repos.allergies(), repos.userAllergies(), repos.allergyFoods(), repos.excludedFoods());
        this.allergyService = new AllergyService(repos.allergies(), repos.userAllergies(),
                userRepository, dietPlanService);

        ExerciseRoutineService routineService = new ExerciseRoutineService(routineRepository,
                exerciseRepository, bmiHistoryRepository, dietPlanRepository, userRepository,
                conditionService, ruleCatalog, repos.userProfiles());
        this.followUpService = new RoutineFollowUpService(
                repos.routineFollowUps(), routineRepository, routineService);

        this.accountDeletionService = new AccountDeletionService(userRepository, bmiHistoryRepository,
                userConditionRepository, consumptionRepository, dietPlanRepository, routineRepository,
                repos.routineFollowUps(), repos.userProfiles(), repos.userAllergies(), repos.excludedFoods());

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

    /** @return consultas del planteamiento del problema */
    public QueryService getQueryService() {
        return queryService;
    }

    /** @return perfil de hábitos del usuario */
    public UserProfileService getUserProfileService() {
        return userProfileService;
    }

    /** @return alergias del usuario (corrige el plan activo al agregar una) */
    public AllergyService getAllergyService() {
        return allergyService;
    }

    /** @return seguimiento de la rutina y progresión de repeticiones */
    public RoutineFollowUpService getFollowUpService() {
        return followUpService;
    }

    /** @return borrado de la cuenta y todo lo que depende de ella */
    public AccountDeletionService getAccountDeletionService() {
        return accountDeletionService;
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