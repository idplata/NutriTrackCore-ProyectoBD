package com.nutritrackcore.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel oscuro de bienvenida con la foto de fondo, el logo, un titular y una
 * lista de puntos destacados. Lo usan el inicio de sesión y el registro, cada
 * uno con sus propios textos.
 */
public class BrandPanel extends JPanel {

    /** Ruta de la foto de fondo dentro de {@code resources}. */
    private static final String HERO_IMAGE = "/images/login-hero.jpg";

    /** Qué parte vertical de la foto se mantiene visible al recortarla (0 = arriba, 1 = abajo). */
    private static final double FOCUS_Y = 0.40;

    private final Image background;

    /**
     * Un punto destacado: ícono, título y descripción corta.
     *
     * @param icon        ícono que va en el círculo de la izquierda
     * @param title       título en negrita
     * @param description texto secundario
     */
    public record Feature(Icon icon, String title, String description) {
    }

    /**
     * Crea el panel.
     *
     * @param headline       primera parte del titular (en blanco); puede tener saltos con {@code \n}
     * @param headlineAccent última parte del titular, resaltada en verde
     * @param paragraph      párrafo bajo el titular
     * @param features       puntos destacados que se listan abajo
     */
    public BrandPanel(String headline, String headlineAccent, String paragraph, List<Feature> features) {
        java.net.URL url = BrandPanel.class.getResource(HERO_IMAGE);
        this.background = url == null ? null : new ImageIcon(url).getImage();

        setOpaque(true);
        setBackground(Theme.DARK_BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(56, 64, 52, 64));

        add(left(createLogo()));
        add(Box.createVerticalGlue());

        JLabel title = new JLabel("<html>" + escape(headline).replace("\n", "<br>")
                + "<br><span style='color:#34D6B5'>" + escape(headlineAccent) + "</span></html>");
        title.setFont(Theme.font(Font.BOLD, 42));
        title.setForeground(Theme.ON_DARK);
        add(left(title));
        add(Box.createVerticalStrut(18));

        JLabel text = new JLabel("<html><div style='width:330px'>" + escape(paragraph) + "</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 15));
        text.setForeground(new Color(0xD5E0E5));
        add(left(text));
        add(Box.createVerticalStrut(36));

        for (int i = 0; i < features.size(); i++) {
            if (i > 0) {
                add(Box.createVerticalStrut(20));
            }
            add(left(createFeature(features.get(i))));
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(420, 600);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int w = getWidth();
            int h = getHeight();
            if (background != null && background.getWidth(null) > 0) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int iw = background.getWidth(null);
                int ih = background.getHeight(null);
                double scale = Math.max((double) w / iw, (double) h / ih);   // "cover": llena sin deformar
                int dw = (int) Math.ceil(iw * scale);
                int dh = (int) Math.ceil(ih * scale);
                int dx = (w - dw) / 2;
                int dy = (int) ((h - dh) * FOCUS_Y);
                g2.drawImage(background, dx, dy, dw, dh, null);
            }
            // Capa oscura para que el texto blanco se lea sobre la foto.
            g2.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(w, h * 0.18f),
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{new Color(8, 32, 40, 240), new Color(8, 32, 40, 220), new Color(8, 32, 40, 150)}));
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }

    private JPanel createLogo() {
        JPanel row = transparentRow(14);

        JComponent badge = new RoundBadge(48, 14, Theme.LOGO, Icons.leaf(28, Color.WHITE));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("<html>Nutri<span style='color:#34D6B5'>Track</span></html>");
        name.setFont(Theme.font(Font.BOLD, 28));
        name.setForeground(Theme.ON_DARK);
        JLabel slogan = new JLabel("Tu nutrición, bajo control");
        slogan.setFont(Theme.font(Font.PLAIN, 13));
        slogan.setForeground(Theme.ON_DARK_MUTED);
        texts.add(name);
        texts.add(slogan);

        row.add(badge);
        row.add(texts);
        return row;
    }

    private JPanel createFeature(Feature feature) {
        JPanel row = transparentRow(16);
        row.add(new RoundBadge(48, 48, new Color(52, 214, 181, 40), feature.icon()));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(feature.title());
        title.setFont(Theme.font("SemiBold", 15));
        title.setForeground(Theme.ON_DARK);
        JLabel desc = new JLabel(feature.description());
        desc.setFont(Theme.font(Font.PLAIN, 13));
        desc.setForeground(Theme.ON_DARK_MUTED);
        texts.add(title);
        texts.add(Box.createVerticalStrut(2));
        texts.add(desc);
        row.add(texts);
        return row;
    }

    private static JPanel transparentRow(int gap) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        row.setOpaque(false);
        // FlowLayout agrega el espacio también antes del primer componente; se compensa.
        row.setBorder(BorderFactory.createEmptyBorder(0, -gap, 0, 0));
        return row;
    }

    private static Component left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Cuadro (o círculo) de color con un ícono centrado. */
    static final class RoundBadge extends JComponent {

        private final int size;
        private final int arc;
        private final Color fill;
        private final Icon icon;

        RoundBadge(int size, int arc, Color fill, Icon icon) {
            this.size = size;
            this.arc = arc;
            this.fill = fill;
            this.icon = icon;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, size, size, arc, arc);
                if (icon != null) {
                    icon.paintIcon(this, g2, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
