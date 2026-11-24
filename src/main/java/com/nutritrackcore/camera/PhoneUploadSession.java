package com.nutritrackcore.camera;

import java.util.concurrent.CompletableFuture;

/**
 * Enlace de un solo uso para que el celular suba una foto.
 * Se cierra al recibir la foto, al cancelar o al cerrar el diálogo.
 */
public class PhoneUploadSession {
    private final String token;
    private final String url;
    private final CompletableFuture<CapturedImage> image = new CompletableFuture<>();
    private final PhoneUploadServer server;

    PhoneUploadSession(String token, String url, PhoneUploadServer server) {
        this.token = token;
        this.url = url;
        this.server = server;
    }

    /** Enlace que se muestra en pantalla, por ejemplo {@code http://192.168.1.20:8085/u/abc...}. */
    public String getUrl() { return url; }

    public String getToken() { return token; }

    /** Se completa cuando llega la foto. */
    public CompletableFuture<CapturedImage> image() { return image; }

    /** Indica si todavía acepta la foto. */
    public boolean isOpen() { return !image.isDone(); }

    /** Cierra el enlace; si la foto no había llegado, cancela la espera. */
    public void close() {
        image.cancel(false);
        server.forget(this);
    }

    /** Lo llama el servidor al recibir una foto válida. Devuelve {@code false} si ya estaba cerrada. */
    boolean deliver(CapturedImage captured) {
        return image.complete(captured);
    }
}