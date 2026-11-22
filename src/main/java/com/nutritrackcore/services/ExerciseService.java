package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.repository.ExerciseRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Catálogo de ejercicios (con borrado lógico).
 */
public class ExerciseService {
    private static final int MAX_NAME_LENGTH = 100;

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = Objects.requireNonNull(exerciseRepository, "repositorio de ejercicios");
    }

    public Exercise create(Exercise exercise) throws NutriTrackException {
        ParameterChecks.requireNonNull(exercise, "ejercicio");
        validateExercise(exercise);
        ensureNameIsFree(exercise.getName(), 0);
        exercise.setActive(true);
        return exerciseRepository.save(exercise);
    }

    public void update(Exercise exercise) throws NutriTrackException {
        ParameterChecks.requireNonNull(exercise, "ejercicio");
        ParameterChecks.requireId(exercise.getExerciseId(), "identificador del ejercicio");
        validateExercise(exercise);
        Exercise stored = getById(exercise.getExerciseId());
        ensureNameIsFree(exercise.getName(), exercise.getExerciseId());
        exercise.setActive(stored.isActive());
        exerciseRepository.update(exercise);
    }

    public void deactivate(int exerciseId) throws NutriTrackException {
        Exercise exercise = getById(exerciseId);
        if (exercise.isActive()) {
            exercise.setActive(false);
            exerciseRepository.update(exercise);
        }
    }

    public Exercise getById(int exerciseId) throws NutriTrackException {
        ParameterChecks.requireId(exerciseId, "identificador del ejercicio");
        return exerciseRepository.findById(exerciseId).orElseThrow(
                () -> new NotFoundEntityException("No existe un ejercicio con el identificador " + exerciseId + "."));
    }

    public List<Exercise> listActive() throws NutriTrackException {
        return exerciseRepository.findAllActive();
    }

    public List<Exercise> listLowImpact() throws NutriTrackException {
        List<Exercise> result = new ArrayList<>();
        for (Exercise exercise : exerciseRepository.findAllActive()) {
            if (exercise.isLowImpact()) {
                result.add(exercise);
            }
        }
        return result;
    }

    /** El grupo muscular es obligatorio y va en mayúsculas, como lo pide la base de datos. */
    private void validateExercise(Exercise exercise) throws ValidationException {
        exercise.setName(ParameterChecks.requireTextMax(exercise.getName(), MAX_NAME_LENGTH, "nombre del ejercicio"));
        exercise.setMuscleGroup(ParameterChecks.requireMuscleGroup(exercise.getMuscleGroup()));
    }

    private void ensureNameIsFree(String name, int ignoredExerciseId) throws NutriTrackException {
        for (Exercise existing : exerciseRepository.findAll()) {
            if (existing.getExerciseId() != ignoredExerciseId && existing.getName() != null
                    && existing.getName().trim().equalsIgnoreCase(name)) {
                throw new BussinesRuleException("Ya existe un ejercicio llamado '" + name + "'.");
            }
        }
    }
}