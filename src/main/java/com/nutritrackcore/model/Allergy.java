/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
/**
 *
 * @author irind
 */
public class Allergy {
    private int allergyId;
    private String name;
    private String description;

    public Allergy() {}

    public Allergy(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getAllergyId() { return allergyId; }
    public void setAllergyId(int allergyId) { this.allergyId = allergyId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
}
