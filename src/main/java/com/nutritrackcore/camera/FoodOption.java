package com.nutritrackcore.camera;

/**
 * Alimento para mostrar en listas y combos (id + nombre).
 *
 * @param foodId id de {@code Alimento}
 * @param name   nombre del catálogo
 */
public record FoodOption(int foodId, String name) {

    @Override
    public String toString() {
        return name;
    }
}