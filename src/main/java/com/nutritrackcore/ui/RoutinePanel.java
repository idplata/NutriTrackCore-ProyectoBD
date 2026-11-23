package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.Exercise;
import com.nutritrackcore.model.ExerciseByReps;
import com.nutritrackcore.model.ExerciseByTime;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.RoutineFollowUp;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.services.BmiCategory;
import com.nutritrackcore.services.RoutineResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Mi rutina, igual al prototipo: selector En casa / Gimnasio (solo sin
 * condiciones), aviso de rutina asignada (con condiciones), tarjeta con foto
 * (intensidad, lugar y plan semanal), tabla de ejercicios con impacto y equipo
 * y la tarjeta para registrar la sesión de hoy.
 */
public class RoutinePanel extends ScreenKit.BlueBackground implements Refreshable {

    private static final Color WARN_BG = new Color(0xFFF3E3);
    private static final Color WARN_TEXT = new Color(0x7A3E00);
    private static final double[] COLUMNS = {2.2, 1.2, 0.7, 1.0, 1.1, 1.1};
    /** Texto claro sobre la foto de la tabla de ejercicios. */
    private static final Color LIGHT = new Color(0xC9D6DC);
    /** Orden de la sesión por grupo: el cardio suave va primero (calienta) y el intenso al final. */
    private static final java.util.Map<String, Integer> ORDER = java.util.Map.of(
            "PIERNAS", 1, "GLUTEOS", 2, "ESPALDA", 3, "PECHO", 4, "HOMBROS", 5, "BRAZOS", 6,
            "CUERPO_COMPLETO", 7, "CORE", 9);

    private final AppServices services;
    private final ScreenKit.ScrollColumn content = new ScreenKit.ScrollColumn();
    /** Avisos de la última rutina generada (el servicio solo los da al generar). */
    private List<String> lastWarnings = new ArrayList<>();
    private boolean sessionDone = true;

    /**
     * @param services servicios de la aplicación
     * @param navigate cambia de sección (no se usa por ahora)
     */
    public RoutinePanel(AppServices services, java.util.function.Consumer<String> navigate) {
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
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        content.revalidate();
        content.repaint();
    }

    private void showRoutine(int userId, ExerciseRoutine routine) throws NutriTrackException {
        List<MedicalCondition> conditions = services.conditions().listByUser(userId);
        List<String> names = conditions.stream().map(MedicalCondition::getName).toList();
        Optional<UserProfile> profile = services.profiles().find(userId);
        double bmi = services.bmi().getCurrentBmi(userId).getValue();
        boolean over = bmi >= BmiCategory.OVERWEIGHT.getMin();
        boolean manual = names.isEmpty() && profile.isPresent();

        // Encabezado: título y "Generar nueva"
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Proto.pageTitle("Mi rutina", "Generada el " + ScreenKit.longDate(routine.getGenerationDate())
                + " · Intensidad " + intensity(routine).toLowerCase() + " · IMC " + ScreenKit.decimal(bmi)),
                BorderLayout.CENTER);
        JButton regen = Proto.button("Generar nueva", Color.WHITE, Proto.GREEN, null, 44, 14);
        Proto.withIcon(regen, Icons.refresh(15, Proto.GREEN), false);
        regen.addActionListener(e -> generate(true));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(regen);
        head.add(right, BorderLayout.EAST);
        content.add(Proto.left(Proto.noStretch(head)));
        content.add(Box.createVerticalStrut(26));

        // Fila: En casa / Gimnasio (solo sin condiciones) y botón pequeño de la sesión de hoy
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        if (manual) {
            Proto.Segmented place = new Proto.Segmented(Proto.options("casa", "En casa", "gimnasio", "Gimnasio"), null);
            place.setValue(profile.get().getTrainingLocation());
            place.onChange(() -> changeLocation(profile.get(), place.getValue()));
            place.getAccessibleContext().setAccessibleName("Dónde entrenas");
            bar.add(place, BorderLayout.WEST);
        }
        RoutineFollowUp today = todaySession(routine);
        JPanel pillHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pillHolder.setOpaque(false);
        pillHolder.add(today == null ? sessionPill(routine) : donePill(today));
        bar.add(pillHolder, BorderLayout.EAST);
        bar.setPreferredSize(new Dimension(10, 46));
        content.add(Proto.left(Proto.noStretch(bar)));
        content.add(Box.createVerticalStrut(24));

        for (String warning : lastWarnings) {
            JPanel w = new Proto.Rounded(new Color(0xFFF8E1), new Color(0xF3D3A6), 1, 20);
            w.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            w.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 14));
            w.add(Proto.text("Aviso:", "Bold", 13, new Color(0x7A4A00)));
            w.add(Proto.text(warning, null, 13, new Color(0x7A4A00)));
            content.add(Proto.left(Proto.noStretch(w)));
            content.add(Box.createVerticalStrut(12));
        }
        content.add(Proto.left(exercisesCard(routine, names, over)));
    }

    /** Sesión registrada hoy en la rutina activa, o {@code null}. */
    private RoutineFollowUp todaySession(ExerciseRoutine routine) throws NutriTrackException {
        RoutineFollowUp today = null;
        for (RoutineFollowUp f : services.followUps().listByRoutine(routine.getRoutineId())) {
            if (LocalDate.now().equals(f.getSessionDate())) {
                today = f;
            }
        }
        return today;
    }

    /** Píldora amarilla, pequeña pero llamativa, que abre el panel para guardar la sesión. */
    private JComponent sessionPill(ExerciseRoutine routine) {
        JButton b = new JButton("Registrar sesión de hoy") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(255, 194, 61, 90));
                g2.fillRoundRect(0, 3, getWidth(), getHeight() - 3, getHeight(), getHeight());
                g2.setColor(new Color(0xFFC23D));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 4, getHeight() - 4, getHeight() - 4);
                g2.setColor(new Color(227, 23, 10, 64));
                g2.fillOval(11, (getHeight() - 4) / 2 - 7, 14, 14);
                g2.setColor(new Color(0xE3170A));
                g2.fillOval(14, (getHeight() - 4) / 2 - 4, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(Proto.font("Bold", 13));
        b.setForeground(new Color(0x1E1400));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.setIcon(Icons.arrowRight(14, new Color(0x1E1400)));
        b.setHorizontalTextPosition(SwingConstants.LEADING);
        b.setIconTextGap(6);
        b.setBorder(BorderFactory.createEmptyBorder(0, 32, 4, 16));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width, 44));
        b.addActionListener(e -> openSessionDialog(routine));
        return b;
    }

    /** Píldora verde cuando la sesión de hoy ya está registrada. */
    private static JComponent donePill(RoutineFollowUp today) {
        boolean plus = today.getObservations() != null && today.getObservations().contains("[+1 rep]");
        JLabel l = new JLabel("Sesión de hoy: " + today.getDurationMinutes() + " min"
                + (today.isCompleted() ? "" : " · incompleta") + (plus ? " · +1 rep" : "")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(new Color(52, 214, 181, 51));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(new Color(52, 214, 181, 166));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Proto.font("SemiBold", 13));
        l.setForeground(Color.WHITE);
        l.setIcon(Icons.check(14, Color.WHITE));
        l.setIconTextGap(6);
        l.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return l;
    }

    // ------------------------------------------------------------------ tarjeta con foto

    // ------------------------------------------------------------------ ejercicios

    private JComponent exercisesCard(ExerciseRoutine routine, List<String> names, boolean over)
            throws NutriTrackException {
        JPanel card = new PhotoCard();
        card.setLayout(new java.awt.BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel col = Proto.column();

        JPanel title = new JPanel(new BorderLayout(10, 0));
        title.setOpaque(false);
        title.add(Proto.text("Ejercicios", "SemiBold", 19, Color.WHITE), BorderLayout.WEST);
        List<String> restr = new ArrayList<>(names);
        if (over) {
            restr.add("IMC alto");
        }
        if (!restr.isEmpty()) {
            boolean moderate = names.stream().anyMatch(n -> n.toLowerCase().contains("diabetes")
                    || n.toLowerCase().contains("hipotiroidismo"));
            String detail = over ? " · solo bajo impacto, 3 series de máximo 10 repeticiones"
                    : moderate ? " · intensidad máxima moderada" : "";
            JLabel note = Proto.text("<html><div style='text-align:right;width:300px'>Ajustada por: "
                    + Proto.escape(String.join(", ", restr) + detail) + "</div></html>", null, 12, new Color(0xFFD08A));
            title.add(note, BorderLayout.EAST);
        }
        col.add(Proto.left(Proto.noStretch(title)));
        col.add(Box.createVerticalStrut(12));

        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        String[] heads = {"Ejercicio", "Grupo", "Series", "Reps / tiempo", "Impacto", "Equipo"};
        for (int i = 0; i < heads.length; i++) {
            JLabel h = Proto.text(heads[i], "SemiBold", 12, LIGHT);
            if (i == 2 || i == 3) {
                h.setHorizontalAlignment(SwingConstants.CENTER);
            }
            h.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 56)),
                    BorderFactory.createEmptyBorder(0, 0, 10, 0)));
            table.add(h, cell(i, 0));
        }
        int r = 1;
        for (ExerciseRoutineExercise line : ordered(routine)) {
            Exercise ex = services.exercises().getById(line.getExerciseId());
            boolean timed = line instanceof ExerciseByTime;
            JComponent[] cells = {
                Proto.wrapped(ex.getName(), 140, "SemiBold", 14, Color.WHITE),
                Proto.wrapped(Labels.muscle(ex.getMuscleGroup()), 80, null, 14, LIGHT),
                centered(Proto.text(String.valueOf(line.getSets()), null, 14, Color.WHITE)),
                centered(Proto.text(amount(line), timed ? "SemiBold" : null, 14,
                        timed ? new Color(0x8FD3FF) : Color.WHITE)),
                badge(ex.isLowImpact()),
                Proto.text(ex.getNeed_Equipment() ? "Con equipo" : "Sin equipo", null, 12, LIGHT)
            };
            for (int i = 0; i < cells.length; i++) {
                JPanel holder = new JPanel(new BorderLayout());
                holder.setOpaque(false);
                holder.setPreferredSize(new Dimension(10, 54));
                holder.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 36)));
                JPanel inner = new JPanel(new GridBagLayout());
                inner.setOpaque(false);
                GridBagConstraints gc = new GridBagConstraints();
                gc.weightx = 1;
                gc.anchor = (i == 2 || i == 3) ? GridBagConstraints.CENTER : GridBagConstraints.WEST;
                inner.add(cells[i], gc);
                holder.add(inner, BorderLayout.CENTER);
                table.add(holder, cell(i, r));
            }
            r++;
        }
        col.add(Proto.left(Proto.noStretch(table)));
        col.add(Box.createVerticalStrut(12));
        col.add(Proto.left(Proto.text("Cardio y core van por tiempo; el resto, por repeticiones. Detente si sientes "
                + "dolor, mareo o falta de aire.", null, 12, LIGHT)));
        card.add(col, BorderLayout.CENTER);
        return Proto.noStretch(card);
    }

    private static GridBagConstraints cell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = COLUMNS[x];
        c.fill = GridBagConstraints.BOTH;
        return c;
    }

    private static JComponent centered(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private static JComponent badge(boolean lowImpact) {
        JLabel l = new JLabel(lowImpact ? "Bajo impacto" : "Alto impacto") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(lowImpact ? Proto.GREEN_SOFT : WARN_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Proto.font("SemiBold", 12));
        l.setForeground(lowImpact ? new Color(0x0B5F52) : WARN_TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return l;
    }

    /** Repeticiones ("12 reps") o duración ("45 s") según el tipo de línea. */
    private static String amount(ExerciseRoutineExercise line) {
        if (line instanceof ExerciseByReps reps) {
            return reps.getReps() + " reps";
        }
        if (line instanceof ExerciseByTime time) {
            return time.getDuration().toSeconds() + " s";
        }
        return "—";
    }

    // ------------------------------------------------------------------ sesión de hoy

    /** Ejercicios en el orden de la sesión: calentamiento, fuerza (grande a pequeño), core y cardio intenso. */
    private List<ExerciseRoutineExercise> ordered(ExerciseRoutine routine) throws NutriTrackException {
        List<ExerciseRoutineExercise> lines = new ArrayList<>(routine.getLines());
        java.util.Map<Integer, Integer> rank = new java.util.HashMap<>();
        for (ExerciseRoutineExercise line : lines) {
            Exercise ex = services.exercises().getById(line.getExerciseId());
            String group = ex.getMuscleGroup() == null ? "" : ex.getMuscleGroup().toUpperCase();
            int r = "CARDIO".equals(group) ? (ex.isLowImpact() ? 0 : 8) : ORDER.getOrDefault(group, 7);
            rank.put(line.getExerciseId(), r);
        }
        lines.sort(java.util.Comparator.comparingInt(l -> rank.get(l.getExerciseId())));
        return lines;
    }

    /** Panel para guardar la sesión: minutos, si se completó, Cancelar / Guardar. */
    private void openSessionDialog(ExerciseRoutine routine) {
        javax.swing.JDialog d = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(this),
                "Registrar sesión", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(new Color(0xD6DEE4)));

        Proto.Photo photo = new Proto.Photo(Images.get(Images.CARD_ROUTINE), 0.5, 0.35, 96, 0, 0).shaded();
        photo.setLayout(new BorderLayout());
        photo.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        JPanel ptxt = Proto.column();
        ptxt.add(Proto.left(Proto.text("Sesión de hoy · " + intensity(routine), null, 12, new Color(0xD5E6EA))));
        ptxt.add(Proto.left(Proto.text("¿Cómo te fue?", "Bold", 19, Color.WHITE)));
        photo.add(ptxt, BorderLayout.SOUTH);
        root.add(photo, BorderLayout.NORTH);

        JPanel col = Proto.column();
        col.setOpaque(true);
        col.setBackground(Color.WHITE);
        col.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        Proto.Stepper minutes = new Proto.Stepper(30, 5, 240, 5, false, Proto.GREEN, Proto.GREEN_SOFT, "minutos");
        col.add(Proto.left(Proto.text("Minutos entrenados", "SemiBold", 13, Proto.INK_2)));
        col.add(Box.createVerticalStrut(8));
        col.add(Proto.left(minutes));
        col.add(Box.createVerticalStrut(16));
        Proto.ChoiceGrid done = new Proto.ChoiceGrid(2, Proto.options("si", "Sí, completa", "no", "No, me faltó"),
                Proto.GREEN, 13);
        done.setValue("si");
        col.add(Proto.left(Proto.text("¿Terminaste la rutina?", "SemiBold", 13, Proto.INK_2)));
        col.add(Box.createVerticalStrut(8));
        col.add(Proto.left(done));
        col.add(Box.createVerticalStrut(16));
        col.add(Proto.left(Proto.wrapped("Al completar sesiones, cada 14 días se suma una repetición a los ejercicios "
                + "por repeticiones.", 300, null, 12, Proto.MUTED)));
        col.add(Box.createVerticalStrut(16));
        JPanel actions = new JPanel(new java.awt.GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        JButton cancel = Proto.cancelButton("Cancelar");
        cancel.addActionListener(e -> d.dispose());
        JButton save = Proto.greenButton("Guardar sesión");
        save.addActionListener(e -> {
            d.dispose();
            saveSession((int) minutes.getValue(), "si".equals(done.getValue()));
        });
        actions.add(cancel);
        actions.add(save);
        col.add(Proto.left(Proto.noStretch(actions)));
        root.add(col, BorderLayout.CENTER);

        d.setUndecorated(true);
        d.setContentPane(root);
        d.setSize(420, 430);
        d.setLocationRelativeTo(this);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setVisible(true);
    }

    private void saveSession(int minutes, boolean completed) {
        try {
            RoutineFollowUp saved = services.followUps().registerSession(services.currentUser().getIdUser(),
                    LocalDate.now(), completed, minutes, null);
            if (saved.getObservations() != null && saved.getObservations().contains("[+1 rep]")) {
                Messages.showInfo(this, "¡Bien! Llevas buen ritmo: sumamos una repetición a tus ejercicios.");
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    /** Tarjeta con la foto del gimnasio de fondo, oscurecida para que se lea la tabla. */
    private static final class PhotoCard extends JPanel {

        PhotoCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                g2.setClip(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 32, 32));
                g2.setColor(new Color(0x0B1E25));
                g2.fillRect(0, 0, getWidth(), getHeight());
                Proto.paintCover(g2, Images.get(Images.CARD_ROUTINE), getWidth(), getHeight(), 0.5, 0.3);
                g2.setPaint(new java.awt.LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 0.6f, 1f},
                        new Color[]{new Color(6, 24, 30, 230), new Color(6, 24, 30, 199), new Color(6, 24, 30, 179)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ acciones

    private void changeLocation(UserProfile profile, String place) {
        if (place == null || place.equalsIgnoreCase(profile.getTrainingLocation())) {
            return;
        }
        try {
            profile.setTrainingLocation(place);
            services.profiles().save(profile);
            RoutineResult result = services.routines().generateRoutine(profile.getUserId());
            lastWarnings = new ArrayList<>(result.getWarnings());
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
    }

    private void generate(boolean replacing) {
        if (replacing && !Messages.confirm(this,
                "Se generará una rutina nueva y la actual pasará a tu historial. ¿Continuar?")) {
            return;
        }
        try {
            RoutineResult result = services.routines().generateRoutine(services.currentUser().getIdUser());
            lastWarnings = new ArrayList<>(result.getWarnings());
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        refresh();
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
}
