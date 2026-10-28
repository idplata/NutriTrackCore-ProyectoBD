package com.nutritrackcore.model;

public class NutritionalIntake {
    private double calories;
    private double proteins;
    private double carbohydrates;
    private double fats;

    public NutritionalIntake() {}

    public NutritionalIntake(double calories, double proteins, double carbohydrates, double fats) {
        this.calories = calories;
        this.proteins = proteins;
        this.carbohydrates = carbohydrates;
        this.fats = fats;
    }

    public double getCalories() { return calories; }
    public double getProteins() { return proteins; }
    public double getCarbohydrates() { return carbohydrates; }
    public double getFats() { return fats; }

    // Key method required by the architecture
    public NutritionalIntake add(NutritionalIntake other) {
        return new NutritionalIntake(
            this.calories + other.calories,
            this.proteins + other.proteins,
            this.carbohydrates + other.carbohydrates,
            this.fats + other.fats
        );
    }
    
    public static NutritionalIntake zero() {
        return new NutritionalIntake(0, 0, 0, 0);
    }
}