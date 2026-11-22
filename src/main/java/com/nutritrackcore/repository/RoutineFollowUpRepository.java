package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.RoutineFollowUp;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del seguimiento de rutinas (tabla
 * {@code SeguimientoRutina}): cada sesión que el usuario hizo de una rutina.
 *
 * @see RoutineFollowUp
 */
public interface RoutineFollowUpRepository {

    /**
     * Guarda una sesión nueva y le asigna su id.
     *
     * @param followUp sesión a guardar; su id se reemplaza por el que se genere
     * @return la misma sesión, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    RoutineFollowUp save(RoutineFollowUp followUp) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de una sesión existente (se busca por su id).
     *
     * @param followUp sesión con los datos nuevos
     * @throws PersistenceExceptions si la sesión no existe o si falla el almacenamiento
     */
    void update(RoutineFollowUp followUp) throws PersistenceExceptions;

    /**
     * Busca una sesión por su id.
     *
     * @param followUpId id de la sesión
     * @return la sesión, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<RoutineFollowUp> findById(int followUpId) throws PersistenceExceptions;

    /**
     * Devuelve las sesiones de una rutina, de la más antigua a la más reciente.
     *
     * @param routineId id de la rutina
     * @return lista ordenada por fecha de sesión; vacía si no hay sesiones
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<RoutineFollowUp> findByRoutineId(int routineId) throws PersistenceExceptions;

    /**
     * Elimina todas las sesiones de una rutina. Equivale al
     * {@code ON DELETE CASCADE} de la base de datos.
     *
     * @param routineId id de la rutina
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByRoutineId(int routineId) throws PersistenceExceptions;
}
