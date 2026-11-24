/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;
import java.time.Duration;

/**
 *
 * @author irind
 */
public class ExerciseByTime extends ExerciseRoutineExercise {
    private Duration duration;
    
    public ExerciseByTime(){
        super();
    }
    public ExerciseByTime(int routineId, int exerciseId, int sets, Duration duration ){
            super(routineId, exerciseId, sets);
            this.duration = duration;
        }
    
    public Duration getDuration(){
        return duration;
    }
    public void setDuration(Duration duration){ this.duration = duration;}
}
