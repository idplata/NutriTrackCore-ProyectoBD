package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.User;
import com.nutritrackcore.services.ParameterChecks;
import java.time.LocalDate;
import java.time.Period;

/**
 * Calcula el requerimiento calórico diario con la ecuación de Mifflin-St Jeor
 * multiplicada por un factor de actividad.
 * <p>
 * Tasa metabólica basal (TMB):
 * <ul>
 *   <li>Hombre: {@code 10·peso(kg) + 6,25·altura(cm) − 5·edad + 5}</li>
 *   <li>Mujer: {@code 10·peso(kg) + 6,25·altura(cm) − 5·edad − 161}</li>
 *   <li>Otro: promedio de las dos constantes ({@code − 78})</li>
 * </ul>
 * El resultado es {@code TMB × factor de actividad}. El tratamiento del sexo
 * "otro" es una decisión académica que el equipo debe validar.
 */
public class MifflinStJeorCalculator implements CalorieRequirementCalculator {

    /** Factor de actividad por defecto: persona sedentaria. */
    public static final double DEFAULT_ACTIVITY_FACTOR = 1.2;

    /** Constante de la ecuación para hombres. */
    private static final double MALE_CONSTANT = 5;

    /** Constante de la ecuación para mujeres. */
    private static final double FEMALE_CONSTANT = -161;

    /** Constante usada cuando el sexo es "otro": promedio de las dos anteriores. */
    private static final double OTHER_CONSTANT = (MALE_CONSTANT + FEMALE_CONSTANT) / 2.0;

    /** Multiplicador de la TMB según el nivel de actividad física. */
    private final double activityFactor;

    /**
     * Crea el calculador con el factor de actividad por defecto (1,2).
     */
    public MifflinStJeorCalculator() {
        this(DEFAULT_ACTIVITY_FACTOR);
    }

    /**
     * Crea el calculador con un factor de actividad propio.
     *
     * @param activityFactor multiplicador de la TMB (entre 1,0 y 2,5)
     * @throws IllegalArgumentException si el factor está fuera del rango 1,0–2,5
     */
    public MifflinStJeorCalculator(double activityFactor) {
        ParameterChecks.requireRange(activityFactor, 1.0, 2.5, "factor de actividad");
        this.activityFactor = activityFactor;
    }

    /**
     * Calcula las calorías diarias de un usuario.
     * <p>
     * Pasos: (1) valida que usuario, fecha, fecha de nacimiento y sexo no sean
     * nulos y que peso y altura estén en rango; (2) calcula la edad en años
     * cumplidos entre la fecha de nacimiento y {@code today}; (3) elige la
     * constante según el código del sexo ({@code M}, {@code F} u otro); (4)
     * aplica la ecuación de Mifflin-St Jeor; (5) multiplica por el factor de
     * actividad. Si el resultado fuera menor o igual que cero (casos extremos),
     * lanza un error en lugar de devolver un valor sin sentido.
     *
     * @param user  usuario del que se calcula el requerimiento
     * @param today fecha de referencia para la edad
     * @return calorías diarias en kcal
     * @throws IllegalArgumentException si algún dato es nulo o inválido, la fecha de nacimiento es posterior a
     *                                  {@code today} o el resultado no es positivo
     */
    @Override
    public double calculateDailyCalories(User user, LocalDate today) {
        ParameterChecks.requireNonNull(user, "usuario");
        ParameterChecks.requireNonNull(today, "fecha de referencia");
        ParameterChecks.requireNonNull(user.getBirthDate(), "fecha de nacimiento del usuario");
        ParameterChecks.requireNonNull(user.getSex(), "sexo del usuario");
        ParameterChecks.requireMeasurements(user.getWeightKg(), user.getHeightCm());
        if (user.getBirthDate().isAfter(today)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser posterior a la fecha de referencia.");
        }

        int age = Period.between(user.getBirthDate(), today).getYears();
        String code = user.getSex().getCode();
        double constant;
        if ("M".equals(code)) {
            constant = MALE_CONSTANT;
        } else if ("F".equals(code)) {
            constant = FEMALE_CONSTANT;
        } else {
            constant = OTHER_CONSTANT;
        }
        double bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * age + constant;
        double calories = bmr * activityFactor;
        if (calories <= 0) {
            throw new IllegalArgumentException("Con los datos del usuario no se puede calcular un requerimiento calórico válido.");
        }
        return calories;
    }
}