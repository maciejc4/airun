package com.mc4.airun.training;

import com.mc4.airun.training.domain.Training;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class TrainingService {

    private static final double METERS_PER_KILOMETER = 1_000;

    private final String resourcePattern;
    private final FitTrainingReader fitReader;
    private final ResourcePatternResolver resourceResolver;

    private volatile List<Training> trainings = List.of();

    public TrainingService(
            @Value("${airun.training.resource-pattern}") String resourcePattern,
            FitTrainingReader fitReader
    ) {
        this.resourcePattern = resourcePattern;
        this.fitReader = fitReader;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
    }

    public List<Training> loadTrainings() {
        List<Training> loadedTrainings = new ArrayList<>();
        int discoveredFiles = 0;
        int failedFiles = 0;

        for (Resource resource : findTrainingResources()) {
            discoveredFiles++;
            try {
                loadedTrainings.add(readTraining(resource));
            } catch (RuntimeException exception) {
                failedFiles++;
                log.warn("Could not load training '{}': {}", sourceName(resource), exception.getMessage());
            }
        }

        loadedTrainings.sort(Comparator.comparing(Training::startedAt));
        trainings = List.copyOf(loadedTrainings);
        logLoadedTrainings(discoveredFiles, failedFiles);
        return trainings;
    }

    public List<Training> trainings() {
        return trainings;
    }

    private List<Resource> findTrainingResources() {
        try {
            return Arrays.stream(resourceResolver.getResources(resourcePattern))
                    .filter(Resource::isReadable)
                    .sorted(Comparator.comparing(this::sourceName))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not scan training resources: " + resourcePattern,
                    exception
            );
        }
    }

    private Training readTraining(Resource resource) {
        try {
            return fitReader.read(sourceName(resource), resource.getContentAsByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not read resource " + resource.getDescription(),
                    exception
            );
        }
    }

    private void logLoadedTrainings(int discoveredFiles, int failedFiles) {
        log.info(
                "Loaded {} of {} training files ({} failed)",
                trainings.size(),
                discoveredFiles,
                failedFiles
        );
        trainings.forEach(this::logTraining);
    }

    private void logTraining(Training training) {
        log.info(
                "Training '{}': started={}, sports={}, sessions={}, distance={} km, duration={}",
                training.sourceName(),
                training.startedAt(),
                String.join(", ", training.sports()),
                training.sessions().size(),
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        training.totalDistanceMeters() / METERS_PER_KILOMETER
                ),
                training.totalElapsedTime()
        );
    }

    private String sourceName(Resource resource) {
        String filename = resource.getFilename();
        return filename == null ? resource.getDescription() : filename;
    }
}
