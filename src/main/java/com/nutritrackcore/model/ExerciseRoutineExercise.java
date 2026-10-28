package com.nutritrackcore.model;

public class ExerciseRoutineExercise {
    private int routineId;
    private int exerciseId;
    private int sets;
    private int repetitions;

    public ExerciseRoutineExercise() {}

    public ExerciseRoutineExercise(int routineId, int exerciseId, int sets, int repetitions) {
        this.routineId = routineId;
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.repetitions = repetitions;
    }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }
}