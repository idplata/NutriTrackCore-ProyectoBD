package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.ValidationException;

/**
 * Categorías del IMC con su rango: el mínimo se incluye y el máximo no.
 * La base de datos no guarda categorías (el IMC se calcula), así que se usan estas.
 */
public enum BmiCategory {
    LOW_WEIGHT("Bajo peso", 0, 18.5),
    NORMAL("Normal", 18.5, 25.0),
    OVERWEIGHT("Sobrepeso", 25, 30),
    OBESITY("Obesidad", 30, Double.MAX_VALUE);

    private final String displayName;
    private final double min;
    private final double max;

    BmiCategory(String displayName, double min, double max) {
        this.displayName = displayName;
        this.min = min;
        this.max = max;
    }

    public String getDisplayName() { return displayName; }
    public double getMin() { return min; }
    public double getMax() { return max; }

    /** Indica si un IMC cae en esta categoría. */
    public boolean contains(double bmi) {
        return bmi >= min && bmi < max;
    }

    /** Devuelve la categoría de un valor de IMC. */
    public static BmiCategory fromValue(double bmi) throws ValidationException {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            throw new ValidationException("El valor del IMC debe ser un número mayor que cero.");
        }
        for (BmiCategory category : values()) {
            if (category.contains(bmi)) {
                return category;
            }
        }
        return OBESITY;
    }
}