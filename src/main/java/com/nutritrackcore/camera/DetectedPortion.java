package com.nutritrackcore.camera;

import java.util.List;

/**
 * Un alimento que reconoció el servicio de visión.
 *
 * @param foodId         id de {@code Alimento}; {@code null} si no lo reconoció
 * @param label          nombre que dio el modelo (solo informativo)
 * @param grams          gramos probables (mayor que 0)
 * @param confidence     confianza de 0 a 1
 * @param alternativeIds hasta 3 ids de alimentos parecidos
 */
public record DetectedPortion(Integer foodId, String label, double grams, double confidence,
                              List<Integer> alternativeIds) {

    public DetectedPortion {
        alternativeIds = alternativeIds == null ? List.of() : List.copyOf(alternativeIds);
    }
}