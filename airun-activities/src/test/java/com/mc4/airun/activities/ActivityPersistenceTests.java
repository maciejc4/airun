package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import com.mc4.airun.activities.domain.TrainingSample;
import com.mc4.airun.activities.domain.TrainingSession;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityPersistenceTests {

    ActivityPersistence persistence;

    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpDatabase() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:storage-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__create_training_tables.sql")
        ).execute(dataSource);
        persistence = new ActivityPersistence(JdbcClient.create(dataSource));
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void storesTrainingStructureAndIgnoresDuplicateSource() {
        Training training = training();

        persistence.save(training);
        persistence.save(training);

        assertThat(count("training")).isEqualTo(1);
        assertThat(count("training_session")).isEqualTo(1);
        assertThat(count("training_sample")).isEqualTo(1);
        assertThat(persistence.findBySourceName("persistence-test.fit"))
                .contains(training);
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private Training training() {
        Instant startedAt = Instant.parse("2026-07-29T08:00:00Z");
        return new Training(
                "persistence-test.fit",
                List.of(new TrainingSession(
                        startedAt,
                        "RUNNING",
                        Duration.ofMinutes(30),
                        5_000,
                        OptionalInt.of(140),
                        OptionalInt.of(165),
                        List.of(new TrainingSample(
                                startedAt,
                                OptionalInt.of(140),
                                OptionalDouble.of(0),
                                OptionalDouble.of(3.2)
                        ))
                ))
        );
    }
}
