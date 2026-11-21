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
public class ValidationRules {
    private ValidationRules(){}
    public static void notEmpty(String value, String fieldName) throws ValidationException{
        if(value == null || value.trim().isEmpty()){
        
        throw new ValidationException("El campo " + fieldName + "no puede estar vacío");
        }
    
    }
    
    
    public static void noSeparator(String value, String fieldName) throws ValidationException{
        if(value != null && value.contains("|")){
            throw new ValidationException("El campo " + fieldName + " no puede contener '|'.");
        }
    
    }

    public static void positive(double value, String fieldName) throws ValidationException{
        if (value<=0){
            throw new ValidationException("El campo " + fieldName + " debe ser mayor que cero");
        }
    
    }
    
    public static void nonNegative(double value, String fieldName) throws ValidationException {
        if (value < 0) {
            throw new ValidationException("El campo '" + fieldName + "' no puede ser negativo.");
        }
    }

    public static void validEmail(String value) throws ValidationException {
        notEmpty(value, "email");
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ValidationException("El email no tiene formato válido.");
        }
    }

    public static void inRange(double value, double min, double max, String fieldName) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException("El campo '" + fieldName + "' debe estar entre " + min + " y " + max + ".");
        }
    }
    
}    

