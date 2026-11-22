package com.nutritrackcore.camera;

import com.nutritrackcore.exceptions.NutriTrackException;

/**
 * Error de la cámara: celular, red o servicio de visión.
 * <p>
 * Hereda de {@link NutriTrackException} para que la interfaz atrape todo con un solo
 * {@code catch}. El motivo permite mostrar el mensaje y la salida correctos.
 */
public class CameraException extends NutriTrackException {

    /** Motivo del error. */
    public enum Reason {
        /** El servicio de visión no responde: solo queda el registro manual. */
        UNAVAILABLE,
        /** Falló la red o se canceló la operación. */
        NETWORK,
        /** La foto o la respuesta no llegaron a tiempo. */
        TIMEOUT,
        /** La imagen no se puede leer, no es JPEG/PNG o es demasiado grande. */
        INVALID_IMAGE,
        /** El servicio respondió algo que no se puede usar. */
        ESTIMATION
    }

    private final Reason reason;

    public CameraException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public CameraException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}