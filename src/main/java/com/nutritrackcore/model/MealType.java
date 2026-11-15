/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author irind
 */
public class MealType {
    private int mealTypeId;
    private String name;
    private int dayOrder;

    public MealType() {
    }

    public MealType(String name, int dayOrder) {
        this.name = name;
        this.dayOrder = dayOrder;
    }

    public int getMealTypeId() { return mealTypeId; }
    public void setMealTypeId(int mealTypeId) { this.mealTypeId = mealTypeId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getDayOrder() { return dayOrder; }
    public void setDayOrder(int dayOrder) { this.dayOrder = dayOrder; }
}
