package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.FoodRepository;
import java.text.Normalizer;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementación de {@link FoodRepository} sobre {@code alimentos.txt}.
 * <p>
 * Cada línea del archivo representa un alimento con el formato
 * {@code id_alimento|nombre|densidad_g_cm3|calorias_100g|proteinas_100g|carbohidratos_100g|grasas_100g|activo}.
 * La densidad y los macronutrientes son opcionales (campo vacío = nulo). Cada
 * operación lee el archivo completo y, si modifica datos, lo reescribe completo.
 */
public class FoodRepositoryTxt implements FoodRepository {

    /** Cabecera del archivo {@code alimentos.txt}. */
    public static final String HEADER =
            "#id_alimento|nombre|densidad_g_cm3|calorias_100g|proteinas_100g|carbohidratos_100g|grasas_100g|activo";

    /** Acceso al archivo .txt de alimentos. */
    private final TxtFile file;

    /**
     * Crea el repositorio sobre un archivo ya preparado.
     *
     * @param file archivo {@code alimentos.txt}
     */
    public FoodRepositoryTxt(TxtFile file) {
        this.file = file;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1.
     */
    @Override
    public Food save(Food food) throws PersistenceExceptions {
        List<Food> all = findAll();
        List<Integer> ids = new ArrayList<>();
        for (Food f : all) {
            ids.add(f.getFoodId());
        }
        food.setFoodId(IdGenerator.next(ids));
        all.add(food);
        write(all);
        return food;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe un alimento con ese id
     */
    @Override
    public void update(Food food) throws PersistenceExceptions {
        List<Food> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getFoodId() == food.getFoodId()) {
                all.set(i, food);
                write(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el alimento con id " + food.getFoodId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Food> findById(int foodId) throws PersistenceExceptions {
        for (Food f : findAll()) {
            if (f.getFoodId() == foodId) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Tanto el texto buscado como el nombre de cada alimento se normalizan
     * (minúsculas y sin tildes) antes de comparar.
     */
    @Override
    public List<Food> findByName(String text) throws PersistenceExceptions {
        String wanted = normalize(text);
        List<Food> result = new ArrayList<>();
        for (Food f : findAll()) {
            if (normalize(f.getName()).contains(wanted)) {
                result.add(f);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<Food> findAllActive() throws PersistenceExceptions {
        List<Food> result = new ArrayList<>();
        for (Food f : findAll()) {
            if (f.isActive()) {
                result.add(f);
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
    public List<Food> findAll() throws PersistenceExceptions {
        List<Food> all = new ArrayList<>();
        for (String line : file.readLines()) {
            all.add(fromLine(line));
        }
        return all;
    }

    /**
     * Normaliza un texto para compararlo: lo pasa a minúsculas y le quita las
     * tildes, de modo que "Plátano" y "platano" se consideren iguales.
     *
     * @param s texto a normalizar (puede ser {@code null})
     * @return el texto normalizado, o cadena vacía si {@code s} es {@code null}
     */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    // ---------- conversión ----------

    /**
     * Reescribe el archivo completo con la lista de alimentos recibida.
     * Convierte todos los alimentos a texto antes de tocar el archivo.
     *
     * @param foods alimentos que quedarán en el archivo
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void write(List<Food> foods) throws PersistenceExceptions {
        List<String> lines = new ArrayList<>();
        try {
            for (Food f : foods) {
                lines.add(toLine(f));
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        file.rewrite(lines);
    }

    /**
     * Convierte un alimento en una línea del archivo. La densidad y los
     * macronutrientes nulos se escriben como campo vacío.
     *
     * @param f alimento a convertir
     * @return la línea con los ocho campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String toLine(Food f) {
        return TxtFormat.join(
                String.valueOf(f.getFoodId()),
                f.getName(),
                TxtFormat.text(f.getDensityGCm3()),          // puede ser nulo -> campo vacío
                String.valueOf(f.getCalories100g()),
                TxtFormat.text(f.getProteins100g()),
                TxtFormat.text(f.getCarbohydrates100g()),
                TxtFormat.text(f.getFats100g()),
                String.valueOf(f.isActive()));
    }

    /**
     * Convierte una línea del archivo en un alimento. Los campos vacíos de
     * densidad y macronutrientes quedan como {@code null}.
     *
     * @param line línea con el formato descrito en {@link FoodRepositoryTxt}
     * @return el alimento leído
     * @throws PersistenceExceptions si la línea no tiene 8 campos o tiene valores inválidos
     */
    private Food fromLine(String line) throws PersistenceExceptions {
        String[] c = TxtFormat.split(line);
        if (c.length != 8) {
            throw new PersistenceExceptions("Línea inválida en alimentos.txt: " + line);
        }
        try {
            Food f = new Food();
            f.setFoodId(Integer.parseInt(c[0]));
            f.setName(c[1]);
            f.setDensityGCm3(TxtFormat.parseDouble(c[2]));
            f.setCalories100g(Double.parseDouble(c[3]));
            f.setProteins100g(TxtFormat.parseDouble(c[4]));
            f.setCarbohydrates100g(TxtFormat.parseDouble(c[5]));
            f.setFats100g(TxtFormat.parseDouble(c[6]));
            f.setActive(TxtFormat.parseBoolean(c[7]));
            return f;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en alimentos.txt: " + line, e);
        }
    }
}
