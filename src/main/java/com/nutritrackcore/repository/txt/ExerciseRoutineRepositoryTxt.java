package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de {@link ExerciseRoutineRepository} sobre tres archivos:
 * {@code rutinas_ejercicio.txt}, {@code rutina_ejercicio_ejercicio.txt} y
 * {@code rutina_ejercicio_condicion.txt}.
 * <p>
 * Formatos de línea:
 * <ul>
 *   <li>Rutina: {@code id_rutina|id_usuario|fecha_generacion|nivel_intensidad|activa|id_historial_base|id_plan_dieta}.</li>
 *   <li>Ejercicio de la rutina: {@code id_rutina|id_ejercicio|series|repeticiones}.</li>
 *   <li>Condición considerada: {@code id_rutina|id_condicion|restriccion_aplicada}.</li>
 * </ul>
 * Al leer, cada rutina recibe sus ejercicios y condiciones agrupándolos por
 * {@code id_rutina}; al escribir, se reescriben los tres archivos completos.
 */
public class ExerciseRoutineRepositoryTxt implements ExerciseRoutineRepository {

    /** Cabecera del archivo {@code rutinas_ejercicio.txt}. */
    public static final String ROUTINES_HEADER =
            "#id_rutina|id_usuario|fecha_generacion|nivel_intensidad|activa|id_historial_base|id_plan_dieta";

    /** Cabecera del archivo {@code rutina_ejercicio_ejercicio.txt}. */
    public static final String EXERCISES_HEADER =
            "#id_rutina|id_ejercicio|series|repeticiones";

    /** Cabecera del archivo {@code rutina_ejercicio_condicion.txt}. */
    public static final String CONDITIONS_HEADER =
            "#id_rutina|id_condicion|restriccion_aplicada";

    /**
     * Orden estándar de las rutinas: por fecha de generación (las nulas van
     * primero) y, en caso de empate, por id.
     */
    private static final Comparator<ExerciseRoutine> BY_DATE =
            Comparator.comparing(ExerciseRoutine::getGenerationDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparingInt(ExerciseRoutine::getRoutineId);

    /** Acceso al archivo .txt de rutinas. */
    private final TxtFile routinesFile;

    /** Acceso al archivo .txt de los ejercicios de cada rutina. */
    private final TxtFile exercisesFile;

    /** Acceso al archivo .txt de las condiciones médicas consideradas en cada rutina. */
    private final TxtFile conditionsFile;

    /**
     * Crea el repositorio sobre tres archivos ya preparados.
     *
     * @param routinesFile   archivo {@code rutinas_ejercicio.txt}
     * @param exercisesFile  archivo {@code rutina_ejercicio_ejercicio.txt}
     * @param conditionsFile archivo {@code rutina_ejercicio_condicion.txt}
     */
    public ExerciseRoutineRepositoryTxt(TxtFile routinesFile, TxtFile exercisesFile, TxtFile conditionsFile) {
        this.routinesFile = routinesFile;
        this.exercisesFile = exercisesFile;
        this.conditionsFile = conditionsFile;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1 y se copia a cada
     * ejercicio y condición de la rutina al escribir.
     */
    @Override
    public ExerciseRoutine save(ExerciseRoutine routine) throws PersistenceExceptions {
        List<ExerciseRoutine> all = readAll();
        List<Integer> ids = new ArrayList<>();
        for (ExerciseRoutine r : all) {
            ids.add(r.getRoutineId());
        }
        routine.setRoutineId(IdGenerator.next(ids));   // writeAll copia este id a ejercicios y condiciones
        all.add(routine);
        writeAll(all);
        return routine;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe una rutina con ese id
     */
    @Override
    public void update(ExerciseRoutine routine) throws PersistenceExceptions {
        List<ExerciseRoutine> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRoutineId() == routine.getRoutineId()) {
                all.set(i, routine);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe la rutina con id " + routine.getRoutineId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ExerciseRoutine> findById(int routineId) throws PersistenceExceptions {
        for (ExerciseRoutine r : readAll()) {
            if (r.getRoutineId() == routineId) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * El resultado se ordena por fecha de generación, y en caso de empate por id.
     */
    @Override
    public List<ExerciseRoutine> findByUserId(int userId) throws PersistenceExceptions {
        List<ExerciseRoutine> result = new ArrayList<>();
        for (ExerciseRoutine r : readAll()) {
            if (r.getUserId() == userId) {
                result.add(r);
            }
        }
        result.sort(BY_DATE);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Recorre las rutinas del usuario ya ordenadas por fecha y se queda con la
     * última que esté activa.
     */
    @Override
    public Optional<ExerciseRoutine> findActiveByUserId(int userId) throws PersistenceExceptions {
        ExerciseRoutine latest = null;
        for (ExerciseRoutine r : findByUserId(userId)) {   // ya viene ordenada: la última activa gana
            if (r.isActive()) {
                latest = r;
            }
        }
        return Optional.ofNullable(latest);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe los archivos si realmente se eliminó alguna rutina.
     */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<ExerciseRoutine> all = readAll();
        if (all.removeIf(r -> r.getUserId() == userId)) {
            writeAll(all);
        }
    }

    // ---------- lectura y escritura de los tres archivos ----------

    /**
     * Lee los tres archivos y arma las rutinas completas. Primero agrupa los
     * ejercicios y las condiciones por {@code id_rutina} y luego se los asigna a
     * cada rutina. Las filas cuya rutina no existe (huérfanas) se ignoran.
     *
     * @return lista con todas las rutinas, cada una con sus ejercicios y condiciones
     * @throws PersistenceExceptions si alguna línea de cualquiera de los archivos
     *                               es inválida o está corrupta
     */
    private List<ExerciseRoutine> readAll() throws PersistenceExceptions {
        Map<Integer, List<ExerciseRoutineExercise>> exercisesByRoutine = new HashMap<>();
        for (String text : exercisesFile.readLines()) {
            ExerciseRoutineExercise e = exerciseFromText(text);
            exercisesByRoutine.computeIfAbsent(e.getRoutineId(), k -> new ArrayList<>()).add(e);
        }
        Map<Integer, List<ExerciseRoutineCondition>> conditionsByRoutine = new HashMap<>();
        for (String text : conditionsFile.readLines()) {
            ExerciseRoutineCondition c = conditionFromText(text);
            conditionsByRoutine.computeIfAbsent(c.getRoutineId(), k -> new ArrayList<>()).add(c);
        }
        List<ExerciseRoutine> routines = new ArrayList<>();
        for (String text : routinesFile.readLines()) {
            ExerciseRoutine r = routineFromText(text);
            r.setLines(new ArrayList<>(exercisesByRoutine.getOrDefault(r.getRoutineId(), new ArrayList<>())));
            r.setConsideredConditions(new ArrayList<>(conditionsByRoutine.getOrDefault(r.getRoutineId(), new ArrayList<>())));
            routines.add(r);
        }
        return routines;
    }

    /**
     * Escribe primero rutinas_ejercicio.txt y después los otros dos, y arma todo el texto
     * antes de tocar ningún archivo. Cada archivo se reemplaza de forma segura; si algo se
     * corta entre ellos, quedan filas huérfanas que se descartan en la siguiente escritura.
     * <p>
     * Antes de convertir cada ejercicio y condición, les copia el id de la rutina
     * a la que pertenecen (las filas creadas antes de guardar la rutina traen
     * {@code routineId} 0).
     *
     * @param routines rutinas que quedarán en los archivos, con sus ejercicios y condiciones
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void writeAll(List<ExerciseRoutine> routines) throws PersistenceExceptions {
        List<String> routineTexts = new ArrayList<>();
        List<String> exerciseTexts = new ArrayList<>();
        List<String> conditionTexts = new ArrayList<>();
        try {
            for (ExerciseRoutine r : routines) {
                routineTexts.add(routineToText(r));
                for (ExerciseRoutineExercise e : r.getLines()) {
                    e.setRoutineId(r.getRoutineId());           // las líneas creadas antes del save traen routineId 0
                    exerciseTexts.add(exerciseToText(e));
                }
                for (ExerciseRoutineCondition c : r.getConsideredConditions()) {
                    c.setRoutineId(r.getRoutineId());
                    conditionTexts.add(conditionToText(c));
                }
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        routinesFile.rewrite(routineTexts);
        exercisesFile.rewrite(exerciseTexts);
        conditionsFile.rewrite(conditionTexts);
    }

    /**
     * Convierte una rutina (sin sus ejercicios ni condiciones) en una línea de
     * {@code rutinas_ejercicio.txt}. El nivel de intensidad se escribe por el
     * nombre del enum y el id del plan de dieta nulo queda como campo vacío.
     *
     * @param r rutina a convertir
     * @return la línea con los siete campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String routineToText(ExerciseRoutine r) {
        return TxtFormat.join(
                String.valueOf(r.getRoutineId()),
                String.valueOf(r.getUserId()),
                TxtFormat.text(r.getGenerationDate()),
                r.getIntensityLevel() == null ? null : r.getIntensityLevel().name(),
                String.valueOf(r.isActive()),
                String.valueOf(r.getBaseHistoryId()),
                TxtFormat.text(r.getDietPlanId()));             // puede ser nulo -> campo vacío
    }

    /**
     * Convierte un ejercicio de rutina en una línea de
     * {@code rutina_ejercicio_ejercicio.txt}.
     *
     * @param e ejercicio de la rutina a convertir
     * @return la línea con los cuatro campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String exerciseToText(ExerciseRoutineExercise e) {
        return TxtFormat.join(
                String.valueOf(e.getRoutineId()),
                String.valueOf(e.getExerciseId()),
                String.valueOf(e.getSets()),
                String.valueOf(e.getRepetitions()));
    }

    /**
     * Convierte una condición considerada en una línea de
     * {@code rutina_ejercicio_condicion.txt}.
     *
     * @param c condición de la rutina a convertir
     * @return la línea con los tres campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String conditionToText(ExerciseRoutineCondition c) {
        return TxtFormat.join(
                String.valueOf(c.getRoutineId()),
                String.valueOf(c.getConditionId()),
                c.getAppliedRestriction());
    }

    /**
     * Convierte una línea de {@code rutinas_ejercicio.txt} en una rutina
     * (todavía sin ejercicios ni condiciones, que los asigna {@link #readAll()}).
     * Los campos vacíos de nivel de intensidad e id del plan de dieta quedan como
     * {@code null}; si el nivel tiene valor, debe ser el nombre de un
     * {@link IntensityLevel}.
     *
     * @param text línea con el formato descrito en {@link ExerciseRoutineRepositoryTxt}
     * @return la rutina leída
     * @throws PersistenceExceptions si la línea no tiene 7 campos o tiene valores inválidos
     */
    private ExerciseRoutine routineFromText(String text) throws PersistenceExceptions {
        String[] c = TxtFormat.split(text);
        if (c.length != 7) {
            throw new PersistenceExceptions("Línea inválida en rutinas_ejercicio.txt: " + text);
        }
        try {
            ExerciseRoutine r = new ExerciseRoutine();
            r.setRoutineId(Integer.parseInt(c[0]));
            r.setUserId(Integer.parseInt(c[1]));
            r.setGenerationDate(TxtFormat.parseDate(c[2]));
            String level = TxtFormat.nullIfEmpty(c[3]);
            r.setIntensityLevel(level == null ? null : IntensityLevel.valueOf(level));   // enums por nombre
            r.setActive(TxtFormat.parseBoolean(c[4]));
            r.setBaseHistoryId(Integer.parseInt(c[5]));
            r.setDietPlanId(TxtFormat.parseInteger(c[6]));
            return r;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en rutinas_ejercicio.txt: " + text, e);
        }
    }

    /**
     * Convierte una línea de {@code rutina_ejercicio_ejercicio.txt} en un
     * ejercicio de rutina.
     *
     * @param text línea con el formato {@code id_rutina|id_ejercicio|series|repeticiones}
     * @return el ejercicio de la rutina leído
     * @throws PersistenceExceptions si la línea no tiene 4 campos o tiene valores inválidos
     */
    private ExerciseRoutineExercise exerciseFromText(String text) throws PersistenceExceptions {
        String[] c = TxtFormat.split(text);
        if (c.length != 4) {
            throw new PersistenceExceptions("Línea inválida en rutina_ejercicio_ejercicio.txt: " + text);
        }
        try {
            ExerciseRoutineExercise e = new ExerciseRoutineExercise();
            e.setRoutineId(Integer.parseInt(c[0]));
            e.setExerciseId(Integer.parseInt(c[1]));
            e.setSets(Integer.parseInt(c[2]));
            e.setRepetitions(Integer.parseInt(c[3]));
            return e;
        } catch (IllegalArgumentException ex) {   // incluye NumberFormatException
            throw new PersistenceExceptions("Línea corrupta en rutina_ejercicio_ejercicio.txt: " + text, ex);
        }
    }

    /**
     * Convierte una línea de {@code rutina_ejercicio_condicion.txt} en una
     * condición considerada. La restricción aplicada vacía queda como {@code null}.
     *
     * @param text línea con el formato {@code id_rutina|id_condicion|restriccion_aplicada}
     * @return la condición de la rutina leída
     * @throws PersistenceExceptions si la línea no tiene 3 campos o tiene valores inválidos
     */
    private ExerciseRoutineCondition conditionFromText(String text) throws PersistenceExceptions {
        String[] c = TxtFormat.split(text);
        if (c.length != 3) {
            throw new PersistenceExceptions("Línea inválida en rutina_ejercicio_condicion.txt: " + text);
        }
        try {
            ExerciseRoutineCondition rc = new ExerciseRoutineCondition();
            rc.setRoutineId(Integer.parseInt(c[0]));
            rc.setConditionId(Integer.parseInt(c[1]));
            rc.setAppliedRestriction(TxtFormat.nullIfEmpty(c[2]));
            return rc;
        } catch (IllegalArgumentException ex) {   // incluye NumberFormatException
            throw new PersistenceExceptions("Línea corrupta en rutina_ejercicio_condicion.txt: " + text, ex);
        }
    }
}
