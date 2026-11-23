package com.nutritrackcore.ui;

import java.util.Locale;

/**
 * Textos amigables para los valores internos que guardan los servicios
 * (por ejemplo "muy_activo" → "Muy activo").
 */
final class Labels {

    private Labels() {
    }

    /** Nivel de actividad. */
    static String activity(String value) {
        return switch (lower(value)) {
            case "sedentario" -> "Sedentario";
            case "ligero" -> "Ligero";
            case "moderado" -> "Moderado";
            case "muy_activo" -> "Muy activo";
            default -> "—";
        };
    }

    /** Objetivo de peso. */
    static String goal(String value) {
        return switch (lower(value)) {
            case "bajar" -> "Bajar de peso";
            case "mantener" -> "Mantener";
            case "subir" -> "Subir de peso";
            default -> "—";
        };
    }

    /** Lugar de entrenamiento. */
    static String location(String value) {
        return switch (lower(value)) {
            case "casa" -> "En casa";
            case "gimnasio" -> "Gimnasio";
            default -> "—";
        };
    }

    /** Tipo de dieta. */
    static String diet(String value) {
        return switch (lower(value)) {
            case "omnivora" -> "Omnívora";
            case "vegetariana" -> "Vegetariana";
            case "vegana" -> "Vegana";
            case "celiaca" -> "Celíaca";
            case "flexitariana" -> "Flexitariana";
            default -> "—";
        };
    }

    /** Grupo muscular de un ejercicio. */
    static String muscle(String value) {
        return switch (lower(value)) {
            case "pecho" -> "Pecho";
            case "espalda" -> "Espalda";
            case "hombros" -> "Hombros";
            case "brazos" -> "Brazos";
            case "piernas" -> "Piernas";
            case "gluteos" -> "Glúteos";
            case "core" -> "Core";
            case "cuerpo_completo" -> "Cuerpo completo";
            case "cardio" -> "Cardio";
            default -> value == null ? "—" : value;
        };
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
