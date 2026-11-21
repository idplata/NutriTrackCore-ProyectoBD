package com.nutritrackcore.services.dto;

import com.nutritrackcore.services.ParameterChecks;

/**
 * Resultado de calcular el IMC: el valor numérico y su clasificación.
 * <p>
 * El IMC <b>nunca se guarda</b> en ningún archivo (decisión del diccionario de
 * datos): siempre se calcula a partir del peso y la altura con
 * {@link #calculate(double, double)}.
 */
public class BmiResult {

    /** Valor del IMC en kg/m². */
    private final double value;

    /** Categoría que corresponde al valor. */
    private final BmiCategory category;

    /**
     * Crea un resultado. Es privado: se obtiene con {@link #calculate}.
     *
     * @param value    valor del IMC
     * @param category categoría correspondiente
     */
    private BmiResult(double value, BmiCategory category) {
        this.value = value;
        this.category = category;
    }

    /**
     * Calcula el IMC con la fórmula {@code peso / (altura en metros)²} y lo
     * clasifica.
     * <p>
     * Pasos: (1) valida que peso y altura estén dentro de los límites del
     * sistema; (2) convierte la altura de centímetros a metros; (3) divide el
     * peso entre la altura al cuadrado; (4) obtiene la categoría con
     * {@link BmiCategory#fromValue(double)}.
     *
     * @param weightKg peso en kilogramos
     * @param heightCm altura en centímetros
     * @return resultado con el valor y su categoría
     * @throws IllegalArgumentException si el peso o la altura están fuera de rango
     */
    public static BmiResult calculate(double weightKg, double heightCm) {
        ParameterChecks.requireMeasurements(weightKg, heightCm);
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        return new BmiResult(bmi, BmiCategory.fromValue(bmi));
    }

    /**
     * Devuelve el valor del IMC.
     *
     * @return IMC en kg/m²
     */
    public double getValue() {
        return value;
    }

    /**
     * Devuelve la clasificación del IMC.
     *
     * @return categoría (bajo peso, normal, sobrepeso u obesidad)
     */
    public BmiCategory getCategory() {
        return category;
    }

    /**
     * Representación de texto para depuración.
     *
     * @return texto con el valor y la categoría
     */
    @Override
    public String toString() {
        return String.format("BmiResult{value=%.2f, category=%s}", value, category);
    }
}