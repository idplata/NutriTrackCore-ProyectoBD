package com.nutritrackcore.camera;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP (del JDK) que recibe la foto del celular.
 * <p>
 * Escucha en {@code 0.0.0.0} y solo está encendido mientras haya un enlace abierto.
 * Rutas: {@code GET /u/{token}} (página de subida) y {@code POST /u/{token}} (la foto).
 * Cada token sirve para una sola foto; un token cerrado o desconocido responde 404.
 */
public class PhoneUploadServer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "/u/";

    private final CameraSettings settings;
    private final Map<String, PhoneUploadSession> sessions = new ConcurrentHashMap<>();
    private HttpServer server;
    private ExecutorService executor;

    public PhoneUploadServer(CameraSettings settings) {
        this.settings = settings;
    }

    /**
     * Abre un enlace nuevo (cierra el anterior, si había) y enciende el servidor si hace falta.
     *
     * @param host IP del PC que verá el celular
     * @throws CameraException si el puerto está ocupado
     */
    public synchronized PhoneUploadSession openSession(String host) throws CameraException {
        for (PhoneUploadSession previous : sessions.values()) {
            previous.image().cancel(false);                   // un solo enlace a la vez
        }
        sessions.clear();
        start();
        String token = newToken();
        String url = "http://" + host + ":" + getPort() + PREFIX + token;
        PhoneUploadSession session = new PhoneUploadSession(token, url, this);
        sessions.put(token, session);
        return session;
    }

    /** Puerto real (útil cuando {@code upload.port=0} en las pruebas). */
    public synchronized int getPort() {
        return server == null ? settings.getUploadPort() : server.getAddress().getPort();
    }

    /** Apaga el servidor y cierra todos los enlaces. */
    public synchronized void stop() {
        for (PhoneUploadSession session : sessions.values()) {
            session.image().cancel(false);
        }
        sessions.clear();
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
            server = null;
            executor = null;
        }
    }

    /** Lo llama la sesión al cerrarse; si no quedan enlaces, apaga el servidor. */
    synchronized void forget(PhoneUploadSession session) {
        sessions.remove(session.getToken(), session);
        if (sessions.isEmpty()) {
            stop();
        }
    }

    private void start() throws CameraException {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", settings.getUploadPort()), 0);
        } catch (IOException e) {
            throw new CameraException(CameraException.Reason.NETWORK,
                    "No se pudo abrir el puerto " + settings.getUploadPort() + "; puede estar en uso.", e);
        }
        executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "nutritrack-subida");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.createContext(PREFIX, this::handle);
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String token = exchange.getRequestURI().getPath().substring(PREFIX.length());
            PhoneUploadSession session = sessions.get(token);
            if (session == null || !session.isOpen()) {
                sendText(exchange, 404, PhoneUploadPage.expired());
                return;
            }
            switch (exchange.getRequestMethod()) {
                case "GET" -> sendText(exchange, 200, PhoneUploadPage.form());
                case "POST" -> receive(exchange, session);
                default -> sendText(exchange, 405, PhoneUploadPage.message("Método no permitido."));
            }
        }
    }

    private void receive(HttpExchange exchange, PhoneUploadSession session) throws IOException {
        long max = settings.getUploadMaxBytes();
        String length = exchange.getRequestHeaders().getFirst("Content-Length");
        if (length != null && parseLong(length) > max) {
            sendPlain(exchange, 413, "La foto es demasiado grande.");
            return;
        }
        byte[] body = readAtMost(exchange.getRequestBody(), max + 1);
        if (body.length > max) {
            sendPlain(exchange, 413, "La foto es demasiado grande.");
            return;
        }
        CapturedImage image;
        try {
            image = CapturedImage.fromBytes(body, max);
        } catch (CameraException e) {
            sendPlain(exchange, 400, e.getMessage());
            return;
        }
        if (!session.deliver(image)) {
            sendPlain(exchange, 404, "Sesión vencida. Abre de nuevo el enlace desde NutriTrack.");
            return;
        }
        sendPlain(exchange, 200, "Foto recibida. Ya puedes volver al computador.");
    }

    private static byte[] readAtMost(InputStream in, long limit) throws IOException {
        return in.readNBytes((int) Math.min(limit, Integer.MAX_VALUE - 8));
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Respuesta a la subida: texto plano, que la página muestra tal cual. */
    private static void sendPlain(HttpExchange exchange, int status, String text) throws IOException {
        send(exchange, status, text, "text/plain; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String html) throws IOException {
        send(exchange, status, html, "text/html; charset=utf-8");
    }

    private static void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[16];                      // 128 bits
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}