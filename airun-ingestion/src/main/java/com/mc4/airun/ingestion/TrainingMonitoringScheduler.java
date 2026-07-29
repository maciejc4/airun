package com.mc4.airun.ingestion;

import com.mc4.airun.ingestion.monitoring.TrainingFolderMonitor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrainingMonitoringScheduler {

    private final TrainingFolderMonitor monitor;

    public TrainingMonitoringScheduler(TrainingFolderMonitor monitor) {
        this.monitor = monitor;
    }

    @Scheduled(fixedDelayString = "${airun.ingestion.poll-interval}")
    public void detectNewFiles() {
        monitor.detectNewFiles();
    }
}
