package com.nutritrackcore.model;

public class Exercise {
    private int exerciseId;
    private String name;
    private String muscleGroup;
    private boolean lowImpact;
    private boolean active;
    private boolean need_Equipment;
    public Exercise() {}

    public Exercise(String name, String muscleGroup, boolean lowImpact, boolean need_Equipment) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.lowImpact = lowImpact;
        this.active = true;
        this.need_Equipment = need_Equipment;
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
    
    public boolean getNeed_Equipment(){
        return need_Equipment;
    }
    public void setNeed_Equipment(boolean need_Equipment){
        this.need_Equipment = need_Equipment;
    }
}