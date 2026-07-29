package com.mc4.airun.training;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decoder;
import com.garmin.fit.MesgListener;
import com.garmin.fit.MesgNum;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.mc4.airun.training.domain.Training;
import com.mc4.airun.training.domain.TrainingSample;
import com.mc4.airun.training.domain.TrainingSession;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Component
public class FitTrainingReader {

    public Training read(String sourceName, byte[] content) {
        List<SessionMesg> sessionMessages = new ArrayList<>();
        List<TrainingSample> samples = new ArrayList<>();

        try {
            Decoder decoder = new Decoder(content);
            decoder.addListener((MesgListener) message -> collect(message, sessionMessages, samples));
            decoder.read();
            return toTraining(sourceName, sessionMessages, samples);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Cannot decode FIT training '%s': %s".formatted(sourceName, exception.getMessage()),
                    exception
            );
        }
    }

    private void collect(
            com.garmin.fit.Mesg message,
            List<SessionMesg> sessions,
            List<TrainingSample> samples
    ) {
        if (message.getNum() == MesgNum.SESSION) {
            sessions.add(new SessionMesg(message));
        } else if (message.getNum() == MesgNum.RECORD) {
            RecordMesg record = new RecordMesg(message);
            if (record.getTimestamp() != null) {
                samples.add(toSample(record));
            }
        }
    }

    private Training toTraining(
            String sourceName,
            List<SessionMesg> sessionMessages,
            List<TrainingSample> allSamples
    ) {
        if (sessionMessages.isEmpty()) {
            throw new IllegalArgumentException("FIT file contains no training sessions");
        }

        List<TrainingSession> sessions = sessionMessages.stream()
                .map(session -> toSession(session, allSamples, sessionMessages.size()))
                .sorted(Comparator.comparing(TrainingSession::startedAt))
                .toList();
        return new Training(sourceName, sessions);
    }

    private TrainingSession toSession(
            SessionMesg session,
            List<TrainingSample> allSamples,
            int sessionCount
    ) {
        Instant startedAt = toInstant(session.getStartTime());
        Duration elapsedTime = duration(session.getTotalElapsedTime());
        Instant endedAt = session.getTimestamp() == null
                ? startedAt.plus(elapsedTime)
                : toInstant(session.getTimestamp());
        List<TrainingSample> sessionSamples = samplesForSession(
                allSamples,
                startedAt,
                endedAt,
                sessionCount
        );

        return new TrainingSession(
                startedAt,
                session.getSport() == null ? "UNKNOWN" : session.getSport().name(),
                elapsedTime,
                distance(session.getTotalDistance(), sessionSamples),
                heartRate(session.getAvgHeartRate(), sessionSamples, false),
                heartRate(session.getMaxHeartRate(), sessionSamples, true),
                sessionSamples
        );
    }

    private List<TrainingSample> samplesForSession(
            List<TrainingSample> allSamples,
            Instant startedAt,
            Instant endedAt,
            int sessionCount
    ) {
        if (sessionCount == 1) {
            return List.copyOf(allSamples);
        }
        return allSamples.stream()
                .filter(sample -> !sample.recordedAt().isBefore(startedAt))
                .filter(sample -> !sample.recordedAt().isAfter(endedAt))
                .toList();
    }

    private TrainingSample toSample(RecordMesg record) {
        return new TrainingSample(
                toInstant(record.getTimestamp()),
                optionalInt(record.getHeartRate()),
                optionalDouble(record.getDistance()),
                optionalDouble(record.getEnhancedSpeed() != null
                        ? record.getEnhancedSpeed()
                        : record.getSpeed())
        );
    }

    private OptionalInt heartRate(
            Short summaryValue,
            List<TrainingSample> samples,
            boolean maximum
    ) {
        if (summaryValue != null) {
            return OptionalInt.of(Short.toUnsignedInt(summaryValue));
        }

        IntSummaryStatistics heartRates = samples.stream()
                .map(TrainingSample::heartRate)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .summaryStatistics();
        if (heartRates.getCount() == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(maximum
                ? heartRates.getMax()
                : (int) Math.round(heartRates.getAverage()));
    }

    private double distance(Float summaryDistance, List<TrainingSample> samples) {
        if (summaryDistance != null) {
            return summaryDistance;
        }
        return samples.stream()
                .map(TrainingSample::distanceMeters)
                .filter(OptionalDouble::isPresent)
                .mapToDouble(OptionalDouble::getAsDouble)
                .max()
                .orElse(0);
    }

    private Duration duration(Float seconds) {
        if (seconds == null) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(Math.round(seconds * 1_000));
    }

    private Instant toInstant(DateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("FIT session has no start time");
        }
        return dateTime.getDate().toInstant();
    }

    private OptionalInt optionalInt(Short value) {
        return value == null
                ? OptionalInt.empty()
                : OptionalInt.of(Short.toUnsignedInt(value));
    }

    private OptionalDouble optionalDouble(Float value) {
        return value == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(value);
    }
}
