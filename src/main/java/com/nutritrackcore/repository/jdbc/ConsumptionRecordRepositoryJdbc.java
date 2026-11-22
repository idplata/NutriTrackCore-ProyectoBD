package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link ConsumptionRecordRepository} sobre la tabla
 * {@code RegistroConsumo} (cabecera de cada comida).
 * <p>
 * {@link #save(ConsumptionRecord, List)} guarda cabecera y alimentos en una
 * sola transacción. Al borrar, el {@code ON DELETE CASCADE} elimina los
 * alimentos de {@code RegistroConsumo_Alimento}.
 */
public class ConsumptionRecordRepositoryJdbc extends JdbcRepository implements ConsumptionRecordRepository {

    private static final String COLUMNS = "id_registro, id_usuario, kcal_estimadas, nivel_estimacion, fecha_hora";

    private static final String INSERT = """
            INSERT INTO RegistroConsumo (id_usuario, kcal_estimadas, nivel_estimacion, fecha_hora)
            VALUES (?, ?, ?, COALESCE(?, now()))
            RETURNING id_registro, fecha_hora
            """;

    /** @param dataSource fuente de conexiones (el pool) */
    public ConsumptionRecordRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ConsumptionRecord save(ConsumptionRecord record) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            return insertHeader(con, record);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cabecera y alimentos van en una transacción: si un alimento falla (por
     * ejemplo, repetido o con gramos fuera de rango) se hace rollback y no
     * queda la cabecera sola.
     */
    @Override
    public ConsumptionRecord save(ConsumptionRecord record, List<ConsumptionRecordByFood> foods)
            throws PersistenceExceptions {
        if (foods == null || foods.isEmpty()) {
            throw new PersistenceExceptions("Un registro de consumo debe tener al menos un alimento");
        }
        return inTransaction(con -> {
            insertHeader(con, record);
            for (ConsumptionRecordByFood food : foods) {
                food.setRecordId(record.getRecordId());
                ConsumptionRecordFoodRepositoryJdbc.insert(con, food);
            }
            return record;
        });
    }

    /** Inserta la cabecera con la conexión dada y le asigna id y fecha. */
    private static ConsumptionRecord insertHeader(Connection con, ConsumptionRecord r) throws SQLException {
        return insertReturning(con, INSERT, ps -> {
            ps.setInt(1, r.getUserId());
            ps.setDouble(2, r.getEstimatedCalories());
            setNullableDouble(ps, 3, r.getEstimationLevel());
            setDateTime(ps, 4, r.getDateTime());
        }, rs -> {
            r.setRecordId(rs.getInt("id_registro"));
            r.setDateTime(getDateTime(rs, "fecha_hora"));
            return r;
        });
    }

    @Override
    public void update(ConsumptionRecord r) throws PersistenceExceptions {
        updateExisting("""
                UPDATE RegistroConsumo SET id_usuario = ?, kcal_estimadas = ?, nivel_estimacion = ?,
                                           fecha_hora = COALESCE(?, fecha_hora)
                WHERE id_registro = ?
                """, ps -> {
                    ps.setInt(1, r.getUserId());
                    ps.setDouble(2, r.getEstimatedCalories());
                    setNullableDouble(ps, 3, r.getEstimationLevel());
                    setDateTime(ps, 4, r.getDateTime());
                    ps.setInt(5, r.getRecordId());
                }, "No existe el registro con id " + r.getRecordId());
    }

    @Override
    public Optional<ConsumptionRecord> findById(int recordId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM RegistroConsumo WHERE id_registro = ?",
                ps -> ps.setInt(1, recordId), this::map);
    }

    /** {@inheritDoc} Ordenados por fecha y hora. */
    @Override
    public List<ConsumptionRecord> findByUserId(int userId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM RegistroConsumo WHERE id_usuario = ? ORDER BY fecha_hora, id_registro",
                ps -> ps.setInt(1, userId), this::map);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Filtra con un rango {@code [día, día + 1)} sobre {@code fecha_hora} en vez
     * de convertir la columna a fecha: así PostgreSQL puede usar el índice
     * {@code idx_consumo_usuario_fecha (id_usuario, fecha_hora)}.
     */
    @Override
    public List<ConsumptionRecord> findByUserIdAndDate(int userId, LocalDate date) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM RegistroConsumo"
                        + " WHERE id_usuario = ? AND fecha_hora >= ? AND fecha_hora < ?"
                        + " ORDER BY fecha_hora, id_registro",
                ps -> {
                    ps.setInt(1, userId);
                    setDateTime(ps, 2, date.atStartOfDay());
                    setDateTime(ps, 3, date.plusDays(1).atStartOfDay());
                }, this::map);
    }

    /** {@inheritDoc} La cascada de la base borra sus alimentos. */
    @Override
    public void deleteById(int recordId) throws PersistenceExceptions {
        update("DELETE FROM RegistroConsumo WHERE id_registro = ?", ps -> ps.setInt(1, recordId));
    }

    /** {@inheritDoc} La cascada de la base borra sus alimentos. */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM RegistroConsumo WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    /** Convierte una fila de {@code RegistroConsumo} en un {@link ConsumptionRecord}. */
    private ConsumptionRecord map(ResultSet rs) throws SQLException {
        ConsumptionRecord r = new ConsumptionRecord(rs.getInt("id_usuario"), rs.getDouble("kcal_estimadas"),
                getDateTime(rs, "fecha_hora"));
        r.setRecordId(rs.getInt("id_registro"));
        r.setEstimationLevel(getNullableDouble(rs, "nivel_estimacion"));
        return r;
    }
}
