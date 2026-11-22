package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.model.ExerciseByReps;
import com.nutritrackcore.model.ExerciseByTime;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.UserProfileRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/**
 * Genera la rutina de ejercicio y la mantiene como la única activa del usuario.
 * <p>
 * Reglas: intensidad base por IMC; condiciones médicas (máximo de intensidad, bajo impacto,
 * sobrepeso 3 series de máximo 10 repeticiones); nivel de actividad bajo = solo bajo impacto
 * y sin alta exigencia; entrenamiento en casa = prioriza ejercicios sin equipo y avisa si faltan.
 * Los ejercicios de CARDIO y CORE se hacen por tiempo; el resto, por repeticiones.
 */
public class ExerciseRoutineService {
    private static final int MAX_EXERCISES = 5;
    private static final Set<String> TIME_GROUPS = Set.of("CARDIO", "CORE");

    private final ExerciseRoutineRepository routineRepository;
    private final ExerciseRepository exerciseRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final DietPlanRepository dietPlanRepository;
    private final UserRepository userRepository;
    private final MedicalConditionService medicalConditionService;
    private final ConditionRuleCatalog ruleCatalog;
    private final UserProfileRepository profileRepository;

    public ExerciseRoutineService(ExerciseRoutineRepository routineRepository, ExerciseRepository exerciseRepository,
                                  BMIHistoryRepository bmiHistoryRepository, DietPlanRepository dietPlanRepository,
                                  UserRepository userRepository, MedicalConditionService medicalConditionService,
                                  ConditionRuleCatalog ruleCatalog, UserProfileRepository profileRepository) {
        this.routineRepository = Objects.requireNonNull(routineRepository, "repositorio de rutinas");
        this.exerciseRepository = Objects.requireNonNull(exerciseRepository, "repositorio de ejercicios");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.dietPlanRepository = Objects.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.medicalConditionService = Objects.requireNonNull(medicalConditionService, "servicio de condiciones");
        this.ruleCatalog = Objects.requireNonNull(ruleCatalog, "catálogo de reglas");
        this.profileRepository = Objects.requireNonNull(profileRepository, "repositorio de perfiles");
    }

    /** Genera una rutina nueva, desactiva la anterior y devuelve los avisos para el usuario. */
    public RoutineResult generateRoutine(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }
        BMIHistory latest = bmiHistoryRepository.findLatestByUserId(userId).orElseThrow(() -> new BussinesRuleException(
                "El usuario no tiene historial de IMC; registre su peso y altura antes de generar una rutina."));
        BmiCategory category = BmiResult.calculate(latest.getWeightKg(), latest.getHeightCm()).getCategory();

        List<MedicalCondition> conditions = medicalConditionService.listActiveByUser(userId);
        List<ConditionRule> rules = new ArrayList<>();
        for (MedicalCondition condition : conditions) {
            rules.add(ruleCatalog.getFor(condition));
        }
        if (category == BmiCategory.OVERWEIGHT) {
            ruleCatalog.findByName(OverweightRule.NAME).ifPresent(rule -> {
                if (!rules.contains(rule)) {
                    rules.add(rule);
                }
            });
        }
        Optional<UserProfile> profile = conditions.isEmpty() ? profileRepository.findByUserId(userId) : Optional.empty();

        IntensityLevel intensity = baseIntensity(category, profile.orElse(null));
        boolean lowImpactOnly = false;
        for (ConditionRule rule : rules) {
            intensity = min(intensity, rule.maxIntensity());
            lowImpactOnly = lowImpactOnly || rule.requiresLowImpact();
        }
        if (UserProfileService.isLowActivity(profile.orElse(null))) {
            lowImpactOnly = true;                                  // actividad baja: sin ejercicios de alta exigencia
            intensity = min(intensity, IntensityLevel.MODERATE);
        }
        boolean atHome = profile.isPresent() && profile.get().getTrainingLocation() != null
                && ParameterChecks.HOME_LOCATION.equalsIgnoreCase(profile.get().getTrainingLocation());

        List<String> warnings = new ArrayList<>();
        List<Exercise> exercises = selectExercises(lowImpactOnly, atHome, warnings);
        List<ExerciseRoutineExercise> lines = buildLines(exercises, intensity, rules);

        List<ExerciseRoutineCondition> considered = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++) {
            considered.add(new ExerciseRoutineCondition(0, conditions.get(i).getConditionId(),
                    rules.get(i).describeRestriction()));
        }

        Optional<DietPlan> activePlan = dietPlanRepository.findActiveByUserId(userId);
        deactivateActiveRoutines(userId);

        ExerciseRoutine routine = new ExerciseRoutine(userId, LocalDate.now(), intensity, latest.getIdHistory(),
                activePlan.map(DietPlan::getPlanId).orElse(null));
        routine.setActive(true);
        routine.setLines(lines);
        routine.setConsideredConditions(considered);
        return new RoutineResult(routineRepository.save(routine), warnings);
    }

    public Optional<ExerciseRoutine> getActiveRoutine(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return routineRepository.findActiveByUserId(userId);
    }

    public List<ExerciseRoutine> listHistory(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return routineRepository.findByUserId(userId);
    }

    /** Máximo de repeticiones permitido al usuario (10 con sobrepeso; sin límite si devuelve null). */
    Integer maxRepetitionsFor(int userId) throws NutriTrackException {
        Integer max = null;
        for (MedicalCondition condition : medicalConditionService.listActiveByUser(userId)) {
            max = minNullable(max, ruleCatalog.getFor(condition).maxRepetitions());
        }
        Optional<BMIHistory> latest = bmiHistoryRepository.findLatestByUserId(userId);
        if (latest.isPresent() && BmiResult.calculate(latest.get().getWeightKg(), latest.get().getHeightCm())
                .getCategory() == BmiCategory.OVERWEIGHT) {
            Optional<ConditionRule> overweight = ruleCatalog.findByName(OverweightRule.NAME);
            if (overweight.isPresent()) {
                max = minNullable(max, overweight.get().maxRepetitions());
            }
        }
        return max;
    }

    /** Bajo peso y obesidad: baja; normal avanzado: alta; el resto: moderada. */
    private IntensityLevel baseIntensity(BmiCategory category, UserProfile profile) {
        switch (category) {
            case LOW_WEIGHT:
            case OBESITY:
                return IntensityLevel.LOW;
            case NORMAL:
                return profile != null && "avanzado".equalsIgnoreCase(profile.getExerciseExperience())
                        ? IntensityLevel.HIGH : IntensityLevel.MODERATE;
            case OVERWEIGHT:
            default:
                return IntensityLevel.MODERATE;
        }
    }

    private static IntensityLevel min(IntensityLevel a, IntensityLevel b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }

    private static Integer minNullable(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }

    /**
     * Elige hasta 5 ejercicios alternando grupos musculares. En casa toma primero los que no
     * necesitan equipo y completa con los que sí, avisando al usuario.
     */
    private List<Exercise> selectExercises(boolean lowImpactOnly, boolean atHome, List<String> warnings)
            throws NutriTrackException {
        List<Exercise> withoutEquipment = new ArrayList<>();
        List<Exercise> withEquipment = new ArrayList<>();
        List<Exercise> active = new ArrayList<>(exerciseRepository.findAllActive());
        active.sort(Comparator.comparing((Exercise e) -> groupOf(e))
                .thenComparing(e -> e.getName() == null ? "" : e.getName().toLowerCase()));
        for (Exercise exercise : active) {
            if (lowImpactOnly && !exercise.isLowImpact()) {
                continue;
            }
            if (atHome && exercise.getNeed_Equipment()) {
                withEquipment.add(exercise);
            } else {
                withoutEquipment.add(exercise);
            }
        }
        List<Exercise> chosen = pickAlternating(withoutEquipment, MAX_EXERCISES);
        if (atHome && chosen.size() < MAX_EXERCISES) {
            List<Exercise> extra = pickAlternating(withEquipment, MAX_EXERCISES - chosen.size());
            if (!extra.isEmpty()) {
                warnings.add("No hay suficientes ejercicios sin equipo para entrenar en casa: se agregaron "
                        + extra.size() + " que necesitan equipo.");
            } else {
                warnings.add("No hay suficientes ejercicios sin equipo para entrenar en casa; la rutina quedó con "
                        + chosen.size() + " ejercicios.");
            }
            chosen.addAll(extra);
        }
        if (chosen.isEmpty()) {
            throw new BussinesRuleException("No hay ejercicios activos que cumplan las restricciones del usuario.");
        }
        return chosen;
    }

    private List<Exercise> pickAlternating(List<Exercise> exercises, int max) {
        Map<String, List<Exercise>> byGroup = new LinkedHashMap<>();
        for (Exercise exercise : exercises) {
            byGroup.computeIfAbsent(groupOf(exercise), g -> new ArrayList<>()).add(exercise);
        }
        List<Exercise> chosen = new ArrayList<>();
        for (int round = 0; chosen.size() < max; round++) {
            boolean added = false;
            for (List<Exercise> group : byGroup.values()) {
                if (round < group.size() && chosen.size() < max) {
                    chosen.add(group.get(round));
                    added = true;
                }
            }
            if (!added) {
                break;
            }
        }
        return chosen;
    }

    private static String groupOf(Exercise exercise) {
        String group = exercise.getMuscleGroup();
        return group == null ? "CUERPO_COMPLETO" : group.trim().toUpperCase();
    }

    /**
     * Series y repeticiones por intensidad (baja 2×10, moderada 3×12, alta 4×15) o duración
     * para CARDIO/CORE (30, 45 o 60 s). Las condiciones pueden fijar series y limitar repeticiones.
     */
    private List<ExerciseRoutineExercise> buildLines(List<Exercise> exercises, IntensityLevel intensity,
                                                     List<ConditionRule> rules) {
        int sets;
        int repetitions;
        int seconds;
        switch (intensity) {
            case LOW:
                sets = 2; repetitions = 10; seconds = 30;
                break;
            case MODERATE:
                sets = 3; repetitions = 12; seconds = 45;
                break;
            case HIGH:
            default:
                sets = 4; repetitions = 15; seconds = 60;
                break;
        }
        for (ConditionRule rule : rules) {
            if (rule.fixedSets() != null) {
                sets = rule.fixedSets();
            }
            if (rule.maxRepetitions() != null) {
                repetitions = Math.min(repetitions, rule.maxRepetitions());
            }
        }
        List<ExerciseRoutineExercise> lines = new ArrayList<>();
        for (Exercise exercise : exercises) {
            if (TIME_GROUPS.contains(groupOf(exercise))) {
                lines.add(new ExerciseByTime(0, exercise.getExerciseId(), sets, Duration.ofSeconds(seconds)));
            } else {
                lines.add(new ExerciseByReps(0, exercise.getExerciseId(), sets, repetitions));
            }
        }
        return lines;
    }

    private void deactivateActiveRoutines(int userId) throws NutriTrackException {
        for (ExerciseRoutine previous : routineRepository.findByUserId(userId)) {
            if (previous.isActive()) {
                previous.setActive(false);
                routineRepository.update(previous);
            }
        }
    }
}