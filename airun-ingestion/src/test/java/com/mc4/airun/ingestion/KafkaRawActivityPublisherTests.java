package com.mc4.airun.ingestion;

import com.mc4.airun.ingestion.events.RawActivity;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaRawActivityPublisherTests {

    private static final String VERSION_ONE_JSON =
            "{\"sourceName\":\"morning.fit\",\"content\":\"AQID\"}";

    @Test
    void publishesRawActivityUsingSourceNameAsKey() {
        KafkaTemplate<String, RawActivity> kafkaTemplate = mock(KafkaTemplate.class);
        RawActivity event = activity();
        when(kafkaTemplate.send("training-topic", "morning.fit", event))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaRawActivityPublisher publisher = new KafkaRawActivityPublisher(
                kafkaTemplate,
                "training-topic"
        );

        publisher.publish(event);

        verify(kafkaTemplate).send("training-topic", "morning.fit", event);
    }

    @Test
    void serializesVersionOneKafkaContract() {
        JacksonJsonSerializer<RawActivity> serializer = new JacksonJsonSerializer<>();

        String json = new String(
                serializer.serialize("training-topic", activity()),
                StandardCharsets.UTF_8
        );

        assertThat(json).isEqualTo(VERSION_ONE_JSON);
    }

    private RawActivity activity() {
        return new RawActivity(
                "morning.fit",
                new byte[]{1, 2, 3}
        );
    }
}
