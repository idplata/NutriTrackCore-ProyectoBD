package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link UserExcludedFoodRepository} sobre
 * {@code user_excluded_foods.txt} (tabla {@code Usuario_AlimentoExcluido}).
 * <p>
 * Formato: {@code id_usuario|id_alimento}.
 */
public class UserExcludedFoodRepositoryTxt extends AbstractTxtRepository<UserExcludedFood>
        implements UserExcludedFoodRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_usuario|id_alimento";

    /**
     * @param file archivo {@code user_excluded_foods.txt}
     */
    public UserExcludedFoodRepositoryTxt(TxtFile file) {
        super(file, "user_excluded_foods.txt", 2);
    }

    @Override
    public void save(UserExcludedFood excluded) throws PersistenceExceptions {
        List<UserExcludedFood> all = readAll();
        for (UserExcludedFood e : all) {
            if (e.getUserId() == excluded.getUserId() && e.getFoodId() == excluded.getFoodId()) {
                throw new PersistenceExceptions("El usuario " + excluded.getUserId()
                        + " ya excluyó el alimento " + excluded.getFoodId());
            }
        }
        all.add(excluded);
        writeAll(all);
    }

    @Override
    public boolean exists(int userId, int foodId) throws PersistenceExceptions {
        for (UserExcludedFood e : readAll()) {
            if (e.getUserId() == userId && e.getFoodId() == foodId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<UserExcludedFood> findByUserId(int userId) throws PersistenceExceptions {
        List<UserExcludedFood> result = new ArrayList<>();
        for (UserExcludedFood e : readAll()) {
            if (e.getUserId() == userId) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public void delete(int userId, int foodId) throws PersistenceExceptions {
        List<UserExcludedFood> all = readAll();
        if (all.removeIf(e -> e.getUserId() == userId && e.getFoodId() == foodId)) {
            writeAll(all);
        }
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<UserExcludedFood> all = readAll();
        if (all.removeIf(e -> e.getUserId() == userId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(UserExcludedFood e) {
        return TxtFormat.join(String.valueOf(e.getUserId()), String.valueOf(e.getFoodId()));
    }

    @Override
    protected UserExcludedFood fromFields(String[] c) {
        return new UserExcludedFood(Integer.parseInt(c[0]), Integer.parseInt(c[1]));
    }
}
