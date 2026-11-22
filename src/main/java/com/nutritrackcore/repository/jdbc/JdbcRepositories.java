package com.nutritrackcore.repository.jdbc;

import com.nutritrackcore.repository.Repositories;
import javax.sql.DataSource;

/**
 * Fábrica de los 17 repositorios JDBC. Todos comparten el mismo
 * {@link DataSource} (el pool), así que piden y devuelven conexiones al mismo
 * conjunto.
 */
public final class JdbcRepositories {

    private JdbcRepositories() {
    }

    /**
     * Crea los repositorios sobre PostgreSQL.
     *
     * @param ds fuente de conexiones; normalmente {@link ConnectionPool#getDataSource()}
     * @return los 17 repositorios agrupados
     */
    public static Repositories create(DataSource ds) {
        return new Repositories(
                new UserRepositoryJdbc(ds),
                new BMIHistoryRepositoryJdbc(ds),
                new UserProfileRepositoryJdbc(ds),
                new MedicalConditionRepositoryJdbc(ds),
                new MedicalConditionRuleRepositoryJdbc(ds),
                new UserConditionRepositoryJdbc(ds),
                new AllergyRepositoryJdbc(ds),
                new UserAllergyRepositoryJdbc(ds),
                new AllergyFoodRepositoryJdbc(ds),
                new FoodRepositoryJdbc(ds),
                new UserExcludedFoodRepositoryJdbc(ds),
                new ExerciseRepositoryJdbc(ds),
                new ConsumptionRecordRepositoryJdbc(ds),
                new ConsumptionRecordFoodRepositoryJdbc(ds),
                new DietPlanRepositoryJdbc(ds),
                new ExerciseRoutineRepositoryJdbc(ds),
                new RoutineFollowUpRepositoryJdbc(ds));
    }
}
