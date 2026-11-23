package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * Grupo de botones tipo "chip" donde solo uno puede estar elegido. Cada chip
 * guarda un valor interno (el que entiende el servicio) y muestra un texto
 * amigable.
 */
public class Chips extends JPanel {

    private final ButtonGroup group = new ButtonGroup();
    private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();

    /**
     * Crea el grupo.
     *
     * @param options valor interno → texto visible, en el orden en que se muestran
     */
    public Chips(Map<String, String> options) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        JPanel wrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 8));
        wrap.setOpaque(false);
        for (Map.Entry<String, String> option : options.entrySet()) {
            JToggleButton chip = new JToggleButton(option.getValue());
            chip.setFont(Theme.font("Medium", 13));
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.setFocusPainted(false);
            chip.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 999; focusWidth: 0; borderWidth: 1; margin: 6,14,6,14;"
                    + "background: #FFFFFF; borderColor: #D6DEE4; foreground: #33424D;"
                    + "hoverBackground: #F1F8F6; selectedBackground: #0B7A69; selectedForeground: #FFFFFF;"
                    + "pressedBackground: #E3F1EE");
            chip.getAccessibleContext().setAccessibleName(option.getValue());
            chip.addItemListener(e -> chip.setForeground(chip.isSelected() ? java.awt.Color.WHITE : Theme.TEXT));
            group.add(chip);
            buttons.put(option.getKey(), chip);
            wrap.add(chip);
        }
        setLayout(new java.awt.BorderLayout());
        add(wrap, java.awt.BorderLayout.CENTER);
    }

    @Override
    public java.awt.Dimension getMaximumSize() {
        return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /**
     * Valor interno del chip elegido.
     *
     * @return valor, o {@code null} si no hay ninguno elegido
     */
    public String getValue() {
        for (Map.Entry<String, JToggleButton> e : buttons.entrySet()) {
            if (e.getValue().isSelected()) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Marca el chip con ese valor (sin distinguir mayúsculas).
     *
     * @param value valor interno; si es {@code null} o no existe, no cambia nada
     */
    public void setValue(String value) {
        if (value == null) {
            return;
        }
        for (Map.Entry<String, JToggleButton> e : buttons.entrySet()) {
            if (e.getKey().equalsIgnoreCase(value)) {
                e.getValue().setSelected(true);
            }
        }
    }

    /**
     * Activa o desactiva un chip (por ejemplo "subir de peso" con sobrepeso).
     *
     * @param value   valor interno del chip
     * @param enabled si se puede elegir
     */
    public void setOptionEnabled(String value, boolean enabled) {
        JToggleButton chip = buttons.get(value);
        if (chip != null) {
            chip.setEnabled(enabled);
            if (!enabled && chip.isSelected()) {
                group.clearSelection();
            }
        }
    }

    /**
     * Ejecuta una acción cada vez que cambia el chip elegido.
     *
     * @param action acción a ejecutar
     */
    public void onChange(Runnable action) {
        for (JToggleButton chip : buttons.values()) {
            chip.addActionListener(e -> action.run());
        }
    }

    /** Fuente común de las etiquetas de cada pregunta. */
    static Font questionFont() {
        return Theme.font("SemiBold", 13);
    }
}
