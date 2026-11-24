package com.nutritrackcore.ui;

import com.nutritrackcore.camera.FoodCaptureService;
import com.nutritrackcore.exceptions.AutenticationException;
import com.nutritrackcore.model.User;
import com.nutritrackcore.services.AccountDeletionService;
import com.nutritrackcore.services.AllergyService;
import com.nutritrackcore.services.AuthenticationService;
import com.nutritrackcore.services.ExcludedFoodService;
import com.nutritrackcore.services.QueryService;
import com.nutritrackcore.services.RoutineFollowUpService;
import com.nutritrackcore.services.UserProfileService;
import com.nutritrackcore.services.BmiService;
import com.nutritrackcore.services.ConsumptionService;
import com.nutritrackcore.services.DietPlanService;
import com.nutritrackcore.services.ExerciseRoutineService;
import com.nutritrackcore.services.ExerciseService;
import com.nutritrackcore.services.FoodService;
import com.nutritrackcore.services.MedicalConditionService;
import com.nutritrackcore.services.UserService;
import com.nutritrackcore.services.UserSession;

/**
 * Paquete con los servicios que usa la interfaz. Lo arma
 * {@code ApplicationContext} y la {@link MainWindow} se lo pasa a cada
 * pantalla, que toma solo los servicios que necesita.
 *
 * @param session        sesión del usuario que inició sesión
 * @param authentication registro, inicio y cierre de sesión
 * @param users          datos y medidas del usuario
 * @param bmi            IMC actual e historial
 * @param conditions     condiciones médicas
 * @param foods          catálogo de alimentos
 * @param exercises      catálogo de ejercicios
 * @param consumption    registro de consumo y resumen del día
 * @param dietPlans      plan de dieta
 * @param routines       rutina de ejercicio
 * @param profiles       hábitos del usuario (perfil de dieta y entrenamiento)
 * @param allergies      alergias del usuario
 * @param excludedFoods  alimentos que el usuario no quiere en su dieta
 * @param followUps      sesiones registradas de la rutina
 * @param queries        consultas para la pantalla de progreso
 * @param accountDeletion eliminación de la cuenta
 * @param camera         registro de comidas con foto (cámara del celular y servicio de visión)
 */
public record AppServices(
        UserSession session,
        AuthenticationService authentication,
        UserService users,
        BmiService bmi,
        MedicalConditionService conditions,
        FoodService foods,
        ExerciseService exercises,
        ConsumptionService consumption,
        DietPlanService dietPlans,
        ExerciseRoutineService routines,
        UserProfileService profiles,
        AllergyService allergies,
        ExcludedFoodService excludedFoods,
        RoutineFollowUpService followUps,
        QueryService queries,
        AccountDeletionService accountDeletion,
        FoodCaptureService camera) {

    /**
     * Usuario que inició sesión.
     *
     * @return el usuario actual
     * @throws IllegalStateException si nadie ha iniciado sesión (no debería pasar dentro de la aplicación)
     */
    public User currentUser() {
        try {
            return session.getCurrentUser();
        } catch (AutenticationException e) {
            throw new IllegalStateException("No hay una sesión iniciada.", e);
        }
    }
}
