package com.nutritrackcore.services;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.Food.FoodPyramid;
import com.nutritrackcore.model.IntensityLevel;
import java.util.Collections;
import java.util.List;

/**
 * Cambios que una condición médica aplica a la dieta y a la rutina.
 */
public interface ConditionRule {

    String getConditionName();

    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /** Ajusta las kcal diarias (por ejemplo, un déficit). */
    double adjustCalories(double baseCalories);

    /** Indica si el alimento se puede usar en la dieta. */
    boolean isFoodAllowed(Food food);

    /** Factor que multiplica los gramos de un grupo de la pirámide (1.0 = sin límite). */
    default double groupFactor(FoodPyramid group) {
        return 1.0;
    }

    IntensityLevel maxIntensity();

    boolean requiresLowImpact();

    /** Series fijas para los ejercicios por repeticiones; {@code null} si no hay. */
    default Integer fixedSets() {
        return null;
    }

    /** Tope de repeticiones; {@code null} si no hay. */
    default Integer maxRepetitions() {
        return null;
    }

    String describeRestriction();
}