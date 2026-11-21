package com.nutritrackcore.services;

import com.nutritrackcore.model.User;

/**
 * Guarda quién es el usuario con la sesión iniciada.
 * <p>
 * No es un singleton ni usa variables estáticas: se crea una sola vez en
 * {@code ApplicationContext} y esa misma instancia se entrega por constructor a
 * los servicios y paneles que la necesiten. Así se puede reemplazar fácilmente
 * en las pruebas.
 */
public class UserSession {

    /** Usuario autenticado, o {@code null} si nadie ha iniciado sesión. */
    private User currentUser;

    /**
     * Abre la sesión para un usuario. Si ya había otra sesión abierta, la
     * reemplaza.
     *
     * @param user usuario que acaba de autenticarse
     * @throws IllegalArgumentException si el usuario es nulo
     */
    public void start(User user) {
        this.currentUser = ParameterChecks.requireNonNull(user, "usuario");
    }

    /**
     * Cierra la sesión actual. No hace nada si no había sesión abierta.
     */
    public void close() {
        this.currentUser = null;
    }

    /**
     * Indica si hay una sesión abierta.
     *
     * @return {@code true} si hay un usuario autenticado
     */
    public boolean hasSession() {
        return currentUser != null;
    }

    /**
     * Devuelve el usuario con la sesión abierta.
     *
     * @return usuario autenticado
     * @throws IllegalStateException si no hay ninguna sesión iniciada
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return currentUser;
    }
}