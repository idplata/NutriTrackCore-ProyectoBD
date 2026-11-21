package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.NutritionalIntake;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
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
import javax.swing.JPanel;

/**
 * Pantalla "Mi dieta": muestra el plan de dieta activo con su resumen y los
 * alimentos de cada comida del día. El botón de arriba genera un plan nuevo
 * con {@code DietPlanService.generatePlan} (el anterior queda en el historial).
 */
public class DietPanel extends ScreenKit.BlueBackground implements Refreshable {

    private static final MealTime[] MEAL_ORDER = {MealTime.BREAKFAST, MealTime.LUNCH, MealTime.SNACK, MealTime.DINNER};

    private final AppServices services;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();

    /**
     * Crea la pantalla.
     *
     * @param services servicios de la aplicación
     * @param navigate cómo ir a otra sección (se reserva para enlaces futuros)
     */
    public DietPanel(AppServices services, Consumer<String> navigate) {
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
            Optional<DietPlan> plan = services.dietPlans().getActivePlan(userId);
            if (plan.isEmpty()) {
                JButton generate = FormKit.primaryButton("Generar mi dieta", Icons.refresh(18, Color.WHITE), 230);
                generate.addActionListener(e -> generate(false));
                content.add(ScreenKit.header("Mi dieta", "Aún no tienes una dieta generada.", null));
                content.add(Box.createVerticalStrut(22));
                content.add(ScreenKit.left(ScreenKit.emptyState(Images.CARD_DIET, 0.72, "Genera tu dieta",
                        "La armamos con tus medidas y tus condiciones médicas.", generate)));
            } else {
                showPlan(plan.get(), services.conditions().listByUser(userId));
            }
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    private void showPlan(DietPlan plan, List<MedicalCondition> conditions) throws PersistenceExceptions {
        // Alimentos del plan (se buscan una sola vez cada uno).
        Map<Integer, Food> foods = new HashMap<>();
        for (DietPlanFood line : plan.getLines()) {
            if (!foods.containsKey(line.getFoodId())) {
                foods.put(line.getFoodId(), services.foods().getById(line.getFoodId()));
            }
        }

        List<String> names = new ArrayList<>();
        for (MedicalCondition c : conditions) {
            names.add(c.getName());
        }
        String subtitle = "Generada el " + ScreenKit.longDate(plan.getGenerationDate()) + " · "
                + (names.isEmpty() ? "Sin condiciones médicas registradas" : "Ajustada por " + ScreenKit.joinNames(names));

        JButton generate = ScreenKit.whiteButton("Generar nueva dieta", Icons.refresh(18, Theme.ACCENT));
        generate.addActionListener(e -> generate(true));
        content.add(ScreenKit.header("Mi dieta", subtitle, generate));
        content.add(Box.createVerticalStrut(22));

        NutritionalIntake total = NutritionalIntake.zero();
        for (DietPlanFood line : plan.getLines()) {
            total = total.add(foods.get(line.getFoodId()).calculateIntake(line.getQuantityG()));
        }
        content.add(ScreenKit.fixedHeight(createSummary(plan, total)));
        content.add(Box.createVerticalStrut(18));

        JPanel meals = new JPanel(new GridLayout(1, MEAL_ORDER.length, 18, 0));
        meals.setOpaque(false);
        for (MealTime meal : MEAL_ORDER) {
            meals.add(createMealCard(meal, plan.getLines(), foods));
        }
        content.add(ScreenKit.fixedHeight(meals));
        content.add(Box.createVerticalStrut(14));
        content.add(ScreenKit.left(ScreenKit.label("Porciones orientativas. Consulta a un profesional de la salud "
                + "antes de cambiar tu alimentación.", Theme.font(Font.PLAIN, 12), ScreenKit.ON_BLUE)));
    }

    private JComponent createSummary(DietPlan plan, NutritionalIntake total) {
        SideImageCard card = new SideImageCard(Images.get(Images.CARD_DIET), 230, 0.72);
        card.setLayout(new GridLayout(1, 5, 12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(0, 230 + 24, 0, 24));
        card.add(stat("Meta diaria", ScreenKit.number(plan.getTargetCalories()), "kcal"));
        card.add(stat("En el plan", ScreenKit.number(total.getCalories()), "kcal"));
        card.add(stat("Proteínas", ScreenKit.number(total.getProteins()), "g"));
        card.add(stat("Carbohidratos", ScreenKit.number(total.getCarbohydrates()), "g"));
        card.add(stat("Grasas", ScreenKit.number(total.getFats()), "g"));
        card.setPreferredSize(new Dimension(600, 132));
        return card;
    }

    private JComponent createMealCard(MealTime meal, List<DietPlanFood> lines, Map<Integer, Food> foods) {
        Card card = new Card();
        card.setPadding(18, 18, 16, 18);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        List<DietPlanFood> items = new ArrayList<>();
        double kcal = 0;
        for (DietPlanFood line : lines) {
            if (line.getMealTime() == meal) {
                items.add(line);
                kcal += foods.get(line.getFoodId()).calculateIntake(line.getQuantityG()).getCalories();
            }
        }

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(ScreenKit.label(mealName(meal), Theme.font("SemiBold", 16), Theme.TEXT), BorderLayout.WEST);
        head.add(ScreenKit.label(ScreenKit.number(kcal) + " kcal", Theme.font("SemiBold", 12), Theme.ACCENT),
                BorderLayout.EAST);
        card.add(ScreenKit.fixedHeight(head));
        card.add(Box.createVerticalStrut(4));

        if (items.isEmpty()) {
            card.add(Box.createVerticalStrut(6));
            card.add(ScreenKit.left(ScreenKit.label("Sin alimentos", Theme.font(Font.PLAIN, 13), ScreenKit.NOTE)));
        }
        for (DietPlanFood line : items) {
            Food food = foods.get(line.getFoodId());
            double itemKcal = food.calculateIntake(line.getQuantityG()).getCalories();
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xEEF2F4)),
                    BorderFactory.createEmptyBorder(9, 0, 9, 0)));
            JPanel texts = ScreenKit.column();
            texts.add(ScreenKit.wrapped(food.getName(), 95, Theme.font("Medium", 14), Theme.TEXT));
            texts.add(ScreenKit.label(ScreenKit.number(line.getQuantityG()) + " g", Theme.font(Font.PLAIN, 12),
                    ScreenKit.NOTE));
            row.add(texts, BorderLayout.CENTER);
            row.add(ScreenKit.label(ScreenKit.number(itemKcal) + " kcal", Theme.font("SemiBold", 13),
                    Theme.TEXT_LABEL), BorderLayout.EAST);
            card.add(ScreenKit.fixedHeight(row));
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    private void generate(boolean replacing) {
        if (replacing && !Messages.confirm(this,
                "Se generará una dieta nueva y la actual pasará a tu historial. ¿Continuar?")) {
            return;
        }
        try {
            services.dietPlans().generatePlan(services.session().getCurrentUser().getIdUser());
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private static JComponent stat(String caption, String value, String unit) {
        JPanel box = ScreenKit.column();
        box.add(ScreenKit.label(caption, Theme.font(Font.PLAIN, 12), ScreenKit.NOTE));
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(0, -4, 0, 0));
        line.add(ScreenKit.label(value, Theme.font(Font.BOLD, 24), Theme.TEXT));
        line.add(ScreenKit.label(unit, Theme.font("Medium", 13), Theme.TEXT_MUTED));
        box.add(ScreenKit.left(line));
        JPanel wrap = new JPanel(new java.awt.GridBagLayout());
        wrap.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.weightx = 1;
        wrap.add(box, c);
        return wrap;
    }

    private static String mealName(MealTime meal) {
        return switch (meal) {
            case BREAKFAST -> "Desayuno";
            case LUNCH -> "Almuerzo";
            case SNACK -> "Merienda";
            case DINNER -> "Cena";
        };
    }

    /** Tarjeta blanca con una foto ocupando su lado izquierdo. */
    private static final class SideImageCard extends JPanel {

        private final java.awt.Image image;
        private final int imageWidth;
        private final double focusY;

        SideImageCard(java.awt.Image image, int imageWidth, double focusY) {
            this.image = image;
            this.imageWidth = imageWidth;
            this.focusY = focusY;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 6; i > 0; i--) {
                    g2.setColor(new Color(8, 32, 40, 7));
                    g2.fillRoundRect(-i / 2, i, getWidth() + i, getHeight() - 6 + i / 2, 28 + i, 28 + i);
                }
                Shape shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() - 6, 28, 28);
                g2.setColor(Color.WHITE);
                g2.fill(shape);
                g2.clip(shape);
                Images.paintCover(g2, image, 0, 0, imageWidth, getHeight() - 6, focusY);
            } finally {
                g2.dispose();
            }
        }
    }
}
