package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración de la conexión a PostgreSQL, leída de {@code db.properties}.
 * <p>
 * Se busca primero el archivo {@code db.properties} en la carpeta desde donde
 * se ejecuta la aplicación y, si no está, dentro del classpath. Así las
 * credenciales no quedan escritas en el código. Claves reconocidas:
 * <ul>
 *   <li>{@code persistencia}: {@code jdbc} o {@code txt} (por defecto {@code txt}).</li>
 *   <li>{@code db.url}, {@code db.usuario}, {@code db.contrasena}: datos de conexión.</li>
 *   <li>{@code db.pool.maximo} (10), {@code db.pool.minimo} (2) y
 *       {@code db.pool.timeout.ms} (30000): tamaño y espera del pool.</li>
 * </ul>
 *
 * @param useJdbc           {@code true} si la aplicación debe usar la base de datos
 * @param url               URL JDBC, por ejemplo {@code jdbc:postgresql://localhost:5432/nutritrack}
 * @param user              usuario de la base de datos
 * @param password          contraseña del usuario
 * @param maxPoolSize       máximo de conexiones abiertas a la vez
 * @param minIdle           conexiones que el pool mantiene listas sin usar
 * @param connectionTimeout milisegundos que se espera por una conexión libre
 */
public record DatabaseConfig(boolean useJdbc, String url, String user, String password,
                             int maxPoolSize, int minIdle, long connectionTimeout) {

    /** Nombre del archivo de configuración. */
    public static final String FILE_NAME = "db.properties";

    /**
     * Carga la configuración. Si no existe {@code db.properties} en ningún
     * lado, devuelve una configuración que usa los archivos .txt.
     *
     * @return la configuración leída
     * @throws PersistenceExceptions si el archivo existe pero no se puede leer,
     *                               o si pide JDBC y le falta la URL o el usuario
     */
    public static DatabaseConfig load() throws PersistenceExceptions {
        Properties p = new Properties();
        Path local = Path.of(FILE_NAME);
        try {
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) {
                    p.load(in);
                }
            } else {
                try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
                    if (in == null) {
                        return txtOnly();                    // sin archivo: se sigue con .txt
                    }
                    p.load(in);
                }
            }
        } catch (IOException e) {
            throw new PersistenceExceptions("No se pudo leer " + FILE_NAME + ": " + e.getMessage(), e);
        }
        return fromProperties(p);
    }

    /**
     * Arma la configuración a partir de propiedades ya cargadas.
     *
     * @param p propiedades con las claves descritas en {@link DatabaseConfig}
     * @return la configuración
     * @throws PersistenceExceptions si pide JDBC y falta la URL o el usuario, o un número es inválido
     */
    public static DatabaseConfig fromProperties(Properties p) throws PersistenceExceptions {
        boolean jdbc = "jdbc".equalsIgnoreCase(p.getProperty("persistencia", "txt").trim());
        String url = p.getProperty("db.url", "").trim();
        String user = p.getProperty("db.usuario", "").trim();
        if (jdbc && (url.isEmpty() || user.isEmpty())) {
            throw new PersistenceExceptions(FILE_NAME + " pide persistencia=jdbc pero falta db.url o db.usuario");
        }
        try {
            return new DatabaseConfig(jdbc, url, user, p.getProperty("db.contrasena", ""),
                    Integer.parseInt(p.getProperty("db.pool.maximo", "10").trim()),
                    Integer.parseInt(p.getProperty("db.pool.minimo", "2").trim()),
                    Long.parseLong(p.getProperty("db.pool.timeout.ms", "30000").trim()));
        } catch (NumberFormatException e) {
            throw new PersistenceExceptions("Valor numérico inválido en " + FILE_NAME + ": " + e.getMessage(), e);
        }
    }

    /** @return una configuración que no usa base de datos */
    private static DatabaseConfig txtOnly() {
        return new DatabaseConfig(false, "", "", "", 10, 2, 30000);
    }

    /** No muestra la contraseña si la configuración se imprime en un log. */
    @Override
    public String toString() {
        return "DatabaseConfig[useJdbc=" + useJdbc + ", url=" + url + ", user=" + user
                + ", maxPoolSize=" + maxPoolSize + ", minIdle=" + minIdle + "]";
    }
}
