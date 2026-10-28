/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.exceptions;

/**
 *
 * @author irind
 */
public class PersistenceExceptions extends Exception {
    public PersistenceExceptions(String message){ super(message);}
    
    public PersistenceExceptions(String message, Throwable cause){
        super(message,cause);
    }
    
}
