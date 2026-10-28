package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.services.ParameterChecks;
import java.util.List;

/**
 * Regla para el hipotiroidismo: no restringe alimentos, pero limita la
 * intensidad del ejercicio a moderada.
 * <p>
 * Valor de demostración: la meta calórica se mantiene sin cambios.
 */
public class HypothyroidismRule implements ConditionRule {

    /**
     * Devuelve el nombre principal de la condición.
     *
     * @return {@code "Hipotiroidismo"}
     */
    @Override
    public String getConditionName() {
        return "Hipotiroidismo";
    }

    /**
     * Devuelve el nombre alternativo (en inglés) con el que puede aparecer en el catálogo.
     *
     * @return lista con {@code "Hypothyroidism"}
     */
    @Override
    public List<String> getAliases() {
        return List.of("Hypothyroidism");
    }

    /**
     * No modifica la meta calórica.
     *
     * @param baseCalories calorías antes del ajuste
     * @return el mismo valor recibido
     * @throws IllegalArgumentException si el valor no es un número mayor que cero
     */
    @Override
    public double adjustCalories(double baseCalories) {
        ParameterChecks.requirePositive(baseCalories, "calorías base");
        return baseCalories;
    }

    /**
     * Acepta cualquier alimento.
     *
     * @param food alimento a evaluar
     * @return siempre {@code true}
     * @throws IllegalArgumentException si el alimento es nulo
     */
    @Override
    public boolean isFoodAllowed(Food food) {
        ParameterChecks.requireNonNull(food, "alimento");
        return true;
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
     * @return texto en español con la limitación de intensidad
     */
    @Override
    public String describeRestriction() {
        return "Intensidad de ejercicio máxima moderada";
    }
}