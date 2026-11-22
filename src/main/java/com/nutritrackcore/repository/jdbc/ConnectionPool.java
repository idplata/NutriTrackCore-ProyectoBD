package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * Pool de conexiones a PostgreSQL con HikariCP.
 * <p>
 * Abrir una conexión nueva en cada consulta es lento (red, autenticación,
 * sesión). El pool abre unas pocas al iniciar, las presta cuando un repositorio
 * las pide con {@code getConnection()} y las recupera cuando el repositorio hace
 * {@code close()}: la conexión no se cierra, vuelve al pool para reutilizarse.
 * <p>
 * Debe existir <b>una sola instancia</b> para toda la aplicación (la crea el
 * {@code ApplicationContext}) y hay que cerrarla al salir con {@link #close()}.
 * Los repositorios no conocen esta clase: reciben el {@link DataSource} que
 * devuelve {@link #getDataSource()}.
 */
public final class ConnectionPool implements AutoCloseable {

    /** Pool de HikariCP; también es el {@link DataSource} que reciben los repositorios. */
    private final HikariDataSource dataSource;

    /**
     * Crea el pool y abre las primeras conexiones. Si la base no responde o las
     * credenciales son incorrectas, falla aquí, al arrancar, y no a mitad de uso.
     *
     * @param config datos de conexión y tamaño del pool
     * @throws PersistenceExceptions si no se puede conectar a la base de datos
     */
    public ConnectionPool(DatabaseConfig config) throws PersistenceExceptions {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("NutriTrackPool");
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        hikari.setMinimumIdle(config.minIdle());
        hikari.setConnectionTimeout(config.connectionTimeout());
        hikari.setAutoCommit(true);                          // las transacciones las abre cada repositorio
        try {
            this.dataSource = new HikariDataSource(hikari);
        } catch (RuntimeException e) {                       // HikariPool.PoolInitializationException
            throw new PersistenceExceptions("No se pudo conectar a la base de datos ("
                    + config.url() + "): " + e.getMessage(), e);
        }
    }

    /**
     * @return el {@link DataSource} que se les entrega a los repositorios JDBC
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Cierra todas las conexiones del pool. Se llama una vez, al salir de la
     * aplicación. Llamarlo más de una vez no tiene efecto.
     */
    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
