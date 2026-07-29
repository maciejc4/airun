package com.mc4.airun.activities;

import com.mc4.airun.activities.events.ActivityStored;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class KafkaActivityStoredPublisher implements ActivityStoredPublisher {

    private final KafkaTemplate<String, ActivityStored> kafkaTemplate;
    private final String topic;

    public KafkaActivityStoredPublisher(
            KafkaTemplate<String, ActivityStored> kafkaTemplate,
            @Value("${airun.kafka.activity-stored-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(ActivityStored event) {
        try {
            kafkaTemplate.send(topic, event.sourceName(), event).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing stored activity", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not publish stored activity", exception.getCause());
        }
    }
}
