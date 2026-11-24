package com.nutritrackcore.camera;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Espera la foto que sube el celular, con tiempo límite.
 * Se debe llamar fuera del hilo de Swing (por ejemplo en un {@code SwingWorker}).
 */
public class PhoneImageSource implements ImageSource {
    private final PhoneUploadSession session;
    private final Duration timeout;

    public PhoneImageSource(PhoneUploadSession session, Duration timeout) {
        this.session = Objects.requireNonNull(session, "sesión del celular");
        this.timeout = Objects.requireNonNull(timeout, "tiempo de espera");
    }

    @Override
    public CapturedImage capture() throws CameraException {
        try {
            return session.image().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new CameraException(CameraException.Reason.TIMEOUT, "No llegó la foto a tiempo.", e);
        } catch (CancellationException e) {
            throw new CameraException(CameraException.Reason.NETWORK, "Se canceló la espera de la foto.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CameraException(CameraException.Reason.NETWORK, "Se interrumpió la espera de la foto.", e);
        } catch (ExecutionException e) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "La foto no se pudo recibir.", e);
        }
    }
}