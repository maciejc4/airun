package com.mc4.airun.training;

import com.mc4.airun.training.domain.Training;
import com.mc4.airun.training.domain.TrainingSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingServiceTests {

    @Test
    void loadsClasspathTrainingsIntoMemory() {
        FitTrainingReader fitReader = mock(FitTrainingReader.class);
        when(fitReader.read(anyString(), any(byte[].class)))
                .thenAnswer(invocation -> training(invocation.getArgument(0)));
        TrainingService service = new TrainingService(
                "classpath*:training-fixtures/*.fit",
                fitReader
        );

        List<Training> loadedTrainings = service.loadTrainings();

        assertThat(loadedTrainings)
                .extracting(Training::sourceName)
                .containsExactly("a.fit", "z.fit");
        assertThat(service.trainings()).isEqualTo(loadedTrainings);
    }

    private Training training(String sourceName) {
        return new Training(
                sourceName,
                List.of(new TrainingSession(
                        Instant.parse("2026-07-29T08:00:00Z"),
                        "RUNNING",
                        Duration.ofMinutes(30),
                        5_000,
                        OptionalInt.of(140),
                        OptionalInt.of(165),
                        List.of()
                ))
        );
    }
}
