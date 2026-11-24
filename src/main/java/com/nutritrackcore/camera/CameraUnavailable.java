package com.nutritrackcore.camera;

import java.util.List;

/**
 * Objeto Nulo: se usa cuando no hay servicio de visión. Siempre avisa que la
 * cámara no está disponible, para que la pantalla ofrezca el registro manual.
 */
public final class CameraUnavailable implements ImageSource, PlateAnalyzer, PortionEstimator {
    private static final String MESSAGE = "El reconocimiento no está disponible. Registra los alimentos a mano.";

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public CapturedImage capture() throws CameraException {
        throw new CameraException(CameraException.Reason.UNAVAILABLE, MESSAGE);
    }

    @Override
    public List<DetectedPortion> analyze(CapturedImage image) throws CameraException {
        throw new CameraException(CameraException.Reason.UNAVAILABLE, MESSAGE);
    }

    @Override
    public PortionEstimate estimate(CapturedImage image, int foodId) throws CameraException {
        throw new CameraException(CameraException.Reason.UNAVAILABLE, MESSAGE);
    }
}