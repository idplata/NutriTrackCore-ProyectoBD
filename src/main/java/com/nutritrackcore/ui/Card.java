package com.nutritrackcore.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Tarjeta blanca con bordes redondeados y una sombra suave. Opcionalmente
 * lleva una foto de portada arriba.
 * <p>
 * El contenido se agrega como en cualquier {@link JPanel}; el borde interno ya
 * deja el espacio de la portada y el margen.
 */
public class Card extends JPanel {

    private static final int ARC = 28;
    private static final int SHADOW = 6;

    private final Image cover;
    private final int coverHeight;
    private final double coverFocusY;

    /** Tarjeta sin portada. */
    public Card() {
        this(null, 0, 0.5);
    }

    /**
     * Tarjeta con foto de portada.
     *
     * @param cover       foto (puede ser {@code null})
     * @param coverHeight alto de la foto
     * @param coverFocusY qué parte vertical de la foto se ve (0 = arriba, 1 = abajo)
     */
    public Card(Image cover, int coverHeight, double coverFocusY) {
        this.cover = cover;
        this.coverHeight = cover == null ? 0 : coverHeight;
        this.coverFocusY = coverFocusY;
        setOpaque(false);
        setPadding(18, 20, 18, 20);
    }

    /**
     * Cambia el margen interno (la portada se suma sola arriba).
     *
     * @param top    arriba
     * @param left   izquierda
     * @param bottom abajo
     * @param right  derecha
     */
    public final void setPadding(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(coverHeight + top, left + SHADOW / 2,
                bottom + SHADOW, right + SHADOW / 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = SHADOW / 2;
            int w = getWidth() - SHADOW;
            int h = getHeight() - SHADOW;

            // Sombra: capas translúcidas cada vez más pequeñas.
            for (int i = SHADOW; i > 0; i--) {
                g2.setColor(new Color(8, 32, 40, 7));
                g2.fill(new RoundRectangle2D.Double(x - i / 2.0, i / 1.5, w + i, h + i / 2.0, ARC + i, ARC + i));
            }

            Shape shape = new RoundRectangle2D.Double(x, 0, w, h, ARC, ARC);
            g2.setColor(Color.WHITE);
            g2.fill(shape);

            if (cover != null) {
                g2.clip(shape);
                Images.paintCover(g2, cover, x, 0, w, coverHeight, coverFocusY);
            }
        } finally {
            g2.dispose();
        }
    }
}
