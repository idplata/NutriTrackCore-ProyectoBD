package com.nutritrackcore.ui;

import com.nutritrackcore.model.User;
import com.nutritrackcore.services.AuthenticationService;
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
    /** Nombre de la pantalla que se ve después de iniciar sesión. */
    public static final String HOME = "home";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final AuthenticationService authenticationService;
    private final LoginPanel loginPanel;
    private final PlaceholderPanel homePanel;

    /**
     * Crea la ventana con sus pantallas.
     *
     * @param authenticationService servicio de registro e inicio de sesión
     */
    public MainWindow(AuthenticationService authenticationService) {
        super("NutriTrack");
        this.authenticationService = authenticationService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 800);
        setLocationRelativeTo(null);

        loginPanel = new LoginPanel(authenticationService, this::onLoggedIn, () -> showPanel(REGISTER));

        // Provisionales mientras se construyen las pantallas reales.
        PlaceholderPanel registerPanel = new PlaceholderPanel("Crear cuenta",
                "La pantalla de registro está en construcción.", "Volver a iniciar sesión",
                () -> showPanel(LOGIN));
        homePanel = new PlaceholderPanel("¡Hola!",
                "Iniciaste sesión correctamente. El menú principal está en construcción.", "Cerrar sesión",
                this::logout);

        content.add(loginPanel, LOGIN);
        content.add(registerPanel, REGISTER);
        content.add(homePanel, HOME);
        setContentPane(content);
        showPanel(LOGIN);
    }

    /**
     * Muestra una de las pantallas.
     *
     * @param name {@link #LOGIN}, {@link #REGISTER} o {@link #HOME}
     */
    public void showPanel(String name) {
        cards.show(content, name);
    }

    private void onLoggedIn(User user) {
        homePanel.setTitle("¡Hola, " + firstName(user.getName()) + "!");
        showPanel(HOME);
    }

    private void logout() {
        authenticationService.logout();
        loginPanel.reset();
        showPanel(LOGIN);
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
