/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Period;

/**
 *
 * @author irind
 */
public class User {
    
    private int idUser;
    private String name;
    private String email;
    private String passwordHash;
    private double heightCm;
    private double weightKg;
    private Sex sex;
    private LocalDateTime registerDate;
    private LocalDate birthDate;
    
    public User(){}
    public User(String name, String email, String passwordHash, double weightKg, double heightCm, LocalDate birthDate, Sex sex){
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.birthDate = birthDate;
        this.sex = sex;
        this.registerDate = LocalDateTime.now();
    
    }
    
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
          
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    
    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    
    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex; }
    
    public LocalDateTime getRegisterDate() { return registerDate; }
    public void setRegisterDate(LocalDateTime registerDate) { this.registerDate = registerDate; }
    public int getAge(){
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
    public double calculateBMI(){
        return BMI.calculate(weightKg, heightCm);
    }
    
    public BMIClasification classifyBmi(){
    
        return BMI.classify(calculateBMI());
    }
}
