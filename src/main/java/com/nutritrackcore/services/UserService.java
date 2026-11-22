package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.AutenticationException;
import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Datos personales, medidas y contraseña del usuario.
 */
public class UserService {
    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                       PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "calculador de hash");
    }

    public User getById(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundEntityException("No existe un usuario con el identificador " + userId + "."));
    }

    public void updateProfile(int userId, String name, String email) throws NutriTrackException {
        String cleanName = ParameterChecks.requireTextMax(name, ParameterChecks.MAX_USER_TEXT_LENGTH, "nombre");
        String cleanEmail = ParameterChecks.requireEmail(email);
        User user = getById(userId);
        Optional<User> owner = userRepository.findByEmail(cleanEmail);
        if (owner.isPresent() && owner.get().getIdUser() != userId) {
            throw new BussinesRuleException("El correo " + cleanEmail + " ya pertenece a otro usuario.");
        }
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        userRepository.update(user);
    }

    /** Actualiza peso y altura y agrega la medición al historial de IMC. */
    public void updateMeasurements(int userId, double weightKg, double heightCm) throws NutriTrackException {
        ParameterChecks.requireMeasurements(weightKg, heightCm);
        User user = getById(userId);
        double previousWeight = user.getWeightKg();
        double previousHeight = user.getHeightCm();
        user.setWeightKg(weightKg);
        user.setHeightCm(heightCm);
        userRepository.update(user);

        BMIHistory row = new BMIHistory(userId, weightKg, heightCm, LocalDateTime.now());
        ParameterChecks.validateBmiHistory(row);
        try {
            bmiHistoryRepository.save(row);
        } catch (PersistenceExceptions e) {
            user.setWeightKg(previousWeight);       // deshace el cambio de medidas
            user.setHeightCm(previousHeight);
            userRepository.update(user);
            throw e;
        }
    }

    public void changePassword(int userId, String currentPassword, String newPassword) throws NutriTrackException {
        ParameterChecks.requireNonNull(currentPassword, "contraseña actual");
        ParameterChecks.requirePassword(newPassword);
        User user = getById(userId);
        if (!passwordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new AutenticationException("La contraseña actual es incorrecta.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("La contraseña nueva debe ser distinta de la actual.");
        }
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.update(user);
    }
}