package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.UserCondition;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Pantalla de condiciones médicas.
 * <p>
 * A la izquierda muestra el catálogo como tarjetas que se marcan o desmarcan;
 * a la derecha, las condiciones del usuario con su fecha de diagnóstico.
 * Agregar llama a {@code MedicalConditionService.assign} y quitar a
 * {@code MedicalConditionService.remove}.
 */
public class ConditionsPanel extends ScreenKit.BlueBackground implements Refreshable {

    private final AppServices services;
    private final Consumer<String> navigate;
    private final JPanel body = new JPanel(new GridBagLayout());

    /**
     * Crea la pantalla.
     *
     * @param services servicios de la aplicación
     * @param navigate cómo ir a otra sección
     */
    public ConditionsPanel(AppServices services, Consumer<String> navigate) {
        super(new BorderLayout(0, 22));
        this.services = services;
        this.navigate = navigate;
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        add(ScreenKit.header("Condiciones médicas",
                "Elige las que tienes. Las usamos para ajustar tu dieta y tu rutina.", null), BorderLayout.NORTH);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        body.removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        int userId = services.session().getCurrentUser().getIdUser();
        try {
            List<MedicalCondition> catalog = services.conditions().listCatalog();
            List<MedicalCondition> mine = services.conditions().listByUser(userId);
            List<UserCondition> assignments = services.conditions().listAssignments(userId);

            Map<Integer, UserCondition> byId = new HashMap<>();
            for (UserCondition uc : assignments) {
                byId.put(uc.getConditionId(), uc);
            }

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 0;
            c.weightx = 1.55;
            c.insets = new Insets(0, 0, 0, 18);
            body.add(createCatalogCard(catalog, byId), c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            body.add(createMineCard(mine, byId), c);
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        body.revalidate();
        body.repaint();
    }

    // ------------------------------------------------------------------ catálogo

    private JComponent createCatalogCard(List<MedicalCondition> catalog, Map<Integer, UserCondition> mine) {
        Card card = new Card();
        card.setPadding(22, 24, 22, 24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(ScreenKit.left(ScreenKit.label("Elige tus condiciones", Theme.font("SemiBold", 18), Theme.TEXT)));
        card.add(Box.createVerticalStrut(2));
        card.add(ScreenKit.left(ScreenKit.label("Pulsa una para agregarla o quitarla.",
                Theme.font(Font.PLAIN, 13), ScreenKit.NOTE)));
        card.add(Box.createVerticalStrut(14));

        if (catalog.isEmpty()) {
            card.add(ScreenKit.left(ScreenKit.wrapped("Todavía no hay condiciones cargadas en el sistema.", 360,
                    Theme.font(Font.PLAIN, 14), Theme.TEXT_MUTED)));
            return card;
        }

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        for (MedicalCondition condition : catalog) {
            boolean selected = mine.containsKey(condition.getConditionId());
            ConditionTile tile = new ConditionTile(condition, selected);
            tile.addActionListener(e -> {
                if (selected) {
                    removeCondition(condition.getConditionId(), condition.getName());
                } else {
                    addCondition(condition);
                }
            });
            grid.add(tile);
        }
        card.add(ScreenKit.fixedHeight(grid));
        card.add(Box.createVerticalStrut(14));

        JButton none = FormKit.secondaryButton("No tengo ninguna condición", null, 10);
        none.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; focusWidth: 0; borderColor: #9AA8B3; background: #FFFFFF;"
                + "hoverBackground: #F4F7F8; pressedBackground: #E9EEF1");
        none.setFont(Theme.font("Medium", 14));
        none.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        none.setPreferredSize(new Dimension(200, 48));
        none.addActionListener(e -> removeAll(mine));
        card.add(ScreenKit.left(none));
        card.add(Box.createVerticalGlue());
        return card;
    }

    // ------------------------------------------------------------------ las del usuario

    private JComponent createMineCard(List<MedicalCondition> mine, Map<Integer, UserCondition> assignments) {
        Card card = new Card(Images.get(Images.CARD_CONDITIONS), 120, 0.28);
        card.setPadding(16, 22, 20, 22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(ScreenKit.left(ScreenKit.label("Tus condiciones (" + mine.size() + ")",
                Theme.font("SemiBold", 18), Theme.TEXT)));
        card.add(Box.createVerticalStrut(8));

        if (mine.isEmpty()) {
            card.add(ScreenKit.left(ScreenKit.wrapped("No tienes condiciones registradas. Tu dieta y tu rutina no "
                    + "tendrán restricciones especiales.", 260, Theme.font(Font.PLAIN, 14), Theme.TEXT_MUTED)));
        }
        for (MedicalCondition condition : mine) {
            card.add(ScreenKit.fixedHeight(conditionRow(condition, assignments.get(condition.getConditionId()))));
        }
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(12));
        card.add(ScreenKit.fixedHeight(createNote()));
        return card;
    }

    private JComponent conditionRow(MedicalCondition condition, UserCondition assignment) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEEF2F4)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JPanel texts = ScreenKit.column();
        texts.add(ScreenKit.label(condition.getName(), Theme.font("SemiBold", 14), Theme.TEXT));
        String date = assignment != null && assignment.getDiagnosisDate() != null
                ? "Diagnosticada el " + ScreenKit.shortDate(assignment.getDiagnosisDate())
                : "Sin fecha de diagnóstico";
        texts.add(ScreenKit.label(date, Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
        if (assignment != null && assignment.getObservations() != null) {
            texts.add(ScreenKit.wrapped(assignment.getObservations(), 200, Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
        }

        JButton remove = FormKit.secondaryButton("Quitar", null, 80);
        remove.setFont(Theme.font("Medium", 13));
        FormKit.fixSize(remove, 80, 32);
        remove.getAccessibleContext().setAccessibleName("Quitar " + condition.getName());
        remove.addActionListener(e -> removeCondition(condition.getConditionId(), condition.getName()));
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(remove);

        row.add(texts, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent createNote() {
        JPanel note = new RoundedBox(new Color(0xFFF4E5));
        note.setLayout(new BoxLayout(note, BoxLayout.Y_AXIS));
        note.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        note.add(ScreenKit.left(ScreenKit.wrapped("Si cambias tus condiciones, genera de nuevo tu dieta y tu rutina "
                + "para que se ajusten.", 250, Theme.font(Font.PLAIN, 13), new Color(0x7A3E00))));
        note.add(Box.createVerticalStrut(4));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        links.setOpaque(false);
        links.setBorder(BorderFactory.createEmptyBorder(0, -14, 0, 0));
        JButton diet = ScreenKit.arrowLink("Ir a Mi dieta");
        diet.addActionListener(e -> navigate.accept(AppShellPanel.DIET));
        JButton routine = ScreenKit.arrowLink("Ir a Mi rutina");
        routine.addActionListener(e -> navigate.accept(AppShellPanel.ROUTINE));
        links.add(diet);
        links.add(routine);
        note.add(ScreenKit.left(links));
        return note;
    }

    // ------------------------------------------------------------------ acciones

    private void addCondition(MedicalCondition condition) {
        AddConditionDialog data = AddConditionDialog.ask(this, condition.getName());
        if (data == null) {
            return;
        }
        int userId = services.session().getCurrentUser().getIdUser();
        try {
            if (data.getDiagnosisDate() == null) {
                services.conditions().assign(userId, condition.getConditionId());
            } else {
                services.conditions().assign(userId, condition.getConditionId(), data.getDiagnosisDate(),
                        data.getNotes());
            }
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void removeCondition(int conditionId, String name) {
        if (!Messages.confirm(this, "¿Quitar \"" + name + "\" de tus condiciones?")) {
            return;
        }
        try {
            services.conditions().remove(services.session().getCurrentUser().getIdUser(), conditionId);
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void removeAll(Map<Integer, UserCondition> mine) {
        if (mine.isEmpty()) {
            Messages.showInfo(this, "Listo. No tienes condiciones registradas.");
            return;
        }
        if (!Messages.confirm(this, "¿Quitar todas tus condiciones médicas?")) {
            return;
        }
        int userId = services.session().getCurrentUser().getIdUser();
        try {
            for (Integer conditionId : mine.keySet()) {
                services.conditions().remove(userId, conditionId);
            }
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    // ------------------------------------------------------------------ piezas

    /** Tarjeta del catálogo: nombre, descripción y un círculo que se marca. */
    private static final class ConditionTile extends JButton {

        private final boolean selected;

        ConditionTile(MedicalCondition condition, boolean selected) {
            this.selected = selected;
            setLayout(new BorderLayout(12, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 14));
            setPreferredSize(new Dimension(200, 70));
            getAccessibleContext().setAccessibleName(condition.getName()
                    + (selected ? ", agregada. Pulsa para quitarla" : ". Pulsa para agregarla"));

            JPanel texts = ScreenKit.column();
            texts.add(ScreenKit.label(condition.getName(), Theme.font("SemiBold", 15), Theme.TEXT));
            String description = condition.getDescription() == null ? "" : condition.getDescription();
            texts.add(ScreenKit.label(description, Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(false);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            center.add(texts, gc);

            add(center, BorderLayout.CENTER);
            add(new CheckCircle(selected), BorderLayout.EAST);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                g2.setColor(selected ? new Color(0xF1F8F6) : (hover ? new Color(0xF7F9FA) : Color.WHITE));
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 24, 24);
                g2.setColor(selected ? Theme.ACCENT : (hover ? new Color(0x9AA8B3) : Theme.BORDER));
                g2.setStroke(new BasicStroke(selected ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Círculo vacío o verde con chulo. */
    private static final class CheckCircle extends JPanel {

        private final boolean checked;

        CheckCircle(boolean checked) {
            this.checked = checked;
            setOpaque(false);
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 22;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                if (checked) {
                    g2.setColor(Theme.ACCENT);
                    g2.fillOval(x, y, size, size);
                    Icons.check(14, Color.WHITE).paintIcon(this, g2, x + 4, y + 4);
                } else {
                    g2.setColor(new Color(0x9AA8B3));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Recuadro de color con esquinas redondeadas. */
    static final class RoundedBox extends JPanel {

        private final Color fill;

        RoundedBox(Color fill) {
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
