/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;

/**
 *
 * @author irind
 */
public final class BMI {
    
    private BMI(){}
    
    public static double calculate(double weightKg, double heightCm){
    
        if(weightKg<=0 || heightCm<=0){
            throw new IllegalArgumentException("El peso y la altura deben ser positivos");
        }
        
        double heightM = heightCm/100;
        return (weightKg/(heightM*heightM));
    }
    
    public static BMIClasification classify(double bmi){
        return BMIClasification.fromValue(bmi);
    }
    
}
