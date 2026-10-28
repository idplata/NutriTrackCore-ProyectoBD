package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import com.nutritrackcore.repository.DietPlanRepository;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.UserConditionRepository;
import com.nutritrackcore.repository.UserRepository;

/**
 * Caso de uso de eliminación de una cuenta, aplicando las políticas de
 * <b>CASCADA</b> del esquema relacional: al borrar un usuario desaparecen todos
 * los datos que le pertenecen.
 * <p>
 * Los catálogos (alimentos, ejercicios y condiciones médicas) no se tocan,
 * porque pertenecen al sistema y no al usuario.
 */
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;
    private final UserConditionRepository userConditionRepository;
    private final ConsumptionRecordRepository consumptionRecordRepository;
    private final DietPlanRepository dietPlanRepository;
    private final ExerciseRoutineRepository exerciseRoutineRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository              repositorio de usuarios
     * @param bmiHistoryRepository        repositorio del historial de IMC
     * @param userConditionRepository     repositorio de las condiciones de cada usuario
     * @param consumptionRecordRepository repositorio de registros de consumo
     * @param dietPlanRepository          repositorio de planes de dieta
     * @param exerciseRoutineRepository   repositorio de rutinas de ejercicio
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public AccountDeletionService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository,
                                  UserConditionRepository userConditionRepository,
                                  ConsumptionRecordRepository consumptionRecordRepository,
                                  DietPlanRepository dietPlanRepository,
                                  ExerciseRoutineRepository exerciseRoutineRepository) {
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
        this.userConditionRepository = ParameterChecks.requireNonNull(userConditionRepository,
                "repositorio de condiciones del usuario");
        this.consumptionRecordRepository = ParameterChecks.requireNonNull(consumptionRecordRepository,
                "repositorio de consumos");
        this.dietPlanRepository = ParameterChecks.requireNonNull(dietPlanRepository, "repositorio de planes");
        this.exerciseRoutineRepository = ParameterChecks.requireNonNull(exerciseRoutineRepository,
                "repositorio de rutinas");
    }

    /**
     * Elimina un usuario y todo lo que le pertenece.
     * <p>
     * Pasos: (1) valida el id y comprueba que el usuario exista; (2) borra, en
     * este orden, sus rutinas (primero, porque referencian al plan de dieta y a
     * una fila del historial), sus planes de dieta, sus registros de consumo, sus
     * condiciones médicas y su historial de IMC; (3) borra el usuario al final,
     * de modo que si algo falla a mitad de camino el usuario sigue existiendo y la
     * operación puede repetirse.
     *
     * @param userId identificador del usuario a eliminar
     * @throws IllegalArgumentException si el id es inválido o el usuario no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void deleteAccount(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("No existe un usuario con el identificador " + userId + ".");
        }
        exerciseRoutineRepository.deleteByUserId(userId);
        dietPlanRepository.deleteByUserId(userId);
        consumptionRecordRepository.deleteByUserId(userId);
        userConditionRepository.deleteByUserId(userId);
        bmiHistoryRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }
}