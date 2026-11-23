package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.User;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.model.UserCondition;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.services.AllergyChangeResult;
import com.nutritrackcore.services.BmiCategory;
import com.nutritrackcore.services.BmiResult;
import java.awt.BasicStroke;
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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Mi perfil, igual al prototipo: tarjeta de datos personales con avatar
 * (editar en el mismo lugar, cambiar contraseña, eliminar cuenta), medidas
 * con contadores y escala de IMC, condiciones en acordeón (o hábitos si no
 * hay condiciones) y alergias / alimentos excluidos como chips con buscador.
 */
public class ProfilePanel extends ScreenKit.BlueBackground implements Refreshable {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Ajustes de dieta y ejercicio que se muestran al abrir cada condición. */
    private static final Map<String, String[]> ADJUST = Map.of(
            "diabetes tipo 2", new String[]{"Baja en carbohidratos simples; prioriza integrales, fibra y porciones "
                + "repartidas en el día.", "Intensidad máxima moderada. Evita entrenar en ayunas y prefiere cardio "
                + "suave con fuerza ligera."},
            "hipotiroidismo", new String[]{"Control de calorías y porciones; más fibra y proteína magra.",
                "Aumento gradual de la intensidad; combina caminata con fuerza."},
            "hipertiroidismo", new String[]{"Más calorías y calcio para compensar el gasto; evita exceso de cafeína.",
                "Evita la alta intensidad; sesiones cortas y de bajo impacto."},
            "sobrepeso", new String[]{"Déficit calórico moderado, menos azúcares y ultraprocesados.",
                "Ejercicios de bajo impacto para cuidar articulaciones; más minutos de cardio."},
            "obesidad", new String[]{"Déficit calórico moderado, menos azúcares y ultraprocesados.",
                "Ejercicios de bajo impacto para cuidar articulaciones; más minutos de cardio."},
            "hipertensión", new String[]{"Menos sodio y embutidos; más frutas y verduras.",
                "Cardio moderado; sin cargas máximas ni retener la respiración."},
            "colesterol alto", new String[]{"Menos grasas saturadas y fritos; más fibra, avena y pescado.",
                "Más cardio aeróbico durante la semana."});

    private final AppServices services;
    private final Consumer<String> navigate;
    private final Runnable onAccountDeleted;
    private final JPanel body = new JPanel(new BorderLayout());
    private boolean editing;
    private final Set<Integer> openConditions = new HashSet<>();

    /**
     * @param services         servicios de la aplicación
     * @param navigate         cambia de sección
     * @param onAccountDeleted qué hacer después de eliminar la cuenta
     */
    public ProfilePanel(AppServices services, Consumer<String> navigate, Runnable onAccountDeleted) {
        super(new BorderLayout(0, 24));
        this.services = services;
        this.navigate = navigate;
        this.onAccountDeleted = onAccountDeleted;
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
        body.setOpaque(false);
    }

    @Override
    public void refresh() {
        removeAll();
        body.removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        int userId = services.currentUser().getIdUser();
        try {
            User user = services.users().getById(userId);
            add(Proto.pageTitle("Mi perfil", "Tus datos, tus medidas y tu salud. Si cambian, tu plan se ajusta."),
                    BorderLayout.NORTH);

            JPanel row = new JPanel(new BorderLayout(20, 0));
            row.setOpaque(false);
            row.add(personal(user), BorderLayout.WEST);

            JPanel right = new JPanel(new BorderLayout(0, 20));
            right.setOpaque(false);
            right.add(measures(user), BorderLayout.NORTH);
            JPanel bottom = new JPanel(new GridBagLayout());
            bottom.setOpaque(false);
            List<MedicalCondition> conditions = services.conditions().listByUser(userId);
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = 0;
            g.fill = GridBagConstraints.BOTH;
            g.weighty = 1;
            g.gridx = 0;
            g.weightx = 0.92;
            g.insets = new Insets(0, 0, 0, 20);
            JComponent leftCard = conditions.isEmpty() ? habits(userId) : conditions(userId, conditions);
            leftCard.setPreferredSize(new Dimension(10, 10));
            bottom.add(leftCard, g);
            g.gridx = 1;
            g.weightx = 1.08;
            g.insets = new Insets(0, 0, 0, 0);
            JComponent rightCard = foodsCard(userId);
            rightCard.setPreferredSize(new Dimension(10, 10));
            bottom.add(rightCard, g);
            right.add(bottom, BorderLayout.CENTER);
            row.add(right, BorderLayout.CENTER);
            body.add(row, BorderLayout.CENTER);
            add(body, BorderLayout.CENTER);
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------ datos personales

    private JComponent personal(User user) {
        Proto.HeaderCard card = new Proto.HeaderCard("Datos personales", Icons.user(17, Color.WHITE),
                Proto.Bar.GREEN, null);
        card.setPreferredSize(new Dimension(290, 10));
        JPanel col = new JPanel(new BorderLayout());
        col.setOpaque(false);

        JPanel top = Proto.column();
        top.setBorder(BorderFactory.createEmptyBorder(22, 20, 14, 20));
        JComponent avatar = avatar(initials(user.getName()));
        avatar.setAlignmentX(CENTER_ALIGNMENT);
        top.add(avatar);
        top.add(Box.createVerticalStrut(12));
        if (!editing) {
            JLabel name = Proto.text(user.getName(), "SemiBold", 18, Proto.INK);
            name.setAlignmentX(CENTER_ALIGNMENT);
            JLabel mail = Proto.text(user.getEmail(), null, 13, Proto.MUTED);
            mail.setAlignmentX(CENTER_ALIGNMENT);
            top.add(name);
            top.add(Box.createVerticalStrut(4));
            top.add(mail);
            top.add(Box.createVerticalStrut(14));
            top.add(infoRow("Edad", user.getAge() + " años", false));
            top.add(infoRow("Nacimiento", user.getBirthDate() == null ? "—" : user.getBirthDate().format(DMY),
                    false));
            top.add(infoRow("Sexo", sexName(user), true));
            col.add(top, BorderLayout.NORTH);

            JPanel actions = new JPanel(new GridLayout(3, 1, 0, 10));
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
            JButton edit = Proto.greenButton("Editar datos");
            edit.addActionListener(e -> {
                editing = true;
                refresh();
            });
            JButton pwd = Proto.outlineButton("Cambiar contraseña");
            pwd.addActionListener(e -> changePassword(user));
            JButton del = Proto.link("Eliminar mi cuenta", Proto.DANGER, 13);
            del.setHorizontalAlignment(SwingConstants.CENTER);
            del.addActionListener(e -> deleteAccount(user));
            actions.add(edit);
            actions.add(pwd);
            actions.add(del);
            col.add(actions, BorderLayout.SOUTH);
        } else {
            JTextField name = field(user.getName());
            JTextField mail = field(user.getEmail());
            top.add(labeled("Nombre completo", name));
            top.add(Box.createVerticalStrut(12));
            top.add(labeled("Correo electrónico", mail));
            col.add(top, BorderLayout.NORTH);
            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
            JButton cancel = Proto.cancelButton("Cancelar");
            cancel.addActionListener(e -> {
                editing = false;
                refresh();
            });
            JButton save = Proto.greenButton("Guardar");
            save.addActionListener(e -> saveData(user, name.getText(), mail.getText()));
            actions.add(cancel);
            actions.add(save);
            col.add(actions, BorderLayout.SOUTH);
        }
        card.body().add(col);
        return card;
    }

    private static JComponent infoRow(String key, String value, boolean last) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, last ? 1 : 0, 0, Proto.LINE),
                BorderFactory.createEmptyBorder(11, 0, 11, 0)));
        row.add(Proto.text(key, null, 14, Proto.MUTED), BorderLayout.WEST);
        row.add(Proto.text(value, "SemiBold", 14, Proto.INK), BorderLayout.EAST);
        row.setAlignmentX(CENTER_ALIGNMENT);
        return Proto.noStretch(row);
    }

    private static JComponent avatar(String initials) {
        JLabel a = new JLabel(initials, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(Proto.GREEN);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        a.setFont(Proto.font("Bold", 30));
        a.setForeground(Color.WHITE);
        Dimension d = new Dimension(84, 84);
        a.setPreferredSize(d);
        a.setMaximumSize(d);
        return a;
    }

    private static String initials(String name) {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ medidas

    private JComponent measures(User user) throws NutriTrackException {
        Proto.HeaderCard card = new Proto.HeaderCard("Medidas e IMC", Icons.bars(17, Color.WHITE), Proto.Bar.GREEN,
                null);
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        Proto.Stepper weight = new Proto.Stepper(user.getWeightKg(), 30, 300, 0.5, true, Proto.GREEN,
                Proto.GREEN_SOFT, "peso");
        Proto.Stepper height = new Proto.Stepper(user.getHeightCm(), 100, 250, 1, false, Proto.GREEN,
                Proto.GREEN_SOFT, "estatura");
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 24);
        c.gridx = 0;
        row.add(labeledFixed("Peso (kg)", weight, 140), c);
        c.gridx = 1;
        row.add(labeledFixed("Estatura (cm)", height, 140), c);

        BmiPanel bmiPanel = new BmiPanel(BmiResult.calculate(user.getWeightKg(), user.getHeightCm()));
        Runnable update = () -> {
            try {
                bmiPanel.set(BmiResult.calculate(weight.getValue(), height.getValue()));
            } catch (NutriTrackException ignored) {
                // medidas fuera de rango: se deja el último valor válido
            }
        };
        weight.onChange(update);
        height.onChange(update);
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        row.add(bmiPanel, c);
        c.gridx = 3;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        JButton save = Proto.greenButton("Guardar");
        save.setMinimumSize(save.getPreferredSize());
        save.addActionListener(e -> saveMeasures(user, weight.getValue(), height.getValue()));
        row.add(save, c);
        card.body().add(row);
        return card;
    }

    /** Valor del IMC con la escala de colores, el marcador y la categoría debajo. */
    private static final class BmiPanel extends JPanel {

        private static final double MIN = 15;
        private static final double MAX = 35;
        private static final double[] FLEX = {3.5, 6.5, 5, 5};
        private static final Color[] COLORS = {new Color(0x7FB3E8), new Color(0x34B38A), new Color(0xF0B44C),
            new Color(0xE46A5B)};
        private BmiResult bmi;

        BmiPanel(BmiResult bmi) {
            this.bmi = bmi;
            setOpaque(false);
            setPreferredSize(new Dimension(150, 88));
            setMinimumSize(new Dimension(150, 88));
        }

        void set(BmiResult value) {
            this.bmi = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                g2.setColor(new Color(0xE6ECEF));
                g2.fillRect(0, 0, 1, getHeight());
                int x0 = 24;
                int w = getWidth() - x0;
                g2.setFont(Proto.font(null, 13));
                g2.setColor(Proto.MUTED);
                g2.drawString("Tu IMC", x0, 30);
                g2.setFont(Proto.font("Bold", 30));
                g2.setColor(Proto.INK);
                g2.drawString(ScreenKit.decimal(bmi.getValue()), x0 + 62, 32);
                int y = 44;
                double total = 20;
                int x = x0;
                for (int i = 0; i < 4; i++) {
                    int sw = (int) Math.round((w - 9) * FLEX[i] / total);
                    g2.setColor(COLORS[i]);
                    g2.fillRoundRect(x, y, sw, 12, 12, 12);
                    x += sw + 3;
                }
                double v = Math.max(MIN, Math.min(MAX, bmi.getValue()));
                int mx = x0 + (int) Math.round((v - MIN) / (MAX - MIN) * w);
                g2.setColor(Color.WHITE);
                g2.fillOval(mx - 10, y - 4, 20, 20);
                g2.setColor(Proto.INK);
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(mx - 9, y - 3, 18, 18);
                String cat = bmi.getCategory().getDisplayName();
                String label = bmi.getCategory() == BmiCategory.NORMAL ? "Peso normal" : cat;
                g2.setFont(Proto.font("SemiBold", 12));
                int tw = g2.getFontMetrics().stringWidth(label) + 20;
                int tx = Math.max(x0, Math.min(x0 + w - tw, mx - tw / 2));
                g2.setColor(pillBg(bmi.getCategory()));
                g2.fillRoundRect(tx, y + 20, tw, 24, 24, 24);
                g2.setColor(pillFg(bmi.getCategory()));
                g2.drawString(label, tx + 10, y + 37);
            } finally {
                g2.dispose();
            }
        }

        private static Color pillBg(BmiCategory c) {
            return switch (c) {
                case LOW_WEIGHT -> new Color(0xE3EEF8);
                case NORMAL -> Proto.GREEN_SOFT;
                case OVERWEIGHT -> new Color(0xFFF3E3);
                case OBESITY -> new Color(0xFBE3DF);
            };
        }

        private static Color pillFg(BmiCategory c) {
            return switch (c) {
                case LOW_WEIGHT -> new Color(0x1F5F99);
                case NORMAL -> new Color(0x0B5F52);
                case OVERWEIGHT -> new Color(0x7A3E00);
                case OBESITY -> new Color(0x9A2B1F);
            };
        }
    }

    // ------------------------------------------------------------------ hábitos / condiciones

    private JComponent habits(int userId) throws NutriTrackException {
        JButton edit = Proto.barButton("Editar");
        edit.addActionListener(e -> {
            HabitsPanel.openFrom(AppShellPanel.PROFILE);
            navigate.accept(AppShellPanel.HABITS);
        });
        Proto.HeaderCard card = new Proto.HeaderCard("Mis hábitos", Icons.target(17, Color.WHITE), Proto.Bar.GREEN,
                edit);
        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(6, 18, 12, 18));
        Optional<UserProfile> profile = services.profiles().find(userId);
        if (profile.isEmpty()) {
            col.add(Proto.left(Proto.wrapped("Aún no has llenado tus hábitos. Pulsa Editar para completarlos.", 220,
                    null, 13, Proto.MUTED)));
        } else {
            UserProfile p = profile.get();
            col.add(habit("Actividad", Labels.activity(p.getActivityLevel())));
            col.add(habit("Objetivo de peso", Labels.goal(p.getWeightGoal()).replace(" de peso", "")));
            col.add(habit("Experiencia", capital(p.getExerciseExperience())));
            col.add(habit("Entrenamiento", p.getTrainingDaysPerWeek() + " días × " + p.getMinutesPerSession()
                    + " min · " + Labels.location(p.getTrainingLocation()).replace("En casa", "Casa")));
            col.add(habit("Tipo de dieta", Labels.diet(p.getDietType())));
            col.add(habit("Comidas al día", String.valueOf(p.getMealsPerDay())));
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(col, BorderLayout.NORTH);
        card.body().add(holder);
        return card;
    }

    private static JComponent habit(String key, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Proto.LINE),
                BorderFactory.createEmptyBorder(7, 0, 7, 0)));
        row.add(Proto.text(key, null, 13, Proto.MUTED), BorderLayout.WEST);
        row.add(Proto.text(value, "SemiBold", 13, Proto.INK), BorderLayout.EAST);
        return Proto.left(Proto.noStretch(row));
    }

    private JComponent conditions(int userId, List<MedicalCondition> conditions) throws NutriTrackException {
        JButton manage = Proto.barButton("Gestionar");
        manage.addActionListener(e -> navigate.accept(AppShellPanel.CONDITIONS));
        Proto.HeaderCard card = new Proto.HeaderCard("Mis condiciones (" + conditions.size() + ")",
                Icons.heartPulse(17, Color.WHITE), Proto.Bar.GREEN, manage);
        Map<Integer, UserCondition> assigned = new HashMap<>();
        for (UserCondition uc : services.conditions().listAssignments(userId)) {
            assigned.put(uc.getConditionId(), uc);
        }
        ScreenKit.ScrollColumn list = new ScreenKit.ScrollColumn();
        list.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        for (MedicalCondition c : conditions) {
            list.add(accordion(c, assigned.get(c.getConditionId())));
            list.add(Box.createVerticalStrut(10));
        }
        card.body().add(ScreenKit.transparentScroll(list));
        return card;
    }

    private JComponent accordion(MedicalCondition c, UserCondition uc) {
        boolean open = openConditions.contains(c.getConditionId());
        JPanel box = new Proto.Rounded(open ? new Color(0xF2F8F6) : new Color(0xF6F9F8),
                open ? Proto.GREEN : new Color(0xDCE7E4), open ? 1.5f : 1f, 24);
        box.setLayout(new BorderLayout());
        JButton head = new JButton(c.getName());
        head.setFont(Proto.font("SemiBold", 15));
        head.setForeground(Proto.INK);
        head.setHorizontalAlignment(SwingConstants.LEFT);
        head.setContentAreaFilled(false);
        head.setBorderPainted(false);
        head.setFocusPainted(false);
        head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        head.setIcon(DietPanel.square(Proto.GREEN, 10, 5));
        head.setIconTextGap(12);
        head.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        head.getAccessibleContext().setAccessibleDescription(open ? "Abierta" : "Cerrada");
        head.addActionListener(e -> {
            if (!openConditions.remove(c.getConditionId())) {
                openConditions.add(c.getConditionId());
            }
            refresh();
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(head, BorderLayout.CENTER);
        JLabel chevron = new JLabel(Icons.chevron(18, Proto.GREEN, open));
        chevron.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
        top.add(chevron, BorderLayout.EAST);
        box.add(top, BorderLayout.NORTH);
        if (open) {
            JPanel detail = Proto.column();
            detail.setBorder(BorderFactory.createEmptyBorder(0, 36, 14, 14));
            String desc = c.getDescription() == null ? "" : c.getDescription();
            detail.add(Proto.left(Proto.wrapped(desc + (desc.endsWith(".") ? "" : "."), 150, null, 12,
                    Proto.MUTED)));
            if (uc != null && uc.getDiagnosisDate() != null) {
                detail.add(Proto.left(Proto.text("Diagnosticada el " + uc.getDiagnosisDate().format(DMY), null, 11,
                        Proto.NOTE)));
            }
            detail.add(Box.createVerticalStrut(10));
            String[] adj = ADJUST.getOrDefault(c.getName().toLowerCase(),
                    new String[]{"Sin restricciones especiales en tu dieta.", "Sin restricciones especiales en tu "
                        + "rutina."});
            detail.add(Proto.left(adjustBox("Plan de dieta", Icons.utensils(14, Proto.GREEN), adj[0])));
            detail.add(Box.createVerticalStrut(8));
            detail.add(Proto.left(adjustBox("Ejercicio", Icons.dumbbell(14, Proto.GREEN), adj[1])));
            box.add(detail, BorderLayout.CENTER);
        }
        return Proto.left(box);
    }

    private static JComponent adjustBox(String title, javax.swing.Icon icon, String text) {
        JPanel b = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(new Color(0xDCE7E4));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.setColor(Proto.GREEN);
                g2.fillRect(0, 4, 3, getHeight() - 8);
                g2.dispose();
            }
        };
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));
        JLabel t = Proto.text(title, "SemiBold", 13, Proto.GREEN);
        t.setIcon(icon);
        t.setIconTextGap(6);
        b.add(t, BorderLayout.NORTH);
        b.add(Proto.wrapped(text, 135, null, 12, Proto.INK_2), BorderLayout.CENTER);
        return b;
    }

    // ------------------------------------------------------------------ alergias y excluidos

    private JComponent foodsCard(int userId) throws NutriTrackException {
        Proto.HeaderCard card = new Proto.HeaderCard("Alergias y alimentos excluidos", Icons.utensils(17, Color.WHITE),
                Proto.Bar.GREEN, null);
        ScreenKit.ScrollColumn col = new ScreenKit.ScrollColumn();
        col.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // Alergias
        List<Allergy> catalog = services.allergies().listCatalog();
        Map<Integer, Allergy> byId = new HashMap<>();
        catalog.forEach(a -> byId.put(a.getAllergyId(), a));
        List<UserAllergy> mine = services.allergies().listByUser(userId);
        col.add(sectionTitle(Icons.alert(16, new Color(0xB4372A)), "Alergias", "· nunca aparecerán en tu dieta"));
        col.add(Box.createVerticalStrut(8));
        JPanel chips = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        chips.setOpaque(false);
        chips.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        for (UserAllergy ua : mine) {
            Allergy a = byId.get(ua.getAllergyId());
            chips.add(allergyChip(userId, ua, a == null ? "Alergia" : a.getName()));
        }
        if (mine.isEmpty()) {
            chips.add(Proto.text("Ninguno registrado.", null, 12, Proto.NOTE));
        }
        col.add(Proto.left(chips));
        col.add(Box.createVerticalStrut(8));
        SearchBox<Allergy> search = new SearchBox<>("Busca tu alergia (ej. maní, mariscos, gluten)", Allergy::getName,
                a -> addAllergy(userId, a));
        search.setItems(catalog.stream().filter(a -> mine.stream().noneMatch(m -> m.getAllergyId() == a.getAllergyId()))
                .toList());
        col.add(Proto.left(Proto.noStretch(search)));
        col.add(Box.createVerticalStrut(14));
        JPanel sep = new JPanel();
        sep.setBackground(new Color(0xE6ECEF));
        sep.setPreferredSize(new Dimension(10, 1));
        col.add(Proto.left(Proto.noStretch(sep)));
        col.add(Box.createVerticalStrut(14));

        // Excluidos
        List<Food> foods = services.foods().listActive();
        Map<Integer, Food> foodById = new HashMap<>();
        foods.forEach(f -> foodById.put(f.getFoodId(), f));
        List<UserExcludedFood> excluded = services.excludedFoods().listByUser(userId);
        col.add(sectionTitle(Icons.ban(15, Proto.INK_2), "Alimentos excluidos", "· no te gustan o no los consumes"));
        col.add(Box.createVerticalStrut(8));
        JPanel ex = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        ex.setOpaque(false);
        ex.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        for (UserExcludedFood e : excluded) {
            Food f = foodById.get(e.getFoodId());
            String name = f == null ? "Alimento " + e.getFoodId() : f.getName();
            ex.add(chip(name, new Color(0xEEF2F4), new Color(0xD6DEE4), Proto.INK_2, null,
                    () -> include(userId, e.getFoodId(), name)));
        }
        if (excluded.isEmpty()) {
            ex.add(Proto.text("Ninguno registrado.", null, 12, Proto.NOTE));
        }
        col.add(Proto.left(ex));
        col.add(Box.createVerticalStrut(8));
        SearchBox<Food> foodSearch = new SearchBox<>("Escribe un alimento (ej. brócoli)", Food::getName,
                f -> exclude(userId, f));
        foodSearch.setItems(foods.stream().filter(f -> excluded.stream().noneMatch(m -> m.getFoodId() == f.getFoodId()))
                .toList());
        col.add(Proto.left(Proto.noStretch(foodSearch)));
        card.body().add(ScreenKit.transparentScroll(col));
        return card;
    }

    private static JComponent sectionTitle(javax.swing.Icon icon, String title, String note) {
        JPanel row = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        JLabel t = Proto.text(title, "SemiBold", 14, Proto.INK);
        t.setIcon(icon);
        t.setIconTextGap(8);
        row.add(t);
        row.add(Proto.text(note, null, 12, Proto.NOTE));
        return Proto.left(row);
    }

    private JComponent allergyChip(int userId, UserAllergy ua, String name) {
        JComboBox<String> severity = new JComboBox<>(new String[]{"Leve", "Moderada", "Severa"});
        severity.setFont(Proto.font(null, 11));
        severity.setForeground(new Color(0x9A2B1F));
        severity.setSelectedItem(capital(ua.getSeverity() == null ? "moderada" : ua.getSeverity()));
        severity.setPreferredSize(new Dimension(90, 24));
        severity.getAccessibleContext().setAccessibleName("Severidad de la alergia a " + name);
        severity.addActionListener(e -> changeSeverity(userId, ua, ((String) severity.getSelectedItem()).toLowerCase()));
        return chip(shortAllergy(name), new Color(0xFBE3DF), new Color(0xF2B8AE), new Color(0x9A2B1F), severity,
                () -> removeAllergy(userId, ua.getAllergyId(), name));
    }

    /** Píldora con texto, un control opcional y botón × para quitar. */
    private static JComponent chip(String text, Color bg, Color border, Color fg, JComponent extra, Runnable onRemove) {
        JPanel p = new Proto.Rounded(bg, border, 1, 34);
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 4));
        p.add(Proto.text(text, "SemiBold", 13, fg));
        if (extra != null) {
            p.add(extra);
        }
        JButton x = new JButton("×") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        x.setFont(Proto.font(null, 13));
        x.setForeground(fg);
        x.setContentAreaFilled(false);
        x.setBorderPainted(false);
        x.setFocusPainted(false);
        x.setMargin(new Insets(0, 0, 0, 0));
        x.setPreferredSize(new Dimension(22, 22));
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.getAccessibleContext().setAccessibleName("Quitar " + text);
        x.addActionListener(e -> onRemove.run());
        p.add(x);
        return p;
    }

    /** "Alergia al maní" → "Maní". */
    private static String shortAllergy(String name) {
        String s = name.replaceFirst("(?i)^(alergia|intolerancia|sensibilidad) (a la |al |a los |a las |a )", "");
        return s.isEmpty() ? name : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ acciones

    private void saveData(User user, String name, String email) {
        try {
            services.users().updateProfile(user.getIdUser(), name, email);
            editing = false;
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void saveMeasures(User user, double weight, double height) {
        try {
            services.users().updateMeasurements(user.getIdUser(), weight, height);
            Messages.showInfo(this, "Medidas guardadas. Si cambió tu categoría de IMC, genera de nuevo tu dieta y "
                    + "tu rutina.");
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void addAllergy(int userId, Allergy allergy) {
        try {
            AllergyChangeResult result = services.allergies().addAllergy(userId, allergy.getAllergyId(), "moderada",
                    null);
            if (result.hasChanges()) {
                Messages.showInfo(this, result.getMessage());
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void changeSeverity(int userId, UserAllergy ua, String severity) {
        if (severity.equalsIgnoreCase(ua.getSeverity())) {
            return;
        }
        try {
            // El servicio no tiene "actualizar": se quita y se vuelve a agregar con la nueva severidad.
            services.allergies().removeAllergy(userId, ua.getAllergyId());
            services.allergies().addAllergy(userId, ua.getAllergyId(), severity, ua.getObservations());
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        SwingUtilities.invokeLater(this::refresh);
    }

    private void removeAllergy(int userId, int allergyId, String name) {
        if (!Messages.confirm(this, "¿Quitar la alergia \"" + name + "\"?")) {
            return;
        }
        try {
            services.allergies().removeAllergy(userId, allergyId);
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void exclude(int userId, Food food) {
        try {
            AllergyChangeResult result = services.excludedFoods().exclude(userId, food.getFoodId());
            if (result.hasChanges()) {
                Messages.showInfo(this, result.getMessage());
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void include(int userId, int foodId, String name) {
        try {
            services.excludedFoods().include(userId, foodId);
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void changePassword(User user) {
        JPasswordField current = new JPasswordField();
        JPasswordField fresh = new JPasswordField();
        JPasswordField confirm = new JPasswordField();
        FormKit.styleField(fresh, "Mínimo 8", null, 300);
        FormKit.styleField(confirm, "Repítela", null, 300);
        FormKit.styleField(current, "", null, 300);
        JButton save = Proto.greenButton("Guardar");
        JDialog dialog = dialog("Cambiar contraseña", Proto.Bar.GREEN, save,
                labeled("Contraseña actual", current), labeled("Nueva contraseña", fresh), labeled("Confirmar", confirm));
        save.addActionListener(e -> {
            char[] a = fresh.getPassword();
            char[] b = confirm.getPassword();
            try {
                if (!Arrays.equals(a, b)) {
                    Messages.showInfo(dialog, "Las contraseñas nuevas no coinciden.");
                    return;
                }
                services.users().changePassword(user.getIdUser(), new String(current.getPassword()), new String(a));
                dialog.dispose();
                Messages.showInfo(this, "Contraseña actualizada.");
            } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
                Messages.showError(dialog, ex);
            } finally {
                Arrays.fill(a, '\0');
                Arrays.fill(b, '\0');
            }
        });
        dialog.setVisible(true);
    }

    private void deleteAccount(User user) {
        JButton confirm = Proto.button("Eliminar", Proto.DANGER, Color.WHITE, null, 42, 14);
        JDialog dialog = dialog("Eliminar mi cuenta", Proto.Bar.DANGER, confirm,
                Proto.wrapped("Se borrarán tu perfil, tus condiciones, alergias, alimentos excluidos, dietas, rutinas, "
                        + "sesiones y consumos. Esta acción no se puede deshacer.", 270, null, 14, Proto.INK_2));
        confirm.addActionListener(e -> {
            try {
                services.accountDeletion().deleteAccount(user.getIdUser());
                dialog.dispose();
                onAccountDeleted.run();
            } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
                Messages.showError(dialog, ex);
            }
        });
        dialog.setVisible(true);
    }

    /** Diálogo modal con barra de color, contenido y botones Cancelar / acción. */
    private JDialog dialog(String title, Proto.Bar bar, JButton action, JComponent... rows) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        Proto.HeaderCard card = new Proto.HeaderCard(title, null, bar, null);
        JPanel col = Proto.column();
        col.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        for (JComponent r : rows) {
            col.add(Proto.left(r));
            col.add(Box.createVerticalStrut(12));
        }
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        JButton cancel = Proto.cancelButton("Cancelar");
        cancel.addActionListener(e -> d.dispose());
        actions.add(cancel);
        actions.add(action);
        col.add(Proto.left(Proto.noStretch(actions)));
        card.body().add(col);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(card);
        d.setContentPane(root);
        d.setUndecorated(true);
        d.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        d.pack();
        d.setSize(Math.max(380, d.getWidth()), d.getHeight());
        d.setLocationRelativeTo(this);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return d;
    }

    // ------------------------------------------------------------------ piezas

    private static JTextField field(String value) {
        JTextField f = new JTextField(value);
        FormKit.styleField(f, "", null, 250);
        return f;
    }

    private static JComponent labeled(String label, JComponent input) {
        JPanel p = Proto.column();
        p.add(Proto.left(Proto.text(label, "SemiBold", 13, Proto.INK_2)));
        p.add(Box.createVerticalStrut(6));
        input.setPreferredSize(new Dimension(250, 42));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        p.add(Proto.left(input));
        p.setAlignmentX(CENTER_ALIGNMENT);
        return Proto.noStretch(p);
    }

    private static JComponent labeledFixed(String label, JComponent input, int width) {
        JPanel p = Proto.column();
        p.add(Proto.left(Proto.text(label, "SemiBold", 13, Proto.INK_2)));
        p.add(Box.createVerticalStrut(6));
        input.setPreferredSize(new Dimension(width, 42));
        input.setMinimumSize(new Dimension(width, 42));
        p.add(Proto.left(input));
        return p;
    }

    private static String capital(String s) {
        return s == null || s.isEmpty() ? "—" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String sexName(User user) {
        if (user.getSex() == null) {
            return "—";
        }
        return switch (user.getSex()) {
            case M -> "Masculino";
            case F -> "Femenino";
            default -> "Otro";
        };
    }
}
