package com.nutritrackcore.repository.txt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Reglas de formato de los archivos .txt.
 * <ul>
 *   <li>Separador de campos: {@code "|"}.</li>
 *   <li>Valor nulo: campo vacío.</li>
 *   <li>Decimales con punto.</li>
 *   <li>Fechas en ISO-8601.</li>
 *   <li>Booleanos {@code true}/{@code false}.</li>
 *   <li>Enums por nombre.</li>
 * </ul>
 * Es una clase de utilidad: no se puede instanciar.
 */
public final class TxtFormat {

    /** Separador de campos dentro de una línea. */
    public static final String SEPARATOR = "|";

    /** Constructor privado: es una clase de utilidad y no debe instanciarse. */
    private TxtFormat() {}

    /**
     * Une los campos con {@code "|"}. Los {@code null} se escriben como campo
     * vacío.
     *
     * @param fields campos de la línea, en orden
     * @return la línea completa lista para escribir en el archivo
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea,
     *                                  porque romperían el archivo
     */
    public static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i];
            if (f != null && (f.indexOf('|') >= 0 || f.indexOf('\n') >= 0 || f.indexOf('\r') >= 0)) {
                throw new IllegalArgumentException("El texto no puede contener '|' ni saltos de línea: " + f);
            }
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(f == null ? "" : f);
        }
        return sb.toString();
    }

    /**
     * Separa una línea en campos. El límite {@code -1} conserva los campos
     * vacíos del final.
     *
     * @param line línea leída del archivo
     * @return arreglo con los campos de la línea
     */
    public static String[] split(String line) {
        return line.split("\\|", -1);
    }

    // ---------- escritura ----------

    /**
     * Convierte un valor a texto. {@code null} se mantiene como {@code null};
     * en otro caso devuelve {@code toString()}. Úsalo para
     * {@code Double}/{@code Integer}/enums que pueden ser nulos.
     *
     * @param value valor a convertir (puede ser {@code null})
     * @return el texto del valor, o {@code null} si el valor es {@code null}
     */
    public static String text(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Da formato ISO-8601 a una fecha y hora, truncada a segundos
     * (por ejemplo {@code 2026-10-24T18:30:00}).
     *
     * @param value fecha y hora a convertir (puede ser {@code null})
     * @return el texto en ISO-8601, o {@code null} si el valor es {@code null}
     */
    public static String dateTime(LocalDateTime value) {
        return value == null ? null
                : value.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ---------- lectura (campo vacío = null) ----------

    /**
     * Convierte un campo vacío en {@code null}.
     *
     * @param field campo leído del archivo
     * @return {@code null} si el campo es nulo o vacío; en otro caso, el texto tal cual
     */
    public static String nullIfEmpty(String field) {
        return field == null || field.isEmpty() ? null : field;
    }

    /**
     * Convierte un campo a {@code Double}.
     *
     * @param field campo leído del archivo
     * @return el número, o {@code null} si el campo está vacío
     * @throws NumberFormatException si el campo no es un número válido
     */
    public static Double parseDouble(String field) {
        String f = nullIfEmpty(field);
        return f == null ? null : Double.valueOf(f);
    }

    /**
     * Convierte un campo a {@code Integer}.
     *
     * @param field campo leído del archivo
     * @return el número entero, o {@code null} si el campo está vacío
     * @throws NumberFormatException si el campo no es un entero válido
     */
    public static Integer parseInteger(String field) {
        String f = nullIfEmpty(field);
        return f == null ? null : Integer.valueOf(f);
    }

    /**
     * Convierte un campo a {@link LocalDate} (formato ISO-8601, {@code yyyy-MM-dd}).
     *
     * @param field campo leído del archivo
     * @return la fecha, o {@code null} si el campo está vacío
     * @throws java.time.format.DateTimeParseException si el texto no es una fecha válida
     */
    public static LocalDate parseDate(String field) {
        String f = nullIfEmpty(field);
        return f == null ? null : LocalDate.parse(f);
    }

    /**
     * Convierte un campo a {@link LocalDateTime} (formato ISO-8601).
     *
     * @param field campo leído del archivo
     * @return la fecha y hora, o {@code null} si el campo está vacío
     * @throws java.time.format.DateTimeParseException si el texto no es una fecha y hora válida
     */
    public static LocalDateTime parseDateTime(String field) {
        String f = nullIfEmpty(field);
        return f == null ? null : LocalDateTime.parse(f);
    }

    /**
     * Convierte un campo a booleano de forma estricta: solo acepta
     * {@code "true"} o {@code "false"}. Cualquier otra cosa indica un archivo
     * corrupto.
     *
     * @param field campo leído del archivo
     * @return el valor booleano
     * @throws IllegalArgumentException si el campo no es {@code "true"} ni {@code "false"}
     */
    public static boolean parseBoolean(String field) {
        if ("true".equals(field)) {
            return true;
        }
        if ("false".equals(field)) {
            return false;
        }
        throw new IllegalArgumentException("Booleano inválido: '" + field + "'");
    }
}
