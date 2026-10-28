package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.calculation.ConditionRule;
import com.nutritrackcore.services.calculation.ConditionRuleCatalog;
import com.nutritrackcore.services.dto.BmiCategory;
import com.nutritrackcore.services.dto.BmiResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Casos de uso de las rutinas de ejercicio: generar una nueva, consultar la
 * vigente y ver el historial.
 * <p>
 * Como pide el diagrama EER, cada rutina nace de varias fuentes: el último
 * registro del historial de IMC ({@code baseHistoryId}), las condiciones
 * médicas del usuario (una fila por condición considerada, con la restricción
 * aplicada) y, si existe, el plan de dieta activo ({@code dietPlanId}).
 * <p>
 * Un usuario puede tener varias rutinas a lo largo del tiempo, pero solo una
 * <b>activa</b>.
 */
public class ExerciseRoutineService {

    /** Número máximo de ejercicios que incluye una rutina. */
    private static final int MAX_EXERCISES = 5;

    private final ExerciseRoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final DietPlanRepository dietPlanRepository;
    private final UserRepository userRepository;
    private final MedicalConditionService medicalConditionService;
    private final ConditionRuleCatalog ruleCatalog;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param routineRepository       repositorio de rutinas
     * @param exerciseRepository      repositorio del catálogo de ejercicios
     * @param bmiHistoryRepository    repositorio del historial de IMC
     * @param dietPlanRepository      repositorio de planes de dieta
     * @param userRepository          repositorio de usuarios
     * @param medicalConditionService servicio que entrega las condiciones del usuario
     * @param ruleCatalog             catálogo de reglas por condición médica
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public ExerciseRoutineService(ExerciseRoutineRepository routineRepository, ExerciseRepository exerciseRepository,
                                  BMIHistoryRepository bmiHistoryRepository, DietPlanRepository dietPlanRepository,
                                  UserRepository userRepository, MedicalConditionService medicalConditionService,
                                  ConditionRuleCatalog ruleCatalog) {
        this.routineRepository = ParameterChecks.requireNonNull(routineRepository, "repositorio de rutinas");
        this.exerciseRepository = ParameterChecks.requireNonNull(exerciseRepository, "repositorio de ejercicios");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.dietPlanRepository = ParameterChecks.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.medicalConditionService = ParameterChecks.requireNonNull(medicalConditionService,
                "servicio de condiciones médicas");
        this.ruleCatalog = ParameterChecks.requireNonNull(ruleCatalog, "catálogo de reglas");
    }

    /**
     * Genera una rutina de ejercicio nueva para un usuario y la deja como la
     * rutina activa.
     * <p>
     * Pasos: (1) valida el id y comprueba que el usuario exista; (2) toma su
     * último registro del historial de IMC (si no tiene, lanza un error); (3)
     * calcula la intensidad base según la categoría del IMC (bajo peso y
     * obesidad: baja; peso normal y sobrepeso: moderada); (4) obtiene las reglas
     * de sus condiciones activas y reduce la intensidad hasta el máximo que
     * permita cada regla; (5) si alguna regla exige bajo impacto, usa solo
     * ejercicios {@code lowImpact}; (6) elige hasta 5 ejercicios activos
     * alternando grupos musculares; (7) asigna series y repeticiones según la
     * intensidad (baja 2×10, moderada 3×12, alta 4×15); (8) registra una fila de
     * condición considerada, con su restricción en español, por cada condición
     * activa; (9) enlaza el plan de dieta activo, si lo hay; (10) desactiva las
     * rutinas activas anteriores y guarda la nueva como activa.
     *
     * @param userId identificador del usuario
     * @return la rutina guardada, con su id, ejercicios y condiciones
     * @throws IllegalArgumentException si el id es inválido o el usuario no existe
     * @throws IllegalStateException    si el usuario no tiene historial de IMC o no hay ejercicios activos que cumplan las restricciones
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public ExerciseRoutine generateRoutine(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("No existe un usuario con el identificador " + userId + ".");
        }
        BMIHistory latest = bmiHistoryRepository.findLatestByUserId(userId).orElseThrow(() -> new IllegalStateException(
                "El usuario no tiene historial de IMC; registre su peso y altura antes de generar una rutina."));

        BmiCategory category = BmiResult.calculate(latest.getWeightKg(), latest.getHeightCm()).getCategory();
        IntensityLevel intensity = baseIntensity(category);

        List<MedicalCondition> conditions = new ArrayList<>();
        List<ConditionRule> rules = new ArrayList<>();
        for (MedicalCondition condition : medicalConditionService.listByUser(userId)) {
            if (condition.isActive()) {
                conditions.add(condition);
                rules.add(ruleCatalog.getFor(condition));
            }
        }

        boolean lowImpactOnly = false;
        for (ConditionRule rule : rules) {
            if (rank(rule.maxIntensity()) < rank(intensity)) {
                intensity = rule.maxIntensity();
            }
            lowImpactOnly = lowImpactOnly || rule.requiresLowImpact();
        }

        List<ExerciseRoutineExercise> lines = buildLines(selectExercises(lowImpactOnly), intensity);

        List<ExerciseRoutineCondition> considered = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            ExerciseRoutineCondition item = new ExerciseRoutineCondition();
            item.setConditionId(conditions.get(i).getConditionId());
            item.setAppliedRestriction(rules.get(i).describeRestriction());
            considered.add(item);
        }

        Optional<DietPlan> activePlan = dietPlanRepository.findActiveByUserId(userId);
        deactivateActiveRoutines(userId);

        ExerciseRoutine routine = new ExerciseRoutine();
        routine.setUserId(userId);
        routine.setGenerationDate(LocalDate.now());
        routine.setIntensityLevel(intensity);
        routine.setActive(true);
        routine.setBaseHistoryId(latest.getIdHistory());
        routine.setDietPlanId(activePlan.map(DietPlan::getPlanId).orElse(null));
        routine.setLines(lines);
        routine.setConsideredConditions(considered);
        return routineRepository.save(routine);
    }

    /**
     * Obtiene la rutina activa de un usuario.
     *
     * @param userId identificador del usuario
     * @return la rutina activa más reciente, o vacío si no tiene ninguna activa
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public Optional<ExerciseRoutine> getActiveRoutine(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return routineRepository.findActiveByUserId(userId);
    }

    /**
     * Lista todas las rutinas de un usuario (activas e inactivas), de la más
     * antigua a la más reciente.
     *
     * @param userId identificador del usuario
     * @return historial de rutinas; vacío si no tiene ninguna
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<ExerciseRoutine> listHistory(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return routineRepository.findByUserId(userId);
    }

    /**
     * Determina la intensidad inicial a partir de la categoría del IMC.
     *
     * @param category categoría del IMC más reciente
     * @return baja para bajo peso y obesidad; moderada para peso normal y sobrepeso
     */
    private IntensityLevel baseIntensity(BmiCategory category) {
        switch (category) {
            case UNDERWEIGHT:
            case OBESITY:
                return IntensityLevel.LOW;
            case NORMAL:
            case OVERWEIGHT:
            default:
                return IntensityLevel.MODERATE;
        }
    }

    /**
     * Convierte un nivel de intensidad en un número para poder compararlos
     * (menor número = menos intensidad).
     *
     * @param level nivel de intensidad
     * @return 0 para baja, 1 para moderada, 2 para alta
     */
    private int rank(IntensityLevel level) {
        switch (level) {
            case LOW:
                return 0;
            case MODERATE:
                return 1;
            case HIGH:
            default:
                return 2;
        }
    }

    /**
     * Elige los ejercicios de la rutina.
     * <p>
     * Toma los ejercicios activos (solo los de bajo impacto si se exige),
     * los agrupa por grupo muscular (sin grupo = "General"), ordena los grupos y
     * los ejercicios por nombre y los va tomando de uno en uno alternando entre
     * grupos hasta llegar a 5, de modo que la rutina trabaje distintas zonas.
     *
     * @param lowImpactOnly {@code true} si solo se permiten ejercicios de bajo impacto
     * @return ejercicios elegidos
     * @throws IllegalStateException si no hay ejercicios que cumplan
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private List<Exercise> selectExercises(boolean lowImpactOnly) throws PersistenceExceptions {
        Map<String, List<Exercise>> byGroup = new LinkedHashMap<>();
        List<Exercise> active = new ArrayList<>(exerciseRepository.findAllActive());
        active.sort(Comparator.comparing((Exercise e) -> groupOf(e).toLowerCase())
                .thenComparing(e -> e.getName() == null ? "" : e.getName().toLowerCase()));
        for (Exercise exercise : active) {
            if (lowImpactOnly && !exercise.isLowImpact()) {
                continue;
            }
            byGroup.computeIfAbsent(groupOf(exercise), g -> new ArrayList<>()).add(exercise);
        }
        if (byGroup.isEmpty()) {
            throw new IllegalStateException(
                    "No hay ejercicios activos que cumplan las restricciones de las condiciones del usuario.");
        }

        List<Exercise> chosen = new ArrayList<>();
        int round = 0;
        while (chosen.size() < MAX_EXERCISES) {
            boolean addedInRound = false;
            for (List<Exercise> group : byGroup.values()) {
                if (round < group.size() && chosen.size() < MAX_EXERCISES) {
                    chosen.add(group.get(round));
                    addedInRound = true;
                }
            }
            if (!addedInRound) {
                break;
            }
            round++;
        }
        return chosen;
    }

    /**
     * Devuelve el grupo muscular de un ejercicio, o "General" si no tiene.
     *
     * @param exercise ejercicio
     * @return grupo muscular legible
     */
    private String groupOf(Exercise exercise) {
        String group = exercise.getMuscleGroup();
        return group == null || group.trim().isEmpty() ? "General" : group.trim();
    }

    /**
     * Asigna series y repeticiones a cada ejercicio elegido según la
     * intensidad: baja 2×10, moderada 3×12, alta 4×15.
     *
     * @param exercises ejercicios elegidos
     * @param intensity intensidad de la rutina
     * @return líneas de la rutina (el id de la rutina lo completa el repositorio al guardar)
     */
    private List<ExerciseRoutineExercise> buildLines(List<Exercise> exercises, IntensityLevel intensity) {
        int sets;
        int repetitions;
        switch (intensity) {
            case LOW:
                sets = 2;
                repetitions = 10;
                break;
            case MODERATE:
                sets = 3;
                repetitions = 12;
                break;
            case HIGH:
            default:
                sets = 4;
                repetitions = 15;
                break;
        }
        List<ExerciseRoutineExercise> lines = new ArrayList<>();
        for (Exercise exercise : exercises) {
            ExerciseRoutineExercise line = new ExerciseRoutineExercise();
            line.setExerciseId(exercise.getExerciseId());
            line.setSets(sets);
            line.setRepetitions(repetitions);
            lines.add(line);
        }
        return lines;
    }

    /**
     * Marca como inactivas todas las rutinas activas del usuario, conservándolas
     * como historial.
     *
     * @param userId identificador del usuario
     * @throws PersistenceExceptions si falla la lectura o escritura del almacenamiento
     */
    private void deactivateActiveRoutines(int userId) throws PersistenceExceptions {
        for (ExerciseRoutine previous : routineRepository.findByUserId(userId)) {
            if (previous.isActive()) {
                previous.setActive(false);
                routineRepository.update(previous);
            }
        }
    }
}