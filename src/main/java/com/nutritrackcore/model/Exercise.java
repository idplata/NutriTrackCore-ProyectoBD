package com.nutritrackcore.model;

public class Exercise {
    private int exerciseId;
    private String name;
    private String muscleGroup;
    private boolean lowImpact;
    private boolean active;

    public Exercise() {}

    public Exercise(String name, String muscleGroup, boolean lowImpact) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.lowImpact = lowImpact;
        this.active = true;
    }

    public int getExerciseId() { return exerciseId; }
    public void setExerciseId(int exerciseId) { this.exerciseId = exerciseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public boolean isLowImpact() { return lowImpact; }
    public void setLowImpact(boolean lowImpact) { this.lowImpact = lowImpact; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}