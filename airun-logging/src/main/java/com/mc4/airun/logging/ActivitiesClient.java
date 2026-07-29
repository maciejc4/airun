package com.mc4.airun.logging;

import com.mc4.airun.logging.domain.Training;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ActivitiesClient {

    private final RestClient restClient;

    public ActivitiesClient(
            RestClient.Builder restClientBuilder,
            @Value("${airun.activities.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public Training training(String sourceName) {
        return restClient.get()
                .uri("/api/trainings/{sourceName}", sourceName)
                .retrieve()
                .body(Training.class);
    }
}
