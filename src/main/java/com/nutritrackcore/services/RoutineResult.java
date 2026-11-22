package com.nutritrackcore.services;

import com.nutritrackcore.model.ExerciseRoutine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rutina generada junto con los avisos para el usuario
 * (por ejemplo, que no alcanzaron los ejercicios sin equipo para entrenar en casa).
 */
public class RoutineResult {
    private final ExerciseRoutine routine;
    private final List<String> warnings;

    public RoutineResult(ExerciseRoutine routine, List<String> warnings) {
        this.routine = routine;
        this.warnings = new ArrayList<>(warnings);
    }

    public ExerciseRoutine getRoutine() { return routine; }

    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    public boolean hasWarnings() { return !warnings.isEmpty(); }
}