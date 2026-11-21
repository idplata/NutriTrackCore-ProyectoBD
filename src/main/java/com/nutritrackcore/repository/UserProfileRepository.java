package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserProfile;
import java.util.Optional;

/**
 * Contrato de persistencia del perfil de cada usuario (tabla
 * {@code PerfilUsuario}).
 * <p>
 * La relación es 1:1 con el usuario: {@code userId} es la clave, así que cada
 * usuario tiene como máximo un perfil y no hay id autogenerado.
 *
 * @see UserProfile
 */
public interface UserProfileRepository {

    /**
     * Guarda el perfil de un usuario que todavía no tiene uno.
     *
     * @param profile perfil a guardar
     * @throws PersistenceExceptions si el usuario ya tiene perfil o si falla el almacenamiento
     */
    void save(UserProfile profile) throws PersistenceExceptions;

    /**
     * Reemplaza el perfil existente de un usuario.
     *
     * @param profile perfil con los datos nuevos (se busca por {@code userId})
     * @throws PersistenceExceptions si el usuario no tiene perfil o si falla el almacenamiento
     */
    void update(UserProfile profile) throws PersistenceExceptions;

    /**
     * Busca el perfil de un usuario.
     *
     * @param userId id del usuario
     * @return el perfil, o {@link Optional#empty()} si el usuario no lo ha llenado
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<UserProfile> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Elimina el perfil de un usuario (borrado en cascada de la cuenta). No
     * falla si no existe.
     *
     * @param userId id del usuario
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
