package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.IntensityLevel;
import java.util.Collections;
import java.util.List;

/**
 * Reglas de negocio asociadas a una condición médica: cómo cambia la dieta y el
 * ejercicio de quien la padece.
 * <p>
 * Cada condición tiene su propia clase que implementa esta interfaz (patrón
 * Estrategia). Para soportar una condición nueva solo se crea una clase nueva y
 * se registra en {@link ConditionRuleCatalog}; los servicios de planes y
 * rutinas no se modifican.
 * <p>
 * Los valores numéricos de las reglas incluidas son de demostración académica y
 * deben ser validados por un profesional de la salud antes de usarse con
 * personas reales.
 */
public interface ConditionRule {

    /**
     * Nombre principal de la condición a la que aplica la regla. Debe coincidir
     * (sin distinguir mayúsculas ni tildes) con el nombre guardado en el
     * catálogo de condiciones médicas.
     *
     * @return nombre de la condición, por ejemplo {@code "Diabetes tipo 2"}
     */
    String getConditionName();

    /**
     * Otros nombres con los que la condición puede aparecer en el catálogo (por
     * ejemplo, su versión en inglés). Por defecto no hay ninguno.
     *
     * @return lista de nombres alternativos; nunca {@code null}
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Ajusta la meta calórica diaria según la condición.
     *
     * @param baseCalories calorías calculadas antes del ajuste (kcal)
     * @return calorías ajustadas (kcal)
     * @throws IllegalArgumentException si el valor no es un número válido mayor que cero
     */
    double adjustCalories(double baseCalories);

    /**
     * Indica si un alimento es adecuado para quien tiene la condición.
     *
     * @param food alimento a evaluar
     * @return {@code true} si puede incluirse en un plan de dieta
     * @throws IllegalArgumentException si el alimento es nulo
     */
    boolean isFoodAllowed(Food food);

    /**
     * Intensidad máxima de ejercicio recomendada para la condición.
     *
     * @return nivel máximo permitido
     */
    IntensityLevel maxIntensity();

    /**
     * Indica si la condición exige limitar la rutina a ejercicios de bajo
     * impacto.
     *
     * @return {@code true} si solo se deben recomendar ejercicios de bajo impacto
     */
    boolean requiresLowImpact();

    /**
     * Describe en español la restricción aplicada. Se guarda junto a la rutina
     * ({@code appliedRestriction}) para que el usuario sepa por qué se limitó.
     *
     * @return texto legible, por ejemplo {@code "Solo ejercicios de bajo impacto"}
     */
    String describeRestriction();
}