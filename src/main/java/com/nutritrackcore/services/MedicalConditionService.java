package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.UserCondition;
import com.nutritrackcore.repository.MedicalConditionRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo de condiciones médicas y su asignación a usuarios.
 */
public class MedicalConditionService {
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_NOTES_LENGTH = 500;

    private final MedicalConditionRepository conditionRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserRepository userRepository;

    public MedicalConditionService(MedicalConditionRepository conditionRepository,
                                   UserConditionRepository userConditionRepository,
                                   UserRepository userRepository) {
        this.conditionRepository = Objects.requireNonNull(conditionRepository, "repositorio de condiciones");
        this.userConditionRepository = Objects.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
    }

    public List<MedicalCondition> listCatalog() throws NutriTrackException {
        return conditionRepository.findAllActive();
    }

    public MedicalCondition create(MedicalCondition condition) throws NutriTrackException {
        ParameterChecks.requireNonNull(condition, "condición médica");
        String name = ParameterChecks.requireTextMax(condition.getName(), MAX_NAME_LENGTH, "nombre de la condición");
        condition.setDescription(ParameterChecks.requireOptionalText(
                condition.getDescription(), MAX_DESCRIPTION_LENGTH, "descripción de la condición"));
        for (MedicalCondition existing : conditionRepository.findAll()) {
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(name)) {
                throw new BussinesRuleException("Ya existe una condición llamada '" + name + "'.");
            }
        }
        condition.setName(name);
        condition.setActive(true);
        return conditionRepository.save(condition);
    }

    public void deactivate(int conditionId) throws NutriTrackException {
        MedicalCondition condition = requireCondition(conditionId);
        if (condition.isActive()) {
            condition.setActive(false);
            conditionRepository.update(condition);
        }
    }

    public void assign(int userId, int conditionId) throws NutriTrackException {
        saveAssignment(userId, conditionId, null, null);
    }

    public void assign(int userId, int conditionId, LocalDate diagnosisDate, String observations)
            throws NutriTrackException {
        if (diagnosisDate != null) {
            ParameterChecks.requireNotFuture(diagnosisDate, "fecha de diagnóstico");
        }
        String notes = ParameterChecks.requireOptionalText(observations, MAX_NOTES_LENGTH, "observaciones");
        saveAssignment(userId, conditionId, diagnosisDate, notes);
    }

    private void saveAssignment(int userId, int conditionId, LocalDate diagnosisDate, String notes)
            throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }
        MedicalCondition condition = requireCondition(conditionId);
        if (!condition.isActive()) {
            throw new BussinesRuleException(
                    "La condición '" + condition.getName() + "' está desactivada y no se puede asignar.");
        }
        if (userConditionRepository.find(userId, conditionId).isPresent()) {
            throw new BussinesRuleException("El usuario ya tiene asignada la condición '" + condition.getName() + "'.");
        }
        userConditionRepository.save(new UserCondition(userId, conditionId, diagnosisDate, notes));
    }

    public void remove(int userId, int conditionId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        if (userConditionRepository.find(userId, conditionId).isEmpty()) {
            throw new NotFoundEntityException("El usuario no tiene asignada esa condición.");
        }
        userConditionRepository.delete(userId, conditionId);
    }

    public List<UserCondition> listAssignments(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userConditionRepository.findByUserId(userId);
    }

    public List<MedicalCondition> listByUser(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        List<MedicalCondition> result = new ArrayList<>();
        for (UserCondition assignment : userConditionRepository.findByUserId(userId)) {
            Optional<MedicalCondition> condition = conditionRepository.findById(assignment.getConditionId());
            condition.ifPresent(result::add);
        }
        return result;
    }

    /** Condiciones activas del usuario (las que cambian dieta y rutina). */
    public List<MedicalCondition> listActiveByUser(int userId) throws NutriTrackException {
        List<MedicalCondition> result = new ArrayList<>();
        for (MedicalCondition condition : listByUser(userId)) {
            if (condition.isActive()) {
                result.add(condition);
            }
        }
        return result;
    }

    private MedicalCondition requireCondition(int conditionId) throws NutriTrackException {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        return conditionRepository.findById(conditionId).orElseThrow(
                () -> new NotFoundEntityException("No existe una condición con el identificador " + conditionId + "."));
    }
}