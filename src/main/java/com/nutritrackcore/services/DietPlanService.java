package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.model.AllergyFood;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlan.DietType;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.User;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.repository.AllergyFoodRepository;
import com.nutritrackcore.repository.AllergyRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserAllergyRepository;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import com.nutritrackcore.repository.UserProfileRepository;
import com.nutritrackcore.repository.UserRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Genera el plan de dieta y lo mantiene libre de alérgenos.
 * <p>
 * Reglas: tipo de dieta del perfil (grupos de la pirámide permitidos), comidas al día
 * del perfil (1 a 4), reglas por condición médica (incluido el sobrepeso por IMC),
 * alergias y alimentos excluidos. Con condiciones médicas: dieta omnívora y 4 comidas.
 */
public class DietPlanService {
    private static final double MIN_TARGET_KCAL = 1200;
    private static final double MIN_GRAMS = 20;
    private static final double MAX_GRAMS = 500;
    private static final int DEFAULT_MEALS = 4;

    /** Comidas y reparto de kcal según la cantidad de comidas al día (1 a 4, como la base de datos). */
    private static final Map<Integer, MealTime[]> MEALS_BY_COUNT = Map.of(
            1, new MealTime[] {MealTime.LUNCH},
            2, new MealTime[] {MealTime.LUNCH, MealTime.DINNER},
            3, new MealTime[] {MealTime.BREAKFAST, MealTime.LUNCH, MealTime.DINNER},
            4, new MealTime[] {MealTime.BREAKFAST, MealTime.LUNCH, MealTime.SNACK, MealTime.DINNER});
    private static final Map<Integer, double[]> SHARES_BY_COUNT = Map.of(
            1, new double[] {1.0},
            2, new double[] {0.55, 0.45},
            3, new double[] {0.30, 0.40, 0.30},
            4, new double[] {0.25, 0.35, 0.15, 0.25});

    private final DietPlanRepository dietPlanRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final MedicalConditionService medicalConditionService;
    private final CalorieRequirementCalculator calorieCalculator;
    private final ConditionRuleCatalog ruleCatalog;
    private final UserProfileRepository profileRepository;
    private final AllergyRepository allergyRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final AllergyFoodRepository allergyFoodRepository;
    private final UserExcludedFoodRepository excludedFoodRepository;

    public DietPlanService(DietPlanRepository dietPlanRepository, UserRepository userRepository,
                           FoodRepository foodRepository, MedicalConditionService medicalConditionService,
                           CalorieRequirementCalculator calorieCalculator, ConditionRuleCatalog ruleCatalog,
                           UserProfileRepository profileRepository, AllergyRepository allergyRepository,
                           UserAllergyRepository userAllergyRepository, AllergyFoodRepository allergyFoodRepository,
                           UserExcludedFoodRepository excludedFoodRepository) {
        this.dietPlanRepository = Objects.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.foodRepository = Objects.requireNonNull(foodRepository, "repositorio de alimentos");
        this.medicalConditionService = Objects.requireNonNull(medicalConditionService, "servicio de condiciones");
        this.calorieCalculator = Objects.requireNonNull(calorieCalculator, "calculadora de kcal");
        this.ruleCatalog = Objects.requireNonNull(ruleCatalog, "catálogo de reglas");
        this.profileRepository = Objects.requireNonNull(profileRepository, "repositorio de perfiles");
        this.allergyRepository = Objects.requireNonNull(allergyRepository, "repositorio de alergias");
        this.userAllergyRepository = Objects.requireNonNull(userAllergyRepository, "repositorio de alergias del usuario");
        this.allergyFoodRepository = Objects.requireNonNull(allergyFoodRepository, "repositorio de alergia-alimento");
        this.excludedFoodRepository = Objects.requireNonNull(excludedFoodRepository, "repositorio de excluidos");
    }

    /** Genera un plan nuevo y desactiva el anterior. */
    public DietPlan generatePlan(int userId) throws NutriTrackException {
        User user = requireUser(userId);
        boolean hasConditions = !medicalConditionService.listActiveByUser(userId).isEmpty();
        Optional<UserProfile> profile = hasConditions ? Optional.empty() : profileRepository.findByUserId(userId);
        DietType dietType = profile.isPresent()
                ? ParameterChecks.requireDietType(profile.get().getDietType()) : DietType.OMNIVORA;
        int meals = profile.isPresent() ? profile.get().getMealsPerDay() : DEFAULT_MEALS;
        ParameterChecks.requireMealsPerDay(meals);

        List<ConditionRule> rules = rulesOf(user);
        double target = calorieCalculator.calculateDailyCalories(user, LocalDate.now());
        for (ConditionRule rule : rules) {
            target = rule.adjustCalories(target);
        }
        target = Math.max(MIN_TARGET_KCAL, Math.min(ParameterChecks.MAX_TARGET_KCAL, target));
        target = Math.round(target * 10.0) / 10.0;
        ParameterChecks.requireRange(target, ParameterChecks.MIN_TARGET_KCAL, ParameterChecks.MAX_TARGET_KCAL,
                "kcal objetivo");

        List<Food> candidates = allowedFoods(dietType, rules, blockedFoodIds(userId));
        List<DietPlanFood> lines = buildLines(candidates, target, meals, rules);

        deactivateActivePlans(userId);
        DietPlan plan = new DietPlan(userId, LocalDate.now(), target, dietType);
        plan.setLines(lines);
        return dietPlanRepository.save(plan);
    }

    public Optional<DietPlan> getActivePlan(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return dietPlanRepository.findActiveByUserId(userId);
    }

    public List<DietPlan> listHistory(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return dietPlanRepository.findByUserId(userId);
    }

    /**
     * Revisa el plan activo después de agregar una alergia (o un alimento excluido).
     * Cada alimento bloqueado se reemplaza por el alimento permitido con las kcal por 100 g
     * más parecidas (prefiriendo el mismo grupo), ajustando los gramos para mantener las kcal.
     * Si no hay ninguno permitido, el alimento se elimina definitivamente del plan.
     */
    public AllergyChangeResult refreshForRestrictions(int userId) throws NutriTrackException {
        User user = requireUser(userId);
        AllergyChangeResult result = new AllergyChangeResult();
        Optional<DietPlan> active = dietPlanRepository.findActiveByUserId(userId);
        if (active.isEmpty()) {
            return result;
        }
        DietPlan plan = active.get();
        Set<Integer> blocked = blockedFoodIds(userId);
        DietType dietType = plan.getDietType() == null ? DietType.OMNIVORA : plan.getDietType();
        List<Food> candidates = allowedFoods(dietType, rulesOf(user), blocked);

        List<DietPlanFood> lines = new ArrayList<>(plan.getLines());
        List<DietPlanFood> pending = new ArrayList<>();
        for (DietPlanFood line : lines) {
            if (blocked.contains(line.getFoodId())) {
                pending.add(line);
            }
        }
        if (pending.isEmpty()) {
            return result;
        }
        for (DietPlanFood line : pending) {
            lines.remove(line);
            Food original = foodRepository.findById(line.getFoodId()).orElse(null);
            String originalName = original == null ? "Alimento " + line.getFoodId() : original.getName();
            String meal = line.getMealTime() == null ? "" : line.getMealTime().getDisplayName();
            Optional<Food> replacement = original == null ? Optional.empty()
                    : closestReplacement(original, candidates, lines, line.getMealTime());
            if (replacement.isEmpty()) {
                result.addRemoved(originalName, meal);
                continue;
            }
            Food food = replacement.get();
            double grams = line.getQuantityG() * original.getCalories100g() / food.getCalories100g();
            grams = round(clamp(grams));
            DietPlanFood existing = findLine(lines, food.getFoodId(), line.getMealTime());
            if (existing != null) {
                existing.setQuantityG(round(clamp(existing.getQuantityG() + grams)));
            } else {
                lines.add(new DietPlanFood(plan.getPlanId(), food.getFoodId(), grams, line.getMealTime()));
            }
            result.addReplaced(originalName, food.getName(), meal);
        }
        plan.setLines(lines);
        dietPlanRepository.update(plan);       // reescribe las líneas en una transacción
        return result;
    }

    /** Reglas que aplican al usuario: sus condiciones activas y el sobrepeso detectado por IMC. */
    List<ConditionRule> rulesOf(User user) throws NutriTrackException {
        List<ConditionRule> rules = new ArrayList<>();
        for (MedicalCondition condition : medicalConditionService.listActiveByUser(user.getIdUser())) {
            ConditionRule rule = ruleCatalog.getFor(condition);
            if (!rules.contains(rule)) {
                rules.add(rule);
            }
        }
        BmiCategory category = BmiResult.calculate(user.getWeightKg(), user.getHeightCm()).getCategory();
        if (category == BmiCategory.OVERWEIGHT) {
            Optional<ConditionRule> overweight = ruleCatalog.findByName(OverweightRule.NAME);
            if (overweight.isPresent() && !rules.contains(overweight.get())) {
                rules.add(overweight.get());
            }
        }
        return rules;
    }

    /** Alimentos ligados a las alergias del usuario (o con el nombre de la alergia) y los que excluyó. */
    private Set<Integer> blockedFoodIds(int userId) throws NutriTrackException {
        Set<Integer> blocked = new HashSet<>();
        List<String> allergyNames = new ArrayList<>();
        for (UserAllergy userAllergy : userAllergyRepository.findByUserId(userId)) {
            for (AllergyFood link : allergyFoodRepository.findByAllergyId(userAllergy.getAllergyId())) {
                blocked.add(link.getFoodId());
            }
            allergyRepository.findById(userAllergy.getAllergyId())
                    .map(Allergy::getName).map(DietPlanService::normalize).ifPresent(allergyNames::add);
        }
        if (!allergyNames.isEmpty()) {
            for (Food food : foodRepository.findAll()) {
                String foodName = normalize(food.getName());
                for (String allergyName : allergyNames) {
                    if (!allergyName.isEmpty() && foodName.contains(allergyName)) {
                        blocked.add(food.getFoodId());
                    }
                }
            }
        }
        for (UserExcludedFood excluded : excludedFoodRepository.findByUserId(userId)) {
            blocked.add(excluded.getFoodId());
        }
        return blocked;
    }

    /** Alimentos activos permitidos por el tipo de dieta, las condiciones y que no estén bloqueados. */
    private List<Food> allowedFoods(DietType dietType, List<ConditionRule> rules, Set<Integer> blocked)
            throws NutriTrackException {
        List<Food> candidates = new ArrayList<>();
        for (Food food : foodRepository.findAllActive()) {
            if (food.getCalories100g() <= 0 || blocked.contains(food.getFoodId())
                    || food.getGrupe() == null || !dietType.getAllowedGroups().contains(food.getGrupe())) {
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
        candidates.sort(Comparator.comparing(f -> f.getName() == null ? "" : f.getName().toLowerCase(Locale.ROOT)));
        return candidates;
    }

    private List<DietPlanFood> buildLines(List<Food> candidates, double target, int meals, List<ConditionRule> rules)
            throws NutriTrackException {
        if (candidates.isEmpty()) {
            throw new BussinesRuleException(
                    "No hay alimentos activos que cumplan el tipo de dieta, las alergias y las condiciones del usuario.");
        }
        MealTime[] mealTimes = MEALS_BY_COUNT.get(meals);
        double[] shares = SHARES_BY_COUNT.get(meals);
        int foodsPerMeal = candidates.size() >= 2 * mealTimes.length ? 2 : 1;
        int mealsUsed = Math.min(mealTimes.length, candidates.size() / foodsPerMeal);
        double shareTotal = 0;
        for (int m = 0; m < mealsUsed; m++) {
            shareTotal += shares[m];
        }
        List<DietPlanFood> lines = new ArrayList<>();
        int cursor = 0;
        for (int m = 0; m < mealsUsed; m++) {
            double kcalPerFood = target * (shares[m] / shareTotal) / foodsPerMeal;
            for (int k = 0; k < foodsPerMeal; k++) {
                Food food = candidates.get(cursor++);
                double grams = kcalPerFood / food.getCalories100g() * 100.0;
                for (ConditionRule rule : rules) {
                    grams *= rule.groupFactor(food.getGrupe());      // límites por grupo de cada condición
                }
                lines.add(new DietPlanFood(0, food.getFoodId(), round(clamp(grams)), mealTimes[m]));
            }
        }
        return lines;
    }

    /** Alimento permitido con kcal por 100 g más parecidas, prefiriendo el mismo grupo. */
    private Optional<Food> closestReplacement(Food original, List<Food> candidates, List<DietPlanFood> lines,
                                              MealTime meal) {
        return candidates.stream()
                .filter(f -> f.getFoodId() != original.getFoodId())
                .min(Comparator.<Food>comparingInt(f -> f.getGrupe() == original.getGrupe() ? 0 : 1)
                        .thenComparingDouble(f -> Math.abs(f.getCalories100g() - original.getCalories100g()))
                        .thenComparingInt(f -> findLine(lines, f.getFoodId(), meal) == null ? 0 : 1));
    }

    private static DietPlanFood findLine(List<DietPlanFood> lines, int foodId, MealTime meal) {
        for (DietPlanFood line : lines) {
            if (line.getFoodId() == foodId && line.getMealTime() == meal) {
                return line;
            }
        }
        return null;
    }

    private void deactivateActivePlans(int userId) throws NutriTrackException {
        for (DietPlan previous : dietPlanRepository.findByUserId(userId)) {
            if (previous.isActive()) {
                previous.setActive(false);
                dietPlanRepository.update(previous);
            }
        }
    }

    private User requireUser(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundEntityException("No existe un usuario con el identificador " + userId + "."));
    }

    private static double clamp(double grams) {
        return Math.min(MAX_GRAMS, Math.max(MIN_GRAMS, grams));
    }

    private static double round(double grams) {
        return Math.round(grams * 10.0) / 10.0;
    }

    private static String normalize(String text) {
        String clean = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}