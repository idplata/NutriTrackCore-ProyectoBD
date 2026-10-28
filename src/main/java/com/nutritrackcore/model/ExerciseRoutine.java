package com.nutritrackcore.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExerciseRoutine {
    private int routineId;
    private int userId;
    private LocalDate generationDate;
    private IntensityLevel intensityLevel;
    private boolean active;
    private int baseHistoryId;
    private Integer dietPlanId; // Can be null
    private List<ExerciseRoutineExercise> lines;
    private List<ExerciseRoutineCondition> consideredConditions;

    public ExerciseRoutine() {
        this.lines = new ArrayList<>();
        this.consideredConditions = new ArrayList<>();
    }

    public ExerciseRoutine(int userId, LocalDate generationDate, IntensityLevel intensityLevel, int baseHistoryId, Integer dietPlanId) {
        this.userId = userId;
        this.generationDate = generationDate;
        this.intensityLevel = intensityLevel;
        this.baseHistoryId = baseHistoryId;
        this.dietPlanId = dietPlanId;
        this.active = true;
        this.lines = new ArrayList<>();
        this.consideredConditions = new ArrayList<>();
    }

    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getGenerationDate() { return generationDate; }
    public void setGenerationDate(LocalDate generationDate) { this.generationDate = generationDate; }

    public IntensityLevel getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(IntensityLevel intensityLevel) { this.intensityLevel = intensityLevel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void deactivate() { this.active = false; }

    public int getBaseHistoryId() { return baseHistoryId; }
    public void setBaseHistoryId(int baseHistoryId) { this.baseHistoryId = baseHistoryId; }

    public Integer getDietPlanId() { return dietPlanId; }
    public void setDietPlanId(Integer dietPlanId) { this.dietPlanId = dietPlanId; }

    // Creator Methods
    public void addExercise(int exerciseId, int sets, int repetitions) {
        this.lines.add(new ExerciseRoutineExercise(this.routineId, exerciseId, sets, repetitions));
    }

    public void registerCondition(int conditionId, String appliedRestriction) {
        this.consideredConditions.add(new ExerciseRoutineCondition(this.routineId, conditionId, appliedRestriction));
    }

    public List<ExerciseRoutineExercise> getLines() {
        return Collections.unmodifiableList(lines);
    }
    
    public void setLines(List<ExerciseRoutineExercise> lines) {
        this.lines = lines;
    }

    public List<ExerciseRoutineCondition> getConsideredConditions() {
        return Collections.unmodifiableList(consideredConditions);
    }
    
    public void setConsideredConditions(List<ExerciseRoutineCondition> conditions) {
        this.consideredConditions = conditions;
    }
}