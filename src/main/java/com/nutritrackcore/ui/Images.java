package com.nutritrackcore.ui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * Carga las fotos de {@code resources/images} una sola vez y las dibuja
 * "en modo portada": llenan el espacio sin deformarse, recortando lo que sobra.
 */
public final class Images {

    /** Foto de fondo del menú lateral. */
    public static final String MENU_BACKGROUND = "menu-bg.jpg";
    /** Fondo azul de las pantallas internas. */
    public static final String HOME_BACKGROUND = "home-bg.jpg";
    /** Portada de la tarjeta de condiciones médicas. */
    public static final String CARD_CONDITIONS = "card-conditions.jpg";
    /** Portada de la tarjeta de dieta. */
    public static final String CARD_DIET = "card-diet.jpg";
    /** Portada de la tarjeta de rutina. */
    public static final String CARD_ROUTINE = "card-routine.jpg";

    private static final Map<String, Image> CACHE = new HashMap<>();

    private Images() {
    }

    /**
     * Devuelve una imagen de {@code resources/images}.
     *
     * @param name nombre del archivo
     * @return la imagen, o {@code null} si no existe
     */
    public static Image get(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            java.net.URL url = Images.class.getResource("/images/" + n);
            return url == null ? null : new ImageIcon(url).getImage();
        });
    }

    /**
     * Dibuja una imagen llenando el rectángulo sin deformarla.
     *
     * @param g2     gráficos donde dibujar
     * @param image  imagen (si es {@code null} no dibuja nada)
     * @param x      izquierda
     * @param y      arriba
     * @param w      ancho
     * @param h      alto
     * @param focusY qué parte vertical se conserva al recortar (0 = arriba, 0,5 = centro, 1 = abajo)
     */
    public static void paintCover(Graphics2D g2, Image image, int x, int y, int w, int h, double focusY) {
        if (image == null || image.getWidth(null) <= 0 || w <= 0 || h <= 0) {
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        int iw = image.getWidth(null);
        int ih = image.getHeight(null);
        double scale = Math.max((double) w / iw, (double) h / ih);
        int dw = (int) Math.ceil(iw * scale);
        int dh = (int) Math.ceil(ih * scale);
        int dx = x + (w - dw) / 2;
        int dy = y + (int) ((h - dh) * focusY);
        Graphics2D clipped = (Graphics2D) g2.create();
        try {
            clipped.clipRect(x, y, w, h);          // nunca se sale del rectángulo pedido
            clipped.drawImage(image, dx, dy, dw, dh, null);
        } finally {
            clipped.dispose();
        }
    }
}
