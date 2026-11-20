package com.nutritrackcore.model;
import com.nutritrackcore.model.Food.FoodPyramid;
import java.util.EnumSet;
import java.util.Set;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DietPlan {
    private int planId;
    private int userId;
    private LocalDate generationDate;
    private double targetCalories;
    private boolean active;
    private List<DietPlanFood> lines;
    

    public DietPlan() {
        this.lines = new ArrayList<>();
    }
    
    public enum DietType{
         VEGANA(
            "Sin productos de origen animal",
            EnumSet.of(
               Food.FoodPyramid.FRUTAS_Y_VERDURAS,
               Food.FoodPyramid.CARBOHIDRATOS,
               Food.FoodPyramid.GRASA_SALUDABLES,
               Food.FoodPyramid.ARROZ       
            )
        ),
        VEGETARIANA(
            "Sin carnes ni pescados, permite lácteos y huevos",
            EnumSet.of(
                FoodPyramid.FRUTAS_Y_VERDURAS,
                FoodPyramid.CARBOHIDRATOS,
                FoodPyramid.GRASA_SALUDABLES,
                FoodPyramid.PROTEINAS_Y_LACTEOS
            )
        ),
        OMNIVORA(
            "Incluye todos los grupos de alimentos",
            EnumSet.allOf(FoodPyramid.class)
        ),
        CELIACA(
            "Libre de gluten",
            EnumSet.allOf(FoodPyramid.class)
        ),
        FLEXITARIANA(
            "Base vegetariana con consumo ocasional de carnes o pescados",
            EnumSet.allOf(FoodPyramid.class)
        );
        
        private final String description;
        private final Set<FoodPyramid> allowedGroups;
        
        DietType(String description, Set<FoodPyramid> allowedGroups) {
            this.description = description;
            this.allowedGroups = allowedGroups;
        }
        
        public String getDescription(){
            return description;
        }
        
        public Set<FoodPyramid> getAllowedGroups(){
            return allowedGroups;
        }
    }
    
    public DietPlan(int userId, LocalDate generationDate, double targetCalories) {
        this.userId = userId;
        this.generationDate = generationDate;
        this.targetCalories = targetCalories;
        this.active = true;
        this.lines = new ArrayList<>();
    }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getGenerationDate() { return generationDate; }
    public void setGenerationDate(LocalDate generationDate) { this.generationDate = generationDate; }

    public double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
   
    public List<DietPlanFood> getLines() {
        return Collections.unmodifiableList(lines);
    }
    
    public void setLines(List<DietPlanFood> lines) {
        this.lines = lines;
    }
}