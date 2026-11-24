package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.AllergyFood;
import com.nutritrackcore.repository.AllergyFoodRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link AllergyFoodRepository} sobre
 * {@code allergy_foods.txt} (tabla {@code Alergia_Alimento}).
 * <p>
 * Formato: {@code id_alergia|id_alimento}.
 */
public class AllergyFoodRepositoryTxt extends AbstractTxtRepository<AllergyFood> implements AllergyFoodRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_alergia|id_alimento";

    /**
     * @param file archivo {@code allergy_foods.txt}
     */
    public AllergyFoodRepositoryTxt(TxtFile file) {
        super(file, "allergy_foods.txt", 2);
    }

    @Override
    public void save(AllergyFood link) throws PersistenceExceptions {
        List<AllergyFood> all = readAll();
        for (AllergyFood af : all) {
            if (af.getAllergyId() == link.getAllergyId() && af.getFoodId() == link.getFoodId()) {
                throw new PersistenceExceptions("El alimento " + link.getFoodId()
                        + " ya está enlazado a la alergia " + link.getAllergyId());
            }
        }
        all.add(link);
        writeAll(all);
    }

    @Override
    public List<AllergyFood> findByAllergyId(int allergyId) throws PersistenceExceptions {
        List<AllergyFood> result = new ArrayList<>();
        for (AllergyFood af : readAll()) {
            if (af.getAllergyId() == allergyId) {
                result.add(af);
            }
        }
        return result;
    }

    @Override
    public List<AllergyFood> findByFoodId(int foodId) throws PersistenceExceptions {
        List<AllergyFood> result = new ArrayList<>();
        for (AllergyFood af : readAll()) {
            if (af.getFoodId() == foodId) {
                result.add(af);
            }
        }
        return result;
    }

    @Override
    public void delete(int allergyId, int foodId) throws PersistenceExceptions {
        List<AllergyFood> all = readAll();
        if (all.removeIf(af -> af.getAllergyId() == allergyId && af.getFoodId() == foodId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(AllergyFood af) {
        return TxtFormat.join(String.valueOf(af.getAllergyId()), String.valueOf(af.getFoodId()));
    }

    @Override
    protected AllergyFood fromFields(String[] c) {
        return new AllergyFood(Integer.parseInt(c[0]), Integer.parseInt(c[1]));
    }
}
