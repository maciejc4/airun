package com.mc4.airun.ingestion;

import com.mc4.airun.ingestion.events.RawActivityPublisher;
import com.mc4.airun.ingestion.monitoring.TrainingFolderMonitor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.nio.file.Path;

@Configuration
public class IngestionConfiguration {

    @Bean
    TrainingFolderMonitor trainingFolderMonitor(
            @Value("${airun.ingestion.directory}") Path directory,
            RawActivityPublisher publisher
    ) {
        return new TrainingFolderMonitor(directory, publisher);
    }

    @Bean
    NewTopic rawActivityTopic(
            @Value("${airun.kafka.raw-activity-topic}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
