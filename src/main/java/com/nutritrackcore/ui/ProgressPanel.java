package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.RoutineFollowUp;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.services.BmiCategory;
import com.nutritrackcore.services.BmiResult;
import com.nutritrackcore.services.NutritionalSummary;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.swing.SwingConstants;

/**
 * Mi progreso, igual al prototipo: tarjetas con barra dorada para el
 * historial del IMC (por mes, sobre las franjas de categoría), los días de
 * ejercicio de la semana (llamas, navegación por semanas y meta semanal) y,
 * de los últimos 7 días, los macronutrientes y las calorías por día.
 */
public class ProgressPanel extends ScreenKit.BlueBackground implements Refreshable {

    private static final Color FLAME = new Color(0xE3170A);
    private static final Color FLAME_DARK = new Color(0xB80F05);
    private static final Color CORE = new Color(0xFFC23D);
    private static final Color OK = new Color(0x00C46A);
    private static final Color OVER = new Color(0xFF5A1F);
    private static final Color TODAY_BAR = new Color(0x9CC7EE);
    private static final Color[] MACRO = {new Color(0x2F6FDE), new Color(0xE8833A), new Color(0x8E5BD8)};

    private final AppServices services;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();
    /** Semana que se muestra: 0 = esta semana, -1 = la anterior, etc. */
    private int weekOffset;

    /**
     * @param services servicios de la aplicación
     * @param navigate cambia de sección (no se usa por ahora)
     */
    public ProgressPanel(AppServices services, Consumer<String> navigate) {
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
        int userId = services.currentUser().getIdUser();
        try {
            content.add(Proto.left(Proto.pageTitle("Mi progreso",
                    "Cómo vas con tu peso, tu ejercicio y tu alimentación.")));
            content.add(Box.createVerticalStrut(26));

            JPanel top = new JPanel(new GridBagLayout());
            top.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 1;
            c.gridx = 0;
            c.weightx = 1.2;
            c.insets = new Insets(0, 0, 0, 18);
            JComponent bmi = bmiCard(userId);
            bmi.setPreferredSize(new Dimension(10, bmi.getPreferredSize().height));
            top.add(bmi, c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 0, 0);
            JComponent days = daysCard(userId);
            days.setPreferredSize(new Dimension(10, days.getPreferredSize().height));
            top.add(days, c);
            content.add(Proto.left(Proto.noStretch(top)));
            content.add(Box.createVerticalStrut(28));

            JPanel dietTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            dietTitle.setOpaque(false);
            dietTitle.setBorder(BorderFactory.createEmptyBorder(0, -10, 0, 0));
            dietTitle.add(Proto.text("Mi dieta", "Bold", 20, Color.WHITE));
            dietTitle.add(Proto.text("Últimos 7 días", null, 13, Proto.ON_BLUE));
            content.add(Proto.left(Proto.noStretch(dietTitle)));
            content.add(Box.createVerticalStrut(14));

            // Consumo de los últimos 7 días (hoy al final)
            LocalDate today = LocalDate.now();
            List<LocalDate> week = new ArrayList<>();
            List<NutritionalSummary> sums = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                week.add(d);
                sums.add(services.consumption().getDailySummary(userId, d));
            }
            double goal = services.dietPlans().getActivePlan(userId).map(DietPlan::getTargetCalories).orElse(0.0);
            String weightGoal = services.profiles().find(userId).map(UserProfile::getWeightGoal).orElse("mantener");

            JPanel bottom = new JPanel(new GridLayout(1, 2, 18, 0));
            bottom.setOpaque(false);
            bottom.add(macrosCard(sums, goal, weightGoal));
            bottom.add(caloriesCard(week, sums, goal));
            content.add(Proto.left(Proto.noStretch(bottom)));
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    // ------------------------------------------------------------------ IMC

    private JComponent bmiCard(int userId) throws NutriTrackException {
        BmiResult now = services.bmi().getCurrentBmi(userId);
        String cat = now.getCategory() == BmiCategory.NORMAL ? "Peso normal" : now.getCategory().getDisplayName();
        JLabel today = Proto.text("Hoy: " + ScreenKit.decimal(now.getValue()) + " · " + cat, "SemiBold", 12,
                new Color(0x2B2111));
        Proto.HeaderCard card = new Proto.HeaderCard("Historial de IMC", Icons.trendUp(18, new Color(0x111111)),
                Proto.Bar.GOLD, today);

        // Último registro de cada mes (máximo 11 meses atrás) y el valor de hoy al final
        List<BMIHistory> history = new ArrayList<>(services.bmi().getHistory(userId));
        history.sort(Comparator.comparing(BMIHistory::getRegisterDate));
        Map<YearMonth, Double> byMonth = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (BMIHistory h : history) {
            YearMonth ym = YearMonth.from(h.getRegisterDate());
            if (!ym.isBefore(current.minusMonths(11)) && ym.isBefore(current)) {
                byMonth.put(ym, services.bmi().calculateBmi(h).getValue());
            }
        }
        List<Double> values = new ArrayList<>(byMonth.values());
        List<String> labels = new ArrayList<>();
        byMonth.keySet().forEach(ym -> labels.add(capital(ym.getMonth().getDisplayName(TextStyle.SHORT,
                ScreenKit.ES_CO).replace(".", ""))));
        values.add(now.getValue());
        labels.add("Hoy");
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
        holder.add(new BmiChart(values, labels));
        card.body().add(holder);
        return card;
    }

    /** Línea del IMC sobre las franjas de categoría (escala de 15 a 35). */
    private static final class BmiChart extends JPanel {

        private final List<Double> values;
        private final List<String> labels;

        BmiChart(List<Double> values, List<String> labels) {
            this.values = values;
            this.labels = labels;
            setOpaque(false);
            setPreferredSize(new Dimension(480, 232));
        }

        private double y(double v, double top, double bottom) {
            double cl = Math.max(15, Math.min(35, v));
            return bottom - (cl - 15) / 20.0 * (bottom - top);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                double left = 40;
                double right = getWidth() - 6;
                double top = 8;
                double bottom = getHeight() - 20;
                double[][] bands = {{30, 35}, {25, 30}, {18.5, 25}, {15, 18.5}};
                Color[] fills = {new Color(0xFBE3DF), new Color(0xFFF1D6), new Color(0xE1F2EC), new Color(0xE3EEF8)};
                Color[] texts = {new Color(0x9A2B1F), new Color(0x7A4A00), new Color(0x0B6656), new Color(0x1F5F99)};
                String[] names = {"Obesidad", "Sobrepeso", "Normal", "Bajo peso"};
                g2.setFont(Proto.font("SemiBold", 11));
                for (int i = 0; i < 4; i++) {
                    int y1 = (int) y(bands[i][1], top, bottom);
                    int y2 = (int) y(bands[i][0], top, bottom);
                    g2.setColor(fills[i]);
                    g2.fillRect((int) left, y1, (int) (right - left), y2 - y1);
                    g2.setColor(texts[i]);
                    int tw = g2.getFontMetrics().stringWidth(names[i]);
                    g2.drawString(names[i], (int) right - tw - 6, y1 + 16);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                for (double cut : new double[]{30, 25, 18.5}) {
                    int yy = (int) y(cut, top, bottom);
                    g2.drawLine((int) left, yy, (int) right, yy);
                }
                g2.setFont(Proto.font(null, 10));
                g2.setColor(Proto.NOTE);
                for (double t : new double[]{35, 30, 25, 18.5, 15}) {
                    String s = t == Math.rint(t) ? String.valueOf((int) t) : "18,5";
                    int tw = g2.getFontMetrics().stringWidth(s);
                    g2.drawString(s, (int) left - 8 - tw, (int) y(t, top, bottom) + 4);
                }
                int n = values.size();
                if (n == 0) {
                    return;
                }
                double step = n == 1 ? 0 : (right - left - 46) / (n - 1);
                Path2D path = new Path2D.Double();
                double[] xs = new double[n];
                for (int i = 0; i < n; i++) {
                    xs[i] = n == 1 ? (left + right) / 2 : left + 24 + i * step;
                    double yy = y(values.get(i), top, bottom);
                    if (i == 0) {
                        path.moveTo(xs[i], yy);
                    } else {
                        path.lineTo(xs[i], yy);
                    }
                }
                g2.setColor(Proto.GREEN);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);
                FontMetrics fm = g2.getFontMetrics();
                for (int i = 0; i < n; i++) {
                    boolean last = i == n - 1;
                    double yy = y(values.get(i), top, bottom);
                    int r = last ? 6 : 5;
                    g2.setColor(last ? Proto.GREEN : Color.WHITE);
                    g2.fillOval((int) xs[i] - r, (int) yy - r, r * 2, r * 2);
                    g2.setColor(Proto.INK);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval((int) xs[i] - r, (int) yy - r, r * 2, r * 2);
                    g2.setFont(Proto.font(last ? "Bold" : null, 10));
                    g2.setColor(last ? Proto.GREEN : Proto.NOTE);
                    String l = labels.get(i);
                    g2.drawString(l, (int) xs[i] - g2.getFontMetrics().stringWidth(l) / 2, getHeight() - 4);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ días de ejercicio

    private JComponent daysCard(int userId) throws NutriTrackException {
        Set<LocalDate> done = new HashSet<>();
        Optional<ExerciseRoutine> routine = services.routines().getActiveRoutine(userId);
        if (routine.isPresent()) {
            for (RoutineFollowUp f : services.followUps().listByRoutine(routine.get().getRoutineId())) {
                if (f.isCompleted()) {
                    done.add(f.getSessionDate());
                }
            }
        }
        int goal = services.profiles().find(userId).map(UserProfile::getTrainingDaysPerWeek).orElse(3);
        goal = Math.max(1, goal);
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate monday = thisMonday.plusWeeks(weekOffset);

        // Semanas seguidas cumpliendo la meta (contando desde esta semana hacia atrás)
        int streak = 0;
        LocalDate ws = thisMonday;
        if (count(done, ws) >= goal) {
            streak++;
        }
        for (int i = 0; i < 60; i++) {
            ws = ws.minusWeeks(1);
            if (count(done, ws) >= goal) {
                streak++;
            } else {
                break;
            }
        }
        JLabel badge = new JLabel(streak + (streak == 1 ? " semana" : " semanas") + " cumpliendo") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(0, 0, 0, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(Proto.font("SemiBold", 12));
        badge.setForeground(new Color(0x2B2111));
        badge.setIcon(flameIcon(14, true, false));
        badge.setIconTextGap(6);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 12));
        Proto.HeaderCard card = new Proto.HeaderCard("Días de ejercicio", Icons.calendar(18, new Color(0x111111)),
                Proto.Bar.GOLD, badge);

        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));
        JPanel nav = new JPanel(new BorderLayout(8, 0));
        nav.setOpaque(false);
        JButton prev = navButton(Icons.arrowLeft(14, Proto.GREEN), "Semana anterior");
        prev.setEnabled(weekOffset > -8);
        prev.addActionListener(e -> {
            weekOffset = Math.max(-8, weekOffset - 1);
            refresh();
        });
        JButton next = navButton(Icons.arrowRight(14, Proto.GREEN), "Semana siguiente");
        next.setEnabled(weekOffset < 0);
        next.addActionListener(e -> {
            weekOffset = Math.min(0, weekOffset + 1);
            refresh();
        });
        LocalDate sunday = monday.plusDays(6);
        String title = monday.getDayOfMonth()
                + (monday.getMonth() != sunday.getMonth() ? " de " + month(monday) : "")
                + " – " + sunday.getDayOfMonth() + " de " + month(sunday) + (weekOffset == 0 ? " · Esta semana" : "");
        JLabel t = Proto.text(title, "SemiBold", 14, Proto.INK);
        t.setHorizontalAlignment(SwingConstants.CENTER);
        nav.add(prev, BorderLayout.WEST);
        nav.add(t, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(nav)));
        col.add(Box.createVerticalStrut(6));

        JPanel daysRow = new JPanel(new GridLayout(1, 7, 6, 0));
        daysRow.setOpaque(false);
        String[] names = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            daysRow.add(dayBox(names[i], d, done.contains(d), d.equals(today), d.isAfter(today)));
        }
        col.add(Proto.left(Proto.noStretch(daysRow)));
        col.add(Box.createVerticalStrut(10));

        int shown = count(done, monday);
        JPanel meta = new JPanel(new BorderLayout());
        meta.setOpaque(false);
        meta.add(Proto.text("Meta semanal", null, 12, Proto.MUTED), BorderLayout.WEST);
        meta.add(Proto.text(shown + " de " + goal + " días", "Bold", 12, Proto.INK), BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(meta)));
        col.add(Box.createVerticalStrut(6));
        double ratio = Math.min(1, shown / (double) goal);
        boolean met = shown >= goal;
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(0xE6ECEF));
                g2.fillRoundRect(0, 0, getWidth(), 8, 8, 8);
                g2.setColor(met ? FLAME : new Color(0xFF6B4A));
                g2.fillRoundRect(0, 0, (int) (getWidth() * ratio), 8, 8, 8);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 8));
        col.add(Proto.left(Proto.noStretch(bar)));
        col.add(Box.createVerticalStrut(8));
        JPanel legend = new JPanel(new BorderLayout());
        legend.setOpaque(false);
        JPanel keys = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        keys.setOpaque(false);
        keys.setBorder(BorderFactory.createEmptyBorder(0, -14, 0, 0));
        JLabel k1 = Proto.text("Entrenaste", null, 11, Proto.MUTED);
        k1.setIcon(flameIcon(12, true, false));
        JLabel k2 = Proto.text("Sin entrenar", null, 11, Proto.MUTED);
        k2.setIcon(flameIcon(12, false, false));
        keys.add(k1);
        keys.add(k2);
        legend.add(keys, BorderLayout.WEST);
        legend.add(Proto.text(met ? "¡Meta cumplida!" : "Te faltan " + (goal - shown) + (goal - shown == 1 ? " día"
                : " días"), "SemiBold", 11, Proto.INK_2), BorderLayout.EAST);
        col.add(Proto.left(Proto.noStretch(legend)));
        card.body().add(col);
        return card;
    }

    private static int count(Set<LocalDate> done, LocalDate monday) {
        int c = 0;
        for (int i = 0; i < 7; i++) {
            if (done.contains(monday.plusDays(i))) {
                c++;
            }
        }
        return c;
    }

    private static JComponent dayBox(String name, LocalDate date, boolean done, boolean today, boolean future) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (today) {
                    Graphics2D g2 = Proto.smooth(g);
                    g2.setColor(new Color(0xFFF6F4));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                    g2.setColor(new Color(0xF4B7B0));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                    g2.dispose();
                }
            }
        };
        box.setOpaque(false);
        box.setLayout(new javax.swing.BoxLayout(box, javax.swing.BoxLayout.Y_AXIS));
        box.setPreferredSize(new Dimension(40, 96));
        box.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        JLabel wd = Proto.text(name, "SemiBold", 11, Proto.NOTE);
        JLabel flame = new JLabel(flameIcon(36, done, future));
        JLabel num = Proto.text(String.valueOf(date.getDayOfMonth()), today ? "Bold" : "SemiBold", 15,
                done ? FLAME_DARK : future ? new Color(0xA9B4BC) : Proto.INK_2);
        for (JLabel l : new JLabel[]{wd, flame, num}) {
            l.setAlignmentX(CENTER_ALIGNMENT);
            box.add(l);
        }
        box.getAccessibleContext().setAccessibleName(name + " " + date.getDayOfMonth() + (done ? ", entrenaste" : ""));
        return box;
    }

    /** Llama del prototipo: roja con centro amarillo si entrenó, solo contorno si no. */
    static javax.swing.Icon flameIcon(int size, boolean done, boolean future) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Proto.smooth(g);
                g2.translate(x, y);
                g2.scale(size / 24.0, size / 24.0);
                Path2D outer = new Path2D.Double();
                outer.moveTo(12, 2.5);
                outer.curveTo(13.6, 6.7, 19, 8.9, 19, 14.6);
                outer.curveTo(19, 18.5, 15.9, 21.5, 12, 21.5);
                outer.curveTo(8.1, 21.5, 5, 18.5, 5, 14.6);
                outer.curveTo(5, 11.6, 6.4, 9.2, 7.9, 7.7);
                outer.curveTo(8, 10.1, 9.2, 11.6, 10.7, 11.6);
                outer.curveTo(10.7, 7.2, 10.1, 5, 12, 2.5);
                outer.closePath();
                if (done) {
                    g2.setColor(FLAME);
                    g2.fill(outer);
                }
                g2.setColor(done ? FLAME_DARK : future ? new Color(0xD3D9DE) : new Color(0xA9B4BC));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(outer);
                if (done) {
                    Path2D core = new Path2D.Double();
                    core.moveTo(12, 11);
                    core.curveTo(12.6, 12.8, 14.6, 13.8, 14.6, 16.1);
                    core.curveTo(14.6, 17.5, 13.4, 18.7, 12, 18.7);
                    core.curveTo(10.6, 18.7, 9.4, 17.5, 9.4, 16.1);
                    core.curveTo(9.4, 14.9, 10, 14.1, 10.6, 13.5);
                    core.curveTo(10.7, 14.3, 11.1, 14.7, 11.6, 14.7);
                    core.curveTo(11.6, 13.2, 11.4, 12.1, 12, 11);
                    core.closePath();
                    g2.setColor(CORE);
                    g2.fill(core);
                }
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

    private static JButton navButton(javax.swing.Icon icon, String name) {
        JButton b = Proto.button("", Color.WHITE, Proto.GREEN, Proto.FIELD_BORDER, 30, 14);
        b.setIcon(icon);
        b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 16; focusWidth: 0;"
                + "innerFocusWidth: 0; borderWidth: 1; borderColor: #D6DEE4; background: #FFFFFF;"
                + "foreground: #0B7A69; margin: 0,0,0,0");
        b.setPreferredSize(new Dimension(30, 30));
        b.getAccessibleContext().setAccessibleName(name);
        return b;
    }

    // ------------------------------------------------------------------ macros

    private static JComponent macrosCard(List<NutritionalSummary> days, double goal, String weightGoal) {
        Proto.HeaderCard card = new Proto.HeaderCard("Macronutrientes", Icons.pie(18, new Color(0x111111)),
                Proto.Bar.GOLD, null);
        NutritionalSummary total = NutritionalSummary.empty();
        int withData = 0;
        for (NutritionalSummary d : days) {
            if (d.getCalories() > 0) {
                withData++;
                total = total.add(d);
            }
        }
        int n = Math.max(1, withData);
        double[] grams = {total.getProteins() / n, total.getCarbohydrates() / n, total.getFats() / n};
        double[] kcal = {grams[0] * 4, grams[1] * 4, grams[2] * 9};
        double sum = kcal[0] + kcal[1] + kcal[2];
        String[] names = {"Proteínas", "Carbohidratos", "Grasas"};
        String[] goals = {"≥ 25%", "40–45%", "≤ 30%"};

        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setOpaque(false);
        col.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.add(new Donut(kcal, total.getCalories() / n, goal), BorderLayout.WEST);
        JPanel legend = Proto.column();
        for (int i = 0; i < 3; i++) {
            JPanel line = new JPanel(new BorderLayout(6, 0));
            line.setOpaque(false);
            JLabel name = Proto.text(names[i], "SemiBold", 13, Proto.INK);
            name.setIcon(DietPanel.square(MACRO[i], 10, 3));
            name.setIconTextGap(6);
            line.add(name, BorderLayout.CENTER);
            line.add(Proto.text((sum <= 0 ? 0 : Math.round(kcal[i] * 100 / sum)) + "%", "Bold", 13, Proto.INK),
                    BorderLayout.EAST);
            legend.add(Proto.left(Proto.noStretch(line)));
            JLabel sub = Proto.text(ScreenKit.number(grams[i]) + " g · " + ScreenKit.number(kcal[i]) + " kcal · meta "
                    + goals[i], null, 11, Proto.NOTE);
            sub.setBorder(BorderFactory.createEmptyBorder(1, 16, 0, 0));
            legend.add(Proto.left(sub));
            legend.add(Box.createVerticalStrut(10));
        }
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        center.add(legend, c);
        row.add(center, BorderLayout.CENTER);
        col.add(row, BorderLayout.CENTER);
        String advice = switch (weightGoal == null ? "" : weightGoal) {
            case "bajar" -> "Tu objetivo es bajar de peso: sube un poco la proteína y baja las grasas.";
            case "subir" -> "Tu objetivo es subir de peso: aumenta calorías con carbohidratos y proteína.";
            default -> "Tu objetivo es mantener tu peso: vas bien, mantén este reparto.";
        };
        col.add(Proto.text(withData == 0 ? "Registra tu consumo para ver el reparto de macronutrientes." : advice,
                null, 11, Proto.MUTED), BorderLayout.SOUTH);
        card.body().add(col);
        return card;
    }

    /** Dona de macronutrientes con el promedio diario en el centro. */
    private static final class Donut extends JPanel {

        private final double[] values;
        private final double average;
        private final double goal;

        Donut(double[] values, double average, double goal) {
            this.values = values;
            this.average = average;
            this.goal = goal;
            setOpaque(false);
            setPreferredSize(new Dimension(130, 130));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                double total = values[0] + values[1] + values[2];
                int s = 104;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                if (total <= 0) {
                    g2.setColor(new Color(0xE6ECEF));
                    g2.drawOval(x, y, s, s);
                } else {
                    double start = 90;
                    for (int i = 0; i < 3; i++) {
                        double extent = -360 * values[i] / total;
                        g2.setColor(MACRO[i]);
                        g2.draw(new Arc2D.Double(x, y, s, s, start, extent + (extent < -2 ? 1.5 : 0), Arc2D.OPEN));
                        start += extent;
                    }
                }
                g2.setColor(Proto.INK);
                g2.setFont(Proto.font("Bold", 18));
                String v = ScreenKit.number(average);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(v, (getWidth() - fm.stringWidth(v)) / 2, getHeight() / 2 + 2);
                g2.setFont(Proto.font(null, 10));
                fm = g2.getFontMetrics();
                g2.setColor(Proto.MUTED);
                String l1 = goal > 0 ? "de " + ScreenKit.number(goal) + " kcal" : "kcal";
                g2.drawString(l1, (getWidth() - fm.stringWidth(l1)) / 2, getHeight() / 2 + 16);
                g2.drawString("promedio/día", (getWidth() - fm.stringWidth("promedio/día")) / 2, getHeight() / 2 + 28);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ calorías

    private static JComponent caloriesCard(List<LocalDate> week, List<NutritionalSummary> days, double goal) {
        Proto.HeaderCard card = new Proto.HeaderCard("Calorías por día", Icons.bars(18, new Color(0x111111)),
                Proto.Bar.GOLD, null);
        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        double[] values = new double[7];
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            values[i] = days.get(i).getCalories();
            labels[i] = i == 6 ? "Hoy" : capital(week.get(i).getDayOfWeek().getDisplayName(TextStyle.SHORT,
                    ScreenKit.ES_CO).replace(".", ""));
        }
        col.add(Proto.left(new Bars(values, labels, goal)));
        col.add(Box.createVerticalStrut(6));
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(0, -12, 0, 0));
        JLabel ok = Proto.text("Dentro de la meta", null, 11, Proto.MUTED);
        ok.setIcon(DietPanel.square(OK, 12, 3));
        ok.setIconTextGap(5);
        JLabel over = Proto.text("Por encima", null, 11, Proto.MUTED);
        over.setIcon(DietPanel.square(OVER, 12, 3));
        over.setIconTextGap(5);
        legend.add(ok);
        legend.add(over);
        if (goal > 0) {
            legend.add(Proto.text("- - - Meta: " + ScreenKit.number(goal) + " kcal", null, 11, Proto.MUTED));
        }
        col.add(Proto.left(Proto.noStretch(legend)));
        col.add(Box.createVerticalStrut(6));
        double sum = 0;
        for (int i = 0; i < 6; i++) {
            sum += values[i];
        }
        col.add(Proto.left(Proto.text("Promedio de 6 días: " + ScreenKit.number(sum / 6) + " kcal"
                + (goal > 0 ? " (meta " + ScreenKit.number(goal) + ")" : ""), "SemiBold", 12, Proto.INK_2)));
        card.body().add(col);
        return card;
    }

    /** Barras de calorías por día con la línea punteada de la meta. */
    private static final class Bars extends JPanel {

        private final double[] values;
        private final String[] labels;
        private final double goal;

        Bars(double[] values, String[] labels, double goal) {
            this.values = values;
            this.labels = labels;
            this.goal = goal;
            setOpaque(false);
            setPreferredSize(new Dimension(400, 142));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 142));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                int chartH = 120;
                int w = getWidth();
                double max = Math.max(goal * 1.25, 1);
                for (double v : values) {
                    max = Math.max(max, v);
                }
                int gap = 8;
                int bw = (w - gap * 6) / 7;
                g2.setFont(Proto.font(null, 11));
                for (int i = 0; i < 7; i++) {
                    int x = i * (bw + gap);
                    int h = (int) Math.round(chartH * Math.min(1, values[i] / max));
                    boolean today = i == 6;
                    g2.setColor(today ? TODAY_BAR : goal > 0 && values[i] > goal ? OVER : OK);
                    if (h > 0) {
                        g2.fillRoundRect(x, chartH - h, bw, h + 6, 12, 12);
                    }
                    g2.setColor(today ? Proto.GREEN : Proto.NOTE);
                    g2.setFont(Proto.font(today ? "Bold" : null, 11));
                    int tw = g2.getFontMetrics().stringWidth(labels[i]);
                    g2.drawString(labels[i], x + (bw - tw) / 2, chartH + 18);
                }
                g2.setColor(getParent() == null ? Color.WHITE : Color.WHITE);
                g2.fillRect(0, chartH, w, 6);
                g2.setColor(Proto.FIELD_BORDER);
                g2.drawLine(0, chartH, w, chartH);
                if (goal > 0) {
                    int gy = chartH - (int) Math.round(chartH * goal / max);
                    g2.setColor(new Color(0x9AA8B3));
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                            new float[]{4, 4}, 0));
                    g2.drawLine(0, gy, w, gy);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    private static String month(LocalDate d) {
        return d.getMonth().getDisplayName(TextStyle.FULL, ScreenKit.ES_CO).toLowerCase();
    }

    private static String capital(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
