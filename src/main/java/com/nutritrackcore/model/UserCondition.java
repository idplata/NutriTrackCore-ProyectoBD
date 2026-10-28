package com.nutritrackcore.model;

import java.time.LocalDate;

public class UserCondition {
    private int userId;
    private int conditionId;
    private LocalDate diagnosisDate;
    private String observations;

    public UserCondition() {}

    public UserCondition(int userId, int conditionId, LocalDate diagnosisDate, String observations) {
        this.userId = userId;
        this.conditionId = conditionId;
        this.diagnosisDate = diagnosisDate;
        this.observations = observations;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }

    public LocalDate getDiagnosisDate() { return diagnosisDate; }
    public void setDiagnosisDate(LocalDate diagnosisDate) { this.diagnosisDate = diagnosisDate; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
