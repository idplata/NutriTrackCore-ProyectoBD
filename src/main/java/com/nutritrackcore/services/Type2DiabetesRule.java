package com.nutritrackcore.services;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.Food.FoodPyramid;
import com.nutritrackcore.model.IntensityLevel;
import java.util.List;
import java.util.Set;

/**
 * Diabetes tipo 2: −5 % de kcal, sin dulces ni ultraprocesados, máximo 30 g de
 * carbohidratos por 100 g e intensidad máxima moderada.
 */
public class Type2DiabetesRule implements ConditionRule {
    private static final double MAX_CARBS_PER_100G = 30;
    private static final double CALORIE_FACTOR = 0.95;
    private static final Set<FoodPyramid> BLOCKED_GROUPS = Set.of(FoodPyramid.DULCES, FoodPyramid.ULTRAPROCESADOS);

    @Override
    public String getConditionName() { return "Diabetes tipo 2"; }

    @Override
    public List<String> getAliases() { return List.of("Type 2 diabetes", "Diabetes"); }

    @Override
    public double adjustCalories(double baseCalories) { return baseCalories * CALORIE_FACTOR; }

    @Override
    public boolean isFoodAllowed(Food food) {
        if (food == null || BLOCKED_GROUPS.contains(food.getGrupe())) {
            return false;
        }
        Double carbs = food.getCarbohydrates100g();
        return carbs == null || carbs <= MAX_CARBS_PER_100G;
    }

    @Override
    public IntensityLevel maxIntensity() { return IntensityLevel.MODERATE; }

    @Override
    public boolean requiresLowImpact() { return false; }

    @Override
    public String describeRestriction() {
        return "Sin dulces ni ultraprocesados, baja en carbohidratos e intensidad máxima moderada";
    }
}