package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.User;
import com.nutritrackcore.services.AuthenticationService;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Pantalla de inicio de sesión.
 * <p>
 * A la izquierda muestra el {@link BrandPanel} con la foto; a la derecha el
 * formulario de correo y contraseña. El panel no tiene lógica de negocio: al
 * pulsar "Iniciar sesión" solo llama a {@link AuthenticationService#login} y,
 * si sale bien, avisa a la ventana principal con {@code onLoginSuccess}.
 */
public class LoginPanel extends JPanel {

    private static final int FORM_WIDTH = 400;
    private static final String PREF_REMEMBERED_EMAIL = "rememberedEmail";

    private final AuthenticationService authenticationService;
    private final Consumer<User> onLoginSuccess;
    private final Runnable onRegisterRequested;
    private final Preferences preferences = Preferences.userNodeForPackage(LoginPanel.class);

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JCheckBox rememberCheck = new JCheckBox("Recordarme");
    private final JButton loginButton =
            FormKit.primaryButton("Iniciar sesión", Icons.arrowRight(18, Color.WHITE), FORM_WIDTH);

    /**
     * Crea la pantalla.
     *
     * @param authenticationService servicio que valida las credenciales y abre la sesión
     * @param onLoginSuccess        qué hacer cuando el usuario entra (recibe el usuario autenticado)
     * @param onRegisterRequested   qué hacer cuando el usuario pulsa "Regístrate"
     */
    public LoginPanel(AuthenticationService authenticationService, Consumer<User> onLoginSuccess,
                      Runnable onRegisterRequested) {
        this.authenticationService = authenticationService;
        this.onLoginSuccess = onLoginSuccess;
        this.onRegisterRequested = onRegisterRequested;

        setLayout(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        BrandPanel brand = new BrandPanel(
                "Come mejor. Entrena.", "Avanza.",
                "Planes de alimentación y rutinas de ejercicio ajustados a tu cuerpo y a tus condiciones médicas.",
                List.of(
                        new BrandPanel.Feature(Icons.dumbbell(22, Theme.ACCENT_LIGHT),
                                "Rutinas de ejercicio", "Según tu nivel y tu condición médica."),
                        new BrandPanel.Feature(Icons.utensils(22, Theme.ACCENT_LIGHT),
                                "Planes de alimentación", "Porciones orientativas y alternativas."),
                        new BrandPanel.Feature(Icons.chart(22, Theme.ACCENT_LIGHT),
                                "Seguimiento de progreso", "Tu IMC y tu historial, siempre a la vista.")));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.52;
        add(brand, c);
        c.gridx = 1;
        c.weightx = 0.48;
        add(createFormArea(), c);

        loadRememberedEmail();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::focusFirstField);
    }

    /**
     * Prepara la pantalla al volver a ella: limpia la contraseña, activa Enter
     * para iniciar sesión y pone el cursor en el primer campo vacío.
     */
    public void reset() {
        passwordField.setText("");
        loadRememberedEmail();
        focusFirstField();
    }

    /**
     * Escribe un correo en el campo (por ejemplo, el de la cuenta recién creada)
     * y deja el cursor en la contraseña.
     *
     * @param email correo a mostrar
     */
    public void prefillEmail(String email) {
        passwordField.setText("");
        emailField.setText(email);
        focusFirstField();
    }

    private void focusFirstField() {
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(loginButton);    // Enter en cualquier campo inicia sesión
        }
        (emailField.getText().isBlank() ? emailField : passwordField).requestFocusInWindow();
    }

    private JPanel createFormArea() {
        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(Theme.BACKGROUND);
        area.setPreferredSize(new Dimension(500, 600));   // mismo ancho que el registro: la foto no salta
        area.setMinimumSize(new Dimension(FORM_WIDTH + 40, 600));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(FormKit.row(FormKit.title("Iniciar sesión"), FORM_WIDTH));
        form.add(Box.createVerticalStrut(6));
        form.add(FormKit.row(FormKit.subtitle(
                "Bienvenido de nuevo. Continúa con tu plan personalizado.", 300), FORM_WIDTH));
        form.add(Box.createVerticalStrut(32));

        FormKit.styleField(emailField, "tu@correo.com", Icons.mail(20, Theme.ICON), FORM_WIDTH);
        form.add(FormKit.row(FormKit.fieldLabel("Correo electrónico", emailField), FORM_WIDTH));
        form.add(Box.createVerticalStrut(8));
        form.add(FormKit.row(emailField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(20));

        FormKit.styleField(passwordField, "Ingresa tu contraseña", Icons.lock(20, Theme.ICON), FORM_WIDTH);
        form.add(FormKit.row(FormKit.fieldLabel("Contraseña", passwordField), FORM_WIDTH));
        form.add(Box.createVerticalStrut(8));
        form.add(FormKit.row(passwordField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(18));

        rememberCheck.setOpaque(false);
        rememberCheck.setFont(Theme.font(Font.PLAIN, 14));
        rememberCheck.setForeground(Theme.TEXT_LABEL);
        form.add(FormKit.row(rememberCheck, FORM_WIDTH));
        form.add(Box.createVerticalStrut(26));

        loginButton.addActionListener(e -> doLogin());
        form.add(FormKit.row(loginButton, FORM_WIDTH));
        form.add(Box.createVerticalStrut(30));

        JButton register = FormKit.linkButton("Regístrate");
        register.addActionListener(e -> onRegisterRequested.run());
        form.add(FormKit.row(FormKit.questionLine("¿No tienes una cuenta?", register), FORM_WIDTH));

        area.add(form, new GridBagConstraints());
        return area;
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        char[] password = passwordField.getPassword();
        try {
            if (email.isEmpty() || password.length == 0) {
                Messages.showInfo(this, "Escribe tu correo y tu contraseña.");
                (email.isEmpty() ? emailField : passwordField).requestFocusInWindow();
                return;
            }
            User user = authenticationService.login(email, new String(password));
            saveRememberedEmail(email);
            passwordField.setText("");
            onLoginSuccess.accept(user);
        } catch (PersistenceExceptions | IllegalArgumentException | IllegalStateException ex) {
            Messages.showError(this, ex);
            passwordField.selectAll();
            passwordField.requestFocusInWindow();
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void loadRememberedEmail() {
        String saved = preferences.get(PREF_REMEMBERED_EMAIL, "");
        emailField.setText(saved);
        rememberCheck.setSelected(!saved.isEmpty());
    }

    private void saveRememberedEmail(String email) {
        if (rememberCheck.isSelected()) {
            preferences.put(PREF_REMEMBERED_EMAIL, email);
        } else {
            preferences.remove(PREF_REMEMBERED_EMAIL);
        }
    }
}
