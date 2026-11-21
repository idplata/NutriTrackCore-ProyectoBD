package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMI;
import com.nutritrackcore.model.BMIClasification;
import com.nutritrackcore.model.Sex;
import com.nutritrackcore.services.AuthenticationService;
import com.nutritrackcore.services.ParameterChecks;
import com.nutritrackcore.services.dto.RegistrationData;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;

/**
 * Pantalla de registro en dos pasos.
 * <ol>
 *   <li><b>Crear cuenta:</b> nombre, correo, contraseña y autorización de datos.
 *       Al pulsar "Continuar" se revisan con las mismas reglas del servicio
 *       ({@link ParameterChecks}) para avisar los errores enseguida.</li>
 *   <li><b>Tus medidas:</b> peso, estatura, fecha de nacimiento y sexo. Muestra
 *       el IMC estimado mientras se escribe. Al pulsar "Crear cuenta" llama a
 *       {@link AuthenticationService#register} con todos los datos.</li>
 * </ol>
 * La cuenta solo se guarda al final del paso 2; volver atrás no pierde lo escrito.
 */
public class UserRegistrationPanel extends JPanel {

    private static final int FORM_WIDTH = 420;
    private static final int HALF_WIDTH = (FORM_WIDTH - 14) / 2;
    private static final String STEP_ACCOUNT = "account";
    private static final String STEP_MEASUREMENTS = "measurements";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final AuthenticationService authenticationService;
    private final Consumer<String> onRegistered;
    private final Runnable onBackToLogin;

    private final CardLayout steps = new CardLayout();

    // Paso 1
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JCheckBox consentCheck = new JCheckBox();
    private final JButton continueButton =
            FormKit.primaryButton("Continuar", Icons.arrowRight(18, Color.WHITE), FORM_WIDTH);

    // Paso 2
    private final JTextField weightField = new JTextField();
    private final JTextField heightField = new JTextField();
    private final JFormattedTextField birthDateField = new JFormattedTextField(dateMask());
    private final JToggleButton femaleButton = new JToggleButton("Femenino");
    private final JToggleButton maleButton = new JToggleButton("Masculino");
    private final JToggleButton otherButton = new JToggleButton("Otro");
    private final ButtonGroup sexGroup = new ButtonGroup();
    private final JLabel bmiValue = new JLabel("—");
    private final PillLabel bmiCategory = new PillLabel();
    private final JButton createButton =
            FormKit.primaryButton("Crear cuenta", Icons.check(18, Color.WHITE), FORM_WIDTH - 142);

    /**
     * Crea la pantalla.
     *
     * @param authenticationService servicio que registra la cuenta
     * @param onRegistered          qué hacer cuando la cuenta se crea (recibe el correo)
     * @param onBackToLogin         qué hacer cuando el usuario pulsa "Inicia sesión"
     */
    public UserRegistrationPanel(AuthenticationService authenticationService, Consumer<String> onRegistered,
                                 Runnable onBackToLogin) {
        this.authenticationService = authenticationService;
        this.onRegistered = onRegistered;
        this.onBackToLogin = onBackToLogin;

        setLayout(steps);
        setBackground(Theme.BACKGROUND);
        add(createScreen(Icons.StepState.CURRENT, Icons.StepState.PENDING, createAccountForm()), STEP_ACCOUNT);
        add(createScreen(Icons.StepState.DONE, Icons.StepState.CURRENT, createMeasurementsForm()), STEP_MEASUREMENTS);
        steps.show(this, STEP_ACCOUNT);
    }

    /** Deja la pantalla vacía y en el paso 1 (al entrar o al cancelar). */
    public void reset() {
        for (JTextField f : List.of(nameField, emailField, passwordField, confirmField, weightField, heightField)) {
            f.setText("");
        }
        birthDateField.setValue(null);
        birthDateField.setText("");
        consentCheck.setSelected(false);
        sexGroup.clearSelection();
        updateBmiPreview();
        showStep(STEP_ACCOUNT);
    }

    // ------------------------------------------------------------------ armado

    private JPanel createScreen(Icons.StepState step1, Icons.StepState step2, JPanel form) {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(Theme.BACKGROUND);

        BrandPanel brand = new BrandPanel(
                "Empieza hoy.", "A tu medida.",
                "Crea tu cuenta y danos tus medidas. Con eso calculamos tu IMC y armamos tu plan "
                + "de alimentación y tu rutina.",
                List.of(
                        BrandPanel.Feature.step(1, step1, "Crea tu cuenta", "Nombre, correo y contraseña."),
                        BrandPanel.Feature.step(2, step2, "Tus medidas",
                                "Peso, estatura, fecha de nacimiento y sexo."),
                        BrandPanel.Feature.step(3, Icons.StepState.PENDING, "Recibe tu plan",
                                "Dieta y rutina según tu condición.")));

        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(Theme.BACKGROUND);
        area.setPreferredSize(new Dimension(FORM_WIDTH + 80, 600));
        area.setMinimumSize(new Dimension(FORM_WIDTH + 40, 600));
        area.add(form, new GridBagConstraints());

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.52;
        screen.add(brand, c);
        c.gridx = 1;
        c.weightx = 0.48;
        screen.add(area, c);
        return screen;
    }

    private JPanel createAccountForm() {
        JPanel form = column();
        form.add(FormKit.row(FormKit.eyebrow("PASO 1 DE 2"), FORM_WIDTH));
        form.add(Box.createVerticalStrut(4));
        form.add(FormKit.row(FormKit.title("Crear cuenta"), FORM_WIDTH));
        form.add(Box.createVerticalStrut(4));
        form.add(FormKit.row(FormKit.subtitle("Solo te tomará un minuto.", 320), FORM_WIDTH));
        form.add(Box.createVerticalStrut(24));

        FormKit.styleField(nameField, "Ej. Ana Gómez", Icons.user(20, Theme.ICON), FORM_WIDTH);
        addLabeled(form, "Nombre completo", nameField);
        form.add(Box.createVerticalStrut(14));

        FormKit.styleField(emailField, "tu@correo.com", Icons.mail(20, Theme.ICON), FORM_WIDTH);
        addLabeled(form, "Correo electrónico", emailField);
        form.add(Box.createVerticalStrut(14));

        FormKit.styleField(passwordField, "Mínimo 8", Icons.lock(20, Theme.ICON), HALF_WIDTH);
        FormKit.styleField(confirmField, "Repítela", Icons.lock(20, Theme.ICON), HALF_WIDTH);
        form.add(FormKit.row(pair(labeled("Contraseña", passwordField), labeled("Confirmar", confirmField)),
                FORM_WIDTH));
        form.add(Box.createVerticalStrut(6));
        JLabel hint = new JLabel("Usa al menos 8 caracteres, con letras y números.");
        hint.setFont(Theme.font(Font.PLAIN, 12));
        hint.setForeground(new Color(0x5C6B77));
        form.add(FormKit.row(hint, FORM_WIDTH));
        form.add(Box.createVerticalStrut(16));

        form.add(FormKit.row(createConsentRow(), FORM_WIDTH));
        form.add(Box.createVerticalStrut(22));

        continueButton.addActionListener(e -> goToMeasurements());
        form.add(FormKit.row(continueButton, FORM_WIDTH));
        form.add(Box.createVerticalStrut(22));

        form.add(FormKit.row(loginLine(), FORM_WIDTH));
        return form;
    }

    private JPanel createMeasurementsForm() {
        JPanel form = column();
        form.add(FormKit.row(FormKit.eyebrow("PASO 2 DE 2"), FORM_WIDTH));
        form.add(Box.createVerticalStrut(4));
        form.add(FormKit.row(FormKit.title("Tus medidas"), FORM_WIDTH));
        form.add(Box.createVerticalStrut(4));
        form.add(FormKit.row(FormKit.subtitle(
                "Las usamos para calcular tu IMC y las calorías de tu plan.", 300), FORM_WIDTH));
        form.add(Box.createVerticalStrut(24));

        FormKit.styleField(weightField, "Ej. 62,5", null, HALF_WIDTH);
        FormKit.addUnit(weightField, "kg");
        FormKit.styleField(heightField, "Ej. 165", null, HALF_WIDTH);
        FormKit.addUnit(heightField, "cm");
        form.add(FormKit.row(pair(labeled("Peso", weightField), labeled("Estatura", heightField)), FORM_WIDTH));
        form.add(Box.createVerticalStrut(14));

        FormKit.styleField(birthDateField, null, Icons.calendar(20, Theme.ICON), FORM_WIDTH);
        birthDateField.setFocusLostBehavior(JFormattedTextField.PERSIST);
        birthDateField.setToolTipText("Escribe la fecha como DD/MM/AAAA");
        addLabeled(form, "Fecha de nacimiento (DD/MM/AAAA)", birthDateField);
        form.add(Box.createVerticalStrut(14));

        form.add(FormKit.row(FormKit.fieldLabel("Sexo", femaleButton), FORM_WIDTH));
        form.add(Box.createVerticalStrut(8));
        form.add(FormKit.row(createSexSelector(), FORM_WIDTH));
        form.add(Box.createVerticalStrut(18));

        form.add(FormKit.row(createBmiCard(), FORM_WIDTH));
        form.add(Box.createVerticalStrut(22));

        JButton back = FormKit.secondaryButton("Atrás", Icons.arrowLeft(18, Theme.TEXT_LABEL), 130);
        back.addActionListener(e -> showStep(STEP_ACCOUNT));
        createButton.addActionListener(e -> createAccount());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, -12, 0, 0));
        buttons.add(back);
        buttons.add(createButton);
        form.add(FormKit.row(buttons, FORM_WIDTH));
        form.add(Box.createVerticalStrut(20));

        form.add(FormKit.row(loginLine(), FORM_WIDTH));

        DocumentListener bmiListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBmiPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBmiPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBmiPreview();
            }
        };
        weightField.getDocument().addDocumentListener(bmiListener);
        heightField.getDocument().addDocumentListener(bmiListener);
        updateBmiPreview();
        return form;
    }

    private JPanel createConsentRow() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        consentCheck.setOpaque(false);
        consentCheck.setVerticalAlignment(JCheckBox.TOP);
        JLabel text = new JLabel("<html><div style='width:290px'>Autorizo el tratamiento de mis datos "
                + "personales y de salud para generar mi plan.</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 13));
        text.setForeground(Theme.TEXT_LABEL);
        text.setLabelFor(consentCheck);
        consentCheck.getAccessibleContext().setAccessibleName(
                "Autorizo el tratamiento de mis datos personales y de salud");
        // Clic en el texto también marca la casilla.
        text.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                consentCheck.doClick();
            }
        });
        row.add(consentCheck, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private JPanel createSexSelector() {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        for (JToggleButton b : List.of(femaleButton, maleButton, otherButton)) {
            b.setFont(Theme.font("Medium", 14));
            b.setForeground(Theme.TEXT_LABEL);
            b.setFocusPainted(false);
            b.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 10; focusWidth: 0; borderColor: #D6DEE4; background: #FFFFFF;"
                    + "hoverBackground: #F4F7F8; selectedBackground: #E7F4F1; selectedForeground: #08594C;"
                    + "pressedBackground: #D3EBE6");
            b.setPreferredSize(new Dimension(10, 44));
            sexGroup.add(b);
            row.add(b);
        }
        row.setPreferredSize(new Dimension(FORM_WIDTH, 44));
        row.setMaximumSize(new Dimension(FORM_WIDTH, 44));
        return row;
    }

    private JPanel createBmiCard() {
        JPanel card = new RoundedPanel(new Color(0xF1F8F6), 12);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel caption = new JLabel("Tu IMC estimado");
        caption.setFont(Theme.font("Medium", 12));
        caption.setForeground(Theme.TEXT_MUTED);
        bmiValue.setFont(Theme.font(Font.BOLD, 26));
        bmiValue.setForeground(Theme.TEXT);
        texts.add(caption);
        texts.add(bmiValue);

        JPanel pillBox = new JPanel(new GridBagLayout());
        pillBox.setOpaque(false);
        pillBox.add(bmiCategory);

        card.add(texts, BorderLayout.CENTER);
        card.add(pillBox, BorderLayout.EAST);
        card.setPreferredSize(new Dimension(FORM_WIDTH, 76));
        card.setMaximumSize(new Dimension(FORM_WIDTH, 76));
        return card;
    }

    private JPanel loginLine() {
        JButton login = FormKit.linkButton("Inicia sesión");
        login.addActionListener(e -> {
            reset();
            onBackToLogin.run();
        });
        return FormKit.questionLine("¿Ya tienes una cuenta?", login);
    }

    // ------------------------------------------------------------------ acciones

    private void goToMeasurements() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        try {
            // Mismas reglas que aplicará el servicio al final: así el error se ve en este paso.
            ParameterChecks.requireTextMax(nameField.getText(), ParameterChecks.MAX_USER_TEXT_LENGTH, "nombre");
            ParameterChecks.requireEmail(emailField.getText());
            ParameterChecks.requirePassword(new String(password));
            if (!Arrays.equals(password, confirm)) {
                Messages.showInfo(this, "Las contraseñas no coinciden.");
                confirmField.selectAll();
                confirmField.requestFocusInWindow();
                return;
            }
            if (!consentCheck.isSelected()) {
                Messages.showInfo(this, "Para continuar debes autorizar el tratamiento de tus datos.");
                consentCheck.requestFocusInWindow();
                return;
            }
            showStep(STEP_MEASUREMENTS);
        } catch (IllegalArgumentException ex) {
            Messages.showError(this, ex);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void createAccount() {
        Double weight = parseNumber(weightField.getText());
        if (weight == null) {
            askToFix("Escribe tu peso en kilogramos, por ejemplo 62,5.", weightField);
            return;
        }
        Double height = parseNumber(heightField.getText());
        if (height == null) {
            askToFix("Escribe tu estatura en centímetros, por ejemplo 165.", heightField);
            return;
        }
        LocalDate birthDate = parseDate(birthDateField.getText());
        if (birthDate == null) {
            askToFix("Escribe tu fecha de nacimiento como DD/MM/AAAA, por ejemplo 14/03/2001.", birthDateField);
            return;
        }
        Sex sex = selectedSex();
        if (sex == null) {
            askToFix("Elige tu sexo.", femaleButton);
            return;
        }

        char[] password = passwordField.getPassword();
        try {
            String email = emailField.getText().trim();
            authenticationService.register(new RegistrationData(nameField.getText().trim(), email,
                    new String(password), weight, height, birthDate, sex));
            Messages.showInfo(this, "¡Listo! Tu cuenta quedó creada.\nInicia sesión con tu correo y contraseña.");
            reset();
            onRegistered.accept(email);
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void updateBmiPreview() {
        Double weight = parseNumber(weightField.getText());
        Double height = parseNumber(heightField.getText());
        boolean valid = weight != null && height != null
                && weight >= ParameterChecks.MIN_WEIGHT_KG && weight <= ParameterChecks.MAX_WEIGHT_KG
                && height >= ParameterChecks.MIN_HEIGHT_CM && height <= ParameterChecks.MAX_HEIGHT_CM;
        if (!valid) {
            bmiValue.setText("—");
            bmiCategory.setVisible(false);
            return;
        }
        double bmi = BMI.calculate(weight, height);
        BMIClasification category = BMI.classify(bmi);
        bmiValue.setText(String.format("%.1f", bmi).replace('.', ','));
        bmiCategory.setText(category.getLabel());
        bmiCategory.setFill(FormKit.bmiColor(category.getLabel()));
        bmiCategory.setVisible(true);
    }

    // ------------------------------------------------------------------ ayudas

    private void showStep(String step) {
        steps.show(this, step);
        SwingUtilities.invokeLater(() -> {
            JButton defaultButton = STEP_ACCOUNT.equals(step) ? continueButton : createButton;
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(defaultButton);   // Enter avanza al siguiente paso
            }
            (STEP_ACCOUNT.equals(step) ? nameField : weightField).requestFocusInWindow();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        showStep(STEP_ACCOUNT);
    }

    private void askToFix(String message, JComponent field) {
        Messages.showInfo(this, message);
        field.requestFocusInWindow();
    }

    private Sex selectedSex() {
        if (femaleButton.isSelected()) {
            return Sex.F;
        }
        if (maleButton.isSelected()) {
            return Sex.M;
        }
        if (otherButton.isSelected()) {
            return Sex.O;
        }
        return null;
    }

    /** Lee un número aceptando coma o punto decimal; devuelve {@code null} si no es válido. */
    private static Double parseNumber(String text) {
        String clean = text == null ? "" : text.trim().replace(',', '.');
        if (clean.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(clean);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Lee una fecha DD/MM/AAAA; devuelve {@code null} si está incompleta o no existe (ej. 31/02). */
    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text == null ? "" : text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static MaskFormatter dateMask() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            return mask;
        } catch (ParseException e) {
            throw new IllegalStateException(e);   // el patrón es fijo; no puede fallar
        }
    }

    private static JPanel column() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        return form;
    }

    private static void addLabeled(JPanel form, String label, JComponent field) {
        form.add(FormKit.row(FormKit.fieldLabel(label, field), FORM_WIDTH));
        form.add(Box.createVerticalStrut(8));
        form.add(FormKit.row(field, FORM_WIDTH));
    }

    private static JPanel labeled(String label, JComponent field) {
        JPanel box = column();
        JLabel l = FormKit.fieldLabel(label, field);
        l.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        box.add(l);
        box.add(Box.createVerticalStrut(8));
        box.add(field);
        return box;
    }

    private static JPanel pair(JPanel left, JPanel right) {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.add(left);
        row.add(right);
        return row;
    }

    /** Panel con fondo redondeado. */
    private static final class RoundedPanel extends JPanel {

        private final Color fill;
        private final int arc;

        RoundedPanel(Color fill, int arc) {
            this.fill = fill;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc * 2, arc * 2);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Etiqueta con forma de píldora de color (para la clasificación del IMC). */
    static final class PillLabel extends JLabel {

        private Color fill = Theme.ACCENT;

        PillLabel() {
            setFont(Theme.font("SemiBold", 13));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            setOpaque(false);
        }

        void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
