package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.services.ParameterChecks;
import java.util.List;

/**
 * Regla para el sobrepeso: déficit calórico, exclusión de alimentos muy
 * calóricos y rutinas solo con ejercicios de bajo impacto.
 * <p>
 * Valores de demostración: la meta calórica se reduce un 15 % y se excluyen los
 * alimentos de más de 500 kcal por cada 100 g.
 */
public class OverweightRule implements ConditionRule {

    /** Multiplicador que reduce la meta calórica un 15 %. */
    private static final double CALORIE_FACTOR = 0.85;

    /** Calorías máximas por 100 g para que un alimento sea aceptado. */
    private static final double MAX_CALORIES_PER_100G = 500;

    /**
     * Devuelve el nombre principal de la condición.
     *
     * @return {@code "Sobrepeso"}
     */
    @Override
    public String getConditionName() {
        return "Sobrepeso";
    }

    /**
     * Devuelve el nombre alternativo (en inglés) con el que puede aparecer en el catálogo.
     *
     * @return lista con {@code "Overweight"}
     */
    @Override
    public List<String> getAliases() {
        return List.of("Overweight");
    }

    /**
     * Aplica un déficit calórico del 15 %.
     *
     * @param baseCalories calorías antes del ajuste
     * @return {@code baseCalories × 0,85}
     * @throws IllegalArgumentException si el valor no es un número mayor que cero
     */
    @Override
    public double adjustCalories(double baseCalories) {
        ParameterChecks.requirePositive(baseCalories, "calorías base");
        return baseCalories * CALORIE_FACTOR;
    }

    /**
     * Acepta el alimento si tiene como máximo 500 kcal por cada 100 g.
     *
     * @param food alimento a evaluar
     * @return {@code true} si el alimento es aceptable
     * @throws IllegalArgumentException si el alimento es nulo
     */
    @Override
    public boolean isFoodAllowed(Food food) {
        ParameterChecks.requireNonNull(food, "alimento");
        return food.getCalories100g() <= MAX_CALORIES_PER_100G;
    }

    /**
     * Limita la intensidad a moderada.
     *
     * @return {@link IntensityLevel#MODERATE}
     */
    @Override
    public IntensityLevel maxIntensity() {
        return IntensityLevel.MODERATE;
    }

    /**
     * Exige ejercicios de bajo impacto para cuidar las articulaciones.
     *
     * @return {@code true}
     */
    @Override
    public boolean requiresLowImpact() {
        return true;
    }

    /**
     * Describe la restricción aplicada.
     *
     * @return texto en español con la limitación de ejercicio
     */
    @Override
    public String describeRestriction() {
        return "Solo ejercicios de bajo impacto e intensidad máxima moderada";
    }
}