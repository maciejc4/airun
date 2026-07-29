package com.mc4.airun.training.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public record TrainingSession(
        Instant startedAt,
        String sport,
        Duration elapsedTime,
        double distanceMeters,
        OptionalInt averageHeartRate,
        OptionalInt maximumHeartRate,
        List<TrainingSample> samples
) {

    public TrainingSession {
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        if (sport == null || sport.isBlank()) {
            throw new IllegalArgumentException("Sport must not be blank");
        }
        Objects.requireNonNull(elapsedTime, "elapsedTime must not be null");
        if (elapsedTime.isNegative()) {
            throw new IllegalArgumentException("Elapsed time must not be negative");
        }
        if (!Double.isFinite(distanceMeters) || distanceMeters < 0) {
            throw new IllegalArgumentException("Distance must be a finite, non-negative number");
        }
        Objects.requireNonNull(averageHeartRate, "averageHeartRate must not be null");
        Objects.requireNonNull(maximumHeartRate, "maximumHeartRate must not be null");
        samples = List.copyOf(Objects.requireNonNull(samples, "samples must not be null"));
    }
}
