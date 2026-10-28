package com.nutritrackcore.ui;

import com.nutritrackcore.model.User;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Ventana principal. Contiene todas las pantallas en un {@link CardLayout} y
 * cambia entre ellas con {@link #showPanel(String)}.
 * <p>
 * Recibe los servicios ya creados (desde {@code ApplicationContext}) y se los
 * entrega a cada panel por constructor; la ventana no crea repositorios ni
 * contiene lógica de negocio.
 */
public class MainWindow extends JFrame {

    /** Nombre de la pantalla de inicio de sesión. */
    public static final String LOGIN = "login";
    /** Nombre de la pantalla de registro. */
    public static final String REGISTER = "register";
    /** Nombre de la aplicación con el menú lateral (después de iniciar sesión). */
    public static final String APP = "app";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final AppServices services;
    private final LoginPanel loginPanel;
    private final UserRegistrationPanel registrationPanel;
    private final AppShellPanel appShell;

    /**
     * Crea la ventana con sus pantallas.
     *
     * @param services servicios de la aplicación
     */
    public MainWindow(AppServices services) {
        super("NutriTrack");
        this.services = services;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        registrationPanel = new UserRegistrationPanel(services.authentication(), this::onRegistered,
                this::backToLogin);
        loginPanel = new LoginPanel(services.authentication(), this::onLoggedIn, () -> {
            registrationPanel.reset();
            showPanel(REGISTER);
        });
        appShell = new AppShellPanel(services, this::logout);

        content.add(loginPanel, LOGIN);
        content.add(registrationPanel, REGISTER);
        content.add(appShell, APP);
        setContentPane(content);
        showPanel(LOGIN);
    }

    /**
     * Muestra una de las pantallas.
     *
     * @param name {@link #LOGIN}, {@link #REGISTER} o {@link #APP}
     */
    public void showPanel(String name) {
        cards.show(content, name);
    }

    private void backToLogin() {
        showPanel(LOGIN);
        loginPanel.reset();
    }

    private void onRegistered(String email) {
        showPanel(LOGIN);
        loginPanel.prefillEmail(email);
    }

    private void onLoggedIn(User user) {
        appShell.open(user);
        showPanel(APP);
    }

    private void logout() {
        if (!Messages.confirm(this, "¿Quieres cerrar sesión?")) {
            return;
        }
        services.authentication().logout();
        showPanel(LOGIN);
        loginPanel.reset();
    }
}
