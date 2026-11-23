package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Piezas visuales copiadas del prototipo de diseño: tarjetas con barra de
 * título (verde, azul rey o dorada), fotos recortadas, grillas de opciones,
 * contadores con − y +, selectores tipo píldora y botones planos.
 */
public final class Proto {

    /** Verde principal de la aplicación. */
    public static final Color GREEN = new Color(0x0B7A69);
    /** Verde claro de fondos suaves. */
    public static final Color GREEN_SOFT = new Color(0xE6F3F0);
    /** Azul rey del formulario de hábitos. */
    public static final Color ROYAL = new Color(0x2F5BD3);
    /** Fondo de los botones − y + en azul. */
    public static final Color ROYAL_SOFT = new Color(0xEEF2FC);
    /** Borde dorado de las tarjetas de progreso. */
    public static final Color GOLD_BORDER = new Color(0xD9A520);
    /** Texto principal. */
    public static final Color INK = new Color(0x0F1F2A);
    /** Texto de etiquetas. */
    public static final Color INK_2 = new Color(0x1E2D38);
    /** Texto secundario. */
    public static final Color MUTED = new Color(0x4F5E6B);
    /** Texto de notas. */
    public static final Color NOTE = new Color(0x5C6B77);
    /** Bordes de campos. */
    public static final Color FIELD_BORDER = new Color(0xD6DEE4);
    /** Separadores de filas. */
    public static final Color LINE = new Color(0xEEF2F4);
    /** Rojo de acciones peligrosas. */
    public static final Color DANGER = new Color(0xB4372A);
    /** Texto claro sobre el fondo azul. */
    public static final Color ON_BLUE = new Color(0xD9F1F4);

    private Proto() {
    }

    // ------------------------------------------------------------------ textos

    /** Etiqueta con fuente Poppins del peso pedido ("Medium", "SemiBold" o null = normal). */
    public static JLabel text(String text, String weight, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(weight, size));
        label.setForeground(color);
        return label;
    }

    /** Fuente Poppins: null o "Regular" = normal, "Bold" = negrita, otra = variante. */
    public static Font font(String weight, float size) {
        if (weight == null || weight.equals("Regular")) {
            return Theme.font(Font.PLAIN, size);
        }
        if (weight.equals("Bold")) {
            return Theme.font(Font.BOLD, size);
        }
        return Theme.font(weight, size);
    }

    /** Texto que se parte en varias líneas dentro del ancho dado. */
    public static JLabel wrapped(String text, int width, String weight, float size, Color color) {
        JLabel label = text("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>", weight,
                size, color);
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }

    /** Escapa los caracteres especiales de HTML. */
    public static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Título blanco de pantalla con subtítulo (32 px, como el prototipo). */
    public static JPanel pageTitle(String title, String subtitle) {
        JPanel box = column();
        box.add(left(text(title, "Bold", 32, Color.WHITE)));
        if (subtitle != null) {
            box.add(left(text(subtitle, null, 14, ON_BLUE)));
        }
        return box;
    }

    // ------------------------------------------------------------------ contenedores

    /** Panel transparente en columna. */
    public static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    /** Fila transparente de izquierda a derecha. */
    public static JPanel row(int gap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        return p;
    }

    /** Alinea a la izquierda dentro de un BoxLayout vertical. */
    public static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Fija el alto máximo al preferido para que BoxLayout no lo estire. */
    public static <T extends JComponent> T noStretch(T c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /**
     * Panel con fondo redondeado y, opcionalmente, borde.
     */
    public static class Rounded extends JPanel {

        private final Color fill;
        private final Color border;
        private final float borderWidth;
        private final int arc;

        /**
         * @param fill        color de fondo (null = sin fondo)
         * @param border      color del borde (null = sin borde)
         * @param borderWidth grosor del borde
         * @param arc         redondeo de las esquinas
         */
        public Rounded(Color fill, Color border, float borderWidth, int arc) {
            this.fill = fill;
            this.border = border;
            this.borderWidth = borderWidth;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            try {
                RoundRectangle2D r = new RoundRectangle2D.Double(borderWidth / 2, borderWidth / 2,
                        getWidth() - borderWidth, getHeight() - borderWidth, arc, arc);
                if (fill != null) {
                    g2.setColor(fill);
                    g2.fill(r);
                }
                if (border != null) {
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(borderWidth));
                    g2.draw(r);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Estilos de la barra de título de una {@link HeaderCard}. */
    public enum Bar {
        /** Barra verde (Mi perfil). */
        GREEN,
        /** Barra azul rey (Mis hábitos). */
        ROYAL,
        /** Barra dorada con degradado (Mi progreso). */
        GOLD,
        /** Barra roja (eliminar cuenta). */
        DANGER
    }

    /**
     * Tarjeta blanca con barra de título de color arriba, ícono, título y un
     * componente opcional a la derecha (botón "Editar", texto, etc.). El
     * contenido va en {@link #body()}.
     */
    public static class HeaderCard extends JPanel {

        private final Bar bar;
        private final int barHeight;
        private final JPanel body = new JPanel(new BorderLayout());
        private final JPanel header = new JPanel(new BorderLayout(10, 0));

        /**
         * @param title título de la barra
         * @param icon  ícono a la izquierda (puede ser null)
         * @param bar   estilo de la barra
         * @param right componente a la derecha de la barra (puede ser null)
         */
        public HeaderCard(String title, Icon icon, Bar bar, JComponent right) {
            super(new BorderLayout());
            this.bar = bar;
            this.barHeight = bar == Bar.GOLD ? 44 : 46;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(0, bar == Bar.GOLD ? 14 : 16, 0, 14));
            header.setPreferredSize(new Dimension(10, barHeight - 2));
            Color fg = bar == Bar.GOLD ? new Color(0x111111) : Color.WHITE;
            JLabel label = text(title, "SemiBold", 15, fg);
            if (icon != null) {
                label.setIcon(icon);
                label.setIconTextGap(10);
            }
            header.add(label, BorderLayout.CENTER);
            if (right != null) {
                JPanel holder = new JPanel(new java.awt.GridBagLayout());
                holder.setOpaque(false);
                holder.add(right);
                header.add(holder, BorderLayout.EAST);
            }
            body.setOpaque(false);
            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
            getAccessibleContext().setAccessibleName(title);
        }

        /** Panel donde va el contenido de la tarjeta. */
        public JPanel body() {
            return body;
        }

        /** Color de la barra según el estilo. */
        private Color borderColor() {
            return switch (bar) {
                case GREEN -> GREEN;
                case ROYAL -> ROYAL;
                case GOLD -> GOLD_BORDER;
                case DANGER -> DANGER;
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            try {
                int w = getWidth();
                int h = getHeight();
                int arc = 26;
                RoundRectangle2D shape = new RoundRectangle2D.Double(0.75, 0.75, w - 1.5, h - 1.5, arc, arc);
                g2.setColor(Color.WHITE);
                g2.fill(shape);
                Area top = new Area(shape);
                top.intersect(new Area(new Rectangle2D.Double(0, 0, w, barHeight)));
                if (bar == Bar.GOLD) {
                    g2.setPaint(new LinearGradientPaint(0, 0, w, barHeight * 0.36f,
                            new float[]{0f, 0.45f, 0.55f, 1f},
                            new Color[]{new Color(0xC9971A), new Color(0xF7D25A), new Color(0xFFE58A),
                                new Color(0xD9A520)}));
                } else {
                    g2.setColor(borderColor());
                }
                g2.fill(top);
                g2.setColor(borderColor());
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(shape);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ fotos

    /**
     * Foto que llena su espacio recortándose (como object-fit: cover). Puede
     * redondear las esquinas de arriba, de abajo o todas.
     */
    public static class Photo extends JPanel {

        private final Image image;
        private final double focusX;
        private final double focusY;
        private final int arcTop;
        private final int arcBottom;
        private boolean shade;

        /**
         * @param image     imagen
         * @param focusX    punto de enfoque horizontal (0 a 1)
         * @param focusY    punto de enfoque vertical (0 a 1)
         * @param height    alto fijo
         * @param arcTop    redondeo de las esquinas de arriba
         * @param arcBottom redondeo de las esquinas de abajo
         */
        public Photo(Image image, double focusX, double focusY, int height, int arcTop, int arcBottom) {
            super(new BorderLayout());
            this.image = image;
            this.focusX = focusX;
            this.focusY = focusY;
            this.arcTop = arcTop;
            this.arcBottom = arcBottom;
            setOpaque(false);
            setPreferredSize(new Dimension(100, height));
            setMinimumSize(new Dimension(10, height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        /** Oscurece la parte de abajo para que se lea el texto encima. */
        public Photo shaded() {
            this.shade = true;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            try {
                int w = getWidth();
                int h = getHeight();
                Area clip = new Area(new RoundRectangle2D.Double(0, 0, w, h + arcTop, arcTop, arcTop));
                clip.intersect(new Area(new Rectangle2D.Double(0, 0, w, h)));
                if (arcBottom > 0) {
                    Area bottom = new Area(new RoundRectangle2D.Double(0, -arcBottom, w, h + arcBottom, arcBottom,
                            arcBottom));
                    clip.intersect(bottom);
                }
                g2.setClip(clip);
                g2.setColor(GREEN_SOFT);
                g2.fillRect(0, 0, w, h);
                paintCover(g2, image, w, h, focusX, focusY);
                if (shade) {
                    g2.setPaint(new GradientPaint(0, h * 0.4f, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 128)));
                    g2.fillRect(0, 0, w, h);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Dibuja la imagen cubriendo w×h con el punto de enfoque dado. */
    static void paintCover(Graphics2D g2, Image image, int w, int h, double focusX, double focusY) {
        if (image == null || image.getWidth(null) <= 0) {
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int iw = image.getWidth(null);
        int ih = image.getHeight(null);
        double scale = Math.max((double) w / iw, (double) h / ih);
        int dw = (int) Math.ceil(iw * scale);
        int dh = (int) Math.ceil(ih * scale);
        g2.drawImage(image, (int) ((w - dw) * focusX), (int) ((h - dh) * focusY), dw, dh, null);
    }

    // ------------------------------------------------------------------ botones

    /**
     * Botón plano con colores propios.
     *
     * @param text   texto
     * @param bg     fondo (null = transparente)
     * @param fg     color del texto
     * @param border color del borde (null = sin borde)
     * @param height alto
     * @param size   tamaño de letra
     * @return botón listo
     */
    public static JButton button(String text, Color bg, Color fg, Color border, int height, float size) {
        JButton b = new JButton(text);
        b.setFont(font("SemiBold", size));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFocusPainted(false);
        String style = "arc: 20; focusWidth: 0; innerFocusWidth: 0; margin: 0,18,0,18;"
                + "foreground: " + hex(fg) + ";"
                + (bg == null ? "background: #FFFFFF00; hoverBackground: #FFFFFF22; pressedBackground: #FFFFFF33;"
                        : "background: " + hex(bg) + "; hoverBackground: " + hex(shade(bg, 0.92)) + ";"
                        + "pressedBackground: " + hex(shade(bg, 0.85)) + ";")
                + (border == null ? "borderWidth: 0;" : "borderWidth: 1; borderColor: " + hex(border) + ";");
        b.putClientProperty(FlatClientProperties.STYLE, style);
        if (bg == null && border == null) {
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
        }
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(d.width, height));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return b;
    }

    /**
     * Pone un ícono al botón y recalcula su ancho para que el texto no se corte.
     *
     * @param b        botón
     * @param icon     ícono
     * @param trailing si va después del texto (flecha →)
     * @return el mismo botón
     */
    public static JButton withIcon(JButton b, Icon icon, boolean trailing) {
        int h = b.getPreferredSize().height;
        b.setIcon(icon);
        b.setIconTextGap(8);
        if (trailing) {
            b.setHorizontalTextPosition(SwingConstants.LEADING);
        }
        b.setPreferredSize(null);
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(d.width + 4, h));
        return b;
    }

    /** Botón verde lleno. */
    public static JButton greenButton(String text) {
        return button(text, GREEN, Color.WHITE, null, 42, 14);
    }

    /** Botón blanco con borde verde. */
    public static JButton outlineButton(String text) {
        return button(text, Color.WHITE, GREEN, GREEN, 42, 14);
    }

    /** Botón blanco con borde gris (Cancelar). */
    public static JButton cancelButton(String text) {
        JButton b = button(text, Color.WHITE, INK_2, FIELD_BORDER, 42, 14);
        b.setFont(font(null, 14));
        return b;
    }

    /** Botón pequeño con borde blanco para poner en una barra de título. */
    public static JButton barButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 34));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                }
                g2.setColor(new Color(255, 255, 255, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font("SemiBold", 12));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        b.setPreferredSize(new Dimension(b.getFontMetrics(b.getFont()).stringWidth(text) + 20, 30));
        return b;
    }

    /** Enlace de texto (sin fondo). */
    public static JButton link(String text, Color color, float size) {
        JButton b = new JButton(text);
        b.setFont(font("SemiBold", size));
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ------------------------------------------------------------------ opciones

    /**
     * Grilla de botones de opción (uno elegido), con el estilo del prototipo:
     * 40 px de alto, esquinas de 10 px y relleno azul rey cuando está elegido.
     */
    public static class ChoiceGrid extends JPanel {

        private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();
        private final ButtonGroup group = new ButtonGroup();
        private final List<Runnable> listeners = new ArrayList<>();

        /**
         * @param columns número de columnas
         * @param options valor interno → texto visible
         * @param accent  color del botón elegido
         * @param size    tamaño de letra
         */
        public ChoiceGrid(int columns, Map<String, String> options, Color accent, float size) {
            super(new GridLayout(0, columns, 8, 8));
            setOpaque(false);
            for (Map.Entry<String, String> o : options.entrySet()) {
                JToggleButton b = new JToggleButton(o.getValue());
                b.setFont(font(null, size));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setFocusPainted(false);
                b.putClientProperty(FlatClientProperties.STYLE,
                        "arc: 20; focusWidth: 0; innerFocusWidth: 0; borderWidth: 1;"
                        + "background: #FFFFFF; borderColor: #D6DEE4; foreground: #1E2D38;"
                        + "hoverBackground: #F4F7FB; selectedBackground: " + hex(accent) + ";"
                        + "selectedForeground: #FFFFFF; pressedBackground: #EEF2F7; margin: 0,0,0,0");
                b.setPreferredSize(new Dimension(60, 40));
                b.addItemListener(e -> {
                    boolean on = b.isSelected();
                    b.setForeground(on ? Color.WHITE : INK_2);
                    b.setFont(font(on ? "SemiBold" : null, size));
                });
                b.addActionListener(e -> listeners.forEach(Runnable::run));
                b.getAccessibleContext().setAccessibleName(o.getValue());
                group.add(b);
                buttons.put(o.getKey(), b);
                add(b);
            }
        }

        /** Valor elegido o null. */
        public String getValue() {
            for (Map.Entry<String, JToggleButton> e : buttons.entrySet()) {
                if (e.getValue().isSelected()) {
                    return e.getKey();
                }
            }
            return null;
        }

        /** Elige la opción con ese valor (sin distinguir mayúsculas). */
        public void setValue(String value) {
            if (value == null) {
                return;
            }
            buttons.forEach((k, b) -> {
                if (k.equalsIgnoreCase(value)) {
                    b.setSelected(true);
                }
            });
        }

        /** Activa o desactiva una opción. */
        public void setOptionEnabled(String value, boolean enabled) {
            JToggleButton b = buttons.get(value);
            if (b != null) {
                b.setEnabled(enabled);
                if (!enabled && b.isSelected()) {
                    group.clearSelection();
                }
            }
        }

        /** Acción a ejecutar cuando el usuario elige otra opción. */
        public void onChange(Runnable r) {
            listeners.add(r);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * Contador con botones − y + a los lados (42 px de alto).
     */
    public static class Stepper extends JPanel {

        private double value;
        private final double min;
        private final double max;
        private final double step;
        private final boolean decimal;
        private final JLabel label = new JLabel("", SwingConstants.CENTER);
        private final List<Runnable> listeners = new ArrayList<>();

        /**
         * @param value   valor inicial
         * @param min     mínimo
         * @param max     máximo
         * @param step    cuánto suma o resta cada clic
         * @param decimal si se muestra con un decimal
         * @param fg      color de − y +
         * @param bg      fondo de − y +
         * @param name    nombre para lectores de pantalla
         */
        public Stepper(double value, double min, double max, double step, boolean decimal, Color fg, Color bg,
                       String name) {
            super(new BorderLayout());
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
            this.decimal = decimal;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            label.setFont(font("SemiBold", 15));
            label.setForeground(INK);
            add(side("−", fg, bg, -1, "Menos " + name), BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
            add(side("+", fg, bg, 1, "Más " + name), BorderLayout.EAST);
            setPreferredSize(new Dimension(160, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            update();
        }

        private JButton side(String sign, Color fg, Color bg, int dir, String name) {
            JButton b = new JButton(sign);
            b.setFont(font(null, 16));
            b.setForeground(fg);
            b.setBackground(bg);
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.putClientProperty(FlatClientProperties.STYLE, "arc: 0; focusWidth: 0; innerFocusWidth: 0;"
                    + "borderWidth: 0; background: " + hex(bg) + "; hoverBackground: " + hex(shade(bg, 0.94)));
            b.setPreferredSize(new Dimension(40, 40));
            b.getAccessibleContext().setAccessibleName(name);
            b.addActionListener(e -> {
                value = Math.max(min, Math.min(max, Math.round((value + dir * step) * 10) / 10.0));
                update();
                listeners.forEach(Runnable::run);
            });
            return b;
        }

        private void update() {
            label.setText(decimal ? String.format(ScreenKit.ES_CO, "%.1f", value) : String.valueOf((int) value));
        }

        /** Valor actual. */
        public double getValue() {
            return value;
        }

        /** Acción al cambiar el valor. */
        public void onChange(Runnable r) {
            listeners.add(r);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            try {
                RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(Color.WHITE);
                g2.fill(r);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            super.paintChildren(g2);
            g2.dispose();
            Graphics2D g3 = smooth(g);
            g3.setColor(FIELD_BORDER);
            g3.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 20, 20));
            g3.dispose();
        }
    }

    /**
     * Selector tipo píldora oscura con opciones; la elegida va en blanco con
     * texto verde (como "En casa / Gimnasio").
     */
    public static class Segmented extends JPanel {

        private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();
        private final List<Runnable> listeners = new ArrayList<>();

        /**
         * @param options valor → texto
         * @param caption texto pequeño antes de las opciones (puede ser null)
         */
        public Segmented(Map<String, String> options, String caption) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 4));
            setOpaque(false);
            if (caption != null) {
                JLabel c = text(caption, null, 11, ON_BLUE);
                c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                add(c);
            }
            ButtonGroup g = new ButtonGroup();
            for (Map.Entry<String, String> o : options.entrySet()) {
                JToggleButton b = new JToggleButton(o.getValue());
                b.setFont(font("SemiBold", 13));
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.putClientProperty(FlatClientProperties.STYLE, "arc: 18; focusWidth: 0; innerFocusWidth: 0;"
                        + "borderWidth: 0; background: #FFFFFF00; foreground: #FFFFFF; hoverBackground: #FFFFFF22;"
                        + "selectedBackground: #FFFFFF; selectedForeground: #0B7A69; margin: 0,18,0,18");
                b.setPreferredSize(new Dimension(b.getPreferredSize().width + 8, 38));
                b.addItemListener(e -> b.setForeground(b.isSelected() ? GREEN : Color.WHITE));
                b.addActionListener(e -> listeners.forEach(Runnable::run));
                g.add(b);
                buttons.put(o.getKey(), b);
                add(b);
            }
        }

        /** Elige una opción. */
        public void setValue(String value) {
            buttons.forEach((k, b) -> {
                if (k.equalsIgnoreCase(String.valueOf(value))) {
                    b.setSelected(true);
                    b.setForeground(GREEN);
                }
            });
        }

        /** Opción elegida. */
        public String getValue() {
            for (Map.Entry<String, JToggleButton> e : buttons.entrySet()) {
                if (e.getValue().isSelected()) {
                    return e.getKey();
                }
            }
            return null;
        }

        /** Acción al cambiar. */
        public void onChange(Runnable r) {
            listeners.add(r);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(new Color(8, 32, 40, 110));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
            g2.dispose();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Graphics con antialias. */
    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** Color en formato #RRGGBB (o #RRGGBBAA si es transparente). */
    public static String hex(Color c) {
        String s = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        return c.getAlpha() < 255 ? s + String.format("%02X", c.getAlpha()) : s;
    }

    /** Oscurece un color (factor menor que 1). */
    public static Color shade(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), c.getAlpha());
    }

    /** Arma un mapa ordenado de pares valor, texto. */
    public static Map<String, String> options(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}
