package com.mc4.airun.activities;

import com.mc4.airun.activities.domain.Training;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trainings")
public class TrainingController {

    private final ActivityService activityService;

    public TrainingController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public List<Training> trainings() {
        return activityService.findAll();
    }

    @GetMapping("/{sourceName}")
    public ResponseEntity<Training> training(@PathVariable String sourceName) {
        return activityService.findBySourceName(sourceName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
