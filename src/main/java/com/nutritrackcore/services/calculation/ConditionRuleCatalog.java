package com.nutritrackcore.services.calculation;

import com.nutritrackcore.model.MedicalCondition;
import com.nutritrackcore.services.ParameterChecks;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de las reglas de negocio por condición médica.
 * <p>
 * Asocia el nombre (normalizado) de cada condición con su {@link ConditionRule}.
 * Cuando se le pide la regla de una condición que no tiene ninguna registrada,
 * devuelve {@link NoRestrictionRule} en lugar de {@code null}.
 * <p>
 * El nombre se normaliza (minúsculas, sin tildes, sin espacios sobrantes) para
 * que {@code "Diabetes tipo 2"}, {@code "diabetes tipo 2"} y
 * {@code "Diabetes Tipo 2 "} sean la misma clave.
 */
public class ConditionRuleCatalog {

    /** Reglas registradas, indexadas por nombre normalizado. */
    private final Map<String, ConditionRule> rules = new HashMap<>();

    /** Regla devuelta cuando la condición no tiene una propia. */
    private final ConditionRule fallbackRule = new NoRestrictionRule();

    /**
     * Crea un catálogo vacío. Las reglas se agregan con {@link #register}.
     */
    public ConditionRuleCatalog() {
    }

    /**
     * Crea un catálogo con las tres reglas incluidas en la primera entrega:
     * diabetes tipo 2, hipotiroidismo y sobrepeso.
     *
     * @return catálogo con las reglas por defecto ya registradas
     */
    public static ConditionRuleCatalog withDefaultRules() {
        ConditionRuleCatalog catalog = new ConditionRuleCatalog();
        catalog.register(new Type2DiabetesRule());
        catalog.register(new HypothyroidismRule());
        catalog.register(new OverweightRule());
        return catalog;
    }

    /**
     * Registra una regla bajo su nombre principal y bajo todos sus alias. Si ya
     * había una regla con ese nombre, la reemplaza.
     *
     * @param rule regla a registrar
     * @throws IllegalArgumentException si la regla, su nombre o alguno de sus alias es nulo o vacío
     */
    public void register(ConditionRule rule) {
        ParameterChecks.requireNonNull(rule, "regla de condición");
        rules.put(normalize(rule.getConditionName()), rule);
        ParameterChecks.requireNonNull(rule.getAliases(), "alias de la regla");
        for (String alias : rule.getAliases()) {
            rules.put(normalize(alias), rule);
        }
    }

    /**
     * Obtiene la regla que corresponde a una condición médica.
     * <p>
     * Normaliza el nombre de la condición y lo busca entre las reglas
     * registradas; si no hay coincidencia, devuelve la regla sin restricciones.
     *
     * @param condition condición médica del catálogo
     * @return la regla registrada o, si no existe, {@link NoRestrictionRule}; nunca {@code null}
     * @throws IllegalArgumentException si la condición es nula o no tiene nombre
     */
    public ConditionRule getFor(MedicalCondition condition) {
        ParameterChecks.requireNonNull(condition, "condición médica");
        return rules.getOrDefault(normalize(condition.getName()), fallbackRule);
    }

    /**
     * Normaliza un nombre: lo recorta, lo pasa a minúsculas y le quita las
     * tildes (descompone los caracteres y elimina las marcas diacríticas).
     *
     * @param name nombre original
     * @return nombre normalizado, usado como clave
     * @throws IllegalArgumentException si el nombre es nulo o está vacío
     */
    private static String normalize(String name) {
        String clean = ParameterChecks.requireText(name, "nombre de la condición");
        String decomposed = Normalizer.normalize(clean.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}