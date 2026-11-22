package com.nutritrackcore.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cambios que sufrió el plan activo al agregar una alergia: alimentos reemplazados
 * y alimentos eliminados (que no tenían reemplazo). Sirve para avisar al usuario.
 */
public class AllergyChangeResult {
    private final List<String> replaced = new ArrayList<>();
    private final List<String> removed = new ArrayList<>();

    void addReplaced(String original, String replacement, String meal) {
        replaced.add(original + " → " + replacement + " (" + meal + ")");
    }

    void addRemoved(String food, String meal) {
        removed.add(food + " (" + meal + ")");
    }

    /** Ejemplo: "Leche entera → Tofu firme (Desayuno)". */
    public List<String> getReplaced() { return Collections.unmodifiableList(replaced); }

    /** Alimentos eliminados definitivamente de la dieta. */
    public List<String> getRemoved() { return Collections.unmodifiableList(removed); }

    public boolean hasChanges() { return !replaced.isEmpty() || !removed.isEmpty(); }

    /** Aviso listo para mostrar; vacío si no hubo cambios. */
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (!replaced.isEmpty()) {
            sb.append("Se reemplazaron en tu dieta: ").append(String.join(", ", replaced)).append(". ");
        }
        if (!removed.isEmpty()) {
            sb.append("Se eliminaron de tu dieta porque no hay un reemplazo permitido: ")
              .append(String.join(", ", removed)).append(".");
        }
        return sb.toString().trim();
    }
}