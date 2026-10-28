package com.nutritrackcore.model;

public class Food {
    private int foodId;
    private String name;
    private Double densityGCm3;
    private double calories100g;
    private Double proteins100g;
    private Double carbohydrates100g;
    private Double fats100g;
    private boolean active;

    public Food() {}

    public Food(String name, double calories100g, Double proteins100g, Double carbohydrates100g, Double fats100g) {
        this.name = name;
        this.calories100g = calories100g;
        this.proteins100g = proteins100g;
        this.carbohydrates100g = carbohydrates100g;
        this.fats100g = fats100g;
        this.active = true;
    }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDensityGCm3() { return densityGCm3; }
    public void setDensityGCm3(Double densityGCm3) { this.densityGCm3 = densityGCm3; }

    public double getCalories100g() { return calories100g; }
    public void setCalories100g(double calories100g) { this.calories100g = calories100g; }

    public Double getProteins100g() { return proteins100g; }
    public void setProteins100g(Double proteins100g) { this.proteins100g = proteins100g; }

    public Double getCarbohydrates100g() { return carbohydrates100g; }
    public void setCarbohydrates100g(Double carbohydrates100g) { this.carbohydrates100g = carbohydrates100g; }

    public Double getFats100g() { return fats100g; }
    public void setFats100g(Double fats100g) { this.fats100g = fats100g; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // Domain methods required by the architecture
    public NutritionalIntake calculateIntake(double grams) {
        double factor = grams / 100.0;
        return new NutritionalIntake(
            calories100g * factor,
            (proteins100g != null ? proteins100g : 0.0) * factor,
            (carbohydrates100g != null ? carbohydrates100g : 0.0) * factor,
            (fats100g != null ? fats100g : 0.0) * factor
        );
    }

    public boolean hasDensity() {
        return densityGCm3 != null && densityGCm3 > 0;
    }

    public double gramsFromVolume(double cm3) {
        return hasDensity() ? cm3 * densityGCm3 : 0.0;
    }
}