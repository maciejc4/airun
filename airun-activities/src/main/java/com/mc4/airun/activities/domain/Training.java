package com.mc4.airun.activities.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record Training(
        String sourceName,
        List<TrainingSession> sessions
) {

    private static final double METERS_PER_KILOMETER = 1_000;

    public Training {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Training source name must not be blank");
        }
        sessions = List.copyOf(Objects.requireNonNull(sessions, "sessions must not be null"));
        if (sessions.isEmpty()) {
            throw new IllegalArgumentException("Training must contain at least one session");
        }
    }

    public Instant startedAt() {
        return sessions.stream()
                .map(TrainingSession::startedAt)
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    public double totalDistanceMeters() {
        return sessions.stream()
                .mapToDouble(TrainingSession::distanceMeters)
                .sum();
    }

    public Duration totalElapsedTime() {
        return sessions.stream()
                .map(TrainingSession::elapsedTime)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public List<String> sports() {
        return sessions.stream()
                .map(TrainingSession::sport)
                .distinct()
                .toList();
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "Training '%s': started=%s, sports=%s, sessions=%d, distance=%.2f km, duration=%s",
                sourceName,
                startedAt(),
                String.join(", ", sports()),
                sessions.size(),
                totalDistanceMeters() / METERS_PER_KILOMETER,
                totalElapsedTime()
        );
    }
}
