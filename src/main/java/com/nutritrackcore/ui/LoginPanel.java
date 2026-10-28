package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.User;
import com.nutritrackcore.services.AuthenticationService;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
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
    private final JButton loginButton = new JButton("Iniciar sesión");

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
        // Enter en cualquier campo dispara "Iniciar sesión".
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(loginButton);
            }
            (emailField.getText().isBlank() ? emailField : passwordField).requestFocusInWindow();
        });
    }

    /** Limpia la contraseña; se usa al cerrar sesión y volver a esta pantalla. */
    public void reset() {
        passwordField.setText("");
        loadRememberedEmail();
    }

    private JPanel createFormArea() {
        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(Theme.BACKGROUND);
        // Ancho base pequeño: el GridBag reparte el espacio sobrante según weightx.
        area.setPreferredSize(new Dimension(FORM_WIDTH + 80, 600));
        area.setMinimumSize(new Dimension(FORM_WIDTH + 40, 600));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Iniciar sesión");
        title.setFont(Theme.font(Font.BOLD, 34));
        title.setForeground(Theme.TEXT);
        form.add(row(title));
        form.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("<html><div style='width:300px'>Bienvenido de nuevo. "
                + "Continúa con tu plan personalizado.</div></html>");
        subtitle.setFont(Theme.font(Font.PLAIN, 15));
        subtitle.setForeground(Theme.TEXT_MUTED);
        form.add(row(subtitle));
        form.add(Box.createVerticalStrut(32));

        styleField(emailField, "tu@correo.com", Icons.mail(20, Theme.ICON));
        form.add(row(fieldLabel("Correo electrónico", emailField)));
        form.add(Box.createVerticalStrut(8));
        form.add(row(emailField));
        form.add(Box.createVerticalStrut(20));

        styleField(passwordField, "Ingresa tu contraseña", Icons.lock(20, Theme.ICON));
        form.add(row(fieldLabel("Contraseña", passwordField)));
        form.add(Box.createVerticalStrut(8));
        form.add(row(passwordField));
        form.add(Box.createVerticalStrut(18));

        rememberCheck.setOpaque(false);
        rememberCheck.setFont(Theme.font(Font.PLAIN, 14));
        rememberCheck.setForeground(Theme.TEXT_LABEL);
        rememberCheck.setFocusPainted(false);
        form.add(row(rememberCheck));
        form.add(Box.createVerticalStrut(26));

        loginButton.setIcon(Icons.arrowRight(18, Color.WHITE));
        loginButton.setHorizontalTextPosition(SwingConstants.LEADING);
        loginButton.setIconTextGap(10);
        loginButton.setFont(Theme.font("SemiBold", 16));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; borderWidth: 0; focusWidth: 0; innerFocusWidth: 0;"
                + "background: #0B7A69; foreground: #FFFFFF;"
                + "hoverBackground: #096A5B; pressedBackground: #08594C;"
                + "default.borderWidth: 0");
        fixSize(loginButton, 54);
        loginButton.addActionListener(e -> doLogin());
        form.add(row(loginButton));
        form.add(Box.createVerticalStrut(30));

        form.add(row(createRegisterLine()));

        area.add(form, new GridBagConstraints());
        return area;
    }

    private JPanel createRegisterLine() {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        line.setOpaque(false);
        JLabel question = new JLabel("¿No tienes una cuenta?");
        question.setFont(Theme.font(Font.PLAIN, 14));
        question.setForeground(Theme.TEXT_MUTED);

        JButton register = new JButton("Regístrate");
        register.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        register.putClientProperty(FlatClientProperties.STYLE,
                "margin: 2,4,2,4; foreground: #0B7A69; hoverBackground: #E7F4F1; pressedBackground: #D3EBE6");
        register.setFont(Theme.font("SemiBold", 14));
        register.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        register.addActionListener(e -> onRegisterRequested.run());

        line.add(question);
        line.add(register);
        return line;
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

    private static void styleField(JTextField field, String placeholder, javax.swing.Icon icon) {
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setForeground(Theme.TEXT);
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, icon);
        field.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; margin: 4,14,4,12; iconTextGap: 12; placeholderForeground: #8A97A3");
        fixSize(field, 50);
    }

    private static JLabel fieldLabel(String text, JComponent target) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font("Medium", 14));
        label.setForeground(Theme.TEXT_LABEL);
        label.setLabelFor(target);
        return label;
    }

    private static void fixSize(JComponent c, int height) {
        c.setPreferredSize(new Dimension(FORM_WIDTH, height));
        c.setMaximumSize(new Dimension(FORM_WIDTH, height));
    }

    /** Alinea a la izquierda y fija el ancho del formulario. */
    private static Component row(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JLabel || c instanceof JCheckBox || c instanceof JPanel) {
            c.setMaximumSize(new Dimension(FORM_WIDTH, c.getPreferredSize().height));
        }
        if (c instanceof JPanel) {
            c.setPreferredSize(new Dimension(FORM_WIDTH, c.getPreferredSize().height));
        }
        return c;
    }
}
