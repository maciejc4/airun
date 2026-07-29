package com.mc4.airun.logging;

import com.mc4.airun.logging.domain.Training;
import com.mc4.airun.logging.domain.TrainingSession;
import com.mc4.airun.logging.events.ActivityStored;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaActivityStoredConsumerTests {

    private static final String VERSION_ONE_JSON = "{\"sourceName\":\"morning.fit\"}";

    @Test
    void loadsAndLogsStoredActivity() {
        ActivitiesClient activitiesClient = mock(ActivitiesClient.class);
        TrainingLogger logger = mock(TrainingLogger.class);
        KafkaActivityStoredConsumer consumer =
                new KafkaActivityStoredConsumer(activitiesClient, logger);
        Training training = training();
        when(activitiesClient.training("morning.fit")).thenReturn(training);

        consumer.consume(new ActivityStored("morning.fit"));

        verify(activitiesClient).training("morning.fit");
        verify(logger).log(training);
    }

    @Test
    void deserializesVersionOneStoredActivityContract() {
        JacksonJsonDeserializer<ActivityStored> deserializer =
                new JacksonJsonDeserializer<>(ActivityStored.class);

        ActivityStored event = deserializer.deserialize(
                "activity-topic",
                VERSION_ONE_JSON.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(event.sourceName()).isEqualTo("morning.fit");
    }

    private Training training() {
        return new Training(
                "morning.fit",
                List.of(new TrainingSession(
                        Instant.parse("2026-07-29T08:00:00Z"),
                        "RUNNING",
                        Duration.ofMinutes(30),
                        5_000,
                        OptionalInt.of(140),
                        OptionalInt.of(165),
                        List.of()
                ))
        );
    }
}
