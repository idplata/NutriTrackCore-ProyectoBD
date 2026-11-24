package com.nutritrackcore.ui;

import com.nutritrackcore.camera.CameraDialog;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.services.NutritionCalculator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Registrar consumo: el usuario elige la comida de su plan (desayuno, almuerzo…)
 * y la registra con una foto de su plato ({@link CameraDialog}). A la derecha,
 * "Tu día": total frente a la meta y cada comida con su estado.
 */
public class ConsumptionPanel extends ScreenKit.BlueBackground implements Refreshable {

    /** Diferencia (±15 %) con la que una comida se considera "según el plan". */
    private static final double TOLERANCE = 0.15;
    private static final Color INK = new Color(0x0F1F2A);
    private static final Color INK_2 = new Color(0x1E2D38);
    private static final Color MUTED = new Color(0x4F5E6B);
    private static final Color NOTE = new Color(0x5C6B77);
    private static final Color GREEN = new Color(0x0B7A69);
    private static final Color GREEN_DARK = new Color(0x0B5F52);
    private static final Color BORDER = new Color(0xD6DEE4);
    private static final Color LINE = new Color(0xEEF2F4);
    private static final Color ON_GREEN = new Color(0xD9F1EC);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("h:mm a", ScreenKit.ES_CO);

    /** Porciones de "¿Cuánto comiste?": clave, texto y fracción del plan. */
    private static final String[][] PORTIONS = {{"todo", "Todo", "1"}, {"mitad", "La mitad", "0.5"},
        {"poco", "Un poco", "0.25"}};

    private final AppServices services;
    private final Consumer<String> navigate;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();

    /** Comida seleccionada. */
    private MealTime selected;
    /** Porción elegida en "¿Cuánto comiste?". */
    private String portion = "todo";

    /**
     * @param services servicios de la aplicación
     * @param navigate cambia de sección
     */
    public ConsumptionPanel(AppServices services, Consumer<String> navigate) {
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
            content.add(Proto.left(Proto.pageTitle("Registrar consumo",
                    "Anota tus comidas completas de hoy y mira cómo vas con tu plan.")));
            content.add(Box.createVerticalStrut(24));

            Optional<DietPlan> plan = services.dietPlans().getActivePlan(userId);
            if (plan.isEmpty() || plan.get().getLines().isEmpty()) {
                content.add(Proto.left(noPlanCard()));
            } else {
                Map<MealTime, List<DietPlanFood>> meals = mealsOf(plan.get());
                Map<MealTime, List<ConsumptionRecord>> eaten = eatenToday(userId);
                if (selected == null || !meals.containsKey(selected)) {
                    selected = suggestedMeal(meals, eaten);
                }
                Map<MealTime, Double> planKcal = new EnumMap<>(MealTime.class);
                for (Map.Entry<MealTime, List<DietPlanFood>> e : meals.entrySet()) {
                    planKcal.put(e.getKey(), planKcal(e.getValue()));
                }

                JPanel grid = new JPanel(new GridBagLayout());
                grid.setOpaque(false);
                GridBagConstraints c = new GridBagConstraints();
                c.gridy = 0;
                c.anchor = GridBagConstraints.NORTH;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.weightx = 1;
                c.insets = new Insets(0, 0, 0, 22);
                JPanel left = Proto.column();
                left.add(Proto.left(mealPicker(meals, eaten, planKcal)));
                left.add(Box.createVerticalStrut(18));
                left.add(Proto.left(mealCard(meals.get(selected), planKcal.get(selected), eaten.get(selected))));
                grid.add(left, c);

                c.gridx = 1;
                c.weightx = 0;
                c.insets = new Insets(0, 0, 0, 0);
                JPanel aside = Proto.column();
                aside.add(Proto.left(todayCard(plan.get(), meals, eaten, planKcal)));
                aside.add(Box.createVerticalStrut(18));
                aside.add(Proto.left(Proto.wrapped("Cada plato se guarda con sus alimentos, así calculamos tus "
                        + "calorías y macronutrientes en Mi progreso.", 240, null, 12, Proto.ON_BLUE)));
                aside.setPreferredSize(new Dimension(344, aside.getPreferredSize().height));
                grid.add(aside, c);
                content.add(ScreenKit.fixedHeight(grid));
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    // ------------------------------------------------------------------ datos

    /** Alimentos del plan agrupados por comida, en el orden del día. */
    private static Map<MealTime, List<DietPlanFood>> mealsOf(DietPlan plan) {
        Map<MealTime, List<DietPlanFood>> meals = new EnumMap<>(MealTime.class);
        for (DietPlanFood line : plan.getLines()) {
            meals.computeIfAbsent(line.getMealTime(), k -> new ArrayList<>()).add(line);
        }
        return meals;
    }

    /** Registros de hoy agrupados por la comida que corresponde a su hora. */
    private Map<MealTime, List<ConsumptionRecord>> eatenToday(int userId) throws NutriTrackException {
        Map<MealTime, List<ConsumptionRecord>> eaten = new EnumMap<>(MealTime.class);
        for (ConsumptionRecord r : services.consumption().listByDay(userId, LocalDate.now())) {
            eaten.computeIfAbsent(mealAt(r.getDateTime().toLocalTime()), k -> new ArrayList<>()).add(r);
        }
        return eaten;
    }

    /** Comida que corresponde a una hora del día. */
    private static MealTime mealAt(LocalTime time) {
        int h = time.getHour();
        if (h < 10) {
            return MealTime.BREAKFAST;
        } else if (h < 12) {
            return MealTime.MID_MORNING;
        } else if (h < 15) {
            return MealTime.LUNCH;
        } else if (h < 18) {
            return MealTime.SNACK;
        }
        return MealTime.DINNER;
    }

    /** Hora con la que se guarda cada comida (dentro de su franja). */
    private static LocalTime timeOf(MealTime meal) {
        return switch (meal) {
            case BREAKFAST -> LocalTime.of(7, 30);
            case MID_MORNING -> LocalTime.of(10, 0);
            case LUNCH -> LocalTime.of(12, 30);
            case SNACK -> LocalTime.of(16, 0);
            case DINNER -> LocalTime.of(19, 30);
        };
    }

    /** Primera comida pendiente; si todas están registradas, la primera. */
    private static MealTime suggestedMeal(Map<MealTime, List<DietPlanFood>> meals,
                                          Map<MealTime, List<ConsumptionRecord>> eaten) {
        for (MealTime m : meals.keySet()) {
            if (!eaten.containsKey(m)) {
                return m;
            }
        }
        return meals.keySet().iterator().next();
    }

    private double planKcal(List<DietPlanFood> lines) throws NutriTrackException {
        double kcal = 0;
        for (DietPlanFood l : lines) {
            kcal += kcal(services.foods().getById(l.getFoodId()), l.getQuantityG());
        }
        return kcal;
    }

    private static double eatenKcal(List<ConsumptionRecord> records) {
        return records == null ? 0 : records.stream().mapToDouble(ConsumptionRecord::getEstimatedCalories).sum();
    }

    private static double kcal(Food food, double grams) {
        try {
            return NutritionCalculator.calculate(food, grams).getCalories();
        } catch (ValidationException e) {
            return 0;
        }
    }

    /** Estado de una comida registrada frente al plan: "plan", "menos" u "otra". */
    private static String kind(double kcal, double plan) {
        if (plan <= 0 || Math.abs(kcal - plan) <= plan * TOLERANCE) {
            return "plan";
        }
        return kcal < plan ? "menos" : "otra";
    }

    // ------------------------------------------------------------------ ¿Qué comida vas a registrar?

    private JComponent mealPicker(Map<MealTime, List<DietPlanFood>> meals,
                                  Map<MealTime, List<ConsumptionRecord>> eaten, Map<MealTime, Double> planKcal) {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        card.add(Proto.text("¿Qué comida vas a registrar?", "SemiBold", 17, INK), BorderLayout.NORTH);
        JPanel row = new JPanel(new GridLayout(1, Math.max(4, meals.size()), 10, 0));
        row.setOpaque(false);
        for (MealTime m : meals.keySet()) {
            List<ConsumptionRecord> done = eaten.get(m);
            String sub = done != null ? ScreenKit.number(eatenKcal(done)) + " kcal"
                    : "Plan: " + ScreenKit.number(planKcal.get(m)) + " kcal";
            row.add(new MealChip(m.getDisplayName(), sub, done != null, m == selected, () -> {
                selected = m;
                portion = "todo";
                refresh();
            }));
        }
        for (int i = meals.size(); i < 4; i++) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            row.add(empty);
        }
        row.setPreferredSize(new Dimension(10, 68));
        card.add(row, BorderLayout.CENTER);
        return Proto.noStretch(card);
    }

    // ------------------------------------------------------------------ tarjeta de la comida

    private JComponent mealCard(List<DietPlanFood> lines, double plan, List<ConsumptionRecord> records)
            throws NutriTrackException {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());

        Proto.Photo photo = new Proto.Photo(Images.get(mealImage(selected)), mealFocusX(selected),
                mealFocusY(selected), 110, 32, 0).shaded();
        photo.setLayout(new BorderLayout());
        photo.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        JPanel names = Proto.column();
        names.add(Proto.left(Proto.text("Registrando", null, 12, new Color(0xE4F2EF))));
        names.add(Proto.left(Proto.text(selected.getDisplayName(), "Bold", 22, Color.WHITE)));
        bottom.add(names, BorderLayout.CENTER);
        JPanel pillHolder = new JPanel(new GridBagLayout());
        pillHolder.setOpaque(false);
        GridBagConstraints pc = new GridBagConstraints();
        pc.anchor = GridBagConstraints.SOUTH;
        pc.weighty = 1;
        pillHolder.add(pill("Plan: " + ScreenKit.number(plan) + " kcal", Color.WHITE, GREEN_DARK, "Bold"), pc);
        bottom.add(pillHolder, BorderLayout.EAST);
        photo.add(bottom, BorderLayout.SOUTH);
        card.add(photo, BorderLayout.NORTH);

        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        if (records != null) {
            JPanel note = new Proto.Rounded(new Color(0xFFF8E1), new Color(0xF3D3A6), 1, 20);
            note.setLayout(new BorderLayout());
            note.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            note.add(Proto.text("Ya registraste tu " + selected.getDisplayName().toLowerCase()
                    + ". Si guardas de nuevo, la reemplazas.", null, 12, new Color(0x7A4A00)));
            col.add(Proto.left(Proto.noStretch(note)));
            col.add(Box.createVerticalStrut(14));
        }
        for (DietPlanFood l : lines) {
            Food food = services.foods().getById(l.getFoodId());
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setOpaque(false);
            r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
            JPanel texts = Proto.column();
            texts.add(Proto.left(Proto.text(food.getName(), "Medium", 14, INK)));
            texts.add(Proto.left(Proto.text(ScreenKit.number(l.getQuantityG()) + " g", null, 12, MUTED)));
            r.add(texts, BorderLayout.CENTER);
            JLabel k = Proto.text(ScreenKit.number(kcal(food, l.getQuantityG())) + " kcal", "SemiBold", 13, INK);
            r.add(k, BorderLayout.EAST);
            col.add(Proto.left(Proto.noStretch(r)));
        }
        col.add(Box.createVerticalStrut(14));

        col.add(Proto.left(Proto.text("¿Cuánto comiste?", "SemiBold", 13, INK_2)));
        col.add(Box.createVerticalStrut(8));
        JPanel portions = new JPanel(new GridLayout(1, 3, 8, 0));
        portions.setOpaque(false);
        for (String[] p : PORTIONS) {
            double part = Double.parseDouble(p[2]);
            portions.add(new PortionChip(p[1], ScreenKit.number(plan * part) + " kcal", p[0].equals(portion), () -> {
                portion = p[0];
                refresh();
            }));
        }
        portions.setPreferredSize(new Dimension(10, 52));
        col.add(Proto.left(Proto.noStretch(portions)));
        col.add(Box.createVerticalStrut(14));

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)));
        footer.add(Proto.text("Tómale una foto a tu plato para registrarlo", "SemiBold", 14, INK_2),
                BorderLayout.CENTER);
        JButton photoButton = new CameraButton();
        photoButton.addActionListener(e -> takePhoto(plan));
        footer.add(photoButton, BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(footer)));
        card.add(col, BorderLayout.CENTER);
        return card;
    }

    private JComponent noPlanCard() {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JPanel texts = Proto.column();
        texts.add(Proto.left(Proto.text("Aún no tienes una dieta", "SemiBold", 18, INK)));
        texts.add(Box.createVerticalStrut(4));
        texts.add(Proto.left(Proto.text("Genera tu dieta para registrar cada comida de tu plan.", null, 14, MUTED)));
        card.add(texts, BorderLayout.CENTER);
        JButton go = Proto.greenButton("Ir a Mi dieta");
        go.addActionListener(e -> navigate.accept(AppShellPanel.DIET));
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(go);
        card.add(holder, BorderLayout.EAST);
        return Proto.noStretch(card);
    }

    // ------------------------------------------------------------------ foto del plato

    /**
     * Abre la ventana de la foto. Si se guarda una comida nueva, reemplaza lo que ya
     * estaba registrado en esa comida y muestra el aviso.
     */
    private void takePhoto(double plan) {
        MealTime meal = selected;
        int userId = services.currentUser().getIdUser();
        try {
            Set<Integer> before = new HashSet<>();
            List<ConsumptionRecord> old = eatenToday(userId).getOrDefault(meal, List.of());
            for (ConsumptionRecord r : old) {
                before.add(r.getRecordId());
            }
            CameraDialog.open(SwingUtilities.getWindowAncestor(this), services.camera(), services.session(),
                    LocalDate.now().atTime(timeOf(meal)), meal.getDisplayName(), Images.get(mealImage(meal)), plan);
            List<ConsumptionRecord> now = eatenToday(userId).getOrDefault(meal, List.of());
            ConsumptionRecord added = null;
            for (ConsumptionRecord r : now) {
                if (!before.contains(r.getRecordId())) {
                    added = r;
                }
            }
            if (added != null) {
                for (ConsumptionRecord r : old) {
                    services.consumption().delete(r.getRecordId());      // "si guardas de nuevo, la reemplazas"
                }
                MealTime next = null;
                Map<MealTime, List<ConsumptionRecord>> eaten = eatenToday(userId);
                Optional<DietPlan> active = services.dietPlans().getActivePlan(userId);
                if (active.isPresent()) {
                    for (MealTime m : mealsOf(active.get()).keySet()) {
                        if (next == null && !eaten.containsKey(m)) {
                            next = m;
                        }
                    }
                }
                selected = next != null ? next : meal;
                portion = "todo";
                Toast.show(this, meal.getDisplayName() + " registrado: "
                        + ScreenKit.number(added.getEstimatedCalories()) + " kcal.");
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    // ------------------------------------------------------------------ tu día

    private JComponent todayCard(DietPlan plan, Map<MealTime, List<DietPlanFood>> meals,
                                 Map<MealTime, List<ConsumptionRecord>> eaten, Map<MealTime, Double> planKcal)
            throws NutriTrackException {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));
        JPanel col = Proto.column();

        int done = 0;
        for (MealTime m : meals.keySet()) {
            if (eaten.containsKey(m)) {
                done++;
            }
        }
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Proto.text("Tu día", "SemiBold", 17, INK), BorderLayout.WEST);
        head.add(Proto.text(done + " de " + meals.size() + (meals.size() == 1 ? " comida" : " comidas"), null, 12,
                MUTED), BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(head)));
        col.add(Box.createVerticalStrut(10));

        double goal = plan.getTargetCalories();
        double total = 0;
        for (List<ConsumptionRecord> list : eaten.values()) {
            total += eatenKcal(list);
        }
        JPanel kcalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        kcalRow.setOpaque(false);
        kcalRow.setBorder(BorderFactory.createEmptyBorder(0, -6, 0, 0));
        kcalRow.add(Proto.text(ScreenKit.number(total), "Bold", 30, INK));
        JLabel of = Proto.text("/ " + ScreenKit.number(goal) + " kcal", null, 13, MUTED);
        of.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        kcalRow.add(of);
        col.add(Proto.left(kcalRow));
        col.add(Box.createVerticalStrut(8));
        col.add(Proto.left(new ProgressBar(goal > 0 ? total / goal : 0)));
        col.add(Box.createVerticalStrut(8));
        double left = goal - total;
        col.add(Proto.left(Proto.text(left >= 0 ? "Te quedan " + ScreenKit.number(left) + " kcal según tu plan"
                : "Te pasaste " + ScreenKit.number(-left) + " kcal de tu meta", "SemiBold", 12,
                left >= 0 ? GREEN : Proto.DANGER)));
        col.add(Box.createVerticalStrut(14));

        for (MealTime m : meals.keySet()) {
            col.add(Proto.left(logRow(m, planKcal.get(m), eaten.get(m))));
        }
        for (Map.Entry<MealTime, List<ConsumptionRecord>> e : eaten.entrySet()) {
            if (!meals.containsKey(e.getKey())) {
                col.add(Proto.left(logRow(e.getKey(), 0, e.getValue())));   // comida fuera del plan
            }
        }
        card.add(col, BorderLayout.CENTER);
        return card;
    }

    private JComponent logRow(MealTime meal, double planKcal, List<ConsumptionRecord> records)
            throws NutriTrackException {
        boolean done = records != null;
        double kcal = eatenKcal(records);
        String[] pill = !done ? new String[]{"Pendiente", "EEF2F4", "5C6B77"}
                : switch (kind(kcal, planKcal)) {
                    case "plan" -> new String[]{"Según el plan", "E6F3F0", "0B5F52"};
                    case "menos" -> new String[]{"Comiste menos", "E3EEF8", "1F5F99"};
                    default -> new String[]{"Más de lo planeado", "FFF3E3", "7A3E00"};
                };
        Color pillFg = new Color(Integer.parseInt(pill[2], 16));

        JPanel r = new JPanel(new BorderLayout(12, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)));
        JPanel dotHolder = new JPanel(new BorderLayout());
        dotHolder.setOpaque(false);
        dotHolder.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        dotHolder.add(new Dot(done ? pillFg : null), BorderLayout.NORTH);
        r.add(dotHolder, BorderLayout.WEST);

        JPanel col = Proto.column();
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        String time = done ? HOUR.format(records.get(records.size() - 1).getDateTime()).replace('\u202f', ' ')
                .replace('\u00a0', ' ')
                : "Plan: " + ScreenKit.number(planKcal) + " kcal";
        top.add(Proto.text(meal.getDisplayName() + " · " + time, "SemiBold", 11, MUTED), BorderLayout.CENTER);
        top.add(pill(pill[0], new Color(Integer.parseInt(pill[1], 16)), pillFg, "SemiBold"), BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(top)));
        col.add(Box.createVerticalStrut(2));
        col.add(Proto.left(Proto.wrapped(done ? title(records) : "Aún no la registras", 230, "SemiBold", 14,
                done ? INK : new Color(0x8A97A2))));
        if (done) {
            boolean photo = records.stream().anyMatch(x -> x.getEstimationLevel() != null);
            JPanel detail = new JPanel(new BorderLayout());
            detail.setOpaque(false);
            detail.add(Proto.text((photo ? "Desde la foto" : "Registro manual") + " · " + ScreenKit.number(kcal)
                    + " kcal", null, 12, NOTE), BorderLayout.CENTER);
            JButton delete = Proto.link("Borrar", Proto.DANGER, 12);
            delete.setFont(Proto.font("SemiBold", 12));
            delete.getAccessibleContext().setAccessibleName("Borrar " + meal.getDisplayName());
            delete.addActionListener(e -> deleteMeal(meal, records));
            detail.add(delete, BorderLayout.EAST);
            col.add(Box.createVerticalStrut(2));
            col.add(Proto.left(Proto.noStretch(detail)));
        }
        r.add(col, BorderLayout.CENTER);
        return Proto.noStretch(r);
    }

    /** "Arroz blanco y Pollo apanado y 2 más", con los alimentos registrados en la comida. */
    private String title(List<ConsumptionRecord> records) throws NutriTrackException {
        Set<String> names = new LinkedHashSet<>();
        for (ConsumptionRecord r : records) {
            for (ConsumptionRecordByFood line : services.consumption().listFoods(r.getRecordId())) {
                names.add(services.foods().getById(line.getFoodId()).getName());
            }
        }
        List<String> list = new ArrayList<>(names);
        if (list.isEmpty()) {
            return "Comida registrada";
        }
        String t = String.join(" y ", list.subList(0, Math.min(2, list.size())));
        return list.size() > 2 ? t + " y " + (list.size() - 2) + " más" : t;
    }

    private void deleteMeal(MealTime meal, List<ConsumptionRecord> records) {
        if (!Messages.confirm(this, "¿Borrar lo registrado en " + meal.getDisplayName().toLowerCase() + "?")) {
            return;
        }
        try {
            for (ConsumptionRecord r : records) {
                services.consumption().delete(r.getRecordId());
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    // ------------------------------------------------------------------ piezas

    static String mealImage(MealTime meal) {
        return switch (meal) {
            case BREAKFAST -> Images.MEAL_BREAKFAST;
            case LUNCH -> Images.MEAL_LUNCH;
            case DINNER -> Images.MEAL_DINNER;
            default -> Images.MEAL_SNACK;
        };
    }

    static double mealFocusX(MealTime meal) {
        return switch (meal) {
            case SNACK -> 0.62;
            case DINNER -> 0.45;
            default -> 0.5;
        };
    }

    static double mealFocusY(MealTime meal) {
        return switch (meal) {
            case BREAKFAST, SNACK -> 0.60;
            case DINNER -> 0.45;
            default -> 0.55;
        };
    }

    /** Píldora de texto con fondo redondeado. */
    private static JLabel pill(String text, Color bg, Color fg, String weight) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Proto.font(weight, 10.5f));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return l;
    }

    /** Botón de una comida: nombre y kcal (registradas con ✓ o del plan). */
    private static final class MealChip extends JButton {

        private final boolean active;
        private final boolean done;

        MealChip(String name, String sub, boolean done, boolean active, Runnable onClick) {
            this.active = active;
            this.done = done;
            setLayout(new BorderLayout());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            JPanel texts = Proto.column();
            texts.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 10));
            texts.add(Proto.left(Proto.text(name, "SemiBold", 15, active ? Color.WHITE : INK)));
            JLabel s = Proto.text(sub, done ? "SemiBold" : null, 12, active ? ON_GREEN : done ? GREEN : MUTED);
            if (done) {
                s.setIcon(Icons.check(11, active ? ON_GREEN : GREEN));
                s.setIconTextGap(4);
            }
            texts.add(Box.createVerticalStrut(2));
            texts.add(Proto.left(s));
            add(texts, BorderLayout.CENTER);
            getAccessibleContext().setAccessibleName(name + ", " + sub);
            addActionListener(e -> onClick.run());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            if (active) {
                g2.setColor(GREEN);
                g2.fillRoundRect(0, 0, w, h, 24, 24);
            } else {
                g2.setColor(done ? new Color(0xF2FAF8) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 24, 24);
                g2.setColor(done ? new Color(0x9FD3C7) : BORDER);
                g2.setStroke(new java.awt.BasicStroke(done ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, w, h, 24, 24);
            }
            g2.dispose();
        }
    }

    /** Opción de "¿Cuánto comiste?": texto y kcal debajo. */
    private static final class PortionChip extends JButton {

        private final boolean active;

        PortionChip(String label, String sub, boolean active, Runnable onClick) {
            this.active = active;
            setLayout(new GridBagLayout());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            JPanel texts = Proto.column();
            JLabel l = Proto.text(label, active ? "SemiBold" : null, 13, active ? Color.WHITE : INK_2);
            l.setAlignmentX(CENTER_ALIGNMENT);
            JLabel s = Proto.text(sub, "Medium", 11, active ? ON_GREEN : NOTE);
            s.setAlignmentX(CENTER_ALIGNMENT);
            texts.add(l);
            texts.add(s);
            add(texts);
            getAccessibleContext().setAccessibleName(label + ", " + sub);
            addActionListener(e -> onClick.run());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            if (active) {
                g2.setColor(GREEN);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
            g2.dispose();
        }
    }

    /** Botón verde "Registrar con foto" con la cámara dentro de un círculo blanco. */
    private static final class CameraButton extends JButton {

        CameraButton() {
            super("Registrar con foto");
            setFont(Proto.font("Bold", 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIcon(new javax.swing.Icon() {
                @Override
                public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = Proto.smooth(g);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x, y, 26, 26);
                    g2.dispose();
                    Icons.camera(15, GREEN).paintIcon(c, g, x + 5, y + 5);
                }

                @Override
                public int getIconWidth() {
                    return 26;
                }

                @Override
                public int getIconHeight() {
                    return 26;
                }
            });
            setIconTextGap(8);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 20));
            setPreferredSize(new Dimension(getPreferredSize().width, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(getModel().isRollover() ? new Color(0x096A5B) : GREEN);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Punto del registro: relleno del color del estado, o un aro gris si está pendiente. */
    private static final class Dot extends JComponent {

        private final Color color;

        Dot(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            if (color != null) {
                g2.setColor(color);
                g2.fillOval(0, 0, 12, 12);
            } else {
                g2.setColor(new Color(0xC4CDD3));
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(1, 1, 10, 10);
            }
            g2.dispose();
        }
    }

    /** Barra de avance hacia la meta de calorías (verde, naranja si se pasa). */
    private static final class ProgressBar extends JPanel {

        private final double ratio;

        ProgressBar(double ratio) {
            this.ratio = ratio;
            setOpaque(false);
            setPreferredSize(new Dimension(290, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(new Color(0xE6ECEF));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(ratio > 1 ? new Color(0xFF5A1F) : new Color(0x00C46A));
            int w = (int) Math.round(getWidth() * Math.min(1, Math.max(0, ratio)));
            g2.fillRoundRect(0, 0, w, getHeight(), 10, 10);
            g2.dispose();
        }
    }
}
