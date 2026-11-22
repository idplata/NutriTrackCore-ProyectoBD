package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserAllergy;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de las alergias de cada usuario (tabla
 * {@code Usuario_Alergia}).
 * <p>
 * La clave es compuesta ({@code userId} + {@code allergyId}): no hay id propio.
 *
 * @see UserAllergy
 */
public interface UserAllergyRepository {

    /**
     * Asigna una alergia a un usuario.
     *
     * @param userAllergy par usuario-alergia a guardar
     * @throws PersistenceExceptions si ese par ya existe o si falla el almacenamiento
     */
    void save(UserAllergy userAllergy) throws PersistenceExceptions;

    /**
     * Cambia la severidad y las observaciones de un par existente.
     *
     * @param userAllergy par usuario-alergia con los datos nuevos
     * @throws PersistenceExceptions si el par no existe o si falla el almacenamiento
     */
    void update(UserAllergy userAllergy) throws PersistenceExceptions;

    /**
     * Busca la alergia de un usuario por su clave compuesta.
     *
     * @param userId    id del usuario
     * @param allergyId id de la alergia
     * @return el par, o {@link Optional#empty()} si el usuario no tiene esa alergia
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<UserAllergy> find(int userId, int allergyId) throws PersistenceExceptions;

    /**
     * Devuelve todas las alergias de un usuario.
     *
     * @param userId id del usuario
     * @return lista de pares; vacía si el usuario no tiene alergias
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<UserAllergy> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Quita una alergia a un usuario. No falla si el par no existe.
     *
     * @param userId    id del usuario
     * @param allergyId id de la alergia
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void delete(int userId, int allergyId) throws PersistenceExceptions;

    /**
     * Elimina todas las alergias de un usuario (borrado en cascada de la cuenta).
     *
     * @param userId id del usuario
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
