package com.nutritrackcore.services;

import com.nutritrackcore.model.MedicalCondition;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Busca la regla de cada condición por su nombre (sin tildes ni mayúsculas).
 * Si no hay regla, devuelve {@link NoRestrictionRule}.
 */
public class ConditionRuleCatalog {
    private final Map<String, ConditionRule> rules = new HashMap<>();
    private final ConditionRule fallbackRule = new NoRestrictionRule();

    public static ConditionRuleCatalog withDefaultRules() {
        ConditionRuleCatalog catalog = new ConditionRuleCatalog();
        catalog.register(new Type2DiabetesRule());
        catalog.register(new HypothyroidismRule());
        catalog.register(new OverweightRule());
        return catalog;
    }

    public void register(ConditionRule rule) {
        Objects.requireNonNull(rule, "regla de condición");
        rules.put(normalize(rule.getConditionName()), rule);
        for (String alias : rule.getAliases()) {
            rules.put(normalize(alias), rule);
        }
    }

    public ConditionRule getFor(MedicalCondition condition) {
        Objects.requireNonNull(condition, "condición médica");
        return rules.getOrDefault(normalize(condition.getName()), fallbackRule);
    }

    /** Busca una regla por nombre; se usa para el sobrepeso detectado por IMC. */
    public Optional<ConditionRule> findByName(String name) {
        return Optional.ofNullable(rules.get(normalize(name)));
    }

    private static String normalize(String name) {
        String clean = name == null ? "" : name.trim();
        String decomposed = Normalizer.normalize(clean.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}