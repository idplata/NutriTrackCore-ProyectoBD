package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.repository.BMIHistoryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link BMIHistoryRepository} sobre la tabla
 * {@code Historial_IMC}. Si la fecha de registro es nula se usa {@code now()}.
 * <p>
 * Ojo al borrar: {@code RutinaEjercicio.id_historial_base} apunta a esta tabla
 * sin cascada, así que primero hay que borrar las rutinas del usuario (como ya
 * hace {@code AccountDeletionService}).
 */
public class BMIHistoryRepositoryJdbc extends JdbcRepository implements BMIHistoryRepository {

    private static final String COLUMNS = "id_historial, id_usuario, peso_kg, altura_cm, fecha_registro";

    /** @param dataSource fuente de conexiones (el pool) */
    public BMIHistoryRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public BMIHistory save(BMIHistory h) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO Historial_IMC (id_usuario, peso_kg, altura_cm, fecha_registro)
                VALUES (?, ?, ?, COALESCE(?, now()))
                RETURNING id_historial, fecha_registro
                """, ps -> {
                    ps.setInt(1, h.getIdUser());
                    ps.setDouble(2, h.getWeightKg());
                    ps.setDouble(3, h.getHeightCm());
                    setDateTime(ps, 4, h.getRegisterDate());
                }, rs -> {
                    h.setIdHistory(rs.getInt("id_historial"));
                    h.setRegisterDate(getDateTime(rs, "fecha_registro"));
                    return h;
                });
    }

    @Override
    public Optional<BMIHistory> findById(int idHistory) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Historial_IMC WHERE id_historial = ?",
                ps -> ps.setInt(1, idHistory), this::map);
    }

    /** {@inheritDoc} Ordenadas de la más antigua a la más reciente. */
    @Override
    public List<BMIHistory> findByUserId(int idUser) throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Historial_IMC WHERE id_usuario = ?"
                        + " ORDER BY fecha_registro, id_historial",
                ps -> ps.setInt(1, idUser), this::map);
    }

    @Override
    public Optional<BMIHistory> findLatestByUserId(int idUser) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Historial_IMC WHERE id_usuario = ?"
                        + " ORDER BY fecha_registro DESC, id_historial DESC LIMIT 1",
                ps -> ps.setInt(1, idUser), this::map);
    }

    @Override
    public void deleteByUserId(int idUser) throws PersistenceExceptions {
        update("DELETE FROM Historial_IMC WHERE id_usuario = ?", ps -> ps.setInt(1, idUser));
    }

    /** Convierte una fila de {@code Historial_IMC} en un {@link BMIHistory}. */
    private BMIHistory map(ResultSet rs) throws SQLException {
        BMIHistory h = new BMIHistory(rs.getInt("id_usuario"), rs.getDouble("peso_kg"),
                rs.getDouble("altura_cm"), getDateTime(rs, "fecha_registro"));
        h.setIdHistory(rs.getInt("id_historial"));
        return h;
    }
}
