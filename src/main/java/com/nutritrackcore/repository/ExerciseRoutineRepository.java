package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ExerciseRoutine;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de las rutinas de ejercicio.
 * <p>
 * Cada rutina se guarda junto con sus ejercicios y las condiciones médicas
 * consideradas al generarla: quien llama no las maneja por separado, siempre
 * viajan dentro del {@link ExerciseRoutine}.
 *
 * @see ExerciseRoutine
 */
public interface ExerciseRoutineRepository {

    /**
     * Guarda una rutina nueva con todo lo que trae (ejercicios y condiciones)
     * y le asigna su id.
     *
     * @param routine rutina a guardar; su id se reemplaza por el que se genere
     * @return la misma rutina, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    ExerciseRoutine save(ExerciseRoutine routine) throws PersistenceExceptions;

    /**
     * Reemplaza la rutina, sus ejercicios y sus condiciones por lo que trae el
     * objeto.
     *
     * @param routine rutina con los datos nuevos (se busca por su id)
     * @throws PersistenceExceptions si la rutina no existe o si falla el almacenamiento
     */
    void update(ExerciseRoutine routine) throws PersistenceExceptions;

    /**
     * Busca una rutina por su id, junto con sus ejercicios y condiciones.
     *
     * @param routineId id de la rutina buscada
     * @return la rutina, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<ExerciseRoutine> findById(int routineId) throws PersistenceExceptions;

    /**
     * Devuelve todas las rutinas de un usuario, de la más antigua a la más
     * reciente.
     *
     * @param userId id del usuario
     * @return lista ordenada por fecha de generación; vacía si el usuario no tiene rutinas
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<ExerciseRoutine> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Devuelve la rutina activa más reciente del usuario, si tiene alguna.
     *
     * @param userId id del usuario
     * @return la rutina activa más reciente, o {@link Optional#empty()} si no hay ninguna activa
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<ExerciseRoutine> findActiveByUserId(int userId) throws PersistenceExceptions;

    /**
     * Elimina todas las rutinas de un usuario junto con sus ejercicios y
     * condiciones. Se usa para el borrado en cascada al eliminar una cuenta.
     *
     * @param userId id del usuario cuyas rutinas se eliminan
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
