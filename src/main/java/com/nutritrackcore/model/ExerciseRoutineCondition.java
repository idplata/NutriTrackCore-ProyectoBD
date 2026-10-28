package com.nutritrackcore.model;

public class ExerciseRoutineCondition {
    private int routineId;
    private int conditionId;
    private String appliedRestriction;

    public ExerciseRoutineCondition() {}

    public ExerciseRoutineCondition(int routineId, int conditionId, String appliedRestriction) {
        this.routineId = routineId;
        this.conditionId = conditionId;
        this.appliedRestriction = appliedRestriction;
    }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }

    public String getAppliedRestriction() { return appliedRestriction; }
    public void setAppliedRestriction(String appliedRestriction) { this.appliedRestriction = appliedRestriction; }
}