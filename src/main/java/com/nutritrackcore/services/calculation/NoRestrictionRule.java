package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;
import com.nutritrackcore.services.ParameterChecks;

/**
 * Regla "nula": no cambia nada. Se usa cuando una condición médica del catálogo
 * todavía no tiene una regla propia, de modo que los servicios no tengan que
 * comprobar {@code null} ni tratar ese caso aparte (patrón Objeto Nulo).
 */
public class NoRestrictionRule implements ConditionRule {

    /**
     * Devuelve un nombre descriptivo; esta regla no está asociada a ninguna
     * condición concreta.
     *
     * @return {@code "Sin restricción"}
     */
    @Override
    public String getConditionName() {
        return "Sin restricción";
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
     * No limita la intensidad.
     *
     * @return {@link IntensityLevel#HIGH}
     */
    @Override
    public IntensityLevel maxIntensity() {
        return IntensityLevel.HIGH;
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
     * Describe que no hay restricción.
     *
     * @return {@code "Sin restricciones"}
     */
    @Override
    public String describeRestriction() {
        return "Sin restricciones";
    }
}