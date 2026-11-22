package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.MedicalConditionRule;
import com.nutritrackcore.repository.MedicalConditionRuleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link MedicalConditionRuleRepository} sobre
 * {@code condition_rules.txt} (tabla {@code ReglaCondicion}).
 * <p>
 * Formato: {@code id_regla|id_condicion|tipo_regla|nutriente|valor_limite|descripcion}.
 * En las reglas {@code solo_bajo_impacto} el nutriente y el valor límite van vacíos.
 */
public class MedicalConditionRuleRepositoryTxt extends AbstractTxtRepository<MedicalConditionRule>
        implements MedicalConditionRuleRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_regla|id_condicion|tipo_regla|nutriente|valor_limite|descripcion";

    /**
     * @param file archivo {@code condition_rules.txt}
     */
    public MedicalConditionRuleRepositoryTxt(TxtFile file) {
        super(file, "condition_rules.txt", 6);
    }

    @Override
    public MedicalConditionRule save(MedicalConditionRule rule) throws PersistenceExceptions {
        List<MedicalConditionRule> all = readAll();
        rule.setRuleId(nextId(all, MedicalConditionRule::getRuleId));
        all.add(rule);
        writeAll(all);
        return rule;
    }

    @Override
    public void update(MedicalConditionRule rule) throws PersistenceExceptions {
        List<MedicalConditionRule> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRuleId() == rule.getRuleId()) {
                all.set(i, rule);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe la regla con id " + rule.getRuleId());
    }

    @Override
    public Optional<MedicalConditionRule> findById(int ruleId) throws PersistenceExceptions {
        for (MedicalConditionRule r : readAll()) {
            if (r.getRuleId() == ruleId) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<MedicalConditionRule> findByConditionId(int conditionId) throws PersistenceExceptions {
        List<MedicalConditionRule> result = new ArrayList<>();
        for (MedicalConditionRule r : readAll()) {
            if (r.getConditionId() == conditionId) {
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public void deleteById(int ruleId) throws PersistenceExceptions {
        List<MedicalConditionRule> all = readAll();
        if (all.removeIf(r -> r.getRuleId() == ruleId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(MedicalConditionRule r) {
        return TxtFormat.join(
                String.valueOf(r.getRuleId()),
                String.valueOf(r.getConditionId()),
                r.getRuleType(),
                r.getNutrient(),                                            // puede ser nulo -> campo vacío
                r.getLimitValue() == null ? null : r.getLimitValue().toPlainString(),
                r.getDescription());
    }

    @Override
    protected MedicalConditionRule fromFields(String[] c) {
        String limit = TxtFormat.nullIfEmpty(c[4]);
        MedicalConditionRule r = new MedicalConditionRule(
                Integer.parseInt(c[1]),
                TxtFormat.nullIfEmpty(c[2]),
                TxtFormat.nullIfEmpty(c[3]),
                limit == null ? null : new BigDecimal(limit),
                TxtFormat.nullIfEmpty(c[5]));
        r.setRuleId(Integer.parseInt(c[0]));
        return r;
    }
}
