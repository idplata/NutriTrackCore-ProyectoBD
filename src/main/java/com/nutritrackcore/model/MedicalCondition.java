package com.nutritrackcore.model;

public class MedicalCondition {
    private int conditionId;
    private String name;
    private String description;
    private boolean active;

    public MedicalCondition() {}

    public MedicalCondition(String name, String description) {
        this.name = name;
        this.description = description;
        this.active = true;
    }

    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}