package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.calculation.CalorieRequirementCalculator;
import com.nutritrackcore.services.calculation.ConditionRule;
import com.nutritrackcore.services.calculation.ConditionRuleCatalog;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de los planes de dieta: generar uno nuevo, consultar el vigente
 * y ver el historial.
 * <p>
 * Un usuario puede tener varios planes a lo largo del tiempo, pero solo uno
 * <b>activo</b>: al generar uno nuevo, los anteriores se marcan como inactivos
 * (se conservan como historial).
 * <p>
 * Las cantidades y los alimentos que genera son una propuesta académica para
 * demostrar el flujo; no sustituyen la recomendación de un nutricionista.
 */
public class DietPlanService {

    /** Meta calórica mínima permitida, para no proponer planes peligrosamente bajos. */
    private static final double MIN_TARGET_CALORIES = 1200;

    /** Comidas del día, en orden. */
    private static final MealTime[] MEALS = {MealTime.BREAKFAST, MealTime.LUNCH, MealTime.DINNER, MealTime.SNACK};

    /** Fracción de la meta calórica que corresponde a cada comida (mismo orden que {@link #MEALS}). */
    private static final double[] MEAL_SHARES = {0.25, 0.35, 0.30, 0.10};

    /** Cantidad mínima, en gramos, que se propone de un alimento. */
    private static final double MIN_GRAMS = 20;

    /** Cantidad máxima, en gramos, que se propone de un alimento. */
    private static final double MAX_GRAMS = 500;

    private final DietPlanRepository dietPlanRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final MedicalConditionService medicalConditionService;
    private final CalorieRequirementCalculator calorieCalculator;
    private final ConditionRuleCatalog ruleCatalog;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param dietPlanRepository      repositorio de planes de dieta
     * @param userRepository          repositorio de usuarios
     * @param foodRepository          repositorio del catálogo de alimentos
     * @param medicalConditionService servicio que entrega las condiciones del usuario
     * @param calorieCalculator       fórmula del requerimiento calórico
     * @param ruleCatalog             catálogo de reglas por condición médica
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public DietPlanService(DietPlanRepository dietPlanRepository, UserRepository userRepository,
                           FoodRepository foodRepository, MedicalConditionService medicalConditionService,
                           CalorieRequirementCalculator calorieCalculator, ConditionRuleCatalog ruleCatalog) {
        this.dietPlanRepository = ParameterChecks.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.foodRepository = ParameterChecks.requireNonNull(foodRepository, "repositorio de alimentos");
        this.medicalConditionService = ParameterChecks.requireNonNull(medicalConditionService,
                "servicio de condiciones médicas");
        this.calorieCalculator = ParameterChecks.requireNonNull(calorieCalculator, "calculadora de calorías");
        this.ruleCatalog = ParameterChecks.requireNonNull(ruleCatalog, "catálogo de reglas");
    }

    /**
     * Genera un plan de dieta nuevo para un usuario y lo deja como el plan
     * activo.
     * <p>
     * Pasos: (1) valida el id y carga el usuario; (2) obtiene las reglas de sus
     * condiciones médicas activas; (3) calcula la meta calórica con la fórmula
     * inyectada y le aplica el ajuste de cada regla (nunca baja de 1200 kcal);
     * (4) selecciona los alimentos activos con calorías que cumplan <b>todas</b>
     * las reglas, ordenados por nombre; (5) reparte la meta entre desayuno
     * (25 %), almuerzo (35 %), cena (30 %) y merienda (10 %), usando un alimento
     * distinto en cada línea (la clave del plan es plan+alimento, así que un
     * alimento no puede repetirse) y calculando los gramos con una regla de tres
     * entre 20 g y 500 g; (6) desactiva los planes activos anteriores; (7) guarda
     * el plan nuevo como activo.
     *
     * @param userId identificador del usuario
     * @return el plan guardado, con su id y sus líneas
     * @throws IllegalArgumentException si el id es inválido o el usuario no existe
     * @throws IllegalStateException    si no hay alimentos activos que cumplan las restricciones del usuario
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public DietPlan generatePlan(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("No existe un usuario con el identificador " + userId + "."));

        List<ConditionRule> rules = rulesOf(userId);
        double target = calorieCalculator.calculateDailyCalories(user, LocalDate.now());
        for (ConditionRule rule : rules) {
            target = rule.adjustCalories(target);
        }
        target = Math.max(MIN_TARGET_CALORIES, Math.round(target * 10.0) / 10.0);

        List<Food> candidates = selectFoods(rules);
        List<DietPlanFood> lines = buildLines(candidates, target);

        deactivateActivePlans(userId);

        DietPlan plan = new DietPlan();
        plan.setUserId(userId);
        plan.setGenerationDate(LocalDate.now());
        plan.setTargetCalories(target);
        plan.setActive(true);
        plan.setLines(lines);
        return dietPlanRepository.save(plan);
    }

    /**
     * Obtiene el plan de dieta activo de un usuario.
     *
     * @param userId identificador del usuario
     * @return el plan activo más reciente, o vacío si el usuario no tiene ninguno activo
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public Optional<DietPlan> getActivePlan(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return dietPlanRepository.findActiveByUserId(userId);
    }

    /**
     * Lista todos los planes de un usuario (activos e inactivos), del más
     * antiguo al más reciente.
     *
     * @param userId identificador del usuario
     * @return historial de planes; vacío si no tiene ninguno
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<DietPlan> listHistory(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return dietPlanRepository.findByUserId(userId);
    }

    /**
     * Obtiene las reglas que aplican a las condiciones médicas <b>activas</b> de
     * un usuario. Las condiciones desactivadas del catálogo no restringen planes
     * nuevos.
     *
     * @param userId identificador del usuario
     * @return una regla por cada condición activa del usuario; vacía si no tiene
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private List<ConditionRule> rulesOf(int userId) throws PersistenceExceptions {
        List<ConditionRule> rules = new ArrayList<>();
        for (MedicalCondition condition : medicalConditionService.listByUser(userId)) {
            if (condition.isActive()) {
                rules.add(ruleCatalog.getFor(condition));
            }
        }
        return rules;
    }

    /**
     * Filtra el catálogo: deja los alimentos activos con calorías mayores que
     * cero que todas las reglas aceptan, ordenados por nombre sin distinguir
     * mayúsculas (para que el resultado sea predecible).
     *
     * @param rules reglas de las condiciones del usuario
     * @return alimentos candidatos
     * @throws IllegalStateException si no queda ningún alimento
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private List<Food> selectFoods(List<ConditionRule> rules) throws PersistenceExceptions {
        List<Food> candidates = new ArrayList<>();
        for (Food food : foodRepository.findAllActive()) {
            if (food.getCalories100g() <= 0) {
                continue;
            }
            boolean allowed = true;
            for (ConditionRule rule : rules) {
                if (!rule.isFoodAllowed(food)) {
                    allowed = false;
                    break;
                }
            }
            if (allowed) {
                candidates.add(food);
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "No hay alimentos activos que cumplan las restricciones de las condiciones del usuario.");
        }
        candidates.sort(Comparator.comparing(f -> f.getName() == null ? "" : f.getName().toLowerCase()));
        return candidates;
    }

    /**
     * Construye las líneas del plan repartiendo la meta calórica entre las
     * comidas.
     * <p>
     * Si hay al menos 8 alimentos candidatos se usan 2 por comida; si no, 1. Se
     * usan tantas comidas como alcancen los alimentos (máximo 4) y el porcentaje
     * de cada una se reescala para que las comidas usadas sumen el 100 %. Cada
     * alimento se usa una sola vez. Los gramos de cada línea salen de
     * {@code calorías asignadas / calorías por 100 g × 100}, limitados a
     * 20–500 g y redondeados a un decimal.
     *
     * @param candidates alimentos disponibles (no vacía)
     * @param target     meta calórica diaria
     * @return líneas del plan
     */
    private List<DietPlanFood> buildLines(List<Food> candidates, double target) {
        int foodsPerMeal = candidates.size() >= 2 * MEALS.length ? 2 : 1;
        int mealsUsed = Math.min(MEALS.length, candidates.size() / foodsPerMeal);
        double shareTotal = 0;
        for (int m = 0; m < mealsUsed; m++) {
            shareTotal += MEAL_SHARES[m];
        }

        List<DietPlanFood> lines = new ArrayList<>();
        int cursor = 0;
        for (int m = 0; m < mealsUsed; m++) {
            double caloriesPerFood = target * (MEAL_SHARES[m] / shareTotal) / foodsPerMeal;
            for (int k = 0; k < foodsPerMeal; k++) {
                Food food = candidates.get(cursor++);
                double grams = caloriesPerFood / food.getCalories100g() * 100.0;
                grams = Math.min(MAX_GRAMS, Math.max(MIN_GRAMS, grams));
                DietPlanFood line = new DietPlanFood();
                line.setFoodId(food.getFoodId());
                line.setQuantityG(Math.round(grams * 10.0) / 10.0);
                line.setMealTime(MEALS[m]);
                lines.add(line);
            }
        }
        return lines;
    }

    /**
     * Marca como inactivos todos los planes activos del usuario, conservándolos
     * como historial.
     *
     * @param userId identificador del usuario
     * @throws PersistenceExceptions si falla la lectura o escritura del almacenamiento
     */
    private void deactivateActivePlans(int userId) throws PersistenceExceptions {
        for (DietPlan previous : dietPlanRepository.findByUserId(userId)) {
            if (previous.isActive()) {
                previous.setActive(false);
                dietPlanRepository.update(previous);
            }
        }
    }
}