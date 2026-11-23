package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NotFoundEntityException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.model.Food;
import com.nutritrackcore.model.UserExcludedFood;
import com.nutritrackcore.repository.FoodRepository;
import com.nutritrackcore.repository.UserExcludedFoodRepository;
import com.nutritrackcore.repository.UserRepository;
import java.util.List;
import java.util.Objects;

/**
 * Alimentos que el usuario no quiere en su dieta (por gusto, no por alergia).
 * Al agregar uno se refresca el plan activo para sacarlo o reemplazarlo.
 */
public class ExcludedFoodService {

    private final UserExcludedFoodRepository excludedFoodRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final DietPlanService dietPlanService;

    /**
     * @param excludedFoodRepository alimentos excluidos por usuario
     * @param foodRepository         catálogo de alimentos
     * @param userRepository         usuarios
     * @param dietPlanService        servicio que ajusta el plan activo
     */
    public ExcludedFoodService(UserExcludedFoodRepository excludedFoodRepository, FoodRepository foodRepository,
                               UserRepository userRepository, DietPlanService dietPlanService) {
        this.excludedFoodRepository = Objects.requireNonNull(excludedFoodRepository, "repositorio de excluidos");
        this.foodRepository = Objects.requireNonNull(foodRepository, "repositorio de alimentos");
        this.userRepository = Objects.requireNonNull(userRepository, "repositorio de usuarios");
        this.dietPlanService = Objects.requireNonNull(dietPlanService, "servicio de dietas");
    }

    /**
     * Alimentos excluidos del usuario.
     *
     * @param userId identificador del usuario
     * @return lista de exclusiones (puede estar vacía)
     * @throws NutriTrackException si el id no es válido o falla la persistencia
     */
    public List<UserExcludedFood> listByUser(int userId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        return excludedFoodRepository.findByUserId(userId);
    }

    /**
     * Excluye un alimento y ajusta el plan de dieta activo.
     *
     * @param userId identificador del usuario
     * @param foodId identificador del alimento
     * @return qué alimentos del plan se reemplazaron o quitaron
     * @throws NutriTrackException si no existe el usuario o el alimento, o ya estaba excluido
     */
    public AllergyChangeResult exclude(int userId, int foodId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(foodId, "identificador del alimento");
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundEntityException("No existe un usuario con el identificador " + userId + ".");
        }
        Food food = foodRepository.findById(foodId).orElseThrow(
                () -> new NotFoundEntityException("No existe un alimento con el identificador " + foodId + "."));
        if (excludedFoodRepository.exists(userId, foodId)) {
            throw new BussinesRuleException("'" + food.getName() + "' ya está en tus alimentos excluidos.");
        }
        excludedFoodRepository.save(new UserExcludedFood(userId, foodId));
        return dietPlanService.refreshForRestrictions(userId);
    }

    /**
     * Vuelve a permitir un alimento. El plan actual no cambia; se tendrá en
     * cuenta en la próxima dieta generada.
     *
     * @param userId identificador del usuario
     * @param foodId identificador del alimento
     * @throws NutriTrackException si el alimento no estaba excluido
     */
    public void include(int userId, int foodId) throws NutriTrackException {
        ParameterChecks.requireId(userId, "identificador del usuario");
        ParameterChecks.requireId(foodId, "identificador del alimento");
        if (!excludedFoodRepository.exists(userId, foodId)) {
            throw new NotFoundEntityException("Ese alimento no está en tus excluidos.");
        }
        excludedFoodRepository.delete(userId, foodId);
    }
}
