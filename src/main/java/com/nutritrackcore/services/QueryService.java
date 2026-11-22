package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Consultas del planteamiento del problema. Lee el consumo como encabezado más líneas.
 */
public class QueryService {
    private final ConsumptionRecordRepository recordRepository;
    private final ConsumptionRecordFoodRepository lineRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final UserConditionRepository userConditionRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final ExerciseRoutineRepository routineRepository;

    public QueryService(ConsumptionRecordRepository recordRepository, ConsumptionRecordFoodRepository lineRepository,
                        FoodRepository foodRepository, UserRepository userRepository,
                        UserConditionRepository userConditionRepository,
                        BMIHistoryRepository bmiHistoryRepository, ExerciseRoutineRepository routineRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "repositorio de consumos");
        this.lineRepository = Objects.requireNonNull(lineRepository, "repositorio de líneas de consumo");
        this.foodRepository = Objects.requireNonNull(foodRepository, "repositorio de alimentos");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.userConditionRepository = Objects.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.routineRepository = Objects.requireNonNull(routineRepository, "repositorio de rutinas");
    }

    /** Gramos de un alimento que consumió un usuario en una fecha. */
    public double gramsConsumed(int userId, int foodId, LocalDate date) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(foodId, "identificador del alimento");
        ParameterChecks.requireNonNull(date, "fecha");
        double total = 0;
        for (ConsumptionRecord record : recordRepository.findByUserIdAndDate(userId, date)) {
            for (ConsumptionRecordByFood line : lineRepository.findByRecordId(record.getRecordId())) {
                if (line.getFoodId() == foodId) {
                    total += line.getEstimatedGrams();
                }
            }
        }
        return total;
    }

    /** Promedio de kcal diarias, usando las kcal guardadas en cada registro. */
    public double averageDailyCalories(int userId, LocalDate from, LocalDate to) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireDateRange(from, to);
        Map<LocalDate, Double> caloriesByDay = new TreeMap<>();
        for (ConsumptionRecord record : recordRepository.findByUserId(userId)) {
            LocalDate day = dayOf(record);
            if (day != null && !day.isBefore(from) && !day.isAfter(to)) {
                caloriesByDay.merge(day, record.getEstimatedCalories(), Double::sum);
            }
        }
        if (caloriesByDay.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (double dayTotal : caloriesByDay.values()) {
            sum += dayTotal;
        }
        return sum / caloriesByDay.size();
    }

    public List<User> usersWithCondition(int conditionId) throws NutriTrackException {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        List<User> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (userConditionRepository.find(user.getIdUser(), conditionId).isPresent()) {
                result.add(user);
            }
        }
        return result;
    }

    public List<FoodFrequency> mostFrequentFoods(int conditionId, int limit) throws NutriTrackException {
        if (limit <= 0) {
            throw new ValidationException("El límite debe ser mayor que cero.");
        }
        Map<Integer, int[]> times = new LinkedHashMap<>();       // foodId -> [veces]
        Map<Integer, Double> grams = new HashMap<>();
        for (User user : usersWithCondition(conditionId)) {
            for (ConsumptionRecord record : recordRepository.findByUserId(user.getIdUser())) {
                for (ConsumptionRecordByFood line : lineRepository.findByRecordId(record.getRecordId())) {
                    times.computeIfAbsent(line.getFoodId(), k -> new int[1])[0]++;
                    grams.merge(line.getFoodId(), line.getEstimatedGrams(), Double::sum);
                }
            }
        }
        List<FoodFrequency> result = new ArrayList<>();
        for (Map.Entry<Integer, int[]> entry : times.entrySet()) {
            Food food = requireFood(entry.getKey());
            result.add(new FoodFrequency(entry.getKey(), food.getName(), entry.getValue()[0], grams.get(entry.getKey())));
        }
        result.sort(Comparator.comparingInt(FoodFrequency::getTimes).reversed()
                .thenComparing(Comparator.comparingDouble(FoodFrequency::getTotalGrams).reversed()));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    public Map<LocalDate, Double> carbsTrend(int conditionId, LocalDate from, LocalDate to)
            throws NutriTrackException {
        ParameterChecks.requireDateRange(from, to);
        List<User> users = usersWithCondition(conditionId);
        Map<LocalDate, Double> trend = new TreeMap<>();
        if (users.isEmpty()) {
            return trend;
        }
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            trend.put(day, 0.0);
        }
        Map<Integer, Food> foodCache = new HashMap<>();
        for (User user : users) {
            for (ConsumptionRecord record : recordRepository.findByUserId(user.getIdUser())) {
                LocalDate day = dayOf(record);
                if (day == null || !trend.containsKey(day)) {
                    continue;
                }
                for (ConsumptionRecordByFood line : lineRepository.findByRecordId(record.getRecordId())) {
                    Food food = foodCache.get(line.getFoodId());
                    if (food == null) {
                        food = requireFood(line.getFoodId());
                        foodCache.put(line.getFoodId(), food);
                    }
                    trend.merge(day, NutritionCalculator.calculate(food, line.getEstimatedGrams()).getCarbohydrates(),
                            Double::sum);
                }
            }
        }
        for (Map.Entry<LocalDate, Double> entry : trend.entrySet()) {
            entry.setValue(entry.getValue() / users.size());
        }
        return trend;
    }

    public List<BMIHistory> bmiByCondition(int conditionId) throws NutriTrackException {
        List<BMIHistory> rows = new ArrayList<>();
        for (User user : usersWithCondition(conditionId)) {
            rows.addAll(bmiHistoryRepository.findByUserId(user.getIdUser()));
        }
        rows.sort(Comparator.comparing(BMIHistory::getRegisterDate, Comparator.nullsFirst(Comparator.naturalOrder())));
        return rows;
    }

    public List<ExerciseRoutine> routinesForCondition(int conditionId) throws NutriTrackException {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        List<ExerciseRoutine> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            for (ExerciseRoutine routine : routineRepository.findByUserId(user.getIdUser())) {
                for (ExerciseRoutineCondition considered : routine.getConsideredConditions()) {
                    if (considered.getConditionId() == conditionId) {
                        result.add(routine);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private LocalDate dayOf(ConsumptionRecord record) {
        return record.getDateTime() == null ? null : record.getDateTime().toLocalDate();
    }

    private Food requireFood(int foodId) throws NutriTrackException {
        return foodRepository.findById(foodId).orElseThrow(() -> new NotFoundEntityException(
                "Un registro de consumo usa el alimento " + foodId + ", que ya no existe en el catálogo."));
    }
}