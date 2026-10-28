package com.nutritrackcore.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Implementación de {@link PasswordHasher} basada en PBKDF2 con HMAC-SHA256,
 * un algoritmo incluido en el JDK (no necesita librerías externas).
 * <p>
 * Formato del texto guardado: {@code iteraciones:sal:hash}, donde la sal y el
 * hash están codificados en Base64. Guardar las iteraciones junto al hash
 * permite subir el costo en el futuro sin invalidar las contraseñas ya
 * guardadas.
 * <p>
 * Nota para el modelo de datos: el resultado ocupa unos 80 caracteres, por lo
 * que la columna {@code contraseña_hash} de la futura base de datos no puede
 * ser {@code CHAR(50)}.
 */
public class Sha256PasswordHasher implements PasswordHasher {

    /** Nombre del algoritmo registrado en el JDK. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Número de iteraciones al generar un hash nuevo (más iteraciones = más lento de atacar). */
    private static final int ITERATIONS = 65_536;

    /** Tope de iteraciones aceptado al verificar, para no procesar un texto manipulado con valores absurdos. */
    private static final int MAX_ITERATIONS = 1_000_000;

    /** Tamaño de la sal aleatoria, en bytes. */
    private static final int SALT_BYTES = 16;

    /** Tamaño del hash resultante, en bits. */
    private static final int HASH_BITS = 256;

    /** Separador entre las tres partes del texto guardado. */
    private static final String PART_SEPARATOR = ":";

    /** Generador de números aleatorios criptográficamente seguro, reutilizado entre llamadas. */
    private final SecureRandom random = new SecureRandom();

    /**
     * Genera el hash de una contraseña.
     * <p>
     * Pasos: (1) valida que no sea nula; (2) genera una sal aleatoria de 16
     * bytes; (3) deriva el hash con PBKDF2 usando esa sal y 65.536 iteraciones;
     * (4) devuelve {@code iteraciones:sal:hash} con sal y hash en Base64.
     *
     * @param plain contraseña en texto plano
     * @return texto con iteraciones, sal y hash
     * @throws IllegalArgumentException si la contraseña es nula
     * @throws IllegalStateException    si el JDK no ofrece el algoritmo PBKDF2
     */
    @Override
    public String hash(String plain) {
        ParameterChecks.requireNonNull(plain, "contraseña");
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(plain, salt, ITERATIONS);
        return ITERATIONS + PART_SEPARATOR
                + Base64.getEncoder().encodeToString(salt) + PART_SEPARATOR
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifica una contraseña contra un hash guardado.
     * <p>
     * Pasos: (1) valida que ninguno sea nulo; (2) separa el texto guardado en
     * sus tres partes, y si no son exactamente tres devuelve {@code false};
     * (3) decodifica iteraciones, sal y hash esperado (si algo no se puede
     * decodificar o las iteraciones son absurdas, devuelve {@code false});
     * (4) recalcula el hash de la contraseña escrita con esa misma sal y esas
     * iteraciones; (5) compara ambos hashes con {@link MessageDigest#isEqual},
     * que tarda lo mismo sin importar dónde difieran (evita ataques de tiempo).
     *
     * @param plain contraseña escrita por la persona
     * @param hash  texto guardado con {@link #hash(String)}
     * @return {@code true} si la contraseña es correcta
     * @throws IllegalArgumentException si alguno de los parámetros es nulo
     */
    @Override
    public boolean verify(String plain, String hash) {
        ParameterChecks.requireNonNull(plain, "contraseña");
        ParameterChecks.requireNonNull(hash, "hash guardado");
        String[] parts = hash.split(PART_SEPARATOR);
        if (parts.length != 3) {
            return false;
        }
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[0]);
            salt = Base64.getDecoder().decode(parts[1]);
            expected = Base64.getDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {   // NumberFormatException y errores de Base64 son IllegalArgumentException
            return false;
        }
        if (iterations <= 0 || iterations > MAX_ITERATIONS) {
            return false;
        }
        byte[] actual = derive(plain, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * Aplica PBKDF2-HMAC-SHA256 a una contraseña con la sal y las iteraciones
     * indicadas.
     * <p>
     * Convierte la contraseña a {@code char[]}, crea la especificación de clave,
     * obtiene la fábrica del algoritmo y deriva los bytes. En el bloque
     * {@code finally} borra la contraseña de la especificación para que no
     * quede en memoria más tiempo del necesario.
     *
     * @param plain      contraseña en texto plano
     * @param salt       sal aleatoria
     * @param iterations número de iteraciones
     * @return hash derivado (32 bytes)
     * @throws IllegalStateException si el algoritmo no existe o la especificación es inválida
     */
    private byte[] derive(String plain, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(plain.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }
}