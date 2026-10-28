package com.nutritrackcore.ui;

import com.nutritrackcore.model.User;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.JPanel;

/**
 * Estructura de la aplicación después de iniciar sesión: el
 * {@link SidebarPanel} a la izquierda y, a la derecha, la sección elegida.
 * <p>
 * Las secciones que todavía no están construidas muestran un
 * {@link PlaceholderPanel}; se reemplazan aquí a medida que se terminen.
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

    private final CardLayout sections = new CardLayout();
    private final JPanel content = new JPanel(sections);
    private final SidebarPanel sidebar;
    private final HomePanel homePanel;

    /**
     * Crea la estructura.
     *
     * @param services servicios de la aplicación
     * @param onLogout qué hacer al cerrar sesión
     */
    public AppShellPanel(AppServices services, Runnable onLogout) {
        setLayout(new BorderLayout());
        sidebar = new SidebarPanel(this::showSection, onLogout);
        homePanel = new HomePanel(services, this::showSection);

        content.add(homePanel, HOME);
        content.add(placeholder("Mi perfil"), PROFILE);
        content.add(placeholder("Condiciones médicas"), CONDITIONS);
        content.add(placeholder("Registrar consumo"), CONSUMPTION);
        content.add(placeholder("Mi dieta"), DIET);
        content.add(placeholder("Mi rutina"), ROUTINE);
        content.add(placeholder("Mi progreso"), PROGRESS);

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
     * Muestra una sección y la marca en el menú.
     *
     * @param section nombre de la sección (constantes de esta clase)
     */
    public void showSection(String section) {
        sidebar.select(section);
        if (HOME.equals(section)) {
            homePanel.refresh();
        }
        sections.show(content, section);
    }

    private PlaceholderPanel placeholder(String title) {
        return new PlaceholderPanel(title, "Esta sección está en construcción.", "Volver al inicio",
                () -> showSection(HOME));
    }
}
