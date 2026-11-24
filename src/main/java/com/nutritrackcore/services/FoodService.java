package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.repository.FoodRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Catálogo de alimentos (con borrado lógico).
 */
public class FoodService {
    private static final int MAX_NAME_LENGTH = 100;
    private static final double MAX_MACROS_PER_100G = 100;

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = Objects.requireNonNull(foodRepository, "repositorio de alimentos");
    }

    public Food create(Food food) throws NutriTrackException {
        ParameterChecks.requireNonNull(food, "alimento");
        validateFood(food);
        ensureNameIsFree(food.getName(), 0);
        food.setActive(true);
        return foodRepository.save(food);
    }

    public void update(Food food) throws NutriTrackException {
        ParameterChecks.requireNonNull(food, "alimento");
        ParameterChecks.requireId(food.getFoodId(), "identificador del alimento");
        validateFood(food);
        Food stored = getById(food.getFoodId());
        ensureNameIsFree(food.getName(), food.getFoodId());
        food.setActive(stored.isActive());
        foodRepository.update(food);
    }

    public void deactivate(int foodId) throws NutriTrackException {
        Food food = getById(foodId);
        if (food.isActive()) {
            food.setActive(false);
            foodRepository.update(food);
        }
    }

    public Food getById(int foodId) throws NutriTrackException {
        ParameterChecks.requireId(foodId, "identificador del alimento");
        return foodRepository.findById(foodId).orElseThrow(
                () -> new NotFoundEntityException("No existe un alimento con el identificador " + foodId + "."));
    }

    public List<Food> searchByName(String text) throws NutriTrackException {
        String clean = ParameterChecks.requireText(text, "texto de búsqueda");
        List<Food> result = new ArrayList<>();
        for (Food food : foodRepository.findByName(clean)) {
            if (food.isActive()) {
                result.add(food);
            }
        }
        return result;
    }

    public List<Food> listActive() throws NutriTrackException {
        return foodRepository.findAllActive();
    }

    /** Valida los datos contra los {@code CHECK} de la tabla Alimento (grupo obligatorio incluido). */
    private void validateFood(Food food) throws ValidationException {
        food.setName(ParameterChecks.requireTextMax(food.getName(), MAX_NAME_LENGTH, "nombre del alimento"));
        ParameterChecks.requireNonNull(food.getGrupe(), "grupo de la pirámide");
        ParameterChecks.requireNonNegative(food.getCalories100g(), "kcal por 100 g");
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
            throw new ValidationException(
                    "Proteínas, carbohidratos y grasas no pueden sumar más de 100 g por cada 100 g de alimento.");
        }
        if (food.getDensityGCm3() != null) {
            ParameterChecks.requirePositive(food.getDensityGCm3(), "densidad (g/cm³)");
        }
    }

    private void ensureNameIsFree(String name, int ignoredFoodId) throws NutriTrackException {
        for (Food existing : foodRepository.findAll()) {
            if (existing.getFoodId() != ignoredFoodId && existing.getName() != null
                    && existing.getName().trim().equalsIgnoreCase(name)) {
                throw new BussinesRuleException("Ya existe un alimento llamado '" + name + "'.");
            }
        }
    }
}