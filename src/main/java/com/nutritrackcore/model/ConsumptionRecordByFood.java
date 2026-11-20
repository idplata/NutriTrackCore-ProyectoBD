/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;

/**
 *
 * @author irind
 */
public class ConsumptionRecordByFood {
    private int recordId;
    private int foodId;
    private int consumptionRecordByFoodId;
    private double estimatedGrams;
    
    public ConsumptionRecordByFood( int recordId, int foodId, int consumptionRecordByFoodId, int estimatedGrams){
        
        this.consumptionRecordByFoodId = consumptionRecordByFoodId;
        this.estimatedGrams = estimatedGrams;
        this.foodId = foodId;
        this.recordId = recordId;
        
    }
    
    public int getconsumptionRecordByFoodId(){
        return consumptionRecordByFoodId;
    }
    public void setConsumptionRecordByFoodId( int consumptionRecordByFoodId){
        this.consumptionRecordByFoodId = consumptionRecordByFoodId;
    }
    
    public int getFoodId(){
        return foodId;
    }
    public void setFoodId(int foodId){
        this.foodId = foodId;
    }
    
    public int getRecordId(){
        return recordId;
    }
    public void setRecordId(int recordId){
        this.recordId = recordId;
    }
    
    public double getEstimatedGrams(){
        return estimatedGrams;
    }
    public void setEstimatedGrams(double estimatedGrams){
        this.estimatedGrams = estimatedGrams;
    }
    
}
