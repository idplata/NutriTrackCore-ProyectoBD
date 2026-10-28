package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.calculation.NutritionCalculator;
import com.nutritrackcore.services.dto.FoodFrequency;
import com.nutritrackcore.services.dto.NutritionalSummary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consultas de solo lectura que responden las preguntas del planteamiento del
 * problema: cuánto comió un usuario, promedios de calorías, alimentos más
 * frecuentes y tendencias por condición médica.
 * <p>
 * Los repositorios no ofrecen consultas "por condición", así que este servicio
 * obtiene primero los usuarios que tienen la condición y filtra en memoria.
 * Cuando exista una base de datos se podrá reemplazar esa lógica por consultas
 * SQL sin cambiar los métodos públicos de esta clase.
 */
public class QueryService {

    private final ConsumptionRecordRepository recordRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final UserConditionRepository userConditionRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final ExerciseRoutineRepository routineRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param recordRepository        repositorio de registros de consumo
     * @param foodRepository          repositorio del catálogo de alimentos
     * @param userRepository          repositorio de usuarios
     * @param userConditionRepository repositorio de las condiciones de cada usuario
     * @param bmiHistoryRepository    repositorio del historial de IMC
     * @param routineRepository       repositorio de rutinas de ejercicio
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public QueryService(ConsumptionRecordRepository recordRepository, FoodRepository foodRepository,
                        UserRepository userRepository, UserConditionRepository userConditionRepository,
                        BMIHistoryRepository bmiHistoryRepository, ExerciseRoutineRepository routineRepository) {
        this.recordRepository = ParameterChecks.requireNonNull(recordRepository, "repositorio de consumos");
        this.foodRepository = ParameterChecks.requireNonNull(foodRepository, "repositorio de alimentos");
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.userConditionRepository = ParameterChecks.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.routineRepository = ParameterChecks.requireNonNull(routineRepository, "repositorio de rutinas");
    }

    /**
     * ¿Cuántos gramos de un alimento consumió un usuario en una fecha?
     * <p>
     * Carga los registros del usuario en ese día y suma los gramos de los que
     * corresponden al alimento indicado.
     *
     * @param userId identificador del usuario
     * @param foodId identificador del alimento
     * @param date   día a consultar
     * @return gramos totales; 0 si no consumió ese alimento ese día
     * @throws IllegalArgumentException si algún id es inválido o la fecha es nula
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public double gramsConsumed(int userId, int foodId, LocalDate date) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(foodId, "identificador del alimento");
        ParameterChecks.requireNonNull(date, "fecha");
        double total = 0;
        for (ConsumptionRecord record : recordRepository.findByUserIdAndDate(userId, date)) {
            if (record.getFoodId() == foodId) {
                total += record.getEstimatedGrams();
            }
        }
        return total;
    }

    /**
     * ¿Cuál es el promedio de consumo calórico diario de un usuario?
     * <p>
     * Pasos: (1) valida id y rango de fechas; (2) carga todos los registros del
     * usuario y descarta los que quedan fuera del rango (ambos extremos
     * incluidos); (3) suma las calorías de cada día; (4) promedia esos totales
     * diarios. Solo cuentan los días que tienen al menos un registro: un día sin
     * registros no baja el promedio.
     *
     * @param userId identificador del usuario
     * @param from   fecha inicial (incluida)
     * @param to     fecha final (incluida)
     * @return promedio de kcal por día con registros; 0 si no hay registros en el rango
     * @throws IllegalArgumentException si el id o el rango son inválidos
     * @throws IllegalStateException    si un registro usa un alimento que ya no existe en el catálogo
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public double averageDailyCalories(int userId, LocalDate from, LocalDate to) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireDateRange(from, to);

        Map<Integer, Food> foodCache = new HashMap<>();
        Map<LocalDate, Double> caloriesByDay = new TreeMap<>();
        for (ConsumptionRecord record : recordRepository.findByUserId(userId)) {
            LocalDate day = dayOf(record);
            if (day == null || day.isBefore(from) || day.isAfter(to)) {
                continue;
            }
            double calories = nutritionOf(record, foodCache).getCalories();
            caloriesByDay.merge(day, calories, Double::sum);
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

    /**
     * Lista los usuarios que tienen una condición médica asignada (por ejemplo,
     * "¿qué usuarios tienen diabetes?").
     * <p>
     * Recorre todos los usuarios y conserva los que tienen el par
     * usuario-condición.
     *
     * @param conditionId identificador de la condición médica
     * @return usuarios con esa condición; vacía si nadie la tiene
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<User> usersWithCondition(int conditionId) throws PersistenceExceptions {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        List<User> result = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (userConditionRepository.find(user.getIdUser(), conditionId).isPresent()) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * ¿Qué alimentos consumen con más frecuencia las personas con una condición?
     * <p>
     * Pasos: (1) obtiene los usuarios con la condición; (2) recorre todos sus
     * registros de consumo y cuenta, por alimento, cuántos registros hay y
     * cuántos gramos suman; (3) ordena de mayor a menor número de veces
     * (desempate: más gramos); (4) devuelve los primeros {@code limit}.
     *
     * @param conditionId identificador de la condición médica
     * @param limit       cantidad máxima de alimentos a devolver (mayor que 0)
     * @return alimentos más frecuentes; vacía si nadie con esa condición ha registrado consumos
     * @throws IllegalArgumentException si el id o el límite son inválidos
     * @throws IllegalStateException    si un registro usa un alimento que ya no existe en el catálogo
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<FoodFrequency> mostFrequentFoods(int conditionId, int limit) throws PersistenceExceptions {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        if (limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor que cero.");
        }
        Map<Integer, int[]> times = new LinkedHashMap<>();       // foodId -> [veces]
        Map<Integer, Double> grams = new HashMap<>();
        for (User user : usersWithCondition(conditionId)) {
            for (ConsumptionRecord record : recordRepository.findByUserId(user.getIdUser())) {
                times.computeIfAbsent(record.getFoodId(), k -> new int[1])[0]++;
                grams.merge(record.getFoodId(), record.getEstimatedGrams(), Double::sum);
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

    /**
     * ¿Cuál fue la tendencia de consumo de carbohidratos de las personas con una
     * condición en un rango de fechas?
     * <p>
     * Pasos: (1) valida id y rango; (2) obtiene los usuarios con la condición;
     * (3) crea una entrada en cero por cada día del rango; (4) suma, día por día,
     * los carbohidratos de todos los registros de esos usuarios; (5) divide cada
     * total entre el número de usuarios con la condición, de modo que cada valor
     * es el <b>promedio de gramos de carbohidratos por usuario</b> ese día.
     *
     * @param conditionId identificador de la condición médica
     * @param from        fecha inicial (incluida)
     * @param to          fecha final (incluida)
     * @return mapa día → gramos promedio de carbohidratos por usuario, ordenado por fecha; todos los días del rango
     *         aparecen (con 0 si no hubo registros); vacío si nadie tiene la condición
     * @throws IllegalArgumentException si el id o el rango son inválidos
     * @throws IllegalStateException    si un registro usa un alimento que ya no existe en el catálogo
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public Map<LocalDate, Double> carbsTrend(int conditionId, LocalDate from, LocalDate to)
            throws PersistenceExceptions {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
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
                if (day != null && trend.containsKey(day)) {
                    trend.merge(day, nutritionOf(record, foodCache).getCarbohydrates(), Double::sum);
                }
            }
        }
        for (Map.Entry<LocalDate, Double> entry : trend.entrySet()) {
            entry.setValue(entry.getValue() / users.size());
        }
        return trend;
    }

    /**
     * ¿Qué relación hay entre el IMC y las condiciones médicas?
     * <p>
     * Devuelve todas las filas del historial de IMC de los usuarios que tienen la
     * condición, ordenadas por fecha. Con ellas la interfaz puede calcular el IMC
     * de cada fila y graficar su evolución.
     *
     * @param conditionId identificador de la condición médica
     * @return filas del historial de esos usuarios, de la más antigua a la más reciente
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<BMIHistory> bmiByCondition(int conditionId) throws PersistenceExceptions {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        List<BMIHistory> rows = new ArrayList<>();
        for (User user : usersWithCondition(conditionId)) {
            rows.addAll(bmiHistoryRepository.findByUserId(user.getIdUser()));
        }
        rows.sort(Comparator.comparing(BMIHistory::getRegisterDate,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return rows;
    }

    /**
     * ¿Qué rutinas de ejercicio se han recomendado para personas con una
     * condición?
     * <p>
     * Recorre las rutinas de todos los usuarios y conserva las que registraron
     * esa condición entre las condiciones consideradas al generarse.
     *
     * @param conditionId identificador de la condición médica
     * @return rutinas generadas considerando esa condición; vacía si no hay ninguna
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<ExerciseRoutine> routinesForCondition(int conditionId) throws PersistenceExceptions {
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

    /**
     * Obtiene el día en que se hizo un registro de consumo.
     *
     * @param record registro de consumo
     * @return fecha del registro, o {@code null} si el registro no tiene fecha
     */
    private LocalDate dayOf(ConsumptionRecord record) {
        return record.getDateTime() == null ? null : record.getDateTime().toLocalDate();
    }

    /**
     * Calcula el aporte nutricional de un registro, consultando el alimento una
     * sola vez gracias a la caché recibida.
     *
     * @param record    registro de consumo
     * @param foodCache caché de alimentos ya consultados (se completa aquí)
     * @return aporte nutricional de los gramos del registro
     * @throws IllegalStateException si el alimento ya no existe en el catálogo
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private NutritionalSummary nutritionOf(ConsumptionRecord record, Map<Integer, Food> foodCache)
            throws PersistenceExceptions {
        Food food = foodCache.get(record.getFoodId());
        if (food == null) {
            food = requireFood(record.getFoodId());
            foodCache.put(record.getFoodId(), food);
        }
        return NutritionCalculator.calculate(food, record.getEstimatedGrams());
    }

    /**
     * Carga un alimento del catálogo, activo o no (el historial puede usar
     * alimentos ya desactivados).
     *
     * @param foodId identificador del alimento
     * @return el alimento
     * @throws IllegalStateException si el alimento no existe
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private Food requireFood(int foodId) throws PersistenceExceptions {
        return foodRepository.findById(foodId).orElseThrow(() -> new IllegalStateException(
                "Un registro de consumo usa el alimento " + foodId + ", que ya no existe en el catálogo."));
    }
}