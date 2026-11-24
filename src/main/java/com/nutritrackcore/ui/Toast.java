package com.nutritrackcore.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Aviso corto, oscuro y redondeado, abajo en el centro de la ventana; se oculta solo. */
public final class Toast {

    private Toast() {
    }

    /**
     * Muestra el aviso unos segundos.
     *
     * @param anyComponent cualquier componente de la ventana
     * @param text         mensaje
     */
    public static void show(Component anyComponent, String text) {
        JRootPane root = SwingUtilities.getRootPane(anyComponent);
        if (root == null) {
            return;
        }
        JLayeredPane layers = root.getLayeredPane();
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(15, 31, 42, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(Proto.font("Medium", 14));
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        Dimension size = label.getPreferredSize();
        label.setBounds((layers.getWidth() - size.width) / 2, layers.getHeight() - size.height - 28,
                size.width, size.height);
        layers.add(label, JLayeredPane.POPUP_LAYER);
        layers.repaint();
        Timer timer = new Timer(2600, e -> {
            layers.remove(label);
            layers.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }
}
