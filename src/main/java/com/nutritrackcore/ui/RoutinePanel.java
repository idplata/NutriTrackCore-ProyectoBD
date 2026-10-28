package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.MedicalCondition;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Pantalla "Mi rutina": muestra la rutina de ejercicio activa, las
 * restricciones que se aplicaron por las condiciones médicas y la lista de
 * ejercicios. El botón de arriba genera una rutina nueva con
 * {@code ExerciseRoutineService.generateRoutine}.
 */
public class RoutinePanel extends ScreenKit.BlueBackground implements Refreshable {

    private static final Color WARN_TEXT = new Color(0x7A3E00);
    private static final int SERIES_WIDTH = 70;
    private static final int REPS_WIDTH = 110;
    private static final int IMPACT_WIDTH = 120;

    private final AppServices services;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();

    /**
     * Crea la pantalla.
     *
     * @param services servicios de la aplicación
     * @param navigate cómo ir a otra sección (se reserva para enlaces futuros)
     */
    public RoutinePanel(AppServices services, Consumer<String> navigate) {
        super(new BorderLayout());
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(32, 36, 24, 36));
        add(ScreenKit.transparentScroll(content), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        content.removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        int userId = services.session().getCurrentUser().getIdUser();
        try {
            Optional<ExerciseRoutine> routine = services.routines().getActiveRoutine(userId);
            if (routine.isEmpty()) {
                JButton generate = FormKit.primaryButton("Generar mi rutina", Icons.refresh(18, Color.WHITE), 230);
                generate.addActionListener(e -> generate(false));
                content.add(ScreenKit.header("Mi rutina", "Aún no tienes una rutina generada.", null));
                content.add(Box.createVerticalStrut(22));
                content.add(ScreenKit.left(ScreenKit.emptyState(Images.CARD_ROUTINE, 0.40, "Genera tu rutina",
                        "La armamos según tu IMC y tus condiciones médicas.", generate)));
            } else {
                showRoutine(userId, routine.get());
            }
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    private void showRoutine(int userId, ExerciseRoutine routine) throws PersistenceExceptions {
        // IMC con el que se generó la rutina
        String bmiText = "";
        for (BMIHistory h : services.bmi().getHistory(userId)) {
            if (h.getIdHistory() == routine.getBaseHistoryId()) {
                bmiText = " · Basada en tu IMC de " + ScreenKit.decimal(services.bmi().calculateBmi(h).getValue());
                break;
            }
        }
        JButton generate = ScreenKit.whiteButton("Generar nueva rutina", Icons.refresh(18, Theme.ACCENT));
        generate.addActionListener(e -> generate(true));
        content.add(ScreenKit.header("Mi rutina",
                "Generada el " + ScreenKit.longDate(routine.getGenerationDate()) + bmiText, generate));
        content.add(Box.createVerticalStrut(22));

        content.add(ScreenKit.fixedHeight(createRestrictions(userId, routine)));
        content.add(Box.createVerticalStrut(18));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 18);
        grid.add(createSummary(userId, routine), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        grid.add(createExercises(routine), c);
        content.add(ScreenKit.fixedHeight(grid));
    }

    private JComponent createRestrictions(int userId, ExerciseRoutine routine) throws PersistenceExceptions {
        Map<Integer, String> names = new HashMap<>();
        for (MedicalCondition c : services.conditions().listCatalog()) {
            names.put(c.getConditionId(), c.getName());
        }
        for (MedicalCondition c : services.conditions().listByUser(userId)) {
            names.put(c.getConditionId(), c.getName());
        }

        ConditionsPanel.RoundedBox box = new ConditionsPanel.RoundedBox(new Color(0xFFF4E5));
        box.setLayout(new BorderLayout(14, 0));
        box.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel icon = new JLabel(Icons.alert(22, WARN_TEXT));
        icon.setVerticalAlignment(SwingConstants.TOP);
        box.add(icon, BorderLayout.WEST);

        JPanel texts = ScreenKit.column();
        texts.add(ScreenKit.label("Ajustada a tus condiciones", Theme.font("SemiBold", 15), WARN_TEXT));
        boolean any = false;
        for (ExerciseRoutineCondition rc : routine.getConsideredConditions()) {
            String restriction = rc.getAppliedRestriction();
            if (restriction == null || restriction.isBlank() || restriction.equalsIgnoreCase("Sin restricciones")) {
                continue;
            }
            any = true;
            String name = names.getOrDefault(rc.getConditionId(), "Condición");
            texts.add(Box.createVerticalStrut(4));
            texts.add(ScreenKit.label("<html><b>" + escape(name) + ":</b> " + escape(lowerFirst(restriction))
                    + ".</html>", Theme.font(Font.PLAIN, 13), WARN_TEXT));
        }
        if (!any) {
            texts.add(Box.createVerticalStrut(4));
            texts.add(ScreenKit.label("No tienes condiciones que limiten tu rutina.", Theme.font(Font.PLAIN, 13),
                    WARN_TEXT));
        }
        box.add(texts, BorderLayout.CENTER);
        return box;
    }

    private JComponent createSummary(int userId, ExerciseRoutine routine) throws PersistenceExceptions {
        Card card = new Card(Images.get(Images.CARD_ROUTINE), 150, 0.40);
        card.setPadding(18, 22, 20, 22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(stat("Intensidad", intensity(routine)));
        card.add(Box.createVerticalStrut(14));
        card.add(stat("Ejercicios", String.valueOf(routine.getLines().size())));
        card.add(Box.createVerticalStrut(14));

        String diet = "Sin dieta";
        if (routine.getDietPlanId() != null) {
            for (DietPlan plan : services.dietPlans().listHistory(userId)) {
                if (plan.getPlanId() == routine.getDietPlanId()) {
                    diet = "Tu dieta del " + ScreenKit.longDate(plan.getGenerationDate());
                    break;
                }
            }
        }
        JPanel coordinated = ScreenKit.column();
        coordinated.add(ScreenKit.label("Coordinada con", Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
        coordinated.add(ScreenKit.label(diet, Theme.font("SemiBold", 14), Theme.TEXT));
        card.add(ScreenKit.left(coordinated));

        Dimension size = new Dimension(290, card.getPreferredSize().height);
        card.setPreferredSize(size);
        card.setMinimumSize(size);
        return card;
    }

    private JComponent createExercises(ExerciseRoutine routine) throws PersistenceExceptions {
        Card card = new Card();
        card.setPadding(20, 24, 18, 24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(ScreenKit.left(ScreenKit.label("Ejercicios", Theme.font("SemiBold", 18), Theme.TEXT)));
        card.add(Box.createVerticalStrut(8));

        Font headFont = Theme.font("SemiBold", 12);
        card.add(ScreenKit.fixedHeight(tableRow(new Color(0xE3E9ED), 8,
                ScreenKit.label("Ejercicio", headFont, ScreenKit.NOTE),
                ScreenKit.label("Grupo muscular", headFont, ScreenKit.NOTE),
                centered(ScreenKit.label("Series", headFont, ScreenKit.NOTE)),
                centered(ScreenKit.label("Repeticiones", headFont, ScreenKit.NOTE)),
                ScreenKit.label("Impacto", headFont, ScreenKit.NOTE))));

        Map<Integer, Exercise> cache = new HashMap<>();
        List<ExerciseRoutineExercise> lines = routine.getLines();
        for (ExerciseRoutineExercise line : lines) {
            Exercise exercise = cache.get(line.getExerciseId());
            if (exercise == null) {
                exercise = services.exercises().getById(line.getExerciseId());
                cache.put(line.getExerciseId(), exercise);
            }
            Pill impact = exercise.isLowImpact()
                    ? new Pill("Bajo impacto", new Color(0xE7F4F1), new Color(0x08594C))
                    : new Pill("Alto impacto", new Color(0xFFF4E5), WARN_TEXT);
            String group = exercise.getMuscleGroup() == null ? "—" : exercise.getMuscleGroup();
            Font cell = Theme.font(Font.PLAIN, 14);
            card.add(ScreenKit.fixedHeight(tableRow(new Color(0xEEF2F4), 13,
                    ScreenKit.label(exercise.getName(), Theme.font("SemiBold", 14), Theme.TEXT),
                    ScreenKit.label(group, cell, Theme.TEXT_MUTED),
                    centered(ScreenKit.label(String.valueOf(line.getSets()), cell, Theme.TEXT)),
                    centered(ScreenKit.label(String.valueOf(line.getRepetitions()), cell, Theme.TEXT)),
                    impact)));
        }
        card.add(Box.createVerticalStrut(14));
        card.add(ScreenKit.left(ScreenKit.label("Detente si sientes dolor, mareo o falta de aire.",
                Theme.font(Font.PLAIN, 12), ScreenKit.NOTE)));
        return card;
    }

    /** Fila de la tabla de ejercicios con columnas alineadas. */
    private static JComponent tableRow(Color line, int padding, JComponent name, JComponent group,
                                       JComponent series, JComponent reps, JComponent impact) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, line),
                BorderFactory.createEmptyBorder(padding, 0, padding, 0)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.weightx = 1.6;
        row.add(fit(name, 10), c);
        c.gridx = 1;
        c.weightx = 1.3;
        row.add(fit(group, 10), c);
        c.weightx = 0;
        c.gridx = 2;
        row.add(fit(series, SERIES_WIDTH), c);
        c.gridx = 3;
        row.add(fit(reps, REPS_WIDTH), c);
        c.gridx = 4;
        c.fill = GridBagConstraints.NONE;
        JPanel impactBox = new JPanel(new BorderLayout());
        impactBox.setOpaque(false);
        impactBox.add(impact, BorderLayout.WEST);
        row.add(fit(impactBox, IMPACT_WIDTH), c);
        return row;
    }

    private static JComponent fit(JComponent c, int width) {
        Dimension pref = c.getPreferredSize();
        c.setPreferredSize(new Dimension(width, pref.height));
        c.setMinimumSize(new Dimension(Math.min(width, 10), pref.height));
        return c;
    }

    private static JLabel centered(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private void generate(boolean replacing) {
        if (replacing && !Messages.confirm(this,
                "Se generará una rutina nueva y la actual pasará a tu historial. ¿Continuar?")) {
            return;
        }
        try {
            services.routines().generateRoutine(services.session().getCurrentUser().getIdUser());
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private static JComponent stat(String caption, String value) {
        JPanel box = ScreenKit.column();
        box.add(ScreenKit.label(caption, Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
        box.add(ScreenKit.label(value, Theme.font(Font.BOLD, 22), Theme.TEXT));
        return ScreenKit.left(box);
    }

    private static String intensity(ExerciseRoutine routine) {
        if (routine.getIntensityLevel() == null) {
            return "—";
        }
        return switch (routine.getIntensityLevel()) {
            case LOW -> "Baja";
            case MODERATE -> "Moderada";
            case HIGH -> "Alta";
        };
    }

    private static String lowerFirst(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
