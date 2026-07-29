package com.mc4.airun.logging;

import com.mc4.airun.logging.domain.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainingLogger {

    private static final Logger log = LoggerFactory.getLogger(TrainingLogger.class);

    public void log(Training training) {
        log.info("{}", training);
    }
}
