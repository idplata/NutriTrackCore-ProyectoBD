package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Piezas de formulario con el estilo de NutriTrack (campos, etiquetas,
 * botones y enlaces). Las usan el inicio de sesión, el registro y las demás
 * pantallas para que todo se vea igual.
 */
public final class FormKit {

    /** Alto de los campos de texto. */
    public static final int FIELD_HEIGHT = 50;
    /** Alto del botón principal. */
    public static final int BUTTON_HEIGHT = 54;

    private FormKit() {
    }

    /**
     * Da estilo a un campo de texto (también sirve para {@code JPasswordField}
     * y {@code JFormattedTextField}).
     *
     * @param field       campo a decorar
     * @param placeholder texto gris que se ve cuando está vacío (puede ser {@code null})
     * @param icon        ícono a la izquierda (puede ser {@code null})
     * @param width       ancho del campo
     */
    public static void styleField(JTextField field, String placeholder, Icon icon, int width) {
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setForeground(Theme.TEXT);
        if (placeholder != null) {
            field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        }
        if (icon != null) {
            field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, icon);
        }
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; margin: 4,14,4,12; iconTextGap: 12; placeholderForeground: #8A97A3");
        fixSize(field, width, FIELD_HEIGHT);
    }

    /**
     * Pone una unidad (por ejemplo "kg") al final del campo.
     *
     * @param field campo
     * @param unit  texto de la unidad
     */
    public static void addUnit(JTextField field, String unit) {
        JLabel label = new JLabel(unit);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(Theme.TEXT_MUTED);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 4));
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, label);
    }

    /**
     * Crea la etiqueta que va sobre un campo y la asocia a él (accesibilidad).
     *
     * @param text   texto de la etiqueta
     * @param target campo al que pertenece
     * @return etiqueta lista
     */
    public static JLabel fieldLabel(String text, JComponent target) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font("Medium", 14));
        label.setForeground(Theme.TEXT_LABEL);
        label.setLabelFor(target);
        return label;
    }

    /**
     * Etiqueta pequeña en mayúsculas sobre el título ("PASO 1 DE 2").
     *
     * @param text texto
     * @return etiqueta lista
     */
    public static JLabel eyebrow(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font("SemiBold", 13));
        label.setForeground(Theme.ACCENT);
        return label;
    }

    /**
     * Título grande de un formulario.
     *
     * @param text texto
     * @return etiqueta lista
     */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 34));
        label.setForeground(Theme.TEXT);
        return label;
    }

    /**
     * Texto gris bajo el título; se parte en varias líneas si no cabe.
     *
     * @param text      texto
     * @param wrapWidth ancho aproximado antes de partir la línea
     * @return etiqueta lista
     */
    public static JLabel subtitle(String text, int wrapWidth) {
        JLabel label = new JLabel("<html><div style='width:" + wrapWidth + "px'>" + text + "</div></html>");
        label.setFont(Theme.font(Font.PLAIN, 15));
        label.setForeground(Theme.TEXT_MUTED);
        return label;
    }

    /**
     * Botón principal verde, con un ícono a la derecha del texto.
     *
     * @param text  texto
     * @param icon  ícono (puede ser {@code null})
     * @param width ancho
     * @return botón listo
     */
    public static JButton primaryButton(String text, Icon icon, int width) {
        JButton button = new JButton(text);
        if (icon != null) {
            button.setIcon(icon);
            button.setHorizontalTextPosition(SwingConstants.LEADING);
            button.setIconTextGap(10);
        }
        button.setFont(Theme.font("SemiBold", 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0;"
                + "background: #0B7A69; foreground: #FFFFFF;"
                + "hoverBackground: #096A5B; pressedBackground: #08594C;"
                + "default.borderWidth: 0");
        fixSize(button, width, BUTTON_HEIGHT);
        return button;
    }

    /**
     * Botón secundario blanco con borde (por ejemplo "Atrás").
     *
     * @param text  texto
     * @param icon  ícono a la izquierda (puede ser {@code null})
     * @param width ancho
     * @return botón listo
     */
    public static JButton secondaryButton(String text, Icon icon, int width) {
        JButton button = new JButton(text, icon);
        button.setIconTextGap(8);
        button.setFont(Theme.font("SemiBold", 15));
        button.setForeground(Theme.TEXT_LABEL);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; focusWidth: 0; borderColor: #D6DEE4; hoverBorderColor: #9AA8B3;"
                + "background: #FFFFFF; hoverBackground: #F4F7F8; pressedBackground: #E9EEF1");
        fixSize(button, width, BUTTON_HEIGHT);
        return button;
    }

    /**
     * Botón con apariencia de enlace verde ("Regístrate", "Inicia sesión").
     *
     * @param text texto
     * @return botón listo
     */
    public static JButton linkButton(String text) {
        JButton link = new JButton(text);
        link.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        link.putClientProperty(FlatClientProperties.STYLE,
                "margin: 2,4,2,4; foreground: #0B7A69; hoverBackground: #E7F4F1; pressedBackground: #D3EBE6");
        link.setFont(Theme.font("SemiBold", 14));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return link;
    }

    /**
     * Línea centrada "pregunta + enlace" que va al pie de los formularios.
     *
     * @param question texto gris
     * @param link     enlace (creado con {@link #linkButton})
     * @return panel con ambos
     */
    public static JPanel questionLine(String question, JButton link) {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        line.setOpaque(false);
        JLabel label = new JLabel(question);
        label.setFont(Theme.font(Font.PLAIN, 14));
        label.setForeground(Theme.TEXT_MUTED);
        line.add(label);
        line.add(link);
        return line;
    }

    /**
     * Fija el tamaño preferido y máximo de un componente.
     *
     * @param c      componente
     * @param width  ancho
     * @param height alto
     */
    public static void fixSize(JComponent c, int width, int height) {
        c.setPreferredSize(new Dimension(width, height));
        c.setMaximumSize(new Dimension(width, height));
        c.setMinimumSize(new Dimension(Math.min(width, 120), height));
    }

    /**
     * Prepara un componente para una columna de {@code BoxLayout}: lo alinea a
     * la izquierda y no lo deja crecer más que el ancho del formulario.
     *
     * @param c     componente
     * @param width ancho del formulario
     * @return el mismo componente
     */
    public static Component row(JComponent c, int width) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c.getMaximumSize().width > width) {
            c.setMaximumSize(new Dimension(width, c.getPreferredSize().height));
        }
        if (c instanceof JPanel) {
            c.setPreferredSize(new Dimension(width, c.getPreferredSize().height));
        }
        return c;
    }

    /**
     * Color de fondo para la clasificación del IMC.
     *
     * @param label nombre de la clasificación ("Bajo peso", "Normal", "Sobrepeso", "Obesidad")
     * @return color de la etiqueta
     */
    public static Color bmiColor(String label) {
        if (label == null) {
            return Theme.TEXT_MUTED;
        }
        return switch (label) {
            case "Normal", "Peso normal" -> Theme.ACCENT;
            case "Bajo peso" -> new Color(0x2F5F9E);
            case "Sobrepeso" -> new Color(0xA35200);
            default -> new Color(0xB3261E);
        };
    }
}
