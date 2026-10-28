package com.nutritrackcore.services;

/**
 * Contrato para convertir contraseñas en un resumen irreversible (hash) y para
 * comprobarlas después.
 * <p>
 * Los servicios dependen de esta interfaz y no de una implementación concreta,
 * de modo que el algoritmo se pueda cambiar (o reemplazar por uno falso en las
 * pruebas) sin tocar la lógica de autenticación.
 */
public interface PasswordHasher {

    /**
     * Calcula el hash de una contraseña en texto plano.
     * <p>
     * Cada llamada produce un resultado distinto aunque la contraseña sea la
     * misma, porque la implementación debe usar una sal aleatoria que viaja
     * incluida en el texto devuelto.
     *
     * @param plain contraseña en texto plano
     * @return texto que contiene todo lo necesario para verificar la contraseña más adelante
     * @throws IllegalArgumentException si la contraseña es nula
     */
    String hash(String plain);

    /**
     * Comprueba si una contraseña en texto plano corresponde a un hash
     * generado antes con {@link #hash(String)}.
     *
     * @param plain contraseña escrita por la persona
     * @param hash  hash guardado
     * @return {@code true} si coinciden; {@code false} si no coinciden o si el hash tiene un formato inválido
     * @throws IllegalArgumentException si alguno de los dos parámetros es nulo
     */
    boolean verify(String plain, String hash);
}