package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.UserProfile;
import com.nutritrackcore.services.BmiCategory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Formulario de hábitos, igual al prototipo: marco oscuro con gotas de agua y
 * tres tarjetas con barra azul rey y foto (Actividad y objetivo,
 * Entrenamiento, Alimentación). Se abre desde Condiciones ("No tengo ninguna
 * condición") o desde Mi perfil ("Editar"). A quien tiene condiciones médicas
 * se le explica que el sistema le asigna el plan.
 */
public class HabitsPanel extends ScreenKit.BlueBackground implements Refreshable {

    /** Sección desde la que se abrió el formulario (a ella vuelve "Volver"). */
    private static String origin = AppShellPanel.CONDITIONS;

    private final AppServices services;
    private final Consumer<String> navigate;

    private Proto.ChoiceGrid activity;
    private Proto.ChoiceGrid goal;
    private Proto.ChoiceGrid experience;
    private Proto.ChoiceGrid place;
    private Proto.ChoiceGrid diet;
    private Proto.ChoiceGrid meals;
    private Proto.Stepper days;
    private Proto.Stepper minutes;

    /**
     * @param services servicios de la aplicación
     * @param navigate cambia de sección
     */
    public HabitsPanel(AppServices services, Consumer<String> navigate) {
        super(new BorderLayout(0, 18));
        this.services = services;
        this.navigate = navigate;
        setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));
    }

    /**
     * Indica desde qué sección se abre el formulario, para el enlace de volver
     * y los textos ("Completa tu perfil" o "Editar mis hábitos").
     *
     * @param section {@link AppShellPanel#CONDITIONS} o {@link AppShellPanel#PROFILE}
     */
    public static void openFrom(String section) {
        origin = section;
    }

    @Override
    public void refresh() {
        removeAll();
        if (!services.session().hasSession()) {
            return;
        }
        int userId = services.currentUser().getIdUser();
        try {
            boolean fromProfile = AppShellPanel.PROFILE.equals(origin);
            add(header(fromProfile), BorderLayout.NORTH);
            if (!services.conditions().listByUser(userId).isEmpty()) {
                add(assigned(), BorderLayout.CENTER);
            } else {
                Optional<UserProfile> profile = services.profiles().find(userId);
                add(form(profile.orElse(null), services.bmi().getCurrentBmi(userId).getValue()), BorderLayout.CENTER);
            }
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
        revalidate();
        repaint();
    }

    private JComponent header(boolean fromProfile) {
        JPanel box = Proto.column();
        JButton back = Proto.link(fromProfile ? "Mi perfil" : "Condiciones médicas", new Color(0xCFEFF2), 13);
        back.setIcon(Icons.arrowLeft(14, new Color(0xCFEFF2)));
        back.setIconTextGap(4);
        back.setFont(Proto.font("Medium", 13));
        back.addActionListener(e -> navigate.accept(origin));
        box.add(Proto.left(back));
        box.add(Proto.left(Proto.text(fromProfile ? "Editar mis hábitos" : "Completa tu perfil", "Bold", 30,
                Color.WHITE)));
        box.add(Proto.left(Proto.text(fromProfile
                ? "Actualiza tus hábitos; tu dieta y tu rutina se ajustan con estos datos."
                : "Como no tienes condiciones médicas, armamos tu dieta y tu rutina con estos datos.",
                null, 14, new Color(0xE2F2F5))));
        return box;
    }

    // ------------------------------------------------------------------ con condiciones

    private JComponent assigned() {
        JPanel center = new JPanel(new java.awt.GridBagLayout());
        center.setOpaque(false);
        Proto.HeaderCard card = new Proto.HeaderCard("Tu plan lo arma el sistema", Icons.heartPulse(18, Color.WHITE),
                Proto.Bar.ROYAL, null);
        JPanel body = Proto.column();
        body.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        body.add(Proto.left(Proto.wrapped("Como tienes condiciones médicas registradas, no se usa el perfil de "
                + "hábitos: tu dieta es omnívora en 4 comidas y tu rutina sale de tu IMC y de las reglas de tus "
                + "condiciones.", 420, null, 14, Proto.INK_2)));
        body.add(Box.createVerticalStrut(16));
        JButton go = Proto.button("Ver mis condiciones", Proto.ROYAL, Color.WHITE, null, 44, 14);
        go.addActionListener(e -> navigate.accept(AppShellPanel.CONDITIONS));
        body.add(Proto.left(go));
        card.body().add(body);
        card.setPreferredSize(new Dimension(500, card.getPreferredSize().height));
        center.add(card);
        return center;
    }

    // ------------------------------------------------------------------ formulario

    private JComponent form(UserProfile profile, double bmi) {
        boolean editing = profile != null;
        activity = new Proto.ChoiceGrid(2, Proto.options("sedentario", "Sedentario", "ligero", "Ligero",
                "moderado", "Moderado", "muy_activo", "Muy activo"), Proto.ROYAL, 13);
        goal = new Proto.ChoiceGrid(3, Proto.options("bajar", "Bajar", "mantener", "Mantener", "subir", "Subir"),
                Proto.ROYAL, 13);
        experience = new Proto.ChoiceGrid(3, Proto.options("principiante", "Principiante", "intermedio",
                "Intermedio", "avanzado", "Avanzado"), Proto.ROYAL, 12);
        place = new Proto.ChoiceGrid(2, Proto.options("casa", "Casa", "gimnasio", "Gimnasio"), Proto.ROYAL, 13);
        diet = new Proto.ChoiceGrid(3, Proto.options("OMNIVORA", "Omnívora", "VEGETARIANA", "Vegetariana",
                "VEGANA", "Vegana", "CELIACA", "Celíaca", "FLEXITARIANA", "Flexitariana"), Proto.ROYAL, 12);
        meals = new Proto.ChoiceGrid(4, Proto.options("1", "1", "2", "2", "3", "3", "4", "4"), Proto.ROYAL, 13);
        days = new Proto.Stepper(editing ? profile.getTrainingDaysPerWeek() : 3, 1, 7, 1, false, Proto.ROYAL,
                Proto.ROYAL_SOFT, "días");
        minutes = new Proto.Stepper(editing ? Math.max(15, profile.getMinutesPerSession()) : 45, 15, 120, 15, false,
                Proto.ROYAL, Proto.ROYAL_SOFT, "minutos");

        goal.setOptionEnabled("subir", bmi < BmiCategory.OVERWEIGHT.getMin());
        if (editing) {
            activity.setValue(profile.getActivityLevel());
            goal.setValue(profile.getWeightGoal());
            experience.setValue(profile.getExerciseExperience());
            place.setValue(profile.getTrainingLocation());
            diet.setValue(profile.getDietType());
            meals.setValue(String.valueOf(profile.getMealsPerDay()));
        } else {
            activity.setValue("ligero");
            goal.setValue("bajar");
            experience.setValue("principiante");
            place.setValue("casa");
            diet.setValue("OMNIVORA");
            meals.setValue("4");
        }

        JPanel frame = new DarkFrame();
        frame.setLayout(new BorderLayout(0, 18));
        frame.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel grid = new JPanel(new GridLayout(1, 3, 20, 0));
        grid.setOpaque(false);

        JPanel c1 = block(grid, "Actividad y objetivo", Icons.target(18, Color.WHITE), Images.HABITS_ACTIVITY, 0.40);
        question(c1, "Nivel de actividad", activity);
        question(c1, "¿Qué quieres con tu peso?", goal);

        JPanel c2 = block(grid, "Entrenamiento", Icons.dumbbell(18, Color.WHITE), Images.HABITS_TRAINING, 0.72);
        question(c2, "Experiencia con el ejercicio", experience);
        JPanel pair = new JPanel(new GridLayout(1, 2, 12, 0));
        pair.setOpaque(false);
        JPanel d = Proto.column();
        question(d, "Días por semana", days);
        JPanel m = Proto.column();
        question(m, "Minutos por sesión", minutes);
        pair.add(d);
        pair.add(m);
        c2.add(Proto.left(Proto.noStretch(pair)));
        question(c2, "¿Dónde entrenas?", place);

        JPanel c3 = block(grid, "Alimentación", Icons.utensils(18, Color.WHITE), Images.HABITS_FOOD, 0.55);
        question(c3, "Tipo de dieta", diet);
        question(c3, "Comidas al día", meals);

        frame.add(grid, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255, 255, 255, 56));
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        footer.add(Proto.text("Puedes cambiar estos datos después desde Mi perfil.", null, 12, new Color(0xD5E0E5)),
                BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        JButton back = Proto.button("Volver", Color.WHITE, Proto.ROYAL, Proto.ROYAL, 44, 14);
        back.addActionListener(e -> navigate.accept(origin));
        JButton save = Proto.button("Guardar y continuar", Proto.ROYAL, Color.WHITE, null, 44, 14);
        Proto.withIcon(save, Icons.arrowRight(16, Color.WHITE), true);
        save.addActionListener(e -> save());
        actions.add(back);
        actions.add(save);
        footer.add(actions, BorderLayout.EAST);
        frame.add(footer, BorderLayout.SOUTH);
        return frame;
    }

    /** Agrega a la grilla una tarjeta azul con foto y devuelve la columna donde van las preguntas. */
    private static JPanel block(JPanel grid, String title, Icon icon, String image, double focusY) {
        Proto.HeaderCard card = new Proto.HeaderCard(title, icon, Proto.Bar.ROYAL, null);
        JPanel inside = new JPanel(new BorderLayout());
        inside.setOpaque(false);
        inside.add(new Proto.Photo(Images.get(image), 0.5, focusY, 90, 0, 0), BorderLayout.NORTH);
        JPanel questions = Proto.column();
        questions.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        inside.add(questions, BorderLayout.CENTER);
        card.body().add(inside);
        grid.add(card);
        return questions;
    }

    private static void question(JPanel column, String label, JComponent input) {
        column.add(Proto.left(label.length() > 16 && input instanceof Proto.Stepper
                ? Proto.wrapped(label, 80, "SemiBold", 13, Proto.INK_2)
                : Proto.text(label, "SemiBold", 13, Proto.INK_2)));
        column.add(Box.createVerticalStrut(8));
        column.add(Proto.left(input));
        column.add(Box.createVerticalStrut(16));
    }

    private void save() {
        String[] required = {activity.getValue(), goal.getValue(), experience.getValue(), place.getValue(),
            diet.getValue(), meals.getValue()};
        for (String value : required) {
            if (value == null) {
                Messages.showInfo(this, "Responde todas las preguntas antes de guardar.");
                return;
            }
        }
        int userId = services.currentUser().getIdUser();
        UserProfile profile = new UserProfile(userId, activity.getValue(), experience.getValue(), goal.getValue(),
                (int) days.getValue(), (int) minutes.getValue(), place.getValue(), diet.getValue(),
                Integer.parseInt(meals.getValue()), null);
        try {
            services.profiles().save(profile);
            // Con los hábitos listos se arman la dieta y la rutina (como en el prototipo).
            services.dietPlans().generatePlan(userId);
            services.routines().generateRoutine(userId);
            navigate.accept(AppShellPanel.DIET);
        } catch (NutriTrackException | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        }
    }

    /** Marco negro redondeado con la foto de gotas de agua de fondo. */
    private static final class DarkFrame extends JPanel {

        DarkFrame() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            try {
                g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 32, 32));
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                Proto.paintCover(g2, Images.get(Images.HABITS_BACKGROUND), getWidth(), getHeight(), 0.5, 0.35);
            } finally {
                g2.dispose();
            }
        }
    }
}
