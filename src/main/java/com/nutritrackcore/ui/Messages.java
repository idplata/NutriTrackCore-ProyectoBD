package com.nutritrackcore.ui;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Diálogos de mensajes para el usuario.
 * <p>
 * Todos los paneles muestran los errores de los servicios por aquí, así el
 * usuario siempre ve un mensaje claro y la aplicación nunca se cierra ni deja
 * el error solo en la consola.
 */
public final class Messages {

    private Messages() {
    }

    /**
     * Muestra un error devuelto por un servicio.
     * <p>
     * Los servicios lanzan {@link IllegalArgumentException} (dato inválido) o
     * {@link IllegalStateException} (regla de negocio) con el mensaje ya escrito
     * en español, así que se muestra tal cual. Los fallos de almacenamiento
     * ({@link PersistenceExceptions}) se explican aparte.
     *
     * @param parent componente sobre el que se centra el diálogo
     * @param error  excepción recibida
     */
    public static void showError(Component parent, Exception error) {
        String message;
        String title;
        if (error instanceof PersistenceExceptions) {
            title = "Error al guardar o leer los datos";
            message = "No se pudieron leer o guardar los datos.\n" + safe(error.getMessage());
        } else if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            title = "Revisa los datos";
            message = safe(error.getMessage());
        } else {
            title = "Ocurrió un error";
            message = "Ocurrió un error inesperado.\n" + safe(error.getMessage());
        }
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Muestra un mensaje informativo.
     *
     * @param parent  componente sobre el que se centra el diálogo
     * @param message texto a mostrar
     */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "NutriTrack", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Pide confirmación con Sí / No.
     *
     * @param parent  componente sobre el que se centra el diálogo
     * @param message pregunta a mostrar
     * @return {@code true} si el usuario eligió "Sí"
     */
    public static boolean confirm(Component parent, String message) {
        Object[] options = {"Sí", "No"};
        int answer = JOptionPane.showOptionDialog(parent, message, "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return answer == 0;
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "" : text;
    }
}
