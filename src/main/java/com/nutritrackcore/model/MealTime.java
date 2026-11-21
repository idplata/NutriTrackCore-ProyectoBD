package com.nutritrackcore.model;

/**
 * Momentos del día para las ingestas. Corresponde al catálogo {@code TipoComida}:
 * cada valor guarda su {@code orden_dia} y su nombre, así la capa repository
 * puede traducir entre el enum y la tabla sin otra clase.
 */
public enum MealTime {
    BREAKFAST(1, "Desayuno"),
    MID_MORNING(2, "Media mañana"),
    LUNCH(3, "Almuerzo"),
    SNACK(4, "Merienda"),
    DINNER(5, "Cena");

    /** Posición en el día (columna {@code orden_dia}). */
    private final int dayOrder;

    /** Nombre en el catálogo (columna {@code nombre}). */
    private final String displayName;

    MealTime(int dayOrder, String displayName) {
        this.dayOrder = dayOrder;
        this.displayName = displayName;
    }

    /** @return la posición en el día, igual a {@code orden_dia} */
    public int getDayOrder() { return dayOrder; }

    /** @return el nombre del catálogo, por ejemplo "Desayuno" */
    public String getDisplayName() { return displayName; }

    /**
     * Busca el valor que corresponde a un {@code orden_dia}.
     *
     * @param dayOrder posición en el día
     * @return el momento del día
     * @throws IllegalArgumentException si ningún valor tiene ese orden
     */
    public static MealTime fromDayOrder(int dayOrder) {
        for (MealTime m : values()) {
            if (m.dayOrder == dayOrder) {
                return m;
            }
        }
        throw new IllegalArgumentException("No existe un tipo de comida con orden " + dayOrder);
    }
}
