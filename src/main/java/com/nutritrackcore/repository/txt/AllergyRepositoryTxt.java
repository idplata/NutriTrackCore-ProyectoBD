package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.repository.AllergyRepository;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link AllergyRepository} sobre {@code allergies.txt}
 * (tabla {@code Alergia}).
 * <p>
 * Formato: {@code id_alergia|nombre|descripcion}. La descripción vacía queda
 * como {@code null}.
 */
public class AllergyRepositoryTxt extends AbstractTxtRepository<Allergy> implements AllergyRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_alergia|nombre|descripcion";

    /**
     * @param file archivo {@code allergies.txt}
     */
    public AllergyRepositoryTxt(TxtFile file) {
        super(file, "allergies.txt", 3);
    }

    @Override
    public Allergy save(Allergy allergy) throws PersistenceExceptions {
        List<Allergy> all = readAll();
        allergy.setAllergyId(nextId(all, Allergy::getAllergyId));
        all.add(allergy);
        writeAll(all);
        return allergy;
    }

    @Override
    public void update(Allergy allergy) throws PersistenceExceptions {
        List<Allergy> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getAllergyId() == allergy.getAllergyId()) {
                all.set(i, allergy);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe la alergia con id " + allergy.getAllergyId());
    }

    @Override
    public Optional<Allergy> findById(int allergyId) throws PersistenceExceptions {
        for (Allergy a : readAll()) {
            if (a.getAllergyId() == allergyId) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Allergy> findByName(String name) throws PersistenceExceptions {
        for (Allergy a : readAll()) {
            if (a.getName() != null && a.getName().equalsIgnoreCase(name)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Allergy> findAll() throws PersistenceExceptions {
        return readAll();
    }

    @Override
    protected String toLine(Allergy a) {
        return TxtFormat.join(String.valueOf(a.getAllergyId()), a.getName(), a.getDescription());
    }

    @Override
    protected Allergy fromFields(String[] c) {
        Allergy a = new Allergy(c[1], TxtFormat.nullIfEmpty(c[2]));
        a.setAllergyId(Integer.parseInt(c[0]));
        return a;
    }
}
