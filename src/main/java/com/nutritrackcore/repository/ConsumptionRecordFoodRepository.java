package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del detalle de cada registro de consumo (tabla
 * {@code RegistroConsumo_Alimento}): qué alimentos se reconocieron en un
 * registro y cuántos gramos se estimaron de cada uno.
 * <p>
 * Tiene id propio autogenerado ({@code id_registro_alimento}) y además el par
 * registro + alimento no se puede repetir.
 *
 * Para registrar una comida completa conviene usar
 * {@link ConsumptionRecordRepository#save(com.nutritrackcore.model.ConsumptionRecord, List)},
 * que guarda cabecera y alimentos juntos.
 *
 * @see ConsumptionRecordByFood
 * @see ConsumptionRecordRepository
 */
public interface ConsumptionRecordFoodRepository {

    /**
     * Guarda una línea nueva y le asigna su id.
     *
     * @param line línea a guardar; su id se reemplaza por el que se genere
     * @return la misma línea, ya con el id asignado
     * @throws PersistenceExceptions si el registro ya tiene ese alimento o si falla el almacenamiento
     */
    ConsumptionRecordByFood save(ConsumptionRecordByFood line) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de una línea existente (se busca por su id).
     *
     * @param line línea con los datos nuevos
     * @throws PersistenceExceptions si la línea no existe o si falla el almacenamiento
     */
    void update(ConsumptionRecordByFood line) throws PersistenceExceptions;

    /**
     * Busca una línea por su id.
     *
     * @param id id de la línea
     * @return la línea, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<ConsumptionRecordByFood> findById(int id) throws PersistenceExceptions;

    /**
     * Devuelve todos los alimentos de un registro de consumo.
     *
     * @param recordId id del registro
     * @return lista de líneas; vacía si el registro no tiene alimentos
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<ConsumptionRecordByFood> findByRecordId(int recordId) throws PersistenceExceptions;

    /**
     * Elimina una línea por su id. No falla si no existe.
     *
     * @param id id de la línea
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteById(int id) throws PersistenceExceptions;

    /**
     * Elimina todas las líneas de un registro. Equivale al
     * {@code ON DELETE CASCADE} de la base de datos. No falla si no hay líneas.
     *
     * @param recordId id del registro
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByRecordId(int recordId) throws PersistenceExceptions;
}
