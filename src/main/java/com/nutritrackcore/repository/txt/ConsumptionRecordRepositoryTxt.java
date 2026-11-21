package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.repository.ConsumptionRecordRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ConsumptionRecordRepository} sobre
 * {@code registros_consumo.txt}.
 * <p>
 * Cada línea del archivo representa un registro de consumo con el formato
 * {@code id_registro|id_usuario|id_alimento|imagen_ref|nivel_estimacion|gramos_estimados|fecha_hora}.
 * Los campos {@code imagen_ref} y {@code nivel_estimacion} quedan vacíos hasta
 * que exista la captura por cámara. Cada operación lee el archivo completo y, si
 * modifica datos, lo reescribe completo.
 */
public class ConsumptionRecordRepositoryTxt implements ConsumptionRecordRepository {

    /** Cabecera del archivo {@code registros_consumo.txt}. */
    public static final String HEADER =
            "#id_registro|id_usuario|id_alimento|imagen_ref|nivel_estimacion|gramos_estimados|fecha_hora";

    /**
     * Orden estándar de los registros: por fecha y hora (las nulas van primero)
     * y, en caso de empate, por id.
     */
    private static final Comparator<ConsumptionRecord> BY_DATE_TIME =
            Comparator.comparing(ConsumptionRecord::getDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparingInt(ConsumptionRecord::getRecordId);

    /** Acceso al archivo .txt de registros de consumo. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code registros_consumo.txt}
     */
    public ConsumptionRecordRepositoryTxt(TxtFile file) {
        this.file = file;
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
     * Solo reescribe el archivo si realmente se eliminó algún registro.
     */
    @Override
    public void deleteById(int recordId) throws PersistenceExceptions {
        List<ConsumptionRecord> all = findAll();
        if (all.removeIf(r -> r.getRecordId() == recordId)) {
            write(all);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe el archivo si realmente se eliminó algún registro.
     */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<ConsumptionRecord> all = findAll();
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
     * Convierte un registro en una línea del archivo. La referencia de imagen y
     * el nivel de estimación nulos se escriben como campo vacío.
     *
     * @param r registro a convertir
     * @return la línea con los siete campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(ConsumptionRecord r) {
        return TxtFormat.join(
                String.valueOf(r.getRecordId()),
                String.valueOf(r.getUserId()),
                String.valueOf(r.getFoodId()),
                r.getImageRef(),                                  // vacío hasta que exista la cámara
                TxtFormat.text(r.getEstimationLevel()),           // vacío hasta que exista la cámara
                String.valueOf(r.getEstimatedGrams()),
                TxtFormat.dateTime(r.getDateTime()));
    }

    /**
     * Convierte una línea del archivo en un registro de consumo. Los campos
     * vacíos de imagen, nivel de estimación y fecha y hora quedan como
     * {@code null}.
     *
     * @param line línea con el formato descrito en {@link ConsumptionRecordRepositoryTxt}
     * @return el registro leído
     * @throws PersistenceExceptions si la línea no tiene 7 campos o tiene valores inválidos
     */
    private ConsumptionRecord fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 7) {
            throw new PersistenceExceptions("Línea inválida en registros_consumo.txt: " + line);
        }
        try {
            ConsumptionRecord r = new ConsumptionRecord();
            r.setRecordId(Integer.parseInt(c[0]));
            r.setUserId(Integer.parseInt(c[1]));
            r.setFoodId(Integer.parseInt(c[2]));
            r.setImageRef(TxtFormat.nullIfEmpty(c[3]));
            r.setEstimationLevel(TxtFormat.parseDouble(c[4]));
            r.setEstimatedGrams(Double.parseDouble(c[5]));
            r.setDateTime(TxtFormat.parseDateTime(c[6]));
            return r;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en registros_consumo.txt: " + line, e);
        }
    }
}
