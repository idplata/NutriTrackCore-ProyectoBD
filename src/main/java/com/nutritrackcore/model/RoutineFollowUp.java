/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
import java.time.LocalDate;
/**
 *
 * @author irind
 */
public class RoutineFollowUp {
    
    private int followUpId;
    private int routineId;
    private LocalDate sessionDate;
    private int durationMinutes;
    private boolean completed;
    private String observations;

    public RoutineFollowUp() {
    }

    public RoutineFollowUp(int routineId, LocalDate sessionDate, int durationMinutes,
                           boolean completed, String observations) {
        this.routineId = routineId;
        this.sessionDate = sessionDate;
        this.durationMinutes = durationMinutes;
        this.completed = completed;
        this.observations = observations;
    }

    public int getFollowUpId() { return followUpId; }
    public void setFollowUpId(int followUpId) { this.followUpId = followUpId; }
    
    public int getRoutineId() { return routineId; }
    public void setRoutineId(int routineId) { this.routineId = routineId; }
    
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    
}
