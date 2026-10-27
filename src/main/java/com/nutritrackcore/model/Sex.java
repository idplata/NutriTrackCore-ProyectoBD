/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nutritrackcore.model;

/**
 *
 * @author irind
 */
public enum Sex {
    M("M"), F("F"),O("O");
    
    private final String code;
    
    Sex(String code){
        this.code = code;
    }
    public String getCode(){
        return code;
    }

    public static Sex fromCode(String code){
        for (Sex s: values()){
            if (s.code.equals(code)){return s;}
        }
        
       throw new IllegalArgumentException("Codigo de sexo inválido: " + code);
    }
}
