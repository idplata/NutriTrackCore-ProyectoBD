/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Samue
 */
public class ConsumptionRecorByFood {
    private int idCoConsumptionRecorByFood;
    private int idRecord;
    private int idfood;
    private double estimatedgrams;

    public ConsumptionRecorByFood(int idCoConsumptionRecorByFood, int idRecord, int idfood, double estimatedgrams) {
        this.idCoConsumptionRecorByFood = idCoConsumptionRecorByFood;
        this.idRecord = idRecord;
        this.idfood = idfood;
        this.estimatedgrams = estimatedgrams;
    }

    public ConsumptionRecorByFood(int idRecord, int idfood, double estimatedgrams) {
        this.idRecord = idRecord;
        this.idfood = idfood;
        this.estimatedgrams = estimatedgrams;
    }
    
    
    

    public ConsumptionRecorByFood() {
    }

    public int getIdCoConsumptionRecorByFood() {
        return idCoConsumptionRecorByFood;
    }

    public void setIdCoConsumptionRecorByFood(int idCoConsumptionRecorByFood) {
        this.idCoConsumptionRecorByFood = idCoConsumptionRecorByFood;
    }

    public int getIdfood() {
        return idfood;
    }

    public void setIdfood(int idfood) {
        this.idfood = idfood;
    }

    public double getStimatedgrams() {
        return estimatedgrams;
    }

    public void setStimatedgrams(double stimatedgrams) {
        this.estimatedgrams = stimatedgrams;
    }
    
    
}             
