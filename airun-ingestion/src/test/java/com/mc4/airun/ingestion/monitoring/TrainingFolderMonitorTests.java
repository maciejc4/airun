package com.mc4.airun.ingestion.monitoring;

import com.mc4.airun.ingestion.events.RawActivity;
import com.mc4.airun.ingestion.events.RawActivityPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TrainingFolderMonitorTests {

    @TempDir
    Path directory;

    @Test
    void publishesStableFitFileOnlyOnce() throws IOException {
        RawActivityPublisher publisher = mock(RawActivityPublisher.class);
        TrainingFolderMonitor monitor = new TrainingFolderMonitor(directory, publisher);
        Files.write(directory.resolve("morning-run.fit"), new byte[]{1, 2, 3});
        Files.write(directory.resolve("notes.txt"), new byte[]{1});

        monitor.detectNewFiles();

        verify(publisher, never()).publish(any(RawActivity.class));

        monitor.detectNewFiles();
        monitor.detectNewFiles();

        verify(publisher, times(1)).publish(new RawActivity(
                "morning-run.fit",
                new byte[]{1, 2, 3}
        ));
    }
}
