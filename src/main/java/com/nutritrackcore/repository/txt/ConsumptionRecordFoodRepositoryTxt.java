package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.ConsumptionRecordByFood;
import com.nutritrackcore.repository.ConsumptionRecordFoodRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link ConsumptionRecordFoodRepository} sobre
 * {@code consumption_record_foods.txt} (tabla {@code RegistroConsumo_Alimento}).
 * <p>
 * Formato: {@code id_registro_alimento|id_registro|id_alimento|gramos_estimados}.
 */
public class ConsumptionRecordFoodRepositoryTxt extends AbstractTxtRepository<ConsumptionRecordByFood>
        implements ConsumptionRecordFoodRepository {

    /** Cabecera del archivo. */
    public static final String HEADER = "#id_registro_alimento|id_registro|id_alimento|gramos_estimados";

    /**
     * @param file archivo {@code consumption_record_foods.txt}
     */
    public ConsumptionRecordFoodRepositoryTxt(TxtFile file) {
        super(file, "consumption_record_foods.txt", 4);
    }

    @Override
    public ConsumptionRecordByFood save(ConsumptionRecordByFood line) throws PersistenceExceptions {
        List<ConsumptionRecordByFood> all = readAll();
        for (ConsumptionRecordByFood l : all) {          // unique (id_registro, id_alimento)
            if (l.getRecordId() == line.getRecordId() && l.getFoodId() == line.getFoodId()) {
                throw new PersistenceExceptions("El registro " + line.getRecordId()
                        + " ya tiene el alimento " + line.getFoodId());
            }
        }
        line.setConsumptionRecordByFoodId(nextId(all, ConsumptionRecordByFood::getconsumptionRecordByFoodId));
        all.add(line);
        writeAll(all);
        return line;
    }

    @Override
    public void update(ConsumptionRecordByFood line) throws PersistenceExceptions {
        List<ConsumptionRecordByFood> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getconsumptionRecordByFoodId() == line.getconsumptionRecordByFoodId()) {
                all.set(i, line);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe la línea de consumo con id " + line.getconsumptionRecordByFoodId());
    }

    @Override
    public Optional<ConsumptionRecordByFood> findById(int id) throws PersistenceExceptions {
        for (ConsumptionRecordByFood l : readAll()) {
            if (l.getconsumptionRecordByFoodId() == id) {
                return Optional.of(l);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ConsumptionRecordByFood> findByRecordId(int recordId) throws PersistenceExceptions {
        List<ConsumptionRecordByFood> result = new ArrayList<>();
        for (ConsumptionRecordByFood l : readAll()) {
            if (l.getRecordId() == recordId) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public void deleteById(int id) throws PersistenceExceptions {
        List<ConsumptionRecordByFood> all = readAll();
        if (all.removeIf(l -> l.getconsumptionRecordByFoodId() == id)) {
            writeAll(all);
        }
    }

    @Override
    public void deleteByRecordId(int recordId) throws PersistenceExceptions {
        List<ConsumptionRecordByFood> all = readAll();
        if (all.removeIf(l -> l.getRecordId() == recordId)) {
            writeAll(all);
        }
    }

    @Override
    protected String toLine(ConsumptionRecordByFood l) {
        return TxtFormat.join(
                String.valueOf(l.getconsumptionRecordByFoodId()),
                String.valueOf(l.getRecordId()),
                String.valueOf(l.getFoodId()),
                String.valueOf(l.getEstimatedGrams()));
    }

    @Override
    protected ConsumptionRecordByFood fromFields(String[] c) {
        ConsumptionRecordByFood l = new ConsumptionRecordByFood();
        l.setConsumptionRecordByFoodId(Integer.parseInt(c[0]));
        l.setRecordId(Integer.parseInt(c[1]));
        l.setFoodId(Integer.parseInt(c[2]));
        l.setEstimatedGrams(Double.parseDouble(c[3]));
        return l;
    }
}
