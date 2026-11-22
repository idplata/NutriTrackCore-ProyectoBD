package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.User;
import java.time.LocalDate;
import java.time.Period;

/**
 * Requerimiento calórico con la fórmula de Mifflin-St Jeor por un factor de actividad.
 * Para el sexo "O" se usa el promedio de las constantes de hombre y mujer.
 */
public class MifflinStJeorCalculator implements CalorieRequirementCalculator {
    public static final double DEFAULT_ACTIVITY_FACTOR = 1.2;
    private static final double MALE_CONSTANT = 5;
    private static final double FEMALE_CONSTANT = -161;
    private static final double OTHER_CONSTANT = (MALE_CONSTANT + FEMALE_CONSTANT) / 2.0;

    private final double activityFactor;

    public MifflinStJeorCalculator() {
        this(DEFAULT_ACTIVITY_FACTOR);
    }

    public MifflinStJeorCalculator(double activityFactor) {
        if (Double.isNaN(activityFactor) || activityFactor < 1.0 || activityFactor > 2.5) {
            throw new IllegalArgumentException("El factor de actividad debe estar entre 1.0 y 2.5.");
        }
        this.activityFactor = activityFactor;
    }

    @Override
    public double calculateDailyCalories(User user, LocalDate today) throws ValidationException {
        ParameterChecks.requireNonNull(user, "usuario");
        ParameterChecks.requireNonNull(today, "fecha de referencia");
        ParameterChecks.requireNonNull(user.getBirthDate(), "fecha de nacimiento del usuario");
        ParameterChecks.requireNonNull(user.getSex(), "sexo del usuario");
        ParameterChecks.requireMeasurements(user.getWeightKg(), user.getHeightCm());
        if (user.getBirthDate().isAfter(today)) {
            throw new ValidationException("La fecha de nacimiento no puede ser posterior a la fecha de referencia.");
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
            throw new ValidationException("Con los datos del usuario no se puede calcular un requerimiento calórico válido.");
        }
        return calories;
    }
}