package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import com.mc4.airun.activities.events.ActivityStored;
import com.mc4.airun.activities.events.RawActivity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaRawActivityConsumer {

    private final ActivityService activityService;
    private final ActivityStoredPublisher storedPublisher;

    public KafkaRawActivityConsumer(
            ActivityService activityService,
            ActivityStoredPublisher storedPublisher
    ) {
        this.activityService = activityService;
        this.storedPublisher = storedPublisher;
    }

    @KafkaListener(topics = "${airun.kafka.raw-activity-topic}")
    public void consume(RawActivity event) {
        Training training = activityService.ingest(event);
        storedPublisher.publish(new ActivityStored(training.sourceName()));
    }
}
