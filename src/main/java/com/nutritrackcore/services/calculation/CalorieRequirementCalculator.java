package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.User;
import java.time.LocalDate;

/**
 * Contrato para calcular cuántas calorías necesita una persona al día.
 * <p>
 * {@code DietPlanService} depende de esta interfaz y no de una fórmula
 * concreta; para usar otra fórmula basta con crear otra clase que la
 * implemente.
 */
public interface CalorieRequirementCalculator {

    /**
     * Calcula el requerimiento calórico diario de un usuario.
     *
     * @param user  usuario con peso, altura, fecha de nacimiento y sexo
     * @param today fecha de referencia para calcular la edad
     * @return calorías diarias (kcal) antes de aplicar ajustes por condiciones médicas
     * @throws IllegalArgumentException si algún parámetro es nulo o el usuario tiene datos incompletos o inválidos
     */
    double calculateDailyCalories(User user, LocalDate today);
}