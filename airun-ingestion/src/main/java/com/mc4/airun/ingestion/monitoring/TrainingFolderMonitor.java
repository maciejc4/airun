package com.mc4.airun.ingestion.monitoring;

import com.mc4.airun.ingestion.events.RawActivity;
import com.mc4.airun.ingestion.events.RawActivityPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TrainingFolderMonitor {

    private static final Logger log = LoggerFactory.getLogger(TrainingFolderMonitor.class);

    private final Path directory;
    private final RawActivityPublisher eventPublisher;
    private final Map<Path, FileState> observedFiles = new HashMap<>();
    private final Set<FileState> publishedFiles = new HashSet<>();

    public TrainingFolderMonitor(
            Path directory,
            RawActivityPublisher eventPublisher
    ) {
        this.directory = directory.toAbsolutePath().normalize();
        this.eventPublisher = eventPublisher;
    }

    public void detectNewFiles() {
        try {
            Files.createDirectories(directory);
            try (Stream<Path> paths = Files.list(directory)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isFitFile)
                        .sorted()
                        .forEach(this::inspect);
            }
        } catch (IOException exception) {
            log.error("Could not scan training directory '{}'", directory, exception);
        }
    }

    private void inspect(Path path) {
        try {
            FileState currentState = FileState.from(path);
            FileState previousState = observedFiles.put(path, currentState);
            if (currentState.equals(previousState) && !publishedFiles.contains(currentState)) {
                publish(currentState);
            }
        } catch (IOException exception) {
            log.warn("Could not inspect training file '{}': {}", path, exception.getMessage());
        }
    }

    private void publish(FileState file) {
        try {
            eventPublisher.publish(new RawActivity(
                    file.path().getFileName().toString(),
                    Files.readAllBytes(file.path())
            ));
            publishedFiles.add(file);
            log.info("Detected training file '{}'", file.path());
        } catch (IOException | RuntimeException exception) {
            log.warn(
                    "Could not process training file '{}'; it will be retried: {}",
                    file.path(),
                    exception.getMessage()
            );
        }
    }

    private boolean isFitFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fit");
    }

    private record FileState(Path path, long size, long lastModifiedMillis) {

        private static FileState from(Path path) throws IOException {
            return new FileState(
                    path.toAbsolutePath().normalize(),
                    Files.size(path),
                    Files.getLastModifiedTime(path).toMillis()
            );
        }
    }
}
