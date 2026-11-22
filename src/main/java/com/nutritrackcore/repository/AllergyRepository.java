package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Allergy;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del catálogo de alergias alimentarias (tabla
 * {@code Alergia}).
 * <p>
 * Es un catálogo cerrado: no se ofrece borrado porque las alergias de los
 * usuarios lo referencian con {@code ON DELETE RESTRICT}.
 *
 * @see Allergy
 */
public interface AllergyRepository {

    /**
     * Guarda una alergia nueva y le asigna su id.
     *
     * @param allergy alergia a guardar; su id se reemplaza por el que se genere
     * @return la misma alergia, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    Allergy save(Allergy allergy) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de una alergia existente (se busca por su id).
     *
     * @param allergy alergia con los datos nuevos
     * @throws PersistenceExceptions si la alergia no existe o si falla el almacenamiento
     */
    void update(Allergy allergy) throws PersistenceExceptions;

    /**
     * Busca una alergia por su id.
     *
     * @param allergyId id de la alergia
     * @return la alergia, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<Allergy> findById(int allergyId) throws PersistenceExceptions;

    /**
     * Busca una alergia por su nombre exacto, sin distinguir mayúsculas. Sirve
     * para detectar nombres repetidos (el nombre es único).
     *
     * @param name nombre buscado
     * @return la alergia, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<Allergy> findByName(String name) throws PersistenceExceptions;

    /**
     * Devuelve todo el catálogo de alergias.
     *
     * @return lista de alergias; vacía si el catálogo está vacío
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Allergy> findAll() throws PersistenceExceptions;
}
