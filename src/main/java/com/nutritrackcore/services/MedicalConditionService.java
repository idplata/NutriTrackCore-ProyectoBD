package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.model.UserCondition;
import com.nutritrackcore.repository.MedicalConditionRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso del catálogo de condiciones médicas y de su asignación a los
 * usuarios.
 * <p>
 * Regla RESTRINGIR del esquema relacional: una condición del catálogo
 * <b>nunca se borra</b>, solo se desactiva; así los usuarios que ya la tenían
 * conservan su historial. Una condición desactivada deja de ofrecerse en las
 * listas de selección pero sigue apareciendo en las asignaciones existentes.
 */
public class MedicalConditionService {

    /** Longitud máxima del nombre de una condición. */
    private static final int MAX_NAME_LENGTH = 100;

    /** Longitud máxima de la descripción de una condición. */
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /** Longitud máxima de las observaciones de una asignación. */
    private static final int MAX_NOTES_LENGTH = 500;

    private final MedicalConditionRepository conditionRepository;
    private final UserConditionRepository userConditionRepository;
    private final UserRepository userRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param conditionRepository     repositorio del catálogo de condiciones
     * @param userConditionRepository repositorio de las condiciones de cada usuario
     * @param userRepository          repositorio de usuarios (para comprobar que el usuario existe)
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public MedicalConditionService(MedicalConditionRepository conditionRepository,
                                   UserConditionRepository userConditionRepository,
                                   UserRepository userRepository) {
        this.conditionRepository = ParameterChecks.requireNonNull(conditionRepository, "repositorio de condiciones");
        this.userConditionRepository = ParameterChecks.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
    }

    /**
     * Lista las condiciones que se pueden elegir, es decir, solo las activas.
     *
     * @return condiciones activas del catálogo; vacía si no hay ninguna
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    public List<MedicalCondition> listCatalog() throws PersistenceExceptions {
        return conditionRepository.findAllActive();
    }

    /**
     * Crea una condición nueva en el catálogo.
     * <p>
     * Pasos: (1) valida que no sea nula, que el nombre sea obligatorio (máximo
     * 100 caracteres) y que la descripción, si trae una, no pase de 500; (2)
     * rechaza el nombre si ya existe otra condición con el mismo nombre (sin
     * distinguir mayúsculas); (3) la marca como activa y la guarda.
     *
     * @param condition condición a crear (su id se ignora; lo asigna el repositorio)
     * @return la condición guardada, con su id
     * @throws IllegalArgumentException si la condición es nula o sus datos son inválidos
     * @throws IllegalStateException    si ya existe una condición con ese nombre
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public MedicalCondition create(MedicalCondition condition) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(condition, "condición médica");
        String name = ParameterChecks.requireTextMax(condition.getName(), MAX_NAME_LENGTH, "nombre de la condición");
        if (condition.getDescription() != null) {
            condition.setDescription(ParameterChecks.requireOptionalText(
                    condition.getDescription(), MAX_DESCRIPTION_LENGTH, "descripción de la condición"));
        }
        for (MedicalCondition existing : conditionRepository.findAll()) {
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(name)) {
                throw new IllegalStateException("Ya existe una condición llamada '" + name + "'.");
            }
        }
        condition.setName(name);
        condition.setActive(true);
        return conditionRepository.save(condition);
    }

    /**
     * Desactiva una condición del catálogo (nunca la borra). Si ya estaba
     * desactivada, no hace nada.
     *
     * @param conditionId identificador de la condición
     * @throws IllegalArgumentException si el id es inválido o la condición no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void deactivate(int conditionId) throws PersistenceExceptions {
        MedicalCondition condition = requireCondition(conditionId);
        if (condition.isActive()) {
            condition.setActive(false);
            conditionRepository.update(condition);
        }
    }

    /**
     * Asigna una condición a un usuario sin fecha de diagnóstico ni
     * observaciones. Equivale a llamar a
     * {@link #assign(int, int, LocalDate, String)} sin esos datos opcionales.
     *
     * @param userId      identificador del usuario
     * @param conditionId identificador de la condición
     * @throws IllegalArgumentException si algún id es inválido, el usuario o la condición no existen o la condición está desactivada
     * @throws IllegalStateException    si el usuario ya tiene esa condición
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void assign(int userId, int conditionId) throws PersistenceExceptions {
        saveAssignment(userId, conditionId, null, null);
    }

    /**
     * Asigna una condición a un usuario con fecha de diagnóstico y
     * observaciones.
     * <p>
     * Ningún parámetro puede ser nulo; las observaciones sí pueden ser una
     * cadena vacía. Para no indicar esos datos, use
     * {@link #assign(int, int)}.
     *
     * @param userId         identificador del usuario
     * @param conditionId    identificador de la condición
     * @param diagnosisDate  fecha en que se diagnosticó (no futura)
     * @param observations   notas adicionales (por ejemplo, la severidad), máximo 500 caracteres
     * @throws IllegalArgumentException si algún dato es nulo/inválido, el usuario o la condición no existen o la condición está desactivada
     * @throws IllegalStateException    si el usuario ya tiene esa condición
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void assign(int userId, int conditionId, LocalDate diagnosisDate, String observations)
            throws PersistenceExceptions {
        ParameterChecks.requireNotFuture(diagnosisDate, "fecha de diagnóstico");
        String notes = ParameterChecks.requireOptionalText(observations, MAX_NOTES_LENGTH, "observaciones");
        saveAssignment(userId, conditionId, diagnosisDate, notes);
    }

    /**
     * Lógica compartida de las dos versiones de {@code assign}.
     * <p>
     * Valida los ids, comprueba que el usuario exista, que la condición exista y
     * esté activa y que el usuario no la tenga ya; después guarda el par
     * usuario-condición. Una observación vacía se guarda como {@code null}.
     *
     * @param userId        identificador del usuario
     * @param conditionId   identificador de la condición
     * @param diagnosisDate fecha de diagnóstico, o {@code null} si no se indica
     * @param notes         observaciones, o {@code null} si no se indican
     * @throws PersistenceExceptions si falla la lectura o escritura del almacenamiento
     */
    private void saveAssignment(int userId, int conditionId, LocalDate diagnosisDate, String notes)
            throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("No existe un usuario con el identificador " + userId + ".");
        }
        MedicalCondition condition = requireCondition(conditionId);
        if (!condition.isActive()) {
            throw new IllegalArgumentException(
                    "La condición '" + condition.getName() + "' está desactivada y no se puede asignar.");
        }
        if (userConditionRepository.find(userId, conditionId).isPresent()) {
            throw new IllegalStateException("El usuario ya tiene asignada la condición '" + condition.getName() + "'.");
        }
        UserCondition assignment = new UserCondition();
        assignment.setUserId(userId);
        assignment.setConditionId(conditionId);
        assignment.setDiagnosisDate(diagnosisDate);
        assignment.setObservations(notes == null || notes.isEmpty() ? null : notes);
        userConditionRepository.save(assignment);
    }

    /**
     * Quita una condición a un usuario.
     *
     * @param userId      identificador del usuario
     * @param conditionId identificador de la condición
     * @throws IllegalArgumentException si algún id es inválido o el usuario no tiene esa condición
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void remove(int userId, int conditionId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        if (userConditionRepository.find(userId, conditionId).isEmpty()) {
            throw new IllegalArgumentException("El usuario no tiene asignada esa condición.");
        }
        userConditionRepository.delete(userId, conditionId);
    }

    /**
     * Lista las asignaciones de un usuario (con fecha de diagnóstico y
     * observaciones), para mostrarlas en una tabla.
     *
     * @param userId identificador del usuario
     * @return asignaciones del usuario; vacía si no tiene condiciones
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<UserCondition> listAssignments(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userConditionRepository.findByUserId(userId);
    }

    /**
     * Lista las condiciones médicas de un usuario como objetos del catálogo.
     * <p>
     * Incluye también las condiciones que luego fueron desactivadas, porque el
     * usuario las sigue teniendo; si una asignación apunta a una condición que ya
     * no está en el catálogo, esa asignación se omite.
     *
     * @param userId identificador del usuario
     * @return condiciones del usuario; vacía si no tiene ninguna
     * @throws IllegalArgumentException si el id es inválido
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<MedicalCondition> listByUser(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        List<MedicalCondition> result = new ArrayList<>();
        for (UserCondition assignment : userConditionRepository.findByUserId(userId)) {
            Optional<MedicalCondition> condition = conditionRepository.findById(assignment.getConditionId());
            condition.ifPresent(result::add);
        }
        return result;
    }

    /**
     * Valida el id y carga una condición, esté activa o no.
     *
     * @param conditionId identificador de la condición
     * @return la condición encontrada
     * @throws IllegalArgumentException si el id es inválido o la condición no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    private MedicalCondition requireCondition(int conditionId) throws PersistenceExceptions {
        ParameterChecks.requireId(conditionId, "identificador de la condición");
        return conditionRepository.findById(conditionId).orElseThrow(
                () -> new IllegalArgumentException("No existe una condición con el identificador " + conditionId + "."));
    }
}