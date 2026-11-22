package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Base común de los repositorios JDBC.
 * <p>
 * Resuelve lo que todos repiten: pedir una conexión al pool, preparar la
 * sentencia con parámetros ({@code ?}, nunca concatenando texto, para evitar
 * inyección SQL), recorrer el {@link ResultSet}, cerrar todo con
 * try-with-resources y convertir cada {@link SQLException} en
 * {@link PersistenceExceptions}, que es lo único que conocen los servicios.
 * <p>
 * También ofrece {@link #inTransaction(TransactionWork)} para las operaciones
 * que escriben en varias tablas y deben guardarse completas o no guardarse.
 */
public abstract class JdbcRepository {

    /** Fuente de conexiones (el pool). */
    protected final DataSource dataSource;

    /**
     * @param dataSource fuente de conexiones; normalmente el {@link ConnectionPool}
     */
    protected JdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ---------- interfaces funcionales ----------

    /** Asigna los parámetros de una sentencia preparada. */
    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    /** Convierte la fila actual de un {@link ResultSet} en un objeto. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Trabajo que se ejecuta dentro de una transacción con la conexión recibida. */
    @FunctionalInterface
    protected interface TransactionWork<T> {
        T run(Connection con) throws SQLException, PersistenceExceptions;
    }

    /** Sin parámetros. */
    protected static final Binder NO_PARAMS = ps -> { };

    // ---------- operaciones con su propia conexión ----------

    /**
     * Ejecuta una consulta y convierte cada fila.
     *
     * @return lista con una entrada por fila, en el orden del {@code ORDER BY}
     * @throws PersistenceExceptions si falla la consulta
     */
    protected <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            return query(con, sql, binder, mapper);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * Ejecuta una consulta que devuelve como máximo una fila.
     *
     * @return la fila convertida, o {@link Optional#empty()} si no hay filas
     * @throws PersistenceExceptions si falla la consulta
     */
    protected <T> Optional<T> queryOne(String sql, Binder binder, RowMapper<T> mapper) throws PersistenceExceptions {
        List<T> rows = query(sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /**
     * Ejecuta un {@code INSERT}, {@code UPDATE} o {@code DELETE}.
     *
     * @return cantidad de filas afectadas
     * @throws PersistenceExceptions si falla la sentencia (por ejemplo, una restricción)
     */
    protected int update(String sql, Binder binder) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            return update(con, sql, binder);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * Ejecuta un {@code UPDATE} que debe afectar exactamente una fila.
     *
     * @param notFoundMessage mensaje si no se actualizó ninguna fila
     * @throws PersistenceExceptions si la fila no existe o falla la sentencia
     */
    protected void updateExisting(String sql, Binder binder, String notFoundMessage) throws PersistenceExceptions {
        if (update(sql, binder) == 0) {
            throw new PersistenceExceptions(notFoundMessage);
        }
    }

    /**
     * Ejecuta un {@code INSERT ... RETURNING} y convierte la fila devuelta
     * (normalmente el id generado y los valores por defecto).
     *
     * @throws PersistenceExceptions si falla la inserción
     */
    protected <T> T insertReturning(String sql, Binder binder, RowMapper<T> mapper) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            return insertReturning(con, sql, binder, mapper);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * Ejecuta varias sentencias como una sola transacción: si todo sale bien
     * hace {@code commit}; si algo falla hace {@code rollback} y no queda nada
     * guardado. La conexión vuelve al pool con {@code autoCommit} restaurado.
     *
     * @param work trabajo a ejecutar con la conexión de la transacción
     * @return lo que devuelva el trabajo
     * @throws PersistenceExceptions si algo falla (ya se hizo rollback)
     */
    protected <T> T inTransaction(TransactionWork<T> work) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            boolean previous = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | PersistenceExceptions | RuntimeException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                if (e instanceof SQLException sql) {
                    throw translate(sql);
                }
                if (e instanceof PersistenceExceptions pe) {
                    throw pe;
                }
                throw (RuntimeException) e;
            } finally {
                con.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    // ---------- operaciones sobre una conexión ya abierta (para transacciones) ----------

    /** Igual que {@link #query(String, Binder, RowMapper)} pero con una conexión dada. */
    protected static <T> List<T> query(Connection con, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }

    /** Igual que {@link #update(String, Binder)} pero con una conexión dada. */
    protected static int update(Connection con, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    /** Igual que {@link #insertReturning(String, Binder, RowMapper)} pero con una conexión dada. */
    protected static <T> T insertReturning(Connection con, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        List<T> rows = query(con, sql, binder, mapper);
        if (rows.isEmpty()) {
            throw new SQLException("El INSERT no devolvió ninguna fila: " + sql);
        }
        return rows.get(0);
    }

    // ---------- valores que pueden ser nulos ----------

    /** Asigna un {@code Double} o {@code NULL}. */
    protected static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setDouble(index, value);
        }
    }

    /** Asigna un {@code Integer} o {@code NULL}. */
    protected static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /** Asigna un {@code BigDecimal} o {@code NULL}. */
    protected static void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, value);
        }
    }

    /** Asigna una fecha o {@code NULL} (el driver de PostgreSQL acepta {@code java.time}). */
    protected static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setObject(index, value);
        }
    }

    /** Asigna una fecha y hora o {@code NULL}. */
    protected static void setDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setObject(index, value);
        }
    }

    /** @return el {@code Double} de la columna, o {@code null} si es {@code NULL} */
    protected static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    /** @return el {@code Integer} de la columna, o {@code null} si es {@code NULL} */
    protected static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** @return la fecha de la columna, o {@code null} */
    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    /** @return la fecha y hora de la columna, o {@code null} */
    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    // ---------- errores ----------

    /**
     * Convierte un error de la base de datos en {@link PersistenceExceptions}
     * con un mensaje entendible. Usa el SQLState estándar para reconocer las
     * restricciones que rechazó el servidor (el mensaje original, que nombra la
     * restricción, se conserva al final).
     *
     * @param e error de JDBC
     * @return la excepción de persistencia equivalente
     */
    protected static PersistenceExceptions translate(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String kind = switch (state) {
            case "23505" -> "Ya existe un registro con ese valor (clave o dato único repetido)";
            case "23503" -> "El dato referencia a otro que no existe, o está en uso y no se puede borrar";
            case "23514" -> "Un valor no cumple las reglas de la base de datos";
            case "23502" -> "Falta un dato obligatorio";
            case "22003" -> "Un número está fuera del rango permitido";
            default -> state.startsWith("08")
                    ? "Se perdió la conexión con la base de datos"
                    : "Error en la base de datos";
        };
        return new PersistenceExceptions(kind + ": " + e.getMessage(), e);
    }
}
