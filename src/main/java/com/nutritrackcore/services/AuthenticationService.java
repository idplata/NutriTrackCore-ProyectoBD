package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.dto.RegistrationData;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Casos de uso de autenticación: registrar una cuenta, iniciar sesión y cerrar
 * sesión.
 * <p>
 * Depende únicamente de interfaces ({@link UserRepository},
 * {@link BMIHistoryRepository}, {@link PasswordHasher}) que recibe por
 * constructor, por lo que no sabe si los datos viven en archivos .txt o en una
 * base de datos.
 * <p>
 * Errores: los datos inválidos lanzan {@link IllegalArgumentException} y los
 * estados incorrectos (correo ya registrado) {@link IllegalStateException}; en
 * ambos casos el mensaje está en español. Los fallos de lectura o escritura
 * llegan como {@link PersistenceExceptions}.
 */
public class AuthenticationService {

    /** Mensaje único para credenciales inválidas: no revela si falló el correo o la contraseña. */
    private static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";

    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final PasswordHasher passwordHasher;
    private final UserSession userSession;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository       repositorio de usuarios
     * @param bmiHistoryRepository repositorio del historial de IMC
     * @param passwordHasher       calculador de hashes de contraseña
     * @param userSession          sesión compartida de la aplicación
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public AuthenticationService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                                 PasswordHasher passwordHasher, UserSession userSession) {
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.passwordHasher = ParameterChecks.requireNonNull(passwordHasher, "calculador de hash");
        this.userSession = ParameterChecks.requireNonNull(userSession, "sesión de usuario");
    }

    /**
     * Registra una cuenta nueva.
     * <p>
     * Pasos: (1) valida que los datos no sean nulos y que cada campo cumpla su
     * regla (nombre de hasta 50 caracteres, correo con formato válido, contraseña
     * de al menos 8 caracteres con letra y número, peso y altura dentro de los
     * límites, fecha de nacimiento no futura, sexo presente); (2) rechaza el
     * correo si ya pertenece a otro usuario; (3) construye el {@link User} con la
     * contraseña convertida en hash y la fecha de registro actual y lo guarda
     * (el repositorio le asigna el id); (4) crea la primera fila del historial
     * de IMC con el peso y la altura iniciales. Si el paso 4 falla, deshace el
     * paso 3 borrando el usuario recién creado para no dejar una cuenta sin
     * historial.
     *
     * @param data datos escritos en el formulario de registro
     * @return el usuario guardado, ya con su id
     * @throws IllegalArgumentException si algún dato es nulo o inválido
     * @throws IllegalStateException    si el correo ya está registrado
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public User register(RegistrationData data) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(data, "datos de registro");
        String name = ParameterChecks.requireTextMax(data.getName(), ParameterChecks.MAX_USER_TEXT_LENGTH, "nombre");
        String email = ParameterChecks.requireEmail(data.getEmail());
        ParameterChecks.requirePassword(data.getPassword());
        ParameterChecks.requireMeasurements(data.getWeightKg(), data.getHeightCm());
        ParameterChecks.requireBirthDate(data.getBirthDate());
        ParameterChecks.requireNonNull(data.getSex(), "sexo");

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Ya existe un usuario registrado con el correo " + email + ".");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(data.getPassword()));
        user.setWeightKg(data.getWeightKg());
        user.setHeightCm(data.getHeightCm());
        user.setBirthDate(data.getBirthDate());
        user.setSex(data.getSex());
        user.setRegisterDate(LocalDateTime.now());
        User saved = userRepository.save(user);

        BMIHistory firstRow = new BMIHistory();
        firstRow.setIdUser(saved.getIdUser());
        firstRow.setWeightKg(saved.getWeightKg());
        firstRow.setHeightCm(saved.getHeightCm());
        firstRow.setRegisterDate(saved.getRegisterDate());
        try {
            bmiHistoryRepository.save(firstRow);
        } catch (PersistenceExceptions e) {
            userRepository.deleteById(saved.getIdUser());   // deshace la creación para no dejar la cuenta a medias
            throw e;
        }
        return saved;
    }

    /**
     * Inicia sesión con correo y contraseña.
     * <p>
     * Pasos: (1) valida que ninguno sea nulo ni, en el caso del correo, vacío;
     * (2) busca el usuario por correo; (3) verifica la contraseña contra el hash
     * guardado; (4) si todo coincide, abre la sesión con ese usuario. Tanto si el
     * correo no existe como si la contraseña es incorrecta se lanza el mismo
     * mensaje, para no revelar cuáles correos están registrados.
     *
     * @param email    correo del usuario
     * @param password contraseña en texto plano
     * @return el usuario autenticado
     * @throws IllegalArgumentException si algún dato es nulo/vacío o las credenciales son incorrectas
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public User login(String email, String password) throws PersistenceExceptions {
        String cleanEmail = ParameterChecks.requireText(email, "correo");
        ParameterChecks.requireNonNull(password, "contraseña");

        Optional<User> found = userRepository.findByEmail(cleanEmail);
        if (found.isEmpty() || !passwordHasher.verify(password, found.get().getPasswordHash())) {
            throw new IllegalArgumentException(INVALID_CREDENTIALS);
        }
        userSession.start(found.get());
        return found.get();
    }

    /**
     * Cierra la sesión actual. No falla si no había ninguna abierta.
     */
    public void logout() {
        userSession.close();
    }
}