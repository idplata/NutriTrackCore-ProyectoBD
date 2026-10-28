package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de usuarios.
 * <p>
 * No conoce archivos ni bases de datos: cualquier implementación (.txt, JDBC)
 * debe cumplirlo. Las reglas de negocio (por ejemplo, rechazar emails
 * duplicados) las aplica la capa de servicio, no el repositorio.
 *
 * @see User
 */
public interface UserRepository {

    /**
     * Guarda un usuario nuevo y le asigna su id.
     *
     * @param user usuario a guardar; su id se reemplaza por el que se genere
     * @return el mismo usuario, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    User save(User user) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de un usuario existente (se busca por {@code idUser}).
     *
     * @param user usuario con los datos nuevos
     * @throws PersistenceExceptions si el usuario no existe o si falla el almacenamiento
     */
    void update(User user) throws PersistenceExceptions;

    /**
     * Busca un usuario por su id.
     *
     * @param idUser id del usuario buscado
     * @return el usuario, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<User> findById(int idUser) throws PersistenceExceptions;

    /**
     * Busca un usuario por su email. Lo usa el inicio de sesión y la detección
     * de emails duplicados (la regla la aplica el servicio).
     *
     * @param email email buscado
     * @return el usuario, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<User> findByEmail(String email) throws PersistenceExceptions;

    /**
     * Devuelve todos los usuarios registrados.
     *
     * @return lista de usuarios; vacía si no hay ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<User> findAll() throws PersistenceExceptions;

    /**
     * Elimina un usuario por su id. Lo usa la eliminación de cuenta. No falla
     * si el id no existe.
     *
     * @param idUser id del usuario a eliminar
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteById(int idUser) throws PersistenceExceptions;
}
