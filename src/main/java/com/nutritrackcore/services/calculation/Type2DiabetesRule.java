package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.services.ParameterChecks;
import java.util.List;

/**
 * Regla para la diabetes tipo 2: dieta baja en carbohidratos y ejercicio de
 * intensidad moderada como máximo.
 * <p>
 * Valores de demostración: se excluyen los alimentos con más de 30 g de
 * carbohidratos por cada 100 g y se reduce la meta calórica un 5 %.
 */
public class Type2DiabetesRule implements ConditionRule {

    /** Carbohidratos máximos por 100 g para que un alimento sea aceptado. */
    private static final double MAX_CARBS_PER_100G = 30;

    /** Multiplicador que reduce la meta calórica un 5 %. */
    private static final double CALORIE_FACTOR = 0.95;

    /**
     * Devuelve el nombre principal de la condición.
     *
     * @return {@code "Diabetes tipo 2"}
     */
    @Override
    public String getConditionName() {
        return "Diabetes tipo 2";
    }

    /**
     * Devuelve el nombre alternativo (en inglés) con el que puede aparecer en el catálogo.
     *
     * @return lista con {@code "Type 2 diabetes"}
     */
    @Override
    public List<String> getAliases() {
        return List.of("Type 2 diabetes");
    }

    /**
     * Reduce la meta calórica un 5 %.
     *
     * @param baseCalories calorías antes del ajuste
     * @return {@code baseCalories × 0,95}
     * @throws IllegalArgumentException si el valor no es un número mayor que cero
     */
    @Override
    public double adjustCalories(double baseCalories) {
        ParameterChecks.requirePositive(baseCalories, "calorías base");
        return baseCalories * CALORIE_FACTOR;
    }

    /**
     * Acepta el alimento si no tiene dato de carbohidratos o si tiene como
     * máximo 30 g por cada 100 g.
     *
     * @param food alimento a evaluar
     * @return {@code true} si el alimento es aceptable
     * @throws IllegalArgumentException si el alimento es nulo
     */
    @Override
    public boolean isFoodAllowed(Food food) {
        ParameterChecks.requireNonNull(food, "alimento");
        Double carbs = food.getCarbohydrates100g();
        return carbs == null || carbs <= MAX_CARBS_PER_100G;
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
     * No exige ejercicios de bajo impacto.
     *
     * @return {@code false}
     */
    @Override
    public boolean requiresLowImpact() {
        return false;
    }

    /**
     * Describe la restricción aplicada.
     *
     * @return texto en español con la limitación de dieta y de intensidad
     */
    @Override
    public String describeRestriction() {
        return "Dieta baja en carbohidratos e intensidad de ejercicio máxima moderada";
    }
}