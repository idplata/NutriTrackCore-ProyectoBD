/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;

/**
 *
 * @author irind
 */
public enum BMIClasification {
    low_weight("Bajo peso", 0, 18.5),
    normal("Normal", 18.5,25.0),
    overweight("Sobrepeso",25,30),
    Obesity("Obesidad", 30, Double.MAX_VALUE);
    
    
    
    private final String label;
    private final double min;
    private final double max;
    
    BMIClasification(String label, double min, double max){
        this.label = label;
        this.min = min;
        this.max = max;
    
    }
    
    public String getLabel(){
        return label;
    }
    
    public static BMIClasification fromValue(double bmi){
    
        for(BMIClasification c: values()){
            if (bmi>=c.min && bmi<c.max){
            
                return c;
            }
            
        }
                
            return Obesity;
        }
    }
