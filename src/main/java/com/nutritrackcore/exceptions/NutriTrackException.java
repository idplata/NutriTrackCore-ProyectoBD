/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.exceptions;

/**
 *
 * @author irind
 */
public class NutriTrackException extends Exception {
    
    public NutriTrackException(String message) {
        super(message);
    }

    public NutriTrackException(String message, Throwable cause) {
        super(message, cause);
    }
}
