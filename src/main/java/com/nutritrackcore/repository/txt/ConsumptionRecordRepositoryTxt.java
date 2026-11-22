package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ConsumptionRecordRepository} sobre
 * {@code consumption_records.txt} (tabla {@code RegistroConsumo}).
 * <p>
 * Cada línea es la <b>cabecera</b> de un registro con el formato
 * {@code id_registro|id_usuario|kcal_estimadas|nivel_estimacion|fecha_hora}.
 * Los alimentos y gramos de cada registro ya no van aquí: viven en
 * {@code RegistroConsumo_Alimento} y los maneja el
 * {@link ConsumptionRecordFoodRepository} que recibe el constructor. Este
 * repositorio lo usa para guardar comidas completas y para borrar en cascada. El campo {@code nivel_estimacion}
 * puede quedar vacío. Cada operación lee el archivo completo y, si modifica
 * datos, lo reescribe completo.
 */
public class ConsumptionRecordRepositoryTxt implements ConsumptionRecordRepository {

    /** Cabecera del archivo {@code registros_consumo.txt}. */
    public static final String HEADER =
            "#id_registro|id_usuario|kcal_estimadas|nivel_estimacion|fecha_hora";

    /**
     * Orden estándar de los registros: por fecha y hora (las nulas van primero)
     * y, en caso de empate, por id.
     */
    private static final Comparator<ConsumptionRecord> BY_DATE_TIME =
            Comparator.comparing(ConsumptionRecord::getDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparingInt(ConsumptionRecord::getRecordId);

    /** Acceso al archivo .txt de registros de consumo. */
    private final TxtFile file;

    /** Repositorio del detalle (alimentos de cada registro). */
    private final ConsumptionRecordFoodRepository foodRepository;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file           archivo {@code consumption_records.txt}
     * @param foodRepository repositorio de los alimentos de cada registro
     */
    public ConsumptionRecordRepositoryTxt(TxtFile file, ConsumptionRecordFoodRepository foodRepository) {
        this.file = file;
        this.foodRepository = foodRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * En .txt no hay transacciones: si falla un alimento, se borran los
     * alimentos ya guardados y la cabecera, y se relanza el error.
     */
    @Override
    public ConsumptionRecord save(ConsumptionRecord record, List<ConsumptionRecordByFood> foods)
            throws PersistenceExceptions {
        if (foods == null || foods.isEmpty()) {
            throw new PersistenceExceptions("Un registro de consumo debe tener al menos un alimento");
        }
        save(record);
        try {
            for (ConsumptionRecordByFood food : foods) {
                food.setRecordId(record.getRecordId());
                foodRepository.save(food);
            }
        } catch (PersistenceExceptions e) {
            try {                                            // deshacer lo que se alcanzó a guardar
                deleteById(record.getRecordId());
            } catch (PersistenceExceptions undoError) {
                e.addSuppressed(undoError);
            }
            throw e;
        }
        return record;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public ConsumptionRecord save(ConsumptionRecord record) throws PersistenceExceptions {
        List<ConsumptionRecord> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (ConsumptionRecord r : all) {
            ids.add(r.getRecordId());
        }
        record.setRecordId(IdGenerator.next(ids));
        all.add(record);
        write(all);
        return record;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe un registro con ese id
     */
    @Override
    public void update(ConsumptionRecord record) throws PersistenceExceptions {
        List<ConsumptionRecord> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRecordId() == record.getRecordId()) {
                all.set(i, record);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el registro con id " + record.getRecordId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ConsumptionRecord> findById(int recordId) throws PersistenceExceptions {
        for (ConsumptionRecord r : findAll()) {
            if (r.getRecordId() == recordId) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * El resultado se ordena por fecha y hora, y en caso de empate por id.
     */
    @Override
    public List<ConsumptionRecord> findByUserId(int userId) throws PersistenceExceptions {
        List<ConsumptionRecord> result = new ArrayList<>();
        for (ConsumptionRecord r : findAll()) {
            if (r.getUserId() == userId) {
                result.add(r);
            }
        }
        result.sort(BY_DATE_TIME);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Los registros sin fecha y hora no coinciden con ningún día.
     */
    @Override
    public List<ConsumptionRecord> findByUserIdAndDate(int userId, LocalDate date) throws PersistenceExceptions {
        List<ConsumptionRecord> result = new ArrayList<>();
        for (ConsumptionRecord r : findByUserId(userId)) {
            if (r.getDateTime() != null && r.getDateTime().toLocalDate().equals(date)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Primero borra los alimentos del registro (equivale al
     * {@code ON DELETE CASCADE}) y solo reescribe el archivo si realmente se
     * eliminó algún registro.
     */
    @Override
    public void deleteById(int recordId) throws PersistenceExceptions {
        foodRepository.deleteByRecordId(recordId);
        List<ConsumptionRecord> all = findAll();
        if (all.removeIf(r -> r.getRecordId() == recordId)) {
            write(all);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Primero borra los alimentos de cada registro del usuario y solo
     * reescribe el archivo si realmente se eliminó algún registro.
     */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<ConsumptionRecord> all = findAll();
        for (ConsumptionRecord r : all) {
            if (r.getUserId() == userId) {
                foodRepository.deleteByRecordId(r.getRecordId());
            }
        }
        if (all.removeIf(r -> r.getUserId() == userId)) {
            write(all);
        }
    }

    // ---------- lectura y conversión ----------

    /**
     * Lee todos los registros del archivo, sin filtrar ni ordenar.
     *
     * @return lista con todos los registros de consumo
     * @throws PersistenceExceptions si alguna línea es inválida o está corrupta
     */
    private List<ConsumptionRecord> findAll() throws PersistenceExceptions {
        List<ConsumptionRecord> all = new ArrayList<>();
        for (String line : file.readLines()) {
            all.add(fromLine(line));
        }
        return all;
    }

    /**
     * Reescribe el archivo completo con los registros recibidos. Convierte todos
     * los registros a texto antes de tocar el archivo.
     *
     * @param records registros que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<ConsumptionRecord> records) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (ConsumptionRecord r : records) {
                lines.add(toLine(r));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte un registro en una línea del archivo. El nivel de estimación
     * nulo se escribe como campo vacío.
     *
     * @param r registro a convertir
     * @return la línea con los cinco campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(ConsumptionRecord r) {
        return TxtFormat.join(
                String.valueOf(r.getRecordId()),
                String.valueOf(r.getUserId()),
                String.valueOf(r.getEstimatedCalories()),
                TxtFormat.text(r.getEstimationLevel()),           // puede ser nulo -> campo vacío
                TxtFormat.dateTime(r.getDateTime()));
    }

    /**
     * Convierte una línea del archivo en un registro. Los campos vacíos de
     * nivel de estimación y fecha y hora quedan como {@code null}.
     *
     * @param line línea con el formato descrito en {@link ConsumptionRecordRepositoryTxt}
     * @return el registro leído
     * @throws PersistenceExceptions si la línea no tiene 5 campos o tiene valores inválidos
     */
    private ConsumptionRecord fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 5) {
            throw new PersistenceExceptions("Línea inválida en registros_consumo.txt: " + line);
        }
        try {
            ConsumptionRecord r = new ConsumptionRecord(
                    Integer.parseInt(c[1]),
                    Double.parseDouble(c[2]),
                    TxtFormat.parseDateTime(c[4]));
            r.setRecordId(Integer.parseInt(c[0]));
            r.setEstimationLevel(TxtFormat.parseDouble(c[3]));
            return r;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en registros_consumo.txt: " + line, e);
        }
    }
}
