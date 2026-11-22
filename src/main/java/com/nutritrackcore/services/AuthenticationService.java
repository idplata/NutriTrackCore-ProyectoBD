package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.AutenticationException;
import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Registro, inicio y cierre de sesión.
 */
public class AuthenticationService {
    private static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";
    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final PasswordHasher passwordHasher;
    private final UserSession userSession;

    public AuthenticationService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                                 PasswordHasher passwordHasher, UserSession userSession) {
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "calculador de hash");
        this.userSession = Objects.requireNonNull(userSession, "sesión de usuario");
    }

    /** Crea la cuenta y su primera fila del historial de IMC. */
    public User register(RegistrationData data) throws NutriTrackException {
        ParameterChecks.requireNonNull(data, "datos de registro");
        ParameterChecks.requirePassword(data.getPassword());
        User user = new User(data.getName() == null ? null : data.getName().trim(),
                data.getEmail() == null ? null : data.getEmail().trim(),
                null, data.getWeightKg(), data.getHeightCm(), data.getBirthDate(), data.getSex());
        ParameterChecks.validateUser(user);
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BussinesRuleException("Ya existe un usuario registrado con el correo " + user.getEmail() + ".");
        }
        user.setPasswordHash(passwordHasher.hash(data.getPassword()));
        user.setRegisterDate(LocalDateTime.now());
        User saved = userRepository.save(user);

        BMIHistory firstRow = new BMIHistory(saved.getIdUser(), saved.getWeightKg(), saved.getHeightCm(),
                saved.getRegisterDate());
        try {
            bmiHistoryRepository.save(firstRow);
        } catch (PersistenceExceptions e) {
            userRepository.deleteById(saved.getIdUser());   // deshace la creación para no dejar la cuenta a medias
            throw e;
        }
        return saved;
    }

    public User login(String email, String password) throws NutriTrackException {
        String cleanEmail = ParameterChecks.requireText(email, "correo");
        ParameterChecks.requireNonNull(password, "contraseña");
        Optional<User> found = userRepository.findByEmail(cleanEmail);
        if (found.isEmpty() || !passwordHasher.verify(password, found.get().getPasswordHash())) {
            throw new AutenticationException(INVALID_CREDENTIALS);
        }
        userSession.start(found.get());
        return found.get();
    }

    public void logout() {
        userSession.close();
    }
}