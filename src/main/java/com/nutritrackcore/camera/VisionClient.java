package com.nutritrackcore.camera;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Base de los clientes del servicio de visión: envía la foto y traduce
 * los errores de red y de HTTP a {@link CameraException}.
 */
abstract class VisionClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

    protected final CameraSettings settings;
    private final HttpClient http;

    VisionClient(CameraSettings settings) {
        this.settings = Objects.requireNonNull(settings, "configuración de la cámara");
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .proxy(HttpClient.Builder.NO_PROXY)          // el servicio está en este mismo PC
                .build();
    }

    /** {@code GET /health}: {@code true} si responde 200 con {@code "status": "ok"}. */
    boolean health() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getVisionUrl() + "/health"))
                .timeout(HEALTH_TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return false;
            }
            JsonElement status = JsonParser.parseString(response.body()).getAsJsonObject().get("status");
            return status != null && "ok".equals(status.getAsString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Envía la foto por POST y devuelve el JSON de la respuesta. */
    JsonObject postImage(String pathAndQuery, CapturedImage image, Duration timeout) throws CameraException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getVisionUrl() + pathAndQuery))
                .timeout(timeout)
                .header("Content-Type", image.mimeType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(image.bytes()))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new CameraException(CameraException.Reason.UNAVAILABLE, "El reconocimiento no está disponible.", e);
        } catch (HttpTimeoutException e) {
            throw new CameraException(CameraException.Reason.TIMEOUT, "El reconocimiento tardó demasiado.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CameraException(CameraException.Reason.NETWORK, "Se canceló el reconocimiento.", e);
        } catch (IOException e) {
            throw new CameraException(CameraException.Reason.NETWORK, "Falló la conexión con el reconocimiento.", e);
        }
        int status = response.statusCode();
        if (status == 400 || status == 413) {
            throw new CameraException(CameraException.Reason.INVALID_IMAGE, "La imagen no es válida.");
        }
        if (status != 200) {
            throw new CameraException(CameraException.Reason.ESTIMATION,
                    "El reconocimiento respondió con un error (" + status + ").");
        }
        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw invalidResponse(e);
        }
    }

    static CameraException invalidResponse(Exception cause) {
        return new CameraException(CameraException.Reason.ESTIMATION,
                "La respuesta del reconocimiento no se pudo leer.", cause);
    }

    static double clampConfidence(double value) {
        return Math.max(0, Math.min(1, value));
    }
}