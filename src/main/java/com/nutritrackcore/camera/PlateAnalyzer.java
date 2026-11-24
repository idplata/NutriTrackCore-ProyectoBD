package com.nutritrackcore.camera;

import java.util.List;

/**
 * Reconoce los alimentos de un plato.
 */
public interface PlateAnalyzer {

    /** Indica si el servicio de visión responde ({@code GET /health}). */
    boolean isAvailable();

    /**
     * Detecta todos los alimentos de la foto.
     *
     * @return alimentos reconocidos (puede estar vacía)
     * @throws CameraException si el servicio no responde o la respuesta no sirve
     */
    List<DetectedPortion> analyze(CapturedImage image) throws CameraException;
}