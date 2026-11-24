package com.nutritrackcore.camera;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Configuración de la cámara, leída de {@code camera.properties} (en el classpath).
 * Si falta el archivo o una clave, se usan los valores por defecto.
 */
public final class CameraSettings {
    private static final String FILE = "/camera.properties";

    private final int uploadPort;
    private final String visionUrl;
    private final Duration analyzeTimeout;
    private final Duration estimateTimeout;
    private final Duration uploadTimeout;
    private final Duration startTimeout;
    private final long uploadMaxBytes;
    private final double confidenceLow;
    private final boolean autostart;
    private final Path mlFolder;
    private final String visionModule;

    /** Crea la configuración desde unas propiedades (sirve para las pruebas). */
    public CameraSettings(Properties p) {
        this.uploadPort = intValue(p, "upload.port", 8085, 0, 65535);
        this.visionUrl = trimSlash(p.getProperty("vision.url", "http://127.0.0.1:8000"));
        this.analyzeTimeout = Duration.ofSeconds(intValue(p, "vision.analyze.timeout.seconds", 30, 1, 600));
        this.estimateTimeout = Duration.ofSeconds(intValue(p, "vision.estimate.timeout.seconds", 5, 1, 600));
        this.uploadTimeout = Duration.ofSeconds(intValue(p, "upload.timeout.seconds", 120, 1, 3600));
        this.startTimeout = Duration.ofSeconds(intValue(p, "vision.start.timeout.seconds", 60, 1, 600));
        this.uploadMaxBytes = intValue(p, "upload.max.bytes", 10_485_760, 1, Integer.MAX_VALUE);
        this.confidenceLow = doubleValue(p, "confidence.low", 0.4);
        this.autostart = Boolean.parseBoolean(p.getProperty("vision.autostart", "true").trim());
        this.mlFolder = Path.of(p.getProperty("ml.folder", "ml").trim());
        this.visionModule = p.getProperty("vision.module", "stub_service").trim();
        URI.create(visionUrl);   // falla pronto si la URL está mal escrita
    }

    /** Lee {@code camera.properties}; si no existe, usa los valores por defecto. */
    public static CameraSettings load() {
        Properties properties = new Properties();
        try (InputStream in = CameraSettings.class.getResourceAsStream(FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            // se siguen usando los valores por defecto
        }
        return new CameraSettings(properties);
    }

    public int getUploadPort() { return uploadPort; }
    public String getVisionUrl() { return visionUrl; }
    public Duration getAnalyzeTimeout() { return analyzeTimeout; }
    public Duration getEstimateTimeout() { return estimateTimeout; }
    public Duration getUploadTimeout() { return uploadTimeout; }
    public Duration getStartTimeout() { return startTimeout; }
    public long getUploadMaxBytes() { return uploadMaxBytes; }
    public double getConfidenceLow() { return confidenceLow; }
    public boolean isAutostart() { return autostart; }
    public Path getMlFolder() { return mlFolder; }
    /** Módulo de Python que se arranca: {@code stub_service} o {@code vision_service}. */
    public String getVisionModule() { return visionModule; }

    /** Puerto del servicio de visión, tomado de {@code vision.url}. */
    public int getVisionPort() {
        int port = URI.create(visionUrl).getPort();
        return port == -1 ? 80 : port;
    }

    private static int intValue(Properties p, String key, int def, int min, int max) {
        String raw = p.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new IllegalArgumentException(key + " debe estar entre " + min + " y " + max + ".");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " debe ser un número entero.", e);
        }
    }

    private static double doubleValue(Properties p, String key, double def) {
        String raw = p.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0 || value > 1) {
                throw new IllegalArgumentException(key + " debe estar entre 0 y 1.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " debe ser un número.", e);
        }
    }

    private static String trimSlash(String url) {
        String clean = url.trim();
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }
}