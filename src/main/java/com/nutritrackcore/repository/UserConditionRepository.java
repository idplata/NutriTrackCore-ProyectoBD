package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserCondition;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de las condiciones médicas de cada usuario.
 * <p>
 * La clave es compuesta ({@code userId} + {@code conditionId}): no hay id
 * propio ni id autogenerado.
 *
 * @see UserCondition
 */
public interface UserConditionRepository {

    /**
     * Asigna una condición a un usuario.
     *
     * @param userCondition par usuario-condición a guardar
     * @throws PersistenceExceptions si ese par ya existe o si falla el almacenamiento
     */
    void save(UserCondition userCondition) throws PersistenceExceptions;

    /**
     * Cambia la fecha de diagnóstico y las observaciones de un par existente.
     *
     * @param userCondition par usuario-condición con los datos nuevos
     * @throws PersistenceExceptions si el par no existe o si falla el almacenamiento
     */
    void update(UserCondition userCondition) throws PersistenceExceptions;

    /**
     * Busca la condición de un usuario por su clave compuesta.
     *
     * @param userId      id del usuario
     * @param conditionId id de la condición médica
     * @return el par, o {@link Optional#empty()} si el usuario no tiene esa condición
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<UserCondition> find(int userId, int conditionId) throws PersistenceExceptions;

    /**
     * Devuelve todas las condiciones asignadas a un usuario.
     *
     * @param userId id del usuario
     * @return lista de pares; vacía si el usuario no tiene condiciones
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<UserCondition> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Quita una condición a un usuario. No falla si el par no existe.
     *
     * @param userId      id del usuario
     * @param conditionId id de la condición médica a quitar
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void delete(int userId, int conditionId) throws PersistenceExceptions;

    /**
     * Elimina todas las condiciones de un usuario. Se usa para el borrado en
     * cascada al eliminar una cuenta. No falla si el usuario no tiene filas.
     *
     * @param userId id del usuario cuyas condiciones se eliminan
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
