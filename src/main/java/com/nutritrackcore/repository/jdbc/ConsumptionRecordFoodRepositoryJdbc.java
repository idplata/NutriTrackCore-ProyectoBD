package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link ConsumptionRecordFoodRepository} sobre la tabla
 * {@code RegistroConsumo_Alimento}. El {@code unique (id_registro, id_alimento)}
 * rechaza un alimento repetido en el mismo registro y el {@code check} exige
 * gramos entre 0 y 2000.
 */
public class ConsumptionRecordFoodRepositoryJdbc extends JdbcRepository implements ConsumptionRecordFoodRepository {

    private static final String COLUMNS = "id_registro_alimento, id_registro, id_alimento, gramos_estimados";

    private static final String INSERT = """
            INSERT INTO RegistroConsumo_Alimento (id_registro, id_alimento, gramos_estimados)
            VALUES (?, ?, ?)
            RETURNING id_registro_alimento
            """;

    /** @param dataSource fuente de conexiones (el pool) */
    public ConsumptionRecordFoodRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ConsumptionRecordByFood save(ConsumptionRecordByFood line) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            return insert(con, line);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * Inserta una línea con una conexión dada. Lo usa también
     * {@link ConsumptionRecordRepositoryJdbc} dentro de su transacción.
     *
     * @param con  conexión (puede estar en una transacción)
     * @param line línea a insertar; recibe el id generado
     * @return la misma línea con el id asignado
     * @throws SQLException si la base rechaza la fila
     */
    static ConsumptionRecordByFood insert(Connection con, ConsumptionRecordByFood line) throws SQLException {
        return insertReturning(con, INSERT, ps -> {
            ps.setInt(1, line.getRecordId());
            ps.setInt(2, line.getFoodId());
            ps.setDouble(3, line.getEstimatedGrams());
        }, rs -> {
            line.setConsumptionRecordByFoodId(rs.getInt("id_registro_alimento"));
            return line;
        });
    }

    @Override
    public void update(ConsumptionRecordByFood line) throws PersistenceExceptions {
        updateExisting("""
                UPDATE RegistroConsumo_Alimento SET id_registro = ?, id_alimento = ?, gramos_estimados = ?
                WHERE id_registro_alimento = ?
                """, ps -> {
                    ps.setInt(1, line.getRecordId());
                    ps.setInt(2, line.getFoodId());
                    ps.setDouble(3, line.getEstimatedGrams());
                    ps.setInt(4, line.getconsumptionRecordByFoodId());
                }, "No existe la línea de consumo con id " + line.getconsumptionRecordByFoodId());
    }

    @Override
    public Optional<ConsumptionRecordByFood> findById(int id) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM RegistroConsumo_Alimento WHERE id_registro_alimento = ?",
                ps -> ps.setInt(1, id), ConsumptionRecordFoodRepositoryJdbc::map);
    }

    @Override
    public List<ConsumptionRecordByFood> findByRecordId(int recordId) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM RegistroConsumo_Alimento WHERE id_registro = ?"
                        + " ORDER BY id_registro_alimento",
                ps -> ps.setInt(1, recordId), ConsumptionRecordFoodRepositoryJdbc::map);
    }

    @Override
    public void deleteById(int id) throws PersistenceExceptions {
        update("DELETE FROM RegistroConsumo_Alimento WHERE id_registro_alimento = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public void deleteByRecordId(int recordId) throws PersistenceExceptions {
        update("DELETE FROM RegistroConsumo_Alimento WHERE id_registro = ?", ps -> ps.setInt(1, recordId));
    }

    /** Convierte una fila de {@code RegistroConsumo_Alimento} en un {@link ConsumptionRecordByFood}. */
    private static ConsumptionRecordByFood map(ResultSet rs) throws SQLException {
        ConsumptionRecordByFood l = new ConsumptionRecordByFood();
        l.setConsumptionRecordByFoodId(rs.getInt("id_registro_alimento"));
        l.setRecordId(rs.getInt("id_registro"));
        l.setFoodId(rs.getInt("id_alimento"));
        l.setEstimatedGrams(rs.getDouble("gramos_estimados"));
        return l;
    }
}
