package com.nutritrackcore.camera;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;

/**
 * Foto recibida (del celular o del disco). Vive solo en memoria: nunca se guarda.
 *
 * @param bytes      contenido del archivo
 * @param mimeType   {@code image/jpeg} o {@code image/png}
 * @param width      ancho en píxeles
 * @param height     alto en píxeles
 * @param capturedAt cuándo llegó
 */
public record CapturedImage(byte[] bytes, String mimeType, int width, int height, LocalDateTime capturedAt) {

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";

    /**
     * Valida los bytes (solo JPEG o PNG, tamaño máximo, imagen legible) y crea la foto.
     *
     * @throws CameraException con motivo {@code INVALID_IMAGE} si no sirve
     */
    public static CapturedImage fromBytes(byte[] bytes, long maxBytes) throws CameraException {
        if (bytes == null || bytes.length == 0) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "La imagen está vacía.");
        }
        if (bytes.length > maxBytes) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE,
                    "La imagen supera el tamaño máximo de " + (maxBytes / (1024 * 1024)) + " MB.");
        }
        String mime = detectMimeType(bytes);
        if (mime == null) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "Solo se aceptan fotos JPEG o PNG.");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "La imagen no se puede leer.", e);
        }
        if (image == null) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "La imagen no se puede leer.");
        }
        return new CapturedImage(bytes, mime, image.getWidth(), image.getHeight(), LocalDateTime.now());
    }

    /** Reconoce JPEG ({@code FF D8 FF}) y PNG ({@code 89 50 4E 47}) por sus primeros bytes. */
    public static String detectMimeType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return JPEG;
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return PNG;
        }
        return null;
    }
}