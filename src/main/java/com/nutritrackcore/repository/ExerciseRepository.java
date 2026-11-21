package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Exercise;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del catálogo de ejercicios.
 * <p>
 * Los ejercicios no se borran: se desactivan, para que las rutinas antiguas
 * puedan seguir mostrándolos.
 *
 * @see Exercise
 */
public interface ExerciseRepository {

    /**
     * Guarda un ejercicio nuevo y le asigna su id.
     *
     * @param exercise ejercicio a guardar; su id se reemplaza por el que se genere
     * @return el mismo ejercicio, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    Exercise save(Exercise exercise) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de un ejercicio existente (se busca por su id).
     * También se usa para activar o desactivar un ejercicio.
     *
     * @param exercise ejercicio con los datos nuevos
     * @throws PersistenceExceptions si el ejercicio no existe o si falla el almacenamiento
     */
    void update(Exercise exercise) throws PersistenceExceptions;

    /**
     * Busca un ejercicio por su id, esté activo o no.
     *
     * @param exerciseId id del ejercicio buscado
     * @return el ejercicio, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<Exercise> findById(int exerciseId) throws PersistenceExceptions;

    /**
     * Devuelve solo los ejercicios activos: los que aparecen en las listas de
     * selección.
     *
     * @return lista de ejercicios activos; vacía si no hay ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Exercise> findAllActive() throws PersistenceExceptions;

    /**
     * Devuelve todos los ejercicios, incluidos los desactivados (las rutinas
     * antiguas los siguen mostrando).
     *
     * @return lista con todos los ejercicios; vacía si el catálogo está vacío
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<Exercise> findAll() throws PersistenceExceptions;
}
