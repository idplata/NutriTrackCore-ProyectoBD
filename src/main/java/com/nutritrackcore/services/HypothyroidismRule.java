package com.nutritrackcore.services;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.Food.FoodPyramid;
import com.nutritrackcore.model.IntensityLevel;
import java.util.List;
import java.util.Set;

/**
 * Hipotiroidismo: sin dulces ni ultraprocesados, carbohidratos y proteínas al 75 %
 * e intensidad máxima moderada.
 */
public class HypothyroidismRule implements ConditionRule {
    private static final double LIMITED_GROUP_FACTOR = 0.75;
    private static final Set<FoodPyramid> BLOCKED_GROUPS = Set.of(FoodPyramid.DULCES, FoodPyramid.ULTRAPROCESADOS);
    private static final Set<FoodPyramid> LIMITED_GROUPS =
            Set.of(FoodPyramid.CARBOHIDRATOS, FoodPyramid.PROTEINAS_Y_LACTEOS);

    @Override
    public String getConditionName() { return "Hipotiroidismo"; }

    @Override
    public List<String> getAliases() { return List.of("Hypothyroidism"); }

    @Override
    public double adjustCalories(double baseCalories) { return baseCalories; }

    @Override
    public boolean isFoodAllowed(Food food) {
        return food != null && !BLOCKED_GROUPS.contains(food.getGrupe());
    }

    @Override
    public double groupFactor(FoodPyramid group) {
        return LIMITED_GROUPS.contains(group) ? LIMITED_GROUP_FACTOR : 1.0;
    }

    @Override
    public IntensityLevel maxIntensity() { return IntensityLevel.MODERATE; }

    @Override
    public boolean requiresLowImpact() { return false; }

    @Override
    public String describeRestriction() {
        return "Sin dulces ni ultraprocesados, carbohidratos y proteínas al 75 % e intensidad máxima moderada";
    }
}