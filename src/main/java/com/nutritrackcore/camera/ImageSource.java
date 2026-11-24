package com.nutritrackcore.camera;

/**
 * De dónde sale la foto: el celular o un archivo del disco.
 */
public interface ImageSource {

    /**
     * Obtiene la foto (puede esperar a que llegue).
     *
     * @throws CameraException si no llega, no sirve o se cancela
     */
    CapturedImage capture() throws CameraException;
}