package com.mc4.airun.logging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class LoggingConfiguration {

    @Bean
    TrainingLogger trainingLogger() {
        return new TrainingLogger();
    }

    @Bean
    NewTopic activityStoredTopic(
            @Value("${airun.kafka.activity-stored-topic}") String topic
    ) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
