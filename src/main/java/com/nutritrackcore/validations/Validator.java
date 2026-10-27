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
public interface Validator<T> {
    void validate(T object) throws ValidationException;
    
}
