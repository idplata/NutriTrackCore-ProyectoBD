package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.repository.DietPlanRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link DietPlanRepository} sobre {@code PlanDieta} y
 * {@code PlanDieta_Alimento}.
 * <p>
 * El plan y sus líneas se guardan y actualizan en una sola transacción. El
 * {@link MealTime} de cada línea se traduce a {@code id_tipo_comida} buscando
 * en {@code TipoComida} la fila con su mismo {@code orden_dia}, y al leer se
 * hace el camino inverso con un {@code JOIN}.
 */
public class DietPlanRepositoryJdbc extends JdbcRepository implements DietPlanRepository {

    private static final String COLUMNS = "id_plan, id_usuario, fecha_generacion, kcal_objetivo, tipo_dieta, activo";

    /** @param dataSource fuente de conexiones (el pool) */
    public DietPlanRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    /** {@inheritDoc} Si la fecha es nula se usa {@code current_date}; si el tipo es nulo, {@code OMNIVORA}. */
    @Override
    public DietPlan save(DietPlan plan) throws PersistenceExceptions {
        return inTransaction(con -> {
            insertReturning(con, """
                    INSERT INTO PlanDieta (id_usuario, fecha_generacion, kcal_objetivo, tipo_dieta, activo)
                    VALUES (?, COALESCE(?, current_date), ?, COALESCE(?, 'OMNIVORA'), ?)
                    RETURNING id_plan, fecha_generacion, tipo_dieta
                    """, ps -> {
                        ps.setInt(1, plan.getUserId());
                        setDate(ps, 2, plan.getGenerationDate());
                        ps.setDouble(3, plan.getTargetCalories());
                        ps.setString(4, plan.getDietType() == null ? null : plan.getDietType().name());
                        ps.setBoolean(5, plan.isActive());
                    }, rs -> {
                        plan.setPlanId(rs.getInt("id_plan"));
                        plan.setGenerationDate(getDate(rs, "fecha_generacion"));
                        plan.setDietType(DietPlan.DietType.valueOf(rs.getString("tipo_dieta")));
                        return plan;
                    });
            insertLines(con, plan);
            return plan;
        });
    }

    /** {@inheritDoc} Reemplaza las líneas: borra las anteriores e inserta las actuales. */
    @Override
    public void update(DietPlan plan) throws PersistenceExceptions {
        inTransaction(con -> {
            int rows = update(con, """
                    UPDATE PlanDieta SET id_usuario = ?, fecha_generacion = COALESCE(?, fecha_generacion),
                           kcal_objetivo = ?, tipo_dieta = COALESCE(?, tipo_dieta), activo = ?
                    WHERE id_plan = ?
                    """, ps -> {
                        ps.setInt(1, plan.getUserId());
                        setDate(ps, 2, plan.getGenerationDate());
                        ps.setDouble(3, plan.getTargetCalories());
                        ps.setString(4, plan.getDietType() == null ? null : plan.getDietType().name());
                        ps.setBoolean(5, plan.isActive());
                        ps.setInt(6, plan.getPlanId());
                    });
            if (rows == 0) {
                throw new PersistenceExceptions("No existe el plan de dieta con id " + plan.getPlanId());
            }
            update(con, "DELETE FROM PlanDieta_Alimento WHERE id_plan = ?", ps -> ps.setInt(1, plan.getPlanId()));
            insertLines(con, plan);
            return null;
        });
    }

    @Override
    public Optional<DietPlan> findById(int planId) throws PersistenceExceptions {
        List<DietPlan> plans = loadPlans("SELECT " + COLUMNS + " FROM PlanDieta WHERE id_plan = ?", planId);
        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(0));
    }

    /** {@inheritDoc} Ordenados por fecha de generación. */
    @Override
    public List<DietPlan> findByUserId(int userId) throws PersistenceExceptions {
        return loadPlans("SELECT " + COLUMNS + " FROM PlanDieta WHERE id_usuario = ?"
                + " ORDER BY fecha_generacion, id_plan", userId);
    }

    /** {@inheritDoc} Si hay varios activos, devuelve el más reciente (igual que en .txt). */
    @Override
    public Optional<DietPlan> findActiveByUserId(int userId) throws PersistenceExceptions {
        List<DietPlan> plans = loadPlans("SELECT " + COLUMNS + " FROM PlanDieta WHERE id_usuario = ? AND activo"
                + " ORDER BY fecha_generacion DESC, id_plan DESC LIMIT 1", userId);
        return plans.isEmpty() ? Optional.empty() : Optional.of(plans.get(0));
    }

    /** {@inheritDoc} La cascada borra las líneas; las rutinas que apuntaban al plan quedan con {@code NULL}. */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        update("DELETE FROM PlanDieta WHERE id_usuario = ?", ps -> ps.setInt(1, userId));
    }

    // ---------- apoyo ----------

    /**
     * Ejecuta la consulta de planes (con un único parámetro entero) y carga las
     * líneas de cada uno, todo con la misma conexión.
     */
    private List<DietPlan> loadPlans(String sql, int param) throws PersistenceExceptions {
        try (Connection con = dataSource.getConnection()) {
            List<DietPlan> plans = query(con, sql, ps -> ps.setInt(1, param), DietPlanRepositoryJdbc::mapPlan);
            for (DietPlan p : plans) {
                p.setLines(query(con, """
                        SELECT pda.id_plan, pda.id_alimento, pda.cantidad_g, tc.orden_dia
                        FROM PlanDieta_Alimento pda
                        JOIN TipoComida tc ON tc.id_tipo_comida = pda.id_tipo_comida
                        WHERE pda.id_plan = ?
                        ORDER BY tc.orden_dia, pda.id_alimento
                        """, ps -> ps.setInt(1, p.getPlanId()), DietPlanRepositoryJdbc::mapLine));
            }
            return plans;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * Inserta las líneas del plan. El {@code id_tipo_comida} se obtiene de
     * {@code TipoComida} por el {@code orden_dia} del {@link MealTime}.
     */
    private static void insertLines(Connection con, DietPlan plan) throws SQLException, PersistenceExceptions {
        if (plan.getLines() == null) {
            return;
        }
        for (DietPlanFood line : plan.getLines()) {
            if (line.getMealTime() == null) {
                throw new PersistenceExceptions("La línea del alimento " + line.getFoodId() + " no tiene tipo de comida");
            }
            line.setPlanId(plan.getPlanId());
            int rows = update(con, """
                    INSERT INTO PlanDieta_Alimento (id_plan, id_alimento, id_tipo_comida, cantidad_g)
                    SELECT ?, ?, tc.id_tipo_comida, ? FROM TipoComida tc WHERE tc.orden_dia = ?
                    """, ps -> {
                        ps.setInt(1, plan.getPlanId());
                        ps.setInt(2, line.getFoodId());
                        ps.setDouble(3, line.getQuantityG());
                        ps.setInt(4, line.getMealTime().getDayOrder());
                    });
            if (rows == 0) {
                throw new PersistenceExceptions("TipoComida no tiene una fila con orden_dia "
                        + line.getMealTime().getDayOrder() + " (" + line.getMealTime().getDisplayName() + ")");
            }
        }
    }

    /** Convierte una fila de {@code PlanDieta} en un {@link DietPlan} (sin líneas). */
    private static DietPlan mapPlan(ResultSet rs) throws SQLException {
        DietPlan p = new DietPlan(rs.getInt("id_usuario"), getDate(rs, "fecha_generacion"),
                rs.getDouble("kcal_objetivo"), DietPlan.DietType.valueOf(rs.getString("tipo_dieta")));
        p.setPlanId(rs.getInt("id_plan"));
        p.setActive(rs.getBoolean("activo"));
        p.setLines(new ArrayList<>());
        return p;
    }

    /** Convierte una fila de {@code PlanDieta_Alimento} (con {@code orden_dia}) en un {@link DietPlanFood}. */
    private static DietPlanFood mapLine(ResultSet rs) throws SQLException {
        return new DietPlanFood(rs.getInt("id_plan"), rs.getInt("id_alimento"), rs.getDouble("cantidad_g"),
                MealTime.fromDayOrder(rs.getInt("orden_dia")));
    }
}
