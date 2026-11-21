/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
/**
 *
 * @author irind
 */
public class UserExcludedFood {
    private int userId;
    private int foodId;

    public UserExcludedFood() {
    }

    public UserExcludedFood(int userId, int foodId) {
        this.userId = userId;
        this.foodId = foodId;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }
    
}
