package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.UserCondition;
import com.nutritrackcore.repository.UserConditionRepository;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link UserConditionRepository} sobre
 * {@code usuario_condicion.txt}.
 * <p>
 * Cada línea del archivo representa una condición asignada a un usuario con el
 * formato {@code id_usuario|id_condicion|fecha_diagnostico|observaciones}. La
 * clave es compuesta (usuario + condición), por lo que no hay id propio. Cada
 * operación lee el archivo completo y, si modifica datos, lo reescribe completo.
 */
public class UserConditionRepositoryTxt implements UserConditionRepository {

    /** Cabecera del archivo {@code usuario_condicion.txt}. */
    public static final String HEADER = "#id_usuario|id_condicion|fecha_diagnostico|observaciones";

    /** Acceso al archivo .txt de condiciones de usuario. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code usuario_condicion.txt}
     */
    public UserConditionRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si el usuario ya tiene esa condición
     */
    @Override
    public void save(UserCondition userCondition) throws PersistenceExceptions {
        List<UserCondition> all = findAll();
        for (UserCondition uc : all) {
            if (sameKey(uc, userCondition.getUserId(), userCondition.getConditionId())) {
                throw new PersistenceExceptions("El usuario " + userCondition.getUserId()
                        + " ya tiene la condición " + userCondition.getConditionId());
            }
        }
        all.add(userCondition);
        write(all);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si el usuario no tiene esa condición
     */
    @Override
    public void update(UserCondition userCondition) throws PersistenceExceptions {
        List<UserCondition> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (sameKey(all.get(i), userCondition.getUserId(), userCondition.getConditionId())) {
                all.set(i, userCondition);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("El usuario " + userCondition.getUserId()
                + " no tiene la condición " + userCondition.getConditionId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserCondition> find(int userId, int conditionId) throws PersistenceExceptions {
        for (UserCondition uc : findAll()) {
            if (sameKey(uc, userId, conditionId)) {
                return Optional.of(uc);
            }
        }
        return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public List<UserCondition> findByUserId(int userId) throws PersistenceExceptions {
        List<UserCondition> result = new ArrayList<>();
        for (UserCondition uc : findAll()) {
            if (uc.getUserId() == userId) {
                result.add(uc);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe el archivo si realmente se eliminó alguna fila.
     */
    @Override
    public void delete(int userId, int conditionId) throws PersistenceExceptions {
        List<UserCondition> all = findAll();
        if (all.removeIf(uc -> sameKey(uc, userId, conditionId))) {
            write(all);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe el archivo si realmente se eliminó alguna fila.
     */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<UserCondition> all = findAll();
        if (all.removeIf(uc -> uc.getUserId() == userId)) {
            write(all);
        }
    }

    /**
     * Indica si una fila corresponde a la clave compuesta indicada.
     *
     * @param uc          fila a comparar
     * @param userId      id de usuario buscado
     * @param conditionId id de condición buscado
     * @return {@code true} si ambos ids coinciden con los de la fila
     */
    private static boolean sameKey(UserCondition uc, int userId, int conditionId) {
        return uc.getUserId() == userId && uc.getConditionId() == conditionId;
    }

    // ---------- lectura y conversión ----------

    /**
     * Lee todas las filas del archivo.
     *
     * @return lista con todos los pares usuario-condición; vacía si el archivo no tiene datos
     * @throws PersistenceExceptions si alguna línea es inválida o está corrupta
     */
    private List<UserCondition> findAll() throws PersistenceExceptions {
        List<UserCondition> all = new ArrayList<>();
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
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<UserCondition> rows) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (UserCondition uc : rows) {
                lines.add(toLine(uc));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte una fila en una línea del archivo. La fecha de diagnóstico y
     * las observaciones nulas se escriben como campo vacío.
     *
     * @param uc fila a convertir
     * @return la línea con los cuatro campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(UserCondition uc) {
        return TxtFormat.join(
                String.valueOf(uc.getUserId()),
                String.valueOf(uc.getConditionId()),
                TxtFormat.text(uc.getDiagnosisDate()),
                uc.getObservations());
    }

    /**
     * Convierte una línea del archivo en un par usuario-condición. Los campos
     * vacíos de fecha de diagnóstico y observaciones quedan como {@code null}.
     *
     * @param line línea con el formato
     *             {@code id_usuario|id_condicion|fecha_diagnostico|observaciones}
     * @return la fila leída
     * @throws PersistenceExceptions si la línea no tiene 4 campos o tiene valores inválidos
     */
    private UserCondition fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 4) {
            throw new PersistenceExceptions("Línea inválida en usuario_condicion.txt: " + line);
        }
        try {
            UserCondition uc = new UserCondition();
            uc.setUserId(Integer.parseInt(c[0]));
            uc.setConditionId(Integer.parseInt(c[1]));
            uc.setDiagnosisDate(TxtFormat.parseDate(c[2]));
            uc.setObservations(TxtFormat.nullIfEmpty(c[3]));
            return uc;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en usuario_condicion.txt: " + line, e);
        }
    }
}
