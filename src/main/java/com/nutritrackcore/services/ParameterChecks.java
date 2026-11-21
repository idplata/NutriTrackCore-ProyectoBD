package com.nutritrackcore.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Utilidad de validación de parámetros para toda la capa de servicios.
 * <p>
 * Concentra en un solo lugar las reglas que antes se repetirían en cada
 * servicio: que un parámetro no sea nulo, que un texto no esté vacío, que un
 * número esté dentro de un rango, etc. Cada método lanza una
 * {@link IllegalArgumentException} cuyo mensaje está <b>en español</b>, listo
 * para mostrarse al usuario en la interfaz.
 * <p>
 * Convención de uso: el segundo argumento ({@code label}) es el nombre del dato
 * tal como lo vería una persona (por ejemplo {@code "correo"}), no el nombre de
 * la variable Java; así el mensaje resultante es comprensible.
 * <p>
 * Es una clase de utilidad: no se instancia y todos sus métodos son
 * {@code static}.
 */
public final class ParameterChecks {

    /** Peso mínimo aceptado, en kilogramos. */
    public static final double MIN_WEIGHT_KG = 20;

    /** Peso máximo aceptado, en kilogramos. */
    public static final double MAX_WEIGHT_KG = 400;

    /** Altura mínima aceptada, en centímetros. */
    public static final double MIN_HEIGHT_CM = 50;

    /** Altura máxima aceptada, en centímetros. */
    public static final double MAX_HEIGHT_CM = 250;

    /** Longitud máxima del nombre y del correo de un usuario. */
    public static final int MAX_USER_TEXT_LENGTH = 50;

    /** Longitud mínima de una contraseña. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /**
     * Carácter separador de los archivos .txt. Se prohíbe en los textos
     * escritos por el usuario porque corrompería el formato del archivo. Se
     * declara aquí (y no se importa de {@code repository.txt}) para que la capa
     * de servicios no dependa de ninguna implementación concreta.
     */
    private static final String TXT_SEPARATOR = "|";

    /** Tolerancia, en minutos, para aceptar fechas "ahora" ligeramente adelantadas. */
    private static final long FUTURE_TOLERANCE_MINUTES = 1;

    /** Patrón sencillo de correo: texto, arroba, dominio y al menos un punto. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    /** Constructor privado: la clase solo ofrece métodos estáticos. */
    private ParameterChecks() {
    }

    /**
     * Verifica que un objeto no sea nulo.
     * <p>
     * Es la validación base: casi todos los métodos públicos de los servicios la
     * invocan primero para cada parámetro de tipo referencia.
     *
     * @param value valor a revisar
     * @param label nombre legible del dato, usado en el mensaje de error
     * @param <T>   tipo del valor
     * @return el mismo valor, para poder usar la llamada como expresión
     * @throws IllegalArgumentException si el valor es {@code null}
     */
    public static <T> T requireNonNull(T value, String label) {
        if (value == null) {
            throw new IllegalArgumentException("El dato '" + label + "' no puede ser nulo.");
        }
        return value;
    }

    /**
     * Verifica que un texto no sea nulo ni esté vacío o formado solo por
     * espacios, y lo devuelve sin espacios en los extremos.
     *
     * @param value texto a revisar
     * @param label nombre legible del dato
     * @return el texto con los espacios iniciales y finales eliminados
     * @throws IllegalArgumentException si el texto es nulo o está en blanco
     */
    public static String requireText(String value, String label) {
        requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("El dato '" + label + "' no puede estar vacío.");
        }
        return trimmed;
    }

    /**
     * Verifica un texto obligatorio con longitud máxima y sin el carácter
     * separador de los archivos .txt.
     * <p>
     * Pasos: (1) exige que no sea nulo ni vacío con {@link #requireText}; (2)
     * comprueba que su longitud, ya recortado, no supere {@code maxLength};
     * (3) rechaza el carácter {@code |}.
     *
     * @param value     texto a revisar
     * @param maxLength longitud máxima permitida
     * @param label     nombre legible del dato
     * @return el texto recortado
     * @throws IllegalArgumentException si falla cualquiera de las tres comprobaciones
     */
    public static String requireTextMax(String value, int maxLength, String label) {
        String trimmed = requireText(value, label);
        checkLengthAndSeparator(trimmed, maxLength, label);
        return trimmed;
    }

    /**
     * Verifica un texto <b>opcional</b>: no puede ser {@code null}, pero sí
     * puede venir vacío (por ejemplo, unas observaciones sin contenido).
     * Comprueba la longitud máxima y que no contenga el separador {@code |}.
     *
     * @param value     texto a revisar
     * @param maxLength longitud máxima permitida
     * @param label     nombre legible del dato
     * @return el texto recortado (puede ser una cadena vacía)
     * @throws IllegalArgumentException si es nulo, demasiado largo o contiene {@code |}
     */
    public static String requireOptionalText(String value, int maxLength, String label) {
        requireNonNull(value, label);
        String trimmed = value.trim();
        checkLengthAndSeparator(trimmed, maxLength, label);
        return trimmed;
    }

    /**
     * Comprueba longitud máxima y ausencia del separador en un texto ya recortado.
     *
     * @param trimmed   texto sin espacios en los extremos
     * @param maxLength longitud máxima permitida
     * @param label     nombre legible del dato
     */
    private static void checkLengthAndSeparator(String trimmed, int maxLength, String label) {
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    "El dato '" + label + "' no puede superar los " + maxLength + " caracteres.");
        }
        if (trimmed.contains(TXT_SEPARATOR)) {
            throw new IllegalArgumentException(
                    "El dato '" + label + "' no puede contener el carácter '" + TXT_SEPARATOR + "'.");
        }
    }

    /**
     * Verifica que un identificador sea válido. Los ids que genera el
     * repositorio empiezan en 1, así que cero o negativos no pueden existir.
     *
     * @param id    identificador a revisar
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si {@code id} es menor o igual que cero
     */
    public static void requireId(int id, String label) {
        if (id <= 0) {
            throw new IllegalArgumentException("El " + label + " debe ser mayor que cero.");
        }
    }

    /**
     * Verifica que un número sea un valor real válido (ni {@code NaN} ni
     * infinito).
     *
     * @param value número a revisar
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si el número no es finito
     */
    public static void requireFinite(double value, String label) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("El dato '" + label + "' debe ser un número válido.");
        }
    }

    /**
     * Verifica que un número sea finito y estrictamente mayor que cero.
     *
     * @param value número a revisar
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si no es finito o es menor o igual que cero
     */
    public static void requirePositive(double value, String label) {
        requireFinite(value, label);
        if (value <= 0) {
            throw new IllegalArgumentException("El dato '" + label + "' debe ser mayor que cero.");
        }
    }

    /**
     * Verifica que un número sea finito y no negativo (cero es válido).
     *
     * @param value número a revisar
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si no es finito o es negativo
     */
    public static void requireNonNegative(double value, String label) {
        requireFinite(value, label);
        if (value < 0) {
            throw new IllegalArgumentException("El dato '" + label + "' no puede ser negativo.");
        }
    }

    /**
     * Verifica que un número finito esté dentro de un rango cerrado
     * {@code [min, max]}.
     *
     * @param value número a revisar
     * @param min   mínimo permitido (incluido)
     * @param max   máximo permitido (incluido)
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si el número no es finito o queda fuera del rango
     */
    public static void requireRange(double value, double min, double max, String label) {
        requireFinite(value, label);
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "El dato '" + label + "' debe estar entre " + min + " y " + max + ".");
        }
    }

    /**
     * Verifica un par peso/altura contra los límites del sistema
     * ({@link #MIN_WEIGHT_KG}–{@link #MAX_WEIGHT_KG} kg y
     * {@link #MIN_HEIGHT_CM}–{@link #MAX_HEIGHT_CM} cm).
     *
     * @param weightKg peso en kilogramos
     * @param heightCm altura en centímetros
     * @throws IllegalArgumentException si alguno de los dos valores es inválido
     */
    public static void requireMeasurements(double weightKg, double heightCm) {
        requireRange(weightKg, MIN_WEIGHT_KG, MAX_WEIGHT_KG, "peso (kg)");
        requireRange(heightCm, MIN_HEIGHT_CM, MAX_HEIGHT_CM, "altura (cm)");
    }

    /**
     * Verifica un correo electrónico: obligatorio, de máximo
     * {@link #MAX_USER_TEXT_LENGTH} caracteres, sin el separador {@code |} y con
     * formato {@code texto@dominio.ext}.
     *
     * @param email correo a revisar
     * @return el correo recortado
     * @throws IllegalArgumentException si el correo es nulo, vacío, largo o mal formado
     */
    public static String requireEmail(String email) {
        String trimmed = requireTextMax(email, MAX_USER_TEXT_LENGTH, "correo");
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato válido (ejemplo: nombre@dominio.com).");
        }
        return trimmed;
    }

    /**
     * Verifica la política de contraseñas: no nula, de al menos
     * {@link #MIN_PASSWORD_LENGTH} caracteres y con al menos una letra y un
     * número. No recorta espacios: la contraseña se usa tal como se escribió.
     *
     * @param password contraseña en texto plano
     * @throws IllegalArgumentException si no cumple la política
     */
    public static void requirePassword(String password) {
        requireNonNull(password, "contraseña");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("La contraseña debe contener al menos una letra y un número.");
        }
    }

    /**
     * Verifica una fecha de nacimiento: no nula, no futura y no anterior al
     * año 1900.
     *
     * @param birthDate fecha de nacimiento
     * @throws IllegalArgumentException si es nula, futura o demasiado antigua
     */
    public static void requireBirthDate(LocalDate birthDate) {
        requireNonNull(birthDate, "fecha de nacimiento");
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        if (birthDate.getYear() < 1900) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser anterior al año 1900.");
        }
    }

    /**
     * Verifica que una fecha sea hoy o anterior.
     *
     * @param date  fecha a revisar
     * @param label nombre legible del dato
     * @throws IllegalArgumentException si la fecha es nula o futura
     */
    public static void requireNotFuture(LocalDate date, String label) {
        requireNonNull(date, label);
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La " + label + " no puede ser futura.");
        }
    }

    /**
     * Verifica que una fecha con hora no esté en el futuro. Se tolera un minuto
     * de adelanto para absorber pequeñas diferencias de reloj entre la interfaz y
     * el servicio.
     *
     * @param dateTime fecha y hora a revisar
     * @param label    nombre legible del dato
     * @throws IllegalArgumentException si es nula o está más de un minuto en el futuro
     */
    public static void requireNotFuture(LocalDateTime dateTime, String label) {
        requireNonNull(dateTime, label);
        if (dateTime.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new IllegalArgumentException("La " + label + " no puede ser futura.");
        }
    }

    /**
     * Verifica un rango de fechas: ninguna nula y {@code from} no posterior a
     * {@code to}.
     *
     * @param from fecha inicial (incluida)
     * @param to   fecha final (incluida)
     * @throws IllegalArgumentException si alguna es nula o el rango está invertido
     */
    public static void requireDateRange(LocalDate from, LocalDate to) {
        requireNonNull(from, "fecha inicial");
        requireNonNull(to, "fecha final");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }
}