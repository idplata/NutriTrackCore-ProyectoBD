package com.nutritrackcore.camera;

import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.model.AllergyFood;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.repository.AllergyFoodRepository;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import com.nutritrackcore.services.AllergyService;
import com.nutritrackcore.services.ConsumptionService;
import com.nutritrackcore.services.FoodService;
import com.nutritrackcore.services.NutritionCalculator;
import com.nutritrackcore.services.ParameterChecks;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordina la captura: enlace del celular, reconocimiento, edición de filas y registro.
 * <p>
 * No guarda la foto. Las kcal de las filas son vista previa; al confirmar,
 * {@link ConsumptionService} vuelve a calcularlas y guarda encabezado y líneas
 * en una sola transacción.
 */
public class FoodCaptureService {
    private final CameraSettings settings;
    private final PlateAnalyzer analyzer;
    private final PortionEstimator estimator;
    private final PhoneUploadServer uploadServer;
    private final VisionServiceLauncher launcher;
    private final FoodService foodService;
    private final ConsumptionService consumptionService;
    private final AllergyService allergyService;
    private final AllergyFoodRepository allergyFoodRepository;
    private final UserExcludedFoodRepository excludedFoodRepository;
    private PhoneUploadSession currentSession;

    public FoodCaptureService(CameraSettings settings, PlateAnalyzer analyzer, PortionEstimator estimator,
                              PhoneUploadServer uploadServer, VisionServiceLauncher launcher,
                              FoodService foodService, ConsumptionService consumptionService,
                              AllergyService allergyService, AllergyFoodRepository allergyFoodRepository,
                              UserExcludedFoodRepository excludedFoodRepository) {
        this.settings = Objects.requireNonNull(settings, "configuración de la cámara");
        this.analyzer = Objects.requireNonNull(analyzer, "analizador");
        this.estimator = Objects.requireNonNull(estimator, "estimador");
        this.uploadServer = Objects.requireNonNull(uploadServer, "servidor de subida");
        this.launcher = Objects.requireNonNull(launcher, "lanzador del servicio");
        this.foodService = Objects.requireNonNull(foodService, "servicio de alimentos");
        this.consumptionService = Objects.requireNonNull(consumptionService, "servicio de consumo");
        this.allergyService = Objects.requireNonNull(allergyService, "servicio de alergias");
        this.allergyFoodRepository = Objects.requireNonNull(allergyFoodRepository, "repositorio alergia-alimento");
        this.excludedFoodRepository = Objects.requireNonNull(excludedFoodRepository, "repositorio de excluidos");
    }

    public CameraSettings getSettings() {
        return settings;
    }

    /**
     * Indica si el reconocimiento está disponible; si no responde, intenta arrancarlo.
     * Puede tardar: llamar fuera del hilo de Swing.
     */
    public boolean checkVisionService() {
        return analyzer.isAvailable() || launcher.ensureRunning();
    }

    /**
     * Abre un enlace nuevo para el celular (cierra el anterior).
     *
     * @param host IP del PC; si es {@code null} se usa la más probable
     */
    public PhoneUploadSession startPhoneSession(String host) throws CameraException {
        String ip = host != null ? host : LocalNetworkAddress.preferred().orElseThrow(() -> new CameraException(
                CameraException.Reason.NETWORK, "El computador no está conectado a una red Wi-Fi."));
        cancel();
        currentSession = uploadServer.openSession(ip);
        return currentSession;
    }

    /** Espera la foto del enlace abierto. Llamar fuera del hilo de Swing. */
    public CapturedImage waitForPhoto() throws CameraException {
        if (currentSession == null) {
            throw new CameraException(CameraException.Reason.NETWORK, "No hay un enlace abierto para el celular.");
        }
        return new PhoneImageSource(currentSession, settings.getUploadTimeout()).capture();
    }

    /** Lee una foto del disco (respaldo). */
    public CapturedImage loadFromFile(java.nio.file.Path file) throws CameraException {
        return new FileImageSource(file, settings.getUploadMaxBytes()).capture();
    }

    /**
     * Reconoce los alimentos y arma las filas: nombre del catálogo, alternativas, kcal
     * y aviso de alergia. Une los alimentos repetidos sumando sus gramos.
     * Los ids inexistentes o inactivos quedan como fila sin alimento.
     */
    public List<CaptureItem> analyze(int userId, CapturedImage image) throws NutriTrackException {
        List<DetectedPortion> portions = analyzer.analyze(image);
        Restrictions restrictions = restrictionsOf(userId);
        Map<Integer, CaptureItem> byFood = new LinkedHashMap<>();
        List<CaptureItem> unknown = new ArrayList<>();
        for (DetectedPortion portion : portions) {
            Optional<Food> food = activeFood(portion.foodId());
            List<FoodOption> alternatives = options(portion.alternativeIds(), portion.foodId());
            double grams = Math.min(portion.grams(), ParameterChecks.MAX_CONSUMED_GRAMS);
            if (food.isEmpty()) {
                CaptureItem item = new CaptureItem(null, null, portion.label(), grams, portion.grams(),
                        portion.confidence());
                item.setAlternatives(alternatives);
                unknown.add(item);
                continue;
            }
            CaptureItem existing = byFood.get(food.get().getFoodId());
            if (existing != null) {                                        // alimento repetido: se suman gramos
                existing.setGrams(Math.min(existing.getGrams() + grams, ParameterChecks.MAX_CONSUMED_GRAMS));
                existing.setConfidence(Math.min(existing.getConfidence(), portion.confidence()));
                mergeAlternatives(existing, alternatives);
                existing.setKcal(kcal(food.get(), existing.getGrams()));
                continue;
            }
            CaptureItem item = new CaptureItem(food.get().getFoodId(), food.get().getName(), portion.label(),
                    grams, portion.grams(), portion.confidence());
            item.setAlternatives(alternatives);
            item.setKcal(kcal(food.get(), grams));
            item.setWarning(restrictions.warningFor(food.get()));
            byFood.put(food.get().getFoodId(), item);
        }
        List<CaptureItem> result = new ArrayList<>(byFood.values());
        result.addAll(unknown);
        return result;
    }

    /** Plan B: gramos del alimento que eligió el usuario. */
    public CaptureItem estimate(int userId, CapturedImage image, int foodId) throws NutriTrackException {
        Food food = requireActiveFood(foodId);
        PortionEstimate estimate = estimator.estimate(image, foodId);
        double grams = Math.min(estimate.grams(), ParameterChecks.MAX_CONSUMED_GRAMS);
        CaptureItem item = new CaptureItem(foodId, food.getName(), null, grams, estimate.grams(), estimate.confidence());
        item.setKcal(kcal(food, grams));
        item.setWarning(restrictionsOf(userId).warningFor(food));
        return item;
    }

    /** Fila nueva que agrega el usuario a mano (sin confianza del modelo). */
    public CaptureItem newItem(int userId, int foodId, double grams) throws NutriTrackException {
        ParameterChecks.requireConsumedGrams(grams);
        Food food = requireActiveFood(foodId);
        CaptureItem item = new CaptureItem(foodId, food.getName(), null, grams, null, null);
        item.setKcal(kcal(food, grams));
        item.setWarning(restrictionsOf(userId).warningFor(food));
        return item;
    }

    /** Cambia los gramos de una fila y recalcula las kcal. */
    public CaptureItem changeGrams(CaptureItem item, double grams) throws NutriTrackException {
        Objects.requireNonNull(item, "fila");
        ParameterChecks.requireConsumedGrams(grams);
        item.setGrams(grams);
        if (item.hasFood()) {
            item.setKcal(kcal(requireActiveFood(item.getFoodId()), grams));
        }
        return item;
    }

    /** Cambia el alimento de una fila, recalcula las kcal y revisa alergias. */
    public CaptureItem changeFood(int userId, CaptureItem item, int foodId) throws NutriTrackException {
        Objects.requireNonNull(item, "fila");
        Food food = requireActiveFood(foodId);
        item.setFoodId(foodId);
        item.setFoodName(food.getName());
        item.setKcal(kcal(food, item.getGrams()));
        item.setWarning(restrictionsOf(userId).warningFor(food));
        return item;
    }

    /** Busca alimentos activos por nombre, para elegir uno a mano. */
    public List<FoodOption> searchFoods(String text) throws NutriTrackException {
        List<FoodOption> result = new ArrayList<>();
        for (Food food : foodService.searchByName(text)) {
            result.add(new FoodOption(food.getFoodId(), food.getName()));
        }
        return result;
    }

    /**
     * Guarda la comida reconocida con la cámara. La confianza del registro es la menor
     * de las filas (las agregadas a mano cuentan como 1).
     *
     * @throws ValidationException si hay una fila sin alimento o no hay filas
     */
    public ConsumptionRecord confirm(int userId, List<CaptureItem> items) throws NutriTrackException {
        return confirm(userId, items, LocalDateTime.now());
    }

    /**
     * Igual que {@link #confirm(int, List)}, pero con la fecha y hora de la comida
     * (por ejemplo, la hora del almuerzo cuando se registra desde la pestaña Almuerzo).
     *
     * @param when fecha y hora de la comida; si es futura se usa la hora actual
     */
    public ConsumptionRecord confirm(int userId, List<CaptureItem> items, LocalDateTime when)
            throws NutriTrackException {
        List<ConsumptionRecordByFood> lines = toLines(items);
        double confidence = 1.0;
        for (CaptureItem item : items) {
            if (item.getConfidence() != null) {
                confidence = Math.min(confidence, item.getConfidence());
            }
        }
        ConsumptionRecord record = consumptionService.register(userId, notFuture(when), confidence, lines);
        cancel();
        return record;
    }

    /** Guarda una comida escrita a mano (sin nivel de estimación). */
    public ConsumptionRecord confirmManual(int userId, List<CaptureItem> items) throws NutriTrackException {
        return confirmManual(userId, items, LocalDateTime.now());
    }

    /** Igual que {@link #confirmManual(int, List)}, con la fecha y hora de la comida. */
    public ConsumptionRecord confirmManual(int userId, List<CaptureItem> items, LocalDateTime when)
            throws NutriTrackException {
        ConsumptionRecord record = consumptionService.registerManual(userId, notFuture(when), toLines(items));
        cancel();
        return record;
    }

    /** La fecha de la comida, o la hora actual si es futura o no se indicó. */
    private static LocalDateTime notFuture(LocalDateTime when) {
        LocalDateTime now = LocalDateTime.now();
        return when == null || when.isAfter(now) ? now : when;
    }

    /** Cierra el enlace del celular, si hay uno abierto. */
    public void cancel() {
        if (currentSession != null) {
            currentSession.close();
            currentSession = null;
        }
    }

    /** Apaga el servidor de subida y el servicio de visión (al salir de la aplicación). */
    public void shutdown() {
        cancel();
        uploadServer.stop();
        launcher.stop();
    }

    private List<ConsumptionRecordByFood> toLines(List<CaptureItem> items) throws ValidationException {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Agrega al menos un alimento.");
        }
        List<ConsumptionRecordByFood> lines = new ArrayList<>();
        for (CaptureItem item : items) {
            if (!item.hasFood()) {
                throw new ValidationException("Hay una fila sin alimento: elígelo o quítala.");
            }
            ParameterChecks.requireConsumedGrams(item.getGrams());
            lines.add(ConsumptionService.line(item.getFoodId(), item.getGrams()));
        }
        return lines;
    }

    private Optional<Food> activeFood(Integer foodId) throws NutriTrackException {
        if (foodId == null || foodId <= 0) {
            return Optional.empty();
        }
        try {
            Food food = foodService.getById(foodId);
            return food.isActive() ? Optional.of(food) : Optional.empty();
        } catch (NotFoundEntityException e) {
            return Optional.empty();
        }
    }

    private Food requireActiveFood(int foodId) throws NutriTrackException {
        return activeFood(foodId).orElseThrow(() -> new NotFoundEntityException(
                "El alimento " + foodId + " no existe o está desactivado."));
    }

    private List<FoodOption> options(List<Integer> ids, Integer ownId) throws NutriTrackException {
        List<FoodOption> result = new ArrayList<>();
        for (Integer id : ids) {
            if (id == null || id.equals(ownId)) {
                continue;
            }
            Optional<Food> food = activeFood(id);
            food.ifPresent(f -> result.add(new FoodOption(f.getFoodId(), f.getName())));
        }
        return result;
    }

    private static void mergeAlternatives(CaptureItem item, List<FoodOption> extra) {
        List<FoodOption> merged = new ArrayList<>(item.getAlternatives());
        for (FoodOption option : extra) {
            if (!merged.contains(option)) {
                merged.add(option);
            }
        }
        item.setAlternatives(merged);
    }

    private static double kcal(Food food, double grams) throws ValidationException {
        return Math.round(NutritionCalculator.calculate(food, grams).getCalories() * 10.0) / 10.0;
    }

    /** Alergias y alimentos excluidos del usuario, con el aviso que se muestra en la fila. */
    private Restrictions restrictionsOf(int userId) throws NutriTrackException {
        Map<Integer, String> byFood = new HashMap<>();
        Map<String, String> byName = new HashMap<>();
        Map<Integer, String> allergyNames = new HashMap<>();
        for (Allergy allergy : allergyService.listCatalog()) {
            allergyNames.put(allergy.getAllergyId(), allergy.getName());
        }
        for (UserAllergy userAllergy : allergyService.listByUser(userId)) {
            String name = allergyNames.getOrDefault(userAllergy.getAllergyId(), "alergia");
            for (AllergyFood link : allergyFoodRepository.findByAllergyId(userAllergy.getAllergyId())) {
                byFood.putIfAbsent(link.getFoodId(), "Alergia: " + name);
            }
            String normalized = normalize(name);
            if (!normalized.isEmpty()) {
                byName.put(normalized, "Alergia: " + name);
            }
        }
        for (UserExcludedFood excluded : excludedFoodRepository.findByUserId(userId)) {
            byFood.putIfAbsent(excluded.getFoodId(), "Lo excluiste de tu dieta");
        }
        return new Restrictions(byFood, byName);
    }

    private static String normalize(String text) {
        String clean = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Avisos por id de alimento y por nombre de alergia (igual que el plan de dieta). */
    private record Restrictions(Map<Integer, String> byFood, Map<String, String> byName) {
        String warningFor(Food food) {
            String warning = byFood.get(food.getFoodId());
            if (warning != null) {
                return warning;
            }
            String name = normalize(food.getName());
            for (Map.Entry<String, String> entry : byName.entrySet()) {
                if (name.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }
}