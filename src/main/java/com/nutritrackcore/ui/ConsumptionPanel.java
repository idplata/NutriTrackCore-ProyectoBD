package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.services.ConsumptionService;
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
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Registrar consumo: el usuario elige una comida de su plan (desayuno,
 * almuerzo…), indica cuánto comió y la registra con una foto de su plato.
 * A la derecha ve su día: total frente a la meta y cada comida con su estado.
 */
public class ConsumptionPanel extends ScreenKit.BlueBackground implements Refreshable {

    /** Diferencia (±15 %) con la que una comida se considera "según el plan". */
    private static final double TOLERANCE = 0.15;
    private static final Color AMBER = new Color(0x9A5B00);
    private static final Color AMBER_SOFT = new Color(0xFFF1D6);
    private static final Color BLUE_TEXT = new Color(0x1F4FB8);

    private final AppServices services;
    private final Consumer<String> navigate;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();

    /** Comida seleccionada en las pestañas. */
    private MealTime selected;
    /** Porción elegida en "¿Cuánto comiste?" (100, 50 o 25). */
    private String portion = "100";

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
            content.add(Proto.left(Proto.pageTitle("Registrar consumo", "Hoy, " + ScreenKit.longDate(LocalDate.now())
                    + ". Registra cada comida de tu plan con una foto de tu plato.")));
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

                JPanel grid = new JPanel(new GridBagLayout());
                grid.setOpaque(false);
                GridBagConstraints c = new GridBagConstraints();
                c.gridy = 0;
                c.anchor = GridBagConstraints.NORTH;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.weightx = 1.25;
                c.insets = new Insets(0, 0, 0, 20);
                JPanel left = Proto.column();
                left.add(Proto.left(mealTabs(meals, eaten)));
                left.add(Box.createVerticalStrut(16));
                left.add(Proto.left(mealCard(meals.get(selected), eaten.get(selected))));
                grid.add(left, c);
                c.gridx = 1;
                c.weightx = 1;
                c.insets = new Insets(0, 0, 0, 0);
                grid.add(todayCard(plan.get(), meals, eaten), c);
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

    /** Primera comida pendiente cuya hora ya pasó; si no hay, la primera pendiente. */
    private static MealTime suggestedMeal(Map<MealTime, List<DietPlanFood>> meals,
                                          Map<MealTime, List<ConsumptionRecord>> eaten) {
        MealTime now = mealAt(LocalTime.now());
        MealTime firstPending = null;
        for (MealTime m : meals.keySet()) {
            if (!eaten.containsKey(m)) {
                if (firstPending == null) {
                    firstPending = m;
                }
                if (m.getDayOrder() <= now.getDayOrder()) {
                    return m;
                }
            }
        }
        return firstPending != null ? firstPending : meals.keySet().iterator().next();
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

    // ------------------------------------------------------------------ pestañas y tarjeta de la comida

    private JComponent mealTabs(Map<MealTime, List<DietPlanFood>> meals,
                                Map<MealTime, List<ConsumptionRecord>> eaten) throws NutriTrackException {
        JPanel row = new JPanel(new GridLayout(1, meals.size(), 8, 0));
        row.setOpaque(false);
        for (Map.Entry<MealTime, List<DietPlanFood>> e : meals.entrySet()) {
            MealTime m = e.getKey();
            boolean done = eaten.containsKey(m);
            String sub = done ? ScreenKit.number(eatenKcal(eaten.get(m))) + " kcal"
                    : "Plan: " + ScreenKit.number(planKcal(e.getValue())) + " kcal";
            row.add(new MealTab(m.getDisplayName(), sub, done, m == selected, () -> {
                selected = m;
                portion = "100";
                refresh();
            }));
        }
        row.setPreferredSize(new Dimension(10, 58));
        return Proto.noStretch(row);
    }

    private JComponent mealCard(List<DietPlanFood> lines, List<ConsumptionRecord> records)
            throws NutriTrackException {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());

        double plan = planKcal(lines);
        Proto.Photo photo = new Proto.Photo(Images.get(mealImage(selected)), 0.5, 0.5, 130, 32, 0).shaded();
        photo.setLayout(new BorderLayout());
        photo.setBorder(BorderFactory.createEmptyBorder(0, 24, 16, 20));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(Proto.text("Registrando " + selected.getDisplayName(), "Bold", 21, Color.WHITE),
                BorderLayout.WEST);
        bottom.add(pill("Plan: " + ScreenKit.number(plan) + " kcal", new Color(255, 255, 255, 235), Proto.GREEN),
                BorderLayout.EAST);
        photo.add(bottom, BorderLayout.SOUTH);
        card.add(photo, BorderLayout.NORTH);

        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        col.add(Proto.left(Proto.text("Lo que dice tu plan", "SemiBold", 15, Proto.INK)));
        col.add(Box.createVerticalStrut(6));
        for (DietPlanFood l : lines) {
            Food food = services.foods().getById(l.getFoodId());
            JPanel r = new JPanel(new BorderLayout(10, 0));
            r.setOpaque(false);
            r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Proto.LINE),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));
            r.add(Proto.text(food.getName(), "Medium", 14, Proto.INK_2), BorderLayout.CENTER);
            r.add(Proto.text(ScreenKit.number(l.getQuantityG()) + " g · "
                    + ScreenKit.number(kcal(food, l.getQuantityG())) + " kcal", null, 13, Proto.NOTE), BorderLayout.EAST);
            col.add(Proto.left(Proto.noStretch(r)));
        }
        col.add(Box.createVerticalStrut(16));

        col.add(Proto.left(Proto.text("¿Cuánto comiste?", "SemiBold", 15, Proto.INK)));
        col.add(Box.createVerticalStrut(8));
        Proto.ChoiceGrid amount = new Proto.ChoiceGrid(3, Proto.options("100", "Todo", "50", "La mitad", "25",
                "Un poco"), Proto.GREEN, 13);
        amount.setValue(portion);
        amount.onChange(() -> portion = amount.getValue() == null ? "100" : amount.getValue());
        col.add(Proto.left(amount));
        col.add(Box.createVerticalStrut(18));

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Proto.LINE),
                BorderFactory.createEmptyBorder(16, 0, 0, 0)));
        String hint = records == null ? "Tómale una foto a tu plato para registrarlo"
                : "Ya registraste esta comida (" + ScreenKit.number(eatenKcal(records)) + " kcal). Puedes agregar otra foto.";
        footer.add(Proto.wrapped(hint, 250, null, 13, Proto.MUTED), BorderLayout.CENTER);
        JButton photoButton = Proto.withIcon(Proto.greenButton("Registrar con foto"), Icons.camera(18, Color.WHITE),
                false);
        photoButton.addActionListener(e -> takePhoto(lines));
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(photoButton);
        footer.add(holder, BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(footer)));
        card.add(col, BorderLayout.CENTER);
        return card;
    }

    private JComponent noPlanCard() {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JPanel texts = Proto.column();
        texts.add(Proto.left(Proto.text("Aún no tienes una dieta", "SemiBold", 18, Proto.INK)));
        texts.add(Box.createVerticalStrut(4));
        texts.add(Proto.left(Proto.text("Genera tu dieta para registrar cada comida de tu plan.", null, 14,
                Proto.MUTED)));
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

    /** Pide la foto del plato y abre la revisión de lo que hay en él. */
    private void takePhoto(List<DietPlanFood> lines) {
        Image image = choosePhoto();
        if (image == null) {
            return;
        }
        new PlateDialog(image, lines).setVisible(true);
    }

    private Image choosePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Foto de tu plato");
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (jpg, png)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        try {
            Image img = ImageIO.read(file);
            if (img == null) {
                Messages.showInfo(this, "Ese archivo no es una imagen válida.");
            }
            return img;
        } catch (java.io.IOException ex) {
            Messages.showInfo(this, "No se pudo abrir la foto: " + ex.getMessage());
            return null;
        }
    }

    /** Revisión del plato: foto, alimentos con sus gramos, comparación con el plan y guardar. */
    private final class PlateDialog extends JDialog {

        private final Map<Food, Proto.Stepper> items = new LinkedHashMap<>();
        private final JPanel rows = Proto.column();
        private final JLabel compare = new JLabel();
        private final double plan;
        private Proto.Photo photo;

        PlateDialog(Image image, List<DietPlanFood> lines) {
            super(SwingUtilities.getWindowAncestor(ConsumptionPanel.this), "Tu plato",
                    ModalityType.APPLICATION_MODAL);
            double factor = Integer.parseInt(portion) / 100.0;
            double p = 0;
            for (DietPlanFood l : lines) {
                try {
                    Food food = services.foods().getById(l.getFoodId());
                    p += kcal(food, l.getQuantityG());
                    addItem(food, Math.max(10, Math.round(l.getQuantityG() * factor / 10.0) * 10));
                } catch (NutriTrackException ex) {
                    Messages.showError(ConsumptionPanel.this, ex);
                }
            }
            plan = p;

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createLineBorder(Proto.FIELD_BORDER));

            photo = new Proto.Photo(image, 0.5, 0.5, 170, 0, 0).shaded();
            photo.setLayout(new BorderLayout());
            photo.setBorder(BorderFactory.createEmptyBorder(0, 22, 12, 22));
            JPanel ptxt = Proto.column();
            ptxt.add(Proto.left(Proto.text("Tu plato · " + selected.getDisplayName(), null, 12, new Color(0xD5E6EA))));
            ptxt.add(Proto.left(Proto.text("¿Esto es lo que comiste?", "Bold", 19, Color.WHITE)));
            photo.add(ptxt, BorderLayout.SOUTH);
            root.add(photo, BorderLayout.NORTH);

            JPanel col = Proto.column();
            col.setOpaque(true);
            col.setBackground(Color.WHITE);
            col.setBorder(BorderFactory.createEmptyBorder(16, 22, 18, 22));
            col.add(Proto.left(Proto.wrapped("Revisa que coincida con tu plato y ajusta los gramos.", 440, null, 13,
                    Proto.MUTED)));
            col.add(Box.createVerticalStrut(8));
            JScrollPane scroll = new JScrollPane(rows);
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getViewport().setBackground(Color.WHITE);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setPreferredSize(new Dimension(500, 200));
            scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            col.add(Proto.left(scroll));
            col.add(Box.createVerticalStrut(10));

            SearchBox<Food> search = new SearchBox<>("Agregar otro alimento (arroz, pollo, banano…)", Food::getName,
                    food -> {
                        if (items.keySet().stream().noneMatch(f -> f.getFoodId() == food.getFoodId())) {
                            addItem(food, 100);
                            rebuild();
                        }
                    });
            try {
                search.setItems(services.foods().listActive());
            } catch (NutriTrackException ex) {
                Messages.showError(ConsumptionPanel.this, ex);
            }
            col.add(Proto.left(ScreenKit.fixedHeight(search)));
            col.add(Box.createVerticalStrut(12));
            compare.setFont(Proto.font("SemiBold", 13));
            col.add(Proto.left(compare));
            col.add(Box.createVerticalStrut(14));

            JPanel actions = new JPanel(new BorderLayout(10, 0));
            actions.setOpaque(false);
            JButton again = Proto.withIcon(Proto.outlineButton("Tomar otra"), Icons.camera(16, Proto.GREEN), false);
            again.addActionListener(e -> {
                Image other = choosePhoto();
                if (other != null) {
                    Proto.Photo next = new Proto.Photo(other, 0.5, 0.5, 170, 0, 0).shaded();
                    next.setLayout(new BorderLayout());
                    next.setBorder(photo.getBorder());
                    next.add(ptxt, BorderLayout.SOUTH);
                    root.remove(photo);
                    photo = next;
                    root.add(photo, BorderLayout.NORTH);
                    root.revalidate();
                    root.repaint();
                }
            });
            JButton cancel = Proto.cancelButton("Cancelar");
            cancel.addActionListener(e -> dispose());
            JButton save = Proto.greenButton("Guardar comida");
            save.addActionListener(e -> save());
            actions.add(again, BorderLayout.WEST);
            JPanel end = new JPanel(new GridLayout(1, 2, 10, 0));
            end.setOpaque(false);
            end.add(cancel);
            end.add(save);
            actions.add(end, BorderLayout.EAST);
            col.add(Proto.left(Proto.noStretch(actions)));
            root.add(col, BorderLayout.CENTER);

            setUndecorated(true);
            setContentPane(root);
            setSize(560, 630);
            setLocationRelativeTo(ConsumptionPanel.this);
            getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
            rebuild();
        }

        private void addItem(Food food, double grams) {
            Proto.Stepper s = new Proto.Stepper(grams, 0, 2000, 10, false, Proto.GREEN, Proto.GREEN_SOFT,
                    "gramos de " + food.getName());
            s.setPreferredSize(new Dimension(130, 36));
            s.setMaximumSize(new Dimension(130, 36));
            s.onChange(this::updateTotals);
            items.put(food, s);
        }

        private void rebuild() {
            rows.removeAll();
            rows.setOpaque(true);
            rows.setBackground(Color.WHITE);
            for (Map.Entry<Food, Proto.Stepper> e : items.entrySet()) {
                Food food = e.getKey();
                JPanel r = new JPanel(new BorderLayout(10, 0));
                r.setOpaque(false);
                r.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Proto.LINE),
                        BorderFactory.createEmptyBorder(6, 0, 6, 0)));
                r.add(Proto.wrapped(food.getName(), 200, "Medium", 14, Proto.INK_2), BorderLayout.CENTER);
                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
                right.setOpaque(false);
                right.add(e.getValue());
                right.add(Proto.text("g", null, 13, Proto.NOTE));
                JButton remove = new JButton(Icons.trash(16, Proto.DANGER));
                remove.setContentAreaFilled(false);
                remove.setBorderPainted(false);
                remove.setFocusPainted(false);
                remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                remove.setToolTipText("Quitar");
                remove.getAccessibleContext().setAccessibleName("Quitar " + food.getName());
                remove.addActionListener(ev -> {
                    items.remove(food);
                    rebuild();
                });
                right.add(remove);
                r.add(right, BorderLayout.EAST);
                rows.add(Proto.left(Proto.noStretch(r)));
            }
            if (items.isEmpty()) {
                rows.add(Proto.left(Proto.text("Agrega lo que hay en tu plato.", null, 13, Proto.MUTED)));
            }
            rows.revalidate();
            rows.repaint();
            updateTotals();
        }

        private double total() {
            double t = 0;
            for (Map.Entry<Food, Proto.Stepper> e : items.entrySet()) {
                t += kcal(e.getKey(), e.getValue().getValue());
            }
            return t;
        }

        private void updateTotals() {
            double t = total();
            String verdict;
            Color color;
            if (plan <= 0 || Math.abs(t - plan) <= plan * TOLERANCE) {
                verdict = "Se parece a tu plan";
                color = Proto.GREEN;
            } else if (t < plan) {
                verdict = "Menos que tu plan";
                color = BLUE_TEXT;
            } else {
                verdict = "Más de lo planeado";
                color = AMBER;
            }
            compare.setText("Total: " + ScreenKit.number(t) + " kcal de " + ScreenKit.number(plan) + " kcal · "
                    + verdict);
            compare.setForeground(color);
        }

        private void save() {
            List<ConsumptionRecordByFood> lines = new ArrayList<>();
            for (Map.Entry<Food, Proto.Stepper> e : items.entrySet()) {
                if (e.getValue().getValue() > 0) {
                    lines.add(ConsumptionService.line(e.getKey().getFoodId(), e.getValue().getValue()));
                }
            }
            if (lines.isEmpty()) {
                Messages.showInfo(this, "Agrega al menos un alimento con gramos.");
                return;
            }
            LocalDateTime when = LocalDate.now().atTime(timeOf(selected));
            if (when.isAfter(LocalDateTime.now())) {
                when = LocalDateTime.now();               // no se puede registrar en el futuro
            }
            try {
                services.consumption().registerManual(services.currentUser().getIdUser(), when, lines);
                dispose();
                portion = "100";
                refresh();
            } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
                Messages.showError(this, ex);
            }
        }
    }

    // ------------------------------------------------------------------ tu día

    private JComponent todayCard(DietPlan plan, Map<MealTime, List<DietPlanFood>> meals,
                                 Map<MealTime, List<ConsumptionRecord>> eaten) throws NutriTrackException {
        JPanel card = new Proto.Rounded(Color.WHITE, null, 0, 32);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel col = Proto.column();
        col.add(Proto.left(Proto.text("Tu día", "SemiBold", 19, Proto.INK)));
        col.add(Box.createVerticalStrut(10));

        double goal = plan.getTargetCalories();
        double total = 0;
        for (List<ConsumptionRecord> list : eaten.values()) {
            total += eatenKcal(list);
        }
        JPanel kcalRow = new JPanel(new BorderLayout());
        kcalRow.setOpaque(false);
        kcalRow.add(Proto.text(ScreenKit.number(total) + " kcal", "Bold", 26, Proto.INK), BorderLayout.WEST);
        JLabel of = Proto.text("de " + ScreenKit.number(goal) + " kcal", null, 13, Proto.NOTE);
        of.setVerticalAlignment(JLabel.BOTTOM);
        kcalRow.add(of, BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(kcalRow)));
        col.add(Box.createVerticalStrut(8));
        col.add(Proto.left(new ProgressBar(goal > 0 ? total / goal : 0)));
        col.add(Box.createVerticalStrut(6));
        double left = goal - total;
        col.add(Proto.left(Proto.text(left >= 0 ? "Te quedan " + ScreenKit.number(left) + " kcal"
                : "Te pasaste " + ScreenKit.number(-left) + " kcal", "Medium", 12,
                left >= 0 ? Proto.GREEN : Proto.DANGER)));
        col.add(Box.createVerticalStrut(14));

        for (Map.Entry<MealTime, List<DietPlanFood>> e : meals.entrySet()) {
            col.add(Proto.left(mealRow(e.getKey(), planKcal(e.getValue()), eaten.get(e.getKey()))));
        }
        // registros fuera de las comidas del plan (por ejemplo, una merienda que el plan no trae)
        for (Map.Entry<MealTime, List<ConsumptionRecord>> e : eaten.entrySet()) {
            if (!meals.containsKey(e.getKey())) {
                col.add(Proto.left(mealRow(e.getKey(), 0, e.getValue())));
            }
        }
        card.add(col, BorderLayout.CENTER);
        return card;
    }

    private JComponent mealRow(MealTime meal, double planKcal, List<ConsumptionRecord> records) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Proto.LINE),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        JPanel texts = Proto.column();
        texts.add(Proto.left(Proto.text(meal.getDisplayName(), "SemiBold", 14, Proto.INK_2)));
        double kcal = eatenKcal(records);
        texts.add(Proto.left(Proto.text(records == null ? "Plan: " + ScreenKit.number(planKcal) + " kcal"
                : ScreenKit.number(kcal) + " de " + ScreenKit.number(planKcal) + " kcal", null, 12, Proto.NOTE)));
        r.add(texts, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        if (records == null) {
            right.add(pill("Pendiente", new Color(0xEEF1F3), Proto.NOTE));
        } else {
            if (planKcal <= 0 || Math.abs(kcal - planKcal) <= planKcal * TOLERANCE) {
                right.add(pill("Según el plan", Proto.GREEN_SOFT, Proto.GREEN));
            } else if (kcal < planKcal) {
                right.add(pill("Comiste menos", Proto.ROYAL_SOFT, BLUE_TEXT));
            } else {
                right.add(pill("Más de lo planeado", AMBER_SOFT, AMBER));
            }
            JButton delete = Proto.link("Borrar", Proto.DANGER, 13);
            delete.getAccessibleContext().setAccessibleName("Borrar " + meal.getDisplayName());
            delete.addActionListener(e -> deleteMeal(meal, records));
            right.add(delete);
        }
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(right);
        r.add(holder, BorderLayout.EAST);
        return Proto.noStretch(r);
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

    private static String mealImage(MealTime meal) {
        return switch (meal) {
            case BREAKFAST -> Images.MEAL_BREAKFAST;
            case LUNCH -> Images.MEAL_LUNCH;
            case DINNER -> Images.MEAL_DINNER;
            default -> Images.MEAL_SNACK;
        };
    }

    /** Píldora de texto con fondo redondeado. */
    private static JLabel pill(String text, Color bg, Color fg) {
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
        l.setFont(Proto.font("SemiBold", 12));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return l;
    }

    /** Pestaña de una comida: nombre y kcal (registradas o del plan). */
    private static final class MealTab extends JButton {

        private final boolean active;
        private final boolean done;

        MealTab(String name, String sub, boolean done, boolean active, Runnable onClick) {
            this.active = active;
            this.done = done;
            setLayout(new BorderLayout());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Color main = active ? Proto.GREEN : Color.WHITE;
            JPanel texts = Proto.column();
            texts.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            texts.add(Proto.left(Proto.text(name, "SemiBold", 14, main)));
            JLabel s = Proto.text(sub, null, 12, active ? Proto.MUTED : Proto.ON_BLUE);
            if (done) {
                s.setIcon(Icons.check(12, active ? Proto.GREEN : new Color(0x7FE3CF)));
                s.setIconTextGap(4);
            }
            texts.add(Proto.left(s));
            add(texts, BorderLayout.CENTER);
            getAccessibleContext().setAccessibleName(name + ", " + sub);
            addActionListener(e -> onClick.run());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            if (active) {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            } else {
                g2.setColor(new Color(255, 255, 255, done ? 46 : 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
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
            setPreferredSize(new Dimension(300, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(new Color(0xE3E9ED));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(ratio > 1 ? new Color(0xFF5A1F) : new Color(0x00C46A));
            int w = (int) Math.round(getWidth() * Math.min(1, Math.max(0, ratio)));
            g2.fillRoundRect(0, 0, w, getHeight(), 10, 10);
            g2.dispose();
        }
    }
}
