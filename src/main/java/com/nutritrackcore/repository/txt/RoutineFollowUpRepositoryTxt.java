package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.RoutineFollowUp;
import com.nutritrackcore.repository.RoutineFollowUpRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link RoutineFollowUpRepository} sobre
 * {@code routine_follow_ups.txt} (tabla {@code SeguimientoRutina}).
 * <p>
 * Formato: {@code id_seguimiento|id_rutina|fecha_sesion|duracion_min|completada|observaciones}.
 */
public class RoutineFollowUpRepositoryTxt extends AbstractTxtRepository<RoutineFollowUp>
        implements RoutineFollowUpRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_seguimiento|id_rutina|fecha_sesion|duracion_min|completada|observaciones";

    /** Orden por fecha de sesión (las nulas primero) y luego por id. */
    private static final Comparator<RoutineFollowUp> BY_DATE =
            Comparator.comparing(RoutineFollowUp::getSessionDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparingInt(RoutineFollowUp::getFollowUpId);

    /**
     * @param file archivo {@code routine_follow_ups.txt}
     */
    public RoutineFollowUpRepositoryTxt(TxtFile file) {
        super(file, "routine_follow_ups.txt", 6);
    }

    @Override
    public RoutineFollowUp save(RoutineFollowUp followUp) throws PersistenceExceptions {
        List<RoutineFollowUp> all = readAll();
        followUp.setFollowUpId(nextId(all, RoutineFollowUp::getFollowUpId));
        all.add(followUp);
        writeAll(all);
        return followUp;
    }

    @Override
    public void update(RoutineFollowUp followUp) throws PersistenceExceptions {
        List<RoutineFollowUp> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getFollowUpId() == followUp.getFollowUpId()) {
                all.set(i, followUp);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el seguimiento con id " + followUp.getFollowUpId());
    }

    @Override
    public Optional<RoutineFollowUp> findById(int followUpId) throws PersistenceExceptions {
        for (RoutineFollowUp f : readAll()) {
            if (f.getFollowUpId() == followUpId) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<RoutineFollowUp> findByRoutineId(int routineId) throws PersistenceExceptions {
        List<RoutineFollowUp> result = new ArrayList<>();
        for (RoutineFollowUp f : readAll()) {
            if (f.getRoutineId() == routineId) {
                result.add(f);
            }
        }
        result.sort(BY_DATE);
        return result;
    }

    @Override
    public void deleteByRoutineId(int routineId) throws PersistenceExceptions {
        List<RoutineFollowUp> all = readAll();
        if (all.removeIf(f -> f.getRoutineId() == routineId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(RoutineFollowUp f) {
        return TxtFormat.join(
                String.valueOf(f.getFollowUpId()),
                String.valueOf(f.getRoutineId()),
                TxtFormat.text(f.getSessionDate()),
                String.valueOf(f.getDurationMinutes()),
                String.valueOf(f.isCompleted()),
                f.getObservations());
    }

    @Override
    protected RoutineFollowUp fromFields(String[] c) {
        RoutineFollowUp f = new RoutineFollowUp(
                Integer.parseInt(c[1]),
                TxtFormat.parseDate(c[2]),
                Integer.parseInt(c[3]),
                TxtFormat.parseBoolean(c[4]),
                TxtFormat.nullIfEmpty(c[5]));
        f.setFollowUpId(Integer.parseInt(c[0]));
        return f;
    }
}
