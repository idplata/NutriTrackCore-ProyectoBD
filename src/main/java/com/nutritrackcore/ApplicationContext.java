package com.nutritrackcore;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.repository.txt.BMIHistoryRepositoryTxt;
import com.nutritrackcore.repository.txt.TxtFile;
import com.nutritrackcore.repository.txt.UserRepositoryTxt;
import com.nutritrackcore.services.AuthenticationService;
import com.nutritrackcore.services.PasswordHasher;
import com.nutritrackcore.services.Sha256PasswordHasher;
import com.nutritrackcore.services.UserSession;
import com.nutritrackcore.ui.MainWindow;
import java.nio.file.Paths;

/**
 * Raíz de composición: el único lugar donde se crean las implementaciones
 * concretas (repositorios .txt, cifrador, servicios) y se conectan entre sí.
 * <p>
 * Para pasar a base de datos solo se cambian aquí los {@code ...RepositoryTxt}
 * por sus versiones JDBC; nada más del proyecto se entera.
 * <p>
 * Por ahora arma solo lo que necesita el inicio de sesión. Se irá completando
 * a medida que se agreguen las demás pantallas.
 */
public class ApplicationContext {

    private final UserSession userSession;
    private final AuthenticationService authenticationService;

    /**
     * Crea todas las piezas de la aplicación.
     *
     * @param dataFolder carpeta donde viven los archivos .txt (se crea si no existe)
     * @throws PersistenceExceptions si no se pueden crear o abrir los archivos de datos
     */
    public ApplicationContext(String dataFolder) throws PersistenceExceptions {
        // 1. Repositorios
        UserRepository userRepository = new UserRepositoryTxt(
                new TxtFile(file(dataFolder, "users.txt"), UserRepositoryTxt.HEADER));
        BMIHistoryRepository bmiHistoryRepository = new BMIHistoryRepositoryTxt(
                new TxtFile(file(dataFolder, "bmi_records.txt"), BMIHistoryRepositoryTxt.HEADER));

        // 2. Seguridad y sesión
        PasswordHasher passwordHasher = new Sha256PasswordHasher();
        this.userSession = new UserSession();

        // 3. Servicios
        this.authenticationService = new AuthenticationService(
                userRepository, bmiHistoryRepository, passwordHasher, userSession);
    }

    /**
     * Crea la ventana principal con los servicios ya conectados.
     *
     * @return ventana lista para mostrarse
     */
    public MainWindow createMainWindow() {
        return new MainWindow(authenticationService);
    }

    /** @return la sesión compartida por servicios y pantallas */
    public UserSession getUserSession() {
        return userSession;
    }

    /** @return el servicio de registro e inicio de sesión */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    private static String file(String folder, String name) {
        return Paths.get(folder, name).toString();
    }
}
