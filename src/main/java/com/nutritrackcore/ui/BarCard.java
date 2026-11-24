package com.nutritrackcore.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * Tarjeta blanca con una barra de color arriba que lleva el título del bloque
 * (los tres bloques de "Mis hábitos" usan azul rey).
 */
public class BarCard extends JPanel {

    /** Azul rey de las barras de los formularios. */
    public static final Color ROYAL_BLUE = new Color(0x2F5BD3);

    private static final int ARC = 24;
    private static final int BAR = 44;

    private final String title;
    private final Color barColor;

    /**
     * @param title    texto de la barra
     * @param barColor color de la barra
     */
    public BarCard(String title, Color barColor) {
        this.title = title;
        this.barColor = barColor;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(BAR + 16, 20, 20, 20));
        getAccessibleContext().setAccessibleName(title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(2, 4, w - 4, h - 4, ARC, ARC);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0, 0, w - 2, h - 4, ARC, ARC);
            g2.setColor(Color.WHITE);
            g2.fill(shape);
            Area bar = new Area(shape);
            bar.intersect(new Area(new Rectangle2D.Double(0, 0, w, BAR)));
            g2.setColor(barColor);
            g2.fill(bar);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.font("SemiBold", 15));
            int baseline = (BAR + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(title, 20, baseline);
        } finally {
            g2.dispose();
        }
    }

    /** Fuente de los textos de ayuda bajo cada pregunta. */
    static Font hintFont() {
        return Theme.font(Font.PLAIN, 12);
    }
}
