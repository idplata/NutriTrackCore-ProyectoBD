package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalCondition;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del catálogo de condiciones médicas.
 * <p>
 * Las condiciones no se borran: se desactivan, para que los registros
 * históricos puedan seguir mostrándolas.
 *
 * @see MedicalCondition
 */
public interface MedicalConditionRepository {

    /**
     * Guarda una condición nueva y le asigna su id.
     *
     * @param condition condición a guardar; su id se reemplaza por el que se genere
     * @return la misma condición, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    MedicalCondition save(MedicalCondition condition) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de una condición existente (se busca por su id).
     * También se usa para activar o desactivar una condición.
     *
     * @param condition condición con los datos nuevos
     * @throws PersistenceExceptions si la condición no existe o si falla el almacenamiento
     */
    void update(MedicalCondition condition) throws PersistenceExceptions;

    /**
     * Busca una condición por su id, esté activa o no.
     *
     * @param conditionId id de la condición buscada
     * @return la condición, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<MedicalCondition> findById(int conditionId) throws PersistenceExceptions;

    /**
     * Devuelve solo las condiciones activas: las que aparecen en las listas de
     * selección.
     *
     * @return lista de condiciones activas; vacía si no hay ninguna
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<MedicalCondition> findAllActive() throws PersistenceExceptions;

    /**
     * Devuelve todas las condiciones, incluidas las desactivadas (los registros
     * históricos las siguen mostrando).
     *
     * @return lista con todas las condiciones; vacía si el catálogo está vacío
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<MedicalCondition> findAll() throws PersistenceExceptions;
}
