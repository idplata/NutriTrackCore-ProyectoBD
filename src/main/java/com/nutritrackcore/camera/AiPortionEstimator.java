package com.nutritrackcore.camera;

import com.google.gson.JsonObject;

/**
 * Cliente de {@code POST /estimate?food_id=...}: gramos de un alimento que eligió el usuario.
 */
public class AiPortionEstimator extends VisionClient implements PortionEstimator {

    public AiPortionEstimator(CameraSettings settings) {
        super(settings);
    }

    @Override
    public PortionEstimate estimate(CapturedImage image, int foodId) throws CameraException {
        JsonObject json = postImage("/estimate?food_id=" + foodId, image, settings.getEstimateTimeout());
        try {
            double grams = json.get("grams").getAsDouble();
            if (!(grams > 0)) {
                throw invalidResponse(null);
            }
            double confidence = json.has("confidence") ? clampConfidence(json.get("confidence").getAsDouble()) : 0;
            return new PortionEstimate(grams, confidence);
        } catch (CameraException e) {
            throw e;
        } catch (RuntimeException e) {
            throw invalidResponse(e);
        }
    }
}