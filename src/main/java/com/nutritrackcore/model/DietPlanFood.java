package com.nutritrackcore.model;

public class DietPlanFood {
    private int planId;
    private int foodId;
    private double quantityG;
    private MealTime mealTime;

    public DietPlanFood() {}

    public DietPlanFood(int planId, int foodId, double quantityG, MealTime mealTime) {
        this.planId = planId;
        this.foodId = foodId;
        this.quantityG = quantityG;
        this.mealTime = mealTime;
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public double getQuantityG() { return quantityG; }
    public void setQuantityG(double quantityG) { this.quantityG = quantityG; }

    public MealTime getMealTime() { return mealTime; }
    public void setMealTime(MealTime mealTime) { this.mealTime = mealTime; }
}