package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import com.mc4.airun.activities.events.RawActivity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {

    private final FitTrainingReader reader;
    private final ActivityPersistence persistence;

    public ActivityService(FitTrainingReader reader, ActivityPersistence persistence) {
        this.reader = reader;
        this.persistence = persistence;
    }

    @Transactional
    public Training ingest(RawActivity activity) {
        Training training = reader.read(activity.sourceName(), activity.content());
        persistence.save(training);
        return training;
    }

    @Transactional(readOnly = true)
    public Optional<Training> findBySourceName(String sourceName) {
        return persistence.findBySourceName(sourceName);
    }

    @Transactional(readOnly = true)
    public List<Training> findAll() {
        return persistence.findAll();
    }
}
