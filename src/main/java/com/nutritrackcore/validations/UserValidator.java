/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.validations;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.User;
/**
 *
 * @author irind
 */
public class UserValidator implements Validator<User>{
    @Override
    public void validate(User user) throws ValidationException{
        if(user == null){
            throw new ValidationException("El usuario no puede ser nulo");
           
        }
        
        ValidationRules.notEmpty(user.getName(), "name");
        ValidationRules.noSeparator(user.getName(), "name");
        ValidationRules.validEmail(user.getEmail());        
        ValidationRules.noSeparator(user.getEmail(), "email");
        ValidationRules.positive(user.getWeightKg(), "weight_kg");
        ValidationRules.positive(user.getHeightCm(), "height_cm");
        ValidationRules.positive(user.getAge(), "age");
        
        if(user.getBirthDate() == null){
            throw new ValidationException("La fecha de nacimiento no puede ser nula");
        }
        
        ValidationRules.inRange(user.getAge(), 0, 120, "age");
    
        if(user.getSex() == null){
            throw new ValidationException("El sexo es obligatorio");
        }
    }
    
}
