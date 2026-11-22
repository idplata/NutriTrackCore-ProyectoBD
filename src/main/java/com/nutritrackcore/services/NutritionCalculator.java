package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.Food;

/**
 * Calcula el aporte nutricional de una cantidad de alimento (kcal y macros).
 */
public final class NutritionCalculator {

    private NutritionCalculator() {
    }

    public static NutritionalSummary calculate(Food food, double grams) throws ValidationException {
        ParameterChecks.requireNonNull(food, "alimento");
        ParameterChecks.requireNonNegative(grams, "gramos");
        double factor = grams / 100.0;
        return new NutritionalSummary(
                food.getCalories100g() * factor,
                valueOrZero(food.getProteins100g()) * factor,
                valueOrZero(food.getCarbohydrates100g()) * factor,
                valueOrZero(food.getFats100g()) * factor);
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}