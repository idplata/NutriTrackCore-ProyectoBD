package com.nutritrackcore.camera;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente de {@code POST /analyze}: detecta todos los alimentos del plato.
 */
public class AiPlateAnalyzer extends VisionClient implements PlateAnalyzer {
    private static final int MAX_ALTERNATIVES = 3;

    public AiPlateAnalyzer(CameraSettings settings) {
        super(settings);
    }

    @Override
    public boolean isAvailable() {
        return health();
    }

    @Override
    public List<DetectedPortion> analyze(CapturedImage image) throws CameraException {
        JsonObject json = postImage("/analyze", image, settings.getAnalyzeTimeout());
        try {
            JsonElement itemsElement = json.get("items");
            if (itemsElement == null || !itemsElement.isJsonArray()) {
                throw invalidResponse(null);
            }
            List<DetectedPortion> result = new ArrayList<>();
            for (JsonElement element : itemsElement.getAsJsonArray()) {
                JsonObject item = element.getAsJsonObject();
                double grams = item.get("grams").getAsDouble();
                if (!(grams > 0)) {
                    continue;                                   // gramos inválidos: se ignora el item
                }
                JsonElement foodId = item.get("food_id");
                JsonElement label = item.get("label");
                JsonElement confidence = item.get("confidence");
                result.add(new DetectedPortion(
                        foodId == null || foodId.isJsonNull() ? null : foodId.getAsInt(),
                        label == null || label.isJsonNull() ? "" : label.getAsString(),
                        grams,
                        confidence == null || confidence.isJsonNull() ? 0 : clampConfidence(confidence.getAsDouble()),
                        alternatives(item.get("alternatives"))));
            }
            return result;
        } catch (CameraException e) {
            throw e;
        } catch (RuntimeException e) {                          // campos faltantes o de otro tipo
            throw invalidResponse(e);
        }
    }

    private static List<Integer> alternatives(JsonElement element) {
        List<Integer> ids = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return ids;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement id : array) {
            if (ids.size() == MAX_ALTERNATIVES) {
                break;
            }
            if (!id.isJsonNull()) {
                ids.add(id.getAsInt());
            }
        }
        return ids;
    }
}