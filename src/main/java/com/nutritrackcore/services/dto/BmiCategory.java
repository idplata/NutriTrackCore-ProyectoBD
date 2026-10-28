package com.nutritrackcore.services.dto;

/**
 * Clasificación del índice de masa corporal (IMC) según los rangos de la OMS
 * para personas adultas.
 * <p>
 * Cada constante guarda el texto en español que la interfaz puede mostrar
 * directamente.
 */
public enum BmiCategory {

    /** IMC menor que 18,5. */
    UNDERWEIGHT("Bajo peso"),

    /** IMC desde 18,5 hasta 24,9. */
    NORMAL("Peso normal"),

    /** IMC desde 25 hasta 29,9. */
    OVERWEIGHT("Sobrepeso"),

    /** IMC de 30 o más. */
    OBESITY("Obesidad");

    /** Nombre legible en español. */
    private final String displayName;

    /**
     * Crea la constante con su nombre legible.
     *
     * @param displayName nombre en español
     */
    BmiCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Devuelve el nombre legible de la categoría.
     *
     * @return nombre en español, por ejemplo {@code "Sobrepeso"}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determina la categoría que corresponde a un valor de IMC.
     * <p>
     * Compara el valor contra los umbrales 18,5, 25 y 30, en ese orden.
     *
     * @param bmi valor del IMC (kg/m²)
     * @return categoría correspondiente
     * @throws IllegalArgumentException si el valor no es un número finito mayor que cero
     */
    public static BmiCategory fromValue(double bmi) {
        if (Double.isNaN(bmi) || Double.isInfinite(bmi) || bmi <= 0) {
            throw new IllegalArgumentException("El valor del IMC debe ser un número mayor que cero.");
        }
        if (bmi < 18.5) {
            return UNDERWEIGHT;
        }
        if (bmi < 25) {
            return NORMAL;
        }
        if (bmi < 30) {
            return OVERWEIGHT;
        }
        return OBESITY;
    }
}