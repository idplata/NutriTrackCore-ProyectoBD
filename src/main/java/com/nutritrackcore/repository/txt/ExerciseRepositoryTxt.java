package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.repository.ExerciseRepository;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ExerciseRepository} sobre {@code ejercicios.txt}.
 * <p>
 * Cada línea del archivo representa un ejercicio con el formato
 * {@code id_ejercicio|nombre|grupo_muscular|bajo_impacto|activo}. Cada
 * operación lee el archivo completo y, si modifica datos, lo reescribe completo.
 */
public class ExerciseRepositoryTxt implements ExerciseRepository {

    /** Cabecera del archivo {@code ejercicios.txt}. */
    public static final String HEADER = "#id_ejercicio|nombre|grupo_muscular|bajo_impacto|activo";

    /** Acceso al archivo .txt de ejercicios. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code ejercicios.txt}
     */
    public ExerciseRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public Exercise save(Exercise exercise) throws PersistenceExceptions {
        List<Exercise> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (Exercise e : all) {
            ids.add(e.getExerciseId());
        }
        exercise.setExerciseId(IdGenerator.next(ids));
        all.add(exercise);
        write(all);
        return exercise;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe un ejercicio con ese id
     */
    @Override
    public void update(Exercise exercise) throws PersistenceExceptions {
        List<Exercise> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getExerciseId() == exercise.getExerciseId()) {
                all.set(i, exercise);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el ejercicio con id " + exercise.getExerciseId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Exercise> findById(int exerciseId) throws PersistenceExceptions {
        for (Exercise e : findAll()) {
            if (e.getExerciseId() == exerciseId) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public List<Exercise> findAllActive() throws PersistenceExceptions {
        List<Exercise> result = new ArrayList<>();
        for (Exercise e : findAll()) {
            if (e.isActive()) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si alguna línea del archivo es inválida o está corrupta
     */
    @Override
    public List<Exercise> findAll() throws PersistenceExceptions {
        List<Exercise> all = new ArrayList<>();
        for (String line : file.readLines()) {
            all.add(fromLine(line));
        }
        return all;
    }

    // ---------- conversión ----------

    /**
     * Reescribe el archivo completo con la lista de ejercicios recibida.
     * Convierte todos los ejercicios a texto antes de tocar el archivo.
     *
     * @param exercises ejercicios que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<Exercise> exercises) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (Exercise e : exercises) {
                lines.add(toLine(e));
            }
        } catch (IllegalArgumentException ex) {
            throw new PersistenceExceptions("No se puede guardar: " + ex.getMessage(), ex);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte un ejercicio en una línea del archivo.
     *
     * @param e ejercicio a convertir
     * @return la línea con los cinco campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(Exercise e) {
        return TxtFormat.join(
                String.valueOf(e.getExerciseId()),
                e.getName(),
                e.getMuscleGroup(),
                String.valueOf(e.isLowImpact()),
                String.valueOf(e.isActive()));
    }

    /**
     * Convierte una línea del archivo en un ejercicio.
     *
     * @param line línea con el formato
     *             {@code id_ejercicio|nombre|grupo_muscular|bajo_impacto|activo}
     * @return el ejercicio leído; el grupo muscular vacío queda como {@code null}
     * @throws PersistenceExceptions si la línea no tiene 5 campos o tiene valores inválidos
     */
    private Exercise fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 5) {
            throw new PersistenceExceptions("Línea inválida en ejercicios.txt: " + line);
        }
        try {
            Exercise e = new Exercise();
            e.setExerciseId(Integer.parseInt(c[0]));
            e.setName(c[1]);
            e.setMuscleGroup(TxtFormat.nullIfEmpty(c[2]));
            e.setLowImpact(TxtFormat.parseBoolean(c[3]));
            e.setActive(TxtFormat.parseBoolean(c[4]));
            return e;
        } catch (DateTimeParseException | IllegalArgumentException ex) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en ejercicios.txt: " + line, ex);
        }
    }
}
