package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Piezas comunes de las pantallas internas (las que van al lado del menú
 * lateral): fondo azul, encabezado, textos, formatos de números y fechas, y
 * el estado vacío ("aún no tienes…").
 */
public final class ScreenKit {

    /** Configuración regional para números y fechas en español de Colombia. */
    public static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    /** Texto gris de apoyo. */
    public static final Color NOTE = new Color(0x5C6B77);
    /** Texto claro sobre el fondo azul. */
    public static final Color ON_BLUE = new Color(0xD9F1F4);

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_CO);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private ScreenKit() {
    }

    // ------------------------------------------------------------------ fondo y estructura

    /**
     * Panel con el fondo azul de las pantallas internas.
     */
    public static class BlueBackground extends JPanel {

        /**
         * Crea el panel.
         *
         * @param layout organización de su contenido
         */
        public BlueBackground(LayoutManager layout) {
            super(layout);
            setOpaque(true);
            setBackground(new Color(0x027690));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Images.paintCover(g2, Images.get(Images.HOME_BACKGROUND), 0, 0, getWidth(), getHeight(), 0.5);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Columna que se ajusta al ancho disponible y deja bajar con la rueda del
     * mouse cuando el contenido es más alto que la ventana.
     */
    public static class ScrollColumn extends JPanel implements Scrollable {

        /** Crea la columna (sin fondo, con los elementos uno debajo del otro). */
        public ScrollColumn() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height - 48;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Envuelve una columna en un panel desplazable transparente.
     *
     * @param column contenido
     * @return panel desplazable sin bordes ni fondo
     */
    public static JScrollPane transparentScroll(JComponent column) {
        JScrollPane scroll = new JScrollPane(column);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    // ------------------------------------------------------------------ encabezado

    /**
     * Encabezado de una sección: título grande en blanco, subtítulo y, a la
     * derecha, un botón blanco opcional.
     *
     * @param title    título
     * @param subtitle subtítulo (puede ser {@code null})
     * @param action   botón de la derecha (puede ser {@code null})
     * @return panel del encabezado
     */
    public static JComponent header(String title, String subtitle, JButton action) {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);
        JPanel texts = column();
        texts.add(label(title, Theme.font(Font.BOLD, 30), Color.WHITE));
        if (subtitle != null) {
            texts.add(Box.createVerticalStrut(4));
            texts.add(label(subtitle, Theme.font(Font.PLAIN, 14), ON_BLUE));
        }
        header.add(texts, BorderLayout.CENTER);
        if (action != null) {
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(action);
            header.add(right, BorderLayout.EAST);
        }
        return fixedHeight(header);
    }

    /**
     * Botón blanco con texto verde, para usar sobre el fondo azul.
     *
     * @param text texto
     * @param icon ícono a la izquierda (puede ser {@code null})
     * @return botón listo
     */
    public static JButton whiteButton(String text, Icon icon) {
        JButton button = new JButton(text, icon);
        button.setFont(Theme.font("SemiBold", 14));
        button.setForeground(Theme.ACCENT);
        button.setIconTextGap(8);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin: 0,18,0,18;"
                + "background: #FFFFFF; hoverBackground: #EEF7F5; pressedBackground: #DDEFEB");
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 44));
        return button;
    }

    /**
     * Enlace verde con una flecha dibujada a la derecha.
     *
     * @param text texto
     * @return botón con apariencia de enlace
     */
    public static JButton arrowLink(String text) {
        JButton link = FormKit.linkButton(text);
        link.setIcon(Icons.arrowRight(14, Theme.ACCENT));
        link.setHorizontalTextPosition(SwingConstants.LEADING);
        link.setIconTextGap(6);
        link.putClientProperty(FlatClientProperties.STYLE,
                "margin: 2,0,2,0; foreground: #0B7A69; hoverBackground: #E7F4F1; pressedBackground: #D3EBE6");
        return link;
    }

    /**
     * Tarjeta para cuando todavía no hay datos: foto, título, explicación y un botón.
     *
     * @param cover   foto de portada
     * @param focusY  parte vertical visible de la foto
     * @param title   título
     * @param text    explicación
     * @param action  botón principal
     * @return tarjeta lista
     */
    public static Card emptyState(String cover, double focusY, String title, String text, JButton action) {
        Card card = new Card(Images.get(cover), 200, focusY);
        card.setPadding(22, 28, 26, 28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(left(label(title, Theme.font("SemiBold", 20), Theme.TEXT)));
        card.add(Box.createVerticalStrut(6));
        card.add(left(label(text, Theme.font(Font.PLAIN, 14), Theme.TEXT_MUTED)));
        card.add(Box.createVerticalStrut(18));
        card.add(left(action));
        card.setMaximumSize(new Dimension(620, Integer.MAX_VALUE));
        return card;
    }

    // ------------------------------------------------------------------ textos y orden

    /** Crea una etiqueta con fuente y color. */
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /**
     * Etiqueta que parte el texto en varias líneas si no cabe.
     *
     * @param text      texto
     * @param wrapWidth ancho aproximado antes de partir la línea
     * @param font      fuente
     * @param color     color
     * @return etiqueta lista
     */
    public static JLabel wrapped(String text, int wrapWidth, Font font, Color color) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return label("<html><div style='width:" + wrapWidth + "px'>" + safe + "</div></html>", font, color);
    }

    /** Panel transparente con los elementos uno debajo del otro. */
    public static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    /** Alinea a la izquierda dentro de una columna. */
    public static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Alinea a la izquierda y evita que se estire a lo alto. */
    public static <T extends JComponent> T fixedHeight(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // ------------------------------------------------------------------ formatos

    /** Número entero con punto de miles (1.734). */
    public static String number(double value) {
        return NumberFormat.getIntegerInstance(ES_CO).format(Math.round(value));
    }

    /** Número con un decimal y coma (26,6). */
    public static String decimal(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(ES_CO);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(value);
    }

    /** Fecha larga ("28 de octubre"). */
    public static String longDate(LocalDate date) {
        return date == null ? "" : date.format(LONG_DATE);
    }

    /** Fecha corta ("02/05/2024"). */
    public static String shortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE);
    }

    /**
     * Une nombres en español: "A", "A y B", "A, B y C".
     *
     * @param names nombres
     * @return texto unido
     */
    public static String joinNames(java.util.List<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " y " + names.get(names.size() - 1);
    }
}
