package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.ExerciseByReps;
import com.nutritrackcore.model.ExerciseRoutine;
import com.nutritrackcore.model.ExerciseRoutineExercise;
import com.nutritrackcore.model.RoutineFollowUp;
import com.nutritrackcore.repository.ExerciseRoutineRepository;
import com.nutritrackcore.repository.RoutineFollowUpRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Seguimiento de la rutina: el usuario indica si completó la sesión y cuántos minutos duró.
 * <p>
 * Progresión: si en los últimos 14 días hubo al menos 6 sesiones y todas se completaron,
 * se suma una repetición a cada ejercicio por repeticiones (máximo una vez cada 14 días
 * y sin pasar el límite de repeticiones del usuario).
 */
public class RoutineFollowUpService {
    static final String PROGRESSION_MARK = "[+1 rep]";
    private static final int WINDOW_DAYS = 14;
    private static final int MIN_SESSIONS = 6;
    private static final int MAX_NOTES_LENGTH = 500;
    private static final int MAX_MINUTES = 600;

    private final RoutineFollowUpRepository followUpRepository;
    private final ExerciseRoutineRepository routineRepository;
    private final ExerciseRoutineService routineService;

    public RoutineFollowUpService(RoutineFollowUpRepository followUpRepository,
                                  ExerciseRoutineRepository routineRepository,
                                  ExerciseRoutineService routineService) {
        this.followUpRepository = Objects.requireNonNull(followUpRepository, "repositorio de seguimiento");
        this.routineRepository = Objects.requireNonNull(routineRepository, "repositorio de rutinas");
        this.routineService = Objects.requireNonNull(routineService, "servicio de rutinas");
    }

    /**
     * Registra una sesión de la rutina activa del usuario.
     *
     * @param completed {@code true} si la terminó
     * @param minutes   minutos que duró (0 si no la hizo)
     * @return la sesión guardada; sus observaciones incluyen "[+1 rep]" si se aplicó la progresión
     */
    public RoutineFollowUp registerSession(int userId, LocalDate sessionDate, boolean completed, int minutes,
                                           String observations) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireNotFuture(sessionDate, "fecha de la sesión");
        ParameterChecks.requireRange(minutes, 0, MAX_MINUTES, "minutos de la sesión");
        if (completed && minutes == 0) {
            throw new BussinesRuleException("Una sesión completada debe durar al menos un minuto.");
        }
        String notes = ParameterChecks.requireOptionalText(observations, MAX_NOTES_LENGTH, "observaciones");
        ExerciseRoutine routine = routineRepository.findActiveByUserId(userId).orElseThrow(
                () -> new NotFoundEntityException("El usuario no tiene una rutina activa."));
        if (sessionDate.isBefore(routine.getGenerationDate())) {
            throw new BussinesRuleException("La sesión no puede ser anterior a la fecha de la rutina.");
        }

        RoutineFollowUp followUp = followUpRepository.save(
                new RoutineFollowUp(routine.getRoutineId(), sessionDate, minutes, completed, notes));
        if (applyProgression(userId, routine, sessionDate)) {
            followUp.setObservations(notes == null ? PROGRESSION_MARK : notes + " " + PROGRESSION_MARK);
            followUpRepository.update(followUp);
        }
        return followUp;
    }

    public List<RoutineFollowUp> listByRoutine(int routineId) throws NutriTrackException {
        ParameterChecks.requireId(routineId, "identificador de la rutina");
        return followUpRepository.findByRoutineId(routineId);
    }

    /** Suma una repetición si se cumplen las condiciones de la progresión. */
    private boolean applyProgression(int userId, ExerciseRoutine routine, LocalDate today) throws NutriTrackException {
        LocalDate from = today.minusDays(WINDOW_DAYS - 1);
        int sessions = 0;
        for (RoutineFollowUp session : followUpRepository.findByRoutineId(routine.getRoutineId())) {
            LocalDate date = session.getSessionDate();
            if (date == null || date.isBefore(from) || date.isAfter(today)) {
                continue;
            }
            if (!session.isCompleted()) {
                return false;
            }
            if (session.getObservations() != null && session.getObservations().contains(PROGRESSION_MARK)) {
                return false;                                    // ya subió en estos 14 días
            }
            sessions++;
        }
        if (sessions < MIN_SESSIONS) {
            return false;
        }
        Integer max = routineService.maxRepetitionsFor(userId);
        boolean changed = false;
        for (ExerciseRoutineExercise line : routine.getLines()) {
            if (line instanceof ExerciseByReps) {
                ExerciseByReps reps = (ExerciseByReps) line;
                if (max == null || reps.getReps() < max) {
                    reps.setReps(reps.getReps() + 1);
                    changed = true;
                }
            }
        }
        if (changed) {
            routineRepository.update(routine);                   // reescribe las líneas en una transacción
        }
        return changed;
    }
}