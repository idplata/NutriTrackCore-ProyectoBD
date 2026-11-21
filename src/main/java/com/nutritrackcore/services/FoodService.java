package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.FoodRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso del catálogo de alimentos.
 * <p>
 * Regla RESTRINGIR del esquema relacional: un alimento <b>nunca se borra</b>,
 * solo se desactiva, para que el historial de consumo y los planes de dieta
 * antiguos puedan seguir mostrándolo. Los valores nutricionales son por cada
 * 100 gramos.
 */
public class FoodService {

    /** Longitud máxima del nombre de un alimento. */
    private static final int MAX_NAME_LENGTH = 100;

    /** Los tres macronutrientes por 100 g no pueden sumar más de 100 g. */
    private static final double MAX_MACROS_PER_100G = 100;

    private final FoodRepository foodRepository;

    /**
     * Crea el servicio con su dependencia.
     *
     * @param foodRepository repositorio del catálogo de alimentos
     * @throws IllegalArgumentException si el repositorio es nulo
     */
    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = ParameterChecks.requireNonNull(foodRepository, "repositorio de alimentos");
    }

    /**
     * Crea un alimento nuevo.
     * <p>
     * Pasos: (1) valida que no sea nulo y que sus datos cumplan las reglas de
     * {@code validateFood}; (2) rechaza el nombre si ya lo usa otro alimento (sin
     * distinguir mayúsculas ni espacios extremos); (3) lo marca como activo y lo
     * guarda.
     *
     * @param food alimento a crear (su id se ignora; lo asigna el repositorio)
     * @return el alimento guardado, con su id
     * @throws IllegalArgumentException si el alimento es nulo o algún dato es inválido
     * @throws IllegalStateException    si ya existe un alimento con ese nombre
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public Food create(Food food) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(food, "alimento");
        validateFood(food);
        ensureNameIsFree(food.getName(), 0);
        food.setActive(true);
        return foodRepository.save(food);
    }

    /**
     * Modifica los datos de un alimento existente.
     * <p>
     * Pasos: (1) valida id y datos; (2) comprueba que el alimento exista; (3)
     * rechaza el nombre si lo usa <b>otro</b> alimento distinto; (4) guarda los
     * cambios. El estado activo/inactivo no se cambia aquí: para eso existe
     * {@link #deactivate(int)}.
     *
     * @param food alimento con los datos nuevos (se identifica por su id)
     * @throws IllegalArgumentException si el alimento es nulo, algún dato es inválido o no existe
     * @throws IllegalStateException    si otro alimento ya usa ese nombre
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void update(Food food) throws PersistenceExceptions {
        ParameterChecks.requireNonNull(food, "alimento");
        ParameterChecks.requireId(food.getFoodId(), "identificador del alimento");
        validateFood(food);
        Food stored = getById(food.getFoodId());
        ensureNameIsFree(food.getName(), food.getFoodId());
        food.setActive(stored.isActive());
        foodRepository.update(food);
    }

    /**
     * Desactiva un alimento (nunca lo borra). Si ya estaba desactivado, no hace
     * nada.
     *
     * @param foodId identificador del alimento
     * @throws IllegalArgumentException si el id es inválido o el alimento no existe
     * @throws PersistenceExceptions    si falla la lectura o escritura del almacenamiento
     */
    public void deactivate(int foodId) throws PersistenceExceptions {
        Food food = getById(foodId);
        if (food.isActive()) {
            food.setActive(false);
            foodRepository.update(food);
        }
    }

    /**
     * Obtiene un alimento por su id, esté activo o no.
     *
     * @param foodId identificador del alimento
     * @return el alimento encontrado
     * @throws IllegalArgumentException si el id es inválido o el alimento no existe
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public Food getById(int foodId) throws PersistenceExceptions {
        ParameterChecks.requireId(foodId, "identificador del alimento");
        return foodRepository.findById(foodId).orElseThrow(
                () -> new IllegalArgumentException("No existe un alimento con el identificador " + foodId + "."));
    }

    /**
     * Busca alimentos <b>activos</b> cuyo nombre contenga un texto, sin
     * distinguir mayúsculas ni tildes. El repositorio también devuelve los
     * desactivados; este método los descarta porque la búsqueda alimenta listas
     * de selección.
     *
     * @param text texto a buscar dentro del nombre
     * @return alimentos activos que coinciden; vacía si no hay ninguno
     * @throws IllegalArgumentException si el texto es nulo o está vacío
     * @throws PersistenceExceptions    si falla la lectura del almacenamiento
     */
    public List<Food> searchByName(String text) throws PersistenceExceptions {
        String clean = ParameterChecks.requireText(text, "texto de búsqueda");
        List<Food> result = new ArrayList<>();
        for (Food food : foodRepository.findByName(clean)) {
            if (food.isActive()) {
                result.add(food);
            }
        }
        return result;
    }

    /**
     * Lista los alimentos activos, que son los que se ofrecen al registrar un
     * consumo o generar un plan.
     *
     * @return alimentos activos; vacía si no hay ninguno
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    public List<Food> listActive() throws PersistenceExceptions {
        return foodRepository.findAllActive();
    }

    /**
     * Valida los datos de un alimento y limpia su nombre.
     * <p>
     * Reglas: nombre obligatorio de máximo 100 caracteres y sin {@code |};
     * calorías finitas y no negativas; proteínas, carbohidratos y grasas, si
     * vienen, finitas y no negativas; la suma de los tres macronutrientes no
     * puede superar 100 g (son valores por cada 100 g de alimento); densidad, si
     * viene, mayor que cero.
     *
     * @param food alimento a validar (se le reemplaza el nombre por la versión recortada)
     * @throws IllegalArgumentException si alguna regla no se cumple
     */
    private void validateFood(Food food) {
        food.setName(ParameterChecks.requireTextMax(food.getName(), MAX_NAME_LENGTH, "nombre del alimento"));
        ParameterChecks.requireNonNegative(food.getCalories100g(), "calorías por 100 g");
        double macros = 0;
        if (food.getProteins100g() != null) {
            ParameterChecks.requireNonNegative(food.getProteins100g(), "proteínas por 100 g");
            macros += food.getProteins100g();
        }
        if (food.getCarbohydrates100g() != null) {
            ParameterChecks.requireNonNegative(food.getCarbohydrates100g(), "carbohidratos por 100 g");
            macros += food.getCarbohydrates100g();
        }
        if (food.getFats100g() != null) {
            ParameterChecks.requireNonNegative(food.getFats100g(), "grasas por 100 g");
            macros += food.getFats100g();
        }
        if (macros > MAX_MACROS_PER_100G) {
            throw new IllegalArgumentException(
                    "Proteínas, carbohidratos y grasas no pueden sumar más de 100 g por cada 100 g de alimento.");
        }
        if (food.getDensityGCm3() != null) {
            ParameterChecks.requirePositive(food.getDensityGCm3(), "densidad (g/cm³)");
        }
    }

    /**
     * Comprueba que ningún otro alimento use un nombre.
     *
     * @param name          nombre ya recortado
     * @param ignoredFoodId id del alimento que se está editando (se ignora a sí mismo), o 0 si es uno nuevo
     * @throws IllegalStateException si otro alimento tiene el mismo nombre
     * @throws PersistenceExceptions si falla la lectura del almacenamiento
     */
    private void ensureNameIsFree(String name, int ignoredFoodId) throws PersistenceExceptions {
        for (Food existing : foodRepository.findAll()) {
            if (existing.getFoodId() != ignoredFoodId && existing.getName() != null
                    && existing.getName().trim().equalsIgnoreCase(name)) {
                throw new IllegalStateException("Ya existe un alimento llamado '" + name + "'.");
            }
        }
    }
}