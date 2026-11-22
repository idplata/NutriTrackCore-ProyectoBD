package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.FoodRepository;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;

/**
 * Implementación de {@link FoodRepository} sobre la tabla {@code Alimento}.
 * <p>
 * El grupo de la pirámide se guarda por el nombre del enum
 * {@link Food.FoodPyramid}, que coincide con los valores del {@code check}.
 */
public class FoodRepositoryJdbc extends JdbcRepository implements FoodRepository {

    private static final String COLUMNS = "id_alimento, nombre, densidad_g_cm3, calorias_100g, proteinas_100g,"
            + " carbohidratos_100g, grasas_100g, activo, grupo";

    /** @param dataSource fuente de conexiones (el pool) */
    public FoodRepositoryJdbc(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Food save(Food food) throws PersistenceExceptions {
        return insertReturning("""
                INSERT INTO Alimento (nombre, densidad_g_cm3, calorias_100g, proteinas_100g,
                                      carbohidratos_100g, grasas_100g, activo, grupo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id_alimento
                """, ps -> bindFields(ps, food), rs -> {
                    food.setFoodId(rs.getInt("id_alimento"));
                    return food;
                });
    }

    @Override
    public void update(Food food) throws PersistenceExceptions {
        updateExisting("""
                UPDATE Alimento SET nombre = ?, densidad_g_cm3 = ?, calorias_100g = ?, proteinas_100g = ?,
                                    carbohidratos_100g = ?, grasas_100g = ?, activo = ?, grupo = ?
                WHERE id_alimento = ?
                """, ps -> {
                    bindFields(ps, food);
                    ps.setInt(9, food.getFoodId());
                }, "No existe el alimento con id " + food.getFoodId());
    }

    @Override
    public Optional<Food> findById(int foodId) throws PersistenceExceptions {
        return queryOne("SELECT " + COLUMNS + " FROM Alimento WHERE id_alimento = ?",
                ps -> ps.setInt(1, foodId), this::map);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Igual que en .txt, no distingue mayúsculas ni tildes: el texto se
     * normaliza en Java y el nombre se normaliza en SQL con {@code translate}.
     */
    @Override
    public List<Food> findByName(String text) throws PersistenceExceptions {
        String pattern = "%" + escapeLike(normalize(text)) + "%";
        return query("SELECT " + COLUMNS + " FROM Alimento"
                        + " WHERE translate(lower(nombre), 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunaeiouun') LIKE ? ESCAPE '\\'"
                        + " ORDER BY id_alimento",
                ps -> ps.setString(1, pattern), this::map);
    }

    @Override
    public List<Food> findAllActive() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Alimento WHERE activo ORDER BY id_alimento", NO_PARAMS, this::map);
    }

    @Override
    public List<Food> findAll() throws PersistenceExceptions {
        return query("SELECT " + COLUMNS + " FROM Alimento ORDER BY id_alimento", NO_PARAMS, this::map);
    }

    /** Asigna los parámetros 1 a 8 (todas las columnas menos el id). */
    private static void bindFields(PreparedStatement ps, Food f) throws SQLException {
        ps.setString(1, f.getName());
        setNullableDouble(ps, 2, f.getDensityGCm3());
        ps.setDouble(3, f.getCalories100g());
        setNullableDouble(ps, 4, f.getProteins100g());
        setNullableDouble(ps, 5, f.getCarbohydrates100g());
        setNullableDouble(ps, 6, f.getFats100g());
        ps.setBoolean(7, f.isActive());
        ps.setString(8, f.getGrupe() == null ? null : f.getGrupe().name());
    }

    /** Convierte una fila de {@code Alimento} en un {@link Food}. */
    private Food map(ResultSet rs) throws SQLException {
        Food f = new Food();
        f.setFoodId(rs.getInt("id_alimento"));
        f.setName(rs.getString("nombre"));
        f.setDensityGCm3(getNullableDouble(rs, "densidad_g_cm3"));
        f.setCalories100g(rs.getDouble("calorias_100g"));
        f.setProteins100g(getNullableDouble(rs, "proteinas_100g"));
        f.setCarbohydrates100g(getNullableDouble(rs, "carbohidratos_100g"));
        f.setFats100g(getNullableDouble(rs, "grasas_100g"));
        f.setActive(rs.getBoolean("activo"));
        String group = rs.getString("grupo");
        f.setGrupe(group == null ? null : Food.FoodPyramid.valueOf(group));
        return f;
    }

    /** Minúsculas y sin tildes, igual que {@code FoodRepositoryTxt}. */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Escapa los comodines de LIKE para que el texto buscado se tome literal. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
