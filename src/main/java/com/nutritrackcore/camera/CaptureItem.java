package com.nutritrackcore.camera;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila editable de la tabla de la cámara.
 * <p>
 * Las kcal son solo vista previa: las que se guardan las calcula {@code ConsumptionService}.
 */
public class CaptureItem {
    private Integer foodId;
    private String foodName;
    private final String label;
    private double grams;
    private final Double modelGrams;
    private Double confidence;
    private double kcal;
    private List<FoodOption> alternatives = new ArrayList<>();
    private String warning;

    /**
     * @param label      nombre que dio el modelo, o {@code null} si el usuario agregó la fila
     * @param modelGrams gramos que propuso el modelo, o {@code null}
     * @param confidence confianza del modelo, o {@code null} si el usuario agregó la fila
     */
    public CaptureItem(Integer foodId, String foodName, String label, double grams, Double modelGrams,
                       Double confidence) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.label = label;
        this.grams = grams;
        this.modelGrams = modelGrams;
        this.confidence = confidence;
    }

    public Integer getFoodId() { return foodId; }
    public void setFoodId(Integer foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    /** Nombre que dio el modelo (por ejemplo "white rice"). */
    public String getLabel() { return label; }

    public double getGrams() { return grams; }
    public void setGrams(double grams) { this.grams = grams; }

    /** Gramos que propuso el modelo (para comparar con lo que editó el usuario). */
    public Double getModelGrams() { return modelGrams; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public double getKcal() { return kcal; }
    public void setKcal(double kcal) { this.kcal = kcal; }

    public List<FoodOption> getAlternatives() { return alternatives; }
    public void setAlternatives(List<FoodOption> alternatives) {
        this.alternatives = alternatives == null ? new ArrayList<>() : new ArrayList<>(alternatives);
    }

    /** Aviso para el usuario (alergia o alimento excluido); {@code null} si no hay. */
    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }

    /** Indica si la fila ya tiene un alimento del catálogo. */
    public boolean hasFood() {
        return foodId != null;
    }
}