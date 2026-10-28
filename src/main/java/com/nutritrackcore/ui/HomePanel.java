package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineCondition;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.User;
import com.nutritrackcore.services.dto.BmiResult;
import com.nutritrackcore.services.dto.NutritionalSummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
 * Pantalla de Inicio del usuario.
 * <ul>
 *   <li><b>Usuario nuevo</b> (sin condiciones, dieta ni rutina): muestra su IMC
 *       y la guía "Arma tu plan en 3 pasos".</li>
 *   <li><b>Con datos:</b> muestra las tarjetas de condiciones, dieta y rutina.
 *       Las que todavía falten aparecen como pendientes con un enlace a su sección.</li>
 *   <li><b>Con plan de dieta:</b> además muestra calorías y macronutrientes del día
 *       frente a la meta del plan.</li>
 * </ul>
 * Los datos se vuelven a leer de los servicios cada vez que se muestra la
 * pantalla ({@link #refresh()}).
 */
public class HomePanel extends JPanel implements Refreshable {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ES_CO);
    private static final Color CAPTION = Theme.TEXT_MUTED;
    private static final Color NOTE = new Color(0x5C6B77);
    private static final int GAP = 18;

    private final AppServices services;
    private final Consumer<String> navigate;
    private final JPanel content = new JPanel();

    /**
     * Crea la pantalla.
     *
     * @param services servicios de la aplicación
     * @param navigate cómo ir a otra sección (recibe su nombre, ver {@link AppShellPanel})
     */
    public HomePanel(AppServices services, Consumer<String> navigate) {
        this.services = services;
        this.navigate = navigate;
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(0x027690));
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        add(content, BorderLayout.NORTH);
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

    @Override
    public void refresh() {
        content.removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        User user = services.session().getCurrentUser();
        content.add(row(createHeader(user)));
        content.add(Box.createVerticalStrut(22));

        try {
            int userId = user.getIdUser();
            BmiResult bmi = services.bmi().getCurrentBmi(userId);
            List<MedicalCondition> conditions = services.conditions().listByUser(userId);
            Optional<DietPlan> plan = services.dietPlans().getActivePlan(userId);
            Optional<ExerciseRoutine> routine = services.routines().getActiveRoutine(userId);

            if (plan.isPresent()) {
                NutritionalSummary today = services.consumption().getDailySummary(userId, LocalDate.now());
                content.add(row(createTodayRow(today, plan.get(), bmi, user)));
            } else {
                content.add(row(createBmiStrip(bmi, user)));
            }
            content.add(Box.createVerticalStrut(22));

            if (conditions.isEmpty() && plan.isEmpty() && routine.isEmpty()) {
                content.add(row(createGettingStarted()));
            } else {
                JPanel cards = new JPanel(new GridLayout(1, 3, GAP, 0));
                cards.setOpaque(false);
                cards.add(createConditionsCard(conditions));
                cards.add(createDietCard(plan));
                cards.add(createRoutineCard(routine, conditions));
                content.add(row(cards));
            }
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    // ------------------------------------------------------------------ encabezado

    private JComponent createHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel texts = column();
        String date = LocalDate.now().format(DATE_FORMAT);
        JLabel dateLabel = label(Character.toUpperCase(date.charAt(0)) + date.substring(1),
                Theme.font(Font.PLAIN, 14), new Color(0xD9F1F4));
        JLabel hello = label("Hola, " + firstName(user.getName()), Theme.font(Font.BOLD, 30), Color.WHITE);
        texts.add(dateLabel);
        texts.add(Box.createVerticalStrut(2));
        texts.add(hello);

        JButton register = new JButton("Registrar consumo", Icons.plus(18, Theme.ACCENT));
        register.setFont(Theme.font("SemiBold", 14));
        register.setForeground(Theme.ACCENT);
        register.setIconTextGap(8);
        register.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        register.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; margin: 0,18,0,18;"
                + "background: #FFFFFF; hoverBackground: #EEF7F5; pressedBackground: #DDEFEB");
        register.setPreferredSize(new Dimension(register.getPreferredSize().width, 44));
        register.addActionListener(e -> navigate.accept(AppShellPanel.CONSUMPTION));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(register);

        header.add(texts, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ------------------------------------------------------------------ resumen

    private JComponent createTodayRow(NutritionalSummary today, DietPlan plan, BmiResult bmi, User user) {
        JPanel rowPanel = new JPanel(new GridLayout(1, 3, GAP, 0));
        rowPanel.setOpaque(false);

        // Calorías frente a la meta del plan
        Card calories = new Card();
        calories.setLayout(new BoxLayout(calories, BoxLayout.Y_AXIS));
        double consumed = today.getCalories();
        double target = plan.getTargetCalories();
        calories.add(left(label("Calorías de hoy", Theme.font("Medium", 13), CAPTION)));
        calories.add(Box.createVerticalStrut(6));
        calories.add(left(valueWithUnit(number(consumed), "/ " + number(target) + " kcal")));
        calories.add(Box.createVerticalStrut(8));
        calories.add(left(new ProgressLine(target > 0 ? consumed / target : 0)));
        calories.add(Box.createVerticalStrut(8));
        double remaining = target - consumed;
        String note = remaining >= 0 ? "Te quedan " + number(remaining) + " kcal según tu plan"
                : "Pasaste tu meta por " + number(-remaining) + " kcal";
        calories.add(left(label(note, Theme.font(Font.PLAIN, 12), NOTE)));

        // IMC
        Card bmiCard = new Card();
        bmiCard.setLayout(new BoxLayout(bmiCard, BoxLayout.Y_AXIS));
        bmiCard.add(left(label("Tu IMC", Theme.font("Medium", 13), CAPTION)));
        bmiCard.add(Box.createVerticalStrut(6));
        JPanel bmiRow = new JPanel(new BorderLayout());
        bmiRow.setOpaque(false);
        bmiRow.add(label(decimal(bmi.getValue()), Theme.font(Font.BOLD, 28), Theme.TEXT), BorderLayout.WEST);
        JPanel pillBox = new JPanel(new java.awt.GridBagLayout());
        pillBox.setOpaque(false);
        pillBox.add(bmiPill(bmi));
        bmiRow.add(pillBox, BorderLayout.EAST);
        bmiCard.add(left(bmiRow));
        bmiCard.add(Box.createVerticalStrut(8));
        bmiCard.add(left(label(measures(user), Theme.font(Font.PLAIN, 12), NOTE)));

        // Macronutrientes
        Card macros = new Card();
        macros.setLayout(new BoxLayout(macros, BoxLayout.Y_AXIS));
        macros.add(left(label("Macronutrientes de hoy", Theme.font("Medium", 13), CAPTION)));
        macros.add(Box.createVerticalStrut(10));
        JPanel grid = new JPanel();
        grid.setOpaque(false);
        grid.setLayout(new BoxLayout(grid, BoxLayout.X_AXIS));
        grid.add(macro(today.getProteins(), "Proteínas"));
        grid.add(Box.createHorizontalStrut(16));
        grid.add(macro(today.getCarbohydrates(), "Carbohidratos"));
        grid.add(Box.createHorizontalStrut(16));
        grid.add(macro(today.getFats(), "Grasas"));
        grid.add(Box.createHorizontalGlue());
        macros.add(left(grid));

        rowPanel.add(calories);
        rowPanel.add(bmiCard);
        rowPanel.add(macros);
        return rowPanel;
    }

    private JComponent createBmiStrip(BmiResult bmi, User user) {
        Card strip = new Card();
        strip.setPadding(18, 24, 18, 24);
        strip.setLayout(new BoxLayout(strip, BoxLayout.X_AXIS));

        JPanel value = column();
        value.add(label("Tu IMC", Theme.font("Medium", 13), CAPTION));
        value.add(label(decimal(bmi.getValue()), Theme.font(Font.BOLD, 32), Theme.TEXT));

        JButton update = arrowLink("Actualizar medidas");
        update.addActionListener(e -> navigate.accept(AppShellPanel.PROFILE));

        strip.add(value);
        strip.add(Box.createHorizontalStrut(28));
        strip.add(bmiPill(bmi));
        strip.add(Box.createHorizontalStrut(28));
        strip.add(label(measures(user), Theme.font(Font.PLAIN, 14), CAPTION));
        strip.add(Box.createHorizontalGlue());
        strip.add(update);
        return strip;
    }

    // ------------------------------------------------------------------ usuario nuevo

    private JComponent createGettingStarted() {
        Card card = new Card();
        card.setPadding(24, 26, 24, 26);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(left(label("Arma tu plan en 3 pasos", Theme.font("SemiBold", 19), Theme.TEXT)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(label("Tus condiciones, tu dieta y tu rutina aparecerán aquí cuando las completes.",
                Theme.font(Font.PLAIN, 14), CAPTION)));
        card.add(Box.createVerticalStrut(16));

        JPanel steps = new JPanel(new GridLayout(1, 3, 14, 0));
        steps.setOpaque(false);
        steps.add(stepTile(1, "Registra tus condiciones", "O indica que no tienes ninguna.",
                AppShellPanel.CONDITIONS));
        steps.add(stepTile(2, "Genera tu dieta", "Calorías y alimentos para ti.",
                AppShellPanel.DIET));
        steps.add(stepTile(3, "Genera tu rutina", "Ejercicios seguros para tu nivel.", AppShellPanel.ROUTINE));
        card.add(left(steps));
        return card;
    }

    private JButton stepTile(int number, String title, String description, String section) {
        JButton tile = new JButton();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; focusWidth: 0; borderColor: #D6DEE4; hoverBorderColor: #0B7A69;"
                + "background: #FFFFFF; hoverBackground: #F4FAF8; pressedBackground: #E7F4F1");
        tile.setBorder(BorderFactory.createCompoundBorder(tile.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        tile.add(left(label("PASO " + number, Theme.font("SemiBold", 12), Theme.ACCENT)));
        tile.add(Box.createVerticalStrut(6));
        tile.add(left(label(title, Theme.font("SemiBold", 15), Theme.TEXT)));
        tile.add(Box.createVerticalStrut(4));
        tile.add(left(label(description, Theme.font(Font.PLAIN, 13), NOTE)));
        tile.getAccessibleContext().setAccessibleName("Paso " + number + ": " + title);
        tile.addActionListener(e -> navigate.accept(section));
        return tile;
    }

    // ------------------------------------------------------------------ tarjetas

    private Card createConditionsCard(List<MedicalCondition> conditions) {
        Card card = sectionCard(Images.CARD_CONDITIONS, 0.28, Icons::heartPulse, "Tus condiciones");
        if (conditions.isEmpty()) {
            card.add(left(label("Aún no registras tus condiciones.", Theme.font(Font.PLAIN, 14), CAPTION)));
            addLink(card, "Registrar condiciones", AppShellPanel.CONDITIONS);
            return card;
        }
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        chips.setOpaque(false);
        chips.setBorder(BorderFactory.createEmptyBorder(-6, -8, 0, 0));
        int shown = Math.min(3, conditions.size());
        for (int i = 0; i < shown; i++) {
            Pill chip = new Pill(conditions.get(i).getName(), new Color(0xEEF2F4), Theme.TEXT_LABEL);
            chip.setFont(Theme.font(Font.PLAIN, 13));
            chips.add(chip);
        }
        if (conditions.size() > shown) {
            chips.add(new Pill("+" + (conditions.size() - shown), new Color(0xEEF2F4), Theme.TEXT_LABEL));
        }
        card.add(fixedHeight(chips));
        addLink(card, "Ver condiciones", AppShellPanel.CONDITIONS);
        return card;
    }

    private Card createDietCard(Optional<DietPlan> plan) {
        Card card = sectionCard(Images.CARD_DIET, 0.72, Icons::utensils, "Tu dieta");
        if (plan.isEmpty()) {
            card.add(left(label("Aún no generas tu dieta.", Theme.font(Font.PLAIN, 14), CAPTION)));
            addLink(card, "Generar mi dieta", AppShellPanel.DIET);
            return card;
        }
        DietPlan p = plan.get();
        List<DietPlanFood> lines = p.getLines();
        long meals = lines.stream().map(DietPlanFood::getMealTime).distinct().count();
        card.add(left(label(number(p.getTargetCalories()) + " kcal al día", Theme.font("SemiBold", 15), Theme.TEXT)));
        card.add(Box.createVerticalStrut(2));
        card.add(left(label(lines.size() + (lines.size() == 1 ? " alimento" : " alimentos") + " en " + meals
                + (meals == 1 ? " comida" : " comidas"), Theme.font(Font.PLAIN, 14), CAPTION)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(label("Generada el " + p.getGenerationDate().format(DateTimeFormatter.ofPattern(
                "d 'de' MMMM", ES_CO)), Theme.font(Font.PLAIN, 12), NOTE)));
        addLink(card, "Ver mi dieta", AppShellPanel.DIET);
        return card;
    }

    private Card createRoutineCard(Optional<ExerciseRoutine> routine, List<MedicalCondition> conditions) {
        Card card = sectionCard(Images.CARD_ROUTINE, 0.40, Icons::dumbbell, "Tu rutina");
        if (routine.isEmpty()) {
            card.add(left(label("Aún no generas tu rutina.", Theme.font(Font.PLAIN, 14), CAPTION)));
            addLink(card, "Generar mi rutina", AppShellPanel.ROUTINE);
            return card;
        }
        ExerciseRoutine r = routine.get();
        int count = r.getLines().size();
        card.add(left(label("Intensidad " + intensity(r) + " · " + count + (count == 1 ? " ejercicio" : " ejercicios"),
                Theme.font(Font.PLAIN, 14), CAPTION)));

        Set<String> restrictions = new LinkedHashSet<>();
        for (ExerciseRoutineCondition c : r.getConsideredConditions()) {
            String text = c.getAppliedRestriction();
            if (text != null && !text.isBlank() && !text.equalsIgnoreCase("Sin restricciones")) {
                restrictions.add(text);
            }
        }
        if (!restrictions.isEmpty()) {
            JPanel chips = column();
            for (String text : restrictions.stream().limit(2).toList()) {
                chips.add(Box.createVerticalStrut(6));
                Pill chip = new Pill("<html><div style='width:175px'>" + text + "</div></html>",
                        new Color(0xFFF4E5), new Color(0x7A3E00));
                chip.setFont(Theme.font("Medium", 12));
                chips.add(left(chip));
            }
            card.add(fixedHeight(chips));
        }
        addLink(card, "Ver mi rutina", AppShellPanel.ROUTINE);
        return card;
    }

    private Card sectionCard(String cover, double focusY, BiFunction<Integer, Color, Icon> icon, String title) {
        Card card = new Card(Images.get(cover), 120, focusY);
        card.setPadding(14, 22, 18, 22);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
        BrandPanel.RoundBadge badge = new BrandPanel.RoundBadge(36, 20, new Color(0xE7F4F1),
                icon.apply(18, Theme.ACCENT));
        head.add(badge);
        head.add(label(title, Theme.font("SemiBold", 17), Theme.TEXT));
        card.add(fixedHeight(head));
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    private void addLink(Card card, String text, String section) {
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));
        JButton link = arrowLink(text);
        link.putClientProperty(FlatClientProperties.STYLE,
                "margin: 2,0,2,0; foreground: #0B7A69; hoverBackground: #E7F4F1; pressedBackground: #D3EBE6");
        link.setHorizontalAlignment(SwingConstants.LEFT);
        link.addActionListener(e -> navigate.accept(section));
        card.add(left(link));
    }

    // ------------------------------------------------------------------ piezas pequeñas

    /** Enlace verde con una flecha dibujada a la derecha. */
    private static JButton arrowLink(String text) {
        JButton link = FormKit.linkButton(text);
        link.setIcon(Icons.arrowRight(14, Theme.ACCENT));
        link.setHorizontalTextPosition(SwingConstants.LEADING);
        link.setIconTextGap(6);
        return link;
    }

    private static JComponent valueWithUnit(String value, String unit) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, -6, 0, 0));
        row.add(label(value, Theme.font(Font.BOLD, 28), Theme.TEXT));
        row.add(label(unit, Theme.font(Font.PLAIN, 14), CAPTION));
        return row;
    }

    private static JComponent macro(double grams, String name) {
        JPanel box = column();
        box.add(label(number(grams) + " g", Theme.font(Font.BOLD, 20), Theme.TEXT));
        box.add(label(name, Theme.font(Font.PLAIN, 12), NOTE));
        return box;
    }

    private static Pill bmiPill(BmiResult bmi) {
        String name = bmi.getCategory().getDisplayName();
        return new Pill(name, FormKit.bmiColor(name), Color.WHITE);
    }

    private static String measures(User user) {
        return decimal(user.getWeightKg()) + " kg · " + number(user.getHeightCm()) + " cm";
    }

    private static String intensity(ExerciseRoutine routine) {
        if (routine.getIntensityLevel() == null) {
            return "—";
        }
        return switch (routine.getIntensityLevel()) {
            case LOW -> "baja";
            case MODERATE -> "moderada";
            case HIGH -> "alta";
        };
    }

    private static String number(double value) {
        NumberFormat f = NumberFormat.getIntegerInstance(ES_CO);
        return f.format(Math.round(value));
    }

    private static String decimal(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(ES_CO);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(value);
    }

    private static String firstName(String fullName) {
        return fullName == null || fullName.isBlank() ? "" : fullName.trim().split("\\s+")[0];
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Alinea a la izquierda y evita que el componente se estire a lo alto. */
    private static JComponent fixedHeight(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private static JComponent row(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** Barra de avance delgada (por ejemplo, calorías consumidas frente a la meta). */
    private static final class ProgressLine extends JPanel {

        private final double fraction;

        ProgressLine(double fraction) {
            this.fraction = Math.max(0, fraction);
            setOpaque(false);
            setPreferredSize(new Dimension(100, 8));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
            getAccessibleContext().setAccessibleName("Avance: " + Math.round(this.fraction * 100) + " %");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(0xE3ECEA));
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(fraction > 1 ? new Color(0xB3261E) : Theme.ACCENT);
                g2.fillRoundRect(0, 0, (int) (w * Math.min(1, fraction)), h, h, h);
            } finally {
                g2.dispose();
            }
        }
    }
}
