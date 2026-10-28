package com.nutritrackcore.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DietPlan {
    private int planId;
    private int userId;
    private LocalDate generationDate;
    private double targetCalories;
    private boolean active;
    private List<DietPlanFood> lines;

    public DietPlan() {
        this.lines = new ArrayList<>();
    }

    public DietPlan(int userId, LocalDate generationDate, double targetCalories) {
        this.userId = userId;
        this.generationDate = generationDate;
        this.targetCalories = targetCalories;
        this.active = true;
        this.lines = new ArrayList<>();
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getGenerationDate() { return generationDate; }
    public void setGenerationDate(LocalDate generationDate) { this.generationDate = generationDate; }

    public double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void deactivate() { this.active = false; }

    // Creator Method
    public void addFood(int foodId, double quantityG, MealTime meal) {
        // Validate that the food is not repeated (composite PK)
        for (DietPlanFood line : lines) {
            if (line.getFoodId() == foodId) {
                throw new IllegalArgumentException("Esta comida ya está en el plan.");
            }
        }
        this.lines.add(new DietPlanFood(this.planId, foodId, quantityG, meal));
    }

    public List<DietPlanFood> getLines() {
        return Collections.unmodifiableList(lines);
    }
    
    public void setLines(List<DietPlanFood> lines) {
        this.lines = lines;
    }
}