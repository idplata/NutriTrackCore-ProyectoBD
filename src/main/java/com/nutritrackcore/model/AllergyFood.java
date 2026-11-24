/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
/**
 *
 * @author irind
 */
public class AllergyFood {
     private int allergyId;
    private int foodId;

    public AllergyFood() {
    }

    public AllergyFood(int allergyId, int foodId) {
        this.allergyId = allergyId;
        this.foodId = foodId;
    }

    public int getAllergyId() { return allergyId; }
    public void setAllergyId(int allergyId) { this.allergyId = allergyId; }
    
    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }
    
}
