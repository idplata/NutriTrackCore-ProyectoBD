package com.nutritrackcore.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Etiqueta con forma de píldora (fondo de color y bordes totalmente
 * redondeados). Se usa para la clasificación del IMC, las condiciones y las
 * restricciones de la rutina.
 */
public class Pill extends JLabel {

    private Color fill;

    /**
     * Crea la etiqueta.
     *
     * @param text  texto
     * @param fill  color de fondo
     * @param color color del texto
     */
    public Pill(String text, Color fill, Color color) {
        super(text);
        this.fill = fill;
        setForeground(color);
        setFont(Theme.font("SemiBold", 12));
        setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        setOpaque(false);
    }

    /**
     * Cambia el color de fondo.
     *
     * @param fill nuevo color
     */
    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            int arc = Math.min(getHeight(), 26);   // en varias líneas queda como tarjeta redondeada
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
