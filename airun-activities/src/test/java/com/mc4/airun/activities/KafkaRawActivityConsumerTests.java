package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import com.mc4.airun.activities.domain.TrainingSession;
import com.mc4.airun.activities.events.ActivityStored;
import com.mc4.airun.activities.events.RawActivity;
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

class KafkaRawActivityConsumerTests {

    private static final String VERSION_ONE_JSON =
            "{\"sourceName\":\"morning.fit\",\"content\":\"AQID\"}";

    @Test
    void parsesStoresAndPublishesConsumedActivity() {
        ActivityService service = mock(ActivityService.class);
        ActivityStoredPublisher publisher = mock(ActivityStoredPublisher.class);
        KafkaRawActivityConsumer consumer = new KafkaRawActivityConsumer(service, publisher);
        RawActivity event = new RawActivity("morning.fit", new byte[]{1, 2, 3});
        when(service.ingest(event)).thenReturn(training());

        consumer.consume(event);

        verify(service).ingest(event);
        verify(publisher).publish(new ActivityStored("morning.fit"));
    }

    @Test
    void deserializesVersionOneRawActivityContract() {
        JacksonJsonDeserializer<RawActivity> deserializer =
                new JacksonJsonDeserializer<>(RawActivity.class);

        RawActivity event = deserializer.deserialize(
                "activity-topic",
                VERSION_ONE_JSON.getBytes(StandardCharsets.UTF_8)
        );

        assertThat(event.sourceName()).isEqualTo("morning.fit");
        assertThat(event.content()).containsExactly(1, 2, 3);
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
