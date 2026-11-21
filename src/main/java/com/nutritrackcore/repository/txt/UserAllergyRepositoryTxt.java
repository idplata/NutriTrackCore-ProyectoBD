package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.repository.UserAllergyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link UserAllergyRepository} sobre
 * {@code user_allergies.txt} (tabla {@code Usuario_Alergia}).
 * <p>
 * Formato: {@code id_usuario|id_alergia|severidad|observaciones}. Clave
 * compuesta, sin id propio.
 */
public class UserAllergyRepositoryTxt extends AbstractTxtRepository<UserAllergy> implements UserAllergyRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_usuario|id_alergia|severidad|observaciones";

    /**
     * @param file archivo {@code user_allergies.txt}
     */
    public UserAllergyRepositoryTxt(TxtFile file) {
        super(file, "user_allergies.txt", 4);
    }

    @Override
    public void save(UserAllergy userAllergy) throws PersistenceExceptions {
        List<UserAllergy> all = readAll();
        for (UserAllergy ua : all) {
            if (sameKey(ua, userAllergy.getUserId(), userAllergy.getAllergyId())) {
                throw new PersistenceExceptions("El usuario " + userAllergy.getUserId()
                        + " ya tiene la alergia " + userAllergy.getAllergyId());
            }
        }
        all.add(userAllergy);
        writeAll(all);
    }

    @Override
    public void update(UserAllergy userAllergy) throws PersistenceExceptions {
        List<UserAllergy> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (sameKey(all.get(i), userAllergy.getUserId(), userAllergy.getAllergyId())) {
                all.set(i, userAllergy);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("El usuario " + userAllergy.getUserId()
                + " no tiene la alergia " + userAllergy.getAllergyId());
    }

    @Override
    public Optional<UserAllergy> find(int userId, int allergyId) throws PersistenceExceptions {
        for (UserAllergy ua : readAll()) {
            if (sameKey(ua, userId, allergyId)) {
                return Optional.of(ua);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<UserAllergy> findByUserId(int userId) throws PersistenceExceptions {
        List<UserAllergy> result = new ArrayList<>();
        for (UserAllergy ua : readAll()) {
            if (ua.getUserId() == userId) {
                result.add(ua);
            }
        }
        return result;
    }

    @Override
    public void delete(int userId, int allergyId) throws PersistenceExceptions {
        List<UserAllergy> all = readAll();
        if (all.removeIf(ua -> sameKey(ua, userId, allergyId))) {
            writeAll(all);
        }
    }

    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<UserAllergy> all = readAll();
        if (all.removeIf(ua -> ua.getUserId() == userId)) {
            writeAll(all);
        }
    }

    /** Compara la clave compuesta usuario + alergia. */
    private static boolean sameKey(UserAllergy ua, int userId, int allergyId) {
        return ua.getUserId() == userId && ua.getAllergyId() == allergyId;
    }

    @Override
    protected String toLine(UserAllergy ua) {
        return TxtFormat.join(
                String.valueOf(ua.getUserId()),
                String.valueOf(ua.getAllergyId()),
                ua.getSeverity(),
                ua.getObservations());
    }

    @Override
    protected UserAllergy fromFields(String[] c) {
        return new UserAllergy(
                Integer.parseInt(c[0]),
                Integer.parseInt(c[1]),
                TxtFormat.nullIfEmpty(c[2]),
                TxtFormat.nullIfEmpty(c[3]));
    }
}
