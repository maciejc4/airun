package com.mc4.airun.training;

import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.Event;
import com.garmin.fit.EventType;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import com.mc4.airun.training.domain.Training;
import com.mc4.airun.training.domain.TrainingSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FitTrainingReaderTests {

    @TempDir
    Path directory;

    @Test
    void decodesSessionSummaryAndRecordSamples() throws IOException {
        Path fitFile = directory.resolve("morning-run.fit");
        writeFitActivity(fitFile);

        Training training = new FitTrainingReader().read(
                "morning-run.fit",
                Files.readAllBytes(fitFile)
        );

        assertThat(training.sourceName()).isEqualTo("morning-run.fit");
        assertThat(training.sessions()).hasSize(1);

        TrainingSession session = training.sessions().getFirst();
        assertThat(session.startedAt()).isEqualTo(Instant.parse("2026-07-29T06:00:00Z"));
        assertThat(session.sport()).isEqualTo("RUNNING");
        assertThat(session.elapsedTime()).hasSeconds(600);
        assertThat(session.distanceMeters()).isEqualTo(2_000);
        assertThat(session.averageHeartRate()).hasValue(140);
        assertThat(session.maximumHeartRate()).hasValue(160);
        assertThat(session.samples()).hasSize(2);
        assertThat(session.samples().getFirst().heartRate()).hasValue(120);
        assertThat(session.samples().getLast().distanceMeters()).hasValue(2_000);
    }

    private void writeFitActivity(Path path) {
        DateTime startedAt = new DateTime(java.util.Date.from(Instant.parse("2026-07-29T06:00:00Z")));
        DateTime endedAt = new DateTime(java.util.Date.from(Instant.parse("2026-07-29T06:10:00Z")));
        FileEncoder encoder = new FileEncoder(path.toFile());

        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.ACTIVITY);
        fileId.setManufacturer(Manufacturer.DEVELOPMENT);
        fileId.setProduct(1);
        fileId.setSerialNumber(1L);
        fileId.setTimeCreated(startedAt);
        encoder.write(fileId);

        encoder.write(record(startedAt, (short) 120, 0));
        encoder.write(record(endedAt, (short) 160, 2_000));

        SessionMesg session = new SessionMesg();
        session.setMessageIndex(0);
        session.setTimestamp(endedAt);
        session.setEvent(Event.SESSION);
        session.setEventType(EventType.STOP);
        session.setStartTime(startedAt);
        session.setSport(Sport.RUNNING);
        session.setSubSport(SubSport.GENERIC);
        session.setTotalElapsedTime(600F);
        session.setTotalTimerTime(600F);
        session.setTotalDistance(2_000F);
        session.setAvgHeartRate((short) 140);
        session.setMaxHeartRate((short) 160);
        session.setFirstLapIndex(0);
        session.setNumLaps(0);
        encoder.write(session);

        ActivityMesg activity = new ActivityMesg();
        activity.setTimestamp(endedAt);
        activity.setTotalTimerTime(600F);
        activity.setNumSessions(1);
        activity.setType(Activity.MANUAL);
        activity.setEvent(Event.ACTIVITY);
        activity.setEventType(EventType.STOP);
        encoder.write(activity);
        encoder.close();
    }

    private RecordMesg record(DateTime timestamp, short heartRate, float distance) {
        RecordMesg record = new RecordMesg();
        record.setTimestamp(timestamp);
        record.setHeartRate(heartRate);
        record.setDistance(distance);
        return record;
    }
}
