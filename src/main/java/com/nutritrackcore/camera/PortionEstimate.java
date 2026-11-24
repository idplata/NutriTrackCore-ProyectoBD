package com.nutritrackcore.camera;

/**
 * Gramos estimados para un alimento que eligió el usuario (plan B).
 *
 * @param grams      gramos probables
 * @param confidence confianza de 0 a 1
 */
public record PortionEstimate(double grams, double confidence) {
}