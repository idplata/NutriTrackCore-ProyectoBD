/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
import java.time.LocalDateTime;
/**
 *
 * @author irind
 */
public class BMIHistory {
    private int idHistory;
    private int idUser;
    private double weightKg;
    private double heightCm;
    private LocalDateTime registerDate;
    
    public BMIHistory(){}
    
    public BMIHistory( int idUser, double weightKg, double heightCm, LocalDateTime registerDate){
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.idUser = idUser;
        this.registerDate = registerDate;
    }
    
    public int getIdHistory() { return idHistory; }
    public void setIdHistory(int idHistory) { this.idHistory = idHistory; }
    
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    
    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }
    
    public LocalDateTime getRegisterDate() { return registerDate; }
    public void setRegisterDate(LocalDateTime registerDate) { this.registerDate = registerDate; }
    
    public double getBMI(){
        return BMI.calculate(weightKg, heightCm);
    }
    
    public BMIClasification getClasification(){
        return BMI.classify(getBMI());
    }
    
}
