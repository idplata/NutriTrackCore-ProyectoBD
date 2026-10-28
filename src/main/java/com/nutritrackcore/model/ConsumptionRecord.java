package com.nutritrackcore.model;

import java.time.LocalDateTime;

public class ConsumptionRecord {
    private int recordId;
    private int userId;
    private int foodId;
    private String imageRef;
    private Double estimationLevel;
    private double estimatedGrams;
    private LocalDateTime dateTime;

    public ConsumptionRecord() {}

    public ConsumptionRecord(int userId, int foodId, double estimatedGrams, LocalDateTime dateTime) {
        this.userId = userId;
        this.foodId = foodId;
        this.estimatedGrams = estimatedGrams;
        this.dateTime = dateTime;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public String getImageRef() { return imageRef; }
    public void setImageRef(String imageRef) { this.imageRef = imageRef; }

    public Double getEstimationLevel() { return estimationLevel; }
    public void setEstimationLevel(Double estimationLevel) { this.estimationLevel = estimationLevel; }

    public double getEstimatedGrams() { return estimatedGrams; }
    public void setEstimatedGrams(double estimatedGrams) { this.estimatedGrams = estimatedGrams; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public boolean isEstimatedByImage() {
        return imageRef != null && !imageRef.isEmpty();
    }
}