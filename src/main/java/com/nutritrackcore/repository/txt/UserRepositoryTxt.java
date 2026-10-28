package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Sex;
import com.nutritrackcore.model.User;
import com.nutritrackcore.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link UserRepository} sobre {@code usuarios.txt}.
 * <p>
 * Cada línea del archivo representa un usuario con el formato
 * {@code id_usuario|nombre|email|contrasena_hash|peso_kg|altura_cm|fecha_nacimiento|sexo|fecha_registro}.
 * Cada operación lee el archivo completo y, si modifica datos, lo reescribe
 * completo.
 */
public class UserRepositoryTxt implements UserRepository {

    /** Cabecera del archivo {@code usuarios.txt}. */
    public static final String HEADER =
            "#id_usuario|nombre|email|contrasena_hash|peso_kg|altura_cm|fecha_nacimiento|sexo|fecha_registro";

    /** Acceso al archivo .txt de usuarios. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code usuarios.txt}
     */
    public UserRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public User save(User user) throws PersistenceExceptions {
        List<User> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (User u : all) {
            ids.add(u.getIdUser());
        }
        user.setIdUser(IdGenerator.next(ids));
        all.add(user);
        write(all);
        return user;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe un usuario con ese id
     */
    @Override
    public void update(User user) throws PersistenceExceptions {
        List<User> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getIdUser() == user.getIdUser()) {
                all.set(i, user);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el usuario con id " + user.getIdUser());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findById(int idUser) throws PersistenceExceptions {
        for (User u : findAll()) {
            if (u.getIdUser() == idUser) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * La comparación del email no distingue mayúsculas de minúsculas.
     */
    @Override
    public Optional<User> findByEmail(String email) throws PersistenceExceptions {
        for (User u : findAll()) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si alguna línea del archivo es inválida o está corrupta
     */
    @Override
    public List<User> findAll() throws PersistenceExceptions {
        List<User> users = new ArrayList<>();
        for (String line : file.readLines()) {
            users.add(fromLine(line));
        }
        return users;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe el archivo si realmente se eliminó algún usuario.
     */
    @Override
    public void deleteById(int idUser) throws PersistenceExceptions {
        List<User> all = findAll();
        if (all.removeIf(u -> u.getIdUser() == idUser)) {
            write(all);
        }
    }

    // ---------- conversión User <-> línea ----------

    /**
     * Reescribe el archivo completo con la lista de usuarios recibida.
     * Convierte todos los usuarios a texto antes de tocar el archivo.
     *
     * @param users usuarios que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<User> users) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (User u : users) {
                lines.add(toLine(u));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte un usuario en una línea del archivo. La fecha de nacimiento, el
     * sexo y la fecha de registro nulos se escriben como campo vacío.
     *
     * @param u usuario a convertir
     * @return la línea con los nueve campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(User u) {
        return TxtFormat.join(
                String.valueOf(u.getIdUser()),
                u.getName(),
                u.getEmail(),
                u.getPasswordHash(),
                String.valueOf(u.getWeightKg()),
                String.valueOf(u.getHeightCm()),
                u.getBirthDate() == null ? null : u.getBirthDate().toString(),
                u.getSex() == null ? null : u.getSex().getCode(),
                TxtFormat.dateTime(u.getRegisterDate()));
    }

    /**
     * Convierte una línea del archivo en un usuario. Los campos vacíos de
     * fecha de nacimiento, sexo y fecha de registro quedan como {@code null}.
     *
     * @param line línea con el formato descrito en {@link UserRepositoryTxt}
     * @return el usuario leído
     * @throws PersistenceExceptions si la línea no tiene 9 campos o tiene valores inválidos
     */
    private User fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 9) {
            throw new PersistenceExceptions("Línea inválida en usuarios.txt: " + line);
        }
        try {
            User u = new User();                       // el constructor vacío no toca registerDate
            u.setIdUser(Integer.parseInt(c[0]));
            u.setName(c[1]);
            u.setEmail(c[2]);
            u.setPasswordHash(c[3]);
            u.setWeightKg(Double.parseDouble(c[4]));
            u.setHeightCm(Double.parseDouble(c[5]));
            String birth = TxtFormat.nullIfEmpty(c[6]);
            u.setBirthDate(birth == null ? null : LocalDate.parse(birth));
            String sex = TxtFormat.nullIfEmpty(c[7]);
            u.setSex(sex == null ? null : Sex.fromCode(sex));
            String reg = TxtFormat.nullIfEmpty(c[8]);
            u.setRegisterDate(reg == null ? null : LocalDateTime.parse(reg));
            return u;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en usuarios.txt: " + line, e);
        }
    }
}
