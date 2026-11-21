package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nutritrackcore.model.User;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.LinearGradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Menú lateral de la aplicación.
 * <p>
 * Arriba está el logo de NutriTrack, que funciona como botón de hamburguesa:
 * al pulsarlo el menú se contrae y quedan solo los íconos (con su nombre como
 * texto de ayuda al pasar el mouse); al pulsarlo otra vez se expande.
 * Abajo muestra el usuario con la sesión iniciada y el botón para salir.
 */
public class SidebarPanel extends JPanel {

    /** Ancho del menú expandido. */
    public static final int EXPANDED_WIDTH = 248;
    /** Ancho del menú contraído (solo íconos). */
    public static final int COLLAPSED_WIDTH = 76;

    private static final Color MUTED = new Color(0xC9D6DC);
    private static final Color SELECTED_BG = new Color(52, 214, 181, 46);
    private static final Color HOVER_BG = new Color(255, 255, 255, 22);

    private final Consumer<String> onNavigate;
    private final List<NavButton> items = new ArrayList<>();
    private final JButton toggleButton = new JButton();
    private final JLabel userName = new JLabel();
    private final JLabel userEmail = new JLabel();
    private final JPanel userTexts = new JPanel();
    private final Avatar avatar = new Avatar();
    private final NavButton logoutButton;

    private boolean expanded = true;

    /**
     * Crea el menú.
     *
     * @param onNavigate qué hacer al elegir una sección (recibe su nombre, ver {@link AppShellPanel})
     * @param onLogout   qué hacer al pulsar "Cerrar sesión"
     */
    public SidebarPanel(Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        setOpaque(true);
        setBackground(Theme.DARK_BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 14, 20, 14));

        createToggle();
        add(toggleButton);
        add(Box.createVerticalStrut(28));

        addItem(AppShellPanel.HOME, "Inicio", Icons::home);
        addItem(AppShellPanel.PROFILE, "Mi perfil", Icons::user);
        addItem(AppShellPanel.CONDITIONS, "Condiciones médicas", Icons::heartPulse);
        addItem(AppShellPanel.CONSUMPTION, "Registrar consumo", Icons::plusCircle);
        addItem(AppShellPanel.DIET, "Mi dieta", Icons::utensils);
        addItem(AppShellPanel.ROUTINE, "Mi rutina", Icons::dumbbell);
        addItem(AppShellPanel.PROGRESS, "Mi progreso", Icons::chart);

        add(Box.createVerticalGlue());
        add(createUserBox());
        add(Box.createVerticalStrut(10));
        logoutButton = new NavButton("Cerrar sesión", Icons::logout);
        logoutButton.addActionListener(e -> onLogout.run());
        add(alignLeft(logoutButton));

        applyExpanded();
    }

    /**
     * Muestra los datos del usuario que inició sesión.
     *
     * @param user usuario autenticado
     */
    public void setUser(User user) {
        userName.setText(user.getName());
        userEmail.setText(user.getEmail());
        avatar.setInitials(initials(user.getName()));
    }

    /**
     * Marca como activa una sección del menú.
     *
     * @param section nombre de la sección
     */
    public void select(String section) {
        for (NavButton item : items) {
            item.setSelected(section.equals(item.section));
        }
    }

    /** @return {@code true} si el menú está expandido */
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH, super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            Images.paintCover(g2, Images.get(Images.MENU_BACKGROUND), 0, 0, w, h, 0.5);
            // Capa oscura para que los textos se lean sobre la foto (un solo degradado, sin cortes).
            g2.setPaint(new LinearGradientPaint(0f, 0f, 0f, Math.max(1, h), new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(8, 32, 40, 238), new Color(8, 32, 40, 220), new Color(8, 32, 40, 240)}));
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ armado

    private void createToggle() {
        toggleButton.setIcon(new LogoIcon(40));
        toggleButton.setIconTextGap(12);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setFont(Theme.font(Font.BOLD, 22));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        toggleButton.putClientProperty(FlatClientProperties.STYLE,
                "margin: 4,6,4,6; arc: 12; hoverBackground: #FFFFFF16; pressedBackground: #FFFFFF26; focusWidth: 0");
        toggleButton.addActionListener(e -> {
            expanded = !expanded;
            applyExpanded();
        });
        alignLeft(toggleButton);
        toggleButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
    }

    private void addItem(String section, String label, BiFunction<Integer, Color, Icon> icon) {
        NavButton item = new NavButton(label, icon);
        item.section = section;
        item.addActionListener(e -> {
            select(section);
            onNavigate.accept(section);
        });
        items.add(item);
        add(alignLeft(item));
        add(Box.createVerticalStrut(4));
    }

    private JComponent createUserBox() {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(201, 214, 220, 50)),
                BorderFactory.createEmptyBorder(16, 4, 0, 0)));

        userTexts.setOpaque(false);
        userTexts.setLayout(new BoxLayout(userTexts, BoxLayout.Y_AXIS));
        userName.setFont(Theme.font("SemiBold", 14));
        userName.setForeground(Color.WHITE);
        userEmail.setFont(Theme.font(Font.PLAIN, 12));
        userEmail.setForeground(MUTED);
        userTexts.add(userName);
        userTexts.add(userEmail);

        box.add(avatar);
        box.add(Box.createHorizontalStrut(12));
        box.add(userTexts);
        alignLeft(box);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        return box;
    }

    private void applyExpanded() {
        toggleButton.setText(expanded ? "<html>Nutri<span style='color:#34D6B5'>Track</span></html>" : null);
        String hint = expanded ? "Contraer menú" : "Expandir menú";
        toggleButton.setToolTipText(hint);
        toggleButton.getAccessibleContext().setAccessibleName(hint);
        for (NavButton item : items) {
            item.setExpanded(expanded);
        }
        logoutButton.setExpanded(expanded);
        userTexts.setVisible(expanded);
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    private static <T extends JComponent> T alignLeft(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    // ------------------------------------------------------------------ piezas

    /** Opción del menú: ícono + texto, con fondo verde suave cuando está activa. */
    private static final class NavButton extends JButton {

        private final String label;
        private final BiFunction<Integer, Color, Icon> iconFactory;
        private String section;
        private boolean active;

        NavButton(String label, BiFunction<Integer, Color, Icon> iconFactory) {
            this.label = label;
            this.iconFactory = iconFactory;
            setFont(Theme.font("Medium", 14));
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(12);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setPreferredSize(new Dimension(EXPANDED_WIDTH - 28, 44));
            getAccessibleContext().setAccessibleName(label);
            refreshStyle();
        }

        void setExpanded(boolean expanded) {
            setText(expanded ? label : null);
            setToolTipText(expanded ? null : label);
        }

        @Override
        public void setSelected(boolean selected) {
            this.active = selected;
            refreshStyle();
        }

        private void refreshStyle() {
            Color color = active ? Theme.ACCENT_LIGHT : MUTED;
            setIcon(iconFactory.apply(20, color));
            setForeground(active ? Color.WHITE : MUTED);
            setFont(active ? Theme.font("SemiBold", 14) : Theme.font("Medium", 14));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color bg = active ? SELECTED_BG : (getModel().isRollover() ? HOVER_BG : null);
            if (bg != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }

    /** Cuadro verde redondeado con la hoja del logo. */
    private static final class LogoIcon implements Icon {

        private final int size;

        LogoIcon(int size) {
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.LOGO);
                g2.fillRoundRect(x, y, size, size, 24, 24);
                Icon leaf = Icons.leaf(22, Color.WHITE);
                leaf.paintIcon(c, g2, x + (size - 22) / 2, y + (size - 22) / 2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Círculo con las iniciales del usuario. */
    private static final class Avatar extends JComponent {

        private String initials = "";

        Avatar() {
            Dimension d = new Dimension(40, 40);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        void setInitials(String initials) {
            this.initials = initials;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT_LIGHT);
                g2.fillOval(0, 0, 40, 40);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Theme.DARK_BG);
                g2.setFont(Theme.font(Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (40 - fm.stringWidth(initials)) / 2, (40 - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }
    }
}
