package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import com.mc4.airun.activities.domain.TrainingSample;
import com.mc4.airun.activities.domain.TrainingSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class ActivityPersistence {

    private static final Logger log = LoggerFactory.getLogger(ActivityPersistence.class);

    private final JdbcClient jdbcClient;

    public ActivityPersistence(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(Training training) {
        Long trainingId = insertTraining(training);
        if (trainingId == null) {
            log.info("Training '{}' is already stored", training.sourceName());
            return;
        }

        for (int sessionIndex = 0; sessionIndex < training.sessions().size(); sessionIndex++) {
            TrainingSession session = training.sessions().get(sessionIndex);
            long sessionId = insertSession(trainingId, sessionIndex, session);
            insertSamples(sessionId, session);
        }
        log.info("Stored training '{}'", training.sourceName());
    }

    public Optional<Training> findBySourceName(String sourceName) {
        return jdbcClient.sql("""
                        SELECT id, source_name
                        FROM training
                        WHERE source_name = :sourceName
                        """)
                .param("sourceName", sourceName)
                .query((resultSet, rowNumber) -> toTraining(
                        resultSet.getLong("id"),
                        resultSet.getString("source_name")
                ))
                .optional();
    }

    public List<Training> findAll() {
        return jdbcClient.sql("""
                        SELECT id, source_name
                        FROM training
                        ORDER BY started_at, id
                        """)
                .query((resultSet, rowNumber) -> toTraining(
                        resultSet.getLong("id"),
                        resultSet.getString("source_name")
                ))
                .list();
    }

    private Long insertTraining(Training training) {
        boolean alreadyStored = jdbcClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM training
                        WHERE source_name = :sourceName
                        """)
                .param("sourceName", training.sourceName())
                .query(Boolean.class)
                .single();
        if (alreadyStored) {
            return null;
        }

        jdbcClient.sql("""
                        INSERT INTO training (
                            source_name,
                            started_at,
                            elapsed_time_millis,
                            distance_meters
                        )
                        VALUES (:sourceName, :startedAt, :elapsedTimeMillis, :distanceMeters)
                        """)
                .param("sourceName", training.sourceName())
                .param("startedAt", timestamp(training.startedAt()))
                .param("elapsedTimeMillis", training.totalElapsedTime().toMillis())
                .param("distanceMeters", training.totalDistanceMeters())
                .update();
        return jdbcClient.sql("SELECT id FROM training WHERE source_name = :sourceName")
                .param("sourceName", training.sourceName())
                .query(Long.class)
                .single();
    }

    private long insertSession(long trainingId, int sessionIndex, TrainingSession session) {
        jdbcClient.sql("""
                        INSERT INTO training_session (
                            training_id,
                            session_index,
                            started_at,
                            sport,
                            elapsed_time_millis,
                            distance_meters,
                            average_heart_rate,
                            maximum_heart_rate
                        )
                        VALUES (
                            :trainingId,
                            :sessionIndex,
                            :startedAt,
                            :sport,
                            :elapsedTimeMillis,
                            :distanceMeters,
                            :averageHeartRate,
                            :maximumHeartRate
                        )
                        """)
                .param("trainingId", trainingId)
                .param("sessionIndex", sessionIndex)
                .param("startedAt", timestamp(session.startedAt()))
                .param("sport", session.sport())
                .param("elapsedTimeMillis", session.elapsedTime().toMillis())
                .param("distanceMeters", session.distanceMeters())
                .param(
                        "averageHeartRate",
                        session.averageHeartRate().isPresent()
                                ? session.averageHeartRate().getAsInt()
                                : null,
                        Types.INTEGER
                )
                .param(
                        "maximumHeartRate",
                        session.maximumHeartRate().isPresent()
                                ? session.maximumHeartRate().getAsInt()
                                : null,
                        Types.INTEGER
                )
                .update();
        return jdbcClient.sql("""
                        SELECT id
                        FROM training_session
                        WHERE training_id = :trainingId AND session_index = :sessionIndex
                        """)
                .param("trainingId", trainingId)
                .param("sessionIndex", sessionIndex)
                .query(Long.class)
                .single();
    }

    private void insertSamples(long sessionId, TrainingSession session) {
        for (int sampleIndex = 0; sampleIndex < session.samples().size(); sampleIndex++) {
            insertSample(sessionId, sampleIndex, session.samples().get(sampleIndex));
        }
    }

    private void insertSample(long sessionId, int sampleIndex, TrainingSample sample) {
        jdbcClient.sql("""
                        INSERT INTO training_sample (
                            session_id,
                            sample_index,
                            recorded_at,
                            heart_rate,
                            distance_meters,
                            speed_meters_per_second
                        )
                        VALUES (
                            :sessionId,
                            :sampleIndex,
                            :recordedAt,
                            :heartRate,
                            :distanceMeters,
                            :speedMetersPerSecond
                        )
                        """)
                .param("sessionId", sessionId)
                .param("sampleIndex", sampleIndex)
                .param("recordedAt", timestamp(sample.recordedAt()))
                .param(
                        "heartRate",
                        sample.heartRate().isPresent() ? sample.heartRate().getAsInt() : null,
                        Types.INTEGER
                )
                .param(
                        "distanceMeters",
                        sample.distanceMeters().isPresent() ? sample.distanceMeters().getAsDouble() : null,
                        Types.DOUBLE
                )
                .param(
                        "speedMetersPerSecond",
                        sample.speedMetersPerSecond().isPresent()
                                ? sample.speedMetersPerSecond().getAsDouble()
                                : null,
                        Types.DOUBLE
                )
                .update();
    }

    private Training toTraining(long trainingId, String sourceName) {
        List<TrainingSession> sessions = jdbcClient.sql("""
                        SELECT
                            id,
                            started_at,
                            sport,
                            elapsed_time_millis,
                            distance_meters,
                            average_heart_rate,
                            maximum_heart_rate
                        FROM training_session
                        WHERE training_id = :trainingId
                        ORDER BY session_index
                        """)
                .param("trainingId", trainingId)
                .query((resultSet, rowNumber) -> new TrainingSession(
                        resultSet.getObject("started_at", OffsetDateTime.class).toInstant(),
                        resultSet.getString("sport"),
                        Duration.ofMillis(resultSet.getLong("elapsed_time_millis")),
                        resultSet.getDouble("distance_meters"),
                        optionalInt(resultSet.getObject("average_heart_rate", Integer.class)),
                        optionalInt(resultSet.getObject("maximum_heart_rate", Integer.class)),
                        samples(resultSet.getLong("id"))
                ))
                .list();
        return new Training(sourceName, sessions);
    }

    private List<TrainingSample> samples(long sessionId) {
        return jdbcClient.sql("""
                        SELECT
                            recorded_at,
                            heart_rate,
                            distance_meters,
                            speed_meters_per_second
                        FROM training_sample
                        WHERE session_id = :sessionId
                        ORDER BY sample_index
                        """)
                .param("sessionId", sessionId)
                .query((resultSet, rowNumber) -> new TrainingSample(
                        resultSet.getObject("recorded_at", OffsetDateTime.class).toInstant(),
                        optionalInt(resultSet.getObject("heart_rate", Integer.class)),
                        optionalDouble(resultSet.getObject("distance_meters", Double.class)),
                        optionalDouble(resultSet.getObject("speed_meters_per_second", Double.class))
                ))
                .list();
    }

    private OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    private OptionalDouble optionalDouble(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    private OffsetDateTime timestamp(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
