package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.ValidationException;

/**
 * Resultado del cálculo del IMC: valor y categoría.
 */
public class BmiResult {
    private final double value;
    private final BmiCategory category;

    private BmiResult(double value, BmiCategory category) {
        this.value = value;
        this.category = category;
    }

    /** Calcula el IMC: peso (kg) / altura (m)². */
    public static BmiResult calculate(double weightKg, double heightCm) throws ValidationException {
        ParameterChecks.requireMeasurements(weightKg, heightCm);
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        return new BmiResult(bmi, BmiCategory.fromValue(bmi));
    }

    public double getValue() { return value; }
    public BmiCategory getCategory() { return category; }

    @Override
    public String toString() {
        return String.format("BmiResult{value=%.2f, category=%s}", value, category);
    }
}