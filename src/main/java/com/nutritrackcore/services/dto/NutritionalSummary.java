package com.nutritrackcore.services.dto;

/**
 * Aporte nutricional total (calorías y macronutrientes) de uno o varios
 * consumos.
 * <p>
 * Es inmutable: sumar dos resúmenes con {@link #add(NutritionalSummary)}
 * produce un objeto nuevo. Lo usan {@code ConsumptionService} (resumen del día),
 * {@code QueryService} (promedios y tendencias) y
 * {@code NutritionCalculator} (aporte de una porción).
 */
public class NutritionalSummary {

    /** Calorías totales (kcal). */
    private final double calories;

    /** Proteínas totales (g). */
    private final double proteins;

    /** Carbohidratos totales (g). */
    private final double carbohydrates;

    /** Grasas totales (g). */
    private final double fats;

    /**
     * Crea un resumen nutricional.
     *
     * @param calories      calorías en kcal
     * @param proteins      proteínas en gramos
     * @param carbohydrates carbohidratos en gramos
     * @param fats          grasas en gramos
     */
    public NutritionalSummary(double calories, double proteins, double carbohydrates, double fats) {
        this.calories = calories;
        this.proteins = proteins;
        this.carbohydrates = carbohydrates;
        this.fats = fats;
    }

    /**
     * Crea un resumen con todos los valores en cero. Es el punto de partida
     * para acumular sumas.
     *
     * @return resumen vacío
     */
    public static NutritionalSummary empty() {
        return new NutritionalSummary(0, 0, 0, 0);
    }

    /**
     * Suma este resumen con otro y devuelve el resultado como un objeto nuevo;
     * ninguno de los dos originales se modifica.
     *
     * @param other resumen a sumar
     * @return nuevo resumen con la suma campo a campo
     * @throws IllegalArgumentException si {@code other} es nulo
     */
    public NutritionalSummary add(NutritionalSummary other) {
        if (other == null) {
            throw new IllegalArgumentException("El dato 'resumen nutricional' no puede ser nulo.");
        }
        return new NutritionalSummary(calories + other.calories, proteins + other.proteins,
                carbohydrates + other.carbohydrates, fats + other.fats);
    }

    /**
     * Devuelve las calorías totales.
     *
     * @return calorías en kcal
     */
    public double getCalories() {
        return calories;
    }

    /**
     * Devuelve las proteínas totales.
     *
     * @return proteínas en gramos
     */
    public double getProteins() {
        return proteins;
    }

    /**
     * Devuelve los carbohidratos totales.
     *
     * @return carbohidratos en gramos
     */
    public double getCarbohydrates() {
        return carbohydrates;
    }

    /**
     * Devuelve las grasas totales.
     *
     * @return grasas en gramos
     */
    public double getFats() {
        return fats;
    }

    /**
     * Representación de texto para depuración.
     *
     * @return texto con los cuatro valores
     */
    @Override
    public String toString() {
        return "NutritionalSummary{calories=" + calories + ", proteins=" + proteins
                + ", carbohydrates=" + carbohydrates + ", fats=" + fats + "}";
    }
}