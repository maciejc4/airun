package com.mc4.airun.ingestion;

import com.mc4.airun.ingestion.events.RawActivity;
import com.mc4.airun.ingestion.events.RawActivityPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class KafkaRawActivityPublisher implements RawActivityPublisher {

    private final KafkaTemplate<String, RawActivity> kafkaTemplate;
    private final String topic;

    public KafkaRawActivityPublisher(
            KafkaTemplate<String, RawActivity> kafkaTemplate,
            @Value("${airun.kafka.raw-activity-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(RawActivity event) {
        try {
            kafkaTemplate.send(topic, event.sourceName(), event).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing raw activity", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not publish raw activity", exception.getCause());
        }
    }
}
