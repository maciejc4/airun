package com.mc4.airun.logging;

import com.mc4.airun.logging.domain.Training;
import com.mc4.airun.logging.events.ActivityStored;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaActivityStoredConsumer {

    private final ActivitiesClient activitiesClient;
    private final TrainingLogger logger;

    public KafkaActivityStoredConsumer(ActivitiesClient activitiesClient, TrainingLogger logger) {
        this.activitiesClient = activitiesClient;
        this.logger = logger;
    }

    @KafkaListener(topics = "${airun.kafka.activity-stored-topic}")
    public void consume(ActivityStored event) {
        Training training = activitiesClient.training(event.sourceName());
        logger.log(training);
    }
}
