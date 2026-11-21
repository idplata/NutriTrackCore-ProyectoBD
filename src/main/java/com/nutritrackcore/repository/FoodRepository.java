package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Food;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del catálogo de alimentos.
 * <p>
 * Los alimentos no se borran: se desactivan, para que el historial de consumo
 * pueda seguir mostrándolos.
 *
 * @see Food
 */
public interface FoodRepository {

    /**
     * Guarda un alimento nuevo y le asigna su id.
     *
     * @param food alimento a guardar; su id se reemplaza por el que se genere
     * @return el mismo alimento, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    Food save(Food food) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de un alimento existente (se busca por su id).
     * También se usa para activar o desactivar un alimento.
     *
     * @param food alimento con los datos nuevos
     * @throws PersistenceExceptions si el alimento no existe o si falla el almacenamiento
     */
    void update(Food food) throws PersistenceExceptions;

    /**
     * Busca un alimento por su id, esté activo o no.
     *
     * @param foodId id del alimento buscado
     * @return el alimento, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<Food> findById(int foodId) throws PersistenceExceptions;

    /**
     * Devuelve los alimentos cuyo nombre contiene el texto, sin distinguir
     * mayúsculas ni tildes.
     * <p>
     * Incluye los desactivados (sirve también para detectar nombres repetidos);
     * quien arma listas de selección debe filtrar con {@code isActive()}.
     *
     * @param text texto a buscar dentro del nombre
     * @return alimentos que coinciden; vacía si no hay ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Food> findByName(String text) throws PersistenceExceptions;

    /**
     * Devuelve solo los alimentos activos: los que aparecen en las listas de
     * selección.
     *
     * @return lista de alimentos activos; vacía si no hay ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Food> findAllActive() throws PersistenceExceptions;

    /**
     * Devuelve todos los alimentos, incluidos los desactivados (el historial de
     * consumo los sigue mostrando).
     *
     * @return lista con todos los alimentos; vacía si el catálogo está vacío
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Food> findAll() throws PersistenceExceptions;
}
