package com.nutritrackcore.services.dto;

/**
 * Resultado de una consulta de "alimentos más consumidos": cuántas veces se
 * registró un alimento y cuántos gramos suman esos registros.
 * <p>
 * Objeto inmutable que {@code QueryService.mostFrequentFoods} entrega a la
 * interfaz para llenar una tabla.
 */
public class FoodFrequency {

    /** Identificador del alimento. */
    private final int foodId;

    /** Nombre del alimento, para mostrarlo sin consultar el catálogo otra vez. */
    private final String foodName;

    /** Número de registros de consumo que incluyen este alimento. */
    private final int times;

    /** Suma de los gramos estimados de todos esos registros. */
    private final double totalGrams;

    /**
     * Crea un resultado de frecuencia.
     *
     * @param foodId     identificador del alimento
     * @param foodName   nombre del alimento
     * @param times      cantidad de registros
     * @param totalGrams gramos totales consumidos
     */
    public FoodFrequency(int foodId, String foodName, int times, double totalGrams) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.times = times;
        this.totalGrams = totalGrams;
    }

    /**
     * Devuelve el identificador del alimento.
     *
     * @return id del alimento
     */
    public int getFoodId() {
        return foodId;
    }

    /**
     * Devuelve el nombre del alimento.
     *
     * @return nombre del alimento
     */
    public String getFoodName() {
        return foodName;
    }

    /**
     * Devuelve cuántas veces se registró el alimento.
     *
     * @return número de registros
     */
    public int getTimes() {
        return times;
    }

    /**
     * Devuelve los gramos totales consumidos del alimento.
     *
     * @return suma de gramos estimados
     */
    public double getTotalGrams() {
        return totalGrams;
    }

    /**
     * Representación de texto para depuración.
     *
     * @return texto con los cuatro datos
     */
    @Override
    public String toString() {
        return "FoodFrequency{foodId=" + foodId + ", foodName='" + foodName + "', times=" + times
                + ", totalGrams=" + totalGrams + "}";
    }
}