package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.AllergyFood;
import java.util.List;

/**
 * Contrato de persistencia del enlace alergia-alimento (tabla
 * {@code Alergia_Alimento}): qué alimentos provocan cada alergia.
 * <p>
 * Es un enlace puro, sin datos propios: solo se crea, se consulta y se borra.
 *
 * @see AllergyFood
 */
public interface AllergyFoodRepository {

    /**
     * Enlaza un alimento con una alergia.
     *
     * @param link par alergia-alimento
     * @throws PersistenceExceptions si el par ya existe o si falla el almacenamiento
     */
    void save(AllergyFood link) throws PersistenceExceptions;

    /**
     * Devuelve los alimentos enlazados a una alergia.
     *
     * @param allergyId id de la alergia
     * @return lista de pares; vacía si no hay alimentos enlazados
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<AllergyFood> findByAllergyId(int allergyId) throws PersistenceExceptions;

    /**
     * Devuelve las alergias que provoca un alimento.
     *
     * @param foodId id del alimento
     * @return lista de pares; vacía si el alimento no está enlazado a ninguna alergia
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<AllergyFood> findByFoodId(int foodId) throws PersistenceExceptions;

    /**
     * Quita el enlace entre una alergia y un alimento. No falla si no existe.
     *
     * @param allergyId id de la alergia
     * @param foodId    id del alimento
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void delete(int allergyId, int foodId) throws PersistenceExceptions;
}
