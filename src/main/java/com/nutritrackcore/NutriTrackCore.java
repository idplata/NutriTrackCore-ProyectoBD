package com.nutritrackcore;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.ui.Theme;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación.
 */
public class NutriTrackCore {

    /** Carpeta de los archivos .txt, junto al ejecutable. */
    private static final String DATA_FOLDER = "data";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                ApplicationContext context = new ApplicationContext(DATA_FOLDER);
                context.createMainWindow().setVisible(true);
            } catch (PersistenceExceptions e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir la carpeta de datos \"" + DATA_FOLDER + "\".\n" + e.getMessage(),
                        "NutriTrack", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
