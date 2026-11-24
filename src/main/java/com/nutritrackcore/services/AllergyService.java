package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.Allergy;
import com.nutritrackcore.model.UserAllergy;
import com.nutritrackcore.repository.AllergyRepository;
import com.nutritrackcore.repository.UserAllergyRepository;
import com.nutritrackcore.repository.UserRepository;
import java.util.List;
import java.util.Objects;

/**
 * Alergias del usuario. Al agregar una, revisa el plan activo y reemplaza o elimina
 * los alimentos que la provocan (ver {@link DietPlanService#refreshForRestrictions(int)}).
 */
public class AllergyService {
    private static final int MAX_NOTES_LENGTH = 500;

    private final AllergyRepository allergyRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserRepository userRepository;
    private final DietPlanService dietPlanService;

    public AllergyService(AllergyRepository allergyRepository, UserAllergyRepository userAllergyRepository,
                          UserRepository userRepository, DietPlanService dietPlanService) {
        this.allergyRepository = Objects.requireNonNull(allergyRepository, "repositorio de alergias");
        this.userAllergyRepository = Objects.requireNonNull(userAllergyRepository, "repositorio de alergias del usuario");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.dietPlanService = Objects.requireNonNull(dietPlanService, "servicio de dietas");
    }

    public List<Allergy> listCatalog() throws NutriTrackException {
        return allergyRepository.findAll();
    }

    public List<UserAllergy> listByUser(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userAllergyRepository.findByUserId(userId);
    }

    /**
     * Agrega una alergia al usuario y actualiza su plan activo.
     *
     * @param severity leve, moderada o severa (puede ser nula)
     * @return los alimentos reemplazados o eliminados del plan, para avisar al usuario
     */
    public AllergyChangeResult addAllergy(int userId, int allergyId, String severity, String observations)
            throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(allergyId, "identificador de la alergia");
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }
        Allergy allergy = allergyRepository.findById(allergyId).orElseThrow(
                () -> new NotFoundEntityException("No existe una alergia con el identificador " + allergyId + "."));
        if (userAllergyRepository.find(userId, allergyId).isPresent()) {
            throw new BussinesRuleException("El usuario ya tiene registrada la alergia '" + allergy.getName() + "'.");
        }
        String cleanSeverity = severity == null || severity.trim().isEmpty() ? null
                : ParameterChecks.requireLowerOption(severity, ParameterChecks.ALLERGY_SEVERITIES, "severidad");
        String notes = ParameterChecks.requireOptionalText(observations, MAX_NOTES_LENGTH, "observaciones");

        userAllergyRepository.save(new UserAllergy(userId, allergyId, cleanSeverity, notes));
        return dietPlanService.refreshForRestrictions(userId);
    }

    public void removeAllergy(int userId, int allergyId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(allergyId, "identificador de la alergia");
        if (userAllergyRepository.find(userId, allergyId).isEmpty()) {
            throw new NotFoundEntityException("El usuario no tiene registrada esa alergia.");
        }
        userAllergyRepository.delete(userId, allergyId);
    }
}