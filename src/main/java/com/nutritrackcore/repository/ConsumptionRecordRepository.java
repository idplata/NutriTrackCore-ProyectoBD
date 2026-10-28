package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de los registros de consumo de alimentos.
 * <p>
 * Cada registro indica qué alimento comió un usuario, cuántos gramos estimados
 * y en qué momento. No conoce el formato de almacenamiento: cualquier
 * implementación (.txt, JDBC) debe cumplirlo.
 *
 * @see ConsumptionRecord
 */
public interface ConsumptionRecordRepository {

    /**
     * Guarda un registro nuevo y le asigna su id.
     *
     * @param record registro a guardar; su id se reemplaza por el que se genere
     * @return el mismo registro, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    ConsumptionRecord save(ConsumptionRecord record) throws PersistenceExceptions;

    /**
     * Reemplaza los datos de un registro existente (se busca por su id).
     *
     * @param record registro con los datos nuevos
     * @throws PersistenceExceptions si el registro no existe o si falla el almacenamiento
     */
    void update(ConsumptionRecord record) throws PersistenceExceptions;

    /**
     * Busca un registro por su id.
     *
     * @param recordId id del registro buscado
     * @return el registro, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<ConsumptionRecord> findById(int recordId) throws PersistenceExceptions;

    /**
     * Devuelve todos los registros de un usuario, del más antiguo al más reciente.
     *
     * @param userId id del usuario
     * @return lista ordenada por fecha y hora; vacía si el usuario no tiene registros
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<ConsumptionRecord> findByUserId(int userId) throws PersistenceExceptions;

    /**
     * Devuelve los registros de un usuario en un día concreto, ordenados por
     * hora. Sirve para calcular el total diario.
     *
     * @param userId id del usuario
     * @param date   día a consultar
     * @return registros de ese día; vacía si no hay ninguno
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<ConsumptionRecord> findByUserIdAndDate(int userId, LocalDate date) throws PersistenceExceptions;

    /**
     * Elimina un registro por su id. No falla si el id no existe.
     *
     * @param recordId id del registro a eliminar
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteById(int recordId) throws PersistenceExceptions;

    /**
     * Elimina todos los registros de un usuario. Se usa para el borrado en
     * cascada al eliminar una cuenta. No falla si el usuario no tiene registros.
     *
     * @param userId id del usuario cuyos registros se eliminan
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int userId) throws PersistenceExceptions;
}
