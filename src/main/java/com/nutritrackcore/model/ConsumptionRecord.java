package com.nutritrackcore.model;

import java.time.LocalDateTime;

public class ConsumptionRecord {
    private int recordId;
    private int userId;
    private Double estimationLevel;
    private double estimatedCalories;
    private LocalDateTime dateTime;

    public ConsumptionRecord() {}

    public ConsumptionRecord(int userId, double estimatedCalories, LocalDateTime dateTime) {
        this.userId = userId;
        this.estimatedCalories = estimatedCalories;
        this.dateTime = dateTime;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Double getEstimationLevel() { return estimationLevel; }
    public void setEstimationLevel(Double estimationLevel) { this.estimationLevel = estimationLevel; }

    public double getEstimatedGrams() { return estimatedCalories; }
    public void setEstimatedGrams(double estimatedGrams) { this.estimatedCalories = estimatedCalories; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

}