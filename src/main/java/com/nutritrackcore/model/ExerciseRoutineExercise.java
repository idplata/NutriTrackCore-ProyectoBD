package com.nutritrackcore.model;

public abstract class ExerciseRoutineExercise {
    private int routineId;
    private int exerciseId;
    private int sets;

    public ExerciseRoutineExercise() {}

    public ExerciseRoutineExercise(int routineId, int exerciseId, int sets) {
        this.routineId = routineId;
        this.exerciseId = exerciseId;
        this.sets = sets;
    }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

}