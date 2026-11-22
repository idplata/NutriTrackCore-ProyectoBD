package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registro de consumo: un encabezado (la comida) con sus líneas (alimento y gramos).
 * <p>
 * El encabezado y las líneas se guardan en una sola transacción: si algo falla o el
 * programa se cierra a la mitad, la base de datos deshace todo y no queda nada guardado.
 */
public class ConsumptionService {
    private final ConsumptionRecordRepository recordRepository;
    private final ConsumptionRecordFoodRepository lineRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    public ConsumptionService(ConsumptionRecordRepository recordRepository,
                              ConsumptionRecordFoodRepository lineRepository,
                              FoodRepository foodRepository, UserRepository userRepository) {
        this.recordRepository = Objects.requireNonNull(recordRepository, "repositorio de consumos");
        this.lineRepository = Objects.requireNonNull(lineRepository, "repositorio de líneas de consumo");
        this.foodRepository = Objects.requireNonNull(foodRepository, "repositorio de alimentos");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
    }

    /**
     * Registra una comida estimada por la cámara.
     *
     * @param estimationLevel confianza de la cámara, de 0 a 1
     * @param foods           líneas con id del alimento y gramos (el id de registro lo pone el repositorio)
     */
    public ConsumptionRecord register(int userId, LocalDateTime consumedAt, Double estimationLevel,
                                      List<ConsumptionRecordByFood> foods) throws NutriTrackException {
        ParameterChecks.requireNonNull(estimationLevel, "nivel de estimación");
        ParameterChecks.requireRange(estimationLevel, 0.0, 1.0, "nivel de estimación");
        return saveRecord(userId, consumedAt, estimationLevel, foods);
    }

    /** Registra una comida escrita a mano (sin nivel de estimación). */
    public ConsumptionRecord registerManual(int userId, LocalDateTime consumedAt,
                                            List<ConsumptionRecordByFood> foods) throws NutriTrackException {
        return saveRecord(userId, consumedAt, null, foods);
    }

    private ConsumptionRecord saveRecord(int userId, LocalDateTime consumedAt, Double estimationLevel,
                                         List<ConsumptionRecordByFood> foods) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNotFuture(consumedAt, "fecha y hora del consumo");
        ParameterChecks.requireNonNull(foods, "alimentos consumidos");
        if (foods.isEmpty()) {
            throw new ValidationException("Un registro de consumo debe tener al menos un alimento.");
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }

        double kcal = 0;
        Set<Integer> seen = new HashSet<>();
        for (ConsumptionRecordByFood line : foods) {
            ParameterChecks.requireNonNull(line, "alimento consumido");
            ParameterChecks.requireId(line.getFoodId(), "identificador del alimento");
            ParameterChecks.requireConsumedGrams(line.getEstimatedGrams());
            if (!seen.add(line.getFoodId())) {
                throw new ValidationException("El mismo alimento no puede aparecer dos veces en una comida.");
            }
            Food food = requireActiveFood(line.getFoodId());
            kcal += NutritionCalculator.calculate(food, line.getEstimatedGrams()).getCalories();
        }

        ConsumptionRecord record = new ConsumptionRecord(userId, Math.round(kcal * 100.0) / 100.0, consumedAt);
        record.setEstimationLevel(estimationLevel);
        return recordRepository.save(record, foods);      // encabezado + líneas en una transacción
    }

    public List<ConsumptionRecord> listByDay(int userId, LocalDate date) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNonNull(date, "fecha");
        return recordRepository.findByUserIdAndDate(userId, date);
    }

    /** Alimentos y gramos de un registro. */
    public List<ConsumptionRecordByFood> listFoods(int recordId) throws NutriTrackException {
        ParameterChecks.requireId(recordId, "identificador del registro");
        return lineRepository.findByRecordId(recordId);
    }

    /** kcal y macros consumidos en un día, sumando las líneas de todos sus registros. */
    public NutritionalSummary getDailySummary(int userId, LocalDate date) throws NutriTrackException {
        Map<Integer, Food> foodCache = new HashMap<>();
        NutritionalSummary total = NutritionalSummary.empty();
        for (ConsumptionRecord record : listByDay(userId, date)) {
            for (ConsumptionRecordByFood line : lineRepository.findByRecordId(record.getRecordId())) {
                Food food = foodCache.get(line.getFoodId());
                if (food == null) {
                    food = foodRepository.findById(line.getFoodId()).orElseThrow(() -> new NotFoundEntityException(
                            "El registro " + record.getRecordId() + " usa un alimento que ya no existe."));
                    foodCache.put(line.getFoodId(), food);
                }
                total = total.add(NutritionCalculator.calculate(food, line.getEstimatedGrams()));
            }
        }
        return total;
    }

    /** Borra un registro; sus líneas se borran en cascada. */
    public void delete(int recordId) throws NutriTrackException {
        ParameterChecks.requireId(recordId, "identificador del registro");
        if (recordRepository.findById(recordId).isEmpty()) {
            throw new NotFoundEntityException("No existe un registro de consumo con el identificador " + recordId + ".");
        }
        recordRepository.deleteById(recordId);
    }

    private Food requireActiveFood(int foodId) throws NutriTrackException {
        Food food = foodRepository.findById(foodId).orElseThrow(
                () -> new NotFoundEntityException("No existe un alimento con el identificador " + foodId + "."));
        if (!food.isActive()) {
            throw new BussinesRuleException("El alimento '" + food.getName() + "' está desactivado y no se puede registrar.");
        }
        return food;
    }

    /** Crea una línea de consumo sin id de registro (lo asigna el repositorio al guardar). */
    public static ConsumptionRecordByFood line(int foodId, double grams) {
        ConsumptionRecordByFood line = new ConsumptionRecordByFood();
        line.setFoodId(foodId);
        line.setEstimatedGrams(grams);
        return line;
    }
}