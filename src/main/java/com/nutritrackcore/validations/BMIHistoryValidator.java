/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.validations;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.BMIHistory;
/**
 *
 * @author irind
 */
public class BMIHistoryValidator implements Validator<BMIHistory>{
    @Override
    public void validate(BMIHistory history) throws ValidationException{
        
        if(history == null){
            throw new ValidationException("El historial no puede ser nulo");
        }
        
        if(history.getIdUser()<= 0){
            throw new ValidationException("El historial debe tener un usuario válido");
        }
        
        ValidationRules.positive(history.getWeightKg(), "weight_kg");
        ValidationRules.positive(history.getHeightCm(), "height_cm");
        
        if(history.getRegisterDate() == null){
            throw new ValidationException("La fecha de registro es obligatoria");
        }
    }   
    
}
