package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.repository.BMIHistoryRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link BMIHistoryRepository} sobre {@code historial_imc.txt}.
 * <p>
 * Cada línea del archivo representa una fila del historial con el formato
 * {@code id_historial|id_usuario|peso_kg|altura_cm|fecha_registro}. Cada
 * operación lee el archivo completo y, si modifica datos, lo reescribe completo.
 */
public class BMIHistoryRepositoryTxt implements BMIHistoryRepository {

    /** Cabecera del archivo {@code historial_imc.txt}. */
    public static final String HEADER =
            "#id_historial|id_usuario|peso_kg|altura_cm|fecha_registro";

    /** Acceso al archivo .txt del historial de IMC. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code historial_imc.txt}
     */
    public BMIHistoryRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public BMIHistory save(BMIHistory history) throws PersistenceExceptions {
        List<BMIHistory> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (BMIHistory h : all) {
            ids.add(h.getIdHistory());
        }
        history.setIdHistory(IdGenerator.next(ids));
        all.add(history);
        write(all);
        return history;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<BMIHistory> findById(int idHistory) throws PersistenceExceptions {
        for (BMIHistory h : findAll()) {
            if (h.getIdHistory() == idHistory) {
                return Optional.of(h);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ordena por fecha de registro (las fechas nulas van primero) y, en caso de
     * empate, por id.
     */
    @Override
    public List<BMIHistory> findByUserId(int idUser) throws PersistenceExceptions {
        List<BMIHistory> result = new ArrayList<>();
        for (BMIHistory h : findAll()) {
            if (h.getIdUser() == idUser) {
                result.add(h);
            }
        }
        result.sort(Comparator.comparing(BMIHistory::getRegisterDate,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparingInt(BMIHistory::getIdHistory));
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Toma la última fila de {@link #findByUserId(int)}, que ya viene ordenada.
     */
    @Override
    public Optional<BMIHistory> findLatestByUserId(int idUser) throws PersistenceExceptions {
        List<BMIHistory> rows = findByUserId(idUser);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(rows.size() - 1));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe el archivo si realmente se eliminó alguna fila.
     */
    @Override
    public void deleteByUserId(int idUser) throws PersistenceExceptions {
        List<BMIHistory> all = findAll();
        if (all.removeIf(h -> h.getIdUser() == idUser)) {
            write(all);
        }
    }

    // ---------- lectura y conversión ----------

    /**
     * Lee todas las filas del archivo, sin filtrar ni ordenar.
     *
     * @return lista con todas las filas del historial
     * @throws PersistenceExceptions si alguna línea es inválida o está corrupta
     */
    private List<BMIHistory> findAll() throws PersistenceExceptions {
        List<BMIHistory> all = new ArrayList<>();
        for (String line : file.readLines()) {
            all.add(fromLine(line));
        }
        return all;
    }

    /**
     * Reescribe el archivo completo con las filas recibidas. Convierte todas las
     * filas a texto antes de tocar el archivo.
     *
     * @param rows filas que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar o si falla la escritura
     */
    private void write(List<BMIHistory> rows) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (BMIHistory h : rows) {
                lines.add(toLine(h));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte una fila del historial en una línea del archivo. La fecha de
     * registro se escribe en ISO-8601 a segundos.
     *
     * @param h fila a convertir
     * @return la línea con los cinco campos separados por {@code "|"}
     */
    private String toLine(BMIHistory h) {
        return TxtFormat.join(
                String.valueOf(h.getIdHistory()),
                String.valueOf(h.getIdUser()),
                String.valueOf(h.getWeightKg()),
                String.valueOf(h.getHeightCm()),
                TxtFormat.dateTime(h.getRegisterDate()));
    }

    /**
     * Convierte una línea del archivo en una fila del historial. La fecha de
     * registro vacía queda como {@code null}.
     *
     * @param line línea con el formato
     *             {@code id_historial|id_usuario|peso_kg|altura_cm|fecha_registro}
     * @return la fila leída
     * @throws PersistenceExceptions si la línea no tiene 5 campos o tiene valores inválidos
     */
    private BMIHistory fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 5) {
            throw new PersistenceExceptions("Línea inválida en historial_imc.txt: " + line);
        }
        try {
            BMIHistory h = new BMIHistory();
            h.setIdHistory(Integer.parseInt(c[0]));
            h.setIdUser(Integer.parseInt(c[1]));
            h.setWeightKg(Double.parseDouble(c[2]));
            h.setHeightCm(Double.parseDouble(c[3]));
            String date = TxtFormat.nullIfEmpty(c[4]);
            h.setRegisterDate(date == null ? null : LocalDateTime.parse(date));
            return h;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en historial_imc.txt: " + line, e);
        }
    }
}
