package com.nutritrackcore.camera;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Respaldo: usa una foto guardada en el disco (la elige el usuario con un {@code JFileChooser}).
 */
public class FileImageSource implements ImageSource {
    private final Path file;
    private final long maxBytes;

    public FileImageSource(Path file, long maxBytes) {
        this.file = Objects.requireNonNull(file, "archivo");
        this.maxBytes = maxBytes;
    }

    @Override
    public CapturedImage capture() throws CameraException {
        try {
            if (Files.size(file) > maxBytes) {
                throw new CameraException(CameraException.Reason.INVALID_IMAGE,
                        "La imagen supera el tamaño máximo de " + (maxBytes / (1024 * 1024)) + " MB.");
            }
            return CapturedImage.fromBytes(Files.readAllBytes(file), maxBytes);
        } catch (IOException e) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "No se pudo leer el archivo.", e);
        }
    }
}