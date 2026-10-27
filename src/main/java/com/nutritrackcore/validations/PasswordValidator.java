/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.validations;
import com.nutritrackcore.exceptions.ValidationException;
/**
 *
 * @author irind
 */
public class PasswordValidator implements Validator<String>{
    
    @Override
    public void validate(String password) throws ValidationException{
        
        if(password == null || password.length()<8){
            throw new ValidationException("La contraseña debe tener al menos 8 caracteres.");
        }
        
        if(!password.matches(".*[A-Za-z].*")){
            throw new ValidationException("La contraseña debe contener al menos una letra");
        }
        
        if(!password.matches(".*\\d.*")){
            throw new ValidationException("La contraseña debe contener al menos un número");
        }
    }
}
