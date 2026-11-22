package com.nutritrackcore.services;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;

/**
 * Regla por defecto (Objeto Nulo): no cambia nada.
 */
public class NoRestrictionRule implements ConditionRule {

    @Override
    public String getConditionName() { return "Sin restricción"; }

    @Override
    public double adjustCalories(double baseCalories) { return baseCalories; }

    @Override
    public boolean isFoodAllowed(Food food) { return food != null; }

    @Override
    public IntensityLevel maxIntensity() { return IntensityLevel.HIGH; }

    @Override
    public boolean requiresLowImpact() { return false; }

    @Override
    public String describeRestriction() { return "Sin restricciones"; }
}