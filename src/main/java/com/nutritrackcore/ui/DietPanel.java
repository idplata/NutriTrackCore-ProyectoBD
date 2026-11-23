package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.services.NutritionCalculator;
import com.nutritrackcore.services.NutritionalSummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Mi dieta, igual al prototipo: resumen con foto (meta, calorías del plan y
 * macros), aviso de lo que la dieta no incluye, barra de distribución por
 * comida y una tarjeta por comida con su foto, nombre y calorías encima.
 */
public class DietPanel extends ScreenKit.BlueBackground implements Refreshable {

    /** Colores de los segmentos de la barra de distribución (en orden del día). */
    private static final Color[] SEG_COLORS = {new Color(0x0B7A69), new Color(0x34B38A), new Color(0xF0B44C),
        new Color(0xE8853A), new Color(0x7A5BD3)};

    private final AppServices services;
    private final Consumer<String> navigate;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();

    /**
     * @param services servicios de la aplicación
     * @param navigate cambia de sección
     */
    public DietPanel(AppServices services, Consumer<String> navigate) {
        super(new BorderLayout());
        this.services = services;
        this.navigate = navigate;
        setBorder(BorderFactory.createEmptyBorder(32, 36, 24, 36));
        add(ScreenKit.transparentScroll(content), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        content.removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        int userId = services.currentUser().getIdUser();
        try {
            Optional<DietPlan> plan = services.dietPlans().getActivePlan(userId);
            if (plan.isEmpty()) {
                JButton generate = FormKit.primaryButton("Generar mi dieta", Icons.refresh(18, Color.WHITE), 230);
                generate.addActionListener(e -> generate(false));
                content.add(ScreenKit.header("Mi dieta", "Aún no tienes una dieta generada.", null));
                content.add(Box.createVerticalStrut(22));
                content.add(ScreenKit.left(ScreenKit.emptyState(Images.CARD_DIET, 0.72, "Genera tu dieta",
                        "La armamos con tus medidas y tus condiciones médicas.", generate)));
            } else {
                showPlan(userId, plan.get(), services.conditions().listByUser(userId));
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    private void showPlan(int userId, DietPlan plan, List<MedicalCondition> conditions) throws NutriTrackException {
        Map<Integer, Food> foods = new HashMap<>();
        NutritionalSummary total = NutritionalSummary.empty();
        for (DietPlanFood line : plan.getLines()) {
            Food food = foods.computeIfAbsent(line.getFoodId(), id -> {
                try {
                    return services.foods().getById(id);
                } catch (NutriTrackException e) {
                    return null;
                }
            });
            if (food != null) {
                total = total.add(intake(food, line.getQuantityG()));
            }
        }

        // Encabezado
        List<String> names = conditions.stream().map(MedicalCondition::getName).toList();
        String subtitle = "Generada el " + ScreenKit.longDate(plan.getGenerationDate())
                + (names.isEmpty() ? " · Dieta " + Labels.diet(plan.getDietType() == null ? null
                : plan.getDietType().name()).toLowerCase() : " · Ajustada por " + ScreenKit.joinNames(names));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Proto.pageTitle("Mi dieta", subtitle), BorderLayout.CENTER);
        JButton regen = Proto.button("Generar nueva dieta", Color.WHITE, Proto.GREEN, null, 44, 14);
        Proto.withIcon(regen, Icons.refresh(15, Proto.GREEN), false);
        regen.addActionListener(e -> generate(true));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(regen);
        head.add(right, BorderLayout.EAST);
        content.add(Proto.left(Proto.noStretch(head)));
        content.add(Box.createVerticalStrut(26));

        content.add(Proto.left(summary(plan, total)));
        content.add(Box.createVerticalStrut(24));

        JComponent filters = filters(userId);
        if (filters != null) {
            content.add(Proto.left(filters));
            content.add(Box.createVerticalStrut(24));
        }

        // Comidas presentes en el plan, en el orden del día
        List<MealTime> present = new ArrayList<>();
        for (MealTime meal : MealTime.values()) {
            if (plan.getLines().stream().anyMatch(l -> l.getMealTime() == meal)) {
                present.add(meal);
            }
        }
        double[] kcal = new double[present.size()];
        for (DietPlanFood line : plan.getLines()) {
            int i = present.indexOf(line.getMealTime());
            Food food = foods.get(line.getFoodId());
            if (i >= 0 && food != null) {
                kcal[i] += intake(food, line.getQuantityG()).getCalories();
            }
        }
        content.add(Proto.left(distribution(present, kcal, total.getCalories(), !names.isEmpty())));
        content.add(Box.createVerticalStrut(24));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        int columns = Math.max(2, present.size());
        for (int i = 0; i < columns; i++) {
            c.gridx = i;
            c.insets = new Insets(0, i == 0 ? 0 : 10, 0, i == columns - 1 ? 0 : 10);
            grid.add(i < present.size() ? mealCard(present.get(i), kcal[i], plan.getLines(), foods) : spacer(), c);
        }
        content.add(Proto.left(Proto.noStretch(grid)));
        content.add(Box.createVerticalStrut(18));
        content.add(Proto.left(Proto.text("Porciones orientativas. Consulta a un profesional de la salud antes de "
                + "cambiar tu alimentación.", null, 13, Proto.ON_BLUE)));
    }

    // ------------------------------------------------------------------ resumen

    private static JComponent summary(DietPlan plan, NutritionalSummary total) {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());
        card.add(new Proto.Photo(Images.get(Images.CARD_DIET), 0.5, 0.5, 126, 0, 0) {
            {
                setPreferredSize(new Dimension(230, 126));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.clip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth() + 40, getHeight(), 32, 32));
                Proto.paintCover(g2, Images.get(Images.CARD_DIET), getWidth(), getHeight(), 0.5, 0.5);
                g2.dispose();
            }
        }, BorderLayout.WEST);
        JPanel stats = new JPanel(new GridLayout(1, 5));
        stats.setOpaque(false);
        stats.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 26));
        stats.add(stat("Meta diaria", ScreenKit.number(plan.getTargetCalories()), "kcal"));
        stats.add(stat("En el plan", ScreenKit.number(total.getCalories()), "kcal"));
        stats.add(stat("Proteínas", ScreenKit.number(total.getProteins()), "g"));
        stats.add(stat("Carbohidratos", ScreenKit.number(total.getCarbohydrates()), "g"));
        stats.add(stat("Grasas", ScreenKit.number(total.getFats()), "g"));
        card.add(stats, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(800, 126));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        return card;
    }

    private static JComponent stat(String caption, String value, String unit) {
        JPanel box = new JPanel(new GridBagLayout());
        box.setOpaque(false);
        JPanel inner = Proto.column();
        inner.add(Proto.left(Proto.text(caption, null, 13, Proto.MUTED)));
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(0, -6, 0, 0));
        line.add(Proto.text(value, "Bold", 26, Proto.INK));
        JLabel u = Proto.text(unit, "Medium", 13, Proto.MUTED);
        u.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        line.add(u);
        inner.add(Proto.left(line));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        box.add(inner, c);
        return box;
    }

    // ------------------------------------------------------------------ "Tu dieta no incluye"

    private JComponent filters(int userId) throws NutriTrackException {
        Map<Integer, String> allergyNames = new HashMap<>();
        for (Allergy a : services.allergies().listCatalog()) {
            allergyNames.put(a.getAllergyId(), a.getName());
        }
        List<String> parts = new ArrayList<>();
        for (UserAllergy ua : services.allergies().listByUser(userId)) {
            parts.add(allergyShort(allergyNames.getOrDefault(ua.getAllergyId(), "Alergia")) + " (alergia)");
        }
        for (UserExcludedFood ex : services.excludedFoods().listByUser(userId)) {
            parts.add(services.foods().getById(ex.getFoodId()).getName());
        }
        if (parts.isEmpty()) {
            return null;
        }
        JPanel box = new Proto.Rounded(Color.WHITE, null, 0, 24);
        box.setLayout(new BorderLayout(12, 0));
        box.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        box.add(new JLabel(Icons.ban(17, new Color(0xB4372A))), BorderLayout.WEST);
        JLabel text = Proto.text("<html><div style='width:500px'><b>Tu dieta no incluye:</b> "
                + Proto.escape(String.join(", ", parts))
                + ". Los cambiamos por alimentos equivalentes en calorías.</div></html>", null, 13, Proto.INK_2);
        box.add(text, BorderLayout.CENTER);
        JButton edit = Proto.link("Editar en Mi perfil", Proto.GREEN, 13);
        edit.setIcon(Icons.arrowRight(13, Proto.GREEN));
        edit.setHorizontalTextPosition(SwingConstants.LEADING);
        edit.addActionListener(e -> navigate.accept(AppShellPanel.PROFILE));
        box.add(edit, BorderLayout.EAST);
        return Proto.noStretch(box);
    }

    /** "Alergia al maní" → "Maní", para el aviso corto. */
    private static String allergyShort(String name) {
        String s = name.replaceFirst("(?i)^(alergia|intolerancia|sensibilidad) (a la |al |a los |a las |a )", "");
        return s.isEmpty() ? name : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ distribución

    private static JComponent distribution(List<MealTime> meals, double[] kcal, double total, boolean withConditions) {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 28);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.setOpaque(false);
        title.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        title.add(Proto.text("Distribución en " + meals.size() + (meals.size() == 1 ? " comida" : " comidas"),
                "SemiBold", 14, Proto.INK));
        title.add(Proto.text("· " + (withConditions ? "con condiciones: 4 comidas y dieta omnívora"
                : "cámbialo en Mis hábitos (1 a 4 comidas)"), null, 12, Proto.MUTED));
        card.add(title, BorderLayout.NORTH);

        int[] pct = new int[meals.size()];
        for (int i = 0; i < pct.length; i++) {
            pct[i] = total <= 0 ? 0 : (int) Math.round(kcal[i] * 100 / total);
        }
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                int gap = 3;
                int avail = getWidth() - gap * (pct.length - 1);
                int x = 0;
                g2.setFont(Proto.font("Bold", 12));
                for (int i = 0; i < pct.length; i++) {
                    int w = i == pct.length - 1 ? getWidth() - x
                            : (int) Math.round(avail * (total <= 0 ? 1.0 / pct.length : kcal[i] / total));
                    g2.setColor(SEG_COLORS[i % SEG_COLORS.length]);
                    g2.fillRoundRect(x, 0, w, getHeight(), 12, 12);
                    String t = pct[i] + "%";
                    g2.setColor(Color.WHITE);
                    int tw = g2.getFontMetrics().stringWidth(t);
                    g2.drawString(t, x + (w - tw) / 2, getHeight() / 2 + 5);
                    x += w + gap;
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 30));
        card.add(bar, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(0, -14, 0, 0));
        for (int i = 0; i < meals.size(); i++) {
            JLabel l = Proto.text(meals.get(i).getDisplayName() + " · " + ScreenKit.number(kcal[i]) + " kcal", null, 12,
                    Proto.INK_2);
            l.setIcon(square(SEG_COLORS[i % SEG_COLORS.length], 10, 3));
            l.setIconTextGap(6);
            legend.add(l);
        }
        card.add(legend, BorderLayout.SOUTH);
        return Proto.noStretch(card);
    }

    // ------------------------------------------------------------------ tarjetas de comida

    private static JComponent mealCard(MealTime meal, double kcal, List<DietPlanFood> lines, Map<Integer, Food> foods) {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());
        Proto.Photo photo = new Proto.Photo(Images.get(mealImage(meal)), mealFocusX(meal), mealFocusY(meal), 112, 32, 0)
                .shaded();
        photo.setLayout(new BorderLayout());
        photo.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        JPanel over = new JPanel(new BorderLayout());
        over.setOpaque(false);
        JLabel name = Proto.text(meal.getDisplayName(), "Bold", 17, Color.WHITE);
        over.add(name, BorderLayout.CENTER);
        JLabel pill = new JLabel(ScreenKit.number(kcal) + " kcal") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(Proto.font("Bold", 12));
        pill.setForeground(new Color(0x0B5F52));
        pill.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        over.add(pill, BorderLayout.EAST);
        photo.add(over, BorderLayout.SOUTH);
        card.add(photo, BorderLayout.NORTH);

        JPanel list = Proto.column();
        list.setBorder(BorderFactory.createEmptyBorder(6, 16, 12, 16));
        for (DietPlanFood line : lines) {
            if (line.getMealTime() != meal) {
                continue;
            }
            Food food = foods.get(line.getFoodId());
            if (food == null) {
                continue;
            }
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Proto.LINE),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
            JPanel texts = Proto.column();
            texts.add(Proto.left(Proto.wrapped(food.getName(), 105, "Medium", 14, Proto.INK)));
            texts.add(Proto.left(Proto.text(ScreenKit.number(line.getQuantityG()) + " g", null, 12, Proto.MUTED)));
            row.add(texts, BorderLayout.CENTER);
            JLabel k = Proto.wrapped(ScreenKit.number(intake(food, line.getQuantityG()).getCalories()) + " kcal", 40,
                    "SemiBold", 13, Proto.INK);
            k.setVerticalAlignment(SwingConstants.CENTER);
            row.add(k, BorderLayout.EAST);
            list.add(Proto.left(Proto.noStretch(row)));
        }
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private static JComponent spacer() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private static String mealImage(MealTime meal) {
        return switch (meal) {
            case BREAKFAST -> Images.MEAL_BREAKFAST;
            case LUNCH -> Images.MEAL_LUNCH;
            case DINNER -> Images.MEAL_DINNER;
            default -> Images.MEAL_SNACK;
        };
    }

    private static double mealFocusX(MealTime meal) {
        return switch (meal) {
            case MID_MORNING -> 0.30;
            case SNACK -> 0.62;
            default -> 0.5;
        };
    }

    private static double mealFocusY(MealTime meal) {
        return switch (meal) {
            case LUNCH -> 0.55;
            default -> 0.60;
        };
    }

    private void generate(boolean replacing) {
        if (replacing && !Messages.confirm(this,
                "Se generará una dieta nueva y la actual pasará a tu historial. ¿Continuar?")) {
            return;
        }
        try {
            services.dietPlans().generatePlan(services.currentUser().getIdUser());
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    /** Cuadrito de color para las leyendas. */
    static javax.swing.Icon square(Color color, int size, int arc) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(color);
                g2.fillRoundRect(x, y, size, size, arc * 2, arc * 2);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private static NutritionalSummary intake(Food food, double grams) {
        try {
            return NutritionCalculator.calculate(food, grams);
        } catch (ValidationException e) {
            return NutritionalSummary.empty();
        }
    }
}
