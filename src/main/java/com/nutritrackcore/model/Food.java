package com.nutritrackcore.model;

public class Food {
    private int foodId;
    private String name;
    private Double densityGCm3;
    private double calories100g;
    private Double proteins100g;
    private Double carbohydrates100g;
    private Double fats100g;
    private boolean active;
    private FoodPyramid grupe;

    public Food() {}
    public enum FoodPyramid{
    
        DULCES(1, "AZUCARES"),
        ULTRAPROCESADOS(2, "CARNES ROJAS, EMBUTIDOS Y GRASAS"),
        GRASA_SALUDABLES(3, "ACEITES VEGETALES, FRUTOS SECOS Y AGUACATE"),
        PROTEINAS_Y_LACTEOS(4, "CARNES BLANCAS, LACTEOS, PESCADO Y HUEVO"),
        FRUTAS_Y_VERDURAS(5, "FRUTAS, VERDURAS Y HORTALIZAS"),
        CARBOHIDRATOS(6, "CEREALES, PAN Y PASTA"),
        ARROZ(7,"ARROZ");
        
        
        private final int PyramidLevel;
        private String description;
        
        FoodPyramid( int PyramidLevel,String description){
            this.PyramidLevel = PyramidLevel;
            this.description = description;
        }
        
        public int getPyramidLevel(){
            return PyramidLevel;
        }
        
        public String getDescription(){
            return description;
        }
        
    }

    public Food(String name, double calories100g, Double proteins100g, Double carbohydrates100g, Double fats100g, FoodPyramid grupe) {
        this.name = name;
        this.calories100g = calories100g;
        this.proteins100g = proteins100g;
        this.carbohydrates100g = carbohydrates100g;
        this.fats100g = fats100g;
        this.active = true;
        this.grupe = grupe;
    }
    
    
    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDensityGCm3() { return densityGCm3; }
    public void setDensityGCm3(Double densityGCm3) { this.densityGCm3 = densityGCm3; }

    public double getCalories100g() { return calories100g; }
    public void setCalories100g(double calories100g) { this.calories100g = calories100g; }

    public Double getProteins100g() { return proteins100g; }
    public void setProteins100g(Double proteins100g) { this.proteins100g = proteins100g; }

    public Double getCarbohydrates100g() { return carbohydrates100g; }
    public void setCarbohydrates100g(Double carbohydrates100g) { this.carbohydrates100g = carbohydrates100g; }

    public Double getFats100g() { return fats100g; }
    public void setFats100g(Double fats100g) { this.fats100g = fats100g; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Grupo de la pirámide alimenticia (columna {@code grupo}). */
    public FoodPyramid getGrupe() { return grupe; }
    public void setGrupe(FoodPyramid grupe) { this.grupe = grupe; }

   }