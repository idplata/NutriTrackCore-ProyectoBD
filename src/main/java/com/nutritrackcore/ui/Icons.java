package com.nutritrackcore.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.Icon;

/**
 * Íconos de línea dibujados con Java2D (sin archivos de imagen).
 * <p>
 * Cada ícono se define en una cuadrícula de 24×24 y se escala al tamaño
 * pedido, así se ve nítido en cualquier pantalla.
 */
public final class Icons {

    private Icons() {
    }

    /** Sobre de correo. */
    public static Icon mail(int size, Color color) {
        return new LineIcon(size, color, 1.8f, List.of(
                new RoundRectangle2D.Double(2, 4, 20, 16, 4, 4),
                polyline(22, 7, 12, 13, 2, 7)));
    }

    /** Candado. */
    public static Icon lock(int size, Color color) {
        Path2D shackle = new Path2D.Double();
        shackle.moveTo(8, 11);
        shackle.lineTo(8, 7);
        shackle.append(new Arc2D.Double(8, 3, 8, 8, 180, -180, Arc2D.OPEN), true);
        shackle.lineTo(16, 11);
        return new LineIcon(size, color, 1.8f, List.of(
                new RoundRectangle2D.Double(4, 11, 16, 10, 4, 4), shackle));
    }

    /** Persona (silueta). */
    public static Icon user(int size, Color color) {
        return new LineIcon(size, color, 1.8f, List.of(
                new Ellipse2D.Double(8, 4, 8, 8),
                new Arc2D.Double(4, 13, 16, 16, 0, 180, Arc2D.OPEN)));
    }

    /** Flecha hacia la derecha. */
    public static Icon arrowRight(int size, Color color) {
        return new LineIcon(size, color, 2.2f, List.of(
                new Line2D.Double(5, 12, 19, 12),
                polyline(13, 6, 19, 12, 13, 18)));
    }

    /** Hoja (logo de NutriTrack). */
    public static Icon leaf(int size, Color color) {
        Path2D leaf = new Path2D.Double();
        leaf.moveTo(5, 19);
        leaf.curveTo(4, 10, 11, 4, 20, 4);
        leaf.curveTo(21, 13, 15, 20, 5, 19);
        leaf.closePath();
        return new LineIcon(size, color, 2f, List.of(leaf, new Line2D.Double(3, 21, 13, 11)));
    }

    /** Mancuerna. */
    public static Icon dumbbell(int size, Color color) {
        return new LineIcon(size, color, 2f, List.of(
                new Line2D.Double(6.5, 6.5, 6.5, 17.5),
                new Line2D.Double(17.5, 6.5, 17.5, 17.5),
                new Line2D.Double(3, 9, 3, 15),
                new Line2D.Double(21, 9, 21, 15),
                new Line2D.Double(3, 12, 21, 12)));
    }

    /** Tenedor y cuchillo. */
    public static Icon utensils(int size, Color color) {
        Path2D fork = new Path2D.Double();
        fork.moveTo(3, 2);
        fork.lineTo(3, 9);
        fork.quadTo(3, 11, 5, 11);
        fork.lineTo(9, 11);
        fork.quadTo(11, 11, 11, 9);
        fork.lineTo(11, 2);
        Path2D knife = new Path2D.Double();
        knife.moveTo(21, 22);
        knife.lineTo(21, 2);
        knife.quadTo(16, 3, 16, 8);
        knife.lineTo(16, 13);
        knife.quadTo(16, 15, 18, 15);
        knife.lineTo(21, 15);
        return new LineIcon(size, color, 2f, List.of(fork, new Line2D.Double(7, 2, 7, 22), knife));
    }

    /** Gráfica de tendencia. */
    public static Icon chart(int size, Color color) {
        return new LineIcon(size, color, 2f, List.of(
                polyline(3, 3, 3, 21, 21, 21),
                polyline(7, 15, 11, 11, 14, 14, 20, 8),
                polyline(16, 8, 20, 8, 20, 12)));
    }

    private static Path2D polyline(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    /** Ícono hecho de trazos sobre una cuadrícula de 24×24. */
    private static final class LineIcon implements Icon {

        private final int size;
        private final Color color;
        private final float strokeWidth;
        private final List<Shape> shapes;

        LineIcon(int size, Color color, float strokeWidth, List<Shape> shapes) {
            this.size = size;
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.shapes = shapes;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double scale = size / 24.0;
                g2.scale(scale, scale);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (Shape s : shapes) {
                    g2.draw(s);
                }
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
}
