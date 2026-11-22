package com.nutritrackcore.camera;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Arranca el servicio de visión de Python si no está corriendo, y lo detiene al salir
 * (solo si lo arrancó la aplicación).
 * <p>
 * Busca, en este orden, dentro de la carpeta {@code ml}:
 * <ol>
 *   <li>{@code vision_service/vision_service.exe} (versión empaquetada con PyInstaller)</li>
 *   <li>{@code .venv/Scripts/python.exe} (Windows) o {@code .venv/bin/python} (Linux/Mac)</li>
 * </ol>
 */
public class VisionServiceLauncher {
    private static final long POLL_MILLIS = 500;

    private final CameraSettings settings;
    private final BooleanSupplier healthCheck;
    private Process process;

    /**
     * @param healthCheck devuelve {@code true} si el servicio responde (normalmente {@code analyzer::isAvailable})
     */
    public VisionServiceLauncher(CameraSettings settings, BooleanSupplier healthCheck) {
        this.settings = Objects.requireNonNull(settings, "configuración de la cámara");
        this.healthCheck = Objects.requireNonNull(healthCheck, "chequeo del servicio");
    }

    /**
     * Deja el servicio listo. Puede tardar varios segundos: llamar fuera del hilo de Swing.
     *
     * @return {@code true} si el servicio responde al terminar
     */
    public synchronized boolean ensureRunning() {
        if (healthCheck.getAsBoolean()) {
            return true;
        }
        if (!settings.isAutostart()) {
            return false;
        }
        if (process == null || !process.isAlive()) {
            Optional<List<String>> command = command();
            if (command.isEmpty()) {
                return false;
            }
            try {
                Path ml = settings.getMlFolder().toAbsolutePath();
                process = new ProcessBuilder(command.get())
                        .directory(ml.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(ml.resolve("vision_service.log").toFile())
                        .start();
            } catch (IOException e) {
                process = null;
                return false;
            }
        }
        return waitUntilHealthy(settings.getStartTimeout());
    }

    /** Detiene el servicio si lo arrancó la aplicación. */
    public synchronized void stop() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        process = null;
    }

    /** Comando para arrancar el servicio, o vacío si no se encontró Python ni el .exe. */
    Optional<List<String>> command() {
        Path ml = settings.getMlFolder().toAbsolutePath();
        String port = String.valueOf(settings.getVisionPort());
        Path exe = ml.resolve("vision_service").resolve("vision_service.exe");
        if (Files.isRegularFile(exe)) {
            return Optional.of(List.of(exe.toString(), "--host", "127.0.0.1", "--port", port));
        }
        Path python = ml.resolve(".venv").resolve(File.separatorChar == '\\' ? "Scripts/python.exe" : "bin/python");
        if (Files.isRegularFile(python)) {
            return Optional.of(List.of(python.toString(), "-m", "uvicorn", settings.getVisionModule() + ":app",
                    "--host", "127.0.0.1", "--port", port));
        }
        return Optional.empty();
    }

    private boolean waitUntilHealthy(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (healthCheck.getAsBoolean()) {
                return true;
            }
            if (process == null || !process.isAlive()) {
                return false;                                   // el proceso murió: ver ml/vision_service.log
            }
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}