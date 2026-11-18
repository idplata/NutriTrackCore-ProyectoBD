/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
import java.math.BigDecimal;
/**
 *
 * @author irind
 */
public class MedicalConditionRule {
    private int ruleId;
    private int conditionId;
    private String ruleType;
    private String nutrient;
    private BigDecimal limitValue;
    private String description;

    public MedicalConditionRule() {
    }

    public MedicalConditionRule(int conditionId, String ruleType, String nutrient,
                                BigDecimal limitValue, String description) {
        this.conditionId = conditionId;
        this.ruleType = ruleType;
        this.nutrient = nutrient;
        this.limitValue = limitValue;
        this.description = description;
    }

    public int getRuleId() { return ruleId; }
    public void setRuleId(int ruleId) { this.ruleId = ruleId; }
    
    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }
    
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    
    public String getNutrient() { return nutrient; }
    public void setNutrient(String nutrient) { this.nutrient = nutrient; }
    
    public BigDecimal getLimitValue() { return limitValue; }
    public void setLimitValue(BigDecimal limitValue) { this.limitValue = limitValue; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
}
