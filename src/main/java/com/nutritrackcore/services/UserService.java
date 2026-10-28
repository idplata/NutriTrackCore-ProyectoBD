package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Casos de uso sobre el perfil de un usuario ya registrado: consultarlo,
 * cambiar sus datos, actualizar peso y altura y cambiar la contraseña.
 * <p>
 * Regla importante del diccionario de datos: el peso y la altura que guarda
 * {@code User} son los <b>actuales</b>, y cada cambio de medidas deja además una
 * fila nueva en el historial de IMC.
 */
public class UserService {

    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final PasswordHasher passwordHasher;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository       repositorio de usuarios
     * @param bmiHistoryRepository repositorio del historial de IMC
     * @param passwordHasher       calculador de hashes de contraseña
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public UserService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                       PasswordHasher passwordHasher) {
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.passwordHasher = ParameterChecks.requireNonNull(passwordHasher, "calculador de hash");
    }

    /**
     * Obtiene un usuario por su id.
     * <p>
     * Valida el id y consulta el repositorio; si no hay resultado, lanza un error
     * en lugar de devolver {@code null}.
     *
     * @param userId identificador del usuario
     * @return el usuario encontrado
     * @throws IllegalArgumentException si el id es inválido o no existe el usuario
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public User getById(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("No existe un usuario con el identificador " + userId + "."));
    }

    /**
     * Cambia el nombre y el correo de un usuario.
     * <p>
     * Pasos: (1) valida id, nombre (máximo 50 caracteres) y correo (formato
     * válido); (2) carga el usuario; (3) si otro usuario distinto ya usa ese
     * correo, rechaza el cambio; (4) actualiza los dos campos y guarda.
     *
     * @param userId identificador del usuario a modificar
     * @param name   nombre nuevo
     * @param email  correo nuevo
     * @throws IllegalArgumentException si algún dato es nulo/inválido o el usuario no existe
     * @throws IllegalStateException    si el correo ya pertenece a otro usuario
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void updateProfile(int userId, String name, String email) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        String cleanName = ParameterChecks.requireTextMax(name, ParameterChecks.MAX_USER_TEXT_LENGTH, "nombre");
        String cleanEmail = ParameterChecks.requireEmail(email);

        User user = getById(userId);
        Optional<User> owner = userRepository.findByEmail(cleanEmail);
        if (owner.isPresent() && owner.get().getIdUser() != userId) {
            throw new IllegalStateException("El correo " + cleanEmail + " ya pertenece a otro usuario.");
        }
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        userRepository.update(user);
    }

    /**
     * Actualiza el peso y la altura actuales del usuario y registra el cambio en
     * el historial de IMC.
     * <p>
     * Pasos: (1) valida id y que peso y altura estén dentro de los límites del
     * sistema; (2) carga el usuario y recuerda sus medidas anteriores; (3) guarda
     * las medidas nuevas en {@code User}; (4) agrega una fila a
     * {@code BMIHistory} con la fecha y hora actuales. Si el paso 4 falla,
     * restaura las medidas anteriores para que {@code User} y el historial no
     * queden desincronizados.
     *
     * @param userId   identificador del usuario
     * @param weightKg peso nuevo en kilogramos
     * @param heightCm altura nueva en centímetros
     * @throws IllegalArgumentException si el id o las medidas son inválidos o el usuario no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void updateMeasurements(int userId, double weightKg, double heightCm) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireMeasurements(weightKg, heightCm);

        User user = getById(userId);
        double previousWeight = user.getWeightKg();
        double previousHeight = user.getHeightCm();

        user.setWeightKg(weightKg);
        user.setHeightCm(heightCm);
        userRepository.update(user);

        BMIHistory row = new BMIHistory();
        row.setIdUser(userId);
        row.setWeightKg(weightKg);
        row.setHeightCm(heightCm);
        row.setRegisterDate(LocalDateTime.now());
        try {
            bmiHistoryRepository.save(row);
        } catch (PersistenceExceptions e) {
            user.setWeightKg(previousWeight);       // deshace el cambio de medidas
            user.setHeightCm(previousHeight);
            userRepository.update(user);
            throw e;
        }
    }

    /**
     * Cambia la contraseña de un usuario.
     * <p>
     * Pasos: (1) valida id, contraseña actual (no nula) y que la nueva cumpla la
     * política (8 caracteres como mínimo, con letra y número); (2) carga el
     * usuario; (3) verifica la contraseña actual contra el hash guardado; (4)
     * rechaza la nueva si es igual a la actual; (5) guarda el hash de la nueva.
     *
     * @param userId          identificador del usuario
     * @param currentPassword contraseña actual en texto plano
     * @param newPassword     contraseña nueva en texto plano
     * @throws IllegalArgumentException si algún dato es nulo/inválido, la contraseña actual es incorrecta
     *                                  o la nueva es igual a la actual
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void changePassword(int userId, String currentPassword, String newPassword) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNonNull(currentPassword, "contraseña actual");
        ParameterChecks.requirePassword(newPassword);

        User user = getById(userId);
        if (!passwordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("La contraseña nueva debe ser distinta de la actual.");
        }
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.update(user);
    }
}