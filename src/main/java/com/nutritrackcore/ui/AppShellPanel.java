package com.nutritrackcore.ui;

import com.nutritrackcore.model.User;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Estructura de la aplicación después de iniciar sesión: el
 * {@link SidebarPanel} a la izquierda y, a la derecha, la sección elegida.
 * <p>
 * Todas las secciones del menú están construidas; además existe la sección
 * oculta {@link #HABITS} (formulario de hábitos).
 */
public class AppShellPanel extends JPanel {

    /** Sección de Inicio. */
    public static final String HOME = "home";
    /** Sección Mi perfil. */
    public static final String PROFILE = "profile";
    /** Sección Condiciones médicas. */
    public static final String CONDITIONS = "conditions";
    /** Sección Registrar consumo. */
    public static final String CONSUMPTION = "consumption";
    /** Sección Mi dieta. */
    public static final String DIET = "diet";
    /** Sección Mi rutina. */
    public static final String ROUTINE = "routine";
    /** Sección Mi progreso. */
    public static final String PROGRESS = "progress";
    /** Formulario de hábitos (no aparece en el menú lateral). */
    public static final String HABITS = "habits";

    private final CardLayout sections = new CardLayout();
    private final JPanel content = new JPanel(sections);
    private final SidebarPanel sidebar;
    private final AppServices services;
    private final HomePanel homePanel;
    private final Map<String, JComponent> screens = new HashMap<>();

    /**
     * Crea la estructura.
     *
     * @param services servicios de la aplicación
     * @param onLogout qué hacer al cerrar sesión
     * @param onAccountDeleted qué hacer después de eliminar la cuenta
     */
    public AppShellPanel(AppServices services, Runnable onLogout, Runnable onAccountDeleted) {
        setLayout(new BorderLayout());
        this.services = services;
        sidebar = new SidebarPanel(this::showSection, onLogout);
        homePanel = new HomePanel(services, this::showSection);

        addSection(HOME, homePanel);
        addSection(PROFILE, new ProfilePanel(services, this::showSection, onAccountDeleted));
        addSection(CONDITIONS, new ConditionsPanel(services, this::showSection));
        addSection(CONSUMPTION, new ConsumptionPanel(services, this::showSection));
        addSection(DIET, new DietPanel(services, this::showSection));
        addSection(ROUTINE, new RoutinePanel(services, this::showSection));
        addSection(PROGRESS, new ProgressPanel(services, this::showSection));
        addSection(HABITS, new HabitsPanel(services, this::showSection));

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    /**
     * Prepara la estructura para el usuario que acaba de entrar y muestra el Inicio.
     *
     * @param user usuario autenticado
     */
    public void open(User user) {
        sidebar.setUser(user);
        showSection(HOME);
    }

    /**
     * Quien ya dijo que no tiene condiciones (no tiene ninguna y ya llenó sus
     * hábitos) no ve "Condiciones médicas" en el menú.
     */
    private void updateMenu() {
        boolean show = true;
        try {
            if (services.session().hasSession()) {
                int userId = services.currentUser().getIdUser();
                show = !services.conditions().listByUser(userId).isEmpty()
                        || services.profiles().find(userId).isEmpty();
            }
        } catch (com.nutritrackcore.exceptions.NutriTrackException | IllegalStateException ignored) {
            // si no se puede consultar, se deja visible
        }
        sidebar.setSectionVisible(CONDITIONS, show);
    }

    /**
     * Muestra una sección y la marca en el menú.
     *
     * @param section nombre de la sección (constantes de esta clase)
     */
    public void showSection(String section) {
        updateMenu();
        sidebar.select(section);
        if (screens.get(section) instanceof Refreshable screen) {
            screen.refresh();                      // cada pantalla relee sus datos al abrirse
        }
        sections.show(content, section);
    }

    private void addSection(String name, JComponent screen) {
        screens.put(name, screen);
        content.add(screen, name);
    }
}
