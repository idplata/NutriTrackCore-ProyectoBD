package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de los planes de dieta.
 * <p>
 * Cada plan se guarda junto con sus líneas ({@code DietPlanFood}): quien llama
 * nunca maneja las líneas por separado, siempre viajan dentro del
 * {@link DietPlan}.
 *
 * @see DietPlan
 */
public interface DietPlanRepository {

    /**
     * Guarda un plan nuevo con todas sus líneas y le asigna su id.
     *
     * @param plan plan a guardar; su id se reemplaza por el que se genere
     * @return el mismo plan, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    DietPlan save(DietPlan plan) throws PersistenceExceptions;

    /**
     * Reemplaza el plan y todas sus líneas por lo que trae el objeto.
     *
     * @param plan plan con los datos nuevos (se busca por su id)
     * @throws PersistenceExceptions si el plan no existe o si falla el almacenamiento
     */
    void update(DietPlan plan) throws PersistenceExceptions;

    /**
     * Busca un plan por su id, junto con sus líneas.
     *
     * @param planId id del plan buscado
     * @return el plan, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<DietPlan> findById(int planId) throws PersistenceExceptions;

    /**
     * Devuelve todos los planes de un usuario, del más antiguo al más reciente.
     *
     * @param userId id del usuario
     * @return lista ordenada por fecha de generación; vacía si el usuario no tiene planes
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<DietPlan> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Devuelve el plan activo más reciente del usuario, si tiene alguno.
     *
     * @param userId id del usuario
     * @return el plan activo más reciente, o {@link Optional#empty()} si no hay ninguno activo
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<DietPlan> findActiveByUserId(int userId) throws PersistenceExceptions;

    /**
     * Elimina todos los planes de un usuario junto con sus líneas. Se usa para
     * el borrado en cascada al eliminar una cuenta. No falla si no hay planes.
     *
     * @param userId id del usuario cuyos planes se eliminan
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
