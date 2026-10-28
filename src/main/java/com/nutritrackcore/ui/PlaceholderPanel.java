package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Pantalla provisional para las secciones que todavía no están construidas.
 * Se irá reemplazando por los paneles reales (registro, menú principal…).
 */
public class PlaceholderPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();

    /**
     * Crea la pantalla provisional.
     *
     * @param title      título grande
     * @param text       explicación corta
     * @param buttonText texto del botón
     * @param action     qué hace el botón
     */
    public PlaceholderPanel(String title, String text, String buttonText, Runnable action) {
        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        titleLabel.setText(title);
        titleLabel.setFont(Theme.font(Font.BOLD, 30));
        titleLabel.setForeground(Theme.TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel body = new JLabel(text);
        body.setFont(Theme.font(Font.PLAIN, 15));
        body.setForeground(Theme.TEXT_MUTED);
        body.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton button = new JButton(buttonText);
        button.setFont(Theme.font("SemiBold", 15));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; margin: 10,22,10,22; foreground: #0B7A69; borderColor: #0B7A69");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> action.run());

        box.add(titleLabel);
        box.add(Box.createVerticalStrut(10));
        box.add(body);
        box.add(Box.createVerticalStrut(28));
        box.add(button);
        add(box, new GridBagConstraints());
    }

    /**
     * Cambia el título (por ejemplo, para saludar al usuario por su nombre).
     *
     * @param title nuevo título
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
