package com.nutritrackcore.ui;

import com.nutritrackcore.services.AuthenticationService;
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
        ExerciseRoutineService routines) {
}
