package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserExcludedFood;
import java.util.List;

/**
 * Contrato de persistencia de los alimentos que cada usuario excluye de su
 * dieta (tabla {@code Usuario_AlimentoExcluido}).
 * <p>
 * Clave compuesta ({@code userId} + {@code foodId}), sin datos propios.
 *
 * @see UserExcludedFood
 */
public interface UserExcludedFoodRepository {

    /**
     * Excluye un alimento para un usuario.
     *
     * @param excluded par usuario-alimento
     * @throws PersistenceExceptions si el par ya existe o si falla el almacenamiento
     */
    void save(UserExcludedFood excluded) throws PersistenceExceptions;

    /**
     * Indica si un usuario tiene excluido un alimento.
     *
     * @param userId id del usuario
     * @param foodId id del alimento
     * @return {@code true} si el par existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    boolean exists(int userId, int foodId) throws PersistenceExceptions;

    /**
     * Devuelve los alimentos excluidos por un usuario.
     *
     * @param userId id del usuario
     * @return lista de pares; vacía si no excluye ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<UserExcludedFood> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Vuelve a permitir un alimento para un usuario. No falla si no estaba excluido.
     *
     * @param userId id del usuario
     * @param foodId id del alimento
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void delete(int userId, int foodId) throws PersistenceExceptions;

    /**
     * Elimina todas las exclusiones de un usuario (borrado en cascada de la cuenta).
     *
     * @param userId id del usuario
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
