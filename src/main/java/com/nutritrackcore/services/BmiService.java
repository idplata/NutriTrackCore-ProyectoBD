package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.BMIHistoryRepository;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * IMC actual, historial y evolución de un usuario.
 */
public class BmiService {
    private final UserRepository userRepository;
    private final BMIHistoryRepository bmiHistoryRepository;

    public BmiService(UserRepository userRepository, BMIHistoryRepository bmiHistoryRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.bmiHistoryRepository = Objects.requireNonNull(bmiHistoryRepository, "repositorio de historial IMC");
    }

    public BmiResult getCurrentBmi(int userId) throws NutriTrackException {
        User user = requireUser(userId);
        return BmiResult.calculate(user.getWeightKg(), user.getHeightCm());
    }

    public BmiResult calculateBmi(BMIHistory history) throws ValidationException {
        ParameterChecks.requireNonNull(history, "fila del historial");
        return BmiResult.calculate(history.getWeightKg(), history.getHeightCm());
    }

    public List<BMIHistory> getHistory(int userId) throws NutriTrackException {
        requireUser(userId);
        return bmiHistoryRepository.findByUserId(userId);
    }

    public List<BMIHistory> getEvolution(int userId, LocalDate from, LocalDate to) throws NutriTrackException {
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

    private User requireUser(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundEntityException("No existe un usuario con el identificador " + userId + "."));
    }
}