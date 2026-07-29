package com.mc4.airun.activities;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ActivitiesConfiguration {

    @Bean
    FitTrainingReader fitTrainingReader() {
        return new FitTrainingReader();
    }

    @Bean
    ActivityPersistence activityPersistence(JdbcClient jdbcClient) {
        return new ActivityPersistence(jdbcClient);
    }

    @Bean
    NewTopic rawActivityTopic(
            @Value("${airun.kafka.raw-activity-topic}") String topic
    ) {
        return topic(topic);
    }

    @Bean
    NewTopic activityStoredTopic(
            @Value("${airun.kafka.activity-stored-topic}") String topic
    ) {
        return topic(topic);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
