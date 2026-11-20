/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
/**
 *
 * @author irind
 */
public class ExerciseByReps extends ExerciseRoutineExercise{
    private int reps;
    public ExerciseByReps(){super();}
    
    public ExerciseByReps(int routineId, int exerciseId, int sets, int reps){
        super(routineId, exerciseId, sets);
        this.reps = reps;
    }
    
    public int getReps(){
        return reps;
    }
    
    public void setReps(int reps){
        this.reps = reps;
    }
}
