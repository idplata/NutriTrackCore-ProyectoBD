package com.nutritrackcore.repository;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del historial de peso/altura (IMC).
 * <p>
 * El historial es un registro: solo se agregan filas, no se editan ni se
 * modifican una vez guardadas. Por eso esta interfaz no ofrece un método
 * {@code update}.
 * <p>
 * No conoce archivos ni bases de datos: cualquier implementación (.txt, JDBC)
 * debe cumplir este contrato.
 *
 * @see BMIHistory
 */
public interface BMIHistoryRepository {

    /**
     * Guarda una fila nueva en el historial y le asigna su id.
     *
     * @param history fila a guardar; su id se reemplaza por el que se genere
     * @return la misma fila, ya con el id asignado
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    BMIHistory save(BMIHistory history) throws PersistenceExceptions;

    /**
     * Busca una fila del historial por su id.
     *
     * @param idHistory id de la fila buscada
     * @return la fila, o {@link Optional#empty()} si no existe
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<BMIHistory> findById(int idHistory) throws PersistenceExceptions;

    /**
     * Devuelve el historial completo de un usuario, de la fila más antigua
     * a la más reciente.
     *
     * @param idUser id del usuario
     * @return lista ordenada por fecha de registro; vacía si el usuario no tiene filas
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    List<BMIHistory> findByUserId(int idUser) throws PersistenceExceptions;

    /**
     * Devuelve la fila más reciente de un usuario. Es la que usa la rutina de
     * ejercicio como {@code id_historial_base}.
     *
     * @param idUser id del usuario
     * @return la fila más reciente, o {@link Optional#empty()} si el usuario no tiene historial
     * @throws PersistenceExceptions si no se puede leer el almacenamiento
     */
    Optional<BMIHistory> findLatestByUserId(int idUser) throws PersistenceExceptions;

    /**
     * Elimina todo el historial de un usuario. Se usa para el borrado en
     * cascada al eliminar una cuenta. No falla si el usuario no tiene filas.
     *
     * @param idUser id del usuario cuyo historial se elimina
     * @throws PersistenceExceptions si no se puede leer o escribir el almacenamiento
     */
    void deleteByUserId(int idUser) throws PersistenceExceptions;
}
