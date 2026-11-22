package com.nutritrackcore.camera;

/**
 * Estima los gramos de un alimento que eligió el usuario (plan B).
 */
public interface PortionEstimator {

    /**
     * @throws CameraException si el servicio no responde o la respuesta no sirve
     */
    PortionEstimate estimate(CapturedImage image, int foodId) throws CameraException;
}