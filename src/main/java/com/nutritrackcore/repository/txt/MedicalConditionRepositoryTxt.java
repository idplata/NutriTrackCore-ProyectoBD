package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.repository.MedicalConditionRepository;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link MedicalConditionRepository} sobre
 * {@code condiciones_medicas.txt}.
 * <p>
 * Cada línea del archivo representa una condición con el formato
 * {@code id_condicion|nombre|descripcion|activa}. Cada operación lee el
 * archivo completo y, si modifica datos, lo reescribe completo.
 */
public class MedicalConditionRepositoryTxt implements MedicalConditionRepository {

    /** Cabecera del archivo {@code condiciones_medicas.txt}. */
    public static final String HEADER = "#id_condicion|nombre|descripcion|activa";

    /** Acceso al archivo .txt de condiciones médicas. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code condiciones_medicas.txt}
     */
    public MedicalConditionRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public MedicalCondition save(MedicalCondition condition) throws PersistenceExceptions {
        List<MedicalCondition> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (MedicalCondition c : all) {
            ids.add(c.getConditionId());
        }
        condition.setConditionId(IdGenerator.next(ids));
        all.add(condition);
        write(all);
        return condition;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe una condición con ese id
     */
    @Override
    public void update(MedicalCondition condition) throws PersistenceExceptions {
        List<MedicalCondition> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getConditionId() == condition.getConditionId()) {
                all.set(i, condition);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe la condición con id " + condition.getConditionId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<MedicalCondition> findById(int conditionId) throws PersistenceExceptions {
        for (MedicalCondition c : findAll()) {
            if (c.getConditionId() == conditionId) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public List<MedicalCondition> findAllActive() throws PersistenceExceptions {
        List<MedicalCondition> result = new ArrayList<>();
        for (MedicalCondition c : findAll()) {
            if (c.isActive()) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si alguna línea del archivo es inválida o está corrupta
     */
    @Override
    public List<MedicalCondition> findAll() throws PersistenceExceptions {
        List<MedicalCondition> all = new ArrayList<>();
        for (String line : file.readLines()) {
            all.add(fromLine(line));
        }
        return all;
    }

    // ---------- conversión ----------

    /**
     * Reescribe el archivo completo con la lista de condiciones recibida.
     * Convierte todas las condiciones a texto antes de tocar el archivo.
     *
     * @param conditions condiciones que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<MedicalCondition> conditions) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (MedicalCondition c : conditions) {
                lines.add(toLine(c));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte una condición en una línea del archivo.
     *
     * @param c condición a convertir
     * @return la línea con los cuatro campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(MedicalCondition c) {
        return TxtFormat.join(
                String.valueOf(c.getConditionId()),
                c.getName(),
                c.getDescription(),
                String.valueOf(c.isActive()));
    }

    /**
     * Convierte una línea del archivo en una condición.
     *
     * @param line línea con el formato {@code id_condicion|nombre|descripcion|activa}
     * @return la condición leída; la descripción vacía queda como {@code null}
     * @throws PersistenceExceptions si la línea no tiene 4 campos o tiene valores inválidos
     */
    private MedicalCondition fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 4) {
            throw new PersistenceExceptions("Línea inválida en condiciones_medicas.txt: " + line);
        }
        try {
            MedicalCondition m = new MedicalCondition();
            m.setConditionId(Integer.parseInt(c[0]));
            m.setName(c[1]);
            m.setDescription(TxtFormat.nullIfEmpty(c[2]));
            m.setActive(TxtFormat.parseBoolean(c[3]));
            return m;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en condiciones_medicas.txt: " + line, e);
        }
    }
}
