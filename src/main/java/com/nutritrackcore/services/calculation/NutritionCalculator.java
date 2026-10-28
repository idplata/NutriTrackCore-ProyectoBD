package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.services.ParameterChecks;
import com.nutritrackcore.services.dto.NutritionalSummary;

/**
 * Calcula cuánto aporta nutricionalmente una porción de un alimento.
 * <p>
 * Los valores del catálogo están expresados por cada 100 gramos, así que el
 * aporte de una porción es una regla de tres. Esta lógica la usan varios
 * servicios (consumo, planes de dieta y consultas); se centraliza aquí para no
 * repetirla.
 * <p>
 * Clase de utilidad: no se instancia y sus métodos son {@code static}.
 */
public final class NutritionCalculator {

    /** Constructor privado: la clase solo ofrece métodos estáticos. */
    private NutritionCalculator() {
    }

    /**
     * Calcula el aporte nutricional de una porción.
     * <p>
     * Pasos: (1) valida que el alimento no sea nulo y que los gramos sean un
     * número finito no negativo; (2) calcula el factor {@code gramos / 100};
     * (3) multiplica por ese factor las calorías y cada macronutriente del
     * alimento. Un macronutriente que el catálogo deja sin dato ({@code null})
     * se cuenta como 0.
     *
     * @param food  alimento consumido
     * @param grams cantidad consumida, en gramos
     * @return aporte de la porción (calorías, proteínas, carbohidratos y grasas)
     * @throws IllegalArgumentException si el alimento es nulo o los gramos no son válidos
     */
    public static NutritionalSummary calculate(Food food, double grams) {
        ParameterChecks.requireNonNull(food, "alimento");
        ParameterChecks.requireNonNegative(grams, "gramos");
        double factor = grams / 100.0;
        return new NutritionalSummary(
                food.getCalories100g() * factor,
                valueOrZero(food.getProteins100g()) * factor,
                valueOrZero(food.getCarbohydrates100g()) * factor,
                valueOrZero(food.getFats100g()) * factor);
    }

    /**
     * Convierte un dato opcional del catálogo en un número utilizable.
     *
     * @param value valor posiblemente nulo
     * @return el valor, o 0 si era {@code null}
     */
    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}