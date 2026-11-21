package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.calculation.NutritionCalculator;
import com.nutritrackcore.services.dto.NutritionalSummary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso del registro de consumo de alimentos.
 * <p>
 * Existen dos caminos para registrar:
 * <ul>
 *   <li>{@link #registerManual}: la persona escribe los gramos a mano (único
 *       camino disponible en la primera entrega). {@code imageRef} y
 *       {@code estimationLevel} quedan vacíos.</li>
 *   <li>{@link #register}: camino que usará la cámara; exige la referencia de
 *       la imagen y el nivel de confianza de la estimación.</li>
 * </ul>
 * Los gramos guardados son una "foto fija" (snapshot): no se recalculan luego.
 */
public class ConsumptionService {

    /** Gramos máximos aceptados en un solo registro. */
    private static final double MAX_GRAMS = 5000;

    /** Longitud máxima de la referencia a la imagen. */
    private static final int MAX_IMAGE_REF_LENGTH = 255;

    private final ConsumptionRecordRepository recordRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param recordRepository repositorio de registros de consumo
     * @param foodRepository   repositorio del catálogo de alimentos
     * @param userRepository   repositorio de usuarios
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public ConsumptionService(ConsumptionRecordRepository recordRepository, FoodRepository foodRepository,
                              UserRepository userRepository) {
        this.recordRepository = ParameterChecks.requireNonNull(recordRepository, "repositorio de consumos");
        this.foodRepository = ParameterChecks.requireNonNull(foodRepository, "repositorio de alimentos");
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
    }

    /**
     * Registra un consumo cuyos gramos fueron estimados a partir de una imagen.
     * <p>
     * Es el punto de entrada de la futura cámara: además de los datos básicos
     * exige una referencia de imagen (no vacía, máximo 255 caracteres) y un
     * nivel de confianza entre 0 y 1. Después delega en la lógica común de
     * guardado.
     *
     * @param userId          identificador del usuario
     * @param foodId          identificador del alimento
     * @param grams           gramos estimados (mayores que 0 y hasta 5000)
     * @param consumedAt      fecha y hora del consumo (no futura)
     * @param imageRef        ruta o referencia de la imagen analizada
     * @param estimationLevel nivel de confianza del estimador, entre 0 y 1
     * @return el registro guardado, con su id
     * @throws IllegalArgumentException si algún dato es nulo o inválido, o el usuario/alimento no existen o el alimento está desactivado
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public ConsumptionRecord register(int userId, int foodId, double grams, LocalDateTime consumedAt,
                                      String imageRef, Double estimationLevel) throws PersistenceExceptions {
        String cleanRef = ParameterChecks.requireTextMax(imageRef, MAX_IMAGE_REF_LENGTH, "referencia de la imagen");
        ParameterChecks.requireNonNull(estimationLevel, "nivel de estimación");
        ParameterChecks.requireRange(estimationLevel, 0.0, 1.0, "nivel de estimación");
        return saveRecord(userId, foodId, grams, consumedAt, cleanRef, estimationLevel);
    }

    /**
     * Registra un consumo cuyos gramos escribió la persona manualmente.
     * <p>
     * Guarda el registro sin imagen ({@code imageRef = null}) y sin nivel de
     * confianza ({@code estimationLevel = null}).
     *
     * @param userId     identificador del usuario
     * @param foodId     identificador del alimento
     * @param grams      gramos consumidos (mayores que 0 y hasta 5000)
     * @param consumedAt fecha y hora del consumo (no futura)
     * @return el registro guardado, con su id
     * @throws IllegalArgumentException si algún dato es nulo o inválido, o el usuario/alimento no existen o el alimento está desactivado
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public ConsumptionRecord registerManual(int userId, int foodId, double grams, LocalDateTime consumedAt)
            throws PersistenceExceptions {
        return saveRecord(userId, foodId, grams, consumedAt, null, null);
    }

    /**
     * Lógica común de {@link #register} y {@link #registerManual}.
     * <p>
     * Pasos: (1) valida ids, gramos (finitos, mayores que 0 y no más de 5000) y
     * fecha (no nula ni futura); (2) comprueba que el usuario exista; (3)
     * comprueba que el alimento exista y esté activo; (4) arma el registro y lo
     * guarda (el repositorio le asigna el id).
     *
     * @param userId          identificador del usuario
     * @param foodId          identificador del alimento
     * @param grams           gramos consumidos
     * @param consumedAt      fecha y hora del consumo
     * @param imageRef        referencia de imagen, o {@code null} en el registro manual
     * @param estimationLevel nivel de confianza, o {@code null} en el registro manual
     * @return el registro guardado
     * @throws PersistenceExceptions si falla la lectura o escritura del almacenamiento
     */
    private ConsumptionRecord saveRecord(int userId, int foodId, double grams, LocalDateTime consumedAt,
                                         String imageRef, Double estimationLevel) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(foodId, "identificador del alimento");
        ParameterChecks.requirePositive(grams, "gramos");
        if (grams > MAX_GRAMS) {
            throw new IllegalArgumentException("Los gramos no pueden superar " + (int) MAX_GRAMS + " en un solo registro.");
        }
        ParameterChecks.requireNotFuture(consumedAt, "fecha y hora del consumo");

        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("No existe un usuario con el identificador " + userId + ".");
        }
        Food food = foodRepository.findById(foodId).orElseThrow(
                () -> new IllegalArgumentException("No existe un alimento con el identificador " + foodId + "."));
        if (!food.isActive()) {
            throw new IllegalArgumentException("El alimento '" + food.getName() + "' está desactivado y no se puede registrar.");
        }

        ConsumptionRecord record = new ConsumptionRecord();
        record.setUserId(userId);
        record.setFoodId(foodId);
        record.setEstimatedGrams(grams);
        record.setDateTime(consumedAt);
        record.setImageRef(imageRef);
        record.setEstimationLevel(estimationLevel);
        return recordRepository.save(record);
    }

    /**
     * Lista los consumos de un usuario en un día, ordenados por hora.
     *
     * @param userId identificador del usuario
     * @param date   día a consultar
     * @return registros del día; vacía si no hay ninguno
     * @throws IllegalArgumentException si el id es inválido o la fecha es nula
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<ConsumptionRecord> listByDay(int userId, LocalDate date) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNonNull(date, "fecha");
        return recordRepository.findByUserIdAndDate(userId, date);
    }

    /**
     * Calcula cuántas calorías y macronutrientes consumió un usuario en un día.
     * <p>
     * Pasos: (1) valida id y fecha; (2) carga los registros del día; (3) por
     * cada registro obtiene el alimento (una sola consulta por alimento, con una
     * caché local) y calcula el aporte de los gramos consumidos con
     * {@link NutritionCalculator}; (4) suma todos los aportes. Si no hay
     * registros, el resultado es un resumen en cero.
     *
     * @param userId identificador del usuario
     * @param date   día a resumir
     * @return resumen nutricional del día
     * @throws IllegalArgumentException si el id es inválido o la fecha es nula
     * @throws IllegalStateException    si un registro apunta a un alimento que ya no existe en el catálogo
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public NutritionalSummary getDailySummary(int userId, LocalDate date) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNonNull(date, "fecha");

        Map<Integer, Food> foodCache = new HashMap<>();
        NutritionalSummary total = NutritionalSummary.empty();
        for (ConsumptionRecord record : recordRepository.findByUserIdAndDate(userId, date)) {
            Food food = foodCache.get(record.getFoodId());
            if (food == null) {
                food = foodRepository.findById(record.getFoodId()).orElseThrow(() -> new IllegalStateException(
                        "El registro " + record.getRecordId() + " usa un alimento que ya no existe en el catálogo."));
                foodCache.put(record.getFoodId(), food);
            }
            total = total.add(NutritionCalculator.calculate(food, record.getEstimatedGrams()));
        }
        return total;
    }

    /**
     * Elimina un registro de consumo.
     * <p>
     * Comprueba primero que el registro exista para informar con un mensaje claro
     * si el id es incorrecto (el repositorio, por su parte, no falla al borrar un
     * id inexistente).
     *
     * @param recordId identificador del registro
     * @throws IllegalArgumentException si el id es inválido o el registro no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void delete(int recordId) throws PersistenceExceptions {
        ParameterChecks.requireId(recordId, "identificador del registro");
        if (recordRepository.findById(recordId).isEmpty()) {
            throw new IllegalArgumentException("No existe un registro de consumo con el identificador " + recordId + ".");
        }
        recordRepository.deleteById(recordId);
    }
}