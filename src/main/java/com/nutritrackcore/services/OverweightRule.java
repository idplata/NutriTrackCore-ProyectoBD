package com.nutritrackcore.services;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.Food.FoodPyramid;
import com.nutritrackcore.model.IntensityLevel;
import java.util.List;
import java.util.Set;

/**
 * Sobrepeso: −15 % de kcal, alimentos de hasta 500 kcal por 100 g, carbohidratos,
 * proteínas y arroz al 75 %, grasas saludables al 50 %, solo bajo impacto,
 * 3 series y máximo 10 repeticiones.
 */
public class OverweightRule implements ConditionRule {
    public static final String NAME = "Sobrepeso";
    private static final double CALORIE_FACTOR = 0.85;
    private static final double MAX_CALORIES_PER_100G = 500;
    private static final double LIMITED_GROUP_FACTOR = 0.75;
    private static final double HEALTHY_FAT_FACTOR = 0.5;
    private static final Set<FoodPyramid> LIMITED_GROUPS =
            Set.of(FoodPyramid.CARBOHIDRATOS, FoodPyramid.PROTEINAS_Y_LACTEOS, FoodPyramid.ARROZ);
    private static final int SETS = 3;
    private static final int MAX_REPETITIONS = 10;

    @Override
    public String getConditionName() { return NAME; }

    @Override
    public List<String> getAliases() { return List.of("Overweight"); }

    @Override
    public double adjustCalories(double baseCalories) { return baseCalories * CALORIE_FACTOR; }

    @Override
    public boolean isFoodAllowed(Food food) {
        return food != null && food.getCalories100g() <= MAX_CALORIES_PER_100G;
    }

    @Override
    public double groupFactor(FoodPyramid group) {
        if (group == FoodPyramid.GRASA_SALUDABLES) {
            return HEALTHY_FAT_FACTOR;
        }
        return LIMITED_GROUPS.contains(group) ? LIMITED_GROUP_FACTOR : 1.0;
    }

    @Override
    public IntensityLevel maxIntensity() { return IntensityLevel.MODERATE; }

    @Override
    public boolean requiresLowImpact() { return true; }

    @Override
    public Integer fixedSets() { return SETS; }

    @Override
    public Integer maxRepetitions() { return MAX_REPETITIONS; }

    @Override
    public String describeRestriction() {
        return "Solo bajo impacto, 3 series de máximo 10 repeticiones e intensidad máxima moderada";
    }
}