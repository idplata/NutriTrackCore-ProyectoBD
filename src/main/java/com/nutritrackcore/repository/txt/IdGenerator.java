package com.nutritrackcore.repository.txt;

import java.util.Collection;

/**
 * Generador de identificadores para los repositorios .txt.
 * <p>
 * Sin base de datos no hay autoincremento: el siguiente id es el máximo
 * existente + 1. Es una clase de utilidad: no se puede instanciar.
 */
public final class IdGenerator {

    /** Constructor privado: es una clase de utilidad y no debe instanciarse. */
    private IdGenerator() {}

    /**
     * Calcula el siguiente id disponible.
     *
     * @param existingIds ids que ya existen en el archivo (puede estar vacía)
     * @return el mayor id existente más uno, o {@code 1} si no hay ids
     */
    public static int next(Collection<Integer> existingIds) {
        int max = 0;
        for (int id : existingIds) {
            if (id > max) {
                max = id;
            }
        }
        return max + 1;
    }
}
