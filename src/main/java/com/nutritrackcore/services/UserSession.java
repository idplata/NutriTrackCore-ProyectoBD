package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.AutenticationException;
import com.nutritrackcore.model.User;
import java.util.Objects;

/**
 * Usuario con la sesión iniciada.
 */
public class UserSession {
    private User currentUser;

    public void start(User user) {
        this.currentUser = Objects.requireNonNull(user, "usuario");
    }

    public void close() {
        this.currentUser = null;
    }

    public boolean hasSession() {
        return currentUser != null;
    }

    public User getCurrentUser() throws AutenticationException {
        if (currentUser == null) {
            throw new AutenticationException("No hay una sesión iniciada.");
        }
        return currentUser;
    }
}