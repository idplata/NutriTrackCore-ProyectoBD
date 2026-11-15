/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
/**
 *
 * @author irind
 */
public class UserAllergy {
    
    private int userId;
    private int allergyId;
    private String severity;
    private String observations;

    public UserAllergy() {
    }

    public UserAllergy(int userId, int allergyId, String severity, String observations) {
        this.userId = userId;
        this.allergyId = allergyId;
        this.severity = severity;
        this.observations = observations;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getAllergyId() { return allergyId; }
    public void setAllergyId(int allergyId) { this.allergyId = allergyId; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    
}
