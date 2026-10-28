package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import com.nutritrackcore.services.dto.BmiResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso relacionados con el índice de masa corporal (IMC): valor actual,
 * historial y evolución en el tiempo.
 * <p>
 * El IMC no se almacena en ningún archivo: este servicio siempre lo calcula a
 * partir del peso y la altura con {@link BmiResult#calculate(double, double)}.
 */
public class BmiService {

    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository       repositorio de usuarios
     * @param bmiHistoryRepository repositorio del historial de IMC
     * @throws IllegalArgumentException si alguna dependencia es nula
     */
    public BmiService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository) {
        this.userRepository = ParameterChecks.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = ParameterChecks.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
    }

    /**
     * Calcula el IMC actual de un usuario con el peso y la altura que tiene
     * guardados en su perfil.
     *
     * @param userId identificador del usuario
     * @return valor del IMC y su categoría
     * @throws IllegalArgumentException si el id es inválido, el usuario no existe o sus medidas están fuera de rango
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public BmiResult getCurrentBmi(int userId) throws PersistenceExceptions {
        User user = requireUser(userId);
        return BmiResult.calculate(user.getWeightKg(), user.getHeightCm());
    }

    /**
     * Calcula el IMC de una fila del historial (para mostrar el IMC de cada fila
     * en una tabla).
     *
     * @param history fila del historial
     * @return valor del IMC y su categoría
     * @throws IllegalArgumentException si la fila es nula o sus medidas están fuera de rango
     */
    public BmiResult calculateBmi(BMIHistory history) {
        ParameterChecks.requireNonNull(history, "fila del historial");
        return BmiResult.calculate(history.getWeightKg(), history.getHeightCm());
    }

    /**
     * Devuelve todo el historial de peso/altura de un usuario, de la fila más
     * antigua a la más reciente.
     *
     * @param userId identificador del usuario
     * @return historial ordenado; vacío si el usuario aún no tiene filas
     * @throws IllegalArgumentException si el id es inválido o el usuario no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<BMIHistory> getHistory(int userId) throws PersistenceExceptions {
        requireUser(userId);
        return bmiHistoryRepository.findByUserId(userId);
    }

    /**
     * Devuelve las filas del historial cuya fecha cae dentro de un rango, para
     * ver cómo cambió el IMC a lo largo de los meses.
     * <p>
     * Pasos: (1) valida id y rango (ninguna fecha nula y la inicial no posterior
     * a la final); (2) carga el historial completo; (3) conserva solo las filas
     * cuya fecha (sin la hora) está entre {@code from} y {@code to}, ambas
     * incluidas.
     *
     * @param userId identificador del usuario
     * @param from   fecha inicial (incluida)
     * @param to     fecha final (incluida)
     * @return filas dentro del rango, de la más antigua a la más reciente
     * @throws IllegalArgumentException si el id o el rango son inválidos o el usuario no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<BMIHistory> getEvolution(int userId, LocalDate from, LocalDate to) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireDateRange(from, to);
        requireUser(userId);

        List<BMIHistory> result = new ArrayList<>();
        for (BMIHistory row : bmiHistoryRepository.findByUserId(userId)) {
            if (row.getRegisterDate() == null) {
                continue;
            }
            LocalDate day = row.getRegisterDate().toLocalDate();
            if (!day.isBefore(from) && !day.isAfter(to)) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Valida el id y carga el usuario; es el paso común de los métodos públicos.
     *
     * @param userId identificador del usuario
     * @return el usuario encontrado
     * @throws IllegalArgumentException si el id es inválido o el usuario no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    private User requireUser(int userId) throws PersistenceExceptions {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("No existe un usuario con el identificador " + userId + "."));
    }
}