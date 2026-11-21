package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.repository.ExerciseRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso del catálogo de ejercicios.
 * <p>
 * Igual que los alimentos y las condiciones, un ejercicio <b>nunca se
 * borra</b>: se desactiva para que las rutinas antiguas puedan seguir
 * mostrándolo.
 */
public class ExerciseService {

    /** Longitud máxima del nombre de un ejercicio. */
    private static final int MAX_NAME_LENGTH = 100;

    /** Longitud máxima del grupo muscular. */
    private static final int MAX_MUSCLE_GROUP_LENGTH = 50;

    private final ExerciseRepository exerciseRepository;

    /**
     * Crea el servicio con su dependencia.
     *
     * @param exerciseRepository repositorio del catálogo de ejercicios
     * @throws IllegalArgumentException si el repositorio es nulo
     */
    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = ParameterChecks.requireNonNull(exerciseRepository, "repositorio de ejercicios");
    }

    /**
     * Crea un ejercicio nuevo.
     * <p>
     * Pasos: (1) valida que no sea nulo, que el nombre sea obligatorio (máximo
     * 100 caracteres) y que el grupo muscular, si viene, no pase de 50; (2)
     * rechaza el nombre si ya lo usa otro ejercicio; (3) lo marca como activo y
     * lo guarda.
     *
     * @param exercise ejercicio a crear (su id se ignora; lo asigna el repositorio)
     * @return el ejercicio guardado, con su id
     * @throws IllegalArgumentException si el ejercicio es nulo o algún dato es inválido
     * @throws IllegalStateException    si ya existe un ejercicio con ese nombre
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public Exercise create(Exercise exercise) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(exercise, "ejercicio");
        validateExercise(exercise);
        ensureNameIsFree(exercise.getName(), 0);
        exercise.setActive(true);
        return exerciseRepository.save(exercise);
    }

    /**
     * Modifica los datos de un ejercicio existente.
     * <p>
     * Valida id y datos, comprueba que exista, rechaza el nombre si lo usa
     * <b>otro</b> ejercicio y guarda. Conserva el estado activo/inactivo que ya
     * tenía; para cambiarlo existe {@link #deactivate(int)}.
     *
     * @param exercise ejercicio con los datos nuevos (se identifica por su id)
     * @throws IllegalArgumentException si el ejercicio es nulo, algún dato es inválido o no existe
     * @throws IllegalStateException    si otro ejercicio ya usa ese nombre
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void update(Exercise exercise) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(exercise, "ejercicio");
        ParameterChecks.requireId(exercise.getExerciseId(), "identificador del ejercicio");
        validateExercise(exercise);
        Exercise stored = getById(exercise.getExerciseId());
        ensureNameIsFree(exercise.getName(), exercise.getExerciseId());
        exercise.setActive(stored.isActive());
        exerciseRepository.update(exercise);
    }

    /**
     * Desactiva un ejercicio (nunca lo borra). Si ya estaba desactivado, no
     * hace nada.
     *
     * @param exerciseId identificador del ejercicio
     * @throws IllegalArgumentException si el id es inválido o el ejercicio no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void deactivate(int exerciseId) throws PersistenceExceptions {
        Exercise exercise = getById(exerciseId);
        if (exercise.isActive()) {
            exercise.setActive(false);
            exerciseRepository.update(exercise);
        }
    }

    /**
     * Obtiene un ejercicio por su id, esté activo o no.
     *
     * @param exerciseId identificador del ejercicio
     * @return el ejercicio encontrado
     * @throws IllegalArgumentException si el id es inválido o el ejercicio no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public Exercise getById(int exerciseId) throws PersistenceExceptions {
        ParameterChecks.requireId(exerciseId, "identificador del ejercicio");
        return exerciseRepository.findById(exerciseId).orElseThrow(
                () -> new IllegalArgumentException("No existe un ejercicio con el identificador " + exerciseId + "."));
    }

    /**
     * Lista los ejercicios activos.
     *
     * @return ejercicios activos; vacía si no hay ninguno
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    public List<Exercise> listActive() throws PersistenceExceptions {
        return exerciseRepository.findAllActive();
    }

    /**
     * Lista los ejercicios activos aptos para personas con sobrepeso o
     * problemas articulares ({@code lowImpact = true}). El repositorio no tiene
     * una consulta específica, así que este método filtra la lista de activos.
     *
     * @return ejercicios activos de bajo impacto; vacía si no hay ninguno
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    public List<Exercise> listLowImpact() throws PersistenceExceptions {
        List<Exercise> result = new ArrayList<>();
        for (Exercise exercise : exerciseRepository.findAllActive()) {
            if (exercise.isLowImpact()) {
                result.add(exercise);
            }
        }
        return result;
    }

    /**
     * Valida los datos de un ejercicio y limpia sus textos.
     * <p>
     * El nombre es obligatorio (máximo 100 caracteres, sin {@code |}). El grupo
     * muscular es opcional: si viene, debe tener máximo 50 caracteres; si viene
     * vacío se guarda como {@code null}.
     *
     * @param exercise ejercicio a validar (se le reemplazan los textos por la versión recortada)
     * @throws IllegalArgumentException si alguna regla no se cumple
     */
    private void validateExercise(Exercise exercise) {
        exercise.setName(ParameterChecks.requireTextMax(exercise.getName(), MAX_NAME_LENGTH, "nombre del ejercicio"));
        if (exercise.getMuscleGroup() != null) {
            String group = ParameterChecks.requireOptionalText(
                    exercise.getMuscleGroup(), MAX_MUSCLE_GROUP_LENGTH, "grupo muscular");
            exercise.setMuscleGroup(group.isEmpty() ? null : group);
        }
    }

    /**
     * Comprueba que ningún otro ejercicio use un nombre.
     *
     * @param name              nombre ya recortado
     * @param ignoredExerciseId id del ejercicio que se está editando, o 0 si es uno nuevo
     * @throws IllegalStateException si otro ejercicio tiene el mismo nombre
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private void ensureNameIsFree(String name, int ignoredExerciseId) throws PersistenceExceptions {
        for (Exercise existing : exerciseRepository.findAll()) {
            if (existing.getExerciseId() != ignoredExerciseId && existing.getName() != null
                    && existing.getName().trim().equalsIgnoreCase(name)) {
                throw new IllegalStateException("Ya existe un ejercicio llamado '" + name + "'.");
            }
        }
    }
}