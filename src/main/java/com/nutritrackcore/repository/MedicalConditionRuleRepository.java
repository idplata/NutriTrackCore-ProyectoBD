package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalConditionRule;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de las reglas clínicas de cada condición médica
 * (tabla {@code ReglaCondicion}).
 *
 * @see MedicalConditionRule
 */
public interface MedicalConditionRuleRepository {

    /**
     * Guarda una regla nueva y le asigna su id.
     *
     * @param rule regla a guardar; su id se reemplaza por el que se genere
     * @return la misma regla, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    MedicalConditionRule save(MedicalConditionRule rule) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de una regla existente (se busca por su id).
     *
     * @param rule regla con los datos nuevos
     * @throws PersistenceExceptions si la regla no existe o si falla el almacenamiento
     */
    void update(MedicalConditionRule rule) throws PersistenceExceptions;

    /**
     * Busca una regla por su id.
     *
     * @param ruleId id de la regla
     * @return la regla, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<MedicalConditionRule> findById(int ruleId) throws PersistenceExceptions;

    /**
     * Devuelve todas las reglas de una condición médica.
     *
     * @param conditionId id de la condición
     * @return lista de reglas; vacía si la condición no tiene reglas
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<MedicalConditionRule> findByConditionId(int conditionId) throws PersistenceExceptions;

    /**
     * Elimina una regla por su id. No falla si no existe.
     *
     * @param ruleId id de la regla
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteById(int ruleId) throws PersistenceExceptions;
}
