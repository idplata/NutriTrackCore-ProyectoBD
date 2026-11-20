/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrack.model;
import java.time.LocalDate;
/**
 *
 * @author irind
 */
public class UserProfile {
    
    private int userId;
    private String activityLevel;
    private String exerciseExperience;
    private String weightGoal;
    private int trainingDaysPerWeek;
    private int minutesPerSession;
    private String trainingLocation;
    private String dietType;
    private int mealsPerDay;
    private LocalDate updatedAt;

    public UserProfile() {}

    public UserProfile(int userId, String activityLevel, String exerciseExperience, String weightGoal,
                       int trainingDaysPerWeek, int minutesPerSession, String trainingLocation,
                       String dietType, int mealsPerDay, String budget, LocalDate updatedAt) {
        this.userId = userId;
        this.activityLevel = activityLevel;
        this.exerciseExperience = exerciseExperience;
        this.weightGoal = weightGoal;
        this.trainingDaysPerWeek = trainingDaysPerWeek;
        this.minutesPerSession = minutesPerSession;
        this.trainingLocation = trainingLocation;
        this.dietType = dietType;
        this.mealsPerDay = mealsPerDay;
        this.updatedAt = updatedAt;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    
    public String getExerciseExperience() { return exerciseExperience; }
    public void setExerciseExperience(String exerciseExperience) { this.exerciseExperience = exerciseExperience; }
    
    public String getWeightGoal() { return weightGoal; }
    public void setWeightGoal(String weightGoal) { this.weightGoal = weightGoal; }
    
    public int getTrainingDaysPerWeek() { return trainingDaysPerWeek; }
    public void setTrainingDaysPerWeek(int trainingDaysPerWeek) { this.trainingDaysPerWeek = trainingDaysPerWeek; }
    
    public int getMinutesPerSession() { return minutesPerSession; }
    public void setMinutesPerSession(int minutesPerSession) { this.minutesPerSession = minutesPerSession; }
    
    public String getTrainingLocation() { return trainingLocation; }
    public void setTrainingLocation(String trainingLocation) { this.trainingLocation = trainingLocation; }
    
    public String getDietType() { return dietType; }
    public void setDietType(String dietType) { this.dietType = dietType; }
    
    public int getMealsPerDay() { return mealsPerDay; }
    public void setMealsPerDay(int mealsPerDay) { this.mealsPerDay = mealsPerDay; }
        
    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }
    
}
