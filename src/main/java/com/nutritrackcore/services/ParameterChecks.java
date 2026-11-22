package com.nutritrackcore.services;

import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.BMIHistory;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validaciones de los datos que entran a los servicios.
 * <p>
 * Reúne lo que antes estaba en el paquete {@code validations} y usa los mismos
 * límites y valores admitidos que la base de datos, para que ningún dato
 * rechazado por un {@code CHECK} salga de los servicios.
 */
public final class ParameterChecks {

    /** Peso mínimo y máximo en kg ({@code CHECK} de Usuario). */
    public static final double MIN_WEIGHT_KG = 30;
    public static final double MAX_WEIGHT_KG = 300;
    /** Altura mínima y máxima en cm ({@code CHECK} de Usuario). */
    public static final double MIN_HEIGHT_CM = 80;
    public static final double MAX_HEIGHT_CM = 272;
    /** Largo máximo de nombre y correo (varchar(100) en la base de datos). */
    public static final int MAX_USER_TEXT_LENGTH = 100;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_AGE = 120;
    /** kcal objetivo del plan ({@code CHECK} de PlanDieta). */
    public static final double MIN_TARGET_KCAL = 1000;
    public static final double MAX_TARGET_KCAL = 6000;
    /** Gramos por alimento en un registro de consumo ({@code CHECK} de RegistroConsumo_Alimento). */
    public static final double MAX_CONSUMED_GRAMS = 2000;
    /** Comidas al día ({@code CHECK} de PerfilUsuario). */
    public static final int MIN_MEALS_PER_DAY = 1;
    public static final int MAX_MEALS_PER_DAY = 4;
    public static final int MAX_TRAINING_DAYS = 7;

    /** Valores admitidos por la base de datos, en minúsculas. */
    public static final Set<String> ACTIVITY_LEVELS = Set.of("sedentario", "ligero", "moderado", "muy_activo");
    public static final Set<String> LOW_ACTIVITY_LEVELS = Set.of("sedentario", "ligero");
    public static final Set<String> EXPERIENCE_LEVELS = Set.of("principiante", "intermedio", "avanzado");
    public static final Set<String> WEIGHT_GOALS = Set.of("bajar", "mantener", "subir");
    public static final Set<String> TRAINING_LOCATIONS = Set.of("casa", "gimnasio");
    public static final String HOME_LOCATION = "casa";
    public static final Set<String> ALLERGY_SEVERITIES = Set.of("leve", "moderada", "severa");

    /** Valores admitidos por la base de datos, en mayúsculas. */
    public static final Set<String> MUSCLE_GROUPS = Set.of(
            "PECHO", "ESPALDA", "HOMBROS", "BRAZOS", "PIERNAS", "GLUTEOS", "CORE", "CUERPO_COMPLETO", "CARDIO");

    private static final long FUTURE_TOLERANCE_MINUTES = 1;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    private ParameterChecks() {
    }

    public static <T> T requireNonNull(T value, String label) throws ValidationException {
        if (value == null) {
            throw new ValidationException("El dato '" + label + "' no puede ser nulo.");
        }
        return value;
    }

    public static String requireText(String value, String label) throws ValidationException {
        requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("El dato '" + label + "' no puede estar vacío.");
        }
        return trimmed;
    }

    public static String requireTextMax(String value, int maxLength, String label) throws ValidationException {
        String trimmed = requireText(value, label);
        checkLength(trimmed, maxLength, label);
        return trimmed;
    }

    /** Texto opcional: devuelve {@code null} si viene nulo o vacío. */
    public static String requireOptionalText(String value, int maxLength, String label) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        checkLength(trimmed, maxLength, label);
        return trimmed;
    }

    private static void checkLength(String trimmed, int maxLength, String label) throws ValidationException {
        if (trimmed.length() > maxLength) {
            throw new ValidationException(
                    "El dato '" + label + "' no puede superar los " + maxLength + " caracteres.");
        }
    }

    public static void requireId(int id, String label) throws ValidationException {
        if (id <= 0) {
            throw new ValidationException("El " + label + " debe ser mayor que cero.");
        }
    }

    public static void requireFinite(double value, String label) throws ValidationException {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ValidationException("El dato '" + label + "' debe ser un número válido.");
        }
    }

    public static void requirePositive(double value, String label) throws ValidationException {
        requireFinite(value, label);
        if (value <= 0) {
            throw new ValidationException("El dato '" + label + "' debe ser mayor que cero.");
        }
    }

    public static void requireNonNegative(double value, String label) throws ValidationException {
        requireFinite(value, label);
        if (value < 0) {
            throw new ValidationException("El dato '" + label + "' no puede ser negativo.");
        }
    }

    public static void requireRange(double value, double min, double max, String label) throws ValidationException {
        requireFinite(value, label);
        if (value < min || value > max) {
            throw new ValidationException("El dato '" + label + "' debe estar entre " + min + " y " + max + ".");
        }
    }

    public static void requireMeasurements(double weightKg, double heightCm) throws ValidationException {
        requireRange(weightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG, "peso (kg)");
        requireRange(heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM, "altura (cm)");
    }

    public static String requireEmail(String email) throws ValidationException {
        String trimmed = requireTextMax(email, MAX_USER_TEXT_LENGTH, "correo");
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("El correo no tiene un formato válido (ejemplo: nombre@dominio.com).");
        }
        return trimmed;
    }

    public static void requirePassword(String password) throws ValidationException {
        requireNonNull(password, "contraseña");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException("La contraseña debe contener al menos una letra y un número.");
        }
    }

    public static void requireBirthDate(LocalDate birthDate) throws ValidationException {
        requireNonNull(birthDate, "fecha de nacimiento");
        if (birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("La fecha de nacimiento no puede ser futura.");
        }
        if (birthDate.isBefore(LocalDate.now().minusYears(MAX_AGE))) {
            throw new ValidationException("La edad no puede superar los " + MAX_AGE + " años.");
        }
    }

    public static void requireNotFuture(LocalDate date, String label) throws ValidationException {
        requireNonNull(date, label);
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("La " + label + " no puede ser futura.");
        }
    }

    public static void requireNotFuture(LocalDateTime dateTime, String label) throws ValidationException {
        requireNonNull(dateTime, label);
        if (dateTime.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new ValidationException("La " + label + " no puede ser futura.");
        }
    }

    public static void requireDateRange(LocalDate from, LocalDate to) throws ValidationException {
        requireNonNull(from, "fecha inicial");
        requireNonNull(to, "fecha final");
        if (from.isAfter(to)) {
            throw new ValidationException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    /**
     * Valida un texto contra una lista de valores que la base de datos guarda en minúsculas.
     *
     * @return el valor en minúsculas, listo para guardar
     */
    public static String requireLowerOption(String value, Set<String> allowed, String label)
            throws ValidationException {
        String clean = requireText(value, label).toLowerCase(Locale.ROOT);
        if (!allowed.contains(clean)) {
            throw new ValidationException("El dato '" + label + "' debe ser uno de: " + allowed + ".");
        }
        return clean;
    }

    /**
     * Valida un texto contra una lista de valores que la base de datos guarda en mayúsculas.
     *
     * @return el valor en mayúsculas, listo para guardar
     */
    public static String requireUpperOption(String value, Set<String> allowed, String label)
            throws ValidationException {
        String clean = requireText(value, label).toUpperCase(Locale.ROOT);
        if (!allowed.contains(clean)) {
            throw new ValidationException("El dato '" + label + "' debe ser uno de: " + allowed + ".");
        }
        return clean;
    }

    /** Convierte el tipo de dieta del perfil (texto) al enum del plan, en mayúsculas como lo guarda la base. */
    public static DietPlan.DietType requireDietType(String value) throws ValidationException {
        String clean = requireText(value, "tipo de dieta").toUpperCase(Locale.ROOT);
        try {
            return DietPlan.DietType.valueOf(clean);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("El tipo de dieta '" + value + "' no es válido.");
        }
    }

    public static String requireMuscleGroup(String value) throws ValidationException {
        return requireUpperOption(value, MUSCLE_GROUPS, "grupo muscular");
    }

    public static void requireMealsPerDay(int meals) throws ValidationException {
        if (meals < MIN_MEALS_PER_DAY || meals > MAX_MEALS_PER_DAY) {
            throw new ValidationException("Las comidas al día deben estar entre "
                    + MIN_MEALS_PER_DAY + " y " + MAX_MEALS_PER_DAY + ".");
        }
    }

    public static void requireConsumedGrams(double grams) throws ValidationException {
        requirePositive(grams, "gramos");
        if (grams > MAX_CONSUMED_GRAMS) {
            throw new ValidationException("Los gramos no pueden superar " + (int) MAX_CONSUMED_GRAMS + " por alimento.");
        }
    }

    /** Antes {@code UserValidator}: datos completos de un usuario. */
    public static void validateUser(User user) throws ValidationException {
        requireNonNull(user, "usuario");
        requireTextMax(user.getName(), MAX_USER_TEXT_LENGTH, "nombre");
        requireEmail(user.getEmail());
        requireMeasurements(user.getWeightKg(), user.getHeightCm());
        requireBirthDate(user.getBirthDate());
        requireNonNull(user.getSex(), "sexo");
    }

    /** Antes {@code BMIHistoryValidator}: una fila del historial de IMC. */
    public static void validateBmiHistory(BMIHistory history) throws ValidationException {
        requireNonNull(history, "historial de IMC");
        requireId(history.getIdUser(), "identificador del usuario");
        requireMeasurements(history.getWeightKg(), history.getHeightCm());
        requireNotFuture(history.getRegisterDate(), "fecha de registro");
    }
}