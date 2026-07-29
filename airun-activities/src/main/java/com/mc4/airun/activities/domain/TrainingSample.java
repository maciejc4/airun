package com.mc4.airun.activities.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record TrainingSample(
        Instant recordedAt,
        OptionalInt heartRate,
        OptionalDouble distanceMeters,
        OptionalDouble speedMetersPerSecond
) {

    public TrainingSample {
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        Objects.requireNonNull(heartRate, "heartRate must not be null");
        Objects.requireNonNull(distanceMeters, "distanceMeters must not be null");
        Objects.requireNonNull(speedMetersPerSecond, "speedMetersPerSecond must not be null");
    }
}
