package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import com.nutritrackcore.model.DietPlan;
import com.nutritrackcore.model.DietPlanFood;
import com.nutritrackcore.model.MealTime;
import com.nutritrackcore.repository.DietPlanRepository;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de {@link DietPlanRepository} sobre dos archivos:
 * {@code planes_dieta.txt} (el plan) y {@code plan_dieta_alimento.txt} (sus
 * líneas).
 * <p>
 * Formatos de línea:
 * <ul>
 *   <li>Plan: {@code id_plan|id_usuario|fecha_generacion|calorias_objetivo|activo}.</li>
 *   <li>Línea: {@code id_plan|id_alimento|cantidad_g|momento_comida}.</li>
 * </ul>
 * Al leer, cada plan recibe sus líneas agrupándolas por {@code id_plan}; al
 * escribir, se reescriben ambos archivos completos.
 */
public class DietPlanRepositoryTxt implements DietPlanRepository {

    /** Cabecera del archivo {@code planes_dieta.txt}. */
    public static final String PLANS_HEADER =
            "#id_plan|id_usuario|fecha_generacion|calorias_objetivo|activo";

    /** Cabecera del archivo {@code plan_dieta_alimento.txt}. */
    public static final String LINES_HEADER =
            "#id_plan|id_alimento|cantidad_g|momento_comida";

    /**
     * Orden estándar de los planes: por fecha de generación (las nulas van
     * primero) y, en caso de empate, por id.
     */
    private static final Comparator<DietPlan> BY_DATE =
            Comparator.comparing(DietPlan::getGenerationDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparingInt(DietPlan::getPlanId);

    /** Acceso al archivo .txt de planes de dieta. */
    private final TxtFile plansFile;

    /** Acceso al archivo .txt de las líneas (alimentos) de cada plan. */
    private final TxtFile linesFile;

    /**
     * Crea el repositorio sobre dos archivos ya preparados.
     *
     * @param plansFile archivo {@code planes_dieta.txt}
     * @param linesFile archivo {@code plan_dieta_alimento.txt}
     */
    public DietPlanRepositoryTxt(TxtFile plansFile, TxtFile linesFile) {
        this.plansFile = plansFile;
        this.linesFile = linesFile;
    }

    /**
     * {@inheritDoc}
     * <p>
     * El id se calcula como el máximo id existente + 1 y se copia a cada línea
     * del plan al escribir.
     */
    @Override
    public DietPlan save(DietPlan plan) throws PersistenceExceptions {
        List<DietPlan> all = readAll();
        List<Integer> ids = new ArrayList<>();
        for (DietPlan p : all) {
            ids.add(p.getPlanId());
        }
        plan.setPlanId(IdGenerator.next(ids));      // writeAll copia este id a cada línea
        all.add(plan);
        writeAll(all);
        return plan;
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenceExceptions si no existe un plan con ese id
     */
    @Override
    public void update(DietPlan plan) throws PersistenceExceptions {
        List<DietPlan> all = readAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPlanId() == plan.getPlanId()) {
                all.set(i, plan);
                writeAll(all);
                return;
            }
        }
        throw new PersistenceExceptions("No existe el plan de dieta con id " + plan.getPlanId());
    }

    /** {@inheritDoc} */
    @Override
    public Optional<DietPlan> findById(int planId) throws PersistenceExceptions {
        for (DietPlan p : readAll()) {
            if (p.getPlanId() == planId) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * El resultado se ordena por fecha de generación, y en caso de empate por id.
     */
    @Override
    public List<DietPlan> findByUserId(int userId) throws PersistenceExceptions {
        List<DietPlan> result = new ArrayList<>();
        for (DietPlan p : readAll()) {
            if (p.getUserId() == userId) {
                result.add(p);
            }
        }
        result.sort(BY_DATE);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Recorre los planes del usuario ya ordenados por fecha y se queda con el
     * último que esté activo.
     */
    @Override
    public Optional<DietPlan> findActiveByUserId(int userId) throws PersistenceExceptions {
        DietPlan latest = null;
        for (DietPlan p : findByUserId(userId)) {       // ya viene ordenada: la última activa gana
            if (p.isActive()) {
                latest = p;
            }
        }
        return Optional.ofNullable(latest);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Solo reescribe los archivos si realmente se eliminó algún plan.
     */
    @Override
    public void deleteByUserId(int userId) throws PersistenceExceptions {
        List<DietPlan> all = readAll();
        if (all.removeIf(p -> p.getUserId() == userId)) {
            writeAll(all);
        }
    }

    // ---------- lectura y escritura de los dos archivos ----------

    /**
     * Lee los dos archivos y arma los planes completos. Primero agrupa las
     * líneas por {@code id_plan} y luego se las asigna a cada plan. Las líneas
     * cuyo plan no existe (huérfanas) se ignoran.
     *
     * @return lista con todos los planes, cada uno con sus líneas
     * @throws PersistenceExceptions si alguna línea de cualquiera de los archivos
     *                               es inválida o está corrupta
     */
    private List<DietPlan> readAll() throws PersistenceExceptions {
        Map<Integer, List<DietPlanFood>> linesByPlan = new HashMap<>();
        for (String text : linesFile.readLines()) {
            DietPlanFood line = lineFromText(text);
            linesByPlan.computeIfAbsent(line.getPlanId(), k -> new ArrayList<>()).add(line);
        }
        List<DietPlan> plans = new ArrayList<>();
        for (String text : plansFile.readLines()) {
            DietPlan plan = planFromText(text);
            plan.setLines(new ArrayList<>(linesByPlan.getOrDefault(plan.getPlanId(), new ArrayList<>())));
            plans.add(plan);
        }
        return plans;
    }

    /**
     * Escribe primero planes_dieta.txt y después las líneas, y arma todo el texto antes de
     * tocar ningún archivo. Cada archivo se reemplaza de forma segura; si algo se corta
     * entre los dos, quedan líneas huérfanas que se descartan en la siguiente escritura.
     * <p>
     * Antes de convertir cada línea, le copia el id del plan al que pertenece
     * (las líneas creadas antes de guardar el plan traen {@code planId} 0).
     *
     * @param plans planes que quedarán en los archivos, con sus líneas
     * @throws PersistenceExceptions si algún texto no se puede guardar (por ejemplo,
     *                               contiene {@code "|"}) o si falla la escritura
     */
    private void writeAll(List<DietPlan> plans) throws PersistenceExceptions {
        List<String> planTexts = new ArrayList<>();
        List<String> lineTexts = new ArrayList<>();
        try {
            for (DietPlan p : plans) {
                planTexts.add(planToText(p));
                for (DietPlanFood line : p.getLines()) {
                    line.setPlanId(p.getPlanId());          // las líneas creadas antes del save traen planId 0
                    lineTexts.add(lineToText(line));
                }
            }
        } catch (IllegalArgumentException e) {
            throw new PersistenceExceptions("No se puede guardar: " + e.getMessage(), e);
        }
        plansFile.rewrite(planTexts);
        linesFile.rewrite(lineTexts);
    }

    /**
     * Convierte un plan (sin sus líneas) en una línea de {@code planes_dieta.txt}.
     *
     * @param p plan a convertir
     * @return la línea con los cinco campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String planToText(DietPlan p) {
        return TxtFormat.join(
                String.valueOf(p.getPlanId()),
                String.valueOf(p.getUserId()),
                TxtFormat.text(p.getGenerationDate()),
                String.valueOf(p.getTargetCalories()),
                String.valueOf(p.isActive()));
    }

    /**
     * Convierte una línea de plan en una línea de {@code plan_dieta_alimento.txt}.
     * El momento de comida se escribe por el nombre del enum.
     *
     * @param l línea del plan a convertir
     * @return la línea con los cuatro campos separados por {@code "|"}
     * @throws IllegalArgumentException si algún campo contiene {@code "|"} o saltos de línea
     */
    private String lineToText(DietPlanFood l) {
        return TxtFormat.join(
                String.valueOf(l.getPlanId()),
                String.valueOf(l.getFoodId()),
                String.valueOf(l.getQuantityG()),
                TxtFormat.text(l.getMealTime() == null ? null : l.getMealTime().name()));
    }

    /**
     * Convierte una línea de {@code planes_dieta.txt} en un plan (todavía sin
     * sus líneas, que las asigna {@link #readAll()}).
     *
     * @param text línea con el formato
     *             {@code id_plan|id_usuario|fecha_generacion|calorias_objetivo|activo}
     * @return el plan leído
     * @throws PersistenceExceptions si la línea no tiene 5 campos o tiene valores inválidos
     */
    private DietPlan planFromText(String text) throws PersistenceExceptions {
        String[] c = TxtFormat.split(text);
        if (c.length != 5) {
            throw new PersistenceExceptions("Línea inválida en planes_dieta.txt: " + text);
        }
        try {
            DietPlan p = new DietPlan();
            p.setPlanId(Integer.parseInt(c[0]));
            p.setUserId(Integer.parseInt(c[1]));
            p.setGenerationDate(TxtFormat.parseDate(c[2]));
            p.setTargetCalories(Double.parseDouble(c[3]));
            p.setActive(TxtFormat.parseBoolean(c[4]));
            return p;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en planes_dieta.txt: " + text, e);
        }
    }

    /**
     * Convierte una línea de {@code plan_dieta_alimento.txt} en una línea de plan.
     * El momento de comida vacío queda como {@code null}; si tiene valor, debe
     * ser el nombre de un {@link MealTime}.
     *
     * @param text línea con el formato {@code id_plan|id_alimento|cantidad_g|momento_comida}
     * @return la línea de plan leída
     * @throws PersistenceExceptions si la línea no tiene 4 campos o tiene valores inválidos
     */
    private DietPlanFood lineFromText(String text) throws PersistenceExceptions {
        String[] c = TxtFormat.split(text);
        if (c.length != 4) {
            throw new PersistenceExceptions("Línea inválida en plan_dieta_alimento.txt: " + text);
        }
        try {
            DietPlanFood l = new DietPlanFood();
            l.setPlanId(Integer.parseInt(c[0]));
            l.setFoodId(Integer.parseInt(c[1]));
            l.setQuantityG(Double.parseDouble(c[2]));
            String meal = TxtFormat.nullIfEmpty(c[3]);
            l.setMealTime(meal == null ? null : MealTime.valueOf(meal));   // enums por nombre
            return l;
        } catch (DateTimeParseException | IllegalArgumentException e) {   // NumberFormatException ya es un IllegalArgumentException
            throw new PersistenceExceptions("Línea corrupta en plan_dieta_alimento.txt: " + text, e);
        }
    }
}
